# Lesende Discord-Zutatenabfrage

Stand: 26. August 2026

Dieses Dokument beschreibt den verbindlichen Fach- und Adaptervertrag der mit Issue #108 eingeführten Discord-Zutatenabfrage einschließlich der UX-Follow-ups aus Issues #111 und #113, der Autorisierungs- und Ownership-Schärfung aus Issue #115 sowie der Länderliste aus Issue #176.

## 1. Ziel und Grundsatz

Mitglieder der konfigurierten Discord-Guild können Informationen aus dem gepflegten Zutatenkatalog direkt über Discord abrufen und von einer angezeigten Zutat zu ihren direkten allgemeineren Begriffen beziehungsweise bekannten Konkretisierungen navigieren. Dafür ist keine fachliche Participant-Registrierung erforderlich.

Der Ablauf ist ausschließlich lesend:

- keine Anlage, Bearbeitung oder Deaktivierung von Zutaten,
- keine Änderung von Gewichtungen, Beziehungen, Rollen oder Eigenschaften,
- keine neue Persistenz, kein Audit und keine Discord-Message-Persistenz,
- kein Generator-, Kurator-, Voting-, Reroll- oder Challenge-Workflow,
- kein OpenAI-Aufruf.

Discord bleibt Transport und Darstellung. Suche und Detailprojektion gehören als kleine öffentliche Read-API in das `catalog`-Modul; der Discord-Adapter greift weder direkt auf JDBC noch auf administrationsinterne Projektionen zu.

## 2. Command und Zugriff

Die Guild-Slash-Commands lauten:

```text
/zutat suche:<Suchtext>
/zutaten land:<Land>
```

`suche` ist genau ein erforderlicher String-Parameter von `/zutat`; für diesen Command gibt es weiterhin kein
Autocomplete, keine Modals und keine weiteren Filter. `/zutaten` verwendet dagegen den ebenfalls erforderlichen
String-Parameter `land` mit Autocomplete für den migrationsgeführten Länderreferenzbestand. Die abweichende
Länderlisten-, Auswahl- und Rücknavigationssemantik steht in Abschnitt 12.

Für `/zutat` gilt bewusst eine andere Zugriffspolitik als für `/challenge`:

- ausschließlich die konfigurierte Guild,
- innerhalb dieser Guild für jedes Mitglied nutzbar,
- keine Teilnehmeridentität in der Datenbank erforderlich,
- keine gültige Nutzung in DMs oder fremden Guilds.

Die fachliche Antwort ist öffentlich. Reine Autorisierungs-, Eingabe-, Stale- oder technische Fehlermeldungen dürfen ephemer bleiben.

Die Interaktion mit einer einmal erzeugten Zutaten-Card bleibt dagegen vollständig an deren ursprünglichen Aufrufer gebunden. Das betrifft sowohl eine initiale Mehrdeutigkeitsauswahl als auch sämtliche anschließend erzeugten Eltern-/Kind-Selects. Andere Guild-Mitglieder dürfen parallel eigene `/zutat`-Abfragen starten, aber keine fremde Card verändern.

Die Berechtigung zum Starten von `/challenge` ist davon unabhängig und wird im Discord-Adapter über die konfigurierte Challenge-Operator-Rolle geprüft. Fachliche Challenge-Teilnahme, Electorate und Operator-Berechtigung sind keine Synonyme.

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
searchCulinaryCountries(searchText, limit)
resolveCulinaryCountry(input)
findActiveByCulinaryCountry(countryCode, page, pageSize)
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
- direkt gepflegte kulinarische Länderzuordnungen mit ISO-Alpha-2-Code und Anzeigename,
- verpflichtende Kuratornotiz.

Die Relation-ID dient ausschließlich der eindeutigen stateless Discord-Navigation. Sie wird nicht als Nutztext angezeigt. Suchtreffer dürfen für ihre optionale Parent-Beschreibung weiterhin ausschließlich Namen transportieren.

Nicht Teil der Projektion beziehungsweise Darstellung sind Saison, Beschaffbarkeit, technischer Code, Version, Änderungszeitpunkt, Auditdaten, Challenge-Spezifität, transitive Vorfahren/Nachfahren oder Ausschlussregeln.

