package io.github.venomenon328.miseendice.discord.internal;

/** Versioned, stateless identifiers for Discord ingredient lookup components. */
final class DiscordIngredientComponentId {
    private static final String PREFIX = "med:v1:ingredient:";
    private static final String SEARCH_SELECTION_PREFIX = PREFIX + "select:";
    private static final String NAVIGATION_SELECT_PREFIX = PREFIX + "navigate-select:";
    private static final String OWNED_NAVIGATION_SELECT_PREFIX = "med:v2:ingredient:navigate-select:";

    private DiscordIngredientComponentId() {
    }

    static String selection(String invokerUserId) {
        return SEARCH_SELECTION_PREFIX + validUserId(invokerUserId);
    }

    static boolean isSelection(String customId) {
        return customId != null && customId.startsWith(SEARCH_SELECTION_PREFIX);
    }

    /** Renderer-level navigation template. The JDA bridge binds the card owner before sending it. */
    static String navigationSelect(String direction) {
        return NAVIGATION_SELECT_PREFIX + validDirection(direction);
    }

    static String navigationSelect(String direction, String invokerUserId) {
        return OWNED_NAVIGATION_SELECT_PREFIX + validDirection(direction) + ":" + validUserId(invokerUserId);
    }

    static String bindNavigationOwner(String customId, String invokerUserId) {
        if (customId == null || !customId.startsWith(NAVIGATION_SELECT_PREFIX)) {
            throw new IllegalArgumentException("Ingredient navigation template expected");
        }
        String direction = customId.substring(NAVIGATION_SELECT_PREFIX.length());
        validateNavigationSelect(customId);
        return navigationSelect(direction, invokerUserId);
    }

    static boolean isNavigationSelect(String customId) {
        return customId != null && (customId.startsWith(NAVIGATION_SELECT_PREFIX)
                || customId.startsWith(OWNED_NAVIGATION_SELECT_PREFIX));
    }

    static String conceptValue(long conceptId) {
        return PREFIX + "concept:" + positiveConceptId(conceptId);
    }

    static Selection parseSelection(String customId) {
        if (!isSelection(customId)) {
            throw new IllegalArgumentException("Unknown ingredient component");
        }
        String userId = customId.substring(SEARCH_SELECTION_PREFIX.length());
        return new Selection(validUserId(userId));
    }

    static NavigationSelect parseNavigationSelect(String customId) {
        if (customId == null || !customId.startsWith(OWNED_NAVIGATION_SELECT_PREFIX)) {
            throw new IllegalArgumentException("Ingredient navigation is not bound to a user");
        }
        String payload = customId.substring(OWNED_NAVIGATION_SELECT_PREFIX.length());
        int separator = payload.indexOf(':');
        if (separator <= 0 || separator != payload.lastIndexOf(':')) {
            throw new IllegalArgumentException("Malformed ingredient navigation select");
        }
        String direction = validDirection(payload.substring(0, separator));
        String userId = validUserId(payload.substring(separator + 1));
        return new NavigationSelect(direction, userId);
    }

    static void validateNavigationSelect(String customId) {
        if (customId == null) {
            throw new IllegalArgumentException("Unknown ingredient navigation select");
        }
        if (customId.startsWith(NAVIGATION_SELECT_PREFIX)) {
            validDirection(customId.substring(NAVIGATION_SELECT_PREFIX.length()));
            return;
        }
        if (customId.startsWith(OWNED_NAVIGATION_SELECT_PREFIX)) {
            parseNavigationSelect(customId);
            return;
        }
        throw new IllegalArgumentException("Unknown ingredient navigation select");
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

    private static String validUserId(String userId) {
        if (userId == null || !userId.matches("[0-9]{5,32}")) {
            throw new IllegalArgumentException("Discord user ID must be numeric");
        }
        return userId;
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

    record NavigationSelect(String direction, String invokerUserId) {
    }
}
