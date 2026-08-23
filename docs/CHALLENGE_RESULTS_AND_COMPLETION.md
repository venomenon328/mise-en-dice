# Challenge-Ergebnisse, Abschluss und öffentliche Statussicht

Stand: 23. August 2026
Status: verbindliche Fach-, Persistenz- und Discord-Spezifikation; Umsetzung in getrennten Entwicklungspaketen

Dieses Dokument spezifiziert die dauerhafte Erfassung tatsächlich gekochter Ergebnisse, den ausdrücklich administrierten Abschluss einer Challenge sowie die Trennung zwischen letzter bestätigter und aktuell aktiver Challenge.

Es baut auf [`CHALLENGE_ARCHIVE_AND_CARDS.md`](CHALLENGE_ARCHIVE_AND_CARDS.md), [`CHALLENGE_VOTING_AND_PARTICIPATION.md`](CHALLENGE_VOTING_AND_PARTICIPATION.md), [`PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md`](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md), [`DATA_MODEL.md`](DATA_MODEL.md) und [`ARCHITECTURE.md`](ARCHITECTURE.md) auf.

Bei Widersprüchen ersetzt dieses Dokument für zukünftige Pakete insbesondere:

- die Ableitung einer „aktuellen“ Challenge ausschließlich als fachlich laufende Challenge,
- die Annahme genau einer aktiven Challenge,
- die Nutzung von `challenge_participation` als Voraussetzung für persönliche Daten,
- sowie die bisherigen bewussten Nicht-Ziele zu Ergebnis und tatsächlichem Abschluss aus Phase 13.

## 1. Ziel und zentrale Semantik

Mise en Dice soll nach einer Challenge dauerhaft festhalten können:

- welche Person tatsächlich ein Gericht umgesetzt hat,
- wie dieses Gericht heißt,
- welche frei gewählten Nicht-Basic-Zutaten sie verwendet hat,
- wie sie Gericht beziehungsweise Rezept beschreibt,
- optional wie sie das Ergebnis bewertet,
- und optional welches Foto das Ergebnis dokumentiert.

Es gibt keine vorgelagerte verbindliche Anmeldung zu einer Challenge.

> Die Existenz eines Ergebnisses ist der Nachweis der Teilnahme.

Daraus wird ausdrücklich **nicht** abgeleitet:

- wer noch ein Ergebnis schuldet,
- wer nur Interesse angekündigt hat,
- ob alle Mitglieder eines früheren Electorates gekocht haben,
- oder wann eine Challenge automatisch abgeschlossen werden müsste.

Der Abschluss einer Challenge ist eine eigene ausdrückliche Admin-Entscheidung.

## 2. Ergebnis gehört direkt zu Challenge und Person

Ein Ergebnis referenziert unmittelbar:

```text
challenge
participant
```

Es referenziert nicht `selection_electorate` und nicht `challenge_participation`.

Verbindlich:

- Pro Challenge und Teilnehmer existiert höchstens ein aktuelles Ergebnis.
- Die Person muss nicht im früheren Electorate gewesen sein.
- Die Person muss nicht vorher über `/teilnehmer anlegen` registriert worden sein.
- Bei einer unbekannten Discord-Identität wird sie gemäß [`PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md`](PARTICIPANT_AND_ELECTORATE_MANAGEMENT.md) atomar als Teilnehmer angelegt.
- Diese automatische Anlage erzeugt keine Elektoratsmitgliedschaft und keine Beschaffbarkeitsdaten.
- Bestehende Ergebnisse bleiben bei späterer Deaktivierung der Person vollständig erhalten.

## 3. Inhalt und Pflichtfelder eines Ergebnisses

Ein fachlich vollständiges Ergebnis enthält zwingend:

- einen nicht leeren Namen des Gerichts,
- eine nicht leere Beschreibung des Gerichts, der Umsetzung oder des Rezepts.

Optional sind:

- null bis mehrere selbst gewählte Zutaten,
- null bis eine persönliche Konkretisierung je historisch offener Challenge-Vorgabe,
- eine textuelle persönliche Bewertung,
- ein Ergebnisfoto.

Ein fehlendes Foto oder eine fehlende Bewertung erzeugt keinen Draft-, Offen- oder Unvollständig-Status. Das Ergebnis ist trotzdem ein normales vollständiges Ergebnis.

Der erste Stand erzwingt keine strukturierte Rezeptnotation, Mengenliste, Arbeitsschritte, Sternebewertung oder Bewertungsrubrik.

### 3.1 Feldgrenzen

