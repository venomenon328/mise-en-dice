# Kandidatengenerator

Stand: 16. August 2026  
Status: verbindliche Spezifikation für den Kandidatengenerator ab Version 1.1

Dieses Dokument konkretisiert die Produktvision für die Erzeugung von Challenge-Kandidaten. Es ist gemeinsam mit [`VISION.md`](VISION.md), [`ARCHITECTURE.md`](ARCHITECTURE.md), [`DATA_MODEL.md`](DATA_MODEL.md) und ADR 0007 verbindlich. Bei Implementierungsdetails ist dieses Dokument die fachliche Hauptquelle; harte Produktregeln aus der Vision bleiben vorrangig.

Der Generator soll weder ein Rezept erraten noch aus möglichst vielen ungewöhnlichen Zutaten ein Kuriositätenquartett bauen. Seine Aufgabe ist enger und zugleich anspruchsvoll genug:

> Er erzeugt reproduzierbar zwölf harte gültige, strukturell brauchbare und untereinander ausreichend unterschiedliche Vierer-Kandidaten, aus denen der spätere Kurator eine Challenge auswählen kann.

## 1. Abgrenzung und Grundsätze

### 1.1 Verantwortlichkeiten

Der Generator verantwortet:

- die Eignung zufällig ergänzter Zutatenkonzepte,
- die Gewichtung einzelner Vorgaben,
- genau vier Vorgaben mit mindestens zwei spezifischen Vorgaben,
- strukturelle Mindestplausibilität über funktionale Rollen,
- Redundanz- und Ausschlussprüfung,
- Cooldowns und historienabhängige Neuigkeitskadenz,
- die Erzeugung eines ausreichend großen Reservoirs,
- die Qualität jedes einzelnen Kandidaten,
- die Vielfalt und Zielverteilung des vollständigen Zwölfer-Satzes,
- begrenzte Suche, Diagnose und reproduzierbares Replay.

Der Generator verantwortet ausdrücklich nicht:

- die Behauptung, dass beliebige konkrete Zutaten geschmacklich harmonieren,
- ein Zielgericht oder eine Musterlösung,
- Küchen-, Länder- oder Rezeptklassifikation,
- die Auswahl des letztlich sichtbaren Kandidaten,
- die Erfindung neuer Vorgaben durch ein Sprachmodell,
- die nachträgliche Aufweichung harter Regeln bei einem dünnen Pool.

Die funktionalen Rollen erlauben einen strukturellen Existenzbeweis. Sie sind kein vollständiges kulinarisches Pairingwissen. Die verbleibende semantische Zutatenkompatibilität ist die zentrale Aufgabe des späteren Kurators.

### 1.2 Drei getrennte Bewertungsebenen

1. **Vorgabenebene:** Eignung und effektives Ziehungsgewicht eines Zutatenkonzepts.
2. **Kandidatenebene:** harte Gültigkeit, Strukturprofil und weiche Bewertung einer Viererkombination.
3. **Satzebene:** Ähnlichkeit, Quoten und marginaler Diversitätsgewinn beim Aufbau des Zwölfer-Satzes.

Ein hoher Kandidatenscore ersetzt keine Satzdiversität. Umgekehrt darf ein formal origineller Kandidat mit schwacher Struktur nicht allein wegen seiner Andersartigkeit in den Satz gelangen.

### 1.3 Harte Regeln und weiche Ziele

Harte Regeln besitzen keine Fallbacks. Weiche Mindestwerte, Zielquoten und Ähnlichkeitsgrenzen dürfen ausschließlich über die in Abschnitt 16 beschriebenen geordneten Fallbackstufen gelockert werden. Jede verwendete Stufe wird im Ergebnis gespeichert.

### 1.4 REROLL-Semantik ab Generator 1.1

Ein freiwilliger REROLL ist kein negatives Signal über einzelne Zutaten. Er verwirft das vollständig präsentierte Offer Set als Kombination. Deshalb besitzt Generatorversion `1.1.0` **keinen dedizierten ingredient-level REROLL-Hardblock** mehr.

Verbindlich gilt:

- Wiederholungswirkung entsteht ausschließlich über den normalen exakten Cooldown des `Visible History Snapshot`.
- Der Cooldown vergleicht stabile Konzeptcodes exakt. Vorfahren, Nachfahren, Konkretisierungen und Geschwister werden nicht mitgesperrt.
- Es gibt keine besondere REROLL-Sperre für Neuigkeitsstufen 4 oder 5.
- Interne Zwölfer-Sätze, Kuratorablehnungen und niemals präsentierte Optionen erzeugen weiterhin keine Exposition.
- Wird später aus einem sichtbaren Offer Set von 1–3 Optionen genau eine Option bestätigt, wirkt nur die bestätigte Challenge als normale Historie.
- Wird stattdessen das vollständig präsentierte Offer Set rerollt, werden seine tatsächlich sichtbaren exakten Katalogkonzepte als **ein gemeinsames Cooldown-only-Expositionsereignis** erfasst. Dieses Ereignis beeinflusst nicht `RECOVERY` oder `SEEKING_VARIETY`.
- Die Persistenz und Projektion eines solchen Multi-Offer-Expositionsereignisses ist Aufgabe von Phase 10/11 und wird nicht in den Phase-9-Generator vorgezogen.

Historische Generatorversion `1.0.0` besaß einen separaten Vierer-REROLL-Block. Dessen Snapshotfelder und Reason-Code `REROLL_EXACT_BLOCKED` dürfen für Altbestände lesbar bleiben, werden von neuen v1.1-Läufen aber nicht mehr fachlich verwendet. Die Semantikänderung ist replayrelevant und begründet den Minor-Versionssprung von `1.0.0` auf `1.1.0`.

### 1.5 Kandidatenspezifische Restriktionen ab Generator 1.2

Generator `1.2.0` ersetzt die attempt-weite Ziehung für neue Läufe durch eine deterministische Entscheidung je
Candidate-Proposal, bevor Profil, Zielwerte und Requirements gezogen werden. `challenge_session.restriction_mode`
ist ein unveränderlicher Sessioninput: `AUTO` ist Default und nutzt exakt 20 %, `NONE` erzeugt keine Restriktion,
`REQUIRED` verlangt eine eligible Regel. Auswahlgewicht, Konflikt mit manuellen Vorgaben und Wiederholung folgen
weiterhin den aktiven `exclusion_rule`-Snapshots; `REQUIRED` ohne eligible Regel ist eine typisierte Erschöpfung,
während `AUTO` dann ohne Restriktion fortfährt.

Die Kandidatenrestriktion umfasst Rule-ID, Rule-Code und Textsnapshot. Sie ist Teil der Candidate-Signatur und der
Ähnlichkeitskomponente `RESTRICTION`; dadurch bleiben gleiche Restriktionen sichtbar, Wiederholungen aber möglich.
Historische `1.0.x`-/`1.1.x`-Snapshots behalten unverändert die attempt-weite Entscheidung und werden durch den
versionsgebundenen Dispatcher mit ihren eigenen Payloadformen reproduziert.

## 2. Begriffe und Datenfluss

### 2.1 Zentrale Begriffe

- **Generation Request:** fachliche Eingabe für einen `INITIAL`- oder vorbereiteten `REROLL`-Versuch.
- **Generation Context:** unveränderlicher vollständiger Kontext für eine Berechnung.
- **Catalog Snapshot:** kanonische Generatorprojektion des Katalogs zu einem Zeitpunkt.
- **Visible History Snapshot:** die für Cooldown und Kadenz relevanten tatsächlich sichtbaren Expositionen.
- **Attempt Exclusion Decision:** einmalige Entscheidung, ob und welche Ausschlussregel für den Versuch gilt.
- **Candidate Profile:** generische strukturelle Zielform eines Vierer-Kandidaten.
- **Proposal:** ein mit Seed erzeugter Entwurf vor oder nach harter Validierung.
- **Candidate Evaluation:** versionierte weiche Bewertung eines harten gültigen Proposals.
- **Reservoir:** deduplizierte Menge harter gültiger und bewerteter Kandidaten.
- **Generated Candidate Set:** finaler Satz aus genau zwölf Kandidaten einschließlich Satzdiagnose und Fingerprint.
- **Generation Batch:** später persistierte Generatorrunde unter einem `generation_attempt`.
- **Curation Round:** spätere, davon getrennte Übergabe eines Generation Batch an den externen Kurator.

### 2.2 Vollständiger Ablauf

1. Request validieren.
2. Katalog und sichtbare Historie jeweils einmal als konsistenten Snapshot materialisieren.
3. manuelle Vorgaben normalisieren und optional mit Katalogkonzepten verknüpfen.
4. Generator- und Konfigurationsversion sowie Master-Seed festlegen.
5. Neuigkeitskadenz aus sichtbarer Historie ableiten.
6. für `1.0.x`/`1.1.x` einmalig die attempt-weite Ausschlussentscheidung ziehen, für `1.2.0` nur die
   regelbezogene Eligibility vorbereiten.
7. viele unabhängige Proposal-Substreams ableiten.
8. pro Proposal bei `1.2.0` zuerst Restriktionsmodus und gegebenenfalls Regel ziehen, dann Zielprofil,
   Spezifitätsmix und Neuigkeitsband ziehen.
9. fehlende Slots gewichtet ohne Zurücklegen füllen.
10. harte Regeln prüfen; Ablehnungen nach Reason-Code zählen.
11. harte gültige Kandidaten weich bewerten und kanonisch deduplizieren.
12. Reservoir bis zur Zielgröße oder zur maximalen Versuchszahl aufbauen.
13. aus dem Reservoir kontrolliert stochastisch zwölf diverse Kandidaten auswählen.
14. Set-Evaluation und Fingerprint berechnen.
15. Ergebnis oder typisierte Erschöpfung zurückgeben.
16. erst Phase 9D persistiert das Ergebnis atomar.

Keine dieser Stufen startet einen Netzwerkaufruf.

### 2.3 Implementierungszuordnung innerhalb Phase 9C

Phase 9C ist in zwei reine Fachpakete geteilt:

- **Phase 9C1 / Issue #35** konsumiert einen bereits materialisierten `Visible History Snapshot`, leitet
  Neuigkeitskadenz und Attempt-Ausschluss genau einmal ab, erzeugt daraus beliebige batch-spezifische
  `GenerationContext`-Objekte und baut das begrenzte, signaturdeduplizierte Reservoir auf.
- **Phase 9C2 / Issue #47** berechnet Kandidatenpaarähnlichkeit, führt die geordneten Soft-Fallbacks aus,
  wählt per MMR-ähnlichem Verfahren den finalen Zwölfer-Satz und führt die breite Baselinesimulation aus.
- **Phase 9D / Issue #36** erweitert das Schema append-only und materialisiert sowie persistiert erst dann
  den vollständigen historischen `Visible History Snapshot` aus PostgreSQL.

Phase 9C1 rekonstruiert fehlende historische Rollen-, Neuigkeits-, Flag- oder Graphwerte niemals aus dem
aktuellen Katalog. Es enthält weder Challenge-JDBC noch Liquibase-Änderungen.

## 3. Generation Context und Snapshots

Der unveränderliche `GenerationContext` enthält mindestens:

- Attempt-Typ `INITIAL` oder `REROLL`,
- fachlichen Stichtag und wirksamen Saisonmonat,
- null bis zwei manuelle Vorgaben in Eingabereihenfolge,
- vollständigen Catalog Snapshot,
- Visible History Snapshot,
- für `1.0.x`/`1.1.x` die Attempt Exclusion Decision, für `1.2.0` Restriction Mode und die
  vorbereiteten Rule-Evaluations,
- Generatorversion,
- Konfigurationsversion und kanonischen Konfigurationssnapshot,
- Master-Seed und RNG-Algorithmus.

Das aus Version 1.0 stammende Feld `rerollBlockedConceptCodes` kann in historischen Snapshots weiterhin vorkommen. Neue v1.1-Requests und -Contexts normalisieren es auf leer; es ist kein fachlicher Input mehr.

### 3.1 Catalog Snapshot

Der öffentliche Katalog-Use-Case liefert kanonisch sortiert mindestens:

- stabile ID, Code und damaligen Anzeigenamen,
- Aktivstatus und Ziehbarkeit,
- Challenge-Spezifität,
- Basisgewicht und Neuigkeitsstufe,
- Rollen, Flags und gepflegte Dimensionen,
- Beschaffbarkeit aller aktiven Teilnehmer,
- Saisonmultiplikator des wirksamen Monats,
- direkte und transitive Vorfahren beziehungsweise Nachfahren in der benötigten Form,
- aktive Ausschlussregeln einschließlich expandierter Ziele,
- alle Werte, die später in Requirement-Snapshots gehören.

