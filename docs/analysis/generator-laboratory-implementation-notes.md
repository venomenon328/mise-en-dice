# Generator-Labor – Implementierungsgrenze Phase 9E1

Stand: 13. August 2026

Issue #37 implementiert ausschließlich den diagnostischen Preview-/Replay-Kern von Phase 9E.

- Vorschauen verwenden die öffentliche Generatorpipeline und persistieren keine Session, keinen Attempt, keinen Batch und keine Challenge.
- `PRODUCTION_VISIBLE` materialisiert dieselbe bestätigte sichtbare Historie wie die produktive Generation; synthetische Szenarien bleiben davon getrennt.
- Kandidatenpaar-Diagnostik verwendet die bereits berechnete `PairAssessment` als Autorität und ergänzt nur erklärende Evidenz.
- Persistierte Attempts und Batches werden ausschließlich über `GenerationQueries` gelesen; historische Snapshotwerte werden nicht aus dem aktuellen Katalog rekonstruiert.
- Replay bleibt read-only und liefert bei Abweichung zusätzlich eine strukturierte erste Differenz.
- Simulation/Report folgen getrennt in #53; der Admin-Simulationsadapter folgt in #54; Kalibrierung bleibt #40.

Dieses Dokument ist eine knappe Implementierungsnotiz. Verbindliche Fachquellen bleiben Issue #37, `CANDIDATE_GENERATOR.md`, `ARCHITECTURE.md`, `ADMINISTRATION_UI.md` und ADR 0007.
