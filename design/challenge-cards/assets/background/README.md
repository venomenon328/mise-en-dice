# Background-Master

`mise-en-dice-background-master.png` ist der verbindliche **1200 × 1200 px** Background-Master für Challenge-Cards.

Er enthält ausschließlich die festen, nicht challenge-spezifischen Bestandteile:

- warmen gelb-orange-braunen Hintergrundverlauf,
- Küchenutensilien links und rechts der freien Wortmarkenzone,
- das helle Holzboard mit seiner Materialstruktur,
- den leeren dunklen Regelbalken.

Nicht Bestandteil des Rasterassets sind Wortmarke, `Challenge #NNN`, Slots, Zutatenillustrationen, `OFFEN`, Regeltext oder Regel-/No-Rule-Symbole.

## Verbindliche Nutzung

- Das PNG wird bei `0,0` unverändert in `1200 × 1200 px` eingesetzt.
- Es wird nicht pro Challenge neu generiert, beschnitten oder farblich verändert.
- Sichtbare Änderungen am Hintergrund sind eine bewusste neue Background-Version.
- Die Overlay-Geometrie wird **an diesen Background-Master kalibriert**; die früheren Wireframe-Maße für die synthetisch erzeugte Boardfläche sind historische Vorstufe und nicht mehr die Pixelgeometrie des finalen Boards.
- Der Regelbalken ist bereits Teil des Background-Masters. Dynamisch gerendert werden nur dessen Inhalt bzw. das neutrale No-Rule-Ornament.

Das Asset gehört nicht in `ASSET_INDEX.csv`; dieser Index bleibt Zutaten- und Konzeptillustrationen vorbehalten.
