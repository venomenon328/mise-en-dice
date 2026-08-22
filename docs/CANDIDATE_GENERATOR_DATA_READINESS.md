# Datenreife für den Kandidatengenerator

Stand: 13. August 2026  
Aktualitätshinweis: 22. August 2026  
Status: historische Phase-9-Datenreife-Baseline; weiterhin Referenz für Messwerte und Kalibrierungsvergleiche, aber keine aktuelle Fachautorität für die Beschaffbarkeitssemantik

Bezugsstand: finaler Katalog-Snapshot aus Issue #52, SHA-256 `d20fdf8278ff8b00c56c28984531836d42e8698da154e1ec36dbcb43341db6bb`

Dieses Dokument hält die Messung des über Liquibase aufgebauten Repository-Baseline-Katalogs für Phase 9 fest. Die Abfragen liegen reproduzierbar unter [`analysis/candidate-generator-data-readiness.sql`](analysis/candidate-generator-data-readiness.sql).

Die Messung wurde gegen echtes PostgreSQL 17.6 mit Testcontainers und dem vollständigen Liquibase-Changelog durchgeführt. Sie verändert keine Katalogdaten.

> **Gültigkeit seit Issue #150:** Die unten dokumentierten Katalogzahlen und Verteilungen bleiben als historische Baseline gültig und werden insbesondere für die Kalibrierung in Issue #152 weiterverwendet. Die damalige Forderung nach vollständiger Beschaffbarkeit aller aktiven Teilnehmer ist jedoch **keine aktuelle Generatorregel mehr**. Für neue Sessions sind [`PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md`](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md) und [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md) autoritativ: Nur das feste Session-Elektorat ist relevant, nur tatsächlich gepflegte Werte werden berücksichtigt, fehlende Werte sind neutral und der restriktivste vorhandene Wert gewinnt. Dieses Dokument ist eine Mess- und Vergleichsreferenz, kein Runtime- oder Testfixture-Vertrag.

## 1. Historische Gate-Definition für Phase 9

### 1.1 Damals hart erforderliche Daten

Vor Beginn von Phase 9B musste der aktive Ziehpool nach dem damaligen Stand erfüllen:

| Merkmal | damaliges Gate |
|---|---:|
| funktionale Rolle | 100 % der aktiven Ziehkandidaten |
| Beschaffbarkeit | 100 % für alle aktiven Teilnehmer |
| Neuigkeitsstufe | 100 % der aktiven Ziehkandidaten |
| Spezifität und positives Basisgewicht | 100 % durch bestehende Constraints und Katalogtests |
| Poolgröße | mindestens 48 spezifische und 48 offene Kandidaten |
| Rollenpool | mindestens 48 Kandidaten je Referenzrolle |
| Graphabdeckung | mindestens 95 % der Ziehkandidaten verbunden; fehlende Kante allein ist kein Ausschlussgrund |

Historisch machten `UNAVAILABLE` oder fehlende Beschaffbarkeit ein einzelnes Konzept zufällig ungeeignet. Die hier gemessene Baseline enthielt im aktiven Ziehpool keinen solchen Fall. **Seit Issue #150 gilt abweichend:** Ein fehlender Beschaffbarkeitswert ist neutral; nur ein tatsächlich vorhandenes `UNAVAILABLE` eines Mitglieds des festen Session-Elektorats blockiert.

### 1.2 Optionale Daten

Kulinarische Dimensionen sind für Generatorversion 1 optional. Sie dürfen nur:

- einen niedrig gewichteten Softscore beeinflussen,
- Paarähnlichkeit ergänzen, wenn beidseitig vergleichbare Werte vorliegen,
- die Datenkonfidenz erhöhen oder bei Fehlen senken.

Sie dürfen aufgrund der gemessenen Abdeckung nicht:

- harte Kandidatenregeln auslösen,
- fehlende Werte als Stufe 1 interpretieren,
- flächendeckende Zutatenkompatibilität vortäuschen.

