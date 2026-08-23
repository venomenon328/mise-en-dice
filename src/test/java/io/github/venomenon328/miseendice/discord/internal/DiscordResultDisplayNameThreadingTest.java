package io.github.venomenon328.miseendice.discord.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.EditPreparation;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.FormData;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.OperatorContext;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Saved;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.junit.jupiter.api.Test;

class DiscordResultDisplayNameThreadingTest {

    @Test
    void maintenanceUsesResolvedSlashMemberWithoutBlockingGuildLookup() {
        DiscordResultCaptureWorkflow workflow = mock(DiscordResultCaptureWorkflow.class);
        DiscordChallengeArchiveWorkflow archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        Guild guild = mock(Guild.class);
        Member operator = mock(Member.class);
        Role role = mock(Role.class);
        User caller = mock(User.class);
        OptionMapping number = mock(OptionMapping.class);
        OptionMapping personOption = mock(OptionMapping.class);
        User person = mock(User.class);
        Member personMember = mock(Member.class);
        ReplyCallbackAction reply = mock(ReplyCallbackAction.class);

        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getMember()).thenReturn(operator);
        when(operator.getRoles()).thenReturn(List.of(role));
        when(role.getIdLong()).thenReturn(77777L);
        when(event.getUser()).thenReturn(caller);
        when(caller.getId()).thenReturn("operator");
        when(event.getSubcommandName()).thenReturn("ergebnis-bearbeiten");
        when(event.getOption("nummer")).thenReturn(number);
        when(number.getAsLong()).thenReturn(4L);
        when(event.getOption("person")).thenReturn(personOption);
        when(personOption.getAsUser()).thenReturn(person);
        when(personOption.getAsMember()).thenReturn(personMember);
        when(person.getId()).thenReturn("10001");
        when(person.getName()).thenReturn("Global-Name");
        when(personMember.getEffectiveName()).thenReturn("Testsklave");
        when(workflow.startEditPreparation(any(OperatorContext.class), eq(4L), eq("10001"), eq("Testsklave")))
                .thenReturn(new EditPreparation("token", 4L, "Testsklave", "Gericht", List.of()));
        when(event.reply(any(MessageCreateData.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        listener(workflow, archiveWorkflow).onSlashCommandInteraction(event);

        verify(workflow).startEditPreparation(any(OperatorContext.class), eq(4L), eq("10001"), eq("Testsklave"));
        verify(personOption).getAsMember();
        verify(guild, never()).retrieveMemberById(anyString());
    }

    @Test
    void publicResultRenderingHappensBeforeEnteringTheRestCallback() {
        DiscordResultCaptureWorkflow workflow = mock(DiscordResultCaptureWorkflow.class);
        DiscordChallengeArchiveWorkflow archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        ModalInteractionEvent event = mock(ModalInteractionEvent.class);
        Guild guild = mock(Guild.class);
        Member operator = mock(Member.class);
        Role role = mock(Role.class);
        User caller = mock(User.class);
        ReplyCallbackAction acknowledgement = mock(ReplyCallbackAction.class);
        InteractionHook hook = mock(InteractionHook.class);
        @SuppressWarnings("unchecked")
        WebhookMessageEditAction<Message> editAction = mock(WebhookMessageEditAction.class);

        when(event.getModalId()).thenReturn("med-result:edit:token");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(99L);
        when(event.getMember()).thenReturn(operator);
        when(operator.getRoles()).thenReturn(List.of(role));
        when(role.getIdLong()).thenReturn(77777L);
        when(event.getUser()).thenReturn(caller);
        when(caller.getId()).thenReturn("operator");
        when(event.deferReply(true)).thenReturn(acknowledgement);
        invokeAcknowledgement(acknowledgement, hook);
        when(workflow.submitEdit(any(OperatorContext.class), eq("token"), any(FormData.class)))
                .thenReturn(new Saved(null, 4L, "Gespeichert.", false));
        DiscordChallengeArchiveRenderer.RenderedChallenge rendered = new DiscordChallengeArchiveRenderer.RenderedChallenge(
                new DiscordChallengeArchiveRenderer.RenderedDetail("Challenge #4", "Detail", null, null), List.of());
        when(archiveWorkflow.renderedDetail(eq(4L), any(DiscordMemberNameResolver.class))).thenReturn(rendered);
        when(hook.editOriginal(any(MessageEditData.class))).thenReturn(editAction);

        listener(workflow, archiveWorkflow).onModalInteraction(event);

        var order = inOrder(workflow, archiveWorkflow, hook);
        order.verify(workflow).submitEdit(any(OperatorContext.class), eq("token"), any(FormData.class));
        order.verify(archiveWorkflow).renderedDetail(eq(4L), any(DiscordMemberNameResolver.class));
        order.verify(hook).editOriginal(any(MessageEditData.class));
    }

    private static DiscordResultCaptureJdaListener listener(DiscordResultCaptureWorkflow workflow,
                                                             DiscordChallengeArchiveWorkflow archiveWorkflow) {
        return new DiscordResultCaptureJdaListener(
                new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"), Map.of()),
                workflow, archiveWorkflow, Runnable::run);
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
