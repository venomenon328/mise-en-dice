# Challenge-Card-Design

Dieses Verzeichnis ist die verbindliche, versionierte Arbeitsgrundlage für die **Mise-en-Dice-Challenge-Cards**.

Die Karten werden bewusst **nicht durch den Discord-Bot erzeugt**. Sie entstehen außerhalb von Discord direkt mit ChatGPT auf Basis der hier abgelegten Spezifikation, Templates, Referenzen und freigegebenen Bildassets. GitHub ist die Source of Truth.

Maßgeblich sind die abgeschlossenen Designpakete zu Geometrie, visuellen Grundlagen und Nutztypografie, Issue #128 für das Illustrationssystem, Issue #130 für die feste Raster-Wortmarke sowie Issue #135 für Background-Master und finales Mastertemplate.

## Abgrenzung zum Anwendungsbuild

- Das Designverzeichnis liegt außerhalb von `src/` und ist kein Bestandteil des Maven-Artefakts.
- `design/` ist aus dem Docker-Build-Kontext ausgeschlossen.
- Laufzeitcode darf nicht stillschweigend von diesen Dateien abhängen.
- Der Discord-Bot erzeugt keine Challenge-Card-Bilder und ruft dafür kein Bildmodell auf.

## Struktur

```text
design/challenge-cards/
├── DESIGN_SPEC.md
├── WORKFLOW.md
├── wireframes/
├── style-studies/
├── typography-studies/
├── illustration-system/
├── assets/
│   ├── ASSET_INDEX.csv
│   ├── background/
│   ├── brand/
│   ├── ingredients/
│   ├── open-concepts/
│   ├── references/
│   └── review/
├── templates/
├── packages/
└── examples/
```

## Aktueller Stand

- Visuelle Grundrichtung: **Style Study A – Helles Honigbrett**.
- Background: [`assets/background/mise-en-dice-background-master.png`](assets/background/mise-en-dice-background-master.png) ist das feste `1200 × 1200 px` Rasterasset mit Verlauf, Küchenutensilien, Holzboard und leerem Regelbalken.
- Overlay-Geometrie: 2-/3-/4-Slot-Layouts sind an diesen Background-Master kalibriert; die frühere synthetische Wireframe-Boardfläche ist nur noch historische Vorstufe.
- Slots: nur noch eine sehr subtile helle Fläche mit einzelner warmer Kontur; keine doppelte Panelumrandung.
- Nutztypografie: **Typography Study A – Kitchen Editorial**; Vorgabennamen werden als Small Caps gerendert.
- Wortmarke: ausschließlich das unveränderliche Rasterasset [`assets/brand/mise-en-dice-wordmark-master.png`](assets/brand/mise-en-dice-wordmark-master.png).
- Illustrationsstil: durch `ILLUSTRATION_GUIDE.md` und die Anchor-Referenz definiert.
- Illustrationen: kleine 3-/4-Slots rendern Motive standardmäßig größer; bei Bedarf kann eine externe CardSpec `visual_scale` zur Feinjustierung des optischen Gewichts verwenden.
- Assetstrategie: **on demand** statt Vollbebilderung des 600+ Konzepte umfassenden Katalogs.
- Mastertemplate: generatorverwaltete 2-/3-/4-Vorgaben-Varianten plus End-to-End-Referenzkarten.

Für konkrete neue Challenges werden die Kartendaten als externe JSON-`CardSpec` an den Renderer unter [`templates/`](templates/) übergeben; der eingecheckte Generator wird dafür nicht verändert. Eine Bot- oder Laufzeitintegration gehört weiterhin nicht zum Designsystem.