Binäre Flags besitzen keine Abdeckungsquote: Eine fehlende Zuordnung bedeutet, dass das Flag nicht gesetzt ist.

## 2. Ziehpool

| Kennzahl | Wert |
|---|---:|
| aktive Ziehkandidaten | 651 |
| `SPECIFIC` | 589 |
| `OPEN` | 62 |
| mit Neuigkeitsstufe | 651 |
| ohne Neuigkeitsstufe | 0 |

Beide Spezifitätsklassen sind groß genug, um 2/3/4-Spezifitätsmixe und ein Reservoir deutlich oberhalb von zwölf Kandidaten zu unterstützen.

## 3. Rollen

Alle 651 Ziehkandidaten besitzen mindestens eine funktionale Rolle.

| Rolle | gesamt | spezifisch | offen |
|---|---:|---:|---:|
| `ACID` | 65 | 63 | 2 |
| `ANIMAL_PROTEIN` | 148 | 130 | 18 |
| `AROMATIC` | 101 | 94 | 7 |
| `FAT` | 140 | 131 | 9 |
| `FRUIT` | 72 | 63 | 9 |
| `PLANT_PROTEIN` | 63 | 58 | 5 |
| `SEASONING` | 228 | 209 | 19 |
| `STARCH` | 95 | 89 | 6 |
| `VEGETABLE` | 130 | 114 | 16 |

Die kleinste Rolle ist `PLANT_PROTEIN` mit 63 Kandidaten und überschreitet das Gate von 48. Profilslots verlangen nicht gleichzeitig eine offene und eine bestimmte Rolle; die teilweise kleinen offenen Teilpools einzelner Rollen sind daher kein Blocker.

Die häufigsten reinen beziehungsweise kombinierten Rollensignaturen sind unter anderem:

| Signatur | gesamt |
|---|---:|
| `ANIMAL_PROTEIN` | 74 |
| `VEGETABLE` | 72 |
| `AROMATIC+SEASONING` | 54 |
| `STARCH` | 54 |
| `ANIMAL_PROTEIN+FAT` | 47 |
| `SEASONING` | 51 |
| `FRUIT` | 37 |
| `ANIMAL_PROTEIN+FAT+SEASONING` | 23 |
| `AROMATIC` | 22 |
| `ACID+SEASONING` | 25 |
| `PLANT_PROTEIN+STARCH` | 19 |

497 Konzepte besitzen mindestens eine der breit als strukturell oder unterstützend gemessenen Rollen einschließlich `FAT`; 154 sind ausschließlich geschmacksgebend. Die verbindliche Generatorspezifikation verwendet für ihre härtere Ankerdefinition kein Fett als alleinige Ankerrolle und verhindert dadurch, dass mehrere reine Fett-/Würzkomponenten eine Struktur nur auf dem Papier erfüllen.

## 4. Beschaffbarkeit – historische Georgia-/Tobias-Baseline

Für 651 Ziehkandidaten und die beiden damaligen Referenzteilnehmer existieren vollständig 1.302 Zuordnungen. Es fehlt in dieser Baseline keine Zeile. Diese Vollständigkeit ist seit Issue #150 **kein allgemeines Datenreife-Gate** mehr; die Verteilung bleibt jedoch als Vergleichsbasis für Generator- und Kalibrierungsanalysen nützlich.

### 4.1 Georgia

| Stufe | Konzepte |
|---|---:|
| `EASY` | 464 |
| `PLANNED` | 170 |
| `DIFFICULT` | 17 |
| `UNAVAILABLE` | 0 |

### 4.2 Tobias

| Stufe | Konzepte |
|---|---:|
| `EASY` | 436 |
| `PLANNED` | 145 |
| `DIFFICULT` | 70 |
| `UNAVAILABLE` | 0 |

### 4.3 Gemeinsame schlechteste Stufe

| schlechteste Stufe beider Teilnehmer | Konzepte |
|---|---:|
| `EASY` | 436 |
| `PLANNED` | 145 |
| `DIFFICULT` | 70 |

