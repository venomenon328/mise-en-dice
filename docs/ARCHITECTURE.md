# Architektur von Mise en Dice

Stand: 12. August 2026

Dieses Dokument beschreibt die verbindliche Zielarchitektur für die nächsten Entwicklungsabschnitte. Die Produktregeln stehen in [`VISION.md`](VISION.md), das fachliche Datenmodell in [`DATA_MODEL.md`](DATA_MODEL.md). Einzelne Architekturentscheidungen werden zusätzlich unter [`adr`](adr) begründet.

## 1. Architekturziele

Die Architektur soll insbesondere folgende Ziele unterstützen:

- eine gemeinsame fachliche Logik für Weboberfläche und Discord-Bot,
- eine einzige persistente Quelle für Katalog und Challenge-Historie,
- nachvollziehbare und reproduzierbare Datenbankänderungen,
- einfache lokale Entwicklung und ein überschaubares Deployment,
- klare Modulgrenzen ohne vorschnelle Verteilung auf mehrere Dienste,
- gute Testbarkeit mit der tatsächlich verwendeten Datenbank,
- spätere Erweiterbarkeit, ohne die erste Version mit Infrastruktur zu überfrachten.

Mise en Dice ist zunächst ein privates System für zwei bekannte Personen. Die Architektur soll solide, aber nicht demonstrativ groß sein. Ein Microservice-Zoo wäre für diesen Anwendungsfall keine Zukunftssicherheit, sondern hauptsächlich zusätzlicher Tierpflegeaufwand.

## 2. Systemzuschnitt

### 2.1 Ein Repository

Datenbankdefinition, gemeinsame Fachlogik, Webverwaltung und Discord-Adapter liegen im selben Repository `venomenon328/mise-en-dice`.

Ein separates Datenbank-, Bot- oder Frontend-Repository ist zunächst nicht vorgesehen. Änderungen am Datenmodell betreffen regelmäßig auch Persistenzcode, Validierung, Verwaltungsoberfläche oder Generatorlogik. Ein gemeinsames Repository erlaubt, solche Änderungen atomar zu entwickeln, zu testen und zu veröffentlichen.

### 2.2 Modularer Monolith

Die Anwendung wird als modularer Spring-Boot-Monolith umgesetzt. Zu Beginn entsteht genau ein deploybares Artefakt und ein Anwendungsprozess.

```text
Browser ───────► Web-Adapter ───────┐
                                    │
Discord ───────► Discord-Adapter ───┼──► Application- und Domain-Module
                                    │                │
                                    │                ├──► PostgreSQL
                                    │                └──► externer Kurator
                                    │
                                    └── ein Spring-Boot-Prozess
```

Web und Discord sind eingehende Adapter. Sie greifen nicht direkt auf Tabellen oder auf interne Implementierungen anderer Module zu, sondern verwenden öffentliche Application-APIs.

Der OpenAI-Zugriff ist ein ausgehender Adapter hinter einer anwendungsinternen Schnittstelle. Modellname, Promptversion und Transportdetails dürfen nicht Teil der fachlichen Challenge-Logik werden.

### 2.3 Spätere Trennung nur bei einem echten Betriebsgrund

Eine Aufteilung in mehrere Prozesse oder Repositories wird erst geprüft, wenn mindestens einer der folgenden Gründe tatsächlich eintritt:

- Web und Bot benötigen unabhängig voneinander deutlich andere Releasezyklen,
- ein Adapter muss unabhängig skaliert oder besonders isoliert betrieben werden,
- verschiedene Teams verantworten die Komponenten dauerhaft getrennt,
- Sicherheits- oder Verfügbarkeitsanforderungen verlangen getrennte Prozesse,
- das gemeinsame Deployment verursacht nachweislich betriebliche Probleme.

Die bloße Existenz zweier Benutzeroberflächen ist kein solcher Grund.

## 3. Technischer Rahmen

Für das erste Anwendungsfundament gelten folgende Vorgaben:

- Java 21,
- Maven inklusive Maven Wrapper,
- Spring Boot 4.1.x mit aktueller kompatibler Patchversion zum Umsetzungszeitpunkt,
- Spring Modulith zur Prüfung fachlicher Modulgrenzen,
- Spring JDBC für explizite relationale Persistenz,
- PostgreSQL als einzige unterstützte Laufzeitdatenbank,
- Liquibase als einzige Autorität für Schema- und Baseline-Datenänderungen,
- Testcontainers für PostgreSQL-Integrationstests,
- Docker Compose für die lokale PostgreSQL-Instanz.

