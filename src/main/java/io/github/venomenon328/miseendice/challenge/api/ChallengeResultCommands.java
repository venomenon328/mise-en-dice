package io.github.venomenon328.miseendice.challenge.api;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Transport-neutral mutations for one durable result per Challenge and participant. */
public interface ChallengeResultCommands {

    ChallengeResultQueries.ChallengeResultView createChallengeResult(CreateChallengeResult command);

    ChallengeResultQueries.ChallengeResultView replaceChallengeResult(ReplaceChallengeResult command);

    /** Replaces the textual fields and own ingredients while deliberately retaining any current photo. */
    ChallengeResultQueries.ChallengeResultView updateChallengeResult(UpdateChallengeResult command);

    void removeChallengeResult(RemoveChallengeResult command);

    ChallengeResultQueries.ChallengeResultPhotoMetadata setChallengeResultPhoto(SetChallengeResultPhoto command);

    void removeChallengeResultPhoto(RemoveChallengeResultPhoto command);

    ChallengeResultQueries.ResultIngredientView setResultIngredientReference(SetResultIngredientReference command);

    record CreateChallengeResult(
            long challengeNumber,
            long participantId,
            ResultData result,
            ChallengeResultPhotoUpload photo
    ) {
        public CreateChallengeResult {
            positiveChallengeNumber(challengeNumber);
            positiveParticipantId(participantId);
            if (result == null) {
                throw new IllegalArgumentException("Challenge result data is required");
            }
        }
    }

    record ReplaceChallengeResult(
            long challengeNumber,
            long participantId,
            long expectedVersion,
            ResultData result,
            PhotoChange photoChange
    ) {
        public ReplaceChallengeResult {
            positiveChallengeNumber(challengeNumber);
            positiveParticipantId(participantId);
            nonNegativeVersion(expectedVersion, "Expected result version");
            if (result == null) {
                throw new IllegalArgumentException("Challenge result data is required");
            }
        }
    }

    record UpdateChallengeResult(long challengeNumber, long participantId, long expectedVersion, ResultData result) {
        public UpdateChallengeResult {
            positiveChallengeNumber(challengeNumber);
            positiveParticipantId(participantId);
            nonNegativeVersion(expectedVersion, "Expected result version");
            if (result == null) {
                throw new IllegalArgumentException("Challenge result data is required");
            }
        }
    }

    record RemoveChallengeResult(long challengeNumber, long participantId) {
        public RemoveChallengeResult {
            positiveChallengeNumber(challengeNumber);
            positiveParticipantId(participantId);
        }
    }

    record SetChallengeResultPhoto(
            long challengeNumber,
            long participantId,
            ChallengeResultPhotoUpload photo,
            boolean replaceExisting,
            Long expectedPhotoVersion
    ) {
        public SetChallengeResultPhoto {
            positiveChallengeNumber(challengeNumber);
            positiveParticipantId(participantId);
            if (photo == null) {
                throw new IllegalArgumentException("Challenge result photo is required");
            }
            nullableNonNegativeVersion(expectedPhotoVersion, "Expected photo version");
        }
    }

    record RemoveChallengeResultPhoto(long challengeNumber, long participantId, long expectedPhotoVersion) {
        public RemoveChallengeResultPhoto {
            positiveChallengeNumber(challengeNumber);
            positiveParticipantId(participantId);
            nonNegativeVersion(expectedPhotoVersion, "Expected photo version");
        }
    }

    /** Changes only the optional catalog reference; the stored free text is never rewritten. */
    record SetResultIngredientReference(long resultIngredientId, Long ingredientConceptId, long expectedResultVersion) {
        public SetResultIngredientReference {
            if (resultIngredientId <= 0) {
                throw new IllegalArgumentException("Result ingredient ID must be positive");
            }
            if (ingredientConceptId != null && ingredientConceptId <= 0) {
                throw new IllegalArgumentException("Ingredient concept ID must be positive when present");
            }
            nonNegativeVersion(expectedResultVersion, "Expected result version");
        }
    }

    record ResultData(String dishName, String description, String evaluation, List<OwnIngredientInput> ownIngredients) {
        public ResultData {
            dishName = requiredText(dishName, "Dish name", 200);
            description = requiredText(description, "Description", 4000);
            evaluation = optionalText(evaluation, "Evaluation", 4000);
            ownIngredients = ownIngredients == null ? List.of() : List.copyOf(ownIngredients);
            if (ownIngredients.size() > 25) {
                throw new IllegalArgumentException("A challenge result may contain at most 25 own ingredients");
            }
            Set<String> normalizedIngredients = new HashSet<>();
            for (OwnIngredientInput ingredient : ownIngredients) {
                if (ingredient == null || !normalizedIngredients.add(ingredient.displayText().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Own ingredients must not contain duplicate display text");
                }
            }
        }
    }

    record OwnIngredientInput(String displayText, Long ingredientConceptId) {
        public OwnIngredientInput {
            displayText = requiredText(displayText, "Own ingredient display text", 200);
            if (ingredientConceptId != null && ingredientConceptId <= 0) {
                throw new IllegalArgumentException("Ingredient concept ID must be positive when present");
            }
        }
    }

    /** A non-null change in a replace command leaves no ambiguity about overwrite permission for an existing photo. */
    record PhotoChange(ChallengeResultPhotoUpload photo, boolean replaceExisting, Long expectedPhotoVersion) {
        public PhotoChange {
            if (photo == null) {
                throw new IllegalArgumentException("Challenge result photo is required");
            }
            nullableNonNegativeVersion(expectedPhotoVersion, "Expected photo version");
        }
    }

    /** Declared content type is transport metadata only; actual image validation happens in the Challenge Core. */
    record ChallengeResultPhotoUpload(byte[] contentBytes, String declaredContentType, String originalFilename) {
        public ChallengeResultPhotoUpload {
            if (contentBytes == null) {
                throw new IllegalArgumentException("Challenge result photo bytes are required");
            }
            contentBytes = contentBytes.clone();
        }

        @Override
        public byte[] contentBytes() {
            return contentBytes.clone();
        }
    }

    private static String requiredText(String value, String label, int maximumLength) {
        String normalized = value == null ? null : value.strip();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maximumLength + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String label, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requiredText(value, label, maximumLength);
    }

    private static void positiveChallengeNumber(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Challenge number must be positive");
        }
    }

    private static void positiveParticipantId(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Participant ID must be positive");
        }
    }

    private static void nonNegativeVersion(long value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
    }

    private static void nullableNonNegativeVersion(Long value, String label) {
        if (value != null) {
            nonNegativeVersion(value, label);
        }
    }
}
