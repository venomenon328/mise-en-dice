package io.github.venomenon328.miseendice.administration.internal;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** External configuration for the optional private administration adapter. */
@ConfigurationProperties(prefix = "mise-en-dice.administration")
public final class AdministrationProperties {

    private boolean enabled;
    private List<Account> accounts = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts == null ? new ArrayList<>() : new ArrayList<>(accounts);
    }

    public static final class Account {

        private String actorKey;
        private String displayName;
        private String passwordHash;

        public String getActorKey() {
            return actorKey;
        }

        public void setActorKey(String actorKey) {
            this.actorKey = actorKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }
}
