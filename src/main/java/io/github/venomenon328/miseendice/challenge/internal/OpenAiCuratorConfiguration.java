package io.github.venomenon328.miseendice.challenge.internal;

import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiCuratorProperties.class)
class OpenAiCuratorConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.curation.openai", name = "enabled", havingValue = "true")
    CuratorClient openAiCuratorClient(OpenAiCuratorProperties properties, ObjectMapper objectMapper,
                                      Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("production"))) {
            throw new IllegalStateException(
                    "The OpenAI curator can only be enabled with the production Spring profile");
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.requestTimeout());
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
        return new OpenAiCuratorClient(restClient, objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.curation.openai", name = "enabled", havingValue = "false",
            matchIfMissing = true)
    CuratorClient disabledCuratorClient(OpenAiCuratorProperties properties) {
        return new CuratorClient() {
            @Override public boolean available() { return false; }
            @Override public String model() { return properties.model(); }
            @Override public PreparedDispatch prepare(String model,
                    io.github.venomenon328.miseendice.challenge.api.CurationRequest request) {
                throw new IllegalStateException("OpenAI curator adapter is disabled");
            }
            @Override public ProviderExchange dispatch(PreparedDispatch dispatch) { throw new IllegalStateException("disabled"); }
            @Override public Interpretation interpret(io.github.venomenon328.miseendice.challenge.api.CurationRequest request,
                                                       ProviderExchange exchange) { throw new IllegalStateException("disabled"); }
        };
    }
}
