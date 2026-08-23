package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries.ChallengeStatus;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.ChallengeChoice;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.ConcretizationField;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.PhotoChoice;
import io.github.venomenon328.miseendice.discord.internal.DiscordResultCaptureWorkflow.Preparation;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordResultPreparationSourceTextTest {

    @Test
    void shortenedConcretizationPreviewAlwaysCarriesTheCompleteSourceTextFile() {
        String fullText = "R".repeat(1_000);
        Preparation preparation = preparation(fullText, false,
                List.of(new ConcretizationField(3, "Weichtiere", "")));

        var message = DiscordResultCaptureJdaListener.preparationCreate(preparation);

        assertThat(message.getContent()).contains("nachrichtentext.txt", "gekennzeichnet gekürzt")
                .hasSizeLessThanOrEqualTo(2_000);
        assertThat(message.getFiles()).singleElement().satisfies(file ->
                assertThat(file.getName()).isEqualTo("nachrichtentext.txt"));
        assertThat(message.getAllowedMentions()).isEmpty();
    }

    @Test
    void completeShortPreviewDoesNotAddAnUnnecessarySourceTextFile() {
        String fullText = "R".repeat(500);
        Preparation preparation = preparation(fullText, false,
                List.of(new ConcretizationField(3, "Weichtiere", "")));

        var message = DiscordResultCaptureJdaListener.preparationCreate(preparation);

        assertThat(message.getContent()).contains("Vollständiger kopierbarer Nachrichtentext")
                .doesNotContain("nachrichtentext.txt");
        assertThat(message.getFiles()).isEmpty();
    }

    private static Preparation preparation(String fullText, boolean attachFullText,
                                           List<ConcretizationField> concretizations) {
        return new Preparation("token", fullText, attachFullText, fullText.getBytes(StandardCharsets.UTF_8),
                "Person", 1L, List.of(new ChallengeChoice(1, ChallengeStatus.ACTIVE, "aktiv")),
                1, 1, List.of(new PhotoChoice(-1, "Kein Foto", true)), true, false, concretizations);
    }
}
