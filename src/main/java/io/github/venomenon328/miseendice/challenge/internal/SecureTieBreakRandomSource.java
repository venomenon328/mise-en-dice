package io.github.venomenon328.miseendice.challenge.internal;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
class SecureTieBreakRandomSource implements TieBreakRandomSource {
    private final SecureRandom random = new SecureRandom();

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
