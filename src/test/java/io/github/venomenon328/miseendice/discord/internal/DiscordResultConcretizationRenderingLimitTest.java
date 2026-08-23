package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.PublicChallenge;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.RestrictionSnapshot;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.Specificity;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ChallengeResultView;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ParticipantReference;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ResultConcretizationView;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries.ResultIngredientView;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscordResultConcretizationRenderingLimitTest {

    private final DiscordChallengeArchiveRenderer renderer =
            new DiscordChallengeArchiveRenderer(ZoneId.of("Europe/Berlin"));

    @Test
    void maximalResultWithConcretizationsKeepsEverySectionVisibleWithinEmbedLimit() {
        List<ResultIngredientView> ingredients = java.util.stream.IntStream.range(0, 25)
                .mapToObj(index -> new ResultIngredientView(index + 1L,
                        "Zutat " + index + " " + "I".repeat(190), null))
                .toList();
        List<ResultConcretizationView> concretizations = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(position -> new ResultConcretizationView(101, position, 300L + position,
                        "Offene Vorgabe " + position + " " + "R".repeat(180),
                        "Konkretisierung " + position + " " + "K".repeat(180), null))
                .toList();
        ChallengeResultView result = new ChallengeResultView(101, 12,
                new ParticipantReference(7, "PARTICIPANT_7", "Georgia", true),
                "Sehr langes Gericht", "B".repeat(4_000), "E".repeat(4_000), ingredients, concretizations,
                false, 0, Instant.parse("2026-08-22T12:15:30Z"), Instant.parse("2026-08-22T12:15:30Z"));
        List<RequirementSnapshot> requirements = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(position -> new RequirementSnapshot(position, "Offene Vorgabe " + position, Specificity.OPEN))
                .toList();
        PublicChallenge challenge = new PublicChallenge(12, Instant.parse("2026-08-22T12:00:00Z"), requirements,
                RestrictionSnapshot.none(), false, ChallengeStatus.ACTIVE, null, 1, List.of(result));

        DiscordChallengeArchiveRenderer.RenderedDetail followUp = renderer
                .detail(challenge, Optional.empty(), Map.of()).resultFollowUps().getFirst();

        assertThat(followUp.description()).contains("**Konkretisierungen**", "**Eigene Zutaten**",
                "**Gericht / Umsetzung**", "BBBB", "**Bewertung**", "EEEE", "…");
        assertThat(followUp.description().length()).isLessThanOrEqualTo(4_096);
    }
}
