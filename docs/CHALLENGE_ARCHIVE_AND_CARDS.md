# Öffentliche Challenge-Historie und Challenge-Cards

Stand: 21. August 2026  
Issue: #139  
Folgepakete: #140 und #141

Dieses Dokument ist die verbindliche Fach-, Persistenz- und Adapter-Spezifikation für die erste Erweiterung nach Version 0.1. Es beschreibt ausschließlich die öffentlich abrufbare letzte Challenge, die Historie bestätigter Challenges und die optionale Zuordnung einer außerhalb des Bots erzeugten Challenge-Card.

Persönliche Konkretisierungen, Zusatz-Zutaten, Kochpläne, Fortschrittsmeldungen, Ergebnisfotos und Bewertungen bleiben ausdrücklich späteren Paketen vorbehalten.

## 1. Ziel

Der private Discord-Channel darf neben der Botbedienung auch für normale Diskussionen über Planung, Beschaffung und Kochfortschritt verwendet werden. Die ursprüngliche Bestätigungsnachricht einer Challenge kann dadurch schnell im Channelverlauf verschwinden.

Jedes Mitglied der konfigurierten Guild soll deshalb jederzeit:

- die letzte bestätigte Challenge erneut abrufen,
- eine kompakte Liste aller bestätigten Challenges sehen,
- eine bestimmte vergangene Challenge anhand ihrer öffentlichen Nummer anzeigen,
- und dabei, sofern vorhanden, die zugehörige gestaltete Challenge-Card sehen können.

Die Anwendung wird damit noch nicht zum vollständigen persönlichen Challenge-Lifecycle. Sie erhält zunächst eine dauerhafte öffentliche Challenge-Akte und ein kleines Archiv.

## 2. Autoritäten und Begriffe

### 2.1 Bestätigte Challenge

Eine Challenge gehört erst dann in dieses Archiv, wenn der bestehende Offer-/Voting-Lifecycle genau eine operative `challenge` materialisiert hat.

Nicht im Archiv erscheinen:

- offene oder fehlgeschlagene Generation Sessions,
- kuratierte, aber noch nicht präsentierte Offer Sets,
- präsentierte Angebote vor der Entscheidung,
- nicht gewählte Angebote,
- vollständig rerollte Offer Sets,
- abgebrochene oder technisch fehlgeschlagene Versuche ohne materialisierte Challenge.

Das Archiv ist damit keine Chronik der Entscheidungsfindung, sondern ausschließlich die Liste der tatsächlich bestätigten Kochvorgaben.

### 2.2 Öffentliche Challenge-Nummer

Jede bestätigte Challenge erhält genau eine positive fortlaufende `challenge_number`. Diese Nummer ist fachlicher öffentlicher Bezeichner und darf nicht aus Session-, Attempt-, Offer-, Candidate- oder Datenbank-IDs abgeleitet beziehungsweise mit ihnen gleichgesetzt werden.

Die Nummer:

- beginnt bei `1`,
- ist eindeutig,
- ist nach der Vergabe unveränderlich,
- wird nur zusammen mit einer erfolgreich commiteten Challenge-Materialisierung vergeben,
- wird durch fehlgeschlagene oder zurückgerollte Materialisierungen nicht verbraucht,
- wird durch Card-Änderungen niemals verändert.

Bereits vorhandene bestätigte Challenges werden bei der Einführung deterministisch aufsteigend nach `shown_at`, danach nach `challenge.id`, mit `1..n` nummeriert. Damit werden auch während des privaten Produktionspiloten entstandene Challenges sauber in die spätere öffentliche Folge aufgenommen.

Die Vergabe neuer Nummern muss global konkurrenzsicher in PostgreSQL erfolgen. Eine nichttransaktionale Datenbanksequenz, die bei Rollbacks sichtbare Lücken erzeugen kann, genügt für diesen öffentlichen Zähler nicht. Geeignet ist beispielsweise ein einzelner transaktional gesperrter Counter-Datensatz; die konkrete SQL-Ausgestaltung bleibt dem Implementierungspaket überlassen.

### 2.3 Aktuelle Challenge

