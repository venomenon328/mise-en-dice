# Kalibrierung und Abnahme des Kandidatengenerators

Stand: 15. August 2026
Status: technischer Stand vorbereitet und fokussiert geprüft; breite Endmessung, operativer Kataloglauf und manuelle Fachabnahme offen

Dieses Dokument ist das Abnahmeprotokoll für Issue #40. Es trennt den reproduzierbaren Repository-Lauf, den
read-only operativen Kataloglauf und die ausschließlich durch den Administrator vorzunehmende kulinarische
Bewertung. Ein grünes technisches Gate ersetzt die letzten beiden Schritte nicht.

## 1. Identität des Ausgangsstands

| Merkmal | Wert |
|---|---|
| Ausgangscommit | `e70a5a53175755f93e09c13e7999c4e807dfe7cd` |
| Ausgangsdatum | 15. August 2026 |
| Generatorversion | `1.0.0` |
| Konfigurationsversion | `2026-08-12.2` |
| Konfigurationsversion nach fokussiertem Tuning | `2026-08-15.1` |
| RNG | `SPLITMIX64_V1` |
| kanonische Payloadversion | `1` |
| Simulationsreportversion | `2026-08-13.1` |
| Kalibrierungsszenarien | `ISSUE_40_CALIBRATION_V1` |
| finaler Repository-Katalogsnapshot | SHA-256 `d20fdf8278ff8b00c56c28984531836d42e8698da154e1ec36dbcb43341db6bb` |
| Run-Katalogfingerprint der unveränderten Baseline | `bc44629340a31b54b6fba7971a24ccffb57ab34772365dc2af12328d5684a149` |

Der Katalogsnapshot ist der fachliche, ID-unabhängige Endzustand aus Issue #52. Der Run-Katalogfingerprint stammt
aus `GeneratorSimulation` und umfasst die zwölf tatsächlich materialisierten Monatsprojektionen. Beide Identitäten
werden deshalb nebeneinander und nicht als austauschbare Hashes geführt.

## 2. Reproduzierbarer technischer Lauf

Der breite Lauf ist strikt manuell und standardmäßig durch Surefire ausgeschlossen:

```bash
./mvnw clean verify -Pgenerator-calibration -Dtest=CandidateGeneratorCalibrationIntegrationTest
```

Der Lauf ist vollständig vorbereitet, wurde in diesem Arbeitspaket aber **nicht ausgeführt**. Nach dem bereits
mehrstündigen 2.304-Fälle-Ausgangslauf hat der Auftraggeber am 15. August 2026 ausdrücklich angewiesen, weder diesen
Lauf noch den 9.216-Fälle-Lauf erneut auszuführen. Diese Anweisung ersetzt für den aktuellen Lieferstand die
ursprünglich verlangte Ausführung des Profilbefehls; sie erklärt dessen Gate nicht nachträglich für bestanden.

Kein PR-, Push-, Nightly- oder Scheduled-Workflow referenziert dieses Profil. Rohreports entstehen ausschließlich
unter `target/generator-calibration/`. Der normale Qualitätslauf bleibt allein:

```bash
./mvnw clean verify
```

### 2.1 Versioniertes Szenariomanifest

Das Manifest liegt in `CandidateGeneratorCalibrationIntegrationTest`. Jeder der vier Hauptrequests bleibt unter der
harten Application-Grenze von 4.096 Fällen und wird streng sequenziell an dieselbe `GeneratorSimulation`-API
übergeben. Es gibt keine zweite Proposal-, Historien-, Hard-Rule-, Ähnlichkeits- oder Statistikimplementierung.

| Partition | Kontexte pro Monat | Seeds | Fälle |
|---|---|---:|---:|
| `main-a` | leere und neutrale Historie, Recovery, Seeking Variety | `40000000..40003071` | 3.072 |
| `main-b` | belasteter Cooldown, REROLL-Hardblock, ein gematchtes Manual, zwei gemischte Manuals | `40003072..40006143` | 3.072 |
| `focus-disabled` | leere, neutrale und belastete Historie sowie REROLL; Ausschluss deaktiviert | `40100000..40101535` | 1.536 |
| `focus-required` | dieselben vier manualfreien Kontexte; Ausschluss erzwungen | `40110000..40111535` | 1.536 |
| **Summe** | zwölf Saisonmonate, INITIAL/REROLL, 0–2 Manuals, alle Ausschlussvarianten |  | **9.216** |