Eine serverseitig gerenderte Weboberfläche soll später mit Spring MVC, Thymeleaf und gezielten HTMX-Interaktionen umgesetzt werden. Eine separate Single-Page-Application ist für die erste Version nicht vorgesehen.

## 4. Anwendungsmodule

Die konkrete Paketstruktur darf während des ersten Entwicklungspakets sinnvoll geschärft werden. Die fachlichen Grenzen sind jedoch verbindlich.

```text
io.github.venomenon328.miseendice
├── catalog
│   ├── api
│   └── internal
├── challenge
│   ├── api
│   └── internal
├── administration
│   └── internal
├── discord
│   └── internal
└── bootstrap
```

### 4.1 `catalog`

Das Katalogmodul besitzt das Zutatenwissen und alle unmittelbar dazugehörenden Regeln:

- Zutatenkonzepte,
- Konkretisierungsbeziehungen,
- funktionale Rollen,
- kulinarische Flags und Dimensionen,
- individuelle Beschaffbarkeit,
- Saisonfaktoren,
- Ausschlussregeln,
- katalogbezogene Validierung.

Es stellt nach außen Commands, Queries und unveränderliche Projektionen bereit. Andere Module erhalten keine frei navigierbaren Persistence-Objekte.

### 4.2 `challenge`

Das Challenge-Modul besitzt:

- Sessions und Generierungsversuche,
- Kandidatengenerierung,
- Kuratierungsrunden,
- ausgewählte sichtbare Challenges,
- Reroll-Semantik,
- Challenge-Historie,
- die Schnittstelle zum externen Kurator.

Es darf die öffentliche API des Katalogmoduls verwenden. Ein direkter Zugriff auf interne Katalog-Repositories oder Tabellen ist unzulässig.

Für den Generator stellt `catalog` eine eigene unveränderliche Projektion bereit. Sie materialisiert Rollen, Neuigkeit, Beschaffbarkeit, Saison, Graphbeziehungen, Ausschlussziele und Snapshotwerte in kanonischer Reihenfolge. Das Challenge-Modul bleibt Eigentümer von Gewichtung, Profilen, Hard Rules, Scores, Reservoir, Satzdiversität, Cooldowns und Generation Lifecycle.

Proposal-Erzeugung und Satzselektion sind reine Fachberechnungen auf einem unveränderlichen `GenerationContext`. Sie benötigen keine offene Datenbanktransaktion. Zufall wird ausschließlich über den in ADR 0007 festgelegten Seed-/Substream-Vertrag injiziert.

Die reine Phase 9C1 bereitet einen Attempt aus bereits materialisierten Snapshots vor. Neuigkeitskadenz und
Ausschlussentscheidung sind attempt-weit und bleiben für alle internen Batchnummern identisch; nur die
Proposal-Substreams sind batch-spezifisch. Das Reservoir wird ausschließlich über die öffentliche
`CandidateProposalEngine`-API aufgebaut und enthält jeden eindeutigen harten gültigen Kandidaten unabhängig
vom späteren Soft-Mindestscore. Phase 9C2 konsumiert es über `CandidateReservoirEngine`, berechnet die
transportneutrale Paar- und Setdiagnose und liefert über `CandidateSetEngine` entweder genau zwölf Kandidaten
in Auswahlreihenfolge oder typisierte Erschöpfung. Proposal-, Hard-Rule-, Score-, Kadenz- und
Ausschlusslogik bleiben dabei Eigentum der vorgelagerten Fachpfade.

Die PostgreSQL-Projektion der vollständigen sichtbaren Historie gehört erst zu Phase 9D. Phase 9C1 erhält den
`VisibleHistorySnapshot` bereits materialisiert und darf fehlende historische Werte nicht aus dem aktuellen
Katalog ergänzen.

### 4.3 `administration`

Das Administrationsmodul ist der Webadapter für die private Datenpflege. Controller und Templates dürfen:

- Requests und Formulardaten entgegennehmen,
- Application-Commands aufrufen,
- Query-Ergebnisse als View Models darstellen,
- verständliches Validierungsfeedback liefern.

