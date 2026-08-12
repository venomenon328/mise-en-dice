package io.github.venomenon328.miseendice.challenge.api;

/** Stable domain validation failure raised before proposal generation begins. */
public final class GeneratorValidationException extends IllegalArgumentException {
    private final GeneratorReasonCode reasonCode;

    public GeneratorValidationException(GeneratorReasonCode reasonCode, String detail) {
        super(reasonCode.name() + ": " + detail);
        this.reasonCode = reasonCode;
    }

    public GeneratorReasonCode reasonCode() {
        return reasonCode;
    }
}
