# Entwicklungsplan

Stand: 21. August 2026

Dieses Dokument beschreibt die aktuelle Umsetzungsreihenfolge. Die [`VISION.md`](VISION.md) beschreibt das gewünschte Produkt; dieser Plan legt fest, in welcher technischen Reihenfolge die dafür notwendigen Bausteine entstehen. Die konkrete Webspezifikation steht in [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md).

## Leitlinien für Entwicklungspakete

Jedes Paket soll:

- einen klar abgegrenzten fachlichen oder technischen Zweck besitzen,
- auf ein verbindliches GitHub-Issue verweisen,
- die maßgeblichen Dokumente nennen, statt deren Inhalt vollständig zu duplizieren,
- keine Funktionen späterer Pakete vorziehen,
- verpflichtende automatisierte Tests enthalten,
- als Draft-PR beginnen und erst nach vollständiger Prüfung zur Abnahme bereitgestellt werden,
- Dokumentation und Implementierung im selben Paket konsistent halten.

Größere zusammenhängende Pakete werden bevorzugt mit Codex umgesetzt. Kleine isolierte Nacharbeiten, Reviews und klar lokalisierte Fehlerbehebungen können direkt übernommen werden.

## Phase 0: Katalog-Baseline abschließen (abgeschlossen)

Der Ausbau des initialen Zutatenkatalogs in PR #1 wurde vor dem Anwendungsfundament abgeschlossen und nach `main` gemergt.

Er bildet die Baseline, die anschließend in Liquibase überführt wurde.

**Erfülltes Gate:**

- PR #1 ist gemergt,
- alle Seed-Manifeste sind strukturell validiert,
- der Inhalt von `main` war der verbindliche Ausgangspunkt für das Anwendungsfundament.

## Phase 1: Anwendungs- und Persistenzfundament (abgeschlossen)

Das Ergebnis ist ein ausführbares, getestetes Spring-Boot-Grundsystem ohne produktive Web-, Bot-, Generator- oder Kuratorfunktionen.

Enthalten sind insbesondere:

- Java-/Maven-/Spring-Boot-Grundgerüst,
- fachliche Modulstruktur und Spring-Modulith-Verifikation,
- Liquibase-Konvertierung der PostgreSQL-Struktur und Baseline,
- Docker-Compose-Konfiguration für PostgreSQL,
- Testcontainers-Integration,
- automatisierte Migration-, Seed- und Triggerprüfungen,
- GitHub-Actions-Build,
- Aktualisierung der Datenbankdokumentation.

**Erfülltes Gate:**

- eine leere PostgreSQL-Datenbank wird ausschließlich über Liquibase vollständig aufgebaut,
- ein zweiter Start verändert die Baseline nicht erneut,
- `./mvnw verify` führt Modul- und PostgreSQL-Integrationstests erfolgreich aus,
- es existiert keine parallele Schema- oder Bootstrap-Autorität mehr.

## Phase 2: Spezifikation der Webverwaltung (abgeschlossen mit Issue #5)

Die private Verwaltungsoberfläche wurde vor umfangreichem UI-Code fachlich, gestalterisch und technisch spezifiziert.

Verbindliche Ergebnisse stehen in [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md), insbesondere:

- globale Navigation ohne vorgeschaltetes Dashboard,
- Split-View aus Suche/Filter/Hierarchie beziehungsweise Liste und Detailansicht,
- korrekte Mehrfach-Eltern-Darstellung des Konkretisierungsgraphen,
- vollständige Zuordnung aller aktuellen Katalogfelder zu UI-Bereichen,
- expliziter Editiermodus ohne Autosave,
- Neuanlage und Deaktivierung statt physischem Löschen,
- Beziehungspflege ohne Drag-and-drop-Nebeneffekte,
- Ausschlussregelpflege,
- begrenzte und atomare Bulk-Aktionen,
- optimistisches Locking auf Aggregatsebene,
- Audit-Trail,
- vom `participant` getrennte Administrationsidentität,
- Spring-Security-basierter Zugriffsschutz,
- Validierungs- und Konfliktdarstellung,
- konkrete Low-Fidelity-Wireframes,
- benötigte Application-Use-Cases und Datenprojektionen.

**Erfülltes Gate:**

- für den Start der Administrationsimplementierung bleiben keine blockierenden UX- oder Datenmodellentscheidungen offen,
- notwendige Schemaergänzungen sind benannt, aber nicht vorzeitig implementiert,
- die Webumsetzung ist in sechs getrennte Pakete zerlegt.

## Phase 3: Administrationssicherheit und Schreibfundament (abgeschlossen mit Issue #7)

Dieses Paket schafft die technischen Voraussetzungen für alle späteren schreibenden Verwaltungsfunktionen, ohne bereits Katalog-CRUD zu implementieren.

### Scope

- Spring Security für `/admin/**`,
- konfigurationsbasierte Administrationsidentitäten mit sicher bereitgestellten Passwort-Hashes,
- keine Default-Zugangsdaten,
- aktivierbarer Administrationsadapter,
- `version bigint not null default 0` auf `ingredient_concept` und `exclusion_rule`,
- `catalog_audit_entry` einschließlich Gruppierung von zusammengehörenden Änderungen,
- interne Locking- und Auditgrundlagen im Katalogmodul,
- PostgreSQL-Integrationstests für Versionierung und Audit,
- Sicherheits- und Modulgrenzentests.

### Nicht enthalten

- produktive Katalogformulare,
- Hierarchie- oder Suchoberfläche,
- fachliche Bearbeitungscommands außerhalb der für die Infrastruktur nötigen Testfälle.

### Gate

- `/admin/**` ist ohne Authentifizierung nicht nutzbar,
- bei deaktiviertem Adapter sind keine Admin-Zugangsdaten erforderlich,
- bei aktiviertem Adapter führt fehlende sichere Konfiguration zu einem klaren Startfehler,
- Versionierungs- und Auditschema ist über echtes PostgreSQL geprüft,
- Passwörter und Sessiondaten gelangen nicht in Auditdaten.

## Phase 4: Lesende Katalogverwaltung (abgeschlossen mit Issue #9)

Die erste sichtbare Webstufe validiert Informationsarchitektur und Query-Projektionen ohne fachliche Schreibzugriffe.

### Scope

