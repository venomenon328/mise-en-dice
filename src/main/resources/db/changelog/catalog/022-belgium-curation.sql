--liquibase formatted sql

--changeset venomenon328:022-belgium-curation
-- Issue #172: country-by-country catalog curation pass (Belgium / BE).
-- Adds only the explicitly approved Belgian associations and catalog gaps,
-- plus the approved reusable baked-goods, waffles and syrup hierarchy.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('BAKED_GOODS', 'Backware', true, false, 'OPEN', 1.0000, 1,
        'Nicht ziehbarer Strukturknoten für gebackene beziehungsweise vergleichbar ausgebackene stärkehaltige Produkte; keine Aussage über süß oder herzhaft.'),
    ('WAFFLES', 'Waffel', true, true, 'OPEN', 0.5500, 1,
        'Offene Waffelfamilie für süße und herzhafte Waffelprodukte. Bekannte Konkretisierungen umfassen Stroopwafel, Brüsseler Waffel und Lütticher Waffel; Belag oder Füllung werden nicht vorausgesetzt.'),
    ('SYRUP', 'Sirup', true, true, 'OPEN', 0.5000, 1,
        'Offene Familie dick- oder dünnflüssiger konzentrierter Sirupe zur Süßung oder Würzung. Umfasst pflanzen- und fruchtbasierte Sirupe sowie vergleichbare konzentrierte Fruchtsirupe; nicht jedes flüssige Süßungsmittel ist automatisch ein Sirup.'),
    ('NORTH_SEA_SHRIMP', 'Nordseekrabbe', true, true, 'SPECIFIC', 0.4500, 2,
        'Kleine graue Nordseegarnele (Crangon crangon), in Deutschland üblicherweise als Nordseekrabbe bezeichnet; typischerweise bereits gekocht und geschält erhältlich. Nicht beliebige Garnelen und trotz des deutschen Namens keine echte Krabbe.'),
    ('HERVE_CHEESE', 'Herve-Käse', true, true, 'SPECIFIC', 0.3000, 4,
        'Belgischer AOP-Weichkäse aus Kuhmilch mit gewaschener Rinde. Umfasst die mildere und die kräftigere/pikante Herve-Ausprägung; charakteristisch würzig und teils sehr intensiv. Nicht mit beliebigem Limburger oder Romadur gleichsetzen.'),
    ('LIEGE_SYRUP', 'Sirop de Liège', true, true, 'SPECIFIC', 0.3500, 3,
        'Dick eingekochter belgischer Apfel-/Birnen-Fruchtsirup beziehungsweise Fruchtaufstrich nach Lütticher Tradition. Kein Markenbegriff; Zuckerzusatz und konkrete Fruchtanteile können variieren. Nicht mit dünnflüssigem Getränkesirup oder beliebigem Fruchtsirup gleichsetzen.'),
    ('LAMBIC', 'Lambic', true, true, 'SPECIFIC', 0.3000, 3,
        'Traditioneller belgischer Bierstil aus spontan vergorener Würze. Gemeint ist Lambic als eigenständiger Bierstil beziehungsweise Basis; Gueuze, Kriek und andere Frucht- oder Blendvarianten werden dadurch nicht automatisch als eigene Katalogkonzepte modelliert.'),
    ('SPECULOOS', 'Spekulatius', true, true, 'SPECIFIC', 0.5000, 1,
        'Knuspriges karamellisiertes Gewürzgebäck der Spekulatius-/Speculoos-Familie. Gemeint ist der fertige Keks, nicht eine Gewürzmischung; konkrete Gewürzzusammensetzung und Fettquelle können variieren.'),
    ('FRENCH_FRIES', 'Pommes frites', true, true, 'SPECIFIC', 0.6500, 1,
        'Pommes frites als eigenständige Kartoffel-Produktform: in Stäbchen geschnittene und frittierte beziehungsweise typischerweise vorfrittierte Kartoffeln. Umfasst auch handelsübliche tiefgekühlte Pommes; nicht beliebige Bratkartoffeln, Kartoffelspalten oder Chips.'),
    ('BRUSSELS_WAFFLE', 'Brüsseler Waffel', true, true, 'SPECIFIC', 0.4000, 2,
        'Große rechteckige belgische Waffel nach Brüsseler Art, außen knusprig und innen luftig. Das Konzept bezeichnet die Waffel selbst; Puderzucker, Schokolade, Obst, Sahne oder andere Beläge werden nicht vorausgesetzt. Nicht mit der kompakteren Lütticher Zuckerwaffel gleichsetzen.'),
    ('LIEGE_WAFFLE', 'Lütticher Waffel', true, true, 'SPECIFIC', 0.4000, 2,
        'Kleinere, kompakte belgische Waffel nach Lütticher Art mit eingebackenem Perl- beziehungsweise Hagelzucker und typischerweise karamellisierten Rändern. Nicht mit der größeren, luftigen Brüsseler Waffel gleichsetzen.');

