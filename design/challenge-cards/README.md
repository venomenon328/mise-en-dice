# Challenge-Card-Design

Dieses Verzeichnis ist die verbindliche, versionierte Arbeitsgrundlage für die **Mise-en-Dice-Challenge-Cards**.

Die Karten werden bewusst **nicht durch den Discord-Bot erzeugt**. Sie entstehen außerhalb von Discord direkt mit ChatGPT auf Basis der hier abgelegten Spezifikation, Templates, Referenzen und freigegebenen Bildassets. GitHub ist die Source of Truth; die ChatGPT File Library wird nicht benötigt.

Maßgeblich sind die abgeschlossenen Issues #122, #124 und #126 sowie die dazugehörigen gemergten Pull Requests.

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
├── wireframes/
├── style-studies/
├── typography-studies/
│   ├── README.md
│   ├── TYPOGRAPHY_STUDIES.md
│   ├── generate_typography_studies.py
│   ├── typography-study-a.svg
│   ├── typography-study-b.svg
│   ├── typography-study-c.svg
│   └── renders/
│       └── compact/
├── packages/
├── templates/
├── assets/
│   ├── ingredients/
│   └── open-concepts/
└── examples/
```

## Verbindlichkeit

- [`DESIGN_SPEC.md`](DESIGN_SPEC.md) enthält den freigegebenen Gesamtstand und trennt ihn von offenen Folgeentscheidungen.
- Freigegebene Templates und Assets werden nicht bei jeder Challenge neu interpretiert oder ersetzt.
- Eine komplette Karte wird nicht frei durch ein Bildmodell komponiert. Layout, Logo, Texte und Abstände folgen einem festen Template.
- Neue Zutaten- oder Konzeptillustrationen werden bei Bedarf erzeugt, geprüft, versioniert und anschließend wiederverwendet.
- Der sichtbare Name einer Vorgabe bleibt verbindlich; die Illustration unterstützt ihn nur.

## Aktueller Stand

- Die Geometrie für zwei, drei und vier Vorgaben ist freigegeben.
- Ohne Zusatzregel bleibt die Regelzone bestehen und zeigt ausschließlich ein neutrales Ornament.
- Als visuelle Richtung ist **Style Study A – Helles Honigbrett** freigegeben.
- Als Typografierichtung ist **Typography Study A – Kitchen Editorial** freigegeben.
- Vorgabennamen werden aus normal geschriebenen Quelldaten als **Small Caps** gerendert.
- Typography Study B und C bleiben als verworfene Gegenproben nachvollziehbar erhalten.

Der nächste größere Schritt ist der **Illustrationsstandard für konkrete Zutaten und offene Konzepte**. Danach wird das finale Mastertemplate aus den bereits eingefrorenen Bausteinen zusammengesetzt.
