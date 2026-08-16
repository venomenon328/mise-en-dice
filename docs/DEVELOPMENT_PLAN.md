# Entwicklungsplan

Stand: 13. August 2026

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

## Phase 9: Generierungsregeln und Kandidatengenerator

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
  fester serverseitiger 30-Sekunden-Deadline und `FAIL_FAST`,
- deterministische Single-Step-Monatsexpansion, ID→Code-Auflösung nur über `CatalogQueries` sowie 0–2 Manuals und
  explizitem REROLL-Viererblock,
- Full-Page-/No-JS-POST und HTMX-Ergebnisfragment mit CSRF- und flüchtigem Session-Doppelstartschutz,
- keine Duplizierung von Generator-, Statistik- oder Persistenzlogik und keine operativen Generation-/Challenge-Writes.

### Phase 9F: Kalibrierung und Abschlussgate (Issue #40)

- feste automatisierte Szenario- und Seedmatrix,
- reproduzierbarer Repository-Baseline-Lauf,
- nicht schreibender operativer Kataloglauf mit Fingerprint,
- manuelles fachliches Abnahmekorpus,
- Ursachenanalyse vor jeder Konfigurationsänderung,
- ausschließlich evidenzbasiertes Tuning innerhalb des spezifizierten Modells,
- verbindlicher Kalibrierungsbericht.

Der technische Lieferstand verwendet dafür ein eigenes, im normalen Verify ausgeschlossenes Profil
`generator-calibration`. Rohreports bleiben unter `target/generator-calibration/`; das versionierte Acht-Satz-Korpus,
die Bewertungsrubrik und die operative Schrittfolge stehen in
[`CANDIDATE_GENERATOR_CALIBRATION.md`](CANDIDATE_GENERATOR_CALIBRATION.md). Phase 9 bleibt ausdrücklich offen, bis
der Administrator den redaktionellen Katalog über den #54-Adapter geprüft und das Korpus fachlich abgenommen hat.

### Abschlussgate

Phase 9 ist erst abgeschlossen, wenn:

- Hard Rules, Scores, Diversität und Replay implementiert sind,
- Seed, Versionen und vollständige Eingabe-/Konfigurationssnapshots persistiert werden,
- PostgreSQL-, Konkurrenz-, Restart- und Replaytests grün sind,
- breite Simulationen keine Hard-Rule-Verletzung oder unkontrollierte Erschöpfung zeigen,
- der operative Kataloglauf dokumentiert ist,
- und eine repräsentative Seed-Auswahl ausdrücklich fachlich abgenommen wurde.

OpenAI-Aufruf, Kuratorauswahl, sichtbare Challenge, Discord und freiwilliger Reroll-Dialog bleiben außerhalb von Phase 9. Nicht gewählte spätere Kurationsangebote zählen ausdrücklich nicht zum `VisibleHistorySnapshot`.

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
- klare Trennung von Generatorstatus, Kuratorstatus, Offerstatus und späterer Challenge-Bestätigung,
- Replay-/Auditdaten für Request, Response, Modell, Promptversion, Bewertungen und Auswahlpfad.

#### Gate

- eine Kurationsrunde kann Kandidaten aus mehreren Generation Batches desselben Attempts nachvollziehbar referenzieren,
- ein erfolgreiches Offer Set enthält exakt `1..3` Angebote und mindestens einen `GOOD`,
- ein unvollständiges oder ausschließlich `BAD`/`ACCEPTABLE` enthaltendes Ergebnis wird nicht als erfolgreicher Offer-Satz maskiert,
- noch kein Discord- oder OpenAI-Netzwerkadapter ist für die fachlichen Tests erforderlich.

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
- nicht gewählte Angebote erzeugen keinerlei Generatorhistorie.

## Phase 11: Discord-Bot für Ziehung, Auswahl, Bestätigung und Reroll

Der eigenständige Discord-Adapter verwendet ausschließlich die öffentlichen Challenge-Application-APIs aus Phase 10. Er besitzt keine eigene Generator-, Kurator-, Fallback- oder Persistenzlogik.

### Scope

- vor der Ziehung kompakte Auswahl `1`, `2` oder `3` Angebote; Default `1`,
- Erzeugung über denselben fachlichen Generation-/Kurations-Use-Case unabhängig von der gewählten Zahl,
- übersichtliche Darstellung exakt der kuratierten Angebote,
- Auswahl genau einer stabilen Candidate-/Offer-ID,
- explizite Bestätigung vor Erzeugung der sichtbaren Challenge,
- nur die bestätigte Challenge fließt in Cooldown und Neuigkeitskadenz ein,
- nicht gewählte Angebote bleiben auditierbar, sind generatorisch aber „nicht gesehen“,
- gemeinsamer einmaliger Reroll der bestätigten Challenge,
- Reroll verwendet dieselbe gewünschte Optionszahl und blockiert nur die vier Vorgaben der tatsächlich bestätigten ursprünglichen Challenge.

### Gate

- bloßes Anzeigen von Angeboten erzeugt noch keine `challenge`,
- Manipulation von Discord-IDs kann keinen Kandidaten außerhalb des aktuellen Offer Sets bestätigen,
- genau eine Option wird atomar bestätigt,
- nicht gewählte Optionen beeinflussen weder normalen Cooldown noch Reroll-Hardblock,
- der freiwillige Reroll bleibt genau einmal gemeinsam möglich und ist von internen Kurationsrunden getrennt.

Persönliche Konkretisierungen, Zusatz-Zutaten, Grundpläne und Ergebnisdokumentation folgen in späteren Paketen.

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
- frühzeitige Implementierung späterer Komfortfunktionen ohne tragfähige Kernabläufe.
