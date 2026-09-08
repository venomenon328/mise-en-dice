# Availability-/Novelty-Kalibrierung aus #202

Stand: 8. September 2026, ergänzt um die gezielte Availability-Nachmessung nach menschlicher Zwischenprüfung und
die produktive Übernahme in #190B/#203.
Der Bericht dokumentiert die abgeschlossene menschliche Entscheidung zwischen #190A und #190B sowie den
technischen Release-QA-Vertrag für den übernommenen Konfigurationsstand.

## Ergebnis und Empfehlung

Die breite Availability-Matrix empfiehlt zunächst `CAUTIOUS`:

| Variante | `EASY` | `PLANNED` | `SPECIALTY` | `DIFFICULT` | `UNAVAILABLE` |
| --- | ---: | ---: | ---: | ---: | ---: |
| `TRANSITION` | 1,00 | 0,45 | 0,15 | 0,03 | 0,00 |
| **`CAUTIOUS`** | **1,00** | **0,30** | **0,06** | **0,01** | **0,00** |
| `STRONG` | 1,00 | 0,22 | 0,03 | 0,005 | 0,00 |

`CAUTIOUS` reduziert zufällige `PLANNED`-/`SPECIALTY`-/`DIFFICULT`-Requirements gegenüber dem
Übergangsstand von 23,72 % auf 13,89 %, ohne eine Stufe vollständig aus der Stichprobe zu verdrängen.
`STRONG` senkt nur noch auf 12,66 %, halbiert dabei aber `SPECIALTY` erneut und erzeugt kein einziges
`DIFFICULT`-Requirement. Außerdem benötigte `CAUTIOUS` nie einen Fallback, während `TRANSITION` zwei und
`STRONG` vier Sätze erst in `RELAXED_1` bilden konnten.

Der Review-Befund zum Novelty-Fehlfit bestätigt sich. Die auf `CAUTIOUS` gezogenen Zielbänder werden von den
tatsächlichen Kandidatenbändern systematisch in Richtung `FAMILIAR` verfehlt. Eine kleine isolierte A/B-Messung
empfiehlt deshalb für die spätere menschliche Freigabe `TARGET_FACTOR_REBALANCED`: nur die Novelty-Zielfaktoren
werden stärker nach Bändern getrennt; Load-Punkte und Caps bleiben unverändert.

| Stufe | `FAMILIAR` | `BALANCED` | `ADVENTUROUS` |
| ---: | ---: | ---: | ---: |
| 1 | 1,25 | **0,40** | **0,05** |
| 2 | 1,10 | **0,75** | **0,15** |
| 3 | 0,70 | **1,50** | **0,80** |
| 4 | 0,15 | **1,20** | **2,00** |
| 5 | 0,00 | **0,35** | **2,00** |

Der B-Arm erhöhte den tatsächlichen `ADVENTUROUS`-Anteil gegenüber A in `NEUTRAL` von 4,17 % auf 12,50 %
und in `SEEKING_VARIETY` von 2,08 % auf 18,75 %. `RECOVERY` blieb bei 0 %. Alle A/B-Sätze blieben `STRICT`;
es gab keine Erschöpfung, technischen Fehler, Novelty-Cap-/Load-Rejections oder Hard-Rule-Verletzungen.
Die Empfehlung war wegen der kleinen Nachmessung ein menschlich zu prüfender Messvorschlag. Die menschliche
Fachabnahme ist erfolgt und hat genau diese Novelty-Matrix freigegeben.

Die danach angeforderte fokussierte Availability-Nachmessung hält genau dieses
`TARGET_FACTOR_REBALANCED` fest und variiert ausschließlich `PLANNED`. Für die menschliche
Entscheidung wurden damit präziser `PLANNED = 0,22`, `SPECIALTY = 0,06` und `DIFFICULT = 0,01` empfohlen.
Unter der rebalancierten Novelty-Matrix liegt `PLANNED = 0,22` bei 12,50 % zufälligen PLANNED-Requirements
und 40,28 % Kandidaten mit mindestens einem PLANNED-Requirement, bleibt in allen 24 Fällen `STRICT` und
hält SPECIALTY/DIFFICULT selten. `0,30` ist in genau dieser Kombination mit 17,53 % beziehungsweise 56,60 %
deutlich präsenter; `0,15` senkt PLANNED weiter, zeigte aber einen `RELAXED_1`-Satz und in der kleinen
Stichprobe höhere SPECIALTY-/DIFFICULT-Anteile. Die Nachmessung ist zu klein für eine statistische
Feinabstimmung; die menschliche Freigabe hat diese begründete Endkombination dennoch verbindlich gewählt.

