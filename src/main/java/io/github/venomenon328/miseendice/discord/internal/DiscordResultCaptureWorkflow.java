package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.catalog.api.ResultIngredientCatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.ResultIngredientCatalogQueries.IngredientConcept;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands.ChallengeResultPhotoUpload;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands.OwnIngredientInput;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultCommands.ResultData;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultIngredientNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultPhotoVersionConflictException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultView;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultVersionConflictException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter-owned, restart-volatile result workflow. It deliberately retains only bounded interaction data and delegates
 * every durable mutation to the public Challenge and Catalog APIs.
 */
final class DiscordResultCaptureWorkflow {
    static final String DISCORD_PROVIDER = "discord";
    static final int MAX_PHOTO_BYTES = 10 * 1024 * 1024;
    static final int CHALLENGE_PAGE_SIZE = 25;
    static final int MAX_CATALOG_OPTIONS = 24; // one Discord select slot always remains for "without reference"
    static final Duration DEFAULT_DRAFT_TTL = Duration.ofMinutes(15);
    static final int DEFAULT_MAX_DRAFTS = 100;

    private final DiscordProperties properties;
    private final ChallengeArchiveQueries archiveQueries;
    private final ChallengeResultCommands resultCommands;
    private final ChallengeResultQueries resultQueries;
    private final ParticipantCommands participantCommands;
    private final ParticipantQueries participantQueries;
    private final ResultIngredientCatalogQueries catalogQueries;
    private final DraftStore drafts;

    DiscordResultCaptureWorkflow(DiscordProperties properties,
                                 ChallengeArchiveQueries archiveQueries,
                                 ChallengeResultCommands resultCommands,
                                 ChallengeResultQueries resultQueries,
                                 ParticipantCommands participantCommands,
                                 ParticipantQueries participantQueries,
                                 ResultIngredientCatalogQueries catalogQueries) {
        this(properties, archiveQueries, resultCommands, resultQueries, participantCommands, participantQueries,
                catalogQueries, Clock.systemUTC(), DEFAULT_DRAFT_TTL, DEFAULT_MAX_DRAFTS);
    }

    DiscordResultCaptureWorkflow(DiscordProperties properties,
                                 ChallengeArchiveQueries archiveQueries,
                                 ChallengeResultCommands resultCommands,
                                 ChallengeResultQueries resultQueries,
                                 ParticipantCommands participantCommands,
                                 ParticipantQueries participantQueries,
                                 ResultIngredientCatalogQueries catalogQueries,
                                 Clock clock, Duration draftTtl, int maximumDrafts) {
        this.properties = Objects.requireNonNull(properties);
        this.archiveQueries = Objects.requireNonNull(archiveQueries);
        this.resultCommands = Objects.requireNonNull(resultCommands);
        this.resultQueries = Objects.requireNonNull(resultQueries);
        this.participantCommands = Objects.requireNonNull(participantCommands);
        this.participantQueries = Objects.requireNonNull(participantQueries);
        this.catalogQueries = Objects.requireNonNull(catalogQueries);
        this.drafts = new DraftStore(clock, draftTtl, maximumDrafts);
    }

    Preparation startCapture(OperatorContext context, String messageText, List<PhotoSource> attachments) {
        requireOperator(context);
        ChallengeArchiveQueries.ChallengePage firstPage = archiveQueries.listChallenges(
                new ChallengeArchiveQueries.PageRequest(1, CHALLENGE_PAGE_SIZE));
        if (firstPage.totalChallenges() == 0) {
            throw rejected("Es gibt noch keine bestätigte Challenge, der ein Ergebnis zugeordnet werden kann.");
        }
        ChallengeArchiveQueries.ChallengePage active = archiveQueries.listActiveChallenges(
                new ChallengeArchiveQueries.PageRequest(1, 2));
        Long selectedChallenge = null;
        if (active.totalChallenges() == 1 && active.challenges().size() == 1) {
            selectedChallenge = active.challenges().getFirst().challengeNumber();
        } else if (active.totalChallenges() == 0) {
            selectedChallenge = archiveQueries.findLatestChallenge().map(PublicChallenge::challengeNumber).orElse(null);
        }

        List<PhotoSource> supported = attachments == null ? List.of() : attachments.stream()
                .filter(Objects::nonNull)
                .filter(DiscordResultCaptureWorkflow::supportedImageMetadata)
                .limit(10)
                .toList();
        int selectedPhoto = supported.isEmpty() ? Draft.NO_PHOTO
                : supported.size() == 1 ? 0 : Draft.PHOTO_NOT_SELECTED;
        Draft draft = Draft.capture(context, messageText == null ? "" : messageText, supported, selectedChallenge,
                selectedPhoto, firstPage.page(), firstPage.totalPages());
        String token = drafts.create(draft);
        return preparation(token, draft, firstPage);
    }

