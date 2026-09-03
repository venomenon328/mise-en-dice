# Beschaffbarkeit und Kochungewöhnlichkeit: Reviewtranche 2

Stand: 3. September 2026

Issue: #188, Tracking: #186

Status: **Kochungewöhnlichkeits-Durchgang vollständig vorgeschlagen; 815 Nicht-Anker warten auf menschliche Freigabe. Schritt 5 wurde nicht begonnen.**

## 1. Umfang und Bewertungsdisziplin

Maßgeblich ist weiterhin der eingefrorene Repository-Katalog auf `main` am Commit
`e61b2358bc0ed240a8aac88caca1d012172a4c1c`. Diese Tranche setzt auf dem nach der menschlichen
Ankerfreigabe entstandenen Review-Branch-Commit `2639bb3e62a9c60128b1efa7687bc3ce4c3d88d5` auf.

Vor der Vollbewertung wurde aus dem eingefrorenen Ledger eine eigene Arbeitsprojektion erzeugt. Sie enthält
ausschließlich:

- stabilen Konzeptcode und Anzeigename,
- Challenge-Spezifität,
- direkte Parent-/Child-Codes,
- Kuratornotiz,
- ausdrücklich freigegebene Review-Anwendbarkeit.

Die Projektion enthält bewusst **weder** aktuelle Georgia-/Tobias-Beschaffbarkeit **noch**
`base_draw_weight` **noch** den bisherigen `novelty_level`. Die 39 menschlich freigegebenen Anker wurden als
verbindliche Kalibrierung übernommen. Alle übrigen Vorschläge wurden anhand der Verwendungsperspektive und der
Kontrollfrage bewertet, ob derselbe Wert bei einer bereits kostenlos in geeigneter Form vorhandenen Zutat gelten
würde.

Erst nach Abschluss und Fixierung aller 853 anwendbaren Vorschläge wurde der bisherige `novelty_level` wieder
zugeschaltet, um das getrennte Vergleichsartefakt und die Ausreißerprüfung zu erzeugen. Beschaffbarkeit und Gewicht
blieben auch in dieser Prüfung außerhalb des Bewertungsinputs.

## 2. Artefakte

- [`availability-novelty-cooking-input-20260903.csv`](availability-novelty-cooking-input-20260903.csv):
  ausgeblendete Arbeitsprojektion mit allen 860 Katalogcodes und ohne Alt-Novelty, Beschaffbarkeit oder Gewicht,
- [`availability-novelty-cooking-review-20260903.tsv`](availability-novelty-cooking-review-20260903.tsv):
  vollständiger Schritt-4-Arbeitsstand mit 853 Vorschlägen, sieben bestätigten Strukturknoten, Begründungen,
  Prüfhinweisen und Freigabestatus,
- [`availability-novelty-cooking-comparison-20260903.tsv`](availability-novelty-cooking-comparison-20260903.tsv):
  erst nach der Bewertung erzeugter Vergleich der 392 geänderten anwendbaren Konzepte zum eingefrorenen Altstand,
- [`validate-availability-novelty-cooking-review-20260903.ps1`](validate-availability-novelty-cooking-review-20260903.ps1):
  reproduzierbare Vollständigkeits-, Anker- und Artefaktkonsistenzprüfung.

Die Freeze-, Vorschlags- und Freigabeartefakte aus Tranche 1 bleiben unverändert als Auditspur erhalten.

## 3. Novelty-Verteilung

| Kochungewöhnlichkeit | Konzepte | Anteil der 853 anwendbaren Konzepte |
|---:|---:|---:|
| 1 – Standardverwendung | 345 | 40,4 % |
| 2 – Vertraute Verwendung | 334 | 39,2 % |
| 3 – Kontextgebundene Verwendung | 137 | 16,1 % |
| 4 – Ungewöhnliche Verwendung | 35 | 4,1 % |
| 5 – Ausgefallene Verwendung | 2 | 0,2 % |
| **Summe anwendbar** | **853** | **100,0 %** |
| `NOT_APPLICABLE_STRUCTURE` | 7 | – |

Die beiden Stufe-5-Konzepte sind `LIQUORICE` und dessen spezifischeres Child `SALTY_LIQUORICE`. Stufe 4 bleibt
auf Produkte mit enger Kochrolle konzentriert, insbesondere überwiegend als Getränk oder fertige Süß-/Beilage
konsumierte Produkte sowie wenige stark form- oder traditionsgebundene Fermente.

## 4. Vergleich zum Altstand

Von 853 anwendbaren Konzepten ändern sich **392** gegenüber dem eingefrorenen `novelty_level`; **461** bleiben
unverändert.

| Richtung / Größe | Anzahl |
|---|---:|
| Absenkungen | 293 |
| Erhöhungen | 99 |
| absolute Änderung um 1 | 294 |
| absolute Änderung um 2 | 88 |
| absolute Änderung um 3 | 10 |

Die vielen Absenkungen sind fachlich erwartbar: Der Altstand hatte seltene oder schwer zu beschaffende Arten und
Produkte häufig sehr hoch eingestuft. Im separaten Kochdurchgang zählen stattdessen etablierte Gar-, Würz- und
Gerichtsrollen. Dadurch liegen etwa Hummer, Austern, Morcheln, Schwertmuscheln und Forellenrogen trotz möglicher
Beschaffungs- oder Preisfragen bei vertrauter Verwendung.

