package io.github.venomenon328.miseendice.challenge.internal;

/** Injectable source used only when a completed voting round has a top-score tie. */
@FunctionalInterface
interface TieBreakRandomSource {

    int nextInt(int bound);
}
