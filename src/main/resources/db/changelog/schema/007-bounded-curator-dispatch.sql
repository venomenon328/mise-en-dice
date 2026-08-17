--liquibase formatted sql
--changeset venomenon328:007-bounded-curator-dispatch splitStatements:false

-- Phase 10B: one durable claim represents permission for exactly one external
-- provider request. The raw request/response columns deliberately use text so
-- audit retains the bytes handed to and received from the HTTP adapter.
ALTER TABLE curation_round
    ADD COLUMN provider text,
    ADD COLUMN dispatch_status text NOT NULL DEFAULT 'UNCLAIMED',
    ADD COLUMN dispatch_claimed_at timestamptz,
    ADD COLUMN dispatch_recovery_deadline_at timestamptz,
    ADD COLUMN provider_request_payload text,
    ADD COLUMN provider_response_payload text,
    ADD COLUMN provider_response_id text,
    ADD COLUMN provider_usage_snapshot jsonb,
    ADD COLUMN provider_http_status smallint,
    ADD COLUMN provider_error_code text,
    ADD COLUMN provider_diagnostic text,
    ADD COLUMN provider_retryable boolean,
    ADD COLUMN provider_result_recorded_at timestamptz,
    ADD CONSTRAINT ck_curation_round_provider
        CHECK (provider IS NULL OR provider = 'OPENAI'),
    ADD CONSTRAINT ck_curation_round_dispatch_status
        CHECK (dispatch_status IN ('UNCLAIMED', 'CLAIMED', 'RESULT_RECORDED', 'UNKNOWN_EXTERNAL_OUTCOME')),
    ADD CONSTRAINT ck_curation_round_provider_http_status
        CHECK (provider_http_status IS NULL OR provider_http_status BETWEEN 100 AND 599),
    ADD CONSTRAINT ck_curation_round_provider_payload_limits
        CHECK ((provider_request_payload IS NULL OR length(provider_request_payload) <= 1000000)
           AND (provider_response_payload IS NULL OR length(provider_response_payload) <= 1000000)
           AND (provider_response_id IS NULL OR length(provider_response_id) <= 255)
           AND (provider_error_code IS NULL OR length(provider_error_code) <= 128)
           AND (provider_diagnostic IS NULL OR length(provider_diagnostic) <= 1000)),
    ADD CONSTRAINT ck_curation_round_dispatch_shape
        CHECK (
            (dispatch_status = 'UNCLAIMED'
                AND provider IS NULL
                AND dispatch_claimed_at IS NULL
                AND dispatch_recovery_deadline_at IS NULL
                AND provider_request_payload IS NULL
                AND provider_response_payload IS NULL
                AND provider_response_id IS NULL
                AND provider_usage_snapshot IS NULL
                AND provider_http_status IS NULL
                AND provider_error_code IS NULL
                AND provider_diagnostic IS NULL
                AND provider_retryable IS NULL
                AND provider_result_recorded_at IS NULL)
            OR
            (dispatch_status = 'CLAIMED'
                AND provider IS NOT NULL
                AND dispatch_claimed_at IS NOT NULL
                AND dispatch_recovery_deadline_at > dispatch_claimed_at
                AND provider_request_payload IS NOT NULL
                AND provider_result_recorded_at IS NULL)
            OR
            (dispatch_status = 'RESULT_RECORDED'
                AND provider IS NOT NULL
                AND dispatch_claimed_at IS NOT NULL
                AND dispatch_recovery_deadline_at > dispatch_claimed_at
                AND provider_request_payload IS NOT NULL
                AND provider_result_recorded_at IS NOT NULL
                AND provider_retryable IS NOT NULL)
            OR
            (dispatch_status = 'UNKNOWN_EXTERNAL_OUTCOME'
                AND provider IS NOT NULL
                AND dispatch_claimed_at IS NOT NULL
                AND dispatch_recovery_deadline_at > dispatch_claimed_at
                AND provider_request_payload IS NOT NULL
                AND provider_result_recorded_at IS NOT NULL
                AND provider_error_code = 'UNKNOWN_EXTERNAL_OUTCOME'
                AND provider_retryable = true)
        );

CREATE INDEX ix_curation_round_dispatch_recovery
    ON curation_round (dispatch_recovery_deadline_at)
    WHERE dispatch_status = 'CLAIMED' AND NOT legacy_migrated;

CREATE OR REPLACE FUNCTION validate_curation_dispatch_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    used_slots integer;
BEGIN
    IF OLD.legacy_migrated THEN
        RETURN NEW;
    END IF;

    IF OLD.dispatch_status <> NEW.dispatch_status THEN
        IF OLD.dispatch_status = 'UNCLAIMED' AND NEW.dispatch_status = 'CLAIMED' THEN
            IF OLD.status <> 'PENDING' OR NEW.status <> 'PENDING' THEN
                RAISE EXCEPTION 'only a pending curation round can claim an external dispatch';
            END IF;
            SELECT count(*) INTO used_slots
              FROM curation_round other
             WHERE other.generation_attempt_id = NEW.generation_attempt_id
               AND NOT other.legacy_migrated
               AND other.id <> NEW.id
               AND other.dispatch_status IN ('CLAIMED', 'RESULT_RECORDED', 'UNKNOWN_EXTERNAL_OUTCOME');
            IF used_slots >= 2 THEN
                RAISE EXCEPTION 'generation attempt % has exhausted its two external request slots',
                    NEW.generation_attempt_id;
            END IF;
        ELSIF OLD.dispatch_status = 'CLAIMED'
              AND NEW.dispatch_status IN ('RESULT_RECORDED', 'UNKNOWN_EXTERNAL_OUTCOME') THEN
            NULL;
        ELSE
            RAISE EXCEPTION 'invalid curator dispatch transition from % to %',
                OLD.dispatch_status, NEW.dispatch_status;
        END IF;
    ELSIF OLD.dispatch_status <> 'UNCLAIMED'
          AND (NEW.provider IS DISTINCT FROM OLD.provider
               OR NEW.dispatch_claimed_at IS DISTINCT FROM OLD.dispatch_claimed_at
               OR NEW.dispatch_recovery_deadline_at IS DISTINCT FROM OLD.dispatch_recovery_deadline_at
               OR NEW.provider_request_payload IS DISTINCT FROM OLD.provider_request_payload) THEN
        RAISE EXCEPTION 'claimed curator dispatch identity is immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_curation_round_dispatch_transition
BEFORE UPDATE OF dispatch_status, provider, dispatch_claimed_at,
                 dispatch_recovery_deadline_at, provider_request_payload
ON curation_round
FOR EACH ROW
EXECUTE FUNCTION validate_curation_dispatch_transition();
