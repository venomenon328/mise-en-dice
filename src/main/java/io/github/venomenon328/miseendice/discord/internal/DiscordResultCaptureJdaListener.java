package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.CaptureSubmission;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.ConcretizationModal;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.EditPreparation;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.FormData;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.MappingComplete;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.MappingProgress;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.MappingStep;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.OperatorContext;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.PhotoSource;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Preparation;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Rejected;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.ReplaceConfirmation;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.ResultModal;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Saved;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.Executor;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JDA transport for result capture; all transient state and mutations stay in {@link DiscordResultCaptureWorkflow}. */
final class DiscordResultCaptureJdaListener {
    static final String CONTEXT_COMMAND_NAME = "Als Challenge-Ergebnis erfassen";
    private static final String PREFIX = "med-result:";
    private static final String DISH = "dish";
    private static final String DESCRIPTION = "description";
    private static final String EVALUATION = "evaluation";
    private static final String INGREDIENTS_ONE = "ingredients-1";
    private static final String INGREDIENTS_TWO = "ingredients-2";
    private static final String CATALOG_SEARCH = "catalog-search";
    private static final Logger log = LoggerFactory.getLogger(DiscordResultCaptureJdaListener.class);

    private final DiscordProperties properties;
    private final DiscordResultCaptureWorkflow workflow;
    private final DiscordChallengeArchiveWorkflow archiveWorkflow;
    private final Executor executor;

    DiscordResultCaptureJdaListener(DiscordProperties properties, DiscordResultCaptureWorkflow workflow,
                                    DiscordChallengeArchiveWorkflow archiveWorkflow, Executor executor) {
        this.properties = properties;
        this.workflow = workflow;
        this.archiveWorkflow = archiveWorkflow;
        this.executor = executor;
    }

    static CommandData contextCommand() {
        return Commands.message(CONTEXT_COMMAND_NAME);
    }

    boolean handlesSlash(SlashCommandInteractionEvent event) {
        if (!"challenges".equals(event.getName())) {
            return false;
        }
        return switch (event.getSubcommandName() == null ? "" : event.getSubcommandName()) {
            case "ergebnis-bearbeiten", "ergebnis-entfernen", "ergebnis-foto-setzen", "ergebnis-foto-entfernen" -> true;
            default -> false;
        };
    }

