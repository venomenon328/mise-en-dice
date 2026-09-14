package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogCommandResult;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogConceptNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogNameCollisionWarningException;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional application service for the current catalog writing surface. */
@Service
class CatalogCommandService implements CatalogCommands {

    private final JdbcTemplate jdbcTemplate;
    private final CatalogQueries catalogQueries;
    private final CatalogGraphLock graphLock;
    private final CatalogNameLock nameLock;

    CatalogCommandService(
            JdbcTemplate jdbcTemplate,
            CatalogQueries catalogQueries,
            CatalogGraphLock graphLock,
            CatalogNameLock nameLock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogQueries = catalogQueries;
        this.graphLock = graphLock;
        this.nameLock = nameLock;
    }

    @Override
    @Transactional
    public CatalogCommandResult createIngredientConcept(CreateIngredientConceptCommand command) {
        validateMetadataReferences(command.metadata());
        nameLock.acquire();
        validateAndAcknowledgeNameCollisions(0, List.of(), command.displayName(), command.aliases(),
                command.nameCollisionAcknowledgements());
        MetadataState metadata = command.metadata() == null
                ? MetadataState.empty()
                : MetadataState.from(command.metadata());
        validateDrawability(command.active(), command.randomDrawEnabled(), metadata);
        List<String> weightWarnings = drawWeightWarnings(
                command.active(), command.randomDrawEnabled(), command.baseDrawWeight(), false);
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
        replaceAliases(conceptId, command.aliases());
        return new CatalogCommandResult(conceptId, findRequired(conceptId).version());
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
        CatalogConceptDetail current = before.get(command.conceptId());
        List<String> resultingAliases = command.aliases() == null ? current.aliases() : command.aliases();
        validateAliasSet(command.displayName(), resultingAliases);
        if (nameState(command.displayName(), resultingAliases)
                .equals(nameState(current.displayName(), current.aliases()))) {
            // An unrelated aggregate save must not reopen already approved ambiguities.
        } else {
            nameLock.acquire();
            validateAndAcknowledgeNameCollisions(
                    command.conceptId(), occurrences(current.displayName(), current.aliases()),
                    command.displayName(), resultingAliases, command.nameCollisionAcknowledgements());
        }
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
        if (command.aliases() != null) {
            jdbcTemplate.update("delete from ingredient_concept_alias where ingredient_concept_id = ?",
                    command.conceptId());
        }
        updateAffectedVersionsAndBaseFields(command, locked);
        if (command.aliases() != null) {
            insertAliases(command.conceptId(), command.aliases());
        }

        return new CatalogCommandResult(command.conceptId(), findRequired(command.conceptId()).version());
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
            boolean directCookingAlcoholParent
    ) {
        return drawWeightWarnings(
                command.active(), command.randomDrawEnabled(), command.baseDrawWeight(), directCookingAlcoholParent);
    }

