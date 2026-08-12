package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;

/** SHA-256 named-substream contract from the generator specification. */
final class SeedDerivation {
    private static final String PREFIX = "MED-SEED-V1";

    private SeedDerivation() {
    }

    static long derive(String generatorVersion, long attemptSeed, String scope, Purpose purpose, long ordinal) {
        return derive(generatorVersion, attemptSeed, scope, purpose.code(), ordinal);
    }

    static long deriveSelection(
            String generatorVersion,
            long attemptSeed,
            int batchNumber,
            FallbackLevel fallbackLevel,
            int position
    ) {
        if (fallbackLevel == null || position < 1 || position > 12) {
            throw new IllegalArgumentException("Selection substreams require a fallback and position from 1 to 12");
        }
        return derive(generatorVersion, attemptSeed, batchScope(batchNumber),
                "batch-selection/" + fallbackLevel.name().toLowerCase(Locale.ROOT) + "/" + position, 0);
    }

    private static long derive(
            String generatorVersion,
            long attemptSeed,
            String scope,
            String purpose,
            long ordinal
    ) {
        String payload = String.join("\0",
                nfc(PREFIX),
                nfc(generatorVersion),
                Long.toString(attemptSeed),
                nfc(scope),
                nfc(purpose),
                Long.toString(ordinal));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    static String attemptScope() {
        return "attempt";
    }

    static String batchScope(int batchNumber) {
        if (batchNumber <= 0) {
            throw new IllegalArgumentException("batchNumber must be positive");
        }
        return "batch/" + batchNumber;
    }

    static Purpose proposalSlot(int slot) {
        if (slot < 1 || slot > 4) {
            throw new IllegalArgumentException("proposal slot must be from 1 to 4");
        }
        return Purpose.valueOf("PROPOSAL_SLOT_" + slot);
    }

    private static String nfc(String value) {
        if (value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Seed fields must not contain a null byte");
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    enum Purpose {
        BATCH_ROOT("batch-root"),
        ATTEMPT_EXCLUSION_MODE("attempt-exclusion-mode"),
        ATTEMPT_EXCLUSION_RULE("attempt-exclusion-rule"),
        PROPOSAL_PROFILE("proposal-profile"),
        PROPOSAL_SPECIFICITY("proposal-specificity"),
        PROPOSAL_NOVELTY("proposal-novelty"),
        PROPOSAL_SLOT_1("proposal-slot/1"),
        PROPOSAL_SLOT_2("proposal-slot/2"),
        PROPOSAL_SLOT_3("proposal-slot/3"),
        PROPOSAL_SLOT_4("proposal-slot/4");

        private final String code;

        Purpose(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }
    }
}