Der damalige Pool war vollständig bewertet. Die 70 gemeinsam mindestens schwierigen Konzepte sind groß genug, um nicht als Einzelfälle behandelt zu werden, und bilden gerade deshalb eine wichtige Ausgangsbasis für die stärkere Gewichtungsdämpfung aus Issue #152.

## 5. Neuigkeit

Die Neuigkeitsstufe ist vollständig gepflegt, auch je Rolle.

| Stufe | gesamt | spezifisch | offen |
|---:|---:|---:|---:|
| 1 | 281 | 245 | 36 |
| 2 | 194 | 174 | 20 |
| 3 | 96 | 91 | 5 |
| 4 | 58 | 57 | 1 |
| 5 | 22 | 22 | 0 |

80 Konzepte liegen auf Stufe 4 oder 5. Außergewöhnlichkeit ist damit kein theoretisches Randmerkmal, sondern ein ausreichend großer Teilpool. Mit `Bagoong` besitzt nun auch eine offene Vorgabe Neuigkeitsstufe 4; die übrigen offenen Vorgaben liegen auf Stufe 1 bis 3.

Das unterstützt die vorgesehene Kadenz:

- vertraute und ausgewogene Kandidaten besitzen breite Pools,
- abenteuerliche Kandidaten bleiben möglich,
- Stufe 4/5 muss innerhalb eines Kandidaten, im Zwölfer-Satz und über sichtbare Wochen begrenzt werden.

## 6. Kulinarische Dimensionen

| Dimension | gepflegt | fehlend | Abdeckung |
|---|---:|---:|---:|
| `ACIDITY` | 93 | 558 | 14,3 % |
| `BITTERNESS` | 75 | 576 | 11,5 % |
| `DOMINANCE` | 595 | 56 | 91,4 % |
| `FATTINESS` | 150 | 501 | 23,0 % |
| `HEAT` | 54 | 597 | 8,3 % |
| `SALTINESS` | 60 | 591 | 9,2 % |
| `SWEETNESS` | 201 | 450 | 30,9 % |
| `UMAMI` | 255 | 396 | 39,2 % |

`DOMINANCE` ist für alle 589 spezifischen Ziehkandidaten und für sechs belastbar bewertbare offene Konzepte gepflegt. Die 56 fehlenden Werte gehören ausschließlich zu heterogenen offenen Familien. Die Rollenabdeckung lautet:

| Rolle | Dominanz bekannt | gesamt |
|---|---:|---:|
| `ACID` | 63 | 65 |
| `ANIMAL_PROTEIN` | 131 | 148 |
| `AROMATIC` | 98 | 101 |
| `FAT` | 132 | 140 |
| `FRUIT` | 63 | 72 |
| `PLANT_PROTEIN` | 58 | 63 |
| `SEASONING` | 215 | 228 |
| `STARCH` | 89 | 95 |
| `VEGETABLE` | 115 | 130 |

Der Backfill beseitigt die frühere Schieflage zwischen Würzkomponenten und strukturellen Zutaten deutlich. Fehlende Werte werden trotzdem weiterhin nicht als Stufe 1 interpretiert; die Generator-Hard-Rules bleiben durch Issue #52 unverändert.

Verteilung der bekannten Dominanzwerte:

| Stufe | Konzepte |
|---:|---:|
| 1 | 23 |
| 2 | 112 |
| 3 | 220 |
| 4 | 154 |
| 5 | 86 |
| unbekannt | 56 |

**Entscheidung:** Dimensionen bleiben für Generatorversion 1 trotz der deutlich besseren Datenlage optional und niedrig gewichtet. Issue #52 ändert keine Generatoralgorithmen oder Hard Rules. Auffällige verbleibende Lücken können später aus Simulation und Generator-Labor als gezielte redaktionelle Aufgaben entstehen.

## 7. Kulinarische Flags

| Flag | Konzepte |
|---|---:|
| `CURED` | 14 |
| `DRIED` | 43 |
| `FERMENTED` | 45 |
| `PICKLED` | 8 |
| `SMOKED` | 6 |

