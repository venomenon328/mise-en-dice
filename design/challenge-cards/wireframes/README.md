# Wireframes

Dieses Verzeichnis enthält die maßhaltigen Low-Fidelity-Wireframes für die quadratische Challenge-Card.

Die SVGs prüfen ausschließlich Informationshierarchie, Geometrie und Lesbarkeit. Graustufen, gestrichelte Slotgrenzen, Platzhaltermotive und die Arial-/Helvetica-Kaskade sind **keine** finale Gestaltung. Das soll hier absichtlich noch ein wenig nach Bauplan aussehen und nicht nach Sternerestaurant.

## Dateien

| Datei | Prüfzweck |
|---|---|
| [`challenge-card-2.svg`](challenge-card-2.svg) | zwei große gleichwertige konkrete Zutaten und kurze Ausschlussregel |
| [`challenge-card-3.svg`](challenge-card-3.svg) | `2 oben + 1 unten mittig`, offenes Konzept mit `OFFEN`-Badge |
| [`challenge-card-4.svg`](challenge-card-4.svg) | `2 × 2`, langer zweizeiliger Konzeptname und lange zweizeilige Regel |
| [`challenge-card-no-rule-text.svg`](challenge-card-no-rule-text.svg) | fehlende Zusatzregel mit neutraler Textbestätigung |
| [`challenge-card-no-rule-ornament.svg`](challenge-card-no-rule-ornament.svg) | fehlende Zusatzregel mit rein dekorativer, stabiler Zone |
| [`GEOMETRY.md`](GEOMETRY.md) | verbindliche Maße des aktuellen Vorschlags und Skalierungsprüfung |
| [`generate_wireframes.py`](generate_wireframes.py) | deterministische Erzeugung und Standardbibliotheks-Validierung der SVGs |

## Testinhalte

Die Beispiele sind bewusst keine vollständigen fachlichen Challenges. Sie reizen problematische Layoutfälle aus:

- kurze Namen: `TOFU`, `MANGO`, `MISO`,
- breiter Name: `SCHWEINEBAUCH`,
- offenes Konzept: `BLATTGEMÜSE`,
- langer zweizeiliger Name: `PFLANZLICHES PROTEINPRODUKT`,
- kurze Regel: `KEINE KOKOSMILCH`,
- lange Regel: `KEINE KOKOSMILCH ODER KOKOSCREME`.

## Erzeugen und prüfen

Die SVGs werden aus einer gemeinsamen Geometrie erzeugt. Dafür ist nur Python 3 erforderlich:

```bash
python design/challenge-cards/wireframes/generate_wireframes.py
python design/challenge-cards/wireframes/generate_wireframes.py --check
```

`--check` schlägt fehl, wenn eine generierte Datei fehlt, vom Generator abweicht, kein valides XML/SVG ist, nicht `1200 × 1200 px` verwendet oder die erwarteten strukturellen Bereiche fehlen.

Optional können die SVGs mit Inkscape rasterisiert werden:

```bash
inkscape design/challenge-cards/wireframes/challenge-card-4.svg \
  --export-type=png \
  --export-width=320 \
  --export-height=320
```

## Visuelle Abnahme

Vor dem Übergang zur Farb- und Typografiephase sind insbesondere zu entscheiden:

1. Wirkt die Kopfzone im Verhältnis zur Boardfläche richtig gewichtet?
2. Sind die 2er-, 3er- und 4er-Anordnungen ruhig und eindeutig gleichwertig?
3. Funktioniert der `OFFEN`-Badge zentriert oberhalb des Namens?
4. Sind der lange Konzeptname und die lange Regel bei kleinen Darstellungen noch vertretbar?
5. Soll eine Challenge ohne Zusatzregel die Text- oder Ornamentvariante verwenden?

Erst nach dieser Freigabe werden die Maße in das eigentliche Mastertemplate übernommen.
