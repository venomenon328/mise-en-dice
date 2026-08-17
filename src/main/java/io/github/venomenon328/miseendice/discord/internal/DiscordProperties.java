package io.github.venomenon328.miseendice.discord.internal;

import java.time.ZoneId;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** External, optional Discord transport configuration. */
@ConfigurationProperties(prefix = "mise-en-dice.discord")
record DiscordProperties(boolean enabled, String token, long guildId, ZoneId effectiveDateZone,
                         Map<String, String> participantUserIds) {
    static final String PROVIDER = "discord";

    DiscordProperties {
        effectiveDateZone = effectiveDateZone == null ? ZoneId.of("Europe/Berlin") : effectiveDateZone;
        participantUserIds = participantUserIds == null ? Map.of() : Map.copyOf(participantUserIds);
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
        for (String code : java.util.List.of("GEORGIA", "TOBIAS")) {
            String userId = participantUserIds.get(code);
            if (userId == null || !userId.matches("[0-9]{5,32}")) {
                throw new IllegalStateException("A numeric Discord user ID for " + code + " is required when Discord is enabled");
            }
        }
    }

    boolean isConfiguredUser(String externalSubject) {
        return participantUserIds.containsValue(externalSubject);
    }
}