Das Challenge-Modul greift nicht direkt auf Katalogtabellen oder interne Repositories zu.

### 3.2 Visible History Snapshot

Für den Phase-9-Bestand zählen bestätigte sichtbare Challenges mit den Statuswerten:

- `ACTIVE`,
- `COMPLETED`,
- `REROLLED`,
- `ABANDONED`.

Intern erzeugte Kandidaten, abgelehnte Generation Batches und spätere Kuratorablehnungen sind keine Exposition. Persönliche Konkretisierungen und Zusatz-Zutaten erzeugen ebenfalls keinen Generator-Cooldown.

Phase 11A ergänzt genau eine zweite Expositionsart: ein vollständig präsentiertes Offer Set, das vor Auswahl einer einzelnen Option freiwillig rerollt wurde. Dessen persistierte exakte Requirement-Codes zählen gemeinsam als **eine** Cooldown-Position, aber nicht für Neuigkeitskadenz oder bestätigte Challenge-Historie. Ein normales Offer Set, aus dem eine Option bestätigt wird, erzeugt über seine übrigen Optionen keine Exposition.

Die Historienprojektion einer bestätigten Challenge enthält mindestens:

- Sichtbarkeitszeitpunkt,
- Session und Attempt-Typ,
- damalige vier Requirement-Snapshots,
- damalige Spezifität und Neuigkeitsstufe, soweit bekannt,
- damalige Ausschlussregel,
- Challenge-Status.

Die spätere Multi-Offer-Projektion muss zusätzlich die Cooldown-only-Wirkung und gemeinsame Distanzposition explizit abbilden, statt mehrere sichtbare Optionen künstlich als mehrere aufeinanderfolgende Challenges auszugeben.

## 4. Datenreife und zulässige Signale

Die Messung und das Gate sind in [`CANDIDATE_GENERATOR_DATA_READINESS.md`](CANDIDATE_GENERATOR_DATA_READINESS.md) dokumentiert. Die reproduzierbare Abfrage liegt unter [`analysis/candidate-generator-data-readiness.sql`](analysis/candidate-generator-data-readiness.sql).

Für zufällige Ziehungen sind hart erforderlich:

- mindestens eine funktionale Rolle,
- vollständige Beschaffbarkeit für alle aktiven Teilnehmer,
- ein gepflegter Neuigkeitswert,
- gültige Spezifität und positives Basisgewicht.

Fehlen diese Daten, ist das einzelne Konzept zufällig nicht geeignet. Der Generator erfindet keinen Ersatzwert.

Optional bleiben:

- kulinarische Dimensionen,
- Kuratornotizen,
- Graphverbindungen für ein ansonsten eigenständig gültiges Konzept.

Fehlende kulinarische Dimensionen bedeuten `UNKNOWN`, niemals Stufe 1. Sie dürfen keine harte Kandidatenablehnung auslösen und werden nur als niedrig gewichtetes Softsignal verwendet. Fehlende Werte senken die ausgewiesene Datenkonfidenz.

Bei `culinary_flag` ist die Semantik anders: Ein nicht zugeordnetes Flag bedeutet, dass das kuratierte binäre Merkmal nicht gesetzt ist. Es ist kein dritter unbekannter Zustand.

## 5. Eignung und effektives Ziehungsgewicht

### 5.1 Harte Eignung eines zufälligen Konzepts

Ein Konzept ist für einen zufälligen Slot nur geeignet, wenn:

- `active = true`,
- `random_draw_enabled = true`,
- mindestens eine Rolle vorhanden ist,
- `novelty_level` vorhanden ist,
- für Georgia und Tobias je ein Beschaffbarkeitswert vorhanden ist,
- keiner dieser Werte `UNAVAILABLE` ist,
- das Konzept nicht durch den exakten Cooldown der sichtbaren Historie gesperrt ist,
- es nicht mit der Attempt-Ausschlussregel kollidiert,
- es für den aktuell zu belegenden Profilslot und Spezifitätsbedarf geeignet ist,
- es zu den bereits gesetzten Requirements keine harte Redundanz erzeugt.

Manuelle Vorgaben sind hiervon ausgenommen.

### 5.2 Beschaffbarkeitsfaktor

Maßgeblich ist die schlechtere Beschaffbarkeit beider Teilnehmer:

| schlechtester Wert | Faktor |
|---|---:|
| `EASY` | 1,00 |
| `PLANNED` | 0,65 |
| `DIFFICULT` | 0,20 |
| `UNAVAILABLE` | 0,00 |
| fehlend | 0,00 |

`DIFFICULT` bleibt damit möglich, wird aber deutlich seltener. Die Satzselektion begrenzt zusätzlich Kandidaten mit schwieriger Beschaffbarkeit.

### 5.3 Saisonfaktor

Der gepflegte Monatsfaktor wird direkt verwendet. Ein fehlender Monatswert bedeutet gemäß Datenmodell `1,0`. Saison verändert die Wahrscheinlichkeit, nicht die grundsätzliche Beschaffbarkeit.

### 5.4 Exakter Wiederholungs-Cooldown

Das Alter wird in sichtbaren Expositionspositionen gemessen, nicht in Kalendertagen.

| letzte exakte Exposition | Faktor |
|---|---:|
| innerhalb der letzten 6 sichtbaren Expositionspositionen | 0,00 |
| 7 bis 9 Positionen zurück | 0,25 |
| 10 bis 12 Positionen zurück | 0,50 |
| 13 bis 16 Positionen zurück | 0,75 |
| älter oder nie sichtbar | 1,00 |

Vorfahren, Nachfahren, Konkretisierungen und Geschwister erzeugen keinen harten Cooldown. Semantische Nähe fließt nur in History-Freshness und Satzähnlichkeit ein. So blockiert eine offene Vorgabe nicht ihren gesamten Bereich für Monate.

Ab Generator 1.1 besitzt `AttemptType.REROLL` **keinen zusätzlichen Cooldown oder Hardblock**. Ein REROLL sieht genau den normalen Visible-History-Context. Phase 11A materialisiert vor dem REROLL-Attempt die Cooldown-only-Exposition eines vollständig sichtbaren Offer Sets als eine gemeinsame Snapshot-Historienposition; alle darin exakt vorkommenden Konzeptcodes erhalten dadurch den normalen Faktor 0, ohne ihre Descendants zu sperren.

### 5.5 Neuigkeits-Zielfaktor

Jedes Proposal besitzt ein Zielband. Die Faktoren steuern die Auswahl einzelner Konzepte, ersetzen aber nicht die Kandidatenklassifikation.

| Neuigkeitsstufe | `FAMILIAR` | `BALANCED` | `ADVENTUROUS` |
|---:|---:|---:|---:|
| 1 | 1,25 | 0,80 | 0,35 |
| 2 | 1,10 | 1,00 | 0,65 |
| 3 | 0,70 | 1,20 | 1,00 |
| 4 | 0,15 | 0,75 | 1,30 |
| 5 | 0,00 | 0,20 | 1,15 |

Ein zufällig ziehbares Konzept ohne Neuigkeitsstufe ist bereits hart ungeeignet. Unklassifizierte manuelle Vorgaben erhalten keinen erfundenen Faktor.

### 5.6 Formel

Für ein geeignetes Konzept gilt:

```text
effectiveWeight =
    baseDrawWeight
  × seasonFactor
  × availabilityFactor
  × exactCooldownFactor
  × noveltyTargetFactor
```

Profil, Spezifität, Ausschluss und Redundanz sind Filter, keine zusätzlichen versteckten Multiplikatoren.

### 5.7 Numerische Stabilität

- Alle Faktoren werden als `BigDecimal` verarbeitet.
- Zwischenergebnisse verwenden Scale 12 und `HALF_EVEN`.
- Für die gewichtete Auswahl wird mit `10^9` in positive ganzzahlige Gewichtseinheiten quantisiert.
- Ein auf null gerundetes Gewicht gilt als nicht auswählbar und erhält einen Diagnosecode.
- Die Summe wird überlaufsicher geprüft; ein Überlauf ist ein Konfigurationsfehler.
- Der Pool ist vor jeder Zufallsoperation nach `code`, anschließend `id` sortiert.
- PostgreSQL-`random()`, `Math.random()` und implizite Iterationsreihenfolgen sind verboten.

## 6. Deterministische Zufälligkeit

### 6.1 Seed und Algorithmus

- Master-Seed: vorzeichenbehafteter 64-Bit-Wert, dezimal persistiert.
- RNG: projektspezifisch implementiertes `SPLITMIX64_V1`.
- Generatorversion: semantische Version der Algorithmen.
- Konfigurationsversion: getrennte Version der Defaultparameter.
- Produktions-Seedquelle: `SecureRandom` hinter einem injizierbaren `SeedSource`; Tests und Labor dürfen einen expliziten Seed vorgeben.

`SPLITMIX64_V1` ist nicht nur ein Name, sondern folgender bitgenauer Vertrag. Alle Operationen erfolgen als Java-`long` modulo `2^64`; `>>>` ist der vorzeichenlose Rechtsshift:

```text
state = state + 0x9E3779B97F4A7C15
z = state
z = (z xor (z >>> 30)) * 0xBF58476D1CE4E5B9
z = (z xor (z >>> 27)) * 0x94D049BB133111EB
return z xor (z >>> 31)
```

Gebundene ganzzahlige Ziehungen verwenden keine Fließkommazahl und kein verzerrendes einfaches Modulo. Für `bound > 0` wird aus `nextLong() >>> 1` per Rejection Sampling gleichverteilt ein Wert in `[0, bound)` erzeugt; Werte im unvollständigen oberen Restintervall werden verworfen. Wahrscheinlichkeiten werden mit Nenner `10^9` in ganzzahlige Einheiten quantisiert und über dieselbe gebundene Ziehung entschieden.

Die Implementierung verlässt sich damit weder auf nicht vertraglich stabile Interna einer JDK-Zufallsklasse noch auf plattformspezifische Fließkommadetails.

### 6.2 Benannte Substreams

Der auf `generation_attempt` gespeicherte Master-Seed ist der Wurzelwert für alle internen Batches. Jeder Batch besitzt zusätzlich einen abgeleiteten und persistierten Batch-Seed. Sämtliche Ableitungen verwenden SHA-256 über folgende null-separierte UTF-8-NFC-Felder:

```text
MED-SEED-V1\0
<generatorVersion>\0
<signedDecimalAttemptSeed>\0
<scope>\0
<purpose>\0
<ordinal>
```

Die ersten acht Digest-Bytes in Big-Endian-Reihenfolge werden unverändert als vorzeichenbehafteter `long` interpretiert.

Verbindliche Scopes:

- `attempt` für die attempt-weite Ausschlussentscheidung,
- `batch/<batchNumber>` für alle Kandidaten- und Satzentscheidungen eines Batches.

Der persistierte Batch-Seed ist die Ableitung mit Purpose `batch-root` und Ordinal `0`. Weitere Substreams werden direkt aus dem Attempt-Seed und ihrem Scope abgeleitet; der Batch-Seed dient als expliziter Replaywert und Kontrollfingerprint.

Verbindliche Zwecke sind mindestens:

- `attempt-exclusion-mode`,
- `attempt-exclusion-rule`,
- `candidate-restriction-mode`,
- `candidate-restriction-rule`,
- `proposal-profile`,
- `proposal-specificity`,
- `proposal-novelty`,
- `proposal-slot/<n>`,
- `batch-selection/<fallback>/<position>`.

Purpose-Strings stammen aus einem versionierten Enum und enthalten weder Nullbyte noch frei eingegebene Inhalte. Dadurch verändert eine neue Zufallsentscheidung in einem Teilbereich nicht stillschweigend alle nachfolgenden Entscheidungen, und ein zweiter interner Batch erhält reproduzierbar einen anderen Zufallsraum bei identischem Attempt-Kontext.

### 6.3 Reproduzierbarkeitsvertrag

Gleicher Generatorstand, gleicher Konfigurationssnapshot, gleicher Generation Context und gleicher Seed erzeugen:

- dieselben Proposal-Ergebnisse,
- dieselben Ablehnungszähler,
- dasselbe Reservoir,
- denselben Zwölfer-Satz,
- dieselben Evaluationen und Reason-Codes,
- denselben Fingerprint.

