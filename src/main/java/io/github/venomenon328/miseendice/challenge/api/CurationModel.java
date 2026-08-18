package io.github.venomenon328.miseendice.challenge.api;

/** Stable transport-neutral vocabulary for the Phase-10A curation boundary. */
public final class CurationModel {

    public static final String CONTRACT_VERSION_V2 = "CURATION_CONTRACT_V2";
    public static final String CURRENT_CONTRACT_VERSION = CONTRACT_VERSION_V2;

    public static boolean supportedContract(String value) {
        return CURRENT_CONTRACT_VERSION.equals(value);
    }

    public static String currentContractVersion() {
        return CURRENT_CONTRACT_VERSION;
    }

    private CurationModel() {
    }

    public enum RequestPurpose {
        INITIAL_PASS,
        TECHNICAL_RETRY,
        QUALITY_FOLLOW_UP
    }

    public enum Participation {
        NEW,
        CARRY_OVER,
        LOCKED_CONTEXT
    }

    public enum Evaluation {
        GOOD,
        ACCEPTABLE,
        BAD
    }

    public enum RoundStatus {
        PENDING,
        COMPLETED,
        TECHNICAL_ERROR,
        INVALID_RESPONSE
    }

    public enum OfferSetStatus {
        CURATED_UNPRESENTED,
        PRESENTED_PENDING_DECISION,
        CONFIRMED,
        REROLLED
    }
}
