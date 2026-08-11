package io.github.venomenon328.miseendice.administration.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdministrationProperties.class)
class AdministrationSecurityConfiguration {

    @Bean
    PasswordEncoder administrationPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
    ConfiguredAdministrationIdentitySource administrationIdentitySource(AdministrationProperties properties) {
        return new ConfiguredAdministrationIdentitySource(properties.getAccounts());
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
    UserDetailsService administrationUserDetailsService(ConfiguredAdministrationIdentitySource identitySource) {
        return identitySource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mise-en-dice.administration", name = "enabled", havingValue = "true")
    SecurityFilterChain administrationSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService administrationUserDetailsService,
            PasswordEncoder administrationPasswordEncoder
    ) throws Exception {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(administrationUserDetailsService);
        authenticationProvider.setPasswordEncoder(administrationPasswordEncoder);

        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "mise-en-dice.administration",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    UserDetailsService disabledAdministrationUserDetailsService() {
        return actorKey -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(
                    "Administration adapter is disabled"
            );
        };
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "mise-en-dice.administration",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    SecurityFilterChain disabledAdministrationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
