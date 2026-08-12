package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Public phase-9C1 API for attempt preparation, batch contexts, and bounded reservoirs. */
public interface CandidateReservoirEngine {

    PreparedGenerationAttempt prepare(GenerationAttemptRequest request);

    GenerationContext contextForBatch(PreparedGenerationAttempt preparedAttempt, int batchNumber);

    ReservoirResult generate(PreparedGenerationAttempt preparedAttempt, int batchNumber);

    sealed interface ReservoirResult permits GeneratedReservoir, ExhaustedReservoir {
        PreparedGenerationAttempt preparedAttempt();
        GenerationContext context();
        GenerationPlan plan();
        ReservoirMetrics metrics();
        ReservoirSizeClass sizeClass();
        List<AcceptedProposal> candidates();
        List<GeneratorReasonCode> diagnostics();
    }

    record GeneratedReservoir(
            PreparedGenerationAttempt preparedAttempt,
            GenerationContext context,
            GenerationPlan plan,
            ReservoirMetrics metrics,
            ReservoirSizeClass sizeClass,
            List<AcceptedProposal> candidates,
            List<GeneratorReasonCode> diagnostics
    ) implements ReservoirResult {
        public GeneratedReservoir {
            if (sizeClass == ReservoirSizeClass.INSUFFICIENT) {
                throw new IllegalArgumentException("A generated reservoir must be large enough for a later set");
            }
            candidates = canonicalCandidates(candidates);
            diagnostics = canonicalDiagnostics(diagnostics);
        }
    }

    record ExhaustedReservoir(
            PreparedGenerationAttempt preparedAttempt,
            GenerationContext context,
            GenerationPlan plan,
            ReservoirMetrics metrics,
            ReservoirSizeClass sizeClass,
            List<AcceptedProposal> candidates,
            List<GeneratorReasonCode> diagnostics
    ) implements ReservoirResult {
        public ExhaustedReservoir {
            if (sizeClass != ReservoirSizeClass.INSUFFICIENT) {
                throw new IllegalArgumentException("An exhausted reservoir must contain fewer than twelve candidates");
            }
            candidates = canonicalCandidates(candidates);
            diagnostics = canonicalDiagnostics(diagnostics);
            if (!diagnostics.contains(GeneratorReasonCode.GENERATION_EXHAUSTED)) {
                throw new IllegalArgumentException("Exhaustion diagnostics must contain GENERATION_EXHAUSTED");
            }
        }
    }

    record ReservoirMetrics(
            int proposalAttempts,
            int acceptedProposalHits,
            int hardRejectedProposalHits,
            int duplicateHits,
            int uniqueAcceptedCandidates,
            Map<GeneratorReasonCode, Long> hardRejectionsByReason
    ) {
        public ReservoirMetrics {
            if (proposalAttempts < 0 || acceptedProposalHits < 0 || hardRejectedProposalHits < 0
                    || duplicateHits < 0 || uniqueAcceptedCandidates < 0
                    || proposalAttempts != acceptedProposalHits + hardRejectedProposalHits
                    || acceptedProposalHits != duplicateHits + uniqueAcceptedCandidates) {
                throw new IllegalArgumentException("Reservoir metrics are inconsistent");
            }
            LinkedHashMap<GeneratorReasonCode, Long> ordered = new LinkedHashMap<>();
            hardRejectionsByReason.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                    .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
            hardRejectionsByReason = Collections.unmodifiableMap(ordered);
        }
    }

    /** Size bands are diagnostic only; phase 9C2 decides which soft fallback starts later. */
    enum ReservoirSizeClass {
        INSUFFICIENT,
        SMALL,
        MEDIUM,
        LARGE
    }

    private static List<AcceptedProposal> canonicalCandidates(List<AcceptedProposal> source) {
        return source.stream().sorted(Comparator.comparing(AcceptedProposal::canonicalSignature)).toList();
    }

    private static List<GeneratorReasonCode> canonicalDiagnostics(List<GeneratorReasonCode> source) {
        List<GeneratorReasonCode> ordered = new ArrayList<>(new LinkedHashSet<>(source));
        ordered.sort(Comparator.comparing(Enum::name));
        return List.copyOf(ordered);
    }
}