- Thymeleaf-/HTMX-Webshell,
- Katalog als Startseite nach Login,
- Suche und Schnell-/Detailfilter,
- Hierarchie mit Mehrfach-Eltern,
- paginierte Listenansicht,
- vollständige read-only Zutaten-Detailansicht,
- Reverse-Referenzen aus Ausschlussregeln,
- Erhalt von Such-/Filterzustand bei Navigation,
- Fallback für kleinere Displays.

### Gate

- alle aktuellen Katalogeigenschaften sind sichtbar,
- Mehrfach-Eltern werden mit realen Katalogdaten korrekt dargestellt,
- ein Konzept kann über Suche und verschiedene Hierarchiepfade erreicht werden, ohne dass sich sein fachlicher Zustand unterscheidet,
- MVC- und PostgreSQL-Integrationstests decken Suche, Filter und Hierarchie ab,
- Controller greifen nicht direkt auf JDBC zu.

### Phase 4a: Nachschärfung der Katalogoberfläche mit Issue #12

Vor den ersten Schreibflows wird die in Phase 4 entstandene Webshell funktional und gestalterisch konsolidiert. Das Zwischenpaket verändert weder Fachlogik noch Persistenz, sondern schafft den belastbaren visuellen Ausgangspunkt für die folgenden Bearbeitungsmasken.

#### Scope

- kompakte, zugängliche `+`-/`−`-Schalter für lazy geladene Hierarchieäste einschließlich erneutem Einklappen,
- grafische Fünfer-Skalen für Ungewöhnlichkeit und alle acht kulinarischen Dimensionen,
- expliziter eigener Zustand für nicht gepflegte Skalenwerte,
- lokal ausgelieferte SVG-, CSS- und JavaScript-Assets,
- konsistentes warmes, modernes und zurückhaltend kulinarisches Designsystem,
- Erhalt von Split-View, URL-Zustand, HTMX-Navigation und Responsive-Fallback,
- MVC-/Fragmenttests für Toggle-, Skalen- und Assetverhalten.

#### Gate

- Hierarchieäste laden direkte Kinder nur beim ersten Öffnen und lassen sich danach ohne weiteren Request ein- und ausklappen,
- grafische Skalen besitzen immer fünf Referenzsymbole und unterscheiden aktive und inaktive Stufen nicht nur durch Farbe,
- fehlende Werte erscheinen weiterhin ausdrücklich als `nicht gepflegt`,
- alle Frontend-Artefakte werden mit der Anwendung ausgeliefert,
- Phase 5 bleibt der nächste fachliche Entwicklungsschritt und erhält keine vorgezogenen Schreibfunktionen.

## Phase 5: Zutatenkonzept-Basisbearbeitung (abgeschlossen mit Issue #11)

Dieses Paket führt die ersten produktiven Schreibzugriffe ein.

### Scope

- Neuanlage eines Zutatenkonzepts,
- Anzeigename,
- Aktivstatus,
- Ziehbarkeit,
- Spezifität,
- Ziehungsgewicht,
- Ungewöhnlichkeit,
- Kuratornotiz,
- stabiler Code nur bei Neuanlage editierbar,
- explizites Speichern/Verwerfen,
- Umgang mit ungespeicherten Änderungen,
- optimistisches Locking und Konfliktansicht,
- Audit für Anlage und Änderungen,
- verständliche Datenbank-Constraint-Fehler.

### Erfülltes Gate

- konkurrierende Änderungen überschreiben sich nicht stillschweigend,
- Codeänderungen nach Anlage sind über die normale Weboberfläche ausgeschlossen,
- Deaktivierung erhält Beziehungen und historische Referenzen,
- unbekannte Datenbankfehler werden nicht als fachliche Konflikte maskiert,
- jeder erfolgreiche Schreibzugriff erzeugt einen korrekten Audit-Eintrag.

## Phase 6: Konkretisierungsbeziehungen (abgeschlossen mit Issue #21)

Dieses Paket macht den Graphen schreibend pflegbar.

### Scope

- direkte Oberbegriffe und Konkretisierungen bearbeiten,
- suchbarer Parent-/Child-Picker,
- Mehrfach-Eltern vollständig erhalten,
- transitive Vorfahren/Nachfahren als read-only Kontext,
- Vorabprüfung auf Selbstbeziehungen, Duplikate und Zyklen,
- PostgreSQL-Trigger bleibt letzte Integritätssicherung,
- Versionierung aller vom Save betroffenen Aggregate,
- PostgreSQL-seitige Serialisierung aller Graphmutationen,
- ein Audit-Eintrag pro betroffenem Konzept mit gemeinsamer `change_group_id`.

### Gate

- Hinzufügen einer Beziehung entfernt keine andere Beziehung stillschweigend,
- echte Zyklus-, Write-Skew- und überlappende Versionskonkurrenzfälle sind gegen PostgreSQL getestet,
- inaktive Konzepte können nach sichtbarer Bestätigung in Beziehungen verbleiben,
- die UI unterscheidet direkte und transitive Beziehungen eindeutig.

## Phase 7: Rollen, Eigenschaften, Beschaffbarkeit und Saison (abgeschlossen mit Issue #24)

Dieses Paket vervollständigt die Zutatenpflege.

### Scope

- funktionale Rollen,
- kulinarische Flags,
- kulinarische Dimensionen,
- Beschaffbarkeit für Georgia und Tobias,
- Saisonmultiplikatoren,
- vollständige Pflichtvalidierung aktiver Ziehkandidaten,
- `Pflegebedarf`-Filter,
- gemeinsamer atomarer Save mit Basisfeldern und vorgemerkten Beziehungen,
- PostgreSQL-Graphlock für Relations-, Rollen- und Spezifitätswrites.

Die Stammdaten `functional_role`, `culinary_flag`, `culinary_dimension` und `participant` bleiben in dieser Phase migrationsgeführt und werden nicht selbst per Web administriert.

### Gate

- aktive ziehbare Konzepte können nicht ohne Rolle und vollständige Beschaffbarkeit gespeichert werden,
- aktive ziehbare offene Konzepte dürfen ohne bekannte direkte Konkretisierung gespeichert werden,
- fehlende Dimensionswerte bleiben semantisch `nicht gepflegt`,
- fehlende Saisonwerte bleiben semantisch Faktor 1.0,
- Änderungen sind versionsgesichert und auditiert.

