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
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.CulinaryCountry;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.CulinaryCountryIngredient;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries.CulinaryCountryIngredientPage;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class DiscordJdaListenerTest {

    @Test
    void readyRegistrationIncludesTheMessageContextCommand() {
        DiscordProperties properties = new DiscordProperties(true, "token", 99, 77777,
                ZoneId.of("Europe/Berlin"), Map.of());
        Guild guild = mock(Guild.class);
        JDA jda = mock(JDA.class);
        ReadyEvent event = mock(ReadyEvent.class);
        CommandCreateAction action = mock(CommandCreateAction.class);
        when(event.getJDA()).thenReturn(jda);
        when(jda.getGuildById(99)).thenReturn(guild);
        when(guild.upsertCommand(any(CommandData.class))).thenReturn(action);

        new DiscordJdaListener(properties, mock(DiscordChallengeWorkflow.class),
                mock(DiscordIngredientLookupWorkflow.class), mock(DiscordChallengeArchiveWorkflow.class),
                mock(DiscordParticipantAdministrationWorkflow.class), mock(DiscordResultCaptureJdaListener.class),
                Runnable::run).onReady(event);

        ArgumentCaptor<CommandData> commands = ArgumentCaptor.forClass(CommandData.class);
        org.mockito.Mockito.verify(guild, org.mockito.Mockito.times(6)).upsertCommand(commands.capture());
        assertThat(commands.getAllValues()).extracting(CommandData::getName)
                .containsExactly("challenge", "zutat", "zutaten", "challenges", "teilnehmer",
                        DiscordResultCaptureJdaListener.CONTEXT_COMMAND_NAME);
    }

    @Test
    void registersCompleteParticipantAdministrationWithRequiredUserOptions() {
        var command = DiscordJdaListener.participantCommand();

        assertThat(command.getName()).isEqualTo("teilnehmer");
        assertThat(command.getSubcommands()).extracting(SubcommandData::getName)
                .containsExactly("anlegen", "aktivieren", "deaktivieren", "elektorat-hinzufuegen",
                        "elektorat-entfernen", "liste");
        assertThat(command.getSubcommands()).filteredOn(subcommand -> !"liste".equals(subcommand.getName()))
                .allSatisfy(subcommand -> assertThat(subcommand.getOptions()).first().satisfies(option -> {
                    assertThat(option.getName()).isEqualTo("person");
                    assertThat(option.getType()).isEqualTo(OptionType.USER);
                    assertThat(option.isRequired()).isTrue();
                }));
        assertThat(command.getSubcommands()).filteredOn(subcommand -> "anlegen".equals(subcommand.getName()))
                .singleElement().satisfies(subcommand -> assertThat(subcommand.getOptions()).extracting(OptionData::getName)
                        .containsExactly("person", "name"));
    }

    @Test
    void rejectsParticipantAdministrationBeforeItTouchesTheCoreWhenCallerIsNotAnOperator() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var participantWorkflow = mock(DiscordParticipantAdministrationWorkflow.class);
        var event = slashEvent("99999", 98, false);
        var reply = mock(ReplyCallbackAction.class);
        when(event.getName()).thenReturn("teilnehmer");
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);
        when(reply.setAllowedMentions(any())).thenReturn(reply);

        new DiscordJdaListener(new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"), Map.of()),
                challengeWorkflow, null, null, participantWorkflow, Runnable::run).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verifyNoInteractions(participantWorkflow, challengeWorkflow);
    }

    @Test
    void permitsAnOperatorWithoutParticipantIdentityToAdministerParticipants() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var participantWorkflow = mock(DiscordParticipantAdministrationWorkflow.class);
        var event = slashEvent("operator-without-participant", 99, true);
        when(event.getName()).thenReturn("teilnehmer");
        when(event.getSubcommandName()).thenReturn("liste");
        ephemeralAcknowledgement(event);

        new DiscordJdaListener(new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"), Map.of()),
                challengeWorkflow, null, null, participantWorkflow, Runnable::run).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(participantWorkflow).list(any(DiscordMemberNameResolver.class), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

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
        var event = slashEvent("99999", 98, false);
        var reply = mock(ReplyCallbackAction.class);
        when(event.getName()).thenReturn("zutat");
        when(lookupWorkflow.acceptsGuild(98)).thenReturn(false);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        ingredientListener(challengeWorkflow, lookupWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verify(lookupWorkflow, never()).search(any(), any(), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void defersIngredientSlashForAnUnregisteredGuildMemberBeforeDelegatingTheSearch() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        var event = slashEvent("99999", 99, false);
        var option = mock(OptionMapping.class);
        var acknowledgement = acknowledgement(event);
        when(event.getName()).thenReturn("zutat");
        when(event.getOption("suche")).thenReturn(option);
        when(option.getAsString()).thenReturn("  Tempeh  ");
        when(lookupWorkflow.acceptsGuild(99)).thenReturn(true);

        ingredientListener(challengeWorkflow, lookupWorkflow).onSlashCommandInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferReply();
        order.verify(lookupWorkflow).search(eq("  Tempeh  "), eq("99999"), any(), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void defersIngredientSelectionBeforeDelegatingItsStatelessValues() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        var event = stringSelectEvent(DiscordIngredientComponentId.selection("10001"),
                List.of(DiscordIngredientComponentId.conceptValue(42)), lookupWorkflow, "10001", 99);

        ingredientListener(challengeWorkflow, lookupWorkflow).onStringSelectInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferEdit();
        order.verify(lookupWorkflow).component(eq(DiscordIngredientComponentId.selection("10001")),
                eq(List.of(DiscordIngredientComponentId.conceptValue(42))), eq("10001"), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void routesIngredientNavigationSelectWithTheClickingUserForOwnerValidation() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var lookupWorkflow = mock(DiscordIngredientLookupWorkflow.class);
        String componentId = DiscordIngredientComponentId.navigationSelect("child", "10001");
        List<String> values = List.of(DiscordIngredientComponentId.conceptValue(42));
        var event = stringSelectEvent(componentId, values, lookupWorkflow, "10001", 99);

        ingredientListener(challengeWorkflow, lookupWorkflow).onStringSelectInteraction(event);

        InOrder order = inOrder(event, lookupWorkflow);
        order.verify(event).deferEdit();
        order.verify(lookupWorkflow).navigateSelect(eq(componentId), eq(values), eq("10001"), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void ingredientProfileMessageMapsDescriptionInlineFieldsColorAndSingleOptionNavigationSelect() {
        var response = new DiscordIngredientLookupRenderer.RenderedEmbed(
                "🥢 Tempeh",
                "```\nGewichtung  1,0\n```",
                DiscordIngredientLookupRenderer.CARD_COLOR,
                List.of(
                        new DiscordIngredientLookupRenderer.EmbedField("Funktion im Gericht", "pflanzliches Protein", true),
                        new DiscordIngredientLookupRenderer.EmbedField("Besondere Eigenschaften", "fermentiert", true)),
                List.of(new DiscordIngredientLookupRenderer.NavigationSelectRow(
                        DiscordIngredientComponentId.navigationSelect("parent"),
                        "⬆️ Allgemeineren Begriff öffnen …",
                        List.of(new DiscordIngredientLookupRenderer.SelectionOption(
                                "Sojaprodukt", DiscordIngredientComponentId.conceptValue(42), null)))));

        var message = DiscordJdaListener.ingredientMessage(response, "10001");

        assertThat(message.getEmbeds()).singleElement().satisfies(embed -> {
            assertThat(embed.getTitle()).isEqualTo("🥢 Tempeh");
            assertThat(embed.getDescription()).contains("Gewichtung");
            assertThat(embed.getColorRaw()).isEqualTo(DiscordIngredientLookupRenderer.CARD_COLOR);
            assertThat(embed.getFields()).hasSize(2).allSatisfy(field -> assertThat(field.isInline()).isTrue());
        });
        assertThat(message.getComponents()).singleElement().satisfies(union -> {
            var row = union.asActionRow();
            assertThat(row.getComponents()).singleElement().isInstanceOf(StringSelectMenu.class);
            var select = (StringSelectMenu) row.getComponents().getFirst();
            assertThat(select.getPlaceholder()).isEqualTo("⬆️ Allgemeineren Begriff öffnen …");
            assertThat(select.getCustomId()).isEqualTo(DiscordIngredientComponentId.navigationSelect("parent", "10001"));
            assertThat(select.getOptions()).singleElement().satisfies(option -> {
                assertThat(option.getLabel()).isEqualTo("Sojaprodukt");
                assertThat(option.getValue()).isEqualTo(DiscordIngredientComponentId.conceptValue(42));
            });
        });
    }

    @Test
    void ingredientProfileWithoutRelationsRemovesTheSelectionMenu() {
        var response = new DiscordIngredientLookupRenderer.RenderedEmbed(
                "🥢 Tempeh", "```\nGewichtung  1,0\n```", DiscordIngredientLookupRenderer.CARD_COLOR,
                List.of(new DiscordIngredientLookupRenderer.EmbedField("🍽️ Geschmacksprofil", "keine", false)), List.of());

        var message = DiscordJdaListener.ingredientMessage(response, "10001");

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
    void defersSlashInteractionBeforeDelegatingDefaultOffersAndAutoRestrictionsForOperator() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent("99999", 99, true);
        var acknowledgement = acknowledgement(event);
        var listener = listener(workflow);

        listener.onSlashCommandInteraction(event);

        InOrder order = inOrder(event, workflow);
        order.verify(event).deferReply();
        order.verify(workflow).start(eq(1), eq(RestrictionMode.AUTO), any(DiscordMemberNameResolver.class), any(), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
        org.mockito.Mockito.verify(workflow, never()).accepts(99, "99999");
    }

    @Test
    void delegatesExplicitTwoAndThreeOfferRequestsAfterAcknowledgement() {
        for (int offerCount : java.util.List.of(2, 3)) {
            var workflow = mock(DiscordChallengeWorkflow.class);
            var event = slashEvent("99999", 99, true);
            var option = mock(OptionMapping.class);
            acknowledgement(event);
            when(event.getOption("angebote")).thenReturn(option);
            when(option.getAsInt()).thenReturn(offerCount);

            listener(workflow).onSlashCommandInteraction(event);

            org.mockito.Mockito.verify(workflow).start(eq(offerCount), eq(RestrictionMode.AUTO),
                    any(DiscordMemberNameResolver.class), any(), any());
        }
    }

    @Test
    void delegatesAllRestrictionModesTogetherWithTheOfferCount() {
        for (RestrictionMode mode : RestrictionMode.values()) {
            var workflow = mock(DiscordChallengeWorkflow.class);
            var event = slashEvent("99999", 99, true);
            var offers = mock(OptionMapping.class);
            var restriction = mock(OptionMapping.class);
            acknowledgement(event);
            when(event.getOption("angebote")).thenReturn(offers);
            when(offers.getAsInt()).thenReturn(3);
            when(event.getOption("einschraenkung")).thenReturn(restriction);
            when(restriction.getAsString()).thenReturn(mode.name());

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
    void rejectsChallengeSlashFromWrongGuildEvenWithOperatorRoleWithoutStartingWorkflow() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent("99999", 98, true);
        var reply = mock(ReplyCallbackAction.class);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        listener(workflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verify(workflow, never()).start(org.mockito.ArgumentMatchers.anyInt(), any(), any(), any(), any());
    }

    @Test
    void rejectsConfiguredParticipantWithoutOperatorRoleBeforeStartingWorkflow() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent("10001", 99, false);
        var reply = mock(ReplyCallbackAction.class);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);

        listener(workflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).reply(any(String.class));
        org.mockito.Mockito.verify(workflow, never()).start(org.mockito.ArgumentMatchers.anyInt(), any(), any(), any(), any());
    }

    @Test
    void operatorDoesNotNeedParticipantMappingToStartChallenge() {
        var workflow = mock(DiscordChallengeWorkflow.class);
        var event = slashEvent("99999", 99, true);
        acknowledgement(event);

        listener(workflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(workflow).start(eq(1), eq(RestrictionMode.AUTO),
                any(DiscordMemberNameResolver.class), any(), any());
        org.mockito.Mockito.verify(workflow, never()).accepts(99, "99999");
    }

    @Test
    void countryIngredientMessageUsesBoundSelectAndCorrectPaginationButtons() {
        var renderer = new DiscordIngredientLookupRenderer();
        var response = renderer.countryIngredients(new CulinaryCountryIngredientPage(
                new CulinaryCountry("XA", "Testland Alpha"), 1, 20, 21,
                java.util.stream.LongStream.rangeClosed(1, 20)
                        .mapToObj(id -> new CulinaryCountryIngredient(id, "Zutat " + id)).toList()));

        var message = DiscordJdaListener.ingredientMessage(response, "10001");

        assertThat(message.getAllowedMentions()).isEmpty();
        assertThat(message.getEmbeds()).singleElement().satisfies(embed ->
                assertThat(embed.getTitle()).contains("Testland Alpha"));
        assertThat(message.getComponents()).hasSize(2);
        var select = (StringSelectMenu) message.getComponents().getFirst().asActionRow().getComponents().getFirst();
        assertThat(select.getPlaceholder()).isEqualTo("🥢 Zutat anzeigen …");
        assertThat(select.getCustomId()).isEqualTo(DiscordIngredientComponentId.countrySelect(
                new DiscordIngredientComponentId.CountryBrowseContext("XA", 1), "10001"));
        var buttons = message.getComponents().get(1).asActionRow().getButtons();
        assertThat(buttons).extracting(button -> button.getLabel(), button -> button.isDisabled())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("◀ Zurück", true),
                        org.assertj.core.groups.Tuple.tuple("Weiter ▶", false));
    }

    @Test
    void registersCountryIngredientBrowseWithRequiredAutocompleteCountry() {
        var command = DiscordJdaListener.ingredientsCommand();

        assertThat(command.getName()).isEqualTo("zutaten");
        assertThat(command.getOptions()).singleElement().satisfies(option -> {
            assertThat(option.getName()).isEqualTo("land");
            assertThat(option.getType()).isEqualTo(OptionType.STRING);
            assertThat(option.isRequired()).isTrue();
            assertThat(option.isAutoComplete()).isTrue();
        });
    }

    @Test
    void registersAllArchiveSubcommandsWithTheirRequiredNativeOptions() {
        var command = DiscordJdaListener.challengesCommand();

        assertThat(command.getName()).isEqualTo("challenges");
        assertThat(command.getSubcommands()).extracting(subcommand -> subcommand.getName())
                .containsExactly("letzte", "aktiv", "liste", "anzeigen", "abschließen", "karte-setzen", "karte-entfernen",
                        "ergebnis-bearbeiten", "ergebnis-entfernen", "ergebnis-foto-setzen", "ergebnis-foto-entfernen")
                .doesNotContain("aktuell");
        assertThat(command.getSubcommands()).filteredOn(subcommand -> subcommand.getName().equals("karte-setzen"))
                .singleElement().satisfies(subcommand -> assertThat(subcommand.getOptions()).satisfiesExactly(
                        option -> {
                            assertThat(option.getName()).isEqualTo("bild");
                            assertThat(option.getType()).isEqualTo(OptionType.ATTACHMENT);
                            assertThat(option.isRequired()).isTrue();
                        },
                        option -> {
                            assertThat(option.getName()).isEqualTo("nummer");
                            assertThat(option.isRequired()).isFalse();
                        },
                        option -> {
                            assertThat(option.getName()).isEqualTo("ersetzen");
                            assertThat(option.getType()).isEqualTo(OptionType.BOOLEAN);
                        }));
        assertThat(command.getSubcommands()).filteredOn(subcommand -> subcommand.getName().equals("anzeigen"))
                .singleElement().satisfies(subcommand -> assertThat(subcommand.getOptions()).singleElement()
                        .satisfies(option -> assertThat(option.isRequired()).isTrue()));
        assertThat(command.getSubcommands()).filteredOn(subcommand -> subcommand.getName().equals("aktiv"))
                .singleElement().satisfies(subcommand -> assertThat(subcommand.getOptions()).singleElement()
                        .satisfies(option -> {
                            assertThat(option.getName()).isEqualTo("seite");
                            assertThat(option.isRequired()).isFalse();
                        }));
        assertThat(command.getSubcommands()).filteredOn(subcommand -> subcommand.getName().equals("abschließen"))
                .singleElement().satisfies(subcommand -> assertThat(subcommand.getOptions()).singleElement()
                        .satisfies(option -> {
                            assertThat(option.getName()).isEqualTo("nummer");
                            assertThat(option.getType()).isEqualTo(OptionType.INTEGER);
                            assertThat(option.isRequired()).isFalse();
                        }));
        assertThat(command.getSubcommands()).filteredOn(subcommand -> subcommand.getName().startsWith("ergebnis-"))
                .allSatisfy(subcommand -> assertThat(subcommand.getOptions()).satisfies(options -> {
                    assertThat(options).anySatisfy(option -> {
                        assertThat(option.getName()).isEqualTo("nummer");
                        assertThat(option.getType()).isEqualTo(OptionType.INTEGER);
                        assertThat(option.isRequired()).isTrue();
                    });
                    assertThat(options).anySatisfy(option -> {
                        assertThat(option.getName()).isEqualTo("person");
                        assertThat(option.getType()).isEqualTo(OptionType.USER);
                        assertThat(option.isRequired()).isTrue();
                    });
                }));
        assertThat(command.getSubcommands()).filteredOn(subcommand -> subcommand.getName().equals("ergebnis-foto-setzen"))
                .singleElement().satisfies(subcommand -> assertThat(subcommand.getOptions()).anySatisfy(option -> {
                    assertThat(option.getName()).isEqualTo("bild");
                    assertThat(option.getType()).isEqualTo(OptionType.ATTACHMENT);
                    assertThat(option.isRequired()).isTrue();
                }));
        assertThat(DiscordResultCaptureJdaListener.contextCommand().getName())
                .isEqualTo("Als Challenge-Ergebnis erfassen");
    }

    @Test
    void permitsGuildWideArchiveReadsWithoutParticipantMapping() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        var event = slashEvent("99999", 99, false);
        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("letzte");
        when(archiveWorkflow.acceptsGuild(99)).thenReturn(true);
        ReplyCallbackAction acknowledgement = acknowledgement(event);

        archiveListener(challengeWorkflow, archiveWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).deferReply();
        org.mockito.Mockito.verify(archiveWorkflow).latest(any(DiscordMemberNameResolver.class), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
    }

    @Test
    void routesGuildWideActiveReadsWithPaginationWithoutParticipantMapping() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        var event = slashEvent("99999", 99, false);
        var page = mock(OptionMapping.class);
        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("aktiv");
        when(event.getOption("seite")).thenReturn(page);
        when(page.getAsInt()).thenReturn(2);
        when(archiveWorkflow.acceptsGuild(99)).thenReturn(true);
        acknowledgement(event);

        archiveListener(challengeWorkflow, archiveWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(archiveWorkflow).active(eq(2), any(), any());
        org.mockito.Mockito.verifyNoInteractions(challengeWorkflow);
    }

    @Test
    void rejectsChallengeCompletionBeforeItTouchesTheCoreWhenCallerIsNotAnOperator() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        var event = slashEvent("99999", 99, false);
        var reply = mock(ReplyCallbackAction.class);
        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("abschließen");
        when(archiveWorkflow.acceptsGuild(99)).thenReturn(true);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);
        when(reply.setAllowedMentions(any())).thenReturn(reply);

        archiveListener(challengeWorkflow, archiveWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event, never()).getOption("nummer");
        org.mockito.Mockito.verify(archiveWorkflow, never()).complete(any(), any(DiscordMemberNameResolver.class), any());
    }

    @Test
    void defersOperatorChallengeCompletionEphemerallyWithAnOptionalNumber() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        var event = slashEvent("99999", 99, true);
        var number = mock(OptionMapping.class);
        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("abschließen");
        when(archiveWorkflow.acceptsGuild(99)).thenReturn(true);
        when(event.getOption("nummer")).thenReturn(number);
        when(number.getAsLong()).thenReturn(4L);
        ReplyCallbackAction acknowledgement = ephemeralAcknowledgement(event);

        archiveListener(challengeWorkflow, archiveWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).deferReply(true);
        org.mockito.Mockito.verify(archiveWorkflow).complete(eq(4L), any(DiscordMemberNameResolver.class), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
    }

    @Test
    void rejectsUnauthorizedCardUploadBeforeReadingItsAttachmentOrCallingTheCore() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        var event = slashEvent("10001", 99, false);
        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("karte-setzen");
        when(archiveWorkflow.acceptsGuild(99)).thenReturn(true);
        ReplyCallbackAction reply = mock(ReplyCallbackAction.class);
        when(event.reply(any(String.class))).thenReturn(reply);
        when(reply.setEphemeral(true)).thenReturn(reply);
        when(reply.setAllowedMentions(any())).thenReturn(reply);

        archiveListener(challengeWorkflow, archiveWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event, never()).getOption("bild");
        org.mockito.Mockito.verify(archiveWorkflow, never()).setCard(any(), eq(false), any(), any(DiscordMemberNameResolver.class), any());
    }

    @Test
    void defersCardMutationEphemerallyAndPassesAnUnresolvedDefaultTargetToTheWorkflow() {
        var challengeWorkflow = mock(DiscordChallengeWorkflow.class);
        var archiveWorkflow = mock(DiscordChallengeArchiveWorkflow.class);
        var event = slashEvent("99999", 99, true);
        var imageOption = mock(OptionMapping.class);
        var attachment = mock(net.dv8tion.jda.api.entities.Message.Attachment.class);
        when(event.getName()).thenReturn("challenges");
        when(event.getSubcommandName()).thenReturn("karte-setzen");
        when(archiveWorkflow.acceptsGuild(99)).thenReturn(true);
        when(event.getOption("bild")).thenReturn(imageOption);
        when(imageOption.getAsAttachment()).thenReturn(attachment);
        when(attachment.getSize()).thenReturn(3);
        when(attachment.getContentType()).thenReturn("image/png");
        ReplyCallbackAction acknowledgement = ephemeralAcknowledgement(event);

        archiveListener(challengeWorkflow, archiveWorkflow).onSlashCommandInteraction(event);

        org.mockito.Mockito.verify(event).deferReply(true);
        org.mockito.Mockito.verify(archiveWorkflow).setCard(org.mockito.ArgumentMatchers.isNull(), eq(false), any(),
                any(DiscordMemberNameResolver.class), any());
        org.mockito.Mockito.verify(acknowledgement).queue(any(), any());
    }

    @Test
    void archiveJdaRenderingUsesTheStableLocalAttachmentNameAndDisablesMentions() {
        var detail = new DiscordChallengeArchiveRenderer.RenderedDetail("Challenge #9", "Bestätigt am 21. August 2026",
                "challenge-9.png", new byte[] {1, 2, 3});

        var edit = DiscordJdaListener.archiveEditMessage(detail);

        assertThat(edit.getAllowedMentions()).isEmpty();
        assertThat(edit.getEmbeds()).singleElement().satisfies(embed ->
                assertThat(embed.getImage().getUrl()).isEqualTo("attachment://challenge-9.png"));
        assertThat(edit.getAttachments()).singleElement().satisfies(attachment ->
                assertThat(((net.dv8tion.jda.api.utils.FileUpload) attachment).getName()).isEqualTo("challenge-9.png"));
    }

    private static DiscordJdaListener listener(DiscordChallengeWorkflow workflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), workflow, Runnable::run);
    }

    private static DiscordJdaListener ingredientListener(DiscordChallengeWorkflow challengeWorkflow,
                                                         DiscordIngredientLookupWorkflow lookupWorkflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), challengeWorkflow, lookupWorkflow, Runnable::run);
    }

    private static DiscordJdaListener archiveListener(DiscordChallengeWorkflow challengeWorkflow,
                                                      DiscordChallengeArchiveWorkflow archiveWorkflow) {
        return new DiscordJdaListener(new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), challengeWorkflow, null, archiveWorkflow, Runnable::run);
    }

    private static SlashCommandInteractionEvent slashEvent() {
        return slashEvent("10001", 99, true);
    }

    private static SlashCommandInteractionEvent slashEvent(String userId, long guildId, boolean operator) {
        var event = mock(SlashCommandInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        var member = mock(Member.class);
        when(event.getName()).thenReturn("challenge");
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(guildId);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(event.getMember()).thenReturn(member);
        if (operator) {
            var role = mock(Role.class);
            when(role.getIdLong()).thenReturn(77777L);
            when(member.getRoles()).thenReturn(List.of(role));
        } else {
            when(member.getRoles()).thenReturn(List.of());
        }
        return event;
    }

    private static StringSelectInteractionEvent stringSelectEvent(String componentId, List<String> values,
                                                                  DiscordIngredientLookupWorkflow lookupWorkflow,
                                                                  String userId, long guildId) {
        var event = mock(StringSelectInteractionEvent.class);
        var guild = mock(Guild.class);
        var user = mock(User.class);
        when(event.getComponentId()).thenReturn(componentId);
        when(event.getValues()).thenReturn(values);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getIdLong()).thenReturn(guildId);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(lookupWorkflow.acceptsGuild(guildId)).thenReturn(true);
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

    private static ReplyCallbackAction ephemeralAcknowledgement(SlashCommandInteractionEvent event) {
        ReplyCallbackAction acknowledgement = mock(ReplyCallbackAction.class);
        InteractionHook hook = mock(InteractionHook.class);
        when(event.deferReply(true)).thenReturn(acknowledgement);
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
