# Teilnehmeridentität, Elektorat und Beschaffbarkeit

Stand: 22. August 2026  
Status: verbindliche Fach- und Architekturspezifikation; Umsetzung in getrennten Entwicklungspaketen

Dieses Dokument spezifiziert die Verwaltung bekannter Personen, das veränderbare Standard-Elektorat für zukünftige Challenge-Auswahlen, den unveränderlichen Session-Snapshot sowie die Wirkung optional gepflegter Beschaffbarkeiten.

Es baut auf [`CHALLENGE_VOTING_AND_PARTICIPATION.md`](CHALLENGE_VOTING_AND_PARTICIPATION.md), [`CANDIDATE_GENERATOR.md`](CANDIDATE_GENERATOR.md), [`DATA_MODEL.md`](DATA_MODEL.md) und [`ARCHITECTURE.md`](ARCHITECTURE.md) auf. Bei Widersprüchen ersetzt dieses Dokument für zukünftige Pakete insbesondere:

- das fest codierte Default-Elektorat aus `GEORGIA` und `TOBIAS`,
- die Ableitung generatorrelevanter Personen aus allen aktiven `participant`-Zeilen,
- die Pflicht vollständiger Beschaffbarkeitswerte für jede generatorrelevante Person,
- sowie die aktive fachliche Nutzung von `challenge_participation`.

## 1. Begriffe und zentrale Trennung

### 1.1 Teilnehmeridentität

Ein `participant` ist eine stabile interne Personenidentität. Der Datensatz sagt nicht aus, dass die Person:

- an der aktuell laufenden Challenge teilnimmt,
- bei irgendeiner Challenge noch ein Ergebnis schuldet,
- automatisch stimmberechtigt ist,
- oder mit ihren Beschaffbarkeiten den Generator beeinflusst.

Der Begriff dient ausschließlich dazu, Votes, Ergebnisse, optionale Beschaffbarkeiten und externe Identitäten dauerhaft derselben Person zuordnen zu können.

### 1.2 Standard-Elektorat

Das Standard-Elektorat ist die veränderbare Menge von Personen, die bei einem künftig gestarteten Challenge-Auswahlprozess standardmäßig stimmberechtigt sein sollen.

Es ist keine Challenge-Teilnehmerliste. Seine Bedeutung entsteht dadurch, dass es beim Start einer Session in einen unveränderlichen Snapshot kopiert wird.

### 1.3 Session-Elektorat

Das Session-Elektorat ist der feste Snapshot für genau eine `challenge_session`.

Es bestimmt für diese Session:

- wer abstimmen darf und muss,
- wessen ausdrücklich gepflegte Beschaffbarkeitswerte bei der Generierung berücksichtigt werden,
- und welche Personen bei einem freiwilligen Reroll unverändert maßgeblich bleiben.

Spätere Änderungen an Teilnehmern oder Standard-Elektorat verändern diesen Snapshot nicht.

### 1.4 Challenge-Teilnahme

Eine vorab erklärte oder vermutete Teilnahme an einer konkreten Challenge wird nicht fachlich modelliert.

Für den nächsten Produktstand gilt:

> Wer für eine Challenge ein Ergebnis besitzt, hat an ihr teilgenommen.

Interesse, Planung oder eine formlose Teilnahmeankündigung bleiben normale Kommunikation im Discord-Channel und erzeugen keine persistente Abgabepflicht.

## 2. Persistente Teilnehmeridentität

Das bestehende `participant` bleibt die einzige Personenentität.

Mindestens vorhanden bleiben:

```text
participant
- id
- code
- display_name
- active
- created_at
```

Verbindlich gilt:

- `id` ist die interne relationale Identität.
- `code` ist stabil, eindeutig und nach Anlage unveränderlich.
- Neue Codes werden serverseitig kollisionssicher erzeugt; ein Discord-Administrator muss keinen technischen Code eingeben.
- `display_name` ist ein gespeicherter Fallback, niemals externe Identität.
- `active` ist ausschließlich ein administrativer Nutzbarkeitsstatus. Fachlich gibt es keine Menge „aktiver Challenge-Teilnehmer“.
- Deaktivierung löscht keine Votes, Ergebnisse, externen Identitäten oder Beschaffbarkeiten.
- Historische Ergebnisse dürfen auch für deaktivierte Personen gelesen und administrativ korrigiert werden.

