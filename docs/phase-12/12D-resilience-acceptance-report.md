# Phase 12D – Restart-, Recovery- und Betriebsabnahme

Stand: 19. August 2026

Issue: #89  
Umbrella: #85  
Evidenzbranch: `test/89-live-resilience-acceptance`

## Ausgangspunkt

Phase 12C ist abgeschlossen. Der Branch basiert auf dem finalen 12C-`main` nach Merge von PR #104.

12D wird ausschließlich gegen die isolierte Acceptance-Instanz ausgeführt. Production darf durch keinen Fehlerblock beeinflusst werden. Live-Secrets werden nie ausgegeben oder committed.

## Baseline vor dem ersten Szenario

- getesteter `main`-Commit: `325996dc0704bdc8139c63fcb04d4ff5322fc7d0`
- Acceptance vor 12D-Deployment: `running/healthy`, Port `18090`, Source `8e7565ef791d4a85cbb7a8aedbbba095d3582164`, Discord/OpenAI aktiviert, `gpt-5.6-terra`, Reasoning `medium`
- Production unverändert gesund: `running/healthy`, Port `18080`, Source `3ffc239fc357a8b8579aeb77b1de637e6f6562db`, Discord/OpenAI deaktiviert
- Acceptance-Baseline-Backup: `/opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-20260818T231430Z-8e7565ef791d.dump`, durch Operator validiert
- OpenAI-Acceptance-Ausgangsstand: 8 Requests / 0,59 USD kumulativ aus 12B+12C
- CI: Der finale `main`-Commit enthält gegenüber dem zuvor abgenommenen Code-Stand nur den 12C-Dokumentationsmerge aus PR #104. `Verify` und `Deployment Verify` waren auf dem unmittelbar vorherigen Produktcode-Stand aus PR #103 grün.

### Deployment des 12D-Ausgangsstands

- `acceptance preflight`: gültig; Discord/OpenAI aktiviert, Modell `gpt-5.6-terra`, Reasoning `medium`
- Image `325996dc0704` erfolgreich gebaut und gegen eine frische PostgreSQL-Smoke-Datenbank geprüft
- automatisches zusätzliches Pre-Deploy-Backup validiert: `/opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-20260818T232126Z-8e7565ef791d.dump`
- Acceptance auf `325996dc0704bdc8139c63fcb04d4ff5322fc7d0` aktualisiert; Port `18090`, Discord/OpenAI weiter aktiviert
- unmittelbarer Status nach Deploy: App lief bereits, Docker-Health noch `starting`; der Acceptance-Bot kam anschließend erfolgreich wieder online
- Production blieb während des Deployments unverändert `running/healthy` auf `3ffc239fc357a8b8579aeb77b1de637e6f6562db`, Discord/OpenAI deaktiviert

### Vorzustand 12D-01

Unmittelbar vor dem Idle-Restart war Acceptance nach acht Minuten Laufzeit `running/healthy` auf dem finalen 12C-Commit. Die Providerpersistenz enthielt exakt sieben `curation_round`-Zeilen und ausschließlich Status `COMPLETED`; es lief damit keine Kuration und kein Provider-Claim.

Aus 12C ist bewusst noch genau eine wartende fachliche Runde vorhanden: `selection_voting_round.id=5`, Session `4`, Runde `2`, Offer-Set `5`, Status `OPEN`, Apply-State `PENDING`, ohne Ergebnis. Das ist die in 12C dokumentierte nicht mehr abgeschlossene Session-D-Runde. Sie ist ein persistierter, auf Nutzereingabe wartender Zustand und kein laufender Generator-/Providerprozess. 12D-01 wird deshalb als **prozess-idler Restart mit transparent dokumentiertem dormant User-State** ausgeführt. Der streng sichtbare Open-Round-Restart aus 12D-02 wird separat mit einer neuen Session getestet, da die alte Discord-Nachricht aus Session D nicht mehr zur Verfügung steht.

### Ergebnis 12D-01 – Stop/Start