    Preparation selectPerson(OperatorContext context, String token, String discordUserId, String displayName) {
        Draft draft = captureDraft(context, token);
        draft.externalSubject = required(discordUserId, "Discord-Person");
        draft.personDisplayName = required(displayName, "Anzeigename");
        return preparation(token, draft, challengePage(draft.challengePage));
    }

    Preparation selectChallenge(OperatorContext context, String token, long challengeNumber) {
        Draft draft = captureDraft(context, token);
        if (challengeNumber <= 0 || archiveQueries.findChallengeByNumber(challengeNumber).isEmpty()) {
            throw rejected("Die gewählte Challenge ist nicht mehr verfügbar.");
        }
        draft.challengeNumber = challengeNumber;
        return preparation(token, draft, challengePage(draft.challengePage));
    }

    Preparation selectPhoto(OperatorContext context, String token, int photoIndex) {
        Draft draft = captureDraft(context, token);
        if (photoIndex < Draft.NO_PHOTO || photoIndex >= draft.photos.size()) {
            throw rejected("Die gewählte Bilddatei gehört nicht zu diesem Erfassungsablauf.");
        }
        draft.selectedPhoto = photoIndex;
        return preparation(token, draft, challengePage(draft.challengePage));
    }

    Preparation navigateChallenges(OperatorContext context, String token, int requestedPage) {
        Draft draft = captureDraft(context, token);
        if (requestedPage < 1 || requestedPage > draft.challengePages) {
            throw rejected("Diese Challenge-Auswahlseite gibt es nicht.");
        }
        ChallengeArchiveQueries.ChallengePage page = challengePage(requestedPage);
        draft.challengePage = requestedPage;
        draft.challengePages = page.totalPages();
        return preparation(token, draft, page);
    }

    ResultModal captureModal(OperatorContext context, String token) {
        Draft draft = captureDraft(context, token);
        if (draft.externalSubject == null) {
            throw rejected("Bitte wähle zuerst ausdrücklich die Ergebnis-Person.");
        }
        if (draft.challengeNumber == null) {
            throw rejected("Bitte wähle zuerst ausdrücklich eine Challenge.");
        }
        if (draft.selectedPhoto == Draft.PHOTO_NOT_SELECTED) {
            throw rejected("Bitte wähle ein Foto oder ausdrücklich `kein Foto`.");
        }
        FormData values = draft.form == null
                ? new FormData("", truncate(draft.messageText, 4000), "", "", "")
                : draft.form;
        return new ResultModal(token, "Challenge-Ergebnis erfassen", values, draft.messageText.length() > 4000);
    }

    CaptureSubmission submitCapture(OperatorContext context, String token, FormData form, boolean replaceConfirmed) {
        Draft draft = captureDraft(context, token);
        draft.form = Objects.requireNonNull(form);
        ResultData data = resultData(form);

        ParticipantQueries.ParticipantView participant = participantCommands.resolveOrCreateParticipant(
                new ParticipantCommands.ResolveOrCreateParticipant(DISCORD_PROVIDER, draft.externalSubject,
                        draft.personDisplayName));
        draft.participantId = participant.participantId();
        Optional<ChallengeResultView> existing = resultQueries.findChallengeResult(draft.challengeNumber,
                participant.participantId());
        if (existing.isPresent() && !replaceConfirmed) {
            draft.expectedResultVersion = existing.get().version();
            snapshotExpectedPhotoVersion(draft, participant.participantId());
            return new ReplaceConfirmation(token, draft.challengeNumber, participant.displayName(),
                    existing.get().dishName());
        }
        if (replaceConfirmed && (existing.isEmpty() || draft.expectedResultVersion == null
                || existing.get().version() != draft.expectedResultVersion)) {
            throw rejected("Das vorhandene Ergebnis wurde zwischenzeitlich geändert. Bitte starte die Erfassung erneut.");
        }

        ChallengeResultPhotoUpload photo = selectedPhoto(draft);
        ChallengeResultView saved;
        try {
            if (existing.isEmpty()) {
                saved = resultCommands.createChallengeResult(new ChallengeResultCommands.CreateChallengeResult(
                        draft.challengeNumber, participant.participantId(), data, photo));
            } else {
                if (photo != null && !draft.expectedPhotoVersionCaptured) {
                    throw rejected("Der Fotostand für diese Ersetzung ist nicht mehr eindeutig. Bitte starte die Erfassung erneut.");
                }
                ChallengeResultCommands.PhotoChange photoChange = photo == null ? null
                        : new ChallengeResultCommands.PhotoChange(photo, true, draft.expectedPhotoVersion);
                saved = resultCommands.replaceChallengeResult(new ChallengeResultCommands.ReplaceChallengeResult(
                        draft.challengeNumber, participant.participantId(), draft.expectedResultVersion, data, photoChange));
            }
        } catch (ChallengeResultAlreadyExistsException exception) {
            ChallengeResultView raced = resultQueries.findChallengeResult(draft.challengeNumber, participant.participantId())
                    .orElseThrow(() -> exception);
            draft.expectedResultVersion = raced.version();
            snapshotExpectedPhotoVersion(draft, participant.participantId());
            return new ReplaceConfirmation(token, draft.challengeNumber, participant.displayName(), raced.dishName());
        } catch (ChallengeResultPhotoValidationException exception) {
            throw rejected("Das gewählte Foto ist kein gültiges PNG/JPEG innerhalb der erlaubten Grenzen.");
        } catch (ChallengeResultVersionConflictException | ChallengeResultPhotoVersionConflictException exception) {
            throw rejected("Das Ergebnis wurde zwischenzeitlich geändert. Bitte starte die Erfassung erneut.");
        } catch (ChallengeNotFoundException exception) {
            throw rejected("Die gewählte Challenge ist nicht mehr verfügbar.");
        }
        return saved(draft, saved, "Das Challenge-Ergebnis wurde gespeichert.");
    }

