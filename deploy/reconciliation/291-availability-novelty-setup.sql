BEGIN;

LOCK TABLE databasechangelog IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE ingredient_concept, ingredient_availability, participant IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE issue_291_availability_novelty_source (
    concept_code text,
    display_name text,
    review_applicability text,
    cooking_novelty text,
    availability_georgia text,
    availability_tobias text,
    availability_note_georgia text,
    availability_note_tobias text,
    novelty_approval_origin text,
    availability_georgia_approval_origin text,
    availability_tobias_approval_origin text,
    availability_note_georgia_approval_origin text,
    availability_note_tobias_approval_origin text,
    overall_approval_origin text,
    final_acceptance_status text,
    base_draw_weight_review text,
    source_provenance text,
    catalog_commit text,
    review_version text
) ON COMMIT DROP;
