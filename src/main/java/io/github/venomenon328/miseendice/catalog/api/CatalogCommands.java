package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Public application commands for the currently supported ingredient-concept writing use cases.
 *
 * <p>Base fields and pending direct-refinement changes deliberately share one update command. This
 * keeps an editor save atomic and makes the full resulting graph available for validation.</p>
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
            boolean weightWarningsAcknowledged,
            List<RefinementChange> refinementChanges,
            Map<Long, Long> expectedRelatedVersions,
            boolean inactiveRelationsAcknowledged
    ) {

        public UpdateIngredientConceptCommand(
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
            this(conceptId, expectedVersion, displayName, active, randomDrawEnabled, challengeSpecificity,
                    baseDrawWeight, noveltyLevel, curatorNote, actorKey, weightWarningsAcknowledged,
                    List.of(), Map.of(), false);
        }

        public UpdateIngredientConceptCommand {
            displayName = normalized(displayName);
            challengeSpecificity = normalized(challengeSpecificity);
            curatorNote = nullableNormalized(curatorNote);
            actorKey = normalized(actorKey);
            refinementChanges = refinementChanges == null ? List.of() : List.copyOf(refinementChanges);
            expectedRelatedVersions = expectedRelatedVersions == null ? Map.of() : Map.copyOf(expectedRelatedVersions);
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
            if (expectedRelatedVersions.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                    || entry.getKey() <= 0 || entry.getValue() == null || entry.getValue() < 0)) {
                errors.put("relations", "Die Versionsdaten einer Beziehung sind ungÃ¼ltig.");
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
        }
    }

    /** A single pending direct edge mutation. Parent-to-child means "child refines parent". */
    record RefinementChange(long parentConceptId, long childConceptId, RefinementChangeType type) {

        public RefinementChange {
            if (parentConceptId <= 0 || childConceptId <= 0 || type == null) {
                throw new IllegalArgumentException("A refinement change requires two concepts and a type");
            }
        }
    }

    enum RefinementChangeType {
        ADD,
        REMOVE
    }

    private static String normalized(String value) {
        return value == null ? "" : value.strip();
    }

    private static String nullableNormalized(String value) {
        String normalized = normalized(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
