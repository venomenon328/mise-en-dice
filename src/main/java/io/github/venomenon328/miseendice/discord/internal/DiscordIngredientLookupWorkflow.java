package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.CulinaryCountry;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupSearchResult;
import java.util.List;
import java.util.function.Consumer;

/** Stateless read-only workflow for the Discord ingredient command. */
final class DiscordIngredientLookupWorkflow {
    private static final int SELECTION_LIMIT = 25;
    private static final int COUNTRY_AUTOCOMPLETE_LIMIT = 25;
    private static final int COUNTRY_PAGE_SIZE = 20;

    private final DiscordProperties properties;
    private final IngredientLookupQueries ingredientLookupQueries;
    private final DiscordIngredientLookupRenderer renderer;

    DiscordIngredientLookupWorkflow(DiscordProperties properties, IngredientLookupQueries ingredientLookupQueries,
                                    DiscordIngredientLookupRenderer renderer) {
        this.properties = properties;
        this.ingredientLookupQueries = ingredientLookupQueries;
        this.renderer = renderer;
    }

    boolean acceptsGuild(long guildId) {
        return guildId == properties.guildId();
    }

    List<CulinaryCountry> autocompleteCountries(String searchText) {
        return ingredientLookupQueries.searchCulinaryCountries(searchText, COUNTRY_AUTOCOMPLETE_LIMIT);
    }

