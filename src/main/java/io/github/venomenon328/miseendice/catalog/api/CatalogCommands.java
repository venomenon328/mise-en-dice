package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Public application commands for the currently supported ingredient-concept writing use cases.
 *
 * <p>Base fields, metadata and pending direct-refinement changes deliberately share one update
 * command. This keeps an editor save atomic and makes the full resulting aggregate and graph
 * available for validation.</p>
 */
public interface CatalogCommands {

    String INGREDIENT_CONCEPT_CODE_PATTERN = "[A-Z][A-Z0-9_]*";
    String CULINARY_COUNTRY_CODE_PATTERN = "[A-Z]{2}";

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

    record CreateIngredientConceptCommand(
            String code,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String challengeSpecificity,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            String curatorNote,
            CatalogMetadata metadata,
            boolean weightWarningsAcknowledged,
            String actorKey
    ) {

        public CreateIngredientConceptCommand(String code, String displayName, String curatorNote, String actorKey) {
            this(code, displayName, true, false, "SPECIFIC", new BigDecimal("1.0000"), null,
                    curatorNote, null, false, actorKey);
        }

        public CreateIngredientConceptCommand {
            code = normalized(code);
            displayName = normalized(displayName);
            challengeSpecificity = normalized(challengeSpecificity);
            curatorNote = normalized(curatorNote);
            actorKey = normalized(actorKey);
            Map<String, String> errors = new LinkedHashMap<>();
            if (!Pattern.matches(INGREDIENT_CONCEPT_CODE_PATTERN, code)) {
                errors.put("code", "Der Code muss dem Muster A-Z, Ziffern und Unterstriche folgen.");
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
            if (curatorNote.isEmpty()) {
                errors.put("curatorNote", "Die Kuratornotiz darf nicht leer sein.");
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
            boolean inactiveRelationsAcknowledged,
            CatalogMetadata metadata
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
                    List.of(), Map.of(), false, null);
        }

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
                boolean weightWarningsAcknowledged,
                List<RefinementChange> refinementChanges,
                Map<Long, Long> expectedRelatedVersions,
                boolean inactiveRelationsAcknowledged
        ) {
            this(conceptId, expectedVersion, displayName, active, randomDrawEnabled, challengeSpecificity,
                    baseDrawWeight, noveltyLevel, curatorNote, actorKey, weightWarningsAcknowledged,
                    refinementChanges, expectedRelatedVersions, inactiveRelationsAcknowledged, null);
        }

        public UpdateIngredientConceptCommand {
            displayName = normalized(displayName);
            challengeSpecificity = normalized(challengeSpecificity);
            curatorNote = normalized(curatorNote);
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
            if (curatorNote.isEmpty()) {
                errors.put("curatorNote", "Die Kuratornotiz darf nicht leer sein.");
            }
            if (actorKey.isEmpty()) {
                errors.put("actorKey", "Für die Auditierung ist ein Administrationsschlüssel erforderlich.");
            }
            if (expectedRelatedVersions.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                    || entry.getKey() <= 0 || entry.getValue() == null || entry.getValue() < 0)) {
                errors.put("relations", "Die Versionsdaten einer Beziehung sind ungültig.");
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

    /**
     * The editable metadata state of one ingredient aggregate.
     *
     * <p>The five-argument compatibility constructor deliberately leaves culinary countries
     * unspecified. Existing callers therefore preserve country relations until they explicitly
     * adopt the country-aware editor contract. A non-null country set, including an empty set,
     * is an exact replacement.</p>
     */
    record CatalogMetadata(
            Set<String> functionalRoleCodes,
            Set<String> culinaryFlagCodes,
            Map<String, Integer> culinaryDimensionLevels,
            Map<String, CatalogQueries.CatalogAvailability> availabilityByParticipant,
            Map<Integer, BigDecimal> seasonalityByMonth,
            Set<String> culinaryCountryCodes
    ) {

        public CatalogMetadata(
                Set<String> functionalRoleCodes,
                Set<String> culinaryFlagCodes,
                Map<String, Integer> culinaryDimensionLevels,
                Map<String, CatalogQueries.CatalogAvailability> availabilityByParticipant,
                Map<Integer, BigDecimal> seasonalityByMonth
        ) {
            this(functionalRoleCodes, culinaryFlagCodes, culinaryDimensionLevels,
                    availabilityByParticipant, seasonalityByMonth, null);
        }

        public CatalogMetadata {
            functionalRoleCodes = normalizedCodes(functionalRoleCodes);
            culinaryFlagCodes = normalizedCodes(culinaryFlagCodes);
            culinaryDimensionLevels = immutableMap(culinaryDimensionLevels);
            availabilityByParticipant = immutableMap(availabilityByParticipant);
            seasonalityByMonth = immutableMap(seasonalityByMonth);
            culinaryCountryCodes = culinaryCountryCodes == null
                    ? null
                    : normalizedCountryCodes(culinaryCountryCodes);
            Map<String, String> errors = new LinkedHashMap<>();
            if (culinaryDimensionLevels.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                    || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue() < 1 || entry.getValue() > 5)) {
                errors.put("culinaryDimensions", "Kulinarische Dimensionen müssen zwischen 1 und 5 liegen.");
            }
            if (availabilityByParticipant.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                    || entry.getKey().isBlank() || entry.getValue() == null)) {
                errors.put("availability", "Beschaffbarkeiten müssen gültige Teilnehmer und Stufen enthalten.");
            }
            if (seasonalityByMonth.entrySet().stream().anyMatch(entry -> entry.getKey() == null
                    || entry.getKey() < 1 || entry.getKey() > 12 || entry.getValue() == null
                    || entry.getValue().signum() <= 0)) {
                errors.put("seasonality", "Saisonfaktoren müssen für Monate 1 bis 12 größer als 0 sein.");
            }
            if (culinaryCountryCodes != null
                    && culinaryCountryCodes.stream().anyMatch(code -> !Pattern.matches(CULINARY_COUNTRY_CODE_PATTERN, code))) {
                errors.put("culinaryCountries", "Ländercodes müssen gültige ISO-Alpha-2-Codes sein.");
            }
            if (!errors.isEmpty()) {
                throw new CatalogCommandValidationException(errors);
            }
        }

        private static Set<String> normalizedCodes(Set<String> codes) {
            if (codes == null || codes.isEmpty()) {
                return Set.of();
            }
            Set<String> normalized = new LinkedHashSet<>();
            codes.forEach(code -> normalized.add(normalized(code)));
            if (normalized.contains("")) {
                throw new CatalogCommandValidationException(Map.of("metadata", "Referenzcodes dürfen nicht leer sein."));
            }
            return Set.copyOf(normalized);
        }

        private static Set<String> normalizedCountryCodes(Set<String> codes) {
            if (codes.isEmpty()) {
                return Set.of();
            }
            Set<String> normalized = new LinkedHashSet<>();
            codes.forEach(code -> normalized.add(normalized(code).toUpperCase(Locale.ROOT)));
            return Set.copyOf(normalized);
        }

        private static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
            return values == null || values.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.strip();
    }
}
