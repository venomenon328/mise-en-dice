# Projektdokumentation

Die Dokumente erfüllen unterschiedliche Zwecke und sollten nicht gegeneinander als austauschbare Gesamtspezifikation gelesen werden.

## Produkt und Fachlichkeit

- [`VISION.md`](VISION.md): Produktidee, Challenge-Regeln, Ziele und Nicht-Ziele
- [`DATA_MODEL.md`](DATA_MODEL.md): fachliche Entscheidungen des PostgreSQL-Datenmodells
- [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md): Aufbau und Pflegeprinzipien der initialen Katalog-Baseline
- [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md): verbindliche Bedien-, Interaktions-, Locking-, Audit- und Sicherheitsentscheidungen für die private Katalogverwaltung
- [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md): verbindliche Regeln für Gewichtung, harte Kandidatengültigkeit, Scores, Diversität, Determinismus, Replay und Simulation
- [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md): verbindlicher Vertrag für 1–3 kuratierte Angebote, maximal zwei externe Kuratoraufrufe, Carry-over, Nutzerbestätigung und Historienwirkung
- [`CHALLENGE_VOTING_AND_PARTICIPATION.md`](CHALLENGE_VOTING_AND_PARTICIPATION.md): verbindliche Mehrnutzer-Semantik für Electorate-Snapshots, geheime Abstimmung, Tie-Break, einmaligen Reroll und von der Stimmberechtigung getrennte Challenge-Teilnahme
- [`CHALLENGE_ARCHIVE_AND_CARDS.md`](CHALLENGE_ARCHIVE_AND_CARDS.md): verbindliche Semantik für öffentliche Challenge-Nummern, aktuelle Challenge, bestätigte Historie und operatorverwaltete Challenge-Cards aus Phase 13
- [`DISCORD_INGREDIENT_LOOKUP.md`](DISCORD_INGREDIENT_LOOKUP.md): verbindliche Such-, Auswahl-, Darstellungs- und Modulgrenzen für die rein lesende Discord-Zutatenabfrage aus Issue #108
- [`CANDIDATE_GENERATOR_DATA_READINESS.md`](CANDIDATE_GENERATOR_DATA_READINESS.md): gemessene Metadatenabdeckung und Gate für den Generatorstart
- [`analysis/candidate-generator-data-readiness.sql`](analysis/candidate-generator-data-readiness.sql): reproduzierbare PostgreSQL-Auswertung des aktiven Ziehpools
- [`analysis/final-catalog-review-20260813.md`](analysis/final-catalog-review-20260813.md): verbindlicher fachlicher Review für den finalen Katalog
- [`analysis/final-catalog-snapshot-contract-20260813.md`](analysis/final-catalog-snapshot-contract-20260813.md): Normalisierung, Upgrade-Schutz, Endfingerprint und Dimensionsabdeckung aus Issue #52
- [`analysis/generator-laboratory-implementation-notes.md`](analysis/generator-laboratory-implementation-notes.md): kompakte Implementierungsgrenze des read-only Preview-/Replay-Kerns aus Phase 9E1

## Architektur, Betrieb und Umsetzung

- [`ARCHITECTURE.md`](ARCHITECTURE.md): verbindliche Zielarchitektur und Modulgrenzen
- [`DEPLOYMENT.md`](DEPLOYMENT.md): VPS-Betrieb, Produktionsdeployment, isolierte Branch-Previews, Backup und Restore
- [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md): aktuelle Reihenfolge der Entwicklungspakete und ihre Gates
- [`adr`](adr): einzelne grundlegende Architekturentscheidungen mit Kontext und Konsequenzen
- [`ADR 0007`](adr/0007-seeded-two-stage-candidate-generator.md): seedbarer zweistufiger Kandidatengenerator und Trennung von Generation und Kuratierung
- [`ADR 0008`](adr/0008-production-only-openai-access.md): echte OpenAI-Aufrufe ausschließlich im explizit aktivierten Produktivbetrieb; Entwicklung und automatisierte Tests bleiben vollständig providerfrei

## Reihenfolge der Verbindlichkeit bei Entwicklungsarbeit

Für ein konkretes Paket gelten in dieser Reihenfolge:

1. das aktuelle GitHub-Issue,
2. konkrete Anforderungen aus dem aktuellen PR-Review,
3. die im Issue oder Review referenzierten Dokumente,
4. die allgemeinen Arbeitsregeln in [`../AGENTS.md`](../AGENTS.md).

Ein Issue soll den Lieferumfang und die Abgrenzung festlegen, aber bereits dokumentierte Produkt- und Architekturentscheidungen nicht vollständig wiederholen.

## Aktueller nächster Schritt

Phase 12E / Issue #90 führt weiterhin den privaten Produktionspilot und die dokumentierte Go/No-Go-Entscheidung für `v0.1.0` durch. Erst danach folgt Phase 13: #140 ergänzt den transportneutralen Challenge-Archiv-/Card-Core, #141 anschließend die guild-weiten Discord-Lesecommands und die operatorgebundene Card-Verwaltung. Entwicklung und automatisierte Tests bleiben weiterhin vollständig ohne echte Discord- oder OpenAI-Verbindungen.
