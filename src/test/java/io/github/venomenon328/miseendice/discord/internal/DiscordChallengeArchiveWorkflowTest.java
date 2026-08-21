package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardMetadata;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengePage;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RestrictionSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.Specificity;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscordChallengeArchiveWorkflowTest {

    @Test
    void guildReadsNeedNoParticipantMappingAndLoadCardBytesOnlyForDetail() {
        ArchiveQueries queries = new ArchiveQueries();
        queries.current = Optional.of(challenge(12, true));
        queries.cards.put(12L, card(12));
        RecordingDelivery delivery = new RecordingDelivery();

        workflow(queries, new RecordingCards()).current(delivery, new RecordingFeedback());

        assertThat(workflow(queries, new RecordingCards()).acceptsGuild(99)).isTrue();
        assertThat(workflow(queries, new RecordingCards()).acceptsGuild(98)).isFalse();
        assertThat(queries.cardLoads).containsExactly(12L);
        DiscordChallengeArchiveRenderer.RenderedDetail rendered =
                (DiscordChallengeArchiveRenderer.RenderedDetail) delivery.response;
        assertThat(rendered.title()).isEqualTo("Challenge #12");
        assertThat(rendered.description()).contains("Bestätigt am 21. August 2026", "1. Tempeh",
                "3. Kohlgemüse (offener Begriff)", "Keine");
        assertThat(rendered.attachmentFilename()).isEqualTo("challenge-12.png");
    }

    @Test
    void listUsesTenEntriesDoesNotLoadBlobsAndMarksCurrentOpenAndCard() {
        ArchiveQueries queries = new ArchiveQueries();
        List<PublicChallenge> challenges = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(number -> challenge(number, number == 10)).toList();
        queries.page = new ChallengePage(1, 10, 12, 10L, 2, challenges);
        RecordingDelivery delivery = new RecordingDelivery();

        workflow(queries, new RecordingCards()).list(1, delivery, new RecordingFeedback());

        assertThat(queries.requests).containsExactly(new ChallengeArchiveQueries.PageRequest(1, 10));
        assertThat(queries.cardLoads).isEmpty();
        String content = ((DiscordChallengeArchiveRenderer.RenderedText) delivery.response).content();
        assertThat(content).contains("Seite 1/2", "#10 · aktuell · 🖼️", "Kohlgemüse (offen)");
    }

    @Test
    void fixesTheDefaultTargetBeforeDownloadEvenWhenCurrentChangesDuringTheDownload() {
        ArchiveQueries queries = new ArchiveQueries();
        queries.current = Optional.of(challenge(4, false));
        queries.byNumber.put(4L, challenge(4, true));
        queries.cards.put(4L, card(4));
        RecordingCards cards = new RecordingCards();
        RecordingMutationDelivery delivery = new RecordingMutationDelivery();
        TestUpload upload = new TestUpload(() -> queries.current = Optional.of(challenge(5, false)));

        workflow(queries, cards).setCard(null, false, upload, delivery);

        assertThat(upload.downloads).isOne();
        assertThat(cards.setCommands).singleElement().satisfies(command -> {
            assertThat(command.challengeNumber()).isEqualTo(4);
            assertThat(command.replaceExisting()).isFalse();
        });
        assertThat(delivery.published).singleElement().satisfies(detail -> {
            assertThat(detail.title()).isEqualTo("Challenge #4");
            assertThat(detail.attachmentFilename()).isEqualTo("challenge-4.png");
        });
        assertThat(delivery.success).containsExactly("Die Card wurde gespeichert.");
    }

    @Test
    void defaultTargetWithoutCurrentChallengeNeverDownloadsOrMutates() {
        ArchiveQueries queries = new ArchiveQueries();
        TestUpload upload = new TestUpload(() -> { });
        RecordingCards cards = new RecordingCards();
        RecordingMutationDelivery delivery = new RecordingMutationDelivery();

        workflow(queries, cards).setCard(null, false, upload, delivery);

        assertThat(upload.downloads).isZero();
        assertThat(cards.setCommands).isEmpty();
        assertThat(delivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("keine aktuelle Challenge"));
    }

    @Test
    void rejectsOversizeAndWrongDeclaredTypeBeforeDownloading() {
        ArchiveQueries queries = new ArchiveQueries();
        queries.current = Optional.of(challenge(4, false));
        RecordingCards cards = new RecordingCards();
        TestUpload oversized = new TestUpload(() -> { });
        oversized.declaredSize = DiscordChallengeArchiveWorkflow.MAX_CARD_BYTES + 1L;
        RecordingMutationDelivery oversizedDelivery = new RecordingMutationDelivery();

        workflow(queries, cards).setCard(null, false, oversized, oversizedDelivery);

        TestUpload wrongType = new TestUpload(() -> { });
        wrongType.contentType = "image/jpeg";
        RecordingMutationDelivery typeDelivery = new RecordingMutationDelivery();
        workflow(queries, cards).setCard(null, false, wrongType, typeDelivery);

        assertThat(oversized.downloads).isZero();
        assertThat(wrongType.downloads).isZero();
        assertThat(cards.setCommands).isEmpty();
        assertThat(oversizedDelivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("5 MiB"));
        assertThat(typeDelivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("PNG"));
    }

    @Test
    void mapsTypedCardFailuresWithoutClaimingAMutation() {
        ArchiveQueries queries = new ArchiveQueries();
        queries.byNumber.put(4L, challenge(4, false));
        RecordingCards cards = new RecordingCards();
        cards.setFailure = new ChallengeCardAlreadyExistsException(4);
        RecordingMutationDelivery setDelivery = new RecordingMutationDelivery();

        workflow(queries, cards).setCard(4L, false, new TestUpload(() -> { }), setDelivery);

        cards.removeFailure = new ChallengeCardNotFoundException(4);
        RecordingMutationDelivery removeDelivery = new RecordingMutationDelivery();
        workflow(queries, cards).removeCard(4, removeDelivery);

        assertThat(setDelivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("ersetzen:true"));
        assertThat(removeDelivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("keine Card"));
        assertThat(setDelivery.success).isEmpty();
        assertThat(removeDelivery.success).isEmpty();
    }

    @Test
    void publicFollowUpFailureAfterPersistenceKeepsTheEphemeralSuccessTruthful() {
        ArchiveQueries queries = new ArchiveQueries();
        queries.byNumber.put(4L, challenge(4, true));
        queries.cards.put(4L, card(4));
        RecordingMutationDelivery delivery = new RecordingMutationDelivery();
        delivery.failPublicDelivery = true;

        workflow(queries, new RecordingCards()).removeCard(4, delivery);

        assertThat(delivery.persistedButNotPublished).containsExactly("Die Card wurde entfernt.");
        assertThat(delivery.technicalFailures).isEmpty();
        assertThat(delivery.success).isEmpty();
    }

    @Test
    void mapsUnknownChallengeAndInvalidCardWithoutLeakingCoreMessages() {
        ArchiveQueries queries = new ArchiveQueries();
        RecordingCards cards = new RecordingCards();
        cards.setFailure = new ChallengeNotFoundException(8);
        RecordingMutationDelivery missing = new RecordingMutationDelivery();
        workflow(queries, cards).setCard(8L, false, new TestUpload(() -> { }), missing);

        cards.setFailure = new ChallengeCardValidationException("internal validation detail");
        RecordingMutationDelivery invalid = new RecordingMutationDelivery();
        workflow(queries, cards).setCard(8L, false, new TestUpload(() -> { }), invalid);

        assertThat(missing.rejected).singleElement().isEqualTo("Challenge #8 wurde nicht gefunden.");
        assertThat(invalid.rejected).singleElement().satisfies(message -> assertThat(message)
                .contains("gültige PNG-Card").doesNotContain("internal"));
    }

    private static DiscordChallengeArchiveWorkflow workflow(ArchiveQueries queries, RecordingCards cards) {
        return new DiscordChallengeArchiveWorkflow(new DiscordProperties(true, "token", 99, 77777,
                ZoneId.of("Europe/Berlin"), Map.of()), queries, cards,
                new DiscordChallengeArchiveRenderer(ZoneId.of("Europe/Berlin")));
    }

    private static PublicChallenge challenge(long number, boolean cardAvailable) {
        return new PublicChallenge(number, Instant.parse("2026-08-21T10:15:30Z"), List.of(
                new RequirementSnapshot(1, "Tempeh", Specificity.SPECIFIC),
                new RequirementSnapshot(2, "Mayonnaise", Specificity.SPECIFIC),
                new RequirementSnapshot(3, "Kohlgemüse", Specificity.OPEN),
                new RequirementSnapshot(4, "Essig", Specificity.OPEN)), RestrictionSnapshot.none(), cardAvailable);
    }

    private static ChallengeCardBinary card(long challengeNumber) {
        return new ChallengeCardBinary(new ChallengeCardMetadata(challengeNumber, "image/png", "original.png", 3,
                "a".repeat(64), Instant.parse("2026-08-21T10:00:00Z"), Instant.parse("2026-08-21T10:01:00Z")),
                new byte[] {1, 2, 3});
    }

    private static final class ArchiveQueries implements ChallengeArchiveQueries {
        private Optional<PublicChallenge> current = Optional.empty();
        private final Map<Long, PublicChallenge> byNumber = new HashMap<>();
        private final Map<Long, ChallengeCardBinary> cards = new HashMap<>();
        private final List<PageRequest> requests = new ArrayList<>();
        private final List<Long> cardLoads = new ArrayList<>();
        private ChallengePage page = new ChallengePage(1, 10, 0, null, 0, List.of());

        @Override public Optional<PublicChallenge> findCurrentChallenge() { return current; }
        @Override public Optional<PublicChallenge> findChallengeByNumber(long challengeNumber) {
            return Optional.ofNullable(byNumber.get(challengeNumber));
        }
        @Override public ChallengePage listChallenges(PageRequest request) {
            requests.add(request);
            return page;
        }
        @Override public Optional<ChallengeCardMetadata> findChallengeCardMetadata(long challengeNumber) {
            return Optional.ofNullable(cards.get(challengeNumber)).map(ChallengeCardBinary::metadata);
        }
        @Override public Optional<ChallengeCardBinary> loadChallengeCard(long challengeNumber) {
            cardLoads.add(challengeNumber);
            return Optional.ofNullable(cards.get(challengeNumber));
        }
    }

    private static final class RecordingCards implements ChallengeCardCommands {
        private final List<SetChallengeCard> setCommands = new ArrayList<>();
        private RuntimeException setFailure;
        private RuntimeException removeFailure;

        @Override public ChallengeCardMetadata setChallengeCard(SetChallengeCard command) {
            setCommands.add(command);
            if (setFailure != null) {
                throw setFailure;
            }
            return new ChallengeCardMetadata(command.challengeNumber(), "image/png", command.upload().originalFilename(),
                    command.upload().contentBytes().length, "a".repeat(64), Instant.now(), Instant.now());
        }
        @Override public void removeChallengeCard(RemoveChallengeCard command) {
            if (removeFailure != null) {
                throw removeFailure;
            }
        }
    }

    private static final class TestUpload implements DiscordChallengeArchiveWorkflow.CardUploadSource {
        private final Runnable afterDownload;
        private int downloads;
        private long declaredSize = 3;
        private String contentType = "image/png";

        private TestUpload(Runnable afterDownload) {
            this.afterDownload = afterDownload;
        }

        @Override public long declaredSize() { return declaredSize; }
        @Override public String declaredContentType() { return contentType; }
        @Override public String originalFilename() { return "card.png"; }
        @Override public byte[] download() {
            downloads++;
            afterDownload.run();
            return new byte[] {1, 2, 3};
        }
    }

    private static final class RecordingDelivery implements DiscordChallengeArchiveWorkflow.Delivery {
        private DiscordChallengeArchiveRenderer.RenderedResponse response;
        @Override public void replace(DiscordChallengeArchiveRenderer.RenderedResponse response, Runnable delivered,
                                      java.util.function.Consumer<Throwable> failed) {
            this.response = response;
            delivered.run();
        }
    }

    private static final class RecordingFeedback implements DiscordChallengeArchiveWorkflow.Feedback {
        private final List<String> rejected = new ArrayList<>();
        @Override public void rejected(String message) { rejected.add(message); }
        @Override public void technicalFailure(Throwable exception) { throw new AssertionError(exception); }
    }

    private static final class RecordingMutationDelivery implements DiscordChallengeArchiveWorkflow.MutationDelivery {
        private final List<String> rejected = new ArrayList<>();
        private final List<DiscordChallengeArchiveRenderer.RenderedDetail> published = new ArrayList<>();
        private final List<String> success = new ArrayList<>();
        private final List<String> persistedButNotPublished = new ArrayList<>();
        private final List<Throwable> technicalFailures = new ArrayList<>();
        private boolean failPublicDelivery;

        @Override public void rejected(String message) { rejected.add(message); }
        @Override public void technicalFailure(Throwable exception) { technicalFailures.add(exception); }
        @Override public void publish(DiscordChallengeArchiveRenderer.RenderedDetail detail, Runnable delivered,
                                      java.util.function.Consumer<Throwable> failed) {
            published.add(detail);
            if (failPublicDelivery) {
                failed.accept(new IllegalStateException("Discord unavailable"));
            } else {
                delivered.run();
            }
        }
        @Override public void persistedAndPublished(String message) { success.add(message); }
        @Override public void persistedButNotPublished(String message) { persistedButNotPublished.add(message); }
    }
}