    CaptureSubmission confirmCaptureReplacement(OperatorContext context, String token) {
        Draft draft = captureDraft(context, token);
        if (draft.form == null || draft.expectedResultVersion == null) {
            throw rejected("Für diesen Entwurf liegt keine bestätigbare Ersetzung vor.");
        }
        return submitCapture(context, token, draft.form, true);
    }

    ResultModal startEdit(OperatorContext context, long challengeNumber, String discordUserId) {
        requireOperator(context);
        ParticipantQueries.ParticipantView participant = knownParticipant(discordUserId);
        ChallengeResultView result = resultQueries.findChallengeResult(challengeNumber, participant.participantId())
                .orElseThrow(() -> rejected("Für diese Person ist bei Challenge #" + challengeNumber
                        + " kein Ergebnis gespeichert."));
        FormData form = form(result);
        Draft draft = Draft.maintenance(context, Mode.EDIT, challengeNumber, participant, form);
        draft.expectedResultVersion = result.version();
        String token = drafts.create(draft);
        return new ResultModal(token, "Challenge-Ergebnis bearbeiten", form, false);
    }

    ResultModal editModal(OperatorContext context, String token) {
        Draft draft = draft(context, token, Mode.EDIT);
        return new ResultModal(token, "Challenge-Ergebnis bearbeiten", draft.form, false);
    }

    Saved submitEdit(OperatorContext context, String token, FormData form) {
        Draft draft = draft(context, token, Mode.EDIT);
        draft.form = Objects.requireNonNull(form);
        try {
            ChallengeResultView current = resultQueries.findChallengeResult(draft.challengeNumber, draft.participantId)
                    .orElseThrow(() -> new ChallengeResultNotFoundException(draft.challengeNumber, draft.participantId));
            if (current.version() != draft.expectedResultVersion) {
                throw new ChallengeResultVersionConflictException(draft.challengeNumber, draft.participantId,
                        draft.expectedResultVersion, current.version());
            }
            ChallengeResultView saved = resultCommands.updateChallengeResult(
                    new ChallengeResultCommands.UpdateChallengeResult(draft.challengeNumber, draft.participantId,
                            draft.expectedResultVersion, resultDataPreservingReferences(form, current)));
            return saved(draft, saved, "Das Challenge-Ergebnis wurde bearbeitet.");
        } catch (ChallengeResultNotFoundException exception) {
            throw rejected("Das Ergebnis existiert nicht mehr.");
        } catch (ChallengeResultVersionConflictException exception) {
            throw rejected("Das Ergebnis wurde zwischenzeitlich geändert. Bitte lade die Bearbeitung neu.");
        }
    }

    RemovalConfirmation startRemove(OperatorContext context, long challengeNumber, String discordUserId) {
        requireOperator(context);
        ParticipantQueries.ParticipantView participant = knownParticipant(discordUserId);
        ChallengeResultView result = resultQueries.findChallengeResult(challengeNumber, participant.participantId())
                .orElseThrow(() -> rejected("Für diese Person ist bei Challenge #" + challengeNumber
                        + " kein Ergebnis gespeichert."));
        Draft draft = Draft.maintenance(context, Mode.REMOVE, challengeNumber, participant, null);
        String token = drafts.create(draft);
        return new RemovalConfirmation(token, challengeNumber, participant.displayName(), result.dishName());
    }

    Saved confirmRemove(OperatorContext context, String token) {
        Draft draft = draft(context, token, Mode.REMOVE);
        try {
            resultCommands.removeChallengeResult(new ChallengeResultCommands.RemoveChallengeResult(
                    draft.challengeNumber, draft.participantId));
        } catch (ChallengeResultNotFoundException exception) {
            throw rejected("Das Ergebnis existiert nicht mehr.");
        }
        drafts.remove(token);
        return new Saved(null, draft.challengeNumber, "Das Challenge-Ergebnis wurde entfernt.", false);
    }

