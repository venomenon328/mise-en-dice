package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirResult;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.SimilarityComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Public transport-neutral phase-9C2 API for one complete deterministic generation batch. */
public interface CandidateSetEngine {

    CandidateSetResult generate(PreparedGenerationAttempt preparedAttempt, int batchNumber);

    sealed interface CandidateSetResult permits GeneratedCandidateSet, ExhaustedCandidateSet {
        ReservoirResult reservoir();
        int batchNumber();
        long batchSeed();
        List<FallbackAttempt> fallbackAttempts();
        List<GeneratorReasonCode> diagnostics();
    }

    record GeneratedCandidateSet(
            GeneratedReservoir reservoir,
            int batchNumber,
            long batchSeed,
            FallbackLevel fallbackLevel,
            List<AcceptedProposal> candidates,
            SetEvaluation evaluation,
            String fingerprint,
            List<FallbackAttempt> fallbackAttempts,
            List<GeneratorReasonCode> diagnostics
    ) implements CandidateSetResult {
        public GeneratedCandidateSet {
            if (reservoir == null || fallbackLevel == null || evaluation == null
                    || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("A generated set requires its complete evaluation and fingerprint");
            }
            candidates = List.copyOf(candidates);
            if (candidates.size() != reservoir.context().configuration().candidateSetSize()
                    || candidates.stream().map(AcceptedProposal::canonicalSignature).distinct().count()
                    != candidates.size()) {
                throw new IllegalArgumentException("A generated set must contain twelve unique candidates");
            }
            fallbackAttempts = List.copyOf(fallbackAttempts);
            diagnostics = canonicalDiagnostics(diagnostics);
        }
    }

    record ExhaustedCandidateSet(
            ReservoirResult reservoir,
            int batchNumber,
            long batchSeed,
            List<FallbackAttempt> fallbackAttempts,
            List<GeneratorReasonCode> diagnostics
    ) implements CandidateSetResult {
        public ExhaustedCandidateSet {
            if (reservoir == null) {
                throw new IllegalArgumentException("An exhausted set requires reservoir diagnostics");
            }
            fallbackAttempts = List.copyOf(fallbackAttempts);
            diagnostics = canonicalDiagnostics(diagnostics);
            if (!diagnostics.contains(GeneratorReasonCode.GENERATION_EXHAUSTED)) {
                throw new IllegalArgumentException("Exhaustion diagnostics must contain GENERATION_EXHAUSTED");
            }
        }
    }

    record PairAssessment(
            int firstCandidateNumber,
            int secondCandidateNumber,
            Map<SimilarityComponent, ComponentSimilarity> components,
            Map<SimilarityComponent, BigDecimal> renormalizedWeights,
            BigDecimal totalSimilarity,
            List<GeneratorReasonCode> diagnostics
    ) {
        public PairAssessment {
            if (firstCandidateNumber < 1 || secondCandidateNumber <= firstCandidateNumber
                    || totalSimilarity == null || totalSimilarity.signum() < 0
                    || totalSimilarity.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Invalid candidate pair assessment");
            }
            components = Map.copyOf(components);
            renormalizedWeights = Map.copyOf(renormalizedWeights);
            diagnostics = canonicalDiagnostics(diagnostics);
        }
    }

    enum Comparability { COMPARABLE, NOT_COMPARABLE }

    record ComponentSimilarity(Comparability comparability, BigDecimal value) {
        public ComponentSimilarity {
            if (comparability == null || (comparability == Comparability.COMPARABLE) != (value != null)
                    || value != null && (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
                throw new IllegalArgumentException("Comparable similarity components require a value in [0,1]");
            }
        }

        public static ComponentSimilarity comparable(BigDecimal value) {
            return new ComponentSimilarity(Comparability.COMPARABLE, value);
        }

        public static ComponentSimilarity notComparable() {
            return new ComponentSimilarity(Comparability.NOT_COMPARABLE, null);
        }
    }

    record PairStatistics(BigDecimal mean, BigDecimal percentile95, BigDecimal maximum) {
    }

    record QuotaEvaluation<T>(Map<T, Integer> targets, Map<T, Integer> actual, Map<T, Integer> deviations) {
        public QuotaEvaluation {
            targets = Map.copyOf(targets);
            actual = Map.copyOf(actual);
            deviations = Map.copyOf(deviations);
        }
    }

    record SelectionDecision(
            int position,
            String canonicalSignature,
            BigDecimal quality,
            BigDecimal diversity,
            BigDecimal quotaFit,
            BigDecimal utility,
            BigDecimal minimumTopBandUtility,
            long selectionWeight
    ) {
    }

    record FallbackAttempt(
            FallbackLevel fallbackLevel,
            List<String> selectedSignatures,
            Map<GeneratorReasonCode, Long> rejectionsByReason,
            boolean completed
    ) {
        public FallbackAttempt {
            selectedSignatures = List.copyOf(selectedSignatures);
            LinkedHashMap<GeneratorReasonCode, Long> ordered = new LinkedHashMap<>();
            rejectionsByReason.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                    .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
            rejectionsByReason = Map.copyOf(ordered);
        }
    }

    record SetEvaluation(
            QuotaEvaluation<Integer> specificity,
            QuotaEvaluation<CandidateProfile> profiles,
            QuotaEvaluation<NoveltyBand> novelty,
            List<PairAssessment> pairs,
            PairStatistics pairStatistics,
            Map<String, Integer> randomConceptUsage,
            Map<String, Integer> informativeAncestorUsage,
            Map<CandidateProfile, Integer> profileUsage,
            int difficultCandidateCount,
            List<SelectionDecision> selectionDecisions,
            List<GeneratorReasonCode> reasonCodes
    ) {
        public SetEvaluation {
            pairs = List.copyOf(pairs);
            if (pairs.size() != 66) {
                throw new IllegalArgumentException("A twelve-candidate set requires exactly 66 pair assessments");
            }
            randomConceptUsage = Map.copyOf(randomConceptUsage);
            informativeAncestorUsage = Map.copyOf(informativeAncestorUsage);
            profileUsage = Map.copyOf(profileUsage);
            selectionDecisions = List.copyOf(selectionDecisions);
            reasonCodes = canonicalDiagnostics(reasonCodes);
        }
    }

    private static List<GeneratorReasonCode> canonicalDiagnostics(List<GeneratorReasonCode> source) {
        List<GeneratorReasonCode> ordered = new ArrayList<>(new LinkedHashSet<>(source));
        ordered.sort(Comparator.comparing(Enum::name));
        return List.copyOf(ordered);
    }
}
