# Agenteneinstieg für Mise en Dice

## Pflichtquellen

Vor auftragsbezogener Entwicklungsarbeit vollständig lesen:

1. [Gemeinsamen Workflow](docs/dev-rules/WORKFLOW.md).
2. [Projektprofil](docs/PROJECT_PROFILE.md) und dessen situationsabhängige Pflichtquellen.
3. Aktuellen vollständigen Issue-/Paket-Body, sofern vorhanden; bei Review/Nacharbeit zusätzlich PR, tatsächlichen Diff und konkret benannten Reviewstand.

Den beauftragten Arbeitsbranch heranziehen, sonst den aktuellen `main`. Geltende Bereichs-/Override-Regeln prüfen. Bei neuen Ideen kein vorhandenes Issue voraussetzen. Pflichtquellen tatsächlich abrufen; Erinnerung ersetzt keinen aktuellen Quellstand.

Nur vor noch auszuführender Implementierung beziehungsweise konkreten technischen Nacharbeiten zusätzlich [Modellauswahl](docs/dev-rules/MODEL_SELECTION.md) und [Modellkatalog](docs/dev-rules/MODEL_CATALOG.md) vollständig lesen. Keine rückblickenden Empfehlungen nach erledigter Arbeit.

## Kulinarische Facharbeit

Nach dem in #172 dokumentierten Einführungspunkt beginnen neue Länder-/Ergänzungsrunden mit dem aktuellen vollständigen zugehörigen Runden-Issue, dem aktuellen #172-Tracker und [CULINARY_CATALOG_WORKFLOW.md](docs/CULINARY_CATALOG_WORKFLOW.md); dessen Phasenmatrix bestimmt die jeweils zusätzlich vollständig beziehungsweise abschnittsweise zu lesenden Pflichtquellen. Solange #172 die Umstellung noch nicht als eingeführt ausweist, bleibt der dort bezeichnete bisherige Ablauf verbindlich. Jede Konzeptneuaufnahme oder wesentliche Produktformänderung folgt weiterhin vollständig [INGREDIENT_CONCEPT_CURATION.md](docs/INGREDIENT_CONCEPT_CURATION.md) und [AVAILABILITY_AND_COOKING_NOVELTY.md](docs/AVAILABILITY_AND_COOKING_NOVELTY.md). Fachliche Recherche-, Einzelwert-, Notiz- und Freigabeverträge bleiben erhalten. Eine allgemeine Implementierungsfreigabe ersetzt keine dort erforderliche menschliche Fachentscheidung.

## Unmittelbar wichtige Schutzgrenzen

PostgreSQL, Liquibase und explizites JDBC/SQL bleiben die technische Grundlage. Veröffentlichte Changesets sind append-only; keine Ersatztests gegen H2 und kein ungefragter ORM-/Datenbankwechsel. Modulgrenzen sowie Versions- und Integritätsverträge aus den Fach-/Architekturquellen beachten.

Entwicklung und automatisierte Tests dürfen keine echten Discord- oder OpenAI-Verbindungen verwenden. Echter OpenAI-Zugriff bleibt ausschließlich im explizit aktivierten Produktivbetrieb zulässig; [ADR 0008](docs/adr/0008-production-only-openai-access.md) ist verbindlich. Vorhandene Schlüssel schaffen keine Ausnahme. Keine produktiven Datenzugriffe, Deployments oder Benachrichtigungen aus einer bloßen Entwicklungsfreigabe ableiten.

## Code Review Rules

Insbesondere Scope, Fachfreigaben, Migrationen, Konkurrenz-/Persistenzsemantik, Adaptergrenzen und echte PostgreSQL-Nachweise prüfen. Redaktionelle QA nicht durch produktive Content-Snapshots als dauerhaftes Test-Oracle ersetzen. Asset-/Katalogausnahmen nur im engen Geltungsbereich des [Projektprofils](docs/PROJECT_PROFILE.md). Allgemeine Prozessregeln nicht erneut definieren; [Herkunft und Regelabgleich](docs/DEV_RULES_ADOPTION.md) erläutern die Ablösung alter Vorgaben.
