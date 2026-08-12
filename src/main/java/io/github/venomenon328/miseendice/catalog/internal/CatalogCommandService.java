package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogCommandResult;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional application service for the first, deliberately small catalog writing surface. */
@Service
class CatalogCommandService implements CatalogCommands {

    private static final String ENTITY_TYPE = "INGREDIENT_CONCEPT";
    /** Stable, transaction-scoped PostgreSQL lock key for every refinement-graph mutation. */
    private static final long REFINEMENT_GRAPH_LOCK_KEY = 6_241_884_431_947_221L;

    private final JdbcTemplate jdbcTemplate;
    private final CatalogQueries catalogQueries;
    private final CatalogAuditLog auditLog;

    CatalogCommandService(JdbcTemplate jdbcTemplate, CatalogQueries catalogQueries, CatalogAuditLog auditLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogQueries = catalogQueries;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public CatalogCommandResult createIngredientConcept(CreateIngredientConceptCommand command) {
        long conceptId;
        try {
            conceptId = jdbcTemplate.queryForObject(
                    """
                    insert into ingredient_concept (
                        code, display_name, active, random_draw_enabled, challenge_specificity,
                        base_draw_weight, novelty_level, curator_note
                    ) values (?, ?, true, false, 'SPECIFIC', 1.0000, null, null)
                    returning id
                    """,
                    Long.class,
                    command.code(), command.displayName()
            );
        } catch (DataIntegrityViolationException exception) {
            throw knownUniqueConstraintOrRethrow(exception);
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
        if (!command.refinementChanges().isEmpty()) {
            return updateIngredientConceptWithRefinements(command);
        }
        CatalogConceptDetail before = findRequired(command.conceptId());
        validateDrawability(command, before);
        validateSpecificityGraph(command, before);

        List<String> warnings = drawWeightWarnings(command, before);
        if (!warnings.isEmpty() && !command.weightWarningsAcknowledged()) {
            throw new CatalogDrawWeightWarningException(warnings);
        }

        int updated;
        try {
            updated = jdbcTemplate.update(
                    """
                    update ingredient_concept
                    set display_name = ?, active = ?, random_draw_enabled = ?, challenge_specificity = ?,
                        base_draw_weight = ?, novelty_level = ?, curator_note = ?, version = version + 1
                    where id = ? and version = ?
                    """,
                    command.displayName(), command.active(), command.randomDrawEnabled(), command.challengeSpecificity(),
                    command.baseDrawWeight(), command.noveltyLevel(), command.curatorNote(),
                    command.conceptId(), command.expectedVersion()
            );
        } catch (DataIntegrityViolationException exception) {
            throw knownUniqueConstraintOrRethrow(exception);
        }
        if (updated == 0) {
            if (conceptExists(command.conceptId())) {
                throw new CatalogVersionConflictException(command.conceptId(), command.expectedVersion());
            }
            throw new CatalogConceptNotFoundException(command.conceptId());
        }

        CatalogConceptDetail after = findRequired(command.conceptId());
        auditLog.append(new CatalogAuditEntryDraft(
                UUID.randomUUID(), command.actorKey(), ENTITY_TYPE, after.id(), "UPDATE", snapshot(before), snapshot(after)
        ));
        return new CatalogCommandResult(after.id(), after.version());
    }

    /**
     * Saves base fields and a complete pending direct-edge delta as one unit. The advisory lock is
     * deliberately obtained before even reading the graph: version checks alone cannot prevent a
     * disjoint write-skew cycle in two application processes.
     */
    private CatalogCommandResult updateIngredientConceptWithRefinements(UpdateIngredientConceptCommand command) {
        jdbcTemplate.execute("select pg_advisory_xact_lock(" + REFINEMENT_GRAPH_LOCK_KEY + ")");

        Set<Long> affectedIds = affectedConceptIds(command);
        Map<Long, LockedConcept> locked = lockAndCheckVersions(command, affectedIds);
        Map<Long, CatalogConceptDetail> before = new LinkedHashMap<>();
        affectedIds.stream().sorted().forEach(id -> before.put(id, findRequired(id)));

        GraphState graph = loadGraph();
        graph.replace(command.conceptId(), command.challengeSpecificity(), command.active(), command.randomDrawEnabled());
        applyPendingRefinements(graph, command);
        validateGraph(graph);
        validateDrawability(command, before.get(command.conceptId()), graph.directChildCount(command.conceptId()));

        List<String> inactiveWarnings = inactiveRelationWarnings(command, graph);
        if (!inactiveWarnings.isEmpty() && !command.inactiveRelationsAcknowledged()) {
            throw new CatalogRelationWarningException(inactiveWarnings);
        }
        List<String> weightWarnings = drawWeightWarnings(command, before.get(command.conceptId()));
        if (!weightWarnings.isEmpty() && !command.weightWarningsAcknowledged()) {
            throw new CatalogDrawWeightWarningException(weightWarnings);
        }

        persistPendingRefinements(command);
        updateAffectedVersionsAndBaseFields(command, locked);

        UUID changeGroupId = UUID.randomUUID();
        Map<Long, CatalogConceptDetail> after = new LinkedHashMap<>();
        affectedIds.stream().sorted().forEach(id -> after.put(id, findRequired(id)));
        for (long conceptId : affectedIds.stream().sorted().toList()) {
            auditLog.append(new CatalogAuditEntryDraft(
                    changeGroupId, command.actorKey(), ENTITY_TYPE, conceptId, "UPDATE_REFINEMENTS",
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
                errors.put("relations", "Dieselbe direkte Beziehung darf pro Speichern nur einmal geÃ¤ndert werden.");
            }
            affected.add(change.parentConceptId());
            affected.add(change.childConceptId());
        }
        for (long conceptId : affected) {
            if (conceptId != command.conceptId() && !command.expectedRelatedVersions().containsKey(conceptId)) {
                errors.put("relations", "FÃ¼r alle beteiligten Zutaten muss die geladene Aggregatversion vorliegen.");
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

    private GraphState loadGraph() {
        Map<Long, GraphNode> nodes = new LinkedHashMap<>();
        jdbcTemplate.query("select id, display_name, active, random_draw_enabled, challenge_specificity from ingredient_concept order by id",
                (RowCallbackHandler) resultSet -> nodes.put(resultSet.getLong("id"), new GraphNode(
                        resultSet.getLong("id"), resultSet.getString("display_name"), resultSet.getBoolean("active"),
                        resultSet.getBoolean("random_draw_enabled"), resultSet.getString("challenge_specificity"))),
                new Object[0]);
        Map<Long, Set<String>> roles = new HashMap<>();
        jdbcTemplate.query("select ifr.ingredient_concept_id, fr.code from ingredient_functional_role ifr "
                        + "join functional_role fr on fr.id = ifr.functional_role_id",
                (RowCallbackHandler) resultSet -> roles.computeIfAbsent(resultSet.getLong("ingredient_concept_id"), ignored -> new HashSet<>())
                        .add(resultSet.getString("code")));
        Map<Edge, Boolean> edges = new LinkedHashMap<>();
        jdbcTemplate.query("select parent_concept_id, child_concept_id from ingredient_refinement order by parent_concept_id, child_concept_id",
                (RowCallbackHandler) resultSet -> edges.put(new Edge(resultSet.getLong("parent_concept_id"), resultSet.getLong("child_concept_id")), Boolean.TRUE));
        return new GraphState(nodes, roles, edges.keySet());
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
            Set<String> commonRoles = new HashSet<>(graph.rolesFor(edge.parentId()));
            commonRoles.retainAll(graph.rolesFor(edge.childId()));
            if (commonRoles.isEmpty()) {
                throw relationError("Die direkte Beziehung " + graph.edgeName(edge)
                        + " hat keine gemeinsame funktionale Rolle.");
            }
        }
        Edge cycle = graph.firstCycleEdge();
        if (cycle != null) {
            throw relationError("Die direkte Beziehung wÃ¼rde einen Zyklus erzeugen: " + graph.edgeName(cycle) + ".");
        }
        for (Edge edge : graph.edges()) {
            if (graph.hasAlternativePath(edge)) {
                throw relationError("Die direkte Beziehung " + graph.edgeName(edge)
                        + " ist Ã¼ber einen anderen Pfad bereits transitiv ableitbar. Entferne bewusst eine der Kanten.");
            }
        }
        for (GraphNode node : graph.nodes().values()) {
            if (node.active() && node.randomDrawEnabled() && "OPEN".equals(node.challengeSpecificity())
                    && graph.directChildCount(node.id()) == 0) {
                throw relationError("Die aktive ziehbare offene Vorgabe „" + node.displayName()
                        + "“ benÃ¶tigt mindestens eine direkte Konkretisierung.");
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

    private void validateDrawability(
            UpdateIngredientConceptCommand command,
            CatalogConceptDetail current,
            int directChildren
    ) {
        if (!command.active() || !command.randomDrawEnabled()) {
            return;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (current.functionalRoles().isEmpty()) {
            errors.put("functionalRoles", "Ziehbare aktive Konzepte benÃ¶tigen mindestens eine funktionale Rolle.");
        }
        boolean georgiaAvailable = current.availability().stream()
                .anyMatch(entry -> entry.participant().code().equals("GEORGIA") && entry.level() != null);
        if (!georgiaAvailable) {
            errors.put("availabilityGeorgia", "Ziehbare aktive Konzepte benÃ¶tigen eine Beschaffbarkeit fÃ¼r Georgia.");
        }
        boolean tobiasAvailable = current.availability().stream()
                .anyMatch(entry -> entry.participant().code().equals("TOBIAS") && entry.level() != null);
        if (!tobiasAvailable) {
            errors.put("availabilityTobias", "Ziehbare aktive Konzepte benÃ¶tigen eine Beschaffbarkeit fÃ¼r Tobias.");
        }
        if ("OPEN".equals(command.challengeSpecificity()) && directChildren == 0) {
            errors.put("challengeSpecificity", "Eine offene ziehbare Vorgabe benÃ¶tigt mindestens eine direkte Konkretisierung.");
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

    private void validateDrawability(UpdateIngredientConceptCommand command, CatalogConceptDetail current) {
        if (!command.active() || !command.randomDrawEnabled()) {
            return;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (current.functionalRoles().isEmpty()) {
            errors.put("functionalRoles", "Ziehbare aktive Konzepte benötigen mindestens eine funktionale Rolle.");
        }
        boolean georgiaAvailable = current.availability().stream()
                .anyMatch(entry -> entry.participant().code().equals("GEORGIA") && entry.level() != null);
        if (!georgiaAvailable) {
            errors.put("availabilityGeorgia", "Ziehbare aktive Konzepte benötigen eine Beschaffbarkeit für Georgia.");
        }
        boolean tobiasAvailable = current.availability().stream()
                .anyMatch(entry -> entry.participant().code().equals("TOBIAS") && entry.level() != null);
        if (!tobiasAvailable) {
            errors.put("availabilityTobias", "Ziehbare aktive Konzepte benötigen eine Beschaffbarkeit für Tobias.");
        }
        if ("OPEN".equals(command.challengeSpecificity()) && current.directChildren().isEmpty()) {
            errors.put("challengeSpecificity", "Eine offene ziehbare Vorgabe benötigt mindestens eine direkte Konkretisierung.");
        }
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
    }

    private void validateSpecificityGraph(UpdateIngredientConceptCommand command, CatalogConceptDetail current) {
        Map<String, String> errors = new LinkedHashMap<>();
        if ("OPEN".equals(command.challengeSpecificity()) && jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    where relation.child_concept_id = ? and parent.challenge_specificity = 'SPECIFIC'
                )
                """, Boolean.class, current.id())) {
            errors.put("challengeSpecificity", "Offene Konzepte dürfen keinen direkten spezifischen Oberbegriff haben.");
        }
        if ("SPECIFIC".equals(command.challengeSpecificity()) && jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from ingredient_refinement relation
                    join ingredient_concept child on child.id = relation.child_concept_id
                    where relation.parent_concept_id = ? and child.challenge_specificity = 'OPEN'
                )
                """, Boolean.class, current.id())) {
            errors.put("challengeSpecificity", "Spezifische Konzepte dürfen keine direkten offenen Konkretisierungen haben.");
        }
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
    }

    private List<String> drawWeightWarnings(UpdateIngredientConceptCommand command, CatalogConceptDetail current) {
        if (!command.active() || !command.randomDrawEnabled()) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        BigDecimal weight = command.baseDrawWeight();
        if (command.noveltyLevel() != null) {
            BigDecimal cap = switch (command.noveltyLevel()) {
                case 5 -> new BigDecimal("0.25");
                case 4 -> new BigDecimal("0.35");
                case 3 -> new BigDecimal("0.55");
                default -> null;
            };
            if (cap != null && weight.compareTo(cap) > 0) {
                warnings.add("Die Ungewöhnlichkeit Stufe %d hat in der Baseline einen Richtwert von höchstens %s."
                        .formatted(command.noveltyLevel(), cap));
            }
        }
        if (current.availability().stream().anyMatch(entry -> entry.level() == CatalogQueries.CatalogAvailability.DIFFICULT)
                && weight.compareTo(new BigDecimal("0.35")) > 0) {
            warnings.add("Schwierig beschaffbare Konzepte haben in der Baseline einen Richtwert von höchstens 0.35.");
        }
        if (jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    where relation.child_concept_id = ? and parent.code = 'COOKING_ALCOHOL'
                )
                """, Boolean.class, current.id()) && weight.compareTo(new BigDecimal("0.35")) > 0) {
            warnings.add("Direkte Konkretisierungen von Kochalkohol haben in der Baseline einen Richtwert von höchstens 0.35.");
        }
        return List.copyOf(warnings);
    }

    private boolean conceptExists(long conceptId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists (select 1 from ingredient_concept where id = ?)", Boolean.class, conceptId
        ));
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
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", detail.id());
        values.put("code", detail.code());
        values.put("displayName", detail.displayName());
        values.put("active", detail.active());
        values.put("randomDrawEnabled", detail.randomDrawEnabled());
        values.put("challengeSpecificity", detail.challengeSpecificity());
        values.put("baseDrawWeight", detail.baseDrawWeight());
        values.put("noveltyLevel", detail.noveltyLevel());
        values.put("curatorNote", detail.curatorNote());
        values.put("version", detail.version());
        values.put("directParents", detail.directParents().stream().map(CatalogCommandService::relationSnapshot).toList());
        values.put("directChildren", detail.directChildren().stream().map(CatalogCommandService::relationSnapshot).toList());
        values.put("functionalRoles", detail.functionalRoles().stream().map(value -> referenceSnapshot(
                value.code(), value.displayName(), value.description())).toList());
        values.put("culinaryFlags", detail.culinaryFlags().stream().map(value -> referenceSnapshot(
                value.code(), value.displayName(), value.description())).toList());
        values.put("culinaryDimensions", detail.culinaryDimensions().stream().map(value -> {
            Map<String, Object> dimension = referenceSnapshot(
                    value.dimension().code(), value.dimension().displayName(), value.dimension().description());
            dimension.put("level", value.level());
            return dimension;
        }).toList());
        values.put("availability", detail.availability().stream().map(value -> {
            Map<String, Object> availability = referenceSnapshot(
                    value.participant().code(), value.participant().displayName(), value.participant().description());
            availability.put("level", value.level() == null ? null : value.level().name());
            return availability;
        }).toList());
        values.put("seasonality", detail.seasonality().stream().map(value -> Map.<String, Object>of(
                "month", value.month(), "weightMultiplier", value.weightMultiplier())).toList());
        values.put("directExclusionRules", detail.directExclusionRules());
        return new CatalogAggregateSnapshot(values);
    }

    private static Map<String, Object> relationSnapshot(CatalogQueries.CatalogConceptRelation relation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", relation.id());
        values.put("code", relation.code());
        values.put("displayName", relation.displayName());
        values.put("active", relation.active());
        return values;
    }

    private static Map<String, Object> referenceSnapshot(String code, String displayName, String description) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("displayName", displayName);
        values.put("description", description);
        return values;
    }

    private record LockedConcept(long id, long version) {
    }

    private record GraphNode(
            long id,
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
        private final Map<Long, Set<String>> roles;
        private final Set<Edge> edges;

        private GraphState(Map<Long, GraphNode> nodes, Map<Long, Set<String>> roles, Set<Edge> edges) {
            this.nodes = new LinkedHashMap<>(nodes);
            this.roles = new HashMap<>();
            roles.forEach((id, values) -> this.roles.put(id, Set.copyOf(values)));
            this.edges = new LinkedHashSet<>(edges);
        }

        private Map<Long, GraphNode> nodes() {
            return nodes;
        }

        private Set<Edge> edges() {
            return edges;
        }

        private void replace(long conceptId, String specificity, boolean active, boolean randomDrawEnabled) {
            GraphNode current = requireNode(conceptId);
            nodes.put(conceptId, new GraphNode(
                    current.id(), current.displayName(), active, randomDrawEnabled, specificity));
        }

        private GraphNode requireNode(long id) {
            GraphNode node = nodes.get(id);
            if (node == null) {
                throw new CatalogConceptNotFoundException(id);
            }
            return node;
        }

        private Set<String> rolesFor(long conceptId) {
            return roles.getOrDefault(conceptId, Set.of());
        }

        private int directChildCount(long parentId) {
            return Math.toIntExact(edges.stream().filter(edge -> edge.parentId() == parentId).count());
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