Ein neuer Teilnehmer wird standardmäßig aktiv angelegt, aber niemals automatisch ins Standard-Elektorat aufgenommen.

## 3. Externe Identitäten und Discord-Autorität

Die vorhandene generische Zuordnung bleibt maßgeblich:

```text
participant_external_identity
- participant_id
- provider
- external_subject
```

Für Discord gilt:

- `provider = discord`,
- `external_subject` ist die unveränderte Discord-User-ID als opaker String,
- `(provider, external_subject)` gehört höchstens zu einem Teilnehmer,
- Anzeigename, Nickname oder Nachrichtenautor sind keine Identität,
- eine vorhandene Zuordnung wird niemals stillschweigend auf einen anderen Teilnehmer umgehängt.

Nach Einführung der Verwaltung ist die laufende PostgreSQL-Datenbank die einzige Runtime-Autorität für Discord-ID → Teilnehmer.

### 3.1 Legacy-Bootstrap für Georgia und Tobias

Die bisherige Properties-Map für `GEORGIA` und `TOBIAS` darf ausschließlich als kompatibler Bootstrap dienen:

- Fehlt die jeweilige Discord-Zuordnung in der Datenbank, darf sie aus der vorhandenen Konfiguration angelegt werden.
- Existiert dieselbe Zuordnung bereits passend, ist der Vorgang idempotent.
- Widerspricht die Konfiguration einer vorhandenen DB-Zuordnung, scheitert der Start mit klarer Diagnose; es wird nichts überschrieben.
- Nach erfolgreichem Bootstrap ist die Map für Runtime-Auflösung, Voting und Namensdarstellung nicht mehr autoritativ.
- Neue Teilnehmer erfordern keine Ergänzung von Server-Properties.
- Die bisherige Startpflicht, immer genau Discord-IDs für Georgia und Tobias zu konfigurieren, entfällt nach erfolgreicher DB-Übernahme.

## 4. Automatische Anlage durch ein Ergebnis

Eine Person kann erstmals dadurch relevant werden, dass ein Administrator ihr ein Challenge-Ergebnis zuordnet.

Dafür stellt der transportneutrale Kern einen idempotenten Resolve-or-Create-Use-Case bereit:

```text
resolveOrCreateParticipant(provider, externalSubject, displayNameFallback)
```

Verbindlich:

- Existiert die externe Identität bereits, wird derselbe Teilnehmer verwendet.
- Existiert sie nicht, werden Teilnehmer und externe Identität atomar angelegt.
- Die neue Person ist aktiv, aber nicht Mitglied des Standard-Elektorats.
- Es werden keine `ingredient_availability`-Zeilen erzeugt.
- Eine bereits bekannte deaktivierte Person wird verwendet, aber nicht automatisch reaktiviert.
- Parallele Anlageversuche für dieselbe externe Identität erzeugen genau einen Teilnehmer.

Damit ist keine vorherige förmliche Registrierung Voraussetzung für die Ergebniserfassung.

## 5. Verwaltung des Standard-Elektorats

Das fest codierte Default-Elektorat wird durch eine persistente Relation ersetzt, fachlich beispielsweise:

```text
default_electorate_member
- participant_id
- added_at
```

Verbindlich:

- Ein Teilnehmer ist höchstens einmal Mitglied.
- Nur aktive Teilnehmer dürfen neu aufgenommen werden.
- Aufnahme und Entfernung wirken ausschließlich auf später gestartete Sessions.
- Reaktivierung stellt eine frühere Elektoratsmitgliedschaft nicht automatisch wieder her.
- Deaktivierung entfernt die Person atomar aus dem Standard-Elektorat.
- Ein technisch leeres Standard-Elektorat ist als Wartungszustand zulässig; eine neue Challenge-Anforderung wird dann verständlich abgewiesen.
- Bestehende Sessions bleiben unverändert, wenn das Standard-Elektorat zwischenzeitlich leer oder anders zusammengesetzt wird.

