# Phase 12E – Privater Produktionspilot

Stand: offen

Issue: #90  
Umbrella: #85  
Evidenzbranch: `test/90-private-production-pilot`

## Ergebnis

**OFFEN – Pilot noch nicht gestartet**

Finale Entscheidung: `GO` | `GO WITH FOLLOW-UPS` | `NO-GO`

## Getesteter Stand und Zeitraum

- Ausgangs-`main`: `3a1f49eaa7c7435ed33ebe724b33db1c246e3882`
- produktiv getesteter Commit: offen
- Pilotzeitraum: offen
- Production-Instanz: `med-production`
- Modell / Reasoning: `gpt-5.6-terra` / `medium`

## Vorbereitendes Sicherheitsgate nach #115

Vor dem ersten produktiven Pilot-Command wird die Rollen- und Ownership-Grenze auf der isolierten Acceptance-Instanz praktisch bestätigt.

| Fall | Erwartung | Evidenz | Status |
| --- | --- | --- | --- |
| `/challenge` mit Operator-Rolle | Start möglich | offen | OFFEN |
| `/challenge` ohne Operator-Rolle, aber gemappter Participant | ephemere Ablehnung; keine Session, kein Attempt, keine Curation-Round, kein OpenAI-Request | offen | OFFEN |
| bestehende Challenge-Interaktion ohne Operator-Rolle | Voting/Reroll weiter gemäß Electorate möglich | offen | OFFEN |
| `/zutat` als nicht gemapptes Guild-Mitglied | Lookup und eigene Navigation möglich | offen | OFFEN |
| fremde `/zutat`-Card bedienen | ephemere Ablehnung; keine Card-Änderung und keine neue Lookup-Arbeit | offen | OFFEN |
| falsche Guild / DM | Ablehnung | offen | OFFEN |

**Gate:** Phase 12E beginnt erst, wenn die sicherheitsrelevanten Negativfälle bestanden sind und kein P0/P1 offen ist.

## Produktionsstart

- [ ] separate Produktions-Discord-Anwendung bzw. produktiver Bot eingerichtet
- [ ] separate Challenge-Operator-Rolle eingerichtet und nur beabsichtigten Operatoren zugewiesen
- [ ] separates OpenAI-Produktionsprojekt und Restricted Key eingerichtet
- [ ] Produktionssecrets getrennt von Acceptance angelegt und Dateirechte geprüft
- [ ] `doctor` erfolgreich
- [ ] Produktionsbackup unmittelbar vor Aktivierung erzeugt
- [ ] `production deploy main` erfolgreich
- [ ] App, PostgreSQL, HTTPS/Admin und Bot gesund
- [ ] negativer produktiver `/challenge`-Test erzeugt weder Session noch Providerrequest

## Pilot-Challenges

Mindestens drei vollständige echte Challenges über mindestens sieben Kalendertage. Die Matrix soll mindestens einmal 1, 2 und 3 gewünschte Angebote enthalten. Ein Reroll wird nur verwendet, wenn er in der echten Nutzung tatsächlich gewählt wird.

| Nr. | UTC / Datum | Commit | Session | Angebote angefordert / geliefert | Requests | Input | Output | Reasoning | Gesamt | Kosten | Latenz bis Status / Offers | Challenge | Ergebnis |
| ---: | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- |
| 1 | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen |
| 2 | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen |
| 3 | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen | offen |

## Produktqualität

Je Challenge bewerten Georgia und Tobias unabhängig und anschließend gemeinsam mit `gut`, `akzeptabel` oder `problematisch`:

- grundsätzliche Lösbarkeit
- kreative Offenheit
- interessante Spannung
- Beschaffbarkeit
- Ausgewogenheit der Außergewöhnlichkeit
- Verschiedenheit mehrerer Offers
- Verständlichkeit der Discord-Darstellung
- Vertrauen in Voting und Ergebnis

Wiederkehrende Muster werden hier aggregiert; Einzelanekdoten werden nicht überinterpretiert.

## Betriebsbeobachtung

- OpenAI-Projektverbrauch und Requestbudget
- ungewöhnliche Logwarnungen
- Container-Restarts und Healthstatus
- Backupstatus
- Secret-/ID-Leaks
- unerwartete Sessions oder Providerrequests
- unerwartete `/challenge`-Ausführung ohne Operator-Rolle

## Befunde

### P0 / P1

Keine / offen.

### P2

Keine / offen.

### P3

Keine / offen.

## Abschlussprüfung

- [ ] mindestens drei vollständige Pilot-Challenges abgeschlossen
- [ ] mindestens sieben Kalendertage Pilotzeitraum
- [ ] 1-, 2- und 3-Offer-Flow real genutzt
- [ ] kein P0/P1 offen
- [ ] Requestbudget in jedem Attempt eingehalten
- [ ] Kosten und Latenzen für privaten Betrieb akzeptabel
- [ ] Challengequalität überwiegend `gut` oder `akzeptabel`
- [ ] `/challenge` ausschließlich für beabsichtigte Operatoren nutzbar
- [ ] `/zutat` guildweit nutzbar und fremde Cards nicht manipulierbar
- [ ] Backup und Restart weiterhin belastbar
- [ ] Produktions-/Acceptance-Secrets getrennt und nicht exponiert
- [ ] finaler Produktionsbackup vorhanden

## Entscheidung

**OFFEN**

Begründung folgt nach Abschluss des Pilotzeitraums.

## Abschlussaktionen bei GO

- [ ] Acceptance stoppen, sofern nicht für Folgefehler benötigt
- [ ] Acceptance-Key/Token bei Stilllegung rotieren oder deaktivieren
- [ ] finalen Bericht mergen
- [ ] Release-Tag `v0.1.0` auf dem abgenommenen Commit anlegen
- [ ] Umbrella #85 schließen
