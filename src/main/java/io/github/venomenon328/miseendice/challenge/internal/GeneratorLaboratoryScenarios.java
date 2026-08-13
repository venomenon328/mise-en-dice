package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

final class GeneratorLaboratoryScenarios {

    private GeneratorLaboratoryScenarios() {
    }

    static VisibleHistorySnapshot synthetic(
            HistoryScenario scenario,
            LocalDate effectiveDate,
            CatalogGeneratorSnapshot catalog
    ) {
        if (scenario == HistoryScenario.EMPTY_HISTORY) {
            return VisibleHistorySnapshot.empty();
        }
        List<GeneratorConcept> drawable = catalog.concepts().stream()
                .filter(concept -> concept.active() && concept.randomDrawEnabled() && concept.noveltyLevel() != null)
                .sorted(Comparator.comparing(GeneratorConcept::code).thenComparingLong(GeneratorConcept::id))
                .toList();
        if (drawable.size() < 24) {
            throw invalid("Synthetic history requires at least 24 drawable concepts with novelty metadata");
        }
        Instant anchor = effectiveDate.atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(12 * 3_600L);
        return switch (scenario) {
            case NEUTRAL_HISTORY -> new VisibleHistorySnapshot(List.of(
                    challenge(anchor.minusSeconds(86_400L), "lab-neutral-1",
                            select(drawable, NoveltyBand.BALANCED, 0), NoveltyBand.BALANCED),
                    challenge(anchor.minusSeconds(2 * 86_400L), "lab-neutral-2",
                            select(drawable, NoveltyBand.FAMILIAR, 4), NoveltyBand.FAMILIAR)));
            case RECOVERY_AFTER_ADVENTUROUS -> new VisibleHistorySnapshot(List.of(
                    challenge(anchor.minusSeconds(86_400L), "lab-recovery-1",
                            adventurous(drawable), NoveltyBand.ADVENTUROUS)));
            case SEEKING_AFTER_THREE_FAMILIAR -> new VisibleHistorySnapshot(List.of(
                    challenge(anchor.minusSeconds(86_400L), "lab-familiar-1",
                            select(drawable, NoveltyBand.FAMILIAR, 0), NoveltyBand.FAMILIAR),
                    challenge(anchor.minusSeconds(2 * 86_400L), "lab-familiar-2",
                            select(drawable, NoveltyBand.FAMILIAR, 4), NoveltyBand.FAMILIAR),
                    challenge(anchor.minusSeconds(3 * 86_400L), "lab-familiar-3",
                            select(drawable, NoveltyBand.FAMILIAR, 8), NoveltyBand.FAMILIAR)));
            case LOADED_COOLDOWN_HISTORY -> loaded(anchor, drawable);
            case PRODUCTION_VISIBLE, EMPTY_HISTORY -> throw new IllegalArgumentException(
                    "Production and empty history are handled outside synthetic scenario construction");
        };
    }

    private static VisibleHistorySnapshot loaded(Instant anchor, List<GeneratorConcept> drawable) {
        List<VisibleChallenge> challenges = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            NoveltyBand band = index % 2 == 0 ? NoveltyBand.BALANCED : NoveltyBand.FAMILIAR;
            challenges.add(challenge(anchor.minusSeconds((index + 1L) * 86_400L),
                    "lab-loaded-" + (index + 1), select(drawable, band, index * 4), band));
        }
        return new VisibleHistorySnapshot(challenges);
    }

    private static List<GeneratorConcept> adventurous(List<GeneratorConcept> drawable) {
        List<GeneratorConcept> levelFive = drawable.stream().filter(concept -> concept.noveltyLevel() == 5).toList();
        if (levelFive.isEmpty()) {
            throw invalid("Recovery scenario requires at least one novelty-level-5 concept");
        }
        LinkedHashSet<GeneratorConcept> selected = new LinkedHashSet<>();
        selected.add(levelFive.getFirst());
        drawable.stream().filter(concept -> concept.noveltyLevel() >= 4).forEach(selected::add);
        drawable.forEach(selected::add);
        return selected.stream().limit(4).toList();
    }

    private static List<GeneratorConcept> select(
            List<GeneratorConcept> drawable,
            NoveltyBand band,
            int offset
    ) {
        List<GeneratorConcept> matching = drawable.stream().filter(concept -> switch (band) {
            case FAMILIAR -> concept.noveltyLevel() <= 2;
            case BALANCED -> concept.noveltyLevel() == 3;
            case ADVENTUROUS -> concept.noveltyLevel() >= 4;
        }).toList();
        if (matching.size() < 4) {
            matching = drawable.stream().filter(concept -> concept.noveltyLevel() != 5).toList();
        }
        if (matching.size() < 4) {
            throw invalid("Synthetic history cannot select four stable concepts for " + band);
        }
        List<GeneratorConcept> selected = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            selected.add(matching.get(Math.floorMod(offset + index, matching.size())));
        }
        return List.copyOf(selected);
    }

    private static VisibleChallenge challenge(
            Instant visibleAt,
            String sessionKey,
            List<GeneratorConcept> concepts,
            NoveltyBand band
    ) {
        if (concepts.size() != 4 || new LinkedHashSet<>(concepts).size() != 4) {
            throw invalid("Synthetic visible challenges require exactly four unique concepts");
        }
        List<VisibleRequirement> requirements = concepts.stream().map(concept -> new VisibleRequirement(
                concept.code(), concept.noveltyLevel(), concept.functionalRoles(), concept.culinaryFlags(),
                concept.transitiveAncestorCodes())).toList();
        return new VisibleChallenge(visibleAt, sessionKey, AttemptType.INITIAL, "COMPLETED", requirements,
                CandidateProfile.FLEXIBLE_BALANCED, band, null);
    }

    private static GeneratorValidationException invalid(String detail) {
        return new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST, detail);
    }
}