Fachliche Regeln, Transaktionsentscheidungen und SQL gehören nicht in Controller, Templates oder Browser-JavaScript.

### 4.4 `discord`

Das Discord-Modul übersetzt Discord-Interaktionen in Application-Commands und stellt Ergebnisse Discord-gerecht dar. Es besitzt keine eigene Generator-, Reroll- oder Persistenzlogik.

Der bestehende Gridwords-Bot wird nicht wiederverwendet. Mise en Dice erhält einen eigenständigen Discord-Bot-Adapter.

Der Adapter ist standardmäßig deaktiviert. Bei Aktivierung kapselt er JDA, Gateway-Lifecycle, Guild-Command-Registrierung, Interaktions-Acknowledgements und Renderer vollständig im `discord`-Modul; es verwendet ausschließlich `challenge :: api`. Vor der sichtbaren Vote-Aktivierung meldet er die erfolgreiche Auslieferung an den Phase-11B-Presentation-Handshake zurück. Discord-IDs bleiben opake externe Subjects, nicht Teil des Challenge-Modells.

### 4.5 `bootstrap`

Das Bootstrap-Paket enthält den Anwendungseinstieg und technische Konfiguration. Es darf Module zusammenbauen, aber keine Fachlogik aufnehmen.

## 5. Abhängigkeitsregeln

Folgende Regeln werden durch Struktur, Sichtbarkeit und Spring-Modulith-Tests abgesichert:

1. Adapter verwenden öffentliche Application-APIs und keine internen Persistence-Klassen.
2. Das Challenge-Modul darf die öffentliche Katalog-API verwenden; das Katalogmodul kennt das Challenge-Modul nicht.
3. Fachlogik kennt weder HTTP-, Discord- noch OpenAI-spezifische Transportobjekte.
4. Domain- und Application-Code hängt nicht von Thymeleaf, JDA oder konkreten API-Clients ab.
5. Ausgehende Netzwerkaufrufe erfolgen nicht innerhalb offener Datenbanktransaktionen.
6. Gemeinsame Logik wird nicht zwischen Adaptern kopiert und nicht über direkte Datenbankzugriffe geteilt.

## 6. Persistenzstrategie

### 6.1 PostgreSQL und explizites SQL

Das bestehende Modell nutzt PostgreSQL-spezifische Funktionen, Trigger, rekursive Abfragen, partielle Indizes und `jsonb`. Spring JDBC ist deshalb der bevorzugte Persistenzzugang.

Repositories sind explizite Adapter hinter Application- oder Domain-Schnittstellen. Abfragen sollen auf den jeweiligen Anwendungsfall zugeschnittene Projektionen liefern, statt einen frei navigierbaren Entity-Graphen aufzubauen.

JPA beziehungsweise Hibernate ist für das Anwendungsfundament nicht vorgesehen. Eine spätere Einführung bedarf einer neuen Architekturentscheidung und eines nachgewiesenen Nutzens.

### 6.2 Transaktionen

Transaktionsgrenzen liegen in Application Services. Ein Command wird entweder vollständig gespeichert oder vollständig verworfen.

Externe Kuratoraufrufe erfolgen außerhalb offener Transaktionen. Request und späteres Ergebnis werden in getrennten, kurzen Transaktionen persistiert. Fehler des externen Dienstes dürfen nicht als fachliche Datenbankkonflikte maskiert werden.

## 7. Liquibase und Datenverantwortung

### 7.1 Eine einzige Migrationsautorität

Liquibase ist der einzige Mechanismus für:

- Tabellen, Spalten, Indizes und Constraints,
- PostgreSQL-Funktionen und Trigger,
- strukturelle Datenmigrationen,
- stabile Referenzdaten,
- die einmalige Katalog-Baseline einer leeren Datenbank.

Das bisherige `db/bootstrap.sql` darf nach der Umstellung keine parallele Bootstrap- oder Migrationsstrecke bleiben.

Die vorhandenen PostgreSQL-Skripte dürfen als Liquibase-formatiertes SQL weitergeführt werden. PostgreSQL-spezifisches SQL soll nicht nur zur dekorativen Verwendung von XML in Tags nachgebaut werden.

### 7.2 Append-only-Changesets