Georgia und Tobias werden bei der Migration in diese Relation übernommen.

## 6. Zeitpunkt und Autorität des Session-Snapshots

Der Session-Snapshot muss künftig **vor der Kandidatengenerierung** feststehen.

Ablauf für einen neuen INITIAL-Versuch:

1. `challenge_session` anlegen beziehungsweise den Auswahlprozess beginnen.
2. Das zu diesem Zeitpunkt gültige Standard-Elektorat in `selection_electorate` materialisieren.
3. Diesen Snapshot als unveränderliche Personenmenge für den vollständigen Generation Context verwenden.
4. Erst danach Catalog Snapshot, Generator und Kurator starten.
5. Bei tatsächlicher Präsentation und Voting denselben Snapshot verwenden.

Die bisherige Initialisierung des Electorates erst nach erfolgreicher Discord-Präsentation wird für neue Sessions ersetzt. Der Presentation-Handshake bleibt für die Sichtbarkeit des Offer Sets und die Öffnung der Voting-Runde maßgeblich, aber nicht mehr für die Wahl der Personenmenge.

Ein Reroll übernimmt denselben Snapshot ohne Neuberechnung.

Eine Deaktivierung nach Sessionstart:

- entfernt die Person nicht aus dem Snapshot,
- verändert die bereits im Generation Context gespeicherten Beschaffbarkeitswerte nicht,
- und darf die offene Abstimmung nicht unabschließbar machen.

## 7. Optionale Beschaffbarkeitsdaten

`ingredient_availability` bleibt eine optionale Relation zwischen Zutatenkonzept und Teilnehmer.

Ein fehlender Datensatz bedeutet ausschließlich:

> Für diese Person ist zu dieser Zutat keine Beschaffbarkeit gepflegt.

Er bedeutet weder `EASY` noch `UNAVAILABLE`, erzeugt keinen Ersatzwert und hat für sich allein keinerlei Einfluss auf Eignung, Gewicht, Score oder Datenkonfidenz des Generators.

### 7.1 Maßgebliche Personen

Für eine konkrete Generation werden ausschließlich die Beschaffbarkeitswerte der Personen im festen Session-Elektorat betrachtet.

Daraus folgt:

- Werte von Personen außerhalb des Session-Electorates werden ignoriert.
- Eine Person, die nur irgendwann ein Ergebnis erhalten hat, beeinflusst den Generator nicht automatisch.
- Eine spätere Änderung des Standard-Electorates beeinflusst eine laufende Session nicht.
- Ein Teilnehmer kann Beschaffbarkeitswerte besitzen, ohne im aktuellen oder zukünftigen Elektorat zu sein.

### 7.2 Restriktivster ausdrücklich gepflegter Wert

Pro Zutatenkonzept werden alle für das Session-Elektorat **vorhandenen** Werte betrachtet. Maßgeblich ist der restriktivste davon:

```text
UNAVAILABLE > DIFFICULT > SPECIALTY > PLANNED > EASY
```

Es wird niemals gemittelt. Eine für eine einzige maßgebliche Person unbeschaffbare Zutat wird nicht dadurch hilfreich, dass andere sie bequem kaufen könnten.

Fehlt für alle Mitglieder des Session-Electorates ein Wert, bleibt der Beschaffbarkeitsbeitrag neutral. Dies ist keine fachliche Umdeutung zu `EASY`, sondern schlicht das Fehlen eines Signals.

Beispiele:

