# Vertrag des finalen Katalog-Snapshots

Stand: 13. August 2026
Umsetzung: Issue #52

## Kanonischer Endzustand

Der normalisierte Endzustand liegt unter [`src/main/resources/db/catalog/final-catalog-snapshot-20260813.txt`](../../src/main/resources/db/catalog/final-catalog-snapshot-20260813.txt).

- normalisierte Fachzeilen: **6.296**
- SHA-256 einschließlich abschließendem LF: `d20fdf8278ff8b00c56c28984531836d42e8698da154e1ec36dbcb43341db6bb`
- aktive Konzepte: **698**
- ziehbare Konzepte: **651** (`589 SPECIFIC`, `62 OPEN`)
- direkte Konkretisierungen: **780**
- Rollen-/Flag-/Dimensionszuordnungen: **1.107 / 117 / 1.518**
- Beschaffbarkeits-/Saisonwerte: **1.384 / 588**
- Ausschlussregeln/-ziele: **22 / 58**

Die Zeilen werden tabellenübergreifend lexikografisch sortiert. Jede Zeile beginnt mit dem stabilen Tabellennamen und enthält anschließend ein JSON-Array aus stabilen Codes und fachlichen Werten. Enthalten sind Teilnehmer, Rollen, Flags, Dimensionen, Konzepte, direkte Konkretisierungen, Rollen-, Flag- und Dimensionszuordnungen, Beschaffbarkeiten, Saisonwerte sowie Ausschlussregeln und ihre Ziele.

Technische Primär- und Fremdschlüssel, Erstellungs-/Änderungszeitpunkte und Optimistic-Locking-Versionen sind ausgeschlossen. Numerische Gewichte werden mit vier Dezimalstellen normalisiert. Bestehende Konzept-IDs sind deshalb kein Bestandteil des Fingerprints, werden beim Upgrade aber unverändert erhalten.

## Zulässige Upgradequellen

[`catalog/016-final-catalog-snapshot.sql`](../../src/main/resources/db/changelog/catalog/016-final-catalog-snapshot.sql) ist append-only, nicht `runAlways` und akzeptiert genau zwei fachliche Ausgangszustände:

| Ausgangszustand | kanonischer Precondition-MD5 |
|---|---|
| unberührte Repository-Baseline auf dem nach PR #51 aktuellen `main` | `f90ba3058230969f5cda13cb93f227c2` |
| Produktions-Fixture vom 13. August 2026 | `759d87bdee666f18e94b787eb4b99217` |

Die Zeilen werden dabei explizit mit der PostgreSQL-Kollation `C` sortiert. Damit
bleiben beide Eingangs-Fingerprints zwischen den Debian-basierten Testcontainern
und dem Alpine-basierten Deployment-Image identisch.

Der MD5 dient ausschließlich als vollständige, in PostgreSQL ohne Erweiterung berechenbare Precondition. Der veröffentlichte Endfingerprint ist SHA-256. Ein anderer Ausgangszustand bricht vor dem ersten schreibenden Statement mit `final catalog snapshot refuses unknown starting state` ab. Nach erfolgreicher einmaliger Ausführung werden spätere redaktionelle Änderungen durch normale Liquibase-Starts nicht überschrieben.

Fresh Build, Baseline-Upgrade und Produktions-Fixture-Upgrade werden gegen dieselben normalisierten Zeilen und denselben SHA-256-Fingerprint getestet. Die Fixture wird zusätzlich vor Verwendung gegen ihre dokumentierten Gzip- und Plain-SQL-Prüfsummen geprüft.

## Dimensionsabdeckung vorher und nachher

Die Werte beziehen sich auf aktive Ziehkandidaten. Der Ausgangswert ist die freigegebene Produktions-Fixture mit 621 Ziehkandidaten; der Endwert umfasst 651 Ziehkandidaten.

| Dimension | vorher | nachher |
|---|---:|---:|
| `ACIDITY` | 78 / 621 (12,6 %) | 93 / 651 (14,3 %) |
| `BITTERNESS` | 53 / 621 (8,5 %) | 75 / 651 (11,5 %) |
| `DOMINANCE` | 349 / 621 (56,2 %) | 595 / 651 (91,4 %) |
| `FATTINESS` | 126 / 621 (20,3 %) | 150 / 651 (23,0 %) |
| `HEAT` | 53 / 621 (8,5 %) | 54 / 651 (8,3 %) |
| `SALTINESS` | nicht vorhanden | 60 / 651 (9,2 %) |
| `SWEETNESS` | 150 / 621 (24,2 %) | 201 / 651 (30,9 %) |
| `UMAMI` | 217 / 621 (34,9 %) | 255 / 651 (39,2 %) |

Alle 589 spezifischen Endkandidaten besitzen einen Dominanzwert. Bei offenen Konzepten steigt die Abdeckung von 4/60 auf 6/62; die übrigen 56 offenen Familien bleiben wegen ihrer heterogenen Spannweite bewusst unbewertet.

| Rolle | Dominanz vorher | Dominanz nachher |
|---|---:|---:|
| `ACID` | 39 / 56 | 63 / 65 |
| `ANIMAL_PROTEIN` | 70 / 153 | 131 / 148 |
| `AROMATIC` | 89 / 96 | 98 / 101 |
| `FAT` | 72 / 130 | 132 / 140 |
| `FRUIT` | 38 / 71 | 63 / 72 |
| `PLANT_PROTEIN` | 21 / 64 | 58 / 63 |
| `SEASONING` | 196 / 212 | 215 / 228 |
| `STARCH` | 45 / 93 | 89 / 95 |
| `VEGETABLE` | 41 / 125 | 115 / 130 |

Die 158 im Review genannten Mindestwerte sind exakt enthalten. Weitere Werte wurden nur für sinnvoll vergleichbare Eigenschaften gepflegt; ein fehlender Wert bleibt semantisch `nicht gepflegt`, nicht Stufe 1. Generatoralgorithmus, Hard Rules und Phase-9-Konfiguration wurden nicht verändert.

Zwei kleine Persistenztests verwenden für ihren vollständigen `Generated`-Batch einen deterministischen Testadapter statt der zufallsbasierten Satzselektion. Damit prüfen sie weiterhin strikt ihre Persistenz-, Konkurrenz-, Restart- beziehungsweise Datenbank-Unique-Semantik, ohne von zufälligen Katalog-Seeds abzuhängen. Assertions, Generatorregeln und Konfiguration blieben unverändert. Tests der tatsächlichen Generatorerschöpfung und Fallback-Semantik bleiben davon getrennt und unverändert.
