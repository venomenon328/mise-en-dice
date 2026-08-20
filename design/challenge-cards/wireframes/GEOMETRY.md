# Geometrie der Low-Fidelity-Wireframes

Stand: 20. August 2026  
Status: maßhaltiger Vorschlag zur visuellen Freigabe; noch kein finales Mastertemplate

## 1. Koordinatensystem

Alle Maße beziehen sich auf das quadratische Masterformat `1200 × 1200 px` mit dem Ursprung links oben.

| Element | X | Y | Breite | Höhe | Anmerkung |
|---|---:|---:|---:|---:|---|
| Canvas | 0 | 0 | 1200 | 1200 | neutrales Wireframe-Feld |
| äußerer Sicherheitsbereich | 60 | 48 | 1080 | 1092 | sichtbare Inhalte unterschreiten diese Zone nicht |
| Boardfläche | 72 | 222 | 1056 | 918 | Radius `44 px` |
| Vorgabenbereich | 120–1080 | 278–918 | 960 | 640 | maximal nutzbare Slotfläche |
| Regelzone | 120 | 974 | 960 | 116 | Radius `28 px` |

Die Kopfzone reicht visuell bis zum Beginn der Boardfläche bei `Y = 222` und beansprucht damit rund `18,5 %` der Kartenhöhe. Die Regelzone bleibt bei allen Varianten geometrisch unverändert.

## 2. Kopfzone

| Element | Mittelpunkt/Basislinie | Wireframe-Schriftgröße |
|---|---:|---:|
| `Mise en Dice` | `X = 600`, Basislinie `Y = 120` | `74 px` |
| `Challenge #NNN` | `X = 600`, Basislinie `Y = 180` | `34 px` |

Die verwendete Arial-/Helvetica-Kaskade ist lediglich ein neutraler Platzhalter. Sie ist weder Logo- noch finale Typografieentscheidung.

## 3. Slotgeometrie

### Zwei Vorgaben

| Slot | X | Y | Breite | Höhe |
|---|---:|---:|---:|---:|
| 1 | 120 | 278 | 456 | 640 |
| 2 | 624 | 278 | 456 | 640 |

- horizontaler Abstand: `48 px`,
- linker und rechter Abstand zur Boardkante: `48 px`,
- beide Slots sind vollständig gleichwertig.

### Drei Vorgaben

| Slot | X | Y | Breite | Höhe |
|---|---:|---:|---:|---:|
| 1 | 144 | 278 | 432 | 300 |
| 2 | 624 | 278 | 432 | 300 |
| 3 | 384 | 618 | 432 | 300 |

- horizontaler Abstand der oberen Slots: `48 px`,
- vertikaler Abstand: `40 px`,
- der dritte Slot ist exakt horizontal zentriert,
- Größe und Behandlung bleiben identisch; die untere Position bedeutet keine fachliche Gewichtung.

### Vier Vorgaben

| Slot | X | Y | Breite | Höhe |
|---|---:|---:|---:|---:|
| 1 | 144 | 278 | 432 | 300 |
| 2 | 624 | 278 | 432 | 300 |
| 3 | 144 | 618 | 432 | 300 |
| 4 | 624 | 618 | 432 | 300 |

- horizontale Lücke: `48 px`,
- vertikale Lücke: `40 px`,
- seitlicher Abstand zur Boardkante: `72 px`.

## 4. Innere Slotbereiche

Die gestrichelten Begrenzungen zeigen im Wireframe ausschließlich die reservierte Geometrie. Sie sind **keine Vorgabe für sichtbare Panels im finalen Design**.

### Kleine Slots (`432 × 300 px`)

- Motivzone: `48 px` seitlicher Innenabstand, `28 px` oberer Innenabstand, Höhe `154 px`,
- optionaler `OFFEN`-Badge: `116 × 32 px`, horizontal zentriert, Oberkante `188 px` unterhalb der Slotoberkante,
- Namenszone: `24 px` seitlicher Innenabstand, Oberkante `220 px`, Höhe `64 px`,
- einzeiliger Name: Basislinie `264 px` unterhalb der Slotoberkante,
- zweizeiliger Name: Basislinien `246 px` und `278 px` unterhalb der Slotoberkante.

### Große Slots (`456 × 640 px`)

- Motivzone: `48 px` Innenabstand, Höhe `390 px`,
- Namenszone: `36 px` seitlicher Innenabstand, Oberkante `500 px`, Höhe `92 px`,
- einzeiliger Name: Basislinie `558 px` unterhalb der Slotoberkante,
- zweizeiliger Name: Basislinien `540 px` und `576 px` unterhalb der Slotoberkante.

Der Badge sitzt bewusst oberhalb der Namenszone und nicht frei im Motiv. Dadurch bleibt seine Position unabhängig von hohen, breiten oder gruppierten Illustrationen.

## 5. Regelzone

Die Regelzone besitzt in allen Wireframes dieselben Außenmaße.

- Symbolmittelpunkt: `X = 182`, `Y = 1032`,
- reservierter Symboldurchmesser: `60 px`,
- Textbeginn: `X = 238`,
- nutzbare Textbreite bis zum rechten Innenrand: ungefähr `818 px`,
- einzeiliger Text: `32 px`,
- zweizeiliger Grenzfall: `28 px` mit `35 px` Basislinienabstand.

Für Challenges ohne Zusatzregel liegen zwei bewusst konkurrierende Varianten vor:

1. `challenge-card-no-rule-text.svg` bestätigt neutral `KEINE ZUSATZREGEL`.
2. `challenge-card-no-rule-ornament.svg` hält die Zone ausschließlich mit einem dezenten Würfelornament stabil.

Die Zone wird in keinem Fall zusammengeklappt; andernfalls würden Board und Zutatenlayout zwischen Challenges springen.

## 6. Prüfung bei `320 × 320 px`

Der Skalierungsfaktor beträgt `0,2667`. Daraus ergeben sich ungefähr:

| Element | Größe bei 1200 px | Größe bei 320 px |
|---|---:|---:|
| Markenüberschrift | 74 px | 19,7 px |
| Challenge-Zeile | 34 px | 9,1 px |
| normaler Vorgabenname | 34 px | 9,1 px |
| zweizeiliger Grenzfall | 29 px | 7,7 px |
| normaler Regeltext | 32 px | 8,5 px |
| zweizeiliger Regeltext | 28 px | 7,5 px |

Die langen Grenzfälle liegen damit bereits am unteren sinnvollen Rand. Eine spätere breitere Schrift darf nicht durch beliebiges weiteres Verkleinern kompensiert werden. Bevorzugt werden dann ein kuratiertes Anzeige-Label, angepasste Laufweite oder eine fachlich saubere Zeilentrennung.

## 7. Noch nicht festgelegt

Die Wireframes entscheiden noch nicht über:

- Farbe und Materialität der Boardfläche,
- konkrete weiche Slotbegrenzung,
- finale Schriften und echtes Small-Caps-Verhalten,
- Logoausführung,
- Form und Farbe des `OFFEN`-Badges,
- endgültige Symbolsprache der Regelzone,
- Auswahl der Variante ohne Zusatzregel.
