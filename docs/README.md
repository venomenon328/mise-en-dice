# Projektdokumentation

Die Dokumente erfüllen unterschiedliche Zwecke und sollten nicht gegeneinander als austauschbare Gesamtspezifikation gelesen werden.

## Produkt und Fachlichkeit

- [`VISION.md`](VISION.md): Produktidee, Challenge-Regeln, Ziele und Nicht-Ziele
- [`DATA_MODEL.md`](DATA_MODEL.md): fachliche Entscheidungen des PostgreSQL-Datenmodells
- [`AVAILABILITY_AND_COOKING_NOVELTY.md`](AVAILABILITY_AND_COOKING_NOVELTY.md): verbindliche Ziel- und Redaktionssemantik für die getrennte Bewertung von personenspezifischer Beschaffbarkeit und gemeinsamer Kochungewöhnlichkeit einschließlich des katalogweiten Reviewprozesses
- [`INGREDIENT_CONCEPT_CURATION.md`](INGREDIENT_CONCEPT_CURATION.md): verbindliche operative Checkliste für jede neue Zutatenkonzeptanlage, Konkretisierung und wesentliche Produktformänderung einschließlich Availability-Marktbreite, Evidenz und Freigabe-Gates
- [`INITIAL_CATALOG.md`](INITIAL_CATALOG.md): Aufbau und Pflegeprinzipien der initialen Katalog-Baseline
- [`CULINARY_CATALOG_WORKFLOW.md`](CULINARY_CATALOG_WORKFLOW.md): verbindlicher Länderworkflow nach #172 von Recherchebeginn und aktuellem Katalogabgleich über zwei menschliche Freigaben bis zu Einpflege, Protokoll und Batch-Verify; enthält den wiederverwendbaren Startprompt
- [`CULINARY_COUNTRY_ASSOCIATIONS.md`](CULINARY_COUNTRY_ASSOCIATIONS.md): redaktionelle Semantik und technische Pflege der expliziten kulinarischen Länderzuordnungen
- [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md): verbindliche Bedien-, Interaktions-, Locking- und Sicherheitsentscheidungen für die private Katalogverwaltung ohne Runtime-Katalogaudit
- [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md): verbindliche Regeln für Gewichtung, harte Kandidatengültigkeit, Scores, Diversität, Determinismus, Recovery und Simulation
- [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md): verbindlicher Vertrag für 1–3 kuratierte Angebote, maximal zwei externe Kuratoraufrufe, Carry-over, Nutzerbestätigung und Historienwirkung
- [`CHALLENGE_VOTING_AND_PARTICIPATION.md`](CHALLENGE_VOTING_AND_PARTICIPATION.md): verbindliche Mehrnutzer-Semantik des bestehenden Voting-Cores; die frühere eigenständige Challenge-Teilnahme und das fest codierte Default-Elektorat werden für neue Pakete durch die nachfolgende Spezifikation ersetzt
- [`PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md`](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md): verbindliche Semantik für stabile Personenidentitäten, persistentes Standard-Elektorat, frühe Session-Snapshots, optionale sessiongebundene Beschaffbarkeit und die Stilllegung von `challenge_participation` als Fachautorität
- [`CHALLENGE_ARCHIVE_AND_CARDS.md`](CHALLENGE_ARCHIVE_AND_CARDS.md): verbindliche Semantik für öffentliche Challenge-Nummern, bestätigte Historie und operatorverwaltete Challenge-Cards aus Phase 13
- [`CHALLENGE_RESULTS_AND_COMPLETION.md`](CHALLENGE_RESULTS_AND_COMPLETION.md): verbindliche Semantik für persönliche Challenge-Ergebnisse, optionale Bewertung und Fotos, expliziten Abschluss sowie die Trennung zwischen letzter und aktiver Challenge
- [`CHALLENGE_MODE_IDEAS.md`](CHALLENGE_MODE_IDEAS.md): ausdrücklich unverbindliche Ideensammlung für Vorab-Anker, Ergänzungsanker, Bot-Anker, persönlichen Twist und Generatorprofile
- [`DISCORD_INGREDIENT_LOOKUP.md`](DISCORD_INGREDIENT_LOOKUP.md): verbindliche Such-, Auswahl-, Darstellungs- und Modulgrenzen für die rein lesende Discord-Zutatenabfrage aus Issue #108
- [`CANDIDATE_GENERATOR_DATA_READINESS.md`](CANDIDATE_GENERATOR_DATA_READINESS.md): gemessene Metadatenabdeckung und Gate für den Generatorstart
- [`analysis/candidate-generator-data-readiness.sql`](analysis/candidate-generator-data-readiness.sql): reproduzierbare PostgreSQL-Auswertung des aktiven Ziehpools
- [`analysis/availability-novelty-final-review-v1-20260907.md`](analysis/availability-novelty-final-review-v1-20260907.md): autoritativer #188-Abschlussstand; ausschließlich das dort verlinkte TSV liefert die freigegebenen Einzelwerte und Notizen
- [`analysis/availability-novelty-migration-20260907.md`](analysis/availability-novelty-migration-20260907.md): #189-Migrationsumfang, exakter einmaliger QA-Abgleich, technischer Fingerprint und Release-Gate #190
- [`analysis/availability-novelty-calibration-20260907.md`](analysis/availability-novelty-calibration-20260907.md): #202-Stichprobenmatrix, Generatorfingerprints, Availability-/Novelty-Befunde und menschlicher Haltepunkt vor #190B
- [`analysis/availability-reference-anchors-v2-20260904.md`](analysis/availability-reference-anchors-v2-20260904.md): historische Vorbereitung des strengeren Availability-Ankersatzes; durch den finalen #188-Vertrag abgelöst
- [`analysis/final-catalog-review-20260813.md`](analysis/final-catalog-review-20260813.md): verbindlicher fachlicher Review für den finalen Katalog
- [`analysis/final-catalog-snapshot-contract-20260813.md`](analysis/final-catalog-snapshot-contract-20260813.md): Normalisierung, Upgrade-Schutz, Endfingerprint und Dimensionsabdeckung aus Issue #52
- [`analysis/generator-laboratory-implementation-notes.md`](analysis/generator-laboratory-implementation-notes.md): historische Implementierungsgrenze des damaligen Preview-/Replay-Kerns aus Phase 9E1; der aktuelle Vertrag steht in ADR 0009