    void search(String searchText, String invokerUserId, Delivery delivery, Feedback feedback) {
        if (IngredientLookupQueries.normalize(searchText).isEmpty()) {
            feedback.staleOrRejected("`suche` darf nicht leer sein.");
            return;
        }
        try {
            IngredientLookupSearchResult result = ingredientLookupQueries.searchActiveByDisplayName(searchText, SELECTION_LIMIT);
            if (result.totalMatches() == 0) {
                delivery.replace(renderer.noMatches(), () -> { }, feedback::technicalFailure);
                return;
            }
            var direct = result.matches().stream()
                    .filter(match -> match.displayName().equalsIgnoreCase(result.searchText()))
                    .findFirst()
                    .or(() -> result.totalMatches() == 1 ? result.matches().stream().findFirst() : java.util.Optional.empty());
            if (direct.isPresent()) {
                renderCurrentProfile(direct.orElseThrow().conceptId(), delivery, feedback);
                return;
            }
            delivery.replace(renderer.selection(result, invokerUserId), () -> { }, feedback::technicalFailure);
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void component(String customId, List<String> values, String userId, Delivery delivery, Feedback feedback) {
        try {
            DiscordIngredientComponentId.Selection selection = DiscordIngredientComponentId.parseSelection(customId);
            if (!selection.invokerUserId().equals(userId)) {
                feedback.staleOrRejected("Diese Zutaten-Auswahl gehört zu einem anderen Nutzer.");
                return;
            }
            renderSingleValue(values, delivery, feedback);
        } catch (IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Zutaten-Auswahl ist ungültig oder nicht mehr aktuell.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void countryComponent(String customId, List<String> values, String userId, Delivery delivery, Feedback feedback) {
        try {
            DiscordIngredientComponentId.CountrySelect selection = DiscordIngredientComponentId.parseCountrySelect(customId);
            if (!selection.invokerUserId().equals(userId)) {
                feedback.staleOrRejected("Diese Länderliste gehört zu einem anderen Nutzer.");
                return;
            }
            renderSingleValue(values, selection.context(), delivery, feedback);
        } catch (IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Zutaten-Auswahl ist ungültig oder nicht mehr aktuell.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void browseCountry(String input, String invokerUserId, Delivery delivery, Feedback feedback) {
        try {
            ingredientLookupQueries.resolveCulinaryCountry(input).ifPresentOrElse(
                    country -> renderCountryPage(new DiscordIngredientComponentId.CountryBrowseContext(country.code(), 1), delivery, feedback),
                    () -> feedback.staleOrRejected("`land` ist kein bekanntes kulinarisches Land. Wähle einen Vorschlag aus der Autovervollständigung."));
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void countryPage(String customId, String userId, Delivery delivery, Feedback feedback) {
        try {
            DiscordIngredientComponentId.CountryPage page = DiscordIngredientComponentId.parseCountryPage(customId);
            if (!page.invokerUserId().equals(userId)) {
                feedback.staleOrRejected("Diese Länderliste gehört zu einem anderen Nutzer.");
                return;
            }
            renderCountryPage(page.context(), delivery, feedback);
        } catch (IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Länderliste ist ungültig oder nicht mehr aktuell.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void countryBack(String customId, String userId, Delivery delivery, Feedback feedback) {
        try {
            DiscordIngredientComponentId.CountryBack back = DiscordIngredientComponentId.parseCountryBack(customId);
            if (!back.invokerUserId().equals(userId)) {
                feedback.staleOrRejected("Diese Zutaten-Card gehört zu einem anderen Nutzer.");
                return;
            }
            renderCountryPage(back.context(), delivery, feedback);
        } catch (IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Rücknavigation ist ungültig oder nicht mehr aktuell.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void navigateSelect(String customId, List<String> values, String userId, Delivery delivery, Feedback feedback) {
        try {
            DiscordIngredientComponentId.NavigationSelect navigation =
                    DiscordIngredientComponentId.parseNavigationSelect(customId);
            if (!navigation.invokerUserId().equals(userId)) {
                feedback.staleOrRejected("Diese Zutaten-Card gehört zu einem anderen Nutzer.");
                return;
            }
            renderSingleValue(values, navigation.countryContext(), delivery, feedback);
        } catch (IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Zutaten-Navigation ist ungültig oder nicht mehr aktuell.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    private void renderSingleValue(List<String> values, Delivery delivery, Feedback feedback) {
        renderSingleValue(values, null, delivery, feedback);
    }

    private void renderSingleValue(List<String> values, DiscordIngredientComponentId.CountryBrowseContext countryContext,
                                   Delivery delivery, Feedback feedback) {
        if (values == null || values.size() != 1) {
            throw new IllegalArgumentException("Ingredient selection requires exactly one value");
        }
        renderCurrentProfile(DiscordIngredientComponentId.parseConceptValue(values.getFirst()), countryContext, delivery, feedback);
    }

    private void renderCurrentProfile(long conceptId, Delivery delivery, Feedback feedback) {
        renderCurrentProfile(conceptId, null, delivery, feedback);
    }

    private void renderCurrentProfile(long conceptId, DiscordIngredientComponentId.CountryBrowseContext countryContext,
                                      Delivery delivery, Feedback feedback) {
        if (countryContext != null) {
            ingredientLookupQueries.findActiveByCulinaryCountry(countryContext.countryCode(), countryContext.page(), COUNTRY_PAGE_SIZE)
                    .ifPresentOrElse(page -> renderProfileForCountryOrigin(conceptId,
                                    new DiscordIngredientLookupRenderer.CountryBrowseOrigin(
                                            new DiscordIngredientComponentId.CountryBrowseContext(page.country().code(), page.page()),
                                            page.country().displayName()), delivery, feedback),
                            () -> delivery.replace(new DiscordIngredientLookupRenderer.RenderedText(
                                    "Dieses kulinarische Land ist nicht mehr verfügbar."), () -> { }, feedback::technicalFailure));
            return;
        }
        renderProfileForCountryOrigin(conceptId, null, delivery, feedback);
    }

    private void renderProfileForCountryOrigin(long conceptId, DiscordIngredientLookupRenderer.CountryBrowseOrigin countryOrigin,
                                               Delivery delivery, Feedback feedback) {
        ingredientLookupQueries.findActiveProfile(conceptId).ifPresentOrElse(
                profile -> delivery.replace(renderer.profile(profile, countryOrigin), () -> { }, feedback::technicalFailure),
                () -> delivery.replace(countryOrigin == null ? renderer.staleSelection() : renderer.staleCountrySelection(countryOrigin),
                        () -> { }, feedback::technicalFailure));
    }

    private void renderCountryPage(DiscordIngredientComponentId.CountryBrowseContext context, Delivery delivery, Feedback feedback) {
        ingredientLookupQueries.findActiveByCulinaryCountry(context.countryCode(), context.page(), COUNTRY_PAGE_SIZE)
                .ifPresentOrElse(page -> delivery.replace(renderer.countryIngredients(page), () -> { }, feedback::technicalFailure),
                        () -> delivery.replace(new DiscordIngredientLookupRenderer.RenderedText(
                                "Dieses kulinarische Land ist nicht mehr verfügbar."), () -> { }, feedback::technicalFailure));
    }

    interface Delivery {
        void replace(DiscordIngredientLookupRenderer.RenderedResponse response, Runnable delivered, Consumer<Throwable> failed);
    }

    interface Feedback {
        void staleOrRejected(String message);
        void technicalFailure(Throwable exception);
    }
}
