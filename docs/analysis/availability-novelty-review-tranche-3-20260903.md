# Beschaffbarkeit und Kochungewöhnlichkeit: Reviewtranche 3

Stand: 4. September 2026

Issue: #188, Tracking: #186

Status: **Die getrennten Beschaffbarkeitsdurchgänge für Georgia und Tobias sind vollständig vorgeschlagen. 815 Nicht-Anker warten auf menschliche Freigabe. Sechs personbezogene Änderungen an vier bereits freigegebenen Ankern sind gesondert ausgewiesen und benötigen erneut menschliche Freigabe. Es erfolgt keine produktive Katalogübernahme.**

## 1. Umfang und Trennung

Maßgeblich ist der gemeinsame Stand von `main` und Arbeitsbranch vor dieser Tranche am Commit
`f8855121af336a7c13cd799cafede5f9b9420f28`. Bewertet wurden alle 860 eingefrorenen Katalogcodes:
853 anwendbare Konzepte und sieben bereits freigegebene reine Strukturknoten.

Für jede Person wurde aus dem geblendeten Kataloginput eine eigene Arbeitsprojektion erzeugt. Sie enthält nur
Konzeptidentität, Taxonomie, Kuratornotiz, Anwendbarkeit, Produktformkontext und das Profil der jeweils bewerteten
Person. Insbesondere fehlen:

- bisherige Georgia-/Tobias-Beschaffbarkeit,
- Vorschlag oder Ergebnis der anderen Person,
- Kochungewöhnlichkeit und deren Reviewvorschlag,
- `base_draw_weight` und Generatorwerte.

Georgia und Tobias werden in zwei getrennten Funktionen bewertet. Erst danach entstehen gemeinsames Review und
Vergleich. Preis, Kochrolle und bloße Ähnlichkeit eines Ersatzprodukts sind keine Beschaffbarkeitskriterien.

`EASY` ist kein Restwert mehr. Für Georgias 572 und Tobias' 573 aktuelle `EASY`-Entscheidungen existiert jeweils
ein eigener positiver Auditdatensatz. Der Generator verlangt für jeden anwendbaren Code genau eine explizite
Bewertungsmenge und bricht bei fehlender oder mehrfacher Zuordnung ab.

## 2. Artefakte

- [`availability-novelty-availability-input-georgia-20260903.csv`](availability-novelty-availability-input-georgia-20260903.csv)
  und [`availability-novelty-availability-input-tobias-20260903.csv`](availability-novelty-availability-input-tobias-20260903.csv):
  personbezogene geblendete Arbeitsprojektionen,
- [`availability-novelty-availability-easy-decisions-20260903.csv`](availability-novelty-availability-easy-decisions-20260903.csv):
  1.145 explizite positive `EASY`-Entscheidungen mit Person, Code, Entscheidungsbasis und Auditstatus,
- [`availability-novelty-availability-review-georgia-20260903.tsv`](availability-novelty-availability-review-georgia-20260903.tsv)
  und [`availability-novelty-availability-review-tobias-20260903.tsv`](availability-novelty-availability-review-tobias-20260903.tsv):
  separat fixierte Vollreviews,
- [`availability-novelty-availability-review-20260903.tsv`](availability-novelty-availability-review-20260903.tsv):
  nachgelagertes gemeinsames Review,
- [`availability-novelty-availability-comparison-20260903.tsv`](availability-novelty-availability-comparison-20260903.tsv):
  nachgelagerter Personen- und Altstandsvergleich,
- [`availability-novelty-availability-evidence-20260903.csv`](availability-novelty-availability-evidence-20260903.csv):
  99 Evidenzeinträge mit exaktem Konzeptscope, Personenbezug, Evidenzrolle, unterstützter Bewertung, URL,
  Suchbegriffen, Befund und Einschränkungen,
- [`availability-novelty-availability-anchor-deltas-20260903.csv`](availability-novelty-availability-anchor-deltas-20260903.csv):
  gesonderte Wiedervorlage bereits freigegebener Anchorwerte,
- [`generate-availability-novelty-availability-review-20260903.ps1`](generate-availability-novelty-availability-review-20260903.ps1):
  reproduzierbare Erzeugung,
- [`validate-availability-novelty-availability-review-20260903.ps1`](validate-availability-novelty-availability-review-20260903.ps1):
  Vollständigkeits-, EASY-, Anker-, Evidenz-, Vergleichs- und Immutabilitätsprüfung.

