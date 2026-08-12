# Entwicklungsplan

Stand: 12. August 2026

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
- grafische Fünfer-Skalen für Ungewöhnlichkeit und alle sieben kulinarischen Dimensionen,
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

Verbindliche Hauptquelle ist [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md). ADR 0007 hält die Architekturentscheidung fest.

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

### Phase 9C: Reservoir, Satzdiversität und Historie (Issue #35)

- sichtbare Historienprojektion,
- Cooldowns, Neuigkeitskadenz und Attempt-Ausschlussentscheidung,
- begrenzte Reservoir-Erzeugung,
- versionierte Kandidatenähnlichkeit,
- MMR-ähnliche kontrolliert stochastische Zwölfer-Auswahl,
- geordnete Soft-Fallbacks und typisierte Erschöpfung,
- reproduzierbarer Simulationsharness.

### Phase 9D: Persistenz und Generation Lifecycle (Issue #36)

- append-only Migration auf `generation_batch`,
- atomare Persistenz von zwölf Kandidaten, Snapshots und Diagnosen,
- öffentliche Generation Commands und Queries,
- Replay gegen gespeicherte Versionen und Snapshots,
- Idempotenz, Konkurrenz, Retry und Restart gegen echtes PostgreSQL,
- keine Kuratorauswahl und keine sichtbare Challenge.

### Phase 9E: Generator-Labor und Diagnostik (Issue #37)

- geschützte nicht persistierende Vorschau,
- verständliche Kandidaten- und Setdiagnosen,
- Kandidatenpaarvergleich,
- read-only Anzeige persistierter Batches,
- Replaydarstellung,
- begrenzte synchrone Simulation und reproduzierbarer Report.

### Phase 9F: Kalibrierung und Abschlussgate (Issue #40)

- feste automatisierte Szenario- und Seedmatrix,
- reproduzierbarer Repository-Baseline-Lauf,
- nicht schreibender operativer Kataloglauf mit Fingerprint,
- manuelles fachliches Abnahmekorpus,
- Ursachenanalyse vor jeder Konfigurationsänderung,
- ausschließlich evidenzbasiertes Tuning innerhalb des spezifizierten Modells,
- verbindlicher Kalibrierungsbericht.

### Abschlussgate

Phase 9 ist erst abgeschlossen, wenn:

- Hard Rules, Scores, Diversität und Replay implementiert sind,
- Seed, Versionen und vollständige Eingabe-/Konfigurationssnapshots persistiert werden,
- PostgreSQL-, Konkurrenz-, Restart- und Replaytests grün sind,
- breite Simulationen keine Hard-Rule-Verletzung oder unkontrollierte Erschöpfung zeigen,
- der operative Kataloglauf dokumentiert ist,
- und eine repräsentative Seed-Auswahl ausdrücklich fachlich abgenommen wurde.

OpenAI-Aufruf, Kuratorauswahl, sichtbare Challenge, Discord und freiwilliger Reroll-Dialog bleiben außerhalb von Phase 9.

## Phase 10: Strukturierter Kuratorvertrag und OpenAI-Adapter

Der Kurator erhält ausschließlich bereits gültige Kandidaten. Request, Response, Reason-Codes, Modell und Promptversion werden strukturiert und auditierbar behandelt.

Netzwerkaufrufe erfolgen außerhalb offener Datenbanktransaktionen. Vollständige Ablehnung eines Kandidatensatzes führt zu einer internen neuen Runde und verbraucht keinen sichtbaren Reroll.

## Phase 11: Discord-Bot

Der eigenständige Discord-Adapter verwendet dieselben Application-APIs wie die Weboberfläche. Die erste Bot-Stufe umfasst Ziehung, Anzeige und den gemeinsamen einmaligen Reroll.

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
- frühzeitige Implementierung späterer Komfortfunktionen ohne tragfähige Kernabläufe.
