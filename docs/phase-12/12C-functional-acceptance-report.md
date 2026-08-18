# Phase 12C – Funktionale Live-Abnahme

Stand: 18. August 2026

Issue: #88  
Umbrella: #85  
Evidenzbranch: `test/88-live-functional-acceptance`  
Getesteter Commit: **NOT RUN**

## Ergebnis

**NOT RUN**

Diese Datei ist das vorab angelegte Evidenzgerüst. Ergebnisse werden ausschließlich nach tatsächlich ausgeführten Live-Szenarien eingetragen. Keine Provider- oder Discord-Evidenz wird aus automatisierten Tests abgeleitet.

## Preflight und Baseline

- [ ] letzter Backup-Stand der bisherigen `med-acceptance` erzeugt
- [ ] alte pre-1.2 Challenge-/Generator-Testdaten durch kontrollierten `acceptance reset` entfernt
- [ ] aktueller `main` frisch auf `med-acceptance` deployt
- [ ] Migrationen einschließlich Generator-1.2-Cleanup erfolgreich
- [ ] Post-Migration-/Pre-Request-Baseline-Backup erzeugt
- [ ] Production/Previews unverändert
- [ ] OpenAI-Usage-Ausgangsstand dokumentiert
- [ ] Logs in zweiter Operatorsitzung verfolgt

## Geplante Session- und Restriction-Matrix

Die Pflichtmatrix bleibt bei höchstens sechs neuen Generation Sessions und höchstens zwölf tatsächlichen OpenAI-Requests. Die Restriction Modes werden ohne zusätzliche reine Restriction-Sessions integriert.

| Flow | Geplanter Command | Restriction Mode | Zweck | Status |
| --- | --- | --- | --- | --- |
| 12C-01/02 | `/challenge` | AUTO (Default) | Default 1 Offer, Tie-Break ACCEPT/REROLL | NOT RUN |
| 12C-03 | `/challenge angebote:2 einschraenkung:keine` | NONE | 2 Offers, geheime Vote-Änderung, explizit `Einschränkung: Keine` | NOT RUN |
| 12C-04 | `/challenge angebote:3 einschraenkung:automatisch` | AUTO | 3 Offers, Tie-Break nur zwischen gewählten Optionen | NOT RUN |
| 12C-05 | `/challenge angebote:2 einschraenkung:erzwingen` | REQUIRED | Multi-Offer-REROLL, Restriction-Exposition, Runde 2 | NOT RUN |
| 12C-06 | nur falls 12C-02 nicht bereits REROLL gewinnt | AUTO | Ein-Offer-REROLL und Auto-Confirm | NOT RUN / OPTIONAL |

12C-07 bis 12C-10 werden soweit möglich mit bereits erzeugten Sessions ausgeführt und erzeugen keine zusätzliche Session nur für Evidenz.

## Gemeinsame Prüfungen je Session

- [ ] Slash-Interaction wird rechtzeitig deferred
- [ ] exakt gewünschte 1..3 Offers, jeweils vier Requirement-Snapshots
- [ ] jedes Offer zeigt genau eine Restriktionszeile (`Einschränkung: …` oder `Einschränkung: Keine`)
- [ ] offene Votes verraten keine konkrete Wahl
- [ ] ephemere Rückmeldung pro Nutzerinteraktion
- [ ] Gewinner/individuelle Stimmen/Tie-Break nach Abschluss korrekt
- [ ] bestätigte Challenge übernimmt Requirement- **und Restriction-Snapshot** des Gewinneroffers exakt
- [ ] keine Providerdiagnostik, SQL-Details oder Secrets in Discord
- [ ] DB-Evidenz bestätigt Requestbudget und Zustandsübergänge

## 12C-01/02 – Default, AUTO und Ein-Offer-Tie-Break

Status: **NOT RUN**

UTC:
Session / Attempt / Offer-Set / Voting-Runde / Challenge:
Restriction-Snapshot:
OpenAI-Requests:
Input / Output / Reasoning / Gesamt-Tokens:
Kosten / Projektlimit:
Latenz:
Beobachtung:

Tie-Break-Ergebnis: `ACCEPT | REROLL | NOT RUN`

## 12C-03 – Zwei Offers, NONE und Vote-Änderung

Status: **NOT RUN**

UTC:
Session / Attempt / Offer-Set / Voting-Runde / Challenge:
OpenAI-Requests:
Latenz:
Beobachtung:

Zusätzliche Restriction-Gates:

- [ ] beide Offers zeigen `Einschränkung: Keine`
- [ ] bestätigte Challenge zeigt ebenfalls `Einschränkung: Keine`
- [ ] nicht gewähltes Offer erzeugt keine normale Restriction-History

## 12C-04 – Drei Offers und Tie-Break

Status: **NOT RUN**

UTC:
Session / Attempt / Offer-Set / Voting-Runde / Challenge:
OpenAI-Requests:
Restriction-Snapshots der drei Offers:
Latenz:
Beobachtung:

AUTO darf null, eine oder mehrere Restriktionen liefern; eine feste Live-Quote ist kein Gate.

## 12C-05 – REQUIRED, Multi-Offer-REROLL und Runde 2

Status: **NOT RUN**