Für Discord-Bedienbarkeit und defensive Persistenz gelten zunächst:

- Gerichtsname: erforderlich, getrimmt, höchstens 200 Zeichen,
- Beschreibung/Rezept: erforderlich, getrimmt, höchstens 4.000 Zeichen,
- Bewertung: optional; wenn vorhanden getrimmt und höchstens 4.000 Zeichen,
- eigene Zutaten: `0..25` Einträge,
- Text einer eigenen Zutat: erforderlich, getrimmt, höchstens 200 Zeichen.

Leere optionale Texte werden als `null` beziehungsweise nicht vorhanden behandelt, nicht als inhaltsleere Bewertung gespeichert.

Die technische Obergrenze von 25 Zutaten ist keine fachliche Erlaubnis, 25 Zusatzzutaten zu verwenden. Sie verhindert lediglich unbeschränkte Eingaben und hält spätere Modi offen.

Die Anwendung kontrolliert in diesem Paket nicht nachträglich, ob die aktuell geltende Challenge-Regel zu den erlaubten Zusatz-Zutaten eingehalten wurde.

### 3.2 Persönliche Konkretisierungen offener Vorgaben

Eine persönliche Konkretisierung erfüllt eine bereits gesetzte offene Challenge-Vorgabe und ist deshalb **keine eigene Zusatz-Zutat**. Sie gehört zum Ergebnis und zu genau einer Requirement-Position `1..4` des bestätigten Candidate-Snapshots. Nur Positionen mit historischer Spezifität `OPEN` sind zulässig; für `SPECIFIC`-Positionen und unbekannte Positionen wird die Eingabe abgewiesen. Pro Ergebnis und Position existiert höchstens eine Konkretisierung.

Konkretisierungen bleiben optional. Weder eine fehlende Konkretisierung noch ein vollständig ohne Konkretisierungen gespeichertes älteres Ergebnis wird dadurch unvollständig. Der Core prüft keine vollständige nachträgliche Gerichtsregel, sondern ausschließlich die sichere Zuordnung zur offenen Vorgabe.

Der maßgebliche Freitext wird getrimmt, darf nicht leer sein und ist auf 200 Zeichen begrenzt. Eine optionale Katalogreferenz ersetzt ihn niemals. Sie ist nur zulässig, wenn das gewählte Konzept ein direkter oder transitiver Nachfahr des damaligen offenen Vorgabenkonzepts im bestehenden `ingredient_refinement`-Graphen ist. Inaktive bekannte Nachfahren bleiben sichtbar und referenzierbar; `ohne Katalogreferenz` und katalogfreier Freitext bleiben immer gültig. Es gibt kein fuzzy oder semantisches Auto-Matching.

## 4. Persistenzziel

Die konkreten Namen dürfen im Implementierungspaket sinnvoll geschärft werden. Das Zielmodell entspricht mindestens:

```text
challenge_result
- id
- challenge_id
- participant_id
- dish_name                 not null
- description               not null
- evaluation                nullable
- version                    not null
- created_at
- updated_at

UNIQUE (challenge_id, participant_id)
```

Eigene Zutaten werden getrennt modelliert:

```text
challenge_result_ingredient
- id
- challenge_result_id
- display_text
- ingredient_concept_id     nullable
```

Persönliche Konkretisierungen werden nochmals getrennt modelliert:

```text
challenge_result_concretization
- challenge_result_id       FK challenge_result, ON DELETE CASCADE
- requirement_position      1..4
- display_text              historische Autorität
- ingredient_concept_id     nullable Auswertungsreferenz

PRIMARY KEY (challenge_result_id, requirement_position)
```

Ein optionales Foto wird getrennt gespeichert:

```text
challenge_result_photo
- challenge_result_id       primary key
- content_bytes
- content_type
- original_filename
- byte_size
- width
- height
- sha256
- created_at
- updated_at
```

Die Abwesenheit einer `challenge_result_photo`-Zeile bedeutet schlicht, dass kein Foto gespeichert ist.

Gründe für die Trennung:

- Listen und Textprojektionen laden keine Bildbytes.
- Zutatenreferenzen bleiben einzeln auswertbar.
- Fotoersatz verändert nicht unnötig die fachlichen Textspalten.
- Challenge-Card und Ergebnisfoto bleiben klar verschiedene Medienarten.

## 5. Eigene Zutaten und optionale Katalogreferenz

### 5.1 Freitext ist Autorität

Jeder Eintrag besitzt einen maßgeblichen `display_text`.

Dieser Text:

