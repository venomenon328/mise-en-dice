--liquibase formatted sql

--changeset mise-en-dice:023-ingredient-concept-aliases splitStatements:false
CREATE TABLE ingredient_concept_alias (
    ingredient_concept_id bigint NOT NULL REFERENCES ingredient_concept (id) ON DELETE CASCADE,
    alias_text            text NOT NULL,
    CONSTRAINT ck_ingredient_concept_alias_trimmed_nonblank
        CHECK (alias_text = btrim(alias_text) AND alias_text <> '')
);

CREATE UNIQUE INDEX uq_ingredient_concept_alias_concept_text_ci
    ON ingredient_concept_alias (ingredient_concept_id, lower(alias_text));

CREATE INDEX ix_ingredient_concept_alias_text_ci
    ON ingredient_concept_alias (lower(alias_text));

CREATE FUNCTION enforce_ingredient_concept_alias_own_name()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ingredient_concept concept
        WHERE concept.id = NEW.ingredient_concept_id
          AND lower(btrim(concept.display_name)) = lower(btrim(NEW.alias_text))
    ) THEN
        RAISE EXCEPTION 'ingredient concept alias duplicates its canonical display name'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER ingredient_concept_alias_own_name_guard
BEFORE INSERT OR UPDATE ON ingredient_concept_alias
FOR EACH ROW
EXECUTE FUNCTION enforce_ingredient_concept_alias_own_name();

CREATE FUNCTION enforce_ingredient_concept_display_name_not_alias()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ingredient_concept_alias alias
        WHERE alias.ingredient_concept_id = NEW.id
          AND lower(btrim(alias.alias_text)) = lower(btrim(NEW.display_name))
    ) THEN
        RAISE EXCEPTION 'ingredient concept display name duplicates one of its aliases'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER ingredient_concept_display_name_alias_guard
BEFORE UPDATE OF display_name ON ingredient_concept
FOR EACH ROW
EXECUTE FUNCTION enforce_ingredient_concept_display_name_not_alias();

--rollback DROP TRIGGER ingredient_concept_display_name_alias_guard ON ingredient_concept;
--rollback DROP FUNCTION enforce_ingredient_concept_display_name_not_alias();
--rollback DROP TRIGGER ingredient_concept_alias_own_name_guard ON ingredient_concept_alias;
--rollback DROP FUNCTION enforce_ingredient_concept_alias_own_name();
--rollback DROP TABLE ingredient_concept_alias;
