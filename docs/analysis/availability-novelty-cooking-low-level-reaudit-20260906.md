# Kochungewöhnlichkeit: gezielte Low-Level-Reaudit

Stand: 6. September 2026

Issue: #188, Tracking: #186

Status: **Gezielte Reaudit abgeschlossen; sechs menschliche Korrekturen aus Charge 1 freigegeben, 48 weitere
Korrekturen bleiben Vorschläge. Beschaffbarkeit wurde nicht verändert.**

## 1. Anlass und Bewertungsregel

Die Reaudit präzisiert den Kochungewöhnlichkeits-Durchgang vom 3. September 2026. Bewertet wird, wie ungewöhnlich
**genau die konkrete Art, regionale beziehungsweise kulturelle Zutat oder Produktform als verpflichtende
Kochzutat** aus der gemeinsamen Perspektive zweier deutsch sozialisierter, experimentierfreudiger Hobbyköche mit
starkem ost- und südostasiatischem Interesse und philippinischem Einfluss ist.

Die Küchentisch-Kontrolle entfernt ausschließlich Beschaffbarkeit, Preis, Marktbreite und persönlichen Vorrat. Eine
vertraute Elternkategorie, bekannte Kochrolle oder breite technische Verarbeitbarkeit vererbt keine niedrige Stufe.
Das konkrete Produkt selbst muss für Stufe 1 oder 2 gemeinsam als Kochzutat vertraut sein.

## 2. Umfang und Reproduzierbarkeit

Ausgangspunkt ist der Reviewstand am Commit `5cb9e82ec9e2d367a996c945750edf9c3bce7559`.

- Alle **679** bisherigen N1/N2-Konzepte wurden gezielt neu geprüft.
- Zusätzlich wurden alle **137** bisherigen N3-Konzepte als angrenzende Kontrollgruppe geprüft.
- Insgesamt umfasst die Reaudit damit **816** Konzepte; höhere Stufen wurden nicht katalogweit neu aufgerollt.
- Alle 39 freigegebenen Referenzanker, davon 38 numerische Anker und ein Strukturanker, blieben unverändert.

Artefakte:

- [`availability-novelty-cooking-low-level-reaudit-20260906.tsv`](availability-novelty-cooking-low-level-reaudit-20260906.tsv)
  protokolliert für jedes geprüfte Konzept Ausgangswert, Reaudit-Wert, Ergebnis, Begründung und Freigabestatus.
- [`generate-availability-novelty-cooking-low-level-reaudit-20260906.ps1`](generate-availability-novelty-cooking-low-level-reaudit-20260906.ps1)
  wendet die redaktionellen Entscheidungen deterministisch auf Vollreview und Vergleich an.
- [`validate-availability-novelty-cooking-review-20260903.ps1`](validate-availability-novelty-cooking-review-20260903.ps1)
  prüft Katalogvollständigkeit, Anker, Auditspur, Freigabestatus, Vergleich und abgeleitete Ausreißer.

## 3. Korrekturen

Insgesamt wurden **54** Novelty-Werte korrigiert.

### 1 → 2 (18)

`FERMENTED_CUCUMBER`, `GHEE`, `GLASS_NOODLES`, `LASAGNE_SHEETS`, `LIGHT_SOY_SAUCE`, `NORI`, `PANKO`,
`RAMEN_NOODLES`, `RISOTTO_RICE`, `SESAME_OIL`, `SILKEN_TOFU`, `SOBA`, `SRIRACHA`, `SUSHI_RICE`, `TAMARI`,
`UDON`, `WAKAME`, `WONTON_WRAPPERS`.

Diese konkreten Formen sind gemeinsam vertraut und besitzen mehrere naheliegende Kochrollen, sind aber nicht breit
genug für die Standardverwendung des exakten Konzepts auf Stufe 1.

### 2 → 3 (35)

`COCKLES`, `CUTTLEFISH`, `DANABLU`, `DANBO`, `DUCK_EGG`, `FLATBROD`, `GARLIC_CHIVES`, `HERVE_CHEESE`,
`JERK_SEASONING`, `KAFFIR_LIME_LEAVES`, `KASHMIRI_CHILI_POWDER`, `LOTUS_ROOT`, `MASA_HARINA`, `MILKFISH`,
`PICKLED_GINGER`, `PIMENT_D_ESPELETTE`, `PIQUILLO_PEPPER`, `POBLANO`, `PRESERVED_LEMON`, `PURSLANE`, `QUAIL`,
`QUAIL_EGG`, `RAZOR_CLAMS`, `RICE_CAKES`, `ROMESCO`, `ROOKWORST`, `SALMON_ROE`, `SAMBAL_BRANDAL`, `TARO`,
`TOMATILLO`, `TROUT_ROE`, `TWAROG`, `VIETNAMESE_CORIANDER`, `WATER_SPINACH`, `YAM`.

Hier hatten vertraute Parents, ähnliche Standardformen oder technisch breite Zubereitungsmöglichkeiten die konkrete
Art, regionale beziehungsweise kulturelle Zutat oder Produktform zu stark auf Stufe 2 gezogen.

### 3 → 4 (1)

`CHOCOLATE_HAGELSLAG`.

Die konkrete fertige Streusel- und Belagsform besitzt als verpflichtende Kochzutat eine deutlich engere Rolle als
allgemeine Schokolade oder Backstreusel.

## 4. Menschliche Korrekturen aus Charge 1

Folgende sechs Entscheidungen sind mit `APPROVED_HUMAN_CORRECTION_CHARGE_1` freigegeben und jeweils auf **3**
gesetzt:

- `DUCK_EGG`
- `HERVE_CHEESE`
- `MILKFISH`
- `PURSLANE`
- `RAZOR_CLAMS`
- `ROOKWORST`

Die übrigen 48 Korrekturen bleiben `PROPOSED_FOR_HUMAN_REVIEW`. Die Freigabe der Referenzanker wurde nicht
verändert.

## 5. Ergebnisbild und verbleibende Grenzfälle

| Kochungewöhnlichkeit | Konzepte nach Reaudit |
|---:|---:|
| 1 | 327 |
| 2 | 317 |
| 3 | 171 |
| 4 | 36 |
| 5 | 2 |
| **Summe anwendbar** | **853** |

Auffällige, bewusst nicht automatisch geänderte Grenzfälle sind insbesondere:

- `BLACK_VINEGAR`, `BONITO_FLAKES`, `GOCHUGARU` und `GALANGAL` auf 2: Der gemeinsame Asien-Schwerpunkt kann die
  konkrete Vertrautheit tragen, die Einordnung bleibt aber stark vom tatsächlich gemeinsamen Kochrepertoire
  abhängig.
- `CROISSANT`, `MEMBRILLO`, `RILLETTES` und `SOUR_RYE_STARTER` auf 3: Ihre konkrete fertige oder enge Form könnte
  als verpflichtende Kochzutat auch Stufe 4 begründen.
- `NDUJA`, `SURIMI` und `YEAST_EXTRACT` auf 2: Mehrere etablierte Rollen sind vorhanden, die konkrete Produktform
  bleibt jedoch deutlich richtungsgebender als ihre Parents.

Diese Fälle sind sichtbare menschliche Entscheidungsgrenzen, keine Validatorfehler. Die Reaudit enthält keine
Änderung an Georgia-/Tobias-Beschaffbarkeit, Beschaffbarkeitsnotizen, Produktivkatalog, Migrationen,
`base_draw_weight` oder Generatorparametern.