- darf einem Katalogbegriff entsprechen,
- darf spezifischer als der Katalog sein,
- darf im Katalog überhaupt nicht existieren,
- und bleibt bei späterer Umbenennung oder Deaktivierung eines Katalogkonzepts unverändert.

`ingredient_concept_id` ist ausschließlich eine optionale Auswertungsreferenz.

### 5.2 Keine feste Reihenfolge

Eigene Zutaten besitzen keine fachliche Reihenfolge und deshalb keine `position`.

Für die Anzeige dürfen sie beispielsweise alphabetisch sortiert werden. Die Persistenz behauptet nicht, dass eine Zutat „erste“, „zweite“ oder „dritte“ Zusatz-Zutat sei.

Case-insensitiv identische getrimmte Texte dürfen innerhalb desselben Ergebnisses nicht doppelt gespeichert werden.

### 5.3 Keine Erzwingung von drei Einträgen

Die Datenbank erzwingt weder exakt drei noch mindestens eine eigene Zutat.

Insbesondere ist es bereits im normalen Regelwerk zulässig, weniger als die maximal erlaubte Zahl zu verwenden. Spätere Challenge-Modi dürfen andere Grenzen besitzen, ohne eine Schemamigration zu benötigen.

### 5.4 Suche und Zuordnung

Die Erfassung darf eine Katalogreferenz nicht allein deshalb leer lassen, weil der erste Freitextabgleich keinen eindeutigen Treffer liefert.

Der Admin-Workflow bietet deshalb eine optionale Zuordnungsstufe ähnlich der vorhandenen `/zutat`-Suche:

1. Der Freitext wird immer als fachliche Autorität erfasst.
2. Ein eindeutiger case-insensitiver exakter Treffer auf Name oder Code darf vorgeschlagen werden.
3. Für nicht eindeutige oder nicht gefundene Einträge kann der Admin eine literale Teilstringsuche starten.
4. Höchstens 25 priorisierte Treffer werden zur Auswahl gezeigt.
5. Die Suche darf für diesen Admin-Anwendungsfall auch inaktive Konzepte liefern, muss sie aber sichtbar kennzeichnen.
6. `ohne Katalogreferenz` bleibt jederzeit eine ausdrückliche gültige Auswahl.
7. Fuzzy Matching oder semantische Ähnlichkeit erzeugen niemals ungefragt eine Referenz.
8. Das spätere Setzen, Ändern oder Entfernen der Referenz verändert den gespeicherten Freitext nicht.

Die Zuordnung ist optional und darf die erfolgreiche Speicherung eines ansonsten vollständigen Ergebnisses nicht blockieren.

## 6. Optionales Ergebnisfoto

Ein Ergebnis besitzt null oder ein aktuelles Foto.

Unterstützt werden zunächst:

- PNG,
- JPEG,
- höchstens `10 MiB`,
- tatsächliche decodierbare Bilddaten,
- positive Breite und Höhe,
- höchstens 50 Millionen Pixel.

Dateiendung oder deklarierter Content-Type allein genügen nicht. Der Core prüft Signatur, Dekodierbarkeit, Dimensionen und Größe.

Die exakten Uploadbytes werden ohne Re-Encoding, Skalierung oder Zuschnitt als PostgreSQL-`bytea` gespeichert. Zusätzlich werden kanonischer Content-Type, ursprünglicher Dateiname, Bytezahl, Dimensionen und SHA-256 gespeichert.

Eine Challenge-Card bleibt davon unabhängig:

- Card: festes gestaltetes `1200 × 1200`-PNG,
- Ergebnisfoto: optionales Teller- oder Kochfoto in zulässigem PNG-/JPEG-Format.

Ein Foto darf später gesetzt, ersetzt oder entfernt werden, ohne die übrigen Ergebnisdaten erneut vollständig einzugeben.

## 7. Anlegen, Ersetzen, Bearbeiten und Entfernen

Der transportneutrale Core bietet mindestens sinngemäße Operationen für:

```text
createChallengeResult(challengeNumber, participantId, resultData, optionalPhoto)
replaceChallengeResult(challengeNumber, participantId, resultData, optionalPhoto)
updateChallengeResult(challengeNumber, participantId, expectedVersion, changes)
removeChallengeResult(challengeNumber, participantId)
setChallengeResultPhoto(challengeNumber, participantId, photo, replaceExisting)
removeChallengeResultPhoto(challengeNumber, participantId)
setResultIngredientReference(resultIngredientId, ingredientConceptId | none)
updateResultConcretizations(challengeNumber, participantId, expectedVersion, concretizations)
setResultConcretizationReference(resultId, requirementPosition, ingredientConceptId | none, expectedVersion)
```

