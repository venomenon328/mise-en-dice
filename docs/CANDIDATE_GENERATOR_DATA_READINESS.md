# Datenreife für den Kandidatengenerator

Stand: 12. August 2026  
Bezugsstand: `main` auf Commit `e184678ba457bdec4798a8d238cd121174e5892f`

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
| aktive Ziehkandidaten | 663 |
| `SPECIFIC` | 576 |
| `OPEN` | 87 |
| mit Neuigkeitsstufe | 663 |
| ohne Neuigkeitsstufe | 0 |

Beide Spezifitätsklassen sind groß genug, um 2/3/4-Spezifitätsmixe und ein Reservoir deutlich oberhalb von zwölf Kandidaten zu unterstützen.

## 3. Rollen

Alle 663 Ziehkandidaten besitzen mindestens eine funktionale Rolle.

| Rolle | gesamt | spezifisch | offen |
|---|---:|---:|---:|
| `ACID` | 56 | 54 | 2 |
| `ANIMAL_PROTEIN` | 158 | 137 | 21 |
| `AROMATIC` | 107 | 95 | 12 |
| `FAT` | 137 | 124 | 13 |
| `FRUIT` | 72 | 62 | 10 |
| `PLANT_PROTEIN` | 68 | 60 | 8 |
| `SEASONING` | 231 | 204 | 27 |
| `STARCH` | 101 | 91 | 10 |
| `VEGETABLE` | 128 | 109 | 19 |

Die kleinste Rolle ist `ACID` mit 56 Kandidaten und überschreitet das Gate von 48. Profilslots verlangen nicht gleichzeitig eine offene und eine bestimmte Rolle; die teilweise kleinen offenen Teilpools einzelner Rollen sind daher kein Blocker.

Die häufigsten reinen beziehungsweise kombinierten Rollensignaturen sind unter anderem:

| Signatur | gesamt |
|---|---:|
| `ANIMAL_PROTEIN` | 78 |
| `VEGETABLE` | 71 |
| `AROMATIC+SEASONING` | 62 |
| `STARCH` | 60 |
| `ANIMAL_PROTEIN+FAT` | 46 |
| `SEASONING` | 43 |
| `FRUIT` | 37 |
| `ANIMAL_PROTEIN+FAT+SEASONING` | 22 |
| `AROMATIC` | 22 |
| `ACID+SEASONING` | 20 |
| `PLANT_PROTEIN+STARCH` | 20 |

514 Konzepte besitzen mindestens eine der breit als strukturell oder unterstützend gemessenen Rollen einschließlich `FAT`; 149 sind ausschließlich geschmacksgebend. Die verbindliche Generatorspezifikation verwendet für ihre härtere Ankerdefinition kein Fett als alleinige Ankerrolle und verhindert dadurch, dass mehrere reine Fett-/Würzkomponenten eine Struktur nur auf dem Papier erfüllen.

## 4. Beschaffbarkeit

Für 663 Ziehkandidaten und zwei aktive Teilnehmer existieren vollständig 1.326 Zuordnungen. Es fehlt keine Zeile.

### 4.1 Georgia

| Stufe | Konzepte |
|---|---:|
| `EASY` | 486 |
| `PLANNED` | 161 |
| `DIFFICULT` | 16 |
| `UNAVAILABLE` | 0 |

### 4.2 Tobias

| Stufe | Konzepte |
|---|---:|
| `EASY` | 462 |
| `PLANNED` | 137 |
| `DIFFICULT` | 64 |
| `UNAVAILABLE` | 0 |

### 4.3 Gemeinsame schlechteste Stufe

| schlechteste Stufe beider Teilnehmer | Konzepte |
|---|---:|
| `EASY` | 462 |
| `PLANNED` | 137 |
| `DIFFICULT` | 64 |

Der Pool ist vollständig nutzbar. Die 64 gemeinsam mindestens schwierigen Konzepte sind groß genug, um nicht als Einzelfälle behandelt zu werden, müssen aber über Gewichtung und Satzcaps dosiert werden.

## 5. Neuigkeit

Die Neuigkeitsstufe ist vollständig gepflegt, auch je Rolle.

| Stufe | gesamt | spezifisch | offen |
|---:|---:|---:|---:|
| 1 | 304 | 248 | 56 |
| 2 | 190 | 164 | 26 |
| 3 | 94 | 89 | 5 |
| 4 | 53 | 53 | 0 |
| 5 | 22 | 22 | 0 |

75 Konzepte liegen auf Stufe 4 oder 5. Außergewöhnlichkeit ist damit kein theoretisches Randmerkmal, sondern ein ausreichend großer Teilpool. Zugleich sind sämtliche offenen Vorgaben auf Stufe 1 bis 3 eingeordnet; extreme Neuigkeit entsteht in der Baseline ausschließlich durch spezifische Zutaten.

