--liquibase formatted sql

--changeset mise-en-dice:020-remove-generator-replay-result
-- Completed batches are read from their persisted candidates and diagnostics.
-- The full duplicate calculation payload has no remaining workflow or display consumer.
-- Keep every other result constraint, including the legacy exception, unchanged.
ALTER TABLE generation_batch DROP CONSTRAINT ck_generation_batch_result;
ALTER TABLE generation_batch DROP COLUMN result_snapshot;
ALTER TABLE generation_batch ADD CONSTRAINT ck_generation_batch_result CHECK (
    legacy_migrated
    OR (
        status = 'GENERATED'
        AND batch_seed IS NOT NULL
        AND fallback_level IN ('STRICT', 'RELAXED_1', 'RELAXED_2')
        AND set_evaluation IS NOT NULL
        AND set_fingerprint ~ '^[0-9a-f]{64}$'
    )
    OR (
        status = 'EXHAUSTED'
        AND batch_seed IS NOT NULL
        AND fallback_level IS NULL
        AND set_evaluation IS NULL
        AND set_fingerprint IS NULL
    )
);
