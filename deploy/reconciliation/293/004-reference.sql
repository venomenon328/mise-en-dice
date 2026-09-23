-- Stable reference is insert-only; an existing code is already installed.
INSERT INTO culinary_country (code, display_name)
VALUES ('GB-SCT','Schottland') ON CONFLICT (code) DO NOTHING;
