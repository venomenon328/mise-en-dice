--liquibase formatted sql
--changeset venomenon328:018-five-level-availability

-- Extend the published availability constraint without changing any catalog rows.
ALTER TABLE ingredient_availability
    DROP CONSTRAINT ck_ingredient_availability_level;

ALTER TABLE ingredient_availability
    ADD CONSTRAINT ck_ingredient_availability_level
        CHECK (availability_level IN ('EASY', 'PLANNED', 'SPECIALTY', 'DIFFICULT', 'UNAVAILABLE'));