Bereits veröffentlichte und ausgeführte Changesets werden nicht nachträglich verändert. Korrekturen und Erweiterungen erhalten neue Changesets mit stabilen IDs.

Der Master-Changelog bindet Dateien explizit und in nachvollziehbarer Reihenfolge ein. `includeAll` wird nicht verwendet.

### 7.3 Drei Datenklassen

Die Daten werden bewusst unterschieden:

1. **Schema und strukturelle Migrationen**: immer neue Liquibase-Changesets.
2. **Stabile Referenzdaten**: einmalige Changesets; spätere Änderungen über neue Changesets oder dafür vorgesehene Verwaltungsfunktionen.
3. **Initialer Zutatenkatalog**: einmalige Baseline für eine leere Datenbank.

Für die Katalog-Baseline wird weder `runAlways` noch ein anderer Mechanismus verwendet, der operative Änderungen bei einem Neustart erneut überschreibt.

### 7.4 Operative Quelle der Wahrheit

Nach Einführung der Webverwaltung ist die laufende PostgreSQL-Datenbank die Quelle der Wahrheit für redaktionelle Katalogänderungen. Die Baseline bleibt der Startstand für neue Installationen, nicht eine ständig erneut angewandte Sollkopie.

Regelmäßige Datenbank-Backups sind deshalb verpflichtender Bestandteil des Betriebs. Ein späterer JSON- oder CSV-Export kann Review, Transport und bewusste Rückführung geeigneter Änderungen in eine neue Baseline unterstützen, ersetzt aber kein Backup.

## 8. Webverwaltung

Die Webverwaltung wird vor dem Discord-Bot spezifiziert und voraussichtlich auch zuerst funktional umgesetzt. Dadurch kann der Katalog vor Entwicklung des Generators und des produktiven Bot-Flows komfortabel geprüft und gepflegt werden.

### 8.1 Oberflächenprinzipien

- zentrale Funktionen sind direkt erreichbar und nicht hinter tiefen Menüketten verborgen,
- Listenansicht, Suche, Filter, Hierarchie und Detailbearbeitung greifen sichtbar ineinander,
- häufig benötigte Eigenschaften sind in einer Ansicht bearbeitbar,
- seltene oder erklärungsbedürftige Einstellungen dürfen gruppiert, aber nicht versteckt werden,
- Fehler werden am betroffenen Feld und mit fachlich verständlicher Ursache angezeigt,
- Desktop-Nutzung ist prioritär; grundlegende Bedienbarkeit auf kleineren Displays bleibt erhalten.

### 8.2 Hierarchie ist fachlich ein Graph

Der Konkretisierungszusammenhang ist kein Baum. Ein Konzept kann mehrere Eltern besitzen und daher in mehreren Ästen einer hierarchischen Darstellung erscheinen.

Die Oberfläche darf diesen Graphen als aufklappbare Hierarchie visualisieren, aber nicht intern auf einen einzelnen Parent reduzieren. Insbesondere gilt:

- alle Eltern und Kinder eines Konzepts müssen erkennbar sein,
- zusätzliche Eltern sind ausdrücklich zu kennzeichnen,
- das Hinzufügen oder Entfernen einer Beziehung darf keine anderen Beziehungen stillschweigend ersetzen,
- Zyklusfehler sollen vor dem Speichern verständlich erklärt werden,
- der Datenbanktrigger bleibt die letzte Integritätssicherung.

Ein künstlicher `primary_parent_id` wird nicht allein für eine bequemere Darstellung eingeführt.

### 8.3 Schreibschutz, Konflikte und Audit

Vor den ersten schreibenden Katalogmasken müssen folgende Punkte spezifiziert und umgesetzt sein:

- optimistisches Locking für veränderliche Hauptobjekte,
- klare Regeln für Deaktivierung und Löschung,
- ein nachvollziehbarer Audit-Trail für redaktionelle Änderungen,
- eine von fachlichen Teilnehmern getrennte Administrationsidentität,
- Schutz der privaten Verwaltungsoberfläche.

Die genaue Tabellen- und UI-Ausgestaltung wird im Webentwicklungspaket festgelegt und ist nicht Bestandteil des reinen Anwendungsfundaments.

### 8.4 Administrationssicherheits- und Schreibfundament

