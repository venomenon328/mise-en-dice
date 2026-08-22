package io.github.venomenon328.miseendice.discord.internal;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/** External, optional Discord transport configuration. */
@ConfigurationProperties(prefix = "mise-en-dice.discord")
record DiscordProperties(boolean enabled, String token, long guildId, long challengeOperatorRoleId,
                         ZoneId effectiveDateZone, Map<String, String> participantUserIds) {
    static final String PROVIDER = "discord";

    DiscordProperties(boolean enabled, String token, long guildId, ZoneId effectiveDateZone,
                      Map<String, String> participantUserIds) {
        this(enabled, token, guildId, 0, effectiveDateZone, participantUserIds);
    }

    @ConstructorBinding
    DiscordProperties {
        effectiveDateZone = effectiveDateZone == null ? ZoneId.of("Europe/Berlin") : effectiveDateZone;
        participantUserIds = canonicalParticipantUserIds(participantUserIds);
    }

    void validateEnabledConfiguration() {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("mise-en-dice.discord.token is required when Discord is enabled");
        }
        if (guildId <= 0) {
            throw new IllegalStateException("mise-en-dice.discord.guild-id is required when Discord is enabled");
        }
        if (challengeOperatorRoleId <= 0) {
            throw new IllegalStateException("mise-en-dice.discord.challenge-operator-role-id is required when Discord is enabled");
        }
        for (Map.Entry<String, String> entry : participantUserIds.entrySet()) {
            if (!entry.getValue().matches("[0-9]{5,32}")) {
                throw new IllegalStateException("Legacy Discord user IDs must be numeric");
            }
        }
    }

    boolean hasLegacyParticipantBootstrap() {
        return !participantUserIds.isEmpty();
    }

    private static Map<String, String> canonicalParticipantUserIds(Map<String, String> configured) {
        if (configured == null || configured.isEmpty()) {
            return Map.of();
        }
        Map<String, String> canonical = new HashMap<>();
        for (Map.Entry<String, String> entry : configured.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalArgumentException("Discord participant mappings need a participant code and user ID");
            }
            String code = entry.getKey().strip().toUpperCase(Locale.ROOT);
            String userId = entry.getValue().strip();
            if (!"GEORGIA".equals(code) && !"TOBIAS".equals(code)) {
                throw new IllegalArgumentException("Legacy Discord participant mappings support only GEORGIA and TOBIAS");
            }
            String existing = canonical.putIfAbsent(code, userId);
            if (existing != null && !existing.equals(userId)) {
                throw new IllegalArgumentException("Conflicting Discord user IDs for participant code " + code);
            }
        }
        if (canonical.size() == 2 && canonical.get("GEORGIA").equals(canonical.get("TOBIAS"))) {
            throw new IllegalArgumentException("Discord user IDs must not be assigned to multiple participant codes");
        }
        return Map.copyOf(canonical);
    }
}
