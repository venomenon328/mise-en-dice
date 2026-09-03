# Beschaffbarkeit und Kochungewöhnlichkeit: Reviewtranche 1

Stand: 3. September 2026

Issue: #188, Tracking: #186

Status: **Wartet auf menschliche Freigabe der Referenzanker. Keine Vollbewertung begonnen.**

## 1. Eingefrorener Katalogstand

Maßgeblich ist der Repository-Katalog auf `main` am Commit
`e61b2358bc0ed240a8aac88caca1d012172a4c1c` (`Merge PR #192: add five-level availability`).
Der Arbeitsbranch `feat/188-availability-novelty-review` startete exakt auf diesem Commit.

Der Snapshot wurde aus einer leeren PostgreSQL-17.6-Datenbank erzeugt, auf die der vollständige explizite
Liquibase-Masterchangelog des Commits angewandt wurde. Er umfasst damit alle Katalogchangesets bis
`catalog/032-thailand-curation.sql`, die Kuratornotiz-Vervollständigung aus
`catalog/028-curator-note-completeness.sql` und die technische Fünfer-Skala aus
`schema/018-five-level-availability.sql`. Es wurde kein Produktionsdatenbankexport verwendet und keine
produktive Katalogzeile verändert.

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
| vorgeschlagene reine Strukturknoten ohne Personenwerte | 8 |

Aktuelle, ausdrücklich nur als Ausgangsdaten konservierte Verteilungen:

| Dimension | Verteilung |
|---|---|
| bisherige Kochungewöhnlichkeit | `1=341`, `2=243`, `3=136`, `4=98`, `5=42` |
| bisherige Beschaffbarkeit Georgia | `EASY=555`, `PLANNED=250`, `DIFFICULT=47` |
| bisherige Beschaffbarkeit Tobias | `EASY=525`, `PLANNED=196`, `DIFFICULT=131` |

`SPECIALTY` und `UNAVAILABLE` kommen im eingefrorenen Katalog noch nicht vor. Das ist kein Reviewurteil:
Issue #187 hat ausschließlich den technischen Wertebereich erweitert; neue produktive Einzelwerte gehören erst
in Issue #189.

## 2. Vollständiges Reviewledger

[`availability-novelty-review-ledger-20260903.csv`](availability-novelty-review-ledger-20260903.csv) enthält
jedes der 860 Konzepte genau einmal, kanonisch nach stabilem Konzeptcode sortiert. Pro Zeile sind festgehalten:

- Anzeigename, Aktiv-/Ziehstatus und Spezifität,
- bisherige Kochungewöhnlichkeit und beide bisherigen Personenwerte,
- bisheriges `base_draw_weight`,
- direkte Parent- und Child-Codes,
- vollständige Kuratornotiz,
- getrennte leere Vorschlags-, Begründungs-, Evidenz- und Freigabefelder für die spätere Reviewphase.

Für 852 fachlich bewertbare Konzepte sind sämtliche neuen Bewertungsfelder leer und der Status lautet
`WAITING_FOR_HUMAN_ANCHOR_APPROVAL`. Es wurde keine katalogweite Klassifikation vorgezogen.

Die acht nicht ziehbaren Konzepte `BAKED_GOODS`, `CONFECTIONERY`, `DAIRY_PRODUCTS`, `FRESH_HERBS`,
`PLANT_DRINKS`, `READY_CURRY_PASTE`, `READY_SAUCES_AND_PASTES` und `SPICES` besitzen im eingefrorenen Stand
bewusst keine Personenwerte. Sie sind sichtbar als `NOT_APPLICABLE_STRUCTURE` beziehungsweise
`PROPOSED_NOT_APPLICABLE` markiert; auch diese Einordnung ist noch nicht menschlich freigegeben.

Der read-only erzeugende SQL-Select liegt in
[`availability-novelty-review-ledger-export.sql`](availability-novelty-review-ledger-export.sql). Gegen eine
frisch migrierte Datenbank lässt sich der Stand beispielsweise mit `psql` reproduzieren. Dabei bezeichnet
`REVIEW_DATABASE_URL` eine `psql`-kompatible PostgreSQL-URI, keine JDBC-URL:

```text
psql "$REVIEW_DATABASE_URL" -X -q --csv -P footer=off \
  -f docs/analysis/availability-novelty-review-ledger-export.sql \
  -o docs/analysis/availability-novelty-review-ledger-20260903.csv
```

SHA-256 des eingefrorenen CSV-Ledgers:
`03cdbe526ad0dae2ae770583d21130876e996ce06699e2120aa65c2f304247fc`.

## 3. Vorgeschlagene Referenzanker

[`availability-novelty-reference-anchors-20260903.csv`](availability-novelty-reference-anchors-20260903.csv)
enthält 39 Vorschläge. Sie stehen auf `PROPOSED` beziehungsweise beim Strukturanker auf
`PROPOSED_NOT_APPLICABLE` und sind weder fachlich freigegeben noch in das Vollledger übertragen. Der Satz deckt
ab:

- alle vier geforderten Kombinationen aus leichter/schwerer Beschaffung und alltäglicher/ungewöhnlicher
  Verwendung,
- alle fünf Stufen der Kochungewöhnlichkeit sowie `NOT_APPLICABLE`; `LIQUORICE=5` kalibriert dabei bewusst eine
  leicht beschaffbare, aber als Kochzutat ausgefallene Süßware, während vermeintlich exotische konventionelle
  Zutaten häufig auf Stufe 3 oder 4 zurückfallen,
- alle fünf Beschaffbarkeitsstufen einschließlich weniger ausdrücklich offener `UNAVAILABLE`-Vorschläge,
- frische, trockene/haltbare, gekühlte und tiefgekühlte Formen,
- offene und spezifische Konzepte,
- europäische, ost-/südostasiatische, philippinische, türkisch/arabische und osteuropäische Beispiele.

Verteilung der vorgeschlagenen Werte:

| Dimension | Verteilung |
|---|---|
| Kochungewöhnlichkeit | `1=5`, `2=7`, `3=18`, `4=7`, `5=1`, `NOT_APPLICABLE=1` |
| Beschaffbarkeit Georgia | `EASY=14`, `PLANNED=8`, `SPECIALTY=8`, `DIFFICULT=6`, `UNAVAILABLE=2`, `NOT_APPLICABLE=1` |
| Beschaffbarkeit Tobias | `EASY=13`, `PLANNED=5`, `SPECIALTY=8`, `DIFFICULT=9`, `UNAVAILABLE=3`, `NOT_APPLICABLE=1` |

Die vier Beispiele der verbindlichen Spezifikation sind sichtbar aufgenommen: `ONION=1`, `BEER=4`,
`BAGOONG=3` und `SAFFRON=3`. Gerade `SAFFRON`, `LOBSTER`, `STOCKFISH`, `MAM_TOM` und
`FRESHWATER_SNAILS` kalibrieren die geforderte Entkopplung von Preis oder Beschaffung und
Kochungewöhnlichkeit.

Der Ankersatz enthält begrenzte aktuelle Händler-Evidenz nur für repräsentative Spezialfälle. Ein Listing zählt
nicht automatisch als zuverlässiger Bezugsweg; Form, Liefergebiet, Kühlkette und Ersatzproduktgefahr bleiben in
den getrennten Feldern sichtbar. CSV-SHA-256:
`90138a21e44345e87c0408655c9483ccf20b87eeb725aa5a76b7fa0daee1204b`.

## 4. Vor Freigabe zu entscheidende Grenzfälle

Die Spalte `open_boundary` nennt die konkrete Frage je betroffenem Anker. Besonders wichtig sind:

- **gültige Produktform:** ungesüßter Calamansi-Saft statt Getränk/Aroma, rohe oder ungesüßte Ube statt
  Pulver/Eiscreme sowie echtes Mẻ statt Cơm rượu oder Koji;
- **exakter Spezialartikel:** fischbasiertes `BAGOONG_ISDA`, Aligue, Mắm tôm, Tai Pla, ungesalzener Stockfisch
  sowie See- und Süßwasserschnecken dürfen nicht durch ähnliche Produkte belegt werden;
- **Kühl- und TK-Logistik:** Longganisa, Natto, Bananenblätter und Froschschenkel brauchen einen tatsächlich
  praktikablen Personenweg;
- **lokale Stufengrenzen:** bei Spezialläden im Rheinland sowie Tobias' großem Edeka ist zwischen `PLANNED`
  und `SPECIALTY` anhand wiederholter Erfahrung zu entscheiden;
- **Extremstufe 5:** `FRESHWATER_SNAILS`, `FISH_MINT` und `COM_ME` sind nur als offene
  `UNAVAILABLE`-Kalibrierung vorgeschlagen und benötigen vor Freigabe eine ausdrückliche Bestätigung;
- **Verwendungsgrenze:** bei Kaffee ist zu entscheiden, ob die breite zulässige Form noch Stufe 3 oder bereits
  Stufe 4 trägt; bei Lakritz ist die vorgeschlagene Extremstufe 5 ausdrücklich zu bestätigen.

## 5. Verbindlicher Haltepunkt

Vor einer Fortsetzung müssen die 39 Zeilen des Ankerartefakts ausdrücklich bestätigt, geändert oder als Anker
verworfen werden. Bis dahin beginnen weder Schritt 4 (katalogweite Kochungewöhnlichkeit) noch Schritt 5
(personengetrennte Beschaffbarkeit). Es gibt in dieser Tranche keine Gewichtsempfehlung, keine produktive
Katalogänderung, keine Liquibase-Datenmigration und keine Generatoranpassung.