Nur direkte und zum Zeitpunkt der Abfrage aktive Beziehungen werden geliefert. Rollen, Flags und Dimensionen werden nicht über den Konkretisierungsgraphen vererbt.

Kulinarische Länderzuordnungen gelten ebenfalls ausschließlich für das konkret angezeigte Konzept. Die Lookup-Projektion liefert sie stabil nach ISO-Code sortiert mit Code und Anzeigename; sie leitet weder Parent→Child noch Child→Parent ab.

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
5. optional `🌍 Kulinarische Zuordnung`,
6. optional `💡 Hinweis aus dem Zutatenkatalog`,
7. `⬆️ Allgemeinere Begriffe`,
8. `⬇️ Bekannte Konkretisierungen`,
9. direkt unter dem Embed die String-Select-Navigation für vorhandene direkte Beziehungen.

Die beiden Inline-Felder werden nicht durch Leerzeichen oder Tabulatoren als Texttabelle simuliert. Lange Werte im linken Feld verschieben daher die rechte Spalte nicht. Mehrere Werte stehen innerhalb ihres Feldes untereinander; leere Listen erscheinen als `keine`. Auf schmalen Clients darf Discord die Felder untereinander stapeln.

Die Hierarchie steht bewusst am Ende, damit zunächst sämtliche Informationen zum konkret angezeigten Konzept zusammenbleiben und erst danach die Katalognavigation folgt.

### 6.1 Kulinarische Länderzuordnung

Hat das angezeigte Konzept mindestens eine explizit gepflegte kulinarische Länderzuordnung, zeigt die Card ein nicht-inline Feld:

```text
🌍 Kulinarische Zuordnung
🇵🇭 🇹🇭 🇻🇳
```

Die zugrunde liegende Lookup-Projektion transportiert weiterhin ISO-Code und ausgeschriebenen Ländernamen. Der Discord-Renderer verwendet jedoch ausschließlich den gültigen ISO-Alpha-2-Code und bildet daraus deterministisch das Regionalindikator-Flag. Die Card zeigt weder Ländernamen noch Codes; die Flaggen sind kein gespeicherter Fachwert.

Der Discord-Adapter besitzt dafür weder einen eigenen Länderreferenzbestand noch Länderfachlogik. Bei sehr vielen Zuordnungen darf er die Flaggen ohne Trennzeichen verdichten, damit das Discord-Field-Limit ohne Weglassen einer Zuordnung eingehalten bleibt. Fehlen Zuordnungen, existiert kein Länderabschnitt. Die Flaggen auf einer `/zutat`-Card selbst bleiben nicht navigierbar; die davon getrennte Länderübersicht wird ausschließlich mit `/zutaten` gemäß Abschnitt 12 geöffnet.

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

Die verpflichtende `curator_note` wird als `💡 Hinweis aus dem Zutatenkatalog` angezeigt. Der Renderer behandelt einen entgegen der zentralen Datenbankinvariante leeren Wert weiterhin defensiv und erzeugt dafür keinen Abschnitt.

Katalogtexte werden so gerendert, dass sie keine unbeabsichtigten Mentions, Codeblöcke, Links oder Markdown-Strukturen auslösen. Allowed Mentions bleiben vollständig deaktiviert. Unvertrauenswürdiger Katalogtext wird nicht in die ausgerichteten Codeblöcke übernommen.

Kuratornotizen dürfen über mehrere Felder geteilt werden; die Card begrenzt ihre Zahl jedoch so, dass die beiden Hierarchiefelder am Ende nicht durch das Embed-Budget verdrängt werden können. Eine notwendige Kürzung bleibt sichtbar.

## 9. Direkte Hierarchienavigation

Jedes sichtbare aktive direkte Eltern-/Kindkonzept ist navigierbar. Die Navigation verwendet unmittelbar die bereits aus der Profilprojektion bekannte stabile Konzept-ID und ruft `findActiveProfile(conceptId)` auf.

Es gibt ausdrücklich:

- keine erneute Namenssuche,
- kein simuliertes `/zutat suche:<name>`,
- keine neue Persistenz, Session oder Message-State-Tabelle,
- keine technische ID im sichtbaren Discord-Text.

Eine Auswahl ersetzt dieselbe öffentliche Nachricht durch die aktuelle Card des Zielkonzepts. Wurde das Ziel inzwischen deaktiviert oder entfernt, wird die bestehende sichere Stale-Antwort angezeigt.

Sämtliche Navigationskomponenten sind an die Discord-User-ID des ursprünglichen `/zutat`-Aufrufers gebunden. Klickt ein anderer Nutzer auf eine sichtbare Card, wird die Interaktion ephemer abgewiesen, bevor eine Katalogabfrage oder Message-Änderung stattfindet. Nach erfolgreicher Navigation bleibt derselbe Owner auch für die neu gerenderte Card maßgeblich.

Die Ownership wird vollständig stateless in versionierten Component-IDs getragen. Ein App-Restart benötigt daher keine Message-State-Tabelle. Vor #115 erzeugte ungebundene Navigationskomponenten werden nach dem Update sicher als veraltet abgewiesen; sie erhalten keine implizite neue Freigabe.

### Einheitliche Komponentenwahl

Die Navigation wird für Eltern und Kinder getrennt und ausschließlich als String Select gerendert:

- mindestens 1 Ziel: genau ein String Select für diese Richtung,
- keine Beziehung: kein Navigationselement,
- maximal 25 Optionen pro Select,
- auch genau ein Ziel wird bewusst als einoptioniges Select dargestellt.

Verbindliche Platzhalter:

```text
⬆️ Allgemeineren Begriff öffnen …
⬇️ Konkretisierung öffnen …
```

Bei mehr als 25 direkten Zielen werden die ersten 25 der stabilen alphabetischen Reihenfolge angeboten. Die Card nennt die exakte Restanzahl und verweist für weitere Ziele auf `/zutat`. Lange Anzeigenamen werden innerhalb der Discord-Grenzen sichtbar mit Ellipse gekürzt; bei nach der Kürzung identischen Labels werden sichtbare Ordinal-Suffixe ergänzt. Der interne Option-Value bleibt die Konzept-ID.

Component-IDs und Values bleiben versioniert, strikt parsebar und vollständig stateless. Owner-gebundene Navigation funktioniert damit nach einem App-Restart weiter, sofern das Ziel weiterhin aktiv ist und derselbe Discord-Nutzer interagiert.

## 10. Längenbegrenzung

Discord-Grenzen werden vor dem Senden deterministisch eingehalten. Eine überlange Ausgabe darf nicht erst durch einen Discord-`Bad Request` auffallen.

- Lange Listen werden an semantischen Grenzen gekürzt.
- Jede Kürzung nennt sichtbar die Restmenge beziehungsweise Zeichenmenge.
- Navigation wird auf maximal 25 Optionen je Richtung beschränkt.
- Die Hierarchiefelder bleiben auch bei langen Kuratornotizen erhalten.
- Die gesamte Card bleibt innerhalb der Embed-, Field-, Component-, Label- und `custom_id`-Grenzen.
- Der Länderabschnitt enthält ausschließlich aus ISO-Codes abgeleitete Flaggen und bleibt auch zusammen mit maximalen übrigen Card-Inhalten innerhalb der Field- und Embed-Grenzen.

## 11. Modul- und Adaptergrenze

- `discord` verwendet ausschließlich `catalog :: api` für die Zutatenabfrage.
- JDA-Typen verbleiben vollständig im `discord`-Adapter.
- Kein JDBC-Zugriff aus Listener, Workflow oder Renderer.
- Keine Wiederverwendung der administrationsorientierten Vollprojektion.
- Keine Schreibtransaktion und kein Katalogaudit durch Suche, Detailanzeige oder Navigation.
- Die Änderung der `/challenge`-Startautorisierung aus #115 betrifft ausschließlich den Discord-Adapter; Generator-, Offer-, Voting- und Participation-Lifecycle bleiben unverändert.