## Menschliche Freigabe und produktive Übernahme

Die menschliche Fachabnahme von #202 hat die gemeinsame Endkombination freigegeben. #203 übernimmt sie als
produktiven Default für neue Generationen:

| Availability | Faktor |
|---|---:|
| `EASY` | 1,00 |
| `PLANNED` | 0,22 |
| `SPECIALTY` | 0,06 |
| `DIFFICULT` | 0,01 |
| `UNAVAILABLE` | 0,00 |

| Stufe | `FAMILIAR` | `BALANCED` | `ADVENTUROUS` |
|---:|---:|---:|---:|
| 1 | 1,25 | 0,40 | 0,05 |
| 2 | 1,10 | 0,75 | 0,15 |
| 3 | 0,70 | 1,50 | 0,80 |
| 4 | 0,15 | 1,20 | 2,00 |
| 5 | 0,00 | 0,35 | 2,00 |

`configuration-version` ist `2026-09-08.1`. Generator `1.2.0`, RNG `SPLITMIX64_V1`, Novelty-Load-Punkte
`0 / 1 / 2 / 4 / 7`, Stufe-5-Cap `1`, Stufe-4/5-Cap `2` und Load-Cap `11` bleiben unverändert. Katalogdaten,
`base_draw_weight`, Score-/Similarity-Gewichte, Profile, Cooldowns, Restriction-Wahrscheinlichkeit,
Setquoten, Reservoir-/Fallback-Regeln sowie Adapterlogik wurden nicht verändert.

Entscheidungsstatus: **`HUMAN_APPROVED_PRODUCTION_CONFIGURATION`**. Der weiterhin explizite Release-QA-Lauf
prüft bei Bedarf, dass sein fokussierter `PLANNED = 0,22`-Arm exakt dem produktiven Konfigurationssnapshot
entspricht.

## Produktiver Konfigurationssnapshot und Fingerprint

- Ausgangscommit für #190B/#203: `8b44d103e51505a941396f0d8035039cbb9aa565`
- Generator: `1.2.0`; Konfiguration: `2026-09-08.1`; RNG: `SPLITMIX64_V1`
- kanonischer Konfigurationsfingerprint:
  `c681b49f50f02299aa25cb50e209e524490cb79ecf05f0ba31f2c72e498ba288`

Der produktive Spring-Integrationstest bindet exakt diesen Snapshot und Fingerprint. Der gespeicherte
Konfigurationssnapshot bleibt damit Teil des unveränderten deterministischen Replayvertrags; Generatoralgorithmus,
RNG, Load-Punkte und Caps werden nicht neu interpretiert.

## Historische eingefrorene Eingaben und Fingerprints (#202)

- Ausgangscommit: `e9f0637a0c0af7720bd79c2be45e92185b70c55b`
- Generator: `1.2.0`; Konfiguration: `2026-09-03.1`; RNG: `SPLITMIX64_V1`
- Reportversion: `ISSUE_202_AVAILABILITY_NOVELTY_CALIBRATION_REPORT_V3`
- Availability-Szenarioversion: `ISSUE_202_CALIBRATION_MATRIX_V1`
- Novelty-A/B-Szenarioversion: `ISSUE_202_NOVELTY_AB_MATRIX_V1`
- Run-Katalogfingerprint: `74fc5f93461c9c20f326a07af78a7f95f63b73538e1e07528668fb17ab599bab`
- Februar-Snapshot: `7d5bb16b35029a92ea2fae59033643b78a9a605420821723a62867ce8f3c8569`
- August-Snapshot: `250c75f6e0be47a1143c98df8f1f0b24c515d7f53cafb2f4f7eea76c4dc4b760`
- Konfiguration `TRANSITION`: `8f75ed7e9f961bd82d4d4874003fc7fe00fe9e1bca30ddb3817f88887dba9cd3`
- Konfiguration `CAUTIOUS` / Novelty A `CURRENT`: `6349be167afc7c9bf304f1dff62b5fd4bb1909b863c2680164bf9f0c0f1386f7`
- Konfiguration Novelty B `TARGET_FACTOR_REBALANCED`: `972a7f9ae3d7dd2ba2a0068d7c491f11d6722992a378b9a07f4d262c2f616be1`
- Fokussiert `PLANNED = 0,22` bei festem `TARGET_FACTOR_REBALANCED`:
  `1cea9436a41f77840729600a4d8509e33e41dad10ad5bcc5f635e4e4e260a9f9`
