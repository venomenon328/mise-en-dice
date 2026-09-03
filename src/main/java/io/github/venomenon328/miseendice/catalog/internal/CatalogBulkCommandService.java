package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkAction;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkOperation;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkSelection;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.CatalogBulkPreview;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.CatalogBulkPreviewItem;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.CatalogBulkResult;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogConceptNotFoundException;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bounded explicit ingredient bulk operations against one resulting state. */
@Service
class CatalogBulkCommandService implements CatalogBulkCommands {

    private static final String ENTITY_TYPE = "INGREDIENT_CONCEPT";

    private final JdbcTemplate jdbcTemplate;
    private final CatalogQueries catalogQueries;
    private final CatalogAuditLog auditLog;

    CatalogBulkCommandService(
            JdbcTemplate jdbcTemplate,
            CatalogQueries catalogQueries,
            CatalogAuditLog auditLog
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogQueries = catalogQueries;
        this.auditLog = auditLog;
    }

    @Override
    @Transactional
    public CatalogBulkPreview preview(BulkOperation operation) {
        validateActionReference(operation);
        Map<Long, CatalogConceptDetail> current = loadAndCheckVersions(operation, false);
        Map<Long, BulkState> resulting = apply(operation, current);
        validateResultingDrawability(resulting);
        return preview(operation, current, resulting);
    }

    @Override
    @Transactional
    public CatalogBulkResult execute(BulkOperation operation) {
        validateActionReference(operation);
        Map<Long, CatalogConceptDetail> before = loadAndCheckVersions(operation, true);
        Map<Long, BulkState> resulting = apply(operation, before);
        validateResultingDrawability(resulting);
        CatalogBulkPreview preview = preview(operation, before, resulting);
        if (!preview.warnings().isEmpty() && !operation.weightWarningsAcknowledged()) {
            throw new CatalogDrawWeightWarningException(preview.warnings());
        }

        List<Long> changedIds = resulting.entrySet().stream()
                .filter(entry -> entry.getValue().changed())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (changedIds.isEmpty()) {
            return new CatalogBulkResult(List.of(), UUID.randomUUID());
        }

        for (long conceptId : changedIds) {
            persistChange(operation, conceptId, before.get(conceptId), resulting.get(conceptId));
        }
        UUID changeGroupId = UUID.randomUUID();
        for (long conceptId : changedIds) {
            CatalogConceptDetail after = findRequired(conceptId);
            auditLog.append(new CatalogAuditEntryDraft(changeGroupId, operation.actorKey(), ENTITY_TYPE, conceptId,
                    "BULK_" + operation.action().name(), CatalogIngredientSnapshotFactory.snapshot(before.get(conceptId)),
                    CatalogIngredientSnapshotFactory.snapshot(after)));
        }
        return new CatalogBulkResult(changedIds, changeGroupId);
    }

    private void validateActionReference(BulkOperation operation) {
        if (operation.action() == BulkAction.ADD_FUNCTIONAL_ROLE || operation.action() == BulkAction.REMOVE_FUNCTIONAL_ROLE) {
            int known = jdbcTemplate.queryForObject("select count(*) from functional_role where code = ?", Integer.class,
                    operation.functionalRoleCode());
            if (known != 1) {
                throw new CatalogCommandValidationException(Map.of("functionalRole", "Die funktionale Rolle ist nicht bekannt."));
            }
        }
    }

