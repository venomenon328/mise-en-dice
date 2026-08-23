package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCardCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeCompletionCommands;
import io.github.venomenon328.miseendice.challenge.api.ChallengeResultQueries;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
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
                                                       SelectionVotingQueries votingQueries,
                                                       ParticipantQueries participantQueries) {
        properties.validateEnabledConfiguration();
        return new DiscordChallengeWorkflow(properties, preparation, offers, votingCommands, votingQueries, participantQueries,
                new DiscordChallengeRenderer());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordParticipantAdministrationWorkflow discordParticipantAdministrationWorkflow(ParticipantCommands participantCommands,
                                                                                         ParticipantQueries participantQueries) {
        return new DiscordParticipantAdministrationWorkflow(participantCommands, participantQueries);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordLegacyParticipantBootstrap discordLegacyParticipantBootstrap(DiscordProperties properties,
                                                                         ParticipantCommands participantCommands,
                                                                         ParticipantQueries participantQueries) {
        properties.validateEnabledConfiguration();
        return new DiscordLegacyParticipantBootstrap(properties, participantCommands, participantQueries);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordIngredientLookupWorkflow discordIngredientLookupWorkflow(DiscordProperties properties,
                                                                     IngredientLookupQueries ingredientLookupQueries) {
        properties.validateEnabledConfiguration();
        return new DiscordIngredientLookupWorkflow(properties, ingredientLookupQueries, new DiscordIngredientLookupRenderer());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordChallengeArchiveWorkflow discordChallengeArchiveWorkflow(DiscordProperties properties,
                                                                     ChallengeArchiveQueries archiveQueries,
                                                                     ChallengeCardCommands cardCommands,
                                                                     ChallengeCompletionCommands completionCommands,
                                                                     ChallengeResultQueries resultQueries) {
        properties.validateEnabledConfiguration();
        return new DiscordChallengeArchiveWorkflow(properties, archiveQueries, cardCommands, completionCommands, resultQueries,
                new DiscordChallengeArchiveRenderer(properties.effectiveDateZone()));
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.discord", name = "enabled", havingValue = "true")
    DiscordJdaLifecycle discordJdaLifecycle(DiscordProperties properties, DiscordChallengeWorkflow workflow,
                                            DiscordIngredientLookupWorkflow ingredientLookupWorkflow,
                                            DiscordChallengeArchiveWorkflow archiveWorkflow,
                                            DiscordParticipantAdministrationWorkflow participantAdministrationWorkflow,
                                            ExecutorService discordChallengeExecutor) {
        return new DiscordJdaLifecycle(properties, workflow, ingredientLookupWorkflow, archiveWorkflow,
                participantAdministrationWorkflow, discordChallengeExecutor);
    }
}
