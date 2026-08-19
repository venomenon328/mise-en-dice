# Phase 12D – Restart-, Recovery- und Betriebsabnahme

Stand: 19. August 2026

Issue: #89  
Umbrella: #85  
Evidenzbranch: `test/89-live-resilience-acceptance`

## Ergebnis

Phase 12D ist fachlich und betrieblich bestanden. Alle Pflichtszenarien wurden entweder live erfolgreich ausgeführt oder – im einzigen nicht sinnvoll live reproduzierbaren stale-Button-Unterfall – durch deterministische automatisierte Evidenz abgedeckt. Der reine Negativ-Untertest des interaktiven Reset-Prompts wurde versehentlich übersprungen und als `SKIPPED` dokumentiert; der eigentliche Reset-/Isolation-/Neuaufbaupfad wurde vollständig geprüft.

Es wurden keine P0/P1-Befunde, keine Datenkorruption, keine Produktionsauswirkung und keine unkontrollierten Providerrequests festgestellt. Die P2-Darstellungsbeobachtung aus dem Discord-Flow ist als #105 separat erfasst.

## Getesteter Stand und Baseline

- getesteter `main`: `325996dc0704bdc8139c63fcb04d4ff5322fc7d0`
- Acceptance: `med-acceptance`, Port `18090`, eigener PostgreSQL-Stack, Discord/OpenAI aktiviert
- Modell: `gpt-5.6-terra`, Reasoning `medium`
- Production während 12D unverändert auf `3ffc239fc357a8b8579aeb77b1de637e6f6562db`, Port `18080`, Discord/OpenAI deaktiviert
- feste OpenAI-Ausgangsbaseline aus 12B+12C: **8 Usage-Requests / 193.348 Tokens / 0,59 USD**
- vor dem ersten Eingriff validiertes Acceptance-Backup: `/opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-20260818T231430Z-8e7565ef791d.dump`
- zusätzliches Pre-Deploy-Backup: `/opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-20260818T232126Z-8e7565ef791d.dump`

Vor 12D-01 waren sieben persistierte Curation-Runden vorhanden, alle `COMPLETED`; es lief kein Providerclaim. Aus 12C existierte bewusst noch eine dormant offene Voting-Runde `5|4|2|5|OPEN|PENDING`. Sie war reiner wartender User-State und kein laufender Generator-/Providerprozess.

## Szenarien

### 12D-01 – Idle-Restart: PASS

Acceptance wurde gestoppt und wieder gestartet.

- Bot ging beim Stop offline und kam nach grob 12 Sekunden wieder online.
- Acceptance kam `running/healthy` auf demselben Commit zurück.
- Providerpersistenz blieb `COMPLETED|7`.
- Die dormant 12C-Runde blieb unverändert `5|4|2|5|OPEN|PENDING`.
- Production blieb unverändert gesund.

Befund: Restart erhält PostgreSQL-Zustand und erzeugt keine Providerarbeit.

### 12D-02 – Restart bei offener Runde ohne Vote: PASS

Session R1 / Session `6`, zwei Offers, `AUTO`:

- neue Voting-Runde `7`, Offer-Set `8`, initial `OPEN|PENDING`
- vor Restart exakt 0 Votes
- Providerpersistenz stieg nur durch die initiale Kuration von 7 auf 8 erfolgreiche Runden

Nach Stop/Start blieb die bereits vorhandene Discord-Nachricht bedienbar. Die erste Stimme wurde über genau diese alte Nachricht gespeichert.

- DB danach: exakt `7|2|OFFER|14`
- Discord zeigte nur `abgestimmt` / `noch offen`, nicht die konkrete Wahl
- Providerpersistenz blieb `COMPLETED|8`

Befund: stateless Discord-Komponenten funktionieren nach Restart gegen den persistierten Fachzustand weiter.

### 12D-03 – Restart nach genau einer Stimme: PASS

Mit exakt `7|2|OFFER|14` wurde erneut gestoppt/gestartet.

