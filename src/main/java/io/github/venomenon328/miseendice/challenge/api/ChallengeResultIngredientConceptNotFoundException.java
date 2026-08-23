package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultIngredientConceptNotFoundException extends RuntimeException {
    public ChallengeResultIngredientConceptNotFoundException(long ingredientConceptId) {
        super("Ingredient concept does not exist: " + ingredientConceptId);
    }
}
