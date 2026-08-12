package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GeneratorProperties.class)
class GeneratorConfigurationModule {

    @Bean
    CandidateProposalEngine candidateProposalEngine(GeneratorProperties properties, ObjectMapper objectMapper) {
        String snapshot = new CanonicalConfigurationSnapshot(objectMapper).serialize(properties.configuration());
        return new DefaultCandidateProposalEngine(properties.configuration(), snapshot);
    }

    @Bean
    CandidateReservoirEngine candidateReservoirEngine(CandidateProposalEngine candidateProposalEngine) {
        return new DefaultCandidateReservoirEngine(candidateProposalEngine);
    }

    @Bean
    SeedSource generatorSeedSource() {
        return new SecureRandomSeedSource();
    }
}