Die Freeze-, Anker- und Cooking-Novelty-Artefakte aus Tranche 1 und 2 bleiben unverändert.

## 3. Availability-Verteilungen

| Availability | Georgia | Anteil | Tobias | Anteil |
|---|---:|---:|---:|---:|
| `EASY` | 572 | 67,1 % | 573 | 67,2 % |
| `PLANNED` | 218 | 25,6 % | 206 | 24,2 % |
| `SPECIALTY` | 50 | 5,9 % | 60 | 7,0 % |
| `DIFFICULT` | 11 | 1,3 % | 12 | 1,4 % |
| `UNAVAILABLE` | 2 | 0,2 % | 2 | 0,2 % |
| **Summe anwendbar** | **853** | **100,0 %** | **853** | **100,0 %** |
| `NOT_APPLICABLE_STRUCTURE` | 7 | – | 7 | – |

Gegenüber dem bisherigen Tranche-3-Entwurf wurden **120 fälschlich als Restwert entstandene
`EASY`-Personenwerte** korrigiert:

- Georgia: 60 Korrekturen, davon 57 zu `PLANNED`, zwei zu `SPECIALTY` und eine zu `DIFFICULT`,
- Tobias: 60 Korrekturen, davon 55 zu `PLANNED`, vier zu `SPECIALTY` und eine zu `DIFFICULT`.

Kein bisheriger Nicht-`EASY`-Wert wurde zum Ausgleich auf `EASY` gesetzt.

Die zweite Reviewnacharbeit korrigiert weitere **neun** unzureichend belegte `EASY`-Personenwerte zu `PLANNED`:

- für beide Personen `DUMPLING_WRAPPERS`, `YEAST_EXTRACT`, `FENUGREEK` und `WATERCRESS`,
- zusätzlich für Georgia `BIRDS_EYE_CHILI`; Tobias' spezifisch dokumentierte Rostocker Asia-Alltagsroute bleibt
  dagegen als positiver `EASY`-Beleg erhalten.

## 4. Personenunterschiede

Bei **23 von 853** anwendbaren Konzepten unterscheiden sich Georgia und Tobias. Jede Abweichung besitzt in beiden
Personendateien eine konkrete Profilbegründung.

Georgia liegt bei 17 Konzepten auf der leichteren Stufe:

- philippinische beziehungsweise asiatische Wege:
  `BAGOONG`, `BAGOONG_ALAMANG`, `BANANA_LEAVES`, `CURRY_LEAVES`, `GIO_LUA`,
  `LONGGANISA`, `MACAPUNO`, `NATTO`, `PLA_RA`, `SALTED_DUCK_EGG` und `UBE`,
- Rheinland- beziehungsweise Kühlversandnähe: `GARLIC_CHIVES`,
- stärkeres türkisch-/arabisches Bornheimer Umfeld:
  `DATE_SYRUP`, `HARISSA`, `PUL_BIBER`, `SUMAC` und `ZAATAR`.

Tobias liegt bei sechs Konzepten auf der leichteren Stufe:

- stärkeres übliches Rostocker Fischsortiment:
  `EEL`, `HADDOCK`, `NORTH_SEA_SHRIMP` und `SMOKED_TROUT`,
- stärkeres russisch-/osteuropäisches Sortiment:
  `TWAROG`,
- bestätigte persönliche Asia-Alltagsroute: `BIRDS_EYE_CHILI`.

Die gezielte Neuauditierung fügt gegenüber dem vorigen Entwurf Unterschiede bei `BAGOONG_ALAMANG`,
`FRANKFURT_GREEN_SAUCE` und `GARLIC_CHIVES` hinzu. Die bisherigen Unterschiede bei `CALAMANSI` und
`BAGOONG_ISDA` entfallen. Der zunächst angenommene Unterschied bei `WATER_SPINACH` entfällt ebenfalls, nachdem ein
belastbarer deutschlandweiter Expressweg bestätigt wurde. Die zweite Nacharbeit entfernt den künstlichen
Unterschied bei `FRANKFURT_GREEN_SAUCE`, weil derselbe deutsche Paketversand beiden offensteht, und ergänzt im
Gegenzug den durch Tobias' persönliche Route getragenen Unterschied bei `BIRDS_EYE_CHILI`. Die Gesamtzahl bleibt
dadurch bei 23.

