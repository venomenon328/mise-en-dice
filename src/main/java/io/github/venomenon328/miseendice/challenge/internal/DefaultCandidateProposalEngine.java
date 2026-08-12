package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ProfileSlot;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class DefaultCandidateProposalEngine implements CandidateProposalEngine {
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final Set<String> STRONG_FLAGS = Set.of("SMOKED", "FERMENTED", "CURED", "DRIED");

    private final GeneratorConfiguration configuration;
    private final String canonicalConfigurationSnapshot;
    private final GenerationPlanProjector planProjector = new GenerationPlanProjector();

    DefaultCandidateProposalEngine(GeneratorConfiguration configuration, String canonicalConfigurationSnapshot) {
        this.configuration = configuration;
        this.canonicalConfigurationSnapshot = canonicalConfigurationSnapshot;
    }

    @Override
    public GeneratorDescriptor descriptor() {
        return new GeneratorDescriptor(configuration.generatorVersion(), configuration.configurationVersion(),
                configuration.rngAlgorithm(), configuration.canonicalPayloadVersion(), canonicalConfigurationSnapshot);
    }

    @Override
    public GenerationPlan validateAndPlan(GenerationContext context) {
        validateConfigurationIdentity(context);
        return planProjector.project(context);
    }

    @Override
    public ProposalResult propose(GenerationContext context, long proposalOrdinal) {
        if (proposalOrdinal < 0) {
            throw new IllegalArgumentException("proposalOrdinal must not be negative");
        }
        validateConfigurationIdentity(context);
        GenerationPlan plan = planProjector.project(context);
        if (!plan.valid()) {
            return new RejectedProposal(proposalOrdinal, null, null, null, plan.validationErrors(), List.of(),
                    plan.diagnostics());
        }

        CandidateProfile profile = draw(plan.profiles().normalizedWeights(), context, proposalOrdinal,
                SeedDerivation.Purpose.PROPOSAL_PROFILE);
        int targetSpecificity = draw(plan.specificity().normalizedWeights(), context, proposalOrdinal,
                SeedDerivation.Purpose.PROPOSAL_SPECIFICITY);
        NoveltyBand targetNovelty = draw(plan.novelty().normalizedWeights(), context, proposalOrdinal,
                SeedDerivation.Purpose.PROPOSAL_NOVELTY);

        List<WorkRequirement> requirements = new ArrayList<>();
        context.manualRequirements().forEach(manual -> requirements.add(WorkRequirement.manual(manual)));
        List<Integer> randomPositions = java.util.stream.IntStream.rangeClosed(1, 4)
                .filter(position -> context.manualRequirements().stream().noneMatch(m -> m.position() == position))
                .boxed().toList();
        int manualSpecific = (int) requirements.stream()
                .filter(requirement -> requirement.specificity() == RequirementSpecificity.SPECIFIC).count();
        int randomSpecificNeeded = targetSpecificity - manualSpecific;
        List<WeightEvaluation> allEvaluations = new ArrayList<>();

        for (int randomIndex = 0; randomIndex < randomPositions.size(); randomIndex++) {
            int position = randomPositions.get(randomIndex);
            Specificity neededSpecificity = randomIndex < randomSpecificNeeded ? Specificity.SPECIFIC : Specificity.OPEN;
            ProfileSlot requiredSlot = requiredSlot(profile, requirements, randomPositions.size() - randomIndex, context);
            List<WeightEvaluation> evaluated = context.catalog().concepts().stream()
                    .sorted(GeneratorConcept.CANONICAL_ORDER)
                    .map(concept -> evaluateWeight(concept, neededSpecificity, requiredSlot, requirements,
                            targetNovelty, context)).toList();
            allEvaluations.addAll(evaluated);
            List<WeightEvaluation> selectable = evaluated.stream().filter(weight -> weight.quantizedWeight() > 0).toList();
            if (manualNoveltyForced(requirements, context.configuration()) && !selectable.isEmpty()) {
                int minimumLoad = selectable.stream().map(weight -> context.catalog().conceptByCode(weight.conceptCode()).orElseThrow())
                        .mapToInt(concept -> context.configuration().novelty().loadPoints().get(concept.noveltyLevel())).min().orElseThrow();
                selectable = selectable.stream().filter(weight -> {
                    GeneratorConcept concept = context.catalog().conceptByCode(weight.conceptCode()).orElseThrow();
                    return context.configuration().novelty().loadPoints().get(concept.noveltyLevel()) == minimumLoad;
                }).toList();
            }
            if (selectable.isEmpty()) {
                Set<GeneratorReasonCode> diagnostics = diagnosticSet(plan.diagnostics());
                diagnostics.add(GeneratorReasonCode.EMPTY_WEIGHTED_POOL);
                return new RejectedProposal(proposalOrdinal, profile, targetSpecificity, targetNovelty,
                        List.of(GeneratorReasonCode.EMPTY_WEIGHTED_POOL), evaluated, diagnostics);
            }
            WeightEvaluation chosen;
            try {
                chosen = choose(selectable, context, proposalOrdinal, SeedDerivation.proposalSlot(position));
            } catch (ArithmeticException exception) {
                Set<GeneratorReasonCode> diagnostics = diagnosticSet(plan.diagnostics());
                diagnostics.add(GeneratorReasonCode.WEIGHT_SUM_OVERFLOW);
                return new RejectedProposal(proposalOrdinal, profile, targetSpecificity, targetNovelty,
                        List.of(GeneratorReasonCode.WEIGHT_SUM_OVERFLOW), evaluated, diagnostics);
            }
            GeneratorConcept concept = context.catalog().conceptByCode(chosen.conceptCode()).orElseThrow();
            requirements.add(WorkRequirement.random(position, concept, chosen));
        }

        requirements.sort(Comparator.comparingInt(WorkRequirement::position));
        Set<GeneratorReasonCode> diagnostics = diagnosticSet(plan.diagnostics());
        addManualDiagnostics(requirements, diagnostics, context);
        List<GeneratorReasonCode> hardReasons = hardReasons(requirements, profile, context);
        if (!hardReasons.isEmpty()) {
            diagnostics.addAll(hardReasons);
            return new RejectedProposal(proposalOrdinal, profile, targetSpecificity, targetNovelty,
                    hardReasons, allEvaluations, diagnostics);
        }

        CandidateEvaluation evaluation = evaluateCandidate(requirements, profile, targetNovelty, context, diagnostics);
        List<RequirementSnapshot> snapshots = requirements.stream().map(WorkRequirement::snapshot).toList();
        return new AcceptedProposal(proposalOrdinal, profile, targetSpecificity, targetNovelty, snapshots,
                evaluation, signature(requirements, context), diagnostics);
    }

    private void validateConfigurationIdentity(GenerationContext context) {
        if (!context.configuration().equals(configuration)) {
            throw new IllegalArgumentException(GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID.name()
                    + ": context configuration differs from engine configuration");
        }
    }

    private ProfileSlot requiredSlot(CandidateProfile profile, List<WorkRequirement> requirements,
                                     int remainingRandomSlots, GenerationContext context) {
        ProfileMatcher.Match match = ProfileMatcher.match(profile, roleRequirements(requirements), configuration);
        if (!match.unfilledSlots().isEmpty()) {
            return match.unfilledSlots().getFirst();
        }
        int anchorRequirements = anchorRequirementCount(requirements);
        int anchorBreadth = anchorRoles(requirements).size();
        if (anchorRequirements < 2 || (anchorBreadth < 2 && remainingRandomSlots <= 2 - anchorBreadth)) {
            return ProfileSlot.ANCHOR_1;
        }
        return null;
    }

    private WeightEvaluation evaluateWeight(GeneratorConcept concept, Specificity neededSpecificity,
                                              ProfileSlot requiredSlot, List<WorkRequirement> selected,
                                              NoveltyBand targetBand, GenerationContext context) {
        Set<GeneratorReasonCode> reasons = EnumSet.noneOf(GeneratorReasonCode.class);
        if (!concept.active()) reasons.add(GeneratorReasonCode.CONCEPT_INACTIVE);
        if (!concept.randomDrawEnabled()) reasons.add(GeneratorReasonCode.RANDOM_DRAW_DISABLED);
        if (concept.functionalRoles().isEmpty()) reasons.add(GeneratorReasonCode.FUNCTIONAL_ROLE_MISSING);
        if (concept.noveltyLevel() == null) reasons.add(GeneratorReasonCode.NOVELTY_MISSING);
        if (concept.specificity() != neededSpecificity) reasons.add(GeneratorReasonCode.PROFILE_SLOT_INELIGIBLE);
        if (requiredSlot != null && !ProfileMatcher.supports(requiredSlot, concept.functionalRoles(), configuration)) {
            reasons.add(GeneratorReasonCode.PROFILE_SLOT_INELIGIBLE);
        }
        if (!concept.availabilityByParticipant().keySet().containsAll(context.catalog().activeParticipantCodes())) {
            reasons.add(GeneratorReasonCode.AVAILABILITY_MISSING);
        }
        if (concept.availabilityByParticipant().values().stream().anyMatch(value -> value == Availability.UNAVAILABLE)) {
            reasons.add(GeneratorReasonCode.AVAILABILITY_UNAVAILABLE);
        }
        if (context.rerollBlockedConceptCodes().contains(concept.code())) {
            reasons.add(GeneratorReasonCode.REROLL_EXACT_BLOCKED);
        }
        int distance = exactHistoryDistance(concept.code(), context);
        BigDecimal cooldownFactor = cooldownFactor(distance, configuration);
        if (cooldownFactor.signum() == 0) reasons.add(GeneratorReasonCode.EXACT_COOLDOWN_BLOCKED);
        if (context.exclusionDecision() instanceof AttemptExclusionDecision.Selected selectedExclusion
                && selectedExclusion.rule().expandedTargetCodes().contains(concept.code())) {
            reasons.add(GeneratorReasonCode.EXCLUSION_TARGET_BLOCKED);
        }
        for (WorkRequirement existing : selected) {
            if (existing.concept() != null && existing.concept().code().equals(concept.code())) {
                reasons.add(existing.source() == RequirementSource.MANUAL
                        ? GeneratorReasonCode.RANDOM_MANUAL_DUPLICATE : GeneratorReasonCode.RANDOM_CONCEPT_DUPLICATE);
            }
            if (existing.concept() != null && related(existing.concept(), concept)) {
                reasons.add(GeneratorReasonCode.REFINEMENT_REDUNDANCY);
            }
        }
        if (concept.noveltyLevel() != null && !noveltyAdditionAllowed(selected, concept, configuration)) {
            reasons.add(noveltyLimitReason(selected, concept, configuration));
        }

        Availability worst = worstAvailability(concept, context);
        BigDecimal availabilityFactor = worst == null ? BigDecimal.ZERO
                : configuration.availabilityFactors().get(worst);
        BigDecimal noveltyFactor = concept.noveltyLevel() == null ? BigDecimal.ZERO
                : configuration.novelty().targetFactors().get(targetBand).get(concept.noveltyLevel());
        if (context.noveltyCadence() == NoveltyCadence.RECOVERY && concept.noveltyLevel() != null
                && concept.noveltyLevel() == 5 && previousVisibleContainsLevelFive(context)) {
            noveltyFactor = BigDecimal.ZERO;
        }
        BigDecimal effective = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        if (reasons.isEmpty()) {
            effective = scaled(concept.baseDrawWeight()).multiply(scaled(concept.seasonMultiplier()))
                    .setScale(SCALE, ROUNDING).multiply(scaled(availabilityFactor)).setScale(SCALE, ROUNDING)
                    .multiply(scaled(cooldownFactor)).setScale(SCALE, ROUNDING)
                    .multiply(scaled(noveltyFactor)).setScale(SCALE, ROUNDING);
        }
        long quantized = effective.multiply(BigDecimal.valueOf(configuration.weightQuantization()))
                .setScale(0, ROUNDING).longValueExact();
        if (reasons.isEmpty() && quantized == 0) {
            reasons.add(GeneratorReasonCode.EFFECTIVE_WEIGHT_ROUNDED_TO_ZERO);
        }
        return new WeightEvaluation(concept.code(), concept.baseDrawWeight(), concept.seasonMultiplier(),
                availabilityFactor, cooldownFactor, noveltyFactor, effective, quantized, reasons);
    }

    private <T> T draw(Map<T, BigDecimal> weights, GenerationContext context, long ordinal,
                       SeedDerivation.Purpose purpose) {
        List<Map.Entry<T, BigDecimal>> ordered = weights.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(canonicalComparator())).toList();
        long sum = 0;
        List<Long> units = new ArrayList<>();
        for (Map.Entry<T, BigDecimal> entry : ordered) {
            long unit = entry.getValue().multiply(BigDecimal.valueOf(configuration.weightQuantization()))
                    .setScale(0, ROUNDING).longValueExact();
            units.add(unit);
            sum = Math.addExact(sum, unit);
        }
        long seed = SeedDerivation.derive(configuration.generatorVersion(), context.attemptSeed(),
                SeedDerivation.batchScope(context.batchNumber()), purpose, ordinal);
        long ticket = new SplitMix64(seed).nextLong(sum);
        for (int index = 0; index < ordered.size(); index++) {
            if (ticket < units.get(index)) return ordered.get(index).getKey();
            ticket -= units.get(index);
        }
        throw new IllegalStateException("Weighted selection did not resolve a target");
    }

    private WeightEvaluation choose(List<WeightEvaluation> weights, GenerationContext context, long ordinal,
                                    SeedDerivation.Purpose purpose) {
        long sum = 0;
        for (WeightEvaluation weight : weights) sum = Math.addExact(sum, weight.quantizedWeight());
        long seed = SeedDerivation.derive(configuration.generatorVersion(), context.attemptSeed(),
                SeedDerivation.batchScope(context.batchNumber()), purpose, ordinal);
        long ticket = new SplitMix64(seed).nextLong(sum);
        for (WeightEvaluation weight : weights) {
            if (ticket < weight.quantizedWeight()) return weight;
            ticket -= weight.quantizedWeight();
        }
        throw new IllegalStateException("Weighted concept selection did not resolve a concept");
    }

    private List<GeneratorReasonCode> hardReasons(List<WorkRequirement> requirements, CandidateProfile profile,
                                                   GenerationContext context) {
        Set<GeneratorReasonCode> reasons = EnumSet.noneOf(GeneratorReasonCode.class);
        if (requirements.size() != 4) reasons.add(GeneratorReasonCode.REQUIREMENT_COUNT_INVALID);
        if (requirements.stream().filter(r -> r.specificity() == RequirementSpecificity.SPECIFIC).count() < 2) {
            reasons.add(GeneratorReasonCode.SPECIFIC_REQUIREMENT_MINIMUM_MISSED);
        }
        List<WorkRequirement> random = requirements.stream().filter(r -> r.source() == RequirementSource.RANDOM).toList();
        if (random.stream().map(r -> r.concept().code()).distinct().count() != random.size()) {
            reasons.add(GeneratorReasonCode.RANDOM_CONCEPT_DUPLICATE);
        }
        for (int first = 0; first < requirements.size(); first++) {
            for (int second = first + 1; second < requirements.size(); second++) {
                WorkRequirement left = requirements.get(first);
                WorkRequirement right = requirements.get(second);
                if (left.concept() == null || right.concept() == null
                        || (left.source() == RequirementSource.MANUAL && right.source() == RequirementSource.MANUAL)) continue;
                if (left.concept().code().equals(right.concept().code())) {
                    reasons.add(left.source() == RequirementSource.MANUAL || right.source() == RequirementSource.MANUAL
                            ? GeneratorReasonCode.RANDOM_MANUAL_DUPLICATE : GeneratorReasonCode.RANDOM_CONCEPT_DUPLICATE);
                } else if (related(left.concept(), right.concept())) {
                    reasons.add(GeneratorReasonCode.REFINEMENT_REDUNDANCY);
                }
            }
        }
        ProfileMatcher.Match match = ProfileMatcher.match(profile, roleRequirements(requirements), configuration);
        if (!match.complete()) reasons.add(GeneratorReasonCode.PROFILE_UNSATISFIED);
        int anchors = anchorRequirementCount(requirements);
        if (anchors < 2) reasons.add(GeneratorReasonCode.ANCHOR_REQUIREMENT_MINIMUM_MISSED);
        if (anchorRoles(requirements).size() < 2) reasons.add(GeneratorReasonCode.ANCHOR_ROLE_BREADTH_MISSED);
        if (requirements.size() - anchors > 2) reasons.add(GeneratorReasonCode.NON_ANCHOR_REQUIREMENT_MAX_EXCEEDED);

        NoveltyStats stats = noveltyStats(requirements, configuration);
        NoveltyStats manualStats = noveltyStats(requirements.stream()
                .filter(r -> r.source() == RequirementSource.MANUAL).toList(), configuration);
        boolean forced = exceeds(manualStats, configuration);
        if (!forced) {
            if (stats.fives() > configuration.novelty().levelFiveCap())
                reasons.add(GeneratorReasonCode.NOVELTY_LEVEL_FIVE_MAX_EXCEEDED);
            if (stats.highs() > configuration.novelty().highLevelCap())
                reasons.add(GeneratorReasonCode.NOVELTY_HIGH_MAX_EXCEEDED);
            if (stats.load() > configuration.novelty().loadCap())
                reasons.add(GeneratorReasonCode.NOVELTY_LOAD_MAX_EXCEEDED);
        } else if (requirements.stream().filter(r -> r.source() == RequirementSource.RANDOM)
                .anyMatch(r -> r.concept().noveltyLevel() >= 4)) {
            reasons.add(GeneratorReasonCode.NOVELTY_HIGH_MAX_EXCEEDED);
        }
        if (context.exclusionDecision() instanceof AttemptExclusionDecision.Selected selected
                && requirements.stream().filter(r -> r.concept() != null)
                .anyMatch(r -> selected.rule().expandedTargetCodes().contains(r.concept().code()))) {
            reasons.add(GeneratorReasonCode.CANDIDATE_EXCLUSION_CONFLICT);
        }
        return reasons.stream().sorted().toList();
    }

    private CandidateEvaluation evaluateCandidate(List<WorkRequirement> requirements, CandidateProfile profile,
                                                   NoveltyBand target, GenerationContext context,
                                                   Set<GeneratorReasonCode> diagnostics) {
        Map<ScoreComponent, BigDecimal> scores = new EnumMap<>(ScoreComponent.class);
        Set<GeneratorReasonCode> reasons = EnumSet.noneOf(GeneratorReasonCode.class);
        int anchors = anchorRequirementCount(requirements);
        int anchorBreadth = anchorRoles(requirements).size();
        long multiRole = requirements.stream().filter(r -> r.concept() != null
                && r.concept().functionalRoles().size() > 1).count();
        int structural = clamp(55 + (anchors - 2) * 12 + (anchorBreadth - 2) * 8 + (int) multiRole * 4);
        scores.put(ScoreComponent.STRUCTURAL_VIABILITY, decimal(structural));
        if (structural >= 75) reasons.add(GeneratorReasonCode.STRONG_STRUCTURE);

        Set<String> allRoles = new HashSet<>();
        requirements.stream().filter(r -> r.concept() != null)
                .forEach(r -> allRoles.addAll(r.concept().functionalRoles()));
        int roleScore = clamp(25 + allRoles.size() * 9
                + (allRoles.stream().anyMatch(configuration.supportRoles()::contains) ? 12 : 0)
                + (allRoles.stream().anyMatch(configuration.flavorRoles()::contains) ? 8 : 0));
        scores.put(ScoreComponent.ROLE_COMPLEMENTARITY, decimal(roleScore));
        if (roleScore >= 70) reasons.add(GeneratorReasonCode.ROLE_COMPLEMENTARY);
        else reasons.add(GeneratorReasonCode.LOW_ROLE_BREADTH);

        NoveltyStats noveltyStats = noveltyStats(requirements, configuration);
        boolean forced = manualNoveltyForced(requirements, configuration);
        NoveltyBand actualBand = classifyNovelty(noveltyStats.load(), noveltyStats.fives(), noveltyStats.highs(), forced);
        int bandDistance = Math.abs(actualBand.ordinal() - target.ordinal());
        int semanticVariety = semanticVariety(requirements);
        int creative = clamp(35 + semanticVariety * 8 + (actualBand == NoveltyBand.BALANCED ? 15 : 8));
        scores.put(ScoreComponent.CREATIVE_TENSION, decimal(creative));
        if (creative >= 65) reasons.add(GeneratorReasonCode.CREATIVE_TENSION_PRESENT);

        int openCount = (int) requirements.stream().filter(r -> r.specificity() == RequirementSpecificity.OPEN).count();
        int unclassified = (int) requirements.stream()
                .filter(r -> r.specificity() == RequirementSpecificity.UNCLASSIFIED).count();
        int openness = clamp(25 + openCount * 24 + (int) multiRole * 8 - (openCount == 0 ? 10 : 0));
        scores.put(ScoreComponent.OPENNESS_NON_TRIVIALITY, decimal(openness));
        if (openness < 45) reasons.add(GeneratorReasonCode.STANDARD_TEMPLATE_RISK);

        int noveltyFit = bandDistance == 0 ? 100 : bandDistance == 1 ? 70 : 25;
        noveltyFit = clamp(noveltyFit - unclassified * 10);
        scores.put(ScoreComponent.NOVELTY_TARGET_FIT, decimal(noveltyFit));
        reasons.add(bandDistance == 0 ? GeneratorReasonCode.NOVELTY_TARGET_MATCH
                : GeneratorReasonCode.NOVELTY_TARGET_MISMATCH);

        List<Availability> randomAvailability = requirements.stream().filter(r -> r.source() == RequirementSource.RANDOM)
                .map(r -> worstAvailability(r.concept(), context)).toList();
        int availability = randomAvailability.isEmpty() ? 100 : (int) Math.round(randomAvailability.stream()
                .mapToInt(value -> value == Availability.EASY ? 100 : value == Availability.PLANNED ? 65 : 20)
                .average().orElse(100));
        scores.put(ScoreComponent.AVAILABILITY_LOAD, decimal(availability));
        if (randomAvailability.contains(Availability.PLANNED)) reasons.add(GeneratorReasonCode.PLANNED_AVAILABILITY_LOAD);
        if (randomAvailability.contains(Availability.DIFFICULT)) reasons.add(GeneratorReasonCode.DIFFICULT_AVAILABILITY_LOAD);

        int freshness = historyFreshness(requirements, profile, context, reasons);
        scores.put(ScoreComponent.HISTORY_FRESHNESS, decimal(freshness));

        BigDecimal confidence = dataConfidence(requirements, context);
        scores.put(ScoreComponent.DATA_CONFIDENCE, confidence);
        if (confidence.compareTo(new BigDecimal("75")) < 0) reasons.add(GeneratorReasonCode.LOW_PROPERTY_CONFIDENCE);

        int culinary = culinaryBalance(requirements, reasons);
        scores.put(ScoreComponent.KNOWN_CULINARY_LOAD_BALANCE, decimal(culinary));
        if (unclassified > 0) reasons.add(GeneratorReasonCode.UNCLASSIFIED_MANUAL_REQUIREMENT);
        if (forced) reasons.add(GeneratorReasonCode.MANUAL_NOVELTY_FORCED);

        BigDecimal total = BigDecimal.ZERO;
        for (ScoreComponent component : ScoreComponent.values()) {
            total = total.add(scores.get(component).multiply(configuration.scoreWeights().get(component)));
        }
        total = total.setScale(2, ROUNDING);
        reasons.addAll(diagnostics);
        ProfileMatcher.Match matching = ProfileMatcher.match(profile, roleRequirements(requirements), configuration);
        return new CandidateEvaluation(scores, total, confidence, actualBand, noveltyStats.load(),
                matching.assignments().stream().map(ProfileMatcher.SlotAssignment::diagnostic).toList(), reasons);
    }

    static NoveltyBand classifyNovelty(int load, int fives, int highs, boolean forced) {
        if (forced || load >= 8 || fives > 0 || highs > 1) return NoveltyBand.ADVENTUROUS;
        if (load >= 4 || highs == 1) return NoveltyBand.BALANCED;
        return NoveltyBand.FAMILIAR;
    }

    private int semanticVariety(List<WorkRequirement> requirements) {
        List<GeneratorConcept> concepts = requirements.stream().map(WorkRequirement::concept)
                .filter(java.util.Objects::nonNull).toList();
        int varied = 0;
        for (int first = 0; first < concepts.size(); first++) {
            for (int second = first + 1; second < concepts.size(); second++) {
                Set<String> shared = new HashSet<>(concepts.get(first).transitiveAncestorCodes());
                shared.retainAll(concepts.get(second).transitiveAncestorCodes());
                if (shared.isEmpty()) varied++;
            }
        }
        return varied;
    }

    private int historyFreshness(List<WorkRequirement> requirements, CandidateProfile profile,
                                 GenerationContext context, Set<GeneratorReasonCode> reasons) {
        int penalty = 0;
        Set<String> candidateAncestors = new HashSet<>();
        Set<String> candidateFlags = new HashSet<>();
        requirements.stream().filter(r -> r.concept() != null).forEach(r -> {
            candidateAncestors.addAll(r.concept().transitiveAncestorCodes());
            candidateFlags.addAll(r.concept().culinaryFlags());
        });
        for (VisibleChallenge challenge : context.visibleHistory().challengesNewestFirst().stream().limit(4).toList()) {
            Set<String> historyAncestors = new HashSet<>();
            Set<String> historyFlags = new HashSet<>();
            challenge.requirements().forEach(requirement -> {
                historyAncestors.addAll(requirement.ancestorCodes());
                historyFlags.addAll(requirement.flags());
            });
            Set<String> commonAncestors = new HashSet<>(candidateAncestors);
            commonAncestors.retainAll(historyAncestors);
            Set<String> commonFlags = new HashSet<>(candidateFlags);
            commonFlags.retainAll(historyFlags);
            penalty += Math.min(12, commonAncestors.size() * 2) + Math.min(8, commonFlags.size() * 2);
            if (challenge.profile() == profile) penalty += 4;
        }
        if (penalty > 0) reasons.add(GeneratorReasonCode.RECENT_SEMANTIC_FAMILY);
        return clamp(100 - penalty);
    }

    private BigDecimal dataConfidence(List<WorkRequirement> requirements, GenerationContext context) {
        Set<String> dimensionCodes = new HashSet<>();
        context.catalog().concepts().forEach(concept -> dimensionCodes.addAll(concept.culinaryDimensions().keySet()));
        int concepts = (int) requirements.stream().filter(r -> r.concept() != null).count();
        int known = requirements.stream().filter(r -> r.concept() != null)
                .mapToInt(r -> r.concept().culinaryDimensions().size()).sum();
        int optionalCapacity = Math.max(1, concepts * Math.max(1, dimensionCodes.size()));
        int unclassified = (int) requirements.stream().filter(r -> r.concept() == null).count();
        BigDecimal optional = BigDecimal.valueOf(known).multiply(new BigDecimal("30"))
                .divide(BigDecimal.valueOf(optionalCapacity), 2, ROUNDING);
        return BigDecimal.valueOf(70 - unclassified * 10).add(optional).max(BigDecimal.ZERO)
                .min(new BigDecimal("100")).setScale(2, ROUNDING);
    }

    private int culinaryBalance(List<WorkRequirement> requirements, Set<GeneratorReasonCode> reasons) {
        Map<String, Integer> highPerDimension = new HashMap<>();
        int strongFlags = 0;
        boolean hasComparable = false;
        for (WorkRequirement requirement : requirements) {
            if (requirement.concept() == null) continue;
            for (Map.Entry<String, Integer> entry : requirement.concept().culinaryDimensions().entrySet()) {
                hasComparable = true;
                if (entry.getValue() >= 4) highPerDimension.merge(entry.getKey(), 1, Integer::sum);
            }
            strongFlags += (int) requirement.concept().culinaryFlags().stream().filter(STRONG_FLAGS::contains).count();
        }
        int stacks = highPerDimension.values().stream().mapToInt(value -> Math.max(0, value - 1)).sum()
                + Math.max(0, strongFlags - 1);
        if (stacks > 0) reasons.add(GeneratorReasonCode.KNOWN_INTENSITY_STACKING);
        return hasComparable ? clamp(90 - stacks * 18) : 70;
    }

    private void addManualDiagnostics(List<WorkRequirement> requirements, Set<GeneratorReasonCode> diagnostics,
                                      GenerationContext context) {
        if (requirements.stream().anyMatch(r -> r.specificity() == RequirementSpecificity.UNCLASSIFIED)) {
            diagnostics.add(GeneratorReasonCode.UNCLASSIFIED_MANUAL_REQUIREMENT);
        }
        List<WorkRequirement> manuals = requirements.stream().filter(r -> r.source() == RequirementSource.MANUAL
                && r.concept() != null).toList();
        if (manuals.size() == 2 && (manuals.get(0).concept().code().equals(manuals.get(1).concept().code())
                || related(manuals.get(0).concept(), manuals.get(1).concept()))) {
            diagnostics.add(GeneratorReasonCode.MANUAL_REQUIREMENT_REDUNDANCY);
        }
        if (manualNoveltyForced(requirements, context.configuration())) {
            diagnostics.add(GeneratorReasonCode.MANUAL_NOVELTY_FORCED);
        }
    }

    private boolean noveltyAdditionAllowed(List<WorkRequirement> selected, GeneratorConcept addition,
                                           GeneratorConfiguration config) {
        if (addition.noveltyLevel() == null) return false;
        NoveltyStats manual = noveltyStats(selected.stream()
                .filter(r -> r.source() == RequirementSource.MANUAL).toList(), config);
        if (exceeds(manual, config)) return addition.noveltyLevel() < 4;
        List<WorkRequirement> withAddition = new ArrayList<>(selected);
        withAddition.add(WorkRequirement.random(0, addition, null));
        return !exceeds(noveltyStats(withAddition, config), config);
    }

    private GeneratorReasonCode noveltyLimitReason(List<WorkRequirement> selected, GeneratorConcept addition,
                                                    GeneratorConfiguration config) {
        List<WorkRequirement> with = new ArrayList<>(selected);
        with.add(WorkRequirement.random(0, addition, null));
        NoveltyStats stats = noveltyStats(with, config);
        if (stats.fives() > config.novelty().levelFiveCap()) return GeneratorReasonCode.NOVELTY_LEVEL_FIVE_MAX_EXCEEDED;
        if (stats.highs() > config.novelty().highLevelCap()) return GeneratorReasonCode.NOVELTY_HIGH_MAX_EXCEEDED;
        return GeneratorReasonCode.NOVELTY_LOAD_MAX_EXCEEDED;
    }

    private static NoveltyStats noveltyStats(List<WorkRequirement> requirements, GeneratorConfiguration config) {
        int load = 0;
        int fives = 0;
        int highs = 0;
        for (WorkRequirement requirement : requirements) {
            if (requirement.concept() == null || requirement.concept().noveltyLevel() == null) continue;
            int level = requirement.concept().noveltyLevel();
            load += config.novelty().loadPoints().get(level);
            if (level == 5) fives++;
            if (level >= 4) highs++;
        }
        return new NoveltyStats(load, fives, highs);
    }

    private static boolean manualNoveltyForced(List<WorkRequirement> requirements, GeneratorConfiguration config) {
        return exceeds(noveltyStats(requirements.stream()
                .filter(r -> r.source() == RequirementSource.MANUAL).toList(), config), config);
    }

    private static boolean exceeds(NoveltyStats stats, GeneratorConfiguration config) {
        return stats.fives() > config.novelty().levelFiveCap()
                || stats.highs() > config.novelty().highLevelCap()
                || stats.load() > config.novelty().loadCap();
    }

    private int anchorRequirementCount(List<WorkRequirement> requirements) {
        return (int) requirements.stream().filter(r -> r.concept() != null
                && r.concept().functionalRoles().stream().anyMatch(configuration.anchorRoles()::contains)).count();
    }

    private Set<String> anchorRoles(List<WorkRequirement> requirements) {
        Set<String> roles = new HashSet<>();
        requirements.stream().filter(r -> r.concept() != null).forEach(requirement -> requirement.concept()
                .functionalRoles().stream().filter(configuration.anchorRoles()::contains).forEach(roles::add));
        return roles;
    }

    private static List<ProfileMatcher.RoleRequirement> roleRequirements(List<WorkRequirement> requirements) {
        return requirements.stream().filter(r -> r.concept() != null)
                .map(r -> new ProfileMatcher.RoleRequirement(r.concept().code(), r.concept().id(),
                        r.concept().functionalRoles())).toList();
    }

    private static boolean related(GeneratorConcept first, GeneratorConcept second) {
        return first.transitiveAncestorCodes().contains(second.code())
                || first.transitiveDescendantCodes().contains(second.code())
                || second.transitiveAncestorCodes().contains(first.code())
                || second.transitiveDescendantCodes().contains(first.code());
    }

    private static Availability worstAvailability(GeneratorConcept concept, GenerationContext context) {
        Availability worst = null;
        for (String participant : context.catalog().activeParticipantCodes()) {
            Availability value = concept.availabilityByParticipant().get(participant);
            if (value == null) return null;
            if (worst == null || value.ordinal() > worst.ordinal()) worst = value;
        }
        return worst;
    }

    private static int exactHistoryDistance(String code, GenerationContext context) {
        for (int index = 0; index < context.visibleHistory().challengesNewestFirst().size(); index++) {
            if (context.visibleHistory().challengesNewestFirst().get(index).requirements().stream()
                    .map(VisibleRequirement::conceptCode).anyMatch(code::equals)) return index + 1;
        }
        return Integer.MAX_VALUE;
    }

    private static BigDecimal cooldownFactor(int distance, GeneratorConfiguration config) {
        if (distance <= config.cooldown().hardWindow()) return BigDecimal.ZERO;
        if (distance <= config.cooldown().firstDecayEnd()) return config.cooldown().firstDecayFactor();
        if (distance <= config.cooldown().secondDecayEnd()) return config.cooldown().secondDecayFactor();
        if (distance <= config.cooldown().thirdDecayEnd()) return config.cooldown().thirdDecayFactor();
        return BigDecimal.ONE;
    }

    private static boolean previousVisibleContainsLevelFive(GenerationContext context) {
        return !context.visibleHistory().challengesNewestFirst().isEmpty()
                && context.visibleHistory().challengesNewestFirst().getFirst().requirements().stream()
                .anyMatch(requirement -> Integer.valueOf(5).equals(requirement.noveltyLevel()));
    }

    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private static Set<GeneratorReasonCode> diagnosticSet(Set<GeneratorReasonCode> source) {
        Set<GeneratorReasonCode> result = EnumSet.noneOf(GeneratorReasonCode.class);
        result.addAll(source);
        return result;
    }

    private static BigDecimal decimal(int value) {
        return BigDecimal.valueOf(value).setScale(2, ROUNDING);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String signature(List<WorkRequirement> requirements, GenerationContext context) {
        List<String> identities = requirements.stream().map(requirement -> {
            if (requirement.source() == RequirementSource.RANDOM) {
                return String.join("\0", "R", requirement.concept().code(), requirement.specificity().name());
            }
            String normalizedText = Normalizer.normalize(
                    requirement.displayText().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
            String matchIdentity = requirement.concept() == null
                    ? "UNMATCHED" : "MATCH:" + requirement.concept().code();
            return String.join("\0", "M", normalizedText, matchIdentity, requirement.specificity().name());
        }).sorted().toList();
        String exclusion = context.exclusionDecision() instanceof AttemptExclusionDecision.Selected selected
                ? selected.rule().code() : "NONE";
        String payload = String.join("\0\0", identities) + "\0\0EXCLUSION\0" + exclusion;
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> Comparator<T> canonicalComparator() {
        return (left, right) -> {
            if (left instanceof Enum<?> a && right instanceof Enum<?> b) return a.name().compareTo(b.name());
            return ((Comparable) left).compareTo(right);
        };
    }

    private record NoveltyStats(int load, int fives, int highs) { }

    private record WorkRequirement(int position, RequirementSource source, String displayText,
                                   RequirementSpecificity specificity, GeneratorConcept concept,
                                   WeightEvaluation weight) {
        static WorkRequirement manual(GenerationContext.ManualRequirement manual) {
            RequirementSpecificity specificity = manual.matchedConcept() == null
                    ? RequirementSpecificity.UNCLASSIFIED
                    : RequirementSpecificity.valueOf(manual.matchedConcept().specificity().name());
            return new WorkRequirement(manual.position(), RequirementSource.MANUAL, manual.displayText(), specificity,
                    manual.matchedConcept(), null);
        }

        static WorkRequirement random(int position, GeneratorConcept concept, WeightEvaluation weight) {
            return new WorkRequirement(position, RequirementSource.RANDOM, concept.displayName(),
                    RequirementSpecificity.valueOf(concept.specificity().name()), concept, weight);
        }

        RequirementSnapshot snapshot() {
            return new RequirementSnapshot(position, source, displayText, specificity, concept, weight);
        }
    }
}
