# Availability-Neuaudit v2 – Tranche 4

Stand: 2026-09-05

Issue: #188, eingeordnet unter Tracking-Issue #186
Bewertungsumfang: 853 anwendbare Konzepte × Georgia und Tobias = 1.706 Personenentscheidungen

## Umfang und Bewertungsbasis

Dieser Audit ist eine vollständige, getrennte Neubewertung der Availability. Maßgeblich waren ausschließlich die im Issue menschlich freigegebene v2-Semantik, die 84 v2-Anker (davon 83 bewertete Konzepte) und neue beziehungsweise im Audit erneut geprüfte markt-, produktform- und personenspezifische Evidenz. Der Vorschlagsstand vom 2026-09-03 wurde erst nach Abschluss beider unabhängiger Personenpässe für den Vergleich geladen; seine Werte und frühere Vorschläge waren kein Bewertungsinput.

Die Nacharbeit vom 2026-09-05/06 ist kein weiterer pauschaler Bewertungsdurchlauf. Sie redigiert die konkret beanstandeten aktiven Begründungen, korrigiert gezielt Händler- und Logistikevidenz und ändert gegenüber `f8f7675` ausschließlich fünf Ratings je Person: `CARP`, `CHERVIL`, `DANABLU` und `GOAT` von DIFFICULT auf PLANNED sowie `FRUIT_DUMPLING` von PLANNED auf DIFFICULT. Die v2-Semantik und sämtliche freigegebenen Ankerstufen bleiben unverändert; bei Ankern ist sichtbar, dass die Ratingfreigabe nicht automatisch einen später redigierten Notiztext oder eine Händlerprüfung freigibt.

Cooking Novelty, produktive Katalogwerte, Gewichte, Migrationen und Generatorparameter bleiben unverändert. Inhalte aus #189 und #190 wurden nicht vorgezogen.

## Ergebnis je Person

| Person | EASY | PLANNED | SPECIALTY | DIFFICULT | UNAVAILABLE | Summe |
|---|---:|---:|---:|---:|---:|---:|
| Georgia, v2 | 423 | 270 | 96 | 62 | 2 | 853 |
| Georgia, Vorschlag 2026-09-03 | 572 | 218 | 50 | 11 | 2 | 853 |
| Georgia, Delta | -149 | +52 | +46 | +51 | 0 | 0 |
| Tobias, v2 | 423 | 264 | 97 | 67 | 2 | 853 |
| Tobias, Vorschlag 2026-09-03 | 573 | 206 | 60 | 12 | 2 | 853 |
| Tobias, Delta | -150 | +58 | +37 | +55 | 0 | 0 |

Georgia hat 287 Entscheidungen gegenüber dem letzten Vorschlagsstand geändert und 566 beibehalten. Tobias hat 288 geändert und 565 beibehalten.

| Übergang | Georgia | Tobias |
|---|---:|---:|
| DIFFICULT → PLANNED | 1 | 1 |
| EASY → PLANNED | 146 | 145 |
| EASY → SPECIALTY | 3 | 5 |
| PLANNED → DIFFICULT | 12 | 9 |
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

Die kanonische Evidenzdatei umfasst 589 Evidenzzeilen für 335 Konzepte. Davon enthalten 495 Zeilen eine URL; es gibt 347 unterschiedliche URLs auf 132 wörtlich unterschiedlichen Hosts. Die Rollen verteilen sich auf 94 `ANCHOR_APPROVAL`, 214 `EXACT_ROUTE`, 211 `MARKET_BREADTH`, 4 `PERSON_ROUTE`, 59 `ROUTE_LIMITATION` und 7 `NEGATIVE_SEARCH`.

| Person | relevante Evidenzzeilen | abgedeckte Konzepte | mit positiver/variabler Route | evidenzpflichtige Entscheidungen | davon SPECIALTY/DIFFICULT/UNAVAILABLE |
|---|---:|---:|---:|---:|---:|
| Georgia | 451 | 326 | 318 | 296 | 160 |
| Tobias | 420 | 314 | 306 | 268 | 166 |

Alle 564 evidenzpflichtigen Personenzuordnungen sind abgedeckt. Acht Konzepte haben aktuell ausschließlich negative oder limitierende Evidenz: `COCKLES`, `CULANTRO`, `DUCK_EGG`, `FENALAR`, `LUTEFISK`, `POBLANO`, `RAZOR_CLAMS` und `TOMATILLO`. Das betrifft 13 Evidenzzeilen und 16 Personenentscheidungen. Der zusätzliche Negativ-Evidenz-Recheck dokumentiert außerdem `GAC_FRUIT`, dessen frühere Negativlage durch den positiven formgenauen Asia-Moin-Treffer korrigiert wurde.

