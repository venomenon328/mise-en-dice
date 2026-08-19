package io.github.venomenon328.miseendice.discord.internal;

/** Versioned, stateless identifiers for the Discord ingredient lookup select menu. */
final class DiscordIngredientComponentId {
    private static final String PREFIX = "med:v1:ingredient:";

    private DiscordIngredientComponentId() {
    }

    static String selection(String invokerUserId) {
        if (invokerUserId == null || !invokerUserId.matches("[0-9]{5,32}")) {
            throw new IllegalArgumentException("Discord user ID must be numeric");
        }
        return PREFIX + "select:" + invokerUserId;
    }

    static String conceptValue(long conceptId) {
        if (conceptId <= 0) {
            throw new IllegalArgumentException("conceptId must be positive");
        }
        return PREFIX + "concept:" + conceptId;
    }

    static Selection parseSelection(String customId) {
        if (customId == null || !customId.startsWith(PREFIX + "select:")) {
            throw new IllegalArgumentException("Unknown ingredient component");
        }
        String userId = customId.substring((PREFIX + "select:").length());
        if (!userId.matches("[0-9]{5,32}")) {
            throw new IllegalArgumentException("Malformed ingredient component");
        }
        return new Selection(userId);
    }

    static long parseConceptValue(String value) {
        if (value == null || !value.startsWith(PREFIX + "concept:")) {
            throw new IllegalArgumentException("Unknown ingredient option");
        }
        String id = value.substring((PREFIX + "concept:").length());
        try {
            long conceptId = Long.parseLong(id);
            if (conceptId <= 0 || !Long.toString(conceptId).equals(id)) {
                throw new IllegalArgumentException("Malformed ingredient option");
            }
            return conceptId;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed ingredient option", exception);
        }
    }

    record Selection(String invokerUserId) {
    }
}
