package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Read-only Curation and Offer projections, including raw persisted contract payloads for audit and replay. */
public interface CurationQueries {

    Optional<AttemptView> findAttempt(long attemptId);

    Optional<RoundView> findRound(long attemptId, int roundNumber);

    Optional<RoundView> findRoundById(long roundId);

    Optional<OfferSetView> findOfferSet(long attemptId);

    record AttemptView(
            long attemptId,
            long sessionId,
            int requestedOfferCount,
            String curationStatus,
            String terminalReasonCode,
            String terminalDetail,
            List<RoundSummary> rounds,
            OfferSetView offerSet
    ) {
        public AttemptView {
            rounds = List.copyOf(rounds);
        }
    }

    record RoundSummary(long roundId, int roundNumber, CurationModel.RoundStatus status,
                        CurationModel.RequestPurpose purpose, long primaryBatchId, Instant completedAt) {
    }

    record RoundView(
            long roundId,
            long attemptId,
            int roundNumber,
            long primaryBatchId,
            String curatorModel,
            String promptVersion,
            CurationModel.RequestPurpose purpose,
            CurationModel.RoundStatus status,
            CurationRequest request,
            String requestPayloadJson,
            String responsePayloadJson,
            String invalidResponseOriginalPayload,
            String terminalReasonCode,
            String terminalDetail,
            Instant createdAt,
            Instant completedAt,
            List<RoundCandidateView> candidates,
            ProviderAuditView providerAudit
    ) {
        public RoundView {
            candidates = List.copyOf(candidates);
        }
    }

    record ProviderAuditView(
            String provider,
            String dispatchStatus,
            Instant dispatchClaimedAt,
            Instant recoveryDeadlineAt,
            String requestPayload,
            String responsePayload,
            String responseId,
            String usageSnapshotJson,
            Integer httpStatus,
            String providerErrorCode,
            String diagnostic,
            Boolean retryable,
            Instant resultRecordedAt
    ) {
    }

    record RoundCandidateView(
            long roundCandidateId,
            long candidateId,
            int requestPosition,
            CurationModel.Participation participation,
            Long sourceRoundCandidateId,
            CurationModel.Evaluation evaluation,
            Integer rank,
            List<String> reasonCodes,
            String diagnosticsJson,
            CurationRequest.CandidateSnapshot snapshot
    ) {
        public RoundCandidateView {
            reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        }
    }

    record OfferSetView(
            long offerSetId,
            long attemptId,
            int requestedOfferCount,
            CurationModel.OfferSetStatus status,
            String selectionPathJson,
            Instant curatedAt,
            Instant presentedAt,
            Instant decidedAt,
            List<OfferView> offers
    ) {
        public OfferSetView {
            offers = List.copyOf(offers);
        }
    }

    record OfferView(int position, long candidateId, long curationRoundCandidateId,
                     CurationModel.Evaluation evaluation, Integer rank,
                     CurationRequest.CandidateSnapshot candidate) {
    }
}
