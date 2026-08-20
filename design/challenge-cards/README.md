# Challenge-Card-Design

Dieses Verzeichnis ist die verbindliche, versionierte Arbeitsgrundlage für die **Mise-en-Dice-Challenge-Cards**.

Die Karten werden bewusst **nicht durch den Discord-Bot erzeugt**. Sie entstehen außerhalb von Discord direkt mit ChatGPT auf Basis der hier abgelegten Spezifikation, Templates, Referenzen und freigegebenen Bildassets. GitHub ist die Source of Truth; die ChatGPT File Library wird nicht benötigt.

Maßgeblich sind die abgeschlossenen Issues [#122](https://github.com/venomenon328/mise-en-dice/issues/122) und [#124](https://github.com/venomenon328/mise-en-dice/issues/124).

## Abgrenzung zum Anwendungsbuild

- Das Designverzeichnis liegt außerhalb von `src/` und ist kein Bestandteil des Maven-Artefakts.
- `design/` ist aus dem Docker-Build-Kontext ausgeschlossen.
- Laufzeitcode darf nicht stillschweigend von diesen Dateien abhängen.
- Eine spätere Bot-Integration wäre ein eigenes Produkt- und Architekturpaket. Der aktuelle Workflow sieht sie ausdrücklich nicht vor.

## Struktur

```text
design/challenge-cards/
├── README.md
├── DESIGN_SPEC.md
├── WORKFLOW.md
├── references/
│   ├── README.md
│   └── discord-bot-banner-reference.jpg
├── wireframes/
│   ├── README.md
│   ├── GEOMETRY.md
│   ├── generate_wireframes.py
│   └── *.svg
├── style-studies/
│   ├── README.md
│   ├── VISUAL_FOUNDATIONS.md
│   ├── generate_style_studies.py
│   ├── style-study-*.svg
│   └── renders/
│       └── compact/
├── packages/
│   ├── README.md
│   └── challenge-card-visual-foundations-v1.zip
├── templates/
│   └── README.md
├── assets/
│   ├── README.md
│   ├── ingredients/
│   └── open-concepts/
└── examples/
    └── README.md
```

## Verbindlichkeit

- [`DESIGN_SPEC.md`](DESIGN_SPEC.md) trennt beschlossene Regeln von offenen Folgeentscheidungen.
- Freigegebene Templates und Assets werden nicht bei jeder Challenge neu interpretiert oder ersetzt.
- Eine komplette Karte wird nicht frei durch ein Bildmodell komponiert. Layout, Logo, Texte und Abstände folgen einem festen Template.
- Neue Zutaten- oder Konzeptillustrationen werden bei Bedarf erzeugt, geprüft, hier versioniert und anschließend wiederverwendet.
- Der sichtbare Name einer Vorgabe bleibt verbindlich; die Illustration unterstützt ihn nur.

## Aktueller Stand

- Die Geometrie für zwei, drei und vier Vorgaben ist freigegeben.
- Ohne Zusatzregel bleibt die Regelzone bestehen und zeigt ausschließlich ein neutrales Ornament.
- Als visuelle Richtung ist **Style Study A – Helles Honigbrett** freigegeben.
- Style Study B bleibt als verworfene Kontrollvariante nachvollziehbar erhalten.
- Farbrollen, Board-, Slot-, Schatten-, Dekorations- und Regelzonenregeln sind dokumentiert.

Der nächste Schritt ist ein eigenes Paket für **Typografie und das feste Wortlogo `Mise en Dice`**. Danach folgen der Illustrationsstandard und das finale Mastertemplate.

## Direkter Zugriff auf das Paket

Das vollständige Visual-Foundations-Paket kann direkt aus dem Repository geladen werden:

[`packages/challenge-card-visual-foundations-v1.zip`](packages/challenge-card-visual-foundations-v1.zip)
