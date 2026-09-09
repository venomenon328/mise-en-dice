# ADR 0010: Runtime-Katalogaudit entfernen, Optimistic Locking behalten

- Status: angenommen und umgesetzt
- Datum: 9. September 2026
- Entscheidungsträger: Projektverantwortlicher, Issue #221
- Ersetzt die auditbezogenen Zielentscheidungen der privaten Katalogverwaltung; Security, Optimistic Locking, Transaktions- und Integritätsverträge bleiben bestehen.

## Kontext

Die private Katalogverwaltung wurde mit einem vollständigen Runtime-Audit aufgebaut. Erfolgreiche Änderungen an Zutatenkonzepten, Konkretisierungsbeziehungen, Ausschlussregeln und Bulk-Aktionen erzeugten fachliche Vorher-/Nachher-Snapshots in `catalog_audit_entry`. Die Administration bot dafür einen eigenen Bereich `Änderungen`, Entity-Historien und feldweise Diffs. Auditfehler waren Teil der Schreibtransaktion und rollten deshalb auch die eigentliche Katalogänderung zurück.

Dieser Vertrag wurde für eine nachvollziehbare Mehrnutzerpflege entworfen. Der reale Betrieb ist jedoch ein privates System mit sehr kleinem bekannten Nutzerkreis. Katalogänderungen erfolgen kontrolliert über die Anwendung, migrations-/automatisierungsgeführt oder bewusst durch den Betreiber. Für diesen Anwendungsfall rechtfertigen Nachher-Snapshots, Diff-/Query-Infrastruktur, UI, zusätzliche Datenbankobjekte und umfangreiche Audittests ihren dauerhaften Pflegeaufwand nicht mehr.

Die Vereinfachung soll ausdrücklich **nicht** auf Kosten der weiterhin relevanten Konkurrenz- und Datenintegritätssicherung gehen. Optimistic Locking, atomare Transaktionen, PostgreSQL-Constraints, Graphlock und Liquibase erfüllen andere Zwecke als eine historische Änderungsliste.

Es existiert eine produktive PostgreSQL-Datenbank. Bereits veröffentlichte Liquibase-Changesets können deshalb nicht rückwirkend auf einen auditfreien historischen Zustand umgeschrieben werden.

## Entscheidung

Der Runtime-Katalogaudit wird vollständig und ersatzlos entfernt.

Mit Issue #221 gilt:

- `catalog_audit_entry` und ausschließlich dazugehörige Indizes beziehungsweise weitere reine Auditobjekte existieren nicht mehr im aktuellen Schema,
- Katalog-, Relations-, Ausschluss- und Bulk-Commands schreiben keine Audit-Einträge und erzeugen keine Audit-Snapshots oder -Diffs,
- ein erfolgreicher fachlicher Write hängt nicht mehr von einem nachgelagerten Audit-Insert ab,
- öffentliche und interne Catalog-Audit-APIs sowie ausschließlich auditbezogene Persistenzadapter entfallen,
- `/admin/audit`, der Navigationspunkt `Änderungen`, Entity-Historien und Audit-Diffdarstellungen entfallen vollständig,
- historische Inhalte aus `catalog_audit_entry` werden beim Schema-Cleanup ohne Export oder Archivierung gelöscht,
- `actor_key` oder vergleichbare Command-Parameter entfallen, soweit nach Entfernung des Audits kein anderer echter Consumer verbleibt.

Ausdrücklich erhalten bleiben:

- `ingredient_concept.version` und `exclusion_rule.version`,
- die bestehende Optimistic-Locking- und `409 Conflict`-Semantik,
- atomare Application-Transaktionen und Rollback bei tatsächlichen Persistenz-/Integritätsfehlern,
- PostgreSQL-Advisory-Lock, Graphvalidierung, Trigger, Constraints und weitere Datenintegritätsregeln,
- Administrationsauthentifizierung und die vom Teilnehmermodell getrennte Security-Identität,
- Liquibase einschließlich `DATABASECHANGELOG` und `DATABASECHANGELOGLOCK`,
- die append-only-Historie bereits veröffentlichter Changesets,
- Challenge-, Offer-, Voting-, Generator-, Ergebnis- und sonstige fachlich benötigte Historien beziehungsweise Snapshots.

