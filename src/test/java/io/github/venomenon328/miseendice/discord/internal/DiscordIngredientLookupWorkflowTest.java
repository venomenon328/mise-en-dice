package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupMatch;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupProfile;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupRelation;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.IngredientLookupSearchResult;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscordIngredientLookupWorkflowTest {

    @Test
    void exactMatchWinsOverOtherSubstringMatchesAndSingleMatchOpensDirectly() {
        var queries = new FakeQueries();
        queries.search = result("apfel", List.of(match(1, "Apfel"), match(2, "Apfelmus")), 2);
        queries.profiles.put(1L, profile(1, "Apfel"));
        var delivery = new CapturingDelivery();
        var feedback = new CapturingFeedback();
        var workflow = workflow(queries);

        workflow.search("  APFEL ", "10001", delivery, feedback);

        assertThat(delivery.response).isInstanceOf(DiscordIngredientLookupRenderer.RenderedEmbed.class);
        assertThat(queries.profileLookups).containsExactly(1L);
        assertThat(feedback.messages).isEmpty();

        queries.search = result("birne", List.of(match(3, "Birne")), 1);
        queries.profiles.put(3L, profile(3, "Birne"));
        workflow.search("birne", "10001", delivery, feedback);
        assertThat(queries.profileLookups).containsExactly(1L, 3L);
    }

    @Test
    void rendersAtMostTwentyFivePublicOptionsWithTheFullCountAndNoTechnicalIdentifiers() {
        var queries = new FakeQueries();
        List<IngredientLookupMatch> matches = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(number -> match(number, "Zutat " + number)).toList();
        queries.search = result("zutat", matches, 30);
        var delivery = new CapturingDelivery();

        workflow(queries).search("zutat", "10001", delivery, new CapturingFeedback());

        var selection = (DiscordIngredientLookupRenderer.RenderedSelection) delivery.response;
        assertThat(selection.content()).contains("30", "ersten 25");
        assertThat(selection.options()).hasSize(25);
        assertThat(selection.customId()).contains("10001").doesNotContain("zutat", "Zutat");
        assertThat(selection.options()).allSatisfy(option -> {
            assertThat(option.label()).doesNotContain("med:");
            assertThat(option.value()).startsWith("med:v1:ingredient:concept:");
        });
    }

    @Test
    void permitsOnlyTheInvokerForTheInitialSelectionAndRechecksItsActiveProfile() {
        var queries = new FakeQueries();
        queries.profiles.put(7L, profile(7, "Sellerie"));
        var workflow = workflow(queries);
        var delivery = new CapturingDelivery();
        var feedback = new CapturingFeedback();
        String component = DiscordIngredientComponentId.selection("10001");

        workflow.component(component, List.of(DiscordIngredientComponentId.conceptValue(7)), "10002", delivery, feedback);
        assertThat(delivery.response).isNull();
        assertThat(feedback.messages).singleElement().satisfies(message -> assertThat(message).contains("anderen Nutzer"));
        assertThat(queries.profileLookups).isEmpty();

        workflow.component(component, List.of(DiscordIngredientComponentId.conceptValue(7)), "10001", delivery, feedback);
        assertThat(delivery.response).isInstanceOf(DiscordIngredientLookupRenderer.RenderedEmbed.class);
        assertThat(queries.profileLookups).containsExactly(7L);

        queries.profiles.remove(7L);
        workflow.component(component, List.of(DiscordIngredientComponentId.conceptValue(7)), "10001", delivery, feedback);
        assertThat(delivery.response).isInstanceOf(DiscordIngredientLookupRenderer.RenderedText.class);
        assertThat(((DiscordIngredientLookupRenderer.RenderedText) delivery.response).content()).contains("nicht mehr aktuell");
    }

    @Test
    void hierarchyNavigationLoadsTheConceptIdDirectlyForTheCardOwnerWithoutAnyNameSearch() {
        var queries = new FakeQueries();
        queries.profiles.put(9L, profile(9, "Tempeh"));
        var workflow = workflow(queries);
        var delivery = new CapturingDelivery();
        var feedback = new CapturingFeedback();
        String navigation = DiscordIngredientComponentId.navigationSelect("child", "10001");

        workflow.navigateSelect(navigation, List.of(DiscordIngredientComponentId.conceptValue(9)),
                "10001", delivery, feedback);

        assertThat(delivery.response).isInstanceOf(DiscordIngredientLookupRenderer.RenderedEmbed.class);
        assertThat(queries.profileLookups).containsExactly(9L);
        assertThat(queries.searchCalls).isZero();
        assertThat(feedback.messages).isEmpty();
    }

    @Test
    void foreignUserCannotNavigateAnotherUsersCardOrTriggerCatalogWork() {
        var queries = new FakeQueries();
        queries.profiles.put(9L, profile(9, "Tempeh"));
        var delivery = new CapturingDelivery();
        var feedback = new CapturingFeedback();

        workflow(queries).navigateSelect(DiscordIngredientComponentId.navigationSelect("child", "10001"),
                List.of(DiscordIngredientComponentId.conceptValue(9)), "10002", delivery, feedback);

        assertThat(delivery.response).isNull();
        assertThat(queries.profileLookups).isEmpty();
        assertThat(queries.searchCalls).isZero();
        assertThat(feedback.messages).singleElement().satisfies(message -> assertThat(message).contains("anderen Nutzer"));
    }

    @Test
    void staleMalformedAndLegacyUnownedHierarchyNavigationNeverShowsAnInactiveProfile() {
        var queries = new FakeQueries();
        var workflow = workflow(queries);
        var delivery = new CapturingDelivery();
        var feedback = new CapturingFeedback();

        workflow.navigateSelect(DiscordIngredientComponentId.navigationSelect("parent", "10001"),
                List.of(DiscordIngredientComponentId.conceptValue(77)), "10001", delivery, feedback);
        assertThat(delivery.response).isInstanceOf(DiscordIngredientLookupRenderer.RenderedText.class);
        assertThat(((DiscordIngredientLookupRenderer.RenderedText) delivery.response).content()).contains("nicht mehr aktuell");

        workflow.navigateSelect(DiscordIngredientComponentId.navigationSelect("parent"),
                List.of(DiscordIngredientComponentId.conceptValue(77)), "10001", delivery, feedback);
        workflow.navigateSelect("med:v2:ingredient:navigate-select:sideways:10001",
                List.of(DiscordIngredientComponentId.conceptValue(77)), "10001", delivery, feedback);

        assertThat(feedback.messages).hasSize(2).allSatisfy(message -> assertThat(message).contains("Navigation", "ungültig"));
        assertThat(queries.profileLookups).containsExactly(77L);
    }

    @Test
    void guildGuardDoesNotDependOnParticipantRegistration() {
        var queries = new FakeQueries();
        var workflow = workflow(queries);

        assertThat(workflow.acceptsGuild(99)).isTrue();
        assertThat(workflow.acceptsGuild(98)).isFalse();
        assertThat(queries.profileLookups).isEmpty();
        assertThat(queries.searchCalls).isZero();
    }

    @Test
    void rejectsBlankSearchWithoutCallingTheCatalogProjection() {
        var queries = new FakeQueries();
        var feedback = new CapturingFeedback();

        workflow(queries).search("   ", "99999", new CapturingDelivery(), feedback);

        assertThat(feedback.messages).singleElement().satisfies(message -> assertThat(message).contains("nicht leer"));
        assertThat(queries.searchCalls).isZero();
    }

    private static DiscordIngredientLookupWorkflow workflow(FakeQueries queries) {
        return new DiscordIngredientLookupWorkflow(new DiscordProperties(true, "token", 99, 77777,
                ZoneId.of("Europe/Berlin"), Map.of("GEORGIA", "10001", "TOBIAS", "10002")),
                queries, new DiscordIngredientLookupRenderer());
    }

    private static IngredientLookupSearchResult result(String text, List<IngredientLookupMatch> matches, long total) {
        return new IngredientLookupSearchResult(text, matches, total);
    }

    private static IngredientLookupMatch match(long id, String displayName) {
        return new IngredientLookupMatch(id, displayName, List.of("Aktiver Oberbegriff"));
    }

    private static IngredientLookupProfile profile(long id, String displayName) {
        return new IngredientLookupProfile(id, displayName, true, new BigDecimal("1.0000"), null,
                List.<IngredientLookupRelation>of(), List.<IngredientLookupRelation>of(), List.of(), List.of(), List.of(), null);
    }

    private static final class FakeQueries implements IngredientLookupQueries {
        private IngredientLookupSearchResult search = result("", List.of(), 0);
        private final Map<Long, IngredientLookupProfile> profiles = new java.util.HashMap<>();
        private final List<Long> profileLookups = new ArrayList<>();
        private int searchCalls;

        @Override
        public IngredientLookupSearchResult searchActiveByDisplayName(String searchText, int limit) {
            searchCalls++;
            return search;
        }

        @Override
        public Optional<IngredientLookupProfile> findActiveProfile(long conceptId) {
            profileLookups.add(conceptId);
            return Optional.ofNullable(profiles.get(conceptId));
        }
    }

    private static final class CapturingDelivery implements DiscordIngredientLookupWorkflow.Delivery {
        private DiscordIngredientLookupRenderer.RenderedResponse response;

        @Override
        public void replace(DiscordIngredientLookupRenderer.RenderedResponse response, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            this.response = response;
            delivered.run();
        }
    }

    private static final class CapturingFeedback implements DiscordIngredientLookupWorkflow.Feedback {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void staleOrRejected(String message) {
            messages.add(message);
        }

        @Override
        public void technicalFailure(Throwable exception) {
            throw new AssertionError(exception);
        }
    }
}
