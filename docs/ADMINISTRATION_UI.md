# Spezifikation der privaten Webverwaltung

Stand: 24. August 2026

Dieses Dokument beschreibt die verbindliche fachliche, gestalterische und technische Spezifikation der privaten Webverwaltung von Mise en Dice. Es konkretisiert die in [`ARCHITECTURE.md`](ARCHITECTURE.md) festgelegten Leitplanken und bildet die Grundlage für die anschließenden Implementierungspakete.

Die Webverwaltung ist **kein öffentliches Produktfrontend**. Sie dient einem sehr kleinen bekannten Nutzerkreis zur zuverlässigen, schnellen und nachvollziehbaren Pflege des Zutatenkatalogs. Die Oberfläche wird serverseitig mit Spring MVC und Thymeleaf gerendert; HTMX wird gezielt für Teilaktualisierungen eingesetzt. Eine SPA ist nicht vorgesehen.

## 1. Ziele und Nicht-Ziele

### 1.1 Ziele

Die erste vollständige Katalogverwaltung soll ermöglichen:

- Zutatenkonzepte schnell zu finden, zu verstehen und zu bearbeiten,
- den Konkretisierungsgraphen trotz Mehrfach-Eltern verständlich zu navigieren,
- alle häufig benötigten Katalogeigenschaften ohne tiefe Menüketten zu erreichen,
- Beziehungen, Rollen, Eigenschaften, Beschaffbarkeit und Saison konsistent zu pflegen,
- Ausschlussregeln nachvollziehbar zu verwalten,
- versehentliche oder konkurrierende Änderungen sichtbar und beherrschbar zu machen,
- redaktionelle Änderungen einem Administrator und einem Zeitpunkt zuordnen zu können,
- kritische Änderungen vor dem Speichern in ihrer Wirkung verständlich zu machen.

### 1.2 Nicht-Ziele

Nicht Bestandteil der ersten Webverwaltung sind:

- öffentliche Accounts oder Registrierung,
- allgemeine Benutzer- und Rollenverwaltung,
- Challenge-Ziehung als primärer Web-Flow,
- Rezept- oder KI-Funktionen,
- eine allgemeine Lebensmittelontologie,
- frei konfigurierbare Generatorregeln,
- visuelle Graph-Editoren mit frei verschiebbaren Knoten,
- eine mobile-first optimierte Oberfläche,
- persistente benutzerdefinierte Filter oder persönliche Dashboards.

Die Oberfläche darf nützlich aussehen. Sie muss deswegen nicht versuchen, Jira, Neo4j Browser und einen Supermarkt-Kassenmonitor gleichzeitig nachzuspielen.

## 2. Grundprinzipien der Bedienung

1. **Der Katalog ist der Startpunkt.** Nach erfolgreichem Login öffnet sich direkt die Katalogverwaltung, kein vorgeschaltetes Dashboard.
2. **Suche, Filter, Navigation und Detailansicht gehören zusammen.** Ein Wechsel zwischen separaten Seiten für jede dieser Tätigkeiten wird vermieden.
3. **Normale Eigenschaften werden nicht versteckt.** Die Detailansicht ist eine scrollbare Gesamtansicht mit sichtbaren Abschnitten, nicht eine Sammlung verschachtelter Einstellungsdialoge.
4. **Bearbeitung ist bewusst, aber nicht umständlich.** Lesen und Navigieren erfolgen sofort; Schreiben beginnt explizit über `Bearbeiten` beziehungsweise `Neu` und endet über `Speichern` oder `Verwerfen`.
5. **Keine Autosaves.** Fachlich relevante Änderungen werden atomar gespeichert. Dadurch bleiben Validierung, Audit und Konfliktbehandlung nachvollziehbar.
6. **Kritische Aktionen erklären ihre Wirkung.** Deaktivierung und Bulk-Aktionen zeigen vor dem Speichern eine kompakte Zusammenfassung der Folgen.
7. **Der Graph bleibt ein Graph.** Die Oberfläche darf ihn hierarchisch darstellen, reduziert ihn aber niemals auf genau einen Parent.
8. **URL und Browser-Navigation bleiben sinnvoll.** Auswahl, Suchbegriff, Filter und Darstellungsmodus sollen soweit praktikabel über URL-Parameter beziehungsweise stabile Routen wiederherstellbar sein.

## 3. Globale Navigation

Die erste vollständige Administrationsoberfläche besitzt drei Hauptbereiche:

- **Katalog** – Zutatenkonzepte, Konkretisierungen und sämtliche zugeordneten Eigenschaften,
- **Ausschlüsse** – kuratierte Ausschlussregeln und ihre Ziele,
- **Änderungen** – Audit-Trail und Änderungsverlauf.

Rechts in der Kopfzeile stehen ausschließlich die aktuelle Administrationsidentität und `Abmelden`. Eine allgemeine Einstellungsseite ist nicht vorgesehen.

`Katalog` ist nach dem Login aktiv. Die Hauptnavigation bleibt auf Desktopbreite permanent sichtbar.

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ Mise en Dice     Katalog     Ausschlüsse     Änderungen        Tobias ▾     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                         aktueller Hauptbereich                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

Die spätere Challenge-Historie erhält erst dann einen eigenen Navigationspunkt, wenn sie tatsächlich als Verwaltungsfunktion implementiert wird. Sie wird nicht vorsorglich als leerer Menüpunkt angelegt.

## 4. Zentrale Katalogansicht

### 4.1 Desktop-Layout

Die Katalogansicht verwendet einen Split-View:

- links ein Navigationsbereich für Suche, Filter und Hierarchie beziehungsweise Liste,
- rechts die dauerhaft sichtbare Detailansicht des ausgewählten Konzepts.