Bis einschließlich Phase 13 bezeichnete `aktuell` die bestätigte Challenge mit der höchsten öffentlichen Challenge-Nummer. Seit der verbindlichen Ergebnis- und Abschlussspezifikation ist dieser Begriff durch **letzte Challenge** ersetzt: Sie ist weiterhin die höchste öffentliche Nummer, unabhängig vom Status. **Aktive Challenges** sind davon getrennt alle Zeilen mit Status `ACTIVE`, stabil neueste zuerst. Die vollständige Status-, Abschluss- und Ergebnisdarstellung regelt [`CHALLENGE_RESULTS_AND_COMPLETION.md`](CHALLENGE_RESULTS_AND_COMPLETION.md); dessen Vorgaben gehen diesem früheren Phase-13-Abschnitt vor.

Daraus folgt:

- Während einer neuen Generierung oder Abstimmung bleibt die bisherige letzte Challenge unverändert.
- Erst die erfolgreiche Materialisierung einer neuen Challenge wechselt die letzte Challenge.
- Eine ältere Challenge muss dafür nicht auf `COMPLETED` gesetzt werden.
- Der Abschluss ist eine unabhängige explizite Admin-Entscheidung und wird nicht automatisch abgeleitet.

Diese bewusst einfache Semantik vermeidet die Behauptung, der Bot wisse bereits, ob eine Challenge tatsächlich gekocht, verschoben oder aufgegeben wurde.

### 2.4 Historische Challenge

Alle bestätigten Challenges außer der letzten mit der höchsten Nummer sind in diesem ersten Stand historische Challenges. Die Historie enthält die letzte Challenge ebenfalls in der Gesamtliste und markiert sie sichtbar als letzte.

### 2.5 Challenge-Card

Eine Challenge-Card ist ein optionales gestaltetes Bild einer bestätigten Challenge. Sie wird außerhalb des Bots nach [`../design/challenge-cards/WORKFLOW.md`](../design/challenge-cards/WORKFLOW.md) erzeugt und anschließend ausdrücklich einer Challenge zugeordnet.

Die Card ist ausschließlich ein Darstellungsartefakt:

- Die persistierten Challenge-Snapshots bleiben fachliche Autorität.
- Textliche Challenge-Informationen werden auch bei vorhandener Card immer angezeigt.
- Der Bot liest Zutaten oder Einschränkungen niemals per OCR aus dem Bild.
- Eine falsch gestaltete Card verändert die Challenge-Fakten nicht.
- Das Entfernen oder Ersetzen einer Card verändert weder Nummer noch Anforderungen, Einschränkung oder Historienwirkung.

## 3. Öffentlich sichtbare Challenge-Fakten

Die öffentliche Detailprojektion enthält ausschließlich:

- `challengeNumber`,
- Bestätigungszeitpunkt aus dem autoritativen Challenge-Snapshot,
- exakt vier Requirement-Snapshots in Position `1..4`,
- je Requirement den damaligen sichtbaren Text,
- je Requirement die damalige Challenge-Spezifität `OPEN` oder `SPECIFIC`, soweit sie im Snapshot vorhanden ist,
- den damaligen Restriction-Text oder ausdrücklich keine Einschränkung,
- die Information, ob eine Challenge-Card vorhanden ist.

Für die Darstellung gilt:

- `OPEN` wird nutzerverständlich als `offener Begriff` gekennzeichnet.
- `SPECIFIC` benötigt keinen zusätzlichen Badge oder technischen Begriff.
- Eine fehlende Restriction erscheint als `Keine`.
- Requirement- und Restriction-Texte stammen ausschließlich aus den historischen Snapshots.
- Spätere Katalogumbenennungen, Deaktivierungen oder Regeländerungen verändern historische Challenge-Anzeigen nicht.

Nicht öffentlich angezeigt oder transportiert werden:

- Session-, Attempt-, Batch-, Round-, Candidate-, Offer-Set-, Offer- oder Datenbank-IDs,
- gewünschte oder gelieferte Zahl der Angebote,
- nicht gewählte oder rerollte Angebote,
- einzelne Votes oder deren Änderungsverlauf,
- Tie-Break und Losentscheid,
- Reroll-Verbrauch oder Reroll-Exposition,
- Teilnehmer-/Electorate-Snapshots,
- Kuratorbewertung, Reason Codes, Prompt- oder Contractversion,
- Modell, Tokens, Kosten, Providerrequest oder technische Diagnose,
- Generatorversion, Seed, Fingerprint oder Replaydaten,
- interne Challenge-Statuswerte.

