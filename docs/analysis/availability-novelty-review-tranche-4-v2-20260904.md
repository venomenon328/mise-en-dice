# Availability-Neuaudit v2 – Tranche 4

Stand: 2026-09-06

Issue: #188, eingeordnet unter Tracking-Issue #186
Bewertungsumfang: 853 anwendbare Konzepte × Georgia und Tobias = 1.706 Personenentscheidungen

## Umfang und Bewertungsbasis

Dieser Audit ist eine vollständige, getrennte Neubewertung der Availability. Maßgeblich waren ausschließlich die im Issue menschlich freigegebene v2-Semantik, die 84 v2-Anker (davon 83 bewertete Konzepte) und neue beziehungsweise im Audit erneut geprüfte markt-, produktform- und personenspezifische Evidenz. Der Vorschlagsstand vom 2026-09-03 wurde erst nach Abschluss beider unabhängiger Personenpässe für den Vergleich geladen; seine Werte und frühere Vorschläge waren kein Bewertungsinput.

Die Nacharbeit vom 2026-09-05/06 ist kein weiterer pauschaler Bewertungsdurchlauf. Gegenüber dem geprüften Stand `75191fff8376afba523eab689a838ac6007844ab` ändert sie ausschließlich zwei Ratings je Person: `FRUIT_DUMPLING` von DIFFICULT auf PLANNED und `GOAT` von PLANNED auf SPECIALTY. Außerdem bereinigt sie 73 aktive Tobias-Marktbegründungen mit unbelegten Freigabe-, Händlerbesuchs- oder Standortprüfungsbehauptungen und schließt den bestehenden 21er-Specialty-Recheck qualitativ ab. Die v2-Semantik und sämtliche freigegebenen Ankerstufen bleiben unverändert; bei Ankern ist sichtbar, dass die Ratingfreigabe nicht automatisch einen später redigierten Notiztext oder eine Händlerprüfung freigibt.

Cooking Novelty, produktive Katalogwerte, Gewichte, Migrationen und Generatorparameter bleiben unverändert. Inhalte aus #189 und #190 wurden nicht vorgezogen.

## Ergebnis je Person

| Person | EASY | PLANNED | SPECIALTY | DIFFICULT | UNAVAILABLE | Summe |
|---|---:|---:|---:|---:|---:|---:|
| Georgia, v2 | 423 | 270 | 97 | 61 | 2 | 853 |
| Georgia, Vorschlag 2026-09-03 | 572 | 218 | 50 | 11 | 2 | 853 |
| Georgia, Delta | -149 | +52 | +47 | +50 | 0 | 0 |
| Tobias, v2 | 423 | 264 | 98 | 66 | 2 | 853 |
| Tobias, Vorschlag 2026-09-03 | 573 | 206 | 60 | 12 | 2 | 853 |
| Tobias, Delta | -150 | +58 | +38 | +54 | 0 | 0 |

Georgia hat 287 Entscheidungen gegenüber dem letzten Vorschlagsstand geändert und 566 beibehalten. Tobias hat 288 geändert und 565 beibehalten.

| Übergang | Georgia | Tobias |
|---|---:|---:|
| DIFFICULT → PLANNED | 1 | 1 |
| EASY → PLANNED | 146 | 145 |
| EASY → SPECIALTY | 3 | 5 |
| PLANNED → DIFFICULT | 11 | 8 |
| PLANNED → SPECIALTY | 85 | 81 |
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

Die kanonische Evidenzdatei umfasst 592 Evidenzzeilen für 335 Konzepte. Davon enthalten 498 Zeilen eine URL; es gibt 350 unterschiedliche URLs auf 134 wörtlich unterschiedlichen Hosts. Die Rollen verteilen sich auf 94 `ANCHOR_APPROVAL`, 214 `EXACT_ROUTE`, 214 `MARKET_BREADTH`, 4 `PERSON_ROUTE`, 59 `ROUTE_LIMITATION` und 7 `NEGATIVE_SEARCH`.

| Person | relevante Evidenzzeilen | abgedeckte Konzepte | mit positiver/variabler Route | evidenzpflichtige Entscheidungen | davon SPECIALTY/DIFFICULT/UNAVAILABLE |
|---|---:|---:|---:|---:|---:|
| Georgia | 454 | 326 | 318 | 296 | 160 |
| Tobias | 423 | 314 | 306 | 268 | 166 |

Alle 564 evidenzpflichtigen Personenzuordnungen sind abgedeckt. Acht Konzepte haben aktuell ausschließlich negative oder limitierende Evidenz: `COCKLES`, `CULANTRO`, `DUCK_EGG`, `FENALAR`, `LUTEFISK`, `POBLANO`, `RAZOR_CLAMS` und `TOMATILLO`. Das betrifft 13 Evidenzzeilen und 16 Personenentscheidungen. Der zusätzliche Negativ-Evidenz-Recheck dokumentiert außerdem `GAC_FRUIT`, dessen frühere Negativlage durch den positiven formgenauen Asia-Moin-Treffer korrigiert wurde.

Zusätzliche gezielte Nachprüfungen schließen zwei zuvor erkannte Evidenzlücken:

- Alle 21 SPECIALTY-Recheck-Fälle wurden qualitativ gegen Händlerart, Sortimentsbreite, unabhängige Vertriebswege und Form-/Logistikgrenzen geprüft: 18 bleiben SPECIALTY; `BELACAN` und `PURSLANE` bleiben korrigiert auf DIFFICULT, `BERBERE` auf PLANNED. Die zuvor auf PLANNED gesetzte `GOAT`-Entscheidung ist nach der verbindlichen Trennung von `GENERAL_BROAD` und `SPECIALTY_BROAD` wieder SPECIALTY.
- 30 aus einem PLANNED-Gate hervorgegangene Fälle wurden erneut gegen die stabile positive Zugangsroute geprüft: 29 sind PLANNED; `FRESH_TURMERIC` bleibt SPECIALTY. `FRUIT_DUMPLING` ist unter seiner vollständigen offenen Form wieder PLANNED, weil der reguläre Germknödel-mit-Fruchtfüllung-Weg im allgemeinen Kettenhandel den ausverkauften engeren MyBio-Artikel fachlich entkräftet.

## Individuelle Notizqualität

Alle 1.706 Kernnotizen wurden auf kurze Beschaffungsbegründungen verdichtet. In dieser Restnacharbeit wurden zusätzlich 73 aktive Tobias-`market_basis`-Zeilen von unbelegten Behauptungen über menschliche Wegfreigabe, Händlerbesuch oder Standortprüfung bereinigt; die sachlichen Markt-, Form- und Logistikkerne bleiben erhalten. Historische `prior_note`- und Auditbeispiele wurden nicht umgeschrieben. Identische Georgia-/Tobias-Notizen bleiben bewusst zulässig, wenn die reale Beschaffungssituation gleich ist.

Der Validator prüft die Auditdateien strukturell, referenziell und anhand ihrer Statusübergänge. Historische Divergenz-, Notizkorrektur- und redaktionelle Vorher-/Nachher-Zeilen werden gegen ihren damaligen Stand geprüft und nicht mit späteren Live-Ratings oder -Notizen gleichgesetzt; aktive Recheck-Folgeentscheidungen bleiben an den aktuellen Stand gebunden. Der vollständige generische Prüfpfad verwendet nur `CONCEPT_ALPHA`: Er akzeptiert gleiche G/T-Notizen, eine neue belegte Nicht-Anker-Differenz nach historischer Angleichung und eine tatsächlich durch passende `PERSON_ROUTE`-Evidenz bestätigte persönliche Route. Unbekannte Evidenz-IDs, falscher Konzept-/Personen-Scope sowie unbelegte Händlerbesuchs-, Standortprüfungs- oder Wegfreigabebehauptungen werden abgelehnt.

## Wichtigste Grenzfälle

