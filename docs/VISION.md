# Produktvision: Mise en Dice

Stand: 16. August 2026

## 1. Zweck

Mise en Dice soll Georgia und Tobias regelmäßig eine kreative Koch-Challenge liefern. Beide kochen gerne, mit deutlichem Interesse an ost- und südostasiatischer Küche, sollen durch das System aber ausdrücklich nicht auf diese Küchen festgelegt werden.

Das Tool ist zunächst ausschließlich für den privaten Gebrauch der beiden gedacht. Die daraus entstehenden Gerichte und Erfahrungen können später als wiederkehrendes Instagram-Format dokumentiert werden; die Anwendung selbst muss dafür zunächst keine öffentliche Community-Funktion besitzen.

Der Reiz der Challenge soll nicht primär aus möglichst exotischen Zutaten entstehen. Entscheidend ist eine **ungewohnte, aber grundsätzlich lösbare Kombination von Vorgaben**, die echte Kochentscheidungen erzwingt.

## 2. Leitidee

> **Vier Vorgaben. Zwei Küchen. Drei Freiheiten.**

Beide Personen erhalten dieselben vier Vorgaben und entwickeln daraus unabhängig voneinander ein Gericht.

Eine Vorgabe kann entweder eine **konkrete Zutat** oder eine **allgemeinere Zutatenkategorie** sein.

Beispiele:

- konkret: `Kabeljau`, `Spitzkohl`, `Birne`, `Miso`
- allgemein: `Fisch`, `Kohlgemüse`, `Zitrusfrucht`, `Hülsenfrucht`

Die Challenge soll nicht jede zulässige Entscheidung absichern. Wenn beispielsweise `Fisch` vorgegeben ist, kann eine bestimmte Fischart hervorragend zu den übrigen Vorgaben passen, während eine andere Wahl das Gericht erheblich schwieriger oder sogar misslungen machen kann. **Dieses Risiko ist erwünscht.**

Das System garantiert daher nur eine Art Existenzbeweis:

> Für eine akzeptierte Challenge muss mindestens ein vernünftiger kulinarischer Lösungsweg erkennbar sein. Das System garantiert ausdrücklich nicht, dass jede Konkretisierung, jede Zubereitungsmethode oder jede Ergänzungszutat funktioniert.

Fehlentscheidungen und auch kaum genießbare Ergebnisse sind ein legitimer Teil des Formats.

## 3. Zusammensetzung einer Challenge

### 3.1 Vier Vorgaben

Eine Standard-Challenge besteht aus genau vier Vorgaben.

Aktueller Konsens:

- Mindestens **zwei Vorgaben müssen konkrete Zutaten** sein.
- Die übrigen Vorgaben dürfen Kategorien oder ebenfalls konkrete Zutaten sein.
- Damit sind insbesondere folgende Mischungen zulässig:
  - 2 konkret + 2 allgemein
  - 3 konkret + 1 allgemein
  - 4 konkret
- Vier konkrete Zutaten sind ausdrücklich erwünscht, sofern die Kombination noch mehrere plausible Lösungswege offenlässt und nicht praktisch bereits ein bekanntes Gericht diktiert.

Eine ungewöhnliche oder selten verwendete Zutat hat **keine besondere feste Rolle**. Sie kann genauso gut die offensichtliche Hauptkomponente sein wie jede andere Zutat. Eine Challenge mit `Weinbergschnecken` als tierischer Hauptkomponente ist daher völlig legitim, sofern die Zutat für beide realistisch beschaffbar ist.

Die vier Vorgaben sind nach außen gleichberechtigt. Interne Klassifikationen dürfen zur Generierung und Plausibilitätsprüfung verwendet werden, sollen den Kochenden aber nicht bereits verraten, welche Zutat als Hauptkomponente, Brücke oder „Störenfried“ gedacht war.

### 3.2 Konkretisierung allgemeiner Vorgaben

Allgemeine Vorgaben werden von beiden Personen unabhängig konkretisiert.

Beispiel:

- Vorgabe: `Fisch`
- Georgia entscheidet sich für Kabeljau.
- Tobias entscheidet sich für Lachs.

Die Konkretisierung einer Vorgabe verbraucht **keinen** der zusätzlichen Zutaten-Slots. Sie erfüllt lediglich die ursprüngliche Vorgabe.