Zusätzliche gezielte Nachprüfungen schließen zwei zuvor erkannte Evidenzlücken:

- 21 SPECIALTY-Fälle wurden erneut auf breite einschlägige Spezialmarkt-Abdeckung geprüft: 17 bestätigt, 4 Konzepte und damit 8 Personenentscheidungen korrigiert.
- 30 aus einem PLANNED-Gate hervorgegangene Fälle wurden erneut gegen die stabile positive Zugangsroute geprüft: 28 blieben PLANNED; `FRESH_TURMERIC` wurde SPECIALTY und `FRUIT_DUMPLING` DIFFICULT. Die aktuellen Recheckzeilen bleiben die autoritative Folgeentscheidung.

## Individuelle Notizqualität

Alle 1.706 Kernnotizen wurden auf kurze Beschaffungsbegründungen verdichtet. Entfernt wurden Geschmacks- und Verwendungstext, vollständige Produktdefinitionen, fingierte Telefon-/Bestands-/Besuchsroutinen sowie falsche Behauptungen, die menschliche Ankerfreigabe genehmige den jeweiligen Notiztext. Identische Georgia-/Tobias-Notizen bleiben bewusst zulässig, wenn die reale Beschaffungssituation gleich ist. Repräsentative Vorher-/Nachher-Belege stehen in `availability-novelty-availability-note-editorial-examples-v2-20260905.csv`.

Der Validator prüft die Auditdateien strukturell, referenziell und anhand ihrer Statusübergänge. Historische Divergenz- und Notizkorrekturzeilen werden gegen ihren damaligen Vorher-/Empfehlungsstand geprüft und nicht mehr mit späteren Live-Ratings gleichgesetzt; aktive Recheck-Folgeentscheidungen und offene Divergenzen bleiben an den aktuellen Stand gebunden. Redaktionelle Sollmengen, konzeptspezifische Routentests, globale Notiz-Eindeutigkeit, Satzgerüst-/Klauselcluster und Vierwortfragment-Gates bleiben entfernt. Der vollständige generische Prüfpfad verwendet nur `CONCEPT_ALPHA`: identische gültige G/T-Notizen und eine neue belegte Nicht-Anker-Differenz mit positiven `EV-G`-/`EV-T`-Referenzen nach historischer Angleichung werden akzeptiert; unbekannte Evidenz-ID sowie falscher Konzept- oder Personen-Scope werden abgelehnt.

## Wichtigste Grenzfälle