Der optional aktivierbare Administrationsadapter schützt `/admin/**` mit Spring Security, Form-Login, einer serverseitigen Session und aktivem CSRF-Schutz. Seine ein oder zwei Identitäten stammen zunächst ausschließlich aus externer Konfiguration: stabiler `actor_key`, Anzeigename und BCrypt-Passworthash. Sie sind keine `participant`-Datensätze und es gibt weder eine Default-Anmeldung noch eine Datenbank-Benutzerverwaltung.

`ingredient_concept` und `exclusion_rule` besitzen eine Aggregatversion für optimistisches Locking. Spätere schreibende Application Services erhöhen sie innerhalb ihrer Transaktion ausschließlich beim erwarteten Versionswert. `catalog_audit_entry` speichert dafür fachliche JSONB-Vorher-/Nachher-Snapshots mit Akteur und `change_group_id`; sie enthält keine HTTP- oder Sicherheitsdaten und hat bewusst keine Fremdschlüssel auf Teilnehmer oder veränderliche Katalogobjekte.

Schreibende Änderungen an `ingredient_refinement` sowie Rollen- und Spezifitätsänderungen nehmen zusätzlich vor jeder Graphvalidierung einen stabilen PostgreSQL-Transaktions-Advisory-Lock. Dieser Lock serialisiert den vollständigen Graph-Read/Validate/Write-Ablauf auch zwischen mehreren Anwendungsprozessen; er ersetzt weder die deterministische Sperrung und Versionsprüfung aller betroffenen Zutatenaggregate noch den Zyklus-Trigger als letzte Datenbanksicherung.

## 9. Konfiguration und Betrieb

Konfiguration erfolgt über Spring-Boot-Properties und Umgebungsvariablen. Geheimnisse werden weder committed noch in Beispielkonfigurationen mit echten Werten abgelegt.

Web-, Discord- und Kuratoradapter müssen so gekapselt sein, dass nicht verwendete Adapter keine Zugangsdaten verlangen. Sobald ein Adapter aktiviert ist, führen fehlende oder ungültige Pflichtwerte zu einem klaren Startfehler.

Für die erste Ausbaustufe genügt ein Prozess mit einer PostgreSQL-Datenbank. Docker Compose stellt mindestens die lokale Datenbank bereit; die Anwendung kann lokal über Maven gestartet werden.

## 10. Teststrategie

### 10.1 Keine Ersatzdatenbank

Persistenztests verwenden eine echte PostgreSQL-Instanz über Testcontainers. H2 oder andere Ersatzdatenbanken sind nicht zulässig, weil sie die verwendeten PostgreSQL-Funktionen und Integritätsregeln nicht zuverlässig abbilden.

### 10.2 Testebenen

- Unit-Tests für reine Fachlogik,
- Spring-Modulith-Tests für Modulgrenzen und Zyklen,
- PostgreSQL-Integrationstests für Repositories, Migrationen, Funktionen und Trigger,
- MVC-Tests für die spätere Verwaltungsoberfläche,
- Adaptertests für Discord und den Kurator,
- wenige vollständige Ablauf-Tests für besonders wichtige Challenge-Flows.

### 10.3 Migrationsprüfung

Eine leere Testdatenbank muss durch Liquibase vollständig aufgebaut und mit der Baseline befüllt werden können. Die vorhandenen strukturellen Seed-Prüfungen werden in einen automatisierten PostgreSQL-Integrationstest überführt.

Mindestens folgende Risiken sind ausdrücklich zu testen:

- vollständiger Aufbau einer leeren Datenbank,
- erneuter Start ohne erneute Baseline-Überschreibung,
- Zyklusvermeidung im Konkretisierungsgraphen,
- Challenge-Integritätstrigger,
- Vollständigkeit der Rollen- und Beschaffbarkeitsdaten im aktiven Ziehungspool.

### 10.4 Generator-, Snapshot- und Kuratierungsfluss

Phase 9 trennt Generatorberechnung, Persistenz und spätere Kuratierung:

```text
Catalog API ──► Generation Context ──► reiner Generator-Kern
                                         │
                                         ├─► 9C1 Reservoir; 9C2 Zwölfer-Satz
                                         │
                                         ▼
                                kurze Persistenztransaktion
                                         │
                                         ▼
                                  generation_batch
                                         │
                                  außerhalb der Transaktion
                                         ▼
                                  externer Kurator
```

