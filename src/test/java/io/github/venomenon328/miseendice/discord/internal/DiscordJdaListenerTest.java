package io.github.venomenon328.miseendice.discord.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Map;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DiscordJdaListenerTest {

    @Test
    void defersSlashInteractionBeforeDelegatingTheDefaultOfferCount() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent();
        var acknowledgement = acknowledgement(event);
        var listener = listener(workflow);
        when(workflow.accepts(99, "10001")).thenReturn(true);

        listener.onSlashCommandInteraction(event);

        InOrder order = inOrder(event, workflow);
        order.verify(event).deferReply();
        order.verify(workflow).start(eq(1), any(), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
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
        org.mockito.Mockito.verify(workflow, never()).start(any(Integer.class), any(), any());
    }

    private static DiscordJdaListener listener(DiscordChallengeWorkflow workflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), workflow, Runnable::run);
    }

    private static SlashCommandInteractionEvent slashEvent() {
        var event = mock(SlashCommandInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        when(event.getName()).thenReturn("challenge");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("10001");
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