## Phase 8: Ausschlüsse, Bulk und Auditoberfläche (abgeschlossen mit Issue #30)

Dieses Paket schließt die vollständige Katalogverwaltung ab.

### Scope

- Ausschlussregeln einschließlich mehrerer Ziele und `include_refinements`,
- Neuanlage, Bearbeitung und Deaktivierung von Ausschlussregeln,
- begrenzte Bulk-Aktionen für explizit ausgewählte Zutatenkonzepte,
- atomare Bulk-Verarbeitung,
- Auditliste, Filter und feldweiser Diff,
- Entity-bezogene Änderungshistorie in den Detailansichten.

### Gate

- aktive Ausschlussregeln besitzen mindestens ein Ziel,
- Bulk-Aktionen verändern höchstens 200 explizit ausgewählte Konzepte und sind vollständig atomar,
- Auditdaten machen normale redaktionelle Änderungen ohne Roh-JSON verständlich nachvollziehbar,
- die in [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md) beschriebene erste vollständige Webverwaltung ist funktional abgedeckt.

## Phase 9: Generierungsregeln und Kandidatengenerator (abgeschlossen mit Issue #40)

Phase 9 liefert einen reproduzierbaren, historienbewussten und kontrolliert zufälligen Zwölfer-Satz. Der spätere Kurator erhält ausschließlich harte gültige und als Satz ausreichend diverse Kandidaten; er ist nicht dafür verantwortlich, einen schwachen Zufallsgenerator zu retten.

Verbindliche Hauptquelle ist [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md). ADR 0007 hält die Architekturentscheidung fest. Die spätere Mehrfachauswahl und Kuratororchestrierung ist davon getrennt in [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md) spezifiziert.

### Phase 9A: Spezifikation und Datenreife (Issue #33)

- Generatorbegriffe, Datenfluss und Verantwortungsgrenze,
- effektive Gewichte, Cooldowns und Ausschlussmodus,
- Profile und harte Kandidatenregeln,
- versioniertes Score- und Ähnlichkeitsmodell,
- Neuigkeitskadenz und diverse Zwölfer-Auswahl,
- deterministischer RNG-, Snapshot- und Replayvertrag,
- Lifecycleentscheidung `generation_batch` getrennt von `curation_round`,
- Simulations- und Kalibrierungsvertrag,
- reproduzierbare Messung der Katalogdatenreife.

Das Repository-Baseline-Gate ist bestanden: Rollen, Neuigkeit und Beschaffbarkeit sind im aktiven Ziehpool vollständig; Graph und Rollenpools sind ausreichend groß. Kulinarische Dimensionen bleiben wegen ihrer lückenhaften Abdeckung optional und dürfen in Generatorversion 1 nur niedrig gewichtete Softsignale liefern. Ein zusätzliches Metadatenpaket vor Phase 9B ist nicht erforderlich.

### Phase 9B: Deterministischer Einzelkandidaten-Kern (Issue #34)

- öffentliche kanonische Generatorprojektion des Katalogmoduls,
- typisierte fail-fast validierte Konfiguration,
- `SPLITMIX64_V1` und benannte Substreams,
- Generation Context und effektive Gewichte,
- Profile, Proposal-Erzeugung und Hard Rules,
- Candidate Evaluation und stabile Reason-Codes,
- reine öffentliche Challenge-API ohne Persistenz.

### Phase 9C1: Attempt-Vorbereitung und Reservoir (Issue #35)

- bereits materialisierten `VisibleHistorySnapshot` als reine Fachdateneingabe konsumieren,
- Cooldowns, Neuigkeitskadenz und Attempt-Ausschlussentscheidung genau einmal pro Attempt ableiten,
- unveränderte attempt-weite Entscheidungen in beliebige batch-spezifische #34-Contexts überführen,
- begrenzte Reservoir-Erzeugung ausschließlich über `CandidateProposalEngine`,
- kanonische Signaturdeduplizierung sowie Proposal-, Treffer-, Duplicate- und Hard-Rejection-Metriken,
- alle eindeutigen hard-valid Kandidaten unabhängig vom späteren Soft-Mindestscore behalten,
- typisierte Erschöpfung unter zwölf eindeutigen Kandidaten,
- repräsentative feste Seeds und Monate gegen die reale PostgreSQL-Katalogprojektion replayen,
- keine Historien-JDBC-Projektion und keine Liquibase-Änderung.

### Phase 9C2: Satzdiversität und Baselinesimulation (abgeschlossen mit Issue #47)

- öffentliche transportneutrale Batch-API auf dem vorhandenen `CandidateReservoirEngine`,
- versionierte siebenkomponentige Kandidatenähnlichkeit mit expliziter Nichtvergleichbarkeit,
- MMR-ähnliche kontrolliert stochastische Zwölfer-Auswahl aus den projizierten `GenerationPlan`-Zielen,
- geordnete Soft-Fallbacks mit Neustart vom leeren Satz und typisierter Set-Erschöpfung,
- finaler kanonischer Set-Fingerprint einschließlich Batch-Seed, Setdiagnose und Auswahlreihenfolge,
- vollständige 2.304-Attempt-CI-Baselinesimulation; das 64-Seed-/9.216-Attempt-Gate bleibt Phase 9F.

### Phase 9D: Persistenz und Generation Lifecycle (Issue #36)

Die Phase-9D-Commands erzeugen ausschließlich Batch 1; Schema und interne Persistenzgrenze erlauben unter demselben eingefrorenen Context genau Batch 2 und verhindern Batch 3.

- append-only Migration auf `generation_batch`,
- historisch vollständige PostgreSQL-Materialisierung und Persistenz des `VisibleHistorySnapshot`,
- atomare Persistenz von zwölf Kandidaten, Snapshots und Diagnosen,
- öffentliche Generation Commands und Queries,
- Replay gegen gespeicherte Versionen und Snapshots,
- Idempotenz, Konkurrenz, Retry und Restart gegen echtes PostgreSQL,
- keine Kuratorauswahl und keine sichtbare Challenge,
- die Persistenz darf die für Phase 10 benötigten höchstens zwei Kurationsrunden, kandidatenübergreifenden Carry-over-Referenzen und ein finales Multi-Offer-Set nicht durch eine starre Ein-Batch-/Ein-Selected-Kardinalität verbauen.

### Finaler Katalog-Snapshot vor Phase 9E (Issue #52)

