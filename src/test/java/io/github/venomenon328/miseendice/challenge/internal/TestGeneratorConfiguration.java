package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.CooldownConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.ExclusionConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.FallbackConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.NoveltyConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.ProfileDefinition;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.SelectionConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.SimilarityConfiguration;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TestGeneratorConfiguration {
    private TestGeneratorConfiguration() { }

    static GeneratorConfiguration defaults() {
        return new GeneratorConfiguration(
                "1.0.0", "2026-08-12.2", RngAlgorithm.SPLITMIX64_V1, 1,
                12, 144, 72, 36, 5_000, 1_000_000_000L, bd("0.30"),
                Map.of(Availability.EASY, bd("1.00"), Availability.PLANNED, bd("0.65"),
                        Availability.DIFFICULT, bd("0.20"), Availability.UNAVAILABLE, bd("0.00")),
                new CooldownConfiguration(6, 9, 12, 16, bd("0.25"), bd("0.50"), bd("0.75")),
                new ExclusionConfiguration(4, 7, bd("0.35")),
                new NoveltyConfiguration(Map.of(1, 0, 2, 1, 3, 2, 4, 4, 5, 7), Map.of(
                        NoveltyBand.FAMILIAR, Map.of(1, bd("1.25"), 2, bd("1.10"), 3, bd("0.70"), 4, bd("0.15"), 5, bd("0.00")),
                        NoveltyBand.BALANCED, Map.of(1, bd("0.80"), 2, bd("1.00"), 3, bd("1.20"), 4, bd("0.75"), 5, bd("0.20")),
                        NoveltyBand.ADVENTUROUS, Map.of(1, bd("0.35"), 2, bd("0.65"), 3, bd("1.00"), 4, bd("1.30"), 5, bd("1.15"))),
                        1, 2, 11),
                Set.of("ANIMAL_PROTEIN", "PLANT_PROTEIN", "VEGETABLE", "FRUIT", "STARCH"),
                Set.of("FAT", "ACID"), Set.of("AROMATIC", "SEASONING"),
                Map.of(
                        CandidateProfile.PROTEIN_PRODUCE, new ProfileDefinition(List.of(ProfileSlot.PROTEIN, ProfileSlot.PRODUCE_1)),
                        CandidateProfile.PRODUCE_DUO, new ProfileDefinition(List.of(ProfileSlot.PRODUCE_1, ProfileSlot.PRODUCE_2)),
                        CandidateProfile.STARCH_ANCHORED, new ProfileDefinition(List.of(ProfileSlot.STARCH, ProfileSlot.PROTEIN_OR_PRODUCE)),
                        CandidateProfile.THREE_ANCHORS, new ProfileDefinition(List.of(ProfileSlot.ANCHOR_1, ProfileSlot.ANCHOR_2, ProfileSlot.ANCHOR_3)),
                        CandidateProfile.FLEXIBLE_BALANCED, new ProfileDefinition(List.of(ProfileSlot.ANCHOR_1, ProfileSlot.ANCHOR_2))),
                Map.of(CandidateProfile.PROTEIN_PRODUCE, 3, CandidateProfile.PRODUCE_DUO, 2,
                        CandidateProfile.STARCH_ANCHORED, 2, CandidateProfile.THREE_ANCHORS, 2,
                        CandidateProfile.FLEXIBLE_BALANCED, 3),
                Map.of(CandidateProfile.PROTEIN_PRODUCE, 3, CandidateProfile.PRODUCE_DUO, 2,
                        CandidateProfile.STARCH_ANCHORED, 2, CandidateProfile.THREE_ANCHORS, 2,
                        CandidateProfile.FLEXIBLE_BALANCED, 3),
                Map.of(2, 4, 3, 5, 4, 3), Map.of(2, 4, 3, 5, 4, 3),
                Map.of(NoveltyCadence.RECOVERY, Map.of(NoveltyBand.FAMILIAR, 5, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 0),
                        NoveltyCadence.NEUTRAL, Map.of(NoveltyBand.FAMILIAR, 3, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 2),
                        NoveltyCadence.SEEKING_VARIETY, Map.of(NoveltyBand.FAMILIAR, 2, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 3)),
                Map.of(ScoreComponent.STRUCTURAL_VIABILITY, bd("0.25"), ScoreComponent.ROLE_COMPLEMENTARITY, bd("0.15"),
                        ScoreComponent.CREATIVE_TENSION, bd("0.15"), ScoreComponent.OPENNESS_NON_TRIVIALITY, bd("0.10"),
                        ScoreComponent.NOVELTY_TARGET_FIT, bd("0.10"), ScoreComponent.AVAILABILITY_LOAD, bd("0.08"),
                        ScoreComponent.HISTORY_FRESHNESS, bd("0.08"), ScoreComponent.DATA_CONFIDENCE, bd("0.05"),
                        ScoreComponent.KNOWN_CULINARY_LOAD_BALANCE, bd("0.04")),
                Map.of(SimilarityComponent.EXACT_RANDOM_CONCEPTS, bd("0.35"), SimilarityComponent.INFORMATIVE_ANCESTORS, bd("0.20"),
                        SimilarityComponent.ROLES_AND_PROFILE, bd("0.15"), SimilarityComponent.SPECIFICITY_MIX, bd("0.05"),
                        SimilarityComponent.NOVELTY, bd("0.10"), SimilarityComponent.AVAILABILITY_LOAD, bd("0.05"),
                        SimilarityComponent.COMPARABLE_PROPERTIES, bd("0.10")),
                new SimilarityConfiguration(bd("0.25"), bd("0.90"), bd("0.10"), bd("0.60"), bd("0.40"),
                        bd("0.40"), bd("0.60")),
                new SelectionConfiguration(bd("0.55"), bd("0.30"), bd("0.15"), bd("0.04"), 20),
                Map.of(FallbackLevel.STRICT, new FallbackConfiguration(55, bd("0.58"), 2, 4, 0, 4, 3),
                        FallbackLevel.RELAXED_1, new FallbackConfiguration(50, bd("0.65"), 2, 5, 1, 5, 4),
                        FallbackLevel.RELAXED_2, new FallbackConfiguration(45, bd("0.72"), 3, 6, 2, 5, 4)),
                Duration.ofMinutes(15));
    }

    static GeneratorConfiguration withLimitsAndExclusion(
            int reservoirTarget,
            int reservoirStrictMinimum,
            int maximumProposalAttempts,
            String exclusionProbability
    ) {
        GeneratorConfiguration defaults = defaults();
        return new GeneratorConfiguration(
                defaults.generatorVersion(), defaults.configurationVersion(), defaults.rngAlgorithm(),
                defaults.canonicalPayloadVersion(), defaults.candidateSetSize(), reservoirTarget,
                reservoirStrictMinimum, Math.max(12, (reservoirStrictMinimum + 1) / 2), maximumProposalAttempts,
                defaults.weightQuantization(),
                bd(exclusionProbability), defaults.availabilityFactors(), defaults.cooldown(), defaults.exclusion(),
                defaults.novelty(), defaults.anchorRoles(), defaults.supportRoles(), defaults.flavorRoles(),
                defaults.profiles(), defaults.profileWeights(), defaults.profileSetTargets(),
                defaults.specificityWeights(), defaults.specificitySetTargets(), defaults.cadenceSetTargets(),
                defaults.scoreWeights(), defaults.similarityWeights(), defaults.similarity(), defaults.selection(),
                defaults.fallbacks(),
                defaults.processingLease());
    }

    private static BigDecimal bd(String value) { return new BigDecimal(value); }
}