`DiscordIngredientLookupRenderer` besitzt ein transportneutrales Render-Modell für Embed-Description, Inline-Felder, String-Select-Navigation und die kompakte Länderliste. Erst `DiscordJdaListener` mappt dieses Modell auf JDA-Embeds, native String Selects und die für Länderpaging/Rückkehr nötigen Buttons und bindet dabei sämtliche Navigationskomponenten an den Card-Owner. Die bestehende Eltern-/Kindnavigation bleibt bewusst selectbasiert.

Die nicht deferbare `/zutaten`-Autocomplete-Antwort nutzt ausschließlich einen eigenen, kleinen Discord-Executor für
ihren begrenzten Katalogread. Dadurch kann sie nicht hinter einem längeren, bereits bestätigten Workflow auf dem
bestehenden Single-Thread-Executor bis zum Discord-Timeout warten. Das ist keine allgemeine Executor- oder
Workflow-Umstellung; alle anderen Discord-Arbeiten bleiben auf dem bisherigen Executor.

Die `/challenge`-Startautorisierung verwendet separat `mise-en-dice.discord.challenge-operator-role-id`. Sie ist kein Ersatz für Participant-Identitäten und verleiht keine Stimme in einem Electorate. Umgekehrt berechtigt eine DB-Teilnehmeridentität nicht zum Start einer Challenge.

## 12. Länderliste, Detail- und Rücknavigation

Neben `/zutat` steht `/zutaten land:<Land>` als Guild-weiter, ausschließlich lesender Command zur Verfügung. `land` ist ein erforderlicher String mit Autocomplete. Die Vorschläge stammen ausschließlich aus der öffentlichen `IngredientLookupQueries`-Projektion des migrationsgeführten `culinary_country`-Referenzbestands: höchstens 25 Treffer, case-insensitive im deutschen Anzeigenamen, Namensanfänge vor sonstigen Teilstrings und anschließend stabil nach Anzeigename und ISO-Code. Sichtbar darf die aus dem ISO-Code abgeleitete Flagge stehen; der Choice-Wert ist ausschließlich der ISO-Alpha-2-Code. Der tatsächliche Command löst diesen Code oder einen getrimmten, case-insensitive exakt passenden deutschen Namen auf. Alias-, Übersetzungs-, Fuzzy- und Regionslogik existieren nicht.

Die Ergebnisansicht zeigt Flagge, deutschen Ländernamen, die Gesamtzahl und ausschließlich aktive Konzepte mit einer **explizit** gepflegten Relation alphabetisch in Seiten zu 20 Zutaten. `random_draw_enabled` ist unerheblich. Gewichtung, Rollen, Flags, Dimensionen, Notizen, Hierarchie- und technische Daten bleiben aus der Liste heraus. Ein Land ohne aktive Zuordnung ist ein gültiger öffentlicher leerer Zustand. Bei mehreren Seiten ersetzen deaktivierbare `◀ Zurück`-/`Weiter ▶`-Buttons dieselbe Nachricht; jede Seite wird frisch gelesen und eine nach Katalogänderung ungültige Seite sicher auf die letzte aktuelle Seite beziehungsweise den leeren Zustand zurückgeführt.

Das genau eine Select einer nicht leeren Seite öffnet die bestehende vollständige `/zutat`-Card per frischem `findActiveProfile`-Read. Diese Card erhält `↩ Zurück zu <Land>`. ISO-Code und Listenposition bleiben auch über die bestehende Parent-/Child-Navigation erhalten, selbst wenn das aktuell geöffnete Ziel keine Relation zu diesem Land besitzt. Die Rückkehr lädt die aktuelle Länderseite; eine inzwischen deaktivierte Zutat zeigt eine sichere Stale-Antwort mit diesem Rückweg. Direkt über `/zutat` geöffnete Cards bleiben unverändert ohne Länder-Rückbutton.

Alle Länderlisten-, Paging-, Auswahl-, Detail-, Hierarchie- und Rückkomponenten tragen Owner, ISO-Code und Seite in versionierten Component-IDs. Sie benötigen weder Session noch Message-State oder Persistenz und funktionieren nach einem App-Restart weiter, soweit die aktuellen Katalogdaten die Handlung zulassen. Eine fremde Interaktion wird vor jeder Katalogabfrage und Nachrichtenänderung ephemer abgewiesen. Bestehende `/zutat`-Component-IDs bleiben parsebar und gültig.

