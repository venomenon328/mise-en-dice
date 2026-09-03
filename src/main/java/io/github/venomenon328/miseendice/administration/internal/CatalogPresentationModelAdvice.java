package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Supplies localized presentation labels and occurrence-scoped hierarchy IDs. */
@ControllerAdvice(assignableTypes = CatalogAdministrationController.class)
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogPresentationModelAdvice {

    private static final Map<CatalogAvailability, String> AVAILABILITY_LABELS = Arrays.stream(CatalogAvailability.values())
            .collect(Collectors.toUnmodifiableMap(Function.identity(), CatalogAvailability::displayName));

    private static final Map<CatalogAvailability, String> AVAILABILITY_DESCRIPTIONS = Arrays.stream(CatalogAvailability.values())
            .collect(Collectors.toUnmodifiableMap(Function.identity(), CatalogAvailability::shortDescription));

    private static final Map<Integer, String> NOVELTY_LABELS = Map.of(
            1, "Standardverwendung",
            2, "Vertraute Verwendung",
            3, "Kontextgebundene Verwendung",
            4, "Ungewöhnliche Verwendung",
            5, "Ausgefallene Verwendung"
    );

    private static final Map<Integer, String> NOVELTY_DESCRIPTIONS = Map.of(
            1, "Breit und selbstverständlich als Kochzutat verwendet.",
            2, "Klar etabliert und wenig überraschend, aber nicht universeller Alltagsstandard.",
            3, "In bestimmten Küchen konventionell, sonst merklich richtungsgebend.",
            4, "Nur begrenzte Kochanwendungen liegen nahe; als Vorgabe ein deutlicher Twist.",
            5, "Sinnvolle Verwendung ist ausgesprochen nischig oder überraschend."
    );

    private static final Map<CatalogSort, String> SORT_LABELS = Map.of(
            CatalogSort.DISPLAY_NAME_ASC, "Name A–Z",
            CatalogSort.DISPLAY_NAME_DESC, "Name Z–A",
            CatalogSort.UPDATED_DESC, "Neueste Änderung",
            CatalogSort.UPDATED_ASC, "Älteste Änderung",
            CatalogSort.DRAW_WEIGHT_DESC, "Gewicht absteigend",
            CatalogSort.DRAW_WEIGHT_ASC, "Gewicht aufsteigend",
            CatalogSort.NOVELTY_DESC, "Kochungewöhnlichkeit absteigend",
            CatalogSort.NOVELTY_ASC, "Kochungewöhnlichkeit aufsteigend"
    );

    @ModelAttribute("availabilityLabels")
    Map<CatalogAvailability, String> availabilityLabels() {
        return AVAILABILITY_LABELS;
    }

    @ModelAttribute("availabilityDescriptions")
    Map<CatalogAvailability, String> availabilityDescriptions() {
        return AVAILABILITY_DESCRIPTIONS;
    }

    @ModelAttribute("noveltyLabels")
    Map<Integer, String> noveltyLabels() {
        return NOVELTY_LABELS;
    }

    @ModelAttribute("noveltyDescriptions")
    Map<Integer, String> noveltyDescriptions() {
        return NOVELTY_DESCRIPTIONS;
    }

    @ModelAttribute("sortLabels")
    Map<CatalogSort, String> sortLabels() {
        return SORT_LABELS;
    }

    @ModelAttribute("hierarchyFragmentId")
    String hierarchyFragmentId() {
        return UUID.randomUUID().toString();
    }
}
