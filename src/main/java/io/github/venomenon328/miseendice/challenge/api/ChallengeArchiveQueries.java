package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Public, transport-neutral reads for confirmed challenges and their optional Cards. */
public interface ChallengeArchiveQueries {

    int MAX_PAGE_SIZE = 50;

    Optional<PublicChallenge> findCurrentChallenge();

    Optional<PublicChallenge> findChallengeByNumber(long challengeNumber);

    ChallengePage listChallenges(PageRequest request);

    Optional<ChallengeCardMetadata> findChallengeCardMetadata(long challengeNumber);

    Optional<ChallengeCardBinary> loadChallengeCard(long challengeNumber);

    record PageRequest(int page, int pageSize) {
        public PageRequest {
            if (page < 1) {
                throw new IllegalArgumentException("Page must be at least 1");
            }
            if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
            }
        }
    }

    record ChallengePage(
            int page,
            int pageSize,
            long totalChallenges,
            Long currentChallengeNumber,
            int totalPages,
            List<PublicChallenge> challenges
    ) {
        public ChallengePage {
            challenges = List.copyOf(challenges);
        }
    }

    /** Contains only historical public snapshots, never the selection, voting, or provider path. */
    record PublicChallenge(
            long challengeNumber,
            Instant confirmedAt,
            List<RequirementSnapshot> requirements,
            RestrictionSnapshot restriction,
            boolean cardAvailable
    ) {
        public PublicChallenge {
            if (challengeNumber < 1) {
                throw new IllegalArgumentException("Challenge number must be positive");
            }
            if (confirmedAt == null) {
                throw new IllegalArgumentException("Challenge confirmation timestamp is required");
            }
            requirements = List.copyOf(requirements);
            if (requirements.size() != 4) {
                throw new IllegalArgumentException("A public challenge must contain exactly four requirements");
            }
            for (int index = 0; index < requirements.size(); index++) {
                if (requirements.get(index).position() != index + 1) {
                    throw new IllegalArgumentException("Public challenge requirements must be ordered from 1 to 4");
                }
            }
            restriction = restriction == null ? RestrictionSnapshot.none() : restriction;
        }
    }

    /** Specificity is null only for an old snapshot that did not contain that historical fact. */
    record RequirementSnapshot(int position, String displayText, Specificity specificity) {
        public RequirementSnapshot {
            if (position < 1 || position > 4) {
                throw new IllegalArgumentException("Requirement position must be between 1 and 4");
            }
            if (displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Requirement display text is required");
            }
        }
    }

    enum Specificity {
        OPEN,
        SPECIFIC
    }

    record RestrictionSnapshot(boolean restricted, String displayText) {
        public RestrictionSnapshot {
            if (restricted && (displayText == null || displayText.isBlank())) {
                throw new IllegalArgumentException("A present restriction requires display text");
            }
            if (!restricted && displayText != null) {
                throw new IllegalArgumentException("An absent restriction must not have display text");
            }
        }

        public static RestrictionSnapshot none() {
            return new RestrictionSnapshot(false, null);
        }

        public static RestrictionSnapshot present(String displayText) {
            return new RestrictionSnapshot(true, displayText);
        }
    }

    record ChallengeCardMetadata(
            long challengeNumber,
            String contentType,
            String originalFilename,
            long byteSize,
            String sha256,
            Instant createdAt,
            Instant updatedAt
    ) {
        public ChallengeCardMetadata {
            if (challengeNumber < 1 || byteSize < 1) {
                throw new IllegalArgumentException("Challenge number and Card size must be positive");
            }
        }
    }

    record ChallengeCardBinary(ChallengeCardMetadata metadata, byte[] contentBytes) {
        public ChallengeCardBinary {
            if (metadata == null || contentBytes == null) {
                throw new IllegalArgumentException("Card metadata and bytes are required");
            }
            contentBytes = contentBytes.clone();
        }

        @Override
        public byte[] contentBytes() {
            return contentBytes.clone();
        }
    }
}