- `BELACAN` ist für beide DIFFICULT: Die belastbare Route bleibt ein enger Importweg mit geringer beziehungsweise unklarer Bestandsstabilität.
- `BERBERE` ist für beide PLANNED: Zwei voneinander unabhängige allgemeine deutsche Gewürzhändler tragen die haltbare Trockenform.
- `GOAT` ist für beide PLANNED: Alber ist aktuell ausdrücklich nicht vorrätig; Heilmanns zeigt ein nicht verfügbares 5-kg-Paket mit nächster Schlachtung am 05.10.2026. Positiv tragen stattdessen Feinkost Pum mit sofort lieferbarer Ziegenkeule und deutschlandweitem Kühlversand sowie der Holler-Hof mit mehreren begrenzt verfügbaren Einzelzuschnitten, Tiefkühlversand und 3–5 Tagen Lieferzeit.
- `PURSLANE` ist für beide DIFFICULT: Der exakte Sommerportulak ist ausverkauft; Alternativen sind regional oder in der Form mehrdeutig.
- `CARP` ist für beide PLANNED: Ein aktuell positiver deutscher TK-Endkundenweg und eine unabhängige wiederkehrende Fischhandelslistung tragen die Stufe; Saison und Kühlannahme bleiben planungsrelevant.
- `CHERVIL` ist für beide PLANNED: fruits & friends ist passiv und zählt nicht positiv. Früchte-Kontor belegt die Bundware mit deutschlandweitem vorgekühltem Standard- oder getrenntem Kühlversand; REWE und OBI ergänzen allgemeine, standortabhängige Kettenwege. Saison und lokaler Bestand bleiben variabel.
- `DANABLU` ist für beide PLANNED: Der exakte Castello-Danablu ist im nationalen REWE-Katalog und bei einem unabhängigen Kühlversender gelistet.
- `FRESH_TURMERIC` bleibt für beide SPECIALTY: Mehrere unabhängige Asia-Frischerouten tragen die breite Spezialmarktpräsenz; allgemeine Listungen bleiben filialabhängig und erzwingen keine leichtere Stufe.
- `GAC_FRUIT` bleibt für beide DIFFICULT: Die korrigierte Evidenz belegt einen positiven formgenauen TK-Importweg, aber keine breite oder robuste Händlerlandschaft.
- `DULSE` bleibt für beide PLANNED: Marktplatzverkäufer Miraherba ist korrekt zugerechnet und eine unabhängige Naturkostroute belegt die planbare Marktpräsenz.
- `FRUIT_DUMPLING` ist für beide DIFFICULT: MyBio zeigt den 1,625-kg-Artikel aktuell als nicht verfügbar und belegt mit seiner allgemeinen DHL-Aussage keine TK-Zustellung gerade dieses Produkts nach Bornheim und Rostock. Es bleibt nur der positive 3-kg-TK-Spezialversandweg mit geringem beobachtetem Bestand; das trägt keinen breiten allgemeinen Haushaltsmarkt.
- `COCONUT_VINEGAR` bleibt für beide SPECIALTY: Die Händler sind breit über mehrere asiatische Länderküchen und Warengruppen sortiert; unterschiedliche haltbare Kokosessigvarianten und gewöhnlicher Paketversand sprechen gegen einen einzelnen philippinischen Nischenkanal.
- `GALANGAL` bleibt für beide SPECIALTY: Zwei breite Asia-Vollsortimenter führen frisches Rhizom als reguläre 100-g-Gemüseware; Versandtage sowie Kühl-/Expressbedingungen bleiben echte, aber beherrschbare Frischegrenzen.
- `MORCILLA` bleibt für beide SPECIALTY: Ein nationaler spanischer Feinkostversender und ein breiter iberischer Supermarkt-/Versandhandel bilden unterschiedliche reguläre Kühlsortimentsformate; die Ware hängt nicht nur an einem produktspezifischen Importeur.
- `PRESERVED_LEMON` bleibt für beide SPECIALTY: Zwei breit sortierte Feinkosthändler führen verschiedene Marken ganzer Salzzitronen im haltbaren Glas über gewöhnlichen Paketversand und damit unabhängige Gourmet-Lieferketten.
- `LAMBIC` ist für beide SPECIALTY: Mehrere unabhängige Spezialbier-Routen tragen die Einordnung, eine allgemeine Route fehlt.
- `DUMPLING_DOUGH` ist für beide PLANNED: Grundlage ist ein ungeschnittener gekühlter Teig; das Zuschneiden zu Wrappern bleibt als transparente Forminferenz dokumentiert.

## Artefakte

Generator und Validator erzeugen beziehungsweise prüfen die getrennten Blindinputs, Personenentscheidungen, Personenreviews, den kombinierten Review, den Vergleich, die kanonische Evidenz sowie die versionierten Ausreißer- und Auditspuren. Die geschützten Cooking-Artefakte, früheren Availability-Freigaben und v2-Ankerentscheidungen wurden nicht verändert. Historische Auditzeilen bleiben als damalige Aussagen erhalten; aktuelle Evidenz und aktive Rechecks dokumentieren die Folgeentscheidungen. Negativ-Evidenz-Recheck und redaktionelle Vorher-/Nachher-Beispiele sind ergänzende Artefakte.

## Pflichtprüfungen

- `pwsh -File docs/analysis/generate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; 860 Zeilen, 853 anwendbare Konzepte, 7 Strukturknoten, 84 v2-Anker, 11 Personenunterschiede und 564/564 evidenzpflichtige Zuordnungen.
- `pwsh -File docs/analysis/validate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; alle Verteilungen, Anker, Evidenzgates, 1.706 Notizgates und Schutzprüfungen erfolgreich.
- `pwsh -File docs/analysis/validate-availability-novelty-cooking-review-20260903.ps1`: PASS; 853 anwendbare Konzepte und 39 freigegebene Cooking-Anker unverändert konsistent.
- `git diff --check`: PASS; vor dem Staging ohne Befund. `git diff --cached --check`: PASS nach dem expliziten Staging der 13 geänderten Lieferartefakte.
- `./mvnw verify`: BUILD SUCCESS; 482 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen. Surefire musste die bereits erfolgreich beendete Fork-JVM nach 30 Sekunden Shutdown-Wartezeit beenden; das Build blieb erfolgreich.

## Freigabegrenze

Diese Tranche liefert nur den Review- und Evidenzstand für die nächste menschliche Freigabe. Es erfolgt keine Übernahme in produktive Katalogwerte und keine weitere menschliche Freigabecharge im Rahmen dieses Pakets.
