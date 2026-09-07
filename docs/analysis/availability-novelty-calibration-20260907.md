# Availability-/Novelty-Kalibrierung aus #202

Stand: 7. September 2026. Dieser Bericht ist der menschliche Haltepunkt zwischen #190A und #190B.
Er empfiehlt eine Faktorvariante, übernimmt sie aber ausdrücklich nicht in die produktive Konfiguration.

## Ergebnis und Empfehlung

Für eine spätere, ausdrücklich freizugebende Übernahme wird `CAUTIOUS` empfohlen:

| Variante | `EASY` | `PLANNED` | `SPECIALTY` | `DIFFICULT` | `UNAVAILABLE` |
| --- | ---: | ---: | ---: | ---: | ---: |
| `TRANSITION` | 1,00 | 0,45 | 0,15 | 0,03 | 0,00 |
| **`CAUTIOUS`** | **1,00** | **0,30** | **0,06** | **0,01** | **0,00** |
| `STRONG` | 1,00 | 0,22 | 0,03 | 0,005 | 0,00 |

`CAUTIOUS` reduziert zufällige `PLANNED`-/`SPECIALTY`-/`DIFFICULT`-Requirements gegenüber dem
Übergangsstand von 23,72 % auf 13,89 %, ohne eine Stufe vollständig aus der Stichprobe zu verdrängen.
`STRONG` senkt nur noch auf 12,66 %, halbiert dabei aber `SPECIALTY` erneut und erzeugt kein einziges
`DIFFICULT`-Requirement. Außerdem benötigte `CAUTIOUS` nie einen Fallback, während `TRANSITION` zwei und
`STRONG` vier Sätze erst in `RELAXED_1` bilden konnten. Reservoirfüllung und Proposal-Aufwand zeigen keinen
gegenläufigen technischen Vorteil von `STRONG`.

Die aktuelle Novelty-Konfiguration bleibt als Empfehlung unverändert. Strengere Availability-Faktoren verschieben
die tatsächlichen Bänder sichtbar in Richtung `FAMILIAR`; die verkleinerte Stichprobe belegt aber keinen isolierten
Novelty-Fehlfit und rechtfertigt daher weder Ziel-Faktor-, Load- noch Cap-Änderungen. Insbesondere ist dies keine
Freigabe, die beobachtete geringe `ADVENTUROUS`-Rate über ein gekoppeltes Tuning zu kompensieren.

Entscheidungsstatus: **`HUMAN_REVIEW_REQUIRED`**. `application.yml`, `configuration-version`, Generatorversion,
RNG, Katalogdaten und produktive Novelty-Werte bleiben unverändert.

## Eingefrorene Eingaben und Fingerprints

- Ausgangscommit: `e9f0637a0c0af7720bd79c2be45e92185b70c55b`
- Generator: `1.2.0`; Konfiguration: `2026-09-03.1`; RNG: `SPLITMIX64_V1`
- Reportversion: `ISSUE_202_AVAILABILITY_NOVELTY_CALIBRATION_REPORT_V1`
- Szenarioversion: `ISSUE_202_CALIBRATION_MATRIX_V1`
- Run-Katalogfingerprint: `74fc5f93461c9c20f326a07af78a7f95f63b73538e1e07528668fb17ab599bab`
- Februar-Snapshot: `7d5bb16b35029a92ea2fae59033643b78a9a605420821723a62867ce8f3c8569`
- August-Snapshot: `250c75f6e0be47a1143c98df8f1f0b24c515d7f53cafb2f4f7eea76c4dc4b760`
- Konfiguration `TRANSITION`: `8f75ed7e9f961bd82d4d4874003fc7fe00fe9e1bca30ddb3817f88887dba9cd3`
- Konfiguration `CAUTIOUS`: `6349be167afc7c9bf304f1dff62b5fd4bb1909b863c2680164bf9f0c0f1386f7`
- Konfiguration `STRONG`: `32a8d5a229cd9d57ff3d8582169046f6c735bcdbc66090a41fa489af4d6011f8`
- kanonischer Reportfingerprint: `1cbe0eda0372f7ad20337f49a44dd42ab0c06bfe22e7069734e8d9ec2b6d69a2`