    void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!CONTEXT_COMMAND_NAME.equals(event.getName())) {
            return;
        }
        OperatorContext context = context(event.getGuild(), event.getMember(), event.getUser());
        if (!context.operator()) {
            reject(event, "Dieser Kontextbefehl ist nur in der konfigurierten Guild und für die Challenge-Operator-Rolle verfügbar.");
            return;
        }
        Message message = event.getTarget();
        try {
            Preparation preparation = workflow.startCapture(context, message.getContentRaw(), message.getAttachments().stream()
                    .map(JdaPhotoSource::new).map(PhotoSource.class::cast).toList());
            event.reply(preparationCreate(preparation)).setEphemeral(true).queue();
        } catch (Rejected rejected) {
            reject(event, rejected.getMessage());
        } catch (RuntimeException exception) {
            log.error("Discord result capture could not be prepared", exception);
            reject(event, "Die Ergebniserfassung konnte technisch nicht vorbereitet werden. Bitte später erneut versuchen.");
        }
    }

    void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        OperatorContext context = context(event.getGuild(), event.getMember(), event.getUser());
        if (!context.operator()) {
            reject(event, "Dieser Command ist nur in der konfigurierten Guild und für die Challenge-Operator-Rolle verfügbar.");
            return;
        }
        Long challengeNumber = positiveLong(event.getOption("nummer"));
        User person = event.getOption("person") == null ? null : event.getOption("person").getAsUser();
        if (challengeNumber == null || person == null) {
            reject(event, "`nummer` muss positiv und `person` muss gesetzt sein.");
            return;
        }
        try {
            switch (event.getSubcommandName()) {
                case "ergebnis-bearbeiten" -> event.reply(editPreparationCreate(
                        workflow.startEditPreparation(context, challengeNumber, person.getId())))
                        .setEphemeral(true).queue();
                case "ergebnis-entfernen" -> {
                    var confirmation = workflow.startRemove(context, challengeNumber, person.getId());
                    event.reply(removalConfirmation(confirmation)).setEphemeral(true).queue();
                }
                case "ergebnis-foto-setzen" -> {
                    Attachment attachment = event.getOption("bild") == null ? null
                            : event.getOption("bild").getAsAttachment();
                    if (attachment == null) {
                        reject(event, "`bild` ist erforderlich.");
                        return;
                    }
                    boolean replace = event.getOption("ersetzen") != null && event.getOption("ersetzen").getAsBoolean();
                    event.deferReply(true).queue(hook -> executor.execute(() -> directMutation(hook,
                            () -> workflow.setPhoto(context, challengeNumber, person.getId(),
                                    new JdaPhotoSource(attachment), replace))),
                            failure -> log.warn("Discord result photo acknowledgement failed", failure));
                }
                case "ergebnis-foto-entfernen" -> event.deferReply(true).queue(hook -> executor.execute(() ->
                                directMutation(hook, () -> workflow.removePhoto(context, challengeNumber, person.getId()))),
                        failure -> log.warn("Discord result photo removal acknowledgement failed", failure));
                default -> reject(event, "Dieser Ergebnis-Command ist nicht bekannt.");
            }
        } catch (Rejected rejected) {
            reject(event, rejected.getMessage());
        } catch (RuntimeException exception) {
            log.error("Discord result maintenance could not be prepared", exception);
            reject(event, "Die Ergebnisverwaltung konnte technisch nicht vorbereitet werden. Bitte später erneut versuchen.");
        }
    }

    boolean handlesButton(ButtonInteractionEvent event) {
        return event.getComponentId().startsWith(PREFIX);
    }

    void onButtonInteraction(ButtonInteractionEvent event) {
        String[] id = parts(event.getComponentId());
        OperatorContext context = context(event.getGuild(), event.getMember(), event.getUser());
        if (!context.operator()) {
            reject(event, "Diese Ergebnis-Interaktion ist hier nicht erlaubt.");
            return;
        }
        try {
            switch (id[0]) {
                case "open" -> event.replyModal(modal(workflow.captureModal(context, id[1]), "capture")).queue();
                case "edit-open" -> event.replyModal(modal(workflow.editModal(context, id[1]), "edit")).queue();
                case "concrete-capture" -> event.replyModal(concretizationModal(
                        workflow.captureConcretizationModal(context, id[1]), "concrete-capture")).queue();
                case "concrete-edit" -> event.replyModal(concretizationModal(
                        workflow.editConcretizationModal(context, id[1]), "concrete-edit")).queue();
                case "page" -> edit(event, preparationEdit(workflow.navigateChallenges(context, id[1],
                        Integer.parseInt(id[2]))));
                case "replace" -> event.deferEdit().queue(hook -> executor.execute(() -> captureSubmission(hook,
                                workflow.confirmCaptureReplacement(context, id[1]))),
                        failure -> log.warn("Discord result replacement acknowledgement failed", failure));
                case "remove" -> event.deferEdit().queue(hook -> executor.execute(() -> directMutation(hook,
                                () -> workflow.confirmRemove(context, id[1]))),
                        failure -> log.warn("Discord result removal acknowledgement failed", failure));
                case "map" -> edit(event, mappingEdit(workflow.startMapping(context, id[1])));
                case "map-search" -> event.replyModal(searchModal(id[1], Long.parseLong(id[2]), id[3])).queue();
                case "map-abort" -> {
                    workflow.abortMapping(context, id[1]);
                    edit(event, textEdit("Die Zutatenzuordnung wurde beendet. Das gespeicherte Ergebnis bleibt vollständig gültig."));
                }
                default -> reject(event, "Diese Ergebnis-Interaktion ist nicht mehr gültig.");
            }
        } catch (Rejected rejected) {
            reject(event, rejected.getMessage());
        } catch (RuntimeException exception) {
            log.error("Discord result button interaction failed", exception);
            reject(event, "Die Ergebnis-Interaktion konnte technisch nicht verarbeitet werden. Bitte später erneut versuchen.");
        }
    }

    boolean handlesStringSelect(StringSelectInteractionEvent event) {
        return event.getComponentId().startsWith(PREFIX);
    }

    void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String[] id = parts(event.getComponentId());
        OperatorContext context = context(event.getGuild(), event.getMember(), event.getUser());
        if (!context.operator()) {
            reject(event, "Diese Ergebnis-Auswahl ist hier nicht erlaubt.");
            return;
        }
        String value = event.getValues().isEmpty() ? "" : event.getValues().getFirst();
        try {
            switch (id[0]) {
                case "challenge" -> edit(event, preparationEdit(workflow.selectChallenge(context, id[1],
                        Long.parseLong(value))));
                case "photo" -> edit(event, preparationEdit(workflow.selectPhoto(context, id[1],
                        Integer.parseInt(value))));
                case "map-choice" -> event.deferEdit().queue(hook -> executor.execute(() -> mappingProgress(hook,
                                workflow.assignMapping(context, id[1], Long.parseLong(id[2]), id[3],
                                        "none".equals(value) ? null : Long.valueOf(value)))),
                        failure -> log.warn("Discord ingredient-reference acknowledgement failed", failure));
                default -> reject(event, "Diese Ergebnis-Auswahl ist nicht mehr gültig.");
            }
        } catch (Rejected rejected) {
            reject(event, rejected.getMessage());
        } catch (RuntimeException exception) {
            log.error("Discord result selection failed", exception);
            reject(event, "Die Ergebnis-Auswahl konnte technisch nicht verarbeitet werden. Bitte später erneut versuchen.");
        }
    }

    boolean handlesEntitySelect(EntitySelectInteractionEvent event) {
        return event.getComponentId().startsWith(PREFIX + "person:");
    }

    void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String[] id = parts(event.getComponentId());
        OperatorContext context = context(event.getGuild(), event.getMember(), event.getUser());
        if (!context.operator()) {
            reject(event, "Diese Personen-Auswahl ist hier nicht erlaubt.");
            return;
        }
        User selected = event.getMentions().getUsers().stream().findFirst().orElse(null);
        if (selected == null) {
            reject(event, "Bitte wähle genau eine Discord-Person.");
            return;
        }
        try {
            Member member = event.getGuild() == null ? null : event.getGuild().getMember(selected);
            String name = member == null ? selected.getName() : member.getEffectiveName();
            edit(event, preparationEdit(workflow.selectPerson(context, id[1], selected.getId(), name)));
        } catch (Rejected rejected) {
            reject(event, rejected.getMessage());
        } catch (RuntimeException exception) {
            log.error("Discord result person selection failed", exception);
            reject(event, "Die Personen-Auswahl konnte technisch nicht verarbeitet werden. Bitte später erneut versuchen.");
        }
    }

    boolean handlesModal(ModalInteractionEvent event) {
        return event.getModalId().startsWith(PREFIX);
    }

    void onModalInteraction(ModalInteractionEvent event) {
        String[] id = parts(event.getModalId());
        OperatorContext context = context(event.getGuild(), event.getMember(), event.getUser());
        if (!context.operator()) {
            reject(event, "Diese Ergebnis-Eingabe ist hier nicht erlaubt.");
            return;
        }
        switch (id[0]) {
            case "capture" -> event.deferEdit().queue(hook -> executor.execute(() -> {
                try {
                    captureSubmission(hook, workflow.submitCapture(context, id[1], form(event), false));
                } catch (Rejected rejected) {
                    hook.editOriginal(retryEdit(rejected.getMessage(), "open", id[1])).queue();
                } catch (RuntimeException exception) {
                    technical(hook, exception);
                }
            }), failure -> log.warn("Discord result modal acknowledgement failed", failure));
            case "edit" -> event.deferReply(true).queue(hook -> executor.execute(() -> {
                try {
                    publishSaved(hook, workflow.submitEdit(context, id[1], form(event)));
                } catch (Rejected rejected) {
                    hook.editOriginal(retryEdit(rejected.getMessage(), "edit-open", id[1])).queue();
                } catch (RuntimeException exception) {
                    technical(hook, exception);
                }
            }), failure -> log.warn("Discord result edit modal acknowledgement failed", failure));
            case "concrete-capture" -> event.deferEdit().queue(hook -> executor.execute(() -> {
                try {
                    hook.editOriginal(preparationEdit(workflow.submitCaptureConcretizations(context, id[1],
                            concretizationValues(event)))).queue();
                } catch (Rejected rejected) {
                    hook.editOriginal(textEdit(rejected.getMessage())).queue();
                } catch (RuntimeException exception) {
                    technical(hook, exception);
                }
            }), failure -> log.warn("Discord result concretization modal acknowledgement failed", failure));
            case "concrete-edit" -> event.deferEdit().queue(hook -> executor.execute(() -> {
                try {
                    publishSaved(hook, workflow.submitEditConcretizations(context, id[1],
                            concretizationValues(event)));
                } catch (Rejected rejected) {
                    hook.editOriginal(textEdit(rejected.getMessage())).queue();
                } catch (RuntimeException exception) {
                    technical(hook, exception);
                }
            }), failure -> log.warn("Discord result concretization edit acknowledgement failed", failure));
            case "map-query" -> event.deferEdit().queue(hook -> executor.execute(() -> {
                try {
                    hook.editOriginal(mappingEdit(workflow.searchMapping(context, id[1], Long.parseLong(id[2]), id[3],
                            value(event, CATALOG_SEARCH)))).queue();
                } catch (Rejected rejected) {
                    hook.editOriginal(textEdit(rejected.getMessage())).queue();
                } catch (RuntimeException exception) {
                    technical(hook, exception);
                }
            }), failure -> log.warn("Discord catalog search modal acknowledgement failed", failure));
            default -> reject(event, "Diese Ergebnis-Eingabe ist nicht mehr gültig.");
        }
    }

    private void captureSubmission(InteractionHook hook, CaptureSubmission submission) {
        if (submission instanceof ReplaceConfirmation confirmation) {
            hook.editOriginal(replaceConfirmation(confirmation)).queue();
        } else {
            publishSaved(hook, (Saved) submission);
        }
    }

    private void directMutation(InteractionHook hook, Mutation operation) {
        try {
            publishSaved(hook, operation.run());
        } catch (Rejected rejected) {
            hook.editOriginal(textEdit(rejected.getMessage())).queue();
        } catch (RuntimeException exception) {
            technical(hook, exception);
        }
    }

    private void mappingProgress(InteractionHook hook, MappingProgress progress) {
        if (progress instanceof MappingStep step) {
            hook.editOriginal(mappingEdit(step)).queue();
        } else {
            MappingComplete complete = (MappingComplete) progress;
            hook.editOriginal(textEdit(complete.message())).queue();
        }
    }

    private void publishSaved(InteractionHook hook, Saved saved) {
        MessageEditData initial = savedEdit(saved, false, false);
        hook.editOriginal(initial).queue(ignored -> {
            DiscordChallengeArchiveRenderer.RenderedChallenge rendered;
            try {
                rendered = archiveWorkflow.renderedDetail(saved.challengeNumber());
            } catch (RuntimeException exception) {
                log.warn("Persisted result could not be rendered publicly", exception);
                hook.editOriginal(savedEdit(saved, false, true)).queue();
                return;
            }
            hook.sendMessage(DiscordJdaListener.archiveCreateMessage(rendered.challenge()))
                    .setAllowedMentions(List.of()).setEphemeral(false)
                    .queue(message -> DiscordJdaListener.publishArchiveFollowUps(hook, rendered.resultFollowUps(), 0,
                                    executor, () -> hook.editOriginal(savedEdit(saved, true, false)).queue(),
                                    failure -> hook.editOriginal(savedEdit(saved, false, true)).queue()),
                            failure -> hook.editOriginal(savedEdit(saved, false, true)).queue());
        }, failure -> log.warn("Persisted result status could not be delivered", failure));
    }

    private void technical(InteractionHook hook, RuntimeException exception) {
        log.error("Discord result interaction failed", exception);
        hook.editOriginal(textEdit("Die Ergebnisverwaltung konnte technisch nicht verarbeitet werden. "
                + "Bitte später erneut versuchen.")).queue();
    }

    private OperatorContext context(Guild guild, Member member, User user) {
        boolean operator = guild != null && guild.getIdLong() == properties.guildId() && member != null
                && member.getRoles().stream().anyMatch(role -> role.getIdLong() == properties.challengeOperatorRoleId());
        return new OperatorContext(guild == null ? 0 : guild.getIdLong(), user.getId(), operator);
    }

    static MessageCreateData preparationCreate(Preparation preparation) {
        MessageCreateBuilder builder = new MessageCreateBuilder().setAllowedMentions(List.of())
                .setContent(preparationContent(preparation)).setComponents(preparationRows(preparation));
        if (preparation.attachFullText()) {
            builder.setFiles(FileUpload.fromData(preparation.fullTextBytes(), "nachrichtentext.txt"));
        }
        return builder.build();
    }

    static MessageEditData preparationEdit(Preparation preparation) {
        return new MessageEditBuilder().setAllowedMentions(List.of()).setContent(preparationContent(preparation))
                .setEmbeds(List.of()).setComponents(preparationRows(preparation)).build();
    }

    private static String preparationContent(Preparation preparation) {
        String text = preparation.messageText().isBlank() ? "(kein Nachrichtentext)" : preparation.messageText();
        int previewLimit = preparation.concretizations().isEmpty() ? 1_200 : 600;
        String displayed = truncate(text, previewLimit);
        boolean previewTruncated = displayed.length() < text.length();
        StringBuilder content = new StringBuilder("**Challenge-Ergebnis vorbereiten**\n")
                .append("Ergebnis-Person: ").append(preparation.selectedPersonName() == null
                        ? "noch nicht gewählt" : safe(preparation.selectedPersonName())).append('\n')
                .append("Challenge: ").append(preparation.selectedChallengeNumber() == null
                        ? "ausdrücklich wählen" : "#" + preparation.selectedChallengeNumber()).append('\n')
                .append("Foto: ").append(safe(cut(selectedPhotoLabel(preparation), 100))).append("\n\n")
                .append(preparation.attachFullText()
                        ? "Der vollständige Nachrichtentext ist kopierbar als `nachrichtentext.txt` angehängt; die Vorschau ist gekennzeichnet gekürzt.\n"
                        : previewTruncated
                                ? "Gekürzte Vorschau; der vollständige Text bleibt in der Ursprungsnachricht kopierbar:\n"
                                : "Vollständiger kopierbarer Nachrichtentext:\n")
                .append("```\n").append(code(displayed)).append("\n```");
        if (preparation.descriptionNeedsCondensing()) {
            content.append("\nDer Text überschreitet 4.000 Zeichen. Bitte verdichte ihn in der Maske; die Anlage bleibt vollständig.");
        }
        if (!preparation.concretizations().isEmpty()) {
            content.append("\n\n**Persönliche Konkretisierungen (optional)**");
            preparation.concretizations().forEach(field -> content.append("\n• ")
                    .append(safe(cut(field.requirementDisplayText(), 100))).append(" → ")
                    .append(field.value().isBlank() ? "noch nicht angegeben" : safe(cut(field.value(), 200))));
        }
        return truncate(content.toString(), 1990);
    }

    private static List<ActionRow> preparationRows(Preparation preparation) {
        List<ActionRow> rows = new ArrayList<>();
        rows.add(ActionRow.of(EntitySelectMenu.create(id("person", preparation.token()), SelectTarget.USER)
                .setPlaceholder("Ergebnis-Person ausdrücklich wählen").setRequiredRange(1, 1).build()));
        rows.add(ActionRow.of(StringSelectMenu.create(id("challenge", preparation.token()))
                .setPlaceholder("Challenge auswählen (Seite " + preparation.challengePage() + "/"
                        + Math.max(1, preparation.challengePages()) + ")").setRequiredRange(1, 1)
                .addOptions(preparation.challenges().stream().map(choice -> SelectOption.of(
                                cut("#" + choice.challengeNumber() + " · " + choice.statusLabel(), 100),
                                Long.toString(choice.challengeNumber()))
                        .withDescription(choice.status().name())
                        .withDefault(Long.valueOf(choice.challengeNumber()).equals(preparation.selectedChallengeNumber())))
                        .toList()).build()));
        rows.add(ActionRow.of(StringSelectMenu.create(id("photo", preparation.token()))
                .setPlaceholder("Foto oder ausdrücklich kein Foto wählen").setRequiredRange(1, 1)
                .addOptions(preparation.photos().stream().map(photo -> SelectOption.of(cut(photo.label(), 100),
                                Integer.toString(photo.index())).withDefault(photo.selected())).toList()).build()));
        List<Button> buttons = new ArrayList<>();
        if (preparation.challengePage() > 1) {
            buttons.add(Button.secondary(id("page", preparation.token(), preparation.challengePage() - 1), "Zurück"));
        }
        if (preparation.challengePage() < preparation.challengePages()) {
            buttons.add(Button.secondary(id("page", preparation.token(), preparation.challengePage() + 1), "Weiter"));
        }
        buttons.add(Button.primary(id("open", preparation.token()), "Textmaske öffnen")
                .withDisabled(!preparation.readyForModal()));
        if (!preparation.concretizations().isEmpty()) {
            buttons.add(Button.secondary(id("concrete-capture", preparation.token()), "Konkretisierungen eingeben"));
        }
        rows.add(ActionRow.of(buttons));
        return List.copyOf(rows);
    }

    static Modal modal(ResultModal modal, String kind) {
        FormData values = modal.values();
        return Modal.create(id(kind, modal.token()), modal.title())
                .addComponents(
                        Label.of("Gerichtsname", input(DISH, TextInputStyle.SHORT, true, 1, 200, values.dishName())),
                        Label.of("Beschreibung / Rezept", modal.sourceTextWasLonger()
                                        ? "Der vollständige Ursprungstext steht weiterhin in der Vorbereitungsansicht."
                                        : "Erforderlich, maximal 4.000 Zeichen.",
                                input(DESCRIPTION, TextInputStyle.PARAGRAPH, true, 1, 4000, values.description())),
                        Label.of("Bewertung", "Optional.",
                                input(EVALUATION, TextInputStyle.PARAGRAPH, false, 0, 4000, values.evaluation())),
                        Label.of("Eigene Zutaten (1/2)", "Optional, eine pro Zeile.",
                                input(INGREDIENTS_ONE, TextInputStyle.PARAGRAPH, false, 0, 4000,
                                        values.ingredientsPartOne())),
                        Label.of("Eigene Zutaten (2/2)", "Optional; Fortsetzung nur bei Bedarf.",
                                input(INGREDIENTS_TWO, TextInputStyle.PARAGRAPH, false, 0, 4000,
                                        values.ingredientsPartTwo())))
                .build();
    }

    static Modal concretizationModal(ConcretizationModal modal, String kind) {
        Modal.Builder builder = Modal.create(id(kind, modal.token()), modal.title());
        for (DiscordResultCaptureWorkflow.ConcretizationField field : modal.fields()) {
            builder.addComponents(Label.of(cut(field.requirementDisplayText() + " →", 45),
                    "Optional; erfüllt die offene Vorgabe und ist keine eigene Zusatz-Zutat.",
                    input(concretizationId(field.requirementPosition()), TextInputStyle.SHORT, false, 0, 200,
                            field.value())));
        }
        return builder.build();
    }

    private static MessageCreateData editPreparationCreate(EditPreparation preparation) {
        StringBuilder content = new StringBuilder("**Challenge-Ergebnis bearbeiten**\nChallenge #")
                .append(preparation.challengeNumber()).append(" · ")
                .append(safe(preparation.participantName())).append(" · **")
                .append(safe(preparation.dishName())).append("**");
        if (!preparation.concretizations().isEmpty()) {
            content.append("\n\n**Persönliche Konkretisierungen**");
            preparation.concretizations().forEach(field -> content.append("\n• ")
                    .append(safe(cut(field.requirementDisplayText(), 100))).append(" → ")
                    .append(field.value().isBlank() ? "nicht angegeben" : safe(cut(field.value(), 200))));
        }
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.primary(id("edit-open", preparation.token()), "Text und eigene Zutaten bearbeiten"));
        if (!preparation.concretizations().isEmpty()) {
            buttons.add(Button.secondary(id("concrete-edit", preparation.token()), "Konkretisierungen bearbeiten"));
        }
        return new MessageCreateBuilder().setAllowedMentions(List.of()).setContent(truncate(content.toString(), 1990))
                .setComponents(List.of(ActionRow.of(buttons))).build();
    }

    private static TextInput input(String id, TextInputStyle style, boolean required, int minimum, int maximum,
                                   String value) {
        TextInput.Builder builder = TextInput.create(id, style).setRequired(required).setMaxLength(maximum);
        if (required) {
            builder.setMinLength(minimum);
        }
        if (value != null && !value.isBlank()) {
            builder.setValue(value);
        }
        return builder.build();
    }

    private static Modal searchModal(String token, long expectedResultVersion, String targetKey) {
        return Modal.create(id("map-query", token, expectedResultVersion, targetKey), "Katalog durchsuchen")
                .addComponents(Label.of("Literaler Suchtext", TextInput.create(CATALOG_SEARCH, TextInputStyle.SHORT)
                        .setRequiredRange(1, 200).build())).build();
    }

    private static MessageCreateData removalConfirmation(DiscordResultCaptureWorkflow.RemovalConfirmation confirmation) {
        return new MessageCreateBuilder().setAllowedMentions(List.of()).setContent("Ergebnis **"
                        + safe(confirmation.dishName()) + "** von **" + safe(confirmation.participantName())
                        + "** für Challenge #" + confirmation.challengeNumber()
                        + " wirklich vollständig einschließlich Foto und Zutaten entfernen?")
                .setComponents(List.of(ActionRow.of(Button.danger(id("remove", confirmation.token()),
                        "Ergebnis endgültig entfernen")))).build();
    }

    private static MessageEditData replaceConfirmation(ReplaceConfirmation confirmation) {
        return new MessageEditBuilder().setAllowedMentions(List.of()).setContent("Für **"
                        + safe(confirmation.participantName()) + "** existiert bei Challenge #"
                        + confirmation.challengeNumber() + " bereits **" + safe(confirmation.existingDishName())
                        + "**. Nur die ausdrückliche Bestätigung ersetzt die Ergebnisfelder; ein vorhandenes Foto bleibt "
                        + "ohne neu gewähltes Foto unverändert.")
                .setComponents(List.of(ActionRow.of(Button.danger(id("replace", confirmation.token()),
                        "Vorhandenes Ergebnis ersetzen")))).build();
    }

    static MessageEditData mappingEdit(MappingStep step) {
        List<SelectOption> options = new ArrayList<>();
        options.add(SelectOption.of("Ohne Katalogreferenz", "none")
                .withDescription("Freitext bleibt maßgeblich"));
        step.choices().forEach(choice -> options.add(SelectOption.of(cut(choice.displayName()
                        + (choice.active() ? "" : " [inaktiv]"), 100), Long.toString(choice.conceptId()))
                .withDescription(cut(choice.code() + (choice.exact() ? " · exakter Vorschlag" : ""), 100))));
        String target = step.concretization() ? "Konkretisierung" : "Eigene Zutat";
        String content = "**Katalogreferenzen zuordnen** · " + target + " " + step.ingredientNumber() + "/"
                + step.ingredientCount()
                + (step.requirementDisplayText() == null ? "" : "\nOffene Vorgabe: `"
                + inline(step.requirementDisplayText()) + "`")
                + "\nMaßgeblicher Freitext: `" + inline(step.ingredientText()) + "`\nSuche: `"
                + inline(step.searchTerm()) + "`" + (step.exactSuggestionId() == null ? ""
                : "\nEin eindeutiger exakter Treffer ist als Vorschlag markiert.");
        return new MessageEditBuilder().setAllowedMentions(List.of()).setContent(content)
                .setComponents(List.of(
                        ActionRow.of(StringSelectMenu.create(id("map-choice", step.token(),
                                        step.expectedResultVersion(), step.targetKey()))
                                .setPlaceholder("Referenz oder ohne Referenz wählen").setRequiredRange(1, 1)
                                .addOptions(options).build()),
                        ActionRow.of(Button.secondary(id("map-search", step.token(),
                                        step.expectedResultVersion(), step.targetKey()), "Andere Treffer suchen"),
                                Button.secondary(id("map-abort", step.token()), "Zuordnung beenden"))))
                .build();
    }

    static MessageEditData savedEdit(Saved saved, boolean published, boolean publishFailed) {
        String status = saved.message();
        if (published) {
            status += " Die aktualisierte Detailansicht wurde öffentlich gepostet.";
        } else if (publishFailed) {
            status += " Die öffentliche Darstellung konnte jedoch nicht vollständig gesendet werden; der gespeicherte Stand "
                    + "bleibt mit `/challenges anzeigen` abrufbar.";
        } else {
            status += " Die öffentliche Detailansicht wird gesendet.";
        }
        MessageEditBuilder builder = new MessageEditBuilder().setAllowedMentions(List.of()).setContent(status);
        if (saved.mappingAvailable() && saved.mappingToken() != null) {
            builder.setComponents(List.of(ActionRow.of(Button.primary(id("map", saved.mappingToken()),
                    "Katalogreferenzen zuordnen"))));
        } else {
            builder.setComponents(List.of());
        }
        return builder.build();
    }

    private static MessageEditData retryEdit(String message, String action, String token) {
        return new MessageEditBuilder().setAllowedMentions(List.of()).setContent(message)
                .setComponents(List.of(ActionRow.of(Button.primary(id(action, token), "Textmaske erneut öffnen"))))
                .build();
    }

    private static MessageEditData textEdit(String message) {
        return new MessageEditBuilder().setAllowedMentions(List.of()).setContent(message).setEmbeds(List.of())
                .setComponents(List.of()).build();
    }

    private static FormData form(ModalInteractionEvent event) {
        return new FormData(value(event, DISH), value(event, DESCRIPTION), value(event, EVALUATION),
                value(event, INGREDIENTS_ONE), value(event, INGREDIENTS_TWO));
    }

    private static Map<Integer, String> concretizationValues(ModalInteractionEvent event) {
        Map<Integer, String> values = new LinkedHashMap<>();
        for (int position = 1; position <= 4; position++) {
            ModalMapping mapping = event.getValue(concretizationId(position));
            if (mapping != null) {
                values.put(position, mapping.getAsString());
            }
        }
        return Map.copyOf(values);
    }

    private static String concretizationId(int requirementPosition) {
        return "concretization-" + requirementPosition;
    }

    private static String value(ModalInteractionEvent event, String id) {
        ModalMapping mapping = event.getValue(id);
        return mapping == null ? "" : mapping.getAsString();
    }

    private static Long positiveLong(OptionMapping option) {
        return option == null || option.getAsLong() < 1 ? null : option.getAsLong();
    }

    private static String selectedPhotoLabel(Preparation preparation) {
        return preparation.photos().stream().filter(DiscordResultCaptureWorkflow.PhotoChoice::selected)
                .map(DiscordResultCaptureWorkflow.PhotoChoice::label).findFirst().orElse("noch nicht gewählt");
    }

    private static String code(String value) {
        return value.replace("```", "``\u200b`");
    }

    private static String inline(String value) {
        return safe(value).replace("`", "' ");
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("@", "@\u200b");
    }

    private static String cut(String value, int length) {
        return value.length() <= length ? value : value.substring(0, Math.max(0, length - 1)) + "…";
    }

    private static String truncate(String value, int length) {
        return value.length() <= length ? value : value.substring(0, Math.max(0, length - 1)) + "…";
    }

    private static String id(String action, String token, Object... suffix) {
        StringBuilder id = new StringBuilder(PREFIX).append(action).append(':').append(token);
        for (Object value : suffix) {
            id.append(':').append(value);
        }
        return id.toString();
    }

    private static String[] parts(String componentId) {
        if (componentId == null || !componentId.startsWith(PREFIX)) {
            throw new Rejected("Diese Ergebnis-Interaktion ist nicht gültig.");
        }
        String[] parts = componentId.substring(PREFIX.length()).split(":", -1);
        if (parts.length < 2) {
            throw new Rejected("Diese Ergebnis-Interaktion ist nicht gültig.");
        }
        return parts;
    }

    private static void edit(ButtonInteractionEvent event, MessageEditData data) {
        event.editMessage(data).queue();
    }

    private static void edit(StringSelectInteractionEvent event, MessageEditData data) {
        event.editMessage(data).queue();
    }

    private static void edit(EntitySelectInteractionEvent event, MessageEditData data) {
        event.editMessage(data).queue();
    }

    private static void reject(MessageContextInteractionEvent event, String message) {
        event.reply(message).setAllowedMentions(List.of()).setEphemeral(true).queue();
    }

    private static void reject(SlashCommandInteractionEvent event, String message) {
        event.reply(message).setAllowedMentions(List.of()).setEphemeral(true).queue();
    }

    private static void reject(ButtonInteractionEvent event, String message) {
        event.reply(message).setAllowedMentions(List.of()).setEphemeral(true).queue();
    }

    private static void reject(StringSelectInteractionEvent event, String message) {
        event.reply(message).setAllowedMentions(List.of()).setEphemeral(true).queue();
    }

    private static void reject(EntitySelectInteractionEvent event, String message) {
        event.reply(message).setAllowedMentions(List.of()).setEphemeral(true).queue();
    }

    private static void reject(ModalInteractionEvent event, String message) {
        event.reply(message).setAllowedMentions(List.of()).setEphemeral(true).queue();
    }

    @FunctionalInterface
    private interface Mutation {
        Saved run();
    }

    private static final class JdaPhotoSource implements PhotoSource {
        private final Attachment attachment;

        private JdaPhotoSource(Attachment attachment) {
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
}
