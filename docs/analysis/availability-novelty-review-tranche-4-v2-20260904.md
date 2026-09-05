# Availability-Neuaudit v2 – Tranche 4

Stand: 2026-09-05

Issue: #188, eingeordnet unter Tracking-Issue #186
Bewertungsumfang: 853 anwendbare Konzepte × Georgia und Tobias = 1.706 Personenentscheidungen

## Umfang und Bewertungsbasis

Dieser Audit ist eine vollständige, getrennte Neubewertung der Availability. Maßgeblich waren ausschließlich die im Issue menschlich freigegebene v2-Semantik, die 84 v2-Anker (davon 83 bewertete Konzepte) und neue beziehungsweise im Audit erneut geprüfte markt-, produktform- und personenspezifische Evidenz. Der Vorschlagsstand vom 2026-09-03 wurde erst nach Abschluss beider unabhängiger Personenpässe für den Vergleich geladen; seine Werte und frühere Vorschläge waren kein Bewertungsinput.

Die Nacharbeit vom 2026-09-05 ist kein weiterer pauschaler Bewertungsdurchlauf. Sie redigiert alle Notizen nach den freigegebenen Qualitätsregeln, korrigiert konkret beanstandete Evidenzen und ändert ausschließlich vier fachlich neu belegte Ratings je Person. Die v2-Semantik und sämtliche freigegebenen Ankerstufen bleiben unverändert; bei Ankern ist nun sichtbar, dass die Ratingfreigabe nicht automatisch den neu redigierten Notiztext freigibt.

Cooking Novelty, produktive Katalogwerte, Gewichte, Migrationen und Generatorparameter bleiben unverändert. Inhalte aus #189 und #190 wurden nicht vorgezogen.

## Ergebnis je Person

| Person | EASY | PLANNED | SPECIALTY | DIFFICULT | UNAVAILABLE | Summe |
|---|---:|---:|---:|---:|---:|---:|
| Georgia, v2 | 423 | 271 | 96 | 61 | 2 | 853 |
| Georgia, Vorschlag 2026-09-03 | 572 | 218 | 50 | 11 | 2 | 853 |
| Georgia, Delta | -149 | +53 | +46 | +50 | 0 | 0 |
| Tobias, v2 | 423 | 265 | 97 | 66 | 2 | 853 |
| Tobias, Vorschlag 2026-09-03 | 573 | 206 | 60 | 12 | 2 | 853 |
| Tobias, Delta | -150 | +59 | +37 | +54 | 0 | 0 |

Georgia hat 286 Entscheidungen gegenüber dem letzten Vorschlagsstand geändert und 567 beibehalten. Tobias hat 287 geändert und 566 beibehalten.

| Übergang | Georgia | Tobias |
|---|---:|---:|
| DIFFICULT → PLANNED | 1 | 1 |
| EASY → PLANNED | 146 | 145 |
| EASY → SPECIALTY | 3 | 5 |
| PLANNED → DIFFICULT | 11 | 8 |
| PLANNED → SPECIALTY | 84 | 80 |
| SPECIALTY → DIFFICULT | 40 | 47 |
| SPECIALTY → PLANNED | 1 | 1 |

Die starke Verschiebung aus EASY folgt der v2-Semantik: Ein Bezug über planbare Vollsortimenter, Spezialmärkte oder enge Importwege ist nicht mehr mit bloßer grundsätzlicher Beschaffbarkeit gleichgesetzt.

## Personenunterschiede

Es gibt 11 Personenunterschiede. Sämtliche Unterschiede sind freigegebene personenbezogene v2-Anker; außerhalb dieser Anker gibt es keine unbegründete Abweichung.

| Konzept | Georgia | Tobias |
|---|---|---|
| CURRY_LEAVES | SPECIALTY | DIFFICULT |
| DATE_SYRUP | PLANNED | SPECIALTY |
| GARLIC_CHIVES | SPECIALTY | DIFFICULT |
| HARISSA | PLANNED | SPECIALTY |
| NATTO | SPECIALTY | DIFFICULT |
| POMEGRANATE_MOLASSES | PLANNED | SPECIALTY |
| PUL_BIBER | PLANNED | SPECIALTY |
| SUMAC | PLANNED | SPECIALTY |
| THAI_BASIL | SPECIALTY | DIFFICULT |
| THAI_EGGPLANT | SPECIALTY | DIFFICULT |
| ZAATAR | PLANNED | SPECIALTY |

