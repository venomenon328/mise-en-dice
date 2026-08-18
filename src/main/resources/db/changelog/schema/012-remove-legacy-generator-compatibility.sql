--liquibase formatted sql
--changeset venomenon328:012-remove-legacy-generator-compatibility splitStatements:false

-- Phase 12B.5A.1: only Generator 1.2 and its candidate-local restriction contract remain executable.
-- Published schema changesets stay intact in Liquibase history. Challenge/generator runtime data from
-- pre-1.2 versions is deliberately not migrated or archived; environments must reset that disposable
-- pre-pilot data before applying this structural cleanup.

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
