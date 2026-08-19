package io.github.venomenon328.miseendice.discord.internal;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** External, optional Discord transport configuration. */
@ConfigurationProperties(prefix = "mise-en-dice.discord")
record DiscordProperties(boolean enabled, String token, long guildId, long challengeOperatorRoleId,
                         ZoneId effectiveDateZone, Map<String, String> participantUserIds) {
    static final String PROVIDER = "discord";

    DiscordProperties(boolean enabled, String token, long guildId, ZoneId effectiveDateZone,
                      Map<String, String> participantUserIds) {
        this(enabled, token, guildId, 0, effectiveDateZone, participantUserIds);
    }

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
        for (String code : java.util.List.of("GEORGIA", "TOBIAS")) {
            String userId = participantUserIds.get(code);
            if (userId == null || !userId.matches("[0-9]{5,32}")) {
                throw new IllegalStateException("A numeric Discord user ID for " + code + " is required when Discord is enabled");
            }
        }
        if (new HashSet<>(participantUserIds.values()).size() != participantUserIds.size()) {
            throw new IllegalStateException("Discord user IDs must not be assigned to multiple participant codes");
        }
    }

    boolean isConfiguredUser(String externalSubject) {
        if (externalSubject == null) {
            return false;
        }
        return java.util.List.of("GEORGIA", "TOBIAS").stream()
                .map(participantUserIds::get)
                .anyMatch(externalSubject::equals);
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
            String existing = canonical.putIfAbsent(code, userId);
            if (existing != null && !existing.equals(userId)) {
                throw new IllegalArgumentException("Conflicting Discord user IDs for participant code " + code);
            }
        }
        return Map.copyOf(canonical);
    }
}
