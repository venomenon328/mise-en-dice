# Designspezifikation für Challenge-Cards

Stand: 20. August 2026  
Status: Geometrie und visuelle Grundlagen sind freigegeben

## 1. Ziel

Eine Challenge-Card stellt eine konkrete Mise-en-Dice-Challenge schnell erfassbar, attraktiv und über viele Ausgaben hinweg eindeutig wiedererkennbar dar.

Das Design verbindet:

- die klare Informationshierarchie eines Küchenplakats,
- die warme illustrative Küchenatmosphäre des Discord-Banners,
- eine helle Board- beziehungsweise Schneidebrettfläche,
- ein deterministisches Layout, das nicht bei jeder Challenge neu erfunden wird.

Die Karten entstehen außerhalb des Discord-Bots direkt mit ChatGPT auf Basis der versionierten Dateien in diesem Verzeichnis.

## 2. Verbindliche Informationsarchitektur

### 2.1 Format und Inhalt

- quadratisches Masterformat: **1200 × 1200 px**,
- feste Kopfzone mit **`Mise en Dice`** und **`Challenge #NNN`**,
- dreistellige Challenge-Nummer, beispielsweise `Challenge #001`,
- zentraler Bereich für zwei bis vier Challenge-Vorgaben,
- feste untere Regelzone für eine optionale Einschränkung oder einen Ausschluss,
- keine weiteren Metadaten ohne einen ausdrücklich beschlossenen Nutzen.

### 2.2 Gesamtkomposition

Der Aufbau enthält dauerhaft:

1. Kopfzone,
2. zentrale Boardfläche,
3. Vorgabenbereich mit festen Slots,
4. Regelzone.

Die Bereiche werden über Materialität, Licht, Flächenwechsel und Schatten getrennt. Harte technische Trennlinien sind nicht vorgesehen.

### 2.3 Layout nach Anzahl der Vorgaben

#### Zwei Vorgaben

Zwei große gleichwertige Slots nebeneinander.

```text
[ Vorgabe 1 ]  [ Vorgabe 2 ]
```

#### Drei Vorgaben

Zwei gleichwertige Slots oben und ein gleich großer Slot unten mittig.

```text
[ Vorgabe 1 ]  [ Vorgabe 2 ]
       [ Vorgabe 3 ]
```

Die untere Position erzeugt keine fachliche Rangfolge. Größe, Kontur und typografische Behandlung bleiben gleichwertig.

#### Vier Vorgaben

Ein gleichmäßiges `2 × 2`-Raster.

```text
[ Vorgabe 1 ]  [ Vorgabe 2 ]
[ Vorgabe 3 ]  [ Vorgabe 4 ]
```

### 2.4 Inhalt eines Slots

Ein Slot enthält:

1. eine große Illustration,
2. darunter den sichtbaren Namen der Vorgabe in Small Caps,
3. nur bei einem offenen Konzept gegebenenfalls den Badge **`OFFEN`**.

Es gibt keine allgemeinen Badges wie `PFLICHT`, `KATEGORIE` oder `STÖRENFRIED`.

### 2.5 Konkrete Zutaten und offene Konzepte

#### Konkrete Zutat

- ein einzelnes eindeutig erkennbares Motiv,
- keine zusätzliche Rollenkennzeichnung.

#### Offenes Konzept

- nach Möglichkeit ein gruppiertes Motiv aus zwei bis drei repräsentativen Beispielen,
- ein dezenter Badge `OFFEN`,
- der Text benennt weiterhin das offene Konzept und nicht die gezeigten Stellvertreter.

Beispiel: Für `Blattgemüse` dürfen Pak Choi, Spinat und ein Kohlblatt gemeinsam erscheinen. Die Illustration darf nicht suggerieren, ausschließlich diese Konkretisierungen seien zulässig.

## 3. Verbindliche Geometrie

Die maßhaltigen Ausgangsdateien liegen unter [`wireframes/`](wireframes/). Verbindlich sind:

- Canvas: `1200 × 1200 px`,
- sichtbare Kopfzone bis `Y = 222`,
- Boardfläche: `X = 72`, `Y = 222`, `1056 × 918 px`,
- Regelzone: `X = 120`, `Y = 974`, `960 × 116 px`,
- horizontaler Slotabstand: `48 px`,
- vertikaler Slotabstand: `40 px`,
- zwei große Slots: jeweils `456 × 640 px`,
- drei beziehungsweise vier Slots: jeweils `432 × 300 px`.

[`wireframes/GEOMETRY.md`](wireframes/GEOMETRY.md) dokumentiert die vollständigen Koordinaten, Innenabstände und Textbereiche.

Die Geometrie wurde in Originalgröße und bei `320 × 320 px` geprüft. Zweizeilige Grenzfälle liegen bei der kleinen Ausgabe bereits am unteren sinnvollen Rand; spätere Typografie darf deshalb nicht beliebig verkleinert werden.

## 4. Verbindliche visuelle Richtung

Freigegeben ist **Style Study A – Helles Honigbrett**. Die vollständigen Design-Tokens und Anwendungsregeln stehen unter [`style-studies/VISUAL_FOUNDATIONS.md`](style-studies/VISUAL_FOUNDATIONS.md).

### 4.1 Hintergrund

- warmes Goldorange mit hellem Fokus in der Bildmitte,
- gebrannte Orange- und Rotbrauntöne zu den Rändern,
- dunkle Küchenutensilien und Pflanzenformen als zurückhaltende Silhouetten,
- keine vollfarbigen dekorativen Lebensmittel im Vorgabenbereich.

### 4.2 Board

