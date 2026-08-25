package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogCommandResult;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogConceptNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogRelationWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional application service for the current catalog writing surface. */
@Service
class CatalogCommandService implements CatalogCommands {

    private static final String ENTITY_TYPE = "INGREDIENT_CONCEPT";
    private final JdbcTemplate jdbcTemplate;
    private final CatalogQueries catalogQueries;
    private final CatalogAuditLog auditLog;
    private final CatalogGraphLock graphLock;

    CatalogCommandService(
            JdbcTemplate jdbcTemplate,
            CatalogQueries catalogQueries,
            CatalogAuditLog auditLog,
            CatalogGraphLock graphLock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogQueries = catalogQueries;
        this.auditLog = auditLog;
        this.graphLock = graphLock;
    }

    @Override
    @Transactional
    public CatalogCommandResult createIngredientConcept(CreateIngredientConceptCommand command) {
        validateMetadataReferences(command.metadata());
        MetadataState metadata = command.metadata() == null
                ? MetadataState.empty()
                : MetadataState.from(command.metadata());
        validateDrawability(command.active(), command.randomDrawEnabled(), metadata);
        List<String> weightWarnings = drawWeightWarnings(
                command.active(), command.randomDrawEnabled(), command.baseDrawWeight(), command.noveltyLevel(),
                metadata, false);
        if (!weightWarnings.isEmpty() && !command.weightWarningsAcknowledged()) {
            throw new CatalogDrawWeightWarningException(weightWarnings);
        }
        long conceptId;
        try {
            conceptId = jdbcTemplate.queryForObject(
                    """
                    insert into ingredient_concept (
                        code, display_name, active, random_draw_enabled, challenge_specificity,
                        base_draw_weight, novelty_level, curator_note
                    ) values (?, ?, ?, ?, ?, ?, ?, ?)
                    returning id
                    """,
                    Long.class,
                    command.code(), command.displayName(), command.active(), command.randomDrawEnabled(),
                    command.challengeSpecificity(), command.baseDrawWeight(), command.noveltyLevel(), command.curatorNote()
            );
        } catch (DataIntegrityViolationException exception) {
            throw knownUniqueConstraintOrRethrow(exception);
        }

        if (command.metadata() != null) {
            replaceMetadata(conceptId, command.metadata());
        }
        CatalogConceptDetail after = findRequired(conceptId);
        auditLog.append(new CatalogAuditEntryDraft(
                UUID.randomUUID(), command.actorKey(), ENTITY_TYPE, conceptId, "CREATE", null, snapshot(after)
        ));
        return new CatalogCommandResult(conceptId, after.version());
    }

    @Override
    @Transactional
    public CatalogCommandResult updateIngredientConcept(UpdateIngredientConceptCommand command) {
        return updateIngredientConceptAggregate(command);
    }

