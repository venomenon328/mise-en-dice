package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Already materialized visible-exposure history; PostgreSQL projection belongs to phase 9D. */
public record VisibleHistorySnapshot(
        List<VisibleChallenge> challengesNewestFirst,
        List<VisibleRerollExposure> rerollExposuresNewestFirst
) {
    private static final Set<String> VISIBLE_STATUSES = Set.of("ACTIVE", "COMPLETED", "REROLLED", "ABANDONED");

    public VisibleHistorySnapshot {
        challengesNewestFirst = challengesNewestFirst == null ? List.of() : List.copyOf(challengesNewestFirst);
        rerollExposuresNewestFirst = rerollExposuresNewestFirst == null
                ? List.of() : List.copyOf(rerollExposuresNewestFirst);
        requireNewestFirst(challengesNewestFirst, VisibleChallenge::visibleAt);
        requireNewestFirst(rerollExposuresNewestFirst, VisibleRerollExposure::visibleAt);
    }

    public VisibleHistorySnapshot(List<VisibleChallenge> challengesNewestFirst) {
        this(challengesNewestFirst, List.of());
    }

    public static VisibleHistorySnapshot empty() {
        return new VisibleHistorySnapshot(List.of(), List.of());
    }

    /** All sources that count as one exact-cooldown position, deterministically ordered across equal timestamps. */
    public List<VisibleCooldownExposure> cooldownExposuresNewestFirst() {
        List<VisibleCooldownExposure> result = new ArrayList<>(
                challengesNewestFirst.size() + rerollExposuresNewestFirst.size());
        result.addAll(challengesNewestFirst);
        result.addAll(rerollExposuresNewestFirst);
        result.sort(Comparator.comparing(VisibleCooldownExposure::visibleAt).reversed()
                .thenComparing(VisibleCooldownExposure::cooldownKey));
        return List.copyOf(result);
    }

    private static <T> void requireNewestFirst(List<T> values, java.util.function.Function<T, Instant> timestamp) {
        Instant previous = null;
        for (T value : values) {
            Instant current = timestamp.apply(value);
            if (previous != null && current.isAfter(previous)) {
                throw new IllegalArgumentException("Visible history must be ordered newest first");
            }
            previous = current;
        }
    }

    public sealed interface VisibleCooldownExposure permits VisibleChallenge, VisibleRerollExposure {
        Instant visibleAt();

        String cooldownKey();

        List<VisibleRequirement> requirements();

        Set<String> restrictionRuleCodes();
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
    ) implements VisibleCooldownExposure {
        public VisibleChallenge {
            if (visibleAt == null || sessionKey == null || sessionKey.isBlank() || attemptType == null
                    || status == null || !VISIBLE_STATUSES.contains(status) || requirements.size() != 4) {
                throw new IllegalArgumentException("Visible challenges need timestamp, identity, type, status, and four requirements");
            }
            requirements = List.copyOf(requirements);
        }

        @Override
        public String cooldownKey() {
            return "challenge:" + sessionKey;
        }

        @Override
        public Set<String> restrictionRuleCodes() {
            return exclusionRuleCode == null ? Set.of() : Set.of(exclusionRuleCode);
        }
    }

    /** A complete previously presented offer set; it contributes only exact cooldown codes, never cadence data. */
    public record VisibleRerollExposure(
            Instant visibleAt,
            String sessionKey,
            String offerSetKey,
            List<VisibleRequirement> requirements,
            Set<String> restrictionRuleCodes
    ) implements VisibleCooldownExposure {
        public VisibleRerollExposure {
            if (visibleAt == null || sessionKey == null || sessionKey.isBlank()
                    || offerSetKey == null || offerSetKey.isBlank() || requirements == null || requirements.size() < 4) {
                throw new IllegalArgumentException("Reroll exposures need stable identity and complete offer snapshots");
            }
            requirements = List.copyOf(requirements);
            restrictionRuleCodes = restrictionRuleCodes == null ? Set.of() : Set.copyOf(restrictionRuleCodes);
        }

        public VisibleRerollExposure(Instant visibleAt, String sessionKey, String offerSetKey,
                                     List<VisibleRequirement> requirements) {
            this(visibleAt, sessionKey, offerSetKey, requirements, Set.of());
        }

        @Override
        public String cooldownKey() {
            return "reroll:" + offerSetKey;
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