- Fokussiert `PLANNED = 0,15` bei festem `TARGET_FACTOR_REBALANCED`:
  `dc9d7223e75c9f8e753baef4305c0dca29e128997e3d05b8fcb896c64abf3db3`
- Konfiguration `STRONG`: `32a8d5a229cd9d57ff3d8582169046f6c735bcdbc66090a41fa489af4d6011f8`
- kanonischer Reportfingerprint: `bb1f3583d17a76b87c114c2992b3b0419973016a430a4202d55265aa05926a2a`

Laufzeitdaten liegen außerhalb des kanonischen Fingerprints. Der maschinenlesbare Bericht wird unter
`target/generator-simulation/availability-novelty-calibration-report.json` erzeugt und nicht eingecheckt.

## Messaufbau

Auf ausdrückliche Entscheidung des Projekteigners wurde die ursprünglich in #202 vorgesehene 4.608-Fall-Matrix
wegen ihrer für dieses private Projekt unverhältnismäßigen mehrstündigen Laufzeit verkleinert. Die akzeptierte
Availability-Stichprobe behält die fachlich wichtigeren Achsen vollständig und behandelt Saisonalität nur als
Winter-/Sommer-Kontrast:

- Februar und August;
- `RECOVERY`, `NEUTRAL` und `SEEKING_VARIETY`;
- `INITIAL` und die generatorisch gleiche `REROLL`-Sicht;
- im Kern `AUTO` und `NONE`;
- in der Manual-Matrix ein beziehungsweise zwei unklassifizierte zulässige Manuals mit `AUTO`;
- feste Seeds `190202001` und `190202008`.

Das sind je Availability-Variante 48 Kern- und 48 Manual-Fälle, insgesamt 96; über drei Varianten wurden 288
vollständige Generatorfälle berechnet. Die Varianten liefen unabhängig parallel, der Fallloop innerhalb jeder
Variante strikt sequenziell. Alle verwendeten dieselben vorab materialisierten Snapshots, Szenarien und Seeds.

Die Novelty-Nachmessung kehrt ausdrücklich nicht zu dieser Matrix oder zur ursprünglich geplanten Großmatrix zurück.
Sie vergleicht auf `CAUTIOUS` nur A `CURRENT` und B `TARGET_FACTOR_REBALANCED` über Februar/August, alle drei
Kadenzen, `INITIAL`, `NONE` und dieselben zwei Seeds: 12 Fälle je Arm, 24 insgesamt. `NONE` isoliert die
Novelty-Wirkung von Restriktionszufall; REROLL und Manuals werden nicht erneut vermessen. Ein zusätzlicher identischer
Einzelfall wurde je Availability- und Novelty-Variante zweimal ausgeführt und kanonisch verglichen.

Die nachfolgende Availability-Nachmessung ist ebenfalls absichtlich klein und verändert weder die breite Matrix
noch die Novelty-A/B-Messung. Sie verwendet fest `TARGET_FACTOR_REBALANCED`, Februar/August, alle drei
Kadenzen, `AUTO` und `NONE`, beide kanonischen Seeds sowie ausschließlich `INITIAL`:

```text
2 Monate × 3 Kadenzen × 2 Restriktionsmodi × 2 Seeds × INITIAL = 24 Fälle je Arm
```

Die drei Arme unterscheiden sich ausschließlich bei `PLANNED = 0,30 / 0,22 / 0,15`; `SPECIALTY = 0,06`,
`DIFFICULT = 0,01`, EASY/UNAVAILABLE, Novelty-Zielfaktoren, Load-Punkte, Caps und alle übrigen
Generatorregler sind identisch. Der Test erzwingt diese Begrenzung zusätzlich per Konfigurationsvergleich.
Eine identische INITIAL/AUTO-Determinismusprobe wurde je Arm doppelt ausgeführt.

Gemessene Laufzeiten: `TRANSITION` 533.590 ms, `CAUTIOUS` 537.475 ms, `STRONG` 538.916 ms; Novelty A 40.032 ms,
Novelty B 40.067 ms; fokussiert `PLANNED = 0,30 / 0,22 / 0,15`: 73.114 / 81.287 / 74.859 ms.
Gesamtlaufzeit einschließlich PostgreSQL-Aufbau, Snapshots und Read-only-Prüfung: 678.456 ms.

