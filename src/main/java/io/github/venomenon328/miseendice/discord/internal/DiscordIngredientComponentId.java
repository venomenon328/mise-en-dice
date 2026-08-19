package io.github.venomenon328.miseendice.discord.internal;

/** Versioned, stateless identifiers for Discord ingredient lookup components. */
final class DiscordIngredientComponentId {
    private static final String PREFIX = "med:v1:ingredient:";
    private static final String SEARCH_SELECTION_PREFIX = PREFIX + "select:";
    private static final String NAVIGATION_SELECT_PREFIX = PREFIX + "navigate-select:";

    private DiscordIngredientComponentId() {
    }

    static String selection(String invokerUserId) {
        if (invokerUserId == null || !invokerUserId.matches("[0-9]{5,32}")) {
            throw new IllegalArgumentException("Discord user ID must be numeric");
        }
        return SEARCH_SELECTION_PREFIX + invokerUserId;
    }

    static boolean isSelection(String customId) {
        return customId != null && customId.startsWith(SEARCH_SELECTION_PREFIX);
    }

    static String navigationSelect(String direction) {
        return NAVIGATION_SELECT_PREFIX + validDirection(direction);
    }

    static boolean isNavigationSelect(String customId) {
        if (customId == null || !customId.startsWith(NAVIGATION_SELECT_PREFIX)) {
            return false;
        }
        String direction = customId.substring(NAVIGATION_SELECT_PREFIX.length());
        return "parent".equals(direction) || "child".equals(direction);
    }

    static String conceptValue(long conceptId) {
        return PREFIX + "concept:" + positiveConceptId(conceptId);
    }

    static Selection parseSelection(String customId) {
        if (!isSelection(customId)) {
            throw new IllegalArgumentException("Unknown ingredient component");
        }
        String userId = customId.substring(SEARCH_SELECTION_PREFIX.length());
        if (!userId.matches("[0-9]{5,32}")) {
            throw new IllegalArgumentException("Malformed ingredient component");
        }
        return new Selection(userId);
    }

    static void validateNavigationSelect(String customId) {
        if (!isNavigationSelect(customId)) {
            throw new IllegalArgumentException("Unknown ingredient navigation select");
        }
    }

    static long parseConceptValue(String value) {
        String prefix = PREFIX + "concept:";
        if (value == null || !value.startsWith(prefix)) {
            throw new IllegalArgumentException("Unknown ingredient option");
        }
        return parsePositiveLong(value.substring(prefix.length()), "ingredient option");
    }

    private static String validDirection(String direction) {
        if (!"parent".equals(direction) && !"child".equals(direction)) {
            throw new IllegalArgumentException("Unknown ingredient navigation direction");
        }
        return direction;
    }

    private static long positiveConceptId(long conceptId) {
        if (conceptId <= 0) {
            throw new IllegalArgumentException("conceptId must be positive");
        }
        return conceptId;
    }

    private static long parsePositiveLong(String value, String label) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0 || !Long.toString(parsed).equals(value)) {
                throw new IllegalArgumentException("Malformed " + label);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed " + label, exception);
        }
    }

    record Selection(String invokerUserId) {
    }
}
