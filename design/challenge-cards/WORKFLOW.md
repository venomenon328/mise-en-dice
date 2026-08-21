# Produktionsvertrag für Challenge-Cards

Challenge-Cards entstehen außerhalb des Discord-Bots und ausschließlich aus
den versionierten Designquellen auf dem aktuellen `main`. Der externe
CardSpec-Renderer ist der normale Einstiegspunkt. Dieses Dokument erlaubt
einen kurzen, vollständigen Produktionsauftrag, ohne den Mastergenerator,
Background, Wortmarke, Templates oder Laufzeitcode anzutasten.

## Kurzer Produktionsauftrag

```text
Challenge #<nummer>
Vorgaben:
- <konkrete Zutat>
- <offenes Konzept> (offen)
- <weitere Vorgabe>
Zusatzregel: <Text oder keine>
Erzwungene Asset-Neugenerierung: <Liste oder keine>
```

- Eine nicht markierte Vorgabe ist ohne Rückfrage immer eine konkrete Zutat
  (`ingredient`). `(konkret)` ist dafür nur die gleichbedeutende, optionale
  Steuerannotation.
- `(offen)` bedeutet immer `open-concept`.
- `(offen)` und `(konkret)` steuern nur die Assetart. Sie werden weder in den
  Card-`display_name` noch in den `display_name` des Produktionsindex
  übernommen: `Brokkoli (offen)` wird beispielsweise als `Brokkoli` mit
  `asset_kind=open-concept` verarbeitet.
- Fehlt die Zeile `Zusatzregel`, bedeutet das `keine`. Fehlt die Zeile
  `Erzwungene Asset-Neugenerierung`, bedeutet das ebenfalls `keine`.
- Eine erzwungene Neugenerierung nennt dieselbe exakte Variante, etwa
  `Mayonnaise (konkret)` oder `Brokkoli (offen)`; sie ersetzt niemals die
  andere Variante desselben Konzeptschlüssels.
- Die Art wird ausschließlich durch diese Annotation oder die konkrete
  Standardannahme bestimmt, niemals aus einem vorhandenen Dateinamen oder einer
  Katalogbeziehung geraten.

## Verbindlicher Ablauf für ChatGPT oder Codex

### 1. Quellen und Identität prüfen

1. Vom aktuellen `main` ausgehen und `AGENTS.md`, diese Datei,
   `README.md`, `DESIGN_SPEC.md`, `assets/README.md`,
   `assets/ASSET_INDEX.csv`, `illustration-system/`, `templates/README.md`
   sowie `templates/render_challenge_card_from_spec.py` lesen.
2. Vor dem Rendern den Produktionskatalog ausführen:

   ```bash
   python design/challenge-cards/tools/validate_asset_catalog.py
   ```

3. Die logische Identität eines Illustrationsassets ist ausschließlich
   `(concept_key, asset_kind)`, mit `asset_kind` `ingredient` oder
   `open-concept`. Beide Varianten dürfen denselben `concept_key` besitzen,
   sind jedoch unterschiedliche Assets und niemals gegenseitige Fallbacks.
4. Die Pfade sind fest:

   ```text
   assets/ingredients/<concept_key>.png
   assets/open-concepts/<concept_key>.png
   ```

   `concept_key` und Dateiname sind stabil, kleingeschrieben und mit
   Bindestrichen gebildet. `ASSET_INDEX.csv` ist das einzige
   Produktionsinventar; jede Zeile darin ist `approved`.

### 2. Assets bestimmen oder erzeugen

1. Für jede Vorgabe die exakte Kombination aus `concept_key` und
   `asset_kind` im Index suchen. Anzeigename, Pfad und Art müssen zur
   CardSpec passen; der Renderer prüft dies nochmals.
2. Ein vorhandenes exaktes `approved`-Asset unverändert wiederverwenden. Ein
   generisches Elternmotiv, ein ähnliches Motiv oder das Asset der jeweils
   anderen Art ist für eine veröffentlichte Karte kein Fallback.
