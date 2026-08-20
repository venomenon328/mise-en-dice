# Designspezifikation für Challenge-Cards

Stand: 20. August 2026  
Status: Low-Fidelity-Wireframes liegen zur visuellen Freigabe vor

## 1. Ziel

Eine Challenge-Card soll eine konkrete Mise-en-Dice-Challenge schnell erfassbar, attraktiv und über viele Ausgaben hinweg eindeutig wiedererkennbar darstellen.

Die Gestaltung verbindet:

- die klare Informationshierarchie eines Küchenplakats,
- die warme, illustrative Küchen- und Schneidebrettatmosphäre des Discord-Banners,
- ein deterministisches Layout, das nicht bei jeder Challenge neu erfunden wird.

## 2. Verbindlich beschlossene Grundregeln

### 2.1 Format und Inhalt

- quadratisches Masterformat: **1200 × 1200 px**,
- feste Kopfzone mit **`Mise en Dice`** und **`Challenge #NNN`**,
- dreistellige Challenge-Nummer, beispielsweise `Challenge #001`,
- zentraler Bereich für zwei bis vier Challenge-Vorgaben,
- fester unterer Bereich für eine optionale Einschränkung oder einen Ausschluss,
- keine zusätzlichen Metadaten ohne einen später ausdrücklich beschlossenen Nutzen.

### 2.2 Gesamtkomposition

Die strukturelle Grundlage ist ein klar gegliedertes Küchenplakat. Es wird atmosphärisch durch eine warme Küchenkulisse und eine zentrale Board- beziehungsweise Schneidebrettfläche angereichert.

Der Aufbau enthält dauerhaft:

1. Kopfzone,
2. zentrale Boardfläche,
3. Zutaten- beziehungsweise Vorgabenbereich,
4. Regelzone.

Die Bereiche dürfen durch Flächenwechsel, Licht, Schatten oder Materialität getrennt werden. Harte technische Trennlinien sind nicht vorgesehen.

### 2.3 Vorgaben-Slots

Die Vorgaben werden weder frei über die Fläche verteilt noch in sterile UI-Panels gezwängt. Jede Vorgabe besitzt einen festen Slot, dessen Grenze weich und dekorativ vermittelt wird, beispielsweise durch:

- eine dezent aufgehellte Fläche,
- eine organische Kontur,
- einen sanften Schatten,
- eine kleine Bodenellipse oder einen ruhigen Etikettbereich.

Die gestrichelten Grenzen der Low-Fidelity-Wireframes markieren ausschließlich die reservierte Geometrie. Sie sind kein Vorschlag für sichtbare technische Kartenrahmen.

### 2.4 Layout nach Anzahl der Vorgaben

#### Zwei Vorgaben

Zwei große, gleichwertige Slots nebeneinander.

```text
[ Vorgabe 1 ]  [ Vorgabe 2 ]
```

#### Drei Vorgaben

Zwei gleichwertige Slots oben, ein gleich großer Slot unten mittig.

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

### 2.5 Inhalt eines Slots

Ein Slot enthält grundsätzlich:

1. eine große Illustration,
2. darunter den sichtbaren Namen der Vorgabe in **Small Caps**,
3. nur bei einem offenen Konzept gegebenenfalls einen kleinen Badge **`OFFEN`**.

Es gibt keine allgemeinen Badges wie `PFLICHT`, `KATEGORIE` oder `STÖRENFRIED`:

- Die dargestellten Vorgaben sind ohnehin verbindlich.
- Eine formale Rolle `Störenfried` ist nicht Bestandteil des aktuellen Challenge-Modells.
- Eine offene Vorgabe wird nur dann zusätzlich gekennzeichnet, wenn die Unterscheidung von einer konkreten Zutat für das Verständnis nützlich ist.

### 2.6 Konkrete Zutaten und offene Konzepte

#### Konkrete Zutat

