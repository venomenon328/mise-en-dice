# Kalibrierung und Abnahme des Kandidatengenerators

Stand: 15. August 2026
Status: technischer Stand vorbereitet und fokussiert geprüft; operativer Kataloglauf bestanden; breite Endmessung und manuelle Fachabnahme offen

Dieses Dokument ist das Abnahmeprotokoll für Issue #40. Es trennt den reproduzierbaren Repository-Lauf, den
read-only operativen Kataloglauf und die ausschließlich durch den Administrator vorzunehmende kulinarische
Bewertung. Ein grünes technisches oder operatives Gate ersetzt die verbleibende manuelle Fachabnahme nicht.

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

Der operative redaktionelle PostgreSQL-Katalog wird nicht durch Repository-Fixtures ersetzt. Der Lauf erfolgte
read-only über `/admin/generator` aus Issue #54 auf einer Preview-Instanz mit einer Kopie des operativen
PostgreSQL-Datenbestands. Die Simulation schrieb weder Sessions, Attempts, Batches, Kandidaten noch Challenges.

### 5.1 Acht begrenzte Adminläufe

Die Abnahme verwendete den Deployment-Commit `59b3e330cad6c0116a5ff8672c5e5eab0dcc8d42`, Generator `1.0.0`,
Konfiguration `2026-08-15.1`, RNG `SPLITMIX64_V1`, Report `2026-08-13.1` und Szenario
`ADMIN_GENERATOR_SIMULATION_V1`.

Eine erste betriebliche Probe mit der vorherigen 30-Sekunden-Grenze endete nach 8 von 48 Fällen als `TIMED_OUT`;
dieser partielle Report wurde nicht als Gate-Nachweis gewertet. Die Admin-Deadline wurde daraufhin auf fünf Minuten
erhöht. OP-01 bis OP-07 liefen danach jeweils als vollständiger 48-Fälle-Request.

OP-08 mit zwei Manuals benötigte auf dem Preview-VPS für 48 Fälle mehr als fünf Minuten und endete nach 26 Fällen
als `TIMED_OUT`. Auch dieser partielle Report wurde nicht als Nachweis gewertet. Weil alle Monate unabhängige
Single-Step-Szenarien sind, wurde exakt dieselbe Menge von 48 Seed-/Monatskombinationen anschließend deterministisch
in vier vollständige 12-Fälle-Requests für Januar–März, April–Juni, Juli–September und Oktober–Dezember partitioniert.
Die Deadline und das Falllimit wurden dafür nicht weiter aufgeweicht.

| Lauf | Startseed | Historie / Typ | Manuals / REROLL-Block |
|---|---:|---|---|
| `OP-01` | `40410000` | `EMPTY_HISTORY`, INITIAL | keine |
| `OP-02` | `40410100` | `NEUTRAL_HISTORY`, INITIAL | keine |
| `OP-03` | `40410200` | `RECOVERY_AFTER_ADVENTUROUS`, INITIAL | keine |
| `OP-04` | `40410300` | `SEEKING_AFTER_THREE_FAMILIAR`, INITIAL | keine |
| `OP-05` | `40410400` | `LOADED_COOLDOWN_HISTORY`, INITIAL | keine |
| `OP-06` | `40410500` | `EMPTY_HISTORY`, REROLL | `ARTICHOKE`, `ASPARAGUS`, `BACON`, `BAMBOO_SHOOTS` |
| `OP-07` | `40410600` | `EMPTY_HISTORY`, INITIAL | Manual 1: „Artischocke“, gematcht auf `ARTICHOKE` |
| `OP-08` | `40410700` | `EMPTY_HISTORY`, INITIAL | Manual 1: „Speck“, gematcht auf `BACON`; Manual 2: „Use a waffle iron“, frei |

Die Katalog-IDs wurden gegen den geprüften Datenbestand auf stabile Codes aufgelöst. Für OP-06 waren insbesondere
`ARTICHOKE` = ID 83 und `BAMBOO_SHOOTS` = ID 82 im geprüften Snapshot vorhanden. Die während der Abnahme gefundenen
Picker-/UX-Probleme sind separat als Issues #60 und #61 erfasst und verändern die Generatorergebnisse nicht.

### 5.2 Operatives Ergebnis

Über alle acht Szenarien wurden **384/384 geplante Fälle erfolgreich verarbeitet**. Es gab **0 fachliche
Erschöpfungen, 0 technische Fehler, 0 Replay-/Integritätsabweichungen, 0 Hard-Rule-Verletzungen, 0
Cooldown-Verletzungen und 0 REROLL-Verletzungen**. Die Fallbacksumme beträgt `STRICT/R1/R2 = 371/13/0`.

