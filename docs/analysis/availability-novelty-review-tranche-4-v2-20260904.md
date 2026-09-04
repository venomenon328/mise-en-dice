# Availability-Neuaudit v2 – Tranche 4

Stand: 2026-09-05

Issue: #188, eingeordnet unter Tracking-Issue #186
Bewertungsumfang: 853 anwendbare Konzepte × Georgia und Tobias = 1.706 Personenentscheidungen

## Umfang und Bewertungsbasis

Dieser Audit ist eine vollständige, getrennte Neubewertung der Availability. Maßgeblich waren ausschließlich die im Issue menschlich freigegebene v2-Semantik, die 84 v2-Anker (davon 83 bewertete Konzepte) und neue beziehungsweise im Audit erneut geprüfte markt-, produktform- und personenspezifische Evidenz. Der Vorschlagsstand vom 2026-09-03 wurde erst nach Abschluss beider unabhängiger Personenpässe für den Vergleich geladen; seine Werte und frühere Vorschläge waren kein Bewertungsinput.

Cooking Novelty, produktive Katalogwerte, Gewichte, Migrationen und Generatorparameter bleiben unverändert. Inhalte aus #189 und #190 wurden nicht vorgezogen.

## Ergebnis je Person

| Person | EASY | PLANNED | SPECIALTY | DIFFICULT | UNAVAILABLE | Summe |
|---|---:|---:|---:|---:|---:|---:|
| Georgia, v2 | 423 | 267 | 96 | 65 | 2 | 853 |
| Georgia, Vorschlag 2026-09-03 | 572 | 218 | 50 | 11 | 2 | 853 |
| Georgia, Delta | -149 | +49 | +46 | +54 | 0 | 0 |
| Tobias, v2 | 423 | 261 | 97 | 70 | 2 | 853 |
| Tobias, Vorschlag 2026-09-03 | 573 | 206 | 60 | 12 | 2 | 853 |
| Tobias, Delta | -150 | +55 | +37 | +58 | 0 | 0 |

Georgia hat 290 Entscheidungen gegenüber dem letzten Vorschlagsstand geändert und 563 beibehalten. Tobias hat 291 geändert und 562 beibehalten.

| Übergang | Georgia | Tobias |
|---|---:|---:|
| DIFFICULT → PLANNED | 1 | 1 |
| EASY → PLANNED | 146 | 145 |
| EASY → SPECIALTY | 3 | 5 |
| PLANNED → DIFFICULT | 15 | 12 |
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

Die kanonische Evidenzdatei umfasst 581 Evidenzzeilen für 335 Konzepte. Davon enthalten 487 Zeilen eine URL; es gibt 338 unterschiedliche URLs auf 123 wörtlich unterschiedlichen Hosts. Die Rollen verteilen sich auf 94 `ANCHOR_APPROVAL`, 211 `EXACT_ROUTE`, 206 `MARKET_BREADTH`, 4 `PERSON_ROUTE` und 66 `ROUTE_LIMITATION`.

| Person | relevante Evidenzzeilen | abgedeckte Konzepte | mit positiver/variabler Route | evidenzpflichtige Entscheidungen | davon SPECIALTY/DIFFICULT/UNAVAILABLE |
|---|---:|---:|---:|---:|---:|
| Georgia | 443 | 326 | 318 | 295 | 163 |
| Tobias | 412 | 314 | 306 | 268 | 169 |

Alle 563 evidenzpflichtigen Personenzuordnungen sind abgedeckt. Acht Konzepte haben bewusst ausschließlich negative oder limitierende Evidenz: `COCKLES`, `CULANTRO`, `FENALAR`, `GAC_FRUIT`, `LUTEFISK`, `POBLANO`, `RAZOR_CLAMS` und `TOMATILLO`. Das betrifft 13 Evidenzzeilen und 16 Personenentscheidungen.

Zusätzliche gezielte Nachprüfungen schließen zwei zuvor erkannte Evidenzlücken:

- 21 SPECIALTY-Fälle wurden erneut auf breite einschlägige Spezialmarkt-Abdeckung geprüft: 17 bestätigt, 4 Konzepte und damit 8 Personenentscheidungen korrigiert.
- 29 PLANNED-Fälle wurden erneut gegen die stabile positive Zugangsroute geprüft: 25 bestätigt, 4 Konzepte und damit 8 Personenentscheidungen korrigiert; 30 neue direkte Evidenzzeilen wurden append-only ergänzt.

## Individuelle Notizqualität