Größte Erhöhungen (`+3`):

- `BEER` `1 → 4` und `PILSNER_LAGER` `1 → 4`: überwiegend Getränke mit vergleichsweise engen Kochrollen;
  `BEER` ist menschlich freigegebener Anker,
- `SPECULOOS` `1 → 4`: fertige Süßware, deren erkennbare Einbindung auf wenige Dessert-, Füllungs- und
  Krustenrollen begrenzt ist.

Größte Absenkungen (`-3`):

- `LOBSTER`, `MOREL`, `OYSTER`, `RAZOR_CLAMS` und `TROUT_ROE` `5 → 2`: selten oder teuer ist nicht dasselbe wie
  ungewöhnlich zu kochen; alle besitzen klassische Verwendungsmuster,
- `SOLE` `4 → 1`: klassischer milder Speisefisch mit sehr vertrauten Garmethoden,
- `VIETNAMESE_CORIANDER` `5 → 2`: in der gemeinsamen südostasiatischen Perspektive ein etabliertes Würzkraut.

Alle 98 großen Änderungen um mindestens zwei Stufen besitzen im Reviewartefakt eine kurze Begründung.

## 5. Konsistenz- und Grenzfallprüfung

Die Prüfung umfasste die Verteilung, alle 37 Stufe-4/5-Werte, alle großen Altwertänderungen, 861 direkte
Parent-/Child-Beziehungen sowie Familien mit auffällig gleichförmigen Werten. Sie erzeugt Hinweise und keine
automatische Fachwahrheit.

Wesentliche Entscheidungen und Grenzfälle:

- `READY_CURRY_PASTE` ist ausdrücklich anwendbar und liegt als breite, unmittelbar nutzbare Familie auf **2**.
  Richtungsstarke konkrete Pasten wie `LAKSA_PASTE`, `MASSAMAN_CURRY_PASTE`, `RENDANG_PASTE` und die drei
  Thai-Farbvarianten liegen auf **3**.
- `TEA`, `BLACK_TEA`, `GREEN_TEA`, `OOLONG_TEA` und `WHITE_TEA` liegen auf **4**, weil Tee überwiegend getrunken
  wird. `MATCHA` liegt auf **3**, da das gemahlene Blattmaterial eine breitere etablierte Back- und Dessertrolle hat.
- Die offene Familie `WAFFLES` liegt auf **3**; fertige regionale Waffeln sowie `STROOPWAFEL` liegen auf **4**.
  Gleiches Prinzip gilt für andere bereits weitgehend fertige Produkte wie `FRUIT_DUMPLING`, `KOLACHE` und
  `SPECULOOS`.
- `CHEESE` liegt wegen der sehr breiten vertrauten Auswahl auf **1**, `BRUNOST` wegen seiner überwiegenden
  Belagsrolle und des süß-karamelligen Profils auf **4**.
- `COCONUT_PRODUCTS` liegt als breite Formenwahl auf **2**; `COCONUT_WATER` und `NATA_DE_COCO` liegen wegen ihrer
  überwiegenden Getränke-/Dessertrolle auf **4**.
- `GRAPE` liegt als überwiegend roh konsumierte Frucht mit begrenzteren warmen Rollen auf **3**. Das getrocknete
  Child `RAISIN` liegt wegen seiner sehr breiten Standardrollen in Backwerk, Reis, Füllung, Sauce und Salat auf **1**.
- Alle 76 direkten Parent-/Child-Abstände von mindestens zwei Stufen wurden einzeln gesichtet und begründet. Die
  einzige homogene Familie mit mindestens acht direkten Kindern ist `CABBAGE_VEGETABLES`: zwölf vertraute
  Kohlkonzepte auf Stufe 1. Das Muster wurde fachlich geprüft und nicht automatisch vererbt.

## 6. Vollständigkeit und Freigabestatus

Die maschinelle Prüfung bestätigt:

- 860 eindeutige bekannte Katalogcodes,
- exakt sieben freigegebene `NOT_APPLICABLE_STRUCTURE`-Knoten,
- exakt 853 anwendbare Konzepte mit genau einem Vorschlag 1–5,
- keine unbekannten, fehlenden oder duplizierten Codes,
- alle 39 freigegebenen Anker unverändert,
- `READY_CURRY_PASTE` anwendbar und numerisch bewertet,
- Begründungen für sämtliche Stufe-4/5-Werte, großen Altwertänderungen und Parent-/Child-Ausreißer,
- Konsistenz zwischen Vollreview und dem erst nachträglich erzeugten Änderungsvergleich.

Freigabestatus des Arbeitsstands:

- 38 numerische Referenzanker: `APPROVED_ANCHOR`,
- sieben Strukturknoten: `APPROVED_NOT_APPLICABLE`,
- 815 neue Vollreview-Vorschläge: `PROPOSED_FOR_HUMAN_REVIEW`.

Diese Tranche enthält keine neue Georgia-/Tobias-Beschaffbarkeit, keine Beschaffbarkeitsnotizen, keine
`base_draw_weight`-Empfehlungen und keine produktiven Migrationen, Generator- oder Anwendungscodeänderungen.
