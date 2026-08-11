package io.github.venomenon328.miseendice.administration.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Supplies localized presentation labels and occurrence-scoped hierarchy IDs. */
@ControllerAdvice(assignableTypes = CatalogAdministrationController.class)
@ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
class CatalogPresentationModelAdvice {

    private static final Map<CatalogAvailability, String> AVAILABILITY_LABELS = Map.of(
            CatalogAvailability.EASY, "einfach",
            CatalogAvailability.PLANNED, "gezielter Einkauf",
            CatalogAvailability.DIFFICULT, "schwierig",
            CatalogAvailability.UNAVAILABLE, "regulär nicht verfügbar"
    );

    private static final Map<CatalogSort, String> SORT_LABELS = Map.of(
            CatalogSort.DISPLAY_NAME_ASC, "Name A–Z",
            CatalogSort.DISPLAY_NAME_DESC, "Name Z–A",
            CatalogSort.UPDATED_DESC, "Neueste Änderung",
            CatalogSort.UPDATED_ASC, "Älteste Änderung",
            CatalogSort.DRAW_WEIGHT_DESC, "Gewicht absteigend",
            CatalogSort.DRAW_WEIGHT_ASC, "Gewicht aufsteigend",
            CatalogSort.NOVELTY_DESC, "Ungewöhnlichkeit absteigend",
            CatalogSort.NOVELTY_ASC, "Ungewöhnlichkeit aufsteigend"
    );

    @ModelAttribute("availabilityLabels")
    Map<CatalogAvailability, String> availabilityLabels() {
        return AVAILABILITY_LABELS;
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
