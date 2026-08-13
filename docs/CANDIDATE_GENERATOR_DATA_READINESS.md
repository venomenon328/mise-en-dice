# Datenreife für den Kandidatengenerator

Stand: 13. August 2026
Bezugsstand: finaler Katalog-Snapshot aus Issue #52, SHA-256 `26c62af11e8b5c41bd93e29960799d2602b322d551afa8d0e1c68d81615e1a52`

Dieses Dokument hält die Messung des über Liquibase aufgebauten Repository-Baseline-Katalogs für Phase 9 fest. Die Abfragen liegen reproduzierbar unter [`analysis/candidate-generator-data-readiness.sql`](analysis/candidate-generator-data-readiness.sql).

Die Messung wurde gegen echtes PostgreSQL 17.6 mit Testcontainers und dem vollständigen Liquibase-Changelog durchgeführt. Sie verändert keine Katalogdaten.

## 1. Gate-Definition

### 1.1 Hart erforderliche Daten

Vor Beginn von Phase 9B muss der aktive Ziehpool erfüllen:

| Merkmal | Gate |
|---|---:|
| funktionale Rolle | 100 % der aktiven Ziehkandidaten |
| Beschaffbarkeit | 100 % für alle aktiven Teilnehmer |
| Neuigkeitsstufe | 100 % der aktiven Ziehkandidaten |
| Spezifität und positives Basisgewicht | 100 % durch bestehende Constraints und Katalogtests |
| Poolgröße | mindestens 48 spezifische und 48 offene Kandidaten |
| Rollenpool | mindestens 48 Kandidaten je Referenzrolle |
| Graphabdeckung | mindestens 95 % der Ziehkandidaten verbunden; fehlende Kante allein ist kein Ausschlussgrund |

`UNAVAILABLE` oder fehlende Beschaffbarkeit machen ein einzelnes Konzept zufällig ungeeignet. Die hier gemessene Baseline enthält im aktiven Ziehpool keinen solchen Fall.

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
| aktive Ziehkandidaten | 652 |
| `SPECIFIC` | 590 |
| `OPEN` | 62 |
| mit Neuigkeitsstufe | 652 |
| ohne Neuigkeitsstufe | 0 |

Beide Spezifitätsklassen sind groß genug, um 2/3/4-Spezifitätsmixe und ein Reservoir deutlich oberhalb von zwölf Kandidaten zu unterstützen.

## 3. Rollen

Alle 652 Ziehkandidaten besitzen mindestens eine funktionale Rolle.

| Rolle | gesamt | spezifisch | offen |
|---|---:|---:|---:|
| `ACID` | 65 | 63 | 2 |
| `ANIMAL_PROTEIN` | 148 | 130 | 18 |
| `AROMATIC` | 102 | 95 | 7 |
| `FAT` | 140 | 131 | 9 |
| `FRUIT` | 72 | 63 | 9 |
| `PLANT_PROTEIN` | 63 | 58 | 5 |
| `SEASONING` | 229 | 210 | 19 |
| `STARCH` | 95 | 89 | 6 |
| `VEGETABLE` | 130 | 114 | 16 |

Die kleinste Rolle ist `PLANT_PROTEIN` mit 63 Kandidaten und überschreitet das Gate von 48. Profilslots verlangen nicht gleichzeitig eine offene und eine bestimmte Rolle; die teilweise kleinen offenen Teilpools einzelner Rollen sind daher kein Blocker.

Die häufigsten reinen beziehungsweise kombinierten Rollensignaturen sind unter anderem:

| Signatur | gesamt |
|---|---:|
| `ANIMAL_PROTEIN` | 74 |
| `VEGETABLE` | 72 |
| `AROMATIC+SEASONING` | 55 |
| `STARCH` | 54 |
| `ANIMAL_PROTEIN+FAT` | 47 |
| `SEASONING` | 51 |
| `FRUIT` | 37 |
| `ANIMAL_PROTEIN+FAT+SEASONING` | 23 |
| `AROMATIC` | 22 |
| `ACID+SEASONING` | 25 |
| `PLANT_PROTEIN+STARCH` | 19 |

497 Konzepte besitzen mindestens eine der breit als strukturell oder unterstützend gemessenen Rollen einschließlich `FAT`; 155 sind ausschließlich geschmacksgebend. Die verbindliche Generatorspezifikation verwendet für ihre härtere Ankerdefinition kein Fett als alleinige Ankerrolle und verhindert dadurch, dass mehrere reine Fett-/Würzkomponenten eine Struktur nur auf dem Papier erfüllen.

## 4. Beschaffbarkeit

Für 652 Ziehkandidaten und zwei aktive Teilnehmer existieren vollständig 1.304 Zuordnungen. Es fehlt keine Zeile.

### 4.1 Georgia

| Stufe | Konzepte |
|---|---:|
| `EASY` | 465 |
| `PLANNED` | 170 |
| `DIFFICULT` | 17 |
| `UNAVAILABLE` | 0 |

### 4.2 Tobias

| Stufe | Konzepte |
|---|---:|
| `EASY` | 437 |
| `PLANNED` | 145 |
| `DIFFICULT` | 70 |
| `UNAVAILABLE` | 0 |

### 4.3 Gemeinsame schlechteste Stufe

| schlechteste Stufe beider Teilnehmer | Konzepte |
|---|---:|
| `EASY` | 437 |
| `PLANNED` | 145 |
| `DIFFICULT` | 70 |

Der Pool ist vollständig nutzbar. Die 70 gemeinsam mindestens schwierigen Konzepte sind groß genug, um nicht als Einzelfälle behandelt zu werden, müssen aber über Gewichtung und Satzcaps dosiert werden.

## 5. Neuigkeit

