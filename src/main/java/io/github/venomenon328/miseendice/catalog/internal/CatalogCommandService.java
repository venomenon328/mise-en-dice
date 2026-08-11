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
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogConceptDetail;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional application service for the first, deliberately small catalog writing surface. */
@Service
class CatalogCommandService implements CatalogCommands {

    private static final String ENTITY_TYPE = "INGREDIENT_CONCEPT";

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
}