Die vorhandenen Audit- und Workflow-Projektionen werden deshalb nicht direkt als Discord-Archivmodell wiederverwendet. Der Challenge-Core erhält eine eigene schmale öffentliche Projektion.

## 4. Transportneutrale Archivprojektion

Der Challenge-Core stellt eine kleine öffentliche Read-API bereit. Die konkreten Java-Namen sind nicht zwingend, der fachliche Vertrag entspricht jedoch mindestens:

```text
findCurrentChallenge()
findChallengeByNumber(challengeNumber)
listChallenges(page, pageSize)
findChallengeCardMetadata(challengeNumber)
loadChallengeCard(challengeNumber)
```

### 4.1 Detail

Die Detailprojektion enthält alle in Abschnitt 3 beschriebenen Challenge-Fakten und `cardAvailable`.

Die Card-Binärdaten werden nicht automatisch mit jedem Detailobjekt geladen. Metadaten und Blob bleiben getrennte Queries, damit Listen- und Textantworten keine unnötigen Binärdaten aus PostgreSQL ziehen.

### 4.2 Liste

Die öffentliche Liste ist stabil nach `challenge_number DESC` sortiert und paginiert.

Für Discord gilt verbindlich:

- zehn Challenges pro Seite,
- Seitenzählung beginnt bei `1`,
- Defaultseite ist `1`,
- die Projektion liefert mindestens Gesamtzahl, letzte Nummer, aktuelle Seite, Gesamtseiten und die Einträge,
- jeder Eintrag enthält Nummer, vier kompakte Requirement-Snapshots und `cardAvailable`,
- kein Card-Blob wird für die Liste geladen.

Die Core-API darf ein allgemeineres, defensiv begrenztes Seitenlimit besitzen. Der Discord-Adapter verwendet dennoch fest zehn Einträge.

Eine Seite außerhalb des gültigen Bereichs erzeugt keine willkürlich leere Erfolgsliste. Sie wird typisiert beziehungsweise eindeutig als ungültig behandelt. Existiert noch keine Challenge, ist Seite `1` eine verständliche leere Ausgangslage.

## 5. Persistenz der öffentlichen Nummer

Die öffentliche Nummer wird an der bestehenden `challenge` gespeichert. Sie ist Teil der Challenge-Identität und kein ausschließlich berechnetes Anzeigeattribut.

Die Migration muss:

1. bestehende Challenge-Zeilen deterministisch nach `shown_at`, `id` nummerieren,
2. anschließend Positivität, Eindeutigkeit und Nicht-Nullbarkeit erzwingen,
3. einen transaktionalen Ausgangszähler auf den höchsten vergebenen Wert beziehungsweise `0` setzen,
4. neue Challenge-Materialisierung und Nummernvergabe in derselben Application-Service-Transaktion ausführen,
5. konkurrierende Bestätigungen ohne Duplikate oder verlorene bestätigte Nummern serialisieren,
6. Rollbacks vollständig zurücknehmen.

Die öffentliche Nummer bleibt bei späteren Schema- oder Lifecycle-Erweiterungen stabil. Challenges werden über den normalen Produktpfad nicht physisch gelöscht; eine spätere fachliche Abbruch- oder Abschlusssemantik darf Nummern nicht neu verwenden.

## 6. Persistenz der Challenge-Card

### 6.1 Eine aktuelle Card pro Challenge

Pro Challenge existiert höchstens eine aktuelle Card. Die Persistenz kann beispielsweise eine Tabelle `challenge_card` mit einer eindeutigen Challenge-Referenz verwenden.

Mindestens zu speichern sind:

- `challenge_id`,
- exakte Bildbytes als PostgreSQL-`bytea`,
- kanonischer Content-Type `image/png`,
- ursprünglicher Dateiname als Metadatum,
- Byteanzahl,
- SHA-256 der gespeicherten Bytes,
- Erstellungszeitpunkt,
- letzter Änderungszeitpunkt.

Ein generisches Medien- oder Dateisystem wird nicht eingeführt. Ein separater Object Store, CDN oder öffentliches Dateiverzeichnis ist für diesen privaten, kleinen Bestand nicht erforderlich.

Die Card gehört damit zu denselben PostgreSQL-Backups und Restores wie die Challenge. Discord-CDN-URLs, Message-IDs oder Attachment-URLs sind keine dauerhafte Fachautorität.

### 6.2 Verbindliches Format

Eine Card ist nur zulässig, wenn alle folgenden Bedingungen erfüllt sind:

- tatsächliches PNG,
- exakt `1200 × 1200 px`,
- höchstens `5 MiB` beziehungsweise `5 * 1024 * 1024` Bytes,
- vollständig decodierbar,
- nicht leer.

Dateiendung und deklarierter Content-Type genügen nicht. Der transportneutrale Core prüft mindestens Dateigröße, PNG-Signatur und tatsächlich decodierte Dimensionen. Der Discord-Adapter darf offensichtliche Verstöße vorab abweisen, ersetzt aber niemals die Core-Validierung.

Die Bildbytes werden unverändert gespeichert. Der Bot:

- skaliert nicht,
- komprimiert nicht neu,
- schneidet nicht zu,
- verändert keine Farben,
- erzeugt keine Vorschaukopie als neue Fachdatei.

### 6.3 Setzen, Ersetzen und Entfernen

Transportneutrale Schreiboperationen erlauben:

```text
setChallengeCard(challengeNumber, upload, replaceExisting)
removeChallengeCard(challengeNumber)
```

Semantik:

- Existiert keine Card, darf sie mit `replaceExisting=false` gesetzt werden.
- Existiert bereits eine Card, wird ein Setzen ohne ausdrückliches Replace abgewiesen und verändert nichts.
- `replaceExisting=true` ersetzt die aktuelle Card atomar.
- Ein Replace verändert `created_at` nicht zwingend, muss aber `updated_at`, Bytes und alle abgeleiteten Metadaten konsistent aktualisieren.
- Entfernen löscht ausschließlich die Card-Zeile beziehungsweise Card-Daten.
- Eine unbekannte Challenge oder fehlende Card wird typisiert und ohne Teilmutation behandelt.
- Eine Card-Versionierung oder Card-Audit-Historie gehört nicht zum ersten Stand.

## 7. Discord-Commandstruktur

Der vorhandene Slash-Command `/challenge` startet weiterhin eine neue Auswahl und behält seine bisherigen Optionen. Da Discord einen Root-Command nicht gleichzeitig mit direkten Optionen und Subcommands mischen soll, erhält das Archiv bewusst einen eigenen pluralischen Root:

```text
/challenges letzte
/challenges aktiv [seite]
/challenges liste [seite]
/challenges anzeigen nummer:<n>
/challenges abschließen [nummer:<n>]
/challenges karte-setzen bild:<attachment> [nummer:<n>] [ersetzen:<bool>]
/challenges karte-entfernen nummer:<n>
```

### 7.1 `/challenges letzte`

Zeigt die bestätigte Challenge mit der höchsten öffentlichen Nummer. Existiert noch keine Challenge, erscheint eine kurze verständliche öffentliche Ausgangsmeldung ohne technische Details.

### 7.2 `/challenges aktiv`

- `seite` ist optional, ganzzahlig und mindestens `1`.
- Ohne Parameter wird Seite `1` angezeigt.
- Die Liste enthält zehn `ACTIVE`-Challenges, neueste zuerst, und darf leer sein.
- Sie lädt keine Card- oder Ergebnisfoto-Bytes.

### 7.3 `/challenges liste`

- `seite` ist optional, ganzzahlig und mindestens `1`.
- Ohne Parameter wird Seite `1` angezeigt.
- Die Liste enthält zehn Challenges, neueste zuerst.
- Die letzte Challenge wird sichtbar markiert.
- Vorhandene Cards werden knapp mit `🖼️` markiert.
- Es gibt im ersten Stand keine Buttons, Selects, Autocomplete- oder zustandsbehaftete Pagination.

### 7.4 `/challenges anzeigen`

- `nummer` ist erforderlich und positiv.
- Die Detaildarstellung entspricht exakt der Darstellung von `letzte`.
- Eine unbekannte Nummer erzeugt eine kurze verständliche Rückmeldung.

### 7.5 `/challenges abschließen`

- `nummer` ist optional; ohne Nummer löst der Adapter nur dann auf, wenn genau eine aktive Challenge existiert.
- Bei keiner oder mehreren aktiven Challenges wird eine explizite Nummer verlangt.
- Der idempotente Core-Abschluss wird nach erfolgreicher Mutation mit der aktualisierten öffentlichen Detailansicht bestätigt.

### 7.6 `/challenges karte-setzen`

