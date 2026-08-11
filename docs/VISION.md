# Produktvision: Mise en Dice

Stand: 11. August 2026

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

Die genaue Gewichtung von 2/3/4 konkreten Zutaten ist noch offen und soll später konfigurierbar sein.

### 5.2 Rolle des Sprachmodells

Die OpenAI API erhält ausschließlich die bereits erzeugten Kandidaten und fungiert als **kulinarischer Kurator**.

Das Modell soll:

- einen geeigneten Kandidaten auswählen **oder alle Kandidaten ablehnen**,
- grundsätzliche kulinarische Kohärenz prüfen,
- kreative Offenheit berücksichtigen,
- vermeiden, dass ein Kandidat bereits fast eindeutig ein Standardgericht vorgibt,
- riskante Entscheidungen innerhalb allgemeiner Kategorien ausdrücklich zulassen.

Das Modell soll **nicht**:

- selbst die vier Vorgaben erfinden,
- die Challenge rückwärts von einem Rezept aus planen,
- eine Musterlösung oder ein Zielgericht erzeugen,
- verraten, welche Konkretisierung einer allgemeinen Kategorie vermutlich die beste wäre,
- oder die harten Regeln der Anwendung überschreiben.

Wenn alle zwölf Kandidaten ungeeignet sind, darf die Anwendung intern einen neuen Kandidaten-Satz erzeugen und erneut kuratieren. Eine solche interne Wiederholung zählt nicht als sichtbarer Reroll der Nutzer.

Die API-Antwort soll strukturiert und kompakt sein, beispielsweise über eine Auswahl-ID, Scores und feste Reason-Codes. Freie Prosa ist für den produktiven Ablauf nicht notwendig.

Welches konkrete Modell verwendet wird (z. B. Terra oder Sol), ist noch nicht entschieden und soll austauschbar bleiben.

## 6. Reroll

Für eine bereits sichtbare Challenge ist derzeit **genau ein gemeinsamer Reroll** vorgesehen.

Arbeitsregeln:

- Der Reroll ersetzt die komplette Challenge, nicht nur eine einzelne ungeliebte Vorgabe.
- Die ursprüngliche Challenge bleibt in der Historie als verworfen erhalten.
- Der Reroll darf nur erfolgen, bevor persönliche Konkretisierungen und Zusatz-Zutaten verbindlich festgelegt wurden.
- Der Reroll soll von beiden Beteiligten gemeinsam bestätigt werden.
- Technische Fehler oder intern vom Kurator verworfene Kandidaten verbrauchen den Reroll nicht.
- Nach einem Reroll gibt es keine zweite freiwillige Neuziehung.

## 7. Zutaten- und Kategorienbasis

Die Datenbasis ist zentral für die Qualität der Challenges und der nächste geplante Spezifikationsschritt.

Sie muss mindestens zwei unterschiedliche Konzepte abbilden können:

1. **konkrete Zutaten**, z. B. `Kabeljau`, `Lachs`, `Spitzkohl`, `Miso`
2. **allgemeine Gruppen/Kategorien**, z. B. `Fisch`, `weißfleischiger Fisch`, `Kohlgemüse`, `Zitrusfrucht`

Eine konkrete Zutat kann sinnvollerweise mehreren Gruppen angehören. Ein starrer einzelner Parent-Child-Baum dürfte daher nicht ausreichen.

Voraussichtlich relevante Informationen sind unter anderem:

- Anzeigename
- aktiv/inaktiv
- konkrete Zutat oder allgemeine Kategorie
- Mitgliedschaften bzw. Beziehungen zwischen Zutaten und Kategorien
- funktionale Tags wie Fleisch, Fisch, Gemüse, Obst, Stärke, Würzmittel usw.
- für die Kuratierung relevante Eigenschaften wie mild, dominant, fettig, süß, bitter, fermentiert usw.
- individuelle Beschaffbarkeit für Georgia und Tobias
- Bezugsart, z. B. Supermarkt, größerer Supermarkt, Asia-Laden, Spezialgeschäft oder online
- optionale Seltenheit/Ungewöhnlichkeit
- Ziehungsgewicht
- Cooldown bzw. letzte Verwendungen
- optional Saisonabhängigkeit
- optional kurze sachliche Modellnotizen für wenig bekannte Zutaten

Diese Liste ist ausdrücklich **keine fertige Datenbankspezifikation**. Das Datenmodell soll im nächsten Schritt separat entworfen werden.

### Beschaffbarkeit

Eine Zutat muss nicht in jedem Discounter liegen. Asia-Läden, Spezialgeschäfte und gegebenenfalls Online-Beschaffung sind legitim, sofern die Beschaffung für beide realistisch bleibt.

Beschaffbarkeit sollte individuell gepflegt werden können, da die Einkaufsmöglichkeiten in Rostock und im Raum Bornheim/Köln unterschiedlich sein können.