## Availability-Ergebnisse

Jede Availability-Variante erzeugte 96 vollständige Sätze mit 1.152 Kandidaten und 3.744 zufälligen Requirements.

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

## Target-/Actual-Bandbefund der Availability-Matrix

`targetBandFrequency` bezeichnet das an jedem ausgewählten Kandidaten gespeicherte gezogene Zielband; `Actual`
ist dessen aus Requirements und Load-Punkten berechnetes tatsächliches Band. Die konfigurierten Zwölfer-Baselines
sind vor einer möglichen Kontextprojektion `5/7/0` für `RECOVERY`, `3/7/2` für `NEUTRAL` und `2/7/3` für
`SEEKING_VARIETY`. Folgende Gegenüberstellung verwendet direkt die bereits im Maschinenreport erhobenen Target-
und Actual-Werte:

| Variante | Kadenz | Target F / B / A | Actual F / B / A |
| --- | --- | ---: | ---: |
| `TRANSITION` | `RECOVERY` | 45,83 % / 54,17 % / 0,00 % | 66,15 % / 33,85 % / 0,00 % |
| `TRANSITION` | `NEUTRAL` | 29,17 % / 56,25 % / 14,58 % | 55,21 % / 42,71 % / 2,08 % |
| `TRANSITION` | `SEEKING_VARIETY` | 23,44 % / 49,48 % / 27,08 % | 49,48 % / 46,35 % / 4,17 % |
| `CAUTIOUS` | `RECOVERY` | 58,85 % / 41,15 % / 0,00 % | 78,65 % / 21,35 % / 0,00 % |
| `CAUTIOUS` | `NEUTRAL` | 36,46 % / 51,04 % / 12,50 % | 61,98 % / 36,98 % / 1,04 % |
| `CAUTIOUS` | `SEEKING_VARIETY` | 25,52 % / 51,56 % / 22,92 % | 55,21 % / 41,15 % / 3,65 % |
| `STRONG` | `RECOVERY` | 49,48 % / 50,52 % / 0,00 % | 75,00 % / 25,00 % / 0,00 % |
| `STRONG` | `NEUTRAL` | 33,33 % / 50,52 % / 16,15 % | 66,15 % / 33,33 % / 0,52 % |
| `STRONG` | `SEEKING_VARIETY` | 29,17 % / 51,56 % / 19,27 % | 63,02 % / 35,42 % / 1,56 % |

Insgesamt liegt `CAUTIOUS` bei Target 40,28 % / 47,92 % / 11,81 %, tatsächlich aber bei
65,28 % / 33,16 % / 1,56 %. Das ist kein Stichprobenargument gegen eine Nachmessung, sondern der bestätigte
systematische Fehlfit: Das gezogene Band steuert die tatsächliche Kandidatenlast zu schwach.

## Gezielte Novelty-A/B-Nachmessung auf `CAUTIOUS`

Beide Arme verwendeten unveränderte Load-Punkte `0/1/2/4/7`, Stufe-5-Cap `1`, Stufe-4/5-Cap `2` und Load-Cap
`11`. Nur B ersetzte die oben dokumentierten Zielfaktoren. Die Target-/Actual-Gegenüberstellung zeigt:

| Arm | Kadenz | Target F / B / A | Actual F / B / A |
| --- | --- | ---: | ---: |
| A `CURRENT` | `RECOVERY` | 52,08 % / 47,92 % / 0,00 % | 68,75 % / 31,25 % / 0,00 % |
| A `CURRENT` | `NEUTRAL` | 31,25 % / 52,08 % / 16,67 % | 45,83 % / 50,00 % / 4,17 % |
| A `CURRENT` | `SEEKING_VARIETY` | 22,92 % / 50,00 % / 27,08 % | 45,83 % / 52,08 % / 2,08 % |
| B `TARGET_FACTOR_REBALANCED` | `RECOVERY` | 39,58 % / 60,42 % / 0,00 % | 47,92 % / 52,08 % / 0,00 % |
| B `TARGET_FACTOR_REBALANCED` | `NEUTRAL` | 14,58 % / 54,17 % / 31,25 % | 27,08 % / 60,42 % / 12,50 % |
| B `TARGET_FACTOR_REBALANCED` | `SEEKING_VARIETY` | 12,50 % / 62,50 % / 25,00 % | 25,00 % / 56,25 % / 18,75 % |