    /** Saves base fields and a complete pending direct-edge delta as one unit. */
    private CatalogCommandResult updateIngredientConceptAggregate(UpdateIngredientConceptCommand command) {
        validateMetadataReferences(command.metadata());
        Set<Long> affectedIds = affectedConceptIds(command);
        Map<Long, LockedConcept> locked = lockAndCheckVersions(command, affectedIds);
        boolean graphSemanticsChange = !command.refinementChanges().isEmpty()
                || !command.challengeSpecificity().equals(currentSpecificity(command.conceptId()));
        if (graphSemanticsChange) {
            // Acquire before reading the graph: row versions alone cannot prevent write-skew.
            graphLock.acquire();
        }
        Map<Long, CatalogConceptDetail> before = new LinkedHashMap<>();
        affectedIds.stream().sorted().forEach(id -> before.put(id, findRequired(id)));
        GraphState graph = null;
        if (graphSemanticsChange) {
            graph = loadGraph();
            graph.replace(
                    command.conceptId(), command.displayName(), command.challengeSpecificity(),
                    command.active(), command.randomDrawEnabled()
            );
            applyPendingRefinements(graph, command);
            validateGraph(graph);
        }
        MetadataState resultingMetadata = command.metadata() == null
                ? MetadataState.from(before.get(command.conceptId()))
                : MetadataState.from(command.metadata());
        validateDrawability(command.active(), command.randomDrawEnabled(), resultingMetadata);

        List<String> inactiveWarnings = command.refinementChanges().isEmpty()
                ? List.of()
                : inactiveRelationWarnings(command, graph);
        if (!inactiveWarnings.isEmpty() && !command.inactiveRelationsAcknowledged()) {
            throw new CatalogRelationWarningException(inactiveWarnings);
        }
        List<String> weightWarnings = drawWeightWarnings(
                command,
                resultingMetadata,
                graphSemanticsChange
                        ? graph.hasDirectParentCode(command.conceptId(), "COOKING_ALCOHOL")
                        : hasDirectParentCode(command.conceptId(), "COOKING_ALCOHOL")
        );
        if (!weightWarnings.isEmpty() && !command.weightWarningsAcknowledged()) {
            throw new CatalogDrawWeightWarningException(weightWarnings);
        }

        persistPendingRefinements(command);
        if (command.metadata() != null) {
            replaceMetadata(command.conceptId(), command.metadata());
        }
        updateAffectedVersionsAndBaseFields(command, locked);

        UUID changeGroupId = UUID.randomUUID();
        Map<Long, CatalogConceptDetail> after = new LinkedHashMap<>();
        affectedIds.stream().sorted().forEach(id -> after.put(id, findRequired(id)));
        for (long conceptId : affectedIds.stream().sorted().toList()) {
            auditLog.append(new CatalogAuditEntryDraft(
                    changeGroupId, command.actorKey(), ENTITY_TYPE, conceptId,
                    command.refinementChanges().isEmpty() ? "UPDATE" : "UPDATE_REFINEMENTS",
                    snapshot(before.get(conceptId)), snapshot(after.get(conceptId))
            ));
        }
        return new CatalogCommandResult(command.conceptId(), after.get(command.conceptId()).version());
    }

    private Set<Long> affectedConceptIds(UpdateIngredientConceptCommand command) {
        Set<Long> affected = new LinkedHashSet<>();
        affected.add(command.conceptId());
        Map<String, String> errors = new LinkedHashMap<>();
        Set<Edge> seenEdges = new HashSet<>();
        for (CatalogCommands.RefinementChange change : command.refinementChanges()) {
            if (change.parentConceptId() != command.conceptId() && change.childConceptId() != command.conceptId()) {
                errors.put("relations", "Eine Beziehung darf nur das aktuell bearbeitete Konzept betreffen.");
            }
            Edge edge = new Edge(change.parentConceptId(), change.childConceptId());
            if (!seenEdges.add(edge)) {
                errors.put("relations", "Dieselbe direkte Beziehung darf pro Speichern nur einmal geändert werden.");
            }
            affected.add(change.parentConceptId());
            affected.add(change.childConceptId());
        }
        for (long conceptId : affected) {
            if (conceptId != command.conceptId() && !command.expectedRelatedVersions().containsKey(conceptId)) {
                errors.put("relations", "Für alle beteiligten Zutaten muss die geladene Aggregatversion vorliegen.");
            }
        }
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
        return Set.copyOf(affected);
    }

    private Map<Long, LockedConcept> lockAndCheckVersions(
            UpdateIngredientConceptCommand command,
            Set<Long> affectedIds
    ) {
        List<Long> orderedIds = affectedIds.stream().sorted().toList();
        String placeholders = String.join(", ", java.util.Collections.nCopies(orderedIds.size(), "?"));
        Map<Long, LockedConcept> locked = new LinkedHashMap<>();
        jdbcTemplate.query("select id, version from ingredient_concept where id in (" + placeholders
                        + ") order by id for update",
                (RowCallbackHandler) resultSet -> locked.put(resultSet.getLong("id"), new LockedConcept(
                        resultSet.getLong("id"), resultSet.getLong("version"))), orderedIds.toArray());
        for (long id : orderedIds) {
            if (!locked.containsKey(id)) {
                throw new CatalogConceptNotFoundException(id);
            }
            long expected = id == command.conceptId()
                    ? command.expectedVersion()
                    : command.expectedRelatedVersions().get(id);
            if (locked.get(id).version() != expected) {
                throw new CatalogVersionConflictException(id, expected);
            }
        }
        return Map.copyOf(locked);
    }