- Nach `acceptance stop` ging der Acceptance-Bot wie erwartet offline.
- Nach `acceptance start` kam der Bot wieder online; beobachtete Zeit von Start bis Discord online: grob **12 Sekunden**.
- Acceptance war danach wieder `running/healthy` auf unverändertem Source-Commit `325996dc0704bdc8139c63fcb04d4ff5322fc7d0`.
- Providerpersistenz blieb unverändert bei `COMPLETED|7`; der Restart erzeugte keinen neuen persistierten Providerrequest.
- Die dormant 12C-Runde blieb unverändert `5|4|2|5|OPEN|PENDING` ohne Ergebnis.
- Production blieb `running/healthy` auf `3ffc239fc357a8b8579aeb77b1de637e6f6562db`, Discord/OpenAI deaktiviert.

**12D-01: PASS.** Der App-/Discord-Restart erhält PostgreSQL-Zustand und erzeugt keine Providerarbeit.

### Session R1 – 12D-02/03/08

Für die Restart-/Redeploy-Szenarien wurde genau eine neue Zwei-Offer-Session erzeugt:

- Session `6`, `requested_offer_count=2`, Restriction-Mode `AUTO`
- Voting-Runde `7`, Runde `1`, Offer-Set `8`, zunächst `OPEN|PENDING`
- Providerpersistenz stieg exakt von `COMPLETED|7` auf `COMPLETED|8`; damit genau ein neuer erfolgreicher Curation-Request
- Discord zeigte beide Offers vollständig und zunächst beide Teilnehmer als `noch offen`

#### Ergebnis 12D-02 – Restart ohne Vote

Vor dem Restart waren für Runde `7` exakt **0 Votes** persistiert. Acceptance wurde gestoppt und wieder gestartet; danach blieb die bereits vorhandene Discord-Nachricht mit ihren alten Buttons verwendbar. Die erste Stimme wurde über genau diese Nachricht erfolgreich abgegeben.

Beobachtung danach:

- öffentlich nur `Testsklave: abgestimmt` und `venomenon: noch offen`, keine konkrete Wahl offengelegt
- DB: exakt eine Stimme `7|2|OFFER|14`
- Providerpersistenz unverändert `COMPLETED|8`
- kein neuer Curation-/Providerrequest durch Restart oder Nutzung des vorhandenen Buttons

**12D-02: PASS.** Die offene erste Runde bleibt über Restart hinweg bedienbar; keine Regenerierung oder Duplizierung.

#### Zwischenstand 12D-03 – Restart nach genau einer Stimme

Mit exakt der einen persistierten Stimme `7|2|OFFER|14` wurde Acceptance erneut gestoppt und gestartet. Nach dem Restart war dieselbe Stimme weiterhin unverändert persistiert. Anschließend konnte derselbe Teilnehmer seine Stimme über die vorhandene Discord-Nachricht regelkonform ändern:

- vor Änderung nach Restart: `7|2|OFFER|14`
- nach Änderung: `7|2|OFFER|13`
- öffentlich weiterhin nur `Testsklave: abgestimmt` und `venomenon: noch offen`; die konkrete Wahl blieb geheim
- ephemere Bestätigung: `Deine Stimme wurde gespeichert.`
- Providerpersistenz weiterhin `COMPLETED|8`

Damit sind Persistenz, Geheimhaltung und Vote-Änderung nach Restart nachgewiesen. Die zweite Stimme wird bewusst noch zurückgehalten, damit derselbe Ein-Vote-Zustand unmittelbar für 12D-08 (Redeploy mit offener Session) verwendet werden kann. 12D-03 bleibt bis zum anschließenden regulären Abschluss der Runde `IN PROGRESS`.

## Optimierte Szenarioreihenfolge

Die Pflichtfälle aus #89 sollen mit höchstens drei erfolgreichen kostenpflichtigen Generation Sessions auskommen. Fehler-Sessions mit absichtlich ungültigem Key oder lokalem Transportziel erzeugen keine normalen kostenpflichtigen Providerläufe.