Zusätzlich laufen 112 synthetische Sequenzschritte mit leerer, Recovery- und Seeking-Historie. Die sichtbare
Kandidatenposition ist je Sequenz explizit festgelegt; erfolgreiche Schritte erweitern nur die flüchtige synthetische
Historie. Die kleine Partitionierungsprüfung führt dieselben vier Fälle einmal gemeinsam und einmal in zwei Requests
aus und vergleicht die vorhandenen szenariobezogenen Fingerprintaggregate.

Absichtlich dünne Pools, geordnete Fallbacks und echte Erschöpfung bleiben durch die bestehenden
`CandidateProposalEngineTest`, `CandidateReservoirEngineTest` und `CandidateSetEngineTest` abgedeckt. Technische
Fehler und Abbruch werden durch `GeneratorSimulationIntegrationTest` getrennt von fachlicher Erschöpfung geprüft.
Diese Fälle werden nicht in den regulären Repository-Erfolgssatz gemischt.

### 2.2 Technische Gates und Ausgangsmetriken

Die unveränderte Baseline wurde vor jeder Konfigurationsänderung mit dem vorhandenen
`CandidateSetBaselineIntegrationTest` erhoben. Der Lauf umfasste 2.304 geplante Fälle und endete wegen 304
fachlicher Erschöpfungen erwartungsgemäß rot; es gab weder technische Fehler noch Hard-Rule- oder Replay-Verstöße.

| Metrik | unveränderte Baseline |
|---|---:|
| Fälle / erfolgreich / erschöpft | 2.304 / 2.000 / 304 |
| technische Fehler | 0 |
| Replayprüfungen / Abweichungen | 2.000 / 0 |
| Hard-Rule-Verstöße | 0 |
| Fallback `STRICT` / `RELAXED_1` / `RELAXED_2` | 84 / 642 / 1.274 |
| Proposalversuche Mittel / P95 / Maximum | 146,366 / 150 / 156 |
| Pair-Mean Mittel / P95 / Maximum | 0,281202 / 0,297001 / 0,313723 |
| Pair-P95 Mittel / P95 / Maximum | 0,360698 / 0,386754 / 0,439106 |
| Pair-Maximum Mittel / P95 / Maximum | 0,407546 / 0,469239 / 0,579643 |
| Zufallskonzentration Top 1 / Top 10 | 1,1270 % / 9,0416 % |
| ausgewählte Ausschlüsse | 832 |

Reportdatei: `target/candidate-generator-baseline/issue-47-baseline.json`; kanonischer Reportfingerprint:
`1566b97499e99df57a76c9a293d9be0679939d3d1cf9d308cc6c8609c9fc9b89`. `target/` bleibt bewusst unversioniert.

Die Rohreports enthalten zusätzlich die vollständigen, begrenzten Reportverträge für Hard-Rejections,
Fallback-Rejections, Rollen, Profile, 2/3/4-Spezifität, Ziel-/Ist-Neuigkeitsbänder, bekannte Neuigkeitslast,
Beschaffbarkeitslast, Kandidatenkonfidenz, informative Vorfahren und Ausschlussregeln. Die committed Tabelle fasst
die entscheidenden Gates zusammen; sie ersetzt nicht die maschinenlesbaren Reports.

## 3. Ursachenanalyse und Tuningentscheidung

