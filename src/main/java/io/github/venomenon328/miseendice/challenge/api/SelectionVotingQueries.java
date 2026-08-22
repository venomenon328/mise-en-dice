package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Read-only transport-neutral projections for the fixed-electorate selection workflow. */
public interface SelectionVotingQueries {

    Optional<ParticipantIdentityView> findParticipantByExternalIdentity(String provider, String externalSubject);

    Optional<SelectionView> findSelection(long sessionId);

    record ParticipantIdentityView(long participantId, String participantCode, String displayName, boolean active,
                                   String provider, String externalSubject) {
    }

    record SelectionView(
            long sessionId,
            List<ElectorateMemberView> electorate,
            OfferDecisionQueries.OfferSetView currentOfferSet,
            VotingRoundView currentRound,
            List<VotingRoundView> completedRounds,
            WaitingForPresentationView waitingForPresentation,
            OfferDecisionQueries.ChallengeView confirmedChallenge
    ) {
        public SelectionView {
            electorate = List.copyOf(electorate);
            completedRounds = List.copyOf(completedRounds);
        }
    }

    record ElectorateMemberView(long participantId, String participantCode, String displayName, boolean active) {
    }

    /** Votes are null while a round is open so projections cannot disclose another participant's choice. */
    record VotingRoundView(
            long roundId,
            int roundNumber,
            long offerSetId,
            VotingRoundStatus status,
            List<AllowedOptionView> allowedOptions,
            List<VoteStatusView> votes,
            RoundResultView result
    ) {
        public VotingRoundView {
            allowedOptions = List.copyOf(allowedOptions);
            votes = List.copyOf(votes);
        }
    }

    record AllowedOptionView(SelectionVotingCommands.VoteOptionType type, Long offerId) {
    }

    record VoteStatusView(long participantId, String participantCode, String displayName, boolean hasVoted,
                          SelectionVotingCommands.VoteChoice vote) {
    }

    record RoundResultView(
            SelectionVotingCommands.VoteChoice winningChoice,
            boolean tieBreakUsed,
            Instant completedAt,
            ApplyState applyState,
            Long resultingOfferSetId,
            String detail
    ) {
    }

    record WaitingForPresentationView(long offerSetId, int offerCount) {
    }

    enum VotingRoundStatus {
        OPEN,
        COMPLETED
    }

    /** Durable state of applying a persisted voting result through the Phase-11A public API. */
    enum ApplyState {
        PENDING,
        CONFIRMED,
        REROLL_IN_PROGRESS,
        REROLL_OFFER_READY,
        REROLL_AUTO_CONFIRM_PENDING,
        REROLL_AUTO_CONFIRMED,
        REROLL_EXHAUSTED,
        REROLL_FAILED
    }
}
