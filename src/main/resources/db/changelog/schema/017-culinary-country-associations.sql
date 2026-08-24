--liquibase formatted sql
--changeset venomenon328:017-culinary-country-associations splitStatements:false

-- Curated positive associations between one ingredient concept and zero or more ISO countries.
-- No relation is inferred from the ingredient refinement graph.
CREATE TABLE culinary_country (
    code          varchar(2) PRIMARY KEY,
    display_name  varchar(120) NOT NULL,

    CONSTRAINT ck_culinary_country_code CHECK (code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_culinary_country_display_name CHECK (display_name = btrim(display_name) AND display_name <> '')
);

CREATE TABLE ingredient_culinary_country (
    ingredient_concept_id  bigint NOT NULL REFERENCES ingredient_concept(id) ON DELETE RESTRICT,
    country_code           varchar(2) NOT NULL REFERENCES culinary_country(code) ON DELETE RESTRICT,

    PRIMARY KEY (ingredient_concept_id, country_code)
);

CREATE INDEX ix_ingredient_culinary_country_country
    ON ingredient_culinary_country (country_code, ingredient_concept_id);
