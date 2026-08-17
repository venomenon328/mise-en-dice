package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands.VoteChoice;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class VotingRoundEvaluatorTest {
    private final VotingRoundEvaluator evaluator = new VotingRoundEvaluator();

    @Test
    void uniqueWinnerDoesNotConsultTheTieBreakSource() {
        AtomicInteger calls = new AtomicInteger();

        VotingRoundEvaluator.Evaluation evaluation = evaluator.evaluate(
                List.of(VoteChoice.offer(10), VoteChoice.offer(10), VoteChoice.reroll()),
                bound -> {
                    calls.incrementAndGet();
                    throw new AssertionError("A unique winner must not use random tie breaking");
                });

        assertThat(evaluation.winningChoice()).isEqualTo(VoteChoice.offer(10));
        assertThat(evaluation.tieBreakUsed()).isFalse();
        assertThat(calls).hasValue(0);
    }

    @Test
    void tieBreakReceivesOnlyTheJointTopOptions() {
        AtomicInteger bound = new AtomicInteger();

        VotingRoundEvaluator.Evaluation evaluation = evaluator.evaluate(
                List.of(VoteChoice.offer(10), VoteChoice.offer(10), VoteChoice.offer(20), VoteChoice.offer(20),
                        VoteChoice.reroll()),
                value -> {
                    bound.set(value);
                    return 1;
                });

        assertThat(bound).hasValue(2);
        assertThat(evaluation.winningChoice()).isEqualTo(VoteChoice.offer(20));
        assertThat(evaluation.tieBreakUsed()).isTrue();
    }

    @Test
    void acceptAndRerollAreNormalTieBreakOptions() {
        VotingRoundEvaluator.Evaluation evaluation = evaluator.evaluate(
                List.of(VoteChoice.accept(), VoteChoice.reroll()), bound -> 1);

        assertThat(evaluation.winningChoice()).isEqualTo(VoteChoice.reroll());
        assertThat(evaluation.tieBreakUsed()).isTrue();
    }
}
