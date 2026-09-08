--liquibase formatted sql
--changeset venomenon328:021-culinary-country-subdivision-codes splitStatements:false
-- Issue #172: allow explicitly curated UK constituent-country codes such as GB-ENG.
-- Existing ISO 3166-1 alpha-2 codes remain unchanged; GB continues to mean United Kingdom.

ALTER TABLE ingredient_culinary_country
    DROP CONSTRAINT ingredient_culinary_country_country_code_fkey;

ALTER TABLE culinary_country
    DROP CONSTRAINT ck_culinary_country_code;

ALTER TABLE culinary_country
    ALTER COLUMN code TYPE varchar(6);

ALTER TABLE ingredient_culinary_country
    ALTER COLUMN country_code TYPE varchar(6);

ALTER TABLE culinary_country
    ADD CONSTRAINT ck_culinary_country_code
    CHECK (code ~ '^[A-Z]{2}(-[A-Z0-9]{1,3})?$');

ALTER TABLE ingredient_culinary_country
    ADD CONSTRAINT ingredient_culinary_country_country_code_fkey
    FOREIGN KEY (country_code) REFERENCES culinary_country(code) ON DELETE RESTRICT;
