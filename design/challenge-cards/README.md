# Challenge-Card-Design

Dieses Verzeichnis ist die verbindliche, versionierte Arbeitsgrundlage für die **Mise-en-Dice-Challenge-Cards**.

Die Karten werden bewusst **nicht durch den Discord-Bot erzeugt**. Sie entstehen außerhalb von Discord direkt mit ChatGPT auf Basis der hier abgelegten Spezifikation, Templates, Referenzen und freigegebenen Bildassets. GitHub ist die Source of Truth.

Maßgeblich sind die abgeschlossenen Designpakete zu Geometrie, visuellen Grundlagen und Typografie, Issue #128 für das Illustrationssystem sowie Issue #135 für das Mastertemplate.

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
├── wordmark-studies/
├── illustration-system/
│   ├── ILLUSTRATION_GUIDE.md
│   ├── CONFUSABLES.md
│   └── PROMPT_TEMPLATES.md
├── assets/
│   ├── ASSET_INDEX.csv
│   ├── ingredients/
│   ├── open-concepts/
│   ├── references/
│   └── review/
├── templates/
├── packages/
└── examples/
```

## Aktueller Stand

- Geometrie für zwei, drei und vier Vorgaben: freigegeben.
- Visuelle Richtung: **Style Study A – Helles Honigbrett**.
- Nutztypografie: **Typography Study A – Kitchen Editorial**; Vorgabennamen werden als echte Small Caps gerendert.
- Wortmarke: die finale Produktionswortmarke ist das unveränderliche Rasterasset [`assets/brand/mise-en-dice-wordmark-master.png`](assets/brand/mise-en-dice-wordmark-master.png). Die SVG-Studien aus Issue #130 bleiben ausschließlich Reviewhistorie.
- Illustrationsstil: durch `ILLUSTRATION_GUIDE.md` und die Anchor-Referenz definiert.
- Visuell ähnliche Konzepte: allgemeine Nachbarschaftsprüfung gemäß `CONFUSABLES.md`; die zuerst getesteten Pasten und Saucen sind nur Beispielmaterial.
- Assetstrategie: **on demand** statt Vollbebilderung des 600+ Konzepte umfassenden Katalogs.

Die deterministischen Mastertemplates, Referenzfälle und PNG-Reviewchecks liegen unter [`templates/`](templates/) beziehungsweise [`examples/`](examples/). Eine Bot- oder Laufzeitintegration gehört weiterhin nicht zum Designsystem.