- ein einzelnes, eindeutig erkennbares Motiv,
- keine zusätzlichen Rollenkennzeichnungen.

#### Offenes Konzept

- nach Möglichkeit ein gruppiertes Motiv aus zwei bis drei repräsentativen Beispielen,
- ein dezenter Badge `OFFEN`,
- der Text benennt weiterhin das offene Konzept und nicht die gezeigten Beispiele.

Beispiel: Bei `Blattgemüse` dürfen Pak Choi, Spinat und ein Kohlblatt als Stellvertreter erscheinen. Die Illustration darf nicht den Eindruck erwecken, ausschließlich diese Konkretisierungen seien zulässig.

Im aktuellen Wireframe sitzt der Badge horizontal zentriert unmittelbar oberhalb der Namenszone. Diese Position ist ein konkreter Prüfgegenstand und noch keine endgültige visuelle Ausformung.

### 2.7 Typografische Hierarchie

- `Mise en Dice` ist die primäre Markenüberschrift.
- `Challenge #NNN` erklärt das Format und trägt die laufende Nummer.
- Vorgabennamen werden in Small Caps gesetzt.
- Der Regeltext ist klar lesbar und gegenüber den Vorgaben nachgeordnet, aber nicht versteckt.
- Dynamische Texte dürfen nicht bis zur Unlesbarkeit verkleinert werden.

Die Wireframes verwenden neutrale Systemschriften und angenäherte Versalien. Konkrete Schriftfamilie, echtes Small-Caps-Verhalten, Zeilenhöhe und Laufweite folgen erst in der Typografiephase.

### 2.8 Regelzone

Die Regelzone ist ein fester, geometrisch stabiler Bereich am unteren Kartenrand. Sie kann eine Einschränkung oder einen Ausschluss enthalten, beispielsweise:

```text
KEINE KOKOSMILCH
```

Die Darstellung darf durch ein kleines Symbol und eine zurückhaltende Akzentfarbe unterstützt werden. Die Zone wird auch ohne Zusatzregel nicht zusammengeklappt, damit die Zutatenkomposition zwischen Challenges nicht springt.

Für den Zustand ohne Zusatzregel liegen zwei Wireframe-Varianten zur Auswahl vor:

- neutrale Bestätigung `KEINE ZUSATZREGEL`,
- rein dekoratives Würfelornament ohne Text.

## 3. Visuelle Richtung aus dem Discord-Banner

Die Datei [`references/discord-bot-banner-reference.jpg`](references/discord-bot-banner-reference.jpg) ist die derzeitige Markenreferenz.

Zu übernehmende Gestaltungsmerkmale:

- warmes Goldorange, gebranntes Orange, dunkle Braun- und Schwarztöne,
- kräftige comicartige Illustrationen mit klaren dunklen Konturen,
- hölzerne Arbeits- oder Schneidebrettfläche,
- Küchenutensilien als dunkle Silhouetten,
- zentral gerichtetes warmes Licht,
- kleine Würfel-, Kräuter- oder Gewürzdetails als Dekoration.

Nicht ungeprüft zu übernehmen:

- vollfarbige dekorative Lebensmittel im eigentlichen Vorgabenbereich,
- die breite Bannerkomposition,
- zufällige oder wechselnde Positionen von Logo, Texten und Motiven.

Auf der finalen Karte sollten möglichst nur die tatsächlichen Challenge-Vorgaben als vollfarbige Lebensmittel erscheinen. Dekoration darf nicht wie ein zusätzlicher Challenge-Bestandteil wirken.

## 4. Konsistenzregeln

