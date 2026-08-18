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
import java.util.Map;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DiscordJdaListenerTest {

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
        order.verify(workflow).start(eq(1), eq(RestrictionMode.AUTO), any(), any());
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

            org.mockito.Mockito.verify(workflow).start(eq(offerCount), eq(RestrictionMode.AUTO), any(), any());
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

            org.mockito.Mockito.verify(workflow).start(eq(3), eq(mode), any(), any());
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
        order.verify(workflow).component(eq("med:v1:resume:1"), eq("10001"), any(), any());
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
        org.mockito.Mockito.verify(workflow, never()).start(any(Integer.class), any(), any(), any());
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
        org.mockito.Mockito.verify(workflow, never()).start(any(Integer.class), any(), any(), any());
    }

    private static DiscordJdaListener listener(DiscordChallengeWorkflow workflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), workflow, Runnable::run);
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
