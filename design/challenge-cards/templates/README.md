# Finale Challenge-Card-Mastertemplates

Die SVGs sind editierbare, generatorverwaltete Mastertemplates für das feste quadratische Format `1200 × 1200 px`:

| Datei | Vorgaben | Regelzustand des Musters |
|---|---:|---|
| `challenge-card-master-2.svg` | 2 | Regel |
| `challenge-card-master-3.svg` | 3 | neutrales Ornament |
| `challenge-card-master-4.svg` | 4 | Regel |

Sie verwenden dieselbe Kopf- und Regelzone, die freigegebene Style-A-Palette und die Wireframe-Geometrie. Die SVGs referenzieren die finale Wortmarke ausschließlich als `../assets/brand/mise-en-dice-wordmark-master.png`; das Rasterasset darf nicht ersetzt oder in SVG-Pfade übertragen werden.

`generate_challenge_card_templates.py` ist die Quelle der drei SVGs und der End-to-End-Referenzkarten. Die `CardSpec`-Eingaben enthalten bewusst normal geschriebene fachliche Anzeigetexte; die Ausgabe rendert sie über `Go Smallcaps` als Small Caps. Jede Slotgruppe besitzt eine feste Bildreferenz, einen Bildbereich, den optionalen `OFFEN`-Badge und eine Namenszone. Die abschließende, durchgezogene innere Kontur liegt `13 px` innerhalb der weichen äußeren Kontur.

## Erzeugen und prüfen

```bash
python design/challenge-cards/templates/generate_challenge_card_templates.py
python design/challenge-cards/templates/generate_challenge_card_templates.py --render
python design/challenge-cards/templates/generate_challenge_card_templates.py --check
python design/challenge-cards/templates/generate_challenge_card_templates.py --render-check
```

`--render` erzeugt die Review-PNGs zu den vier Referenzfällen. Es verwendet Chrome oder Edge im Headless-Modus für den verlustfreien 1200er SVG-Render und erzeugt den verpflichtenden 320er Review per LANCZOS-Downscale. Falls der Browser nicht im Standardpfad liegt, zeigt `CHALLENGE_CARD_BROWSER` auf die ausführbare Datei. `--render-check` rendert frisch und vergleicht die PNGs bytegenau.

Die Templates und dieser Generator gehören nur zum versionierten Designsystem. Sie sind keine Bot-, Web- oder Laufzeitintegration.