- Stimme blieb nach Restart unverändert persistiert.
- derselbe Teilnehmer konnte sie anschließend auf `7|2|OFFER|13` ändern.
- öffentliche Geheimhaltung blieb erhalten.
- kein zusätzlicher Providerrequest.

Nach dem späteren 12D-08-Redeploy wurde die zweite Stimme ebenfalls über die bereits vorhandene Nachricht abgegeben. Final:

- Runde `7|COMPLETED|OFFER|13|false|CONFIRMED`
- Votes `7|1|OFFER|13` und `7|2|OFFER|13`
- genau eine Challenge `5|13`
- Discord zeigte Gewinner, Einzelstimmen und bestätigte Challenge konsistent

### 12D-04 – REROLL-/Resume-Restart: PASS

Session R2 / Session `7`, zwei Offers:

- Initial-Offer-Set `9`, Voting-Runde `8`
- beide Teilnehmer entschieden `REROLL`
- der seit #100 sichtbare persistierte Zwischenzustand `🎲 Neue Angebote werden vorbereitet …` wurde erreicht
- Acceptance wurde gestoppt, bevor neue Offers erschienen

Nach Restart:

- Runde 8 blieb `COMPLETED|REROLL|PENDING`
- exakt eine `reroll_offer_exposure`
- REROLL-Attempt `10` existierte bereits als `CONTEXT_READY|NOT_STARTED`
- noch 0 Generation-Batches und 0 Curation-Runden für Attempt 10

Ein Resume während der noch aktiven Generation-Lease führte nur zu `REROLL_IN_PROGRESS|GENERATION:GENERATION_IN_PROGRESS`; weiterhin keine doppelten Batches oder Curations. Nach Lease-Ablauf wurde derselbe gespeicherte Attempt aus seinem persistierten Frozen Context fortgesetzt:

- genau eine REROLL-Curation `COMPLETED|RESULT_RECORDED|HTTP 200`
- neues Offer-Set `10`
- neue Voting-Runde `9`, ohne weitere REROLL-Option

Final:

- Runde 9 `COMPLETED|OFFER|17|false|CONFIRMED`
- beide Votes auf Offer 17
- Set 9 `REROLLED`
- Set 10 `CONFIRMED`
- genau eine Challenge
- genau eine REROLL-Exposition
- insgesamt zehn erfolgreiche Curation-Runden zu diesem Zeitpunkt

Befund: persistierter Apply-State schreitet über Restart/Resume monoton voran; keine doppelte Exposition, Regeneration oder Providerarbeit.

P2-Folgepunkt: Discord-Nachrichten könnten mit zurückhaltenden Emojis stärker strukturiert werden; separat als #105 erfasst.

### 12D-05 – nicht-retrybarer Authfehler: PASS

Nur Acceptance wurde temporär mit einem eindeutig ungültigen Testkey gestartet; gültige Properties blieben separat mit `0600` gesichert.

Neue Fehler-Session:

- Generation-Attempt `12`: `INITIAL|GENERATED|FAILED`
- genau eine Curation-Runde `13`
- `INITIAL_PASS|TECHNICAL_ERROR|RESULT_RECORDED`
- HTTP `401`
- Provider-Code `invalid_api_key`
- `provider_retryable=false`
- keine zweite Curation-Runde / kein technischer Retry

Discord zeigte nach ca. 10 Sekunden nur eine generische technische Fehlermeldung ohne SQL-/Secretdetails. Danach wurde die gültige Acceptance-Konfiguration vollständig restauriert; der ungültige Key war in der generierten Konfiguration nicht mehr vorhanden.

### 12D-06 – lokaler Transportfehler: PASS

Nur Acceptance wurde temporär gesetzt auf:

```properties
mise-en-dice.curation.openai.base-url=http://127.0.0.1:9
mise-en-dice.curation.openai.connect-timeout=PT2S
mise-en-dice.curation.openai.request-timeout=PT3S
mise-en-dice.curation.openai.recovery-window=PT4S
```

