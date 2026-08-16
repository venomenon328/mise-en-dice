package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.ReservoirResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.FallbackAttempt;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairAssessment;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairStatistics;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.QuotaEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.SelectionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.SetEvaluation;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.FallbackConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import tools.jackson.databind.ObjectMapper;

final class DefaultCandidateSetEngine implements CandidateSetEngine {
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal ONE = scaled(BigDecimal.ONE);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);

    private final CandidateReservoirEngine reservoirEngine;
    private final CandidateSimilarityCalculator similarity = new CandidateSimilarityCalculator();
    private final CanonicalSetFingerprint fingerprints;

    DefaultCandidateSetEngine(CandidateReservoirEngine reservoirEngine, ObjectMapper objectMapper) {
        this.reservoirEngine = reservoirEngine;
        this.fingerprints = new CanonicalSetFingerprint(objectMapper);
    }

    @Override
    public CandidateSetResult generate(PreparedGenerationAttempt preparedAttempt, int batchNumber) {
        ReservoirResult reservoir = reservoirEngine.generate(preparedAttempt, batchNumber);
        GeneratorConfiguration configuration = reservoir.context().configuration();
        long batchSeed = SeedDerivation.derive(configuration.generatorVersion(), reservoir.context().attemptSeed(),
                SeedDerivation.batchScope(batchNumber), SeedDerivation.Purpose.BATCH_ROOT, 0);
        List<GeneratorReasonCode> diagnostics = new ArrayList<>(reservoir.diagnostics());
        if (!(reservoir instanceof GeneratedReservoir generated)) {
            diagnostics.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
            return new ExhaustedCandidateSet(reservoir, batchNumber, batchSeed, List.of(), diagnostics);
        }

        List<AcceptedProposal> canonicalReservoir = generated.candidates().stream()
                .sorted(Comparator.comparing(AcceptedProposal::canonicalSignature)).toList();
        Map<PairKey, BigDecimal> pairCache = new HashMap<>();
        List<FallbackAttempt> attempts = new ArrayList<>();
        FallbackLevel start = startingFallback(canonicalReservoir.size(), configuration);
        for (FallbackLevel level : FallbackLevel.values()) {
            if (level.ordinal() < start.ordinal()) {
                continue;
            }
            SelectionRun run = select(level, canonicalReservoir, generated.plan(), configuration,
                    generated.context().attemptSeed(), batchNumber, generated.context().catalog(), pairCache);
            attempts.add(new FallbackAttempt(level,
                    run.selected().stream().map(AcceptedProposal::canonicalSignature).toList(),
                    run.rejections(), run.complete(configuration.candidateSetSize())));
            if (!run.complete(configuration.candidateSetSize())) {
                continue;
            }
            if (level == FallbackLevel.RELAXED_1) {
                diagnostics.add(GeneratorReasonCode.SOFT_FALLBACK_RELAXED_1);
            } else if (level == FallbackLevel.RELAXED_2) {
                diagnostics.add(GeneratorReasonCode.SOFT_FALLBACK_RELAXED_2);
            }
            SetEvaluation evaluation = evaluate(run, generated.plan(), generated.context().catalog(), configuration);
            String fingerprint = fingerprints.fingerprint(generated, batchNumber, batchSeed, level,
                    run.selected(), evaluation, attempts, diagnostics);
            return new GeneratedCandidateSet(generated, batchNumber, batchSeed, level, run.selected(), evaluation,
                    fingerprint, attempts, diagnostics);
        }
        diagnostics.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
        return new ExhaustedCandidateSet(generated, batchNumber, batchSeed, attempts, diagnostics);
    }

    private SelectionRun select(
            FallbackLevel level,
            List<AcceptedProposal> reservoir,
            GenerationPlan plan,
            GeneratorConfiguration configuration,
            long attemptSeed,
            int batchNumber,
            io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot catalog,
            Map<PairKey, BigDecimal> pairCache
    ) {
        FallbackConfiguration fallback = configuration.fallbacks().get(level);
        List<AcceptedProposal> selected = new ArrayList<>();
        List<SelectionDecision> decisions = new ArrayList<>();
        EnumMap<GeneratorReasonCode, Long> rejections = new EnumMap<>(GeneratorReasonCode.class);
        while (selected.size() < configuration.candidateSetSize()) {
            List<CandidateUtility> eligible = new ArrayList<>();
            for (AcceptedProposal candidate : reservoir) {
                if (selected.stream().anyMatch(existing -> existing.canonicalSignature()
                        .equals(candidate.canonicalSignature()))) {
                    continue;
                }
                Set<GeneratorReasonCode> reasons = admissibilityReasons(candidate, selected, reservoir, plan,
                        configuration, fallback, catalog, pairCache);
                reasons.forEach(reason -> rejections.merge(reason, 1L, Long::sum));
                if (reasons.isEmpty()) {
                    eligible.add(utility(candidate, selected, plan, configuration, catalog, pairCache));
                }
            }
            if (eligible.isEmpty()) {
                break;
            }
            eligible.sort(Comparator.comparing(item -> item.candidate().canonicalSignature()));
            BigDecimal maximum = eligible.stream().map(CandidateUtility::utility).max(BigDecimal::compareTo)
                    .orElseThrow();
            List<CandidateUtility> topBand = eligible.stream()
                    .filter(item -> scaled(maximum.subtract(item.utility()))
                            .compareTo(configuration.selection().topBandWidth()) <= 0)
                    .toList();
            BigDecimal minimum = topBand.stream().map(CandidateUtility::utility).min(BigDecimal::compareTo)
                    .orElseThrow();
            List<WeightedUtility> weighted = topBand.stream()
                    .map(item -> new WeightedUtility(item, selectionWeight(item.utility(), minimum, configuration)))
                    .toList();
            long totalWeight = 0;
            try {
                for (WeightedUtility item : weighted) {
                    totalWeight = Math.addExact(totalWeight, item.weight());
                }
            } catch (ArithmeticException exception) {
                throw weightOverflow("Top-band selection weight sum overflowed", exception);
            }
            int position = selected.size() + 1;
            long selectionSeed = SeedDerivation.deriveSelection(configuration.generatorVersion(), attemptSeed,
                    batchNumber, level, position);
            long ticket = new SplitMix64(selectionSeed).nextLong(totalWeight);
            WeightedUtility chosen = null;
            for (WeightedUtility item : weighted) {
                if (ticket < item.weight()) {
                    chosen = item;
                    break;
                }
                ticket -= item.weight();
            }
            if (chosen == null) {
                throw new IllegalStateException("Weighted top-band selection did not resolve a candidate");
            }
            selected.add(chosen.utility().candidate());
            CandidateUtility values = chosen.utility();
            decisions.add(new SelectionDecision(position, values.candidate().canonicalSignature(), values.quality(),
                    values.diversity(), values.quotaFit(), values.utility(), minimum, chosen.weight()));
        }
        return new SelectionRun(level, List.copyOf(selected), List.copyOf(decisions), Map.copyOf(rejections));
    }

    private Set<GeneratorReasonCode> admissibilityReasons(
            AcceptedProposal candidate,
            List<AcceptedProposal> selected,
            List<AcceptedProposal> reservoir,
            GenerationPlan plan,
            GeneratorConfiguration configuration,
            FallbackConfiguration fallback,
            io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot catalog,
            Map<PairKey, BigDecimal> pairCache
    ) {
        EnumSet<GeneratorReasonCode> reasons = EnumSet.noneOf(GeneratorReasonCode.class);
        if (candidate.evaluation().totalScore().compareTo(BigDecimal.valueOf(fallback.minimumScore())) < 0) {
            reasons.add(GeneratorReasonCode.CANDIDATE_SCORE_MINIMUM);
        }
        if (wouldExceedUsage(candidate, selected, DefaultCandidateSetEngine::randomConcepts, fallback.conceptCap())) {
            reasons.add(GeneratorReasonCode.CONCEPT_SET_CAP);
        }
        if (wouldExceedUsage(candidate, selected,
                item -> similarity.informativeAncestors(item, catalog, configuration), fallback.ancestorCap())) {
            reasons.add(GeneratorReasonCode.ANCESTOR_SET_CAP);
        }
        long sameProfile = selected.stream().filter(item -> item.profile() == candidate.profile()).count();
        if (sameProfile + 1 > fallback.profileCap()) {
            reasons.add(GeneratorReasonCode.PROFILE_SET_CAP);
        }
        long difficult = selected.stream().filter(DefaultCandidateSetEngine::difficult).count();
        if (difficult + (difficult(candidate) ? 1 : 0) > fallback.difficultCandidateCap()) {
            reasons.add(GeneratorReasonCode.DIFFICULT_SET_CAP);
        }
        if (selected.stream().map(item -> pairSimilarity(candidate, item, catalog, configuration, pairCache))
                .anyMatch(value -> value.compareTo(fallback.maximumPairSimilarity()) > 0)) {
            reasons.add(GeneratorReasonCode.PAIR_SIMILARITY_LIMIT);
        }
        if (!quotaFeasible(candidate, selected, reservoir, plan.specificity().setTargets(), fallback,
                DefaultCandidateSetEngine::specificityCount)) {
            reasons.add(GeneratorReasonCode.SPECIFICITY_TARGET_DEVIATION);
        }
        if (!quotaFeasible(candidate, selected, reservoir, plan.profiles().setTargets(), fallback,
                AcceptedProposal::profile)) {
            reasons.add(GeneratorReasonCode.PROFILE_TARGET_DEVIATION);
        }
        if (recoveryForbidsAdventurous(candidate, plan)) {
            reasons.add(GeneratorReasonCode.NOVELTY_TARGET_DEVIATION);
        }
        return reasons;
    }

    private static boolean recoveryForbidsAdventurous(AcceptedProposal candidate, GenerationPlan plan) {
        return plan.novelty().setTargets().getOrDefault(NoveltyBand.ADVENTUROUS, 0) == 0
                && candidate.evaluation().actualNoveltyBand() == NoveltyBand.ADVENTUROUS
                && !candidate.evaluation().reasonCodes().contains(GeneratorReasonCode.MANUAL_NOVELTY_FORCED);
    }

    private <T> boolean quotaFeasible(
            AcceptedProposal candidate,
            List<AcceptedProposal> selected,
            List<AcceptedProposal> reservoir,
            Map<T, Integer> targets,
            FallbackConfiguration fallback,
            Function<AcceptedProposal, T> category
    ) {
        Map<T, Integer> counts = counts(selected, targets.keySet(), category);
        int setSize = targets.values().stream().mapToInt(Integer::intValue).sum();
        T candidateCategory = category.apply(candidate);
        int newCount = counts.getOrDefault(candidateCategory, 0) + 1;
        int upper = Math.min(setSize, targets.getOrDefault(candidateCategory, 0) + fallback.quotaDeviation());
        if (newCount > upper) {
            return false;
        }
        counts.put(candidateCategory, newCount);
        int remainingSlots = setSize - selected.size() - 1;
        int totalRequired = 0;
        Set<String> excluded = new HashSet<>(selected.stream().map(AcceptedProposal::canonicalSignature).toList());
        excluded.add(candidate.canonicalSignature());
        List<AcceptedProposal> remaining = reservoir.stream()
                .filter(item -> !excluded.contains(item.canonicalSignature()))
                .filter(item -> item.evaluation().totalScore()
                        .compareTo(BigDecimal.valueOf(fallback.minimumScore())) >= 0)
                .toList();
        for (Map.Entry<T, Integer> target : targets.entrySet()) {
            int lower = Math.max(0, target.getValue() - fallback.quotaDeviation());
            int needed = Math.max(lower - counts.getOrDefault(target.getKey(), 0), 0);
            totalRequired += needed;
            long available = remaining.stream().filter(item -> category.apply(item).equals(target.getKey())).count();
            if (available < needed) {
                return false;
            }
        }
        return totalRequired <= remainingSlots;
    }

    private CandidateUtility utility(
            AcceptedProposal candidate,
            List<AcceptedProposal> selected,
            GenerationPlan plan,
            GeneratorConfiguration configuration,
            io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot catalog,
            Map<PairKey, BigDecimal> pairCache
    ) {
        BigDecimal quality = ratio(candidate.evaluation().totalScore(), BigDecimal.valueOf(100));
        BigDecimal maximumSimilarity = selected.stream()
                .map(item -> pairSimilarity(candidate, item, catalog, configuration, pairCache))
                .max(BigDecimal::compareTo).orElse(scaled(BigDecimal.ZERO));
        BigDecimal diversity = selected.isEmpty() ? ONE : scaled(ONE.subtract(maximumSimilarity));
        BigDecimal quotaFit = quotaFit(candidate, selected, plan);
        BigDecimal value = scaled(
                scaled(quality.multiply(configuration.selection().qualityWeight()))
                        .add(scaled(diversity.multiply(configuration.selection().diversityWeight())))
                        .add(scaled(quotaFit.multiply(configuration.selection().quotaWeight()))));
        return new CandidateUtility(candidate, quality, diversity, quotaFit, value);
    }

    private BigDecimal quotaFit(AcceptedProposal candidate, List<AcceptedProposal> selected, GenerationPlan plan) {
        BigDecimal specificity = quotaContribution(candidate, selected, plan.specificity().setTargets(),
                DefaultCandidateSetEngine::specificityCount);
        BigDecimal profile = quotaContribution(candidate, selected, plan.profiles().setTargets(),
                AcceptedProposal::profile);
        BigDecimal novelty = quotaContribution(candidate, selected, plan.novelty().setTargets(),
                item -> item.evaluation().actualNoveltyBand());
        return ratio(scaled(specificity.add(profile).add(novelty)), THREE);
    }

    private <T> BigDecimal quotaContribution(
            AcceptedProposal candidate,
            List<AcceptedProposal> selected,
            Map<T, Integer> targets,
            Function<AcceptedProposal, T> category
    ) {
        Map<T, Integer> counts = counts(selected, targets.keySet(), category);
        int maximumDeficit = targets.entrySet().stream()
                .mapToInt(entry -> Math.max(entry.getValue() - counts.getOrDefault(entry.getKey(), 0), 0))
                .max().orElse(0);
        if (maximumDeficit == 0) {
            return scaled(BigDecimal.ZERO);
        }
        int candidateDeficit = Math.max(targets.getOrDefault(category.apply(candidate), 0)
                - counts.getOrDefault(category.apply(candidate), 0), 0);
        return ratio(BigDecimal.valueOf(candidateDeficit), BigDecimal.valueOf(maximumDeficit));
    }

    private SetEvaluation evaluate(
            SelectionRun run,
            GenerationPlan plan,
            io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot catalog,
            GeneratorConfiguration configuration
    ) {
        List<AcceptedProposal> selected = run.selected();
        FallbackConfiguration fallback = configuration.fallbacks().get(run.level());
        List<PairAssessment> pairs = new ArrayList<>();
        List<BigDecimal> similarities = new ArrayList<>();
        List<GeneratorReasonCode> reasons = new ArrayList<>();
        for (int first = 0; first < selected.size(); first++) {
            for (int second = first + 1; second < selected.size(); second++) {
                PairAssessment pair = similarity.assess(first + 1, selected.get(first), second + 1,
                        selected.get(second), catalog, configuration, fallback.maximumPairSimilarity());
                pairs.add(pair);
                similarities.add(pair.totalSimilarity());
                reasons.addAll(pair.diagnostics());
            }
        }
        similarities.sort(BigDecimal::compareTo);
        BigDecimal sum = similarities.stream().reduce(scaled(BigDecimal.ZERO),
                (left, right) -> scaled(left.add(right)));
        BigDecimal mean = ratio(sum, BigDecimal.valueOf(similarities.size()));
        int percentileIndex = (int) Math.ceil(similarities.size() * 0.95d) - 1;
        PairStatistics statistics = new PairStatistics(mean, similarities.get(percentileIndex),
                similarities.getLast());

        Map<Integer, Integer> specificity = counts(selected, plan.specificity().setTargets().keySet(),
                DefaultCandidateSetEngine::specificityCount);
        Map<CandidateProfile, Integer> profiles = counts(selected, plan.profiles().setTargets().keySet(),
                AcceptedProposal::profile);
        Map<NoveltyBand, Integer> novelty = counts(selected, plan.novelty().setTargets().keySet(),
                item -> item.evaluation().actualNoveltyBand());
        return new SetEvaluation(
                quota(plan.specificity().setTargets(), specificity),
                quota(plan.profiles().setTargets(), profiles),
                quota(plan.novelty().setTargets(), novelty),
                pairs, statistics,
                usage(selected, DefaultCandidateSetEngine::randomConcepts),
                usage(selected, item -> similarity.informativeAncestors(item, catalog, configuration)),
                profiles,
                (int) selected.stream().filter(DefaultCandidateSetEngine::difficult).count(),
                run.decisions(), reasons);
    }

    private static <T> QuotaEvaluation<T> quota(Map<T, Integer> targets, Map<T, Integer> actual) {
        Map<T, Integer> deviations = new HashMap<>();
        targets.forEach((key, target) -> deviations.put(key, actual.getOrDefault(key, 0) - target));
        return new QuotaEvaluation<>(targets, actual, deviations);
    }

    private static <T> Map<T, Integer> counts(
            List<AcceptedProposal> candidates,
            Set<T> categories,
            Function<AcceptedProposal, T> category
    ) {
        Map<T, Integer> result = new HashMap<>();
        categories.forEach(value -> result.put(value, 0));
        candidates.forEach(candidate -> result.merge(category.apply(candidate), 1, Integer::sum));
        return result;
    }

    private static Map<String, Integer> usage(
            List<AcceptedProposal> candidates,
            Function<AcceptedProposal, Set<String>> values
    ) {
        Map<String, Integer> result = new TreeMap<>();
        candidates.forEach(candidate -> values.apply(candidate).forEach(value -> result.merge(value, 1, Integer::sum)));
        return result;
    }

    private static boolean wouldExceedUsage(
            AcceptedProposal candidate,
            List<AcceptedProposal> selected,
            Function<AcceptedProposal, Set<String>> values,
            int cap
    ) {
        Map<String, Integer> usage = usage(selected, values);
        return values.apply(candidate).stream().anyMatch(value -> usage.getOrDefault(value, 0) + 1 > cap);
    }

    private BigDecimal pairSimilarity(
            AcceptedProposal first,
            AcceptedProposal second,
            io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot catalog,
            GeneratorConfiguration configuration,
            Map<PairKey, BigDecimal> cache
    ) {
        PairKey key = PairKey.of(first.canonicalSignature(), second.canonicalSignature());
        return cache.computeIfAbsent(key, ignored -> similarity.assess(1, first, 2, second,
                catalog, configuration, null).totalSimilarity());
    }

    private static Set<String> randomConcepts(AcceptedProposal candidate) {
        Set<String> result = new HashSet<>();
        candidate.requirements().stream().filter(item -> item.source() == RequirementSource.RANDOM)
                .map(RequirementSnapshot::concept).forEach(concept -> result.add(concept.code()));
        return result;
    }

    private static int specificityCount(AcceptedProposal candidate) {
        return (int) candidate.requirements().stream()
                .filter(item -> item.specificity() == RequirementSpecificity.SPECIFIC).count();
    }

    private static boolean difficult(AcceptedProposal candidate) {
        return candidate.requirements().stream()
                .filter(item -> item.source() == RequirementSource.RANDOM)
                .anyMatch(item -> item.concept().availabilityByParticipant().values().contains(Availability.DIFFICULT));
    }

    private static long selectionWeight(
            BigDecimal utility,
            BigDecimal minimum,
            GeneratorConfiguration configuration
    ) {
        BigDecimal base = ONE.add(scaled(BigDecimal.valueOf(configuration.selection().topBandSlope())
                .multiply(scaled(utility.subtract(minimum)))));
        try {
            long weight = scaled(base.multiply(base)).multiply(BigDecimal.valueOf(configuration.weightQuantization()))
                    .setScale(0, ROUNDING).longValueExact();
            if (weight <= 0) {
                throw new ArithmeticException("Selection weight is not positive");
            }
            return weight;
        } catch (ArithmeticException exception) {
            throw weightOverflow("Top-band selection weight cannot be represented", exception);
        }
    }

    private static FallbackLevel startingFallback(int reservoirSize, GeneratorConfiguration configuration) {
        if (reservoirSize >= configuration.reservoirStrictMinimum()) {
            return FallbackLevel.STRICT;
        }
        if (reservoirSize >= configuration.reservoirRelaxedOneMinimum()) {
            return FallbackLevel.RELAXED_1;
        }
        return FallbackLevel.RELAXED_2;
    }

    private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private static io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException weightOverflow(
            String detail,
            ArithmeticException cause
    ) {
        var exception = new io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException(
                GeneratorReasonCode.WEIGHT_SUM_OVERFLOW, detail);
        exception.initCause(cause);
        return exception;
    }

    private record PairKey(String first, String second) {
        static PairKey of(String first, String second) {
            return first.compareTo(second) <= 0 ? new PairKey(first, second) : new PairKey(second, first);
        }
    }

    private record CandidateUtility(
            AcceptedProposal candidate,
            BigDecimal quality,
            BigDecimal diversity,
            BigDecimal quotaFit,
            BigDecimal utility
    ) {
    }

    private record WeightedUtility(CandidateUtility utility, long weight) {
    }

    private record SelectionRun(
            FallbackLevel level,
            List<AcceptedProposal> selected,
            List<SelectionDecision> decisions,
            Map<GeneratorReasonCode, Long> rejections
    ) {
        boolean complete(int size) {
            return selected.size() == size;
        }
    }
}
