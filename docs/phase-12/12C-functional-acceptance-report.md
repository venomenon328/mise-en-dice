# Phase 12C – Funktionale Live-Abnahme

Stand: 18. August 2026

Issue: #88  
Umbrella: #85  
Evidenzbranch: `test/88-live-functional-acceptance`  
Getesteter Commit: **aus finaler Acceptance-Status-/DB-Evidenz noch zu übernehmen**

## Ergebnis

**FUNCTIONAL PASS – persistente Detail-/Provider-Evidenz noch zu ergänzen**

Die manuelle Live-Matrix in Discord wurde funktional vollständig erfolgreich durchgeführt. 1, 2 und 3 Angebote,
Tie-Break, Vote-Änderung, REROLL, Runde 2, Ein-Offer-Auto-Confirm sowie die sichtbare Restriction-Darstellung
funktionierten im beobachteten End-to-End-Flow. Es wurde kein P0/P1-Befund gemeldet.

Die noch offenen Punkte dieses Reports sind keine erneuten Live-Sessions, sondern die redigierte Zuordnung der
persistierten Session-/Attempt-/Request-/History-Evidenz. Die bereitgestellten Discord-Screenshots und der
OpenAI-Dashboard-Snapshot werden nicht zu nicht belegten DB-IDs oder Per-Session-Tokenwerten hochgerechnet.

## Preflight und Baseline

Der Live-Lauf erfolgte auf der für 12C vorbereiteten Acceptance-Instanz. Folgende technische Baselinepunkte werden
vor Abschluss des Reports noch mit der Operator-/DB-Evidenz abgeglichen:

- [ ] getesteten Acceptance-Commit aus `acceptance status` übernehmen
- [ ] Post-Migration-/Pre-Request-Baseline dokumentieren
- [ ] Production/Previews unverändert bestätigen
- [ ] finalen secretfreien Logcheck dokumentieren

## Session- und Restriction-Matrix

| Flow | Command / Modus | Beobachtetes Ergebnis | Funktional |
| --- | --- | --- | --- |
| 12C-01/02 | `/challenge` / AUTO Default | 1 Offer; Gleichstand ACCEPT/REROLL; Losentscheid gewann ACCEPT; bestätigte Challenge entsprach Vorschlag 1; Restriktion `Keine` | PASS |
| 12C-03 | 2 Offers / NONE | 2 Offers mit `Einschränkung: Keine`; Vote-Änderung; gemeinsamer Gewinner Vorschlag 2; bestätigte Challenge entsprach Vorschlag 2 | PASS |
| 12C-04 | 3 Offers / AUTO | 3 Offers; Stimmen auf Vorschlag 1 und 3; Losentscheid gewann Vorschlag 3; ungewählter Vorschlag 2 gewann nicht | PASS |
| 12C-05 | Multi-Offer / REQUIRED | gemeinsamer REROLL; neue Offers mit echten Restriktionen; Runde 2 bot nur die neuen Vorschläge und keinen zweiten REROLL | PASS |
| 12C-06 | 1 Offer nach REROLL | neues einzelnes Offer wurde ohne künstliche zweite Abstimmung automatisch bestätigt | PASS |

Die sichtbaren Beispiele bestätigen außerdem, dass uneingeschränkte Kandidaten explizit als `Einschränkung: Keine`
gerendert werden und dass in REQUIRED-/REROLL-Flows echte kandidatenspezifische Restriktionen erscheinen können,
u. a. `keine Kokosmilch` und `kein Fisch und keine Meeresfrüchte`.

## Gemeinsame funktionale Beobachtungen

- [x] exakt 1..3 voneinander getrennte Offers mit je vier Requirement-Snapshots sichtbar
- [x] jedes sichtbare Offer besitzt genau eine Restriktionszeile
- [x] offene Abstimmung verrät die konkrete Wahl anderer Teilnehmer nicht
- [x] Vote-Änderung funktioniert und nur die letzte Stimme zählt
- [x] Tie-Break entscheidet nur zwischen tatsächlich gleichauf liegenden Optionen
- [x] bestätigte Challenge wird aus dem Gewinneroffer dargestellt
- [x] Runde 2 nach Multi-Offer-REROLL besitzt keinen zweiten REROLL
- [x] ein einzelnes REROLL-Offer wird ohne zweite Abstimmung bestätigt
- [x] keine Providerdiagnostik oder SQL-Details wurden in den gezeigten Discord-Nachrichten sichtbar

## UX-Befunde – P2 / Folgeissue #100

Die funktionale Abnahme hat drei nicht blockierende, aber vor dem Produktionspilot sinnvolle UX-Schärfungen ergeben:

1. **Ergebnisdarstellung:** `Abstimmung abgeschlossen` trennt Gewinner, Tie-Break und Einzelstimmen visuell zu wenig.
   Die Formulierung `Der einmalige Losentscheid wurde verwendet` wirkt sperrig. Gewünschte Richtung: Gewinner deutlich
   hervorheben, Einzelstimmen als eigenen Block darstellen und den Tie-Break knapp beim Gewinner notieren, z. B.
   `Gewinner: Vorschlag 3 (Gleichstand – per Los entschieden)`.