Alle 1.706 Kernnotizen sind nichtleer, konzept- und produktformspezifisch sowie nach exakter Normalisierung global eindeutig. Der Validator schließt URL-Fragmente, verbotene technische Platzhalter und bloße katalogweit wiederholte Enum-Paraphrasen aus. Zusätzlich gelten person- und stufenweise Schranken für wiederholte Satzgerüste, normalisierte Klauseln und häufige Vierwortfragmente. Kein exaktes Satzgerüst tritt dreimal oder öfter je Person auf; keine normalisierte Klausel oder Rating-Paraphrase erreicht zehn Wiederholungen je Person.

Der Korrekturaudit enthält 72/72 verifizierte Notizkorrekturen für 64 Konzepte. Die ergänzenden Routen-Audits sind vollständig: 344/344 Produktformrouten, 122/122 Prüfungen der exakten Routenspezifität, 27/27 Root-URL-Prüfungen, 6/6 Route-Mismatch-Prüfungen, 7/7 Statusprüfungen und 178/178 ausgerichtete Divergenzempfehlungen.

## Wichtigste Grenzfälle

- `BELACAN` ist für beide DIFFICULT: Die belastbare Route bleibt ein enger Importweg mit geringer beziehungsweise unklarer Bestandsstabilität.
- `BERBERE` ist für beide PLANNED: Zwei voneinander unabhängige allgemeine deutsche Gewürzhändler tragen die haltbare Trockenform.
- `GOAT` ist für beide DIFFICULT: Eine Versandroute ist nicht als belastbare Kühlkette belegt, die zweite ist lokal auf Dresden begrenzt.
- `PURSLANE` ist für beide DIFFICULT: Der exakte Sommerportulak ist ausverkauft; Alternativen sind regional oder in der Form mehrdeutig.
- `CARP` ist für beide DIFFICULT: Große Fischrouten sind nicht verfügbar; die verbleibende Tiefkühlroute ist an Mindestbestellwert und regionale Lieferfenster gebunden.
- `CHERVIL` ist für beide DIFFICULT: Die belastbare exakte Frischform ist nur als lebende Gärtnereipflanze erreichbar; allgemeine Frischkraut-Routen sind nicht verfügbar oder variabel.
- `DANABLU` ist für beide DIFFICULT: Belastbar bleibt nur kleiner Bestand bei einem skandinavischen Kühlversender; die allgemeine Handelsroute ist variabel.
- `FRESH_TURMERIC` ist für beide SPECIALTY: Zwei unabhängige Asia-Frischerouten führen das exakte Rhizom positiv, aber ohne robuste allgemeine Handelsbreite.
- `LAMBIC` ist für beide SPECIALTY: Mehrere unabhängige Spezialbier-Routen tragen die Einordnung, eine allgemeine Route fehlt.
- `DUMPLING_DOUGH` ist für beide PLANNED: Grundlage ist ein ungeschnittener gekühlter Teig; das Zuschneiden zu Wrappern bleibt als transparente Forminferenz dokumentiert.

## Artefakte

Generator und Validator erzeugen beziehungsweise prüfen die getrennten Blindinputs, Personenentscheidungen, Personenreviews, den kombinierten Review, den Vergleich, die kanonische Evidenz sowie die versionierten Ausreißer- und Auditspuren. Die Nachprüfungen vom 2026-09-05 sind eigene append-only Artefakte; bestehende Auditspuren wurden nicht überschrieben.

## Pflichtprüfungen

- `pwsh -File docs/analysis/generate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; 860 Zeilen, 853 anwendbare Konzepte, 7 Strukturknoten, 84 v2-Anker, 11 Personenunterschiede und 563/563 evidenzpflichtige Zuordnungen.
- `pwsh -File docs/analysis/validate-availability-novelty-availability-review-v2-20260904.ps1`: PASS; alle Verteilungen, Anker, Evidenzgates, 1.706 Notizgates und Schutzprüfungen erfolgreich.
- `pwsh -File docs/analysis/validate-availability-novelty-cooking-review-20260903.ps1`: PASS; 853 anwendbare Konzepte und 39 freigegebene Cooking-Anker unverändert konsistent.
- `git diff --check`: PASS; vor dem Staging ohne Befund. `git diff --cached --check` ist nach dem expliziten Staging aller 22 Lieferartefakte ebenfalls ohne Befund.
- `./mvnw verify`: BUILD SUCCESS; 482 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen. Surefire musste die bereits erfolgreich beendete Fork-JVM nach 30 Sekunden Shutdown-Wartezeit beenden; das Build blieb erfolgreich.

## Freigabegrenze

Diese Tranche liefert nur den Review- und Evidenzstand für die nächste menschliche Freigabe. Es erfolgt keine Übernahme in produktive Katalogwerte und keine weitere menschliche Freigabecharge im Rahmen dieses Pakets.