| Lauf | Fälle | `STRICT/R1/R2` | Proposal Ø | Pair Mean / P95 / Max | Top 1 / Top 10 | kanonischer Report |
|---|---:|---:|---:|---|---|---|
| `OP-01` | 48/48 | 47/1/0 | 145,8542 | 0,288374 / 0,366967 / 0,406643 | 1,39 % / 8,90 % | `4ddc98abd2fb3c50dafd36ffe3b0d06dba13b3b70ce2fe5be42bc369a6110555` |
| `OP-02` | 48/48 | 46/2/0 | 145,9375 | 0,285773 / 0,362784 / 0,409104 | 1,30 % / 8,46 % | `f34097fee8e875175242d402c6ccd7d1b643c2cd077ad783e1283c9ff7a73af1` |
| `OP-03` | 48/48 | 46/2/0 | 145,7292 | 0,290351 / 0,364778 / 0,400639 | 0,91 % / 7,81 % | `fe3b6a92e0059b9a9a59cfaf789bd7a5780b90f62f18eb922fbc7f8e829e456c` |
| `OP-04` | 48/48 | 45/3/0 | 145,7083 | 0,287328 / 0,365524 / 0,404276 | 1,43 % / 8,72 % | `4fee7393047bd72bb4105a8f54fa4363532cb0674c0eeb1afcbdcfa29aef536` |
| `OP-05` | 48/48 | 47/1/0 | 145,8750 | 0,286874 / 0,363011 / 0,404112 | 1,43 % / 9,24 % | `8bfad2f08b84b94724f49ad052e7c5dabe161201d267b3b829cb14e1236c7391` |
| `OP-06` | 48/48 | 44/4/0 | 145,7708 | 0,287005 / 0,365871 / 0,407781 | 1,30 % / 8,94 % | `9c7e2672797edf7147a830e3527c2d40bc02ce5cdede934d41d7062cbb3ab390` |
| `OP-07` | 48/48 | 48/0/0 | 149,0625 | 0,286323 / 0,361027 / 0,406079 | 0,98 % / 7,70 % | `5331c6d12b100337d0222e82cf0690628dd9f18d1b705aa6c4e93b0632448456` |
| `OP-08A` Jan–Mär | 12/12 | 12/0/0 | 165,5000 | 0,288057 / 0,364699 / 0,435843 | 2,08 % / 15,28 % | `89937cb3b4d66a2a8886af0cc78350fc82411ff5da9bfcddab0c40ee4e1b54db` |
| `OP-08B` Apr–Jun | 12/12 | 12/0/0 | 166,8333 | 0,291309 / 0,372470 / 0,417469 | 2,43 % / 17,01 % | `b837ae104d8e24a7b0687c53b1e51b4cb3a97a6170a1728fcaef4f957dcf9c7c` |
| `OP-08C` Jul–Sep | 12/12 | 12/0/0 | 167,2500 | 0,292404 / 0,373617 / 0,410857 | 1,39 % / 12,50 % | `1b624f1634895b2bf54d4e2679d35c391d28673db2c2e2636bd878a0465f5712` |
| `OP-08D` Okt–Dez | 12/12 | 12/0/0 | 167,2500 | 0,293872 / 0,374159 / 0,434685 | 2,08 % / 16,32 % | `5f891ffbe549087138a307032f379407067998afed9b0b162c968a73e8319d50` |

Der Adminreport stellt für Proposalversuche aktuell nur den Mittelwert dar; die im ursprünglichen Protokoll
vorgesehenen Proposal-P95-/Maximumwerte konnten deshalb im operativen UI-Lauf nicht zusätzlich protokolliert werden.
Die technische/fokussierte Kalibrierung deckt diese Quantile weiterhin ab. Dieser Darstellungsunterschied wird nicht
als Generatorfehler gewertet.

Für OP-01 bis OP-07 war der vollständige Zwölf-Monats-Run-Katalogfingerprint identisch:
`063b73f4420d672137373777602515e6be90118dc2f826526adc6b3abf88b01b`. Die vier OP-08-Teilruns besitzen wegen ihrer
jeweils drei materialisierten Monate erwartungsgemäß eigene Run-Katalogfingerprints:

- OP-08A: `41b94b3db86a849b6e50cc36a21f3ab603c7c1db6e4f3e1b0e9850c20c5f3a64`
- OP-08B: `837ae870743612a9cca04efa8c681e5dc1d1e81f570ceb4273d3c7db46e38852`
- OP-08C: `9c4833f854ecd80fd0e92ce8d3269845b108549c303f13876f04cfa2847a46b4`
- OP-08D: `10c21668c675590c395089778a13bb3262200f33045698f2053cd0501c90b92f`

