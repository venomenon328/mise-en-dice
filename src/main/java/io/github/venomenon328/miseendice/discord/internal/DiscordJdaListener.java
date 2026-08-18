package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.util.List;
import java.util.concurrent.Executor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDA-only transport bridge. Every potentially long workflow is delegated after acknowledgement. */
final class DiscordJdaListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DiscordJdaListener.class);
    private final DiscordProperties properties;
    private final DiscordChallengeWorkflow workflow;
    private final Executor executor;

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow, Executor executor) {
        this.properties = properties;
        this.workflow = workflow;
        this.executor = executor;
    }

    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(properties.guildId());
        if (guild == null) {
            log.warn("Configured Discord guild is not available to the bot");
            return;
        }
        guild.upsertCommand(challengeCommand())
                .queue(null, failure -> log.warn("Discord slash command registration failed", failure));
    }

    static SlashCommandData challengeCommand() {
        return Commands.slash("challenge", "Neue Koch-Challenge vorbereiten")
                .addOption(OptionType.INTEGER, "angebote", "Anzahl der Angebote (1 bis 3)", false)
                .addOptions(new OptionData(OptionType.STRING, "einschraenkung",
                        "Einschränkungen automatisch, nie oder für jeden Kandidaten", false)
                        .addChoice("automatisch", RestrictionMode.AUTO.name())
                        .addChoice("keine", RestrictionMode.NONE.name())
                        .addChoice("erzwingen", RestrictionMode.REQUIRED.name()));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"challenge".equals(event.getName())) {
            return;
        }
        if (!accepts(event.getGuild() == null ? 0 : event.getGuild().getIdLong(), event.getUser().getId())) {
            event.reply("Dieser Command ist nur für die konfigurierte Challenge-Gilde und ihre Teilnehmer verfügbar.")
                    .setEphemeral(true).queue();
            return;
        }
        int offers = event.getOption("angebote") == null ? 1 : event.getOption("angebote").getAsInt();
        if (offers < 1 || offers > 3) {
            event.reply("`angebote` muss zwischen 1 und 3 liegen.").setEphemeral(true).queue();
            return;
        }
        RestrictionMode restrictionMode;
        try {
            restrictionMode = restrictionMode(event.getOption("einschraenkung"));
        } catch (IllegalArgumentException exception) {
            event.reply("`einschraenkung` muss automatisch, keine oder erzwingen sein.").setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue(hook -> executor.execute(() -> workflow.start(offers, restrictionMode,
                new HookDelivery(hook), new HookFeedback(hook))),
                failure -> log.warn("Discord slash acknowledgement failed", failure));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("med:")) {
            return;
        }
        if (!accepts(event.getGuild() == null ? 0 : event.getGuild().getIdLong(), event.getUser().getId())) {
            event.reply("Diese Challenge-Interaktion ist hier nicht erlaubt.").setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue(hook -> executor.execute(() -> workflow.component(event.getComponentId(), event.getUser().getId(),
                new HookDelivery(hook), new HookFeedback(hook))),
                failure -> log.warn("Discord component acknowledgement failed", failure));
    }

    private boolean accepts(long guildId, String userId) {
        return workflow.accepts(guildId, userId);
    }

    private static RestrictionMode restrictionMode(OptionMapping option) {
        return option == null ? RestrictionMode.AUTO : RestrictionMode.valueOf(option.getAsString());
    }

    private static final class HookDelivery implements DiscordChallengeWorkflow.Delivery {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;

        private HookDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook) {
            this.hook = hook;
        }

        @Override
        public void replace(DiscordChallengeRenderer.RenderedMessage message, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            hook.editOriginal(new MessageEditBuilder().setContent(message.content()).setComponents(rows(message.components())).build())
                    .queue(ignored -> delivered.run(), failed);
        }
    }

    private static final class HookFeedback implements DiscordChallengeWorkflow.Feedback {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;

        private HookFeedback(net.dv8tion.jda.api.interactions.InteractionHook hook) {
            this.hook = hook;
        }

        @Override
        public void success(String message) {
            hook.sendMessage(message).setEphemeral(true).queue();
        }

        @Override
        public void staleOrRejected(String message) {
            hook.sendMessage(message).setEphemeral(true).queue();
        }

        @Override
        public void technicalFailure(Throwable exception) {
            log.error("Discord challenge interaction failed", exception);
            hook.sendMessage("Die Challenge konnte technisch nicht verarbeitet werden. Bitte später erneut versuchen.")
                    .setEphemeral(true).queue();
        }
    }

    private static List<ActionRow> rows(List<DiscordChallengeRenderer.Component> components) {
        if (components.isEmpty()) {
            return List.of();
        }
        return List.of(ActionRow.of(components.stream()
                .map(component -> Button.primary(component.customId(), component.label())).toList()));
    }
}
