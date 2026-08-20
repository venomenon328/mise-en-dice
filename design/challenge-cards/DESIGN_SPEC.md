# Designspezifikation für Challenge-Cards

Stand: 21. August 2026  
Status: Challenge-Card-Designsystem v1 ist als Mastertemplate umgesetzt; die visuelle Abnahme von Issue #135 steht noch aus.

## 1. Ziel

Eine Challenge-Card stellt eine konkrete Mise-en-Dice-Challenge schnell erfassbar, attraktiv und über viele Ausgaben hinweg eindeutig wiedererkennbar dar. Die Karten entstehen außerhalb des Discord-Bots direkt mit ChatGPT auf Basis versionierter Designquellen.

## 2. Verbindliche Informationsarchitektur

- Masterformat: **1200 × 1200 px**.
- Feste Kopfzone mit `Mise en Dice` und `Challenge #NNN`.
- Zwei Vorgaben: zwei große gleichwertige Slots nebeneinander.
- Drei Vorgaben: zwei Slots oben, ein gleich großer Slot unten mittig.
- Vier Vorgaben: gleichmäßiges `2 × 2`-Raster.
- Fester unterer Regelbalken; ohne Zusatzregel nur neutrales Würfel-/Linienornament.
- Konkrete Zutat: einzelnes klar lesbares Motiv.
- Offenes Konzept: nach Möglichkeit Gruppe aus zwei bis drei repräsentativen Konkretisierungen plus `OFFEN`-Badge aus dem Template.

## 3. Background-Master und Overlay-Geometrie

Der finale Kartenuntergrund ist [`assets/background/mise-en-dice-background-master.png`](assets/background/mise-en-dice-background-master.png). Das `1200 × 1200 px`-Rasterasset enthält Verlauf, Küchengeräte-Silhouetten, Holzboard und leeren Regelbalken. Es bestimmt die Pixelgeometrie des finalen Designs.

Die früheren Wireframes bleiben als historische Layoutvorstufe erhalten; ihre synthetische Board-Geometrie wird **nicht** über den Background-Master gelegt.

Kalibrierte visuelle Bereiche im Background-Master:

- Holzboard ungefähr `X = 69`, `Y = 368`, `1058 × 682 px`,
- Regelbalken ungefähr `X = 115`, `Y = 930`, `970 × 90 px`.

Verbindliche Slotgeometrie:

### Zwei Vorgaben

- Slot 1: `X = 130`, `Y = 400`, `440 × 500 px`,
- Slot 2: `X = 630`, `Y = 400`, `440 × 500 px`.

### Drei Vorgaben

- Slot 1: `X = 140`, `Y = 400`, `430 × 238 px`,
- Slot 2: `X = 630`, `Y = 400`, `430 × 238 px`,
- Slot 3: `X = 385`, `Y = 665`, `430 × 238 px`.

### Vier Vorgaben

- obere Reihe wie beim Dreierlayout,
- untere Reihe: `X = 140` und `630`, jeweils `Y = 665`, `430 × 238 px`.

Die Slots sind bewusst zurückgenommen: nur eine einzelne weiche Fläche mit `7 %` heller Füllung, warmer Kontur mit `18 %` Deckkraft, `1,4 px` Linienstärke und `28 px` Radius. Es gibt **keine zweite innere Slotkontur** mehr.

## 4. Verbindliche visuelle Richtung

Grundrichtung bleibt **Style Study A – Helles Honigbrett**, nun konkretisiert durch den freigegebenen Background-Master:

- warmes Goldorange mit hellem Fokus,
- dunklere Orange-/Brauntöne nach unten,
- klar erkennbare Küchenutensilien nur links und rechts der freien Kopfzone,
- ruhiger, zentraler Kopfbereich ohne Dekoration hinter der Wortmarke,
- helles Holzboard mit subtiler natürlicher Maserung,
- keine synthetischen weißen Maserungslinien,
- zurückgenommene Slotflächen statt UI-artiger Doppelpanels,
- dunkler Regelbalken als Bestandteil des Background-Masters.

## 5. Verbindliche Nutztypografie und Wortmarke

Für die Nutztypografie ist **Typography Study A – Kitchen Editorial** verbindlich.

- `Challenge #NNN`, Badge und Regeltext: robuste Sans.
- Vorgabennamen: normal geschriebene Quelldaten, typografisch als Small Caps gerendert.
- `Go Smallcaps` ist die freigegebene Small-Caps-Referenz.

Die finale Wortmarke ist ausschließlich [`assets/brand/mise-en-dice-wordmark-master.png`](assets/brand/mise-en-dice-wordmark-master.png). Sie wird unverändert als RGBA-Rasterasset eingebunden; weder SVG-Rekonstruktion noch Vektorisierung oder Neugenerierung sind zulässig.