    /** Locks every selected row in deterministic id order before comparing every expected version. */
    private Map<Long, CatalogConceptDetail> loadAndCheckVersions(BulkOperation operation, boolean lock) {
        List<BulkSelection> ordered = operation.selections().stream()
                .sorted(Comparator.comparingLong(BulkSelection::conceptId))
                .toList();
        Map<Long, Long> expected = new LinkedHashMap<>();
        ordered.forEach(selection -> expected.put(selection.conceptId(), selection.expectedVersion()));
        if (lock) {
            String placeholders = String.join(", ", java.util.Collections.nCopies(ordered.size(), "?"));
            Map<Long, Long> locked = new LinkedHashMap<>();
            jdbcTemplate.query("select id, version from ingredient_concept where id in (" + placeholders + ") order by id for update",
                    (RowCallbackHandler) resultSet -> locked.put(resultSet.getLong("id"), resultSet.getLong("version")),
                    ordered.stream().map(BulkSelection::conceptId).toArray());
            for (BulkSelection selection : ordered) {
                Long current = locked.get(selection.conceptId());
                if (current == null) {
                    throw new CatalogConceptNotFoundException(selection.conceptId());
                }
                if (current.longValue() != selection.expectedVersion()) {
                    throw new CatalogVersionConflictException(selection.conceptId(), selection.expectedVersion());
                }
            }
        }
        Map<Long, CatalogConceptDetail> details = new LinkedHashMap<>();
        for (BulkSelection selection : ordered) {
            CatalogConceptDetail detail = findRequired(selection.conceptId());
            if (detail.version() != selection.expectedVersion()) {
                throw new CatalogVersionConflictException(selection.conceptId(), selection.expectedVersion());
            }
            details.put(selection.conceptId(), detail);
        }
        return Map.copyOf(details);
    }

    private Map<Long, BulkState> apply(BulkOperation operation, Map<Long, CatalogConceptDetail> current) {
        Map<Long, BulkState> states = new LinkedHashMap<>();
        current.forEach((id, detail) -> states.put(id, BulkState.from(detail)));
        states.values().forEach(state -> state.apply(operation));
        return Map.copyOf(states);
    }

    private CatalogBulkPreview preview(
            BulkOperation operation,
            Map<Long, CatalogConceptDetail> before,
            Map<Long, BulkState> resulting
    ) {
        Map<Long, Boolean> directCookingAlcoholParents = directCookingAlcoholParents(resulting.keySet());
        List<CatalogBulkPreviewItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (long conceptId : resulting.keySet().stream().sorted().toList()) {
            CatalogConceptDetail detail = before.get(conceptId);
            BulkState state = resulting.get(conceptId);
            items.add(new CatalogBulkPreviewItem(conceptId, detail.displayName(), state.changed(), state.effects(operation)));
            warnings.addAll(drawWeightWarnings(detail, state, directCookingAlcoholParents.getOrDefault(conceptId, false)));
        }
        return new CatalogBulkPreview(operation.withoutAcknowledgement(), items, warnings.stream().distinct().toList());
    }

    private void validateResultingDrawability(Map<Long, BulkState> states) {
        Map<String, String> errors = new LinkedHashMap<>();
        states.forEach((id, state) -> {
            if (!state.active || !state.randomDrawEnabled) {
                return;
            }
            if (state.functionalRoleCodes.isEmpty()) {
                errors.put("selection", "\u201e%s\u201c w\u00e4re aktiv und ziehbar, aber ohne funktionale Rolle.".formatted(state.displayName));
            }
        });
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
    }