Verbindlich:

- Ein neues Ergebnis wird atomar einschließlich seiner Zutaten und eines optionalen Fotos gespeichert.
- Ein vorhandenes Ergebnis wird niemals stillschweigend überschrieben.
- Vollständiger Ersatz erfordert eine ausdrückliche Bestätigung.
- Beim Replace bleiben `created_at` und die fachliche Identität erhalten; `updated_at` ändert sich.
- Textkorrekturen und Konkretisierungsänderungen dürfen das vorhandene Foto unverändert lassen.
- Foto setzen, ersetzen und entfernen sind eigenständige ausdrückliche Operationen.
- Entfernen des Ergebnisses löscht Ergebnis, Zutaten und gegebenenfalls Foto atomar.
- Es gibt im ersten Stand kein Versionsarchiv gelöschter oder ersetzter Ergebnisse.
- Ergebnisse dürfen unabhängig vom Challenge-Status ergänzt, bearbeitet, ersetzt oder entfernt werden.

Optimistisches Locking oder eine gleichwertige explizite Versionsprüfung verhindert, dass zwei Admin-Bearbeitungen einander still überschreiben.

## 8. Admin-exklusiver Nachrichten-Kontextbefehl

Der bevorzugte Discord-Ablauf beginnt auf einer normalen Ergebnisnachricht:

```text
Apps → Als Challenge-Ergebnis erfassen
```

Der Befehl ist ausschließlich für die bestehende Challenge-Operator-Rolle verfügbar.

### 8.1 Drei verschiedene Personenrollen

Der Ablauf unterscheidet ausdrücklich:

1. den Admin, der den Kontextbefehl ausführt,
2. den Autor der ausgewählten Discord-Nachricht,
3. die Person, der das Ergebnis fachlich zugeordnet wird.

Nur Nummer 3 wird als `participant` am Ergebnis gespeichert.

Weder Admin noch Nachrichtenautor werden automatisch als Ergebnisinhaber angenommen. Der Admin wählt die Person ausdrücklich über einen Discord-User-Select aus.

Damit sind insbesondere gültig:

- Georgia postet ihr eigenes Ergebnis und Tobias erfasst es,
- Tobias postet Georgias Foto und ordnet es Georgia zu,
- ein Dritter postet Text oder Bild für eine bislang unbekannte Person,
- der Admin erfasst sein eigenes Ergebnis aus einer eigenen Nachricht.

### 8.2 Nachrichtentext bleibt sichtbar und kopierbar

Nach Auswahl des Kontextbefehls zeigt der Bot zunächst eine ephemere Vorbereitungsansicht mit:

- dem vollständigen verfügbaren Nachrichtentext in kopierbarer Form,
- den erkannten unterstützten Bildanhängen,
- Personen- und Challenge-Auswahl,
- sowie der Aktion zum Öffnen der eigentlichen Eingabemaske.

Der Nachrichtentext wird zusätzlich in das Feld `Beschreibung/Rezept` der Eingabemaske übernommen, soweit er in die Discord-Feldgrenze passt.

Es wird niemals still abgeschnitten. Bei Überlänge wird dies sichtbar gemeldet; der vollständige Text bleibt in der Vorbereitungsansicht beziehungsweise der ursprünglichen Nachricht zugänglich.

Die ursprüngliche Discord-Nachricht wird nicht verändert.

### 8.3 Eingabemaske

Die eigentliche Textmaske enthält mindestens:

- Gerichtsname, erforderlich,
- Beschreibung/Rezept, erforderlich und möglichst aus dem Nachrichtentext vorbelegt,
- Bewertung, optional,
- eigene Zutaten, optional und zeilenweise.

Person und Challenge werden bereits in der Vorbereitungsansicht eindeutig gewählt und müssen nicht in freie Textfelder der Maske gequetscht werden.

Da diese Textmaske alle fünf Discord-Modalfelder nutzt, zeigt die Vorbereitung bei mindestens einer `OPEN`-Vorgabe einen getrennten Schritt `Konkretisierungen eingeben`. Er enthält höchstens ein optionales Feld je offener Vorgabe, also maximal vier Felder. Leere Felder erzeugen keine Zeile. Die Vorbereitung zeigt erfasste Werte als `Vorgabe → Konkretisierung`; sie werden bis zur finalen Speicherung nur im kurzlebigen Adapter-Draft gehalten.

### 8.4 Bildauswahl