Ein Wechsel zwischen Generator-Minorversionen ist absichtlich **kein** gleiches Generatorumfeld: Die
Generatorversion fließt in die Seed-Substreams ein. `1.2.0` ergänzt isolierte Candidate-Restriction-Substreams,
ohne die alten `1.0.x`-/`1.1.x`-Substreams umzudeuten. Historische Snapshots werden über ihre gespeicherte Version
an die passende Implementierung dispatcht und niemals als `1.2.0`-Match ausgegeben.

## 7. Manuelle Vorgaben

Ein Request enthält null bis zwei nicht leere manuelle Freitexte. Sie bleiben autoritativ.

- Ein optionaler Katalogmatch dient nur als Hilfsinformation.
- Ein gematchtes Konzept übernimmt für Struktur und Mindest-Spezifität seine damaligen Rollen und Spezifität.
- Ein nicht gematchter Freitext ist `UNCLASSIFIED` und zählt nicht als spezifische Vorgabe.
- Manuelle Vorgaben ignorieren Aktivstatus, Zufalls-Ziehbarkeit, Beschaffbarkeit, Saison, Gewicht und Cooldown.
- Der Generator erfindet für einen ungematchten Freitext keine Rollen, Neuigkeit oder Dimensionen.
- Er ergänzt so, dass zu gematchten manuellen Vorgaben keine zufällige Dublette oder Parent-/Child-Redundanz entsteht.
- Widersprüche ausschließlich zwischen zwei manuellen Vorgaben werden nicht umgeschrieben oder verworfen; sie erscheinen als Diagnose.
- Ein unbekannter Konflikt zwischen Freitext und Ausschlussregel kann nicht behauptet werden. Nur gematchte Konflikte sind hart erkennbar.

Sind beide manuellen Vorgaben unklassifiziert, müssen die beiden zufälligen Ergänzungen beide `SPECIFIC` sein, damit die Produktregel eingehalten wird.

### 7.1 Auf erreichbare Ziele projizierte Quoten

Manuelle Vorgaben verändern, welche Spezifitätsmixe, Profile und Neuigkeitsbänder überhaupt erreichbar sind. Die unveränderten Baselinequoten blind anzuwenden wäre daher keine Strenge, sondern nur Mathematik mit schlechtem Benehmen.

Vor der ersten Proposal-Erzeugung wird für jede Quotendimension die erreichbare Kategorienmenge aus dem festen Manual-Kontext und dem aktuellen Katalogsnapshot bestimmt.

**Spezifität:**

- `manualSpecific` ist die Zahl gematchter manueller `SPECIFIC`-Vorgaben.
- `randomSlots = 4 - manualRequirementCount`.
- Ein Gesamtwert `s` aus `{2, 3, 4}` ist erreichbar, wenn `s >= 2` und `manualSpecific <= s <= manualSpecific + randomSlots` gilt und die benötigten zufälligen Teilpools nicht leer sind.
- Gematchte `OPEN`- und unklassifizierte manuelle Vorgaben zählen nicht als spezifisch.

**Profile:**

- Ein Profil ist erreichbar, wenn die festen Rollen gematchter manueller Vorgaben zusammen mit den verbleibenden Zufallsslots sowohl seine Slots als auch die allgemeine Mindeststruktur erfüllen können.
- Ein Profil, für dessen fehlende Slots im aktuellen Katalog kein geeigneter Teilpool existiert, erhält Gewicht null.
- Unklassifizierte manuelle Vorgaben erfüllen keinen Profilslot, blockieren aber ihren Positionsslot.

**Neuigkeitsband:**

- Bekannte Neuigkeitswerte gematchter manueller Vorgaben gehören zur Kandidatenlast und können ein Band bereits erzwingen.
- Ein Band ist erreichbar, wenn eine zufällige Ergänzung unter den verbleibenden Neuigkeitsbudgets und den Strukturregeln existiert.
- Ein unklassifizierter manueller Freitext erzeugt keinen erfundenen Neuigkeitswert.

Für jede Dimension werden die Basisgewichte der unerreichbaren Kategorien auf null gesetzt und die übrigen Gewichte neu normiert. Die zwölf Satzquoten werden anschließend deterministisch mit dem größten-Rest-Verfahren berechnet:

1. normiertes Gewicht mit 12 multiplizieren,
2. ganzzahligen Anteil abrunden,
3. verbleibende Plätze nach absteigendem Rest vergeben,
4. Gleichstände in der dokumentierten kanonischen Kategorienreihenfolge auflösen.

Dieselbe normierte Verteilung steuert die Proposal-Ziehung. Die Projektion ist kein Soft-Fallback und keine Quotenverletzung; sie wird mit spezifischen Reason-Codes dokumentiert. Bleibt für eine notwendige Dimension keine erreichbare Kategorie, endet der Versuch mit typisierter Erschöpfung statt mit einer unmöglichen Zielvorgabe.

## 8. Attempt-weite Ausschlussregel

Ausschlussmodus und Regel werden genau einmal pro `generation_attempt` bestimmt. Alle internen Generation Batches dieses Attempts verwenden dieselbe Regel oder gemeinsam keine Regel.

### 8.1 Modus

Defaultwahrscheinlichkeit für eine Ausschlussregel: `0,30`.

Die Bernoulli-Entscheidung besitzt einen eigenen RNG-Substream. Der spätere Kurator darf die Häufigkeit von Ausschlüssen nicht indirekt durch seine Kandidatenwahl bestimmen.

### 8.2 Geeignete Regeln

Eine aktive Regel ist auswählbar, wenn:

- sie mindestens ein Ziel besitzt,
- sie mit keiner gematchten manuellen Vorgabe kollidiert,
- ihr Wiederholungsfaktor positiv ist.

`include_refinements` expandiert über alle bekannten transitiven Nachfahren.

### 8.3 Wiederholung

| letzte sichtbare Verwendung derselben Regel | Faktor |
|---|---:|
| innerhalb der letzten 4 sichtbaren Challenges | 0,00 |
| 5 bis 7 Challenges zurück | 0,35 |
| älter oder nie sichtbar | 1,00 |

Das effektive Regelgewicht ist `base_draw_weight × repetitionFactor`.

Ist keine Regel geeignet, wird ohne Ausschluss weitergearbeitet und `NO_ELIGIBLE_EXCLUSION_RULE` diagnostiziert. Das ist keine Generation-Erschöpfung.

## 9. Rollenklassen und Kandidatenprofile

### 9.1 Rollenklassen

**Ankerrollen:**

- `ANIMAL_PROTEIN`,
- `PLANT_PROTEIN`,
- `VEGETABLE`,
- `FRUIT`,
- `STARCH`.

**Unterstützende Rollen:**

- `FAT`,
- `ACID`.

**Primär geschmacksgebende Rollen:**

- `AROMATIC`,
- `SEASONING`.

Ein Konzept darf mehreren Klassen angehören. Die Klassifikation behauptet keine übliche Portionsgröße; sie dient ausschließlich der Generatorstruktur.

### 9.2 Allgemeine harte Mindeststruktur

Jeder Kandidat muss:

- mindestens zwei unterschiedliche Requirements mit je mindestens einer Ankerrolle besitzen,
- insgesamt mindestens zwei verschiedene Ankerrollen abdecken,
- höchstens zwei Requirements ohne Ankerrolle enthalten.

Damit sind vier Gewürz-, Aromat- oder Saucenkomponenten ebenso ausgeschlossen wie vier Varianten derselben strukturellen Funktion.

### 9.3 Profilfamilien

| Profil | erforderliche unterschiedliche Requirements |
|---|---|
| `PROTEIN_PRODUCE` | eine Proteinrolle und eine Gemüse-/Obstrolle |
| `PRODUCE_DUO` | zwei Gemüse-/Obst-Requirements; allgemeine Mindeststruktur bleibt zusätzlich gültig |
| `STARCH_ANCHORED` | eine Stärke und eine Protein- oder Gemüse-/Obstkomponente |
| `THREE_ANCHORS` | mindestens drei Requirements mit Ankerrolle |
| `FLEXIBLE_BALANCED` | ausschließlich die allgemeine Mindeststruktur |

Default-Zielverteilung im Zwölfer-Satz:

| Profil | Kandidaten |
|---|---:|
| `PROTEIN_PRODUCE` | 3 |
| `PRODUCE_DUO` | 2 |
| `STARCH_ANCHORED` | 2 |
| `THREE_ANCHORS` | 2 |
| `FLEXIBLE_BALANCED` | 3 |

Proposal-Profile werden mit denselben ganzzahligen Gewichten `3:2:2:2:3` gezogen. Diese Verteilung ist die Baseline vor der in Abschnitt 7.1 beschriebenen Projektion auf die im Manual-Kontext erreichbaren Profile.

### 9.4 Mehrfachrollen und Slotbelegung

Die Profilprüfung ist ein deterministisches bipartites Matching zwischen Requirements und Profilslots.

- Ein Requirement darf genau einen erforderlichen Profilslot erfüllen.
- Weitere Rollen bleiben für Scores sichtbar.
- Bei mehreren gültigen Matchings gilt kanonische Reihenfolge nach Slotcode, Requirement-Code und ID.
- Die intern gewählte Slotbelegung wird diagnostiziert, aber nicht als vermeintliche Kochanweisung angezeigt.

## 10. Harte Kandidatenregeln

Ein Proposal ist nur dann hart gültig, wenn alle folgenden Regeln erfüllt sind.

### 10.1 Umfang und Spezifität

- genau vier Requirements,
- mindestens zwei als `SPECIFIC` klassifizierbare Requirements,
- Zielmixe 2/3/4 spezifisch werden mit Gewichten `4:5:3` gezogen.

Default-Zielverteilung im Zwölfer-Satz:

- 4 Kandidaten mit 2 spezifischen Vorgaben,
- 5 Kandidaten mit 3 spezifischen Vorgaben,
- 3 Kandidaten mit 4 spezifischen Vorgaben.

Diese Werte gelten vor der deterministischen Projektion auf die durch manuelle Vorgaben erreichbaren Gesamtmixe.

### 10.2 Identität und Redundanz

- keine doppelte zufällige Konzept-ID,
- keine zufällige Dublette einer gematchten manuellen Vorgabe,
- keine transitive Parent-/Child-Kombination zwischen zufälligen Requirements,
- keine transitive Parent-/Child-Kombination zwischen zufälligem und gematchtem manuellem Requirement.

Gemeinsame Vorfahren und Geschwister sind nicht generell verboten. Sie erhöhen Ähnlichkeit und können History-Freshness oder Set-Diversität verschlechtern.

### 10.3 Profil und Struktur

- das vorab gezogene Profil ist erfüllt,
- die allgemeine Mindeststruktur aus Abschnitt 9.2 ist erfüllt.

### 10.4 Neuigkeitslast

Punktwerte:

| Stufe | Lastpunkte |
|---:|---:|
| 1 | 0 |
| 2 | 1 |
| 3 | 2 |
| 4 | 4 |
| 5 | 7 |

Für den vom Generator kontrollierbaren Ergebnisanteil gilt hart:

- höchstens eine Vorgabe mit Stufe 5,
- höchstens zwei Vorgaben mit Stufe 4 oder 5,
- bekannte Gesamtlast höchstens 11.

Bekannte Neuigkeitswerte gematchter manueller Vorgaben verbrauchen diese Budgets zuerst. Solange die manuellen Vorgaben die Grenzen noch nicht überschreiten, müssen sämtliche zufälligen Ergänzungen in die verbleibenden Stufe-5-, Stufe-4/5- und Lastbudgets passen.

Überschreiten die autoritativen manuellen Vorgaben allein bereits eine Grenze, wird der Request nicht umgeschrieben oder deshalb abgelehnt. Der Kandidat erhält `MANUAL_NOVELTY_FORCED`; zufällige Ergänzungen dürfen keine weitere Stufe 4 oder 5 hinzufügen und werden innerhalb der noch strukturell möglichen Auswahl auf die geringste zusätzliche Neuigkeitslast beschränkt. Eine solche manuell erzwungene Ausnahme gilt nicht als Verletzung einer Generator-Hard-Rule, wird aber im Labor und in der Kuratoranfrage sichtbar diagnostiziert.

Manuelle unklassifizierte Vorgaben besitzen keine erfundene Last. Die Bandklassifikation betrachtet alle bekannten zufälligen und gematchten manuellen Werte:

- `FAMILIAR`: Last 0 bis 3, keine Stufe 4 oder 5,
- `BALANCED`: Last 4 bis 7, höchstens eine Stufe 4, keine Stufe 5,
- `ADVENTUROUS`: Last 8 bis 11 innerhalb der normalen Grenzen oder durch manuelle Vorgaben erzwungen.

