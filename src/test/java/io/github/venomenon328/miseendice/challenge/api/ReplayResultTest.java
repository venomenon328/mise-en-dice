package io.github.venomenon328.miseendice.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayDifference;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayDifferenceType;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayResult;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplayResultTest {

    @Test
    void mismatchRequiresItsFirstStructuredDifference() {
        ReplayResult result = new ReplayResult(ReplayStatus.MISMATCH, "REPLAY_FINGERPRINT_MISMATCH",
                "stored", "replayed", List.of("a"), List.of("b"),
                new ReplayDifference(ReplayDifferenceType.CANDIDATE_SIGNATURE,
                        "candidates[1].canonicalSignature", "stored", "replayed"));

        assertThat(result.difference()).isNotNull();
        assertThat(result.difference().type()).isEqualTo(ReplayDifferenceType.CANDIDATE_SIGNATURE);
        assertThat(result.difference().path()).isEqualTo("candidates[1].canonicalSignature");
        assertThat(result.difference().storedValue()).isEqualTo("stored");
        assertThat(result.difference().replayedValue()).isEqualTo("replayed");
    }

    @Test
    void mismatchCannotUseTheResultRecordToInventAFingerprintDifference() {
        assertThatThrownBy(() -> new ReplayResult(ReplayStatus.MISMATCH, "REPLAY_FINGERPRINT_MISMATCH",
                "stored", "replayed", List.of("a"), List.of("b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("structured first difference");
    }

    @Test
    void nonMismatchDoesNotInventADifference() {
        ReplayResult result = new ReplayResult(ReplayStatus.MATCH, null,
                "same", "same", List.of("a"), List.of("a"));

        assertThat(result.difference()).isNull();
    }
}