Der Begriff `Audit` in anderen Modulen bleibt zulässig, wenn dort ein eigenständiger produktiver Zweck besteht, etwa Provider-/Dispatchdiagnose. Diese ADR betrifft ausschließlich den **Runtime-Katalogaudit der redaktionellen Katalogverwaltung**.

## Produktions- und Migrationsstrategie

Die bereits veröffentlichte Migration `schema/003-administration-foundation.sql` und alle späteren Changesets bleiben unverändert. Das gilt auch für spätere Katalogmigrationen, die aufgrund des damaligen Vertrags Auditzeilen erzeugen.

Die Entfernung erfolgt ausschließlich vorwärtsgerichtet über das neue append-only Liquibase-Changeset `schema/022-remove-runtime-catalog-audit.sql`. Dieses entfernt den vorhandenen Auditbestand, ohne andere Produktionsdaten oder fachliche Historien anzutasten.

Damit darf eine frisch aufgebaute Datenbank historisch zunächst die Auditstruktur anlegen und gegebenenfalls Auditdaten erzeugen, bevor das aktuelle Cleanup-Changeset sie wieder entfernt. Dieser zusätzliche Fresh-DB-Aufwand wird bewusst akzeptiert, weil ein Umschreiben veröffentlichter Changesets den sicheren Upgradepfad der Produktionsdatenbank verletzen würde.

Ein neuer Baseline-/Migrationsschnitt ist keine Voraussetzung und nicht Teil von Issue #221. Er kann später nur als eigenständige Architektur- und Betriebsentscheidung bewertet werden.

## Teststrategie

Audit-only Tests besitzen nach der Umsetzung keinen Vertrag mehr und werden entfernt statt durch neue Attrappen ersetzt. Gemischte Tests behalten ausschließlich Assertions für weiterhin bestehendes Verhalten.

Weiterhin belastbar zu prüfen sind insbesondere:

1. Optimistic Locking und stale-version Konflikte,
2. atomare Katalog-, Relations-, Bulk- und Ausschlusswrites,
3. Graphlock, Trigger, Constraints und relevante PostgreSQL-Semantik,
4. Upgrade des unmittelbar vorherigen Datenbankstands auf das Cleanup-Changeset,
5. vollständiger Liquibase-Aufbau einer leeren PostgreSQL-Datenbank,
6. normale Admin-Security und Katalog-/Ausschlussoberflächen,
7. Modulgrenzen.

Eine kürzere Testsuite ist ein erwünschter Nebeneffekt, aber kein quantitatives Abnahmekriterium. ADR 0004 bleibt maßgeblich: PostgreSQL-Tests bleiben dort bestehen, wo PostgreSQL tatsächlich Teil des zu prüfenden Verhaltens ist.

## Dokumentationsstand

Die aktuellen normativen Dokumente beschreiben den auditfreien Zustand. Historische Issues, PRs, Analyseberichte und veröffentlichte Changesets bleiben als zeitgebundene Nachweise unverändert. Deshalb darf ein Fresh-DB-Aufbau die frühere Auditstruktur vor Ausführung des Cleanup-Changesets vorübergehend anlegen; sie gehört nicht zum aktuellen Runtime-Schema.

## Konsequenzen

### Positiv

- weniger Runtime-, API-, UI- und Persistenzcode,
- weniger Testfixtures und PostgreSQL-/Spring-Tests ausschließlich für Änderungshistorisierung,
- kein Wachstum einer für den privaten Betrieb nicht benötigten Audit-Tabelle,
- einfachere Schreibtransaktionen ohne zusätzlichen Snapshot-/Auditfehlerpfad,
- klarere Trennung zwischen notwendigem Konkurrenzschutz und optionaler historischer Nachvollziehbarkeit.

### Negativ

- operative Katalogänderungen besitzen nach dem Cleanup keine anwendungsinterne Vorher-/Nachher-Historie mehr,
- Akteur und Zeitpunkt einzelner Webänderungen sind nicht mehr nachträglich aus der Anwendung rekonstruierbar,
- versehentlich entfernte oder geänderte Katalogwerte müssen bei Bedarf über Backups beziehungsweise andere vorhandene Betriebsnachweise rekonstruiert werden,
- Fresh-DB-Aufbau führt aus Upgrade-Sicherheitsgründen weiterhin historische Audit-Changesets aus, bevor das Cleanup sie entfernt.

Diese Nachteile sind für den heutigen privaten Betriebs- und Änderungsprozess bewusst akzeptiert.
