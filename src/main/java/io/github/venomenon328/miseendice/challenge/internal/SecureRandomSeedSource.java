package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import java.security.SecureRandom;

final class SecureRandomSeedSource implements SeedSource {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public long nextSeed() {
        return secureRandom.nextLong();
    }
}
