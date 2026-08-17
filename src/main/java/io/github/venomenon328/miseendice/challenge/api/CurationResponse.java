package io.github.venomenon328.miseendice.challenge.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Structured result returned by a future curator adapter; prose is deliberately absent. */
public record CurationResponse(
        String contractVersion,
        long attemptId,
        long roundId,
        long primaryBatchId,
        List<CandidateEvaluation> evaluations
) {
    public CurationResponse {
        if (contractVersion == null || contractVersion.isBlank() || attemptId <= 0 || roundId <= 0
                || primaryBatchId <= 0 || evaluations == null) {
            throw new IllegalArgumentException("Invalid curation response contract");
        }
        evaluations = List.copyOf(evaluations);
        if (evaluations.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Curation response evaluations must not contain null values");
        }
    }

    public record CandidateEvaluation(
            long candidateId,
            CurationModel.Evaluation evaluation,
            int rank,
            List<String> reasonCodes,
            Map<String, String> diagnostics
    ) {
        public CandidateEvaluation {
            if (candidateId <= 0 || evaluation == null || rank < 1 || reasonCodes == null) {
                throw new IllegalArgumentException("Invalid curation candidate evaluation");
            }
            reasonCodes = List.copyOf(reasonCodes);
            diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
        }
    }
}
