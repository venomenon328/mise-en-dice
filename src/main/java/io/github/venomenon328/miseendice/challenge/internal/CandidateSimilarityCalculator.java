package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ComponentSimilarity;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairAssessment;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.SimilarityComponent;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

/** Exact version-1 pair similarity, deliberately independent from proposal generation. */
final class CandidateSimilarityCalculator {
    static final int SCALE = 12;
    static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal ONE = scaled(BigDecimal.ONE);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal FOUR = BigDecimal.valueOf(4);
    private final Map<CatalogGeneratorSnapshot, CatalogIndex> catalogIndexes = new WeakHashMap<>();

    PairAssessment assess(
            int firstNumber,
            AcceptedProposal first,
            int secondNumber,
            AcceptedProposal second,
            CatalogGeneratorSnapshot catalog,
            GeneratorConfiguration configuration,
            BigDecimal maximumSimilarity
    ) {
        EnumMap<SimilarityComponent, ComponentSimilarity> components = new EnumMap<>(SimilarityComponent.class);
        BigDecimal exact = setJaccard(randomConceptCodes(first), randomConceptCodes(second));
        components.put(SimilarityComponent.EXACT_RANDOM_CONCEPTS, ComponentSimilarity.comparable(exact));

        BigDecimal ancestors = ancestorSimilarity(first, second, catalog);
        components.put(SimilarityComponent.INFORMATIVE_ANCESTORS,
                ancestors == null ? ComponentSimilarity.notComparable() : ComponentSimilarity.comparable(ancestors));
        components.put(SimilarityComponent.ROLES_AND_PROFILE,
                ComponentSimilarity.comparable(roleProfileSimilarity(first, second, configuration)));
        components.put(SimilarityComponent.SPECIFICITY_MIX,
                ComponentSimilarity.comparable(specificitySimilarity(first, second)));
        components.put(SimilarityComponent.NOVELTY,
                ComponentSimilarity.comparable(noveltySimilarity(first, second, configuration)));
        components.put(SimilarityComponent.AVAILABILITY_LOAD,
                ComponentSimilarity.comparable(availabilitySimilarity(first, second)));
        BigDecimal properties = propertySimilarity(first, second, configuration);
        components.put(SimilarityComponent.COMPARABLE_PROPERTIES,
                properties == null ? ComponentSimilarity.notComparable() : ComponentSimilarity.comparable(properties));
        if (configuration.generatorVersion().equals("1.2.0")) {
            components.put(SimilarityComponent.RESTRICTION,
                    ComponentSimilarity.comparable(restrictionSimilarity(first, second)));
        }

        BigDecimal comparableWeight = scaled(BigDecimal.ZERO);
        for (Map.Entry<SimilarityComponent, ComponentSimilarity> entry : components.entrySet()) {
            if (entry.getValue().comparability()
                    == io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.Comparability.COMPARABLE) {
                comparableWeight = scaled(comparableWeight.add(configuration.similarityWeights().get(entry.getKey())));
            }
        }
        EnumMap<SimilarityComponent, BigDecimal> renormalized = new EnumMap<>(SimilarityComponent.class);
        for (Map.Entry<SimilarityComponent, ComponentSimilarity> entry : components.entrySet()) {
            SimilarityComponent component = entry.getKey();
            ComponentSimilarity value = entry.getValue();
            if (value.value() == null) {
                continue;
            }
            BigDecimal weight = ratio(configuration.similarityWeights().get(component), comparableWeight);
            renormalized.put(component, weight);
        }
        BigDecimal renormalizedSum = renormalized.values().stream().reduce(scaled(BigDecimal.ZERO),
                (left, right) -> scaled(left.add(right)));
        if (renormalizedSum.compareTo(ONE) != 0) {
            SimilarityComponent last = renormalized.keySet().stream().reduce((left, right) -> right).orElseThrow();
            renormalized.put(last, scaled(renormalized.get(last).add(ONE.subtract(renormalizedSum))));
        }
        BigDecimal total = scaled(BigDecimal.ZERO);
        for (Map.Entry<SimilarityComponent, BigDecimal> weight : renormalized.entrySet()) {
            total = scaled(total.add(scaled(components.get(weight.getKey()).value().multiply(weight.getValue()))));
        }

        List<GeneratorReasonCode> diagnostics = new ArrayList<>();
        if (exact.signum() > 0) {
            diagnostics.add(GeneratorReasonCode.PAIR_EXACT_OVERLAP);
        }
        if (ancestors != null && ancestors.signum() > 0) {
            diagnostics.add(GeneratorReasonCode.PAIR_ANCESTOR_OVERLAP);
        }
        if (maximumSimilarity != null && total.compareTo(maximumSimilarity) > 0) {
            diagnostics.add(GeneratorReasonCode.PAIR_SIMILARITY_LIMIT);
        }
        return new PairAssessment(firstNumber, secondNumber, components, renormalized, total, diagnostics);
    }