Nach explizitem App-Restart erzeugte eine Ein-Offer-Session nach ca. 7 Sekunden einen kontrollierten technischen Fehler:

- Attempt `11`: `INITIAL|GENERATED|FAILED`
- Runde 11: `INITIAL_PASS|TECHNICAL_ERROR|RESULT_RECORDED|OPENAI_TIMEOUT_OR_CONNECTION|retryable=true`
- Runde 12: `TECHNICAL_RETRY|TECHNICAL_ERROR|RESULT_RECORDED|OPENAI_TIMEOUT_OR_CONNECTION|retryable=true`
- kein HTTP-Status, da kein HTTP-Server erreicht wurde
- kein dritter Retry
- kein externer OpenAI-Request möglich

Danach wurde die gültige Konfiguration vollständig restauriert; Acceptance war wieder gesund, der Bot online und Production unverändert.

### Recovery-Session R3: PASS

Nach Restore beider Provider-Fehlerblöcke wurde eine normale neue Ein-Offer-Session gestartet.

- Session `10`, Attempt `13`
- `INITIAL|GENERATED|OFFER_READY`
- genau eine neue Curation-Runde `14`
- `INITIAL_PASS|COMPLETED|RESULT_RECORDED|HTTP 200`
- Provider-Response vorhanden, kein Providerfehler
- Provider-Audit danach: `COMPLETED|11`, `TECHNICAL_ERROR|3`

Die Voting-Runde `10` wurde mit `ACCEPT|ACCEPT` abgeschlossen; genau eine Challenge `7|19` wurde materialisiert. Damit ist die Wiederherstellung des echten Acceptance-Providers nach Transport- und 401-Test vollständig nachgewiesen.

### 12D-07 – Discord-Reconnect: PASS mit nicht live injizierbarem stale-Unterfall

Der Reconnect selbst wurde mehrfach live beobachtet:

- Bot ging bei App-Stop offline.
- Bot kam nach Start ohne manuelle Discord-Neuinstallation wieder online.
- vorhandene Komponenten blieben nach Restart und Redeploy bedienbar.
- danach funktionierten neue zulässige Flows regulär.
- Reconnect allein erzeugte keine Providerarbeit.

Der zusätzliche stale-Button-Unterfall ist über die normale Discord-Oberfläche nicht sinnvoll live injizierbar: terminale Nachrichten werden vom Renderer ohne Buttons aktualisiert. Die deterministische automatisierte Evidenz `DiscordChallengeWorkflowTest.rejectsStaleVoteWithoutChangingDomainState()` prüft explizit eine veraltete Round-ID, verhindert `castVoteDeferred()` und liefert die erwartete stale/rejected-Meldung. Dieser Teil ist daher `NOT PRACTICALLY INJECTABLE` live, aber automatisiert abgedeckt.

### 12D-08 – Redeploy bei offener Session: PASS

Mit Session 6 / Runde 7 `OPEN|PENDING` und exakt einer Stimme `7|2|OFFER|13` wurde `acceptance deploy main` ausgeführt.

Danach:

- Acceptance wieder gesund auf demselben Source-SHA
- Runde weiter `OPEN|PENDING`
- Stimme unverändert vorhanden
- Providerpersistenz weiter `COMPLETED|8`
- dieselbe schon vor Redeploy existierende Discord-Nachricht nahm anschließend die zweite Stimme an
- Session schloss genau einmal auf Challenge `5|13`

Production blieb unverändert.

### 12D-09 – Acceptance-Backup in providerfreier Preview: PASS

Pre-Reset-Backup:

`/opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-20260819T111623Z-325996dc0704.dump`

Vor Restore wurde Preview `12d-restore` frisch aus `main` deployt:

- `DISCORD_ENABLED=false`
- `OPENAI_ENABLED=false`
- relevante Workflowtabellen `0|0|0|0|0|0|0`

Nach Restore:

