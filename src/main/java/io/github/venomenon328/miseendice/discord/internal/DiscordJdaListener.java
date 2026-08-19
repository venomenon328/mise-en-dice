package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.util.List;
import java.util.concurrent.Executor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
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
    private final DiscordIngredientLookupWorkflow ingredientLookupWorkflow;
    private final Executor executor;

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow, Executor executor) {
        this(properties, workflow, null, executor);
    }

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                       DiscordIngredientLookupWorkflow ingredientLookupWorkflow, Executor executor) {
        this.properties = properties;
        this.workflow = workflow;
        this.ingredientLookupWorkflow = ingredientLookupWorkflow;
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
        if (ingredientLookupWorkflow != null) {
            guild.upsertCommand(ingredientCommand())
                    .queue(null, failure -> log.warn("Discord ingredient lookup command registration failed", failure));
        }
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

    static SlashCommandData ingredientCommand() {
        return Commands.slash("zutat", "Aktive Zutat im Katalog nachschlagen")
                .addOption(OptionType.STRING, "suche", "Name der Zutat", true);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if ("zutat".equals(event.getName())) {
            ingredientSlash(event);
            return;
        }
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
        Guild guild = event.getGuild();
        event.deferReply().queue(hook -> executor.execute(() -> workflow.start(offers, restrictionMode, memberNames(guild),
                new HookDelivery(hook, executor), new HookFeedback(hook))),
                failure -> log.warn("Discord slash acknowledgement failed", failure));
    }

    private void ingredientSlash(SlashCommandInteractionEvent event) {
        if (ingredientLookupWorkflow == null) {
            return;
        }
        if (!ingredientLookupWorkflow.accepts(event.getGuild() == null ? 0 : event.getGuild().getIdLong(), event.getUser().getId())) {
            event.reply("Dieser Command ist nur für die konfigurierte Challenge-Gilde und ihre Teilnehmer verfügbar.")
                    .setEphemeral(true).queue();
            return;
        }
        OptionMapping option = event.getOption("suche");
        String searchText = option == null ? "" : option.getAsString();
        if (searchText.strip().isEmpty()) {
            event.reply("`suche` darf nicht leer sein.").setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.search(searchText, event.getUser().getId(),
                new HookIngredientDelivery(hook, executor), new HookFeedback(hook))),
                failure -> log.warn("Discord ingredient lookup acknowledgement failed", failure));
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
        Guild guild = event.getGuild();
        event.deferEdit().queue(hook -> executor.execute(() -> workflow.component(event.getComponentId(), event.getUser().getId(),
                memberNames(guild), new HookDelivery(hook, executor), new HookFeedback(hook))),
                failure -> log.warn("Discord component acknowledgement failed", failure));
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("med:v1:ingredient:")) {
            return;
        }
        if (ingredientLookupWorkflow == null) {
            return;
        }
        if (!ingredientLookupWorkflow.accepts(event.getGuild() == null ? 0 : event.getGuild().getIdLong(), event.getUser().getId())) {
            event.reply("Diese Zutaten-Auswahl ist hier nicht erlaubt.").setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.component(event.getComponentId(), event.getValues(),
                event.getUser().getId(), new HookIngredientDelivery(hook, executor), new HookFeedback(hook))),
                failure -> log.warn("Discord ingredient selection acknowledgement failed", failure));
    }

    private boolean accepts(long guildId, String userId) {
        return workflow.accepts(guildId, userId);
    }

    private static RestrictionMode restrictionMode(OptionMapping option) {
        return option == null ? RestrictionMode.AUTO : RestrictionMode.valueOf(option.getAsString());
    }

    private DiscordMemberNameResolver memberNames(Guild guild) {
        return (userId, storedFallback) -> {
            try {
                Member member = guild.retrieveMemberById(userId).complete();
                return member.getEffectiveName();
            } catch (RuntimeException exception) {
                log.debug("Could not resolve current Discord member display name for user {}", userId, exception);
                return storedFallback;
            }
        };
    }

    private static final class HookDelivery implements DiscordChallengeWorkflow.Delivery {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;
        private final Executor executor;

        private HookDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook, Executor executor) {
            this.hook = hook;
            this.executor = executor;
        }

        @Override
        public void replace(DiscordChallengeRenderer.RenderedMessage message, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            hook.editOriginal(new MessageEditBuilder().setContent(message.content()).setComponents(rows(message.components())).build())
                    .queue(ignored -> executor.execute(delivered), failed);
        }
    }

    private static final class HookIngredientDelivery implements DiscordIngredientLookupWorkflow.Delivery {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;
        private final Executor executor;

        private HookIngredientDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook, Executor executor) {
            this.hook = hook;
            this.executor = executor;
        }

        @Override
        public void replace(DiscordIngredientLookupRenderer.RenderedResponse response, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            hook.editOriginal(ingredientMessage(response)).queue(ignored -> executor.execute(delivered), failed);
        }
    }

    private static final class HookFeedback implements DiscordChallengeWorkflow.Feedback, DiscordIngredientLookupWorkflow.Feedback {
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

    private static net.dv8tion.jda.api.utils.messages.MessageEditData ingredientMessage(
            DiscordIngredientLookupRenderer.RenderedResponse response) {
        MessageEditBuilder builder = new MessageEditBuilder().setAllowedMentions(List.of());
        if (response instanceof DiscordIngredientLookupRenderer.RenderedText text) {
            return builder.setContent(text.content()).setEmbeds(List.of()).setComponents(List.of()).build();
        }
        if (response instanceof DiscordIngredientLookupRenderer.RenderedSelection selection) {
            StringSelectMenu menu = StringSelectMenu.create(selection.customId())
                    .setPlaceholder("Zutat auswählen")
                    .setRequiredRange(1, 1)
                    .addOptions(selection.options().stream().map(option -> {
                        SelectOption selectOption = SelectOption.of(option.label(), option.value());
                        return option.description() == null ? selectOption : selectOption.withDescription(option.description());
                    }).toList())
                    .build();
            return builder.setContent(selection.content()).setEmbeds(List.of())
                    .setComponents(List.of(ActionRow.of(menu))).build();
        }
        DiscordIngredientLookupRenderer.RenderedEmbed embed = (DiscordIngredientLookupRenderer.RenderedEmbed) response;
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(embed.title());
        embed.fields().forEach(field -> embedBuilder.addField(field.name(), field.value(), false));
        return builder.setContent("").setEmbeds(List.of(embedBuilder.build())).setComponents(List.of()).build();
    }
}