    Set<String> informativeAncestors(AcceptedProposal candidate, CatalogGeneratorSnapshot catalog,
                                     GeneratorConfiguration configuration) {
        CatalogIndex index = index(catalog);
        int drawableCount = index.drawableCount();
        if (drawableCount == 0) {
            return Set.of();
        }
        BigDecimal maximumShare = configuration.similarity().informativeAncestorMaximumDrawableShare();
        Set<String> result = new TreeSet<>();
        for (String ancestor : randomAncestorCodes(candidate)) {
            int descendants = index.drawableDescendantCounts().getOrDefault(ancestor, 0);
            if (ratio(BigDecimal.valueOf(descendants), BigDecimal.valueOf(drawableCount))
                    .compareTo(maximumShare) <= 0) {
                result.add(ancestor);
            }
        }
        return Set.copyOf(result);
    }

    private BigDecimal ancestorSimilarity(
            AcceptedProposal first,
            AcceptedProposal second,
            CatalogGeneratorSnapshot catalog
    ) {
        CatalogIndex index = index(catalog);
        int drawableCount = index.drawableCount();
        if (drawableCount == 0) {
            return null;
        }
        Map<String, BigDecimal> firstWeights = positiveAncestorWeights(first, index);
        Map<String, BigDecimal> secondWeights = positiveAncestorWeights(second, index);
        if (firstWeights.isEmpty() || secondWeights.isEmpty()) {
            return null;
        }
        Set<String> union = new HashSet<>(firstWeights.keySet());
        union.addAll(secondWeights.keySet());
        Set<String> intersection = new HashSet<>(firstWeights.keySet());
        intersection.retainAll(secondWeights.keySet());
        BigDecimal numerator = intersection.stream().map(code -> firstWeights.get(code))
                .reduce(scaled(BigDecimal.ZERO), (left, right) -> scaled(left.add(right)));
        BigDecimal denominator = union.stream().map(code ->
                        firstWeights.containsKey(code) ? firstWeights.get(code) : secondWeights.get(code))
                .reduce(scaled(BigDecimal.ZERO), (left, right) -> scaled(left.add(right)));
        return ratio(numerator, denominator);
    }

    private Map<String, BigDecimal> positiveAncestorWeights(
            AcceptedProposal candidate,
            CatalogIndex index
    ) {
        Map<String, BigDecimal> weights = new HashMap<>();
        for (String ancestor : randomAncestorCodes(candidate)) {
            int descendants = index.drawableDescendantCounts().getOrDefault(ancestor, 0);
            BigDecimal remainingShare = ratio(BigDecimal.valueOf(index.drawableCount() - descendants),
                    BigDecimal.valueOf(index.drawableCount()));
            BigDecimal weight = scaled(remainingShare.multiply(remainingShare));
            if (weight.signum() > 0) {
                weights.put(ancestor, weight);
            }
        }
        return weights;
    }

