package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DiscordPropertiesTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(DiscordConfiguration.class);

    @Test
    void disabledAdapterDoesNotRequireCredentialsOrGatewayConfiguration() {
        assertThatCode(() -> new DiscordProperties(false, "", 0, ZoneId.of("Europe/Berlin"), Map.of())
                .validateEnabledConfiguration()).doesNotThrowAnyException();
    }

    @Test
    void enabledAdapterFailsClearlyWithoutCompleteConfiguration() {
        assertThatThrownBy(() -> new DiscordProperties(true, "", 0, ZoneId.of("Europe/Berlin"), Map.of())
                .validateEnabledConfiguration()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token");
    }

    @Test
    void disabledSpringConfigurationCreatesNoJdaLifecycleOrExecutor() {
        context.withPropertyValues("mise-en-dice.discord.enabled=false").run(applicationContext -> {
            assertThat(applicationContext).hasNotFailed();
            assertThat(applicationContext).doesNotHaveBean(DiscordJdaLifecycle.class);
            assertThat(applicationContext).doesNotHaveBean("discordChallengeExecutor");
        });
    }

    @Test
    void enabledSpringConfigurationFailsBeforeAnyGatewayCanStartWhenSecretsAreIncomplete() {
        context.withPropertyValues("mise-en-dice.discord.enabled=true").run(applicationContext -> {
            assertThat(applicationContext).hasFailed();
            assertThat(applicationContext.getStartupFailure()).hasRootCauseMessage(
                    "mise-en-dice.discord.token is required when Discord is enabled");
        });
    }
}