Laufzeitdaten liegen außerhalb des kanonischen Fingerprints. Der maschinenlesbare Bericht wird unter
`target/generator-simulation/availability-novelty-calibration-report.json` erzeugt und nicht eingecheckt.

## Stichprobenmatrix

Auf ausdrückliche Entscheidung des Projekteigners wurde die ursprünglich in #202 vorgesehene 4.608-Fall-Matrix
wegen ihrer für dieses private Projekt unverhältnismäßigen mehrstündigen Laufzeit verkleinert. Die Stichprobe behält
die fachlich wichtigeren Achsen vollständig und behandelt Saisonalität nur als Winter-/Sommer-Kontrast:

- Februar und August;
- `RECOVERY`, `NEUTRAL` und `SEEKING_VARIETY`;
- `INITIAL` und die generatorisch gleiche `REROLL`-Sicht;
- im Kern `AUTO` und `NONE`;
- in der Manual-Matrix ein beziehungsweise zwei unklassifizierte zulässige Manuals mit `AUTO`;
- feste Seeds `190202001` und `190202008`.

Das sind je Variante 48 Kern- und 48 Manual-Fälle, insgesamt 96; über drei Varianten wurden 288 vollständige
Generatorfälle berechnet. Die Varianten liefen unabhängig parallel, der Fallloop innerhalb jeder Variante strikt
sequenziell. Alle Varianten verwendeten dieselben vorab materialisierten Snapshots, Szenarien und Seeds. Ein
zusätzlicher identischer Einzelfall wurde je Variante zweimal ausgeführt und kanonisch verglichen.

Gemessene Variantenlaufzeiten: `TRANSITION` 688.738 ms, `CAUTIOUS` 689.902 ms, `STRONG` 689.800 ms;
Gesamtlaufzeit einschließlich PostgreSQL-Aufbau, Snapshot- und Read-only-Prüfung 698.871 ms.

## Availability-Ergebnisse

Jede Variante erzeugte 96 vollständige Sätze mit 1.152 Kandidaten und 3.744 zufälligen Requirements.

| Variante | `PLANNED` Requirements | `SPECIALTY` Requirements | `DIFFICULT` Requirements | Kandidaten mit P / S / D |
| --- | ---: | ---: | ---: | ---: |
| `TRANSITION` | 754 (20,14 %) | 114 (3,04 %) | 20 (0,53 %) | 50,52 % / 9,03 % / 1,74 % |
| `CAUTIOUS` | 464 (12,39 %) | 52 (1,39 %) | 4 (0,11 %) | 33,68 % / 4,51 % / 0,35 % |
| `STRONG` | 448 (11,97 %) | 26 (0,69 %) | 0 (0,00 %) | 32,64 % / 2,08 % / 0,00 % |

`UNAVAILABLE` erschien in keiner finalen Zufallsziehung und in keinem Kandidaten. In beiden Monatssnapshots waren
je zwei aktive, grundsätzlich ziehbare Konzepte wegen des restriktivsten gepflegten Elektoratswerts auf dem
gewichteten Nullpfad. Fehlende Availability-Werte gab es in finalen Requirements nicht.

| Variante | mittlere Availability-Last | mittlerer Availability-Score | mittlere Availability-Similarity | mittlere Proposal Attempts |
| --- | ---: | ---: | ---: | ---: |
| `TRANSITION` | 0,1412 | 90,7587 | 0,8410 | 152,23 |
| `CAUTIOUS` | 0,0989 | 94,8385 | 0,8643 | 152,88 |
| `STRONG` | 0,0983 | 95,4670 | 0,8589 | 151,65 |

Alle Reservoirs erreichten 144 von 144 Kandidaten und damit die Größenklasse `LARGE`. Die Interaktionsmessung im
Maschinenreport weist je Availability-Stufe Verteilungen von `base_draw_weight`, Saison-, Cooldown-,
Novelty-Ziel- und Effektivfaktor aus. Der Anteil zufälliger Requirements mit `PLANNED` oder schwieriger und zugleich
Novelty 4/5 sank von 2,30 % (`TRANSITION`) auf 1,28 % (`CAUTIOUS`) und 1,12 % (`STRONG`).

