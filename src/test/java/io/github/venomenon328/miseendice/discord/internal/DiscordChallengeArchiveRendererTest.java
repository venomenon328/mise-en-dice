package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardBinary;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeCardMetadata;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengePage;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RestrictionSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.Specificity;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultView;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ParticipantReference;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ResultIngredientView;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscordChallengeArchiveRendererTest {

    private final DiscordChallengeArchiveRenderer renderer = new DiscordChallengeArchiveRenderer(ZoneId.of("Europe/Berlin"));

    @Test
    void rendersOnlyHistoricalPublicFactsWithEscapedTextAndCardAttachment() {
        PublicChallenge challenge = challenge(12, true, List.of(
                new RequirementSnapshot(1, "@everyone *Tempeh*", Specificity.SPECIFIC),
                new RequirementSnapshot(2, "Mayonnaise", Specificity.SPECIFIC),
                new RequirementSnapshot(3, "Kohlgemüse", Specificity.OPEN),
                new RequirementSnapshot(4, "Essig", Specificity.OPEN)), RestrictionSnapshot.present("Keine `Fischsauce`"));

        DiscordChallengeArchiveRenderer.RenderedDetail detail = renderer.detail(challenge, Optional.of(card(12)), Map.of()).challenge();

        assertThat(detail.title()).isEqualTo("Challenge #12");
        assertThat(detail.description()).contains("Bestätigt am 21. August 2026", "1. @\u200beveryone \\*Tempeh\\*",
                "Kohlgemüse (offener Begriff)", "Keine \\`Fischsauce\\`");
        assertThat(detail.description()).doesNotContain("session", "vote", "offer", "provider");
        assertThat(detail.attachmentFilename()).isEqualTo("challenge-12.png");

        var message = DiscordJdaListener.archiveCreateMessage(detail);
        assertThat(message.getAllowedMentions()).isEmpty();
        assertThat(message.getEmbeds()).singleElement().satisfies(embed -> {
            assertThat(embed.getTitle()).isEqualTo("Challenge #12");
            assertThat(embed.getImage()).isNotNull();
            assertThat(embed.getImage().getUrl()).isEqualTo("attachment://challenge-12.png");
        });
        assertThat(message.getAttachments()).singleElement().satisfies(attachment ->
                assertThat(((net.dv8tion.jda.api.utils.FileUpload) attachment).getName()).isEqualTo("challenge-12.png"));
    }

    @Test
    void listsTenCompletedChallengesWithinDiscordMessageLimitWithoutDroppingEntries() {
        List<PublicChallenge> challenges = java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(number -> challenge(number, number == 10, List.of(
                        new RequirementSnapshot(1, "Sehr lange Vorgabe ".repeat(8) + number, Specificity.SPECIFIC),
                        new RequirementSnapshot(2, "Noch eine sehr lange Vorgabe ".repeat(8), Specificity.SPECIFIC),
                        new RequirementSnapshot(3, "Offener Begriff mit langem Namen ".repeat(8), Specificity.OPEN),
                        new RequirementSnapshot(4, "Säure mit langem Namen ".repeat(8), Specificity.OPEN)),
                        RestrictionSnapshot.none(), ChallengeStatus.COMPLETED, 12, List.of())).toList();

        String list = renderer.list(new ChallengePage(1, 10, 12, 10L, 2, challenges)).content();

        assertThat(list).contains("Seite 1/2", "#10 · letzte · Abgeschlossen · 12 Ergebnisse · 22.08.2026 14:15 · 🖼️", "…");
        for (int number = 1; number <= 10; number++) {
            assertThat(list).contains("#" + number);
        }
        assertThat(list.length()).isLessThanOrEqualTo(2_000);
    }

    @Test
    void longResultKeepsEveryRequiredSectionVisibleWithinEmbedLimit() {
        List<ResultIngredientView> ingredients = java.util.stream.IntStream.range(0, 25)
                .mapToObj(index -> new ResultIngredientView(index + 1L,
                        "Zutat " + index + " " + "x".repeat(190), null))
                .toList();
        ChallengeResultView result = new ChallengeResultView(101, 12,
                new ParticipantReference(7, "PARTICIPANT_7", "Georgia", true),
                "Sehr langes Gericht", "B".repeat(4_000), "E".repeat(4_000), ingredients, false, 0,
                Instant.parse("2026-08-22T12:15:30Z"), Instant.parse("2026-08-22T12:15:30Z"));
        PublicChallenge challenge = challenge(12, false, standardRequirements(), RestrictionSnapshot.none(),
                ChallengeStatus.ACTIVE, 1, List.of(result));

        DiscordChallengeArchiveRenderer.RenderedDetail followUp = renderer.detail(challenge, Optional.empty(), Map.of())
                .resultFollowUps().getFirst();

        assertThat(followUp.description()).contains("**Eigene Zutaten**", "**Gericht / Umsetzung**", "BBBB",
                "**Bewertung**", "EEEE", "…");
        assertThat(followUp.description().length()).isLessThanOrEqualTo(4_096);
    }

    private static PublicChallenge challenge(long number, boolean cardAvailable, List<RequirementSnapshot> requirements,
                                              RestrictionSnapshot restriction) {
        return challenge(number, cardAvailable, requirements, restriction, ChallengeStatus.ACTIVE, 0, List.of());
    }

    private static PublicChallenge challenge(long number, boolean cardAvailable, List<RequirementSnapshot> requirements,
                                              RestrictionSnapshot restriction, ChallengeStatus status, long resultCount,
                                              List<ChallengeResultView> results) {
        Instant completedAt = status == ChallengeStatus.COMPLETED ? Instant.parse("2026-08-22T12:15:30Z") : null;
        return new PublicChallenge(number, Instant.parse("2026-08-21T10:15:30Z"), requirements, restriction,
                cardAvailable, status, completedAt, resultCount, results);
    }

    private static List<RequirementSnapshot> standardRequirements() {
        return List.of(
                new RequirementSnapshot(1, "Tempeh", Specificity.SPECIFIC),
                new RequirementSnapshot(2, "Mayonnaise", Specificity.SPECIFIC),
                new RequirementSnapshot(3, "Kohlgemüse", Specificity.OPEN),
                new RequirementSnapshot(4, "Essig", Specificity.OPEN));
    }

    private static ChallengeCardBinary card(long number) {
        return new ChallengeCardBinary(new ChallengeCardMetadata(number, "image/png", "card.png", 3, "a".repeat(64),
                Instant.parse("2026-08-21T10:00:00Z"), Instant.parse("2026-08-21T10:01:00Z")), new byte[] {1, 2, 3});
    }
}
