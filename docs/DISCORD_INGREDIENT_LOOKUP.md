# Lesende Discord-Zutatenabfrage

Stand: 19. August 2026

Dieses Dokument beschreibt den verbindlichen Fach- und Adaptervertrag der mit Issue #108 eingeführten Discord-Zutatenabfrage einschließlich des UX-Follow-ups aus Issue #111.

## 1. Ziel und Grundsatz

Georgia und Tobias können Informationen aus dem gepflegten Zutatenkatalog direkt über Discord abrufen und von einer angezeigten Zutat zu ihren direkten allgemeineren Begriffen beziehungsweise bekannten Konkretisierungen navigieren.

Der Ablauf ist ausschließlich lesend:

- keine Anlage, Bearbeitung oder Deaktivierung von Zutaten,
- keine Änderung von Gewichtungen, Beziehungen, Rollen oder Eigenschaften,
- keine neue Persistenz, kein Audit und keine Discord-Message-Persistenz,
- kein Generator-, Kurator-, Voting-, Reroll- oder Challenge-Workflow,
- kein OpenAI-Aufruf.

Discord bleibt Transport und Darstellung. Suche und Detailprojektion gehören als kleine öffentliche Read-API in das `catalog`-Modul; der Discord-Adapter greift weder direkt auf JDBC noch auf administrationsinterne Projektionen zu.

## 2. Command und Zugriff

Der Guild-Slash-Command lautet:

```text
/zutat suche:<Suchtext>
```

`suche` ist genau ein erforderlicher String-Parameter. Autocomplete, Modals oder weitere Filter gehören nicht zum aktuellen Stand.

Der Command verwendet dieselbe private Zugriffspolitik wie `/challenge`:

- ausschließlich die konfigurierte Guild,
- ausschließlich die bereits konfigurierten Discord-Teilnehmer,
- keine gültige Nutzung in DMs oder fremden Guilds.

Die fachliche Antwort ist öffentlich. Reine Autorisierungs-, Eingabe-, Stale- oder technische Fehlermeldungen dürfen ephemer bleiben.

## 3. Suchsemantik

Durchsucht werden alle aktiven Zutatenkonzepte unabhängig von `random_draw_enabled`.

Gesucht wird ausschließlich im sichtbaren Namen `display_name`:

- führende und folgende Leerzeichen werden entfernt,
- Vergleich erfolgt case-insensitive mit stabiler Locale,
- der Suchtext ist ein literaler Teilstring; `%`, `_`, Backslashes und andere Zeichen erhalten keine SQL-Wildcard-Semantik,
- technische Codes werden nicht durchsucht,
- keine Fuzzy-, Synonym-, Transliteration- oder Akzentnormalisierung.

Eine nach dem Trimmen leere Eingabe wird ohne Katalogabfrage verständlich abgewiesen.

Die Trefferentscheidung ist deterministisch:

1. Ein case-insensitiver exakter Namensfund wird unmittelbar angezeigt.
2. Ohne exakten Fund wird ein einzelner Teilstringtreffer unmittelbar angezeigt.
3. Mehrere Teilstringtreffer erzeugen eine öffentliche String-Select-Auswahl.
4. Ohne Treffer erscheint eine kurze verständliche Meldung.

Mehrdeutige Treffer werden sortiert nach:

1. Name beginnt mit dem Suchtext,
2. Suchtext steht an anderer Stelle im Namen,
3. alphabetisch nach sichtbarem Namen,
4. stabil nach Konzept-ID.

Die Auswahl zeigt höchstens 25 Optionen und nennt bei weiteren Treffern die Gesamtzahl. Das sichtbare Label enthält nur den Anzeigenamen; eine optionale Beschreibung darf aktive direkte allgemeinere Begriffe verwenden. Technische IDs und Codes bleiben unsichtbar.

Die initiale Mehrdeutigkeitsauswahl bleibt an den Nutzer gebunden, der `/zutat` aufgerufen hat.