Über alle drei Kadenzen verbesserte B die tatsächlichen Bänder von 53,47 % / 44,44 % / 2,08 % auf
33,33 % / 56,25 % / 10,42 %. Gegen seine gezogenen Zielbänder reduzierte sich der absolute Gap bei
`FAMILIAR` von 18,06 auf 11,11 Prozentpunkte, bei `BALANCED` von 5,56 auf 2,78 und bei `ADVENTUROUS` von
12,50 auf 8,33 Prozentpunkte. Der verbleibende Gap ist sichtbar und wird nicht als perfekte Kalibrierung ausgegeben.

Die Requirement-Verteilung verschob sich von N1/N2/N3/N4/N5 = 45,49 % / 40,45 % / 10,59 % / 3,13 % /
0,35 % auf 34,55 % / 40,28 % / 17,88 % / 6,94 % / 0,35 %. Die mittlere Kandidatenlast stieg von 3,0625
auf 4,2500. Kandidaten mit mehreren N4/5-Requirements stiegen kontrolliert von 0,69 % auf 4,17 %; Requirements
mit Novelty 4/5 und zugleich `PLANNED` oder schwieriger von 0,87 % auf 2,43 %. Kein Kandidat überschritt Caps
oder Load-Grenze. Damit erhöht B insbesondere kontextgebundene N3- und ungewöhnliche N4-Verwendungen, ohne N5
in dieser Stichprobe häufiger zu machen.

Die kleine A/B-Matrix kann seltene Seed- oder Manual-Interaktionen nicht ausschließen. Ihr Ergebnis ist dennoch
richtungskonsistent in `NEUTRAL` und `SEEKING_VARIETY`, erhält die Recovery-Hardrule und zeigt keinen technischen
Trade-off: beide Arme benötigten im Mittel rund 145 Proposal Attempts, füllten alle Reservoirs vollständig und
erzeugten ausschließlich `STRICT`-Sätze. Deshalb wird B konkret für die menschliche Entscheidung empfohlen;
eine weitere große Matrix ist für #202 nicht erforderlich.

## Gezielte Availability-Nachmessung bei festem `TARGET_FACTOR_REBALANCED`

Diese Nachmessung beantwortet ausschließlich die nach der Zwischenprüfung offene Frage nach `PLANNED` in der
bereits empfohlenen Kombination mit `TARGET_FACTOR_REBALANCED`. Sie misst 24 vollständige Sätze je Arm,
also je 288 Kandidaten und 1.152 zufällige Requirements. `SPECIALTY = 0,06` und `DIFFICULT = 0,01` sind
konfigurationsseitig exakt gleich geblieben; die folgenden beobachteten Unterschiede sind daher Verteilungen
der kleinen festen Matrix, keine stillen Faktoränderungen.

| `PLANNED` | Requirements P / S / D | Kandidaten mit P / S / D | hohe Novelty (N4/5) × P / S / D | Fallback |
| ---: | ---: | ---: | ---: | ---: |
| 0,30 | 202 (17,53 %) / 21 (1,82 %) / 3 (0,26 %) | 163 (56,60 %) / 21 (7,29 %) / 3 (1,04 %) | 27 / 0 / 0; keine N5-Fälle | 24 `STRICT` |
| **0,22** | **144 (12,50 %) / 23 (2,00 %) / 1 (0,09 %)** | **116 (40,28 %) / 21 (7,29 %) / 1 (0,35 %)** | **19 / 0 / 1; keine N5-Fälle** | **24 `STRICT`** |
| 0,15 | 120 (10,42 %) / 36 (3,12 %) / 5 (0,43 %) | 100 (34,72 %) / 35 (12,15 %) / 5 (1,74 %) | 13 / 3 / 2; keine N5-Fälle | 23 `STRICT`, 1 `RELAXED_1` |

### Target-/Actual-Bänder der fokussierten Arme

Auch die fokussierten Läufe erheben die vollständig gezogenen Target- und tatsächlichen Novelty-Bänder pro
Kadenz. Jede Zelle umfasst 96 Kandidaten (zwei Monate × `AUTO`/`NONE` × zwei Seeds × zwölf Kandidaten);
`TARGET_FACTOR_REBALANCED`, Load-Punkte und Caps bleiben zwischen allen drei Armen fest. `Target` ist das
gezogene Band, `Actual` das aus Requirements und Load-Punkten resultierende Band.

