package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Public application commands for the currently supported ingredient-concept writing use cases.
 *
 * <p>The commands deliberately cover only the Phase-5 base fields. Relationships, assignments and
 * other catalog aggregates receive their own commands in later packages.</p>
 */
public interface CatalogCommands {

    String INGREDIENT_CONCEPT_CODE_PATTERN = "[A-Z][A-Z0-9_]*";

    CatalogCommandResult createIngredientConcept(CreateIngredientConceptCommand command);

    CatalogCommandResult updateIngredientConcept(UpdateIngredientConceptCommand command);

    record CatalogCommandResult(long conceptId, long version) {

        public CatalogCommandResult {
            if (conceptId <= 0) {
                throw new IllegalArgumentException("conceptId must be positive");
            }
            if (version < 0) {
                throw new IllegalArgumentException("version must not be negative");
            }
        }
    }

    record CreateIngredientConceptCommand(String code, String displayName, String actorKey) {

        public CreateIngredientConceptCommand {
            code = normalized(code);
            displayName = normalized(displayName);
            actorKey = normalized(actorKey);
            Map<String, String> errors = new LinkedHashMap<>();
            if (!Pattern.matches(INGREDIENT_CONCEPT_CODE_PATTERN, code)) {
                errors.put("code", "Der Code muss dem Muster A-Z, Ziffern und Unterstriche folgen.");
            }
            if (displayName.isEmpty()) {
                errors.put("displayName", "Der Anzeigename darf nicht leer sein.");
            }
            if (actorKey.isEmpty()) {
                errors.put("actorKey", "Für die Auditierung ist ein Administrationsschlüssel erforderlich.");
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
        }
    }

    record UpdateIngredientConceptCommand(
            long conceptId,
            long expectedVersion,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            String curatorNote,
            String actorKey,
            boolean weightWarningsAcknowledged
    ) {

        public UpdateIngredientConceptCommand {
            displayName = normalized(displayName);
            challengeSpecificity = normalized(challengeSpecificity);
            curatorNote = nullableNormalized(curatorNote);
            actorKey = normalized(actorKey);
            Map<String, String> errors = new LinkedHashMap<>();
            if (conceptId <= 0) {
                errors.put("conceptId", "Das Zutatenkonzept ist nicht gültig.");
            }
            if (expectedVersion < 0) {
                errors.put("version", "Die erwartete Version ist nicht gültig.");
            }
            if (displayName.isEmpty()) {
                errors.put("displayName", "Der Anzeigename darf nicht leer sein.");
            }
            if (!"SPECIFIC".equals(challengeSpecificity) && !"OPEN".equals(challengeSpecificity)) {
                errors.put("challengeSpecificity", "Wähle spezifisch oder offen.");
            }
            if (baseDrawWeight == null || baseDrawWeight.signum() <= 0) {
                errors.put("baseDrawWeight", "Das Ziehungsgewicht muss größer als 0 sein.");
            }
            if (noveltyLevel != null && (noveltyLevel < 1 || noveltyLevel > 5)) {
                errors.put("noveltyLevel", "Die Ungewöhnlichkeit muss zwischen 1 und 5 liegen oder leer bleiben.");
            }
            if (actorKey.isEmpty()) {
                errors.put("actorKey", "Für die Auditierung ist ein Administrationsschlüssel erforderlich.");
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.strip();
    }

    private static String nullableNormalized(String value) {
        String normalized = normalized(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