## Novelty-Ergebnisse

Die Novelty-Konfiguration war in allen Varianten identisch (Load-Punkte 0/1/2/4/7, Stufe-5-Cap 1,
Stufe-4/5-Cap 2, Load-Cap 11).

| Variante | N1 | N2 | N3 | N4 | N5 | tatsächliche Bänder F / B / A | mehrere N4/5 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `TRANSITION` | 45,30 % | 36,32 % | 13,19 % | 4,86 % | 0,32 % | 56,94 % / 40,97 % / 2,08 % | 0,52 % |
| `CAUTIOUS` | 49,52 % | 35,95 % | 10,04 % | 4,11 % | 0,37 % | 65,28 % / 33,16 % / 1,56 % | 0,35 % |
| `STRONG` | 51,98 % | 34,62 % | 9,08 % | 4,11 % | 0,21 % | 68,06 % / 31,25 % / 0,69 % | 0,00 % |

Die mittlere Novelty-Last betrug 2,7431 / 2,4410 / 2,2986; kein finaler Kandidat überschritt die unveränderten
Caps und die Reservoir-Metriken verzeichneten null Cap-/Load-bedingte harte Proposal-Ausschlüsse. `RECOVERY`
erzeugte in allen Varianten 0 % `ADVENTUROUS`; `SEEKING_VARIETY` erhöhte den Anteil gegenüber
`NEUTRAL` jeweils sichtbar (4,17 % gegenüber 2,08 % bei `TRANSITION`, 3,65 % gegenüber 1,04 % bei `CAUTIOUS`,
1,56 % gegenüber 0,52 % bei `STRONG`). Die N3-Anteile je tatsächlichem Band und alle Availability-/Novelty-
Kreuzhäufigkeiten stehen vollständig im Maschinenreport.

## Fallback, Integrität und Read-only-Grenze

| Variante | `STRICT` | `RELAXED_1` | `RELAXED_2` | erschöpft | technische Fehler | Hard-Rule-Verletzungen |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `TRANSITION` | 94 | 2 | 0 | 0 | 0 | 0 |
| `CAUTIOUS` | 96 | 0 | 0 | 0 | 0 | 0 |
| `STRONG` | 92 | 4 | 0 | 0 | 0 | 0 |

Cooldown-, Restriction-, Quoten-, Set-Cap-, strikte Paarmittel- und Recovery-Kadenz-Invarianten blieben ohne
Verletzung. Vor und nach dem Lauf waren alle geprüften operativen Tabellen leer und die Katalogfingerprints gleich.
Es gab keine Challenge-, Attempt-, Candidate-, Offer-, Kurator-, Historien- oder Katalogwrites und keine externen
Calls, insbesondere keine OpenAI- oder Discord-Requests.

## Grenzen und Reproduktion

Die Stichprobe kann seltene Erschöpfungen oder Seed-Ausreißer nicht statistisch ausschließen. Februar und August
liefern nur einen groben Saisonkontrast; Monatsverläufe sind nicht ableitbar. Die Manual-Fälle verwenden absichtlich
unklassifizierten Freitext und messen keine Verteilung konkreter manueller Katalogkonzepte. Deshalb ist `CAUTIOUS`
eine begründete Empfehlung für die menschliche Prüfung, keine automatische Produktionsentscheidung.

Der Lauf ist hart opt-in und bleibt außerhalb normaler Builds und CI:

```bash
./mvnw clean verify -Dtest=AvailabilityNoveltyCalibrationReportIntegrationTest -Dissue190.report=true
```

Der normale Nachweis bleibt:

```bash
./mvnw clean verify
```

Lokale Abnahme am 7. September 2026:

- Opt-in-Lauf erfolgreich: 1 Test, 288/288 Generatorfälle, 0 Fehler, 0 Überspringungen, 716,6 s Testzeit;
- regulärer Lauf erfolgreich: 496 Tests, 0 Fehler, 1 erwarteter früher Skip der Opt-in-Klasse;
- `git diff --check` ohne Befund.
