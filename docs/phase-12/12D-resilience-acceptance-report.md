# Phase 12D – Restart-, Recovery- und Betriebsabnahme

Stand: 19. August 2026

Issue: #89  
Umbrella: #85  
Evidenzbranch: `test/89-live-resilience-acceptance`

## Ausgangspunkt

Phase 12C ist abgeschlossen. Der Branch basiert auf dem finalen 12C-`main` nach Merge von PR #104.

12D wird ausschließlich gegen die isolierte Acceptance-Instanz ausgeführt. Production darf durch keinen Fehlerblock beeinflusst werden. Live-Secrets werden nie ausgegeben oder committed.

## Baseline vor dem ersten Szenario

Noch einzutragen:

- getesteter `main`-Commit:
- Acceptance-Status:
- Production-Status unverändert:
- Acceptance-Backup:
- OpenAI-Acceptance-Ausgangsstand: 8 Requests / 0,59 USD kumulativ aus 12B+12C
- Verify / Deployment Verify auf dem getesteten Commit:

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
| 12D-01 Idle-Restart | – | | | 0 | NOT RUN | |
| 12D-02 offene Runde ohne Vote | R1 | | | | NOT RUN | |
| 12D-03 Restart/Redeploy nach einem Vote | R1 | | | 0 zusätzlich | NOT RUN | |
| 12D-04 REROLL-/Resume-Restart | R2 | | | | NOT RUN | |
| 12D-05 ungültiger Acceptance-Key | Fehler-Session | | | | NOT RUN | |
| 12D-06 lokaler Transportfehler | Fehler-Session | | | 0 extern | NOT RUN | |
| 12D-07 Discord-Reconnect | R1/R2 | | | 0 zusätzlich | NOT RUN | |
| 12D-08 Redeploy mit offener Session | R1 | | | 0 zusätzlich | NOT RUN | |
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

- [ ] Idle-, offene-Runde- und Ein-Vote-Restarts funktionieren
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
