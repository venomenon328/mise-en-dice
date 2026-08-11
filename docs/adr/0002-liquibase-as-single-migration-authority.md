# ADR 0002: Liquibase ist die einzige Migrationsautorität

- Status: Akzeptiert
- Datum: 11. August 2026

## Kontext

Das Repository enthält derzeit eigenständige PostgreSQL-Migrationen, Seed-Skripte und einen `psql`-basierten Bootstrap. Mit einer laufenden Anwendung werden reproduzierbare Schemaänderungen, nachvollziehbare Reihenfolgen und automatische Testaufbauten benötigt.

Ein dauerhafter Parallelbetrieb aus manuell aufgerufenen SQL-Dateien, einem Anwendungsframework und einem weiteren Migrationstool würde mehrere konkurrierende Definitionen des Datenbankzustands erzeugen.

## Entscheidung

Liquibase wird der einzige Mechanismus für Schemaänderungen, strukturelle Datenmigrationen, stabile Referenzdaten und die einmalige Katalog-Baseline einer leeren Datenbank.

Die vorhandenen PostgreSQL-Skripte dürfen als Liquibase-formatiertes SQL übernommen werden. PostgreSQL-Funktionen, Trigger, partielle Indizes und andere datenbankspezifische Konstrukte müssen semantisch erhalten bleiben.

Der Master-Changelog bindet alle Dateien explizit in einer festgelegten Reihenfolge ein. `includeAll` wird nicht verwendet.

Bereits veröffentlichte Changesets sind append-only. Korrekturen erfolgen über neue Changesets; ausgeführte Changesets werden nicht nachträglich umgeschrieben.

Das bisherige `db/bootstrap.sql` wird nach der Umstellung entfernt oder höchstens als nicht autoritativer Aufrufwrapper dokumentiert. Es darf keine zweite Migrationstrecke bleiben.

## Konsequenzen

### Positiv

- Jeder Anwendungsstart und jeder Integrationstest verwendet dieselbe Migrationshistorie.
- Reihenfolge, Prüfsummen und bereits angewandte Änderungen sind nachvollziehbar.
- PostgreSQL-spezifisches SQL kann ohne künstliche Abstraktion erhalten bleiben.
- Eine leere Datenbank lässt sich automatisiert und reproduzierbar aufbauen.

### Negativ

- SQL-Dateien müssen sauber in Changesets geschnitten und insbesondere bei PL/pgSQL korrekt konfiguriert werden.
- Nach Veröffentlichung darf eine bequeme Korrektur alter Dateien nicht mehr vorgenommen werden.
- Die Migration eines bereits außerhalb Liquibase aufgebauten Datenbestands würde eine gesonderte Baseline-Strategie benötigen.

## Abgrenzung

Zum Zeitpunkt dieser Entscheidung existiert noch keine produktiv betriebene Datenbank. Das erste Entwicklungspaket darf deshalb eine saubere Liquibase-Baseline für neu aufzubauende Datenbanken etablieren, statt eine unbekannte bestehende Installation nachträglich zu adoptieren.
