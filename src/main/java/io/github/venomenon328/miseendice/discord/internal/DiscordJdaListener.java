package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDA-only transport bridge. Every potentially long workflow is delegated after acknowledgement. */
final class DiscordJdaListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DiscordJdaListener.class);
    private final DiscordProperties properties;
    private final DiscordChallengeWorkflow workflow;
    private final DiscordIngredientLookupWorkflow ingredientLookupWorkflow;
    private final DiscordChallengeArchiveWorkflow archiveWorkflow;
    private final DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow;
    private final DiscordResultCaptureJdaListener resultCaptureListener;
    private final Executor executor;
    private final Executor autocompleteExecutor;

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow, Executor executor) {
        this(properties, workflow, null, null, null, null, executor);
    }

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                       DiscordIngredientLookupWorkflow ingredientLookupWorkflow, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, null, null, null, executor);
    }

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                       DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                       DiscordChallengeArchiveWorkflow archiveWorkflow, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, archiveWorkflow, null, null, executor);
    }

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                       DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                       DiscordChallengeArchiveWorkflow archiveWorkflow,
                       DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, archiveWorkflow, participantAdministrationWorkflow, null, executor);
    }

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                       DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                       DiscordChallengeArchiveWorkflow archiveWorkflow,
                       DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow,
                       DiscordResultCaptureJdaListener resultCaptureListener, Executor executor) {
        this(properties, workflow, ingredientLookupWorkflow, archiveWorkflow, participantAdministrationWorkflow,
                resultCaptureListener, executor, executor);
    }

    DiscordJdaListener(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                       DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                       DiscordChallengeArchiveWorkflow archiveWorkflow,
                       DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow,
                       DiscordResultCaptureJdaListener resultCaptureListener, Executor executor, Executor autocompleteExecutor) {
        this.properties = properties;
        this.workflow = workflow;
        this.ingredientLookupWorkflow = ingredientLookupWorkflow;
        this.archiveWorkflow = archiveWorkflow;
        this.participantAdministrationWorkflow = participantAdministrationWorkflow;
        this.resultCaptureListener = resultCaptureListener;
        this.executor = executor;
        this.autocompleteExecutor = autocompleteExecutor;
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
            guild.upsertCommand(ingredientsCommand())
                    .queue(null, failure -> log.warn("Discord country ingredient browse command registration failed", failure));
        }
        if (archiveWorkflow != null) {
            guild.upsertCommand(challengesCommand())
                    .queue(null, failure -> log.warn("Discord challenge archive command registration failed", failure));
        }
        if (participantAdministrationWorkflow != null) {
            guild.upsertCommand(participantCommand())
                    .queue(null, failure -> log.warn("Discord participant command registration failed", failure));
        }
        if (resultCaptureListener != null) {
            guild.upsertCommand(DiscordResultCaptureJdaListener.contextCommand())
                    .queue(null, failure -> log.warn("Discord result context command registration failed", failure));
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

    static SlashCommandData ingredientsCommand() {
        return Commands.slash("zutaten", "Aktive Zutaten eines kulinarischen Landes anzeigen")
                .addOptions(new OptionData(OptionType.STRING, "land", "Kulinarisches Land", true, true));
    }

    static SlashCommandData challengesCommand() {
        return Commands.slash("challenges", "Challenge-Status, Ergebnisse und Cards")
                .addSubcommands(
                        new SubcommandData("letzte", "Letzte bestätigte Challenge anzeigen"),
                        new SubcommandData("aktiv", "Aktive Challenges auflisten")
                                .addOption(OptionType.INTEGER, "seite", "Seite ab 1", false),
                        new SubcommandData("liste", "Bestätigte Challenges auflisten")
                                .addOption(OptionType.INTEGER, "seite", "Archivseite ab 1", false),
                        new SubcommandData("anzeigen", "Eine bestätigte Challenge anzeigen")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", true),
                        new SubcommandData("abschließen", "Eine aktive Challenge abschließen")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", false),
                        new SubcommandData("karte-setzen", "Challenge-Card setzen oder ersetzen")
                                .addOption(OptionType.ATTACHMENT, "bild", "PNG-Card mit 1200 × 1200 Pixeln", true)
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", false)
                                .addOption(OptionType.BOOLEAN, "ersetzen", "Bestehende Card ausdrücklich ersetzen", false),
                        new SubcommandData("karte-entfernen", "Challenge-Card entfernen")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", true),
                        new SubcommandData("ergebnis-bearbeiten", "Gespeichertes Ergebnis bearbeiten")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", true)
                                .addOption(OptionType.USER, "person", "Ergebnis-Person", true),
                        new SubcommandData("ergebnis-entfernen", "Gespeichertes Ergebnis entfernen")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", true)
                                .addOption(OptionType.USER, "person", "Ergebnis-Person", true),
                        new SubcommandData("ergebnis-foto-setzen", "Ergebnisfoto setzen oder ersetzen")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", true)
                                .addOption(OptionType.USER, "person", "Ergebnis-Person", true)
                                .addOption(OptionType.ATTACHMENT, "bild", "PNG- oder JPEG-Ergebnisfoto", true)
                                .addOption(OptionType.BOOLEAN, "ersetzen", "Vorhandenes Foto ausdrücklich ersetzen", false),
                        new SubcommandData("ergebnis-foto-entfernen", "Ergebnisfoto entfernen")
                                .addOption(OptionType.INTEGER, "nummer", "Öffentliche Challenge-Nummer", true)
                                .addOption(OptionType.USER, "person", "Ergebnis-Person", true));
    }

    static SlashCommandData participantCommand() {
        return Commands.slash("teilnehmer", "Teilnehmer und Standard-Elektorat verwalten")
                .addSubcommands(
                        new SubcommandData("anlegen", "Discord-Person als Teilnehmer anlegen")
                                .addOption(OptionType.USER, "person", "Discord-Person", true)
                                .addOption(OptionType.STRING, "name", "Fallback-Anzeigename", false),
                        new SubcommandData("aktivieren", "Teilnehmer aktivieren")
                                .addOption(OptionType.USER, "person", "Discord-Person", true),
                        new SubcommandData("deaktivieren", "Teilnehmer deaktivieren")
                                .addOption(OptionType.USER, "person", "Discord-Person", true),
                        new SubcommandData("elektorat-hinzufuegen", "Ins Standard-Elektorat aufnehmen")
                                .addOption(OptionType.USER, "person", "Discord-Person", true),
                        new SubcommandData("elektorat-entfernen", "Aus dem Standard-Elektorat entfernen")
                                .addOption(OptionType.USER, "person", "Discord-Person", true),
                        new SubcommandData("liste", "Teilnehmer auflisten"));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (resultCaptureListener != null && resultCaptureListener.handlesSlash(event)) {
            resultCaptureListener.onSlashCommandInteraction(event);
            return;
        }
        if ("teilnehmer".equals(event.getName())) {
            participantSlash(event);
            return;
        }
        if ("zutat".equals(event.getName())) {
            ingredientSlash(event);
            return;
        }
        if ("zutaten".equals(event.getName())) {
            ingredientsSlash(event);
            return;
        }
        if ("challenges".equals(event.getName())) {
            archiveSlash(event);
            return;
        }
        if (!"challenge".equals(event.getName())) {
            return;
        }
        if (!acceptsChallengeCommand(event.getGuild(), event.getMember())) {
            event.reply("Dieser Command ist nur für Mitglieder mit der konfigurierten Challenge-Operator-Rolle verfügbar.")
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

    private void participantSlash(SlashCommandInteractionEvent event) {
        if (participantAdministrationWorkflow == null) {
            return;
        }
        if (!acceptsChallengeCommand(event.getGuild(), event.getMember())) {
            ephemeralReply(event, "Dieser Command ist nur für Mitglieder mit der konfigurierten Challenge-Operator-Rolle verfügbar.");
            return;
        }
        String subcommand = event.getSubcommandName();
        if ("liste".equals(subcommand)) {
            Guild guild = event.getGuild();
            event.deferReply(true).queue(hook -> executor.execute(() -> participantAdministrationWorkflow.list(memberNames(guild),
                    new HookParticipantAdministrationDelivery(hook))),
                    failure -> log.warn("Discord participant list acknowledgement failed", failure));
            return;
        }
        OptionMapping personOption = event.getOption("person");
        if (personOption == null || personOption.getAsUser() == null) {
            ephemeralReply(event, "`person` ist erforderlich.");
            return;
        }
        net.dv8tion.jda.api.entities.User person = personOption.getAsUser();
        String discordUserId = person.getId();
        if ("anlegen".equals(subcommand)) {
            OptionMapping nameOption = event.getOption("name");
            String fallbackName = nameOption == null ? person.getName() : nameOption.getAsString().strip();
            if (fallbackName.isBlank()) {
                ephemeralReply(event, "`name` darf nicht leer sein.");
                return;
            }
            event.deferReply(true).queue(hook -> executor.execute(() -> participantAdministrationWorkflow.create(
                    discordUserId, fallbackName, memberNames(event.getGuild()), new HookParticipantAdministrationDelivery(hook))),
                    failure -> log.warn("Discord participant create acknowledgement failed", failure));
            return;
        }
        if ("aktivieren".equals(subcommand)) {
            event.deferReply(true).queue(hook -> executor.execute(() -> participantAdministrationWorkflow.activate(
                    discordUserId, memberNames(event.getGuild()), new HookParticipantAdministrationDelivery(hook))),
                    failure -> log.warn("Discord participant activation acknowledgement failed", failure));
            return;
        }
        if ("deaktivieren".equals(subcommand)) {
            event.deferReply(true).queue(hook -> executor.execute(() -> participantAdministrationWorkflow.deactivate(
                    discordUserId, memberNames(event.getGuild()), new HookParticipantAdministrationDelivery(hook))),
                    failure -> log.warn("Discord participant deactivation acknowledgement failed", failure));
            return;
        }
        if ("elektorat-hinzufuegen".equals(subcommand)) {
            event.deferReply(true).queue(hook -> executor.execute(() -> participantAdministrationWorkflow.addToDefaultElectorate(
                    discordUserId, memberNames(event.getGuild()), new HookParticipantAdministrationDelivery(hook))),
                    failure -> log.warn("Discord electorate addition acknowledgement failed", failure));
            return;
        }
        if ("elektorat-entfernen".equals(subcommand)) {
            event.deferReply(true).queue(hook -> executor.execute(() -> participantAdministrationWorkflow.removeFromDefaultElectorate(
                    discordUserId, memberNames(event.getGuild()), new HookParticipantAdministrationDelivery(hook))),
                    failure -> log.warn("Discord electorate removal acknowledgement failed", failure));
            return;
        }
        ephemeralReply(event, "Dieser Teilnehmer-Command ist nicht bekannt.");
    }

    private void ingredientSlash(SlashCommandInteractionEvent event) {
        if (ingredientLookupWorkflow == null) {
            return;
        }
        if (!ingredientLookupWorkflow.acceptsGuild(event.getGuild() == null ? 0 : event.getGuild().getIdLong())) {
            event.reply("Dieser Command ist nur in der konfigurierten Guild verfügbar.")
                    .setEphemeral(true).queue();
            return;
        }
        OptionMapping option = event.getOption("suche");
        String searchText = option == null ? "" : option.getAsString();
        if (searchText.strip().isEmpty()) {
            event.reply("`suche` darf nicht leer sein.").setEphemeral(true).queue();
            return;
        }
        String ownerUserId = event.getUser().getId();
        event.deferReply().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.search(searchText, ownerUserId,
                new HookIngredientDelivery(hook, executor, ownerUserId), new HookIngredientFeedback(hook))),
                failure -> log.warn("Discord ingredient lookup acknowledgement failed", failure));
    }

    private void ingredientsSlash(SlashCommandInteractionEvent event) {
        if (ingredientLookupWorkflow == null) {
            return;
        }
        if (!ingredientLookupWorkflow.acceptsGuild(event.getGuild() == null ? 0 : event.getGuild().getIdLong())) {
            ephemeralReply(event, "Dieser Command ist nur in der konfigurierten Guild verfügbar.");
            return;
        }
        OptionMapping option = event.getOption("land");
        String country = option == null ? "" : option.getAsString();
        if (country.strip().isEmpty()) {
            ephemeralReply(event, "`land` darf nicht leer sein.");
            return;
        }
        String ownerUserId = event.getUser().getId();
        event.deferReply().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.browseCountry(country, ownerUserId,
                new HookIngredientDelivery(hook, executor, ownerUserId), new HookIngredientFeedback(hook))),
                failure -> log.warn("Discord country ingredient browse acknowledgement failed", failure));
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (ingredientLookupWorkflow == null || !"zutaten".equals(event.getName())
                || !"land".equals(event.getFocusedOption().getName())) {
            return;
        }
        if (!ingredientLookupWorkflow.acceptsGuild(event.getGuild() == null ? 0 : event.getGuild().getIdLong())) {
            event.replyChoices(List.of()).queue();
            return;
        }
        String searchText = event.getFocusedOption().getValue();
        // Discord does not permit deferring autocomplete interactions. Keep this bounded read independent from
        // deferred, potentially long-running commands on the primary Discord executor.
        autocompleteExecutor.execute(() -> {
            try {
                List<Choice> choices = ingredientLookupWorkflow.autocompleteCountries(searchText).stream()
                        .map(country -> new Choice(DiscordIngredientLookupRenderer.countryFlag(country.code()) + " "
                                + country.displayName(), country.code()))
                        .toList();
                event.replyChoices(choices).queue(null,
                        failure -> log.warn("Discord culinary-country autocomplete response failed", failure));
            } catch (RuntimeException exception) {
                log.warn("Discord culinary-country autocomplete failed", exception);
                event.replyChoices(List.of()).queue();
            }
        });
    }

    private void archiveSlash(SlashCommandInteractionEvent event) {
        if (archiveWorkflow == null) {
            return;
        }
        long guildId = event.getGuild() == null ? 0 : event.getGuild().getIdLong();
        if (!archiveWorkflow.acceptsGuild(guildId)) {
            ephemeralReply(event, "Dieser Command ist nur in der konfigurierten Guild verfügbar.");
            return;
        }
        String subcommand = event.getSubcommandName();
        if ("letzte".equals(subcommand)) {
            Guild guild = event.getGuild();
            event.deferReply().queue(hook -> executor.execute(() -> archiveWorkflow.latest(memberNames(guild),
                    new HookArchiveDelivery(hook, executor), new HookArchiveFeedback(hook))),
                    failure -> log.warn("Discord challenge archive acknowledgement failed", failure));
            return;
        }
        if ("aktiv".equals(subcommand) || "liste".equals(subcommand)) {
            int page = event.getOption("seite") == null ? 1 : event.getOption("seite").getAsInt();
            if (page < 1) {
                ephemeralReply(event, "`seite` muss mindestens 1 sein.");
                return;
            }
            event.deferReply().queue(hook -> executor.execute(() -> {
                if ("aktiv".equals(subcommand)) {
                    archiveWorkflow.active(page, new HookArchiveDelivery(hook, executor), new HookArchiveFeedback(hook));
                } else {
                    archiveWorkflow.list(page, new HookArchiveDelivery(hook, executor), new HookArchiveFeedback(hook));
                }
            }),
                    failure -> log.warn("Discord challenge archive acknowledgement failed", failure));
            return;
        }
        if ("anzeigen".equals(subcommand)) {
            Long challengeNumber = positiveChallengeNumber(event);
            if (challengeNumber == null) {
                ephemeralReply(event, "`nummer` muss positiv sein.");
                return;
            }
            Guild guild = event.getGuild();
            event.deferReply().queue(hook -> executor.execute(() -> archiveWorkflow.show(challengeNumber, memberNames(guild),
                    new HookArchiveDelivery(hook, executor), new HookArchiveFeedback(hook))),
                    failure -> log.warn("Discord challenge archive acknowledgement failed", failure));
            return;
        }
        if (!"karte-setzen".equals(subcommand) && !"karte-entfernen".equals(subcommand)
                && !"abschließen".equals(subcommand)) {
            ephemeralReply(event, "Dieser Challenge-Archiv-Command ist nicht bekannt.");
            return;
        }
        if (!acceptsChallengeCommand(event.getGuild(), event.getMember())) {
            ephemeralReply(event, "Dieser Command ist nur für Mitglieder mit der konfigurierten Challenge-Operator-Rolle verfügbar.");
            return;
        }
        if ("abschließen".equals(subcommand)) {
            OptionMapping numberOption = event.getOption("nummer");
            Long challengeNumber = numberOption == null ? null : numberOption.getAsLong();
            if (challengeNumber != null && challengeNumber < 1) {
                ephemeralReply(event, "`nummer` muss positiv sein.");
                return;
            }
            Guild guild = event.getGuild();
            event.deferReply(true).queue(hook -> executor.execute(() -> archiveWorkflow.complete(challengeNumber, memberNames(guild),
                    new HookArchiveMutationDelivery(hook, executor))),
                    failure -> log.warn("Discord challenge completion acknowledgement failed", failure));
            return;
        }
        if ("karte-entfernen".equals(subcommand)) {
            Long challengeNumber = positiveChallengeNumber(event);
            if (challengeNumber == null) {
                ephemeralReply(event, "`nummer` muss positiv sein.");
                return;
            }
            Guild guild = event.getGuild();
            event.deferReply(true).queue(hook -> executor.execute(() -> archiveWorkflow.removeCard(challengeNumber, memberNames(guild),
                    new HookArchiveMutationDelivery(hook, executor))),
                    failure -> log.warn("Discord challenge Card acknowledgement failed", failure));
            return;
        }
        Attachment attachment = event.getOption("bild") == null ? null : event.getOption("bild").getAsAttachment();
        if (attachment == null) {
            ephemeralReply(event, "`bild` ist erforderlich.");
            return;
        }
        if (attachment.getSize() > DiscordChallengeArchiveWorkflow.MAX_CARD_BYTES) {
            ephemeralReply(event, "Das Bild darf höchstens 5 MiB groß sein.");
            return;
        }
        String contentType = attachment.getContentType();
        if (contentType != null && !contentType.isBlank() && !"image/png".equalsIgnoreCase(contentType.strip())) {
            ephemeralReply(event, "Das Bild muss als PNG hochgeladen werden.");
            return;
        }
        OptionMapping numberOption = event.getOption("nummer");
        Long challengeNumber = numberOption == null ? null : numberOption.getAsLong();
        if (challengeNumber != null && challengeNumber < 1) {
            ephemeralReply(event, "`nummer` muss positiv sein.");
            return;
        }
        boolean replaceExisting = event.getOption("ersetzen") != null && event.getOption("ersetzen").getAsBoolean();
        Guild guild = event.getGuild();
        event.deferReply(true).queue(hook -> executor.execute(() -> archiveWorkflow.setCard(challengeNumber, replaceExisting,
                new JdaAttachmentSource(attachment), memberNames(guild), new HookArchiveMutationDelivery(hook, executor))),
                failure -> log.warn("Discord challenge Card acknowledgement failed", failure));
    }

    private static Long positiveChallengeNumber(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("nummer");
        if (option == null || option.getAsLong() < 1) {
            return null;
        }
        return option.getAsLong();
    }

    private static void ephemeralReply(SlashCommandInteractionEvent event, String message) {
        event.reply(message).setEphemeral(true).setAllowedMentions(List.of()).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (resultCaptureListener != null && resultCaptureListener.handlesButton(event)) {
            resultCaptureListener.onButtonInteraction(event);
            return;
        }
        if (ingredientLookupWorkflow != null && (DiscordIngredientComponentId.isCountryPage(event.getComponentId())
                || DiscordIngredientComponentId.isCountryBack(event.getComponentId()))) {
            if (!ingredientLookupWorkflow.acceptsGuild(event.getGuild() == null ? 0 : event.getGuild().getIdLong())) {
                event.reply("Diese Zutaten-Navigation ist hier nicht erlaubt.").setEphemeral(true).queue();
                return;
            }
            String userId = event.getUser().getId();
            event.deferEdit().queue(hook -> executor.execute(() -> {
                if (DiscordIngredientComponentId.isCountryPage(event.getComponentId())) {
                    ingredientLookupWorkflow.countryPage(event.getComponentId(), userId,
                            new HookIngredientDelivery(hook, executor, userId), new HookIngredientFeedback(hook));
                } else {
                    ingredientLookupWorkflow.countryBack(event.getComponentId(), userId,
                            new HookIngredientDelivery(hook, executor, userId), new HookIngredientFeedback(hook));
                }
            }), failure -> log.warn("Discord country ingredient component acknowledgement failed", failure));
            return;
        }
        if (!event.getComponentId().startsWith("med:")) {
            return;
        }
        if (!acceptsChallengeInteraction(event.getGuild() == null ? 0 : event.getGuild().getIdLong(),
                event.getUser().getId())) {
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
        if (resultCaptureListener != null && resultCaptureListener.handlesStringSelect(event)) {
            resultCaptureListener.onStringSelectInteraction(event);
            return;
        }
        if (ingredientLookupWorkflow == null) {
            return;
        }
        String componentId = event.getComponentId();
        if (!DiscordIngredientComponentId.isSelection(componentId)
                && !DiscordIngredientComponentId.isNavigationSelect(componentId)
                && !DiscordIngredientComponentId.isCountrySelect(componentId)) {
            return;
        }
        if (!ingredientLookupWorkflow.acceptsGuild(event.getGuild() == null ? 0 : event.getGuild().getIdLong())) {
            event.reply("Diese Zutaten-Auswahl ist hier nicht erlaubt.").setEphemeral(true).queue();
            return;
        }
        String userId = event.getUser().getId();
        if (DiscordIngredientComponentId.isNavigationSelect(componentId)) {
            event.deferEdit().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.navigateSelect(componentId,
                    event.getValues(), userId, new HookIngredientDelivery(hook, executor, userId),
                    new HookIngredientFeedback(hook))),
                    failure -> log.warn("Discord ingredient navigation acknowledgement failed", failure));
            return;
        }
        if (DiscordIngredientComponentId.isCountrySelect(componentId)) {
            event.deferEdit().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.countryComponent(componentId,
                    event.getValues(), userId, new HookIngredientDelivery(hook, executor, userId),
                    new HookIngredientFeedback(hook))),
                    failure -> log.warn("Discord country ingredient selection acknowledgement failed", failure));
            return;
        }
        event.deferEdit().queue(hook -> executor.execute(() -> ingredientLookupWorkflow.component(componentId, event.getValues(),
                userId, new HookIngredientDelivery(hook, executor, userId), new HookIngredientFeedback(hook))),
                failure -> log.warn("Discord ingredient selection acknowledgement failed", failure));
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (resultCaptureListener != null) {
            resultCaptureListener.onMessageContextInteraction(event);
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        if (resultCaptureListener != null && resultCaptureListener.handlesEntitySelect(event)) {
            resultCaptureListener.onEntitySelectInteraction(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (resultCaptureListener != null && resultCaptureListener.handlesModal(event)) {
            resultCaptureListener.onModalInteraction(event);
        }
    }

    private boolean acceptsChallengeCommand(Guild guild, Member member) {
        return guild != null && guild.getIdLong() == properties.guildId() && member != null
                && member.getRoles().stream().anyMatch(role -> role.getIdLong() == properties.challengeOperatorRoleId());
    }

    private boolean acceptsChallengeInteraction(long guildId, String userId) {
        return workflow.accepts(guildId, userId);
    }

    private static RestrictionMode restrictionMode(OptionMapping option) {
        return option == null ? RestrictionMode.AUTO : RestrictionMode.valueOf(option.getAsString());
    }

    private DiscordMemberNameResolver memberNames(Guild guild) {
        if (guild == null) {
            return DiscordMemberNameResolver.storedFallback();
        }
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
        private final String ownerUserId;

        private HookIngredientDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook, Executor executor,
                                       String ownerUserId) {
            this.hook = hook;
            this.executor = executor;
            this.ownerUserId = ownerUserId;
        }

        @Override
        public void replace(DiscordIngredientLookupRenderer.RenderedResponse response, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            hook.editOriginal(ingredientMessage(response, ownerUserId)).queue(ignored -> executor.execute(delivered), failed);
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

    private static final class HookIngredientFeedback implements DiscordIngredientLookupWorkflow.Feedback {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;

        private HookIngredientFeedback(net.dv8tion.jda.api.interactions.InteractionHook hook) {
            this.hook = hook;
        }

        @Override
        public void staleOrRejected(String message) {
            hook.sendMessage(message).setEphemeral(true).queue();
        }

        @Override
        public void technicalFailure(Throwable exception) {
            log.error("Discord ingredient lookup interaction failed", exception);
            hook.sendMessage("Die Zutatenabfrage konnte technisch nicht verarbeitet werden. Bitte später erneut versuchen.")
                    .setEphemeral(true).queue();
        }
    }

    private static final class HookParticipantAdministrationDelivery
            implements DiscordParticipantAdministrationWorkflow.Delivery {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;

        private HookParticipantAdministrationDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook) {
            this.hook = hook;
        }

        @Override
        public void success(String message) {
            hook.editOriginal(ephemeralEdit(message)).queue();
        }

        @Override
        public void rejected(String message) {
            hook.editOriginal(ephemeralEdit(message)).queue();
        }

        @Override
        public void technicalFailure(Throwable exception) {
            log.error("Discord participant administration failed", exception);
            hook.editOriginal(ephemeralEdit("Die Teilnehmerverwaltung konnte technisch nicht verarbeitet werden. "
                    + "Bitte später erneut versuchen.")).queue();
        }
    }

    private static final class HookArchiveDelivery implements DiscordChallengeArchiveWorkflow.Delivery {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;
        private final Executor executor;

        private HookArchiveDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook, Executor executor) {
            this.hook = hook;
            this.executor = executor;
        }

        @Override
        public void replace(DiscordChallengeArchiveRenderer.RenderedResponse response, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            hook.editOriginal(archiveEditMessage(response)).queue(ignored -> publishArchiveFollowUps(hook,
                    resultFollowUps(response), 0, executor, delivered, failed), failed);
        }
    }

    private static final class HookArchiveFeedback implements DiscordChallengeArchiveWorkflow.Feedback {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;

        private HookArchiveFeedback(net.dv8tion.jda.api.interactions.InteractionHook hook) {
            this.hook = hook;
        }

        @Override
        public void rejected(String message) {
            hook.editOriginal(ephemeralEdit(message)).queue();
        }

        @Override
        public void technicalFailure(Throwable exception) {
            log.error("Discord challenge archive interaction failed", exception);
            hook.editOriginal(ephemeralEdit("Das Challenge-Archiv konnte technisch nicht geladen werden. Bitte später erneut versuchen."))
                    .queue();
        }
    }

    private static final class HookArchiveMutationDelivery implements DiscordChallengeArchiveWorkflow.MutationDelivery {
        private final net.dv8tion.jda.api.interactions.InteractionHook hook;
        private final Executor executor;

        private HookArchiveMutationDelivery(net.dv8tion.jda.api.interactions.InteractionHook hook, Executor executor) {
            this.hook = hook;
            this.executor = executor;
        }

        @Override
        public void rejected(String message) {
            hook.editOriginal(ephemeralEdit(message)).queue();
        }

        @Override
        public void technicalFailure(Throwable exception) {
            log.error("Discord challenge Card mutation failed", exception);
            hook.editOriginal(ephemeralEdit("Die Card konnte technisch nicht verarbeitet werden. Bitte später erneut versuchen."))
                    .queue();
        }

        @Override
        public void publish(DiscordChallengeArchiveRenderer.RenderedChallenge challenge, Runnable delivered,
                            java.util.function.Consumer<Throwable> failed) {
            hook.editOriginal(ephemeralEdit("Die Änderung wurde gespeichert. Die öffentliche Detailansicht wird gesendet."))
                    .queue(ignored -> hook.sendMessage(archiveCreateMessage(challenge.challenge()))
                                    .setAllowedMentions(List.of())
                                    .setEphemeral(false)
                                    .queue(message -> publishArchiveFollowUps(hook, challenge.resultFollowUps(), 0,
                                            executor, delivered, failed), failed),
                            failed);
        }

        @Override
        public void persistedAndPublished(String message) {
            hook.editOriginal(ephemeralEdit(message + " Die Detailansicht wurde öffentlich gepostet.")).queue();
        }

        @Override
        public void persistedButNotPublished(String message) {
            hook.editOriginal(ephemeralEdit(message + " Die öffentliche Darstellung konnte jedoch nicht vollständig gesendet werden. "
                    + "Mit `/challenges anzeigen` ist der gespeicherte Stand weiterhin abrufbar.")).queue();
        }
    }

    private static final class JdaAttachmentSource implements DiscordChallengeArchiveWorkflow.CardUploadSource {
        private final Attachment attachment;

        private JdaAttachmentSource(Attachment attachment) {
            this.attachment = attachment;
        }

        @Override
        public long declaredSize() {
            return attachment.getSize();
        }

        @Override
        public String declaredContentType() {
            return attachment.getContentType();
        }

        @Override
        public String originalFilename() {
            return attachment.getFileName();
        }

        @Override
        public byte[] download() {
            try (InputStream stream = attachment.getProxy().download().join()) {
                return stream.readAllBytes();
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("Discord attachment download failed", exception);
            }
        }
    }

    static MessageEditData archiveEditMessage(DiscordChallengeArchiveRenderer.RenderedResponse response) {
        MessageEditBuilder builder = new MessageEditBuilder().setAllowedMentions(List.of());
        if (response instanceof DiscordChallengeArchiveRenderer.RenderedText text) {
            return builder.setContent(text.content()).setEmbeds(List.of()).setComponents(List.of()).build();
        }
        DiscordChallengeArchiveRenderer.RenderedDetail detail = response instanceof DiscordChallengeArchiveRenderer.RenderedChallenge challenge
                ? challenge.challenge()
                : (DiscordChallengeArchiveRenderer.RenderedDetail) response;
        EmbedBuilder embed = new EmbedBuilder().setTitle(detail.title()).setDescription(detail.description());
        if (detail.hasAttachment()) {
            embed.setImage("attachment://" + detail.attachmentFilename());
            builder.setAttachments(List.of(FileUpload.fromData(detail.attachmentBytes(), detail.attachmentFilename())));
        }
        return builder.setContent("").setEmbeds(List.of(embed.build())).setComponents(List.of()).build();
    }

    static MessageCreateData archiveCreateMessage(DiscordChallengeArchiveRenderer.RenderedDetail detail) {
        EmbedBuilder embed = new EmbedBuilder().setTitle(detail.title()).setDescription(detail.description());
        MessageCreateBuilder builder = new MessageCreateBuilder().setAllowedMentions(List.of()).setContent("");
        if (detail.hasAttachment()) {
            embed.setImage("attachment://" + detail.attachmentFilename());
            builder.setFiles(FileUpload.fromData(detail.attachmentBytes(), detail.attachmentFilename()));
        }
        return builder.setEmbeds(List.of(embed.build())).build();
    }

    private static List<DiscordChallengeArchiveRenderer.RenderedDetail> resultFollowUps(
            DiscordChallengeArchiveRenderer.RenderedResponse response
    ) {
        return response instanceof DiscordChallengeArchiveRenderer.RenderedChallenge challenge
                ? challenge.resultFollowUps()
                : List.of();
    }

    static void publishArchiveFollowUps(
            net.dv8tion.jda.api.interactions.InteractionHook hook,
            List<DiscordChallengeArchiveRenderer.RenderedDetail> followUps,
            int index,
            Executor executor,
            Runnable delivered,
            java.util.function.Consumer<Throwable> failed
    ) {
        if (index >= followUps.size()) {
            executor.execute(delivered);
            return;
        }
        hook.sendMessage(archiveCreateMessage(followUps.get(index)))
                .setAllowedMentions(List.of())
                .setEphemeral(false)
                .queue(message -> publishArchiveFollowUps(hook, followUps, index + 1, executor, delivered, failed), failed);
    }

    private static net.dv8tion.jda.api.utils.messages.MessageEditData ephemeralEdit(String message) {
        return new MessageEditBuilder().setContent(message).setAllowedMentions(List.of()).build();
    }

    private static List<ActionRow> rows(List<DiscordChallengeRenderer.Component> components) {
        if (components.isEmpty()) {
            return List.of();
        }
        return List.of(ActionRow.of(components.stream()
                .map(component -> Button.primary(component.customId(), component.label())).toList()));
    }

    static net.dv8tion.jda.api.utils.messages.MessageEditData ingredientMessage(
            DiscordIngredientLookupRenderer.RenderedResponse response) {
        return ingredientMessage(response, null);
    }

    static net.dv8tion.jda.api.utils.messages.MessageEditData ingredientMessage(
            DiscordIngredientLookupRenderer.RenderedResponse response, String ownerUserId) {
        MessageEditBuilder builder = new MessageEditBuilder().setAllowedMentions(List.of());
        if (response instanceof DiscordIngredientLookupRenderer.RenderedText text) {
            return builder.setContent(text.content()).setEmbeds(List.of()).setComponents(List.of()).build();
        }
        if (response instanceof DiscordIngredientLookupRenderer.RenderedCountryText text) {
            return builder.setContent(text.content()).setEmbeds(List.of())
                    .setComponents(List.of(ActionRow.of(countryBackButton(text.countryOrigin(), ownerUserId)))).build();
        }
        if (response instanceof DiscordIngredientLookupRenderer.RenderedSelection selection) {
            return builder.setContent(selection.content()).setEmbeds(List.of())
                    .setComponents(List.of(ActionRow.of(selectMenu(selection.customId(), "Zutat auswählen", selection.options())))).build();
        }
        if (response instanceof DiscordIngredientLookupRenderer.RenderedCountryIngredients countryPage) {
            EmbedBuilder countryEmbed = new EmbedBuilder().setTitle(countryPage.title()).setDescription(countryPage.description())
                    .setColor(DiscordIngredientLookupRenderer.CARD_COLOR);
            List<ActionRow> actionRows = new java.util.ArrayList<>();
            if (!countryPage.options().isEmpty()) {
                actionRows.add(ActionRow.of(selectMenu(DiscordIngredientComponentId.countrySelect(countryPage.countryContext(), ownerUserId),
                        "🥢 Zutat anzeigen …", countryPage.options())));
            }
            if (countryPage.hasPreviousPage() || countryPage.hasNextPage()) {
                int page = countryPage.countryContext().page();
                Button previous = Button.secondary(DiscordIngredientComponentId.countryPage(countryPage.countryContext(), ownerUserId,
                        Math.max(1, page - 1)), "◀ Zurück").withDisabled(!countryPage.hasPreviousPage());
                Button next = Button.secondary(DiscordIngredientComponentId.countryPage(countryPage.countryContext(), ownerUserId,
                        page + 1), "Weiter ▶").withDisabled(!countryPage.hasNextPage());
                actionRows.add(ActionRow.of(previous, next));
            }
            return builder.setContent("").setEmbeds(List.of(countryEmbed.build())).setComponents(actionRows).build();
        }
        DiscordIngredientLookupRenderer.RenderedEmbed embed = (DiscordIngredientLookupRenderer.RenderedEmbed) response;
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(embed.title())
                .setDescription(embed.description())
                .setColor(embed.color());
        embed.fields().forEach(field -> embedBuilder.addField(field.name(), field.value(), field.inline()));
        return builder.setContent("").setEmbeds(List.of(embedBuilder.build()))
                .setComponents(ingredientRows(embed.navigationRows(), ownerUserId, embed.countryOrigin())).build();
    }

    private static List<ActionRow> ingredientRows(List<DiscordIngredientLookupRenderer.NavigationRow> rows,
                                                  String ownerUserId,
                                                  DiscordIngredientLookupRenderer.CountryBrowseOrigin countryOrigin) {
        List<ActionRow> actionRows = new java.util.ArrayList<>(rows.stream().map(row -> {
            DiscordIngredientLookupRenderer.NavigationSelectRow select =
                    (DiscordIngredientLookupRenderer.NavigationSelectRow) row;
            String customId = ownerUserId == null ? select.customId()
                    : DiscordIngredientComponentId.bindNavigationOwner(select.customId(), ownerUserId,
                    countryOrigin == null ? null : countryOrigin.context());
            return ActionRow.of(selectMenu(customId, select.placeholder(), select.options()));
        }).toList());
        if (countryOrigin != null) {
            actionRows.add(ActionRow.of(countryBackButton(countryOrigin, ownerUserId)));
        }
        return List.copyOf(actionRows);
    }

    private static Button countryBackButton(DiscordIngredientLookupRenderer.CountryBrowseOrigin origin, String ownerUserId) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Country return navigation requires an owner");
        }
        String label = "↩ Zurück zu " + origin.countryDisplayName();
        if (label.length() > 80) {
            label = label.substring(0, 79).stripTrailing() + "…";
        }
        return Button.secondary(DiscordIngredientComponentId.countryBack(origin.context(), ownerUserId), label);
    }

    private static StringSelectMenu selectMenu(String customId, String placeholder,
                                                List<DiscordIngredientLookupRenderer.SelectionOption> options) {
        return StringSelectMenu.create(customId)
                .setPlaceholder(placeholder)
                .setRequiredRange(1, 1)
                .addOptions(options.stream().map(option -> {
                    SelectOption selectOption = SelectOption.of(option.label(), option.value());
                    return option.description() == null ? selectOption : selectOption.withDescription(option.description());
                }).toList())
                .build();
    }
}
