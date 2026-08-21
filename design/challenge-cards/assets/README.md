# Wiederverwendbare Challenge-Card-Assets

Dieses Verzeichnis enthält freigegebene Zutaten-/Konzeptillustrationen sowie die festen Rasterassets für Brand und Background.

Für Zutaten- und Konzeptillustrationen ist der [`Illustration Guide`](../illustration-system/ILLUSTRATION_GUIDE.md) verbindlich. Für die Wortmarke gelten die Regeln unter [`brand/README.md`](brand/README.md), für den Kartenhintergrund die Regeln unter [`background/README.md`](background/README.md).

## Struktur

```text
assets/
├── ASSET_INDEX.csv
├── background/
│   ├── README.md
│   └── mise-en-dice-background-master.png
├── brand/
│   ├── README.md
│   ├── mise-en-dice-wordmark-master.png
│   ├── mise-en-dice-wordmark-preview-1200.png
│   └── mise-en-dice-wordmark-preview-320.png
├── ingredients/
├── open-concepts/
├── references/
└── review/
```

## Feste Rasterassets

- Die finale `Mise en Dice`-Wortmarke ist ein unveränderliches PNG-Brand-Asset.
- `background/mise-en-dice-background-master.png` ist der unveränderliche `1200 × 1200 px` Kartenhintergrund mit Verlauf, Küchengeräte-Silhouetten, Holzboard und leerem Regelbalken.
- Beide werden nicht pro Challenge neu generiert oder gestalterisch verändert.
- Brand- und Background-Dateien werden **nicht** in `ASSET_INDEX.csv` geführt.

## Illustrationsassets

- keine einmaligen Komplettkarten,
- PNG mit transparentem Hintergrund für Produktionsassets,
- kein Text, Badge oder Kartenhintergrund innerhalb der Illustration,
- vorhandene freigegebene Assets werden wiederverwendet,
- unterschiedliche Konzepte teilen niemals absichtlich dasselbe Asset,
- neue Assets werden erst nach Stil-, Kleinformat- und Nachbarschaftsprüfung freigegeben,
- der Katalog mit 600+ Konzepten wird **nicht** vollständig vorab bebildert.

`ASSET_INDEX.csv` enthält ausschließlich freigegebene Zutaten- und Konzept-Produktionsassets. Brand-, Background- und Referenzdateien gehören nicht hinein.

## Produktionsinventar und Identität

`ASSET_INDEX.csv` ist das einzige Produktionsinventar. Seine exakten Spalten
sind `concept_key`, `display_name`, `asset_path`, `asset_kind`, `status` und
`notes`; jede Produktionszeile hat `status=approved`.

Die logische Assetidentität ist verbindlich `(concept_key, asset_kind)`.
`asset_kind` ist ausschließlich `ingredient` oder `open-concept`. Derselbe
`concept_key` darf daher genau einmal je Art vorkommen: etwa kann ein konkretes
`Brokkoli`-Asset neben `Brokkoli (offen)` existieren. Beide sind getrennte
Motive und niemals gegenseitige Treffer oder Fallbacks.

`concept_key` ist ein stabiler kleingeschriebener Bindestrich-Slug. Die
Produktionspfade sind exakt `assets/ingredients/<concept_key>.png` für
`ingredient` beziehungsweise `assets/open-concepts/<concept_key>.png` für
`open-concept`. Jede Datei unter diesen beiden Verzeichnissen ist genau einmal
im Index enthalten, als `1024 × 1024` RGBA-PNG mit sichtbarem Inhalt und
vollständig transparentem äußerstem Rand.

Vor jedem Assetproduktionscommit und für jede CI-Änderung an diesen Dateien
wird geprüft mit:

```bash
python design/challenge-cards/tools/validate_asset_catalog.py
```

Der vollständige Ablauf einschließlich on-demand Erzeugung, Ersatz und Merge
nach `main` steht im [`../WORKFLOW.md`](../WORKFLOW.md).