    private BigDecimal roleProfileSimilarity(
            AcceptedProposal first,
            AcceptedProposal second,
            GeneratorConfiguration configuration
    ) {
        Map<String, Integer> firstRoles = roleCounts(first);
        Map<String, Integer> secondRoles = roleCounts(second);
        Set<String> union = new HashSet<>(firstRoles.keySet());
        union.addAll(secondRoles.keySet());
        int numerator = union.stream().mapToInt(role ->
                Math.min(firstRoles.getOrDefault(role, 0), secondRoles.getOrDefault(role, 0))).sum();
        int denominator = union.stream().mapToInt(role ->
                Math.max(firstRoles.getOrDefault(role, 0), secondRoles.getOrDefault(role, 0))).sum();
        BigDecimal roleSimilarity = ratio(BigDecimal.valueOf(numerator), BigDecimal.valueOf(denominator));
        BigDecimal sameProfile = first.profile() == second.profile() ? ONE : scaled(BigDecimal.ZERO);
        return scaled(scaled(roleSimilarity.multiply(configuration.similarity().roleWeight()))
                .add(scaled(sameProfile.multiply(configuration.similarity().profileWeight()))));
    }

    private BigDecimal specificitySimilarity(AcceptedProposal first, AcceptedProposal second) {
        int firstSpecific = (int) first.requirements().stream()
                .filter(requirement -> requirement.specificity() == RequirementSpecificity.SPECIFIC).count();
        int secondSpecific = (int) second.requirements().stream()
                .filter(requirement -> requirement.specificity() == RequirementSpecificity.SPECIFIC).count();
        return scaled(ONE.subtract(ratio(BigDecimal.valueOf(Math.abs(firstSpecific - secondSpecific)), TWO)));
    }

    private BigDecimal noveltySimilarity(
            AcceptedProposal first,
            AcceptedProposal second,
            GeneratorConfiguration configuration
    ) {
        int bandDistance = Math.abs(bandIndex(first.evaluation().actualNoveltyBand())
                - bandIndex(second.evaluation().actualNoveltyBand()));
        BigDecimal band = scaled(ONE.subtract(ratio(BigDecimal.valueOf(bandDistance), TWO)));
        int loadDifference = Math.abs(first.evaluation().knownNoveltyLoad()
                - second.evaluation().knownNoveltyLoad());
        BigDecimal load;
        if (configuration.novelty().loadCap() == 0) {
            load = loadDifference == 0 ? ONE : scaled(BigDecimal.ZERO);
        } else {
            load = scaled(ONE.subtract(ratio(BigDecimal.valueOf(Math.min(loadDifference,
                    configuration.novelty().loadCap())), BigDecimal.valueOf(configuration.novelty().loadCap()))));
        }
        return scaled(scaled(band.multiply(configuration.similarity().noveltyBandWeight()))
                .add(scaled(load.multiply(configuration.similarity().noveltyLoadWeight()))));
    }

    private BigDecimal availabilitySimilarity(AcceptedProposal first, AcceptedProposal second) {
        BigDecimal firstMean = meanAvailabilityLoad(first);
        BigDecimal secondMean = meanAvailabilityLoad(second);
        return scaled(ONE.subtract(firstMean.subtract(secondMean).abs()));
    }

    private BigDecimal restrictionSimilarity(AcceptedProposal first, AcceptedProposal second) {
        return java.util.Objects.equals(first.restriction().ruleCode(), second.restriction().ruleCode())
                ? ONE : scaled(BigDecimal.ZERO);
    }

    private BigDecimal meanAvailabilityLoad(AcceptedProposal candidate) {
        List<RequirementSnapshot> random = randomRequirements(candidate);
        BigDecimal sum = random.stream()
                .map(requirement -> scaled(ONE.subtract(requirement.weightEvaluation().availabilityFactor())))
                .reduce(scaled(BigDecimal.ZERO), (left, right) -> scaled(left.add(right)));
        return ratio(sum, BigDecimal.valueOf(random.size()));
    }