Das unterstützt die vorgesehene Kadenz:

- vertraute und ausgewogene Kandidaten besitzen breite Pools,
- abenteuerliche Kandidaten bleiben möglich,
- Stufe 4/5 muss innerhalb eines Kandidaten, im Zwölfer-Satz und über sichtbare Wochen begrenzt werden.

## 6. Kulinarische Dimensionen

| Dimension | gepflegt | fehlend | Abdeckung |
|---|---:|---:|---:|
| `ACIDITY` | 78 | 585 | 11,8 % |
| `BITTERNESS` | 53 | 610 | 8,0 % |
| `DOMINANCE` | 366 | 297 | 55,2 % |
| `FATTINESS` | 128 | 535 | 19,3 % |
| `HEAT` | 57 | 606 | 8,6 % |
| `SWEETNESS` | 154 | 509 | 23,2 % |
| `UMAMI` | 221 | 442 | 33,3 % |

Selbst `DOMINANCE` ist nicht ausreichend flächendeckend für eine faire harte Regel. Die Rollenabdeckung ist zudem stark unterschiedlich:

| Rolle | Dominanz bekannt | gesamt |
|---|---:|---:|
| `ACID` | 39 | 56 |
| `ANIMAL_PROTEIN` | 70 | 158 |
| `AROMATIC` | 97 | 107 |
| `FAT` | 74 | 137 |
| `FRUIT` | 38 | 72 |
| `PLANT_PROTEIN` | 22 | 68 |
| `SEASONING` | 207 | 231 |
| `STARCH` | 49 | 101 |
| `VEGETABLE` | 41 | 128 |

Insbesondere Würzkomponenten und Aromaten sind gut beschrieben, Protein-, Gemüse- und Stärkepools dagegen deutlich schwächer. Eine harte Dominanzprüfung würde daher gerade strukturelle Zutaten systematisch anders behandeln als Würzkomponenten.

Verteilung der bekannten Dominanzwerte:

| Stufe | Konzepte |
|---:|---:|
| 1 | 18 |
| 2 | 42 |
| 3 | 82 |
| 4 | 139 |
| 5 | 85 |
| unbekannt | 297 |

**Entscheidung:** Dimensionen bleiben für Generatorversion 1 optional und niedrig gewichtet. Es wird vor Phase 9B kein flächendeckendes Dimensions-Nachpflegepaket erzwungen. Auffällige Lücken können später aus Simulation und Generator-Labor als gezielte redaktionelle Aufgaben entstehen.

## 7. Kulinarische Flags

| Flag | Konzepte |
|---|---:|
| `CURED` | 14 |
| `DRIED` | 40 |
| `FERMENTED` | 43 |
| `PICKLED` | 8 |
| `SMOKED` | 6 |

Flags eignen sich für Diagnose, bekannte Intensitätshäufung und Ähnlichkeit. Ihre kleinen Teilmengen rechtfertigen keine Pflichtquote und keine Behauptung allgemeiner Inkompatibilität.

## 8. Konkretisierungsgraph

| Kennzahl | Wert |
|---|---:|
| direkte Kanten | 735 |
| maximale transitive Tiefe | 5 |
| Konzepte mit mehreren direkten Eltern | 88 |
| aktive Wurzeln | 24 |
| aktive Blätter | 530 |
| verbundene Ziehkandidaten | 662 von 663 |
| isolierte Ziehkandidaten | 1 von 663 |
| offene Ziehkandidaten mit direktem Kind | 87 von 87 |

Der Graph ist ausreichend tief und besitzt reale Mehrfach-Eltern-Strukturen. Er trägt:

- Parent-/Child-Hardregeln,
- informative Vorfahrenähnlichkeit,
- Ausschlussziel-Expansion.

Die einzelne isolierte Vorgabe ist kein Blocker. Ein Konzept benötigt nicht zwingend eine Hierarchiekante, um eigenständig eine gültige Challenge-Vorgabe zu sein. Für semantische Ähnlichkeit steht bei diesem Konzept lediglich weniger Information zur Verfügung.

## 9. Gate-Ergebnis

**Ergebnis: bestanden mit ausdrücklich begrenzter Dimensionsnutzung.**

Vor Phase 9B ist kein zusätzliches Katalog- oder Metadatenpaket erforderlich, weil:

- Rollen, Beschaffbarkeit und Neuigkeit vollständig sind,
- spezifische und offene Pools deutlich über den Mindestgrößen liegen,
- alle Referenzrollen ausreichend besetzt sind,
- der Graph Redundanz und semantische Nähe zuverlässig genug unterstützt.

Die geringe Dimensionsabdeckung wird nicht durch erfundene Defaults kaschiert. Sie begrenzt bewusst den Anspruch des Generators:

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
