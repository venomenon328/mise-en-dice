--liquibase formatted sql
--changeset venomenon328:003-england-culinary-country splitStatements:false
-- Issue #172: England is curated separately from the United Kingdom.
-- GB-ENG follows the UK Government extended country-code standard; GB remains unchanged.

INSERT INTO culinary_country (code, display_name)
VALUES ('GB-ENG', 'England');