Katalog und sichtbare Historie werden je Attempt einmal in einer `REPEATABLE READ`-Lesetransaktion als unveränderlicher Context Snapshot materialisiert. Die PostgreSQL-Projektion verwendet bestätigte `challenge`-Zeilen mit ihren damaligen Requirement-Snapshots sowie die eine gespeicherte Snapshot-Exposition eines rerollten Offer Sets. Nur die zweite Quelle liefert exakte Codes für den Cooldown, ohne sich als Challenge oder Neuigkeitskadenz auszugeben; bloß generierte Kandidaten sind keine Exposition. Derselbe Snapshot dient allen internen Batches dieses Attempts sowie ihrer Persistenz; ein Satz oder eine spätere interne Neurunde mischt nicht unbemerkt zwei Katalogstände.

Das Modell führt `generation_batch` als eigene Ebene unter `generation_attempt` ein. Kandidaten gehören eindeutig zum Batch. Eine spätere `curation_round` besitzt erst dort Modell-, Prompt-, Request- und Responseinformationen und kann über die erst in Phase 10 eingeführte Teilnahmerelation Kandidaten beider Batches desselben Attempts referenzieren. Fake-Kuratormodelle und eine starre Runde→Batch-Kardinalität sind unzulässig.

Der vollständige erfolgreiche Zwölfer-Satz wird mit Attempt-Status, Kandidaten und Requirements atomar in einer kurzen Schreibtransaktion gespeichert; die reine `CandidateSetEngine`-Berechnung liegt außerhalb davon. Operationstoken, Lease, Zeilensperre und eindeutige Attempt-/Batchschlüssel tragen Retry, Konkurrenz und Restart. Erschöpfung ist ein fachliches Ergebnis; unbekannte PostgreSQL- oder Laufzeitfehler bleiben technisch. Replay verwendet ausschließlich gespeicherte Snapshots, Versionen, RNG und Seed, schreibt nichts und erzeugt keine neue Historienexposition.

Phase 10A ergänzt im Challenge-Modul die öffentlichen, transportneutralen APIs `CurationCommands` und `CurationQueries`. Ein Command plant genau eine `PENDING`-Runde mitsamt immutablem Request und Kandidatenteilnahmen in einer kurzen Transaktion. Ein getrenntes Command validiert und persistiert später die vollständige strukturierte Response oder beendet die Runde typisiert technisch beziehungsweise als strukturell ungültig. Weder diese API noch ihr JDBC-Adapter kennt OpenAI-, HTTP- oder SDK-Typen.

Das finale Offer Set entsteht ausschließlich aus einer explizit geordneten Liste persistierter Kuratorbewertungen in einer weiteren kurzen Transaktion. PostgreSQL sichert Attemptzugehörigkeit, vollständige Ränge sowie per deferrable Constraint die positionsgebundene Vollständigkeit und mindestens ein `GOOD`; der Application Service liefert die Idempotenz- und Konfliktsemantik. Die Phase enthält keinen Auswahlalgorithmus, keine zweite Generation oder Kuratororchestrierung und markiert keine tatsächliche Präsentation.

Die Phase-10A-Zustandsmaschine wird unter der Attempt-Zeilensperre geführt und zusätzlich durch PostgreSQL-Trigger geschützt: `OFFER_READY`, `EXHAUSTED` und Legacy-Historie sind nicht wieder eröffnungsfähig; eine Pending-Runde kann nicht gleichzeitig erschöpft und später doch abgeschlossen werden. Abschlussretries sind nur bei identischem bereits terminalem Payload idempotent, abweichende Daten bleiben Konflikte. Ein Transportadapter kann nicht lesbaren Output über den öffentlichen Raw-Invalid-Response-Command speichern; das ist eine fachliche `INVALID_RESPONSE` mit Originalpayload, kein maskierter technischer Fehler.