Vor dem Generator-Labor wird der redaktionell geprüfte Produktionskatalog als kanonische Deployment-Baseline festgeschrieben. Das Paket ist ein Daten- und Migrationspaket, keine neue Generatorphase:

- Fresh Build und Upgrade der freigegebenen Produktions-Fixture konvergieren auf denselben fachlichen SHA-256-Fingerprint,
- bestehende IDs bleiben erhalten und unbekannte Ausgangszustände werden vor Schreibzugriffen abgelehnt,
- Salzigkeit und der genehmigte breite Dimensions-Backfill werden in Projektion und Administration sichtbar,
- Graph, Rollen, Ausschlüsse, Saisonwerte und neue Kernkonzepte werden final kuratiert,
- Generatoralgorithmen, Hard Rules, Phase-9-Konfiguration und Fallbacklogik bleiben unverändert.

Issue #37 und Issue #40 bleiben eigenständige nachgelagerte Pakete; ihre Labor-, Simulations- und Kalibrierungsumfänge werden durch den Katalog-Snapshot nicht vorgezogen.

### Phase 9E1: Generator-Labor und Diagnostik (Issue #37)

- geschützte, strikt nicht persistierende Vorschau,
- verständliche Kandidaten- und Setdiagnosen einschließlich PairAssessment-basierter Paarerklärung,
- read-only Anzeige persistierter Batches ausschließlich aus ihren Snapshots,
- Replaydarstellung mit strukturierter erster Abweichung,
- keine Simulations- oder Kalibrierungslogik.

### Phase 9E2: Begrenzte Simulations- und Reportlogik (Issue #53, abgeschlossen)

- öffentliche transportneutrale `GeneratorSimulation` mit expliziten Seeds, Fall-/Deadline-/Abbruchgrenzen und
  harter 4.096-Fälle-Grenze,
- ein unter `REPEATABLE READ` eingefrorener Katalog-/Historien-Snapshot pro Lauf und danach strikt sequenzielle,
  schreibfreie Ausführung über den gemeinsamen Preview-/Generator-Kern,
- synthetische Mehrwochensequenzen mit expliziter sichtbarer Kandidatenposition sowie separater Erschöpfungs- und
  Technikfehlerbehandlung,
- begrenzter kanonischer JSON-Report mit Versions-, Seed- und Katalogidentität; Laufzeit bleibt außerhalb des
  kanonischen Fingerprints,
- kleines CI-Szenarioset und die vorhandene explizite Issue-#47-Baseline delegieren an denselben Simulations- und
  Aggregationskern,
- kein Adminadapter, kein fachliches Tuning und kein Phase-9F-Abnahmegate.

### Phase 9E3: Adminadapter für Simulation (Issue #54, abgeschlossen)

- dünne geschützte Verwaltungsansicht über den gemeinsamen Simulationskern mit maximal 64 expandierten Fällen,
  fester serverseitiger Fünf-Minuten-Deadline und `FAIL_FAST`,
- deterministische Single-Step-Monatsexpansion, ID→Code-Auflösung nur über `CatalogQueries` sowie 0–2 Manuals,
- REROLL-Diagnostik ausschließlich über Attempt-Typ und sichtbares Historienszenario; keine editierbaren REROLL-Block-IDs,
- Full-Page-/No-JS-POST und HTMX-Ergebnisfragment mit CSRF- und flüchtigem Session-Doppelstartschutz,
- keine Duplizierung von Generator-, Statistik- oder Persistenzlogik und keine operativen Generation-/Challenge-Writes.

### Phase 9F: Kalibrierung und Abschlussgate (Issue #40, abgeschlossen)

- feste automatisierte Szenario- und Seedmatrix,
- reproduzierbarer Repository-Baseline-Lauf,
- nicht schreibender operativer Kataloglauf mit Fingerprint,
- manuelles fachliches Abnahmekorpus,
- Ursachenanalyse vor jeder Konfigurationsänderung,
- ausschließlich evidenzbasiertes Tuning innerhalb des spezifizierten Modells,
- verbindlicher Kalibrierungsbericht.

Die alten breiten Kalibrierungsprofile und Berichte sind mit Issue #97 entfernt. Der aktuelle Nachweis verwendet
kleine feste 1.2-Szenarien im normalen Verify.

### Erfülltes Abschlussgate

Phase 9 ist abgeschlossen:

- Hard Rules, Scores, Diversität und Replay sind implementiert,
- Seed, Versionen und vollständige Eingabe-/Konfigurationssnapshots werden persistiert,
- PostgreSQL-, Konkurrenz-, Restart- und Replaytests sind grün,
- die ausgeführten fokussierten und operativen Simulationen zeigen keine Hard-Rule-Verletzung oder unkontrollierte Erschöpfung,
- der operative Kataloglauf ist dokumentiert,
- und die repräsentative Seed-Auswahl wurde ausdrücklich fachlich abgenommen.

Die isolierten Generator-Labor-UX-Nacharbeiten aus Issues #60 und #61 verändern den fachlichen Phase-9-Abschluss nicht.

### REROLL-Semantik

- Ein freiwilliger REROLL verwirft das präsentierte Offer Set als Kombination und ist kein Zutaten-Ablehnungssignal.
- Der normale Cooldown bleibt exakt codebasiert; Parent-, Child-, Refinement- oder Sibling-Expansion findet nicht statt.
- Labor und Simulation besitzen keine operativen REROLL-Blockfelder.
- Die persistente Exposition eines vollständig rerollten sichtbaren Offer Sets mit 1–3 Optionen ist in Phase 11A umgesetzt.

OpenAI-Aufruf, Kuratorauswahl, sichtbare Challenge und Discord bleiben außerhalb von Phase 9. Die künftige Historienprojektion unterscheidet bestätigte Challenges, Cooldown-only-Exposition rerollter Offer Sets und vollständig interne/nicht gewählte Kandidaten ausdrücklich.

## Phase 10: Begrenzte Kuratierung, Multi-Offer-Lifecycle und OpenAI-Adapter

Verbindliche Fachquelle ist [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md). Phase 10 baut auf dem vollständig kalibrierten Phase-9-Generator auf und trennt Kuratorbewertung, präsentierbares Offer Set und tatsächlich bestätigte Challenge sauber voneinander.

