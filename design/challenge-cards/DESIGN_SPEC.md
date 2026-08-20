# Designspezifikation für Challenge-Cards

Stand: 20. August 2026  
Status: Geometrie, visuelle Grundlagen und Typografierichtung sind freigegeben

## 1. Ziel

Eine Challenge-Card stellt eine konkrete Mise-en-Dice-Challenge schnell erfassbar, attraktiv und über viele Ausgaben hinweg eindeutig wiedererkennbar dar.

Das Design verbindet:

- die klare Informationshierarchie eines Küchenplakats,
- die warme illustrative Küchenatmosphäre des Discord-Banners,
- eine helle Board- beziehungsweise Schneidebrettfläche,
- ein deterministisches Layout,
- eine feste typografische Hierarchie, die nicht bei jeder Challenge neu interpretiert wird.

Die Karten entstehen außerhalb des Discord-Bots direkt mit ChatGPT auf Basis der versionierten Dateien in diesem Verzeichnis.

## 2. Verbindliche Informationsarchitektur

### 2.1 Format und Inhalt

- quadratisches Masterformat: **1200 × 1200 px**,
- feste Kopfzone mit **`Mise en Dice`** und **`Challenge #NNN`**,
- dreistellige Challenge-Nummer, beispielsweise `Challenge #001`,
- zentraler Bereich für zwei bis vier Challenge-Vorgaben,
- feste untere Regelzone für eine optionale Einschränkung oder einen Ausschluss,
- keine weiteren Metadaten ohne ausdrücklich beschlossenen Nutzen.

### 2.2 Gesamtkomposition

Der Aufbau enthält dauerhaft:

1. Kopfzone,
2. zentrale Boardfläche,
3. Vorgabenbereich mit festen Slots,
4. Regelzone.

Die Bereiche werden über Materialität, Licht, Flächenwechsel und Schatten getrennt. Harte technische Trennlinien sind nicht vorgesehen.

### 2.3 Layout nach Anzahl der Vorgaben

- **Zwei Vorgaben:** zwei große gleichwertige Slots nebeneinander.
- **Drei Vorgaben:** zwei gleichwertige Slots oben und ein gleich großer Slot unten mittig.
- **Vier Vorgaben:** gleichmäßiges `2 × 2`-Raster.

Die Position eines Slots erzeugt keine fachliche Rangfolge.

### 2.4 Inhalt eines Slots

Ein Slot enthält:

1. eine große Illustration,
2. darunter den sichtbaren Namen der Vorgabe in **Small Caps**,
3. nur bei einem offenen Konzept gegebenenfalls den Badge **`OFFEN`**.

Es gibt keine allgemeinen Badges wie `PFLICHT`, `KATEGORIE` oder `STÖRENFRIED`.

### 2.5 Konkrete Zutaten und offene Konzepte

- Eine konkrete Zutat erhält ein einzelnes eindeutig erkennbares Motiv.
- Ein offenes Konzept erhält nach Möglichkeit ein gruppiertes Motiv aus zwei bis drei repräsentativen Beispielen und einen dezenten Badge `OFFEN`.
- Der Text benennt immer das Konzept selbst und nicht die gezeigten Stellvertreter.

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

Die Geometrie wurde in Originalgröße und bei `320 × 320 px` geprüft. Zweizeilige Grenzfälle liegen bei der kleinen Ausgabe bereits am unteren sinnvollen Rand; Texte dürfen deshalb nicht beliebig weiter verkleinert werden.

## 4. Verbindliche visuelle Richtung

Freigegeben ist **Style Study A – Helles Honigbrett**. Die vollständigen Design-Tokens und Anwendungsregeln stehen unter [`style-studies/VISUAL_FOUNDATIONS.md`](style-studies/VISUAL_FOUNDATIONS.md).

### 4.1 Hintergrund und Board

- warmes Goldorange mit hellem Fokus in der Bildmitte,
- gebrannte Orange- und Rotbrauntöne zu den Rändern,
- dunkle Küchenutensilien und Pflanzenformen als zurückhaltende Silhouetten,
- helle honigfarbene Ahorn-/Schneidebrettwirkung,
- ruhige illustrative Maserung statt Fototextur,
- keine vollfarbigen dekorativen Lebensmittel im Vorgabenbereich.

### 4.2 Slots

- feste Positionen aus der freigegebenen Geometrie,
- weiche semitransparente Aufhellung,
- zurückhaltende warme Außenkontur,
- keine harten App-Panels und keine sichtbaren technischen Hilfslinien im finalen Zustand.