UTC:
Session / INITIAL-Attempt / REROLL-Attempt:
Offer-Set Runde 1 / Runde 2:
OpenAI-Requests INITIAL / REROLL:
Latenz:
Beobachtung:

Zusätzliche Restriction-Gates:

- [ ] jedes Offer des ersten Sets besitzt einen Restriction-Snapshot
- [ ] Session behält `restriction_mode = REQUIRED`
- [ ] verworfenes sichtbares Set erzeugt genau eine REROLL-Exposition
- [ ] REROLL-Exposition enthält für jedes exponierte Offer dessen autoritativen Restriction-Snapshot
- [ ] Runde 2 rendert ausschließlich neue Offer-Snapshots
- [ ] keine aktuelle Katalogregel rekonstruiert historische Snapshots
- [ ] Runde 2 bietet keinen zweiten REROLL

## 12C-06 – Ein-Offer-REROLL und Auto-Confirm

Status: **NOT RUN / OPTIONAL**

Nur ausführen, wenn der Tie-Break aus 12C-02 den REROLL-Pfad nicht bereits vollständig abgedeckt hat.

## 12C-07 – Stale Buttons und Doppelklick

Status: **NOT RUN**

Verwendete Session(s):
Beobachtung:

- [ ] alter Vote-Button ändert keinen Zustand
- [ ] schneller Doppelklick erzeugt keine zweite Materialisierung
- [ ] alter Set-Button nach REROLL bleibt stale
- [ ] kein zusätzlicher Providerrequest

## 12C-08 – Requirement- und Restriction-Snapshot-Autorität

Status: **NOT RUN**

Bevorzugt in einem bereits laufenden REQUIRED-Flow kombinieren. Änderungen ausschließlich über die Acceptance-Adminoberfläche, nicht per SQL.

Vor Änderung sichtbarer Requirement-/Restriction-Snapshot:
Adminänderung:
Nach normalem Re-Render / Abschluss sichtbarer Snapshot:
Auditnachweis:

- [ ] bereits sichtbare Requirementtexte bleiben unverändert
- [ ] bereits sichtbare Restrictiontexte bleiben unverändert
- [ ] bestätigte Challenge verwendet die alten Snapshots
- [ ] gegebenenfalls REROLL-Exposition verwendet die alten exponierten Snapshots

## 12C-09 – Zugriff und Identität

Status: **NOT RUN**

Beobachtung:

- [ ] nicht konfigurierter Nutzer bzw. falscher Ort ändert keinen Zustand
- [ ] Identität bleibt Discord-User-ID-basiert
- [ ] Nicknameänderung ändert Identität nicht
- [ ] keine zusätzliche kostenpflichtige Session

## 12C-10 – Provider- und Requestevidenz

Status: **NOT RUN**

| Session | INITIAL Requests | REROLL Requests | Input | Output | Reasoning | Gesamt | Kosten |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |

Gesamte neue Generation Sessions: `0`  
Gesamte tatsächliche OpenAI-Requests: `0`  
Gesamtkosten: `NOT RUN`

- [ ] je Attempt höchstens zwei tatsächliche Requests
- [ ] keine automatische dritte Anfrage
- [ ] Modell/Reasoning/Prompt-/Contractversion wie konfiguriert
- [ ] Dashboardverbrauch plausibel zur persistenten Usage

## Persistenz- und History-Gate

- [ ] genau eine Challenge pro bestätigter Session
- [ ] bestätigtes Offer stimmt mit Challenge-Requirement- und Restriction-Snapshots überein
- [ ] nicht gewählte normale Offers beeinflussen normale Requirement-/Restriction-History nicht
- [ ] rerolltes vollständiges sichtbares Set erzeugt genau eine Cooldown-only-Exposition einschließlich Restrictions
- [ ] Tie-Break exakt einmal persistiert
- [ ] Runde 2 besitzt keine REROLL-Option
- [ ] Electorate und Participation korrekt
- [ ] keine verwaisten offenen Runden nach abgeschlossenem Flow

## Qualitative Beobachtung

Je sichtbarem Offer knapp notieren: plausibler Weg, Offenheit, Beschaffbarkeit, Trivialität/Absurdität und bei Mehrfachangeboten hinreichende Verschiedenheit. Einzelne Geschmackspräferenzen erzeugen kein Generator-/Prompt-Folgeissue ohne reproduzierbares Muster.

## Gesamt-Gate

- [ ] 1, 2 und 3 Angebote live geprüft
- [ ] AUTO, NONE und REQUIRED in der bestehenden Matrix geprüft
- [ ] Vote-Geheimhaltung und Vote-Änderung funktionieren
- [ ] Tie-Break korrekt, einmalig und persistent
- [ ] REROLL und Runde 2 einschließlich Restriction-History korrekt
- [ ] Ein-Offer-REROLL ohne Fake-Vote auto-bestätigt oder bereits durch 12C-02 abgedeckt
- [ ] stale/doppelte Interaktionen ändern keinen abgeschlossenen Zustand
- [ ] Requirement- und Restriction-Snapshots bleiben autoritativ
- [ ] Requestbudget und History-Invarianten nachgewiesen
- [ ] kein P0/P1 offen

## Folge-Issues

Keine – **NOT RUN**.