- Gibt es genau einen unterstützten Bildanhang, darf er vorausgewählt werden.
- Der Admin kann ausdrücklich `ohne Foto` fortfahren.
- Gibt es mehrere unterstützte Bildanhänge, wählt der Admin höchstens einen oder ausdrücklich keinen.
- Gibt es keinen unterstützten Bildanhang, wird die Ergebniserfassung normal ohne Foto fortgesetzt.
- Autorisierung erfolgt vor Download der Bildbytes.
- Ein späteres Foto kann über eine eigene Admin-Operation ergänzt werden.

### 8.5 Challenge-Auswahl

- Existiert genau eine aktive Challenge, wird sie vorausgewählt.
- Existieren mehrere aktive Challenges, muss der Admin eine auswählen.
- Existiert keine aktive Challenge, wird die letzte bestätigte Challenge vorgeschlagen.
- Der Admin darf ausdrücklich auch eine andere bestätigte, bereits abgeschlossene Challenge wählen.
- Status und öffentliche Nummer werden in der Auswahl sichtbar dargestellt.

### 8.6 Kurzlebiger Draft

Vor dem finalen Speichern muss kein dauerhafter fachlicher Draft entstehen.

Kurzlebige Discord-Interaktionsdaten dürfen Zielnachricht, Auswahl und Attachments vorübergehend referenzieren. Sie werden nicht als Ergebnisquelle in PostgreSQL gespeichert. Ein durch Restart oder Zeitablauf veralteter Draft kann erneut über den Kontextbefehl gestartet werden.

### 8.7 Keine persistierte Ursprungsnachricht

Nicht gespeichert werden:

- Discord-Message-ID,
- Channel-ID der Ergebnisnachricht,
- Guild-Jump-URL,
- Autor der Ursprungsnachricht als Ergebnisattribut,
- Discord-CDN-URL des Bildes.

Die dauerhafte Autorität sind ausschließlich die Ergebnisdaten und gegebenenfalls exakten Bildbytes in PostgreSQL.

## 9. Weitere Discord-Admin-Operationen

Der Root-Command `/challenges` wird mindestens ergänzt um:

```text
/challenges abschließen [nummer:<Challenge-Nummer>]
/challenges ergebnis-bearbeiten nummer:<Challenge-Nummer> person:<Discord-Nutzer>
/challenges ergebnis-entfernen nummer:<Challenge-Nummer> person:<Discord-Nutzer>
/challenges ergebnis-foto-setzen nummer:<Challenge-Nummer> person:<Discord-Nutzer> bild:<Attachment> [ersetzen:<bool>]
/challenges ergebnis-foto-entfernen nummer:<Challenge-Nummer> person:<Discord-Nutzer>
```

Semantik:

- `ergebnis-bearbeiten` bietet getrennte Schritte für Gerichtsname/Beschreibung/Bewertung/eigene Zutaten und für persönliche Konkretisierungen; vorhandene Werte sind jeweils sichtbar, das Foto bleibt unverändert.
- `ergebnis-entfernen` erfordert eine ausdrückliche Bestätigung.
- `ergebnis-foto-setzen` setzt oder ersetzt ausschließlich das Foto.
- `ergebnis-foto-entfernen` entfernt ausschließlich das Foto.
- Alle Mutationen sind operatorgebunden und antworten zunächst ephemer.

Ein öffentlicher Self-Service-Command für Teilnehmer ist nicht Bestandteil dieses Pakets.

## 10. Challenge-Abschluss

Die bestehende `challenge.status`-Spalte wird für den tatsächlichen administrativen Abschluss geschärft.

Für neu bestätigte operative Challenges gilt weiterhin:

```text
ACTIVE
```

Der Admin kann ausdrücklich überführen nach:

```text
COMPLETED
```

Zusätzlich wird mindestens gespeichert:

```text
challenge.completed_at
```

Verbindlich:

- Abschluss erfolgt niemals automatisch aufgrund einer Ergebniszahl.
- Es gibt keine erwartete Teilnehmermenge.
- Eine Challenge darf mit null, einem oder mehreren Ergebnissen abgeschlossen werden.
- Eine neue Challenge schließt ältere Challenges nicht automatisch.
- Mehrere `ACTIVE`-Challenges sind fachlich und technisch zulässig.
- Wiederholtes Abschließen derselben bereits abgeschlossenen Challenge ist idempotent.
- `ACTIVE → COMPLETED` setzt `completed_at` genau einmal.
- Es gibt keinen normalen `/challenges wiedereröffnen`-Command.
- Ergebnisänderungen bleiben auch nach Abschluss zulässig.
- Abschluss verändert keine Challenge-Nummer, Card, Generatorhistorie, Cooldown-Exposition oder Ergebnisdaten.

