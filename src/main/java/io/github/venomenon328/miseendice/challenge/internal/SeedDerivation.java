package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;

/** SHA-256 named-substream contract from the generator specification. */
final class SeedDerivation {
    private static final String PREFIX = "MED-SEED-V1";

    private SeedDerivation() {
    }

    static long derive(String generatorVersion, long attemptSeed, String scope, Purpose purpose, long ordinal) {
        String payload = String.join("\0",
                nfc(PREFIX),
                nfc(generatorVersion),
                Long.toString(attemptSeed),
                nfc(scope),
                nfc(purpose.code()),
                Long.toString(ordinal));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(digest, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
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
                selectionPurpose(fallbackLevel, position), 0);
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

    private static Purpose selectionPurpose(FallbackLevel fallbackLevel, int position) {
        String prefix = switch (fallbackLevel) {
            case STRICT -> "BATCH_SELECTION_STRICT_";
            case RELAXED_1 -> "BATCH_SELECTION_RELAXED_1_";
            case RELAXED_2 -> "BATCH_SELECTION_RELAXED_2_";
        };
        return Purpose.valueOf(prefix + position);
    }

    private static String nfc(String value) {
        if (value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Seed fields must not contain a null byte");
        }
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    enum Purpose {
        BATCH_ROOT("batch-root"),
        CANDIDATE_RESTRICTION_MODE("candidate-restriction-mode"),
        CANDIDATE_RESTRICTION_RULE("candidate-restriction-rule"),
        PROPOSAL_PROFILE("proposal-profile"),
        PROPOSAL_SPECIFICITY("proposal-specificity"),
        PROPOSAL_NOVELTY("proposal-novelty"),
        PROPOSAL_SLOT_1("proposal-slot/1"),
        PROPOSAL_SLOT_2("proposal-slot/2"),
        PROPOSAL_SLOT_3("proposal-slot/3"),
        PROPOSAL_SLOT_4("proposal-slot/4"),
        BATCH_SELECTION_STRICT_1("batch-selection/strict/1"),
        BATCH_SELECTION_STRICT_2("batch-selection/strict/2"),
        BATCH_SELECTION_STRICT_3("batch-selection/strict/3"),
        BATCH_SELECTION_STRICT_4("batch-selection/strict/4"),
        BATCH_SELECTION_STRICT_5("batch-selection/strict/5"),
        BATCH_SELECTION_STRICT_6("batch-selection/strict/6"),
        BATCH_SELECTION_STRICT_7("batch-selection/strict/7"),
        BATCH_SELECTION_STRICT_8("batch-selection/strict/8"),
        BATCH_SELECTION_STRICT_9("batch-selection/strict/9"),
        BATCH_SELECTION_STRICT_10("batch-selection/strict/10"),
        BATCH_SELECTION_STRICT_11("batch-selection/strict/11"),
        BATCH_SELECTION_STRICT_12("batch-selection/strict/12"),
        BATCH_SELECTION_RELAXED_1_1("batch-selection/relaxed_1/1"),
        BATCH_SELECTION_RELAXED_1_2("batch-selection/relaxed_1/2"),
        BATCH_SELECTION_RELAXED_1_3("batch-selection/relaxed_1/3"),
        BATCH_SELECTION_RELAXED_1_4("batch-selection/relaxed_1/4"),
        BATCH_SELECTION_RELAXED_1_5("batch-selection/relaxed_1/5"),
        BATCH_SELECTION_RELAXED_1_6("batch-selection/relaxed_1/6"),
        BATCH_SELECTION_RELAXED_1_7("batch-selection/relaxed_1/7"),
        BATCH_SELECTION_RELAXED_1_8("batch-selection/relaxed_1/8"),
        BATCH_SELECTION_RELAXED_1_9("batch-selection/relaxed_1/9"),
        BATCH_SELECTION_RELAXED_1_10("batch-selection/relaxed_1/10"),
        BATCH_SELECTION_RELAXED_1_11("batch-selection/relaxed_1/11"),
        BATCH_SELECTION_RELAXED_1_12("batch-selection/relaxed_1/12"),
        BATCH_SELECTION_RELAXED_2_1("batch-selection/relaxed_2/1"),
        BATCH_SELECTION_RELAXED_2_2("batch-selection/relaxed_2/2"),
        BATCH_SELECTION_RELAXED_2_3("batch-selection/relaxed_2/3"),
        BATCH_SELECTION_RELAXED_2_4("batch-selection/relaxed_2/4"),
        BATCH_SELECTION_RELAXED_2_5("batch-selection/relaxed_2/5"),
        BATCH_SELECTION_RELAXED_2_6("batch-selection/relaxed_2/6"),
        BATCH_SELECTION_RELAXED_2_7("batch-selection/relaxed_2/7"),
        BATCH_SELECTION_RELAXED_2_8("batch-selection/relaxed_2/8"),
        BATCH_SELECTION_RELAXED_2_9("batch-selection/relaxed_2/9"),
        BATCH_SELECTION_RELAXED_2_10("batch-selection/relaxed_2/10"),
        BATCH_SELECTION_RELAXED_2_11("batch-selection/relaxed_2/11"),
        BATCH_SELECTION_RELAXED_2_12("batch-selection/relaxed_2/12");

        private final String code;

        Purpose(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }
    }
}
