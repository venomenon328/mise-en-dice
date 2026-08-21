package io.github.venomenon328.miseendice.challenge.api;

/** Transport-neutral commands for the one optional current Card of a confirmed challenge. */
public interface ChallengeCardCommands {

    ChallengeArchiveQueries.ChallengeCardMetadata setChallengeCard(SetChallengeCard command);

    void removeChallengeCard(RemoveChallengeCard command);

    record SetChallengeCard(long challengeNumber, ChallengeCardUpload upload, boolean replaceExisting) {
        public SetChallengeCard {
            requirePositiveChallengeNumber(challengeNumber);
            if (upload == null) {
                throw new IllegalArgumentException("Challenge Card upload is required");
            }
        }
    }

    record RemoveChallengeCard(long challengeNumber) {
        public RemoveChallengeCard {
            requirePositiveChallengeNumber(challengeNumber);
        }
    }

    /** The declared content type is transport metadata only; the Core verifies the actual PNG bytes. */
    record ChallengeCardUpload(byte[] contentBytes, String declaredContentType, String originalFilename) {
        public ChallengeCardUpload {
            if (contentBytes == null) {
                throw new IllegalArgumentException("Challenge Card bytes are required");
            }
            contentBytes = contentBytes.clone();
        }

        @Override
        public byte[] contentBytes() {
            return contentBytes.clone();
        }
    }

    private static void requirePositiveChallengeNumber(long challengeNumber) {
        if (challengeNumber < 1) {
            throw new IllegalArgumentException("Challenge number must be positive");
        }
    }
}
