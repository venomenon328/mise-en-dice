package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardMetadata;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengePage;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RestrictionSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.Specificity;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardAlreadyExistsException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardValidationException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCompletionCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeNotFoundException;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultPhotoBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultPhotoMetadata;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultView;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ParticipantReference;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ResultIngredientView;
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
    void latestUsesTheHighestPublicChallengeAndLoadsOnlyItsCard() {
        ArchiveQueries archive = new ArchiveQueries();
        archive.latest = Optional.of(challenge(12, ChallengeStatus.COMPLETED, true, 0, List.of()));
        archive.cards.put(12L, card(12));
        RecordingDelivery delivery = new RecordingDelivery();

        workflow(archive, new RecordingCards(), new RecordingCompletions(), new ResultQueries())
                .latest(delivery, new RecordingFeedback());

        assertThat(archive.latestReads).isOne();
        assertThat(archive.cardLoads).containsExactly(12L);
        DiscordChallengeArchiveRenderer.RenderedChallenge rendered =
                (DiscordChallengeArchiveRenderer.RenderedChallenge) delivery.response;
        assertThat(rendered.challenge().title()).isEqualTo("Challenge #12");
        assertThat(rendered.challenge().description()).contains("**Status**", "Abgeschlossen", "**Ergebnisse**", "0 Ergebnisse");
        assertThat(rendered.challenge().attachmentFilename()).isEqualTo("challenge-12.png");
        assertThat(rendered.resultFollowUps()).isEmpty();
    }

    @Test
    void activeSupportsAnEmptyPageAndStablePaginationWithoutLoadingAnyBlob() {
        ArchiveQueries archive = new ArchiveQueries();
        RecordingDelivery empty = new RecordingDelivery();

        workflow(archive, new RecordingCards(), new RecordingCompletions(), new ResultQueries())
                .active(1, empty, new RecordingFeedback());

        assertThat(((DiscordChallengeArchiveRenderer.RenderedText) empty.response).content())
                .isEqualTo("Es gibt keine aktiven Challenges.");

        archive.activePage = new ChallengePage(1, 10, 1, 12L, 1,
                List.of(challenge(12, ChallengeStatus.ACTIVE, false, 0, List.of())));
        RecordingDelivery single = new RecordingDelivery();
        workflow(archive, new RecordingCards(), new RecordingCompletions(), new ResultQueries())
                .active(1, single, new RecordingFeedback());
        assertThat(((DiscordChallengeArchiveRenderer.RenderedText) single.response).content())
                .contains("Aktive Challenges · Seite 1/1", "#12 · letzte · Aktiv · 0 Ergebnisse");

        archive.activePage = new ChallengePage(2, 10, 12, 13L, 2,
                List.of(challenge(12, ChallengeStatus.ACTIVE, true, 2, List.of()),
                        challenge(11, ChallengeStatus.ACTIVE, false, 1, List.of())));
        RecordingDelivery populated = new RecordingDelivery();

        workflow(archive, new RecordingCards(), new RecordingCompletions(), new ResultQueries())
                .active(2, populated, new RecordingFeedback());

        assertThat(archive.activeRequests).containsExactly(new ChallengeArchiveQueries.PageRequest(1, 10),
                new ChallengeArchiveQueries.PageRequest(1, 10), new ChallengeArchiveQueries.PageRequest(2, 10));
        assertThat(archive.cardLoads).isEmpty();
        String content = ((DiscordChallengeArchiveRenderer.RenderedText) populated.response).content();
        assertThat(content).contains("Aktive Challenges · Seite 2/2", "#12 · Aktiv · 2 Ergebnisse · 🖼️",
                "#11 · Aktiv · 1 Ergebnis");
    }

    @Test
    void detailRendersStoredResultsAsSeparateFollowUpsAndLoadsOnlyAvailablePhotos() {
        ChallengeResultView withoutEvaluation = result(101, 12, 7, "Georgia", "Tempeh-Teller", "Mit Miso gekocht", null,
                List.of(new ResultIngredientView(10, "Miso", null)), false);
        ChallengeResultView withPhoto = result(102, 12, 8, "Tobias", "Birnen-Curry", "Mit Geduld gekocht", "Würde ich wieder kochen.",
                List.of(), true);
        ArchiveQueries archive = new ArchiveQueries();
        archive.byNumber.put(12L, challenge(12, ChallengeStatus.COMPLETED, false, 2, List.of(withoutEvaluation, withPhoto)));
        ResultQueries results = new ResultQueries();
        results.photos.put(key(12, 8), photo(12, 8, "image/jpeg"));
        RecordingDelivery delivery = new RecordingDelivery();

        workflow(archive, new RecordingCards(), new RecordingCompletions(), results)
                .show(12, delivery, new RecordingFeedback());

        assertThat(results.photoLoads).containsExactly(key(12, 8));
        DiscordChallengeArchiveRenderer.RenderedChallenge rendered =
                (DiscordChallengeArchiveRenderer.RenderedChallenge) delivery.response;
        assertThat(rendered.resultFollowUps()).hasSize(2);
        assertThat(rendered.resultFollowUps().getFirst().title()).contains("Georgia", "Tempeh-Teller");
        assertThat(rendered.resultFollowUps().getFirst().description())
                .contains("Eigene Zutaten", "• Miso", "Gericht / Umsetzung").doesNotContain("Bewertung");
        assertThat(rendered.resultFollowUps().getFirst().hasAttachment()).isFalse();
        assertThat(rendered.resultFollowUps().get(1).description()).contains("Bewertung", "Würde ich wieder kochen");
        assertThat(rendered.resultFollowUps().get(1).attachmentFilename()).isEqualTo("challenge-12-ergebnis-2.jpg");
        assertThat(rendered.resultFollowUps().toString()).doesNotContain("PARTICIPANT_", "101", "102", "vote", "offer");
    }

    @Test
    void completionWithoutNumberRequiresExactlyOneActiveChallenge() {
        ArchiveQueries archive = new ArchiveQueries();
        RecordingCompletions completions = new RecordingCompletions();
        RecordingMutationDelivery noActive = new RecordingMutationDelivery();

        workflow(archive, new RecordingCards(), completions, new ResultQueries()).complete(null, noActive);

        archive.activePage = new ChallengePage(1, 2, 2, 12L, 1,
                List.of(challenge(12, ChallengeStatus.ACTIVE, false, 0, List.of()),
                        challenge(11, ChallengeStatus.ACTIVE, false, 0, List.of())));
        RecordingMutationDelivery multiple = new RecordingMutationDelivery();
        workflow(archive, new RecordingCards(), completions, new ResultQueries()).complete(null, multiple);

        archive.activePage = new ChallengePage(1, 2, 1, 12L, 1,
                List.of(challenge(12, ChallengeStatus.ACTIVE, false, 0, List.of())));
        archive.byNumber.put(12L, challenge(12, ChallengeStatus.COMPLETED, false, 0, List.of()));
        RecordingMutationDelivery one = new RecordingMutationDelivery();
        workflow(archive, new RecordingCards(), completions, new ResultQueries()).complete(null, one);

        assertThat(noActive.rejected).singleElement().satisfies(message -> assertThat(message).contains("keine aktive Challenge"));
        assertThat(multiple.rejected).singleElement().satisfies(message -> assertThat(message).contains("mehrere aktive Challenges"));
        assertThat(completions.commands).containsExactly(new ChallengeCompletionCommands.CompleteChallenge(12));
        assertThat(one.success).containsExactly("Die Challenge wurde abgeschlossen.");
        assertThat(one.published).singleElement().satisfies(challenge ->
                assertThat(challenge.challenge().description()).contains("Abgeschlossen"));
    }

    @Test
    void completedCoreMutationRemainsTruthfulWhenItsPublicDetailFollowUpFails() {
        ArchiveQueries archive = new ArchiveQueries();
        archive.byNumber.put(4L, challenge(4, ChallengeStatus.COMPLETED, false, 0, List.of()));
        RecordingCompletions completions = new RecordingCompletions();
        RecordingMutationDelivery delivery = new RecordingMutationDelivery();
        delivery.failPublicDelivery = true;

        workflow(archive, new RecordingCards(), completions, new ResultQueries()).complete(4L, delivery);

        assertThat(completions.commands).containsExactly(new ChallengeCompletionCommands.CompleteChallenge(4));
        assertThat(delivery.persistedButNotPublished).containsExactly("Die Challenge wurde abgeschlossen.");
        assertThat(delivery.technicalFailures).isEmpty();
    }

    @Test
    void cardWithoutNumberStillTargetsTheLatestChallengeBeforeDownloading() {
        ArchiveQueries archive = new ArchiveQueries();
        archive.latest = Optional.of(challenge(4, ChallengeStatus.COMPLETED, false, 0, List.of()));
        archive.byNumber.put(4L, challenge(4, ChallengeStatus.COMPLETED, true, 0, List.of()));
        archive.cards.put(4L, card(4));
        RecordingCards cards = new RecordingCards();
        RecordingMutationDelivery delivery = new RecordingMutationDelivery();

        workflow(archive, cards, new RecordingCompletions(), new ResultQueries())
                .setCard(null, false, new TestUpload(), delivery);

        assertThat(cards.setCommands).singleElement().satisfies(command -> assertThat(command.challengeNumber()).isEqualTo(4));
        assertThat(delivery.success).containsExactly("Die Card wurde gespeichert.");
    }

    @Test
    void cardUploadMetadataIsRejectedBeforeDownloading() {
        ArchiveQueries archive = new ArchiveQueries();
        archive.latest = Optional.of(challenge(4, ChallengeStatus.ACTIVE, false, 0, List.of()));
        RecordingCards cards = new RecordingCards();
        TestUpload oversized = new TestUpload();
        oversized.declaredSize = DiscordChallengeArchiveWorkflow.MAX_CARD_BYTES + 1L;
        RecordingMutationDelivery oversizedDelivery = new RecordingMutationDelivery();
        TestUpload wrongType = new TestUpload();
        wrongType.contentType = "image/jpeg";
        RecordingMutationDelivery wrongTypeDelivery = new RecordingMutationDelivery();

        workflow(archive, cards, new RecordingCompletions(), new ResultQueries())
                .setCard(null, false, oversized, oversizedDelivery);
        workflow(archive, cards, new RecordingCompletions(), new ResultQueries())
                .setCard(null, false, wrongType, wrongTypeDelivery);

        assertThat(oversized.downloads).isZero();
        assertThat(wrongType.downloads).isZero();
        assertThat(cards.setCommands).isEmpty();
        assertThat(oversizedDelivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("5 MiB"));
        assertThat(wrongTypeDelivery.rejected).singleElement().satisfies(message -> assertThat(message).contains("PNG"));
    }

    @Test
    void cardFailuresAreTypedWithoutClaimingTheMutationSucceeded() {
        ArchiveQueries archive = new ArchiveQueries();
        RecordingCards cards = new RecordingCards();
        RecordingMutationDelivery existing = new RecordingMutationDelivery();
        cards.setFailure = new ChallengeCardAlreadyExistsException(4);

        workflow(archive, cards, new RecordingCompletions(), new ResultQueries())
                .setCard(4L, false, new TestUpload(), existing);

        cards.removeFailure = new ChallengeCardNotFoundException(4);
        RecordingMutationDelivery missing = new RecordingMutationDelivery();
        workflow(archive, cards, new RecordingCompletions(), new ResultQueries()).removeCard(4, missing);

        assertThat(existing.rejected).singleElement().satisfies(message -> assertThat(message).contains("ersetzen:true"));
        assertThat(missing.rejected).singleElement().satisfies(message -> assertThat(message).contains("keine Card"));
        assertThat(existing.success).isEmpty();
        assertThat(missing.success).isEmpty();
    }

    @Test
    void invalidAndMissingCardsDoNotExposeCoreValidationDetails() {
        ArchiveQueries archive = new ArchiveQueries();
        RecordingCards cards = new RecordingCards();
        cards.setFailure = new ChallengeNotFoundException(8);
        RecordingMutationDelivery missing = new RecordingMutationDelivery();

        workflow(archive, cards, new RecordingCompletions(), new ResultQueries())
                .setCard(8L, false, new TestUpload(), missing);

        cards.setFailure = new ChallengeCardValidationException("internal validation detail");
        RecordingMutationDelivery invalid = new RecordingMutationDelivery();
        workflow(archive, cards, new RecordingCompletions(), new ResultQueries())
                .setCard(8L, false, new TestUpload(), invalid);

        assertThat(missing.rejected).containsExactly("Challenge #8 wurde nicht gefunden.");
        assertThat(invalid.rejected).singleElement().satisfies(message -> assertThat(message)
                .contains("gültige PNG-Card").doesNotContain("internal"));
    }

    private static DiscordChallengeArchiveWorkflow workflow(ArchiveQueries archive, RecordingCards cards,
                                                            RecordingCompletions completions, ResultQueries results) {
        return new DiscordChallengeArchiveWorkflow(new DiscordProperties(true, "token", 99, 77777,
                ZoneId.of("Europe/Berlin"), Map.of()), archive, cards, completions, results,
                new DiscordChallengeArchiveRenderer(ZoneId.of("Europe/Berlin")));
    }

    private static PublicChallenge challenge(long number, ChallengeStatus status, boolean cardAvailable, long resultCount,
                                             List<ChallengeResultView> results) {
        Instant completedAt = status == ChallengeStatus.COMPLETED ? Instant.parse("2026-08-22T12:15:30Z") : null;
        return new PublicChallenge(number, Instant.parse("2026-08-21T10:15:30Z"), List.of(
                new RequirementSnapshot(1, "Tempeh", Specificity.SPECIFIC),
                new RequirementSnapshot(2, "Mayonnaise", Specificity.SPECIFIC),
                new RequirementSnapshot(3, "Kohlgemüse", Specificity.OPEN),
                new RequirementSnapshot(4, "Essig", Specificity.OPEN)), RestrictionSnapshot.none(), cardAvailable,
                status, completedAt, resultCount, results);
    }

    private static ChallengeResultView result(long resultId, long challengeNumber, long participantId, String person,
                                              String dish, String description, String evaluation,
                                              List<ResultIngredientView> ingredients, boolean photoAvailable) {
        return new ChallengeResultView(resultId, challengeNumber,
                new ParticipantReference(participantId, "PARTICIPANT_" + participantId, person, true), dish, description,
                evaluation, ingredients, photoAvailable, 0, Instant.parse("2026-08-21T10:15:30Z"),
                Instant.parse("2026-08-21T10:15:30Z"));
    }

    private static ChallengeCardBinary card(long challengeNumber) {
        return new ChallengeCardBinary(new ChallengeCardMetadata(challengeNumber, "image/png", "original.png", 3,
                "a".repeat(64), Instant.parse("2026-08-21T10:00:00Z"), Instant.parse("2026-08-21T10:01:00Z")),
                new byte[] {1, 2, 3});
    }

    private static ChallengeResultPhotoBinary photo(long challengeNumber, long participantId, String contentType) {
        return new ChallengeResultPhotoBinary(new ChallengeResultPhotoMetadata(challengeNumber, participantId, contentType,
                "result.jpg", 3, 1, 1, "b".repeat(64), 0, Instant.parse("2026-08-21T10:00:00Z"),
                Instant.parse("2026-08-21T10:01:00Z")), new byte[] {4, 5, 6});
    }

    private static String key(long challengeNumber, long participantId) {
        return challengeNumber + ":" + participantId;
    }

    private static final class ArchiveQueries implements ChallengeArchiveQueries {
        private Optional<PublicChallenge> latest = Optional.empty();
        private final Map<Long, PublicChallenge> byNumber = new HashMap<>();
        private final Map<Long, ChallengeCardBinary> cards = new HashMap<>();
        private final List<Long> cardLoads = new ArrayList<>();
        private final List<PageRequest> activeRequests = new ArrayList<>();
        private ChallengePage activePage = new ChallengePage(1, 10, 0, null, 0, List.of());
        private int latestReads;

        @Override public Optional<PublicChallenge> findCurrentChallenge() { return latest; }
        @Override public Optional<PublicChallenge> findLatestChallenge() { latestReads++; return latest; }
        @Override public Optional<PublicChallenge> findChallengeByNumber(long challengeNumber) {
            return Optional.ofNullable(byNumber.get(challengeNumber));
        }
        @Override public ChallengePage listChallenges(PageRequest request) {
            return new ChallengePage(request.page(), request.pageSize(), 0, null, 0, List.of());
        }
        @Override public ChallengePage listActiveChallenges(PageRequest request) {
            activeRequests.add(request);
            return activePage;
        }
        @Override public Optional<ChallengeCardMetadata> findChallengeCardMetadata(long challengeNumber) {
            return Optional.ofNullable(cards.get(challengeNumber)).map(ChallengeCardBinary::metadata);
        }
        @Override public Optional<ChallengeCardBinary> loadChallengeCard(long challengeNumber) {
            cardLoads.add(challengeNumber);
            return Optional.ofNullable(cards.get(challengeNumber));
        }
    }

    private static final class ResultQueries implements ChallengeResultQueries {
        private final Map<String, ChallengeResultPhotoBinary> photos = new HashMap<>();
        private final List<String> photoLoads = new ArrayList<>();

        @Override public Optional<ChallengeResultView> findChallengeResult(long challengeNumber, long participantId) { return Optional.empty(); }
        @Override public List<ChallengeResultView> listChallengeResults(long challengeNumber) { return List.of(); }
        @Override public Optional<ChallengeResultPhotoMetadata> findChallengeResultPhotoMetadata(long challengeNumber, long participantId) {
            return Optional.ofNullable(photos.get(key(challengeNumber, participantId))).map(ChallengeResultPhotoBinary::metadata);
        }
        @Override public Optional<ChallengeResultPhotoBinary> loadChallengeResultPhoto(long challengeNumber, long participantId) {
            String key = key(challengeNumber, participantId);
            photoLoads.add(key);
            return Optional.ofNullable(photos.get(key));
        }
    }

    private static final class RecordingCompletions implements ChallengeCompletionCommands {
        private final List<CompleteChallenge> commands = new ArrayList<>();
        @Override public Completion completeChallenge(CompleteChallenge command) {
            commands.add(command);
            return new Completion(command.challengeNumber(), ChallengeStatus.COMPLETED, Instant.parse("2026-08-22T12:15:30Z"));
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
        private long declaredSize = 3;
        private String contentType = "image/png";
        private int downloads;
        @Override public long declaredSize() { return declaredSize; }
        @Override public String declaredContentType() { return contentType; }
        @Override public String originalFilename() { return "card.png"; }
        @Override public byte[] download() { downloads++; return new byte[] {1, 2, 3}; }
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
        @Override public void rejected(String message) { throw new AssertionError(message); }
        @Override public void technicalFailure(Throwable exception) { throw new AssertionError(exception); }
    }

    private static final class RecordingMutationDelivery implements DiscordChallengeArchiveWorkflow.MutationDelivery {
        private final List<String> rejected = new ArrayList<>();
        private final List<DiscordChallengeArchiveRenderer.RenderedChallenge> published = new ArrayList<>();
        private final List<String> success = new ArrayList<>();
        private final List<String> persistedButNotPublished = new ArrayList<>();
        private final List<Throwable> technicalFailures = new ArrayList<>();
        private boolean failPublicDelivery;

        @Override public void rejected(String message) { rejected.add(message); }
        @Override public void technicalFailure(Throwable exception) { technicalFailures.add(exception); }
        @Override public void publish(DiscordChallengeArchiveRenderer.RenderedChallenge challenge, Runnable delivered,
                                      java.util.function.Consumer<Throwable> failed) {
            published.add(challenge);
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
