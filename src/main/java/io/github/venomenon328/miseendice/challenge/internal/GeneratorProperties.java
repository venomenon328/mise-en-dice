package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed Spring binding wrapper around the immutable domain configuration. */
@ConfigurationProperties(prefix = "mise-en-dice.generator")
record GeneratorProperties(GeneratorConfiguration configuration) {
    GeneratorProperties {
        if (configuration == null) {
            throw new IllegalArgumentException("mise-en-dice.generator.configuration is required");
        }
    }
}