    Saved setPhoto(OperatorContext context, long challengeNumber, String discordUserId, PhotoSource source,
                   boolean replaceExisting) {
        requireOperator(context);
        requirePhotoMetadata(source);
        ParticipantQueries.ParticipantView participant = knownParticipant(discordUserId);
        if (resultQueries.findChallengeResult(challengeNumber, participant.participantId()).isEmpty()) {
            throw rejected("Für diese Person ist bei Challenge #" + challengeNumber + " kein Ergebnis gespeichert.");
        }
        Optional<ChallengeResultQueries.ChallengeResultPhotoMetadata> existing =
                resultQueries.findChallengeResultPhotoMetadata(challengeNumber, participant.participantId());
        if (existing.isPresent() && !replaceExisting) {
            throw rejected("Für dieses Ergebnis existiert bereits ein Foto. Bitte nutze `ersetzen:true`.");
        }
        ChallengeResultPhotoUpload upload = download(source);
        try {
            resultCommands.setChallengeResultPhoto(new ChallengeResultCommands.SetChallengeResultPhoto(challengeNumber,
                    participant.participantId(), upload, replaceExisting,
                    existing.map(ChallengeResultQueries.ChallengeResultPhotoMetadata::version).orElse(null)));
        } catch (ChallengeResultPhotoAlreadyExistsException exception) {
            throw rejected("Für dieses Ergebnis existiert bereits ein Foto. Bitte nutze `ersetzen:true`.");
        } catch (ChallengeResultPhotoValidationException exception) {
            throw rejected("Das gewählte Foto ist kein gültiges PNG/JPEG innerhalb der erlaubten Grenzen.");
        } catch (ChallengeResultPhotoVersionConflictException exception) {
            throw rejected("Das Ergebnisfoto wurde zwischenzeitlich geändert. Bitte versuche es erneut.");
        }
        return new Saved(null, challengeNumber, existing.isPresent()
                ? "Das Ergebnisfoto wurde ersetzt." : "Das Ergebnisfoto wurde gespeichert.", false);
    }

    Saved removePhoto(OperatorContext context, long challengeNumber, String discordUserId) {
        requireOperator(context);
        ParticipantQueries.ParticipantView participant = knownParticipant(discordUserId);
        ChallengeResultQueries.ChallengeResultPhotoMetadata photo = resultQueries
                .findChallengeResultPhotoMetadata(challengeNumber, participant.participantId())
                .orElseThrow(() -> rejected("Für dieses Ergebnis ist kein Foto gespeichert."));
        try {
            resultCommands.removeChallengeResultPhoto(new ChallengeResultCommands.RemoveChallengeResultPhoto(
                    challengeNumber, participant.participantId(), photo.version()));
        } catch (ChallengeResultPhotoNotFoundException exception) {
            throw rejected("Für dieses Ergebnis ist kein Foto gespeichert.");
        } catch (ChallengeResultPhotoVersionConflictException exception) {
            throw rejected("Das Ergebnisfoto wurde zwischenzeitlich geändert. Bitte versuche es erneut.");
        }
        return new Saved(null, challengeNumber, "Das Ergebnisfoto wurde entfernt.", false);
    }

    MappingStep startMapping(OperatorContext context, String token) {
        Draft draft = draft(context, token, Mode.MAPPING);
        draft.mappingIndex = 0;
        draft.catalogSearch = null;
        clearMappingSnapshot(draft);
        return mappingStep(token, draft);
    }

    MappingStep searchMapping(OperatorContext context, String token, String searchTerm) {
        Draft draft = draft(context, token, Mode.MAPPING);
        String normalized = required(searchTerm, "Suchtext");
        draft.catalogSearch = normalized;
        return mappingStep(token, draft);
    }

    MappingProgress assignMapping(OperatorContext context, String token, Long ingredientConceptId) {
        Draft draft = draft(context, token, Mode.MAPPING);
        ChallengeResultView current = currentMappingResult(draft);
        requireMappingSnapshot(draft, current);
        if (draft.mappingExpectedResultVersion == null || draft.mappingExpectedIngredientId == null) {
            throw rejected("Dieser Zuordnungsschritt ist nicht mehr gültig. Bitte starte die Zuordnung erneut.");
        }
        long ingredientId = draft.mappingExpectedIngredientId;
        long expectedVersion = draft.mappingExpectedResultVersion;
        try {
            resultCommands.setResultIngredientReference(new ChallengeResultCommands.SetResultIngredientReference(
                    ingredientId, ingredientConceptId, expectedVersion));
        } catch (ChallengeResultIngredientNotFoundException | ChallengeResultVersionConflictException exception) {
            throw rejected("Das Ergebnis wurde zwischenzeitlich geändert. Bitte starte die Zuordnung erneut.");
        }
        draft.mappingIndex++;
        draft.catalogSearch = null;
        clearMappingSnapshot(draft);
        ChallengeResultView refreshed = currentMappingResult(draft);
        if (draft.mappingIndex >= refreshed.ownIngredients().size()) {
            drafts.remove(token);
            return new MappingComplete(draft.challengeNumber,
                    "Alle Zutatenzuordnungen wurden gespeichert; die Freitexte blieben unverändert.");
        }
        return mappingStep(token, draft);
    }