## Architektur, Betrieb und Umsetzung

- [`ARCHITECTURE.md`](ARCHITECTURE.md): verbindliche Zielarchitektur und Modulgrenzen
- [`DEPLOYMENT.md`](DEPLOYMENT.md): VPS-Betrieb, Produktionsdeployment, isolierte Branch-Previews, Backup und Restore
- [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md): bisherige Umsetzungsreihenfolge und Gates; die neu spezifizierten Folgepakete werden beim jeweiligen Implementierungsstand dort konsistent ergänzt
- [`adr`](adr): einzelne grundlegende Architekturentscheidungen mit Kontext und Konsequenzen
- [`ADR 0007`](adr/0007-seeded-two-stage-candidate-generator.md): seedbarer zweistufiger Kandidatengenerator und Trennung von Generation und Kuratierung
- [`ADR 0008`](adr/0008-production-only-openai-access.md): echte OpenAI-Aufrufe ausschließlich im explizit aktivierten Produktivbetrieb; Entwicklung und automatisierte Tests bleiben vollständig providerfrei
- [`ADR 0009`](adr/0009-determinism-without-historical-generator-replay.md): Determinismus, Frozen-Context-Recovery und Batch 2 bleiben; historisches Generator-Replay entfällt
- [`ADR 0010`](adr/0010-remove-runtime-catalog-audit.md): Runtime-Katalogaudit entfällt vollständig; Optimistic Locking, atomare Transaktionen, PostgreSQL-Integrität und Liquibase bleiben
- [`ADR 0011`](adr/0011-risk-based-ci-verification.md): umgesetzte risikobasierte CI-Verifikation mit wiederverwendeter PostgreSQL-Testinfrastruktur, begrenztem Production-Upgradehorizont, parallelen `fast`-/`postgresql`-/`migration`-Verify-Lanes und stabilem Gate sowie `skip`/`smoke`/`full` für Deployment Verify
- [`analysis/production-migration-baseline-225b.md`](analysis/production-migration-baseline-225b.md): bestätigter Produktionscutoff, Cleanup-Evidenz und vergleichbare Vollsuite-Messung für #225B

## Reihenfolge der Verbindlichkeit bei Entwicklungsarbeit

Für ein konkretes Paket gelten in dieser Reihenfolge:

1. das aktuelle GitHub-Issue,
2. konkrete Anforderungen aus dem aktuellen PR-Review,
3. die im Issue oder Review referenzierten Dokumente,
4. die allgemeinen Arbeitsregeln in [`../AGENTS.md`](../AGENTS.md).

Ein Issue soll den Lieferumfang und die Abgrenzung festlegen, aber bereits dokumentierte Produkt- und Architekturentscheidungen nicht vollständig wiederholen.

## Aktuelle Folgepakete

Die neuen Funktionen sind bewusst in folgende Pakete getrennt:

1. #150 – transportneutraler Teilnehmer-/Elektoratskern und sessiongebundene Sparse-Beschaffbarkeit,
2. #151 – Discord-Administration und DB-basierte Identitätsautorität,
3. #152 – isolierte Beschaffbarkeitskalibrierung,
4. #153 – transportneutraler Ergebnis-/Abschlusskern,
5. #154 – öffentliche Discord-Status- und Ergebnisdarstellung,
6. #155 – operatorgebundene Ergebniserfassung und -pflege über Discord.

#151 und #152 bauen auf #150 auf und können danach unabhängig voneinander umgesetzt werden. #153 benötigt #150, aber nicht zwingend #151 oder #152. #154 folgt auf #153; #155 folgt auf #151, #153 und #154. Issue #145 zum Challenge-Card-Produktionsworkflow bleibt davon unabhängig.

Entwicklung und automatisierte Tests bleiben weiterhin vollständig ohne echte Discord- oder OpenAI-Verbindungen.

## Metadatenprogramm ab #186

Die vollständige Trennung und Neukalibrierung von Beschaffbarkeit und Kochungewöhnlichkeit ist in vier Folgepakete geteilt:

1. #187 – technische Fünfer-Skala und Begriffsschärfung,
2. #188 – katalogweiter fachlicher Vollreview mit menschlicher Freigabe,
3. #189 – migrationsgeführte Übernahme der freigegebenen Werte und Entkopplung pauschaler Gewichtswarnungen,
4. #190A/#202 – explizite reproduzierbare Generator-Kalibrierungsstichprobe und Empfehlung ohne produktive Übernahme,
5. #190B/#203 – menschlich freigegebene Faktoren übernehmen und gezielt regressionsprüfen.

#188 kann nach Freigabe der Spezifikation parallel zu #187 vorbereitet werden. #189 benötigt beide abgeschlossenen Vorgänger; #190 folgt auf #189. Produktive redaktionelle Einzelwerte werden dabei nicht als automatisiertes Test-Oracle dupliziert.
