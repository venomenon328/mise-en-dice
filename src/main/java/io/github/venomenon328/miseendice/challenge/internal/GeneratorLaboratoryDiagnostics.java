package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PairEvidence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class GeneratorLaboratoryDiagnostics {
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private GeneratorLaboratoryDiagnostics() {
    }

    static List<PairEvidence> pairs(
            GeneratedCandidateSet generated,
            CatalogGeneratorSnapshot catalog,
            GeneratorConfiguration configuration
    ) {
        List<PairEvidence> result = new ArrayList<>(66);
        for (CandidateSetEngine.PairAssessment assessment : generated.evaluation().pairs()) {
            AcceptedProposal first = generated.candidates().get(assessment.firstCandidateNumber() - 1);
            AcceptedProposal second = generated.candidates().get(assessment.secondCandidateNumber() - 1);
            result.add(new PairEvidence(
                    assessment.firstCandidateNumber(), assessment.secondCandidateNumber(), assessment,
                    intersection(randomCodes(first), randomCodes(second)),
                    intersection(informativeAncestors(first, catalog, configuration),
                            informativeAncestors(second, catalog, configuration)),
                    intersection(roles(first), roles(second)), intersection(flags(first), flags(second)),
                    intersection(dimensions(first), dimensions(second)), first.profile() == second.profile(),
                    specificityCount(first), specificityCount(second), first.evaluation().actualNoveltyBand(),
                    second.evaluation().actualNoveltyBand(), first.evaluation().knownNoveltyLoad(),
                    second.evaluation().knownNoveltyLoad(), availabilityLoad(first), availabilityLoad(second)));
        }
        return List.copyOf(result);
    }

    private static Set<String> randomCodes(AcceptedProposal candidate) {
        TreeSet<String> result = new TreeSet<>();
        randomRequirements(candidate).stream().map(RequirementSnapshot::concept).map(GeneratorConcept::code)
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private static Set<String> roles(AcceptedProposal candidate) {
        TreeSet<String> result = new TreeSet<>();
        randomRequirements(candidate).stream().map(RequirementSnapshot::concept)
                .forEach(concept -> result.addAll(concept.functionalRoles()));
        return Set.copyOf(result);
    }

    private static Set<String> flags(AcceptedProposal candidate) {
        TreeSet<String> result = new TreeSet<>();
        randomRequirements(candidate).stream().map(RequirementSnapshot::concept)
                .forEach(concept -> result.addAll(concept.culinaryFlags()));
        return Set.copyOf(result);
    }

    private static Set<String> dimensions(AcceptedProposal candidate) {
        TreeSet<String> result = new TreeSet<>();
        randomRequirements(candidate).stream().map(RequirementSnapshot::concept)
                .forEach(concept -> result.addAll(concept.culinaryDimensions().keySet()));
        return Set.copyOf(result);
    }

    private static Set<String> informativeAncestors(
            AcceptedProposal candidate,
            CatalogGeneratorSnapshot catalog,
            GeneratorConfiguration configuration
    ) {
        Set<String> drawableCodes = catalog.concepts().stream()
                .filter(concept -> concept.active() && concept.randomDrawEnabled())
                .map(GeneratorConcept::code).collect(java.util.stream.Collectors.toSet());
        TreeSet<String> ancestors = new TreeSet<>();
        randomRequirements(candidate).stream().map(RequirementSnapshot::concept)
                .forEach(concept -> ancestors.addAll(concept.transitiveAncestorCodes()));
        ancestors.removeIf(code -> catalog.conceptByCode(code).map(ancestor -> {
            long descendants = ancestor.transitiveDescendantCodes().stream().filter(drawableCodes::contains).count();
            BigDecimal share = BigDecimal.valueOf(descendants)
                    .divide(BigDecimal.valueOf(drawableCodes.size()), SCALE, ROUNDING);
            return share.compareTo(configuration.similarity().informativeAncestorMaximumDrawableShare()) > 0;
        }).orElse(true));
        return Set.copyOf(ancestors);
    }

    private static int specificityCount(AcceptedProposal candidate) {
        return (int) candidate.requirements().stream()
                .filter(requirement -> requirement.specificity() == RequirementSpecificity.SPECIFIC).count();
    }

    private static BigDecimal availabilityLoad(AcceptedProposal candidate) {
        List<RequirementSnapshot> random = randomRequirements(candidate);
        if (random.isEmpty()) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        BigDecimal sum = random.stream().map(RequirementSnapshot::weightEvaluation)
                .map(weight -> BigDecimal.ONE.subtract(weight.availabilityFactor()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(random.size()), SCALE, ROUNDING);
    }

    private static List<RequirementSnapshot> randomRequirements(AcceptedProposal candidate) {
        return candidate.requirements().stream()
                .filter(requirement -> requirement.source() == RequirementSource.RANDOM && requirement.concept() != null)
                .toList();
    }

    private static Set<String> intersection(Set<String> left, Set<String> right) {
        TreeSet<String> result = new TreeSet<>(left);
        result.retainAll(right);
        return Set.copyOf(result);
    }
}
