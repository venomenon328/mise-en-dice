package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands.VoteChoice;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pure majority and top-tie evaluation. Persistence guarantees that it is invoked only once per round. */
final class VotingRoundEvaluator {

    Evaluation evaluate(List<VoteChoice> votes, TieBreakRandomSource tieBreakRandom) {
        if (votes == null || votes.isEmpty()) {
            throw new IllegalArgumentException("A completed voting round needs at least one vote");
        }
        if (tieBreakRandom == null) {
            throw new IllegalArgumentException("Tie-break random source must be present");
        }
        Map<VoteChoice, Long> counts = votes.stream().collect(Collectors.groupingBy(
                choice -> choice,
                Collectors.counting()
        ));
        long highest = counts.values().stream().mapToLong(Long::longValue).max().orElseThrow();
        List<VoteChoice> leaders = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == highest)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing((VoteChoice choice) -> choice.type().ordinal())
                        .thenComparing(choice -> choice.offerId() == null ? Long.MIN_VALUE : choice.offerId()))
                .toList();
        if (leaders.size() == 1) {
            return new Evaluation(leaders.getFirst(), false);
        }
        return new Evaluation(leaders.get(tieBreakRandom.nextInt(leaders.size())), true);
    }

    record Evaluation(VoteChoice winningChoice, boolean tieBreakUsed) {
    }
}
