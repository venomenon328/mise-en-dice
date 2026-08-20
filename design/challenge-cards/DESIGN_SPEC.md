# Designspezifikation für Challenge-Cards

Stand: 20. August 2026  
Status: Challenge-Card-Designsystem v1 ist als Mastertemplate umgesetzt; die visuelle Abnahme von Issue #135 steht noch aus.

## 1. Ziel

Eine Challenge-Card stellt eine konkrete Mise-en-Dice-Challenge schnell erfassbar, attraktiv und über viele Ausgaben hinweg eindeutig wiedererkennbar dar. Die Karten entstehen außerhalb des Discord-Bots direkt mit ChatGPT auf Basis versionierter Designquellen.

## 2. Verbindliche Informationsarchitektur

- Masterformat: **1200 × 1200 px**.
- Feste Kopfzone mit `Mise en Dice` und `Challenge #NNN`.
- Zwei Vorgaben: zwei große gleichwertige Slots nebeneinander.
- Drei Vorgaben: zwei Slots oben, ein gleich großer Slot unten mittig.
- Vier Vorgaben: gleichmäßiges `2 × 2`-Raster.
- Feste untere Regelzone; ohne Zusatzregel nur neutrales Würfel-/Linienornament.
- Konkrete Zutat: einzelnes klar lesbares Motiv.
- Offenes Konzept: nach Möglichkeit Gruppe aus zwei bis drei repräsentativen Konkretisierungen plus `OFFEN`-Badge aus dem Template.

## 3. Verbindliche Geometrie

- Canvas: `1200 × 1200 px`.
- Kopfzone bis `Y = 222`.
- Boardfläche: `X = 72`, `Y = 222`, `1056 × 918 px`.
- Regelzone: `X = 120`, `Y = 974`, `960 × 116 px`.
- Slotabstand horizontal: `48 px`.
- Slotabstand vertikal: `40 px`.
- Zwei große Slots: `456 × 640 px`.
- Drei-/Vierer-Slots: `432 × 300 px`.

Die vollständigen Maße stehen unter `wireframes/GEOMETRY.md`.

Die finale Slotkontur besteht aus einer weichen äußeren Kontur mit `2,4 px` und einer **durchgezogenen** inneren Kontur mit `1,5 px` bei `13 px` Innenversatz. Sie ersetzt die gestrichelte Wireframe-Hilfe und darf nicht als hartes UI-Panel wirken.

## 4. Verbindliche visuelle Richtung

Freigegeben ist **Style Study A – Helles Honigbrett**.

- warmes Goldorange mit hellem Fokus,
- gebrannte Orange-/Rotbrauntöne zu den Rändern,
- zurückhaltende dunkle Küchen-Silhouetten,
- helle honigfarbene Schneidebrettfläche,
- weiche semitransparente Slots,
- keine vollfarbigen Dekorationslebensmittel im Vorgabenbereich,
- dunkle Espresso-Regelzone mit hellem Text.

Die vollständigen Tokens stehen unter `style-studies/VISUAL_FOUNDATIONS.md`.

## 5. Verbindliche Nutztypografie und Wortmarke

Für die Nutztypografie ist **Typography Study A – Kitchen Editorial** verbindlich.

- `Challenge #NNN`, Badge und Regeltext: robuste Sans.
- Vorgabennamen: normal geschriebene Quelldaten, typografisch als Small Caps gerendert.
- `Go Smallcaps` ist die freigegebene Small-Caps-Referenz.

Die finale Wortmarke ist ausschließlich [`assets/brand/mise-en-dice-wordmark-master.png`](assets/brand/mise-en-dice-wordmark-master.png). Sie wird unverändert als RGBA-Rasterasset eingebunden; weder SVG-Rekonstruktion noch Vektorisierung oder Neugenerierung sind zulässig.

Im `1200 × 1200 px`-Template liegt sie bei `X = 330`, `Y = 24` mit `540 × 126 px`; `Challenge #NNN` steht zentriert mit Basislinie `Y = 195` und `28 px`.

Frühere SVG-/Vektorisierungsstudien der Wortmarke sind verworfen und gehören nicht zum Designsystem v1.

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

