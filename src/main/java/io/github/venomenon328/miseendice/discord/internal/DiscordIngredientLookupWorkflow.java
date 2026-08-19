package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupSearchResult;
import java.util.List;
import java.util.function.Consumer;

/** Stateless read-only workflow for the Discord ingredient command. */
final class DiscordIngredientLookupWorkflow {
    private static final int SELECTION_LIMIT = 25;

    private final DiscordProperties properties;
    private final IngredientLookupQueries ingredientLookupQueries;
    private final DiscordIngredientLookupRenderer renderer;

    DiscordIngredientLookupWorkflow(DiscordProperties properties, IngredientLookupQueries ingredientLookupQueries,
                                    DiscordIngredientLookupRenderer renderer) {
        this.properties = properties;
        this.ingredientLookupQueries = ingredientLookupQueries;
        this.renderer = renderer;
    }

    boolean accepts(long guildId, String userId) {
        return guildId == properties.guildId() && properties.isConfiguredUser(userId);
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
            if (values == null || values.size() != 1) {
                feedback.staleOrRejected("Diese Zutaten-Auswahl ist ungültig oder nicht mehr aktuell.");
                return;
            }
            renderCurrentProfile(DiscordIngredientComponentId.parseConceptValue(values.getFirst()), delivery, feedback);
        } catch (IllegalArgumentException exception) {
            feedback.staleOrRejected("Diese Zutaten-Auswahl ist ungültig oder nicht mehr aktuell.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    private void renderCurrentProfile(long conceptId, Delivery delivery, Feedback feedback) {
        ingredientLookupQueries.findActiveProfile(conceptId).ifPresentOrElse(
                profile -> delivery.replace(renderer.profile(profile), () -> { }, feedback::technicalFailure),
                () -> delivery.replace(renderer.staleSelection(), () -> { }, feedback::technicalFailure));
    }

    interface Delivery {
        void replace(DiscordIngredientLookupRenderer.RenderedResponse response, Runnable delivered, Consumer<Throwable> failed);
    }

    interface Feedback {
        void staleOrRejected(String message);
        void technicalFailure(Throwable exception);
    }
}
