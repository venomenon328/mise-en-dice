package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class DiscordPropertiesTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(DiscordConfiguration.class);
    private final ApplicationContextRunner bindingContext = new ApplicationContextRunner()
            .withUserConfiguration(BoundDiscordProperties.class);

    @Test
    void disabledAdapterDoesNotRequireCredentialsOrGatewayConfiguration() {
        assertThatCode(() -> new DiscordProperties(false, "", 0, 0, ZoneId.of("Europe/Berlin"), Map.of())
                .validateEnabledConfiguration()).doesNotThrowAnyException();
    }

    @Test
    void enabledAdapterFailsClearlyWithoutCompleteConfiguration() {
        assertThatThrownBy(() -> new DiscordProperties(true, "", 0, 0, ZoneId.of("Europe/Berlin"), Map.of())
                .validateEnabledConfiguration()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token");
    }

    @Test
    void enabledAdapterRequiresChallengeOperatorRoleIndependentlyOfParticipants() {
        assertThatThrownBy(() -> new DiscordProperties(true, "token", 99, 0, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")).validateEnabledConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("challenge-operator-role-id");
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

    @Test
    void canonicalizesParticipantCodesAndBindsOperatorRoleThroughActualSpringPropertyBinding() {
        bindingContext.withPropertyValues(
                "mise-en-dice.discord.enabled=true",
                "mise-en-dice.discord.token=test-token",
                "mise-en-dice.discord.guild-id=99",
                "mise-en-dice.discord.challenge-operator-role-id=77777",
                "mise-en-dice.discord.participant-user-ids.georgia=10001",
                "mise-en-dice.discord.participant-user-ids.Tobias=10002"
        ).run(applicationContext -> {
            assertThat(applicationContext).hasNotFailed();
            DiscordProperties properties = applicationContext.getBean(DiscordProperties.class);
            assertThat(properties.challengeOperatorRoleId()).isEqualTo(77777);
            assertThat(properties.participantUserIds()).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "GEORGIA", "10001", "TOBIAS", "10002"));
            assertThat(properties.isConfiguredUser("10001")).isTrue();
            assertThat(properties.isConfiguredUser(null)).isFalse();
            properties.validateEnabledConfiguration();
        });
    }

    @Test
    void rejectsDuplicateDiscordUserIdsThroughActualSpringPropertyBinding() {
        bindingContext.withPropertyValues(
                "mise-en-dice.discord.enabled=true",
                "mise-en-dice.discord.token=test-token",
                "mise-en-dice.discord.guild-id=99",
                "mise-en-dice.discord.challenge-operator-role-id=77777",
                "mise-en-dice.discord.participant-user-ids.georgia=10001",
                "mise-en-dice.discord.participant-user-ids.tobias=10001"
        ).run(applicationContext -> {
            DiscordProperties properties = applicationContext.getBean(DiscordProperties.class);
            assertThatThrownBy(properties::validateEnabledConfiguration).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must not be assigned to multiple participant codes");
        });
    }

    @Test
    void rejectsConflictingCaseVariantsForTheSameParticipantCode() {
        assertThatThrownBy(() -> new DiscordProperties(true, "token", 99, 77777, ZoneId.of("Europe/Berlin"),
                Map.of("georgia", "10001", "GEORGIA", "10002"))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conflicting Discord user IDs for participant code GEORGIA");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DiscordProperties.class)
    static class BoundDiscordProperties {
    }
}