### 4.3 Regelzone

- feste Größe und Position unabhängig vom Vorhandensein einer Regel,
- dunkles Espresso-/Rotbraun mit hellem Text bei einer Einschränkung oder einem Ausschluss,
- kleines eindeutiges Symbol bei einer tatsächlichen Regel,
- ohne Zusatzregel ausschließlich ein neutrales Würfel-/Linienornament und **kein Text** wie `KEINE ZUSATZREGEL`.

### 4.4 Kontrollvariante

Style Study B – Dunkles Gewürzbrett bleibt als verworfene Kontrollvariante versioniert.

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

## 6. Verbindliche Typografierichtung

Freigegeben ist **Typography Study A – Kitchen Editorial**. Die Studien und Begründung liegen unter [`typography-studies/`](typography-studies/).

### 6.1 Wortmarke

- `Mise en Dice` erhält eine warme, leicht editoriale Serif-Anmutung.
- Die Wortmarke ist das charakterstärkste typografische Element und darf sich bewusst von den Nutztexten unterscheiden.
- Die aktuelle Study verwendet eine verfügbare Serif-Referenzschrift; das spätere feste Wortlogo wird als eigenes SVG-Asset eingefroren und nicht pro Karte neu gesetzt.

### 6.2 Nutzschrift

`Challenge #NNN`, Badge und Regeltext verwenden eine kräftige, robuste Sans mit hoher Kleinformat-Lesbarkeit.

### 6.3 Vorgabennamen in Small Caps

- Vorgabennamen werden **nicht als Vollversalien gespeichert**.
- Die Quelldaten bleiben normal geschrieben, beispielsweise `Knoblauch`, `Blattgemüse` oder `Pflanzliches Proteinprodukt`.
- In der Karte werden sie typografisch als **Small Caps** gerendert.
- Die freigegebene Studie verwendet `Go Smallcaps` als reproduzierbare Small-Caps-Referenz mit Sans-Charakter.
- Zweizeilige Namen behalten die gleiche Zeilenhöhe und Hierarchie; sie werden nicht durch beliebige weitere Verkleinerung passend gemacht.

### 6.4 Verworfene Gegenproben

- **Typography Study B – Rounded Pantry:** sympathisch, aber zu verspielt und zu nah an einem niedlichen Markenlook.
- **Typography Study C – Confident Brand:** sehr robust und klar, aber weniger warm und charaktervoll als Study A.

## 7. Konsistenzregeln

- Wortlogo, Geometrie, Farben und Typografierollen werden versioniert und wiederverwendet.
- Layout und Typografie werden nicht durch ein Bildmodell pro Challenge neu erzeugt.
- Gleiche Challenge-Daten, gleiche Template-Version und gleiche Asset-Version sollen dasselbe Ergebnis liefern.
- Vorhandene Zutaten- und Konzeptillustrationen werden wiederverwendet.
- Neue Illustrationen werden einzeln erstellt, geprüft und erst danach in die Bibliothek aufgenommen.
- Illustrationen enthalten keinen eigenen Text und keinen individuellen Kartenhintergrund.
- Der sichtbare Vorgabenname bleibt die verbindliche Aussage; die Illustration unterstützt ihn nur.

## 8. Referenzpakete

- [`wireframes/`](wireframes/): freigegebene Geometrie.
- [`style-studies/`](style-studies/): visuelle Grundlagen und verworfene Kontrollvariante.
- [`typography-studies/`](typography-studies/): Typografievergleich und freigegebene Richtung A.

Das Visual-Foundations-Paket liegt weiterhin unter [`packages/challenge-card-visual-foundations-v1.zip`](packages/challenge-card-visual-foundations-v1.zip).

## 9. Noch offene Entscheidungen

- finale Ausarbeitung und Einfrieren des Wortlogos als eigenes SVG-Asset,
- verbindlicher Illustrationsstandard für konkrete Zutaten und offene Konzepte,
- endgültige Feinheit der inneren Slotkontur im Mastertemplate,
- möglicher zusätzlicher Export für Querformat oder andere Plattformen.

## 10. Nächster Schritt

Als nächstes wird der **Illustrationsstandard für konkrete Zutaten und offene Konzepte** entwickelt. Parallel beziehungsweise unmittelbar davor kann die freigegebene Wortmarkenrichtung als festes SVG-Logoasset eingefroren werden. Danach wird das finale Mastertemplate zusammengesetzt.