## Evidenzabdeckung

Die kanonische Evidenzdatei umfasst 587 Evidenzzeilen für 335 Konzepte. Davon enthalten 493 Zeilen eine URL; es gibt 345 unterschiedliche URLs auf 130 wörtlich unterschiedlichen Hosts. Die Rollen verteilen sich auf 94 `ANCHOR_APPROVAL`, 215 `EXACT_ROUTE`, 212 `MARKET_BREADTH`, 4 `PERSON_ROUTE`, 55 `ROUTE_LIMITATION` und 7 `NEGATIVE_SEARCH`.

| Person | relevante Evidenzzeilen | abgedeckte Konzepte | mit positiver/variabler Route | evidenzpflichtige Entscheidungen | davon SPECIALTY/DIFFICULT/UNAVAILABLE |
|---|---:|---:|---:|---:|---:|
| Georgia | 449 | 326 | 318 | 295 | 159 |
| Tobias | 418 | 314 | 306 | 268 | 165 |

Alle 563 evidenzpflichtigen Personenzuordnungen sind abgedeckt. Acht Konzepte haben aktuell ausschließlich negative oder limitierende Evidenz: `COCKLES`, `CULANTRO`, `DUCK_EGG`, `FENALAR`, `LUTEFISK`, `POBLANO`, `RAZOR_CLAMS` und `TOMATILLO`. Das betrifft 13 Evidenzzeilen und 16 Personenentscheidungen. Der zusätzliche Negativ-Evidenz-Recheck dokumentiert außerdem `GAC_FRUIT`, dessen frühere Negativlage durch den positiven formgenauen Asia-Moin-Treffer korrigiert wurde.

Zusätzliche gezielte Nachprüfungen schließen zwei zuvor erkannte Evidenzlücken:

- 21 SPECIALTY-Fälle wurden erneut auf breite einschlägige Spezialmarkt-Abdeckung geprüft: 17 bestätigt, 4 Konzepte und damit 8 Personenentscheidungen korrigiert.
- 29 PLANNED-Fälle wurden erneut gegen die stabile positive Zugangsroute geprüft: 25 bestätigt, 4 Konzepte und damit 8 Personenentscheidungen korrigiert; 30 neue direkte Evidenzzeilen wurden append-only ergänzt.

## Individuelle Notizqualität

Alle 1.706 Kernnotizen wurden auf kurze Beschaffungsbegründungen verdichtet. Entfernt wurden Geschmacks- und Verwendungstext, vollständige Produktdefinitionen, fingierte Telefon-/Bestands-/Besuchsroutinen sowie falsche Behauptungen, die menschliche Ankerfreigabe genehmige den jeweiligen Notiztext. Identische Georgia-/Tobias-Notizen bleiben bewusst zulässig, wenn die reale Beschaffungssituation gleich ist. Repräsentative Vorher-/Nachher-Belege stehen in `availability-novelty-availability-note-editorial-examples-v2-20260905.csv`.

Der Validator prüft die Auditdateien strukturell, referenziell und anhand ihrer Statusübergänge. Redaktionelle Sollmengen, konzeptspezifische Routentests, globale Notiz-Eindeutigkeit, Satzgerüst-/Klauselcluster und Vierwortfragment-Gates wurden entfernt. Die verlangten generischen Selbsttests verwenden nur synthetische Konzepte: gleiche gültige Personennotiz und zusätzlicher evidenzgestützter Personenunterschied werden akzeptiert; unbekannte Evidenz-ID sowie falscher Konzept- oder Personen-Scope werden abgelehnt.

## Wichtigste Grenzfälle