    private void persistChange(BulkOperation operation, long conceptId, CatalogConceptDetail before, BulkState state) {
        long expectedVersion = before.version();
        switch (operation.action()) {
            case ACTIVATE, DEACTIVATE -> updateConceptColumn("active", state.active, conceptId, expectedVersion);
            case ENABLE_RANDOM_DRAW, DISABLE_RANDOM_DRAW ->
                    updateConceptColumn("random_draw_enabled", state.randomDrawEnabled, conceptId, expectedVersion);
            case ADD_FUNCTIONAL_ROLE, REMOVE_FUNCTIONAL_ROLE -> {
                if (operation.action() == BulkAction.ADD_FUNCTIONAL_ROLE) {
                    jdbcTemplate.update("""
                            insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                            select ?, id from functional_role where code = ? on conflict do nothing
                            """, conceptId, operation.functionalRoleCode());
                } else {
                    jdbcTemplate.update("""
                            delete from ingredient_functional_role where ingredient_concept_id = ?
                            and functional_role_id = (select id from functional_role where code = ?)
                            """, conceptId, operation.functionalRoleCode());
                }
                advanceVersion(conceptId, expectedVersion);
            }
            case SET_GEORGIA_AVAILABILITY, SET_TOBIAS_AVAILABILITY -> {
                String participant = operation.action() == BulkAction.SET_GEORGIA_AVAILABILITY ? "GEORGIA" : "TOBIAS";
                jdbcTemplate.update("""
                        insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                        select ?, id, ? from participant where code = ?
                        on conflict (ingredient_concept_id, participant_id)
                        do update set availability_level = excluded.availability_level
                        """, conceptId, operation.availability().name(), participant);
                advanceVersion(conceptId, expectedVersion);
            }
        }
    }

    private void updateConceptColumn(String column, boolean value, long conceptId, long expectedVersion) {
        if (jdbcTemplate.update("update ingredient_concept set " + column + " = ?, version = version + 1 where id = ? and version = ?",
                value, conceptId, expectedVersion) != 1) {
            throw new CatalogVersionConflictException(conceptId, expectedVersion);
        }
    }

    private void advanceVersion(long conceptId, long expectedVersion) {
        if (jdbcTemplate.update("update ingredient_concept set version = version + 1 where id = ? and version = ?",
                conceptId, expectedVersion) != 1) {
            throw new CatalogVersionConflictException(conceptId, expectedVersion);
        }
    }

    private Map<Long, Boolean> directCookingAlcoholParents(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Long> ordered = ids.stream().sorted().toList();
        String placeholders = String.join(", ", java.util.Collections.nCopies(ordered.size(), "?"));
        Map<Long, Boolean> result = new HashMap<>();
        jdbcTemplate.query("""
                select edge.child_concept_id
                from ingredient_refinement edge
                join ingredient_concept parent on parent.id = edge.parent_concept_id
                where edge.child_concept_id in (""" + placeholders + ") and parent.code = 'COOKING_ALCOHOL'",
                (RowCallbackHandler) resultSet -> result.put(resultSet.getLong("child_concept_id"), true), ordered.toArray());
        return Map.copyOf(result);
    }

    private List<String> drawWeightWarnings(CatalogConceptDetail detail, BulkState state, boolean directCookingAlcoholParent) {
        if (!state.active || !state.randomDrawEnabled) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        if (detail.noveltyLevel() != null) {
            BigDecimal cap = switch (detail.noveltyLevel()) {
                case 5 -> new BigDecimal("0.25");
                case 4 -> new BigDecimal("0.35");
                case 3 -> new BigDecimal("0.55");
                default -> null;
            };
            if (cap != null && detail.baseDrawWeight().compareTo(cap) > 0) {
                warnings.add("\u201e%s\u201c: Kochungew\u00f6hnlichkeit Stufe %d hat einen Richtwert von h\u00f6chstens %s."
                        .formatted(detail.displayName(), detail.noveltyLevel(), cap));
            }
        }
        if (state.availabilityByParticipant.containsValue(CatalogAvailability.DIFFICULT)
                && detail.baseDrawWeight().compareTo(new BigDecimal("0.35")) > 0) {
            warnings.add("\u201e%s\u201c: schwierig beschaffbare Konzepte haben einen Richtwert von h\u00f6chstens 0.35."
                    .formatted(detail.displayName()));
        }
        if (directCookingAlcoholParent && detail.baseDrawWeight().compareTo(new BigDecimal("0.35")) > 0) {
            warnings.add("\u201e%s\u201c: direkte Konkretisierungen von Kochalkohol haben einen Richtwert von h\u00f6chstens 0.35."
                    .formatted(detail.displayName()));
        }
        return List.copyOf(warnings);
    }