Die Rejection-Auswertung zeigt einen klaren strukturellen Engpass. Von den Hard-Rejections entfielen lediglich 5.295
auf `ANCHOR_ROLE_BREADTH_MISSED`; Hard-Regeln, Replay, Cooldown, REROLL und Ausschluss waren in allen erfolgreichen
Sätzen intakt. Dagegen dominierte `ANCESTOR_SET_CAP` die Fallback-Rejections mit mehreren hunderttausend Treffern je
Level (beispielsweise 807.666 / 774.610 / 451.462 in den drei Default-Levels). Die bisherigen Caps 4/5/6 passen
nicht zur Hierarchietiefe des inzwischen finalen Zwölf-Satz-Katalogs. Gesunde Proposalzahlen, Pair-Verteilungen und
Konzentration sprechen gegen einen Fehler in Proposalgewichtung, Ähnlichkeitsrechnung oder Zufallsauswahl.

Zulässige Ursachenklassen sind Datenlücke, Eignungs-/Gewichtsfehler, Hard-Rule-Fehler, Softscore-Schieflage,
Ähnlichkeitsfehler, Setquote/Fallback, Cooldown/Neuigkeitsfehler, erwartbare Seedvariation und eine bewusst verbleibende
semantische Kuratorgrenze. Eine Auffälligkeit wird erst nach dieser Einordnung zum Tuningkandidaten.

Deshalb wurde ausschließlich die vorhandene Konfiguration `ancestor-set-cap` angepasst. Ein fokussierter Vergleich
verwendete für jede Variante dieselben 48 Fälle (zwölf Monate × leere, neutrale und Recovery-Historie sowie REROLL)
über `GeneratorSimulation`; er führte keine eigene Generator-, Historien-, Hard-Rule- oder Statistiklogik ein.

| Caps `STRICT/R1/R2` | Erfolg | `STRICT/R1/R2` | Proposal-P95 | Pair-Mean | Pair-Maximum | Top 1 / Top 10 |
|---|---:|---:|---:|---:|---:|---:|
| 6/7/8 | 48/48 | 8/36/4 | 150 | 0,284881 | 0,442885 | 1,3021 % / 8,3333 % |
| 12/12/12 | 48/48 | 48/0/0 | 148 | 0,289217 | 0,503217 | 1,2587 % / 9,0712 % |
| 9/10/11 | 48/48 | 38/10/0 | 148 | 0,287217 | 0,487673 | 1,3021 % / 8,9844 % |
| **10/11/12** | **48/48** | **46/2/0** | **148** | **0,288604** | **0,503217** | **1,2587 % / 9,2014 %** |

Gewählt wurde 10/11/12: Das beseitigt im fokussierten Vergleich Erschöpfung und `RELAXED_2`, erhält aber anders als
12/12/12 eine geordnete Reserve zwischen den Levels. Generatorversion, Hard Rules, Scores, Katalog und Metadaten
bleiben unverändert; nur die Konfigurationsversion steigt von `2026-08-12.2` auf `2026-08-15.1`. Wegen der
ausdrücklichen Laufbegrenzung ist dies ein belastbarer fokussierter Befund, aber keine behauptete breite Endmetrik.

## 4. Festes manuelles Abnahmekorpus

Die acht Seeds werden nach dem technischen Baselinelauf festgeschrieben und danach nicht anhand einzelner
kulinarischer Auffälligkeiten ausgetauscht. Jeder Lauf erzeugt über `GeneratorLaboratory` genau einen Zwölfer-Satz;
die Rohdiagnose liegt nach der gezielten Korpusmethode oder dem expliziten Runner unter
`target/generator-calibration/manual-corpus/`.

