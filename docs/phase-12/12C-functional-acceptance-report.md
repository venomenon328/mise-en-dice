# Phase 12C – Funktionale Live-Abnahme

Stand: 18. August 2026

Issue: #88  
Umbrella: #85  
Evidenzbranch: `test/88-live-functional-acceptance`  
Getesteter Commit: `8e7565ef791d4a85cbb7a8aedbbba095d3582164`

## Ergebnis

**FUNCTIONAL PASS – persistente Detail-/Provider-Evidenz noch zu ergänzen**

Die manuelle Live-Matrix in Discord wurde funktional vollständig erfolgreich durchgeführt. 1, 2 und 3 Angebote,
Tie-Break, Vote-Änderung, REROLL, Runde 2, Ein-Offer-Auto-Confirm sowie die sichtbare Restriction-Darstellung
funktionierten im beobachteten End-to-End-Flow. Es wurde kein P0/P1-Befund gemeldet.

Die noch offenen Punkte dieses Reports sind keine erneuten Live-Sessions, sondern die redigierte Zuordnung der
persistierten Session-/Attempt-/Request-/History-Evidenz. Die bereitgestellten Discord-Screenshots und der
OpenAI-Dashboard-Snapshot werden nicht zu nicht belegten DB-IDs oder Per-Session-Tokenwerten hochgerechnet.

## Preflight und Baseline

Die Acceptance lief laut `acceptance status` auf `main`, Commit
`8e7565ef791d4a85cbb7a8aedbbba095d3582164`, Port `18090`, Status `running/healthy`, Discord und OpenAI aktiviert,
Modell `gpt-5.6-terra`, Reasoning `medium`.

Noch vor Abschluss des Reports ergänzen:

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

Die persistierte Session-/Attempt-Matrix bestätigt bisher:

- Session 1: AUTO, 1 Offer, INITIAL Attempt 1, Generator 1.2.0, `OFFER_READY`
- Session 2: NONE, 2 Offers, INITIAL Attempt 2, Generator 1.2.0, `OFFER_READY`
- Session 3: AUTO, 3 Offers, INITIAL Attempt 3, Generator 1.2.0, `OFFER_READY`
- Session 4: REQUIRED, 2 Offers, INITIAL Attempt 4 und REROLL Attempt 5, beide Generator 1.2.0, `OFFER_READY`
- Session 5: AUTO, 1 Offer, INITIAL Attempt 6 und REROLL Attempt 7, beide Generator 1.2.0, `OFFER_READY`

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

## Detail-Evidenz Session A / Session 1

Session A ist persistiert als Session `1`, INITIAL Attempt `1`, AUTO, ein angefordertes Offer, Generator `1.2.0`.
Der einzige Curation-Request war Runde `1` / `INITIAL_PASS` gegen `gpt-5.6-terra`, `CURATOR_PROMPT_V2`,
`CURATION_CONTRACT_V2`. Der Request wurde mit HTTP 200 abgeschlossen; Providerlaufzeit zwischen Claim und
persistiertem Ergebnis: **19,269 s**. Usage: **22.307 Input**, **1.492 Output**, **707 Reasoning**, **23.799 Gesamt-Tokens**.
Es gab genau einen Providerrequest für diesen Attempt.

Kuratorergebnis der zwölf Kandidaten:

| Kandidat | Generator-Score | Kurator | Rang | Kernaussage | Ergebnis |
| ---: | ---: | --- | ---: | --- | --- |
| 1 | 96,82 | GOOD | 2 | starke Kohärenz und kreative Offenheit, geringe Interaktionsgefahr | nicht gewählt |
| 2 | 90,35 | GOOD | 3 | starke Kohärenz und Offenheit, geringe Interaktionsgefahr | nicht gewählt |
| 3 | 94,52 | ACCEPTABLE | 4 | kreative Offenheit, aber schwächere Kohärenz und Lock-in-Risiko | nicht gewählt |
| 4 | 92,35 | GOOD | **1** | starke Kohärenz und kreative Offenheit, geringe Interaktionsgefahr, hoher Diversitätsbeitrag | **Offer 1 / Challenge 1** |
| 5 | 86,14 | ACCEPTABLE | 9 | schwächere Kohärenz, hohe Interaktions- und Offenheitsrisiken | nicht gewählt |
| 6 | 92,58 | ACCEPTABLE | 7 | kohärent, aber begrenzte Offenheit und Standardgericht-Risiko | nicht gewählt |
| 7 | 86,20 | BAD | 11 | schwache Kohärenz und hohe Interaktionsgefahr | nicht gewählt |
| 8 | 88,52 | ACCEPTABLE | 6 | schwächere Kohärenz und begrenzte kreative Offenheit | nicht gewählt |
| 9 | 84,84 | ACCEPTABLE | 10 | schwächere Kohärenz, hohe Interaktionsgefahr und Lock-in-Risiko | nicht gewählt |
| 10 | 88,68 | BAD | 12 | schwache Kohärenz sowie hohe Interaktions- und Offenheitsrisiken | nicht gewählt |
| 11 | 91,48 | ACCEPTABLE | 5 | starke Kohärenz und Offenheit, aber hohes Offenheitsrisiko durch breite Vorgaben | nicht gewählt |
| 12 | 85,34 | ACCEPTABLE | 8 | schwächere Kohärenz, hohe Interaktionsgefahr und Lock-in-Risiko | nicht gewählt |

Der Offer-Selection-Pfad ist `INITIAL_GOOD_SELECTION`. Kandidat 4 wurde als einziges sichtbares Offer materialisiert
und anschließend als Challenge 1 bestätigt. Seine persistierten Requirements waren `Schweineleber`,
`koreanische Reiskuchen`, `Sauerkirsche`, `Lauch- und Zwiebelgemüse`; Restriktion: `Keine`.

Bemerkenswert: Kandidat 1 hatte mit 96,82 den höchsten Generator-Score, der semantische Kurator setzte jedoch
Kandidat 4 auf Rang 1. Damit zeigt der Live-Lauf die beabsichtigte Arbeitsteilung: der Generator bewertet harte
Struktur-/Datenmerkmale, während der Kurator die kulinarische Semantik und kreative Nutzbarkeit separat ordnet.

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

Die persistierten Usage-Snapshots der sieben Attempts summieren sich ebenfalls zu sieben Requestslots; die genaue
Per-Attempt-Tokenevidenz wird sukzessive in diesem Report ergänzt. Session A ist bereits vollständig zugeordnet.

Die zwei REROLL-Expositionen besitzen die erwartete Kardinalität:

- Session 4: 2 exponierte Offers → 8 Requirement-Snapshots und 2 Restriction-Snapshots
- Session 5: 1 exponiertes Offer → 4 Requirement-Snapshots und 1 Restriction-Snapshot

## Noch ausstehende Persistenz-/History-Evidenz

Ohne neue kostenpflichtige Session per read-only Operatorqueries nachweisen und in diesen Report übernehmen:

- [x] Session-/Attempt-Zuordnung der fünf Live-Flows
- [x] Session A: genau ein Providerrequest und persistierte Tokenusage
- [x] REROLL-Expositions-Kardinalität einschließlich Restriction-Snapshots
- [ ] Sessions B–E: Kandidaten-/Curator-/Usage-Detailzuordnung
- [ ] genau eine Challenge pro bestätigter Session
- [ ] bestätigtes Offer stimmt mit Challenge-Requirement- und Restriction-Snapshots überein
- [ ] nicht gewählte normale Offers beeinflussen normale Requirement-/Restriction-History nicht
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