    void abortMapping(OperatorContext context, String token) {
        draft(context, token, Mode.MAPPING);
        drafts.remove(token);
    }

    int liveDraftCount() {
        return drafts.size();
    }

    private Saved saved(Draft draft, ChallengeResultView saved, String message) {
        draft.mode = Mode.MAPPING;
        draft.messageText = "";
        draft.photos = List.of();
        draft.form = null;
        draft.expectedResultVersion = null;
        draft.expectedPhotoVersion = null;
        draft.expectedPhotoVersionCaptured = false;
        draft.participantId = saved.participant().participantId();
        draft.mappingIndex = 0;
        draft.catalogSearch = null;
        clearMappingSnapshot(draft);
        boolean mappingAvailable = !saved.ownIngredients().isEmpty();
        if (!mappingAvailable) {
            drafts.remove(draft.token);
        }
        return new Saved(mappingAvailable ? draft.token : null, saved.challengeNumber(), message, mappingAvailable);
    }

    private MappingStep mappingStep(String token, Draft draft) {
        ChallengeResultView result = currentMappingResult(draft);
        requireMappingSnapshot(draft, result);
        if (draft.mappingIndex >= result.ownIngredients().size()) {
            drafts.remove(token);
            throw rejected("Für dieses Ergebnis gibt es keine weiteren eigenen Zutaten.");
        }
        ChallengeResultQueries.ResultIngredientView ingredient = result.ownIngredients().get(draft.mappingIndex);
        draft.mappingExpectedResultVersion = result.version();
        draft.mappingExpectedIngredientId = ingredient.resultIngredientId();
        Optional<IngredientConcept> exact = catalogQueries.findUniqueExactMatch(ingredient.displayText());
        String term = draft.catalogSearch == null ? ingredient.displayText() : draft.catalogSearch;
        Set<Long> seen = new LinkedHashSet<>();
        List<CatalogChoice> choices = new ArrayList<>();
        exact.ifPresent(match -> {
            seen.add(match.id());
            choices.add(choice(match, true));
        });
        for (IngredientConcept match : catalogQueries.searchLiterally(term)) {
            if (seen.add(match.id()) && choices.size() < MAX_CATALOG_OPTIONS) {
                choices.add(choice(match, false));
            }
        }
        return new MappingStep(token, result.challengeNumber(), draft.mappingIndex + 1, result.ownIngredients().size(),
                ingredient.displayText(), exact.map(IngredientConcept::id).orElse(null), term, List.copyOf(choices));
    }

    private ChallengeResultView currentMappingResult(Draft draft) {
        return resultQueries.findChallengeResult(draft.challengeNumber, draft.participantId)
                .orElseThrow(() -> rejected("Das Ergebnis existiert nicht mehr; die Zuordnung wurde beendet."));
    }

    private static void requireMappingSnapshot(Draft draft, ChallengeResultView current) {
        if (draft.mappingExpectedResultVersion == null && draft.mappingExpectedIngredientId == null) {
            return;
        }
        boolean sameVersion = draft.mappingExpectedResultVersion != null
                && current.version() == draft.mappingExpectedResultVersion;
        boolean sameIngredient = draft.mappingExpectedIngredientId != null
                && draft.mappingIndex < current.ownIngredients().size()
                && current.ownIngredients().get(draft.mappingIndex).resultIngredientId() == draft.mappingExpectedIngredientId;
        if (!sameVersion || !sameIngredient) {
            throw rejected("Das Ergebnis wurde zwischenzeitlich geändert. Bitte starte die Zuordnung erneut.");
        }
    }

    private static void clearMappingSnapshot(Draft draft) {
        draft.mappingExpectedResultVersion = null;
        draft.mappingExpectedIngredientId = null;
    }

    private void snapshotExpectedPhotoVersion(Draft draft, long participantId) {
        if (draft.selectedPhoto == Draft.NO_PHOTO) {
            draft.expectedPhotoVersion = null;
            draft.expectedPhotoVersionCaptured = false;
            return;
        }
        draft.expectedPhotoVersion = resultQueries.findChallengeResultPhotoMetadata(draft.challengeNumber, participantId)
                .map(ChallengeResultQueries.ChallengeResultPhotoMetadata::version)
                .orElse(null);
        draft.expectedPhotoVersionCaptured = true;
    }

