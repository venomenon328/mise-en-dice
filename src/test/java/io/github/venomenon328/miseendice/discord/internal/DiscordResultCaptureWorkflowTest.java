package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.catalog.api.ResultIngredientCatalogQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultView;
import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.FormData;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.MappingComplete;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.MappingStep;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.OperatorContext;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.PhotoSource;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Rejected;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.ReplaceConfirmation;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Saved;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiscordResultCaptureWorkflowTest {
    private static final long GUILD = 99;
    private static final long CHALLENGE = 7;
    private static final long PARTICIPANT = 42;
    private static final OperatorContext OPERATOR = new OperatorContext(GUILD, "9001", true);

    private final ChallengeArchiveQueries archiveQueries = mock(ChallengeArchiveQueries.class);
    private final ChallengeResultCommands resultCommands = mock(ChallengeResultCommands.class);
    private final ChallengeResultQueries resultQueries = mock(ChallengeResultQueries.class);
    private final ParticipantCommands participantCommands = mock(ParticipantCommands.class);
    private final ParticipantQueries participantQueries = mock(ParticipantQueries.class);
    private final ResultIngredientCatalogQueries catalogQueries = mock(ResultIngredientCatalogQueries.class);
    private MutableClock clock;
    private DiscordResultCaptureWorkflow workflow;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-23T09:00:00Z"));
        workflow = workflow(clock, 100);
        when(archiveQueries.listChallenges(any())).thenReturn(page(List.of(challenge(CHALLENGE, ChallengeStatus.ACTIVE)), 1, 1));
        when(archiveQueries.listActiveChallenges(any())).thenReturn(page(List.of(challenge(CHALLENGE, ChallengeStatus.ACTIVE)), 1, 1));
        when(archiveQueries.findChallengeByNumber(CHALLENGE)).thenReturn(Optional.of(challenge(CHALLENGE, ChallengeStatus.ACTIVE)));
        when(archiveQueries.findLatestChallenge()).thenReturn(Optional.of(challenge(CHALLENGE, ChallengeStatus.ACTIVE)));
        when(participantCommands.resolveOrCreateParticipant(any())).thenReturn(participant(false));
        when(participantQueries.findParticipantByExternalIdentity("discord", "1001"))
                .thenReturn(Optional.of(participant(false)));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT)).thenReturn(Optional.empty());
        when(catalogQueries.findUniqueExactMatch(any())).thenReturn(Optional.empty());
        when(catalogQueries.searchLiterally(any())).thenReturn(List.of());
    }

    @Test
    void rejectsWrongGuildOrMissingOperatorBeforeQueriesDownloadOrCoreWork() {
        PhotoSource source = photo("plate.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> workflow.startCapture(new OperatorContext(98, "9001", true), "Text", List.of(source)))
                .isInstanceOf(Rejected.class);
        assertThatThrownBy(() -> workflow.setPhoto(new OperatorContext(GUILD, "9001", false), CHALLENGE, "1001",
                source, false)).isInstanceOf(Rejected.class);

        verify(source, never()).download();
        verifyNoInteractions(archiveQueries, resultCommands, resultQueries, participantCommands, participantQueries,
                catalogQueries);
    }

    @Test
    void distinguishesSelectionsAndAllowsResultWithoutEvaluationAndPhoto() {
        var preparation = workflow.startCapture(OPERATOR, "Die vollständige Rezeptbeschreibung", List.of());
        assertThat(preparation.selectedChallengeNumber()).isEqualTo(CHALLENGE);
        assertThat(preparation.photos()).singleElement().satisfies(photo -> {
            assertThat(photo.index()).isEqualTo(-1);
            assertThat(photo.selected()).isTrue();
        });
        assertThat(preparation.readyForModal()).isFalse();

        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Ergebnis-Person");
        assertThat(preparation.readyForModal()).isTrue();
        assertThat(workflow.captureModal(OPERATOR, preparation.token()).values().description())
                .isEqualTo("Die vollständige Rezeptbeschreibung");
        when(resultCommands.createChallengeResult(any())).thenReturn(result(List.of(), false, 0));

        Saved saved = (Saved) workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "", ""), false);

        ArgumentCaptor<ParticipantCommands.ResolveOrCreateParticipant> participant = ArgumentCaptor.forClass(
                ParticipantCommands.ResolveOrCreateParticipant.class);
        verify(participantCommands).resolveOrCreateParticipant(participant.capture());
        assertThat(participant.getValue().externalSubject()).isEqualTo("1001");
        assertThat(participant.getValue().displayNameFallback()).isEqualTo("Ergebnis-Person");
        verify(participantCommands, never()).addDefaultElectorateMember(any());
        ArgumentCaptor<ChallengeResultCommands.CreateChallengeResult> command = ArgumentCaptor.forClass(
                ChallengeResultCommands.CreateChallengeResult.class);
        verify(resultCommands).createChallengeResult(command.capture());
        assertThat(command.getValue().participantId()).isEqualTo(PARTICIPANT);
        assertThat(command.getValue().result().evaluation()).isNull();
        assertThat(command.getValue().photo()).isNull();
        assertThat(saved.mappingAvailable()).isFalse();
        assertThat(workflow.liveDraftCount()).isZero();
    }

    @Test
    void capturesOnlyOpenRequirementConcretizationsInTheirSeparateBoundedModal() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Ergebnis-Person");
        assertThat(preparation.concretizations())
                .extracting(DiscordResultCaptureWorkflow.ConcretizationField::requirementPosition,
                        DiscordResultCaptureWorkflow.ConcretizationField::requirementDisplayText)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3, "C"),
                        org.assertj.core.groups.Tuple.tuple(4, "D"));
        var modal = workflow.captureConcretizationModal(OPERATOR, preparation.token());
        assertThat(modal.fields()).hasSize(2);
        assertThat(DiscordResultCaptureJdaListener.concretizationModal(modal, "concrete-capture").getComponents())
                .hasSize(2);

        preparation = workflow.submitCaptureConcretizations(OPERATOR, preparation.token(),
                Map.of(3, "   ", 4, "  Nata de Coco  "));
        assertThat(preparation.concretizations().get(1).value()).isEqualTo("Nata de Coco");
        when(resultCommands.createChallengeResult(any())).thenReturn(result(List.of(), false, 0));
        workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "Knoblauch", ""), false);

        ArgumentCaptor<ChallengeResultCommands.CreateChallengeResult> command = ArgumentCaptor.forClass(
                ChallengeResultCommands.CreateChallengeResult.class);
        verify(resultCommands).createChallengeResult(command.capture());
        assertThat(command.getValue().result().concretizations()).singleElement().satisfies(concretization -> {
            assertThat(concretization.requirementPosition()).isEqualTo(4);
            assertThat(concretization.displayText()).isEqualTo("Nata de Coco");
        });
        assertThat(command.getValue().result().ownIngredients()).singleElement()
                .satisfies(ingredient -> assertThat(ingredient.displayText()).isEqualTo("Knoblauch"));
    }

    @Test
    void capturePreparationOmitsConcretizationStepWhenEveryRequirementWasSpecific() {
        var specific = new ChallengeArchiveQueries.PublicChallenge(CHALLENGE,
                Instant.parse("2026-08-20T12:00:00Z"), List.of(
                new ChallengeArchiveQueries.RequirementSnapshot(1, "A", ChallengeArchiveQueries.Specificity.SPECIFIC),
                new ChallengeArchiveQueries.RequirementSnapshot(2, "B", ChallengeArchiveQueries.Specificity.SPECIFIC),
                new ChallengeArchiveQueries.RequirementSnapshot(3, "C", ChallengeArchiveQueries.Specificity.SPECIFIC),
                new ChallengeArchiveQueries.RequirementSnapshot(4, "D", ChallengeArchiveQueries.Specificity.SPECIFIC)),
                ChallengeArchiveQueries.RestrictionSnapshot.none(), false);
        when(archiveQueries.listChallenges(any())).thenReturn(page(List.of(specific), 1, 1));
        when(archiveQueries.listActiveChallenges(any())).thenReturn(page(List.of(specific), 1, 1));
        when(archiveQueries.findChallengeByNumber(CHALLENGE)).thenReturn(Optional.of(specific));

        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        assertThat(preparation.concretizations()).isEmpty();
        assertThatThrownBy(() -> workflow.captureConcretizationModal(OPERATOR, preparation.token()))
                .isInstanceOf(Rejected.class).hasMessageContaining("keine offenen Vorgaben");
    }

    @Test
    void editPreparationShowsConcretizationsAndUpdatesThemWithoutUsingTheTextOrPhotoMutation() {
        var current = result(List.of(ingredient(501, "Knoblauch")), List.of(concretization(3, "Weinbergschnecke")),
                true, 4);
        var updated = result(List.of(ingredient(501, "Knoblauch")), List.of(concretization(4, "Nata de Coco")),
                true, 5);
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT)).thenReturn(Optional.of(current));
        when(resultCommands.updateResultConcretizations(any())).thenReturn(updated);

        var preparation = workflow.startEditPreparation(OPERATOR, CHALLENGE, "1001");
        assertThat(preparation.concretizations()).extracting(
                        DiscordResultCaptureWorkflow.ConcretizationField::value)
                .containsExactly("Weinbergschnecke", "");
        Saved saved = workflow.submitEditConcretizations(OPERATOR, preparation.token(),
                Map.of(3, "", 4, "Nata de Coco"));

        ArgumentCaptor<ChallengeResultCommands.UpdateResultConcretizations> command = ArgumentCaptor.forClass(
                ChallengeResultCommands.UpdateResultConcretizations.class);
        verify(resultCommands).updateResultConcretizations(command.capture());
        assertThat(command.getValue().expectedVersion()).isEqualTo(4);
        assertThat(command.getValue().concretizations()).singleElement()
                .satisfies(value -> assertThat(value.requirementPosition()).isEqualTo(4));
        assertThat(saved.mappingAvailable()).isTrue();
        verify(resultCommands, never()).updateChallengeResult(any());
        verify(resultCommands, never()).setChallengeResultPhoto(any());
    }

    @Test
    void concretizationMappingSearchesOnlyAllowedRefinementsAndCarriesExactTargetAndVersion() {
        var captured = result(List.of(), List.of(concretization(3, "Weinbergschnecke")), false, 0);
        var referenced = result(List.of(), List.of(concretization(3, "Weinbergschnecke")), false, 1);
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Ergebnis-Person");
        preparation = workflow.submitCaptureConcretizations(OPERATOR, preparation.token(), Map.of(3, "Weinbergschnecke"));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT))
                .thenReturn(Optional.empty(), Optional.of(captured), Optional.of(captured), Optional.of(referenced));
        when(resultCommands.createChallengeResult(any())).thenReturn(captured);
        var inactive = new ResultIngredientCatalogQueries.IngredientConcept(901, "SNAIL", "Weinbergschnecke", false);
        when(catalogQueries.findUniqueExactRefinementMatch(300, "Weinbergschnecke"))
                .thenReturn(Optional.of(inactive));
        when(catalogQueries.searchRefinementsLiterally(300, "Weinbergschnecke")).thenReturn(List.of(inactive));

        Saved saved = (Saved) workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "", ""), false);
        MappingStep step = workflow.startMapping(OPERATOR, saved.mappingToken());
        assertThat(step.concretization()).isTrue();
        assertThat(step.requirementDisplayText()).isEqualTo("C");
        assertThat(step.choices()).singleElement().satisfies(choice -> assertThat(choice.active()).isFalse());
        assertThat(workflow.assignMapping(OPERATOR, saved.mappingToken(), step.expectedResultVersion(),
                step.targetKey(), 901L)).isInstanceOf(MappingComplete.class);

        ArgumentCaptor<ChallengeResultCommands.SetResultConcretizationReference> reference = ArgumentCaptor.forClass(
                ChallengeResultCommands.SetResultConcretizationReference.class);
        verify(resultCommands).setResultConcretizationReference(reference.capture());
        assertThat(reference.getValue()).isEqualTo(
                new ChallengeResultCommands.SetResultConcretizationReference(70, 3, 901L, 0));
        verify(catalogQueries, never()).searchLiterally(any());
    }

    @Test
    void onePhotoIsPreselectedWhileMultiplePhotosRequireAnExplicitChoice() {
        PhotoSource first = photo("one.png", "image/png", new byte[]{1});
        PhotoSource second = photo("two.jpg", "image/jpeg", new byte[]{2});
        PhotoSource embedLike = photo("remote.webp", "image/webp", new byte[]{3});

        var one = workflow.startCapture(OPERATOR, "Text", List.of(first, embedLike));
        assertThat(one.photos()).extracting(DiscordResultCaptureWorkflow.PhotoChoice::label)
                .containsExactly("Kein Foto", "one.png");
        assertThat(one.photos()).filteredOn(DiscordResultCaptureWorkflow.PhotoChoice::selected)
                .singleElement().extracting(DiscordResultCaptureWorkflow.PhotoChoice::index).isEqualTo(0);

        var multiple = workflow.startCapture(OPERATOR, "Text", List.of(first, second));
        multiple = workflow.selectPerson(OPERATOR, multiple.token(), "1001", "Person");
        assertThat(multiple.readyForModal()).isFalse();
        multiple = workflow.selectPhoto(OPERATOR, multiple.token(), -1);
        assertThat(multiple.readyForModal()).isTrue();
        verify(first, never()).download();
        verify(second, never()).download();
    }

    @Test
    void fullOverlongMessageRemainsByteExactAndModalSignalsCondensing() {
        String fullText = "Rezept @everyone ``` mit Umlaut ä ".repeat(180);
        var preparation = workflow.startCapture(OPERATOR, fullText, List.of());

        assertThat(preparation.attachFullText()).isTrue();
        assertThat(new String(preparation.fullTextBytes(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(fullText);
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        var modal = workflow.captureModal(OPERATOR, preparation.token());
        assertThat(modal.values().description()).hasSize(4000).isEqualTo(fullText.substring(0, 4000));
        assertThat(modal.sourceTextWasLonger()).isTrue();

        var message = DiscordResultCaptureJdaListener.preparationCreate(preparation);
        assertThat(message.getContent()).contains("vollständige Nachrichtentext", "gekennzeichnet gekürzt");
        assertThat(message.getContent()).hasSizeLessThanOrEqualTo(2000);
        assertThat(message.getFiles()).singleElement().satisfies(file -> assertThat(file.getName())
                .isEqualTo("nachrichtentext.txt"));
        assertThat(message.getAllowedMentions()).isEmpty();
    }

    @Test
    void ownerBindingAndTtlInvalidateBeforePhotoDownloadOrMutation() {
        PhotoSource source = photo("plate.png", "image/png", new byte[]{1});
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of(source));

        assertThatThrownBy(() -> workflow.selectPerson(new OperatorContext(GUILD, "other", true), preparation.token(),
                "1001", "Person")).isInstanceOf(Rejected.class).hasMessageContaining("anderen Operator");
        clock.advance(Duration.ofMinutes(16));
        assertThatThrownBy(() -> workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person"))
                .isInstanceOf(Rejected.class).hasMessageContaining("abgelaufen");
        verify(source, never()).download();
        verifyNoInteractions(participantCommands, resultCommands);
    }

    @Test
    void draftTtlIsAbsoluteAndDoesNotSlideOnInteraction() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        clock.advance(Duration.ofMinutes(10));
        workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");

        clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> workflow.captureModal(OPERATOR, preparation.token()))
                .isInstanceOf(Rejected.class).hasMessageContaining("abgelaufen");
    }

    @Test
    void storeIsBoundedAndARecreatedWorkflowCannotResumeOldDrafts() {
        DiscordResultCaptureWorkflow bounded = workflow(clock, 2);
        String first = bounded.startCapture(OPERATOR, "eins", List.of()).token();
        bounded.startCapture(OPERATOR, "zwei", List.of());
        bounded.startCapture(OPERATOR, "drei", List.of());
        assertThat(bounded.liveDraftCount()).isEqualTo(2);
        assertThatThrownBy(() -> bounded.selectPerson(OPERATOR, first, "1001", "Person"))
                .isInstanceOf(Rejected.class).hasMessageContaining("abgelaufen");

        DiscordResultCaptureWorkflow afterRestart = workflow(clock, 2);
        assertThatThrownBy(() -> afterRestart.selectPerson(OPERATOR, first, "1001", "Person"))
                .isInstanceOf(Rejected.class).hasMessageContaining("Neustart");
    }

    @Test
    void existingResultNeedsExplicitReplaceAndAttachmentDownloadsOnlyAfterConfirmation() {
        AtomicBoolean downloaded = new AtomicBoolean();
        AtomicLong currentPhotoVersion = new AtomicLong(3);
        PhotoSource source = photo("plate.jpg", "image/jpeg", new byte[]{1, 2}, downloaded);
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of(source));
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        ChallengeResultQueries.ChallengeResultView existing = result(List.of(), true, 5);
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT)).thenReturn(Optional.of(existing));
        when(resultQueries.findChallengeResultPhotoMetadata(CHALLENGE, PARTICIPANT))
                .thenAnswer(invocation -> Optional.of(photoMetadata(currentPhotoVersion.get())));
        when(resultCommands.replaceChallengeResult(any())).thenAnswer(invocation -> result(List.of(), true, 6));

        var firstSubmission = workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Neu", "Neue Beschreibung", "", "", ""), false);
        assertThat(firstSubmission).isInstanceOf(ReplaceConfirmation.class);
        assertThat(downloaded).isFalse();
        verify(resultCommands, never()).replaceChallengeResult(any());

        currentPhotoVersion.set(4);
        var order = inOrder(resultQueries, source, resultCommands);
        Saved saved = (Saved) workflow.confirmCaptureReplacement(OPERATOR, preparation.token());
        ArgumentCaptor<ChallengeResultCommands.ReplaceChallengeResult> replace = ArgumentCaptor.forClass(
                ChallengeResultCommands.ReplaceChallengeResult.class);
        order.verify(resultQueries).findChallengeResultPhotoMetadata(CHALLENGE, PARTICIPANT);
        order.verify(source).download();
        order.verify(resultCommands).replaceChallengeResult(replace.capture());
        assertThat(replace.getValue().photoChange()).isNotNull();
        assertThat(replace.getValue().photoChange().expectedPhotoVersion()).isEqualTo(3);
        assertThat(saved.message()).contains("gespeichert");
    }

    @Test
    void requiredFieldsAndDuplicateIngredientLinesAreRejectedWithoutCoreMutation() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");

        String token = preparation.token();
        assertThatThrownBy(() -> workflow.submitCapture(OPERATOR, token,
                new FormData("", "Beschreibung", "", "", ""), false)).isInstanceOf(Rejected.class);
        assertThatThrownBy(() -> workflow.submitCapture(OPERATOR, token,
                new FormData("Gericht", "Beschreibung", "", "Miso", "miso"), false)).isInstanceOf(Rejected.class);
        verifyNoInteractions(participantCommands, resultCommands);
    }

    @Test
    void challengeDefaultsFollowActiveSemanticsAndCompletedChallengesArePagedAndSelectable() {
        ChallengeArchiveQueries.PublicChallenge completed = challenge(6, ChallengeStatus.COMPLETED);
        when(archiveQueries.listChallenges(any())).thenReturn(page(List.of(challenge(CHALLENGE, ChallengeStatus.ACTIVE), completed),
                1, 2), page(List.of(completed), 2, 2));
        when(archiveQueries.listActiveChallenges(any())).thenReturn(page(List.of(
                challenge(CHALLENGE, ChallengeStatus.ACTIVE), challenge(8, ChallengeStatus.ACTIVE)), 1, 1));
        when(archiveQueries.findChallengeByNumber(6)).thenReturn(Optional.of(completed));

        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        assertThat(preparation.selectedChallengeNumber()).isNull();
        assertThat(preparation.challenges()).extracting(DiscordResultCaptureWorkflow.ChallengeChoice::statusLabel)
                .contains("aktiv", "abgeschlossen");
        preparation = workflow.navigateChallenges(OPERATOR, preparation.token(), 2);
        preparation = workflow.selectChallenge(OPERATOR, preparation.token(), 6);
        assertThat(preparation.selectedChallengeNumber()).isEqualTo(6);
    }

    @Test
    void selectedChallengeDoesNotDisplaceAFullChallengeSelectionPage() {
        ChallengeArchiveQueries.PublicChallenge selected = challenge(100, ChallengeStatus.ACTIVE);
        List<ChallengeArchiveQueries.PublicChallenge> firstPage = java.util.stream.LongStream.rangeClosed(76, 100)
                .mapToObj(number -> challenge(number, ChallengeStatus.COMPLETED)).toList();
        List<ChallengeArchiveQueries.PublicChallenge> secondPage = java.util.stream.LongStream.rangeClosed(51, 75)
                .mapToObj(number -> challenge(number, ChallengeStatus.COMPLETED)).toList();
        when(archiveQueries.listChallenges(any())).thenReturn(page(firstPage, 1, 2), page(secondPage, 2, 2));
        when(archiveQueries.listActiveChallenges(any())).thenReturn(page(List.of(selected), 1, 1));
        when(archiveQueries.findChallengeByNumber(100)).thenReturn(Optional.of(selected));

        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        assertThat(preparation.selectedChallengeNumber()).isEqualTo(100);

        preparation = workflow.navigateChallenges(OPERATOR, preparation.token(), 2);

        assertThat(preparation.challenges()).hasSize(25)
                .extracting(DiscordResultCaptureWorkflow.ChallengeChoice::challengeNumber)
                .containsExactlyElementsOf(secondPage.stream().map(ChallengeArchiveQueries.PublicChallenge::challengeNumber).toList())
                .doesNotContain(100L);
        assertThat(preparation.selectedChallengeNumber()).isEqualTo(100);
    }

    @Test
    void noActiveChallengeSuggestsTheLatestConfirmedChallenge() {
        ChallengeArchiveQueries.PublicChallenge completed = challenge(6, ChallengeStatus.COMPLETED);
        when(archiveQueries.listChallenges(any())).thenReturn(page(List.of(completed), 1, 1));
        when(archiveQueries.listActiveChallenges(any())).thenReturn(page(List.of(), 1, 0));
        when(archiveQueries.findLatestChallenge()).thenReturn(Optional.of(completed));
        when(archiveQueries.findChallengeByNumber(6)).thenReturn(Optional.of(completed));

        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());

        assertThat(preparation.selectedChallengeNumber()).isEqualTo(6);
        assertThat(preparation.challenges()).singleElement()
                .satisfies(choice -> assertThat(choice.statusLabel()).isEqualTo("abgeschlossen"));
    }

    @Test
    void ingredientWizardSuggestsExactMatchesShowsInactiveSearchAndAllowsNoReference() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        List<ChallengeResultQueries.ResultIngredientView> ingredients = List.of(
                ingredient(501, "Miso"), ingredient(502, "Chili-Sauce"));
        when(resultCommands.createChallengeResult(any())).thenReturn(result(ingredients, false, 0));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT))
                .thenReturn(Optional.empty(), Optional.of(result(ingredients, false, 0)),
                        Optional.of(result(ingredients, false, 0)), Optional.of(result(ingredients, false, 1)),
                        Optional.of(result(ingredients, false, 1)));
        var exact = new ResultIngredientCatalogQueries.IngredientConcept(11, "MISO", "Miso", true);
        var inactive = new ResultIngredientCatalogQueries.IngredientConcept(12, "OLD_CHILI", "Alte Chili-Sauce", false);
        when(catalogQueries.findUniqueExactMatch("Miso")).thenReturn(Optional.of(exact));
        when(catalogQueries.findUniqueExactMatch("Chili-Sauce")).thenReturn(Optional.empty());
        when(catalogQueries.searchLiterally("Miso")).thenReturn(List.of(exact));
        when(catalogQueries.searchLiterally("Chili-Sauce")).thenReturn(List.of(inactive));

        Saved saved = (Saved) workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "Miso\nChili-Sauce", ""), false);
        MappingStep first = workflow.startMapping(OPERATOR, saved.mappingToken());
        assertThat(first.ingredientText()).isEqualTo("Miso");
        assertThat(first.exactSuggestionId()).isEqualTo(11);
        assertThat(first.choices()).singleElement().satisfies(choice -> assertThat(choice.exact()).isTrue());

        MappingStep second = (MappingStep) workflow.assignMapping(OPERATOR, saved.mappingToken(),
                first.expectedResultVersion(), first.targetKey(), null);
        assertThat(second.ingredientText()).isEqualTo("Chili-Sauce");
        assertThat(second.choices()).singleElement().satisfies(choice -> assertThat(choice.active()).isFalse());
        ArgumentCaptor<ChallengeResultCommands.SetResultIngredientReference> reference = ArgumentCaptor.forClass(
                ChallengeResultCommands.SetResultIngredientReference.class);
        verify(resultCommands).setResultIngredientReference(reference.capture());
        assertThat(reference.getValue().resultIngredientId()).isEqualTo(501);
        assertThat(reference.getValue().ingredientConceptId()).isNull();
        assertThat(DiscordResultCaptureJdaListener.mappingEdit(second).getContent()).contains("Chili-Sauce");
    }

    @Test
    void staleIngredientWizardCannotApplyAnOldChoiceToAReplacementIngredient() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        List<ChallengeResultQueries.ResultIngredientView> initialIngredients = List.of(
                ingredient(501, "Miso"), ingredient(502, "Chili-Sauce"));
        List<ChallengeResultQueries.ResultIngredientView> changedIngredients = List.of(
                ingredient(503, "Tofu"), ingredient(502, "Chili-Sauce"));
        when(resultCommands.createChallengeResult(any())).thenReturn(result(initialIngredients, false, 0));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT))
                .thenReturn(Optional.empty(), Optional.of(result(initialIngredients, false, 0)),
                        Optional.of(result(changedIngredients, false, 1)));

        Saved saved = (Saved) workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "Miso\nChili-Sauce", ""), false);
        MappingStep visible = workflow.startMapping(OPERATOR, saved.mappingToken());
        assertThat(visible.ingredientText()).isEqualTo("Miso");

        assertThatThrownBy(() -> workflow.assignMapping(OPERATOR, saved.mappingToken(),
                visible.expectedResultVersion(), visible.targetKey(), 11L))
                .isInstanceOf(Rejected.class).hasMessageContaining("zwischenzeitlich geändert");
        verify(resultCommands, never()).setResultIngredientReference(any());
    }

    @Test
    void staleVisibleWizardStepCannotApplyItsChoiceOrSearchToTheNextIngredient() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        List<ChallengeResultQueries.ResultIngredientView> ingredients = List.of(
                ingredient(501, "Miso"), ingredient(502, "Chili-Sauce"));
        ChallengeResultView initial = result(ingredients, false, 0);
        ChallengeResultView afterFirstAssignment = result(ingredients, false, 1);
        when(resultCommands.createChallengeResult(any())).thenReturn(initial);
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT))
                .thenReturn(Optional.empty(), Optional.of(initial), Optional.of(initial),
                        Optional.of(afterFirstAssignment));

        Saved saved = (Saved) workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "Miso\nChili-Sauce", ""), false);
        MappingStep first = workflow.startMapping(OPERATOR, saved.mappingToken());
        MappingStep second = (MappingStep) workflow.assignMapping(OPERATOR, saved.mappingToken(),
                first.expectedResultVersion(), first.targetKey(), null);
        assertThat(second.ingredientText()).isEqualTo("Chili-Sauce");

        assertThatThrownBy(() -> workflow.assignMapping(OPERATOR, saved.mappingToken(),
                first.expectedResultVersion(), first.targetKey(), 11L))
                .isInstanceOf(Rejected.class).hasMessageContaining("nicht mehr gültig");
        assertThatThrownBy(() -> workflow.searchMapping(OPERATOR, saved.mappingToken(),
                first.expectedResultVersion(), first.targetKey(), "Tofu"))
                .isInstanceOf(Rejected.class).hasMessageContaining("nicht mehr gültig");
        verify(resultCommands, times(1)).setResultIngredientReference(any());
    }

    @Test
    void abortingMappingLeavesSavedResultAndReferenceMutationNeverRewritesFreeText() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        List<ChallengeResultQueries.ResultIngredientView> ingredients = List.of(ingredient(501, "Freier Text"));
        when(resultCommands.createChallengeResult(any())).thenReturn(result(ingredients, false, 0));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT))
                .thenReturn(Optional.empty(), Optional.of(result(ingredients, false, 0)));

        Saved saved = (Saved) workflow.submitCapture(OPERATOR, preparation.token(),
                new FormData("Gericht", "Beschreibung", "", "Freier Text", ""), false);
        workflow.startMapping(OPERATOR, saved.mappingToken());
        workflow.abortMapping(OPERATOR, saved.mappingToken());

        verify(resultCommands, never()).setResultIngredientReference(any());
        verify(resultCommands, never()).removeChallengeResult(any());
        assertThat(workflow.liveDraftCount()).isZero();
    }

    @Test
    void editAndRemoveTargetKnownParticipantAndKeepPhotoIndependent() {
        var linkedMiso = new ChallengeResultQueries.ResultIngredientView(501, "Miso",
                new ChallengeResultQueries.IngredientConceptReference(11, "MISO", "Miso", true));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT)).thenReturn(Optional.of(result(
                List.of(linkedMiso), true, 4)));
        when(resultCommands.updateChallengeResult(any())).thenReturn(result(List.of(ingredient(502, "Tofu")), true, 5));

        var edit = workflow.startEdit(OPERATOR, CHALLENGE, "1001");
        assertThat(edit.values()).extracting(FormData::dishName, FormData::ingredientsPartOne)
                .containsExactly("Gericht", "Miso");
        Saved edited = workflow.submitEdit(OPERATOR, edit.token(),
                new FormData("Neu", "Text", "", "Tofu", ""));
        ArgumentCaptor<ChallengeResultCommands.UpdateChallengeResult> update = ArgumentCaptor.forClass(
                ChallengeResultCommands.UpdateChallengeResult.class);
        verify(resultCommands).updateChallengeResult(update.capture());
        assertThat(update.getValue().expectedVersion()).isEqualTo(4);
        assertThat(update.getValue().result().ownIngredients()).singleElement().satisfies(ingredient -> {
            assertThat(ingredient.displayText()).isEqualTo("Tofu");
            assertThat(ingredient.ingredientConceptId()).isNull();
        });
        assertThat(edited.mappingAvailable()).isTrue();
        verify(resultCommands, never()).setChallengeResultPhoto(any());

        var removal = workflow.startRemove(OPERATOR, CHALLENGE, "1001", "Guild-Name");
        assertThat(removal.participantName()).isEqualTo("Guild-Name");
        Saved removed = workflow.confirmRemove(OPERATOR, removal.token());
        verify(resultCommands).removeChallengeResult(new ChallengeResultCommands.RemoveChallengeResult(CHALLENGE, PARTICIPANT));
        assertThat(removed.message()).contains("entfernt");
    }

    @Test
    void photoMaintenanceRejectsSilentReplaceBeforeDownloadThenUsesPhotoVersionAndRemovesOnlyPhoto() {
        AtomicBoolean downloaded = new AtomicBoolean();
        PhotoSource source = photo("plate.png", "image/png", new byte[]{1, 2}, downloaded);
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT)).thenReturn(Optional.of(result(List.of(), true, 2)));
        when(resultQueries.findChallengeResultPhotoMetadata(CHALLENGE, PARTICIPANT)).thenReturn(Optional.of(photoMetadata(3)));

        assertThatThrownBy(() -> workflow.setPhoto(OPERATOR, CHALLENGE, "1001", source, false))
                .isInstanceOf(Rejected.class).hasMessageContaining("ersetzen:true");
        assertThat(downloaded).isFalse();
        workflow.setPhoto(OPERATOR, CHALLENGE, "1001", source, true);
        assertThat(downloaded).isTrue();
        ArgumentCaptor<ChallengeResultCommands.SetChallengeResultPhoto> set = ArgumentCaptor.forClass(
                ChallengeResultCommands.SetChallengeResultPhoto.class);
        verify(resultCommands).setChallengeResultPhoto(set.capture());
        assertThat(set.getValue().replaceExisting()).isTrue();
        assertThat(set.getValue().expectedPhotoVersion()).isEqualTo(3);

        workflow.removePhoto(OPERATOR, CHALLENGE, "1001");
        verify(resultCommands).removeChallengeResultPhoto(
                new ChallengeResultCommands.RemoveChallengeResultPhoto(CHALLENGE, PARTICIPANT, 3));
        verify(resultCommands, never()).updateChallengeResult(any());
        verify(resultCommands, never()).removeChallengeResult(any());
    }

    @Test
    void editingUnchangedIngredientTextPreservesItsOptionalCatalogReference() {
        var linkedMiso = new ChallengeResultQueries.ResultIngredientView(501, "Miso",
                new ChallengeResultQueries.IngredientConceptReference(11, "MISO", "Miso", true));
        when(resultQueries.findChallengeResult(CHALLENGE, PARTICIPANT))
                .thenReturn(Optional.of(result(List.of(linkedMiso), false, 4)));
        when(resultCommands.updateChallengeResult(any())).thenReturn(result(List.of(linkedMiso), false, 5));

        var edit = workflow.startEdit(OPERATOR, CHALLENGE, "1001");
        workflow.submitEdit(OPERATOR, edit.token(), new FormData("Gericht", "Beschreibung", "", "miso", ""));

        ArgumentCaptor<ChallengeResultCommands.UpdateChallengeResult> update = ArgumentCaptor.forClass(
                ChallengeResultCommands.UpdateChallengeResult.class);
        verify(resultCommands).updateChallengeResult(update.capture());
        assertThat(update.getValue().result().ownIngredients()).singleElement()
                .satisfies(ingredient -> assertThat(ingredient.ingredientConceptId()).isEqualTo(11));
    }

    @Test
    void modalUsesFiveBoundedFieldsAndPreparationComponentsStayWithinDiscordLimits() {
        var preparation = workflow.startCapture(OPERATOR, "Text", List.of());
        preparation = workflow.selectPerson(OPERATOR, preparation.token(), "1001", "Person");
        var modal = DiscordResultCaptureJdaListener.modal(workflow.captureModal(OPERATOR, preparation.token()), "capture");

        assertThat(modal.getComponents()).hasSize(5);
        assertThat(modal.getId()).hasSizeLessThanOrEqualTo(100);
        MessageEditDataAssert.assertSafe(DiscordResultCaptureJdaListener.preparationEdit(preparation));
    }

    @Test
    void publicFollowUpFailureStillReportsTheAlreadyPersistedResultTruthfully() {
        Saved persisted = new Saved(null, CHALLENGE, "Das Challenge-Ergebnis wurde gespeichert.", false);

        var failed = DiscordResultCaptureJdaListener.savedEdit(persisted, false, true);

        assertThat(failed.getContent()).contains("wurde gespeichert", "öffentliche Darstellung konnte jedoch nicht")
                .doesNotContain("Persistenz fehlgeschlagen", "nicht gespeichert");
        assertThat(failed.getAllowedMentions()).isEmpty();
    }

    private DiscordResultCaptureWorkflow workflow(Clock clock, int capacity) {
        return new DiscordResultCaptureWorkflow(properties(), archiveQueries, resultCommands, resultQueries,
                participantCommands, participantQueries, catalogQueries, clock, Duration.ofMinutes(15), capacity);
    }

    private static DiscordProperties properties() {
        return new DiscordProperties(true, "token", GUILD, 77777, ZoneId.of("Europe/Berlin"), Map.of());
    }

    private static ChallengeArchiveQueries.ChallengePage page(List<ChallengeArchiveQueries.PublicChallenge> challenges,
                                                               int page, int totalPages) {
        return new ChallengeArchiveQueries.ChallengePage(page, 25, challenges.size(),
                challenges.isEmpty() ? null : challenges.getFirst().challengeNumber(), totalPages, challenges);
    }

    private static ChallengeArchiveQueries.PublicChallenge challenge(long number, ChallengeStatus status) {
        return new ChallengeArchiveQueries.PublicChallenge(number, Instant.parse("2026-08-20T12:00:00Z"), List.of(
                new ChallengeArchiveQueries.RequirementSnapshot(1, "A", ChallengeArchiveQueries.Specificity.SPECIFIC),
                new ChallengeArchiveQueries.RequirementSnapshot(2, "B", ChallengeArchiveQueries.Specificity.SPECIFIC),
                new ChallengeArchiveQueries.RequirementSnapshot(3, "C", ChallengeArchiveQueries.Specificity.OPEN),
                new ChallengeArchiveQueries.RequirementSnapshot(4, "D", ChallengeArchiveQueries.Specificity.OPEN)),
                ChallengeArchiveQueries.RestrictionSnapshot.none(), false, status,
                status == ChallengeStatus.COMPLETED ? Instant.parse("2026-08-22T12:00:00Z") : null,
                0, List.of());
    }

    private static ParticipantQueries.ParticipantView participant(boolean electorate) {
        return new ParticipantQueries.ParticipantView(PARTICIPANT, "P-42", "Ergebnis-Person", true, electorate);
    }

    private static ChallengeResultQueries.ChallengeResultView result(
            List<ChallengeResultQueries.ResultIngredientView> ingredients, boolean photo, long version) {
        return result(ingredients, List.of(), photo, version);
    }

    private static ChallengeResultQueries.ChallengeResultView result(
            List<ChallengeResultQueries.ResultIngredientView> ingredients,
            List<ChallengeResultQueries.ResultConcretizationView> concretizations, boolean photo, long version) {
        return new ChallengeResultQueries.ChallengeResultView(70, CHALLENGE,
                new ChallengeResultQueries.ParticipantReference(PARTICIPANT, "P-42", "Ergebnis-Person", true),
                "Gericht", "Beschreibung", null, ingredients, concretizations, photo, version,
                Instant.parse("2026-08-23T08:00:00Z"), Instant.parse("2026-08-23T08:00:00Z"));
    }

    private static ChallengeResultQueries.ResultConcretizationView concretization(int position, String text) {
        return new ChallengeResultQueries.ResultConcretizationView(70, position, position == 3 ? 300 : 400,
                position == 3 ? "C" : "D", text, null);
    }

    private static ChallengeResultQueries.ResultIngredientView ingredient(long id, String text) {
        return new ChallengeResultQueries.ResultIngredientView(id, text, null);
    }

    private static ChallengeResultQueries.ChallengeResultPhotoMetadata photoMetadata(long version) {
        return new ChallengeResultQueries.ChallengeResultPhotoMetadata(CHALLENGE, PARTICIPANT, "image/png", "plate.png",
                2, 1, 1, "aa", version, Instant.parse("2026-08-23T08:00:00Z"),
                Instant.parse("2026-08-23T08:00:00Z"));
    }

    private static PhotoSource photo(String filename, String contentType, byte[] bytes) {
        return photo(filename, contentType, bytes, new AtomicBoolean());
    }

    private static PhotoSource photo(String filename, String contentType, byte[] bytes, AtomicBoolean downloaded) {
        PhotoSource source = mock(PhotoSource.class);
        when(source.originalFilename()).thenReturn(filename);
        when(source.declaredContentType()).thenReturn(contentType);
        when(source.declaredSize()).thenReturn((long) bytes.length);
        when(source.download()).thenAnswer(invocation -> {
            downloaded.set(true);
            return bytes.clone();
        });
        return source;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    /** Small local assertion helper keeps the JDA transport checks readable. */
    private static final class MessageEditDataAssert {
        static void assertSafe(net.dv8tion.jda.api.utils.messages.MessageEditData message) {
            assertThat(message.getContent()).hasSizeLessThanOrEqualTo(2000);
            assertThat(message.getComponents()).hasSizeLessThanOrEqualTo(5);
            assertThat(message.getAllowedMentions()).isEmpty();
        }
    }
}