Flags eignen sich für Diagnose, bekannte Intensitätshäufung und Ähnlichkeit. Ihre kleinen Teilmengen rechtfertigen keine Pflichtquote und keine Behauptung allgemeiner Inkompatibilität.

## 8. Konkretisierungsgraph

| Kennzahl | Wert |
|---|---:|
| direkte Kanten | 780 |
| maximale transitive Tiefe | 4 |
| Konzepte mit mehreren direkten Eltern | 103 |
| aktive Wurzeln | 26 |
| aktive Blätter | 554 |
| verbundene Ziehkandidaten | 650 von 651 |
| isolierte Ziehkandidaten | 1 von 651 |
| offene Ziehkandidaten mit direktem Kind | 62 von 62 |

Der Graph ist ausreichend tief und besitzt reale Mehrfach-Eltern-Strukturen. Er trägt:

- Parent-/Child-Hardregeln,
- informative Vorfahrenähnlichkeit,
- Ausschlussziel-Expansion.

Die einzelne isolierte Vorgabe `Kaffee` ist kein Blocker. Ein Konzept benötigt nicht zwingend eine Hierarchiekante, um eigenständig eine gültige Challenge-Vorgabe zu sein. Für semantische Ähnlichkeit steht bei diesen Konzepten lediglich weniger Information zur Verfügung.

## 9. Historisches Gate-Ergebnis

**Ergebnis zum Phase-9-Bezugsstand: bestanden mit deutlich verbreiterter, weiterhin optionaler Dimensionsnutzung.**

Vor Phase 9B war kein zusätzliches Katalog- oder Metadatenpaket erforderlich, weil:

- Rollen, Beschaffbarkeit und Neuigkeit in der damaligen Baseline vollständig waren,
- spezifische und offene Pools deutlich über den Mindestgrößen lagen,
- alle Referenzrollen ausreichend besetzt waren,
- der Graph Redundanz und semantische Nähe zuverlässig genug unterstützte.

Die Aussage zur vollständigen Beschaffbarkeit beschreibt den damaligen Datenbestand und ist keine aktuelle Pflicht. Die verbleibenden Dimensionslücken werden weiterhin nicht durch erfundene Defaults kaschiert. Sie begrenzen bewusst den Anspruch des Generators:

- Struktur und Neuigkeitsbalance sind belastbar prüfbar.
- bekannte Eigenschaftshäufungen sind als schwaches Softsignal nutzbar.
- echte paarweise Zutatenkompatibilität bleibt beim späteren Kurator.

## 10. Operativer Katalog und heutige Verwendung

Diese Messung beschreibt den reproduzierbaren Repository-Baseline-Katalog vom 13. August 2026. Die laufende PostgreSQL-Datenbank ist nach Einführung der Webverwaltung die redaktionelle Quelle der Wahrheit und kann abweichen.

Für neue Entwicklung gilt deshalb:

- aktuelle Generator-Fachregeln werden aus [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md) und den jeweils neueren, ausdrücklich überschreibenden Spezifikationen abgeleitet,
- Rollen, Neuigkeitsstufe, Spezifität und positives Gewicht behalten ihre dokumentierten harten Voraussetzungen,
- fehlende Beschaffbarkeit ist seit Issue #150 neutral und kein Datenreife- oder Erschöpfungsgrund,
- ein vorhandenes `UNAVAILABLE` im festen Session-Elektorat bleibt ein harter Ausschlussgrund für zufällige Ziehung,
- das Generator-Labor und die Simulation dürfen die historischen Verteilungen dieses Dokuments als Vergleichsbasis verwenden,
- Issue #152 verwendet diese Baseline ausdrücklich für den reproduzierbaren Vorher-/Nachher-Vergleich der Beschaffbarkeitsgewichtung.

Die SQL-Auswertung bleibt deshalb erhalten. Ihre historischen Georgia-/Tobias-Vollständigkeitszahlen sind Messdaten, keine implizite Aufforderung, für jeden künftig bekannten Teilnehmer eine vollständige Beschaffbarkeitsmatrix zu pflegen.