- Preview `running/healthy`, Port `18101`
- Daten exakt `10|13|14|11|10|18|7` für Sessions | Attempts | Curation-Rounds | Offer-Sets | Voting-Rounds | Votes | Challenges
- Curationstatus `COMPLETED|11`, `TECHNICAL_ERROR|3`
- `PREVIEW_PROVIDER_SECRETS_PRESENT=no`
- weder Acceptance-Discord-Token noch Acceptance-OpenAI-Key in Preview-Properties
- Production unverändert

Nach Abschluss wurde `12d-restore` vollständig einschließlich Datenbankvolume entfernt.

### 12D-10 – Acceptance-Reset und Neuaufbau: PASS

Der reine Negativ-Untertest „falsche Bestätigung bricht interaktiven Reset ab“ wurde nicht ausgeführt, weil direkt die korrekte Bestätigung `RESET-acceptance` eingegeben wurde. Dieser Untertest ist `SKIPPED (operator workflow, low practical relevance)` und wurde nicht künstlich nachgestellt.

Der eigentliche Reset-/Isolationstest ist vollständig bestanden:

- Acceptance-Metadaten entfernt
- Acceptance-Container: `0`
- Acceptance-Volumes: `0`
- externe `acceptance.properties` weiter vorhanden
- Dateimodus `0600`
- bytegleich zur gesicherten guten Konfiguration
- Preview `12d-restore` blieb während des Resets gesund und providerfrei
- Production blieb unverändert gesund

Acceptance wurde anschließend frisch aus `main` neu aufgebaut:

- Bot wieder online
- neue Acceptance-DB vollständig leer: `0|0|0|0|0|0|0`
- frische Post-Reset-Baseline gesichert unter `/opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-20260819T121317Z-325996dc0704.dump`

## Request-, Token- und Kostenabgleich

Finaler OpenAI-Usage-Snapshot des Acceptance-Projekts:

- **12 Usage-Requests**
- **272.779 Tokens**
- **0,91 USD**

Gegen Baseline 12B+12C:

- Baseline: 8 Requests / 193.348 Tokens / 0,59 USD
- Delta 12D: **+4 Usage-Requests / +79.431 Tokens / +0,32 USD**

Die vier Usage-Requests entsprechen exakt:

1. R1 initial
2. R2 initial
3. R2 REROLL
4. R3 recovery

Der lokale Transporttest ging ausschließlich an `127.0.0.1:9` und erzeugte keinen externen OpenAI-Request. Der Auth-Test ist DB-seitig als genau ein externer HTTP-Versuch `401 / invalid_api_key / retryable=false` auditierbar, erscheint aber nicht als Usage-/Billing-Request und erzeugte keine Tokens/Kosten.

Damit ergeben sich für 12D **5 tatsächliche externe OpenAI-HTTP-Versuche** (4 erfolgreiche Modellaufrufe + 1 vor Modellverarbeitung abgewiesener 401) und damit weniger als das Gate von maximal 6. Es gibt keinen Kosten- oder Persistenzhinweis auf unerklärte Zusatzrequests.

## Betriebs- und Abschlussbeobachtung

Nach 12D-10 und Cleanup:

- Restore-Test-Preview `12d-restore` entfernt
- bestehendes, nicht zu 12D gehörendes Preview `cal40` blieb unangetastet
- Acceptance `running/healthy`, Source `325996dc0704...`
- Production `running/healthy`, Source `3ffc239fc357...`
- alle Acceptance-/Production-App-/PostgreSQL-Container: `restart=0`, `oom=false`
- Memory-Snapshot:
  - Acceptance App ca. 258,6 MiB / 768 MiB
  - Acceptance PostgreSQL ca. 19,34 MiB / 512 MiB
  - Production App ca. 293,9 MiB / 768 MiB
  - Production PostgreSQL ca. 19,59 MiB / 512 MiB
