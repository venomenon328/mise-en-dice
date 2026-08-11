package io.github.venomenon328.miseendice.administration.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class AdministrationSecurityConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    UserDetailsServiceAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class,
                    SecurityFilterAutoConfiguration.class
            ))
            .withUserConfiguration(AdministrationSecurityConfiguration.class);

    @Test
    void disabledAdapterStartsWithoutCredentialsOrAnIdentitySource() {
        contextRunner
                .withPropertyValues("mise-en-dice.administration.enabled=false")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    assertThat(context).doesNotHaveBean(AdministrationIdentitySource.class);
                });
    }

    @Test
    void enabledAdapterWithoutAccountsFailsWithAConfigurationError() {
        contextRunner
                .withPropertyValues("mise-en-dice.administration.enabled=true")
                .run(context -> {
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Administration adapter is enabled, but its account configuration is invalid: "
                                            + "configure at least one account"
                            );
                });
    }
}
