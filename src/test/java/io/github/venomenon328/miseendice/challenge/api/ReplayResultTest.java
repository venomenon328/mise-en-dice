package io.github.venomenon328.miseendice.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayDifferenceType;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayResult;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplayResultTest {

    @Test
    void mismatchAlwaysExposesItsFirstStructuredDifference() {
        ReplayResult result = new ReplayResult(ReplayStatus.MISMATCH, "REPLAY_FINGERPRINT_MISMATCH",
                "stored", "replayed", List.of("a"), List.of("b"));

        assertThat(result.difference()).isNotNull();
        assertThat(result.difference().type()).isEqualTo(ReplayDifferenceType.SET_FINGERPRINT);
        assertThat(result.difference().path()).isEqualTo("setFingerprint");
        assertThat(result.difference().storedValue()).isEqualTo("stored");
        assertThat(result.difference().replayedValue()).isEqualTo("replayed");
    }

    @Test
    void nonMismatchDoesNotInventADifference() {
        ReplayResult result = new ReplayResult(ReplayStatus.MATCH, null,
                "same", "same", List.of("a"), List.of("a"));

        assertThat(result.difference()).isNull();
    }
}
