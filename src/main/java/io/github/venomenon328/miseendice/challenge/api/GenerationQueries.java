package io.github.venomenon328.miseendice.challenge.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Purpose-built read API for persisted phase-9D attempts, batches, and replay. */
public interface GenerationQueries {

    Optional<AttemptView> findAttempt(long attemptId);

    Optional<BatchView> findBatch(long attemptId, int batchNumber);

    ReplayResult replay(long attemptId, int batchNumber);

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
            String resultSnapshotJson,
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
            List<RequirementView> requirements
    ) {
        public CandidateView {
            requirements = List.copyOf(requirements);
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

    enum ReplayStatus {
        MATCH,
        MISMATCH,
        UNSUPPORTED_VERSION,
        CONTEXT_SNAPSHOT_INVALID,
        NOT_FOUND,
        NOT_GENERATED
    }

    record ReplayResult(
            ReplayStatus status,
            String reasonCode,
            String storedFingerprint,
            String replayedFingerprint,
            List<String> storedCandidateSignatures,
            List<String> replayedCandidateSignatures
    ) {
        public ReplayResult {
            storedCandidateSignatures = List.copyOf(storedCandidateSignatures);
            replayedCandidateSignatures = List.copyOf(replayedCandidateSignatures);
        }
    }
}