Phase 10B ergänzt `CurationOrchestrationCommands` als öffentliche, weiterhin transportneutrale Use-Case-Grenze. Die Orchestrierung plant und claimed eine Runde in kurzen Transaktionen, ruft danach den schmalen internen `CuratorClient` ohne offene Transaktion auf und persistiert das Providerergebnis in einer neuen kurzen Transaktion. Nur dieser eine Codepfad darf `dispatch` aufrufen. PostgreSQL macht die Requestberechtigung irreversibel und zählt `CLAIMED`, `RESULT_RECORDED` sowie unklare Crash-Ausgänge gegen das harte Attempt-Budget. Nach Restart wird ein gespeichertes Ergebnis erneut interpretiert; ein abgelaufener Claim wird als `UNKNOWN_EXTERNAL_OUTCOME` abgeschlossen und niemals erneut auf derselben Runde gesendet.

Phase 11A ergänzt daneben `OfferDecisionCommands` und `OfferDecisionQueries` als transportneutrale Entscheidungsgrenze. Die kurzen Transaktionen präsentieren ein `CURATED_UNPRESENTED`-Set genau einmal und schließen ein sichtbares Set ausschließlich durch Bestätigung eines autoritativen `curated_offer` oder durch den vollständigen Reroll. PostgreSQL sichert die Zustandsmaschine und die wechselseitige Ausschließlichkeit gegen echte Konkurrenz. Der Reroll persistiert vor dem Weiterlauf exakt einen sichtbaren Requirement-Snapshot; die normale Historienprojektion verwendet daraus nur präzise Codes als eine Cooldownposition, ohne Katalog-/Refinement-Auflösung oder Neuigkeitskadenz. Erst nach diesem Commit setzt der Application Service den vorhandenen Generation- und Kurationsorchestrierungspfad fort. Jeder danach erneut eingehende Command liest den dauerhaften Zwischenstand und kann ohne zweiten Reroll oder zweiten Generation-Attempt fortfahren. Der Phase-11A-Code kennt keine Discord- oder sonstigen Transportobjekte.

Phase 11B ergänzt danach ausschließlich die öffentlichen, transportneutralen APIs `SelectionVotingCommands` und `SelectionVotingQueries` für generische Teilnehmeridentitäten, feste Electorate-Snapshots, geheime veränderbare Votes, Mehrheitsauswertung, einmaligen persistierten Tie-Break sowie Challenge-Teilnahme gemäß `CHALLENGE_VOTING_AND_PARTICIPATION.md`. Ein bereits materialisierter Snapshot ist dabei autoritativ und bleibt trotz späterer Participant-Deaktivierung unverändert. Der Presentation-Handshake nimmt ausschließlich die Meldung einer tatsächlich erfolgreichen Auslieferung entgegen; erst dann ruft 11B `OfferDecisionCommands.present(...)` auf beziehungsweise rekonstruiert eine zuvor durch 11A persistierte Präsentation über `resume` und öffnet die passende Runde. Rundenergebnis und Tie-Break werden in einer kurzen Transaktion dauerhaft gespeichert. Erst danach bestätigt oder rerollt ein separater, idempotent fortsetzbarer Schritt über die öffentlichen 11A-APIs; dessen dauerhaft vorwärts gerichtete Apply-Zustände verhindern, dass ein verspäteter Reroll-Status weiter fortgeschrittene Ergebnisse überschreibt. Nach einer Bestätigung zieht er die Electorate-Mitglieder als Challenge-Teilnehmer nach, auch wenn sie seit dem Snapshot deaktiviert wurden; freiwillige spätere Joins bleiben aktivitätsgebunden. Der Core führt weder Discord-Transport noch eine parallele Challenge-/Historienpersistenz ein und maskiert keine unbekannten Datenbankfehler als Votingkonflikte. Erst Phase 11C wird ein dünner Discord-Adapter für Darstellung und Interaktion.

Der ausgehende OpenAI-Adapter verwendet einen eigenen retryfreien `RestClient` über die Responses API. Er kann nur bei expliziter Aktivierung im Spring-Profil `production` entstehen; eine widersprüchliche Aktivierung außerhalb dieses Profils scheitert beim Start gemäß ADR 0008. `CURATOR_PROMPT_V1` und ein strenges, kandidatengebundenes JSON-Schema erlauben ausschließlich qualitative Bewertung, Rang, feste Reason-Codes und kompakte Diagnosen; Tools, Speicherung, Streaming, Background- und Conversation-Zustand sind deaktiviert. HTTP-/OpenAI-Objekte verlassen den Adapter nicht. Providerfehler werden technisch klassifiziert, während strukturwidriger Output explizit `INVALID_RESPONSE` bleibt; unbekannte Datenbank-, Generator- oder Laufzeitfehler werden nicht in fachliche Erschöpfung oder Konflikte übersetzt.