Ohne `nummer` darf `/challenges abschließen` nur dann automatisch auflösen, wenn genau eine aktive Challenge existiert. Bei keiner oder mehreren aktiven Challenges wird eine ausdrückliche Nummer verlangt.

Andere bestehende Statuswerte wie `ABANDONED` oder historisches `REROLLED` werden durch dieses Paket nicht automatisch umgedeutet. Ein Ergebnis darf dennoch an jede bestätigte Challenge mit öffentlicher Nummer angehängt werden; der Status ist kein Schreibschutz.

## 11. „Letzte“ und „aktive“ Challenge

Der bisherige Begriff `aktuell` wird in der öffentlichen Bedienung aufgeteilt.

### 11.1 Letzte Challenge

Die **letzte Challenge** ist immer die bestätigte Challenge mit der höchsten öffentlichen `challenge_number`, unabhängig vom Status.

```text
/challenges letzte
```

zeigt genau diese Challenge.

Eine abgeschlossene letzte Challenge bleibt die letzte, bis eine neue Challenge erfolgreich bestätigt wird.

### 11.2 Aktive Challenges

```text
/challenges aktiv [seite:<n>]
```

zeigt alle Challenges mit Status `ACTIVE`, sortiert nach `challenge_number DESC`.

Die Liste darf leer sein, genau einen Eintrag oder mehrere Einträge enthalten. Im Discord-Adapter werden zunächst zehn Einträge pro Seite verwendet.

### 11.3 Bestehende Commands

Der öffentliche Command-Satz lautet künftig mindestens:

```text
/challenges letzte
/challenges aktiv
/challenges liste
/challenges anzeigen
```

`/challenges aktuell` wird durch `/challenges letzte` ersetzt, statt als dritter nahezu gleichbedeutender Begriff dauerhaft parallel weiterzulaufen.

`karte-setzen` ohne ausdrückliche Nummer bezieht sich weiterhin auf die letzte Challenge, nicht auf eine möglicherweise mehrdeutige aktive Challenge.

## 12. Öffentliche Archivprojektion

Die transportneutrale Archiv-API wird erweitert beziehungsweise begrifflich geschärft:

```text
findLatestChallenge()
listActiveChallenges(page, pageSize)
findChallengeByNumber(challengeNumber)
listChallenges(page, pageSize)
listChallengeResults(challengeNumber)
loadChallengeResultPhoto(challengeNumber, participantId)
```

Eine öffentliche Challenge-Projektion enthält zusätzlich mindestens:

- Status,
- `completedAt`, falls abgeschlossen,
- Zahl der gespeicherten Ergebnisse,
- weiterhin Card-Verfügbarkeit.

Die Detailprojektion enthält die Ergebnisübersichten mit:

- stabiler Teilnehmerreferenz,
- darstellbarem Namen,
- verpflichtendem Gerichtsname,
- eigenen Zutaten als gespeicherten Texten,
- persönlichen Konkretisierungen in Requirement-Reihenfolge samt historischem Vorgaben-Anzeigetext,
- Beschreibung/Rezept,
- optionaler Bewertung,
- Information über ein gegebenenfalls vorhandenes Foto,
- Erfassungs- und Änderungszeitpunkt.

Bildbytes bleiben getrennt von Text- und Listenprojektionen.

## 13. Discord-Darstellung

### 13.1 Challenge-Liste

Ein Listeneintrag zeigt kompakt mindestens:

- öffentliche Nummer,
- vier Challenge-Vorgaben,
- Status `aktiv`, `abgeschlossen` oder vorhandenen anderen Status,
- Zahl der Ergebnisse,
- Card-Verfügbarkeit wie bisher.

Die höchste Nummer darf als `letzte` markiert werden. `aktiv` ist davon unabhängig.

### 13.2 Challenge-Detail

Nach Challenge-Fakten und optionaler Card werden die vorhandenen Ergebnisse dargestellt.

Pro Ergebnis mindestens:

```text
🍽️ <Person> – <Gerichtsname>

Konkretisierungen
• <historische offene Vorgabe> → <persönlicher Freitext>

Eigene Zutaten
• ...

Gericht / Umsetzung
...
```

Die Rubrik `Eigene Zutaten` darf bei null Einträgen als `keine angegeben` erscheinen oder kompakt entfallen. Die Rubrik `Bewertung` wird nur bei vorhandener Bewertung angezeigt. Ein Foto wird nur bei vorhandenen Bilddaten als natives Discord-Attachment ausgeliefert.

