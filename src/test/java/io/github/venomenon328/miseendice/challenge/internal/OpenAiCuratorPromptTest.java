package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import org.junit.jupiter.api.Test;

class OpenAiCuratorPromptTest {

    @Test
    void v3IsCurrentWhilePersistedV2RemainsSupported() {
        assertThat(OpenAiCuratorPrompt.currentVersion()).isEqualTo(OpenAiCuratorPrompt.VERSION_V3);
        assertThat(OpenAiCuratorPrompt.supports(OpenAiCuratorPrompt.VERSION_V3,
                CurationModel.CONTRACT_VERSION_V2)).isTrue();
        assertThat(OpenAiCuratorPrompt.supports(OpenAiCuratorPrompt.VERSION_V2,
                CurationModel.CONTRACT_VERSION_V2)).isTrue();
        assertThat(OpenAiCuratorPrompt.textFor(OpenAiCuratorPrompt.VERSION_V2))
                .isEqualTo(OpenAiCuratorPrompt.TEXT_V2);
        assertThat(OpenAiCuratorPrompt.textFor(OpenAiCuratorPrompt.VERSION_V3))
                .isEqualTo(OpenAiCuratorPrompt.TEXT_V3);
    }

    @Test
    void v3MakesOpenChoiceRiskDiagnosticRatherThanAnAutomaticQualityPenalty() {
        String prompt = OpenAiCuratorPrompt.textFor(OpenAiCuratorPrompt.VERSION_V3);

        assertThat(prompt)
                .contains("OPEN requirements are intentional user choices, not quality defects")
                .contains("Do not judge whether every or the average possible concrete choice would fit")
                .contains("at least one natural")
                .contains("reasonably recognizable, non-contrived concrete choice")
                .contains("must not by themselves lower the evaluation or rank")
                .contains("exceptionally narrow, obscure, contrived")
                .contains("problematic ingredient interactions")
                .contains("lock-in to an established standard dish")
                .contains("diversity of the combined offer");
    }

    @Test
    void v2TextRemainsThePreviousPromptWithoutV3ChoiceRiskRules() {
        String prompt = OpenAiCuratorPrompt.textFor(OpenAiCuratorPrompt.VERSION_V2);

        assertThat(prompt)
                .contains("OPEN requirements are intentional user choices: accept their")
                .contains("real choice risk and do not narrow them to an ideal concrete ingredient")
                .doesNotContain("not quality defects")
                .doesNotContain("must not by themselves lower the evaluation or rank");
    }

    @Test
    void unknownPromptVersionOrContractIsRejected() {
        assertThat(OpenAiCuratorPrompt.supports("CURATOR_PROMPT_UNKNOWN", CurationModel.CONTRACT_VERSION_V2)).isFalse();
        assertThat(OpenAiCuratorPrompt.supports(OpenAiCuratorPrompt.VERSION_V3, "CURATION_CONTRACT_UNKNOWN")).isFalse();
        assertThatThrownBy(() -> OpenAiCuratorPrompt.textFor("CURATOR_PROMPT_UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported curator prompt version");
    }
}
