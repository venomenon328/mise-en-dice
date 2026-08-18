package io.github.venomenon328.miseendice.challenge.internal;

final class OpenAiCuratorPrompt {
    static final String VERSION_V2 = "CURATOR_PROMPT_V2";
    static final String CURRENT_VERSION = VERSION_V2;

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
        if (!CURRENT_VERSION.equals(promptVersion)) {
            throw new IllegalArgumentException("Unsupported curator prompt version: " + promptVersion);
        }
        return TEXT_V2;
    }

    private OpenAiCuratorPrompt() {
    }

    static boolean supports(String promptVersion, String contractVersion) {
        return CURRENT_VERSION.equals(promptVersion)
                && io.github.venomenon328.miseendice.challenge.api.CurationModel.CURRENT_CONTRACT_VERSION.equals(contractVersion);
    }

    static String currentVersion() {
        return CURRENT_VERSION;
    }
}