Manuell erzwungene Bänder werden bei der Zielprojektion aus Abschnitt 7.1 berücksichtigt; der Generator versucht nicht, eine unmögliche Recovery-Quote durch heimlich falsche Klassifikation zu retten.

### 10.5 Ausschlusskonflikt

Kein zufälliges oder gematchtes manuelles Requirement darf Ziel der Attempt-Ausschlussregel sein. Transitive Zielerweiterung wird berücksichtigt.

### 10.6 Kulinarische Dimensionen

Kulinarische Dimensionen lösen in Version 1 keine harte Ablehnung aus. Ihre Baseline-Abdeckung ist hierfür nicht ausreichend. Insbesondere darf ein fehlender Dominanzwert nicht wie eine niedrige Dominanz behandelt werden.

## 11. Weiche Kandidatenbewertung

Alle Komponenten liegen im Bereich 0 bis 100. Der Gesamtscore ist das gewichtete Mittel der folgenden Defaultkomponenten:

| Komponente | Gewicht |
|---|---:|
| strukturelle Tragfähigkeit | 25 % |
| Rollenkomplementarität | 15 % |
| kreative Spannung | 15 % |
| Offenheit / Nicht-Trivialität | 10 % |
| Passung zum Neuigkeitsziel | 10 % |
| Beschaffbarkeitslast | 8 % |
| Historienfrische | 8 % |
| Datenkonfidenz | 5 % |
| bekannte kulinarische Lastbalance | 4 % |

Default-Mindestscore für die strikte Reservoirnutzung: `55`. Die neun Einzelkomponenten dienen ausschließlich dem Ranking und besitzen keine eigenen harten Mindestschwellen. Harte Gültigkeit wird vorher separat geprüft; der Gesamt-Mindestscore ist selbst ein dokumentiertes Softziel und darf nur über Abschnitt 16 gelockert werden.

Phase 9C1 behält deshalb jeden eindeutigen harten gültigen und bewerteten Kandidaten im Reservoir, auch bei
einem Gesamtscore unter 55. Erst Phase 9C2 verwendet die Mindestscorewerte bei Setselektion und Soft-Fallbacks.

### 11.1 Strukturelle Tragfähigkeit

Bewertet werden ausschließlich Reserven oberhalb der harten Mindeststruktur:

- Anzahl ankertragender Requirements,
- Anzahl unterschiedlicher Ankerrollen,
- eindeutige Erfüllung des Zielprofils,
- verbleibende alternative Slotbelegungen.

Mehr Struktur ist nicht unbegrenzt besser. Vier spezifische, vollständig strukturtragende Vorgaben erhalten nicht automatisch 100 Punkte.

### 11.2 Rollenkomplementarität

Bewertet werden:

- Rollenbreite,
- Konzentration auf nur eine Rollenfamilie,
- sinnvolle Verteilung von Anker-, Unterstützungs- und Geschmacksrollen,
- Mehrfachrollen ohne doppelte Slotverwendung.

### 11.3 Kreative Spannung

Dieses Signal belohnt eine kontrollierte Abweichung vom naheliegendsten Rollen- und Graphmuster:

- informative semantische Distanz zwischen Ankerkomponenten,
- ein klarer Akzent statt vier gleichartiger Akzente,
- Neuigkeit im vorgesehenen Band,
- seltenere, aber noch strukturell tragfähige Profilbelegungen.

Es verwendet keine Ländernamen, Rezeptnamen oder Zutaten-Sonderfälle.

### 11.4 Offenheit und Nicht-Trivialität

Offene Requirements und mehrere plausible Rollenbelegungen erhöhen den Wert. Ein Kandidat mit vier spezifischen Vorgaben bleibt zulässig und kann durch semantische Distanz und kreative Spannung gut abschneiden.

Ein niedriger Wert entsteht bei einer sehr dichten, vollständig vertrauten Rollenbelegung, die wenig Entscheidungsspielraum erkennen lässt. Das ist nur ein schwaches Ranking-Signal; der Generator behauptet nicht, ein bestimmtes Standardgericht erkannt zu haben.

### 11.5 Neuigkeitspassung

- 100 bei Zielbandtreffer,
- 70 bei direktem Nachbarband,
- 25 bei zwei Bändern Abstand,
- zusätzlicher Abzug bei einem unklassifizierten manuellen Anteil.

### 11.6 Beschaffbarkeitslast

Pro zufälligem Requirement:

- `EASY = 100`,
- `PLANNED = 65`,
- `DIFFICULT = 20`.

Die Komponente ist der Mittelwert. Gematchte manuelle Vorgaben werden angezeigt, aber nicht gegen den autoritativen Request bewertet.

### 11.7 Historienfrische

Exakte Wiederholungen sind bereits gesperrt. Der Softscore berücksichtigt zusätzlich die letzten vier bestätigten sichtbaren Challenges:

- informative gemeinsame Vorfahren,
- sehr ähnliche Rollensignaturen,
- wiederkehrende Profilfamilien,
- wiederkehrende starke Flags.

Cooldown-only-Expositionen eines rerollten Offer Sets wirken nur auf den exakten Wiederholungs-Cooldown und werden nicht als künstliche vollständige Challenge für diesen Softscore interpretiert. Breite Wurzelkonzepte werden stark abgewertet und erzeugen keinen künstlichen Familienbann.

### 11.8 Datenkonfidenz

Rollen, Beschaffbarkeit und Neuigkeit sind für zufällige Requirements vollständig und zählen als Grundvertrauen. Zusätzliche Konfidenz entsteht durch gepflegte Dimensionen, die für eine konkrete Bewertung tatsächlich verwendet wurden.

Ein Kandidat mit wenig Dimensionswissen darf einen guten Struktur- und Neuigkeitsscore besitzen, wird aber nicht als kulinarisch präzise analysiert ausgegeben.

### 11.9 Bekannte kulinarische Lastbalance

Diese bewusst niedrig gewichtete Komponente betrachtet nur vorhandene Werte:

- Häufung mehrerer bekannter Stufen 4/5 derselben Dimension,
- mehrere bekannte Stufe-5-Dominanzen,
- Häufung stark prägender Flags wie geräuchert, fermentiert, gepökelt oder getrocknet.

Fehlen vergleichbare Werte, ist die Komponente neutral und die Konfidenz sinkt. Es gibt keine paarweise Geschmacksverträglichkeitsbehauptung.

## 12. Neuigkeitskadenz über bestätigte sichtbare Challenges

Der Kadenzzustand wird vor der Attempt-Ausschlussentscheidung ermittelt. Für Kadenzentscheidungen zählen bestätigte Challenges, nicht Cooldown-only-Expositionen eines vor Bestätigung rerollten Offer Sets.

### 12.1 `RECOVERY`

Aktiv, wenn die unmittelbar vorherige bestätigte sichtbare Challenge `ADVENTUROUS` war oder eine Vorgabe der Stufe 5 enthielt.

Zielverteilung im Zwölfer-Satz:

- 5 `FAMILIAR`,
- 7 `BALANCED`,
- 0 `ADVENTUROUS`.

Nach einer bestätigten sichtbaren Stufe-5-Challenge erhalten zufällige Stufe-5-Konzepte für genau den folgenden Attempt zusätzlich Faktor 0.

### 12.2 `NEUTRAL`

Defaultzustand:

- 3 `FAMILIAR`,
- 7 `BALANCED`,
- 2 `ADVENTUROUS`.

### 12.3 `SEEKING_VARIETY`

Aktiv, wenn die letzten drei bestätigten sichtbaren Challenges alle `FAMILIAR` waren und keine davon wegen unklassifizierter manueller Vorgaben eine unsichere Neuigkeitsbewertung besitzt.

- 2 `FAMILIAR`,
- 7 `BALANCED`,
- 3 `ADVENTUROUS`.

Der Zustand garantiert keine ungewöhnliche sichtbare Challenge; er erhöht nur deren Vertretung im Kandidatensatz. Die spätere Kuratorauswahl bleibt notwendig.

Alle drei Verteilungen sind Baselines vor der in Abschnitt 7.1 beschriebenen Projektion. Erzwingen manuelle Vorgaben beispielsweise in jedem Kandidaten mindestens das Band `ADVENTUROUS`, wird die Zielquote entsprechend projiziert und nicht als reguläre Recovery-Verletzung gezählt.

## 13. Reservoir und Deduplizierung

Defaultgrenzen:

- bevorzugte Reservoirgröße: `144`,
- Mindestgröße für strikte Satzselektion: `72`,
- maximale Proposal-Versuche: `5.000`,
- Mindestzahl eindeutiger harter Kandidaten für irgendein Ergebnis: `12`.

Die kanonische Kandidatensignatur ist von der Anzeigereihenfolge unabhängig und enthält:

- normalisierte manuelle Requirement-Identitäten,
- sortierte zufällige Konzeptcodes,
- Attempt-Ausschlussregel,
- damalige Spezifitäten.

Derselbe Vierersatz in anderer Reihenfolge ist kein neuer Kandidat. Feste manuelle Requirements werden später bei der Paarähnlichkeit ausgeblendet, weil sie zwangsläufig in allen Kandidaten desselben Attempts vorkommen.

Für jede Proposal-Art werden Treffer und Ablehnungen nach stabilem Reason-Code gezählt.

Phase 9C1 beginnt beim Proposal-Ordinal `0`, ruft ausschließlich die öffentliche
`CandidateProposalEngine.propose(context, proposalOrdinal)`-API auf und stoppt beim bevorzugten
Reservoirziel oder bei `maximumProposalAttempts`. Signaturduplikate erhöhen die Treffer- und
Duplikatmetriken, nicht die Reservoirgröße. Weniger als zwölf eindeutige harte Kandidaten ergeben typisiert
`GENERATION_EXHAUSTED`; Größen von 12 bis 143 bleiben gültige Reservoirs. Die Größenklassen werden nur
diagnostiziert. Ihre spätere Zuordnung zu einer Fallback-Startstufe ist Aufgabe von Phase 9C2.

## 14. Kandidatenähnlichkeit

Die Gesamtähnlichkeit liegt zwischen 0 und 1. Alle Zwischenwerte verwenden `BigDecimal`, Scale 12 und
`HALF_EVEN`. Nicht vergleichbare optionale Hauptkomponenten werden aus Zähler und Nenner entfernt; die
übrigen Hauptgewichte werden proportional renormiert. Feste manuelle Requirements werden aus sämtlichen
requirementbasierten Paarmerkmalen entfernt. Die Profilidentität bleibt vergleichbar.

| Komponente | Gewicht |
|---|---:|
| exakte zufällige Konzepte | 0,35 |
| informative gemeinsame Vorfahren | 0,20 |
| Rollen- und Profilnähe | 0,15 |
| Spezifitätsmix | 0,05 |
| Neuigkeitsband und -last | 0,10 |
| Beschaffbarkeitslast | 0,05 |
| vergleichbare Flags und Dimensionen | 0,10 |

### 14.1 Exakte Konzepte

Jaccard-Ähnlichkeit `|A ∩ B| / |A ∪ B|` der Codes zufälliger Konzepte. Diese Komponente ist für gültige
v1-Kandidaten immer vergleichbar.

### 14.2 Informative Vorfahren

Sei `N` die Zahl aller Katalogkonzepte mit `active=true && randomDrawEnabled=true` und `d(a)` die Zahl dieser
Konzepte unter den transitiven Nachfahren des Vorfahren `a`. Dann gilt:

```text
w(a) = ((N - d(a)) / N)²
```

Vorfahren mit Gewicht 0 tragen nicht bei. Der Paarwert ist der gewichtete Jaccard aus der Gewichtssumme der
Schnittmenge geteilt durch die Gewichtssumme der Vereinigung. Fehlen bei mindestens einem Kandidaten positive
Vorfahrengewichte, ist die Hauptkomponente `NOT_COMPARABLE`. Für den Set-Cap ist ein Vorfahr informativ eng,
wenn `d(a) / N <= 0,25`; je Kandidat zählt derselbe Vorfahr höchstens einmal.

### 14.3 Rollen und Profil

Aus allen zufälligen Requirements entsteht ein Rollen-Multiset. Seine Ähnlichkeit ist der generalisierte
Jaccard `sum(min(countA, countB)) / sum(max(countA, countB))`. Anschließend gilt:

```text
roleProfileSimilarity = 0,90 × roleSimilarity + 0,10 × sameProfileIndicator
```

### 14.4 Spezifitätsmix

Verwendet wird die tatsächliche Anzahl `SPECIFIC` unter allen vier Requirements:

```text
similarity = 1 - abs(specificCountA - specificCountB) / 2
```

### 14.5 Neuigkeitsband und bekannte Last

Bandindizes sind `FAMILIAR=0`, `BALANCED=1`, `ADVENTUROUS=2`. Bandähnlichkeit ist
`1 - abs(indexA-indexB)/2`. Bei positivem `novelty.loadCap` ist die Lastähnlichkeit
`1 - min(abs(loadA-loadB), loadCap)/loadCap`; bei Cap 0 ist sie 1 genau bei gleicher Last. Insgesamt gilt
`0,60 × bandSimilarity + 0,40 × loadSimilarity`.

### 14.6 Beschaffbarkeitslast

Pro zufälligem Requirement wird der bereits ausgewertete Availability-Faktor verwendet; Last ist
`1 - availabilityFactor`. Die Kandidatenlast ist deren Mittelwert und die Paarähnlichkeit
`1 - abs(meanLoadA-meanLoadB)`.

### 14.7 Optionale Eigenschaftsdaten

Flags werden als Vereinigungsmengen pro Kandidat und bei nichtleerer Gesamtvereinigung per Jaccard verglichen.
Pro Dimension wird je Kandidat der Mittelwert aller bekannten Stufen seiner zufälligen Requirements gebildet.
Nur beidseitig bekannte Dimensionen sind vergleichbar; ihr Einzelwert ist `1 - abs(meanA-meanB)/4`, danach
wird gemittelt. Sind beide Unteranteile verfügbar, gilt `0,40 × flagSimilarity + 0,60 × dimensionSimilarity`;
ist nur einer verfügbar, trägt er den Gesamtwert. Sind beide nicht verfügbar, ist die Hauptkomponente
`NOT_COMPARABLE`. Fehlende Werte werden insbesondere nicht als Stufe 1 interpretiert.

Jede Paarbewertung enthält alle sieben Hauptkomponenten als Wert oder `NOT_COMPARABLE`, die tatsächlich
renormierten Hauptgewichte, die Gesamtähnlichkeit und stabile Paar-Reason-Codes. Daraus wird keine
Geschmacksverträglichkeitsbehauptung abgeleitet.

Da alle Kandidaten eines Attempts dieselbe Ausschlussregel besitzen, trägt diese innerhalb des Satzes nicht zur Paarähnlichkeit bei.

## 15. Aufbau des Zwölfer-Satzes

### 15.1 Strikte Zielgrenzen

- maximal zwei Vorkommen desselben zufälligen Konzepts,
- maximal zehn Kandidaten mit demselben informativen engen Vorfahren,
- maximal vier Kandidaten derselben Profilfamilie,
- höchstens drei Kandidaten mit mindestens einem `DIFFICULT`-Requirement,
- Paarähnlichkeit höchstens `0,58`,
- Spezifitäts-, Profil- und Neuigkeitsquoten gemäß den Zieltabellen.

### 15.2 MMR-ähnlicher Nutzen

Für Profil, tatsächliche Spezifitätszahl und tatsächliches Neuigkeitsband gilt je Kategorie und Fallback:

```text
lower = max(0, target - quotaDeviation)
upper = min(12, target + quotaDeviation)
```

Eine Aufnahme darf keinen Upper Bound überschreiten. Nach hypothetischer Aufnahme dürfen die insgesamt noch
benötigten Lower-Bound-Plätze die verbleibenden Satzplätze nicht übersteigen; außerdem müssen im noch nicht
ausgewählten Reservoir je Kategorie genügend passende Kandidaten für den Lower Bound verbleiben. Die Prüfung
bleibt eine einfache Restfeasibility ohne globalen Solver oder Backtracking.

Für jede der drei Quotendimensionen ist `deficit(category) = max(target-selectedCount, 0)`. Der Beitrag eines
Kandidaten ist 0, wenn alle Defizite 0 sind, sonst sein Kategoriedefizit geteilt durch das maximale Defizit
dieser Dimension. `quotaFit` ist der arithmetische Mittelwert der drei Beiträge.

Für jeden noch wählbaren Kandidaten:

```text
quality   = candidateScore / 100
diversity = 1 - maximumSimilarityToSelected
quotaFit  = normierter marginaler Beitrag zu noch offenen Zielquoten

utility = 0.55 × quality + 0.30 × diversity + 0.15 × quotaFit
```

Der erste Kandidat verwendet `diversity = 1`.

### 15.3 Kontrollierte Zufallsauswahl

Der Kandidat mit maximalem Nutzen wird nicht stets deterministisch genommen.

1. alle geeigneten Kandidaten kanonisch nach `canonicalSignature` ordnen,
2. Topband: alle geeigneten Kandidaten höchstens `0,04` unter dem maximalen Nutzen,
3. `minimumTopBandUtility` ist das kleinste tatsächlich vorhandene Utility im Topband.
4. Gewicht innerhalb des Topbands:

```text
selectionWeight = (1 + 20 × (utility - minimumTopBandUtility))²
```

5. mit `weightQuantization` / `HALF_EVEN` positiv ganzzahlig quantisieren und Summenüberlauf verhindern,
6. gewichtete Auswahl ausschließlich über `batch-selection/<fallback>/<position>` im Scope
   `batch/<batchNumber>`,
7. Quoten, Caps und Ähnlichkeiten aktualisieren und bis zwölf Kandidaten wiederholen.

Die Auswahl bleibt für denselben Seed vollständig reproduzierbar, erzeugt aber über verschiedene Seeds echte Variation.

Die Kandidatennummern 1 bis 12 folgen der deterministischen Auswahlreihenfolge. Interne Profilslots werden nicht als Kochanweisung dargestellt.

## 16. Geordnete Soft-Fallbacks

| Stufe | Mindestscore | Paarähnlichkeit | Konzeptcap | Vorfahrencap | Quotenabweichung | Profilcap | schwierige Kandidaten |
|---|---:|---:|---:|---:|---:|---:|---:|
| `STRICT` | 55 | 0,58 | 2 | 10 | 0 | 4 | 3 |
| `RELAXED_1` | 50 | 0,65 | 2 | 11 | ±1 | 5 | 4 |
| `RELAXED_2` | 45 | 0,72 | 3 | 12 | ±2 | 5 | 4 |

Zusätzlich gilt:

- bei Reservoirgröße ab 72 startet `STRICT`,
- bei 36 bis 71 darf direkt `RELAXED_1` beginnen,
- bei 12 bis 35 darf direkt `RELAXED_2` beginnen.

Scheitert eine Stufe vor Kandidat 12, beginnt die nächste zulässige Stufe mit leerem Satz auf demselben
Reservoir. Teilmengen werden nicht weitergetragen und es findet keine zweite Proposal-/Reservoirsuche statt.

Nie gelockert werden:

- vier Requirements,
- mindestens zwei spezifische Requirements,
- zufällige Eignung,
- Redundanzregeln,
- Profil-Mindeststruktur,
- Neuigkeits-Hardcaps,
- Ausschlusskonflikte,
- exakter Cooldown sichtbarer Exposition,
- Eindeutigkeit des Kandidaten,
- Begrenzung aller Schleifen.

Kann auch `RELAXED_2` keinen vollständigen Satz bilden, lautet das fachliche Ergebnis `GENERATION_EXHAUSTED`. Ein unvollständiger Satz ist kein Erfolg.

## 17. Typisierte Konfiguration

Die Implementierung bildet sämtliche Defaults in einem unveränderlichen fachlichen Konfigurationsobjekt und validierten Spring-Boot-Properties ab. Es gibt keine verstreuten Magic Numbers.

### 17.1 Kernwerte und Wertebereiche

| Parameter | Default | zulässiger Bereich / Relation |
|---|---:|---|
| Kandidaten pro Satz | 12 | exakt 12 |
| bevorzugte Reservoirgröße | 144 | 12 bis 2.000 |
| strikte Reservoir-Mindestgröße | 72 | 12 bis bevorzugte Größe |
| Mindestgröße für direkten Start in `RELAXED_1` | 36 | 12 bis strikte Mindestgröße |
| maximale Proposal-Versuche | 5.000 | mindestens bevorzugte Größe, höchstens 1.000.000 |
| Ausschlusswahrscheinlichkeit | 0,30 | 0 bis 1 |
| Gewichtsquantisierung | `10^9` | `10^3` bis `10^12` |
| exakter Cooldown | 6 Expositionspositionen | 0 bis 52 |
| Cooldown-Grenzen | 9 / 12 / 16 | streng aufsteigend, jeweils höchstens 104 |
| Ausschluss-Hardcooldown | 4 Challenges | 0 bis 52 |
| Ausschluss-Abklinggrenze | 7 Challenges | größer als Hardcooldown, höchstens 104 |
| `PLANNED`-Faktor | 0,65 | größer als `DIFFICULT`, höchstens 1 |
| `DIFFICULT`-Faktor | 0,20 | größer 0, kleiner als `PLANNED` |
| Stufe-5-Cap | 1 | 0 bis 4 |
| Stufe-4/5-Cap | 2 | mindestens Stufe-5-Cap, höchstens 4 |
| Neuigkeitslast-Cap | 11 | 0 bis 28 |
| strikter Mindestscore | 55 | 0 bis 100 |
| `RELAXED_1`-Mindestscore | 50 | 0 bis strikter Wert |
| `RELAXED_2`-Mindestscore | 45 | 0 bis `RELAXED_1` |
| strikte Paarähnlichkeit | 0,58 | 0 bis 1 |
| `RELAXED_1`-Ähnlichkeit | 0,65 | mindestens strikt, höchstens 1 |
| `RELAXED_2`-Ähnlichkeit | 0,72 | mindestens `RELAXED_1`, höchstens 1 |
| Konzeptcap strikt | 2 | 1 bis 12 |
| Vorfahrencap strikt | 10 | 1 bis 12 |
| Profilcap strikt | 4 | 1 bis 12 |
| Kandidaten mit `DIFFICULT` strikt | 3 | 0 bis 12 |
| MMR Qualität / Diversität / Quote | 0,55 / 0,30 / 0,15 | jeweils 0 bis 1, Summe exakt 1 |
| Topbandbreite | 0,04 | 0 bis 0,25 |
| informative Vorfahren, maximaler Ziehpoolanteil | 0,25 | 0 bis 1 |
| Rollen-/Profil-Untergewichte | 0,90 / 0,10 | jeweils 0 bis 1, Summe exakt 1 |
| Neuigkeitsband-/Last-Untergewichte | 0,60 / 0,40 | jeweils 0 bis 1, Summe exakt 1 |
| Flag-/Dimensions-Untergewichte | 0,40 / 0,60 | jeweils 0 bis 1, Summe exakt 1 |
| kanonische Payloadversion | 1 | positive ganze Zahl |
| Verarbeitungslease für `PENDING`/`CONTEXT_READY` | 15 Minuten | 1 Minute bis 24 Stunden |

Die vollständigen Faktor-, Punkte-, Profil- und Quotentabellen aus den vorangehenden Abschnitten sind ebenfalls Bestandteil des Default-Konfigurationssnapshots. Jede Verteilung muss nichtnegative Werte besitzen und nach der kontextabhängigen Projektion mindestens eine positive Kategorie behalten.

Mindestens enthalten:

- `generatorVersion = 1.1.0`,
- `configurationVersion = 2026-08-15.1`,
- `rngAlgorithm = SPLITMIX64_V1`,
- `candidateSetSize = 12`,
- Reservoir- und Versuchslimits,
- Rollenklassen und Profildefinitionen,
- Spezifitätsgewichte und Satzquoten,
- Neuigkeitspunkte, Faktoren, Bänder und Kadenzquoten,
- Verfügbarkeitsfaktoren,
- Cooldownfenster und Faktoren,
- Ausschlusswahrscheinlichkeit und Wiederholungsfaktoren,
- Scorekomponenten und Gewichte,
- Ähnlichkeitskomponenten,
- MMR-Gewichte und Topband,
- alle Fallbackstufen.

Der produktive Default für neue Sessions ist `generatorVersion = 1.2.0`,
`configurationVersion = 2026-08-15.1`, `exclusionProbability = 0.20` und eine vollständige
`RESTRICTION`-Ähnlichkeitsgewichtung. Die vorstehenden `1.1.0`-Werte bleiben der historische
Konfigurationsvertrag für gespeicherte Replays und werden nicht nachträglich überschrieben.

Fail-fast-Validierung mindestens für:

- positive und endliche Faktoren,
- Score- und Ähnlichkeitsgewichte mit Summe 1,
- jede Ähnlichkeits-Untergewichtsgruppe mit Summe exakt 1 und Vorfahrenanteil in `[0,1]`,
- Zielquoten mit Summe 12 vor und nach jeder kontextabhängigen Projektion,
- aufsteigende Fallbacklockerung,
- `reservoirTarget >= reservoirMinimum >= candidateSetSize`,
- ausreichende maximale Proposal-Versuche,
- gültige Cooldownintervalle,
- keine widersprüchlichen Caps,
- ausschließlich erreichbare projizierte Kategorien und deterministische größte-Rest-Zuordnung,
- numerisch darstellbare Gewichtssummen.

Eine frei administrierbare Regel-DSL ist nicht vorgesehen.

## 18. Reason-Codes

Reason-Codes sind stabile maschinenlesbare Großschreibungswerte. Freitext ist ergänzend, nicht autoritativ.

### 18.1 Vorgabeneignung

- `CONCEPT_INACTIVE`
- `RANDOM_DRAW_DISABLED`
- `FUNCTIONAL_ROLE_MISSING`
- `NOVELTY_MISSING`
- `AVAILABILITY_MISSING`
- `AVAILABILITY_UNAVAILABLE`
- `EXACT_COOLDOWN_BLOCKED`
- `REROLL_EXACT_BLOCKED` — **Legacy v1.0**; bleibt für historische Diagnosen lesbar und wird von v1.1 nicht neu emittiert.
- `EXCLUSION_TARGET_BLOCKED`
- `PROFILE_SLOT_INELIGIBLE`
- `EFFECTIVE_WEIGHT_ROUNDED_TO_ZERO`
- `WEIGHT_SUM_OVERFLOW`
- `EMPTY_WEIGHTED_POOL`

### 18.2 Harte Kandidatenablehnung

- `REQUIREMENT_COUNT_INVALID`
- `SPECIFIC_REQUIREMENT_MINIMUM_MISSED`
- `RANDOM_CONCEPT_DUPLICATE`
- `RANDOM_MANUAL_DUPLICATE`
- `REFINEMENT_REDUNDANCY`
- `PROFILE_UNSATISFIED`
- `ANCHOR_REQUIREMENT_MINIMUM_MISSED`
- `ANCHOR_ROLE_BREADTH_MISSED`
- `NON_ANCHOR_REQUIREMENT_MAX_EXCEEDED`
- `NOVELTY_LEVEL_FIVE_MAX_EXCEEDED`
- `NOVELTY_HIGH_MAX_EXCEEDED`
- `NOVELTY_LOAD_MAX_EXCEEDED`
- `CANDIDATE_EXCLUSION_CONFLICT`

### 18.3 Weiche Bewertung

- `STRONG_STRUCTURE`
- `ROLE_COMPLEMENTARY`
- `LOW_ROLE_BREADTH`
- `CREATIVE_TENSION_PRESENT`
- `STANDARD_TEMPLATE_RISK`
- `NOVELTY_TARGET_MATCH`
- `NOVELTY_TARGET_MISMATCH`
- `PLANNED_AVAILABILITY_LOAD`
- `DIFFICULT_AVAILABILITY_LOAD`
- `RECENT_SEMANTIC_FAMILY`
- `KNOWN_INTENSITY_STACKING`
- `LOW_PROPERTY_CONFIDENCE`
- `UNCLASSIFIED_MANUAL_REQUIREMENT`
- `MANUAL_REQUIREMENT_REDUNDANCY`
- `MANUAL_NOVELTY_FORCED`

### 18.4 Satz und Lifecycle

- `NOVELTY_CADENCE_RECOVERY`
- `NOVELTY_CADENCE_NEUTRAL`
- `NOVELTY_CADENCE_SEEKING_VARIETY`
- `EXCLUSION_MODE_NOT_SELECTED`
- `EXCLUSION_RULE_SELECTED`
- `EXCLUSION_RULE_NO_TARGETS`
- `EXCLUSION_RULE_MANUAL_CONFLICT`
- `EXCLUSION_RULE_REPEAT_BLOCKED`
- `EXCLUSION_RULE_WEIGHT_ROUNDED_TO_ZERO`
- `DUPLICATE_CANDIDATE_SIGNATURE`
- `RESERVOIR_TARGET_REACHED`
- `PROPOSAL_ATTEMPT_LIMIT_REACHED`

- `PAIR_EXACT_OVERLAP`
- `PAIR_ANCESTOR_OVERLAP`
- `PAIR_SIMILARITY_LIMIT`
- `CANDIDATE_SCORE_MINIMUM`
- `CONCEPT_SET_CAP`
- `ANCESTOR_SET_CAP`
- `PROFILE_SET_CAP`
- `DIFFICULT_SET_CAP`
- `PROFILE_TARGET_DEVIATION`
- `SPECIFICITY_TARGET_DEVIATION`
- `NOVELTY_TARGET_DEVIATION`
- `SPECIFICITY_TARGET_PROJECTED`
- `PROFILE_TARGET_PROJECTED`
- `NOVELTY_TARGET_PROJECTED`
- `SOFT_FALLBACK_RELAXED_1`
- `SOFT_FALLBACK_RELAXED_2`
- `NO_ELIGIBLE_EXCLUSION_RULE`
- `GENERATION_EXHAUSTED`
- `INVALID_GENERATION_REQUEST`
- `UNSUPPORTED_GENERATOR_VERSION`
- `REPLAY_FINGERPRINT_MISMATCH`
- `TECHNICAL_GENERATION_FAILURE`
- `ATTEMPT_IN_PROGRESS`
- `STALE_PENDING_ATTEMPT`
- `CONTEXT_SNAPSHOT_INVALID`
- `CURATION_BATCH_MISMATCH`

Neue Codes werden versioniert ergänzt und nicht durch wechselnde Freitexte ersetzt.

## 19. Persistenz- und Lifecycleentscheidung

Das aktuelle Baselineschema hängt Kandidaten unmittelbar unter `curation_round`, obwohl dort bereits Kuratormodell und Promptversion verpflichtend sind. Phase 9D trennt dies append-only.

Zielstruktur:

```text
challenge_session
  └─ generation_attempt
       ├─ generation_manual_requirement (0..2)
       ├─ generation_context_snapshot (genau 1 ab CONTEXT_READY)
       └─ generation_batch (1..2)
            └─ challenge_candidate (genau 12 bei GENERATED)
                 └─ candidate_requirement (genau 4)

curation_round
  └─ curation_round_candidate (erst Phase 10)
       ├─ verweist auf Kandidaten desselben Attempts aus Batch 1 oder 2
       └─ trägt Kuratorbewertung und Teilnahmerolle
```

Generator- und Kuratordaten bleiben damit auch dann getrennt, wenn derselbe Kandidat später erneut analysiert werden müsste.

### 19.1 Generation Attempt und Context Snapshot

Der Attempt besitzt die für alle seine internen Batches unveränderlichen Werte:

- Session und Attempt-Typ,
- manuelle Vorgaben,
- angeforderten Stichtag und wirksamen Monat,
- Attempt-Master-Seed und RNG,
- Generator- und Konfigurationsversion,
- kanonischen Konfigurationssnapshot,
- kanonischen Katalog-, Request- und sichtbaren Historiensnapshot,
- Attempt-Ausschlussentscheidung,
- Context- und Snapshotfingerprints,
- Status, Verarbeitungstoken und Lease-Zeitstempel.

Der Context Snapshot wird einmal eingefroren und von späteren internen Batches desselben Attempts wiederverwendet. Eine komplette Kuratorablehnung führt daher nicht nebenbei zu einem anderen Katalog-, Saison-, Historien- oder Ausschlusszustand.

### 19.2 Generation Batch

Ein Batch speichert ausschließlich seine rundenbezogenen Werte:

- eindeutige positive Batchnummer innerhalb des Attempts,
- abgeleiteten Batch-Seed,
- Status und Zeitstempel,
- Reservoirgröße und Proposal-Versuche,
- Hard-/Soft-Rejection-Zähler,
- verwendete Fallbackstufe,
- Satzquoten, Ähnlichkeitsstatistik und weitere Setdiagnosen,
- Set-Fingerprint.

Ein `GENERATED`-Batch enthält genau zwölf eindeutige Kandidaten. Ein `EXHAUSTED`-Batch enthält keine scheinbar teilweise erfolgreichen Kandidaten, aber vollständige Diagnose. Technische Fehler erzeugen keinen als fachlich erschöpft markierten Batch.

### 19.3 Candidate, Requirement und Kuratorbewertung

Kandidaten speichern generatorseitig Profil, Band, Gesamt- und Komponentenscores, Konfidenz, Reason-Codes und Kandidatensignatur. Requirements speichern damaligen Text, Quelle, Spezifität, Rollen, Neuigkeit, Beschaffbarkeit, relevante bekannte Eigenschaften und Gewichtsfaktoren.

Kuratorscores, Kurator-Reason-Codes und Auswahlstatus gehören nicht auf den generatorseitigen Kandidaten. Sie werden später je `curation_round` und Kandidat in der flexiblen Teilnahmerelation gespeichert. Phase 9D erzeugt weder diese Bewertung noch ein Offer oder eine sichtbare Challenge.

Phase 11A persistiert zusätzlich ein rerolltes sichtbares Offer Set als genau ein Cooldown-only-Expositionsereignis mit den damaligen exakten Requirement-Codes. Dieses Modell ist keine neue Phase-9-Tabelle.

### 19.4 Attempt- und Batchzustände

Phase 9 verwendet für `generation_attempt` mindestens:

- `PENDING`: Request, manuelle Vorgaben und Attempt-Seed sind angelegt; der vollständige Context Snapshot fehlt noch.
- `CONTEXT_READY`: der unveränderliche Context Snapshot ist persistiert; Berechnung darf außerhalb einer Write-Transaktion laufen.
- `GENERATED`: mindestens der aktuelle Batch wurde vollständig atomar gespeichert und wartet auf spätere Kuratierung.
- `EXHAUSTED`: die angeforderte Generatorrunde endete fachlich ohne vollständigen Zwölfer-Satz.
- `FAILED`: ein technischer oder nicht wiederherstellbarer Lifecyclefehler ist terminal dokumentiert.

Phase 10 darf den Lifecycle um erfolgreiche Kuratierung beziehungsweise sichtbare Challenge erweitern; Phase 9 simuliert diese Zustände nicht.

`generation_batch.status` ist in Phase 9 genau `GENERATED` oder `EXHAUSTED`. Ein halbfertiger Batch wird nie sichtbar persistiert.

### 19.5 Idempotenz, Konkurrenz und Restart

- Die Eindeutigkeit von `(challenge_session_id, attempt_type)` verhindert doppelte INITIAL- oder REROLL-Attempts.
- Ein Retry nach erfolgreichem Commit liest den vorhandenen Attempt und Batch, statt neu zu würfeln.
- Ein nicht abgelaufenes `PENDING` oder `CONTEXT_READY` liefert `ATTEMPT_IN_PROGRESS`.
- Verarbeitung wird über ein zufälliges Operationstoken und eine Defaultlease von 15 Minuten beansprucht. Claim und Tokenwechsel erfolgen unter PostgreSQL-Zeilensperre.
- Ein abgelaufenes `PENDING` darf mit demselben Request und Attempt-Seed neu beansprucht werden; da noch kein Context Snapshot existiert, wird er erst beim erfolgreichen Recoverylauf materialisiert.
- Ein abgelaufenes `CONTEXT_READY` wird ausschließlich mit dem bereits gespeicherten Snapshot und Seed fortgesetzt.
- Fehlen bei einem verwaisten Attempt notwendige persistierte Eingaben oder ist der Snapshotfingerprint inkonsistent, wird er mit `CONTEXT_SNAPSHOT_INVALID` technisch auf `FAILED` gesetzt; dies ist keine fachliche Erschöpfung.
- Der vollständige Batch, seine Kandidaten, Requirements, Scores, Diagnosen und die Attempt-Statusänderung werden in einer kurzen Transaktion atomar gespeichert.
- Unbekannte PostgreSQL- und Laufzeitfehler bleiben `TECHNICAL_GENERATION_FAILURE` und werden weder als Konkurrenz noch als Erschöpfung maskiert.

Phase 9D implementiert ausschließlich Batch 1. Phase 10B erzeugt bei Bedarf genau einen Batch 2 unter demselben unveränderlichen, persistierten Context Snapshot und dem bestehenden Seedvertrag; sie lädt weder Katalog noch Historie neu. Batch 3 und unbeschränkte interne Regeneration werden verhindert. Diese Orchestrierung verändert keine Phase-9-Generatorregel und erzeugt keine sichtbare Historienexposition.