- `BELACAN` ist für beide DIFFICULT: Die belastbare Route bleibt ein enger Importweg mit geringer beziehungsweise unklarer Bestandsstabilität.
- `BERBERE` ist für beide PLANNED: Zwei voneinander unabhängige allgemeine deutsche Gewürzhändler tragen die haltbare Trockenform.
- `GOAT` ist für beide SPECIALTY: Feinkost Pum und Holler-Hof liefern unabhängig gekühlte beziehungsweise tiefgekühlte Zuschnitte; Kaufmela und DSW zeigen Ziegenfleisch zusätzlich als reguläre TK-Ware breiter afrikanischer beziehungsweise südasiatischer Spezialsortimente. Das belegt `SPECIALTY_BROAD`, aber keinen robusten allgemeinen deutschen Fleischhandel für `PLANNED`.
- `PURSLANE` ist für beide DIFFICULT: Der exakte Sommerportulak ist ausverkauft; Alternativen sind regional oder in der Form mehrdeutig.
- `CARP` ist für beide PLANNED: Ein aktuell positiver deutscher TK-Endkundenweg und eine unabhängige wiederkehrende Fischhandelslistung tragen die Stufe; Saison und Kühlannahme bleiben planungsrelevant.
- `CHERVIL` ist für beide PLANNED: fruits & friends ist passiv und zählt nicht positiv. Früchte-Kontor belegt die Bundware mit deutschlandweitem vorgekühltem Standard- oder getrenntem Kühlversand; REWE und OBI ergänzen allgemeine, standortabhängige Kettenwege. Saison und lokaler Bestand bleiben variabel.
- `DANABLU` ist für beide PLANNED: Der exakte Castello-Danablu ist im nationalen REWE-Katalog und bei einem unabhängigen Kühlversender gelistet.
- `FRESH_TURMERIC` bleibt für beide SPECIALTY: Mehrere unabhängige Asia-Frischerouten tragen die breite Spezialmarktpräsenz; allgemeine Listungen bleiben filialabhängig und erzwingen keine leichtere Stufe.
- `GAC_FRUIT` bleibt für beide DIFFICULT: Die korrigierte Evidenz belegt einen positiven formgenauen TK-Importweg, aber keine breite oder robuste Händlerlandschaft.
- `DULSE` bleibt für beide PLANNED: Marktplatzverkäufer Miraherba ist korrekt zugerechnet und eine unabhängige Naturkostroute belegt die planbare Marktpräsenz.
- `FRUIT_DUMPLING` ist für beide PLANNED: Das offene Konzept umfasst ausdrücklich Germknödel mit Fruchtfüllung. REWE listet einen tiefgekühlten Haushaltsartikel aus Hefeteig mit Powidl-/Pflaumenfüllung im nationalen Kettenkatalog; die standortabhängige TK-Verfügbarkeit verlangt Planung, aber keinen Nischenimport. Der weiterhin ausverkaufte MyBio-Marillenknödel ist nur eine engere Ausprägung.
- `COCONUT_VINEGAR` bleibt für beide SPECIALTY: Die Händler sind breit über mehrere asiatische Länderküchen und Warengruppen sortiert; unterschiedliche haltbare Kokosessigvarianten und gewöhnlicher Paketversand sprechen gegen einen einzelnen philippinischen Nischenkanal.
- `GALANGAL` bleibt für beide SPECIALTY: Zwei breite Asia-Vollsortimenter führen frisches Rhizom als reguläre 100-g-Gemüseware; Versandtage sowie Kühl-/Expressbedingungen bleiben echte, aber beherrschbare Frischegrenzen.
- `MORCILLA` bleibt für beide SPECIALTY: Ein nationaler spanischer Feinkostversender und ein breiter iberischer Supermarkt-/Versandhandel bilden unterschiedliche reguläre Kühlsortimentsformate; die Ware hängt nicht nur an einem produktspezifischen Importeur.
- `PRESERVED_LEMON` bleibt für beide SPECIALTY: Zwei breit sortierte Feinkosthändler führen verschiedene Marken ganzer Salzzitronen im haltbaren Glas über gewöhnlichen Paketversand und damit unabhängige Gourmet-Lieferketten.
- Die gleichartig beanstandeten Nicht-Anker `EGUSI_SEEDS`, `ENOKI`, `FERMENTED_TOFU`, `GREEN_PAPAYA`, `HIJIKI`, `KAFFIR_LIME_LEAVES`, `KASHMIRI_CHILI_POWDER`, `LAKSA_PASTE`, `LOTUS_ROOT`, `LOTUS_SEEDS`, `MASA_HARINA`, `MEMBRILLO` und `MOLE_PASTE` bleiben nach vollständiger qualitativer Nachprüfung SPECIALTY: Je Fall ist nun dokumentiert, welche breit sortierten Fachmärkte, unterschiedlichen Marken oder getrennten Vertriebswege die Kategorie tragen und welche Form-/Logistikgrenze den allgemeinen Handel weiterhin ausschließt.
- `LAMBIC` ist für beide SPECIALTY: Mehrere unabhängige Spezialbier-Routen tragen die Einordnung, eine allgemeine Route fehlt.
- `DUMPLING_DOUGH` ist für beide PLANNED: Grundlage ist ein ungeschnittener gekühlter Teig; das Zuschneiden zu Wrappern bleibt als transparente Forminferenz dokumentiert.

## Artefakte

Generator und Validator erzeugen beziehungsweise prüfen die getrennten Blindinputs, Personenentscheidungen, Personenreviews, den kombinierten Review, den Vergleich, die kanonische Evidenz sowie die versionierten Ausreißer- und Auditspuren. Die geschützten Cooking-Artefakte, früheren Availability-Freigaben und v2-Ankerentscheidungen wurden nicht verändert. Historische Auditzeilen bleiben als damalige Aussagen erhalten; aktuelle Evidenz und aktive Rechecks dokumentieren die Folgeentscheidungen. Negativ-Evidenz-Recheck und redaktionelle Vorher-/Nachher-Beispiele sind ergänzende Artefakte.

## Pflichtprüfungen

- `pwsh -File docs/analysis/generate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; 860 Zeilen, 853 anwendbare Konzepte, 7 Strukturknoten, 84 v2-Anker, 11 Personenunterschiede und 564/564 evidenzpflichtige Zuordnungen.
- `pwsh -File docs/analysis/validate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; alle Verteilungen, Anker, Evidenzgates, 1.706 Notizgates und Schutzprüfungen erfolgreich.
- `pwsh -File docs/analysis/validate-availability-novelty-cooking-review-20260903.ps1`: PASS; 853 anwendbare Konzepte und 39 freigegebene Cooking-Anker unverändert konsistent.
- `git diff --check`: PASS; vor dem Staging ohne Befund. `git diff --cached --check`: PASS nach dem expliziten Staging der 12 geänderten Lieferartefakte.
- `./mvnw verify`: BUILD SUCCESS; 482 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen. Surefire musste die bereits erfolgreich beendete Fork-JVM nach 30 Sekunden Shutdown-Wartezeit beenden; das Build blieb erfolgreich.

## Freigabegrenze

Diese Tranche liefert nur den Review- und Evidenzstand für die nächste menschliche Freigabe. Es erfolgt keine Übernahme in produktive Katalogwerte und keine weitere menschliche Freigabecharge im Rahmen dieses Pakets.
