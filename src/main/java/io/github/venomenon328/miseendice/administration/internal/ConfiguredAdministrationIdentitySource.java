package io.github.venomenon328.miseendice.administration.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

final class ConfiguredAdministrationIdentitySource implements AdministrationIdentitySource, UserDetailsService {

    private static final Pattern ACTOR_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$(?:0[4-9]|[12]\\d|3[01])\\$[./A-Za-z0-9]{53}$");

    private final Map<String, ConfiguredAccount> accounts;

    ConfiguredAdministrationIdentitySource(List<AdministrationProperties.Account> configuredAccounts) {
        if (configuredAccounts == null || configuredAccounts.isEmpty()) {
            throw configurationError("configure at least one account");
        }
        if (configuredAccounts.size() > 2) {
            throw configurationError("configure no more than two accounts");
        }

        Map<String, ConfiguredAccount> validatedAccounts = new LinkedHashMap<>();
        for (AdministrationProperties.Account configuredAccount : configuredAccounts) {
            if (configuredAccount == null) {
                throw configurationError("remove empty account entries");
            }

            String actorKey = requiredValue(configuredAccount.getActorKey(), "actor-key");
            if (!ACTOR_KEY.matcher(actorKey).matches()) {
                throw configurationError("use an actor-key containing only letters, digits, underscores, or hyphens");
            }
            String displayName = requiredValue(configuredAccount.getDisplayName(), "display-name");
            String passwordHash = requiredValue(configuredAccount.getPasswordHash(), "password-hash");
            if (!BCRYPT_HASH.matcher(passwordHash).matches()) {
                throw configurationError("provide a valid BCrypt password-hash");
            }

            ConfiguredAccount previous = validatedAccounts.putIfAbsent(
                    actorKey,
                    new ConfiguredAccount(new AdministrationIdentity(actorKey, displayName), passwordHash)
            );
            if (previous != null) {
                throw configurationError("configure each actor-key only once");
            }
        }
        this.accounts = Map.copyOf(validatedAccounts);
    }

    @Override
    public Optional<AdministrationIdentity> findByActorKey(String actorKey) {
        return Optional.ofNullable(accounts.get(actorKey)).map(ConfiguredAccount::identity);
    }

    @Override
    public UserDetails loadUserByUsername(String actorKey) {
        ConfiguredAccount account = accounts.get(actorKey);
        if (account == null) {
            throw new UsernameNotFoundException("Unknown administration account");
        }
        return User.withUsername(account.identity().actorKey())
                .password(account.passwordHash())
                .roles("ADMIN")
                .build();
    }

    private static String requiredValue(String value, String property) {
        if (value == null || value.isBlank()) {
            throw configurationError("set accounts[]." + property);
        }
        return value.strip();
    }

    private static IllegalStateException configurationError(String detail) {
        return new IllegalStateException(
                "Administration adapter is enabled, but its account configuration is invalid: " + detail
        );
    }

    private record ConfiguredAccount(AdministrationIdentity identity, String passwordHash) {
    }
}