Die Rubrik `Konkretisierungen` steht vor `Eigene Zutaten` und entfällt vollständig, wenn kein Eintrag gespeichert ist. Links wird immer der historische Requirement-Snapshot, rechts immer der gespeicherte Konkretisierungs-Freitext angezeigt; eine Katalogreferenz ersetzt keinen der Texte.

Bei mehreren Ergebnissen sind getrennte öffentliche Nachrichten beziehungsweise Follow-ups zulässig, damit Text- und Attachmentgrenzen nicht zu einer unlesbaren Sammelkarte führen.

Nicht angezeigt werden:

- früheres Electorate,
- einzelne Votes,
- Wahlalternativen,
- Tie-Break,
- vermeintlich noch fehlende Teilnehmer,
- `challenge_participation`-Zeilen,
- interne Ergebnis- oder Datenbank-IDs.

## 14. Application- und Modulgrenzen

Ergebnis-, Abschluss- und Archivsemantik gehören in das `challenge`-Modul.

Das Challenge-Modul verwendet für optionale Zutatenreferenzen eine schmale öffentliche Katalog-API. Es greift nicht direkt auf interne Katalog-Repositories zu.

Der Discord-Adapter darf ausschließlich:

- Operatorrolle und Guild prüfen,
- Nachrichteninhalt und Attachments entgegennehmen,
- Personen-, Challenge- und Katalogauswahl darstellen,
- transportneutrale Commands aufrufen,
- Query-Projektionen rendern.

Er besitzt keine eigene Ergebnis-, Abschluss-, Replace-, Bildvalidierungs- oder Katalogmatch-Fachlogik.

## 15. Persistenz- und Konkurrenzinvarianten

PostgreSQL bleibt letzte Integritätssicherung. Mindestens abzusichern sind:

- Ergebnis eindeutig je Challenge und Teilnehmer,
- nicht leerer Gerichtsname und nicht leere Beschreibung,
- höchstens ein optionales Foto je Ergebnis,
- Zutatenzeile gehört genau zu einem Ergebnis,
- Konkretisierung gehört eindeutig zu Ergebnis und einer historisch `OPEN`en Requirement-Position,
- höchstens eine Konkretisierung je Ergebnis und Position,
- optionale Katalogreferenz verweist auf ein existierendes Konzept,
- Konkretisierungsreferenz verweist nur auf einen bekannten direkten oder transitiven Nachfahren der offenen Vorgabe,
- case-insensitiv identischer Zutatenfreitext höchstens einmal je Ergebnis,
- Challenge und Teilnehmer existieren,
- ein unbekannter Discord-Nutzer wird bei paralleler Erfassung höchstens einmal angelegt,
- stilles Überschreiben eines vorhandenen Ergebnisses ist ausgeschlossen,
- konkurrierende Bearbeitung erkennt einen Versionskonflikt,
- Replace von Texten, Zutaten und gegebenenfalls Foto ist atomar,
- Fotooperationen verlieren keine parallelen Text- oder Konkretisierungsänderungen,
- Entfernen löscht das vollständige Ergebnis atomar,
- `completed_at` und `COMPLETED` bleiben konsistent,
- konkurrierender Abschluss setzt denselben finalen Zustand genau einmal,
- Ergebnisänderung und Abschluss dürfen einander nicht unnötig blockieren oder Daten verlieren,
- unbekannte `DataAccessException` wird nicht als fachlicher Konflikt maskiert.

Bereits veröffentlichte Changesets bleiben append-only.

## 16. Migration und Kompatibilität

Die Einführung muss mindestens:

- `completed_at` append-only ergänzen,
- vorhandene Challenges und Nummern unverändert erhalten,
- für bestehende `COMPLETED`-Zeilen einen konsistenten, dokumentierten Backfill vornehmen oder den fehlenden historischen Zeitpunkt ausdrücklich als nicht rekonstruierbar behandeln,
- neue Ergebnistabellen leer einführen,
- bestehende Cards unverändert erhalten,
- bisherige `/challenges aktuell`-Semantik auf `letzte` überführen,
- keine `challenge_participation`-Zeile in ein Ergebnis umdeuten,
- und keine Auswahl-, Vote- oder Kuratorhistorie öffentlich exponieren.

## 17. Tests

Mindestens erforderlich sind:

