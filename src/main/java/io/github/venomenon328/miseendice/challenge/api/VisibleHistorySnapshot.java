package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Already prepared visible-exposure history; database projection belongs to phase 9C. */
public record VisibleHistorySnapshot(List<VisibleChallenge> challengesNewestFirst) {
    private static final Set<String> VISIBLE_STATUSES = Set.of("ACTIVE", "COMPLETED", "REROLLED", "ABANDONED");

    public VisibleHistorySnapshot {
        challengesNewestFirst = List.copyOf(challengesNewestFirst);
        Instant previous = null;
        for (VisibleChallenge challenge : challengesNewestFirst) {
            if (previous != null && challenge.visibleAt().isAfter(previous)) {
                throw new IllegalArgumentException("Visible history must be ordered newest first");
            }
            previous = challenge.visibleAt();
        }
    }

    public static VisibleHistorySnapshot empty() {
        return new VisibleHistorySnapshot(List.of());
    }

    public record VisibleChallenge(
            Instant visibleAt,
            String sessionKey,
            AttemptType attemptType,
            String status,
            List<VisibleRequirement> requirements,
            CandidateProfile profile,
            NoveltyBand noveltyBand,
            String exclusionRuleCode
    ) {
        public VisibleChallenge {
            if (visibleAt == null || sessionKey == null || sessionKey.isBlank() || attemptType == null
                    || status == null || !VISIBLE_STATUSES.contains(status) || requirements.size() != 4) {
                throw new IllegalArgumentException("Visible challenges need timestamp, identity, type, status, and four requirements");
            }
            requirements = List.copyOf(requirements);
        }
    }

    public record VisibleRequirement(
            String conceptCode,
            Integer noveltyLevel,
            Set<String> roles,
            Set<String> flags,
            Set<String> ancestorCodes
    ) {
        public VisibleRequirement {
            roles = Set.copyOf(roles);
            flags = Set.copyOf(flags);
            ancestorCodes = Set.copyOf(ancestorCodes);
        }
    }
}
