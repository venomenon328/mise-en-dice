package io.github.venomenon328.miseendice.challenge.api;

import java.util.List;

/** Public commands for explicit Phase-10A curation decisions; this API performs no network calls or orchestration. */
public interface CurationCommands {

    RoundOutcome planRound(PlanRound command);

    CompletionOutcome completeRound(CompleteRound command);

    RoundOutcome recordTechnicalFailure(TechnicalFailure command);

    ExhaustedAttempt recordExhaustion(Exhaustion command);

    OfferSetOutcome createOfferSet(CreateOfferSet command);

    record PlanRound(
            long attemptId,
            int roundNumber,
            long primaryBatchId,
            CurationModel.RequestPurpose purpose,
            String curatorModel,
            String promptVersion,
            int openOfferSlots,
            List<CandidateParticipation> candidates
    ) {
        public PlanRound {
            if (attemptId <= 0 || roundNumber < 1 || roundNumber > 2 || primaryBatchId <= 0 || purpose == null
                    || curatorModel == null || curatorModel.isBlank() || promptVersion == null || promptVersion.isBlank()
                    || openOfferSlots < 1 || openOfferSlots > 3 || candidates == null || candidates.isEmpty()) {
                throw new IllegalArgumentException("Invalid planned curation round");
            }
            candidates = List.copyOf(candidates);
        }
    }

    record CandidateParticipation(
            long candidateId,
            CurationModel.Participation participation,
            Long sourceRoundCandidateId
    ) {
        public CandidateParticipation {
            if (candidateId <= 0 || participation == null
                    || (participation == CurationModel.Participation.NEW && sourceRoundCandidateId != null)
                    || (participation != CurationModel.Participation.NEW
                        && (sourceRoundCandidateId == null || sourceRoundCandidateId <= 0))) {
                throw new IllegalArgumentException("Invalid curation candidate participation");
            }
        }
    }

    record CompleteRound(long roundId, CurationResponse response) {
        public CompleteRound {
            if (roundId <= 0 || response == null) {
                throw new IllegalArgumentException("A round and response are required");
            }
        }
    }

    record TechnicalFailure(long roundId, String reasonCode, String detail) {
        public TechnicalFailure {
            if (roundId <= 0 || reasonCode == null || !reasonCode.matches("[A-Z][A-Z0-9_]{0,63}")) {
                throw new IllegalArgumentException("Technical failures require a stable reason code");
            }
            detail = detail == null ? null : detail.strip();
            if (detail != null && detail.length() > 1_000) {
                throw new IllegalArgumentException("Technical failure details are limited");
            }
        }
    }

    /** Explicit terminal decision for an attempt without an offer set; Phase 10A never decides this automatically. */
    record Exhaustion(long attemptId, String reasonCode, String detail) {
        public Exhaustion {
            if (attemptId <= 0 || reasonCode == null || !reasonCode.matches("[A-Z][A-Z0-9_]{0,63}")) {
                throw new IllegalArgumentException("Curation exhaustion requires a stable reason code");
            }
            detail = detail == null ? null : detail.strip();
            if (detail != null && detail.length() > 1_000) {
                throw new IllegalArgumentException("Curation exhaustion details are limited");
            }
        }
    }

    record CreateOfferSet(long attemptId, List<OfferSelection> offers, List<String> selectionReasonCodes) {
        public CreateOfferSet {
            if (attemptId <= 0 || offers == null || offers.isEmpty() || offers.size() > 3
                    || selectionReasonCodes == null || selectionReasonCodes.isEmpty()) {
                throw new IllegalArgumentException("An explicit ordered offer set and selection reasons are required");
            }
            offers = List.copyOf(offers);
            selectionReasonCodes = List.copyOf(selectionReasonCodes);
            selectionReasonCodes.forEach(code -> {
                if (code == null || !code.matches("[A-Z][A-Z0-9_]{0,63}")) {
                    throw new IllegalArgumentException("Selection reason codes must be stable uppercase identifiers");
                }
            });
        }
    }

    record OfferSelection(long candidateId, long curationRoundCandidateId) {
        public OfferSelection {
            if (candidateId <= 0 || curationRoundCandidateId <= 0) {
                throw new IllegalArgumentException("Offer selections require a candidate and its authoritative evaluation");
            }
        }
    }

    sealed interface RoundOutcome permits PlannedRound, FailedRound {
        long roundId();
        long attemptId();
    }

    record PlannedRound(long roundId, long attemptId, CurationRequest request) implements RoundOutcome {
    }

    record FailedRound(long roundId, long attemptId, CurationModel.RoundStatus status,
                       String reasonCode, String detail) implements RoundOutcome {
        public FailedRound {
            if (status != CurationModel.RoundStatus.TECHNICAL_ERROR
                    && status != CurationModel.RoundStatus.INVALID_RESPONSE) {
                throw new IllegalArgumentException("Only terminal curation failures use FailedRound");
            }
        }
    }

    record ExhaustedAttempt(long attemptId, String reasonCode, String detail) {
    }

    sealed interface CompletionOutcome permits CompletedRound, InvalidResponse {
        long roundId();
        long attemptId();
    }

    record CompletedRound(long roundId, long attemptId, CurationResponse response) implements CompletionOutcome {
    }

    /** Invalid structured output is persisted as an invalid response, never as a generator failure. */
    record InvalidResponse(long roundId, long attemptId, String reasonCode, String detail)
            implements CompletionOutcome {
    }

    sealed interface OfferSetOutcome permits CuratedOfferSet {
        long offerSetId();
        long attemptId();
    }

    record CuratedOfferSet(long offerSetId, long attemptId, int requestedOfferCount,
                           CurationModel.OfferSetStatus status) implements OfferSetOutcome {
    }
}
