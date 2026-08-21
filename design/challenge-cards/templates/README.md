# Finale Challenge-Card-Mastertemplates

Die SVGs sind generatorverwaltete Mastertemplates für das feste quadratische Format `1200 × 1200 px`:

| Datei | Vorgaben | Regelzustand des Musters |
|---|---:|---|
| `challenge-card-master-2.svg` | 2 | Regel |
| `challenge-card-master-3.svg` | 3 | neutrales Ornament |
| `challenge-card-master-4.svg` | 4 | Regel |

## Hintergrund und Geometrie

Die Templates legen keine künstliche Boardfläche mehr über das Bild. Stattdessen wird [`../assets/background/mise-en-dice-background-master.png`](../assets/background/mise-en-dice-background-master.png) unverändert als `1200 × 1200 px` Background-Master eingebunden. Er enthält Verlauf, Küchenutensilien, Holzboard und leeren Regelbalken.

Die Slotgeometrie ist auf dieses Rasterasset kalibriert:

- zwei Slots: `130/630 × 400`, jeweils `440 × 500 px`,
- drei/vier Slots oben: `140/630 × 400`, jeweils `430 × 238 px`,
- untere Reihe: `Y = 665`; beim Dreierlayout zentriert bei `X = 385`, beim Viererlayout bei `X = 140/630`.

Slots bestehen nur noch aus einer sehr leichten hellen Fläche (`7 %`) und einer einzelnen warmen Kontur (`18 %`, `1,4 px`). Die frühere zweite innere Kontur entfällt vollständig.

Die kleinen Drei-/Vierer-Slots rendern die Illustrationen standardmäßig um `18 %` größer innerhalb der Motivzone. Eine externe CardSpec kann zusätzlich pro Vorgabe `visual_scale` von `0.75` bis `1.50` setzen, um das optische Gewicht unterschiedlicher freigegebener Assets auszugleichen.

Der dunkle Regelbalken ist Bestandteil des Background-Masters; dynamisch gerendert werden nur Regeltext/Symbol oder das neutrale No-Rule-Ornament.

## Kopfzone

Die finale Wortmarke wird als unverändertes Rasterasset bei `X = 324`, `Y = 30`, `552 × 129 px` gesetzt. `Challenge #NNN` steht zentriert auf Basislinie `Y = 203`. Diese Werte sind bewusst leicht größer und tiefer als in der vorherigen Reviewfassung.

## Vorgabennamen und `OFFEN`

- einzeilige Namen in Drei-/Vierer-Slots: `34 px`,
- einzeilige Namen in großen Zweier-Slots: `40 px`,
- zweizeilige Namen in Drei-/Vierer-Slots: `29 px`,
- zweizeilige Namen in großen Zweier-Slots: `33 px`.

Der `OFFEN`-Badge ist nicht an den Namensblock gekoppelt. Bei offenen Konzepten sitzt er fest oben links im Slot, jeweils `18 px` vom linken und oberen Rand entfernt, bei `104 × 27 px`. Dadurch bleibt der untere Slotbereich ausschließlich der Vorgabenbezeichnung vorbehalten.

## Mastertemplates und Referenzkarten erzeugen und prüfen

```bash
python design/challenge-cards/templates/generate_challenge_card_templates.py
python design/challenge-cards/templates/generate_challenge_card_templates.py --render
python design/challenge-cards/templates/generate_challenge_card_templates.py --check
python design/challenge-cards/templates/generate_challenge_card_templates.py --render-check
```

`--render` erzeugt die Review-PNGs zu den vier Referenzfällen. Es verwendet Chrome oder Edge im Headless-Modus für den 1200er SVG-Render und erzeugt die 320er Prüfung per LANCZOS-Downscale. Falls der Browser nicht im Standardpfad liegt, zeigt `CHALLENGE_CARD_BROWSER` auf die ausführbare Datei.

`--render-check` rendert in der **aktuellen Rendering-Umgebung** erneut und vergleicht bytegenau mit den eingecheckten PNGs. Browser-, Betriebssystem- und Fontversionen gehören zu dieser Umgebung; plattformübergreifende Byteidentität wird nicht behauptet.

## Konkrete Challenge aus externer CardSpec rendern

Für eine normale neue Challenge wird **nicht** `generate_challenge_card_templates.py` bearbeitet. Stattdessen wird eine externe JSON-Datei verwendet. [`card-spec.example.json`](card-spec.example.json) zeigt das Format.

```bash
python design/challenge-cards/templates/render_challenge_card_from_spec.py \
  --spec card.json \
  --output challenge-139.svg \
  --render
```

Eine CardSpec enthält:

- `challenge_number`: Ganzzahl von `0` bis `999`,
- `requirements`: exakt zwei, drei oder vier Einträge,
- je Vorgabe `display_name` und repository-relativer `asset`-Pfad,
- optional `open_concept: true`,
- optional `lines` mit maximal zwei kuratierten Anzeigezeilen,
- optional `visual_scale` zwischen `0.75` und `1.50`,
- optional `rule_lines` mit maximal zwei Zeilen,
- optional `description`.

Der Renderer akzeptiert nur in `assets/ASSET_INDEX.csv` als `approved` geführte Produktionsassets unter `assets/ingredients/` beziehungsweise `assets/open-concepts/`. `display_name` und `open_concept` müssen zur dort freigegebenen Asset-Metadatenzeile passen.

Die Templates und Renderer gehören nur zum versionierten Designsystem. Sie sind keine Bot-, Web- oder Laufzeitintegration.