1. **12D-01 Idle-Restart** – providerfrei.
2. **Session R1** – Zwei-Offer-Session für 12D-02 und 12D-03; Redeploy nach genau einer Stimme kann zugleich 12D-08 abdecken.
3. **12D-07 Discord-Reconnect** – an denselben Restarts/Redeploys beobachten; anschließend stale Button prüfen.
4. **Session R2** – Zwei-Offer-REROLL für 12D-04; den seit #100 sichtbaren persistierten Zwischenzustand `🎲 Neue Angebote werden vorbereitet …` als Stop-/Resume-Fenster nutzen.
5. **12D-06 Transportfehler** – Acceptance auf `127.0.0.1:9`, danach Konfiguration vollständig restaurieren.
6. **12D-05 nicht-retrybarer Authfehler** – nur Acceptance-Key temporär ungültig setzen, danach vollständig restaurieren.
7. **Session R3** – erfolgreicher Recovery-Flow mit echten Acceptance-Secrets; bestätigt Wiederherstellung nach beiden Providerfehlerblöcken.
8. **12D-09 Backup → secretfreie Preview** – providerfrei.
9. **12D-10 Acceptance-Reset und Neuaufbau** – providerfrei; finales Backup vorher erzeugen.

## Szenarioevidenz

| Szenario | Session | Zustand vorher | Eingriff | Requests | Ergebnis | Befund / Folgeissue |
| --- | --- | --- | --- | ---: | --- | --- |
| 12D-01 Idle-Restart | – | healthy; 7/7 Curation-Runden COMPLETED; eine dormant offene 12C-Runde | `acceptance stop/start`; Bot offline und nach ~12 s wieder online | 0 | PASS | Providerzustand und dormant Runde unverändert; Production unverändert |
| 12D-02 offene Runde ohne Vote | R1 / Session 6 / Round 7 | 2 Offers sichtbar, 0 Votes, `OPEN|PENDING` | Stop/Start, danach vorhandenen Vote-Button verwendet | 0 zusätzlich | PASS | alte Buttons funktionsfähig; eine Stimme persistiert; Provider blieb bei 8 |
| 12D-03 Restart/Redeploy nach einem Vote | R1 / Session 6 / Round 7 | eine Stimme `OFFER|14` | Stop/Start; Stimme danach auf `OFFER|13` geändert | 0 zusätzlich | IN PROGRESS | Persistenz/Änderung/Geheimhaltung PASS; Abschluss nach 12D-08 |
| 12D-04 REROLL-/Resume-Restart | R2 | | | | NOT RUN | |
| 12D-05 ungültiger Acceptance-Key | Fehler-Session | | | | NOT RUN | |
| 12D-06 lokaler Transportfehler | Fehler-Session | | | 0 extern | NOT RUN | |
| 12D-07 Discord-Reconnect | R1/R2 | | | 0 zusätzlich | NOT RUN | |
| 12D-08 Redeploy mit offener Session | R1 / Session 6 / Round 7 | eine geänderte Stimme `OFFER|13`, zweite Stimme offen | | 0 zusätzlich | NOT RUN | |
| 12D-09 Backup in Preview | – | | | 0 | NOT RUN | |
| 12D-10 Reset / Neuaufbau | – | | | 0 | NOT RUN | |

## Beobachtungen Betrieb

Je relevanter Operation notieren:

- App-/PostgreSQL-Memory und Restartzähler
- Zeit bis Health wieder grün ist
- Zeit bis Discord wieder online/bedienbar ist
- unerwartete Warnungsflut oder Executor-/Thread-Probleme
- Loggröße/Rotation
- Production weiterhin unverändert

## Stop-the-line

Sofort abbrechen und eigenes P0/P1-Issue anlegen bei:

- Secret-Leak
- Production-Auswirkung
- Datenverlust/-korruption
- unkontrollierten Providerrequests
- dauerhaft festhängender Session
- doppelter Challenge-/REROLL-Materialisierung

## Gate

- [x] Idle-Restart funktioniert
- [x] offene-Runde-Restart funktioniert
- [ ] Ein-Vote-Restart vollständig abgeschlossen (Persistenz und Vote-Änderung bereits PASS)
- [ ] REROLL-/Resume-Pfad restartfest oder Mikrofenster sauber als nicht injizierbar begründet
- [ ] Auth- und Transportfehler bleiben begrenzt und technisch korrekt
- [ ] Discord verbindet sich sauber neu
- [ ] Redeploy erhält persistierte offene Sessions
- [ ] Acceptance-Backup lässt sich secretfrei in Preview restaurieren
- [ ] Reset betrifft ausschließlich Acceptance
- [ ] Requestbudget bleibt eingehalten
- [ ] keine Secrets, unkontrollierten Requests oder P0/P1-Befunde

## Abschluss

Noch offen.
