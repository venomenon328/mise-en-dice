--liquibase formatted sql

--changeset venomenon328:023-czechia-curation
-- Issue #172: country-by-country catalog curation pass (Czechia / CZ).
-- Adds only the explicitly approved Czech associations and catalog gaps.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('BREAD_DUMPLING', 'Semmelknödel', true, true, 'SPECIFIC', 0.6500, 1,
        'Gekochter oder gedämpfter böhmisch-mitteleuropäischer Brot-/Semmelknödel als fertige Beilage; nicht Dumpling-Teig, Teighülle oder gefüllter Dumpling.'),
    ('POTATO_DUMPLING', 'Kartoffelknödel', true, true, 'SPECIFIC', 0.6500, 1,
        'Gekochter kartoffelbasierter Knödel als fertige Beilage; nicht mit Gnocchi gleichsetzen.'),
    ('FRUIT_DUMPLING', 'Obstknödel', true, true, 'OPEN', 0.4500, 2,
        'Süßer gekochter oder gedämpfter Knödel mit Fruchtfüllung; Teig kann unter anderem Hefe-, Quark- oder Kartoffelteig sein, die verwendete Frucht variiert.'),
    ('PILSNER_LAGER', 'Pilsner', true, true, 'SPECIFIC', 0.3000, 1,
        'Helles untergäriges Lagerbier nach Pilsner Art mit deutlich hopfenbetontem Profil. Gemeint ist der Bierstil, nicht eine bestimmte Marke; Pilsner Urquell ist historischer Referenztyp.'),
    ('OLOMOUC_TVARUZKY', 'Olmützer Quargel', true, true, 'SPECIFIC', 0.2500, 4,
        'Olomoucké tvarůžky: tschechischer g.g.A.-Schmierkäse aus Sauermilchquark mit höchstens 1 % Fett, kräftigem bis sehr scharfem Aroma und ausgeprägter Reifungsnote. Nicht mit beliebigem Harzer Käse oder Quark gleichsetzen.'),
    ('SLIVOVICE', 'Sliwowitz', true, true, 'SPECIFIC', 0.2500, 3,
        'Pflaumenbrand als generische Spirituosenart; nicht auf eine Marke beschränkt und nicht mit Pflaumenlikör gleichsetzen.'),
    ('PLUM_BUTTER', 'Pflaumenmus', true, true, 'SPECIFIC', 0.5500, 2,
        'Dick eingekochte Pflaumenzubereitung nach Powidl-/Pflaumenmus-Art; als Füllung, Aufstrich oder süß-fruchtige Kochzutat. Zuckerzusatz kann je nach Produkt variieren. Nicht mit Trockenpflaumen oder beliebiger Pflaumenmarmelade gleichsetzen.'),
    ('PARSLEY_ROOT', 'Petersilienwurzel', true, true, 'SPECIFIC', 0.6000, 2,
        'Essbare Wurzel der Wurzelpetersilie; aromatisches Wurzelgemüse. Nicht mit Pastinake und nicht mit Petersilienblättern gleichsetzen.'),
    ('KOLACHE', 'Kolatschen', true, true, 'OPEN', 0.3500, 3,
        'Rundes mitteleuropäisches Hefeteiggebäck der tschechischen Koláč-Familie mit typischerweise zentraler Füllung, etwa Mohn, Quark, Pflaumenmus oder Obst. Nicht beliebiges Hefegebäck und nicht Trdelník.'),
    ('MEAT_ASPIC', 'Fleischsülze', true, true, 'SPECIFIC', 0.3500, 3,
        'Kalt schnittfähiges Fleischprodukt aus gegarten Fleischstücken in natürlichem oder zugesetztem Aspik beziehungsweise Gelee; umfasst unter anderem tschechische Tlačenka und vergleichbare Fleischsülzen. Keine beliebige Fisch-, Gemüse- oder Dessertsülze.'),
    ('PICKLED_SAUSAGE', 'eingelegte Wurst', true, true, 'SPECIFIC', 0.3500, 3,
        'Verzehrfertige Wurst, die über längere Zeit in einer überwiegend essig- beziehungsweise säurebasierten Lake eingelegt wird. Umfasst unter anderem tschechischen Utopenec und vergleichbare eingelegte Wurstprodukte. Nicht bloß mit Essig angemachte Wurst und nicht nur kurz in Essigsud gegarte Wurst.');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('STARCHES', 'BREAD_DUMPLING'),
    ('POTATO', 'POTATO_DUMPLING'),
    ('STARCHES', 'FRUIT_DUMPLING'),
    ('BEER', 'PILSNER_LAGER'),
    ('CHEESE', 'OLOMOUC_TVARUZKY'),
    ('COOKING_ALCOHOL', 'SLIVOVICE'),
    ('PLUM', 'PLUM_BUTTER'),
    ('PRESERVED_PRODUCE', 'PLUM_BUTTER'),
    ('ROOT_VEGETABLES', 'PARSLEY_ROOT'),
    ('BAKED_GOODS', 'KOLACHE'),
    ('CONFECTIONERY', 'KOLACHE'),
    ('MEAT', 'MEAT_ASPIC'),
    ('SAUSAGE', 'PICKLED_SAUSAGE')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('BREAD_DUMPLING', 'STARCH'),
    ('POTATO_DUMPLING', 'STARCH'),
    ('FRUIT_DUMPLING', 'STARCH'),
    ('FRUIT_DUMPLING', 'FRUIT'),
    ('PILSNER_LAGER', 'ACID'),
    ('PILSNER_LAGER', 'SEASONING'),
    ('OLOMOUC_TVARUZKY', 'ANIMAL_PROTEIN'),
    ('OLOMOUC_TVARUZKY', 'SEASONING'),
    ('SLIVOVICE', 'ACID'),
    ('SLIVOVICE', 'SEASONING'),
    ('PLUM_BUTTER', 'FRUIT'),
    ('PLUM_BUTTER', 'SEASONING'),
    ('PARSLEY_ROOT', 'VEGETABLE'),
    ('PARSLEY_ROOT', 'AROMATIC'),
    ('KOLACHE', 'STARCH'),
    ('KOLACHE', 'FAT'),
    ('MEAT_ASPIC', 'ANIMAL_PROTEIN'),
    ('MEAT_ASPIC', 'FAT'),
    ('PICKLED_SAUSAGE', 'ANIMAL_PROTEIN'),
    ('PICKLED_SAUSAGE', 'FAT'),
    ('PICKLED_SAUSAGE', 'ACID')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('BREAD_DUMPLING', 'DOMINANCE', 2),
    ('BREAD_DUMPLING', 'SWEETNESS', 1),
    ('BREAD_DUMPLING', 'SALTINESS', 2),
    ('POTATO_DUMPLING', 'DOMINANCE', 2),
    ('POTATO_DUMPLING', 'SWEETNESS', 1),
    ('FRUIT_DUMPLING', 'DOMINANCE', 3),
    ('FRUIT_DUMPLING', 'SWEETNESS', 4),
    ('FRUIT_DUMPLING', 'ACIDITY', 3),
    ('PILSNER_LAGER', 'DOMINANCE', 3),
    ('PILSNER_LAGER', 'SWEETNESS', 1),
    ('PILSNER_LAGER', 'ACIDITY', 2),
    ('PILSNER_LAGER', 'BITTERNESS', 4),
    ('OLOMOUC_TVARUZKY', 'DOMINANCE', 5),
    ('OLOMOUC_TVARUZKY', 'ACIDITY', 3),
    ('OLOMOUC_TVARUZKY', 'FATTINESS', 1),
    ('OLOMOUC_TVARUZKY', 'UMAMI', 5),
    ('OLOMOUC_TVARUZKY', 'SALTINESS', 4),
    ('SLIVOVICE', 'DOMINANCE', 4),
    ('SLIVOVICE', 'SWEETNESS', 1),
    ('SLIVOVICE', 'ACIDITY', 2),
    ('SLIVOVICE', 'BITTERNESS', 2),
    ('PLUM_BUTTER', 'DOMINANCE', 4),
    ('PLUM_BUTTER', 'SWEETNESS', 5),
    ('PLUM_BUTTER', 'ACIDITY', 3),
    ('PARSLEY_ROOT', 'DOMINANCE', 3),
    ('PARSLEY_ROOT', 'SWEETNESS', 2),
    ('PARSLEY_ROOT', 'BITTERNESS', 2),
    ('KOLACHE', 'DOMINANCE', 3),
    ('KOLACHE', 'SWEETNESS', 4),
    ('KOLACHE', 'FATTINESS', 3),
    ('MEAT_ASPIC', 'DOMINANCE', 4),
    ('MEAT_ASPIC', 'FATTINESS', 4),
    ('MEAT_ASPIC', 'UMAMI', 4),
    ('MEAT_ASPIC', 'SALTINESS', 3),
    ('PICKLED_SAUSAGE', 'DOMINANCE', 4),
    ('PICKLED_SAUSAGE', 'ACIDITY', 4),
    ('PICKLED_SAUSAGE', 'FATTINESS', 4),
    ('PICKLED_SAUSAGE', 'UMAMI', 4),
    ('PICKLED_SAUSAGE', 'SALTINESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('PILSNER_LAGER', 'FERMENTED'),
    ('OLOMOUC_TVARUZKY', 'FERMENTED'),
    ('OLOMOUC_TVARUZKY', 'CURED'),
    ('PICKLED_SAUSAGE', 'PICKLED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('BREAD_DUMPLING', 'TOBIAS', 'EASY'),
    ('BREAD_DUMPLING', 'GEORGIA', 'EASY'),
    ('POTATO_DUMPLING', 'TOBIAS', 'EASY'),
    ('POTATO_DUMPLING', 'GEORGIA', 'EASY'),
    ('FRUIT_DUMPLING', 'TOBIAS', 'PLANNED'),
    ('FRUIT_DUMPLING', 'GEORGIA', 'PLANNED'),
    ('PILSNER_LAGER', 'TOBIAS', 'EASY'),
    ('PILSNER_LAGER', 'GEORGIA', 'EASY'),
    ('OLOMOUC_TVARUZKY', 'TOBIAS', 'DIFFICULT'),
    ('OLOMOUC_TVARUZKY', 'GEORGIA', 'PLANNED'),
    ('SLIVOVICE', 'TOBIAS', 'PLANNED'),
    ('SLIVOVICE', 'GEORGIA', 'PLANNED'),
    ('PLUM_BUTTER', 'TOBIAS', 'EASY'),
    ('PLUM_BUTTER', 'GEORGIA', 'EASY'),
    ('PARSLEY_ROOT', 'TOBIAS', 'PLANNED'),
    ('PARSLEY_ROOT', 'GEORGIA', 'PLANNED'),
    ('KOLACHE', 'TOBIAS', 'DIFFICULT'),
    ('KOLACHE', 'GEORGIA', 'PLANNED'),
    ('MEAT_ASPIC', 'TOBIAS', 'EASY'),
    ('MEAT_ASPIC', 'GEORGIA', 'EASY'),
    ('PICKLED_SAUSAGE', 'TOBIAS', 'DIFFICULT'),
    ('PICKLED_SAUSAGE', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'CZ'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'BEER',
    'PORK',
    'PORK_KNUCKLE',
    'SAUSAGE',
    'LARD',
    'POTATO',
    'SAUERKRAUT',
    'WHITE_CABBAGE',
    'RED_CABBAGE',
    'DUCK',
    'CARP',
    'CARAWAY',
    'MARJORAM',
    'DILL',
    'MUSHROOMS',
    'DRIED_WILD_MUSHROOMS',
    'QUARK',
    'POPPY_SEEDS',
    'PLUM',
    'RYE_BREAD',
    'HORSERADISH',
    'CELERIAC',
    'PICKLED_CUCUMBER',
    'PEARL_BARLEY',
    'LOVAGE',
    'BREAD_DUMPLING',
    'POTATO_DUMPLING',
    'FRUIT_DUMPLING',
    'PILSNER_LAGER',
    'OLOMOUC_TVARUZKY',
    'SLIVOVICE',
    'PLUM_BUTTER',
    'PARSLEY_ROOT',
    'KOLACHE',
    'MEAT_ASPIC',
    'PICKLED_SAUSAGE'
);
