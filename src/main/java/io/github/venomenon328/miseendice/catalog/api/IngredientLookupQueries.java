package io.github.venomenon328.miseendice.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Public, read-only projection for the private Discord ingredient lookup.
 *
 * <p>This deliberately exposes neither catalog administration details nor write operations.
 * All returned relationships are direct, current catalog values.</p>
 */
public interface IngredientLookupQueries {

    IngredientLookupSearchResult searchActiveByDisplayName(String searchText, int limit);

    Optional<IngredientLookupProfile> findActiveProfile(long conceptId);

    /**
     * Searches the migration-managed culinary-country reference data for Discord autocomplete.
     * The returned values are presentation-independent catalog facts, not a Discord-owned list.
     */
    List<CulinaryCountry> searchCulinaryCountries(String searchText, int limit);

    /** Resolves either an ISO alpha-2 value or an exact German display name from the reference data. */
    Optional<CulinaryCountry> resolveCulinaryCountry(String input);

    /**
     * Returns active concepts with an explicit association to the given culinary country.
     * No refinement-graph inference is applied.
     */
    Optional<CulinaryCountryIngredientPage> findActiveByCulinaryCountry(String countryCode, int page, int pageSize);

    record IngredientLookupSearchResult(String searchText, List<IngredientLookupMatch> matches, long totalMatches) {

        public IngredientLookupSearchResult {
            searchText = normalize(searchText);
            matches = List.copyOf(matches);
            if (totalMatches < matches.size()) {
                throw new IllegalArgumentException("totalMatches must include every returned match");
            }
        }

        public boolean hasMoreMatches() {
            return totalMatches > matches.size();
        }
    }

    record IngredientLookupMatch(long conceptId, String displayName, List<String> activeDirectParents) {

        public IngredientLookupMatch {
            positiveId(conceptId);
            displayName = required(displayName, "displayName");
            activeDirectParents = List.copyOf(activeDirectParents);
        }
    }

    record IngredientLookupRelation(long conceptId, String displayName) {

        public IngredientLookupRelation {
            positiveId(conceptId);
            displayName = required(displayName, "displayName");
        }
    }

    record IngredientLookupProfile(
            long conceptId,
            String displayName,
            boolean randomDrawEnabled,
            BigDecimal baseDrawWeight,
            Integer noveltyLevel,
            List<IngredientLookupRelation> activeDirectParents,
            List<IngredientLookupRelation> activeDirectChildren,
            List<String> functionalRoles,
            List<String> culinaryFlags,
            List<IngredientLookupDimension> culinaryDimensions,
            List<IngredientLookupCountry> culinaryCountries,
            String curatorNote
    ) {

        public IngredientLookupProfile(
                long conceptId,
                String displayName,
                boolean randomDrawEnabled,
                BigDecimal baseDrawWeight,
                Integer noveltyLevel,
                List<IngredientLookupRelation> activeDirectParents,
                List<IngredientLookupRelation> activeDirectChildren,
                List<String> functionalRoles,
                List<String> culinaryFlags,
                List<IngredientLookupDimension> culinaryDimensions,
                String curatorNote
        ) {
            this(conceptId, displayName, randomDrawEnabled, baseDrawWeight, noveltyLevel,
                    activeDirectParents, activeDirectChildren, functionalRoles, culinaryFlags,
                    culinaryDimensions, List.of(), curatorNote);
        }

        public IngredientLookupProfile {
            positiveId(conceptId);
            displayName = required(displayName, "displayName");
            if (baseDrawWeight == null) {
                throw new IllegalArgumentException("baseDrawWeight is required");
            }
            if (noveltyLevel != null && (noveltyLevel < 1 || noveltyLevel > 5)) {
                throw new IllegalArgumentException("noveltyLevel must be between 1 and 5");
            }
            activeDirectParents = List.copyOf(activeDirectParents);
            activeDirectChildren = List.copyOf(activeDirectChildren);
            functionalRoles = List.copyOf(functionalRoles);
            culinaryFlags = List.copyOf(culinaryFlags);
            culinaryDimensions = List.copyOf(culinaryDimensions);
            culinaryCountries = List.copyOf(culinaryCountries);
        }
    }

    record CulinaryCountry(String code, String displayName) {

        public CulinaryCountry {
            if (code == null || !code.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("country code must be an ISO alpha-2 code");
            }
            displayName = required(displayName, "displayName");
        }
    }

    record CulinaryCountryIngredient(long conceptId, String displayName) {

        public CulinaryCountryIngredient {
            positiveId(conceptId);
            displayName = required(displayName, "displayName");
        }
    }

    record CulinaryCountryIngredientPage(
            CulinaryCountry country,
            int page,
            int pageSize,
            long totalIngredients,
            List<CulinaryCountryIngredient> ingredients
    ) {

        public CulinaryCountryIngredientPage {
            if (country == null) {
                throw new IllegalArgumentException("country is required");
            }
            if (page < 1 || pageSize < 1 || pageSize > 25 || totalIngredients < ingredients.size()) {
                throw new IllegalArgumentException("Invalid culinary-country ingredient page");
            }
            ingredients = List.copyOf(ingredients);
        }

        public int totalPages() {
            return Math.max(1, (int) Math.ceil((double) totalIngredients / pageSize));
        }

        public boolean hasPreviousPage() {
            return page > 1;
        }

        public boolean hasNextPage() {
            return page < totalPages();
        }
    }

    record IngredientLookupDimension(String code, String displayName, int level) {

        public IngredientLookupDimension {
            code = required(code, "code");
            displayName = required(displayName, "displayName");
            if (level < 1 || level > 5) {
                throw new IllegalArgumentException("level must be between 1 and 5");
            }
        }
    }

    /** Explicit culinary-country association of the displayed ingredient concept. */
    record IngredientLookupCountry(String code, String displayName) {

        public IngredientLookupCountry {
            if (code == null || !code.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("country code must be an ISO alpha-2 code");
            }
            displayName = required(displayName, "displayName");
        }
    }

    static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static void positiveId(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("conceptId must be positive");
        }
    }
}