- Das Logo beziehungsweise der feste Schriftzug wird einmal gestaltet und anschließend als versioniertes Asset verwendet.
- Layout und Typografie werden nicht in jeder Challenge durch ein Bildmodell neu erzeugt.
- Gleiche Challenge-Daten, gleiche Template-Version und gleiche Asset-Version sollen zum gleichen Ergebnis führen.
- Vorhandene Zutaten- und Konzeptillustrationen werden wiederverwendet.
- Neue Illustrationen werden einzeln erstellt, geprüft und erst danach in die Bibliothek aufgenommen.
- Illustrationen enthalten keinen eigenen Text und keinen individuellen Kartenhintergrund.
- Die Karte bleibt zusätzlich durch Text beziehungsweise ein Discord-Embed beschreibbar; das Bild ist nicht die einzige fachliche Quelle.

## 5. Low-Fidelity-Wireframe-Paket

Die maßhaltigen Vorschläge liegen unter [`wireframes/`](wireframes/):

- `challenge-card-2.svg`,
- `challenge-card-3.svg`,
- `challenge-card-4.svg`,
- `challenge-card-no-rule-text.svg`,
- `challenge-card-no-rule-ornament.svg`.

[`wireframes/GEOMETRY.md`](wireframes/GEOMETRY.md) dokumentiert alle Pixelkoordinaten, Abstände, Textbereiche und die Skalierungsprüfung bei `320 × 320 px`.

Die SVGs werden deterministisch durch [`wireframes/generate_wireframes.py`](wireframes/generate_wireframes.py) erzeugt und validiert. Das verhindert, dass die Varianten bei späteren Korrekturen unbemerkt auseinanderlaufen.

### 5.1 Geometrischer Vorschlag

- sichtbare Kopfzone bis `Y = 222`,
- Boardfläche `X = 72`, `Y = 222`, `1056 × 918 px`,
- Regelzone `X = 120`, `Y = 974`, `960 × 116 px`,
- Slotabstände von `48 px` horizontal und `40 px` vertikal,
- zwei große Slots mit `456 × 640 px`,
- drei beziehungsweise vier Slots mit `432 × 300 px`.

Diese Maße sind nun präzise prüfbar, aber erst nach visueller Zustimmung verbindlich für das Mastertemplate.

### 5.2 Abgedeckte Grenzfälle

- ein offenes Konzept mit gruppiertem Motiv und `OFFEN`-Badge,
- breiter Name `SCHWEINEBAUCH`,
- zweizeiliger Name `PFLANZLICHES PROTEINPRODUKT`,
- zweizeilige Regel `KEINE KOKOSMILCH ODER KOKOSCREME`,
- zwei alternative Zustände ohne Zusatzregel,
- Rasterung in Originalgröße und bei `320 × 320 px`.

Die kleinsten zweizeiligen Texte liegen bei `320 × 320 px` bereits am unteren akzeptablen Rand. Spätere Typografie darf deshalb nicht durch beliebiges weiteres Verkleinern passend gemacht werden.

## 6. Noch offene Entscheidungen

- visuelle Freigabe oder Korrektur der vorgeschlagenen Pixelgeometrie,
- Auswahl der Text- oder Ornamentvariante ohne Zusatzregel,
- exakte Form und Materialität der Boardfläche,
- konkrete weiche Slotbegrenzung,
- finale Schriftfamilien und Logoausführung,
- verbindliche Farbwerte,
- endgültige Position, Form und Farbe des `OFFEN`-Badges,
- verbindliche Regeln für sehr lange Namen und Regeltexte in der finalen Schrift,
- konkrete Stilvorgaben für Zutaten- und Konzeptillustrationen,
- möglicher zusätzlicher Export für Querformat oder andere Plattformen.

## 7. Nächster Schritt

Nach visueller Freigabe der Wireframes werden:

1. die Geometriemaße eingefroren oder gezielt korrigiert,
2. die Variante ohne Zusatzregel festgelegt,
3. Farbpalette und Materialität entwickelt,
4. Typografie und fester `Mise en Dice`-Schriftzug ausgewählt,
5. anschließend die genaue Slot-Optik und der Illustrationsstandard definiert.

Erst danach entsteht die erste vollständig gestaltete Testkarte.
