# Finale Challenge-Card-Mastertemplates

Die SVGs sind generatorverwaltete Mastertemplates für das feste quadratische Format `1200 × 1200 px`:

| Datei | Vorgaben | Regelzustand des Musters |
|---|---:|---|
| `challenge-card-master-2.svg` | 2 | Regel |
| `challenge-card-master-3.svg` | 3 | neutrales Ornament |
| `challenge-card-master-4.svg` | 4 | Regel |

Sie verwenden dieselbe Kopf- und Regelzone, die freigegebene Style-A-Palette und die Wireframe-Geometrie. Die SVGs referenzieren die finale Wortmarke ausschließlich als `../assets/brand/mise-en-dice-wordmark-master.png`; das Rasterasset darf nicht ersetzt oder in SVG-Pfade übertragen werden.

`generate_challenge_card_templates.py` ist die Quelle der drei Master-SVGs und der vier eingecheckten End-to-End-Referenzkarten. Die `CardSpec`-Eingaben enthalten normal geschriebene fachliche Anzeigetexte; die Ausgabe rendert die Vorgabennamen gemäß der freigegebenen Small-Caps-Rolle. Jede Slotgruppe besitzt einen festen Bildbereich, den optionalen `OFFEN`-Badge und eine Namenszone. Die abschließende, durchgezogene innere Kontur liegt `13 px` innerhalb der weichen äußeren Kontur.

## Mastertemplates und Referenzkarten erzeugen und prüfen

```bash
python design/challenge-cards/templates/generate_challenge_card_templates.py
python design/challenge-cards/templates/generate_challenge_card_templates.py --render
python design/challenge-cards/templates/generate_challenge_card_templates.py --check
python design/challenge-cards/templates/generate_challenge_card_templates.py --render-check
```

`--render` erzeugt die Review-PNGs zu den vier Referenzfällen. Es verwendet Chrome oder Edge im Headless-Modus für den 1200er SVG-Render und erzeugt die 320er Prüfung per LANCZOS-Downscale. Falls der Browser nicht im Standardpfad liegt, zeigt `CHALLENGE_CARD_BROWSER` auf die ausführbare Datei.

`--render-check` rendert die Referenzkarten in der **aktuellen lokalen Rendering-Umgebung** erneut und vergleicht sie bytegenau mit den eingecheckten PNGs. Das ist ein lokaler Regressionstest. Browser-, Betriebssystem- und Fontversionen gehören zur Rendering-Umgebung; zwischen unterschiedlichen Umgebungen wird keine Byteidentität behauptet.

## Konkrete Challenge aus externer CardSpec rendern

Für eine normale neue Challenge wird **nicht** `generate_challenge_card_templates.py` bearbeitet. Stattdessen wird eine externe JSON-Datei verwendet. [`card-spec.example.json`](card-spec.example.json) zeigt das Format.

```bash
python design/challenge-cards/templates/render_challenge_card_from_spec.py \
  --spec card.json \
  --output challenge-139.svg \
  --render
```

Mit `--render` entstehen neben dem SVG automatisch `challenge-139-1200.png` und `challenge-139-320.png`.

Eine CardSpec enthält:

- `challenge_number`: Ganzzahl von `0` bis `999`,
- `requirements`: exakt zwei, drei oder vier Einträge,
- je Vorgabe `display_name` und repository-relativer `asset`-Pfad,
- optional `open_concept: true`,
- optional `lines` mit maximal zwei kuratierten Anzeigezeilen,
- optional `rule_lines` mit maximal zwei Zeilen,
- optional `description`.

Der Renderer akzeptiert nur in `assets/ASSET_INDEX.csv` als `approved` geführte Produktionsassets unter `assets/ingredients/` beziehungsweise `assets/open-concepts/`. `display_name` und `open_concept` müssen zur dort freigegebenen Asset-Metadatenzeile passen; zusätzlich werden Existenz und `1024 × 1024 px`-RGBA-Format geprüft. Fehlende Assets werden vorher gemäß Illustration Guide erzeugt und freigegeben.

Die Templates und Renderer gehören nur zum versionierten Designsystem. Sie sind keine Bot-, Web- oder Laufzeitintegration.