-- Introduce a reusable syrup family between SWEETENERS and the existing syrup
-- concepts. This replaces the former direct edges without changing the
-- transitive "is a sweetener" semantics.
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'SWEETENERS'
  AND child.code IN ('AGAVE_SYRUP', 'DATE_SYRUP', 'MAPLE_SYRUP', 'POMEGRANATE_MOLASSES');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('BAKED_GOODS', 'BREAD'),
    ('BAKED_GOODS', 'WAFFLES'),
    ('SWEETENERS', 'SYRUP'),
    ('SYRUP', 'AGAVE_SYRUP'),
    ('SYRUP', 'DATE_SYRUP'),
    ('SYRUP', 'MAPLE_SYRUP'),
    ('SYRUP', 'POMEGRANATE_MOLASSES'),
    ('SYRUP', 'LIEGE_SYRUP'),
    ('PRESERVED_PRODUCE', 'LIEGE_SYRUP'),
    ('SHRIMP', 'NORTH_SEA_SHRIMP'),
    ('CHEESE', 'HERVE_CHEESE'),
    ('BEER', 'LAMBIC'),
    ('CONFECTIONERY', 'SPECULOOS'),
    ('POTATO', 'FRENCH_FRIES'),
    ('WAFFLES', 'STROOPWAFEL'),
    ('WAFFLES', 'BRUSSELS_WAFFLE'),
    ('WAFFLES', 'LIEGE_WAFFLE'),
    ('CONFECTIONERY', 'BRUSSELS_WAFFLE'),
    ('CONFECTIONERY', 'LIEGE_WAFFLE')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('BAKED_GOODS', 'STARCH'),
    ('WAFFLES', 'STARCH'),
    ('SYRUP', 'SEASONING'),
    ('NORTH_SEA_SHRIMP', 'ANIMAL_PROTEIN'),
    ('HERVE_CHEESE', 'ANIMAL_PROTEIN'),
    ('HERVE_CHEESE', 'FAT'),
    ('HERVE_CHEESE', 'SEASONING'),
    ('LIEGE_SYRUP', 'FRUIT'),
    ('LIEGE_SYRUP', 'ACID'),
    ('LIEGE_SYRUP', 'SEASONING'),
    ('LAMBIC', 'ACID'),
    ('LAMBIC', 'SEASONING'),
    ('SPECULOOS', 'STARCH'),
    ('SPECULOOS', 'FAT'),
    ('SPECULOOS', 'AROMATIC'),
    ('SPECULOOS', 'SEASONING'),
    ('FRENCH_FRIES', 'STARCH'),
    ('FRENCH_FRIES', 'FAT'),
    ('BRUSSELS_WAFFLE', 'STARCH'),
    ('BRUSSELS_WAFFLE', 'FAT'),
    ('LIEGE_WAFFLE', 'STARCH'),
    ('LIEGE_WAFFLE', 'FAT')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('WAFFLES', 'DOMINANCE', 2),
    ('SYRUP', 'DOMINANCE', 3),
    ('SYRUP', 'SWEETNESS', 5),
    ('NORTH_SEA_SHRIMP', 'DOMINANCE', 3),
    ('NORTH_SEA_SHRIMP', 'SWEETNESS', 2),
    ('NORTH_SEA_SHRIMP', 'UMAMI', 4),
    ('NORTH_SEA_SHRIMP', 'SALTINESS', 3),
    ('HERVE_CHEESE', 'DOMINANCE', 5),
    ('HERVE_CHEESE', 'ACIDITY', 2),
    ('HERVE_CHEESE', 'BITTERNESS', 2),
    ('HERVE_CHEESE', 'FATTINESS', 4),
    ('HERVE_CHEESE', 'UMAMI', 4),
    ('HERVE_CHEESE', 'SALTINESS', 3),
    ('LIEGE_SYRUP', 'DOMINANCE', 4),
    ('LIEGE_SYRUP', 'SWEETNESS', 5),
    ('LIEGE_SYRUP', 'ACIDITY', 3),
    ('LAMBIC', 'DOMINANCE', 4),
    ('LAMBIC', 'SWEETNESS', 1),
    ('LAMBIC', 'ACIDITY', 5),
    ('LAMBIC', 'BITTERNESS', 2),
    ('SPECULOOS', 'DOMINANCE', 4),
    ('SPECULOOS', 'SWEETNESS', 4),
    ('SPECULOOS', 'BITTERNESS', 2),
    ('SPECULOOS', 'FATTINESS', 3),
    ('FRENCH_FRIES', 'DOMINANCE', 2),
    ('FRENCH_FRIES', 'SWEETNESS', 1),
    ('FRENCH_FRIES', 'FATTINESS', 4),
    ('BRUSSELS_WAFFLE', 'DOMINANCE', 2),
    ('BRUSSELS_WAFFLE', 'SWEETNESS', 2),
    ('BRUSSELS_WAFFLE', 'FATTINESS', 3),
    ('LIEGE_WAFFLE', 'DOMINANCE', 3),
    ('LIEGE_WAFFLE', 'SWEETNESS', 5),
    ('LIEGE_WAFFLE', 'FATTINESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('HERVE_CHEESE', 'FERMENTED'),
    ('HERVE_CHEESE', 'CURED'),
    ('LAMBIC', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('WAFFLES', 'TOBIAS', 'EASY'),
    ('WAFFLES', 'GEORGIA', 'EASY'),
    ('SYRUP', 'TOBIAS', 'EASY'),
    ('SYRUP', 'GEORGIA', 'EASY'),
    ('NORTH_SEA_SHRIMP', 'TOBIAS', 'EASY'),
    ('NORTH_SEA_SHRIMP', 'GEORGIA', 'PLANNED'),
    ('HERVE_CHEESE', 'TOBIAS', 'PLANNED'),
    ('HERVE_CHEESE', 'GEORGIA', 'PLANNED'),
    ('LIEGE_SYRUP', 'TOBIAS', 'DIFFICULT'),
    ('LIEGE_SYRUP', 'GEORGIA', 'PLANNED'),
    ('LAMBIC', 'TOBIAS', 'DIFFICULT'),
    ('LAMBIC', 'GEORGIA', 'PLANNED'),
    ('SPECULOOS', 'TOBIAS', 'EASY'),
    ('SPECULOOS', 'GEORGIA', 'EASY'),
    ('FRENCH_FRIES', 'TOBIAS', 'EASY'),
    ('FRENCH_FRIES', 'GEORGIA', 'EASY'),
    ('BRUSSELS_WAFFLE', 'TOBIAS', 'PLANNED'),
    ('BRUSSELS_WAFFLE', 'GEORGIA', 'PLANNED'),
    ('LIEGE_WAFFLE', 'TOBIAS', 'DIFFICULT'),
    ('LIEGE_WAFFLE', 'GEORGIA', 'PLANNED')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'BE'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'BEER',
    'CHOCOLATE',
    'CHICORY',
    'MUSSELS',
    'JENEVER',
    'EEL',
    'NORTH_SEA_SHRIMP',
    'HERVE_CHEESE',
    'LIEGE_SYRUP',
    'LAMBIC',
    'SPECULOOS',
    'FRENCH_FRIES',
    'BRUSSELS_WAFFLE',
    'LIEGE_WAFFLE'
);