    private BigDecimal propertySimilarity(
            AcceptedProposal first,
            AcceptedProposal second,
            GeneratorConfiguration configuration
    ) {
        Set<String> firstFlags = flags(first);
        Set<String> secondFlags = flags(second);
        Set<String> flagUnion = new HashSet<>(firstFlags);
        flagUnion.addAll(secondFlags);
        BigDecimal flag = flagUnion.isEmpty() ? null : setJaccard(firstFlags, secondFlags);

        Map<String, BigDecimal> firstDimensions = dimensionMeans(first);
        Map<String, BigDecimal> secondDimensions = dimensionMeans(second);
        Set<String> comparable = new HashSet<>(firstDimensions.keySet());
        comparable.retainAll(secondDimensions.keySet());
        BigDecimal dimensions = null;
        if (!comparable.isEmpty()) {
            BigDecimal sum = comparable.stream().map(dimension -> scaled(ONE.subtract(ratio(
                            firstDimensions.get(dimension).subtract(secondDimensions.get(dimension)).abs(), FOUR))))
                    .reduce(scaled(BigDecimal.ZERO), (left, right) -> scaled(left.add(right)));
            dimensions = ratio(sum, BigDecimal.valueOf(comparable.size()));
        }
        if (flag == null) {
            return dimensions;
        }
        if (dimensions == null) {
            return flag;
        }
        return scaled(scaled(flag.multiply(configuration.similarity().flagWeight()))
                .add(scaled(dimensions.multiply(configuration.similarity().dimensionWeight()))));
    }

    private Map<String, BigDecimal> dimensionMeans(AcceptedProposal candidate) {
        Map<String, List<Integer>> values = new HashMap<>();
        for (RequirementSnapshot requirement : randomRequirements(candidate)) {
            requirement.concept().culinaryDimensions().forEach((dimension, level) ->
                    values.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(level));
        }
        Map<String, BigDecimal> result = new HashMap<>();
        values.forEach((dimension, levels) -> result.put(dimension, ratio(
                BigDecimal.valueOf(levels.stream().mapToInt(Integer::intValue).sum()),
                BigDecimal.valueOf(levels.size()))));
        return result;
    }

    private Set<String> flags(AcceptedProposal candidate) {
        Set<String> flags = new HashSet<>();
        randomRequirements(candidate).forEach(requirement -> flags.addAll(requirement.concept().culinaryFlags()));
        return flags;
    }

    private Map<String, Integer> roleCounts(AcceptedProposal candidate) {
        Map<String, Integer> counts = new HashMap<>();
        randomRequirements(candidate).forEach(requirement -> requirement.concept().functionalRoles()
                .forEach(role -> counts.merge(role, 1, Integer::sum)));
        return counts;
    }

    private Set<String> randomConceptCodes(AcceptedProposal candidate) {
        Set<String> codes = new HashSet<>();
        randomRequirements(candidate).forEach(requirement -> codes.add(requirement.concept().code()));
        return codes;
    }

    private Set<String> randomAncestorCodes(AcceptedProposal candidate) {
        Set<String> codes = new HashSet<>();
        randomRequirements(candidate).forEach(requirement ->
                codes.addAll(requirement.concept().transitiveAncestorCodes()));
        return codes;
    }

    private List<RequirementSnapshot> randomRequirements(AcceptedProposal candidate) {
        return candidate.requirements().stream()
                .filter(requirement -> requirement.source() == RequirementSource.RANDOM)
                .toList();
    }

    private synchronized CatalogIndex index(CatalogGeneratorSnapshot catalog) {
        return catalogIndexes.computeIfAbsent(catalog, snapshot -> {
            int drawableCount = 0;
            Map<String, Integer> descendantCounts = new HashMap<>();
            for (var concept : snapshot.concepts()) {
                if (!concept.active() || !concept.randomDrawEnabled()) {
                    continue;
                }
                drawableCount++;
                concept.transitiveAncestorCodes().forEach(ancestor ->
                        descendantCounts.merge(ancestor, 1, Integer::sum));
            }
            return new CatalogIndex(drawableCount, Map.copyOf(descendantCounts));
        });
    }

    private int bandIndex(NoveltyBand band) {
        return switch (band) {
            case FAMILIAR -> 0;
            case BALANCED -> 1;
            case ADVENTUROUS -> 2;
        };
    }

    private static BigDecimal setJaccard(Set<String> first, Set<String> second) {
        Set<String> union = new HashSet<>(first);
        union.addAll(second);
        if (union.isEmpty()) {
            return ONE;
        }
        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);
        return ratio(BigDecimal.valueOf(intersection.size()), BigDecimal.valueOf(union.size()));
    }

    static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            throw new IllegalArgumentException("A similarity denominator must be positive");
        }
        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    static BigDecimal scaled(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private record CatalogIndex(int drawableCount, Map<String, Integer> drawableDescendantCounts) {
    }
}
