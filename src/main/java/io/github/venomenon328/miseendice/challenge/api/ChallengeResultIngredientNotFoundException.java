package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultIngredientNotFoundException extends RuntimeException {
    public ChallengeResultIngredientNotFoundException(long resultIngredientId) {
        super("Challenge result ingredient does not exist: " + resultIngredientId);
    }
}
