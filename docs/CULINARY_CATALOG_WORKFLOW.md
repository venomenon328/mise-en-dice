# Kulinarischer Katalogausbau: Recherche, Freigabe und Einpflege

Stand: 11. September 2026
Status: Zielablauf für neue Länder- und Ergänzungsrunden nach dem in Issue #172 dokumentierten Einführungspunkt

## Ziel und Geltung

Die Redaktionssemantik aus
[CULINARY_COUNTRY_ASSOCIATIONS.md](CULINARY_COUNTRY_ASSOCIATIONS.md) wird kontrolliert auf den realen
Zutatenkatalog angewandt. Länderarbeit bleibt pro Land oder ausdrücklich beauftragter Ergänzungsrunde fachlich
nachvollziehbar; die technische Auslieferung darf mehrere vollständig freigegebene Runden in einem Batch bündeln.

Die Umstellung gilt für neue Runden erst ab dem ausdrücklich in
[#172](https://github.com/venomenon328/mise-en-dice/issues/172) dokumentierten Einführungspunkt. Bis dahin bleibt
der dort noch als gültig bezeichnete bisherige Ablauf maßgeblich. Der bestehende Branch
`feat/172-country-catalog-curation` und Draft-PR #247 werden durch diese Dokumentänderung weder ersetzt noch
zurückgesetzt, synchronisiert, gemergt oder anderweitig verändert. Ihr tatsächlicher Abschluss- und
Übergangspunkt wird in #172 festgehalten.

Nach der Einführung ist #172 die kompakte Übersicht über Runden, wichtige offene Themen und technische Batches.
Die aktuelle Entscheidungsquelle einer neuen Runde ist dagegen ihr zugehöriges Länder-/Ergänzungs-Issue nach
der Vorlage [`.github/ISSUE_TEMPLATE/culinary-country.md`](../.github/ISSUE_TEMPLATE/culinary-country.md).
Historische Kommentare unter #172 bleiben unveränderte Belege; es werden keine alten Länder-Issues auf Vorrat
erzeugt und keine früheren Freigaben wiederholt.

## Rollen der Arbeitsstände

- **Runden-Issue:** aktuelle fachliche Arbeits- und Entscheidungsquelle für genau ein Land oder eine ausdrücklich
  beauftragte Ergänzungsrunde. Beschlüsse werden aus Kommentaren zeitnah in den Body übernommen.
- **Freigabeschnappschuss:** fest verlinkter Kommentar mit Revision und vollständigem freigegebenem Delta,
  insbesondere exakten Notiztexten. Ein solcher Kommentar wird anschließend nicht editiert; Änderungen erhalten
  eine neue Revision und einen neuen Schnappschuss. Er ist Nachweis, kein zweiter aktueller Katalog.
- **#172:** kompakter Tracker für Rundenstatus, Altbelege, offene Themen und den aktuellen technischen Batch.
- **Technischer Batch:** beauftragter Branch/PR, der eine oder mehrere vollständig freigegebene Runden bündelt.
  Der Branch wird im jeweiligen Auftrag und in #172 genannt, nicht in wiederverwendbaren Prompts fest verdrahtet.
- **Repository-Katalogindex:** maschinenlesbare Rechercheprojektion eines ausgewiesenen Liquibase-Stands. Er ist
  weder zweite Katalogquelle noch Produktionsnachweis; sein Vertrag steht in
  [`catalog-index/README.md`](catalog-index/README.md).

Vor einer neuen Runde nach einem bereits passenden aktiven Issue suchen. Kein Issue erfinden, duplizieren oder
ohne ausdrücklichen Auftrag anlegen. Ein beauftragter Recherchelauf darf sein zugehöriges Runden-Issue bis Gate 1
pflegen; er darf daraus keine Katalog-, Migrations- oder Metadatenfreigabe ableiten.

## Phasen und Haltepunkte

| Phase | Eingabe | Ergebnis und Haltepunkt | Zulässige Änderung |
|---|---|---|---|
| 0. Technische Vorbereitung | Auftrag, aktueller Tracker, Runden-Issue, Branch-/PR-Stand, Index | Konsistenter Gesamtstand mit Quellenkennungen, gültigem Index und separat benannter offener Sammelarbeit | Runden-Issue und lokale Rechercheartefakte; nötige technische Synchronisierung nur im beauftragten Scope |
| 1. Fachliche Recherche | Vorbereiteter Bestand, Ländersemantik, Küchen- und Produktquellen | Küchenkontext, Kandidaten in beide Suchrichtungen, vollständige Existenzauflösung, getrennte Relations-/Aufnahmeempfehlungen; **Stopp vor Gate 1** | Runden-Issue einschließlich Evidenz und offener Fragen; keine Katalogdaten oder vollständigen Metadaten ungeklärter Neuaufnahmen |
| Gate 1. Menschliche Auswahl | Revisionsgebundene Recherchefassung | Explizit freigegebene Relationen und Neuaufnahmen; offene Kandidaten bleiben offen | Fest verlinkter Gate-1-Schnappschuss |
| 2. Metadatenentwurf, falls nötig | Gate-1-freigegebene Neuaufnahmen oder ausdrücklich geöffnete bestehende Metadaten | Vollständige Vorlage und exakte Texte; **Stopp vor Gate 2** | Runden-Issue/zugehöriger Anhang, noch keine Katalogpersistierung |
| Gate 2. Menschliche Metadatenfreigabe | Revisionsgebundener vollständiger Metadatenentwurf | Alle erforderlichen Werte, Kanten und exakten Texte freigegeben | Fest verlinkter Gate-2-Schnappschuss; bei reinen bestehenden Relationen ohne Metadatendelta entfällt Gate 2 |
| 3. Beauftragte Einpflege | Beide erforderlichen Gates, aktueller technischer Stand | Nur freigegebene append-only Deltas, einmalige Diff-QA, Commit und PR-Nachweis | Repositoryänderungen auf dem beauftragten Batchbranch |
| 4. Review und Batch-Abschluss | Konkreter Commit/PR und akkumuliertes Batch | Review am Head; vollständige technische Batchprüfung bei beauftragter Mergevorbereitung | Review-/PR-Dokumentation; Merge und Deployment nur mit eigener Befugnis |

Ein kombinierter oder bedingter Auftrag darf mehrere bereits ausreichend bestimmte Phasen abdecken. Er erzeugt
keine künstliche Wiederfreigabe, überspringt aber kein fachlich noch offenes Gate.

## Quellenmatrix und Revalidierung

Die Matrix verteilt die bisherige Pflichtlektüre auf den Zeitpunkt, an dem sie benötigt wird. `vollständig`
bedeutet vollständige aktuelle Fassung; ein genannter Abschnitt genügt nur, wenn die Matrix ihn ausdrücklich
begrenzt. Ein alter Prompt, Suchauszug oder Chattext ersetzt keine Pflichtquelle.

| Zeitpunkt | Pflichtquelle oder Pflichtabschnitt | Zweck | Erneut prüfen, wenn |
|---|---|---|---|
| Beginn jedes Auftrags und jeder Wiederaufnahme | [`AGENTS.md`](../AGENTS.md), lokale [`WORKFLOW.md`](dev-rules/WORKFLOW.md), [`PROJECT_PROFILE.md`](PROJECT_PROFILE.md), vollständiger aktueller Body des Runden-Issues sowie konkret benannter PR-/Reviewstand | Befugnis, Scope, Schutzgrenzen und aktueller Entscheidungsstand | Auftrag, Body, Branch, PR, Review oder eine dieser Regeln geändert wurde |
| Technische Vorbereitung | aktueller Body von [#172](https://github.com/venomenon328/mise-en-dice/issues/172), dieses Dokument, [`catalog-index/README.md`](catalog-index/README.md) und `catalog-index.manifest.json` vollständig | Runde, aktiven Batch, offene Sammelarbeit, Indexvertrag und Quellenumfang bestimmen | Tracker/Batch/Manifest geändert wurde oder der Indexvalidator keinen aktuellen passenden Stand bestätigt |
| Erste fachliche Phase | [`CULINARY_COUNTRY_ASSOCIATIONS.md`](CULINARY_COUNTRY_ASSOCIATIONS.md) vollständig; #165 als Ursprungsentscheidung bei erstmaliger Einführung, geänderter Normherkunft oder echtem Auslegungswiderspruch; [`VISION.md`, Abschnitt 7](VISION.md#7-zutaten--und-kategorienbasis); [`INITIAL_CATALOG.md`, Abschnitt 13](INITIAL_CATALOG.md#13-pflegeprinzip-für-neue-zutaten); [`DATA_MODEL.md`, Abschnitte 2–7](DATA_MODEL.md#2-zutatenkonzepte-statt-zutatkategorie-dichotomie) | Ländersemantik, Identität, Granularität, Graph, Aufnahmegrundsätze und vorhandene Metadaten verstehen | Norm-/Produktform-/Graphvertrag geändert wurde oder Identität/Umfang unklar ist |
| Kandidatenrecherche | belastbare aktuelle externe Quellen gemäß Ländersemantik; bei Zweifeln zusätzliche unabhängige Gegenrecherche | Rolle, Küchenkontext, Reichweite und Gegenbefunde belegen | Quelle die Entscheidung nicht mehr trägt, veraltet/unerreichbar ist oder ein materieller Gegenbefund erscheint |
| Vor jedem Metadatenentwurf | [`INGREDIENT_CONCEPT_CURATION.md`](INGREDIENT_CONCEPT_CURATION.md) und [`AVAILABILITY_AND_COOKING_NOVELTY.md`](AVAILABILITY_AND_COOKING_NOVELTY.md) vollständig; [finaler #188-Review](analysis/availability-novelty-final-review-v1-20260907.md) vollständig; passende tatsächlich freigegebene Zeilen des [finalen TSV](analysis/availability-novelty-final-review-v1-20260907.tsv) | Vollständige Konzeptvorlage, Exaktproduktbewertung, aktuelle Skalen, Personenprofile sowie Notiz-/Vergleichsanker | Produktform, Markt, Personenprofil, Fachregel oder betroffener Entwurf geändert wurde; historische Anker gelten nie als aktueller Händlernachweis |
| Vor technischer Einpflege | [`MODEL_SELECTION.md`](dev-rules/MODEL_SELECTION.md) und [`MODEL_CATALOG.md`](dev-rules/MODEL_CATALOG.md), [`ARCHITECTURE.md`, Abschnitte 6–7](ARCHITECTURE.md#6-persistenzstrategie), [`DATA_MODEL.md`, Abschnitte 13 und 16](DATA_MODEL.md#13-administrationsversionen), [ADR 0002](adr/0002-liquibase-as-single-migration-authority.md), [ADR 0003](adr/0003-runtime-catalog-owned-by-postgresql.md), [ADR 0010](adr/0010-remove-runtime-catalog-audit.md) sowie aktueller [Master-Changelog](../src/main/resources/db/changelog/db.changelog-master.yaml) und betroffener technischer Review vollständig beziehungsweise wie dort eingebunden | Modellauswahl für noch auszuführende Implementierung sowie Append-only-, Datenhoheits-, Transaktions-, Versions- und Integritätsvertrag | Basis, Changesets, operative Deltaauskunft, Zielbranch, Review oder Modellkatalog geändert wurde |
| Technische Prüfung und Mergevorbereitung | Projektprofil, [ADR 0004](adr/0004-postgresql-only-persistence-tests.md) und [ADR 0011](adr/0011-risk-based-ci-verification.md) sowie betroffene Workflows/Testhilfen | aktuelle risikobasierte Verify-/Deployment-Verify- und PostgreSQL-Regeln | Diffklasse, Workflow, Testinfrastruktur, Basis oder PR-Head geändert wurde |

Unveränderte freigegebene Teile werden bei Wiederaufnahme nicht pauschal neu geöffnet. Aktuelle
Beschaffungsrecherche wird trotzdem erneuert, soweit sie eine aktuelle Entscheidung trägt. Eine allgemeine
Implementierungsfreigabe ersetzt keine fachliche Einzelentscheidung.

## Phase 0: Technische Vorbereitung

### 0.1 Auftrag, Runde und Batch sichern

1. Runden-Issue, Land/Küchenumfang, bewusste Abgrenzungen, Phase und Revision feststellen.
2. Aktuelle Remote-Stände von `main`, beauftragtem Batchbranch und betroffenem PR prüfen; Commit-SHAs im Issue
   festhalten. Bestehende Arbeit erhalten, keine fremden Branches resetten oder force-pushen.
3. Bereits eingepflegte Sammelarbeit gehört in den materialisierten Recherchebestand. Nur freigegebene, aber noch
   nicht eingepflegte Neuaufnahmen werden aus ihren Runden-Issues als **offene Arbeit** ergänzt und nie als bereits
   vorhandene DB-Konzepte ausgegeben. Überschneidungen werden koordiniert, nicht doppelt angelegt.
4. Später bekannt gewordene operative Abweichungen sichtbar klären. Nach #248/D2 ist kein regelmäßiger
   Produktions-Export-/Importpfad Bestandteil des Standardablaufs; keine ungefragte Produktionsabfrage oder
   automatische Synchronisierung.

Der Recherchelauf muss die Vorbereitung nicht selbst ausführen, wenn er ihr gültiges, überprüfbares Ergebnis
lesen kann. Ein fehlender oder unpassender Index bleibt ein Hindernis für vollständige Bestandsbehauptungen.

### 0.2 Repository-Index prüfen

Einstieg ist [`catalog-index/catalog-index.manifest.json`](catalog-index/catalog-index.manifest.json). Der dort
benannte inhaltsadressierte JSONL-Payload und die Index-README bilden zusammen den Vertrag. Mit Java 21:

```bash
./mvnw -Pcatalog-index -DskipTests compile exec:java \
  -Dexec.args="validate --repository-root . --manifest docs/catalog-index/catalog-index.manifest.json"
```

Erfolg muss mit `sourceCheck=CURRENT` enden. Zusätzlich im Runden-Issue getrennt festhalten:

- tatsächlichen Katalog-Quellcommit und Quellref,
- Artefaktcommit, der Manifest/Payload enthält,
- relevanten Input-/Payload-Fingerprint,
- gewählten Rechercheumfang und ausdrücklich ausgeschlossene Parallelstände,
- bekannte freigegebene, noch nicht eingepflegte Vorschläge.

`sourceCheck=CURRENT` macht einen im Manifest ausgeschlossenen Sammelstand nicht enthalten. Der eingecheckte
Erstindex basiert auf `main@baf77832dc4064602759eea37d7e164e043883fa` und schließt PR #247 ausdrücklich aus.
Vor einer tatsächlichen Recherche auf einem anderen konsistenten Gesamtstand wird der Index gemäß seiner README
neu erzeugt oder seine Gültigkeit für genau diesen Stand nachgewiesen. Zwei Exporte werden nicht vereinigt.

### 0.3 Suchlauf und Existenzauflösung

Kandidaten als JSONL eingeben und den dokumentierten vollständigen Suchlauf verwenden, zum Beispiel:

```bash
./mvnw -Pcatalog-index -DskipTests compile exec:java \
  -Dexec.args="search --repository-root . --manifest docs/catalog-index/catalog-index.manifest.json --candidates <eingabe.jsonl> --output <ausgabe.jsonl> --page-size 100"
```

Die Ausgabe ist nur vollständig, wenn ihr `searchSummary` `complete: true`, Kandidaten-/Seitenzahl,
Quellcommit und Payload-SHA ausweist. `--integrity-only true`, ein Top-N-Ausschnitt, ein einzelner SQL-Treffer oder
ein fehlender Zugriff sind kein abschließender Existenznachweis.

Jeder Kandidat erhält nach manueller Identitätsprüfung genau einen Zustand aus dem Indexvertrag:

- `PRESENT_MATCH`,
- `PRESENT_OTHER_CODE_OR_NAME`,
- `RELATED_NOT_IDENTICAL`,
- `ABSENT_AFTER_FULL_REVIEW`,
- `UNRESOLVED`.

Ein automatischer Exakttreffer bleibt ohne begründete Auflösung ungeklärt. Nulltreffer, Mehrdeutigkeit oder
fehlender Zugriff erzeugen keine bestätigte Kataloglücke. Nur `ABSENT_AFTER_FULL_REVIEW` mit vollständiger
fachlicher Prüfung trägt einen Neuaufnahmevorschlag wegen Abwesenheit.

## Phase 1: Fachliche Recherche bis Gate 1

### 1.1 Küchenkontext und Kandidatensuche in beide Richtungen

Eine kurze fachliche Einordnung der nationalen beziehungsweise ausdrücklich abgegrenzten Küche erarbeiten:
prägende Grundprodukte, Würzlogik, Produktformen und entscheidungsrelevante regionale Unterschiede.

Danach beide Suchrichtungen durchführen und zusammenführen:

1. **Katalog → Küche:** Bestand breit über relevante Zutatenfamilien, Produktformen und Verwendungsbereiche auf
   plausible Kandidaten prüfen.
2. **Küche → Katalog:** prägende Produkte und Traditionen unabhängig recherchieren und anschließend gegen
   denselben vollständigen Bestand auflösen.

Die Abdeckungsübersicht ist eine Suchhilfe, keine Trefferquote. Parent und Child, Saat und Öl, Mischung und
Einzelgewürz sowie Zutat und Fertiggericht nicht gleichsetzen. Spezifischere vorhandene Konzepte zuerst auf
Produktpassung prüfen; dies ist keine Verdrängungsregel. Globale Verbreitung, stärkere Assoziation einer anderen
Küche, bereits zugeordnete Geschwister oder nur ein repräsentatives Gericht sind keine pauschalen Ausschlüsse.

### 1.2 Ergebnisdarstellung

Pro ernsthaft geprüftem Kandidaten mindestens festhalten:

- stabile Arbeitsreferenz,
- Konzeptcode beziehungsweise noch ungeklärte Identität,
- Existenzstatus samt Index-/Quellkennung und kurzer Auflösung,
- Relationsvorschlag `setzen`, `Grenzfall / bewusst prüfen` oder `nicht setzen`,
- bei bestätigter Lücke getrennten Aufnahmevorschlag `aufnehmen`, `Grenzfall` oder `nicht aufnehmen`,
- Quellenbefund, redaktionelle Schlussfolgerung und verbleibende Unsicherheit,
- offene Produktform-, Granularitäts- oder Graphfragen,
- Freigabestatus.

Auch ernsthaft geprüfte Grenzfälle und Ablehnungen zeigen; nicht jeden offensichtlich unplausiblen Katalogeintrag
mit einer künstlichen Ablehnung versehen. Der vollständige Existenzabgleich bleibt im Issue oder einem eindeutig
versionierten Hauptanhang nachvollziehbar, während die menschliche Übersicht lesbar bleibt.

### 1.3 Gate 1

Die Recherche liefert Empfehlungen, keine Datenpflege. Der aktuelle Body wird auf eine benannte Revision gebracht.
Die menschliche Entscheidung nennt explizit freigegebene positive Relationen, Neuaufnahmen und weiterhin offene
oder abgelehnte Fälle. Anschließend hält ein eigener, fest verlinkter Gate-1-Kommentar das vollständige freigegebene
Delta und die Revision fest. Spätere Änderungen erfolgen in einer neuen Revision; der alte Schnappschuss bleibt
unverändert rekonstruierbar.

Bis Gate 1 sind keine neuen Katalogdaten/Migrationen und keine vollständigen Metadaten für ungeklärte
Neuaufnahmen zulässig. Lokale Rechercheunterlagen, Issuepflege und notwendige technische Vorbereitung sind noch
keine fachliche Einpflege.

## Phase 2: Metadatenentwurf und Gate 2

Nur für Gate-1-freigegebene Neuaufnahmen oder ausdrücklich geöffnete bestehende Metadaten die vollständige Vorlage
aus `INGREDIENT_CONCEPT_CURATION.md` verwenden. Mindestens sichtbar sind:

- Code, Anzeigename, Aktivstatus, Spezifität, Ziehbarkeit, zulässige und ausgeschlossene Produktformen,
- echte allgemeine Kuratornotiz,
- explizite Parent-/Child-Kanten, Rollen, Dimensionen, Flags und gegebenenfalls Saison,
- eigenständig begründete Kochungewöhnlichkeit und `base_draw_weight`,
- Georgia-/Tobias-Beschaffbarkeit jeweils mit Stufe, Markt-/Logistikbegründung, aktueller Evidenz und exakt
  vorgeschlagenem individuellem Notiztext,
- Länderrelationen, Exclusions und andere besondere Metadaten oder begründete Nichtanwendbarkeit.

Kochungewöhnlichkeit und Beschaffung unabhängig bewerten. Historische Referenzanker dienen der Skalen- und
Textkalibrierung, nicht als aktueller Händlernachweis. Research-Evidenz bleibt von Nutztexten getrennt. Beide
Availability-Stufen und beide exakten Personennotizen sowie die allgemeine Kuratornotiz gehören ausdrücklich zur
Freigabe; eine Zahlenfreigabe genehmigt keine später erfundenen Texte.

Gate 2 wird wie Gate 1 revisionsgebunden in einem nicht nachträglich editierten, fest verlinkten Kommentar
festgehalten. Ändert sich nachher nur ein Teil des Entwurfs, wird ausschließlich dieses Delta mit neuer Revision
erneut vorgelegt; unveränderte freigegebene Teile bleiben gültig.

Sind ausschließlich bestehende Länderrelationen ohne Neuaufnahme oder Metadatendelta freigegeben, entfällt das
inhaltlich leere Gate 2. Sobald neue Konzepte Teil eines gemeinsamen Länderpakets sind, wartet dessen technische
Einpflege auf Gate 2, sofern kein ausdrücklich beauftragter Teilschnitt beschlossen wurde.

## Phase 3: Beauftragte technische Einpflege

Vor dem Schreiben Basis, Batchbranch, betroffene Konzepte, offene Parallelvorschläge und Freigabedeltas erneut
abgleichen. Nur den ausdrücklich freigegebenen Umfang übernehmen:

- neue append-only Liquibase-Changesets nach den bestehenden Includes; veröffentlichte Changesets, #188-Review
  und historische Manifeste nicht umschreiben,
- stabile Codes statt geratener IDs; Kollisionen oder unbekannte Deltas sichtbar stoppen,
- Kurator- und Availability-Notizen exakt wie freigegeben übernehmen,
- Kanten und Änderungen an bestehenden Aggregaten explizit pflegen; keine abgeleiteten Länder-, Rating-, Rollen-
  oder sonstigen Fachänderungen ergänzen,
- aktuelle Transaktions-, Integritäts-, Graphlock- und Versionsverträge wahren,
- vor Commit den vollständigen Diff einmalig gegen beide Freigabeschnappschüsse prüfen.

Marktklassen, URLs, Prüfdaten und ausführliche Evidenz sind Reviewunterlagen, keine neu einzuführenden
DB-Metadaten. Ein Commit/PR schließt #172 nicht. Das Runden-Issue verlinkt später Implementierungscommit, Batch-PR
und tatsächliche Prüfungen; #172 verweist kompakt auf die Runde und den Batch.

## Phase 4: Technische Prüfung und Batch-Abschluss

Automatisierte Tests bilden keine redaktionelle Fachlichkeit des produktiven Katalogs ab. Unzulässig bleiben
insbesondere Assertions auf konkrete produktive Konzept-/Relationslisten, Sollmengen, Länderablehnungen, Ratings,
Notizen, Rollen, Dimensionen, Flags, Availability, Saison, Kanten oder Content-Snapshots. Testeigene synthetische
Daten dürfen technische Verträge prüfen.

Ein reiner weiterer Länder-/Katalogcommit verlangt keinen Vollsuite-Lauf. Gezielt dürfen technische
PostgreSQL-/Migrationstests laufen, wenn sie zusätzlichen Schutz bieten. Der vollständige
`./mvnw clean verify` und die relevanten CI-Gates sind Batch-Abschlussprüfung bei ausdrücklich beauftragter
Mergevorbereitung oder früher bei einem konkreten technischen Umbau/Fehler. Technische Anwendungscodeänderungen
erhalten keine pauschale Länderbatch-Ausnahme.

Vor jedem Push mindestens:

- vollständiger Scope-/Freigabe-Diffabgleich,
- `git diff --check <Basis-SHA> <Head-SHA>`,
- Prüfung der tatsächlich betroffenen Links, Vorlagen und Startprompts,
- ehrliche Zuordnung ausgeführter, nicht anwendbarer, laufender und fehlgeschlagener Checks.

Merge, Deployment, Produktionszugriff und Branchlöschung bleiben getrennte Befugnisse.

## Nachvollziehbarkeit und lange Anhänge

Der aktuelle Runden-Body enthält mindestens Küchenumfang/Code, Abgrenzungen, Phase/Revision, Katalog-/Regelstand,
offene Sammeldeltas, Kandidatenentscheidungen/Evidenz, Gate-Status, Änderungsliste und später Commit/PR/Prüfungen.
Diskussionen bleiben Kommentare; relevante Beschlüsse werden in den Body konsolidiert.

Keine obligatorische Vollkopie derselben Tabellen in Chat, Repository und PR. Bei langen Anhängen eine eindeutig
versionierte Hauptquelle verlustfrei verlinken. Body und Anhang dürfen keine konkurrierenden aktuellen Fassungen
bilden. Freigabeschnappschüsse referenzieren exakt die freigegebene Revision und enthalten das freigegebene Delta,
damit ein später bearbeiteter Body die damalige Entscheidung nicht unkenntlich macht.

## Übergang, D3 und #252

Die vorbereitete Einführung, der synthetische Verfahrensdurchgang und die begrenzte D3-Fallliste stehen in
[`analysis/country-workflow-transition-20260911.md`](analysis/country-workflow-transition-20260911.md).

- Die D3-Liste ist vor ihrer Übergabe an #252 vom Auftraggeber zu bestätigen. Ihre Aufnahme in das Repository ist
  keine fachliche Umkehr einer Altentscheidung.
- #252 führt erst danach die fachliche Nachprüfung im eingeführten Verfahren durch. Jede Datenänderung benötigt
  neue fachliche Einzel-/Rundenfreigaben und einen passenden Einpflegeauftrag.
- Pilotland und Probedurchlauf sind keine Vorbedingung für diese Dokumentimplementierung, bleiben aber offene
  Einführungsschritte.
- PR #247 und sein Branch bleiben bis zu ihrem gesondert beauftragten Abschluss unverändert erhalten.

## Nicht-Ziele

- keine automatische Länderklassifikation oder Regionenontologie,
- keine Vollständigkeitsdatenbank sämtlicher Weltküchen,
- keine Herkunfts- oder Exklusivitätsbehauptungen,
- keine automatische Vererbung zwischen Zutatenkonzepten,
- keine ungeprüfte Katalogerweiterung oder automatische Umkehr früherer Entscheidungen,
- kein Produktions-Exportdienst, keine neue Workflow-Automatisierungsplattform und keine neuen Pflichtlabels,
- keine neue fachliche Ratingregel und keine zusätzliche Freigaberunde.

## Wiederverwendbarer Startprompt nach Einführung

```text
Wir bearbeiten die ausdrücklich beauftragte Länder-/Ergänzungsrunde im Repository
`venomenon328/mise-en-dice`.

Runden-Issue: <URL>
Land/Küchenumfang: <Name und Code>
Technischer Batchbranch, falls bereits beauftragt: <Branch oder „noch keiner“>

Lies `AGENTS.md`, den aktuellen Body des Runden-Issues, den aktuellen #172-Tracker und
`docs/CULINARY_CATALOG_WORKFLOW.md`. Ziehe danach die Quellen aus dessen Matrix für die jetzt beauftragte Phase
vollständig beziehungsweise in den dort ausdrücklich genannten Abschnitten heran. Prüfe den vorbereiteten
Gesamtstand und den Repository-Katalogindex samt Manifest; separat dokumentierte freigegebene, noch nicht
eingepflegte Vorschläge sind offene Arbeit und keine existenten DB-Konzepte.

Führe jetzt ausschließlich Phase <Phase> bis zum nächsten Haltepunkt durch und pflege die aktuelle Revision im
zugehörigen Runden-Issue. Überspringe kein offenes fachliches Gate, verlange aber keine Wiederfreigabe bereits
eindeutig freigegebener unveränderter Teile. Keine Katalog-/Migrationsänderung vor dem beauftragten Einpflegeschritt,
kein Merge oder Deployment.
```