Eine Kategorie soll grundsätzlich durch eine erkennbare einzelne Zutat erfüllt werden, nicht durch ein bereits zusammengesetztes Fertigprodukt. Details und Ausnahmen müssen später noch präzisiert werden.

### 3.3 Drei zusätzliche Zutaten

Zusätzlich zu den vier Vorgaben darf jede Person standardmäßig genau **drei weitere Zutaten** frei wählen.

Diese Begrenzung ist ein wesentlicher Bestandteil der Challenge: Die vorgegebenen Zutaten sollen nicht durch eine beliebig große Zahl zusätzlicher Komponenten in den Hintergrund gedrängt werden können.

Welche Küchenbasics ohne Slot erlaubt sind, ist noch abschließend zu spezifizieren. Der bisherige Arbeitsstand sieht eine kleine Gruppe frei verfügbarer Grundlagen vor, beispielsweise:

- Wasser
- Salz
- neutrales Öl
- einzelne trockene Gewürze
- gegebenenfalls Zucker/Honig
- gegebenenfalls grundlegende Aromaten wie Zwiebel, Knoblauch, Ingwer und Chili

Dagegen sollen geschmacksprägende Saucen, Pasten, Beilagen und zusätzliche Hauptzutaten grundsätzlich einen Slot belegen, zum Beispiel:

- Sojasauce, Fischsauce, Austernsauce
- Essig oder Zitrussaft
- Miso, Gochujang, Sambal, Currypaste
- Kokosmilch, Sahne, Joghurt
- Reis, Nudeln, Brot oder andere Beilagen
- Ei
- frische Kräuter
- weiteres Gemüse, Obst oder Protein

Die endgültige Abgrenzung gehört in die spätere Regelspezifikation und sollte nicht implizit im Anwendungscode verstreut werden.

### 3.4 Optionale Einschränkung

Eine Challenge kann zusätzlich eine einzelne Ausschlussregel enthalten, zum Beispiel:

- keine Kokosmilch
- kein Reis
- keine Nudeln
- keine Sojasauce
- keine Milchprodukte
- keine fertige Currypaste

Einschränkungen sollen **nicht in jeder Runde** vorkommen und vor allem offensichtliche Standardlösungen gelegentlich blockieren.

Nicht vorgesehen sind derzeit:

- Utensilienzwänge wie „nur Air-Fryer“ oder „nur ein Topf“
- vorgegebene Geschmacksziele
- vorgegebene Texturziele

Die kreative Richtung des Gerichts soll möglichst frei bleiben.

## 4. Was eine gute Challenge ausmacht

Eine Challenge soll bewusst **nicht** aus einem Zielgericht rückwärts konstruiert werden.

Sie ist geeignet, wenn:

- mindestens ein vernünftiger gemeinsamer Lösungsweg existiert,
- die vier Vorgaben gemeinsam verwendet werden können,
- die Kombination noch mehrere Interpretationen zulässt,
- allgemeine Vorgaben echte, potenziell riskante Auswahlentscheidungen ermöglichen,
- die Zutaten für beide Personen in Deutschland realistisch beschaffbar sind,
- und die Kombination nicht bereits nahezu vollständig ein etabliertes Standardgericht vorgibt.

Sie darf dagegen durchaus:

- anspruchsvoll sein,
- eine ungewöhnliche konkrete Zutat enthalten,
- durch eine ungeschickte Konkretisierung deutlich schwieriger werden,
- bei schlechter Planung scheitern,
- oder sich im Nachhinein als eine schlechte persönliche Entscheidung erweisen.

Nicht akzeptabel wäre eine Ausgangslage, bei der bereits die Vorgaben selbst praktisch keine sinnvolle gemeinsame Lösung zulassen.

## 5. Kuratierter Zufall

Der Zufall soll aus der Anwendung kommen, **nicht aus dem Sprachmodell**.

### 5.1 Kandidatengenerierung

Die Anwendung erzeugt aus der eigenen Datenbasis zunächst mehrere Kandidaten-Challenges, derzeit vorgesehen: **12 Kandidaten pro Auswahlrunde**.

Harte Regeln werden vollständig im eigenen Code geprüft, beispielsweise:

- genau vier Vorgaben,
- mindestens zwei konkrete Zutaten,
- keine logisch redundanten Kombinationen wie gleichzeitig `Fisch` und `Lachs`, sofern dies als zwei eigenständige Vorgaben gemeint wäre,
- nur aktuell zugelassene und für beide beschaffbare Einträge,
- Wiederholungs- und Cooldown-Regeln,
- ausreichende strukturelle Vielfalt des Warenkorbs,
- gegebenenfalls Regeln zur optionalen Einschränkung.

Die Default-Zielverteilung eines Zwölfer-Satzes beträgt vier Kandidaten mit zwei, fünf Kandidaten mit drei und drei Kandidaten mit vier spezifischen Vorgaben. Sie bleibt typisiert konfigurierbar, wird aber nicht pro Bedienvorgang frei verändert. Die vollständigen Regeln stehen in [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md).

### 5.2 Qualitätsmodell des Generators

Der Generator arbeitet zweistufig: Er erzeugt zunächst ein größeres Reservoir harter gültiger Vierer-Kandidaten und wählt daraus anschließend einen diversen Zwölfer-Satz. Weder die ersten zwölf Treffer noch ein reines Top-12-Ranking genügen.

Er trennt:

- Eignung und effektives Gewicht einzelner Vorgaben,
- harte Gültigkeit und weiche Qualität eines Kandidaten,
- Ähnlichkeit und Zielverteilung des gesamten Satzes.

Funktionale Rollen, Spezifität, Beschaffbarkeit, Neuigkeit und der Konkretisierungsgraph tragen die harten Strukturregeln. Kulinarische Dimensionen sind aufgrund ihrer derzeit lückenhaften Abdeckung nur ein niedrig gewichtetes Softsignal; fehlende Werte werden nicht als niedrige Werte ausgelegt. Der Generator prüft damit strukturelle Plausibilität, behauptet aber keine allgemeine paarweise Geschmacksverträglichkeit.

Außergewöhnlichkeit wird innerhalb eines Kandidaten, im Zwölfer-Satz und über bestätigte sichtbare Challenges dosiert. Nach einer abenteuerlichen bestätigten Challenge folgt eine Recovery-Runde ohne abenteuerliche Kandidaten; nach mehreren sehr vertrauten bestätigten Challenges darf ihr Anteil kontrolliert steigen. Ein lediglich präsentiertes und anschließend vollständig rerolltes Offer Set beeinflusst dagegen nur den exakten Zutaten-Cooldown, nicht die Neuigkeitskadenz.

Alle Zufallsentscheidungen verwenden einen gespeicherten Seed, einen benannten RNG, kanonische Eingaben sowie Generator- und Konfigurationsversionen. Harte Regeln werden nie gelockert; begrenzte Fallbacks betreffen ausschließlich dokumentierte Softziele.

Eine optionale Ausschlussregel wird einmal pro Generierungsversuch bestimmt und gilt für alle zwölf Kandidaten und alle internen Runden dieses Versuchs.

### 5.3 Rolle des Sprachmodells und Auswahl mehrerer Angebote

Die OpenAI API erhält ausschließlich die bereits erzeugten Kandidaten und fungiert als **kulinarischer Kurator**.

Der Nutzer kann vor der Erzeugung festlegen, ob **ein, zwei oder drei Challenge-Angebote** präsentiert werden sollen; Default ist eins. Diese Zahl verändert den Generator nicht: Ein Generation Batch enthält weiterhin genau zwölf Kandidaten.

Das Modell soll:

- alle übergebenen Kandidaten als `GOOD`, `ACCEPTABLE` oder `BAD` klassifizieren und innerhalb der relevanten Auswahl ranken,
- grundsätzliche kulinarische Kohärenz prüfen,
- kreative Offenheit berücksichtigen,
- vermeiden, dass ein Kandidat bereits fast eindeutig ein Standardgericht vorgibt,
- bei mehreren gewünschten Angeboten auch deren gegenseitige Verschiedenheit berücksichtigen,
- riskante Entscheidungen innerhalb allgemeiner Kategorien ausdrücklich zulassen.

Das Modell soll **nicht**:

- selbst die vier Vorgaben erfinden,
- die Challenge rückwärts von einem Rezept aus planen,
- eine Musterlösung oder ein Zielgericht erzeugen,
- verraten, welche Konkretisierung einer allgemeinen Kategorie vermutlich die beste wäre,
- oder die harten Regeln der Anwendung überschreiben.