2. **Teilnehmernamen:** Sichtbar sollen aktuelle Discord-/Guild-Displaynamen erscheinen. Die stabile fachliche Identität
   bleibt ID-basiert; mutable Discord-Namen sollen nicht als autoritativer DB-Zustand gespeichert werden.
3. **REROLL-Zwischenfeedback:** Beim entscheidenden zweiten `Neu würfeln`-Vote blieb die öffentliche Nachricht während
   der längeren Generation/Kuration zunächst unverändert; der klickende Teilnehmer konnte noch als `noch offen`
   erscheinen. Nach Ende der Angebotsermittlung war der Zustand korrekt. Gewünscht ist ein sofortiger persistierter
   Zwischenstand mit abgeschlossener Abstimmung und Hinweis wie `Neue Angebote werden vorbereitet …`.

Diese Punkte sind als P2 in #100 erfasst und ändern das funktionale PASS von 12C nicht.

## Semantikbeobachtung: `keine Nudeln` + `Dumpling-Hülle`

Im Live-Lauf fiel die Kombination einer Restriktion `keine Nudeln` mit einer Requirement-Vorgabe
`Dumpling-Hülle` auf.

Nach Prüfung der aktuellen Generator- und Katalogsemantik ist dies **kein nachgewiesener Generatorfehler**:

- `NO_NOODLES` zielt auf das Katalogkonzept `NOODLES` einschließlich dessen bekannten Konkretisierungen.
- `DUMPLING_WRAPPERS` ist im aktuellen Refinement-Graph kein Kind von `NOODLES`, sondern ein eigener Stärke-Zweig;
  darunter liegen u. a. Dumpling-Teig, Reispapier und Wonton-Hüllen.
- Der Generator blockiert bekannte Restriktionskonflikte anhand der expandierten Zielcodes. Er soll keine zusätzlichen
  semantischen Freitextkonflikte erfinden, die im Katalog nicht modelliert sind.

Damit ist die Kombination gemäß aktuellem Datenmodell zulässig und auch sprachlich vertretbar: eine Dumpling-Hülle
ist nicht automatisch eine Nudel. Falls die Produktabsicht eigentlich `keine Nudeln oder vergleichbare Teigwaren/-hüllen`
lauten soll, muss die kuratierte Ausschlussregel beziehungsweise deren Zielmenge bewusst verbreitert werden; das wäre
eine Katalog-/Regelentscheidung und keine Reparatur der Generator-Konfliktprüfung.

## Provider-Snapshot

Der bereitgestellte OpenAI-Dashboard-Screenshot nach der Live-Matrix zeigt für das ausgewählte Acceptance-Projekt/
den dargestellten Zeitraum kumulativ:

- **7 Requests**
- **159.082 Tokens**
- **0,52 USD Spend**

Diese Werte werden vor dem Abschluss von 12C noch gegen die persistierten `curation_round`-/Usage-Snapshots
abgeglichen. Aus dem Dashboard allein wird keine Per-Session-Verteilung behauptet.

## Noch ausstehende Persistenz-/History-Evidenz

Ohne neue kostenpflichtige Session per read-only Operatorqueries nachweisen und in diesen Report übernehmen:

- [ ] Session-/Attempt-/Offer-Set-/Voting-/Challenge-IDs der fünf Live-Flows
- [ ] je Attempt 1 oder höchstens 2 tatsächliche Providerrequests; keine dritte Anfrage
- [ ] persistierte Tokenusage plausibel zum Dashboard-Snapshot
- [ ] genau eine Challenge pro bestätigter Session
- [ ] bestätigtes Offer stimmt mit Challenge-Requirement- und Restriction-Snapshots überein
- [ ] nicht gewählte normale Offers beeinflussen normale Requirement-/Restriction-History nicht
- [ ] Multi-Offer-REROLL erzeugt genau eine Cooldown-only-Exposition einschließlich Restriction-Snapshots
- [ ] REROLL-Session behält den INITIAL-Restriction-Mode
- [ ] Tie-Break genau einmal persistiert
- [ ] Runde 2 besitzt keine REROLL-Option
- [ ] Electorate/Participation korrekt und keine verwaisten offenen Runden

## Gesamt-Gate

- [x] 1, 2 und 3 Angebote live geprüft
- [x] AUTO, NONE und REQUIRED funktional in der Matrix geprüft
- [x] Vote-Geheimhaltung und Vote-Änderung funktionieren
- [x] Tie-Break funktional korrekt
- [x] REROLL und Runde 2 funktional korrekt
- [x] Ein-Offer-REROLL ohne Fake-Vote auto-bestätigt
- [x] sichtbare Requirement-/Restriction-Snapshotdarstellung funktioniert
- [ ] persistente Request-/History-/Snapshot-Invarianten abschließend per DB-Evidenz dokumentiert
- [x] kein gemeldeter P0/P1

## Folge-Issues

- #100 – P2: Discord-Ergebnisformatierung, aktuelle Discord-Namen und sofortiges REROLL-Zwischenfeedback.
