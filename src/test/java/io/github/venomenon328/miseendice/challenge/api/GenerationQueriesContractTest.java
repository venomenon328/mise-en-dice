package io.github.venomenon328.miseendice.challenge.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class GenerationQueriesContractTest {
    @Test
    void publicQueriesOnlyReadStoredAttemptsContextsAndBatches() {
        assertThat(GenerationQueries.class.getDeclaredMethods()).extracting(Method::getName)
                .containsExactlyInAnyOrder("findAttempt", "findContext", "findBatch");
        assertThat(GenerationQueries.class.getDeclaredClasses()).extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("NextAction", "AttemptView", "ContextView", "BatchView",
                        "CandidateView", "RequirementView");
    }
}