Der linke Bereich belegt ungefähr 40 %, der rechte ungefähr 60 % der verfügbaren Breite. Die Trennlinie darf später verschiebbar gemacht werden, ist aber kein Muss der ersten Implementierung.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Katalog                                      [+ Neue Zutat]                  │
├───────────────────────────────┬──────────────────────────────────────────────┤
│ 🔎 Suche nach Name oder Code  │ Kabeljau                          [Bearbeiten]│
│                               │ COD                                           │
│ [Ziehbar] [Offen] [Inaktiv]   │ Aktiv · spezifisch · ziehbar                  │
│ [Pflegebedarf]   [Filter ▾]   │                                               │
│                               │ BASIS                                         │
│ [Hierarchie] [Liste]          │ Anzeigename      Kabeljau                     │
│                               │ Code             COD                           │
│ ▾ Fisch                       │                                               │
│   ▾ weißfleischiger Fisch     │ ZIEHUNG                                       │
│     ● Kabeljau                 │ Gewicht          1.0000                       │
│     ○ Seelachs                │ Kochungewöhnlich 2                            │
│   ▸ fettreicher Fisch         │                                               │
│ ▾ Meeresfrüchte               │ BEZIEHUNGEN                                   │
│ ...                           │ Eltern: Fisch, weißfleischiger Fisch          │
│                               │ Kinder: —                                     │
│                               │                                               │
│ 698 Konzepte                  │ ROLLEN · EIGENSCHAFTEN · BESCHAFFBARKEIT ... │
└───────────────────────────────┴──────────────────────────────────────────────┘
```

### 4.2 Auswahlzustand

Ein ausgewähltes Konzept wird immer anhand seiner stabilen ID behandelt, nicht anhand seiner Position im Baum.

Wenn ein Konzept aufgrund mehrerer Eltern in mehreren geladenen Ästen vorkommt:

- werden alle sichtbaren Vorkommen als dasselbe Konzept erkennbar markiert,
- erhält der Eintrag einen Hinweis wie `3 Eltern`,
- bleibt die rechte Detailansicht identisch, unabhängig davon, welches Vorkommen angeklickt wurde,
- ersetzt die Auswahl niemals bestehende Parent-Beziehungen.

### 4.3 Kein initiales Dashboard

Ist beim Aufruf noch kein Konzept ausgewählt, zeigt die rechte Seite eine kurze Hilfestellung und die wichtigsten Katalogzahlen. Sie ist kein eigener Dashboard-Bereich und enthält keine zusätzlichen Navigationsentscheidungen.

## 5. Suche, Filter, Sortierung und Schnellfilter

### 5.1 Suche

Die Suche steht permanent oben im linken Bereich.

Standardmäßig durchsucht sie:

- `display_name`,
- `code`.

Die Kuratornotiz wird nicht standardmäßig in die Volltextsuche einbezogen, damit inhaltliche Notizen nicht zu überraschenden Treffern führen. Ein späterer expliziter Filter `auch Kuratornotizen durchsuchen` ist zulässig, aber kein Muss der ersten Stufe.

Die Suche ist fehlertolerant bezüglich Groß-/Kleinschreibung. Bei ungefähr 700 Konzepten genügt eine PostgreSQL-basierte Teilstringsuche; eine separate Suchinfrastruktur ist nicht gerechtfertigt.

### 5.2 Schnellfilter

Direkt sichtbar sind vier Schnellfilter:

- `Ziehbar` – aktiv und `random_draw_enabled = true`,
- `Offen` – `challenge_specificity = OPEN`,
- `Inaktiv`,
- `Pflegebedarf`.

`Pflegebedarf` umfasst mindestens:

- aktive ziehbare Konzepte ohne funktionale Rolle,
- aktive ziehbare Konzepte mit redaktionell ungepflegter Beschaffbarkeit für Georgia oder Tobias.

### 5.3 Erweiterte Filter

Der kompakte Filterbereich darf aufklappbar sein, ist aber **direkt neben den Schnellfiltern erreichbar** und keine separate Einstellungsseite.

Mindestens verfügbar sind:

- Aktivstatus: alle / aktiv / inaktiv,
- Ziehbarkeit: alle / aktiviert / deaktiviert,
- Spezifität: alle / spezifisch / offen,
- funktionale Rolle: Mehrfachauswahl,
- kulinarische Flags: Mehrfachauswahl,
- kulinarische Zuordnung: Mehrfachauswahl aus dem migrationsgeführten Länder-Referenzbestand,
- Beschaffbarkeit Georgia: alle fünf Stufen plus `nicht gepflegt`,
- Beschaffbarkeit Tobias: alle fünf Stufen plus `nicht gepflegt`,
- Kochungewöhnlichkeitsstufe: 1–5 beziehungsweise `nicht gepflegt`.

Filter kombinieren sich mit UND; Mehrfachwerte innerhalb desselben Filters verwenden ODER.

### 5.4 Sortierung

In der Listenansicht sind mindestens verfügbar:

- Anzeigename auf-/absteigend,
- zuletzt geändert,
- Ziehungsgewicht,
- Kochungewöhnlichkeit.

Die Hierarchie sortiert Kinder alphabetisch nach Anzeigename. Eine fachliche manuelle Sortierreihenfolge wird nicht eingeführt.

### 5.5 Filterzustand

Suchbegriff, Filter, Sortierung, Ansichtsmodus und ausgewähltes Konzept sollen in der URL repräsentierbar sein. Browser-Zurück darf den vorherigen Arbeitskontext wiederherstellen.

Persistente benutzerdefinierte Filtersets sind für zwei Administratoren unnötig und werden zunächst nicht gebaut.

## 6. Hierarchie und Liste

### 6.1 Hierarchiemodus

Der Hierarchiemodus zeigt **direkte** Konkretisierungsbeziehungen.

Roots sind aktive oder inaktive Konzepte ohne direkten Parent. Inaktive Konzepte werden nicht aus dem Graphen entfernt, sondern visuell gedämpft dargestellt.

Äste werden bei Bedarf per HTMX nachgeladen. Dadurch muss der vollständige Graph nicht bei jedem Seitenaufruf als verschachteltes HTML übertragen werden.

Jeder Knoten zeigt kompakt:

- Anzeigename,
- Statusmarker aktiv/inaktiv,
- offen/spezifisch,
- Ziehbarkeit,
- Anzahl direkter Eltern, falls größer als 1.

### 6.2 Mehrfach-Eltern

Ein Child wird unter **jedem direkten Parent** angezeigt. Es gibt keinen `primary_parent` und keine UI-Heuristik, die einen Parent zur eigentlichen Heimat erklärt.

Beispiel:

```text
Fisch
└─ weißfleischiger Fisch
   └─ Kabeljau  [2 Eltern]

