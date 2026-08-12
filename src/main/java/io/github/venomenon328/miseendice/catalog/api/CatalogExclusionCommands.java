package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Public commands for the curated exclusion-rule aggregate. */
public interface CatalogExclusionCommands {

    CatalogExclusionCommandResult createExclusionRule(CreateExclusionRuleCommand command);

    CatalogExclusionCommandResult updateExclusionRule(UpdateExclusionRuleCommand command);

    record CatalogExclusionCommandResult(long exclusionRuleId, long version) {
        public CatalogExclusionCommandResult {
            if (exclusionRuleId <= 0 || version < 0) {
                throw new IllegalArgumentException("An exclusion command result needs a positive id and version");
            }
        }
    }

    record ExclusionTarget(long ingredientConceptId, boolean includeRefinements) {
        public ExclusionTarget {
            if (ingredientConceptId <= 0) {
                throw new CatalogCommandValidationException(Map.of("targets", "Ein Ausschlussziel ist nicht gültig."));
            }
        }
    }

    record CreateExclusionRuleCommand(
            String code,
            String displayText,
            boolean active,
            BigDecimal baseDrawWeight,
            String curatorNote,
            List<ExclusionTarget> targets,
            String actorKey
    ) {
        public CreateExclusionRuleCommand(String code, String displayText, String actorKey) {
            this(code, displayText, true, new BigDecimal("1.0000"), null, List.of(), actorKey);
        }

        public CreateExclusionRuleCommand {
            code = required(code);
            displayText = required(displayText);
            curatorNote = nullable(curatorNote);
            actorKey = required(actorKey);
            targets = targets == null ? List.of() : List.copyOf(targets);
            validate(code, displayText, active, baseDrawWeight, targets, actorKey);
        }
    }

    record UpdateExclusionRuleCommand(
            long exclusionRuleId,
            long expectedVersion,
            String displayText,
            boolean active,
            BigDecimal baseDrawWeight,
            String curatorNote,
            List<ExclusionTarget> targets,
            String actorKey
    ) {
        public UpdateExclusionRuleCommand {
            displayText = required(displayText);
            curatorNote = nullable(curatorNote);
            actorKey = required(actorKey);
            targets = targets == null ? List.of() : List.copyOf(targets);
            Map<String, String> errors = new LinkedHashMap<>();
            if (exclusionRuleId <= 0) {
                errors.put("exclusionRuleId", "Die Ausschlussregel ist nicht gültig.");
            }
            if (expectedVersion < 0) {
                errors.put("version", "Die erwartete Version ist nicht gültig.");
            }
            validateInto(errors, null, displayText, active, baseDrawWeight, targets, actorKey);
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
        }
    }

    private static void validate(
            String code,
            String displayText,
            boolean active,
            BigDecimal baseDrawWeight,
            List<ExclusionTarget> targets,
            String actorKey
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateInto(errors, code, displayText, active, baseDrawWeight, targets, actorKey);
        if (!errors.isEmpty()) {
            throw new CatalogCommandValidationException(errors);
        }
    }

    private static void validateInto(
            Map<String, String> errors,
            String code,
            String displayText,
            boolean active,
            BigDecimal baseDrawWeight,
            List<ExclusionTarget> targets,
            String actorKey
    ) {
        if (code != null && !Pattern.matches(CatalogCommands.INGREDIENT_CONCEPT_CODE_PATTERN, code)) {
            errors.put("code", "Der Code muss dem Muster A-Z, Ziffern und Unterstriche folgen.");
        }
        if (displayText.isEmpty()) {
            errors.put("displayText", "Der Anzeigetext darf nicht leer sein.");
        }
        if (baseDrawWeight == null || baseDrawWeight.signum() <= 0) {
            errors.put("baseDrawWeight", "Das Ziehungsgewicht muss größer als 0 sein.");
        }
        if (active && targets.isEmpty()) {
            errors.put("targets", "Aktive Ausschlussregeln benötigen mindestens ein Ziel.");
        }
        long distinctTargets = targets.stream().map(ExclusionTarget::ingredientConceptId).distinct().count();
        if (distinctTargets != targets.size()) {
            errors.put("targets", "Dasselbe Zutatenkonzept darf nur einmal als Ausschlussziel vorkommen.");
        }
        if (actorKey.isEmpty()) {
            errors.put("actorKey", "Für die Auditierung ist ein Administrationsschlüssel erforderlich.");
        }
    }

    private static String required(String value) {
        return value == null ? "" : value.strip();
    }

    private static String nullable(String value) {
        String normalized = required(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
