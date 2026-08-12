package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
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
                valid.reservoirStrictMinimum(), valid.maximumProposalAttempts(), valid.weightQuantization(),
                valid.exclusionProbability(), valid.availabilityFactors(), valid.cooldown(), valid.exclusion(),
                valid.novelty(), valid.anchorRoles(), valid.supportRoles(), valid.flavorRoles(), valid.profiles(),
                valid.profileWeights(), valid.profileSetTargets(), valid.specificityWeights(),
                valid.specificitySetTargets(), valid.cadenceSetTargets(), invalidWeights,
                valid.similarityWeights(), valid.selection(), valid.fallbacks(), valid.processingLease()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scoreWeights");
    }
}