- finaler secretfreier Logscan: `SUSPICIOUS_SECRET_LINES=0`
- finaler `WARN`/`ERROR`-Scan ergab genau zwei Startup-Zeilen, beide als nicht blockierend klassifiziert:
  - PostgreSQL meldete beim ersten Start der frisch zurückgesetzten Acceptance-DB einmalig `relation "public.databasechangeloglock" does not exist`. Der Befund trat im Liquibase-Bootstrapfenster der leeren DB auf; die Migration legte die Metadaten anschließend erfolgreich an, die App wurde gesund und es gab keinen Restart. In diesem Kontext ist die Zeile transientes Bootstrap-Rauschen, kein Persistenzfehler.
  - Spring Security warnte vor zwei `UserDetailsService`-Beans und deshalb nicht verwendeter globaler Auto-Konfiguration. Die Administration registriert jedoch ihren `DaoAuthenticationProvider` explizit im eigenen `SecurityFilterChain`; der globale Auto-Konfigurationspfad ist nicht die Authentifizierungsautorität. Funktion und Security-Tests blieben grün. Die Zeile ist nicht blockierendes Startup-Rauschen; eine spätere Bereinigung wäre höchstens P3.

Kein OOM, kein Container-Restart, keine auffällige Ressourcensättigung.

## Szenariomatrix

| Szenario | Ergebnis | Providerwirkung | Kernaussage |
| --- | --- | ---: | --- |
| 12D-01 Idle-Restart | PASS | 0 | DB und Bot kommen sauber zurück |
| 12D-02 offene Runde ohne Vote | PASS | 0 zusätzlich | alte Discord-Komponenten bleiben bedienbar |
| 12D-03 Restart nach einem Vote | PASS | 0 zusätzlich | Stimme, Geheimhaltung und Änderung bleiben korrekt |
| 12D-04 REROLL-/Resume-Restart | PASS | 1 REROLL-Request | monotone Recovery ohne Duplikate |
| 12D-05 ungültiger Acceptance-Key | PASS | 1 HTTP-401, 0 Usage | nicht retrybar, auditierbar, secretfrei |
| 12D-06 lokaler Transportfehler | PASS | 0 extern | genau ein begrenzter Technical Retry |
| 12D-07 Discord-Reconnect | PASS* | 0 | Reconnect live PASS; stale-Unterfall automatisiert, live nicht sinnvoll injizierbar |
| 12D-08 Redeploy offene Session | PASS | 0 zusätzlich | persistierte offene Session überlebt Redeploy |
| 12D-09 Backup → Preview | PASS | 0 | vollständiger Restore bei providerfreier Secret-Isolation |
| 12D-10 Reset / Neuaufbau | PASS** | 0 | nur Acceptance entfernt und sauber neu aufgebaut |

`*` stale-Button-Unterfall: `NOT PRACTICALLY INJECTABLE` live, deterministischer Test PASS.  
`**` falsche-Reset-Bestätigung-Untertest: `SKIPPED`; eigentlicher Reset-/Isolation-/Neuaufbaupfad PASS.

## Gate

- [x] Idle-, offene-Runde- und Ein-Vote-Restarts funktionieren
- [x] REROLL-/Resume-Pfad restartfest
- [x] nicht-retrybarer Providerfehler und Transportfehler bleiben begrenzt und technisch korrekt
- [x] Discord verbindet sich nach Restart erneut
- [x] Redeploy erhält persistierte Sessions
- [x] Acceptance-Backup lässt sich providerfrei und secret-isoliert in Preview restaurieren
- [x] Reset entfernt ausschließlich Acceptance; Neuaufbau erfolgreich
- [x] Requestbudget eingehalten
- [x] keine Secrets, unkontrollierten Requests oder P0/P1-Befunde
- [x] finale WARN/ERROR-Logzeilen klassifiziert; keine davon blockiert 12D

## Folgepunkte

- #105: Discord-Nachrichten mit zurückhaltenden Emoji-Cues strukturieren; P2, kein 12D-Blocker.

## Abschluss

**12D ist vollständig bestanden.** Die beiden finalen Logzeilen sind als nicht blockierendes Startup-Rauschen klassifiziert. Der Evidenz-PR kann ohne weitere Live-Providerläufe gemergt und #89 geschlossen werden.