### 6.3 Confusables und visuelle Nachbarschaft

Die Ähnlichkeitsprüfung gilt **für jedes neue Asset** und ist nicht auf bestimmte Beispielzutaten beschränkt. Bei über 600 Konzepten werden weitere Cluster erwartet.

Unterschiedliche Konzepte teilen niemals absichtlich dasselbe Asset. Nahe Nachbarn werden bevorzugt über mindestens zwei primäre Dimensionen unterschieden: Silhouette, Farbe/Helligkeit, Textur, Stückigkeit, Glanz/Transparenz, Viskosität, Anschnitt/innere Struktur oder charakteristische Details.

Eine andere neutrale Behälterform ist als sekundäre Differenzierung erlaubt, wenn die Zutat selbst sonst zu ähnlich wirkt. Das Gefäß darf nie das einzige Erkennungsmerkmal sein.

Die Methode und der erste Pasten-/Saucen-Stresstest stehen unter `illustration-system/CONFUSABLES.md`.

### 6.4 Assetstrategie

- Assets entstehen **on demand**.
- Keine Vollbebilderung des 600+ Konzepte umfassenden Katalogs.
- Bestehende freigegebene Assets werden wiederverwendet und nicht pro Challenge neu generiert.
- `assets/ASSET_INDEX.csv` enthält nur tatsächlich freigegebene Produktionsassets.

## 7. Konsistenz- und Reproduzierbarkeitsregeln

- Wortmarke, Geometrie, Farben, Typografierollen und Illustrationsregeln werden versioniert.
- Gleiche Challenge-Daten, gleiche Template-Version und gleiche Asset-Versionen erzeugen dasselbe SVG-Dokument.
- Der sichtbare Vorgabenname bleibt die fachlich verbindliche Aussage; die Illustration unterstützt ihn.
- Die Bildgenerierung erfolgt bewusst außerhalb des Discord-Bots.
- Die Wortmarke wird per SHA-256 gegen stillen Austausch abgesichert.
- PNG-Renderings hängen zusätzlich von Browser, Betriebssystem und verfügbarer Fontumgebung ab. Ein bytegenauer `--render-check` ist deshalb ein **Regressionstest innerhalb derselben Rendering-Umgebung**, keine plattformübergreifende Byte-Garantie.
- Für visuelle Abnahmen sind die eingecheckten 1200er Referenzrenderings maßgeblich; die 320er Dateien sind LANCZOS-Downscales genau dieser Vollformatbilder.

## 8. Referenzen

- `wireframes/`: Geometrie.
- `style-studies/`: visuelle Grundlagen.
- `typography-studies/`: Nutztypografie und historische Typografie-Studien.
- `illustration-system/`: Illustrationsregeln und Prompt-Templates.
- `assets/brand/`: finale Raster-Wortmarke.
- `assets/references/anchor-style-study.jpg`: freigegebene Stilkalibrierung.
- `assets/references/confusables-example-study.jpg`: exemplarischer Ähnlichkeits-Stresstest.

## 9. Finales Mastertemplate und Referenzprüfung

- Die generatorverwalteten Varianten liegen unter [`templates/`](templates/) für zwei, drei und vier Vorgaben.
- `generate_challenge_card_templates.py` fixiert Geometrie, Wortmarkenhash, Slotkontur und die vier eingecheckten Referenzfälle.
- Für neue konkrete Challenges wird **nicht** der Generator geändert. Ihre Daten werden als externe JSON-`CardSpec` über `render_challenge_card_from_spec.py` eingespeist.
- Ohne Zusatzregel zeigt die Regelzone ausschließlich das neutrale Würfel-/Linienornament, niemals einen erklärenden Ersatztext.
- Review-PNGs entstehen bei `1200 × 1200`; die `320 × 320`-Prüfung wird aus dem 1200er Render heruntergerechnet.

## 10. Offene Entscheidungen

Die festgelegten Designparameter dieses Pakets haben keine offene Alternativvariante mehr. Ausstehend ist allein die visuelle Abnahme der erzeugten Referenzkarten; bis dahin bleibt der zugehörige PR ein Draft. Querformat und weitere Plattformformate sind nicht Bestandteil von v1.
