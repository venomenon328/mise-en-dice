# Verbindliche visuelle Grundlagen

Stand: 20. August 2026  
Status: Style Study A freigegeben

## 1. Freigegebene Richtung

Verbindliche Grundlage ist **Style Study A – Helles Honigbrett**.

Die Karte kombiniert:

- eine warme goldorange Küchenkulisse,
- einen hellen zentralen Lichtfokus,
- dunkle Küchenutensilien als zurückhaltende Silhouetten,
- eine helle honigfarbene Board- beziehungsweise Schneidebrettfläche,
- weiche Slotflächen ohne technische Panelwirkung,
- eine dunkle, klar lesbare Regelzone,
- ein neutrales Würfel-/Linienornament, wenn keine Zusatzregel existiert.

Style Study B – Dunkles Gewürzbrett bleibt als Vergleich erhalten, ist aber nicht die Zielrichtung. Das dunkle Board verschiebt zu viel visuelles Gewicht in den Untergrund und verlangt auffälligere helle Slotinseln.

## 2. Design-Tokens

Die Namen beschreiben Rollen. Spätere Templates sollen diese Rollen verwenden, statt Farbwerte unverbunden zu duplizieren.

### Hintergrund

| Rolle | Wert | Verwendung |
|---|---|---|
| `background-glow-top` | `#F8B327` | heller oberer Goldton |
| `background-glow-center` | `#F6A51A` | zentraler warmer Lichtbereich |
| `background-edge` | `#C77115` | gebrannte Randbereiche |
| `background-deep` | `#772D0E` | dunkelste Vignette und Tiefenakzent |
| `background-silhouette` | `#7C4517` | Utensilien- und Pflanzenformen mit reduzierter Deckkraft |

### Board

| Rolle | Wert | Verwendung |
|---|---|---|
| `board-surface-top` | `#F7E0A8` | obere helle Boardfläche |
| `board-surface-bottom` | `#F1CE83` | unterer Honigton |
| `board-edge` | `#D59D56` | Kante und Fase |
| `board-outline` | `#8A5B27` | schlanke Außenkontur |
| `board-shadow` | `rgba(41,20,11,0.18)` | weicher Außenschatten |

### Slots

| Rolle | Wert | Verwendung |
|---|---|---|
| `slot-surface` | `rgba(255,248,233,0.44)` | ruhige semitransparente Aufhellung |
| `slot-outline` | `rgba(104,63,22,0.34)` | weiche äußere Begrenzung |
| `slot-inner-highlight` | `rgba(104,63,22,0.24)` | sehr dezente innere Kontur |

Die innere Kontur darf im finalen Template weiter reduziert werden. Sie ist keine gestrichelte UI-Hilfslinie und keine eigenständige Karte auf dem Board.

### Text und Badge

| Rolle | Wert | Verwendung |
|---|---|---|
| `text-primary` | `#2A140B` | Titel und Vorgabennamen |
| `text-secondary` | `#6E4620` | Challenge-Nummer und untergeordnete Angaben |
| `badge-surface` | `#FFF0C7` | Badge `OFFEN` |
| `badge-outline` | `#8A5B27` | Badge-Kontur |
| `badge-text` | `#5C3114` | Badge-Text |

Die exakten Schriften, Small-Caps-Umsetzung und das Wortlogo sind noch nicht festgelegt.

### Regelzone

| Rolle | Wert | Verwendung |
|---|---|---|
| `rule-surface` | `#5F2B18` | dunkle Regelzone |
| `rule-outline` | `#7C4517` | Kontur der Regelzone |
| `rule-text` | `#FFF5DE` | Regeltext und Ausschlusssymbol |
| `neutral-ornament` | `#EBC88C` | Würfel-/Linienornament ohne Zusatzregel |

## 3. Materialien und Tiefe

### Hintergrund

- Der Hintergrund ist illustrativ, nicht fotorealistisch.
- Ein heller Fokus liegt in der Bildmitte; Ränder werden dunkler und satter.
- Küchenutensilien erscheinen als ruhige Silhouetten mit geringer Deckkraft.
- Vollfarbige dekorative Lebensmittel sind im Vorgabenbereich verboten, damit sie nicht als zusätzliche Challenge-Zutaten gelesen werden.

### Board

- Die Boardfläche wirkt wie helles Ahorn- oder Honigholz.
- Maserung bleibt breit, zurückhaltend und illustrativ.
- Fotorealistische Holztexturen, aggressive Kontraste und kleinteilige Maserung sind ausgeschlossen.
- Eine schlanke Kontur, eine dunklere Kante und ein weicher Schatten trennen das Board vom Hintergrund.

### Slots

- Slots sind klar auffindbar, aber keine harten UI-Panels.
- Sie verwenden eine sehr leichte Aufhellung, weiche Rundungen und eine zurückhaltende Kontur.
- Die Motive dürfen die Slotfläche visuell dominieren.
- Slotgrößen und Positionen bleiben durch die Wireframe-Geometrie festgelegt.

### Regelzone

- Die Zone bleibt immer in gleicher Größe und Position bestehen.
- Mit Regel enthält sie ein kleines eindeutiges Symbol und maximal zwei Textzeilen.
- Ohne Regel enthält sie **keinen Text** wie `KEINE ZUSATZREGEL`, sondern ausschließlich das neutrale Würfel-/Linienornament.

## 4. Kontrast- und Semantikregeln

- Helle Motive wie Knoblauch oder Tofu benötigen eine sichtbare dunkle Außenkontur beziehungsweise einen ausreichenden Eigenschatten.
- Dunkle Motive wie Aubergine, Nori oder schwarze Bohnen müssen sich klar von Text und Boardkonturen lösen.
- Offene Konzepte verwenden nach Möglichkeit ein gruppiertes Motiv aus zwei bis drei Stellvertretern und den Badge `OFFEN`.
- Das gruppierte Motiv darf keine abschließende Auswahl suggerieren.
- Vorgabennamen bleiben die verbindliche Aussage; Illustrationen dienen der Erkennung.

## 5. Kontrollvariante B

Style Study B verwendet ein dunkles Gewürzbrett und helle Slotinseln. Sie bleibt versioniert, um die verworfene Alternative nachvollziehbar zu machen. Ihre Farben sind nicht Teil des freigegebenen Token-Satzes für das spätere Mastertemplate.

## 6. Nächste Designpakete

1. finale Typografie und festes Wortlogo `Mise en Dice`,
2. verbindlicher Illustrationsstandard für konkrete Zutaten und offene Konzepte,
3. finales Mastertemplate auf Basis der freigegebenen Geometrie und Style Study A.
