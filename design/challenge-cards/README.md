# Challenge-Card-Design

Dieses Verzeichnis ist die verbindliche, versionierte Arbeitsgrundlage für die **Mise-en-Dice-Challenge-Cards**.

Die Karten werden bewusst **nicht durch den Discord-Bot erzeugt**. Sie entstehen außerhalb von Discord direkt mit ChatGPT auf Basis der hier abgelegten Spezifikation, Templates, Referenzen und freigegebenen Bildassets. GitHub ist dafür die Source of Truth; die ChatGPT File Library wird nicht benötigt.

Maßgeblich für den aktuellen Aufbau ist [Issue #122](https://github.com/venomenon328/mise-en-dice/issues/122).

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
├── templates/
│   └── README.md
├── wireframes/
│   └── README.md
├── assets/
│   ├── README.md
│   ├── ingredients/
│   │   └── README.md
│   └── open-concepts/
│       └── README.md
└── examples/
    └── README.md
```

## Verbindlichkeit

- [`DESIGN_SPEC.md`](DESIGN_SPEC.md) trennt beschlossene Regeln von noch offenen Gestaltungsfragen.
- Freigegebene Templates und Assets werden nicht bei jeder Challenge neu interpretiert oder ersetzt.
- Eine komplette Karte wird nicht frei durch ein Bildmodell komponiert. Layout, Logo, Texte und Abstände folgen einem festen Template.
- Neue Zutaten- oder Konzeptillustrationen werden bei Bedarf erzeugt, geprüft, hier versioniert und anschließend wiederverwendet.
- Der sichtbare Name einer Vorgabe bleibt verbindlich; die Illustration unterstützt ihn nur und ersetzt ihn nicht.

## Aktueller Stand

Das Grundsystem und die bisher abgestimmten Entscheidungen sind dokumentiert. Der nächste Schritt ist ein maßhaltiges Low-Fidelity-Wireframe-Paket für zwei, drei und vier Vorgaben sowie die Regelzone. Farben, finale Typografie und Zutatenillustrationen werden erst danach festgelegt.
