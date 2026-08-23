package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Bytes-free Challenge-result reads plus the deliberate binary-photo read. */
public interface ChallengeResultQueries {

    Optional<ChallengeResultView> findChallengeResult(long challengeNumber, long participantId);

    List<ChallengeResultView> listChallengeResults(long challengeNumber);

    Optional<ChallengeResultPhotoMetadata> findChallengeResultPhotoMetadata(long challengeNumber, long participantId);

    Optional<ChallengeResultPhotoBinary> loadChallengeResultPhoto(long challengeNumber, long participantId);

    record ChallengeResultView(
            long resultId,
            long challengeNumber,
            ParticipantReference participant,
            String dishName,
            String description,
            String evaluation,
            List<ResultIngredientView> ownIngredients,
            List<ResultConcretizationView> concretizations,
            boolean photoAvailable,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public ChallengeResultView(long resultId, long challengeNumber, ParticipantReference participant,
                                   String dishName, String description, String evaluation,
                                   List<ResultIngredientView> ownIngredients, boolean photoAvailable, long version,
                                   Instant createdAt, Instant updatedAt) {
            this(resultId, challengeNumber, participant, dishName, description, evaluation, ownIngredients, List.of(),
                    photoAvailable, version, createdAt, updatedAt);
        }

        public ChallengeResultView {
            if (resultId <= 0 || challengeNumber <= 0 || participant == null || version < 0
                    || createdAt == null || updatedAt == null) {
                throw new IllegalArgumentException("Challenge result identity and timestamps are required");
            }
            ownIngredients = List.copyOf(ownIngredients);
            concretizations = List.copyOf(concretizations);
        }
    }

    record ParticipantReference(long participantId, String participantCode, String displayName, boolean active) {
        public ParticipantReference {
            if (participantId <= 0 || participantCode == null || participantCode.isBlank()
                    || displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Stable participant reference and display name are required");
            }
        }
    }

    record ResultIngredientView(long resultIngredientId, String displayText, IngredientConceptReference ingredientConcept) {
        public ResultIngredientView {
            if (resultIngredientId <= 0 || displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Result ingredient identity and display text are required");
            }
        }
    }

    record ResultConcretizationView(long resultId, int requirementPosition, long openRequirementConceptId,
                                    String requirementDisplayText, String displayText,
                                    IngredientConceptReference ingredientConcept) {
        public ResultConcretizationView {
            if (resultId <= 0 || requirementPosition < 1 || requirementPosition > 4 || openRequirementConceptId <= 0
                    || requirementDisplayText == null || requirementDisplayText.isBlank()
                    || displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Result concretization identity and display texts are required");
            }
        }
    }

    record IngredientConceptReference(long ingredientConceptId, String code, String displayName, boolean active) {
        public IngredientConceptReference {
            if (ingredientConceptId <= 0 || code == null || code.isBlank() || displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Ingredient concept reference is incomplete");
            }
        }
    }

    record ChallengeResultPhotoMetadata(
            long challengeNumber,
            long participantId,
            String contentType,
            String originalFilename,
            long byteSize,
            int width,
            int height,
            String sha256,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public ChallengeResultPhotoMetadata {
            if (challengeNumber <= 0 || participantId <= 0 || byteSize <= 0 || width <= 0 || height <= 0
                    || version < 0 || createdAt == null || updatedAt == null) {
                throw new IllegalArgumentException("Challenge result photo metadata is incomplete");
            }
        }
    }

    record ChallengeResultPhotoBinary(ChallengeResultPhotoMetadata metadata, byte[] contentBytes) {
        public ChallengeResultPhotoBinary {
            if (metadata == null || contentBytes == null) {
                throw new IllegalArgumentException("Challenge result photo metadata and bytes are required");
            }
            contentBytes = contentBytes.clone();
        }

        @Override
        public byte[] contentBytes() {
            return contentBytes.clone();
        }
    }
}