- `bild` ist genau ein erforderliches Discord-Attachment.
- `nummer` ist optional; ohne Nummer gilt die letzte Challenge.
- `ersetzen` ist optional und standardmäßig `false`.
- Bei bereits vorhandener Card ist `ersetzen:true` erforderlich.
- Nach erfolgreichem Setzen oder Ersetzen postet der Bot öffentlich unmittelbar die vollständige Challenge-Detaildarstellung einschließlich Card.

### 7.7 `/challenges karte-entfernen`

- `nummer` ist erforderlich.
- Eine explizite Nummer verhindert versehentliches Entfernen an einer zwischenzeitlich neu gewordenen letzten Challenge.
- Nach erfolgreicher Entfernung postet der Bot öffentlich die textuelle Challenge-Detaildarstellung ohne Bild.

## 8. Discord-Berechtigungen

### 8.1 Guild-weite Lesezugriffe

`letzte`, `aktiv`, `liste` und `anzeigen` dürfen von jedem Mitglied der konfigurierten Guild verwendet werden. Eine Registrierung als Challenge-Teilnehmer oder DB-Identitätszuordnung ist nicht erforderlich.

In DMs und fremden Guilds wird vor jeder Core-Query abgewiesen.

### 8.2 Operatorgebundene Schreibzugriffe

`abschließen`, `karte-setzen` und `karte-entfernen` dürfen ausschließlich Mitglieder mit der bereits konfigurierten `challenge-operator-role-id` verwenden.

Dabei gilt dieselbe Trennung wie für `/challenge`:

- Operatorberechtigung ist unabhängig von Challenge-Teilnahme und Electorate.
- Ein Operator muss keine Teilnehmeridentität in der Datenbank besitzen.
- Ein Teilnehmer ohne Operatorrolle darf Cards lesen, aber nicht verändern.
- Der Bot benötigt keine Discord-Administratorberechtigung.

Die Rollenprüfung erfolgt vor:

- Attachment-Download,
- Bilddekodierung,
- Core-Schreibzugriff,
- Datenbankmutation.

Es werden keine neuen privilegierten Intents und kein allgemeiner Message-Content-Listener eingeführt.

## 9. Discord-Darstellung

### 9.1 Detailansicht

`letzte` und `anzeigen` verwenden denselben Renderer. Verbindlicher Inhalt:

```text
Challenge #12
Bestätigt am 21. August 2026

Status
Abgeschlossen
Abgeschlossen am 23. August 2026, 14:30 Uhr

Ergebnisse
2 Ergebnisse

Vorgaben
1. Tempeh
2. Mayonnaise
3. Kohlgemüse · offener Begriff
4. Essig · offener Begriff

Einschränkung
Keine
```

Das konkrete Layout darf als kompaktes Embed umgesetzt werden. Wichtig sind:

- Challenge-Nummer im Titel,
- Bestätigungsdatum in der konfigurierten Zone, standardmäßig `Europe/Berlin`,
- stabile Positionen `1..4`,
- sichtbare, textliche `OPEN`-Kennzeichnung,
- explizite Restriction-Zeile,
- keine internen IDs,
- keine Informationen über den Auswahlweg.

Ist eine Card vorhanden, wird sie als großes Bild derselben Antwort angehängt. Der Adapter lädt die persistierten Bytes gezielt und sendet sie mit einem stabilen Dateinamen:

```text
challenge-<nummer>.png
```

Das Embed darf `attachment://challenge-<nummer>.png` referenzieren. Eine Discord-CDN-URL wird nicht gespeichert.

Die Textdarstellung bleibt auch mit Card vollständig vorhanden. Das Bild allein ist weder zugänglich genug noch fachliche Autorität.

### 9.2 Listenansicht

Beispiel:

```text
Bisherige Challenges · Seite 1/3

#12 · letzte · abgeschlossen · 2 Ergebnisse · 🖼️
Tempeh · Mayonnaise · Kohlgemüse (offen) · Essig (offen)

#11
Kabeljau · Birne · Miso · Blattgemüse (offen)
```

Die Liste zeigt keine Bilder. Bei sehr langen Namen wird innerhalb eines Eintrags deterministisch mit sichtbarer Ellipse gekürzt; keine Challenge der angeforderten Seite wird still weggelassen. Die vollständigen Texte bleiben über `anzeigen` abrufbar.

### 9.3 Sichtbarkeit und Fehler

