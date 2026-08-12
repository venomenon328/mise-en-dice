package io.github.venomenon328.miseendice.challenge.api;

/** Injectable source for production attempt seeds; explicit test/labor seeds bypass it. */
@FunctionalInterface
public interface SeedSource {
    long nextSeed();
}
