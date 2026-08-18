package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class CandidateRestrictionProbabilityTest {

    @Test
    void autoRestrictionSubstreamSamplesAroundConfiguredTwentyPercent() {
        var configuration = TestGeneratorConfiguration.candidateRestrictionDefaults();
        long threshold = configuration.exclusionProbability().multiply(BigDecimal.valueOf(1_000_000_000L))
                .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
        long attemptSeed = 93_000_001L;
        long selected = LongStream.range(0, 1_000)
                .filter(ordinal -> {
                    long seed = SeedDerivation.derive(configuration.generatorVersion(), attemptSeed,
                            SeedDerivation.batchScope(1), SeedDerivation.Purpose.CANDIDATE_RESTRICTION_MODE, ordinal);
                    return new SplitMix64(seed).nextLong(1_000_000_000L) < threshold;
                })
                .count();

        assertThat(configuration.exclusionProbability()).isEqualByComparingTo("0.20");
        assertThat(selected).isBetween(160L, 240L);
    }
}
