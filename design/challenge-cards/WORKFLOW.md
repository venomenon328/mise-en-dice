# Arbeitsablauf für Challenge-Cards

## 1. Grundsatz

Die Kartenproduktion erfolgt außerhalb des Discord-Bots direkt mit ChatGPT. Das Repository stellt die verbindlichen Designquellen bereit.

## 2. Normale Kartenerstellung

1. Challenge-Daten und Nummer bestimmen.
2. Freigegebenes Mastertemplate und den Generator unter `templates/` laden.
3. Für jede Vorgabe zuerst ein exaktes Asset aus `assets/` suchen.
4. Fehlende Assets on demand gemäß `illustration-system/` erzeugen und prüfen.
5. Freigegebene Assets in die feste `CardSpec` des Generators einsetzen; keine SVG-Ausgabe direkt nachbearbeiten.
6. Namen, `OFFEN`-Badge und optionale Regel aus dem Template rendern.
7. `python design/challenge-cards/templates/generate_challenge_card_templates.py --render` ausführen.
8. Mit `--check` und `--render-check` bei voller Größe und im Kleinformat prüfen.

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

## 4. Designänderungen

1. Entscheidung im Gespräch treffen.
2. Entscheidung in den maßgeblichen Designdokumenten festhalten.
3. Betroffene Referenzen, Templates oder Assetregeln aktualisieren.
4. Änderung versionieren und prüfen.
5. Erst danach auf neue Karten anwenden.

Designentscheidungen dürfen nicht nur im Chatverlauf verbleiben.

## 5. Fertige Karten

Fertige Challenge-Cards sind Ergebnisse und nicht automatisch Bestandteile des Designsystems. Unter `examples/` werden nur solche Karten abgelegt, die als verbindliche Referenz für Layout oder Sonderfälle dienen.

## 6. Reproduzierbarkeit

Eine Karte soll mindestens auf Challenge-Daten, Template-Version, Asset-Versionen und den Stand der Designspezifikation zurückgeführt werden können. Bestehende freigegebene Assets werden nicht still ersetzt.

Das finale Template verwendet den unveränderlichen PNG-Hash der Raster-Wortmarke als zusätzliche Sperre. Der PNG-Reviewweg benötigt einen lokalen Chrome- oder Edge-Browser im Headless-Modus; bei einer abweichenden Installation wird dessen Pfad über `CHALLENGE_CARD_BROWSER` gesetzt. Die `320 × 320 px`-Prüfdatei ist immer ein deterministischer LANCZOS-Downscale des gerenderten `1200 × 1200 px`-Bilds, nie ein abgeschnittener Browser-Viewport.
