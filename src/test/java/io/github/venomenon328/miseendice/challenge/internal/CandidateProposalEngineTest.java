package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.ProposalResult;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CandidateProposalEngineTest {
    private GeneratorConfiguration configuration;
    private DefaultCandidateProposalEngine engine;

    @BeforeEach
    void setUp() {
        configuration = TestGeneratorConfiguration.defaults();
        engine = new DefaultCandidateProposalEngine(configuration, "{\"configurationVersion\":\"2026-08-12.1\"}");
    }

    @Test
    void sameContextSeedAndOrdinalProduceExactlyTheSameResult() {
        GenerationContext context = context(catalog(false), List.of(), 812_034L);

        assertThat(engine.propose(context, 42)).isEqualTo(engine.propose(context, 42));
        assertThat(findAccepted(context).evaluation().components()).hasSize(9);
    }

    @Test
    void differentSeedsAndOrdinalsProvideVariationWithoutMutableRandomState() {
        GenerationContext first = context(catalog(false), List.of(), 1L);
        GenerationContext second = context(catalog(false), List.of(), 2L);

        Set<String> signatures = java.util.stream.LongStream.range(0, 100)
                .mapToObj(ordinal -> engine.propose(first, ordinal))
                .filter(AcceptedProposal.class::isInstance)
                .map(AcceptedProposal.class::cast)
                .map(AcceptedProposal::canonicalSignature)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(signatures).hasSizeGreaterThan(1);
        assertThat(findAccepted(first).canonicalSignature()).isNotEqualTo(findAccepted(second).canonicalSignature());
    }

    @Test
    void catalogCollectionOrderCannotInfluenceAProposal() {
        CatalogGeneratorSnapshot canonical = catalog(false);
        List<GeneratorConcept> reversed = new ArrayList<>(canonical.concepts());
        Collections.reverse(reversed);
        CatalogGeneratorSnapshot shuffledInput = new CatalogGeneratorSnapshot(
                canonical.seasonMonth(), List.of("TOBIAS", "GEORGIA"), reversed, canonical.exclusionRules());

        GenerationContext first = context(canonical, List.of(), 99L);
        GenerationContext second = context(shuffledInput, List.of(), 99L);

        assertThat(engine.propose(first, 7)).isEqualTo(engine.propose(second, 7));
    }

    @Test
    void unmatchedManualProjectsImpossibleFourSpecificTargetAndInventsNoMetadata() {
        GenerationContext context = context(catalog(false),
                List.of(new ManualRequirement(1, "use a waffle iron", null)), 41L);

        GenerationPlan plan = engine.validateAndPlan(context);

        assertThat(plan.valid()).isTrue();
        assertThat(plan.specificity().normalizedWeights()).containsOnlyKeys(2, 3);
        assertThat(plan.specificity().setTargets().values()).allMatch(value -> value >= 0 && value <= 12);
        assertThat(plan.specificity().setTargets().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(12);
        assertThat(plan.diagnostics()).contains(GeneratorReasonCode.SPECIFICITY_TARGET_PROJECTED,
                GeneratorReasonCode.UNCLASSIFIED_MANUAL_REQUIREMENT);
    }

    @Test
    void inactiveNonDrawableManualMatchRetainsItsSnapshotAndRemainsAuthoritative() {
        CatalogGeneratorSnapshot catalog = catalog(true);
        GeneratorConcept legacy = catalog.conceptByCode("LEGACY_PROTEIN").orElseThrow();
        GenerationContext context = context(catalog,
                List.of(new ManualRequirement(1, "Legacy protein", legacy)), 123L);

        AcceptedProposal proposal = findAccepted(context);

        assertThat(proposal.requirements()).anySatisfy(requirement -> {
            assertThat(requirement.source()).isEqualTo(RequirementSource.MANUAL);
            assertThat(requirement.concept()).isEqualTo(legacy);
            assertThat(requirement.concept().active()).isFalse();
            assertThat(requirement.concept().randomDrawEnabled()).isFalse();
        });
    }

    @Test
    void matchedManualTextIsPartOfCanonicalCandidateIdentity() {
        CatalogGeneratorSnapshot catalog = catalog(false);
        GeneratorConcept matched = catalog.conceptByCode("ANIMAL_A").orElseThrow();
        GenerationContext first = context(catalog,
                List.of(new ManualRequirement(1, "Animal protein", matched)), 321L);
        GenerationContext second = context(catalog,
                List.of(new ManualRequirement(1, "Animal protein, served minced", matched)), 321L);

        AcceptedProposal firstProposal = findAccepted(first);
        AcceptedProposal secondProposal = findAccepted(second);

        assertThat(firstProposal.requirements()).filteredOn(r -> r.source() == RequirementSource.RANDOM)
                .extracting(r -> r.concept().code())
                .containsExactlyElementsOf(secondProposal.requirements().stream()
                        .filter(r -> r.source() == RequirementSource.RANDOM)
                        .map(r -> r.concept().code()).toList());
        assertThat(firstProposal.canonicalSignature()).isNotEqualTo(secondProposal.canonicalSignature());
    }

    @Test
    void rejectsManualMatchThatDoesNotEqualTheCatalogSnapshot() {
        CatalogGeneratorSnapshot catalog = catalog(false);
        GeneratorConcept original = catalog.conceptByCode("ANIMAL_A").orElseThrow();
        GeneratorConcept tampered = new GeneratorConcept(
                original.id(), original.code(), original.displayName(), original.active(), original.randomDrawEnabled(),
                original.specificity(), original.baseDrawWeight(), original.noveltyLevel(), Set.of("SEASONING"),
                original.culinaryFlags(), original.culinaryDimensions(), original.availabilityByParticipant(),
                original.seasonMultiplier(), original.directAncestorCodes(), original.directDescendantCodes(),
                original.transitiveAncestorCodes(), original.transitiveDescendantCodes());
        GenerationContext context = context(catalog,
                List.of(new ManualRequirement(1, "Animal protein", tampered)), 82L);

        assertThat(engine.validateAndPlan(context).validationErrors())
                .containsExactly(GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID);
    }

    @Test
    void rejectsSelectedExclusionThatDoesNotEqualTheCatalogSnapshot() {
        CatalogGeneratorSnapshot catalog = catalog(false);
        GeneratorExclusionRule original = catalog.exclusionRules().getFirst();
        GeneratorExclusionRule tampered = new GeneratorExclusionRule(
                original.id(), original.code(), original.displayText(), original.baseDrawWeight(), original.targets(),
                Set.of("ANIMAL_A"));
        GenerationContext context = new GenerationContext(AttemptType.INITIAL, LocalDate.of(2026, 8, 12), 8,
                catalog, VisibleHistorySnapshot.empty(), List.of(), Set.of(), AttemptExclusionDecision.selected(tampered),
                NoveltyCadence.NEUTRAL,
                Map.of(NoveltyBand.FAMILIAR, 3, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 2),
                configuration, 83L, 1);

        assertThat(engine.validateAndPlan(context).validationErrors())
                .containsExactly(GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID);
    }

    @Test
    void projectedProfilesRequireDistinctDrawableRequirements() {
        CatalogGeneratorSnapshot base = catalog(false);
        Set<String> disabledProduce = Set.of("VEGETABLE_B", "FRUIT_A", "VEGETABLE_OPEN", "FRUIT_OPEN");
        List<GeneratorConcept> concepts = base.concepts().stream()
                .map(concept -> disabledProduce.contains(concept.code())
                        ? withRandomDrawEnabled(concept, false) : concept)
                .toList();
        CatalogGeneratorSnapshot thin = new CatalogGeneratorSnapshot(
                base.seasonMonth(), base.activeParticipantCodes(), concepts, base.exclusionRules());

        GenerationPlan plan = engine.validateAndPlan(context(thin, List.of(), 84L));

        assertThat(plan.valid()).isTrue();
        assertThat(plan.profiles().normalizedWeights()).doesNotContainKey(CandidateProfile.PRODUCE_DUO);
        assertThat(plan.diagnostics()).contains(GeneratorReasonCode.PROFILE_TARGET_PROJECTED);
    }

    @Test
    void culinaryDimensionsNeverBecomeHardRulesAndMissingValuesReduceOnlyConfidence() {
        GenerationContext context = context(catalog(false), List.of(), 5L);

        AcceptedProposal accepted = findAccepted(context);

        assertThat(accepted.evaluation().dataConfidence()).isBetween(BigDecimal.ZERO, new BigDecimal("100"));
        assertThat(accepted.evaluation().reasonCodes()).contains(GeneratorReasonCode.LOW_PROPERTY_CONFIDENCE);
    }

    @Test
    void attemptExclusionAndRerollBlockAreAppliedToEveryRandomSlot() {
        CatalogGeneratorSnapshot catalog = catalog(false);
        GeneratorExclusionRule exclusion = catalog.exclusionRules().getFirst();
        GenerationContext excluded = new GenerationContext(AttemptType.REROLL, LocalDate.of(2026, 8, 12), 8,
                catalog, VisibleHistorySnapshot.empty(), List.of(), Set.of("ANIMAL_A"),
                AttemptExclusionDecision.selected(exclusion), NoveltyCadence.NEUTRAL,
                Map.of(NoveltyBand.FAMILIAR, 3, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 2),
                configuration, 77L, 1);

        AcceptedProposal accepted = findAccepted(excluded);

        assertThat(accepted.requirements()).filteredOn(requirement -> requirement.source() == RequirementSource.RANDOM)
                .extracting(requirement -> requirement.concept().code())
                .doesNotContain("ANIMAL_A", "SEASONING_A");
    }

    @Test
    void proposalStreamCoversEveryProfileAndEverySpecificityMix() {
        GenerationContext context = context(catalog(false), List.of(), 871L);
        Set<CandidateProfile> profiles = new java.util.HashSet<>();
        Set<Integer> specificityMixes = new java.util.HashSet<>();

        for (long ordinal = 0; ordinal < 2_000 && (profiles.size() < 5 || specificityMixes.size() < 3); ordinal++) {
            if (engine.propose(context, ordinal) instanceof AcceptedProposal accepted) {
                profiles.add(accepted.profile());
                specificityMixes.add(accepted.targetSpecificity());
            }
        }

        assertThat(profiles).containsExactlyInAnyOrder(CandidateProfile.values());
        assertThat(specificityMixes).containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    void randomCompletionCannotDuplicateOrRefineAMatchedManual() {
        CatalogGeneratorSnapshot base = catalog(false);
        GeneratorConcept parent = base.conceptByCode("VEGETABLE_OPEN").orElseThrow();
        List<GeneratorConcept> relatedConcepts = base.concepts().stream().map(concept ->
                concept.code().equals("VEGETABLE_A")
                        ? new GeneratorConcept(concept.id(), concept.code(), concept.displayName(), concept.active(),
                        concept.randomDrawEnabled(), concept.specificity(), concept.baseDrawWeight(),
                        concept.noveltyLevel(), concept.functionalRoles(), concept.culinaryFlags(),
                        concept.culinaryDimensions(), concept.availabilityByParticipant(), concept.seasonMultiplier(),
                        Set.of(parent.code()), concept.directDescendantCodes(), Set.of(parent.code()),
                        concept.transitiveDescendantCodes())
                        : concept).toList();
        CatalogGeneratorSnapshot related = new CatalogGeneratorSnapshot(8, base.activeParticipantCodes(),
                relatedConcepts, base.exclusionRules());
        GenerationContext context = context(related,
                List.of(new ManualRequirement(1, "Vegetable", parent)), 563L);

        for (long ordinal = 0; ordinal < 500; ordinal++) {
            if (engine.propose(context, ordinal) instanceof AcceptedProposal accepted) {
                assertThat(accepted.requirements()).filteredOn(r -> r.source() == RequirementSource.RANDOM)
                        .extracting(r -> r.concept().code())
                        .doesNotContain("VEGETABLE_OPEN", "VEGETABLE_A");
            }
        }
    }

    private AcceptedProposal findAccepted(GenerationContext context) {
        for (long ordinal = 0; ordinal < 2_000; ordinal++) {
            ProposalResult result = engine.propose(context, ordinal);
            if (result instanceof AcceptedProposal accepted) return accepted;
        }
        throw new AssertionError("Synthetic catalog did not produce an accepted proposal");
    }

    private GenerationContext context(CatalogGeneratorSnapshot catalog, List<ManualRequirement> manuals, long seed) {
        return new GenerationContext(AttemptType.INITIAL, LocalDate.of(2026, 8, 12), 8, catalog,
                VisibleHistorySnapshot.empty(), manuals, Set.of(), AttemptExclusionDecision.none(),
                NoveltyCadence.NEUTRAL,
                Map.of(NoveltyBand.FAMILIAR, 3, NoveltyBand.BALANCED, 7, NoveltyBand.ADVENTUROUS, 2),
                configuration, seed, 1);
    }

    private CatalogGeneratorSnapshot catalog(boolean includeLegacy) {
        List<GeneratorConcept> concepts = new ArrayList<>(List.of(
                concept(1, "ANIMAL_A", Specificity.SPECIFIC, 1, Set.of("ANIMAL_PROTEIN")),
                concept(2, "ANIMAL_B", Specificity.SPECIFIC, 2, Set.of("ANIMAL_PROTEIN", "FAT")),
                concept(3, "PLANT_A", Specificity.SPECIFIC, 2, Set.of("PLANT_PROTEIN")),
                concept(4, "VEGETABLE_A", Specificity.SPECIFIC, 1, Set.of("VEGETABLE")),
                concept(5, "VEGETABLE_B", Specificity.SPECIFIC, 4, Set.of("VEGETABLE", "AROMATIC")),
                concept(6, "FRUIT_A", Specificity.SPECIFIC, 3, Set.of("FRUIT")),
                concept(7, "STARCH_A", Specificity.SPECIFIC, 2, Set.of("STARCH")),
                concept(8, "ANIMAL_OPEN", Specificity.OPEN, 1, Set.of("ANIMAL_PROTEIN")),
                concept(9, "VEGETABLE_OPEN", Specificity.OPEN, 2, Set.of("VEGETABLE")),
                concept(10, "FRUIT_OPEN", Specificity.OPEN, 3, Set.of("FRUIT")),
                concept(11, "STARCH_OPEN", Specificity.OPEN, 1, Set.of("STARCH")),
                concept(12, "ACID_A", Specificity.SPECIFIC, 1, Set.of("ACID")),
                concept(13, "SEASONING_A", Specificity.SPECIFIC, 2, Set.of("SEASONING")),
                concept(14, "AROMATIC_A", Specificity.OPEN, 2, Set.of("AROMATIC"))
        ));
        if (includeLegacy) {
            concepts.add(new GeneratorConcept(15, "LEGACY_PROTEIN", "Legacy protein", false, false,
                    Specificity.SPECIFIC, BigDecimal.ONE, 5, Set.of("ANIMAL_PROTEIN"), Set.of(), Map.of(),
                    availability(), BigDecimal.ONE, Set.of(), Set.of(), Set.of(), Set.of()));
        }
        GeneratorExclusionRule exclusion = new GeneratorExclusionRule(1, "NO_SEASONING", "No seasoning",
                BigDecimal.ONE, List.of(new GeneratorExclusionTarget(13, "SEASONING_A", "Seasoning A", false)),
                Set.of("SEASONING_A"));
        return new CatalogGeneratorSnapshot(8, List.of("GEORGIA", "TOBIAS"), concepts, List.of(exclusion));
    }

    private GeneratorConcept concept(long id, String code, Specificity specificity, int novelty, Set<String> roles) {
        return new GeneratorConcept(id, code, code.replace('_', ' '), true, true, specificity,
                BigDecimal.ONE, novelty, roles, Set.of(), Map.of(), availability(), BigDecimal.ONE,
                Set.of(), Set.of(), Set.of(), Set.of());
    }

    private GeneratorConcept withRandomDrawEnabled(GeneratorConcept concept, boolean enabled) {
        return new GeneratorConcept(
                concept.id(), concept.code(), concept.displayName(), concept.active(), enabled, concept.specificity(),
                concept.baseDrawWeight(), concept.noveltyLevel(), concept.functionalRoles(), concept.culinaryFlags(),
                concept.culinaryDimensions(), concept.availabilityByParticipant(), concept.seasonMultiplier(),
                concept.directAncestorCodes(), concept.directDescendantCodes(), concept.transitiveAncestorCodes(),
                concept.transitiveDescendantCodes());
    }

    private Map<String, Availability> availability() {
        return Map.of("GEORGIA", Availability.EASY, "TOBIAS", Availability.EASY);
    }
}
