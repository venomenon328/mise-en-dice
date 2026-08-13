package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.SimilarityConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import java.math.BigDecimal;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class GeneratorConfigurationTest {

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
}
