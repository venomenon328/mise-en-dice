-- Mise en Dice - initial reference data
-- Requires: db/migrations/001_catalog_schema.sql
--
-- Idempotent bootstrap data. Existing rows are deliberately not overwritten.

INSERT INTO participant (code, display_name)
VALUES
    ('TOBIAS', 'Tobias'),
    ('GEORGIA', 'Georgia')
ON CONFLICT (code) DO NOTHING;

INSERT INTO functional_role (code, display_name, description)
VALUES
    ('ANIMAL_PROTEIN', 'tierisches Protein', 'Tierische Haupt- oder Nebenproteinquelle.'),
    ('PLANT_PROTEIN', 'pflanzliches Protein', 'Pflanzliche Proteinquelle.'),
    ('VEGETABLE', 'Gemüse', 'Gemüseartige Haupt- oder Nebenkomponente.'),
    ('FRUIT', 'Obst', 'Fruchtige Haupt- oder Nebenkomponente.'),
    ('STARCH', 'Stärke', 'Stärkehaltige Sättigungs- oder Strukturkomponente.'),
    ('FAT', 'Fett', 'Ausgeprägt fettreiche oder als Fettträger nutzbare Komponente.'),
    ('ACID', 'Säure', 'Primär säuernde Komponente.'),
    ('AROMATIC', 'Aromat', 'Aromatische Komponente, typischerweise in kleinerer bis mittlerer Menge.'),
    ('SEASONING', 'Würzkomponente', 'Geschmacksprägende Würz- oder Saucenkomponente.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO culinary_flag (code, display_name, description)
VALUES
    ('FERMENTED', 'fermentiert', 'Durch Fermentation geprägt.'),
    ('PICKLED', 'eingelegt', 'Durch Einlegen beziehungsweise Beizen geprägt.'),
    ('SMOKED', 'geräuchert', 'Durch Räuchern geprägt.'),
    ('CURED', 'gepökelt/gereift', 'Durch Pökeln oder vergleichbare Reifung geprägt.'),
    ('DRIED', 'getrocknet', 'Typischerweise als getrocknete Zutat verwendet.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO culinary_dimension (code, display_name, description)
VALUES
    ('DOMINANCE', 'Dominanz', 'Wie stark die Zutat geschmacklich andere Komponenten prägt.'),
    ('SWEETNESS', 'Süße', 'Typische wahrgenommene Süße der Zutat.'),
    ('ACIDITY', 'Säure', 'Typische wahrgenommene Säure der Zutat.'),
    ('BITTERNESS', 'Bitterkeit', 'Typische wahrgenommene Bitterkeit der Zutat.'),
    ('FATTINESS', 'Fettigkeit', 'Typische Fettigkeit beziehungsweise reichhaltige Wirkung.'),
    ('HEAT', 'Schärfe', 'Typische pikante beziehungsweise chiliartige Schärfe.'),
    ('UMAMI', 'Umami', 'Typische Umami-Intensität.')
ON CONFLICT (code) DO NOTHING;
