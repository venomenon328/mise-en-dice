package io.github.venomenon328.miseendice.challenge.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateRestriction;

/** Immutable persisted request handed to a future external curator adapter. */
public record CurationRequest(
        String contractVersion,
        String promptVersion,
        long attemptId,
        long roundId,
        long primaryBatchId,
        int requestedOfferCount,
        int openOfferSlots,
        AttemptExclusion attemptExclusion,
        List<Candidate> candidates
) {
    public CurationRequest {
        if (!CurationModel.supportedContract(contractVersion) || promptVersion == null || promptVersion.isBlank()
                || attemptId <= 0 || roundId <= 0
                || primaryBatchId <= 0 || requestedOfferCount < 1 || requestedOfferCount > 3
                || openOfferSlots < 1 || openOfferSlots > requestedOfferCount || candidates == null
                || candidates.isEmpty()) {
            throw new IllegalArgumentException("Invalid curation request contract");
        }
        if (CurationModel.CONTRACT_VERSION_V1.equals(contractVersion) && attemptExclusion == null) {
            throw new IllegalArgumentException("Curation contract V1 requires its historic attempt exclusion snapshot");
        }
        if (CurationModel.CONTRACT_VERSION_V2.equals(contractVersion)
                && (attemptExclusion != null || candidates.stream().anyMatch(candidate -> candidate.snapshot().restriction() == null))) {
            throw new IllegalArgumentException("Curation contract V2 carries exactly one restriction snapshot per candidate");
        }
        promptVersion = promptVersion.strip();
        candidates = List.copyOf(candidates);
        if (candidates.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Curation request candidates must not contain null values");
        }
    }

    /** Immutable attempt-wide exclusion decision relevant to every candidate in this curation request. */
    public record AttemptExclusion(Long exclusionRuleId, String exclusionTextSnapshot) {
        public AttemptExclusion {
            if ((exclusionRuleId == null) != (exclusionTextSnapshot == null)
                    || (exclusionRuleId != null && (exclusionRuleId <= 0 || exclusionTextSnapshot.isBlank()))) {
                throw new IllegalArgumentException("Invalid attempt exclusion snapshot");
            }
            if (exclusionTextSnapshot != null) {
                exclusionTextSnapshot = exclusionTextSnapshot.strip();
            }
        }
    }

    public record Candidate(
            long candidateId,
            int requestPosition,
            CurationModel.Participation participation,
            Long sourceRoundCandidateId,
            CandidateSnapshot snapshot
    ) {
        public Candidate {
            if (candidateId <= 0 || requestPosition < 1 || participation == null || snapshot == null
                    || (participation == CurationModel.Participation.NEW && sourceRoundCandidateId != null)
                    || (participation != CurationModel.Participation.NEW && sourceRoundCandidateId == null)) {
                throw new IllegalArgumentException("Invalid curation request candidate");
            }
        }
    }

    /** Complete persisted generator-side candidate snapshot; it is not a recipe prompt. */
    public record CandidateSnapshot(
            int candidateNumber,
            String profile,
            Integer targetSpecificity,
            String targetNoveltyBand,
            String actualNoveltyBand,
            Integer knownNoveltyLoad,
            BigDecimal totalScore,
            BigDecimal dataConfidence,
            String canonicalSignature,
            String componentScoresJson,
            String generatorReasonCodesJson,
            String generatorDiagnosticsJson,
            List<RequirementSnapshot> requirements,
            CandidateRestriction restriction
    ) {
        public CandidateSnapshot {
            if (candidateNumber < 1 || profile == null || targetSpecificity == null || targetNoveltyBand == null
                    || actualNoveltyBand == null || knownNoveltyLoad == null || totalScore == null
                    || dataConfidence == null || canonicalSignature == null || requirements == null
                    || requirements.size() != 4) {
                throw new IllegalArgumentException("A curation candidate requires its complete generator snapshot");
            }
            requirements = List.copyOf(requirements);
        }

        /** Compatibility constructor for immutable Curation Contract V1 payloads. */
        public CandidateSnapshot(int candidateNumber, String profile, Integer targetSpecificity,
                                 String targetNoveltyBand, String actualNoveltyBand, Integer knownNoveltyLoad,
                                 BigDecimal totalScore, BigDecimal dataConfidence, String canonicalSignature,
                                 String componentScoresJson, String generatorReasonCodesJson,
                                 String generatorDiagnosticsJson, List<RequirementSnapshot> requirements) {
            this(candidateNumber, profile, targetSpecificity, targetNoveltyBand, actualNoveltyBand,
                    knownNoveltyLoad, totalScore, dataConfidence, canonicalSignature, componentScoresJson,
                    generatorReasonCodesJson, generatorDiagnosticsJson, requirements, null);
        }
    }

    public record RequirementSnapshot(
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
            String generatorReasonCodesJson
    ) {
        public RequirementSnapshot {
            if (position < 1 || position > 4 || source == null || displayTextSnapshot == null
                    || displayTextSnapshot.isBlank()) {
                throw new IllegalArgumentException("Invalid persisted requirement snapshot");
            }
        }
    }
}