- `BELACAN` ist für beide DIFFICULT: Die belastbare Route bleibt ein enger Importweg mit geringer beziehungsweise unklarer Bestandsstabilität.
- `BERBERE` ist für beide PLANNED: Zwei voneinander unabhängige allgemeine deutsche Gewürzhändler tragen die haltbare Trockenform.
- `GOAT` ist für beide PLANNED: Zwei unabhängige deutsche Online-Metzgereien belegen Ziegenfleisch mit planbarer Kühlzustellung; enge Einzelangebote tragen die Stufe nicht allein.
- `PURSLANE` ist für beide DIFFICULT: Der exakte Sommerportulak ist ausverkauft; Alternativen sind regional oder in der Form mehrdeutig.
- `CARP` ist für beide PLANNED: Ein aktuell positiver deutscher TK-Endkundenweg und eine unabhängige wiederkehrende Fischhandelslistung tragen die Stufe; Saison und Kühlannahme bleiben planungsrelevant.
- `CHERVIL` ist für beide PLANNED: Zwei unabhängige Bundwarenwege und essbarer Kerbel im allgemeinen Pflanzenhandel belegen die Frischform; Saison und lokaler Bestand bleiben variabel.
- `DANABLU` ist für beide PLANNED: Der exakte Castello-Danablu ist im nationalen REWE-Katalog und bei einem unabhängigen Kühlversender gelistet.
- `FRESH_TURMERIC` bleibt für beide SPECIALTY: Mehrere unabhängige Asia-Frischerouten tragen die breite Spezialmarktpräsenz; allgemeine Listungen bleiben filialabhängig und erzwingen keine leichtere Stufe.
- `GAC_FRUIT` bleibt für beide DIFFICULT: Die korrigierte Evidenz belegt einen positiven formgenauen TK-Importweg, aber keine breite oder robuste Händlerlandschaft.
- `DULSE` bleibt für beide PLANNED: Marktplatzverkäufer Miraherba ist korrekt zugerechnet und eine unabhängige Naturkostroute belegt die planbare Marktpräsenz.
- `FRUIT_DUMPLING` bleibt für beide PLANNED: Neben dem 3-kg-Gastronomiegebinde ist eine unabhängige verfügbare 1,625-kg-Lebensmittelroute belegt; TK-Annahme bleibt nötig.
- `LAMBIC` ist für beide SPECIALTY: Mehrere unabhängige Spezialbier-Routen tragen die Einordnung, eine allgemeine Route fehlt.
- `DUMPLING_DOUGH` ist für beide PLANNED: Grundlage ist ein ungeschnittener gekühlter Teig; das Zuschneiden zu Wrappern bleibt als transparente Forminferenz dokumentiert.

## Artefakte

Generator und Validator erzeugen beziehungsweise prüfen die getrennten Blindinputs, Personenentscheidungen, Personenreviews, den kombinierten Review, den Vergleich, die kanonische Evidenz sowie die versionierten Ausreißer- und Auditspuren. Die geschützten Cooking-Artefakte, früheren Availability-Freigaben und v2-Ankerentscheidungen wurden nicht verändert. Beanstandete v2-Evidenz- und Auditzeilen wurden fachlich korrigiert; Negativ-Evidenz-Recheck und redaktionelle Vorher-/Nachher-Beispiele sind ergänzende Artefakte vom 2026-09-05.

## Pflichtprüfungen

- `pwsh -File docs/analysis/generate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; 860 Zeilen, 853 anwendbare Konzepte, 7 Strukturknoten, 84 v2-Anker, 11 Personenunterschiede und 563/563 evidenzpflichtige Zuordnungen.
- `pwsh -File docs/analysis/validate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; alle Verteilungen, Anker, Evidenzgates, 1.706 Notizgates und Schutzprüfungen erfolgreich.
- `pwsh -File docs/analysis/validate-availability-novelty-cooking-review-20260903.ps1`: PASS; 853 anwendbare Konzepte und 39 freigegebene Cooking-Anker unverändert konsistent.
- `git diff --check`: PASS; vor dem Staging ohne Befund. `git diff --cached --check` ist nach dem expliziten Staging aller 22 Lieferartefakte ebenfalls ohne Befund.
- `./mvnw verify`: BUILD SUCCESS; 482 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen. Surefire musste die bereits erfolgreich beendete Fork-JVM nach 30 Sekunden Shutdown-Wartezeit beenden; das Build blieb erfolgreich.

## Freigabegrenze

Diese Tranche liefert nur den Review- und Evidenzstand für die nächste menschliche Freigabe. Es erfolgt keine Übernahme in produktive Katalogwerte und keine weitere menschliche Freigabecharge im Rahmen dieses Pakets.
