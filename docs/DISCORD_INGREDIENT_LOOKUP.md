# Lesende Discord-Zutatenabfrage

Stand: 19. August 2026

Dieses Dokument ist die verbindliche Fach- und Adapter-Spezifikation für Issue #108. Das Paket wird nach der abgeschlossenen Live-Abnahme aus Phase 12D und vor dem privaten Produktionspilot aus Phase 12E / #90 umgesetzt.

## 1. Ziel und Grundsatz

Georgia und Tobias sollen Informationen aus dem gepflegten Zutatenkatalog direkt über Discord abrufen können. Die Auskunft dient der Einordnung unbekannter oder wenig vertrauter Challenge-Zutaten und macht die fachlichen Katalogdaten dort sichtbar, wo sie tatsächlich gebraucht werden.

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

`suche` ist genau ein erforderlicher String-Parameter. Weitere Filter, Autocomplete, Modals oder Untercommands sind für den ersten Stand nicht vorgesehen.

Der Command verwendet dieselbe private Zugriffspolitik wie `/challenge`:

- ausschließlich die konfigurierte Guild,
- ausschließlich die bereits konfigurierten Discord-Teilnehmer,
- keine gültige Nutzung in DMs oder fremden Guilds.

Die fachliche Antwort ist öffentlich. Das gilt sowohl für eine notwendige Trefferauswahl als auch für das fertige Zutatenprofil. Reine Autorisierungs-, Eingabe-, Stale- oder technische Fehlermeldungen dürfen ephemer bleiben.

## 3. Suchsemantik

### 3.1 Suchbestand

Durchsucht werden alle aktiven Zutatenkonzepte:

```text
active = true
```

`random_draw_enabled` schränkt die Suche nicht ein. Aktive Gruppenkonzepte und andere nicht eigenständig ziehbare Konzepte bleiben damit auffindbar.

Gesucht wird ausschließlich im sichtbaren Namen `display_name`:

- führende und folgende Leerzeichen werden entfernt,
- Vergleich erfolgt case-insensitive mit stabiler Locale,
- der Suchtext wird als literaler Teilstring behandelt,
- `%`, `_`, Backslashes und andere Zeichen erhalten keine SQL-Wildcard-Semantik,
- technische Codes werden nicht durchsucht,
- keine Fuzzy-, Synonym-, Transliteration- oder Akzentnormalisierung.

Eine nach dem Trimmen leere Eingabe wird ohne Katalogabfrage verständlich abgewiesen.

### 3.2 Entscheidung zwischen Direktanzeige und Auswahl

Die Trefferentscheidung ist deterministisch:

1. Existiert ein case-insensitiver exakter Namensfund, wird genau dieser unmittelbar angezeigt. Weitere Namen, die denselben Text nur enthalten, erzwingen keine Auswahl.
2. Existiert kein exakter Fund, aber genau ein Teilstringtreffer, wird dieser unmittelbar angezeigt.
3. Existieren mehrere Teilstringtreffer, wird eine öffentliche String-Select-Auswahl angezeigt.
4. Existiert kein Treffer, erhält der aufrufende Nutzer eine kurze verständliche Meldung und den Hinweis, den Suchtext zu verändern.

Mehrdeutige Treffer werden wie folgt sortiert:

1. sichtbarer Name beginnt mit dem Suchtext,
2. Suchtext steht an anderer Stelle im sichtbaren Namen,
3. innerhalb derselben Gruppe alphabetisch nach sichtbarem Namen,
4. als letzter stabiler Tie-Break nach Konzept-ID.

Die Auswahl zeigt höchstens 25 Optionen. Gibt es mehr Treffer, werden die ersten 25 nach dieser Ordnung angeboten; die öffentliche Nachricht nennt zugleich die Gesamtzahl und fordert zu einer präziseren Suche auf.

Das Select-Label enthält ausschließlich den sichtbaren Namen. Eine kurze Beschreibung darf zur Unterscheidung die aktiven direkten allgemeineren Begriffe nennen. IDs und technische Codes bleiben auch dort unsichtbar.

## 4. Zutatenprofil

Das fertige Profil zeigt ausschließlich fachlich nützliche aktuelle Katalogdaten.