## 13. Fehlerdarstellung und Verifikation

Nutzertexte unterscheiden knapp zwischen ungültiger Eingabe, keinem Treffer, veralteter Navigation/Auswahl, fremder Card und unbekanntem technischen Fehler. Interne Codes, IDs, SQL-Details, Stacktraces und Providerdiagnostik erscheinen niemals in Discord.

Automatisierte Tests decken mindestens ab:

- echte PostgreSQL-Suche und aktive Relation-ID/Name-Projektion,
- exakten Direktfund, Einzel- und Mehrfachtreffer,
- `/zutat` für nicht als Participant registrierte Mitglieder der konfigurierten Guild,
- Abweisung von `/zutat` außerhalb der konfigurierten Guild vor Lookup-Arbeit,
- ziehbare/nicht ziehbare Profile und fehlende Werte,
- native Inline-Felder mit langen linken Inhalten,
- Lookup-Profil ohne, mit einer und mit mehreren expliziten Länderzuordnungen in stabiler Code-Reihenfolge ohne Parent-/Child-Vererbung,
- korrekte deterministische ISO-Alpha-2→Flaggen-Darstellung ohne Ländertexte oder Codes sowie ohne leeren Länderabschnitt,
- Länderabschnitt zusammen mit maximalen übrigen Card-Inhalten innerhalb der Discord-Limits,
- verbale Stufen, `○`-Leersymbol und exakt fünf Skalenpositionen,
- Kuratornotiz- und Längenbegrenzung,
- 1 bis 25 Beziehungen als Select, leere Richtungen ohne Navigation, mehr als 25 mit sichtbarer Restanzahl,
- getrennte Eltern-/Kind-Selects mit den verbindlichen Platzhaltern,
- direkte Navigation per Konzept-ID ohne Namenssuche,
- Owner-Bindung der initialen Auswahl und aller nachfolgenden Hierarchie-Selects,
- keine Katalogabfrage und keine Card-Änderung bei fremder Navigation,
- Stale-Verhalten nach Deaktivierung sowie für alte ungebundene Navigationskomponenten,
- stateless Component-Parsing,
- JDA-Routing der Zutaten-Navigation als String Select,
- `/zutaten`-Autocomplete mit höchstens 25 Ländern aus der öffentlichen Katalogprojektion und einer Antwort, die nicht
  hinter Arbeiten des primären Discord-Executors wartet,
- Länderauflösung per ISO-Code oder exaktem deutschen Namen, aktive explizite Relation, Pagination, Leerzustand,
  Rücknavigation und fremde Owner-Interaktionen ohne Katalogabfrage,
- `/challenge`-Start nur mit Operator-Rolle und unabhängig von einer Teilnehmeridentität,
- bestehende Challenge-Interaktions-/Voting-Regressionen.

Verpflichtendes Gate:

```bash
./mvnw clean verify
```

Automatisierte Tests und Entwicklung öffnen weder eine echte Discord-Verbindung noch einen echten OpenAI-Zugriff.

## 14. Nicht-Ziele

- Kataloganlage oder -bearbeitung aus Discord,
- Suche in technischen Codes,
- Autocomplete für `/zutat` oder freie Filter,
- transitive Hierarchienavigation,
- Vor-/Zurück-History innerhalb Discord,
- Navigation in neuen Discord-Nachrichten,
- Pagination großer Hierarchien,
- Saison- oder Beschaffbarkeitsauskunft,
- Rollen- oder Eigenschaftsvererbung,
- Zutatenempfehlungen, Rezeptvorschläge oder KI-Erklärungen,
- Änderung des bestehenden Challenge-Lifecycles,
- dynamische Participant-Registrierung oder Erweiterung des Electorates,
- Vorziehen persönlicher Konkretisierungen, Zusatz-Zutaten, Kochpläne, Fotos oder Ergebnisdokumentation.

Nach vollständiger Umsetzung und Abnahme von #115 kann Phase 12E / #90 mit dem privaten Produktionspilot beginnen.
