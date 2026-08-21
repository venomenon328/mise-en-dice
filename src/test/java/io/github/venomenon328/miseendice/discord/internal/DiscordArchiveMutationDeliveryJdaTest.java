package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiscordArchiveMutationDeliveryJdaTest {

    @Test
    void resolvesEphemeralOriginalBeforeSendingExplicitlyPublicMutationFollowUp() {
        DiscordChallengeWorkflow challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        DiscordChallengeArchiveWorkflow archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        Guild guild = mock(Guild.class);
        Member member = mock(Member.class);
        Role role = mock(Role.class);
        OptionMapping number = mock(OptionMapping.class);
        ReplyCallbackAction acknowledgement = mock(ReplyCallbackAction.class);
        InteractionHook hook = mock(InteractionHook.class);

        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("karte-entfernen");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getMember()).thenReturn(member);
        when(member.getRoles()).thenReturn(List.of(role));
        when(role.getIdLong()).thenReturn(77777L);
        when(event.getOption("nummer")).thenReturn(number);
        when(number.getAsLong()).thenReturn(4L);
        when(archiveWorkflow.acceptsGuild(99L)).thenReturn(true);
        when(event.deferReply(true)).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, hook);

        DiscordJdaListener listener = new DiscordJdaListener(
                new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"),
                        Map.of("GEORGIA", "10001", "TOBIAS", "10002")),
                challengeWorkflow, null, archiveWorkflow, Runnable::run);
        listener.onSlashCommandInteraction(event);

        ArgumentCaptor<DiscordChallengeArchiveWorkflow.MutationDelivery> deliveryCaptor =
                ArgumentCaptor.forClass(DiscordChallengeArchiveWorkflow.MutationDelivery.class);
        verify(archiveWorkflow).removeCard(org.mockito.ArgumentMatchers.eq(4L), deliveryCaptor.capture());

        @SuppressWarnings("unchecked")
        WebhookMessageEditAction<Message> editAction = mock(WebhookMessageEditAction.class);
        @SuppressWarnings("unchecked")
        WebhookMessageCreateAction<Message> createAction = mock(WebhookMessageCreateAction.class);
        when(hook.editOriginal(any(MessageEditData.class))).thenReturn(editAction);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Message> success = invocation.getArgument(0);
            success.accept(mock(Message.class));
            return null;
        }).when(editAction).queue(any(), any());
        when(hook.sendMessage(any(MessageCreateData.class))).thenReturn(createAction);
        when(createAction.setAllowedMentions(any())).thenReturn(createAction);
        when(createAction.setEphemeral(false)).thenReturn(createAction);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<Message> success = invocation.getArgument(0);
            success.accept(mock(Message.class));
            return null;
        }).when(createAction).queue(any(), any());

        Runnable delivered = mock(Runnable.class);
        Consumer<Throwable> failed = failure -> {
            throw new AssertionError(failure);
        };
        DiscordChallengeArchiveRenderer.RenderedDetail detail =
                new DiscordChallengeArchiveRenderer.RenderedDetail(
                        "Challenge #4", "Bestätigt am 21. August 2026", null, null);

        deliveryCaptor.getValue().publish(detail, delivered, failed);

        var order = inOrder(hook, editAction, createAction);
        order.verify(hook).editOriginal(any(MessageEditData.class));
        order.verify(editAction).queue(any(), any());
        order.verify(hook).sendMessage(any(MessageCreateData.class));
        order.verify(createAction).setEphemeral(false);
        order.verify(createAction).queue(any(), any());
        verify(delivered).run();

        ArgumentCaptor<MessageEditData> editData = ArgumentCaptor.forClass(MessageEditData.class);
        verify(hook).editOriginal(editData.capture());
        assertThat(editData.getValue().getContent()).contains("gespeichert", "öffentliche Detailansicht");
    }

    @SuppressWarnings("unchecked")
    private static void invokeAcknowledgement(
            InteractionCallbackAction<InteractionHook> acknowledgement,
            InteractionHook hook
    ) {
        doAnswer(invocation -> {
            ((Consumer<InteractionHook>) invocation.getArgument(0)).accept(hook);
            return null;
        }).when(acknowledgement).queue(any(), any());
    }
}