| Korpus | Seed | Monat / Kontext | Eingabe | technisches Ergebnis |
|---|---:|---|---|---|
| `MC-01` | `40400001` | Januar, leere Historie | INITIAL, keine Manuals | `STRICT`, `NO_BEEF`, Fingerprint `e92a90200f87…e14e57ff` |
| `MC-02` | `40400002` | April, neutrale Historie | INITIAL, Manual „Artischocke“ gematcht | `STRICT`, `NO_PORK`, Fingerprint `382f59658287…b4d1f3cc` |
| `MC-03` | `40400003` | Juli, Recovery | INITIAL, „Speck“ gematcht + freies Manual | `STRICT`, kein Ausschluss, Fingerprint `391bd9d85472…6c91cdf3` |
| `MC-04` | `40400004` | Oktober, Seeking Variety | INITIAL, keine Manuals | `STRICT`, `NO_POULTRY`, Fingerprint `40a5d3363c0a…fa631cf4` |
| `MC-05` | `40400005` | August, belasteter Cooldown | INITIAL, keine Manuals | `STRICT`, kein Ausschluss, Fingerprint `65050737c262…5ef2e952` |
| `MC-06` | `40400006` | März, leere Historie | REROLL, vier feste blockierte Konzepte | `STRICT`, kein Ausschluss, Fingerprint `83be258c40c8…794d1b17` |
| `MC-07` | `40400007` | November, leere Historie | INITIAL, „Artischocke“ + „Speck“ gematcht | `STRICT`, kein Ausschluss, Fingerprint `694ed1f0ffa7…a91e07d8` |
| `MC-08` | `40400008` | Februar, neutrale Historie | INITIAL, freies Manual „Cook without an oven“ | `STRICT`, kein Ausschluss, Fingerprint `8c670e790f7d…92e13674` |

Die technische Materialisierung war für alle acht Fälle erfolgreich und deckt Vorschauen mit und ohne Ausschluss ab.
Das ist nur die Reproduzierbarkeits- und Vollständigkeitsprüfung; die folgende kulinarische Bewertung bleibt offen.

### 4.1 Bewertungsrubrik

Der Administrator bewertet jeden Satz mit `OK`, `auffällig` oder `nicht akzeptabel` und ergänzt bei Abweichungen
eine kurze Begründung. Die Kriterien bedeuten:

- **Lösbarkeit:** Für jeden Kandidaten ist mindestens ein plausibler gemeinsamer Lösungsweg denkbar.
- **Offenheit:** Der Kandidat schreibt nicht praktisch nur ein einzelnes Standardgericht vor.
- **Kreative Spannung:** Die Kombination erzwingt eine echte, aber nicht willkürliche Entscheidung.
- **Set-Vielfalt:** Die zwölf Kandidaten sind mehr als kosmetische Varianten.
- **Neuigkeitsbalance:** Weder Kuriositätenparade noch ausschließlich sichere Routine.
- **Beschaffbarkeit:** Für Georgia und Tobias realistisch.
- **Ausschlusswirkung:** Falls gezogen, interessant und lösbar statt bloß schikanös.
- **Datenvertrauen:** Fehlende optionale Metadaten und niedrige Konfidenz werden sichtbar, nicht als Gewissheit ausgegeben.

| Korpus | Lösbarkeit | Offenheit | Spannung | Vielfalt | Neuigkeit | Beschaffbarkeit | Ausschluss | Datenvertrauen | Notiz |
|---|---|---|---|---|---|---|---|---|---|
| `MC-01` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-02` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-03` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-04` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-05` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-06` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-07` | offen | offen | offen | offen | offen | offen | offen | offen | |
| `MC-08` | offen | offen | offen | offen | offen | offen | offen | offen | |

**Manuelles Gate:** nicht durchgeführt. Codex erklärt Diagnosen und sortiert Auffälligkeiten vor, erklärt die eigene
Ausgabe aber nicht zur bestandenen Fachabnahme.

## 5. Operativer Kataloglauf durch den Administrator

Der operative redaktionelle PostgreSQL-Katalog wird nicht durch Repository-Fixtures ersetzt. Der Lauf erfolgt
ausschließlich read-only über `/admin/generator` aus Issue #54. Vorher ist ein Backup nach dem normalen
Betriebsverfahren sinnvoll; die Simulation selbst schreibt weder Sessions, Attempts, Batches, Kandidaten noch
Challenges.

### 5.1 Acht begrenzte Adminläufe

Jeder Lauf verwendet vier Seeds über zwölf Monate und bleibt mit 48 Fällen unter dem Adapterlimit 64. Die Startseeds
sind fest und nicht Teil des manuellen Acht-Satz-Korpus.

