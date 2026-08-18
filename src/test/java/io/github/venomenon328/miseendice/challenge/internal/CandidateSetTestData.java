package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.WeightEvaluation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CandidateSetTestData {
    private CandidateSetTestData() {
    }

    static GeneratorConcept concept(long id, String code, Specificity specificity, int novelty,
                                    Set<String> roles, Set<String> flags, Map<String, Integer> dimensions,
                                    Set<String> ancestors, Set<String> descendants, Availability availability) {
        return new GeneratorConcept(id, code, code, true, true, specificity, bd("1.0000"), novelty,
                roles, flags, dimensions, Map.of("GEORGIA", availability, "TOBIAS", availability), bd("1.0000"),
                Set.of(), Set.of(), ancestors, descendants);
    }

    static CatalogGeneratorSnapshot catalog(List<GeneratorConcept> concepts) {
        return new CatalogGeneratorSnapshot(8, List.of("GEORGIA", "TOBIAS"), concepts, List.of());
    }

    static AcceptedProposal candidate(String signature, CandidateProfile profile, int specificCount,
                                      NoveltyBand band, int noveltyLoad, BigDecimal score,
                                      List<GeneratorConcept> concepts) {
        if (concepts.size() != 4) {
            throw new IllegalArgumentException("Test candidates require four concepts");
        }
        List<RequirementSnapshot> requirements = new ArrayList<>();
        for (int index = 0; index < concepts.size(); index++) {
            GeneratorConcept concept = concepts.get(index);
            RequirementSpecificity specificity = index < specificCount
                    ? RequirementSpecificity.SPECIFIC : RequirementSpecificity.OPEN;
            BigDecimal availability = TestGeneratorConfiguration.defaults().availabilityFactors()
                    .get(concept.availabilityByParticipant().values().iterator().next());
            requirements.add(new RequirementSnapshot(index + 1, RequirementSource.RANDOM, concept.displayName(),
                    specificity, concept, new WeightEvaluation(concept.code(), bd("1.0000"), bd("1.0000"),
                    availability, bd("1.0000"), bd("1.0000"), bd("1.000000000000"), 1_000_000_000L,
                    Set.of())));
        }
        EnumMap<ScoreComponent, BigDecimal> components = new EnumMap<>(ScoreComponent.class);
        for (ScoreComponent component : ScoreComponent.values()) {
            components.put(component, score);
        }
        CandidateEvaluation evaluation = new CandidateEvaluation(components, score, bd("80.000000000000"), band,
                noveltyLoad, List.of(), Set.of());
        return new AcceptedProposal(Math.abs(signature.hashCode()), profile, specificCount, band, requirements,
                evaluation, signature, Set.of(), CandidateProposalEngine.CandidateRestriction.none());
    }

    static AcceptedProposal withManual(
            String signature,
            CandidateProfile profile,
            NoveltyBand band,
            BigDecimal score,
            GeneratorConcept manual,
            List<GeneratorConcept> random
    ) {
        List<RequirementSnapshot> requirements = new ArrayList<>();
        requirements.add(new RequirementSnapshot(1, RequirementSource.MANUAL, manual.displayName(),
                RequirementSpecificity.SPECIFIC, manual, null));
        int position = 2;
        for (GeneratorConcept concept : random) {
            requirements.add(new RequirementSnapshot(position++, RequirementSource.RANDOM, concept.displayName(),
                    RequirementSpecificity.SPECIFIC, concept, new WeightEvaluation(concept.code(), bd("1.0000"),
                    bd("1.0000"), bd("1.000000000000"), bd("1.0000"), bd("1.0000"),
                    bd("1.000000000000"), 1_000_000_000L, Set.of())));
        }
        EnumMap<ScoreComponent, BigDecimal> components = new EnumMap<>(ScoreComponent.class);
        for (ScoreComponent component : ScoreComponent.values()) {
            components.put(component, score);
        }
        return new AcceptedProposal(Math.abs(signature.hashCode()), profile, 4, band, requirements,
                new CandidateEvaluation(components, score, bd("80.000000000000"), band, 3, List.of(), Set.of()),
                signature, Set.of(), CandidateProposalEngine.CandidateRestriction.none());
    }

    static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