Meeresfisch
└─ Kabeljau  [2 Eltern]
```

Die Mehrfachdarstellung ist beabsichtigt und kein Duplikatfehler.

### 6.3 Suchergebnisse im Graphkontext

Bei aktiver Suche wechselt der linke Bereich standardmäßig in eine flache Trefferliste. Jeder Treffer zeigt seine **direkten Eltern** als Breadcrumbs beziehungsweise Chips.

`Im Baum zeigen` öffnet den Hierarchiemodus. Hat ein Konzept mehrere Pfade, werden die direkten Parent-Pfade als kleine Auswahlliste angeboten. Dies ändert keine Beziehung; es entscheidet nur, welcher Ast geöffnet wird.

### 6.4 Listenmodus

Der Listenmodus ist für Sortierung, Filtervergleich und Bulk-Aktionen vorgesehen.

Eine Zeile zeigt mindestens:

- Anzeigename,
- Code,
- aktiv/inaktiv,
- offen/spezifisch,
- ziehbar/nicht ziehbar,
- funktionale Rollen in kompakter Form,
- Beschaffbarkeit Georgia,
- Beschaffbarkeit Tobias.

Die Liste ist serverseitig paginiert. Standardgröße: 100 Einträge; zulässige Größen: 50, 100, 250.

Bulk-Auswahl erfolgt ausschließlich in der Listenansicht, damit ein Konzept mit mehreren Baumvorkommen nicht versehentlich mehrfach ausgewählt wird.

## 7. Zutatenkonzept-Detailansicht

Die Detailansicht ist eine **einzige scrollbare Seite**. Eine kompakte Sprungnavigation zu Abschnitten ist erlaubt; die Abschnitte werden nicht in verschachtelten Tabs versteckt.

Reihenfolge:

1. Kopf und Status,
2. Basis,
3. Ziehung,
4. Konkretisierungsbeziehungen,
5. funktionale Rollen,
6. kulinarische Eigenschaften,
7. kulinarische Zuordnung,
8. Beschaffbarkeit,
9. Saison,
10. Kuratornotiz,
11. Referenzen und letzter Änderungsverlauf.

### 7.1 Kopf und Status

Sichtbar sind immer:

- Anzeigename,
- stabiler Code,
- Aktivstatus,
- Challenge-Spezifität,
- Ziehbarkeit,
- `Bearbeiten` beziehungsweise im Editiermodus `Speichern` und `Verwerfen`.

### 7.2 Basis

Pflegbar:

- `display_name`,
- `active`.

`code` wird bei Neuanlage vorgeschlagen und darf **bis zum ersten Speichern** bearbeitet werden. Danach ist der Code in der normalen Weboberfläche unveränderlich. Eine Änderung des stabilen technischen Schlüssels erfordert eine bewusste Migration.

Für neue Codes gilt als Anwendungskonvention:

```text
[A-Z][A-Z0-9_]*
```

Der vorgeschlagene Code wird aus dem Anzeigenamen abgeleitet, kann vor der Anlage aber korrigiert werden.

### 7.3 Ziehung

Pflegbar:

- `random_draw_enabled`,
- `challenge_specificity`,
- `base_draw_weight`,
- `novelty_level`.

Darstellung:

- Spezifität als klare Zweierauswahl `spezifisch` / `offen`,
- Gewicht als positive Dezimalzahl, Standard `1.0000`,
- Kochungewöhnlichkeit als Stufen 1–5 plus `nicht gepflegt`, mit verständlichen Stufenbezeichnungen und Hilfetexten.

`active = false` übersteuert die operative Ziehbarkeit. `random_draw_enabled` wird bei Deaktivierung **nicht automatisch verändert**, damit eine spätere Reaktivierung den vorherigen Zustand wiederherstellen kann.

`OPEN` beschreibt eine bewusst offene Challenge-Vorgabe. Auch ein aktives ziehbares offenes Konzept darf ohne direkt gespeicherte Konkretisierung bestehen; der Graph enthält kuratiertes, aber nicht vollständiges Wissen und ist keine Whitelist möglicher Kochentscheidungen.

### 7.4 Funktionale Rollen

Die neun aktuell bekannten Rollen werden als sichtbare Checkboxen beziehungsweise Chips dargestellt. Mehrfachauswahl ist normal.

Die Definition und Beschreibung einer Rolle ist per Tooltip beziehungsweise Hilfetext verfügbar.

### 7.5 Kulinarische Flags

Die binären Flags werden als Mehrfachauswahl dargestellt:

- fermentiert,
- eingelegt,
- geräuchert,
- gepökelt/gereift,
- getrocknet.

Nicht gesetzte Flags werden nicht persistiert.

### 7.6 Kulinarische Dimensionen

Für jede bekannte Dimension wird eine Zeile gezeigt:

- Dominanz,
- Süße,
- Säure,
- Bitterkeit,
- Fettigkeit,
- Schärfe,
- Umami,
- Salzigkeit.

Jede Zeile bietet:

- `nicht gepflegt`,
- Stufe 1,
- Stufe 2,
- Stufe 3,
- Stufe 4,
- Stufe 5.

Ein fehlender Wert bleibt semantisch `nicht gepflegt` und ist **nicht** identisch mit Stufe 1.

Jede Dimension verwendet ein lokales Symbol aus `catalog-icons.svg`. Für `SALTINESS` ist `icon-saltiness` hinterlegt; Darstellung und Bearbeitungsformular erzeugen den Sprite-Verweis dynamisch aus dem stabilen Dimensionscode.

### 7.7 Kulinarische Zuordnung

Die Detailansicht enthält einen klar benannten Abschnitt `Kulinarische Zuordnung`.

- Vorhandene Länder erscheinen deterministisch mit deutschem Anzeigenamen und ISO-3166-1-Alpha-2-Code.
- Ohne gepflegte Relation erscheint der neutrale leere Zustand `Keine kulinarische Zuordnung gepflegt.`; daraus wird keine Herkunfts-, Exklusivitäts- oder Negativaussage abgeleitet.
- Im Editiermodus steht eine einfache Mehrfachauswahl über den vollständigen migrationsgeführten Referenzbestand bereit. Vorhandene Werte sind vorausgewählt.
- Das Hinzufügen oder Entfernen ist kein eigener Save und kein Autosave: Die explizit übermittelte Auswahl ersetzt die Ländermenge gemeinsam mit allen übrigen Zutatenmetadaten in derselben Transaktion.

Eine eigene Länder-CRUD-Oberfläche gehört nicht zur Verwaltung. Die fachliche Bedeutung der positiven, bewusst unvollständigen Zuordnung ist in [`CULINARY_COUNTRY_ASSOCIATIONS.md`](CULINARY_COUNTRY_ASSOCIATIONS.md) beschrieben.

### 7.8 Beschaffbarkeit

Georgia und Tobias werden nebeneinander dargestellt.

Je Person:

- `EASY` – Spontan beschaffbar,
- `PLANNED` – Gezielt beschaffbar,
- `SPECIALTY` – Spezialbeschaffung,
- `DIFFICULT` – Schwer beschaffbar,
- `UNAVAILABLE` – Praktisch nicht beschaffbar,
- `nicht gepflegt`.

`nicht gepflegt` ist nur zulässig, solange das Konzept nicht gleichzeitig aktiv und zufällig ziehbar ist.

Die Detail-, Editier-, Filter-, Bulk- und Konfliktansicht verwenden diese Bezeichnungen statt technischer Codes. Die Fünfer-Skala, der Unterschied zu Kochungewöhnlichkeit und die verbindlichen Hilfetexte sind in [`AVAILABILITY_AND_COOKING_NOVELTY.md`](AVAILABILITY_AND_COOKING_NOVELTY.md) festgelegt.

### 7.9 Saison

Die zwölf Monate werden gleichzeitig in einem kompakten Raster dargestellt.

```text
Jan  Feb  Mär  Apr  Mai  Jun  Jul  Aug  Sep  Okt  Nov  Dez
1.0  1.0  1.2  1.4  1.4  1.2  1.0  0.8  0.8  1.0  1.0  1.0
```

- Der effektive Standard ist `1.0`.
- Werte müssen größer als 0 sein.
- `Zurück auf Standard` setzt einen Monat auf 1.0.
- Monate mit 1.0 sollen bevorzugt **ohne Datenbankzeile** gespeichert werden, weil fehlende Saisonwerte bereits semantisch 1.0 bedeuten.

### 7.10 Kuratornotiz

`curator_note` ist ein verpflichtendes mehrzeiliges Textfeld. Es steht nicht in einem versteckten Expertenmenü und akzeptiert keinen leeren oder ausschließlich aus Leerraum bestehenden Inhalt.

Die Notiz bleibt in der Regel bei ein bis zwei kurzen deutschen Sätzen. Sie beschreibt die kulinarische Identität, übliche sinnvolle Produktformen oder wichtige Abgrenzungen, statt strukturierte Metadaten zu wiederholen. Neuanlage und Bearbeitung erzeugen keinen automatischen Platzhalter; die redaktionell verantwortliche Person gibt einen echten Inhalt ein.

### 7.11 Referenzen

Read-only sichtbar sind mindestens:

- Ausschlussregeln, die dieses Konzept direkt als Ziel verwenden,
- Anzahl direkter Eltern,
- Anzahl direkter Kinder.

Challenge-Historie kann später ergänzt werden, sobald dafür ein sinnvoller Verwaltungs-Use-Case existiert.

## 8. Anlegen eines Zutatenkonzepts

`+ Neue Zutat` ersetzt die rechte Detailansicht durch ein Neuanlageformular; der Katalogkontext links bleibt sichtbar. Basis, Ziehung, Rollen, Eigenschaften, kulinarische Zuordnungen, Beschaffbarkeit und Saison werden im selben atomaren Save gepflegt. Direkte Beziehungen stehen erst nach dem ersten Speichern zur Verfügung.

Defaults:

- `active = true`,
- `random_draw_enabled = false`,
- `challenge_specificity = SPECIFIC`,
- `base_draw_weight = 1.0000`,
- `novelty_level = nicht gepflegt`,
- Kuratornotiz ohne Default und vor dem ersten Speichern verpflichtend,
- keine Rollen,
- keine Flags,
- keine kulinarischen Länderzuordnungen,
- keine Dimensionen,
- keine Saisonabweichungen,
- Beschaffbarkeit zunächst `nicht gepflegt`.

Die bewusst konservative Vorgabe `random_draw_enabled = false` verhindert, dass ein halb gepflegtes Konzept versehentlich in den Challenge-Pool gelangt.

Sobald `random_draw_enabled` aktiviert wird, gelten die Pflichtvalidierungen aus Abschnitt 17.

Nach erfolgreicher Anlage wird das neue Konzept ausgewählt und normal angezeigt. Der Code ist ab diesem Zeitpunkt read-only.

## 9. Bearbeitungszustand und Speichern

### 9.1 Expliziter Editiermodus

Die Detailansicht ist standardmäßig read-only. `Bearbeiten` aktiviert die Eingabefelder.

Vorteile:

- versehentliche Toggle-Klicks verändern keine Daten,
- Auswahl und Navigation bleiben leichtgewichtig,
- alle Änderungen können zusammen validiert, gelockt und auditiert werden.

### 9.2 Keine Autosaves

`Speichern` persistiert alle Änderungen des aktuellen Aggregats in **einer Transaktion**. Dazu gehört auch die explizit gewählte Menge kulinarischer Länderzuordnungen; sie besitzt keinen separaten Speichern-Button oder Autosave.

`Verwerfen` stellt den zuletzt geladenen Serverzustand wieder her.

### 9.3 Ungespeicherte Änderungen

Bei Navigation zu einem anderen Konzept erscheint kein allgemeiner Browserdialog, sondern eine anwendungsinterne Warnung:

```text
Ungespeicherte Änderungen
[Weiter bearbeiten] [Änderungen verwerfen und wechseln]
```

Zusätzlich darf `beforeunload` als letzte Sicherung für Tab-Schließen oder externe Navigation verwendet werden.

### 9.4 Tastatur

Mindestens sinnvoll:

- `/` fokussiert die Katalogsuche, sofern kein Eingabefeld aktiv ist,
- `Ctrl+S` beziehungsweise `Cmd+S` speichert im Editiermodus,
- nach `Neu` erhält der Anzeigename den Fokus.

Eine große Sammlung kryptischer Ein-Tasten-Shortcuts ist nicht vorgesehen.

## 10. Konkretisierungsbeziehungen

### 10.1 Direkte Beziehungen sind editierbar

Die Detailansicht zeigt getrennt:

- **Direkte Oberbegriffe**,
- **Direkte Konkretisierungen**.

Jede Beziehung kann gezielt entfernt werden. Es gibt keine Drag-and-drop-Geste, die implizit andere Beziehungen ersetzt.

### 10.2 Transitive Beziehungen sind read-only

Vorfahren und Nachfahren aus mehreren Schritten werden in einem Kontextbereich zusammengefasst:

- `weitere Vorfahren`,
- `weitere Nachfahren`.

Sie sind nicht direkt löschbar. Die Oberfläche nennt den jeweils direkten Pfad, über den die transitive Beziehung entsteht.

### 10.3 Parent hinzufügen

Ablauf:

1. `Oberbegriff hinzufügen`,
2. Suchfeld im Detailbereich öffnet sich,
3. Suche liefert Name, Code, Status und direkte Eltern der Kandidaten,
4. Selbstbeziehung und bereits vorhandene direkte Beziehung werden ausgeschlossen,
5. offensichtliche Zyklen werden vor dem Speichern erkannt und verständlich erklärt,
6. Auswahl wird als ungespeicherte Änderung in das aktuelle Formular übernommen,
7. endgültige Speicherung erfolgt zusammen mit dem Konzept.

Die vorgemerkte Kante zeigt die beim Laden bekannte Aggregatversion des Gegenknotens. Erst `Speichern` prüft diese zusammen mit der Version des bearbeiteten Konzepts. Ändert sich die Menge vorgemerkter Kanten, wird eine zuvor nötige Inaktivitätsbestätigung erneut verlangt.

### 10.4 Child hinzufügen

Analog zu Parent hinzufügen. Auch hier werden keine anderen Parent-Beziehungen des Childs entfernt.

### 10.5 Zyklusbehandlung

Die Anwendung führt eine verständliche Vorabprüfung aus. Der PostgreSQL-Trigger bleibt dennoch die letzte Integritätssicherung.

Bei einem Triggerfehler wird die Meldung als fachlicher Beziehungskonflikt dargestellt, **nur wenn** tatsächlich die bekannte Zyklusbedingung erkannt wird. Andere SQL-Fehler werden nicht als Zyklus umetikettiert.

### 10.6 Inaktive beteiligte Konzepte

Beziehungen zu inaktiven Konzepten bleiben zulässig und werden nicht automatisch gelöscht.

Beim Hinzufügen einer Beziehung zu einem inaktiven Konzept wird dessen Status sichtbar angezeigt. Es gibt eine Warnung, aber kein pauschales Verbot.

Deaktivierung eines Konzepts entfernt keine Parent-/Child-Beziehungen.

## 11. Deaktivierung und physisches Löschen

### 11.1 Zutatenkonzepte

Die normale Weboberfläche bietet **kein physisches Löschen** eines Zutatenkonzepts an.

Begründung:

- Historie und Fremdschlüssel sollen stabil bleiben,
- Deaktivierung genügt für den operativen Ausschluss,
- versehentliches Löschen eines Graphknotens hätte schwer überschaubare Nebenwirkungen.

Deaktivierung zeigt vor dem Speichern:

- das Konzept wird nicht mehr normal operativ verwendet,
- eine aktivierte Ziehflagge bleibt gespeichert, wirkt aber wegen `active = false` nicht,
- Beziehungen bleiben bestehen,
- historische Referenzen bleiben unverändert.

### 11.2 Ausschlussregeln

Auch Ausschlussregeln werden in der Weboberfläche nur deaktiviert, nicht physisch gelöscht.

### 11.3 Beziehungen und Zuordnungen

Direkte Konkretisierungsbeziehungen, Rollen-, Flag-, Dimensions-, Verfügbarkeits-, Saison- und Ausschlussziel-Zuordnungen dürfen entfernt werden, weil ihre Entfernung selbst die fachliche Bearbeitung darstellt.

## 12. Bulk-Operationen

Bulk-Aktionen stehen ausschließlich im Listenmodus nach expliziter Auswahl von Zeilen zur Verfügung.

Erste sinnvolle Bulk-Aktionen:

- aktivieren,
- deaktivieren,
- Ziehbarkeit aktivieren,
- Ziehbarkeit deaktivieren,
- funktionale Rolle hinzufügen,
- funktionale Rolle entfernen,
- Beschaffbarkeit für Georgia setzen,
- Beschaffbarkeit für Tobias setzen.

Bewusst **nicht** als erste Bulk-Aktion vorgesehen:

- Namen oder Codes ändern,
- Parent-/Child-Beziehungen massenhaft ändern,
- Saisonprofile ersetzen,
- Kuratornotizen ersetzen,
- Ziehungsgewichte pauschal setzen,
- physisch löschen.

Es gibt kein `alle 698 Treffer auswählen` ohne Sichtkontrolle. Bulk-Aktionen gelten nur für explizit ausgewählte Zeilen; die erste Version begrenzt eine Operation auf höchstens 200 Konzepte.

Vor Ausführung erscheint eine Zusammenfassung:

```text
37 Konzepte werden geändert
Aktion: Ziehbarkeit deaktivieren
Davon aktuell ziehbar: 34
Davon inaktiv: 3