### Phase 10A: Kuratorvertrag und persistenter Offer-Lifecycle

Dieses Paket schafft die fachliche und persistente Grenze noch ohne Discord-Adapter.

#### Scope

- `requested_offer_count` im Bereich `1..3`, Default `1`, als Session-/Kurationsparameter,
- strukturierter Kuratorrequest ohne Rezept- oder Vorgabenerfindung,
- strukturierte Candidate-Bewertung `GOOD`, `ACCEPTABLE`, `BAD` mit Rang und Reason-Codes,
- `curation_round_candidate` oder gleichwertige relationale Referenz für `NEW`, `CARRY_OVER` und `LOCKED_CONTEXT`,
- finales `curated_offer_set` mit exakt der angeforderten Zahl positionsgebundener Angebote,
- mindestens ein `GOOD` als Voraussetzung für ein erfolgreiches Offer Set,
- persistierbarer Präsentationszustand des Offer Sets als Vorbereitung auf Bestätigung oder den einmaligen freiwilligen Reroll,
- fachlich saubere Möglichkeit, ein vollständig präsentiertes und später rerolltes Offer Set samt seinen exakten Candidate-/Requirement-Snapshots historisch zu referenzieren,
- klare Trennung von Generatorstatus, Kuratorstatus, Offerstatus und späterer Challenge-Bestätigung,
- Replay-/Auditdaten für Request, Response, Modell, Promptversion, Bewertungen und Auswahlpfad.

#### Gate

- eine Kurationsrunde kann Kandidaten aus mehreren Generation Batches desselben Attempts nachvollziehbar referenzieren,
- ein erfolgreiches Offer Set enthält exakt `1..3` Angebote und mindestens einen `GOOD`,
- ein unvollständiges oder ausschließlich `BAD`/`ACCEPTABLE` enthaltendes Ergebnis wird nicht als erfolgreicher Offer-Satz maskiert,
- die persistente Grenze kann später unterscheiden, ob ein Offer Set niemals präsentiert, normal durch Bestätigung beendet oder vollständig rerollt wurde,
- noch kein Discord- oder OpenAI-Netzwerkadapter ist für die fachlichen Tests erforderlich.

#### Erfüllter Stand mit Issue #71

- `requested_offer_count` ist ein unveränderlicher Sessionwert im Bereich `1..3` und bleibt außerhalb des Generatorcontexts, der Generatorversion und des Set-Fingerprints,
- `CurationCommands`/`CurationQueries` bilden den transportneutralen Request-/Responsevertrag, explizite Fehlerabschlüsse und read-only Auditprojektionen ab,
- `curation_round_candidate` trennt `NEW`, `CARRY_OVER` und `LOCKED_CONTEXT` relational und PostgreSQL-seitig von Legacydaten,
- `curated_offer_set` und `curated_offer` erzwingen atomar exakt die gewünschte Anzahl, Positionen und mindestens ein `GOOD`,
- echte PostgreSQL-Tests decken Migration, Legacyerhalt, strukturierte Fehler, Integrität und konkurrierende Round-/Offer-Starts ab,
- produktiv entsteht ausschließlich `CURATED_UNPRESENTED`; kein Adapter, keine Orchestrierung und keine Offer-Exposition wurden vorgezogen.
- Nacharbeit PR #72 härtet die terminale Attempt-State-Machine, die verbindliche Round-1-/Round-2-Form, Raw-`INVALID_RESPONSE`-Auditdaten und statusunabhängige Offer-Vollständigkeit ab; sie bleibt weiterhin reine Phase 10A.

### Phase 10B: OpenAI-Adapter und strikt gedeckelte Kurationsorchestrierung

Dieses Paket implementiert den tatsächlichen externen Kurator und die höchstens zweistufige Orchestrierung.

#### Scope

- erster Generation Batch mit zwölf Kandidaten und genau ein Kuratorrequest im Normalfall,
- unmittelbarer Abschluss, sobald mindestens `requested_offer_count` geeignete `GOOD`-Optionen vorliegen,
- bei zu wenigen `GOOD` höchstens eine zweite Generation unter demselben Attempt,
- `GOOD` aus Runde 1 verbindlich locken,
- höchstens so viele beste `ACCEPTABLE`/`BAD`-Fallbacks aus Runde 1 als Carry-over behalten, wie Plätze fehlen,
- zweiter Kuratorrequest mit Locked-Kontext, Carry-over und zwölf neuen Kandidaten,
- nach Runde 2 keine weitere Qualitätsrunde,
- bei mindestens einem `GOOD` fehlende Plätze deterministisch nach Kuratorrang mit `ACCEPTABLE` und anschließend den am wenigsten problematischen `BAD` auffüllen,
- ohne irgendeinen `GOOD` typisierte Kurationserschöpfung und kein Offer Set,
- technisch **höchstens zwei tatsächliche externe Requests pro `generation_attempt`**, einschließlich Retries,
- automatische Client-Retries deaktivieren oder vollständig in dasselbe Budget integrieren,
- Netzwerkaufrufe außerhalb offener Datenbanktransaktionen.

#### Gate

- der Normalfall benötigt genau einen externen Request,
- kein fachlicher oder technischer Pfad kann einen dritten Request erzeugen,
- ein technischer Retry verbraucht dasselbe Budget wie eine zweite Qualitätsrunde,
- bereits gelockte gute Kandidaten werden durch Runde 2 nicht verdrängt,
- ein brauchbarer Carry-over aus Runde 1 kann einen schlechteren neuen Kandidaten schlagen,
- nach erfolgreicher Kuratierung existiert exakt die gewünschte Zahl von Angeboten,
- noch nicht präsentierte oder normal nicht gewählte Angebote erzeugen keinerlei Generatorhistorie.

#### Erfüllter Stand mit Issue #73