| `PLANNED` | Kadenz | Target F / B / A | Actual F / B / A |
| ---: | --- | ---: | ---: |
| 0,30 | `RECOVERY` | 31,25 % / 68,75 % / 0,00 % | 44,79 % / 55,21 % / 0,00 % |
| 0,30 | `NEUTRAL` | 17,71 % / 55,21 % / 27,08 % | 29,17 % / 58,33 % / 12,50 % |
| 0,30 | `SEEKING_VARIETY` | 15,62 % / 58,33 % / 26,04 % | 25,00 % / 58,33 % / 16,67 % |
| **0,22** | `RECOVERY` | 32,29 % / 67,71 % / 0,00 % | 52,08 % / 47,92 % / 0,00 % |
| **0,22** | `NEUTRAL` | 15,62 % / 63,54 % / 20,83 % | **32,29 % / 57,29 % / 10,42 %** |
| **0,22** | `SEEKING_VARIETY` | 14,58 % / 42,71 % / 42,71 % | **29,17 % / 54,17 % / 16,67 %** |
| 0,15 | `RECOVERY` | 36,46 % / 63,54 % / 0,00 % | 47,92 % / 52,08 % / 0,00 % |
| 0,15 | `NEUTRAL` | 14,58 % / 64,58 % / 20,83 % | 32,29 % / 58,33 % / 9,38 % |
| 0,15 | `SEEKING_VARIETY` | 14,58 % / 38,54 % / 46,88 % | 23,96 % / 56,25 % / 19,79 % |

Damit wird das gewünschte `TARGET_FACTOR_REBALANCED`-Profil in allen fokussierten Armen erhalten: `RECOVERY`
bleibt ohne `ADVENTUROUS`; `NEUTRAL` und `SEEKING_VARIETY` behalten jeweils einen klaren tatsächlichen
`ADVENTUROUS`-Anteil. Für den empfohlenen `0,22`-Arm sind das 10,42 % in `NEUTRAL` und 16,67 % in
`SEEKING_VARIETY`. Die frühere isolierte Novelty-B-Messung ergab 12,50 % beziehungsweise 18,75 %; sie war
jedoch `NONE`-only, während die fokussierte Matrix zusätzlich `AUTO` enthält. Die Differenzen von jeweils
2,08 Prozentpunkten sind deshalb kein kausaler Nachweis für eine Verschlechterung durch `PLANNED = 0,22`,
sondern ein transparenter Vergleich zweier unterschiedlicher kleiner Matrizen. Gegen den CURRENT-A-Arm
bleibt die Rebalanced-Wirkung klar: dort lagen die Werte bei 4,17 % und 2,08 %.

Alle 72 fokussierten Fälle füllten das Reservoir vollständig in Größe `LARGE`; es gab weder Erschöpfungen,
technische Fehler noch Hard-Rule-Verletzungen. Der mittlere Proposal-Aufwand lag bei 145,00 (`0,30`), 144,83
(`0,22`) und 145,63 (`0,15`). Die direkte Kreuztabelle im Maschinenreport enthält zusätzlich die vollständige
Availability-×-Novelty-Verteilung; die Tabelle zeigt die risikorelevanten N4/5-Zellen explizit. Das einmalige
DIFFICULT/N4-Ereignis bei `0,22` und die höheren SPECIALTY-/DIFFICULT-Anteile bei `0,15` sind bei diesem kleinen
N keine belastbare Rangfolge der seltenen Stufen, machen aber sichtbar, dass die feste Matrix diese Interaktion
nicht verdeckt. Für die Entscheidung spricht `0,22` gegen beide Ziele: Gegenüber `0,30` reduziert es PLANNED
deutlich und erreicht nahezu den PLANNED-Requirement-Anteil der breiten CAUTIOUS-Matrix; zugleich erhält es
das Rebalanced-Profil mit 10,42 % beziehungsweise 16,67 % `ADVENTUROUS` in den beiden progressiveren Kadenzen.
`0,15` erreicht in `SEEKING_VARIETY` zwar 19,79 %, fällt aber in `NEUTRAL` auf 9,38 % und benötigt als einziger
Arm einen `RELAXED_1`-Fallback. Es gibt daher keinen neuen Befund, der eine vierte Faktorvariante oder eine
Änderung an der bereits empfohlenen Novelty-Matrix, ihren Load-Punkten oder Caps rechtfertigt.

