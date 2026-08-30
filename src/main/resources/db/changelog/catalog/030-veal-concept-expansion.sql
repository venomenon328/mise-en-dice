--liquibase formatted sql

--changeset venomenon328:030-veal-concept-expansion
-- Issue #172: approved expansion of the open veal concept and narrowly
-- supported country associations for already reviewed cuisines.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('VEAL_STEAK', 'Kalbssteak', true, true, 'SPECIFIC', 0.5500, 2,
        'Dicke, ausgelöste Scheibe aus einem zarten Kalbsteilstück, etwa Rücken oder Hüfte; nicht das dünne Kalbsschnitzel. Sie eignet sich zum kurzen Braten oder Grillen und sollte wegen ihrer mageren, feinen Struktur nicht übergart werden.'),
    ('VEAL_CHOP', 'Kalbskotelett', true, true, 'SPECIFIC', 0.5000, 2,
        'Knochenhaltige Scheibe aus dem Kalbsrücken beziehungsweise Karree mit magerem Fleischauge und meist etwas Randfett. Sie wird kurz gebraten oder gegrillt; der Knochen grenzt sie vom ausgelösten Kalbssteak ab.'),
    ('VEAL_STRIPS', 'Kalbsgeschnetzeltes', true, true, 'SPECIFIC', 0.6000, 2,
        'In schmale Streifen geschnittenes, meist mageres Kalbfleisch zum sehr kurzen Anbraten. Gemeint ist der rohe Zuschnitt; eine Sauce oder ein bestimmtes Gericht wie Zürcher Geschnetzeltes ist nicht vorgegeben.'),
    ('VEAL_GOULASH', 'Kalbsgulasch', true, true, 'SPECIFIC', 0.5500, 2,
        'Rohe Würfel oder grobe Stücke vom Kalb für Ragouts und andere langsam gegarte Gerichte; nicht das fertig gewürzte Gericht. Bindegewebsreichere Zuschnitte werden durch Schmoren zart und geben der Sauce Körper.'),
    ('VEAL_ROAST', 'Kalbsbraten', true, true, 'SPECIFIC', 0.5000, 2,
        'Größeres zusammenhängendes Stück Kalbfleisch zum Braten oder Schmoren; der anatomische Zuschnitt bleibt bewusst offen. Garmethode und Garzeit richten sich nach Fett- und Bindegewebsanteil des gewählten Stücks.'),
    ('VEAL_MINCE', 'Kalbshackfleisch', true, true, 'SPECIFIC', 0.4000, 3,
        'Gewolftes Kalbfleisch mit mildem Geschmack und meist geringerem Fettanteil als gemischtes Hack. Es eignet sich etwa für Füllungen, Klöße, Frikadellen oder Saucen, verlangt wegen seiner mageren Struktur aber vorsichtige Garung.'),
    ('VEAL_CHEEK', 'Kalbsbäckchen', true, true, 'SPECIFIC', 0.3000, 3,
        'Kleines, stark beanspruchtes Muskelstück aus der Backe mit viel Kollagen und feiner Faser. Es benötigt langes Schmoren, wird dann sehr zart und ergibt eine kräftige, gelatinebetonte Sauce.'),
    ('VEAL_SWEETBREAD', 'Kalbsbries', true, true, 'SPECIFIC', 0.2000, 3,
        'Kalbsbries bezeichnet eine zarte Drüse des jungen Kalbes, vor allem den Thymus, mit feiner, cremiger Textur. Vor der Zubereitung wird es meist gewässert, blanchiert und von Häuten befreit; es ist weder Hirn noch gewöhnliches Muskelfleisch.');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('VEAL', 'VEAL_STEAK'),
    ('VEAL', 'VEAL_CHOP'),
    ('VEAL', 'VEAL_STRIPS'),
    ('VEAL', 'VEAL_GOULASH'),
    ('VEAL', 'VEAL_ROAST'),
    ('VEAL', 'VEAL_MINCE'),
    ('VEAL', 'VEAL_CHEEK'),
    ('VEAL', 'VEAL_SWEETBREAD'),
    ('MINCED_MEAT', 'VEAL_MINCE'),
    ('OFFAL', 'VEAL_SWEETBREAD')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('VEAL_STEAK', 'ANIMAL_PROTEIN'),
    ('VEAL_CHOP', 'ANIMAL_PROTEIN'),
    ('VEAL_STRIPS', 'ANIMAL_PROTEIN'),
    ('VEAL_GOULASH', 'ANIMAL_PROTEIN'),
    ('VEAL_ROAST', 'ANIMAL_PROTEIN'),
    ('VEAL_MINCE', 'ANIMAL_PROTEIN'),
    ('VEAL_CHEEK', 'ANIMAL_PROTEIN'),
    ('VEAL_SWEETBREAD', 'ANIMAL_PROTEIN')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('VEAL_STEAK', 'DOMINANCE', 3),
    ('VEAL_STEAK', 'FATTINESS', 2),
    ('VEAL_STEAK', 'UMAMI', 3),
    ('VEAL_CHOP', 'DOMINANCE', 3),
    ('VEAL_CHOP', 'FATTINESS', 3),
    ('VEAL_CHOP', 'UMAMI', 3),
    ('VEAL_STRIPS', 'DOMINANCE', 3),
    ('VEAL_STRIPS', 'FATTINESS', 2),
    ('VEAL_STRIPS', 'UMAMI', 3),
    ('VEAL_GOULASH', 'DOMINANCE', 3),
    ('VEAL_GOULASH', 'FATTINESS', 2),
    ('VEAL_GOULASH', 'UMAMI', 4),
    ('VEAL_ROAST', 'DOMINANCE', 3),
    ('VEAL_ROAST', 'FATTINESS', 2),
    ('VEAL_ROAST', 'UMAMI', 4),
    ('VEAL_MINCE', 'DOMINANCE', 3),
    ('VEAL_MINCE', 'FATTINESS', 2),
    ('VEAL_MINCE', 'UMAMI', 3),
    ('VEAL_CHEEK', 'DOMINANCE', 4),
    ('VEAL_CHEEK', 'FATTINESS', 3),
    ('VEAL_CHEEK', 'UMAMI', 4),
    ('VEAL_SWEETBREAD', 'DOMINANCE', 4),
    ('VEAL_SWEETBREAD', 'FATTINESS', 4),
    ('VEAL_SWEETBREAD', 'UMAMI', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('VEAL_STEAK', 'TOBIAS', 'PLANNED'),
    ('VEAL_STEAK', 'GEORGIA', 'PLANNED'),
    ('VEAL_CHOP', 'TOBIAS', 'PLANNED'),
    ('VEAL_CHOP', 'GEORGIA', 'PLANNED'),
    ('VEAL_STRIPS', 'TOBIAS', 'PLANNED'),
    ('VEAL_STRIPS', 'GEORGIA', 'PLANNED'),
    ('VEAL_GOULASH', 'TOBIAS', 'PLANNED'),
    ('VEAL_GOULASH', 'GEORGIA', 'PLANNED'),
    ('VEAL_ROAST', 'TOBIAS', 'PLANNED'),
    ('VEAL_ROAST', 'GEORGIA', 'PLANNED'),
    ('VEAL_MINCE', 'TOBIAS', 'DIFFICULT'),
    ('VEAL_MINCE', 'GEORGIA', 'DIFFICULT'),
    ('VEAL_CHEEK', 'TOBIAS', 'DIFFICULT'),
    ('VEAL_CHEEK', 'GEORGIA', 'DIFFICULT'),
    ('VEAL_SWEETBREAD', 'TOBIAS', 'DIFFICULT'),
    ('VEAL_SWEETBREAD', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

-- France is a strong positive signal for veal as a broad ingredient and for
-- sweetbread in particular. The other new cuts remain deliberately unassigned:
-- ordinary availability or generic use is not enough for Issue #172.
INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, assignment.country_code
FROM (VALUES
    ('VEAL', 'FR'),
    ('VEAL_SWEETBREAD', 'FR')
) AS assignment(concept_code, country_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code;