Sind bereits im ersten Zwölfer-Satz genügend `GOOD`-Kandidaten für die gewünschte Zahl von Angeboten vorhanden, endet die Kuratierung nach genau einem API-Aufruf. Sind es zu wenige, darf die Anwendung unter demselben Generierungsversuch **höchstens einmal** einen zweiten Zwölfer-Satz erzeugen und erneut kuratieren. Gute Kandidaten aus Runde 1 bleiben dabei gesetzt; wenige der besten nicht gesetzten Fallbacks dürfen gegen den neuen Satz weiterverglichen werden.

Pro `generation_attempt` sind technisch strikt höchstens **zwei tatsächliche externe Kuratorrequests** erlaubt. Auch technische Retries verbrauchen dieses Budget. Eine fachlich schwache Antwort darf niemals eine unbegrenzte Kette weiterer Aufrufe auslösen.

Nach Ausschöpfung der erlaubten Runden gilt: Wenn mindestens ein Kandidat als `GOOD` bewertet wurde, wird die gewünschte Zahl von Angeboten bei Bedarf mit den bestgerankten `ACCEPTABLE`- und anschließend den am wenigsten problematischen `BAD`-Kandidaten aufgefüllt. Gibt es überhaupt keinen `GOOD`-Kandidaten, endet der Versuch mit einer typisierten Kurationserschöpfung statt einer ausschließlich schlechten Auswahl.

Die API-Antwort soll strukturiert und kompakt sein, insbesondere über Candidate-ID, qualitative Klasse, Rang und feste Reason-Codes. Freie Prosa ist für den produktiven Ablauf nicht notwendig.

Die vollständige Kurations-, Carry-over-, Fallback-, API-Budget-, Bestätigungs- und Reroll-Semantik steht in [`CURATION_AND_CHALLENGE_SELECTION.md`](CURATION_AND_CHALLENGE_SELECTION.md).

Welches konkrete Modell verwendet wird (z. B. Terra oder Sol), ist noch nicht entschieden und soll austauschbar bleiben.

## 6. Reroll

Für das aktuell präsentierte Offer Set ist **genau ein gemeinsamer Reroll** vorgesehen.

Arbeitsregeln:

- Der Reroll verwirft das komplette sichtbare Offer Set mit seinen ein bis drei Optionen, bevor eine Option als Challenge bestätigt wurde.
- Er tauscht weder gezielt eine einzelne Option noch eine einzelne ungeliebte Zutat aus. Ein Reroll bedeutet „diese Kombination beziehungsweise dieses Angebot noch einmal neu“, nicht „diese Zutaten wollen wir nicht“.
- Die exakten Katalogkonzepte aller tatsächlich gezeigten Optionen werden für den normalen Wiederholungs-Cooldown als **ein gemeinsames Expositionsereignis** erfasst. Ein Offer Set mit drei Optionen lässt historische Abstände nicht dreimal schneller altern als eines mit einer Option.
- Der Cooldown wird nicht auf Vorfahren, Nachfahren, Konkretisierungen oder Geschwister erweitert. `Spargel` im verworfenen Angebot sperrt also nicht automatisch `grünen Spargel`.
- Es gibt keinen zusätzlichen REROLL-Zutatenblock und keine Sonderbehandlung sehr hoher Neuigkeitsstufen.
- Die rerollten, aber nicht bestätigten Optionen beeinflussen die Neuigkeitskadenz nicht. Recovery und Seeking-Variety beruhen weiterhin auf bestätigten sichtbaren Challenges.
- Technische Fehler oder intern vom Kurator verworfene Kandidaten verbrauchen den Reroll nicht und erzeugen keinerlei Exposition.
- Wird dagegen eine Option normal bestätigt, beeinflusst nur diese bestätigte Challenge Cooldown und Neuigkeitskadenz; die übrigen Angebote bleiben generatorisch unsichtbar.
- Der Reroll soll von beiden Beteiligten gemeinsam bestätigt werden.
- Nach einem Reroll gibt es keine zweite freiwillige Neuziehung.

## 7. Zutaten- und Kategorienbasis

Die Datenbasis ist zentral für die Qualität der Challenges. Das konkrete fachliche Modell ist inzwischen in [`DATA_MODEL.md`](DATA_MODEL.md) beschrieben; die folgende Produktperspektive bleibt dafür maßgeblich.

Sie muss insbesondere konkrete und allgemeinere Zutatenkonzepte gemeinsam abbilden können:

1. **konkretere Zutaten**, z. B. `Kabeljau`, `Lachs`, `Spitzkohl`, `Miso`
2. **allgemeinere Konzepte**, z. B. `Fisch`, `weißfleischiger Fisch`, `Kohlgemüse`, `Zitrusfrucht`

Eine konkrete Zutat kann mehreren allgemeineren Konzepten zugeordnet sein. Ein starrer einzelner Parent-Child-Baum ist deshalb nicht ausreichend.

Für Auswahl und Kuratierung relevante Informationen sind unter anderem:

- Anzeigename,
- aktiv/inaktiv,
- Challenge-Spezifität,
- bekannte Konkretisierungen,
- funktionale Rollen,
- kulinarische Eigenschaften,
- individuelle Beschaffbarkeit für Georgia und Tobias,
- optionale Ungewöhnlichkeit,
- Ziehungsgewicht,
- Saisonabhängigkeit,
- kurze sachliche Kuratornotizen,
- kuratierte Ausschlussregeln.

Die Datenbasis bleibt bewusst kuratiertes Systemwissen und keine universelle Lebensmittelontologie.

### Beschaffbarkeit

Eine Zutat muss nicht in jedem Discounter liegen. Asia-Läden, Spezialgeschäfte und gegebenenfalls Online-Beschaffung sind legitim, sofern die Beschaffung für beide realistisch bleibt.

Beschaffbarkeit wird individuell gepflegt, da die Einkaufsmöglichkeiten in Rostock und im Raum Bornheim/Köln unterschiedlich sein können.

Außergewöhnliche Zutaten dürfen Teil des Pools sein, sollten aber nur aktiviert werden, wenn ihre tatsächliche Beschaffbarkeit plausibel bestätigt ist.

## 8. Nutzung und Oberfläche

### 8.1 Private Verwaltungsoberfläche

Die private Webverwaltung wird vor Generator und Discord-Bot funktional ausgebaut, damit der umfangreiche Katalog komfortabel geprüft und gepflegt werden kann.

Sie dient insbesondere für:

- Zutatenkonzepte suchen, filtern, anlegen, bearbeiten und deaktivieren,
- den Konkretisierungsgraphen hierarchisch navigieren und direkte Beziehungen pflegen,
- Rollen und kulinarische Eigenschaften zuordnen,
- individuelle Beschaffbarkeit verwalten,
- Ziehungsgewichte, Ungewöhnlichkeit und Saisonfaktoren pflegen,
- Ausschlussregeln verwalten,
- spätere redaktionelle Änderungen über einen Audit-Trail nachvollziehen.

Die konkrete Bedien- und Interaktionsspezifikation steht in [`ADMINISTRATION_UI.md`](ADMINISTRATION_UI.md).

Die Webverwaltung ist kein Ersatz für die eigentliche Challenge-Oberfläche. Sie ist das Werkzeug zur Pflege der Datenbasis.

### 8.2 Discord-Bot

Mise en Dice erhält als eigentliche Challenge-Oberfläche einen **eigenständigen Discord-Bot**. Er ist ausdrücklich kein Modul des bestehenden Gridwords-Bots.

Discord ist vorgesehen für:

- vor einer Ziehung die gewünschte Zahl von `1..3` Challenge-Angeboten wählen; Default `1`,
- Challenge-Angebote ziehen und anzeigen,
- genau eine angebotene Candidate-ID auswählen und ausdrücklich als Challenge bestätigen,
- alternativ vor der Bestätigung den gemeinsamen einmaligen Reroll des gesamten sichtbaren Offer Sets auslösen und bestätigen,
- später persönliche Konkretisierungen und drei Zusatz-Zutaten verdeckt erfassen,
- Entscheidungen beider Personen gleichzeitig offenlegen,
- später gegebenenfalls Ergebnisse und Bewertungen dokumentieren.

Im normalen Auswahlweg wird nur die ausdrücklich bestätigte Option zur operativen Challenge und beeinflusst Cooldown und Neuigkeitskadenz. Nicht gewählte Angebote bleiben für Audit und Replay nachvollziehbar, sind für den Generator aber so zu behandeln, als wären sie nie angeboten worden. Nur wenn das **gesamte** sichtbare Offer Set stattdessen rerollt wird, erzeugen seine exakten Katalogkonzepte die in Abschnitt 6 definierte Cooldown-only-Exposition.