    private List<String> drawWeightWarnings(
            boolean active,
            boolean randomDrawEnabled,
            BigDecimal weight,
            boolean directCookingAlcoholParent
    ) {
        if (!active || !randomDrawEnabled) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
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

        // Replace levels for the supported participants, preserving omitted notes for older callers.
        for (String participant : List.of("GEORGIA", "TOBIAS")) {
            var level = metadata.availabilityByParticipant().get(participant);
            if (level == null) {
                jdbcTemplate.update("delete from ingredient_availability where ingredient_concept_id = ? "
                        + "and participant_id = (select id from participant where code = ?)", conceptId, participant);
                continue;
            }
            boolean noteSupplied = metadata.availabilityNotesByParticipant().containsKey(participant);
            String note = metadata.availabilityNotesByParticipant().get(participant);
            note = note == null || note.isBlank() ? null : note.strip();
            jdbcTemplate.update("""
                    insert into ingredient_availability
                        (ingredient_concept_id, participant_id, availability_level, curator_note)
                    select ?, id, ?, ? from participant where code = ?
                    on conflict (ingredient_concept_id, participant_id) do update
                    set availability_level = excluded.availability_level,
                        curator_note = case when ? then excluded.curator_note else ingredient_availability.curator_note end
                    """, conceptId, level.name(), note, participant, noteSupplied);
        }

        jdbcTemplate.update("delete from ingredient_seasonality where ingredient_concept_id = ?", conceptId);
        metadata.seasonalityByMonth().entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ONE) != 0)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> jdbcTemplate.update(
                        "insert into ingredient_seasonality (ingredient_concept_id, month, weight_multiplier) values (?, ?, ?)",
                        conceptId, entry.getKey(), entry.getValue()));
    }

    private void replaceAliases(long conceptId, List<String> aliases) {
        jdbcTemplate.update("delete from ingredient_concept_alias where ingredient_concept_id = ?", conceptId);
        insertAliases(conceptId, aliases);
    }

    private void insertAliases(long conceptId, List<String> aliases) {
        aliases.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                .forEach(alias -> jdbcTemplate.update(
                        "insert into ingredient_concept_alias (ingredient_concept_id, alias_text) values (?, ?)",
                        conceptId, alias));
    }

    private void validateAliasSet(String displayName, List<String> aliases) {
        Set<String> identities = new HashSet<>();
        String displayIdentity = normalizeName(displayName);
        for (String alias : aliases) {
            String identity = normalizeName(alias);
            if (identity.isEmpty()) {
                throw new CatalogCommandValidationException(Map.of("aliases", "Aliasse dürfen nicht leer sein."));
            }
            if (identity.equals(displayIdentity)) {
                throw new CatalogCommandValidationException(Map.of(
                        "aliases", "Ein Alias darf nicht dem eigenen Anzeigenamen entsprechen."));
            }
            if (!identities.add(identity)) {
                throw new CatalogCommandValidationException(Map.of(
                        "aliases", "Derselbe Alias darf unabhängig von Groß-/Kleinschreibung nur einmal vorkommen."));
            }
        }
    }

    private void validateAndAcknowledgeNameCollisions(
            long conceptId,
            List<NameOccurrence> previousOccurrences,
            String displayName,
            List<String> aliases,
            Set<CatalogCommands.NameCollisionAcknowledgement> acknowledgements
    ) {
        List<NameOccurrence> resulting = occurrences(displayName, aliases);
        Set<NameOccurrenceIdentity> previous = previousOccurrences.stream()
                .map(NameOccurrence::identity)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<NameOccurrence> affected = resulting.stream()
                .filter(occurrence -> !previous.contains(occurrence.identity()))
                .toList();
        if (affected.isEmpty()) {
            return;
        }
        Set<String> normalizedTexts = affected.stream().map(NameOccurrence::normalizedText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        String placeholders = placeholders(normalizedTexts.size());
        List<OtherNameOccurrence> others = jdbcTemplate.query("""
                select concept.id, concept.code, concept.display_name, concept.display_name as matched_text,
                       false as alias
                from ingredient_concept concept
                where concept.id <> ? and lower(btrim(concept.display_name)) in (%s)
                union all
                select concept.id, concept.code, concept.display_name, alias.alias_text as matched_text,
                       true as alias
                from ingredient_concept_alias alias
                join ingredient_concept concept on concept.id = alias.ingredient_concept_id
                where concept.id <> ? and lower(btrim(alias.alias_text)) in (%s)
                order by 2, 1, 4
                """.formatted(placeholders, placeholders), (resultSet, rowNumber) -> new OtherNameOccurrence(
                resultSet.getLong("id"), resultSet.getString("code"), resultSet.getString("display_name"),
                resultSet.getString("matched_text"), resultSet.getBoolean("alias")),
                collisionArguments(conceptId, normalizedTexts));

        for (NameOccurrence local : affected) {
            if (local.alias()) {
                continue;
            }
            boolean canonicalDuplicate = others.stream().anyMatch(other -> !other.alias()
                    && normalizeName(other.matchedText()).equals(local.normalizedText()));
            if (canonicalDuplicate) {
                throw new CatalogCommandValidationException(Map.of(
                        "displayName", "Dieser Anzeigename wird bereits als kanonischer Name verwendet."));
            }
        }

        List<CatalogCommands.NameCollision> collisions = new ArrayList<>();
        for (NameOccurrence local : affected) {
            others.stream()
                    .filter(other -> normalizeName(other.matchedText()).equals(local.normalizedText()))
                    .filter(other -> local.alias() || other.alias())
                    .map(other -> new CatalogCommands.NameCollision(
                            new CatalogCommands.NameCollisionAcknowledgement(
                                    other.conceptId(), local.normalizedText()),
                            local.text(), other.code(), other.displayName(), other.matchedText()))
                    .forEach(collisions::add);
        }
        collisions.sort(Comparator.comparing(
                        (CatalogCommands.NameCollision collision) -> collision.acknowledgement().normalizedText())
                .thenComparing(CatalogCommands.NameCollision::otherConceptCode)
                .thenComparing(CatalogCommands.NameCollision::otherText));
        if (!collisions.isEmpty()) {
            Set<CatalogCommands.NameCollisionAcknowledgement> required = collisions.stream()
                    .map(CatalogCommands.NameCollision::acknowledgement)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (!acknowledgements.containsAll(required)) {
                throw new CatalogNameCollisionWarningException(collisions);
            }
        }
    }

    private static Object[] collisionArguments(long conceptId, Set<String> normalizedTexts) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(conceptId);
        arguments.addAll(normalizedTexts);
        arguments.add(conceptId);
        arguments.addAll(normalizedTexts);
        return arguments.toArray();
    }

    private static Set<NameOccurrenceIdentity> nameState(String displayName, List<String> aliases) {
        return occurrences(displayName, aliases).stream().map(NameOccurrence::identity)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static List<NameOccurrence> occurrences(String displayName, List<String> aliases) {
        List<NameOccurrence> occurrences = new ArrayList<>();
        occurrences.add(new NameOccurrence(displayName, normalizeName(displayName), false));
        aliases.forEach(alias -> occurrences.add(new NameOccurrence(alias, normalizeName(alias), true)));
        return List.copyOf(occurrences);
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
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

    private record LockedConcept(long id, long version) {
    }

    private record NameOccurrence(String text, String normalizedText, boolean alias) {
        private NameOccurrenceIdentity identity() {
            return new NameOccurrenceIdentity(normalizedText, alias);
        }
    }

    private record NameOccurrenceIdentity(String normalizedText, boolean alias) {
    }

    private record OtherNameOccurrence(
            long conceptId,
            String code,
            String displayName,
            String matchedText,
            boolean alias
    ) {
    }

    private record MetadataState(Set<String> functionalRoleCodes) {
        private static MetadataState empty() {
            return new MetadataState(Set.of());
        }

        private static MetadataState from(CatalogMetadata metadata) {
            return new MetadataState(metadata.functionalRoleCodes());
        }

        private static MetadataState from(CatalogConceptDetail detail) {
            return new MetadataState(detail.functionalRoles().stream()
                    .map(CatalogQueries.CatalogReferenceValue::code)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet()));
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