- Erfolgreiche Leseantworten sind öffentlich.
- Erfolgreiches Setzen, Ersetzen oder Entfernen wird ebenfalls durch die neue öffentliche Detaildarstellung sichtbar bestätigt.
- Autorisierungs-, Eingabe-, Format-, Größen-, Missing- und technische Fehlermeldungen dürfen ephemer bleiben.
- Allowed Mentions werden für sämtliche Archiv- und Card-Antworten deaktiviert.
- Snapshottexte und Dateinamen werden gegen unbeabsichtigte Markdown- oder Mention-Wirkung abgesichert.

## 10. Upload- und Fehlerablauf

Der Discord-Adapter prüft zunächst Guild und Operatorrolle. Erst danach darf er das Attachment laden.

Der empfohlene Ablauf lautet:

1. Command und Berechtigung prüfen.
2. Deklarierte Attachmentgröße und offensichtlichen Content-Type prüfen.
3. Interaktion rechtzeitig deferieren.
4. Attachmentbytes außerhalb einer Datenbanktransaktion laden.
5. Bytes und Metadaten an den transportneutralen Core übergeben.
6. Core validiert tatsächliches Format und persistiert atomar.
7. Erfolgszustand frisch lesen und öffentlich rendern.

Externe Netzwerkzugriffe finden niemals innerhalb offener Datenbanktransaktionen statt.

Schlägt die Discord-Auslieferung nach einer erfolgreich commiteten Card-Mutation fehl, bleibt der persistierte Core-Zustand autoritativ. Der Adapter darf den Vorgang nicht als fachlich zurückgerollt darstellen. Ein erneuter `letzte`-/`anzeigen`-Aufruf zeigt den tatsächlichen Zustand; ein sicherer erneuter Upload mit passender Replace-Semantik bleibt möglich.

## 11. Architekturgrenzen

### 11.1 Challenge-Core

Der transportneutrale Core aus Phase 13A / #140 besitzt:

- Nummernvergabe in der bestehenden Challenge-Materialisierung,
- öffentliche Archivqueries,
- Card-Queries,
- Card-Commands,
- PNG- und Dimensionsvalidierung,
- Spring-JDBC-Persistenz und Transaktionen.

Er kennt keine:

- JDA-Typen,
- Slash Commands,
- Discord-Rollen,
- Attachments oder Discord-CDN-URLs,
- Discord-Nachrichten oder Embeds.

### 11.2 Discord-Adapter

Der Adapter aus Phase 13B / #141:

- registriert und parst `/challenges`,
- prüft Guild beziehungsweise Operatorrolle,
- lädt autorisierte Attachments,
- mappt Core-Projektionen auf Discord-Ausgaben,
- sendet persistierte Cardbytes als Attachment.

Er greift niemals direkt auf JDBC oder Tabellen zu und rekonstruiert keine Challenge-Snapshots aus Offer-, Voting- oder Katalogdaten.

### 11.3 Kein neues Modul

Das Feature rechtfertigt kein allgemeines Medienmodul. Archiv und Card gehören fachlich zur bestätigten Challenge und bleiben im bestehenden `challenge`-Modul. `discord` verwendet ausschließlich dessen öffentliche API.

## 12. Migration und Betrieb

- Neue Schemaänderungen erfolgen ausschließlich über append-only Liquibase-Changesets.
- Bereits veröffentlichte Changesets werden nicht umgeschrieben.
- Der vollständige Aufbau einer leeren PostgreSQL-Datenbank bleibt möglich.
- Ein Upgrade mit mehreren bestehenden Challenges muss Nummern und Counter korrekt backfillen.
- Challenge-Card-Bytes sind Bestandteil normaler Datenbankbackups und Restores.
- Preview-, Acceptance- und Production-Isolation bleibt unverändert.
- Weder Migration noch normale Archivreads lösen Discord- oder OpenAI-Aufrufe aus.
- Das Einführen der Card-Persistenz erfordert keinen öffentlich erreichbaren Dateipfad und keine neue Caddy-Route.

## 13. Verbindliche Testabdeckung

### 13.1 Phase 13A / Core

Mindestens:

1. deterministischer Backfill nach `shown_at`, `challenge.id`,
2. leere Datenbank beginnt bei Challenge `#1`,
3. parallele Challenge-Materialisierungen erhalten eindeutige aufeinanderfolgende Nummern,
4. ein Transaktionsrollback verbraucht keine öffentliche Nummer,
5. offene Sessions und unbestätigte Offers verändern die letzte Challenge nicht,
6. Detail- und Listenprojektionen verwenden ausschließlich historische Snapshots,
7. `OPEN` und fehlende Restriction werden korrekt projiziert,
8. öffentliche Projektionen enthalten keinerlei Auswahl-/Providerhistorie,
9. Pagination, Gesamtzahl und stabile Sortierung,
10. Card-Insert, ausdrücklicher Replace, Ablehnung stillen Überschreibens und Remove,
11. tatsächliche PNG-Signatur, Decodierbarkeit, `1200 × 1200` und 5-MiB-Grenze,
12. SHA-256, Byteanzahl, Dateiname und Timestamps bleiben konsistent,
13. Card-Blob wird für Listenqueries nicht geladen,
14. Migration, Konkurrenz und Persistenz gegen echtes PostgreSQL über Testcontainers,
15. Modulgrenzen und bestehende Challenge-Regressionen,
16. `./mvnw clean verify`.

### 13.2 Phase 13B / Discord

Mindestens:

1. alle fünf Subcommands und ihre Optionspflichten,
2. guild-weite Reads ohne Teilnehmermapping,
3. DMs und fremde Guilds werden vor Core-Zugriff abgewiesen,
4. text-only und Card-Detaildarstellung,
5. Listenpagination mit Markierung der letzten Challenge und Card-Indikator ohne Blob-Read,
6. keinerlei interne IDs oder Auswahlhistorie im Renderer,
7. Card-Schreibbefehle ausschließlich mit Operatorrolle,
8. Operator muss kein Teilnehmer sein,
9. unautorisierte Aufrufe laden kein Attachment,
10. Defaultziel letzte Challenge und explizites historisches Ziel,
11. Replace- und Remove-Semantik,
12. nativer JDA-Upload mit stabilem Attachmentnamen,
13. sichere Abbildung typisierter Core- und Uploadfehler,
14. Allowed Mentions und Discord-Längengrenzen,
15. bestehende `/challenge`-, Voting- und `/zutat`-Regressionen,
16. keine echten Discord- oder OpenAI-Aufrufe,
17. `./mvnw clean verify`.

## 14. Paketfolge

### Phase 13A – Issue #140

Transportneutraler Core:

- öffentliche Challenge-Nummer,
- deterministischer Backfill und transaktionale Nummernvergabe,
- öffentliche Archivprojektion,
- Card-Persistenz und Card-APIs,
- PostgreSQL- und Modulgrenzentests.

Keine Discord-Bedienung.

### Phase 13B – Issue #141

Dünner Discord-Adapter:

- `/challenges`-Subcommands,
- guild-weite öffentliche Reads,
- operatorgebundene Card-Verwaltung,
- Attachment-Download und -Auslieferung,
- Discord-Renderer und Adaptertests.

Phase 13 beginnt erst nach Abschluss des privaten Produktionspiloten aus #90 und dem Release `v0.1.0`. Innerhalb von Phase 13 ist #141 durch #140 blockiert.

## 15. Nicht-Ziele

Ausdrücklich nicht Bestandteil dieses Stands sind:

- persönliche Konkretisierungen allgemeiner Vorgaben,
- drei persönliche Zusatz-Zutaten,
- privates Festlegen oder gemeinsames Reveal,
- Kochpläne, Einkaufslisten oder Fortschrittsstatus,
- Ergebnisfotos, Gerichtstitel, Fazit oder Bewertung,
- Markieren als gekocht, abgeschlossen, verschoben oder abgebrochen,
- automatische Erkennung normal im Channel geposteter Bilder,
- Message-Kontextcommand zur Card-Zuordnung,
- automatische Card-Erzeugung im Bot,
- OCR oder semantische Prüfung des Card-Inhalts,
- Card-Versionierung oder Card-Audit-Historie,
- interaktive Listenpagination, Autocomplete oder freie Suche,
- öffentliche Webgalerie oder Instagram-Automation,
- generischer Medienservice, Object Storage oder CDN,
- Offenlegung von Abstimmung, Reroll oder Kuratorentscheidung.

## 16. Leitprinzip

**Die bestätigte Challenge ist der Fakt. Die Challenge-Card ist ihre hübschere Visitenkarte — und darf sich entsprechend wichtig fühlen, aber nicht die Datenbank überschreiben.**