Discord-Bot und Verwaltungsoberfläche verwenden dieselbe fachliche Logik und dieselbe persistente Datenbasis. Eine unnötig verteilte Architektur ist für zwei Benutzer nicht das Ziel.

## 9. Nachgelagerter Challenge-Ablauf

Später kann das System den gesamten Ablauf begleiten:

1. Ein bis drei kuratierte Optionen werden angezeigt; anschließend wird entweder einmalig das gesamte Offer Set rerollt oder genau eine Option ausdrücklich bestätigt.
2. Nach Bestätigung werden ihre vier Vorgaben zur operativen Challenge veröffentlicht.
3. Beide konkretisieren allgemeine Vorgaben unabhängig voneinander.
4. Beide wählen unabhängig ihre drei zusätzlichen Zutaten.
5. Optional wird jeweils ein sehr kurzer Grundplan hinterlegt.
6. Erst wenn beide festgelegt haben, werden die Entscheidungen gegenseitig sichtbar.
7. Beide kochen unabhängig.
8. Nachher werden Gericht, Foto und Fazit dokumentiert.

Ein klassischer Gesamtsieger ist nicht zwingend sinnvoll, insbesondere wenn beide die Gerichte des anderen nicht probieren können. Interessanter sind Vergleich und Reflexion über Auswahlentscheidungen, gelungene Verbindungen und Fehlentscheidungen.

## 10. Öffentliches Format

Die Challenges können später auf Instagram dokumentiert werden, ohne dass die Anwendung selbst dafür zunächst Veröffentlichungsfunktionen benötigt.

Denkbar sind beispielsweise:

- Challenge-Karte mit den vier Vorgaben,
- Offenlegung der jeweiligen Konkretisierungen und drei Freiheiten,
- Bilder der fertigen Gerichte,
- kurzes gemeinsames Fazit,
- Rückblick darauf, welche Entscheidung besonders gut oder besonders fatal war.

Ein bisher diskutierter Formatname ist **„Umami oder Unfall“**. Dieser ist nicht zwingend identisch mit dem technischen Projektnamen `Mise en Dice` und gilt derzeit nicht als endgültige Produktentscheidung.

## 11. Nicht-Ziele der ersten Version

Für den ersten Entwicklungsabschnitt sind ausdrücklich nicht notwendig:

- öffentliche Accounts oder Benutzerregistrierung,
- Community-Abstimmungen,
- Rezeptgenerierung durch KI,
- KI-Bewertung der fertigen Gerichte,
- Video- oder Instagram-Automation,
- komplexe Turnier- oder Punktesysteme,
- Microservice-Architektur,
- allgemeine Unterstützung beliebig vieler Nutzer.

Zunächst soll das System **für exakt zwei bekannte Personen zuverlässig gute Challenge-Ausgangslagen erzeugen**.

## 12. Nächste Schritte

Bereits abgeschlossen sind:

1. fachliche Modellierung der Zutaten-/Kategorien-Datenbasis,
2. umfangreiche initiale Katalogbefüllung,
3. Spring-Boot-/Liquibase-/PostgreSQL-Anwendungsfundament,
4. Spezifikation und Umsetzung der privaten Katalogverwaltung,
5. Spezifikation sowie fachlicher Kern des reproduzierbaren Kandidatengenerators bis einschließlich diverser Zwölfer-Auswahl,
6. persistente Generator-, Replay-, Konkurrenz- und Kalibrierungsgrundlage,
7. strukturierter Kuratorvertrag, begrenzte produktive Orchestrierung und Multi-Offer-Lifecycle,
8. transportneutraler Offer-Decision-/Reroll-Lifecycle für Auswahl von `1..3` Angeboten und Bestätigung genau einer Challenge,
9. transportneutraler Teilnehmer-, Electorate- und Voting-Core mit persistenter Teilnahme.

Als nächste Schritte folgen:

10. ein dünner Discord-Adapter für Darstellung, Interaktion und externe Identitätsauflösung ohne eigene Fachlogik,
11. später: persönliche Auswahl, Historie und Ergebnisdokumentation.

## 13. Leitprinzip in einem Satz

**Mise en Dice soll nicht verhindern, dass wir schlecht kochen — es soll nur sicherstellen, dass wir dafür nicht von Anfang an eine Ausrede haben.**
