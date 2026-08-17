package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DiscordProperties.class)
class DiscordConfiguration {
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    ExecutorService discordChallengeExecutor(DiscordProperties properties) {
        properties.validateEnabledConfiguration();
        return Executors.newSingleThreadExecutor(Thread.ofPlatform().name("discord-challenge-", 0).factory());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordChallengeWorkflow discordChallengeWorkflow(DiscordProperties properties,
                                                       ChallengeOfferPreparationCommands preparation,
                                                       OfferDecisionQueries offers,
                                                       SelectionVotingCommands votingCommands,
                                                       SelectionVotingQueries votingQueries) {
        properties.validateEnabledConfiguration();
        return new DiscordChallengeWorkflow(properties, preparation, offers, votingCommands, votingQueries,
                new DiscordChallengeRenderer());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordJdaLifecycle discordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                                            ExecutorService discordChallengeExecutor) {
        return new DiscordJdaLifecycle(properties, workflow, discordChallengeExecutor);
    }
}