1. Ergebnisanlage für bekannte Person,
2. Ergebnisanlage mit atomarem Resolve-or-Create einer unbekannten Discord-Person,
3. Nachrichtenautor, Admin und Ergebnisinhaber dürfen drei verschiedene Personen sein,
4. keine Ursprungsnachrichten-ID oder CDN-URL wird persistiert,
5. Gerichtsname und Beschreibung sind erforderlich,
6. fehlende Bewertung und fehlendes Foto sind zulässig,
7. null, eine und mehrere eigene Zutaten sind zulässig,
8. mehr als drei eigene Zutaten werden auf Persistenzebene nicht als Regelverstoß behandelt,
9. doppelte Zutatenfreitexte werden case-insensitiv abgewiesen,
10. eindeutiger Katalogmatch, Suchauswahl und ausdrückliches `ohne Referenz`,
11. Katalogreferenzänderung verändert den Freitext nicht,
12. Ergebniserfassung aus einer Nachricht ohne Bild funktioniert,
13. gültiges PNG und JPEG können optional gesetzt werden,
14. Ablehnung falscher Signatur, beschädigter Datei, Übergröße und pathologischer Pixelzahl,
15. exakte Bildbytes, SHA-256 und Metadaten bleiben nach Restart erhalten,
16. Foto setzen, ersetzen und entfernen verändert übrige Ergebnisdaten nicht,
17. stilles Überschreiben eines vorhandenen Ergebnisses wird abgewiesen,
18. ausdrücklicher Replace ist atomar,
19. Textbearbeitung kann vorhandenes Foto erhalten,
20. Entfernen löscht Ergebnis, Zutaten und gegebenenfalls Foto,
21. Ergebnisse bleiben nach Abschluss bearbeitbar,
22. Abschluss mit null, einem und mehreren Ergebnissen,
23. mehrere aktive Challenges sind zulässig,
24. `letzte` verwendet immer die höchste Challenge-Nummer,
25. `aktiv` liefert null, eine oder mehrere aktive Challenges stabil neueste zuerst,
26. parameterloser Abschluss funktioniert nur bei genau einer aktiven Challenge,
27. Liste und Detail zeigen Status und Ergebniszahl,
28. optionale Bewertung und optionales Foto werden nur bei Vorhandensein dargestellt,
29. Detail zeigt keine Electorate-, Vote-, Offer- oder Kuratorhistorie,
30. Operatorprüfung erfolgt vor Attachment-Download und Mutation,
31. Modulgrenzen und `./mvnw clean verify` bleiben grün.

Persistenz-, Migration-, Bild-, Konkurrenz- und Transaktionstests verwenden echtes PostgreSQL über Testcontainers. Discord-Adaptertests verwenden lokale Fixtures und keine echten Gateway- oder CDN-Aufrufe.

## 18. Entwicklungspakete

Die Umsetzung wird in drei getrennte Pakete geschnitten:

1. **Transportneutraler Ergebnis-/Abschlusskern**: Schema, Application-APIs, optionales Foto, Katalogreferenzen, Statusübergang und Archivprojektionen.
2. **Discord-Status- und Ergebnisdarstellung**: `letzte`, `aktiv`, Abschlusscommand sowie öffentliche Ausgabe bereits gespeicherter Ergebnisse und optionaler Fotos.
3. **Discord-Erfassung und Pflege**: Nachrichten-Kontextbefehl, Vorbereitungsansicht, Textmaske, optionale Zutatenzuordnung, automatische Personenanlage sowie Bearbeiten-, Entfernen- und Fotooperationen.

Der komplexe Nachrichten-/Modalablauf wird bewusst nicht mit der Kernmigration oder der reinen Statusdarstellung in ein einzelnes Paket gepresst.

## 19. Nicht-Ziele

- keine verbindliche Anmeldung zu einer Challenge,
- keine Anzeige „noch offen“ oder automatische Erinnerung an vermeintlich fehlende Ergebnisse,
- kein automatischer Abschluss,
- kein Wiedereröffnen über den normalen Bot-Flow,
- kein Self-Service-Formular für Teilnehmer,
- kein strukturiertes vollständiges Rezeptsystem,
- keine Zutatenregel-Polizei bei der Ergebnisspeicherung,
- keine Sterne-, Punkte- oder Ranglistenbewertung,
- keine Kommentare, Likes oder soziale Feeds,
- keine mehreren Fotos pro Ergebnis,
- keine Speicherung der Discord-Ursprungsnachricht,
- kein öffentliches Voting- oder Kuratoraudit im Archiv,
- keine automatische Bilderkennung oder OCR,
- keine Webgalerie in diesem Paket,
- keine Umsetzung der explorativen Challenge-Modi.
