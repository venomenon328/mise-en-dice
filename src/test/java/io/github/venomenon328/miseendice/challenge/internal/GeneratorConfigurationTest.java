package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.SimilarityConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneratorConfigurationTest {

    @Test
    void acceptsTheCalibratedAvailabilityFactorsAndRejectsAnyBrokenOrdering() {
        GeneratorConfiguration valid = TestGeneratorConfiguration.defaults();
        Map<Availability, BigDecimal> calibratedFactors = Map.of(
                Availability.EASY, new BigDecimal("1.00"), Availability.PLANNED, new BigDecimal("0.45"),
                Availability.SPECIALTY, new BigDecimal("0.15"), Availability.DIFFICULT, new BigDecimal("0.03"),
                Availability.UNAVAILABLE, new BigDecimal("0.00"));
        GeneratorConfiguration calibrated = copyWithAvailability(valid, calibratedFactors);
        assertThat(calibrated.availabilityFactors().get(Availability.EASY)).isEqualByComparingTo("1.00");
        assertThat(calibrated.availabilityFactors().get(Availability.PLANNED)).isEqualByComparingTo("0.45");
        assertThat(calibrated.availabilityFactors().get(Availability.SPECIALTY)).isEqualByComparingTo("0.15");
        assertThat(calibrated.availabilityFactors().get(Availability.DIFFICULT)).isEqualByComparingTo("0.03");
        assertThat(calibrated.availabilityFactors().get(Availability.UNAVAILABLE)).isEqualByComparingTo("0.00");

        Map<Availability, BigDecimal> invalid = new EnumMap<>(calibrated.availabilityFactors());
        invalid.put(Availability.SPECIALTY, new BigDecimal("0.45"));
        assertThatThrownBy(() -> copyWithAvailability(valid, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Availability factors");
    }

    @Test
    void rejectsScoreWeightsThatDoNotSumToOne() {
        GeneratorConfiguration valid = TestGeneratorConfiguration.defaults();
        var invalidWeights = new EnumMap<>(valid.scoreWeights());
        invalidWeights.put(ScoreComponent.STRUCTURAL_VIABILITY, new BigDecimal("0.24"));

        assertThatThrownBy(() -> new GeneratorConfiguration(
                valid.generatorVersion(), valid.configurationVersion(), valid.rngAlgorithm(),
                valid.canonicalPayloadVersion(), valid.candidateSetSize(), valid.reservoirTarget(),
                valid.reservoirStrictMinimum(), valid.reservoirRelaxedOneMinimum(),
                valid.maximumProposalAttempts(), valid.weightQuantization(),
                valid.exclusionProbability(), valid.availabilityFactors(), valid.cooldown(), valid.exclusion(),
                valid.novelty(), valid.anchorRoles(), valid.supportRoles(), valid.flavorRoles(), valid.profiles(),
                valid.profileWeights(), valid.profileSetTargets(), valid.specificityWeights(),
                valid.specificitySetTargets(), valid.cadenceSetTargets(), invalidWeights,
                valid.similarityWeights(), valid.similarity(), valid.selection(), valid.fallbacks(),
                valid.processingLease()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scoreWeights");
    }

    @Test
    void rejectsSimilaritySubweightsThatDoNotSumToOne() {
        assertThatThrownBy(() -> new SimilarityConfiguration(new BigDecimal("0.25"),
                new BigDecimal("0.90"), new BigDecimal("0.11"), new BigDecimal("0.60"),
                new BigDecimal("0.40"), new BigDecimal("0.40"), new BigDecimal("0.60")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subweight");
    }

    @Test
    void rejectsUnrepresentableTopBandSelectionWeightSums() {
        GeneratorConfiguration valid = TestGeneratorConfiguration.defaults();
        var unsafeSelection = new GeneratorConfiguration.SelectionConfiguration(new BigDecimal("0.55"),
                new BigDecimal("0.30"), new BigDecimal("0.15"), new BigDecimal("0.25"), 2_000_000_000);

        assertThatThrownBy(() -> copy(valid, valid.similarity(), unsafeSelection))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selection weights");
    }

    private GeneratorConfiguration copy(GeneratorConfiguration valid,
                                        SimilarityConfiguration similarity,
                                        GeneratorConfiguration.SelectionConfiguration selection) {
        return new GeneratorConfiguration(valid.generatorVersion(), valid.configurationVersion(),
                valid.rngAlgorithm(), valid.canonicalPayloadVersion(), valid.candidateSetSize(),
                valid.reservoirTarget(), valid.reservoirStrictMinimum(), valid.reservoirRelaxedOneMinimum(),
                valid.maximumProposalAttempts(), valid.weightQuantization(), valid.exclusionProbability(),
                valid.availabilityFactors(), valid.cooldown(), valid.exclusion(), valid.novelty(), valid.anchorRoles(),
                valid.supportRoles(), valid.flavorRoles(), valid.profiles(), valid.profileWeights(),
                valid.profileSetTargets(), valid.specificityWeights(), valid.specificitySetTargets(),
                valid.cadenceSetTargets(), valid.scoreWeights(), valid.similarityWeights(), similarity, selection,
                valid.fallbacks(), valid.processingLease());
    }

    private GeneratorConfiguration copyWithAvailability(GeneratorConfiguration valid,
                                                         Map<Availability, BigDecimal> availabilityFactors) {
        return new GeneratorConfiguration(valid.generatorVersion(), valid.configurationVersion(), valid.rngAlgorithm(),
                valid.canonicalPayloadVersion(), valid.candidateSetSize(), valid.reservoirTarget(),
                valid.reservoirStrictMinimum(), valid.reservoirRelaxedOneMinimum(),
                valid.maximumProposalAttempts(), valid.weightQuantization(), valid.exclusionProbability(),
                availabilityFactors, valid.cooldown(), valid.exclusion(), valid.novelty(), valid.anchorRoles(),
                valid.supportRoles(), valid.flavorRoles(), valid.profiles(), valid.profileWeights(),
                valid.profileSetTargets(), valid.specificityWeights(), valid.specificitySetTargets(),
                valid.cadenceSetTargets(), valid.scoreWeights(), valid.similarityWeights(), valid.similarity(),
                valid.selection(), valid.fallbacks(), valid.processingLease());
    }
}
