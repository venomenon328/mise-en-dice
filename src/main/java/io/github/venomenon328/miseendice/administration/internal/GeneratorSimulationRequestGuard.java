package io.github.venomenon328.miseendice.administration.internal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Ephemeral adapter guard; it deliberately creates neither jobs nor persistent simulation state. */
@Component
class GeneratorSimulationRequestGuard {
    private final Set<String> runningSessionIds = ConcurrentHashMap.newKeySet();

    boolean tryAcquire(String sessionId) {
        return runningSessionIds.add(sessionId);
    }

    void release(String sessionId) {
        runningSessionIds.remove(sessionId);
    }
}
