--liquibase formatted sql

--changeset venomenon328:022-remove-runtime-catalog-audit
DROP TABLE catalog_audit_entry;
