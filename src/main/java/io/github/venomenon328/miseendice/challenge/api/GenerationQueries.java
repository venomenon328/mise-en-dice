package io.github.venomenon328.miseendice.challenge.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Purpose-built read API for persisted phase-9D attempts, contexts, batches, and replay. */
public interface GenerationQueries {
    Optional<AttemptView> findAttempt(long attemptId);
    Optional<ContextView> findContext(long attemptId);
    Optional<BatchView> findBatch(long attemptId, int batchNumber);
    ReplayResult replay(long attemptId, int batchNumber);

    enum NextAction { WAIT_OR_RECOVER, AWAIT_CURATION, NONE }

    record AttemptView(long sessionId, long attemptId, GeneratorModel.AttemptType attemptType, String status,
                       LocalDate effectiveDate, Integer seasonMonth, Long attemptSeed, String rngAlgorithm,
                       String generatorVersion, String configurationVersion, Integer canonicalPayloadVersion,
                       Instant createdAt, Instant completedAt, String failureReasonCode, String failureDetail,
                       NextAction nextAction, List<Integer> batchNumbers) {
        public AttemptView { batchNumbers = List.copyOf(batchNumbers); }
        public AttemptView withNextAction(NextAction value) {
            return new AttemptView(sessionId, attemptId, attemptType, status, effectiveDate, seasonMonth,
                    attemptSeed, rngAlgorithm, generatorVersion, configurationVersion, canonicalPayloadVersion,
                    createdAt, completedAt, failureReasonCode, failureDetail, value, batchNumbers);
        }
    }

    record ContextView(long attemptId, String configurationSnapshotJson, String catalogSnapshotJson,
                       String requestSnapshotJson, String visibleHistorySnapshotJson,
                       String preparedAttemptSnapshotJson, String contextFingerprint,
                       String configurationFingerprint, String catalogFingerprint,
                       String requestFingerprint, String historyFingerprint) { }

    record BatchView(long batchId, long attemptId, int batchNumber, boolean legacyMigrated, Long batchSeed,
                     String status, String fallbackLevel, String setFingerprint, String reservoirMetricsJson,
                     String fallbackAttemptsJson, String setEvaluationJson, String diagnosticsJson,
                     String resultSnapshotJson, List<CandidateView> candidates, Instant completedAt) {
        public BatchView { candidates = List.copyOf(candidates); }
    }

    record CandidateView(long candidateId, int candidateNumber, Long proposalOrdinal, String profile,
                         Integer targetSpecificity, String targetNoveltyBand, String actualNoveltyBand,
                         Integer knownNoveltyLoad, BigDecimal totalScore, BigDecimal dataConfidence,
                         String canonicalSignature, String componentScoresJson, String profileSlotAssignmentsJson,
                         String reasonCodesJson, String diagnosticsJson, List<RequirementView> requirements) {
        public CandidateView { requirements = List.copyOf(requirements); }
    }

    record RequirementView(int position, String source, Long ingredientConceptId, Long manualRequirementId,
                           String conceptCodeSnapshot, String displayTextSnapshot, String specificitySnapshot,
                           Integer noveltyLevelSnapshot, String conceptSnapshotJson,
                           String weightEvaluationSnapshotJson, String reasonCodesJson) { }

    enum ReplayStatus { MATCH, MISMATCH, UNSUPPORTED_VERSION, CONTEXT_SNAPSHOT_INVALID, NOT_FOUND, NOT_GENERATED }
    enum ReplayDifferenceType { SET_FINGERPRINT, CANDIDATE_SIGNATURE, CANDIDATE_TOTAL_SCORE,
        CANDIDATE_COMPONENT_SCORES, CANDIDATE_REASON_CODES, SET_EVALUATION }

    record ReplayDifference(ReplayDifferenceType type, String path, String storedValue, String replayedValue) {
        public ReplayDifference {
            if (type == null || path == null || path.isBlank()) {
                throw new IllegalArgumentException("Replay differences require a type and path");
            }
        }
    }

    record ReplayResult(ReplayStatus status, String reasonCode, String storedFingerprint,
                        String replayedFingerprint, List<String> storedCandidateSignatures,
                        List<String> replayedCandidateSignatures, ReplayDifference difference) {
        public ReplayResult {
            storedCandidateSignatures = List.copyOf(storedCandidateSignatures);
            replayedCandidateSignatures = List.copyOf(replayedCandidateSignatures);
            if (status == ReplayStatus.MISMATCH && difference == null) {
                difference = new ReplayDifference(ReplayDifferenceType.SET_FINGERPRINT, "setFingerprint",
                        limited(storedFingerprint), limited(replayedFingerprint));
            } else if (status != ReplayStatus.MISMATCH && difference != null) {
                throw new IllegalArgumentException("Only replay mismatches may contain a structured difference");
            }
        }
        public ReplayResult(ReplayStatus status, String reasonCode, String storedFingerprint,
                            String replayedFingerprint, List<String> storedCandidateSignatures,
                            List<String> replayedCandidateSignatures) {
            this(status, reasonCode, storedFingerprint, replayedFingerprint,
                    storedCandidateSignatures, replayedCandidateSignatures, null);
        }
        private static String limited(String value) {
            return value == null || value.length() <= 500 ? value : value.substring(0, 497) + "...";
        }
    }
}
