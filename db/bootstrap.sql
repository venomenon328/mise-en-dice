-- Mise en Dice - complete database bootstrap for a fresh PostgreSQL database
-- Run from any directory with:
--   psql -v ON_ERROR_STOP=1 -1 -f db/bootstrap.sql "$DATABASE_URL"
--
-- \ir resolves included paths relative to this file.

\set ON_ERROR_STOP on

\ir migrations/001_catalog_schema.sql
\ir migrations/002_challenge_history_schema.sql

\ir seeds/001_reference_data.sql
\ir seeds/002_ingredient_catalog.sql
\ir seeds/003_functional_roles.sql
\ir seeds/004_availability.sql
\ir seeds/005_culinary_properties.sql
\ir seeds/006_seasonality.sql
\ir seeds/007_exclusion_rules.sql
\ir seeds/008_ingredient_catalog_expansion_1.sql
\ir seeds/009_ingredient_catalog_expansion_2.sql
\ir seeds/010_ingredient_catalog_expansion_3.sql
\ir seeds/011_ingredient_refinements_expansion.sql
\ir seeds/012_seasonality_expansion.sql
\ir seeds/013_exclusion_rules_expansion.sql

\ir checks/001_seed_sanity.sql
