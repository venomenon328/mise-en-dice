# Arbeitsablauf für Challenge-Cards

## 1. Grundsatz

Die Kartenproduktion erfolgt außerhalb des Discord-Bots direkt mit ChatGPT. Das Repository stellt dafür die verbindlichen Designquellen bereit.

## 2. Normale Kartenerstellung

1. Challenge-Daten und Nummer bestimmen.
2. Freigegebenes Mastertemplate laden.
3. Wortmarke aus `assets/brand/mise-en-dice-wordmark-master.png` gemäß Template proportional platzieren.
4. Für jede Vorgabe zuerst ein exaktes Asset aus `assets/` suchen.
5. Fehlende Assets on demand gemäß `illustration-system/` erzeugen und prüfen.
6. Freigegebene Assets in die festen Slots einsetzen.
7. `Challenge #NNN`, Namen, `OFFEN`-Badge und optionale Regel mit der freigegebenen Nutztypografie rendern.
8. Ausgabe bei voller Größe und im Kleinformat prüfen.

Die Wortmarke wird nicht als Text neu gesetzt und nicht als SVG rekonstruiert.

## 3. Neue Illustrationen

1. Exaktes bestehendes Asset suchen.
2. Visuell ähnliche vorhandene Assets bestimmen.
3. Anchor-Referenz und relevante Nachbarassets verwenden.
4. Passendes Prompt-Template aus [`illustration-system/PROMPT_TEMPLATES.md`](illustration-system/PROMPT_TEMPLATES.md) wählen.
5. Kandidat erzeugen.
6. QA nach [`illustration-system/ILLUSTRATION_GUIDE.md`](illustration-system/ILLUSTRATION_GUIDE.md) durchführen.
7. Bei Confusables gezielt über mindestens zwei primäre Dimensionen differenzieren; bei Bedarf zusätzlich die neutrale Behälterform variieren.
8. Nach Freigabe Asset versionieren und in `assets/ASSET_INDEX.csv` aufnehmen.
9. Zukünftig exakt dieses Asset wiederverwenden.

Der Katalog mit 600+ Konzepten wird nicht vorab vollständig bebildert.

## 4. Wortmarken-Workflow

Die Wortmarke unter `assets/brand/` ist ein eingefrorenes Rasterasset. Sie wird nicht neu generiert, vektorisiert, farblich verändert oder in Einzelbestandteile zerlegt. Zulässig sind proportionale Skalierung, Positionierung und technisch unsichtbare Optimierung.

Eine sichtbare Änderung der Wortmarke wäre eine neue Brand-Version und muss bewusst beschlossen und versioniert werden.

## 5. Designänderungen

1. Entscheidung im Gespräch treffen.
2. Entscheidung in den maßgeblichen Designdokumenten festhalten.
3. Betroffene Referenzen, Templates oder Assetregeln aktualisieren.
4. Änderung versionieren und prüfen.
5. Erst danach auf neue Karten anwenden.

Designentscheidungen dürfen nicht nur im Chatverlauf verbleiben.

## 6. Fertige Karten

Fertige Challenge-Cards sind Ergebnisse und nicht automatisch Bestandteile des Designsystems. Unter `examples/` werden nur solche Karten abgelegt, die als verbindliche Referenz für Layout oder Sonderfälle dienen.

## 7. Reproduzierbarkeit

Eine Karte soll mindestens auf Challenge-Daten, Template-Version, Wortmarkenasset, Zutaten-/Konzept-Assetversionen und den Stand der Designspezifikation zurückgeführt werden können. Bestehende freigegebene Assets werden nicht still ersetzt.