## 4. Öffentliche Katalogprojektion

`IngredientLookupQueries` stellt genau die für diesen Use Case benötigte Read-API bereit:

```text
searchActiveByDisplayName(searchText, limit)
findActiveProfile(conceptId)
```

Das aktive Profil enthält ausschließlich:

- Konzept-ID als opake interne Interaktionsreferenz,
- sichtbaren Namen,
- Ziehbarkeit und Gewichtung,
- Ungewöhnlichkeit,
- aktive direkte Eltern und Kinder als `IngredientLookupRelation(conceptId, displayName)`,
- direkt gepflegte funktionale Rollen,
- direkt gepflegte kulinarische Flags,
- direkt gepflegte kulinarische Dimensionen,
- optionale Kuratornotiz.

Die Relation-ID dient ausschließlich der eindeutigen stateless Discord-Navigation. Sie wird nicht als Nutztext angezeigt. Suchtreffer dürfen für ihre optionale Parent-Beschreibung weiterhin ausschließlich Namen transportieren.

Nicht Teil der Projektion beziehungsweise Darstellung sind Saison, Beschaffbarkeit, technischer Code, Version, Änderungszeitpunkt, Auditdaten, Challenge-Spezifität, transitive Vorfahren/Nachfahren oder Ausschlussregeln.

Nur direkte und zum Zeitpunkt der Abfrage aktive Beziehungen werden geliefert. Rollen, Flags und Dimensionen werden nicht über den Konkretisierungsgraphen vererbt.

## 5. Gewichtung und Ungewöhnlichkeit

Bei einem aktiven ziehbaren Konzept wird die gespeicherte Gewichtung deutsch formatiert angezeigt, beispielsweise:

```text
Gewichtung        0,85
```

Es folgt kein erläuternder Zusatz zur relativen Bedeutung.

Bei einem aktiven, aber nicht eigenständig ziehbaren Konzept wird stattdessen angezeigt:

```text
Gewichtung        nicht eigenständig ziehbar
```

Die Ungewöhnlichkeit verwendet dieselbe fünfstufige verbale Skala wie das Datenmodell. Ein fehlender Wert erscheint ausdrücklich als `nicht gepflegt`; er wird weder als Stufe 0 noch als Stufe 1 interpretiert.

## 6. Zutaten-Card

Das fertige Profil ist ein kompaktes Discord-Embed mit einer festen zurückhaltenden warmen Akzentfarbe. Es folgt dieser Reihenfolge:

1. Titel `🥢 <Anzeigename>`,
2. Basisdaten als Embed-Description ohne separate Überschrift,
3. `Funktion im Gericht` und `Besondere Eigenschaften` als zwei native Inline-Embed-Felder,
4. `🍽️ Geschmacksprofil`,
5. optional `💡 Hinweis aus dem Zutatenkatalog`,
6. `⬆️ Allgemeinere Begriffe`,
7. `⬇️ Bekannte Konkretisierungen`,
8. direkt unter dem Embed die Navigationselemente für vorhandene direkte Beziehungen.

Die beiden Inline-Felder werden nicht durch Leerzeichen oder Tabulatoren als Texttabelle simuliert. Lange Werte im linken Feld verschieben daher die rechte Spalte nicht. Mehrere Werte stehen innerhalb ihres Feldes untereinander; leere Listen erscheinen als `keine`. Auf schmalen Clients darf Discord die Felder untereinander stapeln.

Die Hierarchie steht bewusst am Ende, damit zunächst sämtliche Informationen zum konkret angezeigten Konzept zusammenbleiben und erst danach die Katalognavigation folgt.

## 7. Skalen und Geschmacksprofil

Basisdaten und Geschmacksprofil verwenden mehrzeilige Codeblöcke. Eigenschaftsname, verbale Stufe und Beginn der Symbolskala starten in jeder Zeile an denselben berechneten Textspalten; hinter der Emoji-Skala folgt keine weitere auszurichtende Spalte.

