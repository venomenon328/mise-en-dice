package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class DiscordJdaListenerTest {

    @Test
    void registersIngredientLookupWithOneRequiredSearchString() {
        var command = DiscordJdaListener.ingredientCommand();

        assertThat(command.getName()).isEqualTo("zutat");
        assertThat(command.getOptions()).singleElement().satisfies(option -> {
            assertThat(option.getName()).isEqualTo("suche");
            assertThat(option.getType()).isEqualTo(OptionType.STRING);
            assertThat(option.isRequired()).isTrue();
        });
    }

    @Test
    void rejectsIngredientLookupFromWrongGuildWithoutStartingLookupWork() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        var event = slashEvent();
        var reply = mock(ReplyCallbackAction.class);
        when(event.getName()).thenReturn("zutat");
        when(lookupWorkflow.accepts(99, "10001")).thenReturn(false);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        ingredientListener(challengeWorkflow, lookupWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verify(lookupWorkflow, never()).search(any(), any(), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void defersIngredientSlashBeforeDelegatingTheRequiredSearchString() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        var event = slashEvent();
        var option = mock(OptionMapping.class);
        var acknowledgement = acknowledgement(event);
        when(event.getName()).thenReturn("zutat");
        when(event.getOption("suche")).thenReturn(option);
        when(option.getAsString()).thenReturn("  Tempeh  ");
        when(lookupWorkflow.accepts(99, "10001")).thenReturn(true);

        ingredientListener(challengeWorkflow, lookupWorkflow).onSlashCommandInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferReply();
        order.verify(lookupWorkflow).search(eq("  Tempeh  "), eq("10001"), any(), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void defersIngredientSelectionBeforeDelegatingItsStatelessValues() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        var event = stringSelectEvent(DiscordIngredientComponentId.selection("10001"),
                List.of(DiscordIngredientComponentId.conceptValue(42)), lookupWorkflow);

        ingredientListener(challengeWorkflow, lookupWorkflow).onStringSelectInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferEdit();
        order.verify(lookupWorkflow).component(eq(DiscordIngredientComponentId.selection("10001")),
                eq(List.of(DiscordIngredientComponentId.conceptValue(42))), eq("10001"), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void routesIngredientNavigationButtonBeforeTheGenericChallengeComponentPath() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        var event = buttonEvent(DiscordIngredientComponentId.navigationButton(42), lookupWorkflow);

        ingredientListener(challengeWorkflow, lookupWorkflow).onButtonInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferEdit();
        order.verify(lookupWorkflow).navigateButton(eq(DiscordIngredientComponentId.navigationButton(42)), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void routesIngredientNavigationSelectWithoutInvokerBinding() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        String componentId = DiscordIngredientComponentId.navigationSelect("child");
        List<String> values = List.of(DiscordIngredientComponentId.conceptValue(42));
        var event = stringSelectEvent(componentId, values, lookupWorkflow);

        ingredientListener(challengeWorkflow, lookupWorkflow).onStringSelectInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferEdit();
        order.verify(lookupWorkflow).navigateSelect(eq(componentId), eq(values), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void ingredientProfileMessageMapsDescriptionInlineFieldsColorAndNavigationComponents() {
        var response = new DiscordIngredientLookupRenderer.RenderedEmbed(
                "🥢 Tempeh",
                "```\nGewichtung  1,0\n```",
                DiscordIngredientLookupRenderer.CARD_COLOR,
                List.of(
                        new DiscordIngredientLookupRenderer.EmbedField("Funktion im Gericht", "pflanzliches Protein", true),
                        new DiscordIngredientLookupRenderer.EmbedField("Besondere Eigenschaften", "fermentiert", true)),
                List.of(new DiscordIngredientLookupRenderer.NavigationButtonRow(List.of(
                        new DiscordIngredientLookupRenderer.NavigationButton("⬆ Sojaprodukt",
                                DiscordIngredientComponentId.navigationButton(42))))));

        var message = DiscordJdaListener.ingredientMessage(response);

        assertThat(message.getEmbeds()).singleElement().satisfies(embed -> {
            assertThat(embed.getTitle()).isEqualTo("🥢 Tempeh");
            assertThat(embed.getDescription()).contains("Gewichtung");
            assertThat(embed.getColorRaw()).isEqualTo(DiscordIngredientLookupRenderer.CARD_COLOR);
            assertThat(embed.getFields()).hasSize(2).allSatisfy(field -> assertThat(field.isInline()).isTrue());
        });
        assertThat(message.getComponents()).hasSize(1);
    }

    @Test
    void ingredientProfileWithoutRelationsRemovesTheSelectionMenu() {
        var response = new DiscordIngredientLookupRenderer.RenderedEmbed(
                "🥢 Tempeh", "```\nGewichtung  1,0\n```", DiscordIngredientLookupRenderer.CARD_COLOR,
                List.of(new DiscordIngredientLookupRenderer.EmbedField("🍽️ Geschmacksprofil", "keine", false)), List.of());

        var message = DiscordJdaListener.ingredientMessage(response);

        assertThat(message.getComponents()).isEmpty();
        assertThat(message.getEmbeds()).hasSize(1);
    }

    @Test
    void registersRestrictionModeAsTheThreeGermanChoices() {
        var command = DiscordJdaListener.challengeCommand();
        var restriction = command.getOptions().stream()
                .filter(option -> "einschraenkung".equals(option.getName()))
                .findFirst().orElseThrow();

        assertThat(restriction.getType()).isEqualTo(OptionType.STRING);
        assertThat(restriction.isRequired()).isFalse();
        assertThat(restriction.getChoices())
                .extracting(choice -> choice.getName() + ":" + choice.getAsString())
                .containsExactly("automatisch:AUTO", "keine:NONE", "erzwingen:REQUIRED");
    }

    @Test
    void defersSlashInteractionBeforeDelegatingDefaultOffersAndAutoRestrictions() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent();
        var acknowledgement = acknowledgement(event);
        var listener = listener(workflow);
        when(workflow.accepts(99, "10001")).thenReturn(true);

        listener.onSlashCommandInteraction(event);

        InOrder order = inOrder(event, workflow);
        order.verify(event).deferReply();
        order.verify(workflow).start(eq(1), eq(RestrictionMode.AUTO), any(DiscordMemberNameResolver.class), any(), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
    }

    @Test
    void delegatesExplicitTwoAndThreeOfferRequestsAfterAcknowledgement() {
        for (int offerCount : java.util.List.of(2, 3)) {
            var workflow = mock(DiscordChallengeWorkflow.class);
            var event = slashEvent();
            var option = mock(OptionMapping.class);
            acknowledgement(event);
            when(event.getOption("angebote")).thenReturn(option);
            when(option.getAsInt()).thenReturn(offerCount);
            when(workflow.accepts(99, "10001")).thenReturn(true);

            listener(workflow).onSlashCommandInteraction(event);

            org.mockito.Mockito.verify(workflow).start(eq(offerCount), eq(RestrictionMode.AUTO),
                    any(DiscordMemberNameResolver.class), any(), any());
        }
    }

    @Test
    void delegatesAllRestrictionModesTogetherWithTheOfferCount() {
        for (RestrictionMode mode : RestrictionMode.values()) {
            var workflow = mock(DiscordChallengeWorkflow.class);
            var event = slashEvent();
            var offers = mock(OptionMapping.class);
            var restriction = mock(OptionMapping.class);
            acknowledgement(event);
            when(event.getOption("angebote")).thenReturn(offers);
            when(offers.getAsInt()).thenReturn(3);
            when(event.getOption("einschraenkung")).thenReturn(restriction);
            when(restriction.getAsString()).thenReturn(mode.name());
            when(workflow.accepts(99, "10001")).thenReturn(true);

            listener(workflow).onSlashCommandInteraction(event);

            org.mockito.Mockito.verify(workflow).start(eq(3), eq(mode), any(DiscordMemberNameResolver.class), any(), any());
        }
    }

    @Test
    void defersComponentBeforeDelegatingToTheAdapterExecutor() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = mock(ButtonInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        when(event.getComponentId()).thenReturn("med:v1:resume:1");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("10001");
        when(workflow.accepts(99, "10001")).thenReturn(true);
        MessageEditCallbackAction acknowledgement = mock(MessageEditCallbackAction.class);
        InteractionHook hook = mock(InteractionHook.class);
        when(event.deferEdit()).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, hook);

        listener(workflow).onButtonInteraction(event);

        InOrder order = inOrder(event, workflow);
        order.verify(event).deferEdit();
        order.verify(workflow).component(eq("med:v1:resume:1"), eq("10001"), any(DiscordMemberNameResolver.class), any(), any());
    }

    @Test
    void resolvesCurrentGuildMemberNamesFromTheConfiguredStableDiscordId() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = mock(ButtonInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        var member = mock(Member.class);
        @SuppressWarnings("unchecked")
        CacheRestAction<Member> memberLookup = mock(CacheRestAction.class);
        when(event.getComponentId()).thenReturn("med:v1:resume:1");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("10001");
        when(workflow.accepts(99, "10001")).thenReturn(true);
        when(guild.retrieveMemberById("10002")).thenReturn(memberLookup);
        when(memberLookup.complete()).thenReturn(member);
        when(member.getEffectiveName()).thenReturn("Tobias aktuell");
        MessageEditCallbackAction acknowledgement = mock(MessageEditCallbackAction.class);
        when(event.deferEdit()).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, mock(InteractionHook.class));

        listener(workflow).onButtonInteraction(event);

        var resolver = ArgumentCaptor.forClass(DiscordMemberNameResolver.class);
        org.mockito.Mockito.verify(workflow).component(eq("med:v1:resume:1"), eq("10001"), resolver.capture(), any(), any());
        assertThat(resolver.getValue().resolve("10002", "Tobias gespeichert")).isEqualTo("Tobias aktuell");
        org.mockito.Mockito.verify(guild).retrieveMemberById("10002");
    }

    @Test
    void rejectsInteractionsFromWrongGuildWithoutStartingAnyWorkflow() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent();
        var reply = mock(ReplyCallbackAction.class);
        when(workflow.accepts(99, "10001")).thenReturn(false);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        listener(workflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verify(workflow, never()).start(any(Integer.class), any(), any(), any(), any());
    }

    @Test
    void rejectsUnknownDiscordUsersWithoutStartingAnyWorkflow() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent("99999");
        var reply = mock(ReplyCallbackAction.class);
        when(workflow.accepts(99, "99999")).thenReturn(false);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        listener(workflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verify(workflow, never()).start(any(Integer.class), any(), any(), any(), any());
    }

    private static DiscordJdaListener listener(DiscordChallengeWorkflow workflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), workflow, Runnable::run);
    }

    private static DiscordJdaListener ingredientListener(DiscordChallengeWorkflow challengeWorkflow,
                                                         DiscordIngredientLookupWorkflow lookupWorkflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), challengeWorkflow, lookupWorkflow, Runnable::run);
    }

    private static SlashCommandInteractionEvent slashEvent() {
        return slashEvent("10001");
    }

    private static SlashCommandInteractionEvent slashEvent(String userId) {
        var event = mock(SlashCommandInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        when(event.getName()).thenReturn("challenge");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        return event;
    }

    private static ButtonInteractionEvent buttonEvent(String componentId, DiscordIngredientLookupWorkflow lookupWorkflow) {
        var event = mock(ButtonInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        when(event.getComponentId()).thenReturn(componentId);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("10002");
        when(lookupWorkflow.accepts(99, "10002")).thenReturn(true);
        MessageEditCallbackAction acknowledgement = mock(MessageEditCallbackAction.class);
        when(event.deferEdit()).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, mock(InteractionHook.class));
        return event;
    }

    private static StringSelectInteractionEvent stringSelectEvent(String componentId, List<String> values,
                                                                  DiscordIngredientLookupWorkflow lookupWorkflow) {
        var event = mock(StringSelectInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        when(event.getComponentId()).thenReturn(componentId);
        when(event.getValues()).thenReturn(values);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("10001");
        when(lookupWorkflow.accepts(99, "10001")).thenReturn(true);
        MessageEditCallbackAction acknowledgement = mock(MessageEditCallbackAction.class);
        when(event.deferEdit()).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, mock(InteractionHook.class));
        return event;
    }

    private static ReplyCallbackAction acknowledgement(SlashCommandInteractionEvent event) {
        ReplyCallbackAction acknowledgement = mock(ReplyCallbackAction.class);
        InteractionHook hook = mock(InteractionHook.class);
        when(event.deferReply()).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, hook);
        return acknowledgement;
    }

    @SuppressWarnings("unchecked")
    private static void invokeAcknowledgement(InteractionCallbackAction<InteractionHook> acknowledgement, InteractionHook hook) {
        doAnswer(invocation -> {
            ((Consumer<InteractionHook>) invocation.getArgument(0)).accept(hook);
            return null;
        }).when(acknowledgement).queue(any(), any());
    }
}
