package io.github.venomenon328.miseendice.discord.internal;

/** Versioned, stateless identifiers for Discord ingredient lookup components. */
final class DiscordIngredientComponentId {
    private static final String PREFIX = "med:v1:ingredient:";
    private static final String SEARCH_SELECTION_PREFIX = PREFIX + "select:";
    private static final String NAVIGATION_SELECT_PREFIX = PREFIX + "navigate-select:";
    private static final String OWNED_NAVIGATION_SELECT_PREFIX = "med:v2:ingredient:navigate-select:";
    private static final String OWNED_COUNTRY_NAVIGATION_SELECT_PREFIX = "med:v3:ingredient:navigate-select:";
    private static final String COUNTRY_PREFIX = "med:v1:country-ingredients:";
    private static final String COUNTRY_SELECT_PREFIX = COUNTRY_PREFIX + "select:";
    private static final String COUNTRY_PAGE_PREFIX = COUNTRY_PREFIX + "page:";
    private static final String COUNTRY_BACK_PREFIX = COUNTRY_PREFIX + "back:";

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

    static String navigationSelect(String direction, String invokerUserId, CountryBrowseContext countryContext) {
        return OWNED_COUNTRY_NAVIGATION_SELECT_PREFIX + validDirection(direction) + ":" + validUserId(invokerUserId)
                + ":" + countryContext.countryCode() + ":" + countryContext.page();
    }

    static String bindNavigationOwner(String customId, String invokerUserId) {
        if (customId == null || !customId.startsWith(NAVIGATION_SELECT_PREFIX)) {
            throw new IllegalArgumentException("Ingredient navigation template expected");
        }
        String direction = customId.substring(NAVIGATION_SELECT_PREFIX.length());
        validateNavigationSelect(customId);
        return navigationSelect(direction, invokerUserId);
    }

    static String bindNavigationOwner(String customId, String invokerUserId, CountryBrowseContext countryContext) {
        if (countryContext == null) {
            return bindNavigationOwner(customId, invokerUserId);
        }
        if (customId == null || !customId.startsWith(NAVIGATION_SELECT_PREFIX)) {
            throw new IllegalArgumentException("Ingredient navigation template expected");
        }
        String direction = customId.substring(NAVIGATION_SELECT_PREFIX.length());
        validateNavigationSelect(customId);
        return navigationSelect(direction, invokerUserId, countryContext);
    }

    static boolean isNavigationSelect(String customId) {
        return customId != null && (customId.startsWith(NAVIGATION_SELECT_PREFIX)
                || customId.startsWith(OWNED_NAVIGATION_SELECT_PREFIX)
                || customId.startsWith(OWNED_COUNTRY_NAVIGATION_SELECT_PREFIX));
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
            if (customId == null || !customId.startsWith(OWNED_COUNTRY_NAVIGATION_SELECT_PREFIX)) {
                throw new IllegalArgumentException("Ingredient navigation is not bound to a user");
            }
            return parseCountryNavigationSelect(customId);
        }
        String payload = customId.substring(OWNED_NAVIGATION_SELECT_PREFIX.length());
        int separator = payload.indexOf(':');
        if (separator <= 0 || separator != payload.lastIndexOf(':')) {
            throw new IllegalArgumentException("Malformed ingredient navigation select");
        }
        String direction = validDirection(payload.substring(0, separator));
        String userId = validUserId(payload.substring(separator + 1));
        return new NavigationSelect(direction, userId, null);
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
        if (customId.startsWith(OWNED_COUNTRY_NAVIGATION_SELECT_PREFIX)) {
            parseNavigationSelect(customId);
            return;
        }
        throw new IllegalArgumentException("Unknown ingredient navigation select");
    }

    static String countrySelect(CountryBrowseContext context, String invokerUserId) {
        return COUNTRY_SELECT_PREFIX + validUserId(invokerUserId) + ":" + context.countryCode() + ":" + context.page();
    }

    static boolean isCountrySelect(String customId) {
        return customId != null && customId.startsWith(COUNTRY_SELECT_PREFIX);
    }

    static CountrySelect parseCountrySelect(String customId) {
        String[] fields = countryPayload(customId, COUNTRY_SELECT_PREFIX, "country ingredient selection");
        return new CountrySelect(validUserId(fields[0]), new CountryBrowseContext(fields[1], parsePage(fields[2])));
    }

    static String countryPage(CountryBrowseContext context, String invokerUserId, int targetPage) {
        return COUNTRY_PAGE_PREFIX + validUserId(invokerUserId) + ":" + context.countryCode() + ":" + positivePage(targetPage);
    }

    static boolean isCountryPage(String customId) {
        return customId != null && customId.startsWith(COUNTRY_PAGE_PREFIX);
    }

    static CountryPage parseCountryPage(String customId) {
        String[] fields = countryPayload(customId, COUNTRY_PAGE_PREFIX, "country ingredient page");
        return new CountryPage(validUserId(fields[0]), new CountryBrowseContext(fields[1], parsePage(fields[2])));
    }

    static String countryBack(CountryBrowseContext context, String invokerUserId) {
        return COUNTRY_BACK_PREFIX + validUserId(invokerUserId) + ":" + context.countryCode() + ":" + context.page();
    }

    static boolean isCountryBack(String customId) {
        return customId != null && customId.startsWith(COUNTRY_BACK_PREFIX);
    }

    static CountryBack parseCountryBack(String customId) {
        String[] fields = countryPayload(customId, COUNTRY_BACK_PREFIX, "country ingredient return");
        return new CountryBack(validUserId(fields[0]), new CountryBrowseContext(fields[1], parsePage(fields[2])));
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

    private static NavigationSelect parseCountryNavigationSelect(String customId) {
        String payload = customId.substring(OWNED_COUNTRY_NAVIGATION_SELECT_PREFIX.length());
        String[] fields = payload.split(":", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Malformed ingredient country navigation select");
        }
        return new NavigationSelect(validDirection(fields[0]), validUserId(fields[1]),
                new CountryBrowseContext(fields[2], parsePage(fields[3])));
    }

    private static String[] countryPayload(String customId, String prefix, String label) {
        if (customId == null || !customId.startsWith(prefix)) {
            throw new IllegalArgumentException("Unknown " + label);
        }
        String[] fields = customId.substring(prefix.length()).split(":", -1);
        if (fields.length != 3) {
            throw new IllegalArgumentException("Malformed " + label);
        }
        return fields;
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

    private static int parsePage(String value) {
        try {
            return positivePage(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Malformed country ingredient page", exception);
        }
    }

    private static int positivePage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be positive");
        }
        return page;
    }

    record Selection(String invokerUserId) {
    }

    record NavigationSelect(String direction, String invokerUserId, CountryBrowseContext countryContext) {
        NavigationSelect(String direction, String invokerUserId) {
            this(direction, invokerUserId, null);
        }
    }

    record CountryBrowseContext(String countryCode, int page) {
        CountryBrowseContext {
            if (countryCode == null || !countryCode.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("countryCode must be an ISO alpha-2 code");
            }
            positivePage(page);
        }
    }

    record CountrySelect(String invokerUserId, CountryBrowseContext context) {
    }

    record CountryPage(String invokerUserId, CountryBrowseContext context) {
    }

    record CountryBack(String invokerUserId, CountryBrowseContext context) {
    }
}
