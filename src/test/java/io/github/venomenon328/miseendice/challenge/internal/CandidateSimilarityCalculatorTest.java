package io.github.venomenon328.miseendice.challenge.internal;

import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.bd;
import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.candidate;
import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.catalog;
import static io.github.venomenon328.miseendice.challenge.internal.CandidateSetTestData.concept;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.Comparability;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateRestriction;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.SimilarityComponent;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CandidateSimilarityCalculatorTest {
    private final CandidateSimilarityCalculator calculator = new CandidateSimilarityCalculator();

    @Test
    void calculatesEveryMainComponentWithScaleTwelveAndStableOverlapReasons() {
        List<GeneratorConcept> concepts = List.of(
                concept(1, "A1", Specificity.SPECIFIC, 1, Set.of("VEGETABLE"), Set.of("FERMENTED"),
                        Map.of("DOMINANCE", 5), Set.of("NARROW", "ROOT"), Set.of(), Availability.EASY),
                concept(2, "A2", Specificity.SPECIFIC, 1, Set.of("VEGETABLE"), Set.of(), Map.of(),
                        Set.of("NARROW", "ROOT"), Set.of(), Availability.EASY),
                concept(3, "A3", Specificity.OPEN, 1, Set.of("ACID"), Set.of(), Map.of(),
                        Set.of("LEFT", "ROOT"), Set.of(), Availability.EASY),
                concept(4, "A4", Specificity.OPEN, 1, Set.of("STARCH"), Set.of(), Map.of(),
                        Set.of("LEFT", "ROOT"), Set.of(), Availability.EASY),
                concept(5, "B1", Specificity.SPECIFIC, 5, Set.of("VEGETABLE"), Set.of("FERMENTED", "SMOKED"),
                        Map.of("DOMINANCE", 3), Set.of("NARROW", "ROOT"), Set.of(), Availability.PLANNED),
                concept(6, "B2", Specificity.SPECIFIC, 2, Set.of("ANIMAL_PROTEIN"), Set.of(), Map.of(),
                        Set.of("NARROW", "ROOT"), Set.of(), Availability.PLANNED),
                concept(7, "B3", Specificity.SPECIFIC, 2, Set.of("FAT"), Set.of(), Map.of(),
                        Set.of("RIGHT", "ROOT"), Set.of(), Availability.PLANNED),
                concept(8, "B4", Specificity.SPECIFIC, 2, Set.of("SEASONING"), Set.of(), Map.of(),
                        Set.of("RIGHT", "ROOT"), Set.of(), Availability.PLANNED));
        var first = candidate("first", CandidateProfile.PROTEIN_PRODUCE, 2, NoveltyBand.FAMILIAR, 0,
                bd("70"), concepts.subList(0, 4));
        var second = candidate("second", CandidateProfile.STARCH_ANCHORED, 4, NoveltyBand.ADVENTUROUS, 11,
                bd("70"), concepts.subList(4, 8));

        var result = calculator.assess(1, first, 2, second, catalog(concepts),
                TestGeneratorConfiguration.defaults(), bd("0.10"));

        assertThat(result.components()).containsOnlyKeys(
                SimilarityComponent.EXACT_RANDOM_CONCEPTS,
                SimilarityComponent.INFORMATIVE_ANCESTORS,
                SimilarityComponent.ROLES_AND_PROFILE,
                SimilarityComponent.SPECIFICITY_MIX,
                SimilarityComponent.NOVELTY,
                SimilarityComponent.AVAILABILITY_LOAD,
                SimilarityComponent.COMPARABLE_PROPERTIES);
        assertThat(result.components().values()).allSatisfy(component -> {
            if (component.value() != null) {
                assertThat(component.value().scale()).isEqualTo(12);
            }
        });
        assertThat(result.components().get(SimilarityComponent.EXACT_RANDOM_CONCEPTS).value())
                .isEqualByComparingTo("0.000000000000");
        assertThat(result.components().get(SimilarityComponent.INFORMATIVE_ANCESTORS).value())
                .isEqualByComparingTo("0.181818181818");
        assertThat(result.components().get(SimilarityComponent.ROLES_AND_PROFILE).value())
                .isEqualByComparingTo("0.128571428571");
        assertThat(result.components().get(SimilarityComponent.SPECIFICITY_MIX).value())
                .isEqualByComparingTo("0.000000000000");
        assertThat(result.components().get(SimilarityComponent.NOVELTY).value())
                .isEqualByComparingTo("0.000000000000");
        assertThat(result.components().get(SimilarityComponent.AVAILABILITY_LOAD).value())
                .isEqualByComparingTo("0.650000000000");
        assertThat(result.components().get(SimilarityComponent.COMPARABLE_PROPERTIES).value())
                .isEqualByComparingTo("0.500000000000");
        assertThat(result.totalSimilarity()).isEqualByComparingTo("0.138149350650");
        assertThat(result.diagnostics()).contains(GeneratorReasonCode.PAIR_ANCESTOR_OVERLAP,
                GeneratorReasonCode.PAIR_SIMILARITY_LIMIT);
    }

    @Test
    void exactRandomConceptComponentUsesSetJaccardAndEmitsOverlapReason() {
        List<GeneratorConcept> concepts = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            concepts.add(concept(index + 1L, "SHARED_" + index, Specificity.SPECIFIC, 2,
                    Set.of(index % 2 == 0 ? "VEGETABLE" : "STARCH"), Set.of(), Map.of(), Set.of(), Set.of(),
                    Availability.EASY));
        }
        var first = candidate("jaccard-left", CandidateProfile.FLEXIBLE_BALANCED, 4, NoveltyBand.FAMILIAR, 2,
                bd("60"), concepts.subList(0, 4));
        var second = candidate("jaccard-right", CandidateProfile.FLEXIBLE_BALANCED, 4, NoveltyBand.FAMILIAR, 2,
                bd("60"), List.of(concepts.get(0), concepts.get(1), concepts.get(4), concepts.get(5)));

        var result = calculator.assess(1, first, 2, second, catalog(concepts),
                TestGeneratorConfiguration.defaults(), null);

        assertThat(result.components().get(SimilarityComponent.EXACT_RANDOM_CONCEPTS).value())
                .isEqualByComparingTo("0.333333333333");
        assertThat(result.diagnostics()).contains(GeneratorReasonCode.PAIR_EXACT_OVERLAP);
    }

    @Test
    void broadZeroWeightRootsAndMissingPropertiesAreNotComparableAndWeightsRedistributeProportionally() {
        List<GeneratorConcept> concepts = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            concepts.add(concept(index + 1L, "C" + index, Specificity.SPECIFIC, 2,
                    Set.of(index < 4 ? "VEGETABLE" : "STARCH"), Set.of(), Map.of(), Set.of("ROOT"), Set.of(),
                    Availability.EASY));
        }
        var first = candidate("left", CandidateProfile.PRODUCE_DUO, 4, NoveltyBand.BALANCED, 4,
                bd("60"), concepts.subList(0, 4));
        var second = candidate("right", CandidateProfile.THREE_ANCHORS, 4, NoveltyBand.BALANCED, 4,
                bd("60"), concepts.subList(4, 8));

        var result = calculator.assess(1, first, 2, second, catalog(concepts),
                TestGeneratorConfiguration.defaults(), null);

        assertThat(result.components().get(SimilarityComponent.INFORMATIVE_ANCESTORS).comparability())
                .isEqualTo(Comparability.NOT_COMPARABLE);
        assertThat(result.components().get(SimilarityComponent.COMPARABLE_PROPERTIES).comparability())
                .isEqualTo(Comparability.NOT_COMPARABLE);
        assertThat(result.renormalizedWeights()).doesNotContainKeys(
                SimilarityComponent.INFORMATIVE_ANCESTORS, SimilarityComponent.COMPARABLE_PROPERTIES);
        assertThat(result.renormalizedWeights().get(SimilarityComponent.EXACT_RANDOM_CONCEPTS))
                .isEqualByComparingTo("0.500000000000");
        assertThat(result.renormalizedWeights().values().stream().reduce(bd("0"), java.math.BigDecimal::add))
                .isEqualByComparingTo("1.000000000000");
    }

    @Test
    void fixedManualRequirementsDoNotEnterRequirementBasedPairFeatures() {
        GeneratorConcept manual = concept(100, "MANUAL", Specificity.SPECIFIC, 5, Set.of("SEASONING"),
                Set.of("FERMENTED"), Map.of("DOMINANCE", 5), Set.of("ROOT"), Set.of(), Availability.DIFFICULT);
        List<GeneratorConcept> random = List.of(
                concept(1, "L1", Specificity.SPECIFIC, 1, Set.of("VEGETABLE"), Set.of(), Map.of(),
                        Set.of(), Set.of(), Availability.EASY),
                concept(2, "L2", Specificity.SPECIFIC, 1, Set.of("ACID"), Set.of(), Map.of(),
                        Set.of(), Set.of(), Availability.EASY),
                concept(3, "L3", Specificity.SPECIFIC, 1, Set.of("STARCH"), Set.of(), Map.of(),
                        Set.of(), Set.of(), Availability.EASY),
                concept(4, "R1", Specificity.SPECIFIC, 1, Set.of("ANIMAL_PROTEIN"), Set.of(), Map.of(),
                        Set.of(), Set.of(), Availability.EASY),
                concept(5, "R2", Specificity.SPECIFIC, 1, Set.of("FAT"), Set.of(), Map.of(),
                        Set.of(), Set.of(), Availability.EASY),
                concept(6, "R3", Specificity.SPECIFIC, 1, Set.of("AROMATIC"), Set.of(), Map.of(),
                        Set.of(), Set.of(), Availability.EASY));
        var first = CandidateSetTestData.withManual("manual-left", CandidateProfile.PRODUCE_DUO,
                NoveltyBand.FAMILIAR, bd("60"), manual, random.subList(0, 3));
        var second = CandidateSetTestData.withManual("manual-right", CandidateProfile.STARCH_ANCHORED,
                NoveltyBand.FAMILIAR, bd("60"), manual, random.subList(3, 6));
        List<GeneratorConcept> catalogConcepts = new ArrayList<>(random);
        catalogConcepts.add(manual);

        var result = calculator.assess(1, first, 2, second, catalog(catalogConcepts),
                TestGeneratorConfiguration.defaults(), null);

        assertThat(result.components().get(SimilarityComponent.EXACT_RANDOM_CONCEPTS).value())
                .isEqualByComparingTo("0.000000000000");
        assertThat(result.components().get(SimilarityComponent.ROLES_AND_PROFILE).value())
                .isEqualByComparingTo("0.000000000000");
        assertThat(result.components().get(SimilarityComponent.COMPARABLE_PROPERTIES).comparability())
                .isEqualTo(Comparability.NOT_COMPARABLE);
        assertThat(result.components().get(SimilarityComponent.AVAILABILITY_LOAD).value())
                .isEqualByComparingTo("1.000000000000");
        assertThat(result.diagnostics()).doesNotContain(GeneratorReasonCode.PAIR_EXACT_OVERLAP);
    }

    @Test
    void generator12MakesCandidateRestrictionsAnExplicitDiversityComponent() {
        List<GeneratorConcept> concepts = List.of(
                concept(1, "A", Specificity.SPECIFIC, 1, Set.of("VEGETABLE"), Set.of(), Map.of(), Set.of(), Set.of(), Availability.EASY),
                concept(2, "B", Specificity.SPECIFIC, 1, Set.of("ACID"), Set.of(), Map.of(), Set.of(), Set.of(), Availability.EASY),
                concept(3, "C", Specificity.SPECIFIC, 1, Set.of("STARCH"), Set.of(), Map.of(), Set.of(), Set.of(), Availability.EASY),
                concept(4, "D", Specificity.SPECIFIC, 1, Set.of("FAT"), Set.of(), Map.of(), Set.of(), Set.of(), Availability.EASY));
        AcceptedProposal base = candidate("same", CandidateProfile.FLEXIBLE_BALANCED, 4, NoveltyBand.BALANCED,
                4, bd("60"), concepts);
        AcceptedProposal first = withRestriction(base, new CandidateRestriction(1L, "NO_A", "No A"));
        AcceptedProposal sameRestriction = withRestriction(base, new CandidateRestriction(1L, "NO_A", "No A"));
        AcceptedProposal otherRestriction = withRestriction(base, new CandidateRestriction(2L, "NO_B", "No B"));
        AcceptedProposal unrestricted = withRestriction(base, CandidateRestriction.none());

        var same = calculator.assess(1, first, 2, sameRestriction, catalog(concepts),
                TestGeneratorConfiguration.candidateRestrictionDefaults(), null);
        var other = calculator.assess(1, first, 2, otherRestriction, catalog(concepts),
                TestGeneratorConfiguration.candidateRestrictionDefaults(), null);
        var neitherRestricted = calculator.assess(1, unrestricted, 2, unrestricted, catalog(concepts),
                TestGeneratorConfiguration.candidateRestrictionDefaults(), null);

        assertThat(same.components()).containsKey(SimilarityComponent.RESTRICTION);
        assertThat(same.components().get(SimilarityComponent.RESTRICTION).value()).isEqualByComparingTo("1.000000000000");
        assertThat(other.components().get(SimilarityComponent.RESTRICTION).value()).isEqualByComparingTo("0.000000000000");
        assertThat(neitherRestricted.components().get(SimilarityComponent.RESTRICTION).value())
                .isEqualByComparingTo("0.000000000000");
        assertThat(other.totalSimilarity()).isLessThan(same.totalSimilarity());
    }

    private static AcceptedProposal withRestriction(AcceptedProposal source, CandidateRestriction restriction) {
        return new AcceptedProposal(source.proposalOrdinal(), source.profile(), source.targetSpecificity(),
                source.targetNoveltyBand(), source.requirements(), source.evaluation(), source.canonicalSignature(),
                source.diagnostics(), restriction);
    }
}