- Georgia `EASY`, Tobias `UNAVAILABLE` → Konzept ist nicht zufällig ziehbar.
- Georgia `EASY`, Tobias `DIFFICULT` → der `DIFFICULT`-Faktor gilt.
- Georgia `PLANNED`, Tobias `SPECIALTY` → der `SPECIALTY`-Faktor gilt.
- Georgia ohne Wert, Tobias `PLANNED` → der `PLANNED`-Faktor gilt.
- Beide ohne Wert → Beschaffbarkeit verändert die Ziehung nicht.
- Eine dritte Person außerhalb des Session-Electorates mit `UNAVAILABLE` → ohne Wirkung auf diese Session.

### 7.3 Zielwerte der Beschaffbarkeitsfaktoren

Die bislang verwendeten Faktoren `PLANNED = 0,65` und `DIFFICULT = 0,20` gewichten erschwerte Beschaffbarkeit zu großzügig. Für den nächsten Konfigurationsstand gelten als verbindliches Ziel:

| restriktivster vorhandener Wert | Faktor |
|---|---:|
| `EASY` | 1,00 |
| `PLANNED` | 0,45 |
| `SPECIALTY` | 0,15 |
| `DIFFICULT` | 0,03 |
| `UNAVAILABLE` | 0,00 |
| kein einziger gepflegter Wert | neutraler Faktor 1,00 |

`DIFFICULT` bezeichnet eine zwar nicht logisch unmögliche, praktisch aber nur mit unverhältnismäßigem Reise-, Import- oder Kostenaufwand realisierbare Beschaffung. Es soll deshalb selten, aber nicht vollständig ausgeschlossen bleiben.

Im Übergangspaket gilt für `SPECIALTY` der Faktor `0,15`; die bestehenden Faktoren bleiben unverändert. Die spätere numerische Kalibrierung erfolgt bewusst in einem eigenen Paket und verändert nicht stillschweigend Neuigkeitsziele, Kandidaten-Caps oder andere Generatorparameter. Die fachliche Stufendefinition steht verbindlich in [`AVAILABILITY_AND_COOKING_NOVELTY.md`](AVAILABILITY_AND_COOKING_NOVELTY.md).

### 7.4 Katalogprojektion und Modulgrenze

Das Challenge-Modul bleibt Eigentümer des Session-Electorates. Das Katalogmodul bleibt Eigentümer der Zutaten- und Beschaffbarkeitsdaten.

Die öffentliche Generatorprojektion des Katalogmoduls erhält die feste Personenmenge als expliziten Request, beispielsweise über stabile Teilnehmer-IDs und -Codes. Sie leitet diese Menge nicht mehr selbst aus `participant.active` ab.

Der Catalog Snapshot enthält:

- die angeforderte feste Personenmenge,
- pro Konzept nur die tatsächlich vorhandenen Beschaffbarkeitswerte dieser Personen,
- keine erfundenen Lückenfüllwerte.

Replay und Audit verwenden denselben gespeicherten Snapshot; spätere Änderungen an Elektorat oder Beschaffbarkeit verändern alte Attempts nicht.

## 8. Bestehende Webverwaltung

Dieses Vorhaben führt keine allgemeine Teilnehmer- oder Beschaffbarkeitsmatrix in der Weboberfläche ein.

Für den ersten Stand gilt:

- Die bestehenden Felder und Filter für Georgia und Tobias dürfen unverändert sichtbar bleiben.
- Es müssen keine dynamischen zusätzlichen Spalten oder Eingabereihen für weitere Personen gebaut werden.
- Fehlende Beschaffbarkeit darf nicht mehr als harte Speicher- oder Generatorvoraussetzung behandelt werden.
- Die Weboberfläche darf fehlende Werte weiterhin als redaktionellen Hinweis beziehungsweise `nicht gepflegt` anzeigen.
- Die Pflege von Beschaffbarkeiten weiterer Teilnehmer bleibt späteren Paketen vorbehalten.

## 9. Discord-Admin-Commands

Alle folgenden Commands sind ausschließlich für die bestehende Challenge-Operator-Rolle verfügbar:

```text
/teilnehmer anlegen person:<Discord-Nutzer> [name:<Fallback-Anzeigename>]
/teilnehmer aktivieren person:<Discord-Nutzer>
/teilnehmer deaktivieren person:<Discord-Nutzer>
/teilnehmer elektorat-hinzufügen person:<Discord-Nutzer>
/teilnehmer elektorat-entfernen person:<Discord-Nutzer>
/teilnehmer liste
```

Semantik:

- `anlegen` erzeugt Teilnehmer und Discord-Zuordnung atomar; eine bereits vorhandene Zuordnung wird idempotent angezeigt.
- `aktivieren` ändert nur den administrativen Status.
- `deaktivieren` entfernt zusätzlich die künftige Standard-Elektoratsmitgliedschaft, niemals einen laufenden Snapshot.
- `elektorat-hinzufügen` verlangt einen bekannten aktiven Teilnehmer.
- `elektorat-entfernen` verändert keine laufende Session.
- `liste` zeigt mindestens Fallback-Anzeigename, Aktivstatus und Mitgliedschaft im Standard-Elektorat.
- Alle Antworten dürfen ephemer sein.

Es gibt keinen Command zum Hinzufügen einer Person zu einer konkreten Challenge.

## 10. `challenge_participation` verliert seine Fachautorität

Die vorhandene Tabelle und die zugehörigen APIs bilden derzeit automatisch Mitglieder des früheren Electorates als vermeintliche Challenge-Teilnehmer ab. Diese Semantik wird nicht für Ergebnisse weiterverwendet.

Für zukünftige Pakete gilt:

- keine automatische Initialisierung neuer `challenge_participation`-Zeilen,
- kein `joinChallenge`-Command im Produktfluss,
- keine Anzeige erwarteter oder noch offener Challenge-Teilnehmer,
- keine Ergebnisreferenz auf `challenge_participation`,
- keine Abschlussbedingung aus dieser Tabelle.

Die vorhandene Tabelle darf zunächst als nicht autoritative Legacy-Struktur bestehen bleiben. Bestehende Zeilen besitzen keine neue fachliche Bedeutung. Eine spätere Entfernung ist zulässig.

## 11. Öffentliche Application-APIs

Die konkreten Java-Namen sind nicht zwingend. Der transportneutrale Vertrag benötigt mindestens:

```text
resolveOrCreateParticipant(provider, externalSubject, displayNameFallback)
findParticipantByExternalIdentity(provider, externalSubject)
listParticipants()
activateParticipant(participantId)
deactivateParticipant(participantId)
addDefaultElectorateMember(participantId)
removeDefaultElectorateMember(participantId)
listDefaultElectorate()
```

Zusätzlich muss der Challenge-Start denselben Electorate-Snapshot vor Generation materialisieren und an die öffentliche Katalogprojektion übergeben können.

Rollenprüfung, Discord-User-Selects und Guild-Namensauflösung verbleiben im Discord-Adapter.

## 12. Persistenz- und Konkurrenzinvarianten

PostgreSQL bleibt letzte Integritätssicherung. Mindestens abzusichern sind:

- externe Identität global eindeutig je Provider/Subject,
- höchstens eine externe Identität desselben Providers je Teilnehmer, solange kein späterer Fachentscheid dies erweitert,
- Standard-Elektoratsmitglied eindeutig,
- Session-Elektoratsmitglied eindeutig,
- Session-Snapshot nach Materialisierung unveränderlich,
- keine automatische Kopplung zwischen Ergebnisexistenz, Aktivstatus und Elektoratsmitgliedschaft,
- paralleles Resolve-or-Create derselben Discord-ID erzeugt genau einen Teilnehmer,
- Deaktivierung und Entfernung aus dem Standard-Elektorat sind atomar,
- Challenge-Start und Electorate-Snapshot können nicht mit einer halben Mitgliederänderung konkurrieren,
- unbekannte Datenbankfehler werden nicht als fachliche Konflikte maskiert.

Bereits veröffentlichte Changesets bleiben append-only.

## 13. Migration und Kompatibilität

Die Einführung muss mindestens:

- Georgia und Tobias in das persistente Standard-Elektorat übernehmen,
- vorhandene externe Discord-Zuordnungen erhalten beziehungsweise konfliktfrei bootstrappen,
- bestehende `selection_electorate`-Snapshots unverändert erhalten,
- bestehende Votes und Challenges unverändert lesbar halten,
- neue Sessions nach der neuen Vor-Generation-Snapshotregel starten,
- die Generatorprojektion von `participant.active` entkoppeln,
- und die aktive Participation-Initialisierung aus dem Confirm-/Resume-Pfad entfernen.

Ein Upgrade darf keine vorhandene Challenge oder öffentliche Challenge-Nummer neu materialisieren.

## 14. Tests

Mindestens erforderlich sind:

1. Anlage, idempotente Wiederholung und konkurrierende Anlage derselben Discord-ID,
2. Konflikt einer bereits anders verknüpften externen Identität,
3. Aktivierung und Deaktivierung mit Erhalt historischer Daten,
4. atomare Entfernung eines deaktivierten Teilnehmers aus dem Standard-Elektorat,
5. Änderungen des Standard-Electorates wirken nur auf neue Sessions,
6. Session-Snapshot wird vor Catalog Snapshot und Generation festgeschrieben,
7. Reroll übernimmt exakt denselben Snapshot,
8. nachträgliche Deaktivierung verändert offene Votes und Snapshot-Beschaffbarkeit nicht,
9. ausschließlich Werte des Session-Electorates werden ausgewertet,
10. restriktivster vorhandener Wert gewinnt ohne Mittelung,
11. vollständig fehlende Werte bleiben neutral und lösen keinen Missing-Fehler aus,
12. `UNAVAILABLE` einer einzigen maßgeblichen Person blockiert,
13. Werte außerhalb des Electorates bleiben wirkungslos,
14. Legacy-Bootstrap ist idempotent und widersprüchliche Konfiguration scheitert sichtbar,
15. keine neue `challenge_participation`-Zeile wird nach Challenge-Bestätigung erzeugt,
16. leeres Standard-Elektorat verhindert einen neuen Start mit typisierter Rückmeldung,
17. Modulgrenzen und `./mvnw clean verify` bleiben grün.

Persistenz-, Migrations-, Snapshot- und Konkurrenztests verwenden echtes PostgreSQL über Testcontainers.

## 15. Entwicklungspakete

Die Umsetzung wird in drei getrennte Pakete geschnitten:

1. **Transportneutraler Teilnehmer-/Elektoratskern**: Schema, Application-APIs, frühes Session-Elektorat, sparse Beschaffbarkeit und Stilllegung der Participation-Autorität.
2. **Discord-Administration und Legacy-Bootstrap**: `/teilnehmer`-Commands, DB-basierte Runtime-Auflösung und kontrollierte Übernahme der bisherigen Properties-Map.
3. **Beschaffbarkeitskalibrierung**: reproduzierbare Vorher-/Nachher-Messung, Faktoren `0,45` und `0,03`, Konfigurationsversionswechsel und gezielte Regressionstests.

Die Pakete bauen in dieser Reihenfolge aufeinander auf. Die numerische Kalibrierung wird nicht in das strukturelle Kernpaket gemischt.

## 16. Nicht-Ziele

- keine öffentliche Selbstregistrierung,
- keine Benutzerkonten außerhalb der vorhandenen Discord-Identität,
- keine dynamische Webverwaltung beliebig vieler Beschaffbarkeitsprofile,
- keine individuelle Electorate-Auswahl pro Session im Discord-Command,
- keine Challenge-Anmeldung oder erwartete Ergebnisliste,
- keine automatische Aufnahme neuer Teilnehmer ins Elektorat,
- keine Löschung historischer Teilnehmerdaten,
- keine stillen Änderungen weiterer Generatorparameter im Kalibrierungspaket,
- keine Umsetzung der explorativen Challenge-Modi aus [`CHALLENGE_MODE_IDEAS.md`](CHALLENGE_MODE_IDEAS.md).