- `CurationOrchestrationCommands` führt den transportneutralen, restartfähigen Use Case bis zu `CURATED_UNPRESENTED`, typisierter Kurations-/Generatorerschöpfung oder einem sichtbaren technischen Kuratorfehler.
- Eine persistierte Dispatch-Claim pro Runde erteilt genau einmal die Berechtigung zu einem externen Request. `CLAIMED`, `RESULT_RECORDED` und `UNKNOWN_EXTERNAL_OUTCOME` sind irreversible PostgreSQL-Zustände; damit bleiben auch Konkurrenz, Prozessabbruch und Restart innerhalb des Attempt-Budgets von zwei Requests.
- Der OpenAI-Adapter verwendet die Responses API direkt, ohne SDK-, HTTP- oder versteckte Client-Retries, mit `store=false`, deaktiviertem Streaming/Background-Modus, strengem JSON-Schema, `CURATOR_PROMPT_V2`, Standardmodell `gpt-5.6-terra` und Reasoning `medium`. Gemäß ADR 0008 erfordert er zusätzlich zur expliziten Aktivierung das Spring-Profil `production`; widersprüchliche Konfiguration scheitert beim Start.
- Providerrequest, Raw-Response beziehungsweise Transportfehler, Response-ID, Tokenverbrauch und Diagnose werden auf der tatsächlich verbrauchten Runde auditiert. Der Netzwerkzugriff liegt zwischen zwei kurzen Datenbanktransaktionen.
- Batch 2 wird ausschließlich aus dem verifizierten, gespeicherten Context Snapshot desselben Attempts berechnet. Runde-1-`GOOD`s bleiben Locked Context; nur die besten benötigten `ACCEPTABLE`/`BAD` werden Carry-over; alle zwölf Kandidaten aus Batch 2 sind `NEW`.
- Ein technischer Fehler in Runde 1 kann den zweiten Request als `TECHNICAL_RETRY` verbrauchen und schließt damit eine Qualitätsrunde aus. Ungültiger strukturierter Output wird nie erneut gesendet. Bei technischem Fehler der Qualitätsrunde oder erschöpftem Batch 2 gilt der dokumentierte Runde-1-Fallback nur, wenn dort mindestens ein `GOOD` vorliegt.
- Lokale HTTP-Adaptertests und echte PostgreSQL-Tests decken Statusklassen einschließlich Responses-`failed`, Header und Timeouts, Prompt/Schema, Konkurrenz, gespeicherte Result-Replays bei deaktiviertem Adapter, unklare Crash-Ausgänge, Migration und die Ein-/Zwei-Request-Pfade ab. Der normale Build ruft OpenAI nicht auf.
- Discord-Präsentation, Bestätigung, sichtbare Challenge, freiwilliger Reroll und dessen Historienexposition verbleiben vollständig in Phase 11.

## Phase 11: Entscheidung über kuratierte Angebote

### Phase 11A: Persistenter, transportneutraler Offer-Decision-Lifecycle (Issue #76)

Phase 11A stellt ausschließlich die öffentlichen Challenge-Application-APIs für Präsentation, Bestätigung, Reroll und Resume bereit. Sie materialisiert die Präsentation genau einmal, bestätigt genau einen `curated_offer` als operative `challenge` und verwendet dabei die Offer-Referenz statt des Legacy-Felds `is_selected` als Fachautorität.

Ein Reroll markiert das vollständige präsentierte Set atomar als `REROLLED` und persistiert genau eine Exposition mit den exakten damaligen Requirement-Codes. Diese Snapshot-Exposition erweitert nur den Zutaten-Cooldown um genau eine Historienposition: ohne Refinement-Expansion und ohne Neuigkeitskadenz. Anschließend setzt sie den bestehenden Generation-/Kurationspfad mit einem `REROLL`-Attempt fort; nach jedem Commit- oder Crashfenster ist derselbe Workflow idempotent fortsetzbar. PostgreSQL schützt die Zustandsübergänge, die Referenzen, die Vollständigkeit und Confirm-vs.-Reroll-Konkurrenz.

Phase 11A enthält ausdrücklich keinen Discord-SDK-, Gateway-, Command-, Button-, Message- oder User-ID-Code.

### Phase 11B: Transportneutraler Voting-/Participation-Core (erfüllt mit Issue #81)

Phase 11B folgt der verbindlichen Spezifikation [`CHALLENGE_VOTING_AND_PARTICIPATION.md`](CHALLENGE_VOTING_AND_PARTICIPATION.md): `SelectionVotingCommands`/`SelectionVotingQueries` materialisieren generische externe Teilnehmeridentitäten, den stabilen Default-Electorate-Snapshot aus den Codes `GEORGIA` und `TOBIAS`, geheime bis zum Abschluss veränderbare Votes, Mehrheitsauswertung und genau einen persistenten Tie-Break. Das Ergebnis wird vor der anschließenden 11A-Wirkung gespeichert; `resume` setzt Confirm, Reroll, Reroll-Fortschritt sowie Participation-Initialisierung nach Restart idempotent fort. Der Presentation-Handshake behandelt ein Offer Set erst nach gemeldeter tatsächlicher Auslieferung als präsentiert. Challenge-Teilnahme bleibt eine getrennte, später erweiterbare Relation. 11B orchestriert ausschließlich über die öffentlichen Phase-11A-APIs und enthält weder Discord- noch andere Transporttypen.

### Scope

- Electorate-Snapshot und geheime, bis zum Abschluss änderbare Votes,
- persistente Mehrheitsauswertung und einmaliger Tie-Break,
- erste und optionale zweite Voting-Runde nach Reroll,
- getrennte Challenge-Teilnahme mit späteren Beitritten,
- keine Veränderung des Generator-, Kurator- oder Historienvertrags aus 11A.

### Phase 11C: Dünner Discord-Adapter (erfüllt mit Issue #83)

Erst Phase 11C rendert Offers und Votingstatus, transportiert Slash-/Component-Interaktionen und ordnet Discord-IDs zu den 11B-Teilnehmern zu. Der Adapter verwendet ausschließlich die öffentlichen APIs aus 11A und 11B; er besitzt keine eigene Generator-, Kurator-, Voting-, Fallback- oder Persistenzlogik.

Issue #83 ergänzt die kleine öffentliche `ChallengeOfferPreparationCommands`-Fassade für den normalen INITIAL-Generation-/Kurationspfad. Der Discord-Adapter deferiert Interaktionen vor längerer Arbeit, verwendet einen begrenzten adaptereigenen Executor und aktiviert Vote-Components erst nach erfolgreicher sichtbarer Auslieferung und dem unveränderten 11B-Presentation-Handshake. Previews, Smoke-Tests und der deaktivierte Standardbetrieb starten weder JDA noch erhalten sie Discord-Secrets.

### Gate

