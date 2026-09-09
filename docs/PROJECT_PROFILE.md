# Projektprofil: Mise en Dice

## Quellen und Zuständigkeit

`venomenon328/mise-en-dice` organisiert Koch-Challenges und ihren Zutatenkatalog. Der [gemeinsame Workflow](dev-rules/WORKFLOW.md) regelt Entwicklung und Übergaben; dieses Profil konkretisiert technische und fachliche Pflichtquellen. Die [Herkunftsnotiz](DEV_RULES_ADOPTION.md) dokumentiert die Einführung.

Bei Produkt-, Architektur-, Persistenz- oder Schnittstellenänderungen [VISION.md](VISION.md), [ARCHITECTURE.md](ARCHITECTURE.md), [DATA_MODEL.md](DATA_MODEL.md) und die betroffenen [ADRs](adr) vollständig lesen. Weitere Fachquellen nach Gegenstand aus dem [Dokumentindex](README.md) wählen; explizite vollständige Issue-Pflichtquellen bleiben vollständig zu lesen. Reine Prozessdokumentation benötigt dagegen die betroffenen Prozessquellen, keine erneute Vollanalyse oder Neubewertung unveränderter Katalog-/Produktverträge.

| Gegenstand | Zusätzliche Pflichtquellen |
| --- | --- |
| Länderreview nach #172 | Aktueller vollständiger Issue #172 und [Länderworkflow](CULINARY_CATALOG_WORKFLOW.md) samt Pflichtquellen; Ländersemantik nach dem dort maßgeblichen #165-Vertrag |
| Konzeptanlage oder wesentliche Produktformänderung | [Konzeptkuratierung](INGREDIENT_CONCEPT_CURATION.md), [Availability und Kochungewöhnlichkeit](AVAILABILITY_AND_COOKING_NOVELTY.md) |
| Katalogdaten, verfügbare operative Bestände und Migrationen | Relevante ADRs einschließlich ADR 0003 sowie aktuelle freigegebene Änderungen, nicht nur historische Baselines |
| Generator/Kuratierung | [Generator](CANDIDATE_GENERATOR.md), [Datenbereitschaft](CANDIDATE_GENERATOR_DATA_READINESS.md), [Kuratierung](CURATION_AND_CHALLENGE_SELECTION.md), relevante ADRs |
| Teilnehmer, Voting, Challenge-Ergebnisse, Karten oder Discord-Lookup | Die jeweils zuständigen vollständigen Fachverträge aus dem Dokumentindex |
| Betriebs-/Deploymentänderung | [DEPLOYMENT.md](DEPLOYMENT.md), [PRODUCTION_VALIDATION.md](PRODUCTION_VALIDATION.md) und betroffene Deploymentwerkzeuge/CI |

Die Einführung ändert keine fachlichen Werte, Gewichtungen, Regeln, Freigaben oder Produktformen. Individuelle Beschaffungsnotizen und gemeinsame Kochungewöhnlichkeit bleiben gemäß ihren eigenen Quellen getrennt. Spezielle Länder-/Katalogfreigaben sind keine abzulösende allgemeine Git-Zeremonie, sondern sichern konkrete fachliche Entscheidungen; sie bleiben wirksam.

## Begründete Architektur- und Datenbankgrenzen

Mise en Dice bleibt ein modularer Monolith. Web und Discord sind Adapter ohne eigene Fach-/Persistenzlogik. Module verwenden öffentliche APIs anderer Module; kein direkter Datenbankzugriff aus Webcontrollern, Templates, JDA-Listenern oder API-Clients. Transportobjekte gehören nicht in Domain-/Application-Code; externe Netzwerkaufrufe nicht in offene Datenbanktransaktionen. Gemeinsame Fachlogik nicht zwischen Adaptern kopieren.