## 20. Replay

Replay verwendet ausschließlich:

- gespeicherten Input-/Katalog-/Historiensnapshot,
- gespeicherten Konfigurationssnapshot,
- Attempt-Seed, Batchnummer, abgeleiteten Batch-Seed und RNG,
- exakt unterstützte Generatorversion.

Aktuelle Katalogwerte überschreiben historische Snapshots nicht. Replay schreibt keine operativen Daten und erzeugt keine Exposition.

Verglichen werden in dieser Reihenfolge:

1. Request- und Snapshotfingerprints,
2. Attempt-Ausschlussentscheidung,
3. Rejection-Zähler und Reservoirsignaturen,
4. Kandidatensignaturen,
5. Scores und Reason-Codes,
6. Setreihenfolge und Set-Fingerprint.

Die erste Abweichungsstelle wird diagnostiziert. Eine nicht mehr unterstützte Version liefert `UNSUPPORTED_GENERATOR_VERSION`, nicht einen scheinbaren Zufallsfehler. Historische v1.0-Snapshots dürfen weiter angezeigt werden; die v1.1-Implementierung gibt für nicht unterstütztes v1.0-Replay ausdrücklich `UNSUPPORTED_GENERATOR_VERSION` aus, statt den entfernten REROLL-Hardblock unter neuer Semantik nachzubauen.

### 20.1 Kanonische Serialisierung und Fingerprints

Replayrelevante Payloads verwenden `canonicalPayloadVersion = 1` und folgenden Bytevertrag:

- UTF-8 ohne BOM,
- JSON-Objektschlüssel lexikografisch nach Unicode-Codepoint,
- Mengen und Maps vor Serialisierung nach ihren stabilen Codes, danach IDs sortiert,
- positionsgebundene Requirements nach Position,
- Kandidaten im finalen Set nach ihrer Auswahl-/Kandidatennummer,
- Reason-Codes lexikografisch,
- Strings in Unicode-NFC,
- Dezimalzahlen in Plain-Notation ohne Exponent und mit der für das Feld festgelegten Scale,
- Zeitpunkte als UTC-RFC-3339 mit Mikrosekundenauflösung und Suffix `Z`,
- keine abgeleiteten Anzeigenamen oder aktuellen Katalogwerte außerhalb des gespeicherten Snapshots.

Der Fingerprint ist SHA-256 über die kanonischen Bytes und wird als kleingeschriebene hexadezimale
Zeichenfolge gespeichert. Der Set-Fingerprint umfasst Generator- und Konfigurationsversion, Payloadversion,
Batchnummer und abgeleiteten Batch-Seed, bei `1.0.x`/`1.1.x` die Attempt-Ausschlussentscheidung und bei
`1.2.0` den Restriction Mode sowie die Restriktion jedes Kandidaten, verwendete Fallbackstufe,
Setdiagnose einschließlich Reservoirmetriken und Fallbackversuchen sowie die geordnete vollständige
Kandidatenliste in Auswahlreihenfolge. Nicht ausgewählte Reservoirkandidaten gehören nicht zur Set-Payload.
Separate Snapshotfingerprints erlauben, eine Abweichung vor dem eigentlichen Generatorlauf zu lokalisieren.

### 20.2 Generator-Labor (Phase 9E1 / Issue #37)

Das Generator-Labor verwendet dieselbe öffentliche Generatorpipeline wie die persistente Generation, ruft aber
niemals `GenerationCommands` auf. Eine Preview materialisiert ihren Katalog- und Historiensnapshot nur lesend;
`PRODUCTION_VISIBLE` verwendet die sichtbare Historienprojektion, synthetische Szenarien bleiben explizit getrennt.
Es entstehen weder Session, Attempt, Batch, Candidate noch Historienexposition.

Ab Generator 1.1 besitzt das Labor keine editierbaren REROLL-Hardblock-IDs mehr. Ein diagnostischer REROLL wird ausschließlich durch Attempt-Typ, Katalog, Manuals, Seed und den gewählten Historiensnapshot bestimmt; exakte Cooldowns kommen aus diesem Snapshot.

Persistierte Attempts und Batches werden ausschließlich aus ihren gespeicherten Snapshots angezeigt. Replay ist
ebenfalls read-only und vergleicht Fingerprint, Kandidatenreihenfolge/-signatur, Gesamt- und Komponentenscores,
Reason-Codes sowie Setevaluation. Die erste relevante Abweichung ist als begrenzter strukturierter Wert sichtbar;
eine nicht unterstützte Version bleibt ausdrücklich kein Mismatch.

### 20.3 Begrenzte Simulation und kanonischer Report (Phase 9E2 / Issue #53)

`GeneratorSimulation` ist die kleine öffentliche, transportneutrale Application-API für einen begrenzten
Simulationslauf. Ein Request benennt nur explizite Seeds (`SeedRange` oder feste Liste), einen stabilen
`HistoryScenario`, `INITIAL` oder `REROLL`, null bis zwei Manuals, die effektiven Daten und
eine sichtbare Kandidatenposition `1..12`. Eine Datumsfolge ist eine Sequenz; ihre Schritte sind strikt aufsteigend.
Legacy-v1.0-Felder für einen separaten REROLL-Block werden in v1.1 auf leer normalisiert und beeinflussen die Simulation nicht.
Die API akzeptiert nie `SeedSource`, begrenzt jeden Application-Run fail-fast auf 4.096 Fälle und erlaubt dem
aufrufenden Adapter nur strengere Grenzen.

Zu Beginn eines Laufs materialisiert eine einzige read-only-`REPEATABLE READ`-Transaktion alle benötigten
Monats-`CatalogGeneratorSnapshot`s sowie bei `PRODUCTION_VISIBLE` genau einen `VisibleHistorySnapshot`. Danach
nutzen Preview und Simulation denselben reinen `GeneratorRunExecution`-Kern über diese Eingaben: kein JDBC-Zugriff,
keine Produktionwrites, keine implizite Parallelisierung. Erfolgreiche Sequenzschritte schreiben ausschließlich eine
synthetische, nicht persistierte Exposure aus der gewählten Kandidatenposition, deren Requirements, Profil,
Ist-Neuigkeit und Ausschlussentscheidung fort. Erschöpfung oder ein technischer Fehler erzeugen keine Exposure und
lassen die betroffene Sequenz ausdrücklich unvollständig.

Der `SimulationReport` trennt fachliche Erschöpfung, technische Fehler und Replay-/Integritätsabweichungen. Seine
Invariantenzähler lesen ausschließlich vorhandene Set-, Weight- und Diagnoseartefakte; er enthält keinen zweiten
Hard-Rule- oder Statistikpfad. Ein historisch vorhandener `rerollViolations`-Zähler bleibt aus Reportkompatibilitätsgründen lesbar und muss für neue v1.1-Läufe null bleiben. Frequenz- und Fingerprintlisten sind nach stabilen Schlüsseln sortiert und jeweils auf
50 Einträge begrenzt. Der JSON-Report unter `target/generator-simulation/ci-scenarios-report.json` enthält eine
kanonische Nutzlast mit Report-, Generator-, Konfigurations-, RNG- und Szenarioversion, Seedplan,
`catalogFingerprintsByMonth` und Run-Katalogfingerprint. `elapsedMillis` liegt nur im separaten Laufzeitabschnitt und
nie im kanonischen Reportfingerprint.

Der kleine PostgreSQL-/Testcontainers-Reportweg ist absichtlich explizit und CI-tauglich:

```bash
./mvnw clean verify -Dtest=GeneratorSimulationIntegrationTest
```

Er schreibt den maschinenlesbaren Report, prüft seine kanonische Reproduzierbarkeit sowie die 4.096/4.097-Grenze und
deckt INITIAL/REROLL, null bis zwei Manuals, echte und synthetische Historie, Sequenzfortschreibung, Timeout und
technische Fehler ab. Die große Issue-#47-Matrix bleibt bewusst opt-in und delegiert ebenfalls an diesen Kern:

```bash
./mvnw clean verify -Pgenerator-baseline -Dtest=CandidateSetBaselineIntegrationTest
```

### 20.4 Vollständige Kalibrierung (Phase 9F / Issue #40)

Der vollständige 9.216-Attempt-Lauf ist ebenfalls strikt opt-in. Sein versioniertes Manifest partitioniert die
6.144 Default- und 3.072 Ausschlussfokus-Fälle in vier sequentielle `GeneratorSimulation`-Requests, von denen keiner
die harte Application-Grenze von 4.096 Fällen überschreitet. Zusätzliche Mehrwochensequenzen bleiben davon getrennt.

```bash
./mvnw clean verify -Pgenerator-calibration -Dtest=CandidateGeneratorCalibrationIntegrationTest
```

Der Runner ist standardmäßig durch Surefire ausgeschlossen und wird in keinem GitHub-Actions-, PR-, Push-, Nightly-
oder Scheduled-Workflow aufgerufen. Er schreibt kanonische Rohreports ausschließlich unter
`target/generator-calibration/`. Zusammenfassung, Ursachenklassifikation, festes Acht-Satz-Korpus und die Anleitung
für den read-only operativen Adminlauf stehen in [`CANDIDATE_GENERATOR_CALIBRATION.md`](CANDIDATE_GENERATOR_CALIBRATION.md).
Der technische Lauf erklärt weder den operativen Kataloglauf noch die Administratorbewertung für bestanden.

Generatorversion 1.1 ändert bewusst die REROLL-Semantik nach Abschluss des historischen Phase-9F-Gates. Sie löst keinen fingierten nachträglichen 9.216-Attempt-Erfolg aus; zielgerichtete REROLL-/Cooldown-/Replay-Regressionsprüfungen plus `clean verify` sind für Issue #63 maßgeblich. Ein erneuter vollständiger Kalibrierungslauf bleibt eine separate bewusste Entscheidung.

## 21. Test- und Simulationsvertrag

### 21.1 Reine Fachtests

Kleine synthetische Kataloge decken mindestens ab:

- identisches Replay innerhalb derselben unterstützten Generatorversion,
- Variation verschiedener Seeds,
- Gewicht, Saison und Beschaffbarkeit,
- alle Cooldownstufen,
- REROLL ohne separaten Zutatenblock,
- exakter sichtbarer Cooldown bei REROLL ohne Descendant-/Ancestor-/Sibling-Expansion,
- Profile und Mehrfachrollen,
- 2/3/4-Spezifitätsmixe,
- Parent-/Child-Redundanz,
- null bis zwei manuelle Vorgaben einschließlich projizierter Spezifitäts-, Profil- und Neuigkeitsquoten,
- Ausschlüsse mit und ohne Refinements,
- Neuigkeits-Hardcaps und Kadenz,
- fehlende optionale Dimensionen,
- alle Fallbackstufen,
- begrenzte Erschöpfung.

### 21.2 PostgreSQL-Integration

Testcontainers prüft mindestens:

- vollständige Katalogprojektion,
- transitive Graphauflösung und Mehrfach-Eltern,
- Rollen, Neuigkeit, Verfügbarkeit und Saison,
- Ausschlussziel-Expansion,
- sichtbare Historienprojektion,
- keine Exposition durch interne Kandidaten,
- stabile kanonische Reihenfolge,
- später Persistenz, Konkurrenz, Restart und atomaren Rollback.

H2 ist kein Ersatz.

### 21.3 Versionierte Kontextfixtures

Die vollständige 2.304-Attempt-Matrix, Paarähnlichkeits- und Satzdiversitätsgates werden in Phase 9C2 /
Issue #47 umgesetzt. Phase 9C1 prüft stattdessen repräsentative feste Seeds und Monate gegen die reale
PostgreSQL-`CatalogGeneratorProjection`, einschließlich deterministischem Reservoir-Replay und vollständiger
Proposal-/Treffermetriken.

Die Baselinesimulation besitzt historisch folgende unveränderlich benannte Fixtures:

1. `EMPTY_INITIAL`: INITIAL ohne sichtbare Historie und ohne manuelle Vorgabe.
2. `NEUTRAL_HISTORY`: normale gemischte sichtbare Historie.
3. `RECOVERY_AFTER_ADVENTUROUS`: unmittelbar vorherige abenteuerliche Challenge mit Stufe 5.
4. `SEEKING_AFTER_THREE_FAMILIAR`: drei unmittelbar vorherige vertraute Challenges.
5. `LOADED_COOLDOWN_HISTORY`: mehrere exakte und semantisch nahe Expositionen in den relevanten Fenstern.
6. `REROLL_EXACT_BLOCK`: historischer Fixture-Code aus v1.0. Unter v1.1 bleibt der Name für Berichtskontinuität erhalten, ein separater Blockinput wird jedoch leer normalisiert; REROLL-Wiederholung stammt ausschließlich aus der sichtbaren Historie.
7. `ONE_MATCHED_MANUAL`: eine gematchte manuelle Vorgabe mit Rollen und Neuigkeit.
8. `TWO_MIXED_MANUALS`: eine gematchte und eine unklassifizierte manuelle Vorgabe einschließlich Quotenprojektion.

Jedes Fixture läuft für alle zwölf Saisonmonate. Gezielte synthetische Fixtures ergänzen dünne Rollenpools, fehlende optionale Dimensionen, ausschließlich schwierige Beschaffbarkeit und echte Erschöpfung; sie werden getrennt von der regulären Baseline ausgewertet.

### 21.4 Reproduzierbare Suitegrößen

**Explizite Issue-#47-Baseline (nicht Teil des normalen `verify`):**

- 12 Monate × 8 Fixtures × 16 aufeinanderfolgende feste Seeds = 1.536 Attempts mit Defaultkonfiguration,
- zusätzlich 12 Monate × 4 manualfreie Fixtures × 2 Konfigurationsvarianten (`exclusionProbability = 0` und `1`) × 8 feste Seeds = 768 Attempts,
- insgesamt 2.304 Attempts; jeder erfolgreiche Satz wird einmal replayt.

**Vollständiger Kalibrierungslauf aus Phase 9F:**

- dieselbe Hauptmatrix mit 64 Seeds = 6.144 Attempts,
- dieselbe fokussierte Ausschlussmatrix mit 32 Seeds = 3.072 Attempts,
- insgesamt 9.216 Attempts plus die dokumentierten synthetischen Fehler- und Dünnpoolfälle.

Seedbereiche, Fixtureversion und Konfigurationsversion werden im Report gespeichert. Zusätzliche Zufallsstichproben sind erlaubt, ersetzen diese Matrix aber nicht.

### 21.5 Verbindliche Metriken

Mindestens zu berichten sind:

- Hard-Rule-Verletzungen und manuell erzwungene Ausnahmen getrennt,
- Replay- und Fingerprintabweichungen,
- Erschöpfungsquote getrennt nach regulärer Baseline und absichtlichem Dünnpool,
- Proposal-Trefferquote, Median, 95. Perzentil und Maximum der Versuche,
- erreichte Reservoirgröße,
- Fallbacknutzung je Stufe und Fixture,
- Konzeptfrequenzen relativ zu effektiven Gewichten sowie Top-1-/Top-10-Konzentration,
- Rollen-, Profil- und projizierte Spezifitätsverteilung,
- Neuigkeitsbänder und mehrwöchige Recovery-/Seeking-Sequenzen,
- Ausschlussfrequenz, Regelfrequenz und Wiederholung,
- Beschaffbarkeitslast für beide Teilnehmer,
- Mittelwert, 95. Perzentil und Maximum der Kandidatenpaarähnlichkeit je Satz,
- exakte Konzept- und informative Vorfahrenüberschneidungen,
- Kandidaten- und Setkonfidenz bei fehlenden optionalen Metadaten,
- Laufzeit als Berichtswert sowie Einhaltung sämtlicher Suchgrenzen.

### 21.6 Automatische Baseline-Gates

Für den regulären Repository-Baseline-Katalog gelten vor der manuellen Abnahme:

- exakt 0 Generator-Hard-Rule-Verletzungen,
- exakt 0 unerklärte Replay- oder Fingerprintabweichungen,
- exakt 0 Erschöpfungen und 0 unvollständige Erfolgssätze,
- exakt 0 Cooldown- und Ausschlussverletzungen; der Legacy-Zähler für REROLL-Hardblock-Verletzungen bleibt bei v1.1 ebenfalls 0, weil diese Regel nicht mehr erzeugt wird,
- 100 % der erfolgreichen Sätze mit zwölf eindeutigen Kandidaten und je vier Requirements,
- mindestens 95 % `STRICT`, höchstens 5 % `RELAXED_1` und 0 % `RELAXED_2`,
- 95. Perzentil der Proposal-Versuche höchstens 4.000 und absolutes Maximum höchstens 5.000,
- in `STRICT` exakt die nach Abschnitt 7.1 projizierten Spezifitäts-, Profil- und Neuigkeitsquoten,
- sämtliche Konzept-, Vorfahren-, Profil-, Beschaffbarkeits- und Paarähnlichkeitscaps innerhalb der jeweils verwendeten Fallbackstufe,
- mittlere Paarähnlichkeit eines `STRICT`-Satzes höchstens 0,42,
- in `RECOVERY_AFTER_ADVENTUROUS` ohne manuell erzwungene Ausnahme exakt 0 `ADVENTUROUS`-Kandidaten,
- in `SEEKING_AFTER_THREE_FAMILIAR` bei `STRICT` exakt die projizierte Zielzahl abenteuerlicher Kandidaten,
- bei Default-Ausschlusswahrscheinlichkeit im vollständigen Lauf eine tatsächliche Ausschlussquote von 25 % bis 35 %,
- bei erzwungen ausgeschalteter beziehungsweise eingeschalteter Ausschlussvariante 0 % beziehungsweise 100 % geeignete Attempts mit Ausschluss,
- im vollständigen Kalibrierungslauf aus Phase 9F pro Monat und Fixture mindestens 95 % unterschiedliche
  Set-Fingerprints über die dort verbindlichen 64 Seeds; die 16-Seed-Matrix aus Phase 9C2 berichtet diesen
  Wert nur als Variationsmetrik und interpretiert ihn nicht als Ersatzgate,
- kein einzelnes zufälliges Konzept über 5 % und die zehn häufigsten zusammen nicht über 30 % aller zufälligen Requirement-Slots der vollständigen Baseline.

Die Konzentrations- und Ausschlussgrenzen sind bewusst breit. Schlagen sie fehl, wird die Ursache untersucht; die Grenzen werden nicht nachträglich bequem um das erste Ergebnis gemalt. Synthetische Dünnpools dürfen erwartbar erschöpfen oder Fallbacks nutzen, müssen aber ebenfalls null Hard-Rule- und Replayverletzungen besitzen.

## 22. Beispiele

Die Beispiele erklären Regeln. Sie werden nicht als Zutaten-Sonderfälle implementiert.

### 22.1 Hart gültig und ausgewogen

`Kabeljau`, `Kohlgemüse`, `Birne`, `Miso`

- mindestens zwei spezifische Vorgaben,
- mehrere Ankerrollen,
- kein Parent-/Child-Konflikt,
- überschaubare Neuigkeitslast,
- mehrere denkbare Richtungen.

### 22.2 Hart gültig, aber trivialitätsgefährdet

`Hähnchen`, `Paprika`, `Kokosmilch`, `Currypaste`

Strukturell brauchbar und daher nicht hart verboten. Der Offenheits- und Spannungsscore kann niedrig sein; zwölf Varianten dieses Musters dürfen den Satz nicht dominieren.

### 22.3 Vier spezifische Vorgaben mit Spannung

`Schweinefleisch`, `Fenchel`, `Pflaume`, `Miso`

Vier spezifische Vorgaben sind nicht automatisch zu geschlossen. Rollen, semantische Distanz und kontrollierte Spannung können einen guten Kandidaten ergeben.

### 22.4 Interessant, aber nicht garantiert lecker

`weißfleischiger Fisch`, `Kohlgemüse`, `Kaffee`, `Orange`

Die Struktur kann tragfähig sein. Der Generator garantiert keine konkrete Fischwahl und keine perfekte Kaffee-Dosierung. Genau diese Restunsicherheit gehört zur Challenge und zur späteren Kuratierung.

### 22.5 Parent-/Child-Redundanz

`Fisch`, `Lachs`, `Fenchel`, `Miso`

Hart abzulehnen, weil `Lachs` die Vorgabe `Fisch` bereits erfüllt.

### 22.6 Ausschlusskonflikt

Vorgaben enthalten `Tofu`; Attempt-Regel schließt `pflanzliche Proteinprodukte` einschließlich Refinements aus.

Hart abzulehnen.

### 22.7 Drei-Wochen-Neuigkeitsfolge

- Woche 1 bestätigt sichtbar: Kandidat mit Stufe-5-Hauptzutat, Band `ADVENTUROUS`.
- Woche 2: Zustand `RECOVERY`, keine abenteuerlichen Kandidaten im Satz und keine zufällige Stufe 5.
- Woche 3: bei normaler Woche 2 wieder `NEUTRAL`; ungewöhnliche Kandidaten sind möglich, aber nicht garantiert.

Damit wird Außergewöhnlichkeit nicht verbannt, aber eine automatische Zungen-Schnecken-Stinky-Tofu-Trilogie unterbunden.

### 22.8 REROLL

Das sichtbare Offer Set enthält beispielsweise drei Optionen. In einer davon steht `Spargel`; der Nutzer verwirft das gesamte Offer Set, weil ihm die angebotenen **Kombinationen** nicht zusagen. Für den Ersatzattempt gibt es keinen separaten Zutatenblock. Stattdessen enthält die normale Cooldown-Historie genau eine neue Expositionsposition mit den exakten Katalogkonzepten aller tatsächlich gezeigten Optionen.

`Spargel` erhält dadurch den normalen exakten Cooldownfaktor 0. `Grüner Spargel` bleibt grundsätzlich ziehbar, weil Konkretisierungen nicht mitgesperrt werden. Die verworfenen Angebote verändern die Neuigkeitskadenz nicht. Wäre stattdessen eine der Optionen bestätigt worden, hätte nur diese bestätigte Challenge die normale vollständige Historienwirkung erzeugt.

### 22.9 Manuelle Vorgabe und Quotenprojektion

Die einzige manuelle Vorgabe ist ein unklassifizierter Freitext. Es bleiben drei Zufallsslots; vier spezifische Gesamtvorgaben sind dadurch unerreichbar. Die Basisgewichte `4:5:3` werden auf die erreichbaren Gesamtwerte 2 und 3 projiziert, die Zwölfer-Quote per größtem Rest neu berechnet und `SPECIFICITY_TARGET_PROJECTED` diagnostiziert.

### 22.10 Dünner Pool mit Soft-Fallback

Das Reservoir enthält 48 harte Kandidaten. `RELAXED_1` darf Score, Quoten und Paarähnlichkeit begrenzt lockern. Parent-/Child-Regeln oder Mindeststruktur bleiben unverändert.

### 22.11 Echte Erschöpfung

Nach 5.000 Proposals existieren nur neun eindeutige harte Kandidaten. Ergebnis ist `GENERATION_EXHAUSTED` mit Ablehnungszählern, kein Neuner-Satz und keine heimliche Regelaufweichung.

### 22.12 Niedrige Datenkonfidenz

Ein strukturell guter Kandidat besitzt nur wenige gepflegte Dimensionen. Er bleibt zulässig, erhält aber eine niedrige Eigenschaftskonfidenz. Die Anwendung darf daraus keine präzise Aussage wie „harmoniert sehr gut“ ableiten.

## 23. Phase-9-Gate und spätere Semantikänderungen

Phase 9 wurde auf Generatorversion 1.0 fachlich und technisch abgeschlossen. Issue #63 präzisiert anschließend die Produktsemantik des freiwilligen Rerolls und führt Generatorversion 1.1 ein. Der historische Phase-9-Abnahmebericht bleibt deshalb historisch korrekt; insbesondere werden frühere MC-/Fixture-Ergebnisse nicht rückwirkend umetikettiert.

Für Generator 1.1 gilt als Nachweis:

- dedizierter REROLL-Zutatenblock wird bei neuen Läufen nicht mehr angewendet,
- normaler exakter Cooldown bleibt unverändert wirksam,
- Parent-/Child-/Sibling-Expansion findet beim Cooldown nicht statt,
- Replay und Fingerprints sind versionssauber getrennt,
- Labor und Simulation besitzen keinen operativen REROLL-Blockparameter mehr,
- gezielte REROLL-/Cooldown-/Replaytests sowie `./mvnw clean verify` sind grün.

Phase 11A implementiert die persistente Cooldown-only-Exposition eines vollständig rerollten Offer Sets mit 1–3 sichtbaren Optionen. Weder der nachfolgende transportneutrale 11B-Voting-/Participation-Core noch der spätere 11C-Discord-Adapter verändern diese Generator-Hardrule.