### 4.1 Sichtbare Angaben

- **Name**
- **Gewichtung**
- **Ungewöhnlichkeit**
- aktive direkte Eltern als **Allgemeinere Begriffe**
- aktive direkte Kinder als **Bekannte Konkretisierungen**
- direkt gepflegte funktionale Rollen als **Funktion im Gericht**
- direkt gepflegte kulinarische Flags als **Besondere Eigenschaften**
- direkt gepflegte kulinarische Dimensionen als **Geschmacksprofil**
- nicht leere Kuratornotiz als **Hinweis aus dem Zutatenkatalog**

### 4.2 Bewusst nicht sichtbare Angaben

- Saison und Beschaffbarkeit,
- technischer Code und Datenbank-ID,
- Version, Änderungszeitpunkt und Auditdaten,
- Aktivstatus und technische Challenge-Spezifität,
- transitive Vorfahren und Nachfahren,
- Ausschlussregeln,
- aktuelle oder historische Generator-/Kuratorinformationen.

### 4.3 Gewichtung und Ungewöhnlichkeit

Bei einem aktiven ziehbaren Konzept wird die gespeicherte Gewichtung deutsch formatiert angezeigt, beispielsweise:

```text
Gewichtung        0,85
```

Es folgt kein erläuternder Zusatz zur relativen Bedeutung.

Bei einem aktiven, aber nicht eigenständig ziehbaren Konzept wird keine wirkungslose Zahl präsentiert:

```text
Gewichtung        nicht eigenständig ziehbar
```

Die Ungewöhnlichkeit verwendet dieselbe fünfstufige verbale Skala wie das Datenmodell. Ein fehlender Wert erscheint ausdrücklich als `nicht gepflegt`; er wird weder als Stufe 0 noch als Stufe 1 interpretiert.

## 5. Beziehungen und direkt gepflegte Metadaten

Die Bezeichnungen `Eltern` und `Kinder` werden in der Nutzeroberfläche nicht verwendet. Der Kataloggraph beschreibt bekannte Konkretisierungen und ist bewusst keine vollständige Lebensmittelontologie.

Daher gelten:

- `directParents` → **Allgemeinere Begriffe**
- `directChildren` → **Bekannte Konkretisierungen**

Nur direkte und zum Zeitpunkt der Abfrage aktive Beziehungen werden angezeigt. Inaktive Konzepte werden weder als Suchtreffer noch innerhalb der sichtbaren Beziehungen ausgegeben.

Rollen, Flags und Dimensionen werden ausschließlich so angezeigt, wie sie direkt am gewählten Konzept gepflegt sind. Es gibt für diese Auskunft keine automatische Vererbung über den Konkretisierungsgraphen.

Leere Listen werden knapp als `keine` dargestellt. Listen besitzen eine stabile alphabetische Reihenfolge.

## 6. Darstellung und Skalen

### 6.1 Grundaufbau

Das Zutatenprofil wird als kompaktes Discord-Embed dargestellt. Eine allgemeine neue Discord-UI-Schicht oder ein Umbau der vorhandenen Challenge-Nachrichten ist nicht Teil des Pakets.

Empfohlene Reihenfolge:

1. Embed-Titel mit Zutatenname,
2. Basisdaten mit Gewichtung und Ungewöhnlichkeit,
3. allgemeinere Begriffe,
4. bekannte Konkretisierungen,
5. Funktion im Gericht,
6. besondere Eigenschaften,
7. Geschmacksprofil,
8. optionaler Hinweis aus dem Zutatenkatalog.

Die Basisdaten und das Geschmacksprofil verwenden mehrzeilige Codeblöcke. Damit werden die Textspalten durch Monospace-Schrift stabil ausgerichtet. Ungleich breite Emoji-Glyphen stehen immer am Ende einer Zeile; hinter ihnen folgt keine weitere auszurichtende Spalte.

### 6.2 Verbale Fünfer-Skala

Für Ungewöhnlichkeit und kulinarische Dimensionen gelten:

| Stufe | Bezeichnung |
|---:|---|
| 1 | sehr niedrig |
| 2 | niedrig |
| 3 | mittel |
| 4 | hoch |
| 5 | sehr hoch |

