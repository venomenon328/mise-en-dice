package io.github.venomenon328.miseendice.challenge.api;

import java.util.List;

/**
 * Public, transport-neutral Phase-11B commands for electorate snapshots, voting, and challenge participation.
 * Actual offer presentation, confirmation, and rerolling remain owned by {@link OfferDecisionCommands}.
 */
public interface SelectionVotingCommands {

    SelectionVotingQueries.SelectionView initialize(InitializeSelection command);

    SelectionVotingQueries.SelectionView presentationSucceeded(PresentationSucceeded command);

    SelectionVotingQueries.SelectionView castVote(CastVote command);

    /**
     * Persists a vote and, if it is decisive, the immutable round result without applying that result yet.
     * Callers can render the durable state before explicitly continuing it through {@link #resume(ResumeSelection)}.
     */
    SelectionVotingQueries.SelectionView castVoteDeferred(CastVote command);

    SelectionVotingQueries.SelectionView resume(ResumeSelection command);

    SelectionVotingQueries.ChallengeParticipantView joinChallenge(JoinChallenge command);

    SelectionVotingQueries.ParticipantIdentityView linkExternalIdentity(LinkExternalIdentity command);

    /**
     * Starts the fixed electorate snapshot. An empty participant list deliberately selects the configured
     * transport-neutral default electorate (currently the stable GEORGIA and TOBIAS participant codes).
     */
    record InitializeSelection(long sessionId, List<Long> participantIds) {
        public InitializeSelection {
            requireId(sessionId, "Challenge session");
            participantIds = participantIds == null ? List.of() : List.copyOf(participantIds);
            if (participantIds.stream().anyMatch(id -> id == null || id <= 0)
                    || participantIds.stream().distinct().count() != participantIds.size()) {
                throw new IllegalArgumentException("Electorate participant IDs must be distinct positive values");
            }
        }

        public InitializeSelection(long sessionId) {
            this(sessionId, List.of());
        }
    }

    /** Reports that a previously rendered set was actually delivered visibly by an adapter. */
    record PresentationSucceeded(long sessionId, long offerSetId) {
        public PresentationSucceeded {
            requireId(sessionId, "Challenge session");
            requireId(offerSetId, "Offer set");
        }
    }

    record CastVote(long sessionId, long participantId, VoteChoice choice) {
        public CastVote {
            requireId(sessionId, "Challenge session");
            requireId(participantId, "Participant");
            if (choice == null) {
                throw new IllegalArgumentException("Vote choice must be present");
            }
        }
    }

    record ResumeSelection(long sessionId) {
        public ResumeSelection {
            requireId(sessionId, "Challenge session");
        }
    }

    record JoinChallenge(long challengeId, long participantId) {
        public JoinChallenge {
            requireId(challengeId, "Challenge");
            requireId(participantId, "Participant");
        }
    }

    /** Generic external identity mapping; providers and subjects are opaque transport values. */
    record LinkExternalIdentity(long participantId, String provider, String externalSubject) {
        public LinkExternalIdentity {
            requireId(participantId, "Participant");
            requireText(provider, "Identity provider");
            requireText(externalSubject, "External subject");
        }
    }

    record VoteChoice(VoteOptionType type, Long offerId) {
        public VoteChoice {
            if (type == null) {
                throw new IllegalArgumentException("Vote option type must be present");
            }
            if (type == VoteOptionType.OFFER && (offerId == null || offerId <= 0)) {
                throw new IllegalArgumentException("An offer vote needs a positive offer ID");
            }
            if (type != VoteOptionType.OFFER && offerId != null) {
                throw new IllegalArgumentException(type + " must not carry an offer ID");
            }
        }

        public static VoteChoice offer(long offerId) {
            return new VoteChoice(VoteOptionType.OFFER, offerId);
        }

        public static VoteChoice accept() {
            return new VoteChoice(VoteOptionType.ACCEPT, null);
        }

        public static VoteChoice reroll() {
            return new VoteChoice(VoteOptionType.REROLL, null);
        }
    }

    enum VoteOptionType {
        OFFER,
        ACCEPT,
        REROLL
    }

    private static void requireId(long value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " ID must be positive");
        }
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
    }
}
