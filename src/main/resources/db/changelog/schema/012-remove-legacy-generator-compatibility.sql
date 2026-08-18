--liquibase formatted sql
--changeset venomenon328:012-remove-legacy-generator-compatibility splitStatements:false

-- Phase 12B.5A.1: only Generator 1.2 and its candidate-local restriction contract remain executable.
-- Previously published schemas stay intact in Liquibase history. Existing V1 rounds are retained as
-- non-executable technical history, while the obsolete attempt-wide columns are removed forward-only.

UPDATE curation_round
SET legacy_migrated = true
WHERE NOT legacy_migrated
  AND contract_version = 'CURATION_CONTRACT_V1';

SET CONSTRAINTS ALL IMMEDIATE;

ALTER TABLE curation_round
    DROP CONSTRAINT ck_curation_round_new_contract;

ALTER TABLE curation_round
    ADD CONSTRAINT ck_curation_round_new_contract
        CHECK (
            legacy_migrated
            OR (
                primary_generation_batch_id IS NOT NULL
                AND request_purpose IS NOT NULL
                AND contract_version = 'CURATION_CONTRACT_V2'
                AND request_payload IS NOT NULL
                AND status IN ('PENDING', 'COMPLETED', 'TECHNICAL_ERROR', 'INVALID_RESPONSE')
            )
        );

ALTER TABLE generation_attempt
    DROP CONSTRAINT ck_generation_attempt_exclusion_snapshot,
    DROP COLUMN exclusion_rule_id,
    DROP COLUMN exclusion_text_snapshot;

ALTER TABLE challenge_candidate
    DROP CONSTRAINT ck_challenge_candidate_exclusion_snapshot,
    DROP COLUMN exclusion_rule_id,
    DROP COLUMN exclusion_text_snapshot;