- Discord darf nur transportieren und rendern,
- ein Vote, eine Entscheidung oder ein Reroll wird niemals außerhalb der 11A-/11B-Application-Services persistiert,
- die in 11A garantierte Offer-Autorität, exakte Cooldown-Exposition und Confirm-vs.-Reroll-Serialisierung bleiben unverändert.

## Phase 12: Produktionsnahe Live-Validierung und privater Pilot

Phase 12 ergänzt keine Challenge-Fachfunktion. Sie validiert den nach Phase 11 nutzbaren Kern mit einer festen,
serverseitigen Acceptance-Instanz, getrennten Providersecrets und nachvollziehbarer manueller Evidenz. Echte
Discord- und OpenAI-Aufrufe bleiben ausdrücklich außerhalb von CI, Maven-Tests, Previews und Smokes.

### Phase 12A: Isolierte Live-Acceptance und Secret-Härtung (Issue #86)

- festes Compose-Projekt `med-acceptance`, eigener Loopback-Port, eigener Instanzordner und eigenes PostgreSQL-Volume,
- ausschließlich `runtime/acceptance.properties` für Acceptance-Testbot und Acceptance-OpenAI-Projektkey,
- Produktion behält `discord.properties` und kann zusätzlich `openai.properties` nutzen; keine Instanz erhält die
  Properties eines anderen Instanztyps,
- explizit providerfreie Preview- und Smoke-Konfiguration sowie `production`-Profil nur für Produktion und Acceptance,
- Preflight, sichere Diagnose, Acceptance-Backup, read-only SQL und bestätigungspflichtiger Acceptance-Reset,
- Runbook, Evidenztemplate und Kosten-/Latenzprotokoll unter [`PRODUCTION_VALIDATION.md`](PRODUCTION_VALIDATION.md),
- Offline-Tests für Secret-Isolation, Port-/Projekt-/Volume-Trennung, Legacy-Runtime und Reset; keine Live-Providercalls.

### Phase 12B.5A: Kandidatenspezifischer Restriction-Core (Issue #93)

Issue #93 schließt eine replayrelevante Kernkorrektur vor der manuellen Abnahme ab, ohne Discord-Bedienung oder
Darstellung vorwegzunehmen:

- Generator `1.2.0` führt `AUTO` (deterministisch 20 %), `NONE` und `REQUIRED` als persistierten Sessionmodus ein;
- Rule-Auswahl, Hard-Konflikte, Candidate-Signatur und Satzdiversität sind kandidatenspezifisch und versioniert;
- `CURATION_CONTRACT_V2`/`CURATOR_PROMPT_V2`, Offer, bestätigte Challenge und Reroll-Exposition tragen den
  unveränderlichen Restriktionssnapshot;
- echte PostgreSQL-Tests prüfen Snapshotkopien, History und Migration.

Discord-Commands, Buttons, Nachrichten oder Darstellung gehören ausdrücklich nicht zu diesem Paket und bleiben
Gegenstand des nachfolgenden Adapters.

### Phase 12B.5A.1: Entfernen der Legacy-Kompatibilität (Issue #97)

Issue #97 reduziert den Generator auf die aktuelle 1.2-Architektur:

- entfernt die obsoleten Generator-, attempt-weiten Ausschluss- sowie Curation- und Prompt-Pfade;
- entfernt obsolete Schemaanteile ausschließlich durch eine vorwärtsgerichtete Liquibase-Migration;
- behält Katalog- und Administrationsdaten sowie aktuelle 1.2-Snapshots, Restriction Mode und Candidate Restrictions;
- vereinfacht Labor, Simulation und Tests auf `AUTO`, `NONE` und `REQUIRED` ohne künstliche Kompatibilitätsmatrix.

### Phase 12B.5B: Discord-Adapter (Issue #94)

Erst danach ergänzt Issue #94 Command-Option, Darstellung und dünne Discord-Adapterintegration. Dieser Schritt ist
nicht Teil von Issue #97.

### Phase 12D.5: Lesende Discord-Zutatenabfrage (Issue #108)

Vor dem privaten Produktionspilot ergänzt Issue #108 den zusätzlichen Guild-Command `/zutat suche:<Suchtext>` als
rein lesenden, invokergebundenen Discord-Ablauf. Die kleine öffentliche `catalog :: api`-Projektion durchsucht nur
aktive sichtbare Namen literaler, case-insensitiver Teilstringsuche und liefert ausschließlich die freigegebenen
aktuellen Profildaten mit direkten aktiven Beziehungen. Exakte Treffer und einzelne Teilstringtreffer werden sofort
als kompaktes Embed angezeigt; sonst bleiben höchstens 25 priorisierte Optionen in einem stateless String-Select
öffentlich sichtbar. Weder der bestehende `/challenge`-Flow noch Generator, Kurator, Voting, Audit, Persistenz oder
OpenAI werden dadurch berührt.

### Phase 12D.5.1: Zutaten-Card und Hierarchienavigation (Issue #111)

Nach der Live-Abnahme von #108 verdichtet Issue #111 die Zutaten-Card vor dem privaten Pilot: Basisdaten stehen kompakt
unter dem Titel, Funktion und besondere Eigenschaften verwenden native Inline-Embed-Felder und die direkte Hierarchie
steht erst am Ende der Card. Aktive direkte Eltern und Kinder tragen in derselben read-only `catalog :: api`-Projektion
stabile Konzept-IDs und sind stateless direkt navigierbar. Die Navigation lädt das Zielprofil frisch per ID und führt
keine Namenssuche aus. Suchsemantik, Challenge-Lifecycle, Persistenz, Generator, Kurator und Providerzugriffe bleiben
unverändert. Die Acceptance-Abnahme bestätigt Card-Aufbau, Inline-Felder und die ID-basierte In-place-Navigation.

### Phase 12D.5.2: Einheitliche Hierarchie-Dropdowns (Issue #113)

Issue #113 vereinheitlicht die in #111 eingeführte Hierarchienavigation: Jede nicht leere Beziehungsrichtung verwendet
unabhängig von der Zahl der Ziele genau ein String-Select, auch bei nur einem Ziel. Eltern und Kinder bleiben getrennte
Dropdowns; bei leeren Beziehungen erscheint kein Navigationselement. Die Auswahl ersetzt weiterhin dieselbe Discord-
Nachricht, verwendet unmittelbar die stabile Konzept-ID und bleibt auf höchstens 25 sichtbare Ziele je Richtung
begrenzt. Der nicht mehr benötigte Zutaten-Navigationsbutton-Pfad entfällt; Challenge-Buttons und sämtliche Katalog-,
Such-, Persistenz-, Generator-, Kurator- und Providersemantik bleiben unverändert. Danach folgt Phase 12E / #90.