3. Fehlt die exakte Kombination oder ist genau diese Kombination im Auftrag
   zur Neugenerierung genannt, das Asset ausschließlich nach
   [`illustration-system/`](illustration-system/) produzieren:

   - `1024 × 1024` RGBA-PNG mit transparentem Hintergrund und ungefähr
     `8 %` Safe Area;
   - Anchor-Referenz und visuell nahe vorhandene Assets als Referenzen nutzen;
   - keine Schrift, kein `OFFEN`-Badge, kein Kartenrahmen und kein
     eingebrannter Bodenschatten;
   - konkrete Zutaten als klar lesbares Hauptmotiv, offene Konzepte als
     kompakte Gruppe aus zwei bis drei repräsentativen Konkretisierungen;
   - Confusables über mindestens zwei primäre visuelle Dimensionen
     unterscheiden, soweit möglich;
   - den finalen Kandidaten bei ungefähr `96 px` und innerhalb einer
     `320 × 320`-Kartenprüfung verständlich abnehmen.

4. Bei einer erzwungenen Neugenerierung nur die explizit benannte logische
   Identität am stabilen Produktionspfad ersetzen und genau deren Indexzeile
   nachvollziehbar aktualisieren. Es gibt keine Versionssuffixe wie
   `-v2-final`; die vorherige Version bleibt ausschließlich in Git erhalten.
   Nicht genannte Varianten bleiben unverändert. Bereits freigegebene Assets
   werden nie stillschweigend ersetzt.

### 3. Produktionsassets zuerst nach `main` bringen

Wenn alle exakten Assets schon freigegeben sind, entsteht keine
Repositoryänderung. Andernfalls:

1. Einen kurzlebigen Assetbranch vom aktuellen `main` erstellen.
2. Ausschließlich die final ausgewählten Produktions-PNGs und
   `design/challenge-cards/assets/ASSET_INDEX.csv` atomar committen. Ein
   normaler Assetproduktionscommit enthält nur:

   ```text
   design/challenge-cards/assets/ASSET_INDEX.csv
   design/challenge-cards/assets/ingredients/*.png
   design/challenge-cards/assets/open-concepts/*.png
   ```

   Nicht committen: temporäre CardSpecs, erzeugte SVGs, 1200er/320er
   Prüfrenderings konkreter Challenges, fertige Karten, verworfene Kandidaten,
   einmalige Generator- oder Transportscripte oder temporäre Workflows.
3. Den leichten Assetpfad mit dem Katalogvalidator prüfen. Er gilt nur für
   `ASSET_INDEX.csv` allein oder zusammen mit hinzugefügten oder geänderten
   Produktions-PNGs. Jede weitere Datei, jede Löschung oder Umbenennung eines
   Produktionsassets und jeder unklare Diff erzwingen den vollständigen
   CI-Pfad.
4. Den Assetbranch im selben Produktionsablauf nach `main` bringen und den
   temporären Branch beziehungsweise PR anschließend entfernen oder schließen.
   Erst dann die finale Karte vom aktualisierten `main` erzeugen.

### 4. Karte deterministisch rendern und abnehmen

1. Die temporäre CardSpec außerhalb der versionierten Designquellen anlegen;
   sie enthält die Challenge-Nummer, zwei bis vier Vorgaben, ihre exakten
   approved Assetpfade, `open_concept` nur für offene Konzepte und optional
   die Zusatzregel. `templates/card-spec.example.json` ist das Formatmuster.
2. Den vorhandenen Renderer unverändert einsetzen und die SVG-Ausgabe nicht
   manuell nachbearbeiten:

   ```bash
   python design/challenge-cards/templates/render_challenge_card_from_spec.py \
     --spec <temporäre-card-spec.json> \
     --output <arbeitsverzeichnis>/challenge-<nummer>.svg \
     --render
   ```

3. Den gerenderten 1200er PNG-Output außerhalb des Repositorys als
   `challenge-<nummer>.png` ausliefern; die erzeugte 320er Variante bleibt
   ausschließlich interne QA.
4. Beide Größen visuell prüfen: Namen, `OFFEN`-Badges, Regeltext, optisches
   Gewicht, Safe Area und Beschnitt. Vor Auslieferung außerdem PNG-Signatur,
   `1200 × 1200 px` und eine Größe von höchstens `5 MiB` prüfen.

## Abschluss eines Kartenlaufs

Die Abschlussmeldung nennt kompakt die wiederverwendeten, neu erzeugten und
ausdrücklich ersetzten Assets, `Merge-Commit auf main` oder `keine
Repositoryänderung`, die Asset- und Renderprüfungen sowie den finalen
1200er-PNG-Pfad. Fertige konkrete Karten bleiben außerhalb des
Designrepositorys, sofern sie nicht ausdrücklich als neue Referenzkarte
beauftragt wurden.