Die verbale Fünfer-Skala lautet:

| Stufe | Bezeichnung |
|---:|---|
| 1 | sehr niedrig |
| 2 | niedrig |
| 3 | mittel |
| 4 | hoch |
| 5 | sehr hoch |

Symbolik:

- Ungewöhnlichkeit: `✨`
- Dominanz: `📣`
- Süße: `🍯`
- Säure: `🍋`
- Bitterkeit: `☕`
- Fettigkeit: `🧈`
- Schärfe: `🌶️`
- Salzigkeit: `🧂`
- Umami: `🍄`
- unbesetzte Position: `○`

Jede gepflegte Skala besitzt exakt fünf Positionen. Nicht gepflegte kulinarische Dimensionen werden vollständig ausgelassen, weil ihr Fehlen keine niedrige Ausprägung behauptet.

Beispiel:

```text
Gewichtung        0,75
Ungewöhnlichkeit  niedrig  ✨✨○○○
```

```text
Dominanz  mittel  📣📣📣○○
Umami     mittel  🍄🍄🍄○○
```

## 8. Kuratornotiz und sichere Katalogtexte

Eine nicht leere `curator_note` wird als `💡 Hinweis aus dem Zutatenkatalog` angezeigt. Leere oder nur aus Whitespace bestehende Notizen erzeugen keinen Abschnitt.

Katalogtexte werden so gerendert, dass sie keine unbeabsichtigten Mentions, Codeblöcke, Links oder Markdown-Strukturen auslösen. Allowed Mentions bleiben vollständig deaktiviert. Unvertrauenswürdiger Katalogtext wird nicht in die ausgerichteten Codeblöcke übernommen.

Kuratornotizen dürfen über mehrere Felder geteilt werden; die Card begrenzt ihre Zahl jedoch so, dass die beiden Hierarchiefelder am Ende nicht durch das Embed-Budget verdrängt werden können. Eine notwendige Kürzung bleibt sichtbar.

## 9. Direkte Hierarchienavigation

Jedes sichtbare aktive direkte Eltern-/Kindkonzept ist navigierbar. Die Navigation verwendet unmittelbar die bereits aus der Profilprojektion bekannte stabile Konzept-ID und ruft `findActiveProfile(conceptId)` auf.

Es gibt ausdrücklich:

- keine erneute Namenssuche,
- kein simuliertes `/zutat suche:<name>`,
- keine neue Persistenz, Session oder Message-State-Tabelle,
- keine technische ID im sichtbaren Discord-Text.

Ein Klick ersetzt dieselbe öffentliche Nachricht durch die aktuelle Card des Zielkonzepts. Wurde das Ziel inzwischen deaktiviert oder entfernt, wird die bestehende sichere Stale-Antwort angezeigt.

Die Navigation ist nicht an den ursprünglichen `/zutat`-Aufrufer gebunden. Jeder konfigurierte Teilnehmer derselben Guild darf eine sichtbare Zutaten-Card weiter navigieren. Die Invoker-Bindung der initialen Mehrdeutigkeitsauswahl bleibt davon unberührt.

### Komponentenwahl

Die Navigation wird für Eltern und Kinder getrennt gerendert:

- 1 bis 4 Ziele: neutrale/sekundäre Buttons,
- ab 5 Zielen: ein String Select für diese Richtung,
- keine Beziehung: kein Navigationselement,
- maximal 25 Selectoptionen.

Bei mehr als 25 direkten Zielen werden die ersten 25 der stabilen alphabetischen Reihenfolge angeboten. Die Card nennt die exakte Restanzahl und verweist für weitere Ziele auf `/zutat`. Lange Anzeigenamen werden innerhalb der Discord-Grenzen sichtbar mit Ellipse gekürzt; der interne Wert bleibt die Konzept-ID.

Component-IDs und Values bleiben versioniert, strikt parsebar und vollständig stateless. Vorhandene Navigation funktioniert damit nach einem App-Restart weiter, sofern das Ziel weiterhin aktiv ist.