Die verbale Bezeichnung bleibt zusätzlich zur Symbolskala sichtbar. Emoji sind nie die einzige Information.

### 6.3 Symbolik

- Ungewöhnlichkeit: `✨`
- Dominanz: `📣`
- Süße: `🍯`
- Säure: `🍋`
- Bitterkeit: `☕`
- Fettigkeit: `🧈`
- Schärfe: `🌶️`
- Umami: `🍄`
- unbesetzte Position: ein einheitliches zurückhaltendes leeres Symbol, bevorzugt `▫`

Jede gepflegte Skala besitzt exakt fünf Positionen. Beispiele:

```text
Gewichtung        0,85
Ungewöhnlichkeit  hoch          ✨✨✨✨▫
```

```text
Dominanz    hoch          📣📣📣📣▫
Süße        niedrig       🍯🍯▫▫▫
Säure       mittel        🍋🍋🍋▫▫
Bitterkeit  sehr niedrig  ☕▫▫▫▫
Fettigkeit  mittel        🧈🧈🧈▫▫
Schärfe     hoch          🌶️🌶️🌶️🌶️▫
Umami       sehr hoch     🍄🍄🍄🍄🍄
```

Eigenschaftsname, verbale Stufe und Beginn der Symbolskala starten in jeder Zeile an denselben berechneten Spalten. Nicht gepflegte kulinarische Dimensionen werden vollständig ausgelassen, weil ihr Fehlen keine niedrige Ausprägung behauptet.

## 7. Kuratornotiz und sichere Katalogtexte

Eine nicht leere `curator_note` wird als fachlicher Nutzerhinweis angezeigt:

```text
💡 Hinweis aus dem Zutatenkatalog
```

Die Notiz ist kein technisches Diagnosefeld und darf deshalb sichtbar sein. Leere oder nur aus Whitespace bestehende Notizen erzeugen keinen Abschnitt.

Sämtliche Katalogtexte werden so gerendert, dass sie keine unbeabsichtigten Mentions, Codeblöcke, Links oder Markdown-Strukturen auslösen. Allowed Mentions bleiben für die Antwort vollständig deaktiviert.

Unvertrauenswürdiger Katalogtext wird nicht in die ausgerichteten Codeblöcke übernommen. Dort stehen nur kontrollierte Labels, verbale Skalenwerte und formatierte Zahlen.

## 8. Längenbegrenzung und vollständige Information

Discord-Grenzen werden vor dem Senden deterministisch eingehalten. Eine überlange Ausgabe darf nicht erst durch einen Discord-`Bad Request` auffallen.

- Lange Listen werden an semantischen Grenzen gekürzt oder auf mehrere Felder verteilt.
- Eine Kürzung nennt sichtbar die exakte Restanzahl, beispielsweise `… (+7 weitere)`.
- Eine lange Kuratornotiz darf über nummerierte Felder verteilt werden.
- Reicht selbst das gesamte Embed-Budget nicht, wird nachvollziehbar und ausdrücklich gekürzt; Inhalt verschwindet nie stillschweigend.
- Die Ausgabe verwendet höchstens die notwendige Zahl von Feldern und bleibt auf Mobilgeräten lesbar.

## 9. Auswahlinteraktion, Stale-Verhalten und Restart

Bei mehreren Treffern wird genau ein String Select mit maximal einer Auswahl verwendet.

Component-ID und Option-Value sind strikt und versioniert:

- eigener Zutaten-Namespace statt vorgetäuschter Challenge-Session,
- Aufrufer-ID als notwendige opake Discord-ID im Component-State,
- Konzept-ID als opaker Auswahlwert,
- keine Namen, Suchtexte oder Kataloginformationen in Custom-IDs.

Nur der Nutzer, der `/zutat` aufgerufen hat, darf die Auswahl bedienen. Ein anderer Nutzer erhält eine ephemere Ablehnung; die öffentliche Nachricht bleibt unverändert.

Nach einer gültigen Auswahl wird die ursprüngliche öffentliche Nachricht durch das aktuelle Zutatenprofil ersetzt. Das Select wird vollständig entfernt.

