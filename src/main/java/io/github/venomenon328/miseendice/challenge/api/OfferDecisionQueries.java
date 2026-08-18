package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Read-only audit projections for the Phase-11A offer-decision lifecycle. */
public interface OfferDecisionQueries {

    Optional<OfferSetView> findOfferSet(long offerSetId);

    Optional<SessionDecisionView> findSession(long sessionId);

    Optional<RerollExposureView> findRerollExposure(long offerSetId);

    record OfferSetView(
            long sessionId,
            long attemptId,
            long offerSetId,
            int requestedOfferCount,
            CurationModel.OfferSetStatus status,
            Instant curatedAt,
            Instant presentedAt,
            Instant decidedAt,
            ChallengeView confirmedChallenge,
            List<OfferView> offers
    ) {
        public OfferSetView {
            offers = List.copyOf(offers);
        }
    }

    record OfferView(long offerId, int position, long candidateId,
                     List<CurationRequest.RequirementSnapshot> requirements,
                     CandidateProposalEngine.CandidateRestriction restriction) {
        public OfferView {
            requirements = List.copyOf(requirements);
            restriction = restriction == null ? CandidateProposalEngine.CandidateRestriction.none() : restriction;
        }

        public OfferView(long offerId, int position, long candidateId,
                         List<CurationRequest.RequirementSnapshot> requirements) {
            this(offerId, position, candidateId, requirements, CandidateProposalEngine.CandidateRestriction.none());
        }
    }

    record ChallengeView(long challengeId, long offerId, long candidateId, Instant shownAt, String status) {
    }

    record SessionDecisionView(
            long sessionId,
            int requestedOfferCount,
            Long pendingOfferSetId,
            Long confirmedOfferSetId,
            Long rerolledOfferSetId,
            Long confirmedChallengeId,
            Long rerollAttemptId,
            boolean rerollConsumed
    ) {
    }

    record RerollExposureView(long exposureId, long sessionId, long offerSetId, Instant exposedAt,
                              List<ExposedRequirementView> requirements,
                              List<ExposedRestrictionView> restrictions) {
        public RerollExposureView {
            requirements = List.copyOf(requirements);
            restrictions = List.copyOf(restrictions);
        }

        public RerollExposureView(long exposureId, long sessionId, long offerSetId, Instant exposedAt,
                                  List<ExposedRequirementView> requirements) {
            this(exposureId, sessionId, offerSetId, exposedAt, requirements, List.of());
        }
    }

    record ExposedRequirementView(long offerId, long candidateId, int position, String source,
                                  Long ingredientConceptId, String conceptCodeSnapshot,
                                  String displayTextSnapshot) {
    }

    record ExposedRestrictionView(long offerId, long candidateId,
                                  CandidateProposalEngine.CandidateRestriction restriction) {
        public ExposedRestrictionView {
            restriction = restriction == null ? CandidateProposalEngine.CandidateRestriction.none() : restriction;
        }
    }
}
