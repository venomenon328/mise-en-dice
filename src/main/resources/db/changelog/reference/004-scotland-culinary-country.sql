--liquibase formatted sql
--changeset venomenon328:004-scotland-culinary-country splitStatements:false
-- Issue #172: Scotland is curated separately from the United Kingdom and England.
-- GB-SCT follows the UK Government extended country-code standard; GB remains unchanged.

INSERT INTO culinary_country (code, display_name)
VALUES ('GB-SCT', 'Schottland');
