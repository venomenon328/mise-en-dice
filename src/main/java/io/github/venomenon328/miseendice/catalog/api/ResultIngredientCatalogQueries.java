package io.github.venomenon328.miseendice.catalog.api;

import java.util.List;
import java.util.Optional;

/** Small literal lookup contract used when a Challenge-result free-text ingredient is optionally linked to the catalog. */
public interface ResultIngredientCatalogQueries {

    Optional<IngredientConcept> findUniqueExactMatch(String displayTextOrCode);

    List<IngredientConcept> searchLiterally(String searchTerm);

    Optional<IngredientConcept> findIngredientConcept(long ingredientConceptId);

    /** Exact literal match restricted to known direct or transitive refinements of one OPEN requirement. */
    Optional<IngredientConcept> findUniqueExactRefinementMatch(long openRequirementConceptId, String displayTextOrCode);

    /** Literal search restricted to known direct or transitive refinements, including inactive historical concepts. */
    List<IngredientConcept> searchRefinementsLiterally(long openRequirementConceptId, String searchTerm);

    boolean isKnownRefinement(long openRequirementConceptId, long ingredientConceptId);

    record IngredientConcept(long id, String code, String displayName, boolean active) {
        public IngredientConcept {
            if (id <= 0 || code == null || code.isBlank() || displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Ingredient concept projection is incomplete");
            }
        }
    }
}