PostgreSQL bleibt die einzige Laufzeitdatenbank. Liquibase bleibt Autorität für Schema, strukturelle Migrationen, Referenzdaten und einmalige Baseline. Veröffentlichte Changesets sind append-only; explizite Includes statt `includeAll`, kein `runAlways` für den Katalog. PostgreSQL-Trigger, Constraints, partielle Indizes und JSONB-Semantik erhalten. JDBC mit explizitem SQL; Transaktionen in Application Services; Integrität und Konkurrenz nicht allein durch Vorabprüfungen absichern. Repositories liefern passende Domainobjekte oder unveränderliche Projektionen. Unbekannte technische Fehler nicht als Fachkonflikt maskieren. ORM/JPA/Hibernate oder H2 erfordern eine neue begründete Architekturentscheidung.

Gemäß [ADR 0010](adr/0010-remove-runtime-catalog-audit.md) besitzt die Katalogverwaltung keinen Runtime-Audit mehr. Optimistic Locking, atomare Transaktionen, PostgreSQL-Integritätsverträge und Liquibase bleiben unverändert maßgeblich. Historische Issues, PRs und veröffentlichte Changesets werden nicht umgeschrieben; das aktuelle Schema entfernt die frühere Audit-Tabelle vorwärtsgerichtet.

Diese Grenzen werden bewusst wegen der implementierten Daten-/Integritätsverträge fortgeführt, nicht allein wegen ihres Alters. Ihre Abschaffung wäre eine eigenständige technische Migration und ist nicht Teil der Regelintegration.

## Dienste, Daten und sichere Verifikation

Keine echten Discord-/OpenAI-Aufrufe in Entwicklung und automatisierten Tests. [ADR 0008](adr/0008-production-only-openai-access.md) erlaubt echten OpenAI-Zugriff ausschließlich im explizit aktivierten Produktivbetrieb. Deterministische Fakes, Fixtures oder lokale HTTP-Stubs verwenden. CI benötigt keinen produktiven Schlüssel; zufällig vorhandene Schlüssel dürfen keine Live-Nutzung aktivieren.

Reine Fachlogik schnell ohne Spring-Kontext testen; Persistenz-, Migrations-, Trigger- und Transaktionsverhalten mit echten PostgreSQL-Testcontainern. Kein H2-Ersatz. Migrationsänderungen müssen weiterhin den vollständigen Aufbau einer leeren PostgreSQL-Datenbank bestehen lassen. Bei Compose-Änderungen zusätzlich die betroffene Konfiguration mit `docker compose config` beziehungsweise dem entsprechenden `-f`-Pfad prüfen. Kein echter Produktionszugriff oder Reset ohne passenden Auftrag und Betriebsvertrag.

## Abschlussprüfpfad und begrenzte Ausnahmen

Für normale Änderungen sind die aktuellen PR-Prüfungen **Verify / verify** und **Deployment Verify / classify, deployment, legacy-preview** maßgeblich. `legacy-preview` ist im bestehenden Workflow bei Nicht-Asset-PRs vorgesehen, nicht bei jedem Push nach `main`.

Verify führt Challenge-Card-Werkzeugtests und den vollständigen Maven-Lauf `./mvnw -DforkCount=2 clean verify` aus. Eine geeignete lokale Prüfung kann `./mvnw clean verify` verwenden. Deployment Verify prüft Shell-/Betriebswerkzeuge und isolierte Docker-Instanzen einschließlich Wiederanlauf, Backup/Restore und Legacy-Preview. Die Workflows bleiben die konkreten Ausführungspfade: [Verify](../.github/workflows/verify.yml), [Deployment Verify](../.github/workflows/deployment-verify.yml).

Passende aktuelle CI-Belege können den Abschlussnachweis liefern; keinen identischen vollständigen lokalen Lauf nur zusätzlich erzwingen. Lokale Prüfungen benötigen die tatsächlich geeignete Umgebung und Ressourcen. Ein fehlender Docker-Daemon ist kein bestandener PostgreSQL-Test. Es wird kein pauschales lokales Testverbot aus CSC übernommen. Vollständigen Diff, Dateiverweise und `git diff --check <Basis-SHA> <Head-SHA>` prüfen.

Zwei bestehende enge Ausnahmen bleiben wegen ihrer unterschiedlichen fachlichen/technischen Prüfgegenstände bestehen:

- **Redaktionelle Länder-/Katalogbatches nach #172:** Teststrategie des [Länderworkflows](CULINARY_CATALOG_WORKFLOW.md) anwenden. Keine produktiven Content-Assertions und kein Vollsuite-Pflichtlauf pro Land; vollständiges `./mvnw clean verify` bei Merge-Vorbereitung oder technischem Anlass. Keine pauschale Ausnahme für Anwendungscode.
- **Eindeutig klassifizierte Challenge-Card-Assets:** Nur `ASSET_INDEX.csv` allein oder mit hinzugefügten/geänderten Produktions-PNGs im erlaubten Bereich. Der vorhandene Klassifikator entscheidet; andere Dateien, Löschungen, Umbenennungen oder unklarer Diff bleiben im vollständigen Prüfpfad. Der [Assetvalidator](../design/challenge-cards/tools/validate_asset_catalog.py) ist verbindlich; der bestehende CI-Workflow führt außerdem die Werkzeugtests aus. Übersprungene volle Builds sind nur in diesem nachgewiesenen engen Fall nicht anwendbar.

Diese Regelintegration verändert Dokumente außerhalb des Assetbereichs: **vollständiger Nicht-Asset-CI-Pfad**, keine Ausnahme. Die redaktionellen Fachtests/-verbote werden nicht umgeschrieben; einmalige Bestands-/Diff-QA ist kein dauerhaftes Content-Test-Oracle.

## Abnahme, Branches und Betriebswirkung

Manuelle fachliche oder Bedienabnahmen konkret nach betroffenem Issue/Fachvertrag mit dem Zeitpunkt vor Merge oder vor Release/Deployment festhalten. Vorhandene verpflichtende Fachfreigaben nicht eigenmächtig verschieben. Die reine Dokumentintegration braucht keine Wiederholung unveränderten Produktverhaltens und keine neue Katalogfreigabe.

Zielbranch `main`; neue Branches nach gemeinsamem Workflow. Bestehende benannte Sammel-/Arbeitsbranches bleiben auftragsgebunden und werden nicht zurückgesetzt. Vorbereitung erstellt standardmäßig noch keinen neuen Branch/PR, außer dies ist ausdrücklich beauftragt. Draft bis Abnahme. Als Projektstandard Mergecommit nach Freigabe: bereits referenzierte Paketcommits bleiben damit nachvollziehbar; eine abweichende Methode konkret vereinbaren. Keine automatische Branchlöschung und keine ungefragte Änderung von Schutzregeln.

Die eingecheckten Merge-CI-Workflows verifizieren isolierte Runner-Instanzen. Auch ein dort `production deploy` genannter Test verwendet den isolierten CI-Runtimepfad, nicht automatisch den echten VPS. Reales Deployment, Produktionsmigration, Discordwirkung und OpenAI-Aktivierung bleiben separate Betriebsaufträge nach den Betriebsquellen. Unbekannte externe Automatisierung nicht als geprüft behaupten; vor einem konkreten Betriebsauftrag den tatsächlichen Zustand klären.

## Ablösung alter allgemeiner Vorgaben

Allgemeine Scope-, Branch-, Dokumentpflege-, Abschluss- und Reviewregeln aus dem alten AGENTS sowie allgemeine Prozesspassagen in DEVELOPMENT_PLAN und Dokumentindex werden durch den gemeinsamen Workflow und dieses Profil ersetzt. Die alte pauschale Rangliste „Issue vor Review vor Dokumenten vor AGENTS“ gilt nicht weiter: Lieferumfang, Fach-/Architekturgrenzen und Prozess haben die Zuständigkeiten aus WORKFLOW. Fachliche Spezialverträge und konkrete Paketabhängigkeiten bleiben erhalten.

Die Modellauswahl folgt ausschließlich der lokalen Regelkopie. High-Standard und eigenständiger Qualitätsnutzen bleiben unverändert. Die [bereitgestellten Projekteinstellungen](CHATGPT_PROJECT_INSTRUCTIONS.md) ersetzen nach Einführungsmerge den bisherigen allgemeinen Entwicklungstext einschließlich des Pflichtverweises auf `Codex-Empfehlung.txt`; ihre Ablage im Repository aktualisiert ChatGPT nicht automatisch.
