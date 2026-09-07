--liquibase formatted sql
--changeset venomenon328:019-availability-curator-note
-- Notes remain nullable for the generic sparse participant contract.
ALTER TABLE ingredient_availability ADD COLUMN curator_note text;
ALTER TABLE ingredient_availability ADD CONSTRAINT ck_ingredient_availability_curator_note
    CHECK (curator_note IS NULL OR curator_note ~ '[^[:space:]]');
