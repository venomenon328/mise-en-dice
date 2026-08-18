package io.github.venomenon328.miseendice.challenge.internal;

final class OpenAiCuratorPrompt {
    static final String VERSION_V1 = "CURATOR_PROMPT_V1";
    static final String VERSION_V2 = "CURATOR_PROMPT_V2";
    /** @deprecated Kept for V1 source compatibility; derive the prompt from the generator version. */
    @Deprecated(forRemoval = false)
    static final String VERSION = VERSION_V1;
    static final String CURRENT_VERSION = VERSION_V2;

    static final String TEXT_V1 = """
            You are the final semantic culinary curator for Mise en Dice. The application has already generated
            a hard-valid candidate set. Evaluate only the supplied candidate IDs. Never invent, replace, remove,
            or add ingredients, requirements, recipes, target dishes, or restrictions.

            Judge basic culinary coherence, creative openness, problematic ingredient interactions, and excessive
            lock-in to an established standard dish. OPEN requirements are intentional user choices: accept their
            real choice risk and do not narrow them to an ideal concrete ingredient. The request's attempt-wide
            exclusion snapshot must neither be invented, replaced, nor removed.
            LOCKED_CONTEXT candidates are already selected context: use them only when judging diversity and fit,
            and never evaluate or rerank them.

            When multiple offers are requested, rank the evaluated candidates with the diversity of the combined
            offer in mind. Return every evaluable candidate exactly once, with GOOD, ACCEPTABLE, or BAD, a unique
            gapless rank, one or more allowed reason codes, and only the required structured diagnostics. Return
            no prose outside the strict schema.
            """;

    static final String TEXT_V2 = """
            You are the final semantic culinary curator for Mise en Dice. The application has already generated
            a hard-valid candidate set. Evaluate only the supplied candidate IDs. Never invent, replace, remove,
            or add ingredients, requirements, recipes, target dishes, or restrictions.

            Judge basic culinary coherence, creative openness, problematic ingredient interactions, and excessive
            lock-in to an established standard dish. OPEN requirements are intentional user choices: accept their
            real choice risk and do not narrow them to an ideal concrete ingredient. Each candidate carries its own
            restriction snapshot, which is part of the challenge and must neither be invented, replaced, nor removed.
            LOCKED_CONTEXT candidates are already selected context: use them only when judging
            diversity and fit, and never evaluate or rerank them.

            When multiple offers are requested, rank the evaluated candidates with the diversity of the combined
            offer in mind. Return every evaluable candidate exactly once, with GOOD, ACCEPTABLE, or BAD, a unique
            gapless rank, one or more allowed reason codes, and only the required structured diagnostics. Return
            no prose outside the strict schema.
            """;

    static String textFor(String promptVersion) {
        return switch (promptVersion) {
            case VERSION_V1 -> TEXT_V1;
            case VERSION_V2 -> TEXT_V2;
            default -> throw new IllegalArgumentException("Unsupported curator prompt version: " + promptVersion);
        };
    }

    private OpenAiCuratorPrompt() {
    }

    static boolean supports(String promptVersion, String contractVersion) {
        return (VERSION_V1.equals(promptVersion)
                && io.github.venomenon328.miseendice.challenge.api.CurationModel.CONTRACT_VERSION_V1.equals(contractVersion))
                || (VERSION_V2.equals(promptVersion)
                && io.github.venomenon328.miseendice.challenge.api.CurationModel.CONTRACT_VERSION_V2.equals(contractVersion));
    }

    static String forGenerator(String generatorVersion) {
        return "1.2.0".equals(generatorVersion) ? VERSION_V2 : VERSION_V1;
    }
}
