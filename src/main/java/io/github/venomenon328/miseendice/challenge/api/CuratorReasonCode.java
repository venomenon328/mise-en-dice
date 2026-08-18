package io.github.venomenon328.miseendice.challenge.api;

import java.util.Set;

/** Closed vocabulary accepted from the current external curator contract. */
public enum CuratorReasonCode {
    /** General culinary-coherence classification. */
    CULINARY_COHERENCE,
    CULINARY_COHERENCE_STRONG,
    CULINARY_COHERENCE_WEAK,
    CREATIVE_OPENNESS_STRONG,
    CREATIVE_OPENNESS_LIMITED,
    STANDARD_DISH_LOCK_IN_RISK,
    INGREDIENT_INTERACTION_RISK,
    OPEN_REQUIREMENT_CHOICE_RISK,
    EXCLUSION_CONFLICT,
    MULTI_OFFER_DIVERSITY_STRONG,
    MULTI_OFFER_DIVERSITY_WEAK;

    private static final Set<String> VALUES = java.util.Arrays.stream(values())
            .map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());

    public static boolean supports(String value) {
        return VALUES.contains(value);
    }
}