## 5. Verpflichtende Korrekturen und Anchor-Deltas

Die im Review ausdrücklich benannten Korrekturen und die beim anschließenden fail-closed Evidenzrecheck besonders
hervorgetretenen Fälle sind:

- `LA_LOT_LEAVES`: Georgia `SPECIALTY`, Tobias `SPECIALTY`,
- `SEA_SNAILS`: Georgia `SPECIALTY`, Tobias `SPECIALTY`,
- `CALAMANSI`: Georgia `SPECIALTY`, Tobias `SPECIALTY`,
- `BAGOONG_ALAMANG`: Georgia `PLANNED`, Tobias `SPECIALTY`,
- zusätzlich nach dem fail-closed Evidenzrecheck `BAGOONG_ISDA`: Georgia `SPECIALTY`, Tobias `SPECIALTY`,
- `TOMATILLO`: Georgia `SPECIALTY`, Tobias `SPECIALTY`; eine aktuelle exakte Frischproduktseite ersetzt den
  bisherigen Formmismatch-Beleg,
- `WATER_SPINACH`: Georgia `SPECIALTY`, Tobias `SPECIALTY`; der genaue Frische-Expressweg ersetzt die vorherige
  pauschale Kühlkettenannahme für Tobias,
- `STOCKFISH`: Georgia `SPECIALTY`, Tobias `SPECIALTY`; zwei deutsche Endkundenwege mit haushaltsüblichen Mengen
  widerlegen die zuvor allein aus einem ausverkauften Norwegenshop abgeleitete `DIFFICULT`-Einstufung,
- `DAING`, `GREEN_RICE_FLAKES` und `MILKFISH`: jeweils Georgia und Tobias `DIFFICULT`, weil exakte Form
  beziehungsweise belastbare Kühlkette nicht bestätigt sind; die Einzelgrenzen sind in Abschnitt 7 ausgewiesen.

Von diesen Werten verändern sechs Personenentscheidungen bereits freigegebener Anchors:

| Konzept | Person | Freigegebener Wert | Neuer Vorschlag | Status |
|---|---|---|---|---|
| `BAGOONG_ISDA` | Tobias | `DIFFICULT` | `SPECIALTY` | `REQUIRES_HUMAN_REAPPROVAL` |
| `CALAMANSI` | Tobias | `DIFFICULT` | `SPECIALTY` | `REQUIRES_HUMAN_REAPPROVAL` |
| `SEA_SNAILS` | Georgia | `DIFFICULT` | `SPECIALTY` | `REQUIRES_HUMAN_REAPPROVAL` |
| `SEA_SNAILS` | Tobias | `DIFFICULT` | `SPECIALTY` | `REQUIRES_HUMAN_REAPPROVAL` |
| `STOCKFISH` | Georgia | `DIFFICULT` | `SPECIALTY` | `REQUIRES_HUMAN_REAPPROVAL` |
| `STOCKFISH` | Tobias | `DIFFICULT` | `SPECIALTY` | `REQUIRES_HUMAN_REAPPROVAL` |

Die freigegebenen Baselines bleiben in der Anchor-Datei unverändert. In den Reviewdateien tragen die vier
betroffenen Konzeptzeilen `PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL`. Diese Tranche erteilt keine Freigabe.

## 6. Fail-closed Evidenzzuordnung und Validatorgrenzen

Evidenz bleibt für `SPECIALTY`, `DIFFICULT` und `UNAVAILABLE` verpflichtend. Zusätzlich wird sie bei recherchierten
`PLANNED`-Grenzfällen und personenspezifischen Unterschieden erhalten. Jede vorhandene Zuordnung muss gleichzeitig
Konzeptcode, Person, Bewertung und erforderliche Rolle exakt abdecken:

- belegtes `PLANNED` und jedes `SPECIALTY` verlangen `EXACT_RETAIL`,
- `DIFFICULT` und `UNAVAILABLE` verlangen `DECISION_LIMITATION`,
- `CONTEXT_ONLY` darf niemals als Bewertungsbeleg referenziert werden.

Fehlt eine exakte Zuordnung, bricht der Generator ab. Es gibt keinen generischen Fallback. Der Validator prüft
außerdem, dass jede bewertungstragende Evidenz tatsächlich referenziert wird.

Korrigierte Fehlzuordnungen:

- `EV10` gilt nur noch als Kontext für `THAI_BASIL`; `HOLY_BASIL` verwendet die exakte Produktseite `EV38`.
- `EV18` belegt ausschließlich `OLOMOUC_TVARUZKY`; `KOLACHE` verwendet `EV66` und `PICKLED_SAUSAGE` `EV43`.
- Die generische Fleischkategorie `EV22` ist nur Kontext; `MOOSE`, `REINDEER`, `MORCILLA` und `SOBRASADA`
  verwenden `EV44` bis `EV47`.
- Der Handelskatalog `EV24` ist nur Kontext; `COCKLES`, `RAZOR_CLAMS` und `SEA_SNAILS` verwenden
  Endkundenbelege `EV48`, `EV49` und `EV40`.
- `EV31` belegt ausschließlich `VIETNAMESE_CORIANDER`; `LA_LOT_LEAVES` verwendet `EV39` und
  `RICE_PADDY_HERB` den negativen exakten Audit `EV64`.
- `EV37` gilt nur für den Formmismatch bei `NORWEGIAN_WAFFLE` und ist kein Fallback.
- Der generische Egusi-Händlerbeleg `EV14` wurde durch eine vorrätige Einzelproduktseite ersetzt.
- Der frühere norwegische Sammelbeleg `EV15` trägt nur noch `BRUNOST`; `FENALAR`, `FLATBROD`, `LEFSE`,
  `PINNEKJOTT`, `KLIPPFISH` und `ROD_POLSE` verwenden die scope-genauen Einzelproduktbelege `EV90` sowie
  `EV85` bis `EV89`.
- Veraltete oder tote Einzelproduktlinks für `OLOMOUC_TVARUZKY`, `VIETNAMESE_CORIANDER`, `PERILLA_LEAVES`,
  `SALTED_DUCK_EGG`, `FRANKFURT_GREEN_SAUCE` und `KLIPPFISH` wurden durch aktuelle exakte Endkundenwege ersetzt.
- `BAGOONG_ISDA` für Tobias referenziert mit `EV68|EV80` zwei exakte deutsche Endkundenwege; die daraus folgende
  Anchoränderung bleibt trotzdem unfreigegeben.
- `EV59` belegt einen deutschlandweiten Expressweg für exakten frischen `WATER_SPINACH`; der Haftungsausschluss
  eines zweiten Standardversands in `EV69` bleibt nur Kontext und kann den exakten Weg nicht fail-open überstimmen.
- `EV05`, `EV26`, `EV53` und `EV64` dokumentieren nun jeweils den konkreten ausverkauften Artikel, die konkrete
  Fehlform oder die konkrete nicht als Lebensmittel ausgewiesene Route statt einer pauschalen Kategoriesuche.
- `EV34` bleibt auf den weiter unbelegten frischen `POBLANO` begrenzt; `TOMATILLO` verwendet mit `EV91` eine
  vorrätige frische Einzelproduktseite samt deutschlandweitem Versand.
- `STOCKFISH` verwendet mit `EV92` und `EV94` zwei exakte deutsche Endkundenwege; der bisherige ausverkaufte
  Norwegenshop `EV05` und die zusätzliche Händlerkategorie `EV93` bleiben nur als Kontext sichtbar.
- Die recherchierten `PLANNED`-Wege für `CURRY_LEAVES`, `BAGOONG_ALAMANG` und
  `FRANKFURT_GREEN_SAUCE` bleiben jetzt mit `EV12`, `EV42` und `EV72` in beiden Personendateien erhalten.
- `EV95` bis `EV99` dokumentieren die nachgeschärften Grenzen für echte TK-Dumpling-Wrapper, Hefeextrakt,
  Bockshornkleesamen, Brunnenkresse und Georgias frische Bird-Eye-Chili.

Der Validator prüft keine konkrete redaktionelle Verteilung, Personenunterschiedszahl, Änderungszahl,
Evidenzmenge, Anchor-Delta-Liste oder Zutaten-Zielwerte mehr. Er berechnet diese Kennzahlen ausschließlich aus den
aktuellen Reviewartefakten und gibt sie als Bericht aus. Hart bleiben nur Vollständigkeit, Eindeutigkeit, zulässige
Stufen, explizite `EASY`-Entscheidungen, Statuslogik, die datengetriebene Baseline-/Delta-Mechanik,
Evidence-Scope/Person/Rolle und die Unverändertheit der geschützten Vorartefakte.

## 7. Grenzfälle

Die `DIFFICULT`-Mengen sind:

- Georgia: `ALIGUE`, `DAING`, `DUMPLING_DOUGH`, `GREEN_RICE_FLAKES`, `LUTEFISK`, `MILKFISH`,
  `NIPA_PALM_VINEGAR`, `NORWEGIAN_WAFFLE`, `POBLANO`, `RICE_PADDY_HERB` und `TAI_PLA`,
- Tobias: dieselben 11 sowie `UBE`.

Für beide `UNAVAILABLE` sind `COM_ME` und `RAKFISK`.

Verbleibende bewusst konservative beziehungsweise stichtagsabhängige Fälle:

- `DUMPLING_DOUGH`: bestellbar sind zugeschnittene Wrapper, nicht die geforderte ungeschnittene Teigform.
- `GREEN_RICE_FLAKES`: das deutsche Produkt enthält Pandanextrakt und ist nicht als Form aus jungen grünen
  Reiskörnern bestätigt.
- `DAING`: lieferbare Treffer sind allgemeiner getrockneter Fisch oder abweichend marinierter Bangus.
- `MILKFISH`: ein exaktes TK-Produkt ist gelistet, aber die durchgehende Tiefkühlzustellung wird nicht garantiert.
- `CORIANDER_ROOT`: exakte Produktseite und technische Bestellbarkeit sind vorhanden, die sichtbare
  Bestandsdarstellung ist widersprüchlich.
- `PEA_EGGPLANT`: eine exakte frische `Solanum torvum`-Verbraucherroute ist bestätigt, aber derzeit nur über einen
  Anbieter; deshalb `SPECIALTY`.
- `SEA_SNAILS`: der exakte Endkundenweg ist eine gewürzte Wellhornschnecken-Konserve; die offene Produktform
  erlaubt sie, die Anchoränderung bleibt dennoch freigabepflichtig.
- `BAGOONG_ISDA`: Zwei deutsche Shops belegen die exakte haltbare Fischform. Tobias wird deshalb als `SPECIALTY`
  vorgeschlagen; der bisherige `DIFFICULT`-Anchor bleibt bis zur menschlichen Re-Freigabe die Baseline.
- `UBE`: die unterschiedliche Einstufung beruht auf den ausdrücklich getrennten persönlichen Routen und den für
  Tobias belegten Formmismatches.
- `WATER_SPINACH`: der genaue Expressweg trägt nun für beide Personen `SPECIALTY`; Frischebestand und Zustelltag
  bleiben vor einer konkreten Bestellung zu prüfen.
- `TOMATILLO`: der konkrete Frischeweg trägt nun für beide Personen `SPECIALTY`; zwei bis sechs Werktage Versand
  und schwankende Frischequalität bleiben als Bestellrisiko sichtbar.
- `STOCKFISH`: zwei unabhängige deutsche Haushaltswege tragen nun für beide `SPECIALTY`; die sechs Personendeltas
  einschließlich dieses Anchorpaars bleiben bis zur ausdrücklichen menschlichen Re-Freigabe Vorschläge.

## 8. Vergleich, Status und Abgrenzung

| Vergleich zum eingefrorenen Altstand | Georgia | Tobias |
|---|---:|---:|
| unverändert | 683 | 620 |
| auf leichtere Stufe korrigiert | 96 | 191 |
| auf schwierigere Stufe korrigiert | 73 | 41 |
| zuvor nicht gepflegt, jetzt bewertet | 1 | 1 |
| **Änderungen gesamt** | **170** | **233** |

Freigabestatus im kombinierten Review:

- 815 Nicht-Anker: `PROPOSED_FOR_HUMAN_REVIEW`,
- 34 unveränderte numerische Anchorzeilen: `APPROVED_REFERENCE_ANCHOR`,
- vier Konzeptzeilen mit insgesamt sechs Personen-Deltas: `PROPOSED_ANCHOR_DELTA_FOR_HUMAN_REAPPROVAL`,
- sieben Strukturknoten: `APPROVED_NOT_APPLICABLE`.

Diese Tranche ändert keine produktiven Katalog-, Migrations-, Schema-, Java-, UI-, Runtime- oder Gewichtswerte.
Sie zieht weder #189 noch #190 vor. Die vorhandenen Cooking-Novelty-Artefakte bleiben bytegenau unverändert und
werden vom Availability-Validator zusätzlich über zeilenendenkanonische SHA-256-Prüfsummen geschützt.