Die Interaktion ist stateless und nach einem App-Restart weiterhin verwendbar. Vor dem Rendern wird das Konzept erneut als aktuell aktiv gelesen. Wurde es inzwischen deaktiviert oder entfernt, gilt die Auswahl als veraltet; es wird kein inaktives Profil angezeigt.

Doppelklicks, malforme IDs, fremde Nutzer und wiederholte Auswahl nach bereits erfolgtem Ersatz verändern keine Daten und erzeugen keine technischen Details im Nutzertext.

## 10. Öffentliche Katalog-API und Modulgrenze

Das `catalog`-Modul stellt für dieses Feature eine kleine anwendungsfallbezogene Read-API bereit. Sie soll fachlich mindestens ermöglichen:

- aktive Namenssuche mit der festgelegten Normalisierung, Sortierung, Gesamtzahl und Begrenzung,
- Auflösung eines aktiven Konzepts in genau die für Discord freigegebenen Profilfelder.

Die Projektion darf intern stabile Konzept-IDs zur Interaktion transportieren, exponiert aber weder Schreibcommands noch administrationsspezifische Felder. Ein möglicher Zuschnitt ist:

```text
IngredientLookupQueries
  searchActiveByDisplayName(searchText, limit)
  findActiveProfile(conceptId)
```

Die konkrete Java-Benennung darf im Implementierungspaket geringfügig abweichen. Verbindlich bleiben die schmale Projektion und folgende Grenzen:

- `discord` verwendet nur `catalog :: api`,
- JDA-Typen verbleiben vollständig im `discord`-Adapter,
- kein JDBC-Zugriff aus Listener, Workflow oder Renderer,
- keine Wiederverwendung der administrationsorientierten Vollprojektion als zufälliger Datenbeutel,
- keine Schreibtransaktion und kein Katalogaudit durch Suche oder Detailanzeige.

## 11. Fehlerdarstellung

Nutzertexte unterscheiden knapp:

- nicht autorisiert beziehungsweise falsche Guild,
- leerer Suchtext,
- kein aktiver Treffer,
- veraltete oder bereits verbrauchte Auswahl,
- unbekannter technischer Fehler.

Interne Codes, IDs, SQL-Details, Stacktraces und Providerdiagnostik erscheinen niemals in Discord. Unbekannte Fehler bleiben technisch, werden geloggt und nicht als `kein Treffer` oder fachlicher Konflikt maskiert.

## 12. Verifikation

Issue #108 legt die vollständige Testmatrix des Implementierungspakets fest. Als Mindestgate gelten:

- echte PostgreSQL-Tests für aktive literale Teilstringsuche, Rangfolge, Begrenzung und Detailprojektion,
- reine Renderer-Tests für alle sichtbaren Felder, Skalen, leere Werte, Kuratornotiz und Längenbegrenzung,
- Interaction-Tests für Guild-/Nutzerzugriff, öffentlichen Select, Invoker-Bindung, Stale-Verhalten und Ersatz der Nachricht,
- Component-ID-/Value-Tests einschließlich malformer Eingaben,
- Modulgrenzentest für die neue reine Lesekante `discord -> catalog :: api`,
- vollständiger bestehender Build:

```bash
./mvnw clean verify
```

Automatisierte Tests und Entwicklung öffnen weder eine echte Discord-Verbindung noch einen echten OpenAI-Zugriff.

## 13. Nicht-Ziele

- Kataloganlage oder -bearbeitung aus Discord,
- Suche in technischen Codes,
- Autocomplete, freie Filter oder transitive Navigation,
- Saison- oder Beschaffbarkeitsauskunft,
- Rollen- oder Eigenschaftsvererbung,
- Zutatenempfehlungen, Rezeptvorschläge oder KI-Erklärungen,
- Nutzung außerhalb der konfigurierten privaten Guild und Teilnehmer,
- Änderung des bestehenden Challenge-Lifecycles,
- Vorziehen persönlicher Konkretisierungen, Zusatz-Zutaten, Kochpläne, Fotos oder Ergebnisdokumentation.

Nach vollständiger Umsetzung und Abnahme von #108 kann Phase 12E / #90 mit dem privaten Produktionspilot beginnen.