    private String currentSpecificity(long conceptId) {
        return jdbcTemplate.queryForObject(
                "select challenge_specificity from ingredient_concept where id = ?", String.class, conceptId);
    }

    private boolean hasDirectParentCode(long conceptId, String parentCode) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    where relation.child_concept_id = ? and parent.code = ?
                )
                """, Boolean.class, conceptId, parentCode));
    }

    private GraphState loadGraph() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        jdbcTemplate.query(
                "select id, code, display_name, active, random_draw_enabled, challenge_specificity "
                        + "from ingredient_concept order by id",
                (RowCallbackHandler) resultSet -> nodes.put(resultSet.getLong("id"), new GraphNode(
                        resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("display_name"),
                        resultSet.getBoolean("active"), resultSet.getBoolean("random_draw_enabled"),
                        resultSet.getString("challenge_specificity"))),
                new Object[0]
        );
        Map<Edge, Boolean> edges = new LinkedHashMap<>();
        jdbcTemplate.query("select parent_concept_id, child_concept_id from ingredient_refinement order by parent_concept_id, child_concept_id",
                (RowCallbackHandler) resultSet -> edges.put(new Edge(resultSet.getLong("parent_concept_id"), resultSet.getLong("child_concept_id")), Boolean.TRUE));
        return new GraphState(nodes, edges.keySet());
    }

    private void applyPendingRefinements(GraphState graph, UpdateIngredientConceptCommand command) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (CatalogCommands.RefinementChange change : command.refinementChanges()) {
            Edge edge = new Edge(change.parentConceptId(), change.childConceptId());
            if (change.type() == CatalogCommands.RefinementChangeType.ADD) {
                if (!graph.edges().add(edge)) {
                    errors.put("relations", "Die direkte Beziehung " + graph.edgeName(edge) + " besteht bereits.");
                }
            } else if (!graph.edges().remove(edge)) {
                errors.put("relations", "Die direkte Beziehung " + graph.edgeName(edge) + " besteht nicht mehr.");
            }
        }
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
    }

    private void validateGraph(GraphState graph) {
        for (Edge edge : graph.edges()) {
            GraphNode parent = graph.requireNode(edge.parentId());
            GraphNode child = graph.requireNode(edge.childId());
            if (edge.parentId() == edge.childId()) {
                throw relationError("Eine Zutat kann nicht ihre eigene Konkretisierung sein.");
            }
            if ("SPECIFIC".equals(parent.challengeSpecificity()) && "OPEN".equals(child.challengeSpecificity())) {
                throw relationError("Eine spezifische Zutat darf keine offene direkte Konkretisierung haben: "
                        + graph.edgeName(edge) + ".");
            }
        }
        Edge cycle = graph.firstCycleEdge();
        if (cycle != null) {
            throw relationError("Die direkte Beziehung würde einen Zyklus erzeugen: " + graph.edgeName(cycle) + ".");
        }
        for (Edge edge : graph.edges()) {
            if (graph.hasAlternativePath(edge)) {
                throw relationError("Die direkte Beziehung " + graph.edgeName(edge)
                        + " ist über einen anderen Pfad bereits transitiv ableitbar. Entferne bewusst eine der Kanten.");
            }
        }
    }

    private List<String> inactiveRelationWarnings(UpdateIngredientConceptCommand command, GraphState graph) {
        return command.refinementChanges().stream()
                .filter(change -> change.type() == CatalogCommands.RefinementChangeType.ADD)
                .map(change -> new Edge(change.parentConceptId(), change.childConceptId()))
                .filter(edge -> !graph.requireNode(edge.parentId()).active() || !graph.requireNode(edge.childId()).active())
                .map(edge -> "Die Beziehung " + graph.edgeName(edge) + " betrifft ein inaktives Konzept.")
                .toList();
    }

    private void persistPendingRefinements(UpdateIngredientConceptCommand command) {
        try {
            command.refinementChanges().stream()
                    .filter(change -> change.type() == CatalogCommands.RefinementChangeType.REMOVE)
                    .sorted(Comparator.comparingLong(CatalogCommands.RefinementChange::parentConceptId)
                            .thenComparingLong(CatalogCommands.RefinementChange::childConceptId))
                    .forEach(change -> jdbcTemplate.update("delete from ingredient_refinement "
                            + "where parent_concept_id = ? and child_concept_id = ?",
                            change.parentConceptId(), change.childConceptId()));
            command.refinementChanges().stream()
                    .filter(change -> change.type() == CatalogCommands.RefinementChangeType.ADD)
                    .sorted(Comparator.comparingLong(CatalogCommands.RefinementChange::parentConceptId)
                            .thenComparingLong(CatalogCommands.RefinementChange::childConceptId))
                    .forEach(change -> jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) "
                            + "values (?, ?)", change.parentConceptId(), change.childConceptId()));
        } catch (DataAccessException exception) {
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("ingredient refinement would create a cycle")) {
                throw relationError("Die Datenbank hat einen Zyklus in den direkten Beziehungen erkannt.");
            }
            throw exception;
        }
    }

    private void updateAffectedVersionsAndBaseFields(
            UpdateIngredientConceptCommand command,
            Map<Long, LockedConcept> locked
    ) {
        int centralUpdated;
        try {
            centralUpdated = jdbcTemplate.update(
                    """
                    update ingredient_concept
                    set display_name = ?, active = ?, random_draw_enabled = ?, challenge_specificity = ?,
                        base_draw_weight = ?, novelty_level = ?, curator_note = ?, version = version + 1
                    where id = ? and version = ?
                    """,
                    command.displayName(), command.active(), command.randomDrawEnabled(), command.challengeSpecificity(),
                    command.baseDrawWeight(), command.noveltyLevel(), command.curatorNote(),
                    command.conceptId(), command.expectedVersion());
        } catch (DataIntegrityViolationException exception) {
            throw knownUniqueConstraintOrRethrow(exception);
        }
        if (centralUpdated != 1) {
            throw new CatalogVersionConflictException(command.conceptId(), command.expectedVersion());
        }
        locked.keySet().stream().filter(id -> id != command.conceptId()).sorted().forEach(id -> {
            long expected = command.expectedRelatedVersions().get(id);
            if (jdbcTemplate.update("update ingredient_concept set version = version + 1 where id = ? and version = ?", id, expected) != 1) {
                throw new CatalogVersionConflictException(id, expected);
            }
        });
    }

    private void validateDrawability(boolean active, boolean randomDrawEnabled, MetadataState metadata) {
        if (!active || !randomDrawEnabled) {
            return;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (metadata.functionalRoleCodes().isEmpty()) {
            errors.put("functionalRoles", "Ziehbare aktive Konzepte benötigen mindestens eine funktionale Rolle.");
        }
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
    }

    private static CatalogCommandValidationException relationError(String message) {
        return new CatalogCommandValidationException(Map.of("relations", message));
    }

    private CatalogConceptDetail findRequired(long conceptId) {
        return catalogQueries.findConcept(conceptId)
                .orElseThrow(() -> new CatalogConceptNotFoundException(conceptId));
    }

    private List<String> drawWeightWarnings(
            UpdateIngredientConceptCommand command,
            MetadataState metadata,
            boolean directCookingAlcoholParent
    ) {
        return drawWeightWarnings(
                command.active(), command.randomDrawEnabled(), command.baseDrawWeight(), command.noveltyLevel(),
                metadata, directCookingAlcoholParent);
    }

    private List<String> drawWeightWarnings(
            boolean active,
            boolean randomDrawEnabled,
            BigDecimal weight,
            Integer noveltyLevel,
            MetadataState metadata,
            boolean directCookingAlcoholParent
    ) {
        if (!active || !randomDrawEnabled) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        if (noveltyLevel != null) {
            BigDecimal cap = switch (noveltyLevel) {
                case 5 -> new BigDecimal("0.25");
                case 4 -> new BigDecimal("0.35");
                case 3 -> new BigDecimal("0.55");
                default -> null;
            };
            if (cap != null && weight.compareTo(cap) > 0) {
                warnings.add("Die Ungewöhnlichkeit Stufe %d hat in der Baseline einen Richtwert von höchstens %s."
                        .formatted(noveltyLevel, cap));
            }
        }
        if (metadata.availabilityByParticipant().containsValue(CatalogQueries.CatalogAvailability.DIFFICULT)
                && weight.compareTo(new BigDecimal("0.35")) > 0) {
            warnings.add("Schwierig beschaffbare Konzepte haben in der Baseline einen Richtwert von höchstens 0.35.");
        }
        if (directCookingAlcoholParent && weight.compareTo(new BigDecimal("0.35")) > 0) {
            warnings.add("Direkte Konkretisierungen von Kochalkohol haben in der Baseline einen Richtwert von höchstens 0.35.");
        }
        return List.copyOf(warnings);
    }

    private void validateMetadataReferences(CatalogMetadata metadata) {
        if (metadata == null) {
            return;
        }
        requireKnownCodes("functionalRoles", metadata.functionalRoleCodes(), "functional_role");
        requireKnownCodes("culinaryFlags", metadata.culinaryFlagCodes(), "culinary_flag");
        requireKnownCodes("culinaryDimensions", metadata.culinaryDimensionLevels().keySet(), "culinary_dimension");
        if (metadata.culinaryCountryCodes() != null) {
            requireKnownCodes("culinaryCountries", metadata.culinaryCountryCodes(), "culinary_country");
        }
        Set<String> participantCodes = metadata.availabilityByParticipant().keySet();
        requireKnownCodes("availability", participantCodes, "participant");
        if (!Set.of("GEORGIA", "TOBIAS").containsAll(participantCodes)) {
            throw new CatalogCommandValidationException(Map.of(
                    "availability", "Beschaffbarkeit darf nur für Georgia und Tobias gepflegt werden."));
        }
    }

    private void requireKnownCodes(String field, Set<String> codes, String table) {
        if (codes.isEmpty()) {
            return;
        }
        List<String> known = jdbcTemplate.queryForList(
                "select code from " + table + " where code in (" + placeholders(codes.size()) + ")",
                String.class,
                codes.toArray());
        if (known.size() != codes.size()) {
            throw new CatalogCommandValidationException(Map.of(field, "Eine übermittelte Referenz ist nicht bekannt."));
        }
    }

    private void replaceMetadata(long conceptId, CatalogMetadata metadata) {
        jdbcTemplate.update("delete from ingredient_functional_role where ingredient_concept_id = ?", conceptId);
        metadata.functionalRoleCodes().stream().sorted().forEach(code -> jdbcTemplate.update(
                "insert into ingredient_functional_role (ingredient_concept_id, functional_role_id) "
                        + "select ?, id from functional_role where code = ?", conceptId, code));

        jdbcTemplate.update("delete from ingredient_culinary_flag where ingredient_concept_id = ?", conceptId);
        metadata.culinaryFlagCodes().stream().sorted().forEach(code -> jdbcTemplate.update(
                "insert into ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id) "
                        + "select ?, id from culinary_flag where code = ?", conceptId, code));

        jdbcTemplate.update("delete from ingredient_culinary_dimension where ingredient_concept_id = ?", conceptId);
        metadata.culinaryDimensionLevels().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> jdbcTemplate.update(
                "insert into ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level) "
                        + "select ?, id, ? from culinary_dimension where code = ?",
                conceptId, entry.getValue(), entry.getKey()));

        if (metadata.culinaryCountryCodes() != null) {
            jdbcTemplate.update("delete from ingredient_culinary_country where ingredient_concept_id = ?", conceptId);
            metadata.culinaryCountryCodes().stream().sorted().forEach(code -> jdbcTemplate.update(
                    "insert into ingredient_culinary_country (ingredient_concept_id, country_code) values (?, ?)",
                    conceptId, code));
        }

        jdbcTemplate.update("delete from ingredient_availability where ingredient_concept_id = ? "
                + "and participant_id in (select id from participant where code in ('GEORGIA', 'TOBIAS'))", conceptId);
        metadata.availabilityByParticipant().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> jdbcTemplate.update(
                "insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level) "
                        + "select ?, id, ? from participant where code = ?",
                conceptId, entry.getValue().name(), entry.getKey()));

        jdbcTemplate.update("delete from ingredient_seasonality where ingredient_concept_id = ?", conceptId);
        metadata.seasonalityByMonth().entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ONE) != 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> jdbcTemplate.update(
                        "insert into ingredient_seasonality (ingredient_concept_id, month, weight_multiplier) values (?, ?, ?)",
                        conceptId, entry.getKey(), entry.getValue()));
    }

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    private RuntimeException knownUniqueConstraintOrRethrow(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("ingredient_concept_code_key")) {
            return new CatalogCommandValidationException(Map.of("code", "Dieser Code wird bereits verwendet."));
        }
        if (detail != null && detail.contains("uq_ingredient_concept_display_name_ci")) {
            return new CatalogCommandValidationException(Map.of("displayName", "Dieser Anzeigename wird bereits verwendet."));
        }
        return exception;
    }

    private static CatalogAggregateSnapshot snapshot(CatalogConceptDetail detail) {
        return CatalogIngredientSnapshotFactory.snapshot(detail);
    }

    private record LockedConcept(long id, long version) {
    }

    private record MetadataState(
            Set<String> functionalRoleCodes,
            Map<String, CatalogQueries.CatalogAvailability> availabilityByParticipant
    ) {

        private static MetadataState empty() {
            return new MetadataState(Set.of(), Map.of());
        }

        private static MetadataState from(CatalogMetadata metadata) {
            return new MetadataState(metadata.functionalRoleCodes(), metadata.availabilityByParticipant());
        }

        private static MetadataState from(CatalogConceptDetail detail) {
            Set<String> roles = detail.functionalRoles().stream()
                    .map(CatalogQueries.CatalogReferenceValue::code)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            Map<String, CatalogQueries.CatalogAvailability> availability = new LinkedHashMap<>();
            detail.availability().forEach(entry -> {
                if (entry.level() != null) {
                    availability.put(entry.participant().code(), entry.level());
                }
            });
            return new MetadataState(roles, Map.copyOf(availability));
        }
    }

    private record GraphNode(
            long id,
            String code,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity
    ) {
    }

    private record Edge(long parentId, long childId) {
    }

    /** Mutable, transaction-local view of the result graph; it is never exposed from the module. */
    private static final class GraphState {

        private final Map<Long, GraphNode> nodes;
        private final Set<Edge> edges;

        private GraphState(Map<Long, GraphNode> nodes, Set<Edge> edges) {
            this.nodes = new LinkedHashMap<>(nodes);
            this.edges = new LinkedHashSet<>(edges);
        }

        private Set<Edge> edges() {
            return edges;
        }

        private void replace(
                long conceptId,
                String displayName,
                String specificity,
                boolean active,
                boolean randomDrawEnabled
        ) {
            GraphNode current = requireNode(conceptId);
            nodes.put(conceptId, new GraphNode(
                    current.id(), current.code(), displayName, active, randomDrawEnabled, specificity));
        }

        private GraphNode requireNode(long id) {
            GraphNode node = nodes.get(id);
            if (node == null) {
                throw new CatalogConceptNotFoundException(id);
            }
            return node;
        }

        private boolean hasDirectParentCode(long childId, String parentCode) {
            return edges.stream().anyMatch(edge -> edge.childId() == childId
                    && parentCode.equals(requireNode(edge.parentId()).code()));
        }

        private String edgeName(Edge edge) {
            return "„" + requireNode(edge.parentId()).displayName() + " → "
                    + requireNode(edge.childId()).displayName() + "“";
        }

        private Edge firstCycleEdge() {
            return edges.stream().sorted(Comparator.comparingLong(Edge::parentId).thenComparingLong(Edge::childId))
                    .filter(edge -> hasPath(edge.childId(), edge.parentId(), null))
                    .findFirst().orElse(null);
        }

        private boolean hasAlternativePath(Edge edge) {
            return hasPath(edge.parentId(), edge.childId(), edge);
        }

        private boolean hasPath(long from, long target, Edge excludedEdge) {
            Set<Long> visited = new HashSet<>();
            ArrayList<Long> queue = new ArrayList<>();
            queue.add(from);
            visited.add(from);
            for (int index = 0; index < queue.size(); index++) {
                long current = queue.get(index);
                for (Edge edge : edges) {
                    if (edge.equals(excludedEdge) || edge.parentId() != current || !visited.add(edge.childId())) {
                        continue;
                    }
                    if (edge.childId() == target) {
                        return true;
                    }
                    queue.add(edge.childId());
                }
            }
            return false;
        }
    }
}