Die Neuigkeitsstufe ist vollständig gepflegt, auch je Rolle.

| Stufe | gesamt | spezifisch | offen |
|---:|---:|---:|---:|
| 1 | 282 | 246 | 36 |
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
| `ACIDITY` | 93 | 559 | 14,3 % |
| `BITTERNESS` | 76 | 576 | 11,7 % |
| `DOMINANCE` | 596 | 56 | 91,4 % |
| `FATTINESS` | 150 | 502 | 23,0 % |
| `HEAT` | 54 | 598 | 8,3 % |
| `SALTINESS` | 60 | 592 | 9,2 % |
| `SWEETNESS` | 201 | 451 | 30,8 % |
| `UMAMI` | 256 | 396 | 39,3 % |

`DOMINANCE` ist für alle 590 spezifischen Ziehkandidaten und für sechs belastbar bewertbare offene Konzepte gepflegt. Die 56 fehlenden Werte gehören ausschließlich zu heterogenen offenen Familien. Die Rollenabdeckung lautet:

| Rolle | Dominanz bekannt | gesamt |
|---|---:|---:|
| `ACID` | 63 | 65 |
| `ANIMAL_PROTEIN` | 131 | 148 |
| `AROMATIC` | 99 | 102 |
| `FAT` | 132 | 140 |
| `FRUIT` | 63 | 72 |
| `PLANT_PROTEIN` | 58 | 63 |
| `SEASONING` | 216 | 229 |
| `STARCH` | 89 | 95 |
| `VEGETABLE` | 115 | 130 |

Der Backfill beseitigt die frühere Schieflage zwischen Würzkomponenten und strukturellen Zutaten deutlich. Fehlende Werte werden trotzdem weiterhin nicht als Stufe 1 interpretiert; die Generator-Hard-Rules bleiben durch Issue #52 unverändert.

Verteilung der bekannten Dominanzwerte:

| Stufe | Konzepte |
|---:|---:|
| 1 | 23 |
| 2 | 112 |
| 3 | 221 |
| 4 | 154 |
| 5 | 86 |
| unbekannt | 56 |

**Entscheidung:** Dimensionen bleiben für Generatorversion 1 trotz der deutlich besseren Datenlage optional und niedrig gewichtet. Issue #52 ändert keine Generatoralgorithmen oder Hard Rules. Auffällige verbleibende Lücken können später aus Simulation und Generator-Labor als gezielte redaktionelle Aufgaben entstehen.

## 7. Kulinarische Flags

| Flag | Konzepte |
|---|---:|
| `CURED` | 14 |
| `DRIED` | 44 |
| `FERMENTED` | 45 |
| `PICKLED` | 8 |
| `SMOKED` | 6 |

Flags eignen sich für Diagnose, bekannte Intensitätshäufung und Ähnlichkeit. Ihre kleinen Teilmengen rechtfertigen keine Pflichtquote und keine Behauptung allgemeiner Inkompatibilität.

## 8. Konkretisierungsgraph

| Kennzahl | Wert |
|---|---:|
| direkte Kanten | 777 |
| maximale transitive Tiefe | 4 |
| Konzepte mit mehreren direkten Eltern | 102 |
| aktive Wurzeln | 28 |
| aktive Blätter | 554 |
| verbundene Ziehkandidaten | 650 von 652 |
| isolierte Ziehkandidaten | 2 von 652 |
| offene Ziehkandidaten mit direktem Kind | 62 von 62 |

Der Graph ist ausreichend tief und besitzt reale Mehrfach-Eltern-Strukturen. Er trägt:

- Parent-/Child-Hardregeln,
- informative Vorfahrenähnlichkeit,
- Ausschlussziel-Expansion.

Die beiden isolierten Vorgaben sind kein Blocker. Ein Konzept benötigt nicht zwingend eine Hierarchiekante, um eigenständig eine gültige Challenge-Vorgabe zu sein. Für semantische Ähnlichkeit steht bei diesen Konzepten lediglich weniger Information zur Verfügung.

## 9. Gate-Ergebnis

**Ergebnis: bestanden mit deutlich verbreiterter, weiterhin optionaler Dimensionsnutzung.**

Vor Phase 9B ist kein zusätzliches Katalog- oder Metadatenpaket erforderlich, weil:

- Rollen, Beschaffbarkeit und Neuigkeit vollständig sind,
- spezifische und offene Pools deutlich über den Mindestgrößen liegen,
- alle Referenzrollen ausreichend besetzt sind,
- der Graph Redundanz und semantische Nähe zuverlässig genug unterstützt.

Die verbleibenden Dimensionslücken werden nicht durch erfundene Defaults kaschiert. Sie begrenzen weiterhin bewusst den Anspruch des Generators:

- Struktur und Neuigkeitsbalance sind belastbar prüfbar.
- bekannte Eigenschaftshäufungen sind als schwaches Softsignal nutzbar.
- echte paarweise Zutatenkompatibilität bleibt beim späteren Kurator.

## 10. Operativer Katalog

Diese Messung beschreibt den reproduzierbaren Repository-Baseline-Katalog. Die laufende PostgreSQL-Datenbank ist nach Einführung der Webverwaltung die redaktionelle Quelle der Wahrheit und kann abweichen.

Deshalb gelten zusätzlich:

- Phase 9B behandelt fehlende hart erforderliche Daten pro Konzept konservativ.
- Eine zu kleine geeignete Projektion führt zu einem klaren Datenreife-/Erschöpfungsergebnis.
- Das Generator-Labor zeigt fehlende Metadaten und verlinkt auf die Katalogpflege.
- Phase 9F führt vor Abschluss einen gesonderten nicht schreibenden Lauf gegen den operativen Katalog durch und dokumentiert dessen Fingerprint.