| Lauf | Startseed | Historie / Typ | Manuals / REROLL-Block |
|---|---:|---|---|
| `OP-01` | `40410000` | `EMPTY_HISTORY`, INITIAL | keine |
| `OP-02` | `40410100` | `NEUTRAL_HISTORY`, INITIAL | keine |
| `OP-03` | `40410200` | `RECOVERY_AFTER_ADVENTUROUS`, INITIAL | keine |
| `OP-04` | `40410300` | `SEEKING_AFTER_THREE_FAMILIAR`, INITIAL | keine |
| `OP-05` | `40410400` | `LOADED_COOLDOWN_HISTORY`, INITIAL | keine |
| `OP-06` | `40410500` | `EMPTY_HISTORY`, REROLL | vier unterschiedliche Konzepte über den Picker |
| `OP-07` | `40410600` | `EMPTY_HISTORY`, INITIAL | ein gematchtes Manual |
| `OP-08` | `40410700` | `EMPTY_HISTORY`, INITIAL | ein gematchtes und ein freies Manual |

Für jeden Lauf im Formular: Startdatum am 15. Januar des Prüfjahrs, `Seedanzahl = 4`, `Monatsanzahl = 12`. Beim
REROLL sind vier aktuell vorhandene, unterschiedliche Konzepte auszuwählen. Beim gematchten Manual bleibt der
Freitext autoritativ; der Picker liefert nur die stabile Katalogzuordnung.

### 5.2 Zu protokollieren

1. Generator-, Konfigurations-, RNG-, Report- und Szenarioversion aus dem Ergebnis übernehmen.
2. `runCatalogFingerprint` und die Monatsfingerprints festhalten.
3. Für jeden Lauf Fälle, Erfolge, Erschöpfungen, technische Fehler, Fallbacks, Proposal-P95/-Maximum,
   Ausschlussquote, Konzentration und Pair-Mean/P95/Maximum eintragen.
4. Jede Auffälligkeit einer Ursachenklasse aus Abschnitt 3 zuordnen; keine operative Einzelgewichtung als
   Algorithmusfix verwenden.
5. Anschließend die acht Preview-Eingaben aus Abschnitt 4 einzeln im Labor reproduzieren und die Rubrik ausfüllen.

| Operativer Nachweis | Ergebnis |
|---|---|
| Datum / geprüfter Deployment-Commit | offen |
| operativer Run-Katalogfingerprint | offen |
| Versionen identisch zum technischen Lauf | offen |
| 8 × 48 Fälle vollständig | offen |
| technische Gates | offen |
| Administratorname / Fachabnahme | offen |

## 6. Gate-Ergebnis und verbleibende Grenzen

- Reguläres Repository-Gate: `./mvnw clean verify` am 15. August 2026 mit 219 Tests, 0 Fehlern und 0 übersprungenen
  Tests erfolgreich. Die Kalibrierungsklasse war standardmäßig ausgeschlossen.
- Fokussierte Endprüfung: 48/48 erfolgreich, 0 Erschöpfungen, 0 Hard-Rule- und Replay-Verstöße. Die gezielte
  Profilprüfung von 112 Sequenzfällen, Partitionierungsäquivalenz und festem Korpus lief mit 3/3 grünen Tests;
  das Korpus selbst war 8/8 erfolgreich.
- Breites technisches End-Gate: **nicht ausgeführt und nicht als bestanden erklärt**; 2.304- und 9.216-Fälle-Läufe
  wurden auf ausdrückliche Auftraggeberanweisung nicht wiederholt.
- Operativer Kataloglauf: **offen**.
- Manuelle fachliche Abnahme: **offen**.
- Phase 9: **nicht abgeschlossen**, solange die beiden offenen Administratornachweise fehlen.

Bewusst verbleibende Grenzen: Der Generator prüft strukturelle Plausibilität und bekannte Metadaten, nicht allgemeine
Geschmacksverträglichkeit oder Standardgerichte. Lückenhafte optionale Dimensionen senken die Konfidenz. Diese Grenze
gehört zum späteren Kurator und wird weder durch namenbasierte Sonderfälle noch durch erfundene Metadaten kaschiert.