Die Monatsfingerprints waren über alle Läufe für denselben Monat identisch:

| Monat | Katalogfingerprint |
|---:|---|
| 1 | `05d36287efcdf70b296a99cf7297f2902fc1d35175159775f61045236f60932d` |
| 2 | `55ac5eeaa83d06522187c6227a97921191261267fd1e5e8c35045c6560f1a5d7` |
| 3 | `f0e60cac3e60bf8d59c46f869ed71dce9914fb4761f39871e4a54d9f79bc98ea` |
| 4 | `b2f020b47d74b168a08d91049df2b1c638fe9060dd2723b5952edf3322429f72` |
| 5 | `388aa22188cf39587bf2d18ccb71c23eed587375e93fe377c28e0f0631ede67f` |
| 6 | `16d3e9ef0e6291ceae5efb10b39a4aa13e2419933505babea978c4bde7d2b433` |
| 7 | `6b22fa6f71a19838271633e7660ccd129579ab0288333da2df9819220e479f4f` |
| 8 | `8d95ce655677c9f867e4d1d64933307b5e86c9eddcca737d645cf3892045f8f2` |
| 9 | `5d3426bab8ea0a884080ad4df85c32bfd43488d8abc1053eedf536c1c43041f3` |
| 10 | `767a003e6187f86aa87955330b1bbe6e339167f193d0196ecd53a951c5f46b09` |
| 11 | `9ae173e6b00367cca26eb60f83313cafb291cb3ed5c3b72414a7115603493164` |
| 12 | `c098fe105e6495052d7eb9ff751800610c81e244a71c708aab1cc4756a4c0773` |

| Operativer Nachweis | Ergebnis |
|---|---|
| Datum / geprüfter Deployment-Commit | 15. August 2026 / `59b3e330cad6c0116a5ff8672c5e5eab0dcc8d42` |
| operativer Katalog | vollständiger Monatsnachweis; OP-01 bis OP-07 Run-Fingerprint `063b73f4…8b01b` |
| Versionen identisch zum technischen Tuningstand | ja |
| acht Szenarien / 384 Fälle vollständig | ja; OP-08 nach verworfener Timeout-Probe deterministisch 4 × 12 partitioniert |
| technische Gates | bestanden; sämtliche Integritätszähler 0, keine Erschöpfung, `STRICT/R1/R2 = 371/13/0` |
| manuelle Fachabnahme | offen |

**Operatives Gate: bestanden.** Der reale redaktionelle Katalog zeigt auf dem getunten Generator keine technische
oder fachliche Integritätsauffälligkeit. Die höhere Suchlast im Zwei-Manual-Szenario ist reproduzierbar sichtbar,
führt in den vollständigen Teilruns aber weder zu Fallback noch Erschöpfung.

## 6. Gate-Ergebnis und verbleibende Grenzen

- Reguläres Repository-Gate: `Verify` und `Deployment Verify` waren auf dem für die operative Abnahme verwendeten
  Code-Head `59b3e330cad6c0116a5ff8672c5e5eab0dcc8d42` erfolgreich. Die Kalibrierungsklasse bleibt standardmäßig
  ausgeschlossen.
- Fokussierte Endprüfung: 48/48 erfolgreich, 0 Erschöpfungen, 0 Hard-Rule- und Replay-Verstöße. Die gezielte
  Profilprüfung von 112 Sequenzfällen, Partitionierungsäquivalenz und festem Korpus lief erfolgreich; das Korpus
  selbst war technisch 8/8 erfolgreich.
- Breites technisches End-Gate: **nicht ausgeführt und nicht als bestanden erklärt**; die ausdrücklich ausgeschlossenen
  breiten Wiederholungsläufe bleiben unbestätigt.
- Operativer Kataloglauf: **bestanden**, 384/384 erfolgreiche Fälle über alle acht Szenarien.
- Manuelle fachliche Abnahme: **offen**.
- Phase 9: **nicht abgeschlossen**, solange die manuelle Administrator-Fachabnahme des festen Korpus fehlt.

Bewusst verbleibende Grenzen: Der Generator prüft strukturelle Plausibilität und bekannte Metadaten, nicht allgemeine
Geschmacksverträglichkeit oder Standardgerichte. Lückenhafte optionale Dimensionen senken die Konfidenz. Diese Grenze
gehört zum späteren Kurator und wird weder durch namenbasierte Sonderfälle noch durch erfundene Metadaten kaschiert.