## 10. Längenbegrenzung

Discord-Grenzen werden vor dem Senden deterministisch eingehalten. Eine überlange Ausgabe darf nicht erst durch einen Discord-`Bad Request` auffallen.

- Lange Listen werden an semantischen Grenzen gekürzt.
- Jede Kürzung nennt sichtbar die Restmenge beziehungsweise Zeichenmenge.
- Navigation wird auf die dokumentierten Button-/Select-Grenzen beschränkt.
- Die Hierarchiefelder bleiben auch bei langen Kuratornotizen erhalten.
- Die gesamte Card bleibt innerhalb der Embed-, Field-, Component-, Label- und `custom_id`-Grenzen.

## 11. Modul- und Adaptergrenze

- `discord` verwendet ausschließlich `catalog :: api` für die Zutatenabfrage.
- JDA-Typen verbleiben vollständig im `discord`-Adapter.
- Kein JDBC-Zugriff aus Listener, Workflow oder Renderer.
- Keine Wiederverwendung der administrationsorientierten Vollprojektion.
- Keine Schreibtransaktion und kein Katalogaudit durch Suche, Detailanzeige oder Navigation.
- Der bestehende `/challenge`-Flow bleibt unverändert.

`DiscordIngredientLookupRenderer` besitzt ein transportneutrales Render-Modell für Embed-Description, Inline-Felder und Navigation. Erst `DiscordJdaListener` mappt dieses Modell auf JDA-Embeds, sekundäre Buttons und String Selects.

## 12. Fehlerdarstellung und Verifikation

Nutzertexte unterscheiden knapp zwischen ungültiger Eingabe, keinem Treffer, veralteter Navigation/Auswahl und unbekanntem technischen Fehler. Interne Codes, IDs, SQL-Details, Stacktraces und Providerdiagnostik erscheinen niemals in Discord.

Automatisierte Tests decken mindestens ab:

- echte PostgreSQL-Suche und aktive Relation-ID/Name-Projektion,
- exakten Direktfund, Einzel- und Mehrfachtreffer,
- ziehbare/nicht ziehbare Profile und fehlende Werte,
- native Inline-Felder mit langen linken Inhalten,
- verbale Stufen, `○`-Leersymbol und exakt fünf Skalenpositionen,
- Kuratornotiz- und Längenbegrenzung,
- 1..4 Beziehungen als Buttons, ab 5 als Select, mehr als 25 mit sichtbarer Restanzahl,
- direkte Navigation per Konzept-ID ohne Namenssuche,
- Stale-Verhalten nach Deaktivierung,
- stateless Component-Parsing,
- JDA-Routing der Zutaten-Navigation vor dem generischen Challenge-Buttonpfad,
- bestehende `/zutat`-Suche und `/challenge`-Regressionen.

Verpflichtendes Gate:

```bash
./mvnw clean verify
```

Automatisierte Tests und Entwicklung öffnen weder eine echte Discord-Verbindung noch einen echten OpenAI-Zugriff.

## 13. Nicht-Ziele

- Kataloganlage oder -bearbeitung aus Discord,
- Suche in technischen Codes,
- Autocomplete oder freie Filter,
- transitive Hierarchienavigation,
- Vor-/Zurück-History innerhalb Discord,
- Pagination großer Hierarchien,
- Saison- oder Beschaffbarkeitsauskunft,
- Rollen- oder Eigenschaftsvererbung,
- Zutatenempfehlungen, Rezeptvorschläge oder KI-Erklärungen,
- Änderung des bestehenden Challenge-Lifecycles,
- Vorziehen persönlicher Konkretisierungen, Zusatz-Zutaten, Kochpläne, Fotos oder Ergebnisdokumentation.

Nach vollständiger Umsetzung und Live-Abnahme von #111 kann Phase 12E / #90 mit dem privaten Produktionspilot beginnen.