### Phase 12D.5.3: Discord-Operator-Autorisierung (Issue #115)

Vor dem privaten Produktionspilot trennt Issue #115 die Berechtigung, `/challenge` zu starten, von fachlicher
Teilnahme und Electorate: Ausschließlich Mitglieder einer separat konfigurierten Discord-Operator-Rolle dürfen den
Command in der konfigurierten Guild ausführen. `/zutat` bleibt innerhalb dieser Guild für jedes Mitglied nutzbar,
aber sowohl die anfängliche Trefferauswahl als auch jede Hierarchienavigation bleiben stateless an den jeweiligen
Card-Owner gebunden. Voting-, Participation-, Generator-, Kurator- und Persistenzsemantik bleiben unverändert.

### Phase 12B–12E: Manuelle Inbetriebnahme, Abnahme und Pilot (Issues #87–#90)

Die folgenden Pakete führen erst nach der isolierten 12A-Basis manuelle Discord-/OpenAI-Smokes, die vollständigen
1..3-Offer- und Voting-Flows, Restart-/Recovery-/Backup-Szenarien sowie den privaten Produktionspilot durch. P0- oder
P1-Befunde stoppen diese Folge bis zu einem separaten Fix. Die gemeinsame Evidenzregel bleibt: keine Tokens, API-Keys,
vollständigen Authorization-Header oder unredigierten Runtime-Dateien dokumentieren.

Persönliche Konkretisierungen, Zusatz-Zutaten, Grundpläne und Ergebnisdokumentation folgen in späteren Paketen.

## Phase 13: Öffentliche Challenge-Historie und Challenge-Cards

Phase 13 beginnt erst nach Abschluss von Phase 12E / #90 und dem Release `v0.1.0`. Verbindliche Fachquelle ist [`CHALLENGE_ARCHIVE_AND_CARDS.md`](CHALLENGE_ARCHIVE_AND_CARDS.md).

Die Phase bleibt bewusst kleiner als der später mögliche persönliche Challenge-Lifecycle. Sie macht ausschließlich bestätigte Challenge-Fakten dauerhaft öffentlich abrufbar und erlaubt die optionale Zuordnung einer außerhalb des Bots erzeugten Challenge-Card.

### Phase 13A: Transportneutraler Archiv-/Card-Core (abgeschlossen mit Issue #140)

- positive, eindeutige und unveränderliche öffentliche `challenge_number`,
- deterministischer Backfill bestehender Challenges ab `1`,
- global konkurrenzsichere und transaktionale Nummernvergabe ohne Verbrauch durch Rollbacks,
- kleine öffentliche Challenge-Archivprojektion für aktuelle Challenge, Detail und paginierte Historie,
- ausschließlich Requirement-/Restriction-Snapshots; keine Offer-, Voting-, Reroll-, Kurator- oder Providerhistorie,
- optionale Eins-zu-eins-Challenge-Card als valides `1200 × 1200`-PNG bis `5 MiB`,
- dauerhafte PostgreSQL-`bytea`-Persistenz mit SHA-256 und Metadaten,
- ausdrückliches Setzen, Ersetzen und Entfernen über transportneutrale APIs,
- echte PostgreSQL-Migrations-, Konkurrenz-, Rollback- und Binärdatentests,
- keine Discord-Typen oder Rollenprüfung.

### Phase 13B: Discord-Archiv und operatorgebundene Card-Verwaltung (abgeschlossen mit Issue #141)

- neuer Root-Command `/challenges` getrennt vom vorhandenen `/challenge`,
- guild-weite öffentliche Subcommands `aktuell`, `liste` und `anzeigen`,
- zehn Listeneinträge pro Seite, neueste zuerst und aktuelle Challenge markiert,
- gemeinsame Detaildarstellung aus Nummer, Bestätigungsdatum, vier Snapshots und Restriction,
- optionale Auslieferung der persistierten Card als natives Discord-Attachment,
- `karte-setzen` und `karte-entfernen` ausschließlich für die vorhandene Operatorrolle,
- Autorisierung vor Attachment-Download und Core-Mutation,
- keine automatische Erkennung normal geposteter Bilder und keine zusätzlichen privilegierten Intents,
- dünner Adapter ausschließlich über `challenge :: api`,
- keine persönliche Planung oder Ergebnisdokumentation.

### Gate

- bestätigte Challenges besitzen stabile öffentliche Nummern,
- `aktuell` wechselt erst durch eine neue erfolgreich materialisierte Challenge,
- Liste und Detail zeigen ausschließlich bestätigte Snapshots,
- Cards bleiben optionale Darstellung und verändern keine Challenge-Fakten,
- guild-weite Leserechte und operatorgebundene Schreibrechte sind getrennt,
- Migration, Konkurrenz, Restart und Discord-Grenzen sind automatisiert geprüft,
- Phase 13 zieht keine persönlichen Konkretisierungen, Zusatz-Zutaten oder Ergebnisse vor.

## Bewusste Nicht-Ziele der nahen Pakete

- mehrere unabhängig deployte Dienste,
- separates Frontend-Repository,
- öffentliche Registrierung oder Unterstützung beliebig vieler Nutzer,
- allgemeine Lebensmittelontologie,
- Rezeptgenerierung als Ersatz für die Challenge-Idee,
- frei konfigurierbare Datenbank-Rule-Engine,
- physisches Löschen von Katalogobjekten über die normale Webverwaltung,
- Webadministration der kleinen Referenzvokabulare,
- mehr als drei gleichzeitig angebotene Challenges,
- unbeschränkte Kurator-/Retry-Schleifen,
- drei voneinander unabhängige Zwölfer-Generierungen nur deshalb, weil drei Angebote gewünscht wurden,
- persönliche Konkretisierungen, Zusatz-Zutaten, Kochpläne oder Ergebnisdokumentation in Phase 13,
- automatische Challenge-Card-Erzeugung oder Bilderkennung im Bot,
- frühzeitige Implementierung späterer Komfortfunktionen ohne tragfähige Kernabläufe.
