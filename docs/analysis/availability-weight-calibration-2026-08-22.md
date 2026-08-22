# Availability-Gewichtung: Kalibrierungsbericht

Stand: 22. August 2026
Issue: #152

## Änderung

| restriktivster gepflegter Wert | bisher `2026-08-15.1` | neu `2026-08-22.1` |
|---|---:|---:|
| `EASY` | 1,00 | 1,00 |
| `PLANNED` | 0,65 | 0,45 |
| `DIFFICULT` | 0,20 | 0,03 |
| `UNAVAILABLE` | 0,00 | 0,00 |
| kein gepflegter Wert | neutral 1,00 | neutral 1,00 |

Generator `1.2.0`, Neuigkeitsziele, Caps, Scores, Reservoirgrößen, Set-Targets und Kuratorregeln sind in beiden Varianten identisch. Die frühere Variante entsteht im Test nur durch Austausch der Konfigurationsversion und dieser vier Faktoren.

## Reproduzierbarer Messaufbau

Der PostgreSQL-/Testcontainers-Lauf nutzt den Katalog mit 698 Konzepten und vier feste, identische
Fälle je Variante: `152000001`/Februar/`NEUTRAL_HISTORY`,
`152000002`/Mai/`RECOVERY_AFTER_ADVENTUROUS`,
`152000003`/August/`SEEKING_AFTER_THREE_FAMILIAR` und
`152000004`/November/`NEUTRAL_HISTORY`. Alle verwenden `AUTO`-Restriktionen. Nach dem Vergleich
wird ein einzelner neuer Fall mit identischem Seed erneut ausgeführt und sein kanonischer Report
exakt verglichen. Die kleine Matrix ist absichtlich ein Kalibrierungssmoke, keine breite Testmatrix;
sie läuft nur auf ausdrückliche Anforderung, nicht im normalen `clean verify`.

Standardbefehl:

```bash
./mvnw clean verify -Dtest=AvailabilityWeightCalibrationReportIntegrationTest -Dissue152.report=true
```

Er schreibt die vollständige kanonische Maschinenauswertung nach
`target/generator-simulation/availability-weight-calibration-report.json`. Der Lauf ist read-only:
er erzeugt keine Challenge-, Offer-, Kurator-, Discord- oder Historienwrites.

## Ergebnis

Anteile beziehen sich auf 192 zufällige Requirements beziehungsweise 48 Kandidaten je Variante.

| Messwert | bisher | neu |
|---|---:|---:|
| zufällige Requirements `EASY` | 82,29 % (158) | 85,94 % (165) |
| zufällige Requirements `PLANNED` | 16,67 % (32) | 13,02 % (25) |
| zufällige Requirements `DIFFICULT` | 1,04 % (2) | 1,04 % (2) |
| Kandidaten mit mindestens einem `PLANNED` | 52,08 % (25) | 45,83 % (22) |
| Kandidaten mit mindestens einem `DIFFICULT` | 4,17 % (2) | 4,17 % (2) |
| `PLANNED` zusammen mit Neuigkeit 4/5 | 0,00 % (0) | 0,52 % (1) |
| `DIFFICULT` zusammen mit Neuigkeit 4/5 | 0,52 % (1) | 1,04 % (2) |
| tatsächliches Band `FAMILIAR` | 47,92 % (23) | 43,75 % (21) |
| tatsächliches Band `BALANCED` | 52,08 % (25) | 52,08 % (25) |
| tatsächliches Band `ADVENTUROUS` | 0,00 % (0) | 4,17 % (2) |
| `STRICT` | 100,00 % (4) | 75,00 % (3) |
| `RELAXED_1` | 0,00 % (0) | 25,00 % (1) |
| `RELAXED_2` | 0,00 % (0) | 0,00 % (0) |
| Erschöpfungen / technische Fehler / Hard-Rule-Verletzungen | 0 / 0 / 0 | 0 / 0 / 0 |
| Reservoir: `LARGE`, mittlere Füllrate | 4/4, 100 % | 4/4, 100 % |

Die vier festen Fälle reduzieren `PLANNED`, ohne Erschöpfung oder technische Fehler zu erzeugen.
`DIFFICULT` blieb mit zwei Requirements absolut gleich; die gemeinsame
`DIFFICULT`-/Neuigkeit-4/5-Häufigkeit stieg in dieser absichtlich kleinen Matrix von eins auf zwei.
Das ist ein dokumentierter, wegen des kleinen N nicht abschließend bewertbarer Auffälligkeitsbefund.
Es werden in diesem Paket ausdrücklich keine weiteren Regler verändert. Falls er bei der fachlichen
Abnahme als problematisch gilt, ist ein gezieltes Folgeissue für eine breitere Offline-Kalibrierung
erforderlich. Der PR bleibt bis zur fachlichen Abnahme des Berichts ein Draft.

Die Katalogfingerprints beider Varianten sind identisch:
`a571b00fef1a9a2b0a5e51b8dfb19bc80d4ebf95f68c6e61ccf7ef0fd98f7721`.
Die Konfigurationsfingerprints unterscheiden sich erwartungsgemäß: vorher
`2a7c831f712c92c08f463dffff8fa6776a8f624d40ac8ebf2412af198d5f3675`, nachher
`984c5bb6fd5d8cbc312abe767dd255b2c8f2e1400183ddcfbf8ccabf7e5b69b2`.
