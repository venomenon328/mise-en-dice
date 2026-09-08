package io.github.venomenon328.miseendice.challenge.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Purpose-built read API for persisted phase-9D attempts, contexts, and batches. */
public interface GenerationQueries {

    Optional<AttemptView> findAttempt(long attemptId);

    Optional<ContextView> findContext(long attemptId);

    Optional<BatchView> findBatch(long attemptId, int batchNumber);

    enum NextAction {
        WAIT_OR_RECOVER,
        AWAIT_CURATION,
        NONE
    }

    record AttemptView(
            long sessionId,
            long attemptId,
            GeneratorModel.AttemptType attemptType,
            String status,
            LocalDate effectiveDate,
            Integer seasonMonth,
            Long attemptSeed,
            String rngAlgorithm,
            String generatorVersion,
            String configurationVersion,
            Integer canonicalPayloadVersion,
            Instant createdAt,
            Instant completedAt,
            String failureReasonCode,
            String failureDetail,
            NextAction nextAction,
            List<Integer> batchNumbers
    ) {
        public AttemptView {
            batchNumbers = List.copyOf(batchNumbers);
        }

        public AttemptView withNextAction(NextAction value) {
            return new AttemptView(sessionId, attemptId, attemptType, status, effectiveDate, seasonMonth,
                    attemptSeed, rngAlgorithm, generatorVersion, configurationVersion, canonicalPayloadVersion,
                    createdAt, completedAt, failureReasonCode, failureDetail, value, batchNumbers);
        }
    }

    record ContextView(
            long attemptId,
            String configurationSnapshotJson,
            String catalogSnapshotJson,
            String requestSnapshotJson,
            String visibleHistorySnapshotJson,
            String preparedAttemptSnapshotJson,
            String contextFingerprint,
            String configurationFingerprint,
            String catalogFingerprint,
            String requestFingerprint,
            String historyFingerprint
    ) {
    }

    record BatchView(
            long batchId,
            long attemptId,
            int batchNumber,
            boolean legacyMigrated,
            Long batchSeed,
            String status,
            String fallbackLevel,
            String setFingerprint,
            String reservoirMetricsJson,
            String fallbackAttemptsJson,
            String setEvaluationJson,
            String diagnosticsJson,
            List<CandidateView> candidates,
            Instant completedAt
    ) {
        public BatchView {
            candidates = List.copyOf(candidates);
        }
    }

    record CandidateView(
            long candidateId,
            int candidateNumber,
            Long proposalOrdinal,
            String profile,
            Integer targetSpecificity,
            String targetNoveltyBand,
            String actualNoveltyBand,
            Integer knownNoveltyLoad,
            BigDecimal totalScore,
            BigDecimal dataConfidence,
            String canonicalSignature,
            String componentScoresJson,
            String profileSlotAssignmentsJson,
            String reasonCodesJson,
            String diagnosticsJson,
            List<RequirementView> requirements,
            CandidateProposalEngine.CandidateRestriction restriction
    ) {
        public CandidateView {
            requirements = List.copyOf(requirements);
            restriction = restriction == null ? CandidateProposalEngine.CandidateRestriction.none() : restriction;
        }

        public CandidateView(long candidateId, int candidateNumber, Long proposalOrdinal, String profile,
                             Integer targetSpecificity, String targetNoveltyBand, String actualNoveltyBand,
                             Integer knownNoveltyLoad, BigDecimal totalScore, BigDecimal dataConfidence,
                             String canonicalSignature, String componentScoresJson, String profileSlotAssignmentsJson,
                             String reasonCodesJson, String diagnosticsJson, List<RequirementView> requirements) {
            this(candidateId, candidateNumber, proposalOrdinal, profile, targetSpecificity, targetNoveltyBand,
                    actualNoveltyBand, knownNoveltyLoad, totalScore, dataConfidence, canonicalSignature,
                    componentScoresJson, profileSlotAssignmentsJson, reasonCodesJson, diagnosticsJson,
                    requirements, CandidateProposalEngine.CandidateRestriction.none());
        }
    }

    record RequirementView(
            int position,
            String source,
            Long ingredientConceptId,
            Long manualRequirementId,
            String conceptCodeSnapshot,
            String displayTextSnapshot,
            String specificitySnapshot,
            Integer noveltyLevelSnapshot,
            String conceptSnapshotJson,
            String weightEvaluationSnapshotJson,
            String reasonCodesJson
    ) {
    }

}
