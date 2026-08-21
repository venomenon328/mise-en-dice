package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchivePageOutOfRangeException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import java.util.Optional;
import java.util.function.Consumer;

/** Thin Discord-facing workflow over the public challenge archive and Card APIs. */
final class DiscordChallengeArchiveWorkflow {
    static final int PAGE_SIZE = 10;
    static final int MAX_CARD_BYTES = 5 * 1024 * 1024;

    private final DiscordProperties properties;
    private final ChallengeArchiveQueries archiveQueries;
    private final ChallengeCardCommands cardCommands;
    private final DiscordChallengeArchiveRenderer renderer;

    DiscordChallengeArchiveWorkflow(DiscordProperties properties, ChallengeArchiveQueries archiveQueries,
                                    ChallengeCardCommands cardCommands, DiscordChallengeArchiveRenderer renderer) {
        this.properties = properties;
        this.archiveQueries = archiveQueries;
        this.cardCommands = cardCommands;
        this.renderer = renderer;
    }

    boolean acceptsGuild(long guildId) {
        return guildId == properties.guildId();
    }

    void current(Delivery delivery, Feedback feedback) {
        try {
            archiveQueries.findCurrentChallenge().ifPresentOrElse(
                    challenge -> detail(challenge, delivery, feedback),
                    () -> delivery.replace(renderer.noCurrentChallenge(), () -> { }, feedback::technicalFailure));
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void show(long challengeNumber, Delivery delivery, Feedback feedback) {
        try {
            archiveQueries.findChallengeByNumber(challengeNumber).ifPresentOrElse(
                    challenge -> detail(challenge, delivery, feedback),
                    () -> delivery.replace(renderer.unknownChallenge(challengeNumber), () -> { }, feedback::technicalFailure));
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void list(int page, Delivery delivery, Feedback feedback) {
        try {
            delivery.replace(renderer.list(archiveQueries.listChallenges(new ChallengeArchiveQueries.PageRequest(page, PAGE_SIZE))),
                    () -> { }, feedback::technicalFailure);
        } catch (ChallengeArchivePageOutOfRangeException | IllegalArgumentException exception) {
            feedback.rejected("Diese Archivseite gibt es nicht. Bitte wähle eine gültige Seitennummer.");
        } catch (RuntimeException exception) {
            feedback.technicalFailure(exception);
        }
    }

    void setCard(Long requestedChallengeNumber, boolean replaceExisting, CardUploadSource uploadSource,
                 MutationDelivery delivery) {
        if (!validateUploadMetadata(uploadSource, delivery)) {
            return;
        }
        long challengeNumber;
        try {
            challengeNumber = requestedChallengeNumber == null
                    ? archiveQueries.findCurrentChallenge().map(PublicChallenge::challengeNumber)
                    .orElseThrow(NoCurrentChallengeException::new)
                    : requestedChallengeNumber;
        } catch (NoCurrentChallengeException exception) {
            delivery.rejected("Es gibt keine aktuelle Challenge, der eine Card zugeordnet werden kann.");
            return;
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
            return;
        }

        byte[] contentBytes;
        try {
            contentBytes = uploadSource.download();
        } catch (RuntimeException exception) {
            delivery.rejected("Das Bild konnte nicht geladen werden. Bitte versuche den Upload erneut.");
            return;
        }
        try {
            cardCommands.setChallengeCard(new ChallengeCardCommands.SetChallengeCard(challengeNumber,
                    new ChallengeCardCommands.ChallengeCardUpload(contentBytes, uploadSource.declaredContentType(),
                            uploadSource.originalFilename()), replaceExisting));
        } catch (ChallengeCardAlreadyExistsException exception) {
            delivery.rejected("Für diese Challenge existiert bereits eine Card. Bitte nutze `ersetzen:true`.");
            return;
        } catch (ChallengeCardValidationException exception) {
            delivery.rejected("Das Bild ist keine gültige PNG-Card mit 1200 × 1200 Pixeln und maximal 5 MiB.");
            return;
        } catch (ChallengeNotFoundException exception) {
            delivery.rejected("Challenge #" + challengeNumber + " wurde nicht gefunden.");
            return;
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
            return;
        }
        publishPersistedDetail(challengeNumber, "Die Card wurde gespeichert.", delivery);
    }

    void removeCard(long challengeNumber, MutationDelivery delivery) {
        try {
            cardCommands.removeChallengeCard(new ChallengeCardCommands.RemoveChallengeCard(challengeNumber));
        } catch (ChallengeCardNotFoundException exception) {
            delivery.rejected("Für diese Challenge ist keine Card gespeichert.");
            return;
        } catch (ChallengeNotFoundException exception) {
            delivery.rejected("Challenge #" + challengeNumber + " wurde nicht gefunden.");
            return;
        } catch (RuntimeException exception) {
            delivery.technicalFailure(exception);
            return;
        }
        publishPersistedDetail(challengeNumber, "Die Card wurde entfernt.", delivery);
    }

    private boolean validateUploadMetadata(CardUploadSource uploadSource, MutationDelivery delivery) {
        if (uploadSource.declaredSize() > MAX_CARD_BYTES) {
            delivery.rejected("Das Bild darf höchstens 5 MiB groß sein.");
            return false;
        }
        String contentType = uploadSource.declaredContentType();
        if (contentType != null && !contentType.isBlank() && !"image/png".equalsIgnoreCase(contentType.strip())) {
            delivery.rejected("Das Bild muss als PNG hochgeladen werden.");
            return false;
        }
        return true;
    }

    private void detail(PublicChallenge challenge, Delivery delivery, Feedback feedback) {
        Optional<ChallengeCardBinary> card = challenge.cardAvailable()
                ? archiveQueries.loadChallengeCard(challenge.challengeNumber())
                : Optional.empty();
        delivery.replace(renderer.detail(challenge, card), () -> { }, feedback::technicalFailure);
    }

    private void publishPersistedDetail(long challengeNumber, String successMessage, MutationDelivery delivery) {
        try {
            PublicChallenge challenge = archiveQueries.findChallengeByNumber(challengeNumber)
                    .orElseThrow(() -> new IllegalStateException("Persisted challenge could not be read"));
            Optional<ChallengeCardBinary> card = challenge.cardAvailable()
                    ? archiveQueries.loadChallengeCard(challengeNumber)
                    : Optional.empty();
            delivery.publish(renderer.detail(challenge, card),
                    () -> delivery.persistedAndPublished(successMessage),
                    failure -> delivery.persistedButNotPublished(successMessage));
        } catch (RuntimeException exception) {
            delivery.persistedButNotPublished(successMessage);
        }
    }

    interface Delivery {
        void replace(DiscordChallengeArchiveRenderer.RenderedResponse response, Runnable delivered,
                     Consumer<Throwable> failed);
    }

    interface Feedback {
        void rejected(String message);
        void technicalFailure(Throwable exception);
    }

    interface MutationDelivery {
        void rejected(String message);
        void technicalFailure(Throwable exception);
        void publish(DiscordChallengeArchiveRenderer.RenderedDetail detail, Runnable delivered,
                     Consumer<Throwable> failed);
        void persistedAndPublished(String message);
        void persistedButNotPublished(String message);
    }

    interface CardUploadSource {
        long declaredSize();
        String declaredContentType();
        String originalFilename();
        byte[] download();
    }

    private static final class NoCurrentChallengeException extends RuntimeException {
    }
}