Außergewöhnliche Zutaten dürfen Teil des Pools sein, sollten aber nur aktiviert werden, wenn ihre tatsächliche Beschaffbarkeit plausibel bestätigt ist.

## 8. Nutzung und Oberfläche

### 8.1 Discord-Bot

Mise en Dice wird als **eigenständiger Discord-Bot** entwickelt. Er ist ausdrücklich kein Modul des bestehenden Gridwords-Bots.

Discord ist zunächst die primäre Benutzeroberfläche für:

- Challenge ziehen
- Challenge anzeigen
- gemeinsamen Reroll auslösen/bestätigen
- später eventuell persönliche Konkretisierungen und drei Zusatz-Zutaten verdeckt erfassen
- Entscheidungen beider Personen gleichzeitig offenlegen
- später gegebenenfalls Ergebnisse und Bewertungen dokumentieren

### 8.2 Verwaltungsoberfläche

Für die Pflege einer größeren Zutatenbasis ist später eine kleine private Weboberfläche sinnvoll. Sie kann insbesondere dienen für:

- Zutaten und Kategorien anlegen/bearbeiten/deaktivieren
- Beziehungen und Tags pflegen
- individuelle Beschaffbarkeit verwalten
- Ziehungsgewichte und Cooldowns konfigurieren
- Challenge-Historie einsehen

Discord-Bot und Verwaltungsoberfläche sollen auf dieselbe fachliche Logik und dieselbe persistente Datenbasis zugreifen. Eine unnötig verteilte Architektur ist für zwei Benutzer nicht das Ziel.

## 9. Nachgelagerter Challenge-Ablauf

Später kann das System den gesamten Ablauf begleiten:

1. Vier Vorgaben werden veröffentlicht.
2. Beide konkretisieren allgemeine Vorgaben unabhängig voneinander.
3. Beide wählen unabhängig ihre drei zusätzlichen Zutaten.
4. Optional wird jeweils ein sehr kurzer Grundplan hinterlegt.
5. Erst wenn beide festgelegt haben, werden die Entscheidungen gegenseitig sichtbar.
6. Beide kochen unabhängig.
7. Nachher werden Gericht, Foto und Fazit dokumentiert.

Ein klassischer Gesamtsieger ist nicht zwingend sinnvoll, insbesondere wenn beide die Gerichte des anderen nicht probieren können. Interessanter sind Vergleich und Reflexion über Auswahlentscheidungen, gelungene Verbindungen und Fehlentscheidungen.

## 10. Öffentliches Format

Die Challenges können später auf Instagram dokumentiert werden, ohne dass die Anwendung selbst dafür zunächst Veröffentlichungsfunktionen benötigt.

Denkbar sind beispielsweise:

- Challenge-Karte mit den vier Vorgaben
- Offenlegung der jeweiligen Konkretisierungen und drei Freiheiten
- Bilder der fertigen Gerichte
- kurzes gemeinsames Fazit
- Rückblick darauf, welche Entscheidung besonders gut oder besonders fatal war

Ein bisher diskutierter Formatname ist **„Umami oder Unfall“**. Dieser ist nicht zwingend identisch mit dem technischen Projektnamen `Mise en Dice` und gilt derzeit nicht als endgültige Produktentscheidung.

## 11. Nicht-Ziele der ersten Version

Für den ersten Entwicklungsabschnitt sind ausdrücklich nicht notwendig:

- öffentliche Accounts oder Benutzerregistrierung
- Community-Abstimmungen
- Rezeptgenerierung durch KI
- KI-Bewertung der fertigen Gerichte
- Video- oder Instagram-Automation
- komplexe Turnier- oder Punktesysteme
- Microservice-Architektur
- allgemeine Unterstützung beliebig vieler Nutzer

Zunächst soll das System **für exakt zwei bekannte Personen zuverlässig gute Challenge-Ausgangslagen erzeugen**.

## 12. Nächste Schritte

1. Fachliche Spezifikation der Zutaten-/Kategorien-Datenbasis
2. Initiale Befüllung mit realistisch beschaffbaren Zutaten und Kategorien
3. Festlegung der harten Generierungsregeln und Gewichtungen
4. Implementierung des Kandidatengenerators
5. Definition des strukturierten Kurator-Requests/-Responses
6. OpenAI-Anbindung und Testvergleich geeigneter Modelle
7. Discord-Flow für Ziehung und einen gemeinsamen Reroll
8. Später: persönliche Auswahl, Historie, Ergebnisdokumentation und Webverwaltung

## 13. Leitprinzip in einem Satz

**Mise en Dice soll nicht verhindern, dass wir schlecht kochen — es soll nur sicherstellen, dass wir dafür nicht von Anfang an eine Ausrede haben.**