[Abbrechen] [37 Konzepte ändern]
```

Die Bulk-Operation ist atomar. Scheitert ein Element fachlich, wird die gesamte Operation verworfen und die problematischen Einträge werden benannt.

Die Vorschau und die Ausführung enthalten für jede explizit ausgewählte Zeile die geladene Aggregatversion. Die Ausführung sperrt alle ausgewählten Konzepte in deterministischer ID-Reihenfolge und prüft sämtliche Versionen, bevor sie etwas ändert; ein Konflikt verwirft die gesamte Aktion. Bereits im Zielzustand befindliche Konzepte werden weder versioniert noch auditiert. Rollen-Bulkaktionen verwenden vor jeder Graphprüfung denselben PostgreSQL-Transaktionslock wie Rollen-, Spezifitäts- und Beziehungsänderungen im Einzelsave. Die Prüfung betrachtet den gemeinsamen resultierenden Graphen, nicht einzelne Zeilen nacheinander.

## 13. Ausschlussregeln

`Ausschlüsse` ist ein eigener Hauptbereich, verwendet aber dasselbe Grundmuster aus Liste und Detailansicht.

### 13.1 Übersicht

Eine Zeile zeigt:

- `display_text`,
- Code,
- aktiv/inaktiv,
- Ziehungsgewicht,
- Anzahl Ziele.

Filter:

- aktiv/inaktiv,
- Zielkonzept,
- `include_refinements` vorhanden ja/nein.

### 13.2 Detail

Pflegbar:

- `display_text`,
- `active`,
- `base_draw_weight`,
- `curator_note`,
- ein oder mehrere Ziele,
- je Ziel `include_refinements`.

Der Code ist wie beim Zutatenkonzept nur bei Neuanlage veränderbar.

### 13.3 Ziele

Ein Ziel wird über denselben suchbaren Zutatenpicker ausgewählt.

`include_refinements` wird als verständliche Checkbox formuliert:

> bekannte Konkretisierungen dieses Ziels mit ausschließen

Die Oberfläche zeigt bei gesetzter Option beispielhaft, dass darunter bekannte direkte und transitive Konkretisierungen fallen.

Eine aktive Ausschlussregel benötigt mindestens ein Ziel.

Inaktive Zielkonzepte sind zulässig, werden aber sichtbar als inaktiv markiert.

## 14. Referenzdaten: Rollen, Flags und Dimensionen

Die Stammdaten

- `functional_role`,
- `culinary_flag`,
- `culinary_dimension`,
- `culinary_country`,
- `participant`

werden in der ersten Webversion **nicht selbst administrierbar**.

Begründung:

- es handelt sich um kleine, fachlich bedeutende Vokabulare,
- Änderungen beeinflussen Generatorsemantik und Auswertungen,
- ein versehentliches Umbenennen oder Entfernen wäre wesentlich weitreichender als die Zuordnung zu einer Zutat,
- die aktuelle Menge ist so klein, dass Änderungen über explizite Liquibase-Migrationen vertretbar sind.

Display-Namen und Beschreibungen werden in der UI angezeigt und als Hilfetexte genutzt. Eine spätere Webpflege dieser Stammdaten benötigt eine eigene fachliche Entscheidung.

## 15. Optimistisches Locking

### 15.1 Aggregate

Für die ersten schreibenden Verwaltungsfunktionen werden mindestens folgende Spalten ergänzt:

```text
ingredient_concept.version bigint not null default 0
exclusion_rule.version     bigint not null default 0
```

Die Version eines Zutatenkonzepts schützt **das gesamte in der Weboberfläche bearbeitete Zutatenaggregat**, also auch Änderungen an:

- direkten Konkretisierungsbeziehungen,
- Rollen,
- Flags,
- Dimensionen,
- kulinarischen Länderzuordnungen,
- Beschaffbarkeit,
- Saisonwerten.

Diese Relationen erhalten nicht jeweils eigene UI-Versionen.

### 15.2 Schreibablauf

Ein Command enthält die erwartete Version. Innerhalb derselben Transaktion wird die Version nur dann erhöht, wenn der erwartete Wert noch aktuell ist.

Ein fehlgeschlagenes Version-Update erzeugt einen fachlichen Konflikt (`409 Conflict`), keine generische Erfolgsmeldung.

### 15.3 Konfliktdarstellung

Bei einem Konflikt wird **nicht automatisch überschrieben**.

Die Detailansicht zeigt:

- `Dein Stand`,
- `Aktueller Stand in der Datenbank`,
- hervorgehobene abweichende Felder.

Aktionen:

- `Aktuellen Stand laden und meine Änderungen verwerfen`,
- `Mit aktuellem Stand weiterbearbeiten`.

Bei der zweiten Variante werden die eigenen Eingaben wieder in ein Formular auf Basis der neuen Version übernommen. Vor einem erneuten Speichern muss der Nutzer die markierten Konfliktfelder bewusst prüfen.

## 16. Audit-Trail

### 16.1 Ziel

Git ist nach Einführung der Webverwaltung nicht mehr die Historie operativer Katalogänderungen. Jede erfolgreiche schreibende Administrationsaktion wird deshalb auditierbar gespeichert.

### 16.2 Datenmodell

Vorgesehen ist eine neue Tabelle `catalog_audit_entry` mit mindestens:

```text
id                bigint identity primary key
change_group_id   uuid not null
actor_key         text not null
entity_type       text not null
entity_id         bigint not null
action            text not null
before_state      jsonb
 after_state       jsonb