    private ParticipantQueries.ParticipantView knownParticipant(String discordUserId) {
        return participantQueries.findParticipantByExternalIdentity(DISCORD_PROVIDER,
                        required(discordUserId, "Discord-Person"))
                .orElseThrow(() -> rejected("Diese Discord-Person ist noch keinem Teilnehmer zugeordnet."));
    }

    private ChallengeResultPhotoUpload selectedPhoto(Draft draft) {
        if (draft.selectedPhoto == Draft.NO_PHOTO) {
            return null;
        }
        if (draft.selectedPhoto == Draft.PHOTO_NOT_SELECTED || draft.selectedPhoto >= draft.photos.size()) {
            throw rejected("Bitte wähle ein Foto oder ausdrücklich `kein Foto`.");
        }
        return download(draft.photos.get(draft.selectedPhoto));
    }

    private static ChallengeResultPhotoUpload download(PhotoSource source) {
        requirePhotoMetadata(source);
        byte[] bytes;
        try {
            bytes = source.download();
        } catch (RuntimeException exception) {
            throw rejected("Das Foto konnte nicht geladen werden. Bitte starte den Upload erneut.");
        }
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_PHOTO_BYTES) {
            throw rejected("Das Foto ist leer oder größer als 10 MiB.");
        }
        return new ChallengeResultPhotoUpload(bytes, source.declaredContentType(), source.originalFilename());
    }

    private static void requirePhotoMetadata(PhotoSource source) {
        if (source == null || !supportedImageMetadata(source)) {
            throw rejected("Es werden nur PNG- und JPEG-Attachments unterstützt.");
        }
        if (source.declaredSize() <= 0 || source.declaredSize() > MAX_PHOTO_BYTES) {
            throw rejected("Das Foto muss vorhanden und höchstens 10 MiB groß sein.");
        }
    }

    private static boolean supportedImageMetadata(PhotoSource source) {
        String type = source.declaredContentType() == null ? ""
                : source.declaredContentType().strip().toLowerCase(Locale.ROOT);
        if ("image/png".equals(type) || "image/jpeg".equals(type)) {
            return true;
        }
        if (!type.isEmpty()) {
            return false;
        }
        String filename = source.originalFilename() == null ? "" : source.originalFilename().toLowerCase(Locale.ROOT);
        return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg");
    }

    private Preparation preparation(String token, Draft draft, ChallengeArchiveQueries.ChallengePage page) {
        Map<Long, ChallengeChoice> choices = new LinkedHashMap<>();
        page.challenges().stream().limit(CHALLENGE_PAGE_SIZE).map(DiscordResultCaptureWorkflow::challengeChoice)
                .forEach(choice -> choices.putIfAbsent(choice.challengeNumber(), choice));
        List<PhotoChoice> photos = new ArrayList<>();
        photos.add(new PhotoChoice(Draft.NO_PHOTO, "Kein Foto", draft.selectedPhoto == Draft.NO_PHOTO));
        for (int index = 0; index < draft.photos.size(); index++) {
            PhotoSource source = draft.photos.get(index);
            String label = source.originalFilename() == null || source.originalFilename().isBlank()
                    ? "Bild " + (index + 1) : source.originalFilename();
            photos.add(new PhotoChoice(index, label, draft.selectedPhoto == index));
        }
        boolean ready = draft.externalSubject != null && draft.challengeNumber != null
                && draft.selectedPhoto != Draft.PHOTO_NOT_SELECTED;
        return new Preparation(token, draft.messageText, draft.messageText.length() > 1500,
                draft.messageText.getBytes(StandardCharsets.UTF_8), draft.personDisplayName, draft.challengeNumber,
                List.copyOf(choices.values()), draft.challengePage, draft.challengePages, List.copyOf(photos), ready,
                draft.messageText.length() > 4000);
    }

    private ChallengeArchiveQueries.ChallengePage challengePage(int page) {
        return archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(page, CHALLENGE_PAGE_SIZE));
    }

    private static ChallengeChoice challengeChoice(PublicChallenge challenge) {
        return new ChallengeChoice(challenge.challengeNumber(), challenge.status(),
                challenge.status() == ChallengeStatus.ACTIVE ? "aktiv" : "abgeschlossen");
    }

    private Draft captureDraft(OperatorContext context, String token) {
        return draft(context, token, Mode.CAPTURE);
    }

    private Draft draft(OperatorContext context, String token, Mode expectedMode) {
        requireOperator(context);
        Draft draft = drafts.get(token, context.guildId(), context.operatorUserId());
        if (draft.mode != expectedMode) {
            throw rejected("Dieser Interaktionsschritt ist nicht mehr gültig. Bitte starte den Ablauf erneut.");
        }
        return draft;
    }

    private void requireOperator(OperatorContext context) {
        if (context == null || context.guildId() != properties.guildId() || !context.operator()
                || context.operatorUserId() == null || context.operatorUserId().isBlank()) {
            throw rejected("Dieser Ablauf ist nur in der konfigurierten Guild und für die Challenge-Operator-Rolle verfügbar.");
        }
    }

    private static ResultData resultData(FormData form) {
        try {
            List<OwnIngredientInput> ingredients = ingredientLines(form.ingredientsPartOne(), form.ingredientsPartTwo())
                    .stream().map(line -> new OwnIngredientInput(line, null)).toList();
            return new ResultData(form.dishName(), form.description(), form.evaluation(), ingredients);
        } catch (IllegalArgumentException exception) {
            throw rejected("Bitte prüfe die Eingaben: Gerichtsname und Beschreibung sind erforderlich; maximal 25 "
                    + "eindeutige Zutaten mit je 200 Zeichen sind erlaubt.");
        }
    }

    private static ResultData resultDataPreservingReferences(FormData form, ChallengeResultView current) {
        ResultData plain = resultData(form);
        Map<String, Long> existingReferences = new LinkedHashMap<>();
        current.ownIngredients().stream().filter(ingredient -> ingredient.ingredientConcept() != null)
                .forEach(ingredient -> existingReferences.put(ingredient.displayText().toLowerCase(Locale.ROOT),
                        ingredient.ingredientConcept().ingredientConceptId()));
        return new ResultData(plain.dishName(), plain.description(), plain.evaluation(), plain.ownIngredients().stream()
                .map(ingredient -> new OwnIngredientInput(ingredient.displayText(),
                        existingReferences.get(ingredient.displayText().toLowerCase(Locale.ROOT))))
                .toList());
    }

    private static List<String> ingredientLines(String first, String second) {
        List<String> lines = new ArrayList<>();
        for (String part : List.of(first == null ? "" : first, second == null ? "" : second)) {
            part.lines().map(String::strip).filter(line -> !line.isEmpty()).forEach(lines::add);
        }
        return List.copyOf(lines);
    }

    private static FormData form(ChallengeResultView result) {
        List<String> ingredientLines = result.ownIngredients().stream()
                .map(ChallengeResultQueries.ResultIngredientView::displayText).toList();
        List<String> parts = splitIngredientPrefill(ingredientLines);
        return new FormData(result.dishName(), result.description(), result.evaluation() == null ? "" : result.evaluation(),
                parts.get(0), parts.get(1));
    }

    private static List<String> splitIngredientPrefill(List<String> lines) {
        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        for (String line : lines) {
            StringBuilder target = first.length() == 0 || first.length() + 1 + line.length() <= 4000 ? first : second;
            if (target.length() > 0) {
                target.append('\n');
            }
            target.append(line);
        }
        return List.of(first.toString(), second.toString());
    }

    private static CatalogChoice choice(IngredientConcept concept, boolean exact) {
        return new CatalogChoice(concept.id(), concept.code(), concept.displayName(), concept.active(), exact);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw rejected(label + " ist erforderlich.");
        }
        return value.strip();
    }

    private static String truncate(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private static Rejected rejected(String message) {
        return new Rejected(message);
    }

    record OperatorContext(long guildId, String operatorUserId, boolean operator) {
    }

    interface PhotoSource {
        long declaredSize();
        String declaredContentType();
        String originalFilename();
        byte[] download();
    }

    record Preparation(String token, String messageText, boolean attachFullText, byte[] fullTextBytes,
                       String selectedPersonName, Long selectedChallengeNumber, List<ChallengeChoice> challenges,
                       int challengePage, int challengePages, List<PhotoChoice> photos, boolean readyForModal,
                       boolean descriptionNeedsCondensing) {
        Preparation {
            fullTextBytes = fullTextBytes.clone();
            challenges = List.copyOf(challenges);
            photos = List.copyOf(photos);
        }

        @Override
        public byte[] fullTextBytes() {
            return fullTextBytes.clone();
        }
    }

    record ChallengeChoice(long challengeNumber, ChallengeStatus status, String statusLabel) {
    }

    record PhotoChoice(int index, String label, boolean selected) {
    }

    record FormData(String dishName, String description, String evaluation, String ingredientsPartOne,
                    String ingredientsPartTwo) {
        FormData {
            dishName = dishName == null ? "" : dishName;
            description = description == null ? "" : description;
            evaluation = evaluation == null ? "" : evaluation;
            ingredientsPartOne = ingredientsPartOne == null ? "" : ingredientsPartOne;
            ingredientsPartTwo = ingredientsPartTwo == null ? "" : ingredientsPartTwo;
        }
    }

    record ResultModal(String token, String title, FormData values, boolean sourceTextWasLonger) {
    }

    sealed interface CaptureSubmission permits ReplaceConfirmation, Saved {
    }

    record ReplaceConfirmation(String token, long challengeNumber, String participantName, String existingDishName)
            implements CaptureSubmission {
    }

    record Saved(String mappingToken, long challengeNumber, String message, boolean mappingAvailable)
            implements CaptureSubmission {
    }

    record RemovalConfirmation(String token, long challengeNumber, String participantName, String dishName) {
    }

    sealed interface MappingProgress permits MappingStep, MappingComplete {
    }

    record MappingStep(String token, long challengeNumber, int ingredientNumber, int ingredientCount,
                       String ingredientText, Long exactSuggestionId, String searchTerm,
                       List<CatalogChoice> choices) implements MappingProgress {
        MappingStep {
            choices = List.copyOf(choices);
        }
    }

    record MappingComplete(long challengeNumber, String message) implements MappingProgress {
    }

    record CatalogChoice(long conceptId, String code, String displayName, boolean active, boolean exact) {
    }

    static final class Rejected extends RuntimeException {
        Rejected(String message) {
            super(message);
        }
    }

    private enum Mode {
        CAPTURE,
        EDIT,
        REMOVE,
        MAPPING
    }

    private static final class Draft {
        static final int PHOTO_NOT_SELECTED = -2;
        static final int NO_PHOTO = -1;

        private String token;
        private final long guildId;
        private final String ownerUserId;
        private Mode mode;
        private String messageText;
        private List<PhotoSource> photos;
        private Long challengeNumber;
        private int selectedPhoto;
        private int challengePage;
        private int challengePages;
        private String externalSubject;
        private String personDisplayName;
        private Long participantId;
        private FormData form;
        private Long expectedResultVersion;
        private Long expectedPhotoVersion;
        private boolean expectedPhotoVersionCaptured;
        private int mappingIndex;
        private String catalogSearch;
        private Long mappingExpectedResultVersion;
        private Long mappingExpectedIngredientId;
        private Instant expiresAt;

        private Draft(long guildId, String ownerUserId, Mode mode) {
            this.guildId = guildId;
            this.ownerUserId = ownerUserId;
            this.mode = mode;
        }

        static Draft capture(OperatorContext context, String messageText, List<PhotoSource> photos,
                             Long selectedChallenge, int selectedPhoto, int challengePage, int challengePages) {
            Draft draft = new Draft(context.guildId(), context.operatorUserId(), Mode.CAPTURE);
            draft.messageText = messageText;
            draft.photos = List.copyOf(photos);
            draft.challengeNumber = selectedChallenge;
            draft.selectedPhoto = selectedPhoto;
            draft.challengePage = challengePage;
            draft.challengePages = challengePages;
            return draft;
        }

        static Draft maintenance(OperatorContext context, Mode mode, long challengeNumber,
                                 ParticipantQueries.ParticipantView participant, FormData form) {
            Draft draft = new Draft(context.guildId(), context.operatorUserId(), mode);
            draft.challengeNumber = challengeNumber;
            draft.participantId = participant.participantId();
            draft.personDisplayName = participant.displayName();
            draft.form = form;
            draft.messageText = "";
            draft.photos = List.of();
            draft.selectedPhoto = NO_PHOTO;
            return draft;
        }
    }

    private static final class DraftStore {
        private final Clock clock;
        private final Duration ttl;
        private final int maximumDrafts;
        private final SecureRandom random = new SecureRandom();
        private final LinkedHashMap<String, Draft> entries = new LinkedHashMap<>();

        private DraftStore(Clock clock, Duration ttl, int maximumDrafts) {
            this.clock = Objects.requireNonNull(clock);
            if (ttl == null || ttl.isZero() || ttl.isNegative() || maximumDrafts < 1) {
                throw new IllegalArgumentException("Draft TTL and capacity must be positive");
            }
            this.ttl = ttl;
            this.maximumDrafts = maximumDrafts;
        }

        synchronized String create(Draft draft) {
            evictExpired();
            while (entries.size() >= maximumDrafts) {
                entries.remove(entries.keySet().iterator().next());
            }
            byte[] bytes = new byte[18];
            String token;
            do {
                random.nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            } while (entries.containsKey(token));
            draft.token = token;
            draft.expiresAt = clock.instant().plus(ttl);
            entries.put(token, draft);
            return token;
        }

        synchronized Draft get(String token, long guildId, String ownerUserId) {
            evictExpired();
            Draft draft = entries.get(token);
            if (draft == null) {
                throw rejected("Dieser kurzlebige Entwurf ist abgelaufen oder durch einen Neustart ungültig. "
                        + "Bitte starte den Ablauf erneut.");
            }
            if (draft.guildId != guildId || !draft.ownerUserId.equals(ownerUserId)) {
                throw rejected("Dieser kurzlebige Entwurf gehört zu einem anderen Operator oder einer anderen Guild.");
            }
            return draft;
        }

        synchronized void remove(String token) {
            entries.remove(token);
        }

        synchronized int size() {
            evictExpired();
            return entries.size();
        }

        private void evictExpired() {
            Instant now = clock.instant();
            entries.values().removeIf(draft -> !draft.expiresAt.isAfter(now));
        }
    }
}
