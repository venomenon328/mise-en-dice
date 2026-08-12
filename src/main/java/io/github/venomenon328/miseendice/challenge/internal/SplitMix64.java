package io.github.venomenon328.miseendice.challenge.internal;

/** Project-owned bit-exact SPLITMIX64_V1 implementation. */
final class SplitMix64 {
    private long state;

    SplitMix64(long seed) {
        this.state = seed;
    }

    long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        long value = state;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    long nextLong(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long bits;
        long value;
        do {
            bits = nextLong() >>> 1;
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0L);
        return value;
    }
}
