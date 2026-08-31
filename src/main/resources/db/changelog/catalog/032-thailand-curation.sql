--liquibase formatted sql

--changeset venomenon328:032-thailand-curation
-- Issue #172: country-by-country catalog curation pass (Thailand / TH).
-- Adds only the explicitly approved Thai associations, catalog gaps and metadata.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('HOLY_BASIL', 'Krapao', true, true, 'SPECIFIC', 0.3000, 4,
        'Frische Blätter von Ocimum tenuiflorum mit pfeffrig-nelkenartigem Aroma, besonders für Pad Krapao. Nicht mit Thai-Basilikum verwechseln; getrockneter Tulsi-Tee ist keine gleichwertige Kochzutat.'),
    ('PLA_RA', 'Pla Ra', true, true, 'OPEN', 0.2200, 5,
        'Thailändischer, besonders mit Isan verbundener salzfermentierter Süßwasserfisch; je nach Produkt als Fischstücke, Paste oder gekochte flüssige Würze erhältlich. Nicht Fischsauce oder Garnelenpaste.'),
    ('FINGERROOT', 'Fingerwurz', true, true, 'SPECIFIC', 0.3000, 4,
        'Fingerartige Rhizome von Boesenbergia rotunda mit erdig-zitrischem, leicht pfeffrig-kampferartigem Aroma; frisch, tiefgekühlt oder in Lake verwendbar. Nicht Ingwer oder Galgant.'),
    ('THAI_EGGPLANT', 'Thai-Aubergine', true, true, 'SPECIFIC', 0.3000, 4,
        'Kleine runde Thai-Auberginen, meist grün-weiß und fest, die in Currys ihre Form behalten und leicht herb schmecken. Nicht mit Erbsenauberginen gleichsetzen.'),
    ('PEA_EGGPLANT', 'Erbsenaubergine', true, true, 'SPECIFIC', 0.1800, 5,
        'Erbsengroße, traubenartig wachsende Früchte von Solanum torvum mit knackigem Biss und deutlich herber Note, besonders für Currys. Keine Miniaturform der gewöhnlichen Aubergine.'),
    ('CORIANDER_ROOT', 'Korianderwurzel', true, true, 'SPECIFIC', 0.2500, 4,
        'Wurzelansatz des Korianders mit konzentriert erdig-pfeffrigem Aroma, besonders für Würzpasten und Marinaden. Gemeint sind frische oder tiefgekühlte Wurzeln, nicht Koriandergrün oder -saat.'),
    ('NAM_PRIK_PAO', 'Nam Prik Pao', true, true, 'SPECIFIC', 0.4000, 3,
        'Geröstete thailändische Chilipaste mit Öl, süßlich-rauchigem Profil und kräftigem Umami; häufig mit Schalotten, Knoblauch, Tamarinde sowie Garnelen- oder Fischbestandteilen. Nicht bloß Chiliöl oder Sriracha.'),
    ('SAI_UA', 'Sai Ua', true, true, 'SPECIFIC', 0.2500, 4,
        'Nordthailändische grobe Schweinewurst mit Zitronengras, Makrut-Limettenblatt, Chili und weiteren Kräutern; meist gegrillt und aromatisch-intensiv. Nicht mit der fermentierten Sai Krok Isan gleichsetzen.'),
    ('TAI_PLA', 'Tai Pla', true, true, 'SPECIFIC', 0.1200, 5,
        'Sehr intensive südthailändische Würze aus salzfermentierten Fischinnereien, vor allem als Basis für Kaeng Tai Pla. Nicht Fischsauce, Pla Ra oder bloß rohe Fischinnereien.');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('BASIL', 'HOLY_BASIL'),
    ('FERMENTED_SEASONINGS', 'PLA_RA'),
    ('READY_SAUCES_AND_PASTES', 'PLA_RA'),
    ('SPICES', 'FINGERROOT'),
    ('EGGPLANT', 'THAI_EGGPLANT'),
    ('FRUIT_VEGETABLES', 'PEA_EGGPLANT'),
    ('SPICES', 'CORIANDER_ROOT'),
    ('CHILI_CONDIMENTS', 'NAM_PRIK_PAO'),
    ('SAUSAGE', 'SAI_UA'),
    ('PORK', 'SAI_UA'),
    ('FERMENTED_SEASONINGS', 'TAI_PLA'),
    ('READY_SAUCES_AND_PASTES', 'TAI_PLA')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('HOLY_BASIL', 'AROMATIC'),
    ('PLA_RA', 'SEASONING'),
    ('FINGERROOT', 'AROMATIC'),
    ('FINGERROOT', 'SEASONING'),
    ('THAI_EGGPLANT', 'VEGETABLE'),
    ('PEA_EGGPLANT', 'VEGETABLE'),
    ('CORIANDER_ROOT', 'AROMATIC'),
    ('CORIANDER_ROOT', 'SEASONING'),
    ('NAM_PRIK_PAO', 'FAT'),
    ('NAM_PRIK_PAO', 'AROMATIC'),
    ('NAM_PRIK_PAO', 'SEASONING'),
    ('SAI_UA', 'ANIMAL_PROTEIN'),
    ('SAI_UA', 'FAT'),
    ('SAI_UA', 'SEASONING'),
    ('TAI_PLA', 'SEASONING')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('HOLY_BASIL', 'DOMINANCE', 4),
    ('HOLY_BASIL', 'BITTERNESS', 2),
    ('PLA_RA', 'DOMINANCE', 5),
    ('PLA_RA', 'UMAMI', 5),
    ('PLA_RA', 'SALTINESS', 5),
    ('FINGERROOT', 'DOMINANCE', 4),
    ('FINGERROOT', 'BITTERNESS', 2),
    ('FINGERROOT', 'HEAT', 2),
    ('THAI_EGGPLANT', 'DOMINANCE', 2),
    ('THAI_EGGPLANT', 'SWEETNESS', 1),
    ('THAI_EGGPLANT', 'BITTERNESS', 2),
    ('PEA_EGGPLANT', 'DOMINANCE', 3),
    ('PEA_EGGPLANT', 'SWEETNESS', 1),
    ('PEA_EGGPLANT', 'BITTERNESS', 3),
    ('CORIANDER_ROOT', 'DOMINANCE', 4),
    ('CORIANDER_ROOT', 'BITTERNESS', 2),
    ('NAM_PRIK_PAO', 'DOMINANCE', 5),
    ('NAM_PRIK_PAO', 'SWEETNESS', 4),
    ('NAM_PRIK_PAO', 'FATTINESS', 4),
    ('NAM_PRIK_PAO', 'HEAT', 3),
    ('NAM_PRIK_PAO', 'UMAMI', 5),
    ('NAM_PRIK_PAO', 'SALTINESS', 4),
    ('SAI_UA', 'DOMINANCE', 5),
    ('SAI_UA', 'FATTINESS', 4),
    ('SAI_UA', 'HEAT', 3),
    ('SAI_UA', 'UMAMI', 4),
    ('SAI_UA', 'SALTINESS', 3),
    ('TAI_PLA', 'DOMINANCE', 5),
    ('TAI_PLA', 'UMAMI', 5),
    ('TAI_PLA', 'SALTINESS', 5)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('PLA_RA', 'FERMENTED'),
    ('TAI_PLA', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('HOLY_BASIL', 'TOBIAS', 'DIFFICULT'),
    ('HOLY_BASIL', 'GEORGIA', 'PLANNED'),
    ('PLA_RA', 'TOBIAS', 'DIFFICULT'),
    ('PLA_RA', 'GEORGIA', 'PLANNED'),
    ('FINGERROOT', 'TOBIAS', 'DIFFICULT'),
    ('FINGERROOT', 'GEORGIA', 'PLANNED'),
    ('THAI_EGGPLANT', 'TOBIAS', 'DIFFICULT'),
    ('THAI_EGGPLANT', 'GEORGIA', 'PLANNED'),
    ('PEA_EGGPLANT', 'TOBIAS', 'DIFFICULT'),
    ('PEA_EGGPLANT', 'GEORGIA', 'PLANNED'),
    ('CORIANDER_ROOT', 'TOBIAS', 'DIFFICULT'),
    ('CORIANDER_ROOT', 'GEORGIA', 'PLANNED'),
    ('NAM_PRIK_PAO', 'TOBIAS', 'PLANNED'),
    ('NAM_PRIK_PAO', 'GEORGIA', 'PLANNED'),
    ('SAI_UA', 'TOBIAS', 'DIFFICULT'),
    ('SAI_UA', 'GEORGIA', 'PLANNED'),
    ('TAI_PLA', 'TOBIAS', 'DIFFICULT'),
    ('TAI_PLA', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, assignment.include_refinements
FROM (VALUES
    ('NO_FISH_OR_SEAFOOD', 'PLA_RA', false),
    ('NO_FISH_OR_SEAFOOD', 'TAI_PLA', false)
) AS assignment(rule_code, concept_code, include_refinements)
JOIN exclusion_rule rule
  ON rule.code = assignment.rule_code
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'TH'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'FISH_SAUCE',
    'RICE',
    'JASMINE_RICE',
    'STICKY_RICE',
    'NOODLES',
    'RICE_NOODLES',
    'LEMONGRASS',
    'GALANGAL',
    'KAFFIR_LIME_LEAVES',
    'CHILI',
    'BIRDS_EYE_CHILI',
    'LIME',
    'GARLIC',
    'SHALLOT',
    'CILANTRO',
    'THAI_BASIL',
    'SHRIMP_PASTE',
    'COCONUT_MILK',
    'COCONUT_CREAM',
    'TAMARIND',
    'PALM_SUGAR',
    'THAI_GREEN_CURRY_PASTE',
    'THAI_RED_CURRY_PASTE',
    'THAI_YELLOW_CURRY_PASTE',
    'MASSAMAN_CURRY_PASTE',
    'GREEN_PAPAYA',
    'DRIED_SHRIMP',
    'BEAN_SPROUTS',
    'PEANUT',
    'TOFU',
    'PANDAN_LEAVES',
    'MANGO',
    'WATER_SPINACH',
    'TURMERIC',
    'SOY_SAUCE',
    'OYSTER_SAUCE',
    'SRIRACHA',
    'FISH',
    'SHRIMP',
    'PORK',
    'CHICKEN',
    'HOLY_BASIL',
    'PLA_RA',
    'FINGERROOT',
    'THAI_EGGPLANT',
    'PEA_EGGPLANT',
    'CORIANDER_ROOT',
    'NAM_PRIK_PAO',
    'SAI_UA',
    'TAI_PLA'
);