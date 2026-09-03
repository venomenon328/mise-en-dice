# Beschaffbarkeit und Kochungewöhnlichkeit: Reviewtranche 1

Stand: 3. September 2026

Issue: #188, Tracking: #186

Status: **Referenzanker menschlich freigegeben. Vollbewertung darf mit Schritt 4 fortgesetzt werden.**

## 1. Eingefrorener Katalogstand

Maßgeblich bleibt der Repository-Katalog auf `main` am Commit
`e61b2358bc0ed240a8aac88caca1d012172a4c1c` (`Merge PR #192: add five-level availability`).
Der Arbeitsbranch `feat/188-availability-novelty-review` startete exakt auf diesem Commit.

Der Snapshot wurde aus einer leeren PostgreSQL-17.6-Datenbank erzeugt, auf die der vollständige explizite
Liquibase-Masterchangelog des Commits angewandt wurde. Es wurde kein Produktionsdatenbankexport verwendet und
keine produktive Katalogzeile verändert.

| Merkmal | Umfang |
|---|---:|
| Zutatenkonzepte gesamt | 860 |
| aktiv | 860 |
| aktiv und zufällig ziehbar | 809 |
| davon offen / spezifisch | 81 / 728 |
| nicht zufällig ziehbar | 51 |
| offene / spezifische Konzepte gesamt | 116 / 744 |
| direkte Konkretisierungsbeziehungen | 983 |
| Konzepte mit Georgia-/Tobias-Wert | 852 / 852 |

Die ursprünglichen Verteilungen bleiben nur Ausgangsdaten. `SPECIALTY` und `UNAVAILABLE` kamen im eingefrorenen
Produktivkatalog noch nicht vor; sie werden erst durch den Review fachlich vergeben.

## 2. Auditspur der ersten Tranche

Die ursprünglich vorgeschlagenen Artefakte bleiben als Auditspur erhalten:

- [`availability-novelty-review-ledger-20260903.csv`](availability-novelty-review-ledger-20260903.csv):
  vollständiger eingefrorener Pre-Approval-Arbeitsstand mit 860 Konzepten,
- [`availability-novelty-reference-anchors-20260903.csv`](availability-novelty-reference-anchors-20260903.csv):
  die 39 ursprünglich vorgeschlagenen Referenzanker,
- [`availability-novelty-review-ledger-export.sql`](availability-novelty-review-ledger-export.sql):
  der ursprüngliche Pre-Approval-Export.

Diese Dateien dokumentieren bewusst, **was vor der menschlichen Freigabe vorgeschlagen war**. Sie werden nicht
nachträglich umgeschrieben, um die Reviewhistorie zu verwischen.

Ab jetzt sind zusätzlich maßgeblich:

- [`availability-novelty-reference-anchor-decisions-20260903.csv`](availability-novelty-reference-anchor-decisions-20260903.csv):
  menschlich freigegebene effektive Werte für alle 39 Anker,
- [`availability-novelty-structure-decisions-20260903.csv`](availability-novelty-structure-decisions-20260903.csv):
  explizite Anwendbarkeitsentscheidung für die acht zuvor als mögliche Strukturknoten identifizierten Konzepte,
- [`availability-novelty-review-ledger-export-approved.sql`](availability-novelty-review-ledger-export-approved.sql):
  Exportgrundlage für den post-approval Arbeitsstand ohne technische Ableitung von `NOT_APPLICABLE`.

## 3. Menschliche Freigabe der Referenzanker

Alle 39 Referenzanker wurden fachlich freigegeben. Die Freigabe umfasst insbesondere:

- `COFFEE` bleibt Kochungewöhnlichkeit **4**,
- `LIQUORICE` bleibt Kochungewöhnlichkeit **5**,
- bei `CALAMANSI` ist neben der frischen Frucht auch **ungesüßter sortenreiner Saft** eine zulässige Produktform,
- bei `UBE` sind **frisch, TK und ungesüßtes reines Püree** zulässige Produktformen,
- bei `FISH_MINT` darf eine **essbare lebende Houttuynia-cordata-Pflanze** als Beschaffungsweg zählen,
- die vorgeschlagenen persönlichen Beschaffbarkeiten für `GOCHUJANG`, `SUMAC`, `SAFFRON`, `BAGOONG`,
  `LONGGANISA`, `BANANA_LEAVES`, `HOLY_BASIL`, `THAI_EGGPLANT` und `NATTO` wurden bestätigt.

Die verbindlichen Trennungsanker bleiben damit unter anderem:

| Konzept | Kochungewöhnlichkeit | Georgia | Tobias |
|---|---:|---|---|
| `ONION` | 1 | `EASY` | `EASY` |
| `MISO` | 2 | `EASY` | `EASY` |
| `BAGOONG` | 3 | `PLANNED` | `SPECIALTY` |
| `SAFFRON` | 3 | `EASY` | `EASY` |
| `BEER` | 4 | `EASY` | `EASY` |
| `LIQUORICE` | 5 | `EASY` | `EASY` |

## 4. Nachrecherchierte Availability-Korrekturen

Vier Anker wurden vor der Freigabe anhand exakter aktueller Bezugswege korrigiert:

| Konzept | Georgia | Tobias | Korrektur |
|---|---|---|---|
| `MAM_TOM` | `SPECIALTY` | `SPECIALTY` | Tobias `DIFFICULT → SPECIALTY` |
| `FROG_LEGS` | `SPECIALTY` | `SPECIALTY` | beide `DIFFICULT → SPECIALTY` |
| `FRESHWATER_SNAILS` | `SPECIALTY` | `SPECIALTY` | beide `UNAVAILABLE → SPECIALTY` |
| `FISH_MINT` | `SPECIALTY` | `SPECIALTY` | Georgia `DIFFICULT → SPECIALTY`, Tobias `UNAVAILABLE → SPECIALTY` |

Begründung:

- echte Mắm-tôm-Garnelenpaste ist bei mehreren deutschen Händlern regulär in haushaltsüblicher Menge bestellbar,
- TK-Froschschenkel und TK-Apfelschneckenfleisch sind bei deutschem Spezialhandel mit isoliertem
  Tiefkühlversand erhältlich,
- essbare `Houttuynia cordata` wird als Pflanze im spezialisierten deutschen Pflanzenhandel angeboten und
  kann innerhalb des definierten Beschaffungshorizonts als frische Bezugsform dienen.

`STOCKFISH` bleibt nach gezielter Nachrecherche für beide **`DIFFICULT`**: exakter ungesalzener Stockfisch ist
im europäischen Spezialhandel grundsätzlich vorhanden, aktuell aber nicht zuverlässig lieferbar; viele scheinbare
Treffer sind tatsächlich gesalzener Klippfisch/Bacalhau und zählen nicht als Ersatzprodukt.

Die Quellen und die konkreten Overrides stehen maschinenlesbar in der Anchor-Decision-Datei.

Effektive Verteilung der 39 freigegebenen Anker:

| Dimension | Verteilung |
|---|---|
| Kochungewöhnlichkeit | `1=5`, `2=7`, `3=18`, `4=7`, `5=1`, `NOT_APPLICABLE=1` |
| Beschaffbarkeit Georgia | `EASY=14`, `PLANNED=8`, `SPECIALTY=11`, `DIFFICULT=4`, `UNAVAILABLE=1`, `NOT_APPLICABLE=1` |
| Beschaffbarkeit Tobias | `EASY=13`, `PLANNED=5`, `SPECIALTY=12`, `DIFFICULT=7`, `UNAVAILABLE=1`, `NOT_APPLICABLE=1` |

Damit bleiben alle fünf Beschaffbarkeitsstufen im Ankersatz vertreten; `COM_ME` kalibriert weiterhin die
Extremstufe `UNAVAILABLE`.

## 5. Explizite Strukturentscheidungen

`NOT_APPLICABLE` darf nicht aus `random_draw_enabled = false` oder fehlenden Availability-Zeilen abgeleitet
werden. Die menschliche Entscheidung lautet:

**Reine Strukturknoten / `NOT_APPLICABLE_STRUCTURE`:**

- `BAKED_GOODS`
- `CONFECTIONERY`
- `DAIRY_PRODUCTS`
- `FRESH_HERBS`
- `PLANT_DRINKS`
- `READY_SAUCES_AND_PASTES`
- `SPICES`

**Regulär fachlich zu bewerten:**

- `READY_CURRY_PASTE`

Der neue post-approval SQL-Export enthält diese Entscheidung deshalb als explizite CTE. Alle anderen Konzepte
sind standardmäßig `APPLICABLE`; technische Eigenschaften erzeugen keine redaktionelle Nichtanwendbarkeit.

## 6. Fortsetzung des Vollreviews

Der Haltepunkt aus Schritt 3 ist aufgehoben. Die nächste Tranche darf nun strikt nach Issue #188 fortfahren:

1. aus dem eingefrorenen Katalog mit dem post-approval Export den Arbeitsstand für die Vollbewertung erzeugen,
2. **Schritt 4:** alle anwendbaren Konzepte ausschließlich nach Kochungewöhnlichkeit bewerten,
3. danach **Schritt 5:** Beschaffbarkeit für Georgia und Tobias getrennt bewerten,
4. unsichere Spezialfälle gemäß Schritt 6 gezielt recherchieren,
5. keine Werte aus Beschaffbarkeit, bestehendem Gewicht oder Beschaffungsaufwand in die Kochungewöhnlichkeit
   hineinziehen,
6. keine produktiven Katalogwerte, Liquibase-Datenmigrationen oder Generatorparameter ändern.

Die sieben bestätigten Strukturknoten bleiben sichtbar nicht anwendbar. `READY_CURRY_PASTE` muss im Vollreview
wie jedes andere anwendbare Konzept drei reguläre Bewertungen erhalten.

## 7. Noch nicht Teil dieses Pakets

Es gibt weiterhin:

- keine produktive Katalogmigration,
- keine Änderung von `base_draw_weight`,
- keine Generator-Kalibrierung,
- keine Tests, die konkrete redaktionelle Einzelwerte als dauerhafte Fachwahrheit festschreiben.

Die menschliche Präferenz, schwierige Beschaffbarkeit künftig **noch stärker** im Generator abzuwerten und bereits
`PLANNED` mit deutlicher Vorsicht zu behandeln, wird getrennt in Issue #190 für die empirische Kalibrierung
festgehalten. #188 entscheidet keine numerischen Generatorfaktoren.
