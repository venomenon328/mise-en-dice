# Typografie-Studien für Challenge-Cards

## Ausgangspunkt

Die visuelle Grundrichtung ist durch das gemergte Paket zu den visuellen Grundlagen festgelegt:

- **Style Study A – Helles Honigbrett** ist die verbindliche visuelle Basis,
- die Regelzone ohne Zusatzregel verwendet ein neutrales Ornament,
- die freigegebene Geometrie bleibt unverändert.

Dieses Paket stellt drei kontrollierte Richtungen gegenüber. Nach visueller Auswahl ist **Study A – Kitchen Editorial** verbindlich freigegeben. Die Vorgabennamen werden mit normal geschriebenen Quelldaten typografisch in **Small Caps** gesetzt.

## Gemeinsame Testdaten

Alle Studien verwenden identische Inhalte, damit sich die Beurteilung nicht mit wechselnden Layoutfällen vermischt:

- Wortlogo: `Mise en Dice`,
- `Challenge #012`,
- konkrete Zutaten `Knoblauch` und `Aubergine`,
- offenes Konzept `Blattgemüse` mit Badge `OFFEN`,
- zweizeilige lange Bezeichnung `Pflanzliches Proteinprodukt`,
- zweizeilige Ausschlussregel `AUSSCHLUSS · KEINE KOKOSMILCH ODER KOKOSCREME`.

## Studien

### Study A – Kitchen Editorial

- Wortlogo in warmer, kursiver Serifenschrift,
- alle Nutztexte in kräftiger Sans,
- klare Trennung zwischen Marke und Informationssystem.

**Stärken**
- hohe Eigenständigkeit des Wortlogos,
- warme, leicht kulinarische Anmutung,
- gute Balance aus Charakter und Lesbarkeit.

**Risiken**
- Serifenschrift muss im finalen Logo sauber gewählt werden, damit sie nicht generisch oder zu elegant wirkt.

### Study B – Rounded Pantry

- durchgehend rundere Sans-Schriften,
- freundlichste und spielerischste Richtung.

**Stärken**
- sympathisch und zugänglich,
- formal sehr konsistent.

**Risiken**
- droht am ehesten ins Niedliche abzurutschen,
- wirkt als Markenlogo am wenigsten souverän.

### Study C – Confident Brand

- schweres, markiges Wortlogo in einer robusten Sans,
- sehr klare Nutzschrift mit hoher Lesbarkeit.

**Stärken**
- beste Lesbarkeit auf kleiner Fläche,
- sehr robust für lange Begriffe und Regeltexte,
- technisch am sichersten.

**Risiken**
- weniger warm und charaktervoll als Study A,
- Gefahr eines etwas generischen Brandings, wenn das endgültige Logo nicht sorgfältig verfeinert wird.

## Entscheidung

**Statusänderung durch Issue #130:** Die hier dokumentierte Entscheidung bleibt für die Nutztypografie verbindlich. Die Aussage, dass Study A die finale Wortmarke als reine Serifenausführung bestimmt, ist dagegen abgelöst. Für das Wortlogo sind die expressiven, fontunabhängigen SVG-Studien unter [`../wordmark-studies/`](../wordmark-studies/) maßgeblich, bis eine davon in einem getrennten Paket finalisiert wird.

**Für die Nutztypografie freigegeben ist Study A – Kitchen Editorial.**

- `Challenge #NNN`, `OFFEN` und Regeltext bleiben robuste Sans-Nutzschrift.
- Vorgabennamen werden mit normal geschriebenen Quelldaten über die Small-Caps-Nutzschrift `Go Smallcaps` gesetzt.
- Für das Wortlogo gilt die frühere Serif-Anmutung nicht mehr als Auswahl; die neue Auswahl bleibt bis zur Finalisierung im Review.
- Study B und Study C bleiben als verworfene Gegenproben der **Nutztypografie** nachvollziehbar erhalten.

## Nächster Schritt

Auf Basis der freigegebenen Study A werden:

1. eine expressive Wortmarkenstudie aus [`../wordmark-studies/`](../wordmark-studies/) in einem getrennten Paket finalisiert,
2. die verbindlichen Schriftrollen für `Challenge #NNN`, Vorgabennamen, Badge und Regeltext festgelegt,
3. Grenzfälle für Laufweite, Zeilenumbrüche und Größen eingefroren,
4. erst danach der Illustrationsstandard für Zutaten und offene Konzepte entwickelt.