- helle honigfarbene Ahorn- beziehungsweise Schneidebrettwirkung,
- ruhige illustrative Maserung statt Fototextur,
- dunklere Kante, schlanke Kontur und weicher Außenschatten,
- das Board bleibt die ruhige Hauptbühne für die Challenge-Vorgaben.

### 4.3 Slots

- feste Positionen aus der freigegebenen Geometrie,
- weiche semitransparente Aufhellung,
- zurückhaltende warme Außenkontur und sehr dezente innere Kontur,
- keine harten App-Panels und keine sichtbaren technischen Hilfslinien.

### 4.4 Regelzone

- feste Größe und Position unabhängig vom Vorhandensein einer Regel,
- dunkles Espresso-/Rotbraun mit hellem Text bei einer Einschränkung oder einem Ausschluss,
- kleines eindeutiges Symbol bei einer tatsächlichen Regel,
- ohne Zusatzregel **kein Text** wie `KEINE ZUSATZREGEL`, sondern ausschließlich ein neutrales Würfel-/Linienornament.

### 4.5 Kontrollvariante

Style Study B – Dunkles Gewürzbrett bleibt versioniert, ist aber verworfen. Das dunkle Board erzeugt zu viel visuelles Gewicht und benötigt stärkere helle Slotinseln. Seine Werte sind keine Tokens des späteren Mastertemplates.

## 5. Farbrollen der freigegebenen Richtung

### Hintergrund

- `background-glow-top`: `#F8B327`
- `background-glow-center`: `#F6A51A`
- `background-edge`: `#C77115`
- `background-deep`: `#772D0E`
- `background-silhouette`: `#7C4517`

### Board

- `board-surface-top`: `#F7E0A8`
- `board-surface-bottom`: `#F1CE83`
- `board-edge`: `#D59D56`
- `board-outline`: `#8A5B27`
- `board-shadow`: `rgba(41,20,11,0.18)`

### Text, Slots und Badge

- `text-primary`: `#2A140B`
- `text-secondary`: `#6E4620`
- `slot-surface`: `rgba(255,248,233,0.44)`
- `slot-outline`: `rgba(104,63,22,0.34)`
- `slot-inner-highlight`: `rgba(104,63,22,0.24)`
- `badge-surface`: `#FFF0C7`
- `badge-outline`: `#8A5B27`
- `badge-text`: `#5C3114`

### Regelzone

- `rule-surface`: `#5F2B18`
- `rule-outline`: `#7C4517`
- `rule-text`: `#FFF5DE`
- `neutral-ornament`: `#EBC88C`

## 6. Typografische Hierarchie

- `Mise en Dice` ist die primäre Markenüberschrift.
- `Challenge #NNN` erklärt das Format und trägt die laufende Nummer.
- Vorgabennamen werden in Small Caps gesetzt.
- Der Regeltext ist klar lesbar und gegenüber den Vorgaben nachgeordnet, aber nicht versteckt.
- Dynamische Texte dürfen nicht bis zur Unlesbarkeit verkleinert werden.

Die Style-Studies verwenden noch Systemschriften. Schriftfamilien, echtes Small-Caps-Verhalten, Laufweiten, Zeilenhöhen und das feste Wortlogo werden in einem eigenen Folgepaket festgelegt.

## 7. Konsistenzregeln

- Das Wortlogo wird einmal gestaltet und anschließend als versioniertes Asset verwendet.
- Layout und Typografie werden nicht durch ein Bildmodell pro Challenge neu erzeugt.
- Gleiche Challenge-Daten, gleiche Template-Version und gleiche Asset-Version sollen dasselbe Ergebnis liefern.
- Vorhandene Zutaten- und Konzeptillustrationen werden wiederverwendet.
- Neue Illustrationen werden einzeln erstellt, geprüft und erst danach in die Bibliothek aufgenommen.
- Illustrationen enthalten keinen eigenen Text und keinen individuellen Kartenhintergrund.
- Der sichtbare Vorgabenname bleibt die verbindliche Aussage; die Illustration unterstützt ihn nur.
- Das Bild ersetzt nicht die textuelle Challenge-Darstellung.

## 8. Referenz- und Style-Study-Pakete

### Wireframes

Unter [`wireframes/`](wireframes/) liegen Varianten für zwei, drei und vier Vorgaben sowie die früher geprüften Zustände ohne Zusatzregel. Der ornamentale Zustand ist inzwischen verbindlich.

### Visuelle Grundlagen

Unter [`style-studies/`](style-studies/) liegen:

- die freigegebene Richtung A mit und ohne Regel,
- die Kontrollvariante B mit und ohne Regel,
- SVG-Exporte bei `1200 × 1200 px` und explizite SVG-Prüfexporte bei `320 × 320 px`,
- der deterministische Generator,
- die vollständigen visuellen Tokens.

Ein herunterladbares Paket liegt unter [`packages/challenge-card-visual-foundations-v1.zip`](packages/challenge-card-visual-foundations-v1.zip).

## 9. Noch offene Entscheidungen

- finale Schriftfamilien und Wortlogo,
- verbindliche Regeln für sehr lange Namen und Regeltexte in der finalen Schrift,
- finaler Illustrationsstandard für konkrete Zutaten und offene Konzepte,
- endgültige Feinheit der inneren Slotkontur im Mastertemplate,
- möglicher zusätzlicher Export für Querformat oder andere Plattformen.

## 10. Nächster Schritt

Als nächstes werden Typografie und das feste Wortlogo `Mise en Dice` entwickelt. Erst danach folgt der verbindliche Illustrationsstandard und anschließend das finale Mastertemplate.
