--liquibase formatted sql
--changeset venomenon328:013-challenge-archive-core splitStatements:false

-- Phase 13A: public, immutable challenge numbers and the optional current Challenge Card.
-- A single row is deliberately used instead of a PostgreSQL sequence: incrementing it is part of the
-- confirmation transaction, so a rolled-back challenge does not consume a public number.

SET CONSTRAINTS ALL IMMEDIATE;

ALTER TABLE challenge
    ADD COLUMN challenge_number bigint;

WITH numbered_challenges AS (
    SELECT id, row_number() OVER (ORDER BY shown_at, id)::bigint AS challenge_number
    FROM challenge
)
UPDATE challenge
   SET challenge_number = numbered_challenges.challenge_number
  FROM numbered_challenges
 WHERE challenge.id = numbered_challenges.id;

ALTER TABLE challenge
    ALTER COLUMN challenge_number SET NOT NULL,
    ADD CONSTRAINT ck_challenge_number_positive CHECK (challenge_number > 0),
    ADD CONSTRAINT uq_challenge_number UNIQUE (challenge_number);

CREATE TABLE challenge_archive_counter (
    singleton                boolean PRIMARY KEY DEFAULT true,
    last_challenge_number    bigint NOT NULL DEFAULT 0,

    CONSTRAINT ck_challenge_archive_counter_singleton CHECK (singleton),
    CONSTRAINT ck_challenge_archive_counter_non_negative CHECK (last_challenge_number >= 0)
);

INSERT INTO challenge_archive_counter (singleton, last_challenge_number)
SELECT true, coalesce(max(challenge_number), 0)
FROM challenge;

CREATE OR REPLACE FUNCTION validate_challenge_number_immutable()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.challenge_number IS DISTINCT FROM OLD.challenge_number THEN
        RAISE EXCEPTION 'challenge number is immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_challenge_number_immutable
BEFORE UPDATE OF challenge_number
ON challenge
FOR EACH ROW
EXECUTE FUNCTION validate_challenge_number_immutable();

CREATE TABLE challenge_card (
    challenge_id             bigint PRIMARY KEY REFERENCES challenge(id) ON DELETE RESTRICT,
    content_bytes            bytea NOT NULL,
    content_type             text NOT NULL,
    original_filename        text NOT NULL,
    byte_size                bigint NOT NULL,
    sha256                   bytea NOT NULL,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT ck_challenge_card_content_type CHECK (content_type = 'image/png'),
    CONSTRAINT ck_challenge_card_byte_size CHECK (byte_size > 0 AND byte_size <= 5242880),
    CONSTRAINT ck_challenge_card_content_size CHECK (octet_length(content_bytes) = byte_size),
    CONSTRAINT ck_challenge_card_sha256_size CHECK (octet_length(sha256) = 32),
    CONSTRAINT ck_challenge_card_filename_not_blank CHECK (btrim(original_filename) <> '')
);

CREATE OR REPLACE FUNCTION maintain_challenge_card_identity()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.challenge_id <> OLD.challenge_id THEN
        RAISE EXCEPTION 'challenge card cannot move to another challenge';
    END IF;
    IF NEW.created_at <> OLD.created_at THEN
        RAISE EXCEPTION 'challenge card creation timestamp is immutable';
    END IF;
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_challenge_card_maintain_identity
BEFORE UPDATE
ON challenge_card
FOR EACH ROW
EXECUTE FUNCTION maintain_challenge_card_identity();