    private CatalogConceptDetail findRequired(long id) {
        return catalogQueries.findConcept(id).orElseThrow(() -> new CatalogConceptNotFoundException(id));
    }

    private static final class BulkState {
        private final String displayName;
        private final boolean originalActive;
        private final boolean originalRandomDrawEnabled;
        private final Set<String> originalRoles;
        private final Map<String, CatalogAvailability> originalAvailability;
        private boolean active;
        private boolean randomDrawEnabled;
        private Set<String> functionalRoleCodes;
        private Map<String, CatalogAvailability> availabilityByParticipant;

        private BulkState(
                String displayName,
                boolean active,
                boolean randomDrawEnabled,
                Set<String> functionalRoleCodes,
                Map<String, CatalogAvailability> availabilityByParticipant
        ) {
            this.displayName = displayName;
            this.originalActive = active;
            this.originalRandomDrawEnabled = randomDrawEnabled;
            this.originalRoles = Set.copyOf(functionalRoleCodes);
            this.originalAvailability = Map.copyOf(availabilityByParticipant);
            this.active = active;
            this.randomDrawEnabled = randomDrawEnabled;
            this.functionalRoleCodes = new LinkedHashSet<>(functionalRoleCodes);
            this.availabilityByParticipant = new LinkedHashMap<>(availabilityByParticipant);
        }

        static BulkState from(CatalogConceptDetail detail) {
            Set<String> roles = detail.functionalRoles().stream().map(CatalogQueries.CatalogReferenceValue::code)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            Map<String, CatalogAvailability> availability = new LinkedHashMap<>();
            detail.availability().forEach(value -> {
                if (value.level() != null) {
                    availability.put(value.participant().code(), value.level());
                }
            });
            return new BulkState(detail.displayName(), detail.active(), detail.randomDrawEnabled(), roles, availability);
        }

        void apply(BulkOperation operation) {
            switch (operation.action()) {
                case ACTIVATE -> active = true;
                case DEACTIVATE -> active = false;
                case ENABLE_RANDOM_DRAW -> randomDrawEnabled = true;
                case DISABLE_RANDOM_DRAW -> randomDrawEnabled = false;
                case ADD_FUNCTIONAL_ROLE -> functionalRoleCodes.add(operation.functionalRoleCode());
                case REMOVE_FUNCTIONAL_ROLE -> functionalRoleCodes.remove(operation.functionalRoleCode());
                case SET_GEORGIA_AVAILABILITY -> availabilityByParticipant.put("GEORGIA", operation.availability());
                case SET_TOBIAS_AVAILABILITY -> availabilityByParticipant.put("TOBIAS", operation.availability());
            }
        }

        boolean changed() {
            return active != originalActive || randomDrawEnabled != originalRandomDrawEnabled
                    || !functionalRoleCodes.equals(originalRoles) || !availabilityByParticipant.equals(originalAvailability);
        }

        List<String> effects(BulkOperation operation) {
            if (!changed()) {
                return List.of("bereits im Zielzustand");
            }
            return switch (operation.action()) {
                case ACTIVATE -> List.of("wird aktiviert");
                case DEACTIVATE -> List.of("wird deaktiviert");
                case ENABLE_RANDOM_DRAW -> List.of("wird ziehbar");
                case DISABLE_RANDOM_DRAW -> List.of("wird nicht ziehbar");
                case ADD_FUNCTIONAL_ROLE -> List.of("Rolle wird hinzugef\u00fcgt: " + operation.functionalRoleCode());
                case REMOVE_FUNCTIONAL_ROLE -> List.of("Rolle wird entfernt: " + operation.functionalRoleCode());
                case SET_GEORGIA_AVAILABILITY -> List.of("Georgia: " + operation.availability().displayName());
                case SET_TOBIAS_AVAILABILITY -> List.of("Tobias: " + operation.availability().displayName());
            };
        }
    }
}
