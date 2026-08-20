# Arbeitsablauf für Challenge-Cards

## 1. Grundsatz

Die Kartenproduktion erfolgt außerhalb des Discord-Bots direkt mit ChatGPT. Das Repository stellt die verbindlichen Designquellen bereit.

## 2. Normale Kartenerstellung

1. Challenge-Daten und Nummer bestimmen.
2. Für jede Vorgabe zuerst ein exaktes Asset aus `assets/` suchen.
3. Fehlende Assets on demand gemäß `illustration-system/` erzeugen und prüfen.
4. Eine externe JSON-`CardSpec` nach dem Muster `templates/card-spec.example.json` anlegen. Der eingecheckte Mastergenerator wird für konkrete Challenges **nicht** geändert.
5. Falls ein freigegebenes Motiv im Slot optisch zu klein oder zu groß wirkt, optional `visual_scale` zwischen `0.75` und `1.50` setzen. Das Produktions-PNG selbst bleibt unverändert.
6. Die Karte mit `templates/render_challenge_card_from_spec.py` erzeugen; die generierte SVG-Ausgabe nicht manuell nachbearbeiten.
7. Optional über `--render` gleichzeitig die 1200er und 320er PNG-Prüfausgaben erzeugen.
8. Ausgabe bei voller Größe und im Kleinformat visuell prüfen.

Beispiel:

```bash
python design/challenge-cards/templates/render_challenge_card_from_spec.py \
  --spec card.json \
  --output challenge-139.svg \
  --render
```

## 3. Feste Rasterassets

Die Karte verwendet zwei unveränderliche Rasterquellen:

- `assets/background/mise-en-dice-background-master.png` für Verlauf, Küchenutensilien, Holzboard und leeren Regelbalken,
- `assets/brand/mise-en-dice-wordmark-master.png` für die Wortmarke.

Beide werden weder pro Challenge neu generiert noch zugeschnitten oder farblich verändert. Die Overlay-Geometrie ist auf den Background-Master kalibriert.

## 4. Neue Illustrationen

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

## 5. Mastertemplate- und Referenzänderungen

1. Entscheidung im Gespräch treffen.
2. Entscheidung in den maßgeblichen Designdokumenten festhalten.
3. `templates/generate_challenge_card_templates.py` nur ändern, wenn sich das **Designsystem selbst** oder ein verbindlicher Referenzfall ändert.
4. Betroffene Referenzen, Templates oder Assetregeln aktualisieren.
5. `--check` und bei Renderänderungen `--render-check` ausführen.
6. Erst danach auf neue Karten anwenden.

Designentscheidungen dürfen nicht nur im Chatverlauf verbleiben.

## 6. Fertige Karten

Fertige Challenge-Cards sind Ergebnisse und nicht automatisch Bestandteile des Designsystems. Unter `examples/` werden nur solche Karten abgelegt, die als verbindliche Referenz für Layout oder Sonderfälle dienen.

## 7. Reproduzierbarkeit

Eine Karte soll mindestens auf Challenge-Daten, Template-Version, Background-Version, Asset-Versionen und den Stand der Designspezifikation zurückgeführt werden können. Bestehende freigegebene Assets werden nicht still ersetzt.

Wortmarke und Background-Master werden gegen stillen Austausch abgesichert. Das generierte SVG ist bei identischen Eingaben deterministisch. PNG-Rendering benötigt einen lokalen Chrome- oder Edge-Browser im Headless-Modus; bei einer abweichenden Installation wird dessen Pfad über `CHALLENGE_CARD_BROWSER` gesetzt.

Browser-, Betriebssystem- und Fontversionen können die PNG-Bytes beeinflussen. `--render-check` ist deshalb ein Regressionstest **innerhalb derselben Rendering-Umgebung** und keine plattformübergreifende Byte-Garantie. Die `320 × 320 px`-Prüfdatei ist immer ein LANCZOS-Downscale des gerenderten `1200 × 1200 px`-Bilds.