payload_version   smallint not null default 1
occurred_at       timestamptz not null default now()
```

`before_state` ist bei einer Anlage leer, `after_state` bei zukünftigen echten Löschoperationen gegebenenfalls leer. Für die aktuell vorgesehenen Deaktivierungen bleiben beide Entitäten erhalten.

Notwendige Indizes:

- `(entity_type, entity_id, occurred_at desc)`,
- `(actor_key, occurred_at desc)`,
- `(change_group_id)`.

Auditdaten werden in diesem kleinen privaten System zunächst unbegrenzt aufbewahrt.

### 16.3 Snapshot-Inhalt

Snapshots sind **fachliche Aggregate-Snapshots**, keine Kopien von HTTP-Formularen. Ein Zutaten-Snapshot enthält die zu diesem Zeitpunkt relevanten editierbaren Werte einschließlich Zuordnungen, darunter Ländercode und Anzeigename jeder kulinarischen Länderzuordnung.

Passwörter, Sessiondaten oder sonstige Sicherheitsgeheimnisse gelangen niemals in den Audit-Trail.

### 16.4 Beziehungspflege

Eine Konkretisierungsbeziehung betrifft zwei Zutatenkonzepte. Deshalb werden bei Hinzufügen oder Entfernen zwei Audit-Einträge mit derselben `change_group_id` geschrieben, jeweils mit Vorher-/Nachher-Snapshot des betroffenen Konzepts.

Bulk-Aktionen erzeugen pro betroffenem Konzept einen Audit-Eintrag mit gemeinsamer `change_group_id`.

### 16.5 Auditoberfläche

`Änderungen` zeigt chronologisch:

- Zeitpunkt,
- Akteur,
- Aktion,
- Entität,
- Kurzbeschreibung.

Filter:

- Akteur,
- Zeitraum,
- Entitätstyp,
- konkretes Zutatenkonzept beziehungsweise Ausschlussregel,
- Aktion.

Die Detailansicht zeigt einen feldweisen Diff. Rohes JSON ist höchstens als technische Zusatzansicht vorgesehen, nicht als primäre Darstellung.

Im Zutaten- und Ausschlussdetail werden die letzten Änderungen der jeweiligen Entität direkt eingeblendet; `Alle Änderungen anzeigen` führt gefiltert in den Auditbereich.

## 17. Administrationsidentität und Zugriffsschutz

### 17.1 Trennung vom Teilnehmermodell

`participant` bleibt ein fachliches Challenge-Konzept. Ein eingeloggter Administrator ist ein Sicherheitskonzept.

Es gibt deshalb **keinen Fremdschlüssel von Auditdaten auf `participant`** und keine automatische Ableitung von Rechten aus einem Teilnehmerdatensatz.

### 17.2 Erste Identitätsquelle

Für den privaten Zwei-Personen-Betrieb genügt zunächst eine konfigurationsbasierte Administrationsidentität hinter einer kleinen anwendungsinternen Schnittstelle.

Vorgesehen:

- ein oder zwei konfigurierte Administrationskonten,
- stabiler `actor_key`,
- Anzeigename,
- extern per Umgebungsvariable bereitgestellter, bereits sicher gehashter Passwortwert,
- keine Standardpasswörter im Repository,
- keine Registrierungs- oder Passwort-Reset-Oberfläche.

Eine Datenbanktabelle `admin_user` ist für die erste Version **nicht notwendig**. Sollte später OIDC, externe Authentifizierung oder eine echte Benutzerverwaltung benötigt werden, kann die Identitätsquelle hinter derselben Anwendungsschnittstelle ersetzt werden.

### 17.3 Spring Security

Für `/admin/**` gilt:

- Form-Login,
- Session-basierte Authentifizierung,
- CSRF-Schutz aktiv,
- `HttpOnly`-Sessioncookie,
- `SameSite=Lax` oder strenger,
- in produktiver Bereitstellung `Secure` über HTTPS,
- Session-Fixation-Schutz gemäß Spring-Security-Standard,
- keine Remember-me-Funktion in der ersten Version.

Ein inaktiver oder nicht konfigurierter Administrationsadapter darf keine unsichere Fallback-Anmeldung erzeugen.

Der Health-Endpunkt kann weiterhin ohne Login erreichbar bleiben, solange er keine sensiblen Details preisgibt.

## 18. Validierungsregeln

Die Anwendung validiert verständlich vor dem Datenbankzugriff; die Datenbank bleibt letzte Sicherung.

### 18.1 Zutatenkonzept

| Fall | Reaktion |
|---|---|
| Anzeigename leer | Feldfehler am Anzeigenamen |
| Anzeigename bereits vorhanden, unabhängig von Groß-/Kleinschreibung | Feldfehler mit Link zum bestehenden Konzept |
| Code leer/ungültiges Format bei Neuanlage | Feldfehler am Code |
| Code bereits vorhanden | Feldfehler mit bestehendem Konzept |
| Codeänderung nach Anlage | UI bietet sie nicht an; manipulierte Requests werden abgelehnt |
| `base_draw_weight <= 0` | Feldfehler am Gewicht |
| `novelty_level` außerhalb 1–5 | Feldfehler |
| Dimensionsstufe außerhalb 1–5 | Fehler an betroffener Dimension |
| Saisonmultiplikator `<= 0` | Fehler am betroffenen Monat |
| aktiv + ziehbar ohne Rolle | Fehler im Abschnitt Rollen |
| aktiv + ziehbar ohne Georgia-Beschaffbarkeit | Fehler bei Georgia |
| aktiv + ziehbar ohne Tobias-Beschaffbarkeit | Fehler bei Tobias |

### 18.2 Beziehungen

| Fall | Reaktion |
|---|---|
| Parent = Child | Auswahl wird verhindert; manipulierte Requests werden abgelehnt |
| direkte Beziehung existiert bereits | Kandidat im Picker nicht auswählbar beziehungsweise klare Meldung |
| Beziehung würde Zyklus erzeugen | verständlicher Konflikt im Beziehungsabschnitt |
| Ziel zwischen Auswahl und Speichern inaktiv geworden | Beziehung darf gespeichert werden, aber aktuelle Inaktivität wird vor Speichern angezeigt |
| Ziel nicht mehr vorhanden | Datensatz neu laden; kein technischer Fehler als Zyklus ausgeben |

Rollen- und Spezifitätsänderungen dürfen mit vorgemerkten Beziehungen in einem Save kombiniert werden. Der Picker verhindert nur sichere Strukturfehler; die finale Prüfung erfolgt gegen den vollständigen resultierenden Graphen.

### 18.3 Ausschlussregel

| Fall | Reaktion |
|---|---|
| Anzeigetext leer | Feldfehler |
| Anzeigetext bereits vorhanden, case-insensitive | Feldfehler |
| Gewicht `<= 0` | Feldfehler |
| aktive Regel ohne Ziel | Fehler im Zielabschnitt |
| doppeltes Ziel | Picker verhindert Auswahl beziehungsweise Feldfehler |

### 18.4 Konkurrenz und technische Fehler

| Fall | Reaktion |
|---|---|
| veraltete Version | `409 Conflict`, Konfliktansicht gemäß Abschnitt 15 |
| bekannte Datenbank-Constraint-Verletzung | fachlich passende Feld-/Bereichsmeldung |
| unbekannter SQL-/Technikfehler | globale Fehlermeldung mit Korrelations-ID; keine erfundene fachliche Ursache |
| Datensatz während Bearbeitung nicht mehr lesbar | klare `nicht mehr verfügbar`-Meldung und Rückkehr zur Liste |

### 18.5 Darstellung der Fehler

- Feldfehler stehen unmittelbar am Feld.
- Zusätzlich gibt es am Anfang des Detailformulars eine kompakte Fehlerzusammenfassung mit Sprunglinks.
- Erfolg wird als kurze nicht blockierende Bestätigung angezeigt.
- Eine technische Fehlermeldung enthält keine Stacktraces oder Datenbankdetails.

## 19. Lade- und UI-Zustände

### 19.1 Laden

HTMX-Teilaktualisierungen zeigen lokal dort einen Ladezustand, wo Inhalte wechseln. Der Rest der Seite bleibt bedienbar, sofern keine widersprüchliche Aktion möglich wäre.

### 19.2 Speichern

Während eines Speichervorgangs:

- ist der Speichern-Button deaktiviert,
- zeigt er eine Aktivitätsanzeige,
- wird ein Doppelsubmit verhindert.

### 19.3 Leere Treffer

Keine Suchtreffer:

```text
Keine Zutaten gefunden.
[Filter zurücksetzen]
```

Bei leerem Katalog – praktisch nur in einer fehlerhaften oder neuen Installation – wird zusätzlich `Neue Zutat anlegen` angeboten.

### 19.4 Erfolgreiches Speichern

Nach Speichern:

- bleibt dasselbe Konzept ausgewählt,
- wechselt die Ansicht zurück in read-only,
- wird die neue Version übernommen,
- erscheint eine kurze Bestätigung `Gespeichert`,
- betroffene Listen-/Baumdarstellungen werden aktualisiert.

## 20. Kleinere Displays

Desktop ist Priorität. Unter ungefähr 900 Pixel Breite wird der Split-View aufgelöst:

1. Katalogliste/Hierarchie als eigene Ansicht,
2. Auswahl öffnet eine vollständige Detailseite,
3. `Zurück zum Katalog` erhält Suche und Filter.

Die Hauptnavigation bleibt erreichbar. Erweiterte Filter dürfen auf kleinen Displays in einem direkt erreichbaren Filterpanel erscheinen.

Alle Kernfunktionen bleiben nutzbar; eine perfekte Smartphone-Pflege von Saisonprofilen mit zwölf Monatsfeldern ist ausdrücklich kein Designziel.

## 21. Application-Use-Cases und Datenprojektionen

Die folgenden Anforderungen beschreiben **fachliche Use Cases**, keine vorweggenommenen Java-Methodensignaturen.

### 21.1 Lesen

**Katalog durchsuchen**

Benötigt:

- paginierte Zeilenprojektion,
- Suchbegriff,
- Filter,
- Sortierung,
- kompakte Rollen- und Beschaffbarkeitsinformation,
- Gesamtzahl und Filterfacetten soweit für die UI nötig.

Der Länderfilter verwendet ausschließlich die öffentliche Katalog-Query. Mehrere Ländercodes werden dort mit ODER kombiniert und bleiben mit Suche, Status, Rollen, Flags und allen anderen Filterfamilien per UND verbunden. Seine Codes verbleiben wie die übrigen Filter in der URL und damit im Browser-Navigationszustand.

**Hierarchiewurzeln und direkte Kinder laden**

Benötigt pro Knoten:

- ID,
- Anzeigename,
- Status,
- Spezifität,
- Ziehbarkeit,
- Anzahl direkter Eltern,
- Kennzeichen, ob direkte Kinder existieren.

**Zutatenkonzept anzeigen**

Benötigt die vollständige Detailprojektion einschließlich direkter Beziehungen, transitiver Kontextinformation, Eigenschaften, kulinarischer Länderzuordnungen, Beschaffbarkeit, Saison und Reverse-Referenzen aus Ausschlussregeln.

**Ausschlussregeln durchsuchen und anzeigen**

Benötigt Listen- und Detailprojektionen einschließlich Zielinformationen.

**Audit durchsuchen**

Benötigt Listenprojektion, Filter, feldweisen Diff und Entity-bezogene Historie.

### 21.2 Schreiben

**Zutatenkonzept anlegen**

Eine Transaktion für Basisdaten und die in diesem Paket bereits implementierten Zuordnungen einschließlich der expliziten Ländermenge. Bei Anlage zunächst Version 0; Audit-Eintrag nach erfolgreicher Persistenz in derselben Transaktion.

**Zutatenkonzept ändern**

Eine Transaktion mit erwartetem Versionswert. Basisfelder, Rollen, Eigenschaften, kulinarische Länderzuordnungen, Beschaffbarkeit, Saison und vorgemerkte direkte Beziehungen werden gegen denselben resultierenden Zustand validiert, atomar gespeichert, genau einmal versioniert und auditiert. Vor dem Graph-Read/Validate/Write-Ablauf serialisiert ein PostgreSQL-Transaktionslock Relations- und Spezifitätsänderungen; Rollen bleiben davon unabhängig.

**Konkretisierungsbeziehung hinzufügen/entfernen**

Eine Transaktion; beide betroffenen Konzepte werden versionsgeprüft beziehungsweise konsistent gesperrt und gemeinsam auditiert. Mehrere Kanten desselben Saves behandeln jeden Gegenknoten nur einmal. Vor der resultierenden Graphprüfung serialisiert ein PostgreSQL-Transaktionslock sämtliche Graphmutationen; kein Netzwerkzugriff in der Transaktion.

**Ausschlussregel anlegen/ändern**

Eine Transaktion mit Zielen, Version und Audit.

**Bulk-Änderung**

Eine Transaktion für alle explizit ausgewählten Konzepte. Versionen aller Elemente werden geprüft; Teilupdates sind nicht zulässig.

### 21.3 Keine direkte Adapterpersistenz

Controller kennen weder `JdbcTemplate` noch SQL. Sie rufen öffentliche Application-Use-Cases des Katalogmoduls auf und transformieren Ergebnisse in Web-View-Models.

## 22. Technische Webschnittstelle

Die konkrete Routenstruktur darf während der Umsetzung geringfügig geschärft werden. Die fachliche Form soll jedoch ungefähr so aussehen:

```text
GET  /admin/catalog
GET  /admin/catalog/{id}
GET  /admin/catalog/hierarchy/roots
GET  /admin/catalog/{id}/children
GET  /admin/catalog/new
POST /admin/catalog
POST /admin/catalog/{id}

GET  /admin/exclusions
GET  /admin/exclusions/{id}
GET  /admin/exclusions/new
POST /admin/exclusions
POST /admin/exclusions/{id}

GET  /admin/audit
```

HTMX-spezifische Fragmentantworten dürfen dieselben Use Cases verwenden. Es wird keine parallele JSON-API ausschließlich deshalb angelegt, weil moderne Anwendungen angeblich eine brauchen.

## 23. Folgepakete

Die Implementierung wird nach dieser Spezifikation in sechs fachlich getrennte Pakete zerlegt.

### Paket A – Administrationssicherheit und Schreibfundament

**Scope**

- Spring Security und konfigurationsbasierte Administrationsidentitäten,
- Aktivierung/Deaktivierung des Administrationsadapters,
- `version` auf `ingredient_concept` und `exclusion_rule`,
- `catalog_audit_entry`,
- technische Audit- und Locking-Grundlagen im Katalogmodul,
- noch keine produktive Katalogbearbeitung.

**Gate**

- `/admin/**` ist ohne Login nicht erreichbar,
- keine Default-Zugangsdaten,
- Versionierungs- und Auditschema ist per PostgreSQL-Integrationstest geprüft,
- Modulgrenzen bleiben grün.

### Paket B – Lesende Katalogverwaltung

**Scope**

- Thymeleaf-/HTMX-Webshell,
- Katalogsuche und Filter,
- Hierarchie und Liste,
- vollständige read-only Detailansicht,
- kleine-Display-Fallback,
- keine fachlichen Schreibzugriffe.

**Gate**

- alle Katalogfelder sind lesend sichtbar,
- Mehrfach-Eltern funktionieren in realen Daten,
- Suche/Filter bleiben bei Navigation erhalten,
- MVC-/Integrationstests decken die Hauptansichten ab.

### Paket C – Zutatenkonzept-Basisbearbeitung

**Scope**

- Neuanlage,
- Anzeigename,
- Aktivstatus,
- Ziehbarkeit,
- Spezifität,
- Gewicht,
- Kochungewöhnlichkeit,
- Kuratornotiz,
- optimistisches Locking und Audit in produktiven Schreibflows.

**Gate**

- Konfliktverhalten ist getestet,
- Code ist nach Anlage unveränderlich,
- Deaktivierung erhält Beziehungen und Historie,
- unbekannte Datenbankfehler werden nicht fachlich maskiert.

### Paket D – Konkretisierungsbeziehungen

**Scope**

- Parent-/Child-Picker,
- Hinzufügen/Entfernen direkter Beziehungen,
- transitive Kontextanzeige,
- Vorab-Zyklusprüfung plus PostgreSQL-Trigger als letzte Sicherung,
- beidseitiger Audit-Eintrag.

**Gate**

- Mehrfach-Eltern bleiben vollständig erhalten,
- echte Konkurrenz- und Zyklusfälle sind gegen PostgreSQL getestet,
- keine stillschweigenden Beziehungslöschungen.

### Paket E – Rollen, Eigenschaften, Beschaffbarkeit und Saison (abgeschlossen mit Issue #24)

**Scope**

- Rollen,
- Flags,
- Dimensionen,
- Georgia-/Tobias-Beschaffbarkeit,
- Monatsfaktoren,
- vollständige Ziehbarkeits-Pflichtvalidierung,
- ein gemeinsamer Save mit Basisfeldern und vorgemerkten Beziehungen.

**Gate**

- aktive Ziehkandidaten können nicht ohne Rollen gespeichert werden; ungepflegte Beschaffbarkeit für Georgia oder Tobias bleibt ein redaktioneller Hinweis, `OPEN` benötigt keine direkte Konkretisierung,
- fehlende Dimensions- und Saisonwerte behalten ihre dokumentierte Semantik,
- alle Änderungen sind versionsgesichert und auditiert.

### Paket F – Ausschlüsse, Bulk und Auditoberfläche

**Scope**

- Ausschlussregeln und Ziele,
- Bulk-Aktionen,
- Auditliste und Diffansicht,
- Entity-bezogene Änderungshistorie.

**Gate**

- aktive Ausschlüsse besitzen Ziele,
- Bulk-Aktionen sind atomar und begrenzt,
- Auditänderungen sind für die normalen Pflegeflows verständlich nachvollziehbar.

Erst nach diesen Paketen gilt die Webverwaltung als vollständige Katalogpflegebasis für die anschließende Generatorarbeit.

## 24. Bewusst vertagte Punkte

Folgende Punkte blockieren die erste Webverwaltung nicht:

- persistente gespeicherte Filter,
- physisches Löschen von Katalogobjekten,
- Webpflege von Rollen/Flags/Dimensionen,
- visuelle frei positionierbare Graphdarstellung,
- Challenge-Historie als eigener Verwaltungsbereich,
- OIDC oder externe Identity Provider,
- Passwortänderung über die Anwendung,
- feinere Rollen wie read-only versus editor,
- Export/Import des operativen Katalogs.

Wenn einer dieser Punkte später relevant wird, wird er als eigenes Paket spezifiziert statt stillschweigend in einen bestehenden CRUD-Flow eingeschmuggelt.

## 25. Generator-Labor (Phase 9E1 / Issue #37)

`/admin/generator` ist eine geschützte serverseitig gerenderte Diagnoseansicht. Sie ergänzt die Katalogverwaltung,
ersetzt aber keinen produktiven Challenge-Flow und enthält keine Katalogbearbeitung.

Die Preview nimmt fachliches Datum, INITIAL/REROLL, optionalen Seed, einen stabilen Historienszenariocode und
null bis zwei manuelle Vorgaben und einen Restriction Mode entgegen. Der verwendete Seed wird angezeigt. Die Oberfläche besitzt
**keine REROLL-Hardblock-IDs**: ein diagnostischer REROLL erhält Wiederholungswirkung ausschließlich aus den exakten
Konzeptcodes des gewählten sichtbaren Historiensnapshots und dem normalen Cooldown. Der Konkretisierungsgraph erweitert
diesen Cooldown nicht auf Parent-, Child- oder Sibling-Konzepte. Die Aktion verwendet die öffentliche Challenge-API,
besitzt CSRF-Schutz und erzeugt weder Session, Attempt, Batch, Candidate noch sichtbare Challenge; sie verändert deshalb
weder Cooldowns noch Neuigkeitskadenz.

Die Ergebnisansicht zeigt die zwölf Kandidaten mit Requirements, Ziel-/Ist-Neuigkeit, Scores und Reason-Codes,
Setquoten, Reservoir-/Fallbackdiagnostik, Nutzung, Auswahlentscheidungen und die bestehende PairAssessment als
Autorität des Paarvergleichs. Rohsnapshots sind nur ergänzende einklappbare Diagnose.

Ein Persisted-Abschnitt lädt Attempt und Batch ausschließlich über `GenerationQueries`: Datum, Statuszeiten,
Versionen, Fingerprints, historische Candidate-/Requirement-Snapshots und Legacy-Grenzen bleiben sichtbar.
Replay ist ein read-only POST mit CSRF-Schutz und zeigt Match, nicht unterstützte Version, ungültigen Snapshot oder
die erste strukturierte Differenz. Aktuelle Katalogwerte reparieren keine historischen Anzeige- oder Replaydaten.
Nicht unterstützte Snapshotversionen werden sichtbar als nicht unterstützt ausgewiesen und nicht nachgebildet.

### Simulation (Phase 9E3 / Issue #54)

Der klar getrennte Bereich **Simulation** ruft ausschließlich die öffentliche `GeneratorSimulation`-API auf. Er nimmt
Startseed, Seedanzahl, Startdatum, einen Monatsdurchlauf von `1..12`, Historienszenario, INITIAL/REROLL sowie null bis
zwei Manuals entgegen. Katalog-IDs aus dem vorhandenen Picker werden vor dem Lauf lesend über `CatalogQueries` auf
stabile Codes aufgelöst. Generatorgewichte, Quoten, Ausschlussvariante und sichtbare Kandidatenposition sind nicht editierbar.
REROLL besitzt auch hier keine separaten Blockfelder; die Simulation verwendet ausschließlich die normale exakte
Cooldown-Semantik ihres Historienszenarios.

```text
Simulation
  Startseed | Seedanzahl | Startdatum | Monatsanzahl
  Historienszenario | INITIAL/REROLL
  0–2 Manuals mit optionalem Katalogmatch
  REROLL: normaler exakter Cooldown aus dem gewählten Historienszenario
  [Simulation starten]

  Status: vollständig / TIMED_OUT / ABORTED / INCOMPLETE
  Fälle, Erfolge, Erschöpfungen und technische Fehler
  begrenzte Reportaggregate, Versionen, Seedpläne und Katalogfingerprints
```

Ein Request umfasst höchstens 64 expandierte Fälle und erhält ausschließlich serverseitig eine feste Deadline mit
`FAIL_FAST` für unbekannte technische Fehler. Jeder Monat ist ein eigener Single-Step-Szenarioschritt; die gewählte
Historie startet je Monat neu. Die Seite zeigt unvollständige Läufe ausdrücklich als Teilreport und trennt fachliche
Erschöpfung von technischen Fehlern. Frequenzlisten werden ohne erneute Sortierung oder Erweiterung aus dem begrenzt
gelieferten Report gerendert.

`POST /admin/generator/simulation` besitzt CSRF-Schutz und funktioniert als vollständiger servergerenderter POST ohne
JavaScript. Mit HTMX liefert derselbe Endpunkt nur das Ergebnisfragment; das Submit wird währenddessen deaktiviert.
Zusätzlich erlaubt ein flüchtiger, in `finally` freigegebener Guard höchstens einen laufenden Request pro
Administrationssession. Es gibt weder Hintergrundjob noch persistierten Simulationszustand.

Simulation und Report aus #53, ihr Adminadapter aus #54 und die historische Kalibrierung aus #40 bleiben getrennte
Pakete. Die spätere Persistenz eines tatsächlich sichtbaren, vollständig rerollten Offer Sets mit 1–3 Optionen ist
Phase 10/11 und wird im Generator-Labor nicht simuliert oder gespeichert.

## 26. Abnahmekriterium dieser Spezifikation

Nach diesem Dokument sind für den Start von Paket A keine fachlichen oder gestalterischen Entscheidungen mehr offen, die dessen Scope blockieren.

Insbesondere sind entschieden:

- Hauptnavigation,
- Zusammenspiel von Hierarchie, Liste und Detailansicht,
- Mehrfach-Eltern-Semantik,
- Sichtbarkeit und Bearbeitungsort aller aktuellen Katalogfelder,
- Anlage und Deaktivierung,
- Löschregeln,
- Beziehungspflege,
- Ausschlussregeln,
- Bulk-Grenzen,
- Locking,
- Audit,
- Administrationsidentität,
- Zugriffsschutz,
- Validierungsdarstellung,
- notwendige Schemaergänzungen,
- Use Cases und Implementierungsreihenfolge.