Im `1200 × 1200 px`-Template liegt sie zunächst weiterhin bei `X = 330`, `Y = 24` mit `540 × 126 px`; `Challenge #NNN` steht zentriert mit Basislinie `Y = 195` und `28 px`. Diese Position wird im nächsten visuellen Review auf dem neuen Background-Master mitgeprüft.

## 6. Verbindliches Illustrationssystem

Maßgeblich ist [`illustration-system/ILLUSTRATION_GUIDE.md`](illustration-system/ILLUSTRATION_GUIDE.md).

### 6.1 Produktionsassets

- PNG, `1024 × 1024 px`, transparenter Hintergrund.
- Ungefähr 8 % Safe Area.
- leicht erhöhte Dreiviertelperspektive.
- warmes Licht von links oben.
- kräftige natürliche Farben und dunkelbraune Kontur.
- poliert comicartig-illustrativ, nicht fotorealistisch.
- kein Text, Badge, Kartenrahmen oder eingebrannter Bodenschatten.
- neutrale unmarkierte Behälter nur wenn sachlich sinnvoll.

### 6.2 Offene Konzepte

- normalerweise zwei bis drei repräsentative Konkretisierungen als Gruppe,
- keine abschließende Auswahl suggerieren,
- `OFFEN` bleibt Teil des Kartentemplates.

### 6.3 Assetgröße innerhalb der Slots

Die kleinen Drei-/Vierer-Slots vergrößern Illustrationen gegenüber der ersten Mastertemplate-Fassung standardmäßig um **18 %** innerhalb der reservierten Motivzone. Damit wird die zuvor zu kleine Symbolwirkung korrigiert.

Zusätzlich darf eine konkrete externe `CardSpec` pro Vorgabe optional `visual_scale` zwischen `0.75` und `1.50` setzen. Dieser Faktor dient ausschließlich dem optischen Gewicht unterschiedlich geformter freigegebener Assets und verändert nicht die PNG-Datei selbst.

### 6.4 Confusables und Assetstrategie

Die Ähnlichkeitsprüfung gilt für jedes neue Asset. Unterschiedliche Konzepte teilen niemals absichtlich dasselbe Asset. Assets entstehen **on demand**; es gibt keine Vorabbebilderung des 600+ Konzepte umfassenden Katalogs. `assets/ASSET_INDEX.csv` enthält nur tatsächlich freigegebene Produktionsassets.

## 7. Konsistenz- und Reproduzierbarkeitsregeln

- Wortmarke, Background-Master, Geometrie, Typografierollen und Illustrationsregeln werden versioniert.
- Gleiche Challenge-Daten, gleiche Template-Version und gleiche Asset-Versionen erzeugen dasselbe SVG-Dokument.
- Wortmarke und Background-Master werden gegen stillen Austausch abgesichert.
- PNG-Renderings hängen zusätzlich von Browser, Betriebssystem und Fontumgebung ab. Ein bytegenauer `--render-check` ist ein Regressionstest innerhalb derselben Rendering-Umgebung, keine plattformübergreifende Byte-Garantie.
- Für visuelle Abnahmen sind die 1200er Referenzrenderings maßgeblich; die 320er Dateien sind LANCZOS-Downscales genau dieser Vollformatbilder.

## 8. Referenzen

- `wireframes/`: historische Geometrievorstufe,
- `style-studies/`: visuelle Grundlagen,
- `typography-studies/`: Nutztypografie,
- `illustration-system/`: Illustrationsregeln,
- `assets/background/`: finaler Background-Master,
- `assets/brand/`: finale Raster-Wortmarke,
- `assets/references/anchor-style-study.jpg`: Stilkalibrierung.

## 9. Finales Mastertemplate und Referenzprüfung

- Die generatorverwalteten Varianten liegen unter [`templates/`](templates/) für zwei, drei und vier Vorgaben.
- `generate_challenge_card_templates.py` fixiert Background-Master, Geometrie, Wortmarkenhash, Slotstil und Referenzfälle.
- Für neue konkrete Challenges wird nicht der Mastergenerator geändert; ihre Daten werden als externe JSON-`CardSpec` über `render_challenge_card_from_spec.py` eingespeist.
- Der Regelbalken stammt aus dem Background-Master; dynamisch sind ausschließlich Symbol/Ornament und Text.

## 10. Offene visuelle Abnahme

Vor dem Merge werden die vier Referenzkarten erneut bei `1200 × 1200` und `320 × 320 px` geprüft. Insbesondere zu beurteilen sind Slotzurückhaltung, Motivgröße, Headerposition und die vertikale Ausrichtung des Regelbalkeninhalts auf dem neuen Background-Master.
