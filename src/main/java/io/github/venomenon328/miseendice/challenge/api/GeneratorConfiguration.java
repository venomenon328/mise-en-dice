package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ProfileSlot;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RngAlgorithm;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.SimilarityComponent;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete immutable versioned generator configuration. */
public record GeneratorConfiguration(
        String generatorVersion,
        String configurationVersion,
        RngAlgorithm rngAlgorithm,
        int canonicalPayloadVersion,
        int candidateSetSize,
        int reservoirTarget,
        int reservoirStrictMinimum,
        int reservoirRelaxedOneMinimum,
        int maximumProposalAttempts,
        long weightQuantization,
        BigDecimal exclusionProbability,
        Map<Availability, BigDecimal> availabilityFactors,
        CooldownConfiguration cooldown,
        ExclusionConfiguration exclusion,
        NoveltyConfiguration novelty,
        Set<String> anchorRoles,
        Set<String> supportRoles,
        Set<String> flavorRoles,
        Map<CandidateProfile, ProfileDefinition> profiles,
        Map<CandidateProfile, Integer> profileWeights,
        Map<CandidateProfile, Integer> profileSetTargets,
        Map<Integer, Integer> specificityWeights,
        Map<Integer, Integer> specificitySetTargets,
        Map<NoveltyCadence, Map<NoveltyBand, Integer>> cadenceSetTargets,
        Map<ScoreComponent, BigDecimal> scoreWeights,
        Map<SimilarityComponent, BigDecimal> similarityWeights,
        SimilarityConfiguration similarity,
        SelectionConfiguration selection,
        Map<FallbackLevel, FallbackConfiguration> fallbacks,
        Duration processingLease
) {
    private static final BigDecimal ONE = BigDecimal.ONE;

    public GeneratorConfiguration {
        requireText(generatorVersion, "generatorVersion");
        requireText(configurationVersion, "configurationVersion");
        if (rngAlgorithm != RngAlgorithm.SPLITMIX64_V1 || canonicalPayloadVersion <= 0) {
            throw new IllegalArgumentException("Unsupported RNG or canonical payload version");
        }
        if (candidateSetSize != 12 || reservoirTarget < 12 || reservoirTarget > 2_000
                || reservoirStrictMinimum < candidateSetSize || reservoirStrictMinimum > reservoirTarget
                || reservoirRelaxedOneMinimum < candidateSetSize
                || reservoirRelaxedOneMinimum > reservoirStrictMinimum
                || maximumProposalAttempts < reservoirTarget || maximumProposalAttempts > 1_000_000
                || weightQuantization < 1_000L || weightQuantization > 1_000_000_000_000L) {
            throw new IllegalArgumentException("Invalid reservoir, set-size, attempt, or quantization configuration");
        }
        requireProbability(exclusionProbability, "exclusionProbability");
        if (generatorVersion.equals("1.2.0") && exclusionProbability.compareTo(new BigDecimal("0.20")) != 0) {
            throw new IllegalArgumentException("Generator 1.2.0 requires a candidate restriction probability of 0.20");
        }
        availabilityFactors = immutableEnumMap(Availability.class, availabilityFactors);
        profiles = immutableEnumMap(CandidateProfile.class, profiles);
        profileWeights = immutableEnumMap(CandidateProfile.class, profileWeights);
        profileSetTargets = immutableEnumMap(CandidateProfile.class, profileSetTargets);
        specificityWeights = Map.copyOf(specificityWeights);
        specificitySetTargets = Map.copyOf(specificitySetTargets);
        cadenceSetTargets = immutableNestedEnumMap(cadenceSetTargets);
        scoreWeights = immutableEnumMap(ScoreComponent.class, scoreWeights);
        similarityWeights = immutableEnumMap(SimilarityComponent.class, similarityWeights);
        fallbacks = immutableEnumMap(FallbackLevel.class, fallbacks);
        anchorRoles = Set.copyOf(anchorRoles);
        supportRoles = Set.copyOf(supportRoles);
        flavorRoles = Set.copyOf(flavorRoles);

        requireComplete(Availability.class, availabilityFactors, "availabilityFactors");
        requireComplete(CandidateProfile.class, profiles, "profiles");
        requireComplete(CandidateProfile.class, profileWeights, "profileWeights");
        requireComplete(CandidateProfile.class, profileSetTargets, "profileSetTargets");
        requireComplete(ScoreComponent.class, scoreWeights, "scoreWeights");
        Set<SimilarityComponent> expectedSimilarityComponents = generatorVersion.equals("1.2.0")
                ? EnumSet.allOf(SimilarityComponent.class)
                : EnumSet.complementOf(EnumSet.of(SimilarityComponent.RESTRICTION));
        if (!similarityWeights.keySet().equals(expectedSimilarityComponents)) {
            throw new IllegalArgumentException("similarityWeights must match the generator version");
        }
        requireComplete(FallbackLevel.class, fallbacks, "fallbacks");
        validateAvailabilityFactors(availabilityFactors);
        if (anchorRoles.isEmpty() || supportRoles.isEmpty() || flavorRoles.isEmpty()) {
            throw new IllegalArgumentException("Role classes must not be empty");
        }
        if (!specificityWeights.keySet().equals(Set.of(2, 3, 4))
                || !specificitySetTargets.keySet().equals(Set.of(2, 3, 4))) {
            throw new IllegalArgumentException("Specificity configuration must contain exactly 2, 3, and 4");
        }
        requirePositiveWeights(profileWeights, "profileWeights");
        requirePositiveWeights(specificityWeights, "specificityWeights");
        requireTargetSum(profileSetTargets, candidateSetSize, "profileSetTargets");
        requireTargetSum(specificitySetTargets, candidateSetSize, "specificitySetTargets");
        for (NoveltyCadence state : NoveltyCadence.values()) {
            Map<NoveltyBand, Integer> targets = cadenceSetTargets.get(state);
            requireComplete(NoveltyBand.class, targets, "cadenceSetTargets." + state);
            requireTargetSum(targets, candidateSetSize, "cadenceSetTargets." + state);
        }
        requireDecimalSum(scoreWeights, ONE, "scoreWeights");
        requireDecimalSum(similarityWeights, ONE, "similarityWeights");
        if (similarity == null || selection == null) {
            throw new IllegalArgumentException("Similarity and selection configuration are required");
        }
        if (processingLease == null || processingLease.compareTo(Duration.ofMinutes(1)) < 0
                || processingLease.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalArgumentException("processingLease must be between 1 minute and 24 hours");
        }
        validateFallbacks(fallbacks);
        validateSelectionWeightBounds(reservoirTarget, weightQuantization, selection);
    }

    /** Returns the same immutable generator contract with only the diagnostic exclusion mode changed. */
    public GeneratorConfiguration withExclusionProbability(BigDecimal probability) {
        return new GeneratorConfiguration(generatorVersion, configurationVersion, rngAlgorithm, canonicalPayloadVersion,
                candidateSetSize, reservoirTarget, reservoirStrictMinimum, reservoirRelaxedOneMinimum,
                maximumProposalAttempts, weightQuantization, probability, availabilityFactors, cooldown, exclusion,
                novelty, anchorRoles, supportRoles, flavorRoles, profiles, profileWeights, profileSetTargets,
                specificityWeights, specificitySetTargets, cadenceSetTargets, scoreWeights, similarityWeights,
                similarity, selection, fallbacks, processingLease);
    }

    public record SimilarityConfiguration(
            BigDecimal informativeAncestorMaximumDrawableShare,
            BigDecimal roleWeight,
            BigDecimal profileWeight,
            BigDecimal noveltyBandWeight,
            BigDecimal noveltyLoadWeight,
            BigDecimal flagWeight,
            BigDecimal dimensionWeight
    ) {
        public SimilarityConfiguration {
            requireProbability(informativeAncestorMaximumDrawableShare,
                    "informativeAncestorMaximumDrawableShare");
            requireProbability(roleWeight, "roleWeight");
            requireProbability(profileWeight, "profileWeight");
            requireProbability(noveltyBandWeight, "noveltyBandWeight");
            requireProbability(noveltyLoadWeight, "noveltyLoadWeight");
            requireProbability(flagWeight, "flagWeight");
            requireProbability(dimensionWeight, "dimensionWeight");
            if (roleWeight.add(profileWeight).compareTo(ONE) != 0
                    || noveltyBandWeight.add(noveltyLoadWeight).compareTo(ONE) != 0
                    || flagWeight.add(dimensionWeight).compareTo(ONE) != 0) {
                throw new IllegalArgumentException("Every similarity subweight group must sum exactly to one");
            }
        }
    }

    public record CooldownConfiguration(
            int hardWindow,
            int firstDecayEnd,
            int secondDecayEnd,
            int thirdDecayEnd,
            BigDecimal firstDecayFactor,
            BigDecimal secondDecayFactor,
            BigDecimal thirdDecayFactor
    ) {
        public CooldownConfiguration {
            if (hardWindow < 0 || hardWindow > 52 || firstDecayEnd <= hardWindow
                    || secondDecayEnd <= firstDecayEnd || thirdDecayEnd <= secondDecayEnd || thirdDecayEnd > 104) {
                throw new IllegalArgumentException("Cooldown boundaries must be strictly increasing");
            }
            requireProbability(firstDecayFactor, "firstDecayFactor");
            requireProbability(secondDecayFactor, "secondDecayFactor");
            requireProbability(thirdDecayFactor, "thirdDecayFactor");
            if (firstDecayFactor.signum() <= 0 || secondDecayFactor.compareTo(firstDecayFactor) <= 0
                    || thirdDecayFactor.compareTo(secondDecayFactor) <= 0 || thirdDecayFactor.compareTo(ONE) >= 0) {
                throw new IllegalArgumentException("Cooldown decay factors must increase strictly between zero and one");
            }
        }
    }

    public record ExclusionConfiguration(int hardWindow, int decayEnd, BigDecimal decayFactor) {
        public ExclusionConfiguration {
            if (hardWindow < 0 || hardWindow > 52 || decayEnd <= hardWindow || decayEnd > 104) {
                throw new IllegalArgumentException("Invalid exclusion cooldown boundaries");
            }
            requireProbability(decayFactor, "exclusion decayFactor");
            if (decayFactor.signum() <= 0) {
                throw new IllegalArgumentException("Exclusion decay factor must be positive");
            }
        }
    }

    public record NoveltyConfiguration(
            Map<Integer, Integer> loadPoints,
            Map<NoveltyBand, Map<Integer, BigDecimal>> targetFactors,
            int levelFiveCap,
            int highLevelCap,
            int loadCap
    ) {
        public NoveltyConfiguration {
            loadPoints = Map.copyOf(loadPoints);
            targetFactors = immutableNestedEnumIntegerMap(targetFactors);
            if (!loadPoints.keySet().equals(Set.of(1, 2, 3, 4, 5))
                    || loadPoints.values().stream().anyMatch(value -> value == null || value < 0)
                    || levelFiveCap < 0 || levelFiveCap > 4 || highLevelCap < levelFiveCap
                    || highLevelCap > 4 || loadCap < 0 || loadCap > 28) {
                throw new IllegalArgumentException("Invalid novelty points or caps");
            }
            for (NoveltyBand band : NoveltyBand.values()) {
                Map<Integer, BigDecimal> factors = targetFactors.get(band);
                if (factors == null || !factors.keySet().equals(Set.of(1, 2, 3, 4, 5))
                        || factors.values().stream().anyMatch(value -> value == null || value.signum() < 0)) {
                    throw new IllegalArgumentException("Incomplete novelty target factors for " + band);
                }
            }
        }
    }

    public record ProfileDefinition(List<ProfileSlot> requiredSlots) {
        public ProfileDefinition {
            requiredSlots = List.copyOf(requiredSlots);
            if (requiredSlots.isEmpty()) {
                throw new IllegalArgumentException("A profile needs required slots");
            }
        }
    }

    public record SelectionConfiguration(
            BigDecimal qualityWeight,
            BigDecimal diversityWeight,
            BigDecimal quotaWeight,
            BigDecimal topBandWidth,
            int topBandSlope
    ) {
        public SelectionConfiguration {
            requireProbability(qualityWeight, "qualityWeight");
            requireProbability(diversityWeight, "diversityWeight");
            requireProbability(quotaWeight, "quotaWeight");
            if (qualityWeight.add(diversityWeight).add(quotaWeight).compareTo(ONE) != 0
                    || topBandWidth == null || topBandWidth.signum() < 0
                    || topBandWidth.compareTo(new BigDecimal("0.25")) > 0 || topBandSlope <= 0) {
                throw new IllegalArgumentException("Invalid MMR selection configuration");
            }
        }
    }

    public record FallbackConfiguration(
            int minimumScore,
            BigDecimal maximumPairSimilarity,
            int conceptCap,
            int ancestorCap,
            int quotaDeviation,
            int profileCap,
            int difficultCandidateCap
    ) {
        public FallbackConfiguration {
            if (minimumScore < 0 || minimumScore > 100 || maximumPairSimilarity == null
                    || maximumPairSimilarity.signum() < 0 || maximumPairSimilarity.compareTo(ONE) > 0
                    || conceptCap < 1 || conceptCap > 12 || ancestorCap < 1 || ancestorCap > 12
                    || quotaDeviation < 0 || quotaDeviation > 12 || profileCap < 1 || profileCap > 12
                    || difficultCandidateCap < 0 || difficultCandidateCap > 12) {
                throw new IllegalArgumentException("Invalid fallback configuration");
            }
        }
    }

    private static void validateFallbacks(Map<FallbackLevel, FallbackConfiguration> values) {
        FallbackConfiguration strict = values.get(FallbackLevel.STRICT);
        FallbackConfiguration one = values.get(FallbackLevel.RELAXED_1);
        FallbackConfiguration two = values.get(FallbackLevel.RELAXED_2);
        if (one.minimumScore() > strict.minimumScore() || two.minimumScore() > one.minimumScore()
                || one.maximumPairSimilarity().compareTo(strict.maximumPairSimilarity()) < 0
                || two.maximumPairSimilarity().compareTo(one.maximumPairSimilarity()) < 0
                || one.conceptCap() < strict.conceptCap() || two.conceptCap() < one.conceptCap()
                || one.ancestorCap() < strict.ancestorCap() || two.ancestorCap() < one.ancestorCap()
                || one.profileCap() < strict.profileCap() || two.profileCap() < one.profileCap()
                || one.quotaDeviation() < strict.quotaDeviation()
                || two.quotaDeviation() < one.quotaDeviation()
                || one.difficultCandidateCap() < strict.difficultCandidateCap()
                || two.difficultCandidateCap() < one.difficultCandidateCap()) {
            throw new IllegalArgumentException("Fallbacks must relax monotonically");
        }
    }

    private static void validateSelectionWeightBounds(
            int reservoirTarget,
            long weightQuantization,
            SelectionConfiguration selection
    ) {
        try {
            BigDecimal maximumBase = ONE.add(BigDecimal.valueOf(selection.topBandSlope())
                    .multiply(selection.topBandWidth()));
            long maximumWeight = maximumBase.multiply(maximumBase)
                    .multiply(BigDecimal.valueOf(weightQuantization))
                    .setScale(0, java.math.RoundingMode.HALF_EVEN)
                    .longValueExact();
            Math.multiplyExact(maximumWeight, reservoirTarget);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Selection weights cannot be represented without overflow", exception);
        }
    }

    private static void validateAvailabilityFactors(Map<Availability, BigDecimal> factors) {
        BigDecimal easy = factors.get(Availability.EASY);
        BigDecimal planned = factors.get(Availability.PLANNED);
        BigDecimal difficult = factors.get(Availability.DIFFICULT);
        BigDecimal unavailable = factors.get(Availability.UNAVAILABLE);
        if (easy.compareTo(ONE) != 0 || unavailable.signum() != 0 || difficult.signum() <= 0
                || planned.compareTo(difficult) <= 0 || planned.compareTo(ONE) > 0) {
            throw new IllegalArgumentException(
                    "Availability factors require EASY=1, UNAVAILABLE=0, and 0<DIFFICULT<PLANNED<=1");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireProbability(BigDecimal value, String name) {
        if (value == null || value.signum() < 0 || value.compareTo(ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    private static <E extends Enum<E>, V> Map<E, V> immutableEnumMap(Class<E> type, Map<E, V> source) {
        if (source == null) {
            throw new IllegalArgumentException(type.getSimpleName() + " map must not be null");
        }
        EnumMap<E, V> copy = new EnumMap<>(type);
        copy.putAll(source);
        return Map.copyOf(copy);
    }

    private static Map<NoveltyCadence, Map<NoveltyBand, Integer>> immutableNestedEnumMap(
            Map<NoveltyCadence, Map<NoveltyBand, Integer>> source) {
        EnumMap<NoveltyCadence, Map<NoveltyBand, Integer>> copy = new EnumMap<>(NoveltyCadence.class);
        source.forEach((key, value) -> copy.put(key, immutableEnumMap(NoveltyBand.class, value)));
        return Map.copyOf(copy);
    }

    private static Map<NoveltyBand, Map<Integer, BigDecimal>> immutableNestedEnumIntegerMap(
            Map<NoveltyBand, Map<Integer, BigDecimal>> source) {
        EnumMap<NoveltyBand, Map<Integer, BigDecimal>> copy = new EnumMap<>(NoveltyBand.class);
        source.forEach((key, value) -> copy.put(key, Map.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static <E extends Enum<E>, V> void requireComplete(Class<E> type, Map<E, V> values, String name) {
        if (values == null || !values.keySet().equals(Set.of(type.getEnumConstants()))) {
            throw new IllegalArgumentException(name + " must contain every " + type.getSimpleName());
        }
    }

    private static void requirePositiveWeights(Map<?, Integer> values, String name) {
        if (values.values().stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException(name + " must contain positive weights");
        }
    }

    private static void requireTargetSum(Map<?, Integer> values, int target, String name) {
        if (values.values().stream().anyMatch(value -> value == null || value < 0)
                || values.values().stream().mapToInt(Integer::intValue).sum() != target) {
            throw new IllegalArgumentException(name + " must contain non-negative targets summing to " + target);
        }
    }

    private static void requireDecimalSum(Map<?, BigDecimal> values, BigDecimal target, String name) {
        if (values.values().stream().anyMatch(value -> value == null || value.signum() < 0)
                || values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(target) != 0) {
            throw new IllegalArgumentException(name + " must contain non-negative weights summing to " + target);
        }
    }
}
