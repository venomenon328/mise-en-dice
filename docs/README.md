# Projektdokumentation

Die Dokumente erfüllen unterschiedliche Zwecke und sollten nicht gegeneinander als austauschbare Gesamtspezifikation gelesen werden.

## Produkt und Fachlichkeit

- [`VISION.md`](VISION.md): Produktidee, Challenge-Regeln, Ziele und Nicht-Ziele
- [`DATA_MODEL.md`](DATA_MODEL.md): fachliche Entscheidungen des PostgreSQL-Datenmodells
- [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md): Aufbau und Pflegeprinzipien der initialen Katalog-Baseline
- [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md): verbindliche Bedien-, Interaktions-, Locking-, Audit- und Sicherheitsentscheidungen für die private Katalogverwaltung
- [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md): verbindliche Regeln für Gewichtung, harte Kandidatengültigkeit, Scores, Diversität, Determinismus, Replay und Simulation
- [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md): verbindlicher Vertrag für 1–3 kuratierte Angebote, maximal zwei externe Kuratoraufrufe, Carry-over, Nutzerbestätigung und Historienwirkung
- [`CANDIDATE_GENERATOR_9C2_REVIEW_CLARIFICATION.md`](CANDIDATE_GENERATOR_9C2_REVIEW_CLARIFICATION.md): verbindliche Nachschärfung aus dem Review von PR #49 zu tatsächlichen Neuigkeitsquoten und zur ausschließlich expliziten großen Baselinesimulation; ersetzt für Issue #47 widersprechende ältere 9C2-Formulierungen
- [`CANDIDATE_GENERATOR_DATA_READINESS.md`](CANDIDATE_GENERATOR_DATA_READINESS.md): gemessene Metadatenabdeckung und Gate für den Generatorstart
- [`analysis/candidate-generator-data-readiness.sql`](analysis/candidate-generator-data-readiness.sql): reproduzierbare PostgreSQL-Auswertung des aktiven Ziehpools

## Architektur, Betrieb und Umsetzung

- [`ARCHITECTURE.md`](ARCHITECTURE.md): verbindliche Zielarchitektur und Modulgrenzen
- [`DEPLOYMENT.md`](DEPLOYMENT.md): VPS-Betrieb, Produktionsdeployment, isolierte Branch-Previews, Backup und Restore
- [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md): aktuelle Reihenfolge der Entwicklungspakete und ihre Gates
- [`adr`](adr): einzelne grundlegende Architekturentscheidungen mit Kontext und Konsequenzen
- [`ADR 0007`](adr/0007-seeded-two-stage-candidate-generator.md): seedbarer zweistufiger Kandidatengenerator und Trennung von Generation und Kuratierung

## Reihenfolge der Verbindlichkeit bei Entwicklungsarbeit

Für ein konkretes Paket gelten in dieser Reihenfolge:

1. das aktuelle GitHub-Issue,
2. konkrete Anforderungen aus dem aktuellen PR-Review,
3. die im Issue oder Review referenzierten Dokumente,
4. die allgemeinen Arbeitsregeln in [`../AGENTS.md`](../AGENTS.md).

Ein Issue soll den Lieferumfang und die Abgrenzung festlegen, aber bereits dokumentierte Produkt- und Architekturentscheidungen nicht vollständig wiederholen.

## Aktueller nächster Schritt

Phase 9A bis 9D sind abgeschlossen. Als nächstes folgt Phase 9E / Issue #37 mit Generator-Labor, Preview, Replaydarstellung und begrenzter Simulationsdiagnostik. Danach schließt Phase 9F / Issue #40 die breite Kalibrierung und fachliche Abnahme ab; OpenAI- und Discord-Implementierung bleiben weiterhin nachgelagert.
