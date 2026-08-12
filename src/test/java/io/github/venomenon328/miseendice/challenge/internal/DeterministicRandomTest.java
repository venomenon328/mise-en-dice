package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeterministicRandomTest {

    @Test
    void splitMix64MatchesGoldenVectorsIncludingNegativeSeeds() {
        assertThat(outputs(0L)).containsExactly(
                -2152535657050944081L, 7960286522194355700L,
                487617019471545679L, -537132696929009172L);
        assertThat(outputs(-1L)).containsExactly(
                -1956407806741107680L, -1612297016619662647L,
                4048727598324417001L, 7862637804313477842L);
        assertThat(outputs(Long.MIN_VALUE)).containsExactly(
                5196802822362493915L, -4292029157624213486L,
                7036458801432265024L, 6426116064599561977L);
    }

    @Test
    void boundedDrawsUseTheSpecifiedRejectionAlgorithm() {
        assertThat(new SplitMix64(0).nextLong(1)).isZero();
        assertThat(new SplitMix64(0).nextLong(3)).isZero();
        assertThat(new SplitMix64(0).nextLong(12)).isEqualTo(3);
        assertThat(new SplitMix64(0).nextLong(1_000_000_000L)).isEqualTo(329_303_767L);
        assertThat(new SplitMix64(0).nextLong((1L << 62) + 1)).isEqualTo(3_980_143_261_097_177_850L);
        assertThat(new SplitMix64(-1).nextLong(12)).isEqualTo(4);
    }

    @Test
    void sha256SubstreamsUseNfcAndRealNullSeparators() {
        assertThat(SeedDerivation.derive("1.0.0", 0, "batch/1",
                SeedDerivation.Purpose.PROPOSAL_PROFILE, 0)).isEqualTo(-2040276992208456327L);
        assertThat(SeedDerivation.derive("1.0.0", -1, "batch/7",
                SeedDerivation.Purpose.PROPOSAL_SLOT_4, 123)).isEqualTo(7593157400136693928L);
        assertThat(SeedDerivation.derive("1.0.0", Long.MIN_VALUE, "attempt",
                SeedDerivation.Purpose.ATTEMPT_EXCLUSION_MODE, 0)).isEqualTo(-3974647204649309894L);
    }

    private long[] outputs(long seed) {
        SplitMix64 random = new SplitMix64(seed);
        return new long[] { random.nextLong(), random.nextLong(), random.nextLong(), random.nextLong() };
    }
}
