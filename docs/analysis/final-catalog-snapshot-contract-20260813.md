# Vertrag des finalen Katalog-Snapshots

Stand: 13. August 2026
Umsetzung: Issue #52

## Kanonischer Endzustand

Der normalisierte Endzustand liegt unter [`src/main/resources/db/catalog/final-catalog-snapshot-20260813.txt`](../../src/main/resources/db/catalog/final-catalog-snapshot-20260813.txt).

- normalisierte Fachzeilen: **6.291**
- SHA-256 einschließlich abschließendem LF: `358be33c5f4edc856d9ddcd278d3f94ec84cec1991fc57ad54d94ec41acd0756`
- aktive Konzepte: **698**
- ziehbare Konzepte: **652** (`590 SPECIFIC`, `62 OPEN`)
- direkte Konkretisierungen: **777**
- Rollen-/Flag-/Dimensionszuordnungen: **1.107 / 117 / 1.518**
- Beschaffbarkeits-/Saisonwerte: **1.384 / 588**
- Ausschlussregeln/-ziele: **22 / 56**

Die Zeilen werden tabellenübergreifend lexikografisch sortiert. Jede Zeile beginnt mit dem stabilen Tabellennamen und enthält anschließend ein JSON-Array aus stabilen Codes und fachlichen Werten. Enthalten sind Teilnehmer, Rollen, Flags, Dimensionen, Konzepte, direkte Konkretisierungen, Rollen-, Flag- und Dimensionszuordnungen, Beschaffbarkeiten, Saisonwerte sowie Ausschlussregeln und ihre Ziele.

Technische Primär- und Fremdschlüssel, Erstellungs-/Änderungszeitpunkte und Optimistic-Locking-Versionen sind ausgeschlossen. Numerische Gewichte werden mit vier Dezimalstellen normalisiert. Bestehende Konzept-IDs sind deshalb kein Bestandteil des Fingerprints, werden beim Upgrade aber unverändert erhalten.

## Zulässige Upgradequellen

[`catalog/016-final-catalog-snapshot.sql`](../../src/main/resources/db/changelog/catalog/016-final-catalog-snapshot.sql) ist append-only, nicht `runAlways` und akzeptiert genau zwei fachliche Ausgangszustände:

| Ausgangszustand | kanonischer Precondition-MD5 |
|---|---|
| unberührte Repository-Baseline auf dem nach PR #51 aktuellen `main` | `511118414a53aa9118a3212b7912a961` |
| Produktions-Fixture vom 13. August 2026 | `94be058535b8f5cc026085bfaf268173` |

Der MD5 dient ausschließlich als vollständige, in PostgreSQL ohne Erweiterung berechenbare Precondition. Der veröffentlichte Endfingerprint ist SHA-256. Ein anderer Ausgangszustand bricht vor dem ersten schreibenden Statement mit `final catalog snapshot refuses unknown starting state` ab. Nach erfolgreicher einmaliger Ausführung werden spätere redaktionelle Änderungen durch normale Liquibase-Starts nicht überschrieben.

Fresh Build, Baseline-Upgrade und Produktions-Fixture-Upgrade werden gegen dieselben normalisierten Zeilen und denselben SHA-256-Fingerprint getestet. Die Fixture wird zusätzlich vor Verwendung gegen ihre dokumentierten Gzip- und Plain-SQL-Prüfsummen geprüft.

## Dimensionsabdeckung vorher und nachher

Die Werte beziehen sich auf aktive Ziehkandidaten. Der Ausgangswert ist die freigegebene Produktions-Fixture mit 621 Ziehkandidaten; der Endwert umfasst 652 Ziehkandidaten.

| Dimension | vorher | nachher |
|---|---:|---:|
| `ACIDITY` | 78 / 621 (12,6 %) | 93 / 652 (14,3 %) |
| `BITTERNESS` | 53 / 621 (8,5 %) | 76 / 652 (11,7 %) |
| `DOMINANCE` | 349 / 621 (56,2 %) | 596 / 652 (91,4 %) |
| `FATTINESS` | 126 / 621 (20,3 %) | 150 / 652 (23,0 %) |
| `HEAT` | 53 / 621 (8,5 %) | 54 / 652 (8,3 %) |
| `SALTINESS` | nicht vorhanden | 60 / 652 (9,2 %) |
| `SWEETNESS` | 150 / 621 (24,2 %) | 201 / 652 (30,8 %) |
| `UMAMI` | 217 / 621 (34,9 %) | 256 / 652 (39,3 %) |

Alle 590 spezifischen Endkandidaten besitzen einen Dominanzwert. Bei offenen Konzepten steigt die Abdeckung von 4/60 auf 6/62; die übrigen 56 offenen Familien bleiben wegen ihrer heterogenen Spannweite bewusst unbewertet.

| Rolle | Dominanz vorher | Dominanz nachher |
|---|---:|---:|
| `ACID` | 39 / 56 | 63 / 65 |
| `ANIMAL_PROTEIN` | 70 / 153 | 131 / 148 |
| `AROMATIC` | 89 / 96 | 99 / 102 |
| `FAT` | 72 / 130 | 132 / 140 |
| `FRUIT` | 38 / 71 | 63 / 72 |
| `PLANT_PROTEIN` | 21 / 64 | 58 / 63 |
| `SEASONING` | 196 / 212 | 216 / 229 |
| `STARCH` | 45 / 93 | 89 / 95 |
| `VEGETABLE` | 41 / 125 | 115 / 130 |

Die 158 im Review genannten Mindestwerte sind exakt enthalten. Weitere Werte wurden nur für sinnvoll vergleichbare Eigenschaften gepflegt; ein fehlender Wert bleibt semantisch `nicht gepflegt`, nicht Stufe 1. Generatoralgorithmus, Hard Rules und Phase-9-Konfiguration wurden nicht verändert.

Zwei kleine Persistenztests verwendeten feste Seeds, die mit dem neuen Datenstand fachlich erschöpften. Ihre Seeds wurden auf einen unter dem finalen Snapshot erfolgreichen deterministischen Testfall aktualisiert; Assertions, Generatorregeln und Konfiguration blieben unverändert. Die Erschöpfung ist damit ausdrücklich als datenabhängige Testfixture-Änderung und nicht als Anlass für eine beiläufige Kalibrierung klassifiziert.