## Fallback, Integrität und Read-only-Grenze

| Messbereich | Variante | `STRICT` | `RELAXED_1` | `RELAXED_2` | erschöpft | technische Fehler | Hard-Rule-Verletzungen |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Availability | `TRANSITION` | 94 | 2 | 0 | 0 | 0 | 0 |
| Availability | `CAUTIOUS` | 96 | 0 | 0 | 0 | 0 | 0 |
| Availability | `STRONG` | 92 | 4 | 0 | 0 | 0 | 0 |
| Novelty A/B | `CURRENT` | 12 | 0 | 0 | 0 | 0 | 0 |
| Novelty A/B | `TARGET_FACTOR_REBALANCED` | 12 | 0 | 0 | 0 | 0 | 0 |
| Fokussiert Availability | `PLANNED = 0,30` | 24 | 0 | 0 | 0 | 0 | 0 |
| Fokussiert Availability | `PLANNED = 0,22` | 24 | 0 | 0 | 0 | 0 | 0 |
| Fokussiert Availability | `PLANNED = 0,15` | 23 | 1 | 0 | 0 | 0 | 0 |

Cooldown-, Restriction-, Quoten-, Set-Cap-, strikte Paarmittel- und Recovery-Kadenz-Invarianten blieben ohne
Verletzung. Vor und nach dem Lauf waren alle geprüften operativen Tabellen leer und die Katalogfingerprints gleich.
Es gab keine Challenge-, Attempt-, Candidate-, Offer-, Kurator-, Historien- oder Katalogwrites und keine externen
Calls, insbesondere keine OpenAI- oder Discord-Requests.

## Grenzen und Reproduktion

Die Availability-Stichprobe kann seltene Erschöpfungen oder Seed-Ausreißer nicht statistisch ausschließen. Februar
und August liefern nur einen groben Saisonkontrast; Monatsverläufe sind nicht ableitbar. Die Manual-Fälle verwenden
absichtlich unklassifizierten Freitext und messen keine Verteilung konkreter manueller Katalogkonzepte. Die Novelty-
A/B-Nachmessung ist noch enger und isoliert absichtlich nur Zielfaktoren. Die fokussierte Availability-Nachmessung
variiert nur PLANNED und deckt weder REROLL noch Manuals erneut ab. Deshalb waren `PLANNED = 0,22`,
`SPECIALTY = 0,06`, `DIFFICULT = 0,01` und `TARGET_FACTOR_REBALANCED` zum Messzeitpunkt begründete
Empfehlungen für die menschliche Prüfung, keine automatische Freigabe. Die nachfolgende menschliche Fachabnahme
hat diese Freigabe erteilt.

Der Lauf ist hart opt-in und bleibt außerhalb normaler Builds und CI:

```bash
./mvnw clean verify -Dtest=AvailabilityNoveltyCalibrationReportIntegrationTest -Dissue190.report=true
```

Der normale Nachweis bleibt:

```bash
./mvnw clean verify
```

Historischer Mess- und Abnahmenachweis vom 8. September 2026:

- Opt-in-Lauf erfolgreich: 1 Test, 384/384 dokumentierte Generatorfälle, 0 Fehler, 0 Überspringungen,
  698,7 s Testzeit; davon 288 Availability-, 24 Novelty-A/B- und 72 fokussierte Availability-Fälle;
- regulärer Lauf erfolgreich: 496 Tests, 0 Fehler, 1 erwartete Überspringung der Opt-in-Klasse;
- `git diff --check`: ohne Befund.

Die produktive Übernahme führt keine neue Kalibrierungsentscheidung ein. Der Opt-in-Matrixlauf bleibt deshalb
hart opt-in und dient weiter als reproduzierbarer Messnachweis; der aktuelle Produktionssnapshot wird zusätzlich
durch den regulären Integrations- und Replaytestpfad abgesichert.

Aktuelle Release-Verifikation für #190B/#203 am 8. September 2026:

- `./mvnw clean verify` erfolgreich: 496 Tests, 0 Fehler, 0 Errors, 1 erwartete Überspringung der
  hart opt-in geschützten Reportklasse;
- der Produktionssnapshot einschließlich seiner Availability- und Novelty-Faktoren, Konfigurationsversion und
  kanonischem Fingerprint wird durch den Spring-Integrationstest festgeschrieben.
