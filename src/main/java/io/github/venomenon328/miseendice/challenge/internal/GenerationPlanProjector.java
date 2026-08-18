package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan.ProjectedDistribution;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GenerationPlanProjector {
    GenerationPlan project(GenerationContext context) {
        GeneratorConfiguration config = context.configuration();
        Set<GeneratorReasonCode> diagnostics = EnumSet.noneOf(GeneratorReasonCode.class);
        List<GeneratorReasonCode> errors = new ArrayList<>();

        validateContextReferences(context, errors, diagnostics);
        List<GeneratorConcept> drawable = context.catalog().concepts().stream()
                .filter(concept -> basicallyDrawable(concept, context)).toList();

        int manualSpecific = (int) context.manualRequirements().stream()
                .filter(manual -> manual.matchedConcept() != null
                        && manual.matchedConcept().specificity() == Specificity.SPECIFIC).count();
        int randomSlots = 4 - context.manualRequirements().size();
        Set<Integer> reachableSpecificity = new HashSet<>();
        long specificPool = drawable.stream().filter(c -> c.specificity() == Specificity.SPECIFIC).count();
        long openPool = drawable.stream().filter(c -> c.specificity() == Specificity.OPEN).count();
        for (int total : List.of(2, 3, 4)) {
            int randomSpecific = total - manualSpecific;
            int randomOpen = randomSlots - randomSpecific;
            if (randomSpecific >= 0 && randomOpen >= 0 && specificPool >= randomSpecific && openPool >= randomOpen) {
                reachableSpecificity.add(total);
            }
        }
        if (!reachableSpecificity.equals(config.specificityWeights().keySet())) {
            diagnostics.add(GeneratorReasonCode.SPECIFICITY_TARGET_PROJECTED);
        }

        Set<CandidateProfile> reachableProfiles = EnumSet.noneOf(CandidateProfile.class);
        List<ProfileMatcher.RoleRequirement> manuals = context.manualRequirements().stream()
                .filter(manual -> manual.matchedConcept() != null)
                .map(manual -> new ProfileMatcher.RoleRequirement(manual.matchedConcept().code(),
                        manual.matchedConcept().id(), manual.matchedConcept().functionalRoles())).toList();
        List<ProfileMatcher.RoleRequirement> randomRoleCandidates = drawable.stream()
                .map(concept -> new ProfileMatcher.RoleRequirement(
                        concept.code(), concept.id(), concept.functionalRoles()))
                .toList();
        for (CandidateProfile profile : CandidateProfile.values()) {
            if (ProfileMatcher.canComplete(profile, manuals, randomRoleCandidates, randomSlots, config)
                    && anchorStructureReachable(manuals, drawable, randomSlots, config)) {
                reachableProfiles.add(profile);
            }
        }
        if (!reachableProfiles.equals(config.profileWeights().keySet())) {
            diagnostics.add(GeneratorReasonCode.PROFILE_TARGET_PROJECTED);
        }

        Set<NoveltyBand> reachableNovelty = reachableNoveltyBands(context, drawable, randomSlots);
        Set<NoveltyBand> positivelyTargeted = EnumSet.noneOf(NoveltyBand.class);
        context.noveltyTargetDistribution().forEach((band, target) -> {
            if (target > 0) positivelyTargeted.add(band);
        });
        if (!reachableNovelty.containsAll(positivelyTargeted) || !reachableNovelty.equals(EnumSet.allOf(NoveltyBand.class))) {
            diagnostics.add(GeneratorReasonCode.NOVELTY_TARGET_PROJECTED);
        }

        if (errors.isEmpty() && (reachableSpecificity.isEmpty() || reachableProfiles.isEmpty() || reachableNovelty.isEmpty())) {
            errors.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
        }

        return new GenerationPlan(
                distribution(config.specificityWeights(), reachableSpecificity, config.candidateSetSize()),
                distribution(config.profileWeights(), reachableProfiles, config.candidateSetSize()),
                distribution(context.noveltyTargetDistribution(), reachableNovelty, config.candidateSetSize()),
                diagnostics,
                errors
        );
    }

    private void validateContextReferences(GenerationContext context, List<GeneratorReasonCode> errors,
                                           Set<GeneratorReasonCode> diagnostics) {
        for (GenerationContext.ManualRequirement manual : context.manualRequirements()) {
            if (manual.matchedConcept() == null) {
                diagnostics.add(GeneratorReasonCode.UNCLASSIFIED_MANUAL_REQUIREMENT);
                continue;
            }
            boolean exactSnapshotMatch = context.catalog().conceptById(manual.matchedConcept().id())
                    .filter(manual.matchedConcept()::equals)
                    .isPresent();
            if (!exactSnapshotMatch) {
                addError(errors, GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID);
            }
        }
        List<GeneratorConcept> matched = context.manualRequirements().stream()
                .map(GenerationContext.ManualRequirement::matchedConcept).filter(java.util.Objects::nonNull).toList();
        if (matched.size() == 2 && (matched.get(0).code().equals(matched.get(1).code())
                || related(matched.get(0), matched.get(1)))) {
            diagnostics.add(GeneratorReasonCode.MANUAL_REQUIREMENT_REDUNDANCY);
        }
    }

    private static void addError(List<GeneratorReasonCode> errors, GeneratorReasonCode reason) {
        if (!errors.contains(reason)) {
            errors.add(reason);
        }
    }

    private boolean basicallyDrawable(GeneratorConcept concept, GenerationContext context) {
        return concept.active() && concept.randomDrawEnabled() && !concept.functionalRoles().isEmpty()
                && concept.noveltyLevel() != null
                && concept.availabilityByParticipant().keySet().containsAll(context.catalog().activeParticipantCodes())
                && concept.availabilityByParticipant().values().stream()
                .noneMatch(value -> value == io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability.UNAVAILABLE)
                && exactHistoryDistance(concept.code(), context) > context.configuration().cooldown().hardWindow()
                && context.manualRequirements().stream().filter(manual -> manual.matchedConcept() != null)
                .noneMatch(manual -> manual.matchedConcept().code().equals(concept.code())
                        || related(manual.matchedConcept(), concept));
    }

    private int exactHistoryDistance(String code, GenerationContext context) {
        for (int index = 0; index < context.visibleHistory().cooldownExposuresNewestFirst().size(); index++) {
            if (context.visibleHistory().cooldownExposuresNewestFirst().get(index).requirements().stream()
                    .anyMatch(requirement -> code.equals(requirement.conceptCode()))) {
                return index + 1;
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean anchorStructureReachable(List<ProfileMatcher.RoleRequirement> manuals,
                                             List<GeneratorConcept> drawable,
                                             int randomSlots,
                                             GeneratorConfiguration config) {
        int manualAnchorRequirements = (int) manuals.stream()
                .filter(item -> item.roles().stream().anyMatch(config.anchorRoles()::contains)).count();
        List<GeneratorConcept> randomAnchors = drawable.stream()
                .filter(item -> item.functionalRoles().stream().anyMatch(config.anchorRoles()::contains)).toList();
        if (manualAnchorRequirements + Math.min(randomSlots, randomAnchors.size()) < 2) {
            return false;
        }
        Set<String> reachableAnchorRoles = new HashSet<>();
        manuals.forEach(item -> item.roles().stream().filter(config.anchorRoles()::contains)
                .forEach(reachableAnchorRoles::add));
        if (randomSlots > 0) {
            randomAnchors.forEach(item -> item.functionalRoles().stream().filter(config.anchorRoles()::contains)
                    .forEach(reachableAnchorRoles::add));
        }
        return reachableAnchorRoles.size() >= 2;
    }

    private Set<NoveltyBand> reachableNoveltyBands(GenerationContext context, List<GeneratorConcept> drawable,
                                                    int randomSlots) {
        GeneratorConfiguration config = context.configuration();
        int manualLoad = context.manualRequirements().stream().filter(m -> m.matchedConcept() != null)
                .map(GenerationContext.ManualRequirement::matchedConcept)
                .filter(c -> c.noveltyLevel() != null)
                .mapToInt(c -> config.novelty().loadPoints().get(c.noveltyLevel())).sum();
        int manualFive = (int) context.manualRequirements().stream().filter(m -> m.matchedConcept() != null)
                .map(GenerationContext.ManualRequirement::matchedConcept)
                .filter(c -> Integer.valueOf(5).equals(c.noveltyLevel())).count();
        int manualHigh = (int) context.manualRequirements().stream().filter(m -> m.matchedConcept() != null)
                .map(GenerationContext.ManualRequirement::matchedConcept)
                .filter(c -> c.noveltyLevel() != null && c.noveltyLevel() >= 4).count();
        boolean manualForced = manualFive > config.novelty().levelFiveCap()
                || manualHigh > config.novelty().highLevelCap() || manualLoad > config.novelty().loadCap();

        Set<State> states = new HashSet<>();
        states.add(new State(0, manualLoad, manualFive, manualHigh));
        for (GeneratorConcept concept : drawable.stream().sorted(GeneratorConcept.CANONICAL_ORDER).toList()) {
            int level = concept.noveltyLevel();
            if (manualForced && level >= 4) {
                continue;
            }
            Set<State> next = new HashSet<>(states);
            for (State state : states) {
                if (state.slots() >= randomSlots) {
                    continue;
                }
                State added = new State(
                        state.slots() + 1,
                        state.load() + config.novelty().loadPoints().get(level),
                        state.fives() + (level == 5 ? 1 : 0),
                        state.highs() + (level >= 4 ? 1 : 0));
                if (manualForced || withinNoveltyCaps(added, config)) {
                    next.add(added);
                }
            }
            states = next;
        }

        Set<NoveltyBand> result = EnumSet.noneOf(NoveltyBand.class);
        states.stream().filter(state -> state.slots() == randomSlots)
                .forEach(state -> result.add(DefaultCandidateProposalEngine.classifyNovelty(
                        state.load(), state.fives(), state.highs(), manualForced)));
        return result;
    }

    private boolean withinNoveltyCaps(State state, GeneratorConfiguration config) {
        return state.fives() <= config.novelty().levelFiveCap()
                && state.highs() <= config.novelty().highLevelCap()
                && state.load() <= config.novelty().loadCap();
    }

    private static boolean related(GeneratorConcept first, GeneratorConcept second) {
        return first.transitiveAncestorCodes().contains(second.code())
                || first.transitiveDescendantCodes().contains(second.code())
                || second.transitiveAncestorCodes().contains(first.code())
                || second.transitiveDescendantCodes().contains(first.code());
    }

    private static <T> ProjectedDistribution<T> distribution(Map<T, Integer> base, Set<T> reachable, int setSize) {
        if (reachable.isEmpty()) {
            return new ProjectedDistribution<>(Map.of(), Map.of());
        }
        Map<T, BigDecimal> positive = new LinkedHashMap<>();
        base.entrySet().stream().sorted(Map.Entry.comparingByKey(canonicalComparator())).forEach(entry -> {
            if (reachable.contains(entry.getKey()) && entry.getValue() > 0) {
                positive.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
            }
        });
        if (positive.isEmpty() && !reachable.isEmpty()) {
            reachable.stream().sorted(canonicalComparator()).forEach(key -> positive.put(key, BigDecimal.ONE));
        }
        BigDecimal sum = positive.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<T, BigDecimal> normalized = new LinkedHashMap<>();
        positive.forEach((key, value) -> normalized.put(key, value.divide(sum, 12, RoundingMode.HALF_EVEN)));
        return new ProjectedDistribution<>(normalized, largestRemainder(normalized, setSize));
    }

    private static <T> Map<T, Integer> largestRemainder(Map<T, BigDecimal> normalized, int setSize) {
        Map<T, Integer> targets = new LinkedHashMap<>();
        List<Remainder<T>> remainders = new ArrayList<>();
        int assigned = 0;
        for (Map.Entry<T, BigDecimal> entry : normalized.entrySet()) {
            BigDecimal exact = entry.getValue().multiply(BigDecimal.valueOf(setSize));
            int floor = exact.setScale(0, RoundingMode.DOWN).intValueExact();
            targets.put(entry.getKey(), floor);
            assigned += floor;
            remainders.add(new Remainder<>(entry.getKey(), exact.subtract(BigDecimal.valueOf(floor))));
        }
        remainders.sort(Comparator.comparing(Remainder<T>::remainder).reversed()
                .thenComparing(Remainder::key, canonicalComparator()));
        for (int index = 0; index < setSize - assigned; index++) {
            T key = remainders.get(index).key();
            targets.put(key, targets.get(key) + 1);
        }
        return Map.copyOf(targets);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Comparator<T> canonicalComparator() {
        return (left, right) -> {
            if (left instanceof Enum<?> leftEnum && right instanceof Enum<?> rightEnum) {
                return leftEnum.name().compareTo(rightEnum.name());
            }
            if (left instanceof Comparable comparable) {
                return comparable.compareTo(right);
            }
            return left.toString().compareTo(right.toString());
        };
    }

    private record State(int slots, int load, int fives, int highs) { }
    private record Remainder<T>(T key, BigDecimal remainder) { }
}