Batch 2 wird durch einen eigenen Application Service ausschließlich aus dem verifizierten gespeicherten Attempt-Snapshot berechnet, außerhalb einer Transaktion und idempotent unter dem eindeutigen `(attempt, batch_number)`-Schlüssel gespeichert. Die Orchestrierung übernimmt Runde-1-`GOOD`s als Locked Context, begrenzt Carry-over nach offenen Plätzen und füllt das Ergebnis deterministisch nach `GOOD`, `ACCEPTABLE`, `BAD`. Phase-11-Zustände und Historienexposition werden dabei nicht berührt.

Das Generator-Labor aus Issue #37 und die nachgelagerten, getrennten Simulationspakete #53/#54 rufen denselben
reinen `GeneratorRunExecution`-Kern über bereits materialisierten Katalog- und Historien-Snapshots auf wie spätere
Adapter. Ein #53-Simulationslauf friert alle benötigten Monatskataloge und gegebenenfalls die produktive sichtbare
Historie einmal unter `REPEATABLE READ` ein; im anschließenden streng sequenziellen Fallloop gibt es weder JDBC noch
`SeedSource`, parallele Verarbeitung oder operative Writes. Der gemeinsame Report-/Aggregationskern ist die einzige
Grundlage für die explizite #47-Baseline. #54 darf ihn nur als begrenzten Adminadapter aufrufen und #40 nur für
Kalibrierung auswerten; beide schaffen keine zweite Generator-, Historien-, Hard-Rule- oder Statistikimplementierung.

Der #54-Adminadapter beschränkt diese öffentliche API zusätzlich auf 64 expandierte Fälle, eine feste serverseitige
30-Sekunden-Deadline und einen flüchtigen Session-Guard. Er übersetzt nur das Webformular und Katalog-IDs über
`CatalogQueries`, rechnet keine Metrik nach und besitzt keinen JDBC-Zugriff. #40 wertet denselben Kern ausschließlich
zur Kalibrierung aus; weder #54 noch #40 schaffen eine zweite Generator-, Historien-, Hard-Rule- oder
Statistikimplementierung.

## 11. Entwicklungsreihenfolge

Die aktuelle Reihenfolge ist:

1. laufenden Ausbau des initialen Zutatenkatalogs abschließen,
2. Anwendungsfundament mit Spring Boot, Modulen, Liquibase, PostgreSQL und Tests schaffen,
3. Webverwaltung fachlich und gestalterisch spezifizieren,
4. lesende und anschließend schreibende Katalogverwaltung umsetzen,
5. harte Generierungsregeln spezifizieren und Kandidatengenerator implementieren,
6. strukturierten Kuratorvertrag und OpenAI-Adapter umsetzen,
7. Discord-Flow für Ziehung und gemeinsamen Reroll umsetzen,
8. nachgelagerte persönliche Auswahl- und Ergebnisfunktionen ergänzen.

Die einzelnen Pakete sollen jeweils nur den für ihren Zweck notwendigen Umfang enthalten. Insbesondere zieht das Anwendungsfundament weder Generatorlogik noch Web-CRUD, JDA oder OpenAI-Integration vor.

## 12. Verwandte Entscheidungen

- [`ADR 0001`](adr/0001-single-repository-modular-monolith.md): ein Repository und modularer Monolith
- [`ADR 0002`](adr/0002-liquibase-as-single-migration-authority.md): Liquibase als einzige Migrationsautorität
- [`ADR 0003`](adr/0003-runtime-catalog-owned-by-postgresql.md): operative Katalogdaten gehören der Laufzeitdatenbank
- [`ADR 0004`](adr/0004-postgresql-only-persistence-tests.md): Persistenztests verwenden PostgreSQL
- [`ADR 0005`](adr/0005-server-rendered-administration-ui.md): serverseitig gerenderte Webverwaltung
- [`ADR 0006`](adr/0006-spring-jdbc-persistence.md): explizite Persistenz mit Spring JDBC
- [`ADR 0007`](adr/0007-seeded-two-stage-candidate-generator.md): seedbarer zweistufiger Kandidatengenerator und Trennung von Generation und Kuratierung
