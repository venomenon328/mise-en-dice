--liquibase formatted sql

--changeset venomenon328:023-confectionery-role-alignment
-- CONFECTIONERY is also the explicit parent of the two approved Belgian
-- waffle concepts. Its structural roles must therefore cover their shared
-- starch function as required by the catalog graph contract.
INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM ingredient_concept concept
JOIN functional_role role ON role.code = 'STARCH'
WHERE concept.code = 'CONFECTIONERY'
ON CONFLICT DO NOTHING;
