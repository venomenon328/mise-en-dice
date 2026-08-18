# Produktionsnahe Live-Validierung

Stand: 18. August 2026

## 1. Zweck und Paketfolge

Phase 12 validiert den nach Phase 11 erreichten Kern unter kontrollierten echten Bedingungen. Sie führt keine neue
Challenge-Fachlogik ein. Die feste Acceptance-Instanz trennt Live-Abnahme von Produktionshistorie, Produktionsbot
und Produktions-OpenAI-Projekt.

Die Reihenfolge ist verbindlich:

1. **12A / #86:** isolierte Acceptance, Providersecret-Härtung, Operatorpfad und diese Evidenzgrundlage.
2. **12B / #87:** manuelle Discord-/OpenAI-Inbetriebnahme und negativer Zugriffssmoke.
3. **12C / #88:** vollständige 1..3-Offer-, Voting-, Reroll- und Snapshot-Abnahme.
4. **12D / #89:** Restart, Recovery, Redeploy, Backup/Restore und Acceptance-Reset.
5. **12E / #90:** privater Produktionspilot und dokumentiertes Go/No-Go.

Ein P0- oder P1-Befund stoppt die Folgepakete. Tokens, API-Keys, vollständige Authorization-Header, unredigierte
Runtime-Dateien und nicht zwingend erforderliche persönliche Discord-IDs gehören weder in diese Datei noch in
Evidenz, Issues oder Commits.

## 2. Sicherheitsmodell

| Instanz | Compose-Projekt / Daten | Providerproperties | Profil |
| --- | --- | --- | --- |
| Produktion | `med-production` / eigenes Volume | `discord.properties`, optional `openai.properties` | bei aktivem OpenAI `production` |
| Acceptance | `med-acceptance` / eigenes Volume | nur `acceptance.properties` | immer `production` |
| Preview | `med-preview-<name>` / eigenes Volume | keine; beide Adapter explizit deaktiviert | kein `production` nur für Provider |
| Smoke | temporäres `med-smoke-*` / frisches Volume | keine; beide Adapter explizit deaktiviert | kein `production` nur für Provider |

Alle Webports binden nur an `127.0.0.1`; PostgreSQL erhält keinen Hostport. Acceptance benötigt keinen Caddy-Eintrag
und keine öffentliche Domain. Die Properties-Datei wird read-only nach `/run/mise-en-dice/application.properties`
gemountet. Providerwerte erscheinen weder als Compose-Environment noch in Metadaten oder Statusausgaben.

## 3. Installation der Acceptance-Instanz

Auf dem VPS zuerst den Operator auf den Stand dieses Pakets bringen und die vorhandene Runtime unverändert lassen:

```bash
cd /opt/mise-en-dice/repository
git switch main
git pull --ff-only
export MISE_EN_DICE_DEPLOY_ROOT=/opt/mise-en-dice/runtime
./deploy/mise-en-dice.sh doctor
```

Eine alte `operator.conf` bleibt kompatibel: Fehlt `ACCEPTANCE_PORT`, verwendet der Operator den festen Standard
`18090`. Er darf weder dem Produktionsport entsprechen noch im Previewbereich liegen. Für eine bewusste neue
Initialisierung kann der Wert vor `init` gesetzt werden; ein bestehendes Deployment benötigt kein `init --force`.

```bash
export MISE_EN_DICE_ACCEPTANCE_PORT=18090
```

Die laufende Datenbank und `admin.properties` bleiben unverändert. `admin.properties` darf für Acceptance verwendet
werden, enthält aber keine Providerwerte.

## 4. Providerdateien außerhalb des Repositories

Alle Dateien liegen unter `/opt/mise-en-dice/runtime`, nicht im Git-Checkout. Sie gehören dem Betriebsbenutzer und
sind höchstens für ihn lesbar:

```bash
umask 077
touch /opt/mise-en-dice/runtime/acceptance.properties
chmod 0600 /opt/mise-en-dice/runtime/acceptance.properties
```

Produktion behält die bestehende Datei `discord.properties` und kann zusätzlich `openai.properties` erhalten:

```properties
# runtime/discord.properties
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=PRODUCTION_DISCORD_TOKEN
mise-en-dice.discord.guild-id=PRODUCTION_GUILD_ID
mise-en-dice.discord.effective-date-zone=Europe/Berlin
mise-en-dice.discord.participant-user-ids.GEORGIA=PRODUCTION_GEORGIA_ID
mise-en-dice.discord.participant-user-ids.TOBIAS=PRODUCTION_TOBIAS_ID
```

```properties
# runtime/openai.properties
mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.api-key=PRODUCTION_OPENAI_PROJECT_KEY
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
```

Acceptance besitzt nur eine kombinierte Datei mit **anderen** Providerwerten:

```properties
# runtime/acceptance.properties
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=ACCEPTANCE_DISCORD_TESTBOT_TOKEN
mise-en-dice.discord.guild-id=ACCEPTANCE_GUILD_ID
mise-en-dice.discord.effective-date-zone=Europe/Berlin
mise-en-dice.discord.participant-user-ids.GEORGIA=ACCEPTANCE_GEORGIA_ID
mise-en-dice.discord.participant-user-ids.TOBIAS=ACCEPTANCE_TOBIAS_ID

mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.api-key=ACCEPTANCE_OPENAI_PROJECT_KEY
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
```

Der Preflight akzeptiert nur die dokumentierten Providerkeys. Er verlangt bei aktivem Discord positive Guild- und
User-IDs sowie verschiedene IDs für `GEORGIA` und `TOBIAS`; bei aktivem OpenAI Modell und unterstützte
Reasoning-Stufe. Bei deaktiviertem Provider muss der jeweilige Secretwert fehlen:

```properties
mise-en-dice.discord.enabled=false
mise-en-dice.curation.openai.enabled=false
```

Die Operatorprüfung lehnt Symlinks, Dateien im Repository, Gruppen-/Other-Rechte sowie gleiche Acceptance- und
Produktions-Discord-Token beziehungsweise OpenAI-Keys ab, ohne die Werte auszugeben. Ein eigener Discord-Testbot
wird im [Discord Developer Portal](https://discord.com/developers/applications) angelegt; ein getrenntes,
kostenbegrenztes Projekt und dessen Key werden in der [OpenAI Platform](https://platform.openai.com/settings/organization/projects)
verwaltet.

## 5. Preflight und Operatorbefehle

Vor jedem Live-Deployment:

```bash
./deploy/mise-en-dice.sh acceptance preflight
./deploy/mise-en-dice.sh acceptance deploy main
```

Der Preflight beendet sich vor dem Dockerstart bei fehlender, unsicherer oder unvollständiger Konfiguration sowie
Port- oder Secretkollision. Der Deploy verwendet denselben Commitauflösungs-, Imagebuild-, Smoke-, Healthcheck- und
geschützten Adminpfad wie Produktion. Acceptance setzt `server.servlet.session.cookie.secure=false`; ein lokaler
SSH-Tunnel ist deshalb der einzige vorgesehene Adminzugang:

```bash
ssh -N -L 18090:127.0.0.1:18090 operator@vps.example
# Danach lokal: http://localhost:18090/admin
```

Weitere sichere Operatorbefehle:

```bash
./deploy/mise-en-dice.sh acceptance status
./deploy/mise-en-dice.sh acceptance logs [--follow]
./deploy/mise-en-dice.sh acceptance stop
./deploy/mise-en-dice.sh acceptance start
./deploy/mise-en-dice.sh acceptance sql '<eine read-only Query ohne Semikolon>'
./deploy/mise-en-dice.sh acceptance backup
./deploy/mise-en-dice.sh acceptance reset [--yes]
```

`status` zeigt ausschließlich Commit, Port, Container-/Healthstatus, Aktivierung sowie bei aktivem OpenAI Modell und
Reasoning-Stufe. Es gibt keine Ausgabe für Tokens, API-Keys oder vollständige Properties-Zeilen. Logs nur in einer
zugriffsgeschützten Operatorsitzung prüfen und vor dem Kopieren in ein Issue auf Secrets untersuchen.

## 6. Read-only Diagnosen

`acceptance sql` akzeptiert eine einzelne `SELECT`-, `WITH`-, `EXPLAIN`- oder `SHOW`-Query ohne Semikolon und führt
sie in einer PostgreSQL-Read-only-Transaktion aus. Fachzustände werden nie per SQL erzeugt oder geändert.

Für jede Testszenario-ID die zugehörigen IDs separat in der Evidenz halten und diese Abfragen verwenden:

```bash
# Tatsächlich beanspruchte externe Request-Slots und Curatorstatus je Attempt
./deploy/mise-en-dice.sh acceptance sql \
  'SELECT generation_attempt_id, count(*) AS request_slots, array_agg(dispatch_status ORDER BY round_number) AS dispatches FROM curation_round WHERE NOT legacy_migrated GROUP BY generation_attempt_id ORDER BY generation_attempt_id DESC'

# OpenAI-Tokenverbrauch und HTTP-/Dispatchzustand, ohne Request- oder Responsepayloads auszugeben
./deploy/mise-en-dice.sh acceptance sql \
  "SELECT id, generation_attempt_id, dispatch_status, provider_http_status, coalesce((provider_usage_snapshot ->> 'inputTokens')::int, 0) AS input_tokens, coalesce((provider_usage_snapshot ->> 'outputTokens')::int, 0) AS output_tokens, coalesce((provider_usage_snapshot ->> 'reasoningTokens')::int, 0) AS reasoning_tokens, coalesce((provider_usage_snapshot ->> 'totalTokens')::int, 0) AS total_tokens FROM curation_round WHERE NOT legacy_migrated ORDER BY id DESC LIMIT 20"

# Session, Offer-Set, Voting- und Challengezustand
./deploy/mise-en-dice.sh acceptance sql \
  'SELECT session_row.id AS session_id, session_row.requested_offer_count, attempt.id AS attempt_id, attempt.curation_status, voting.status AS voting_status, voting.result_option_type, voting.apply_state, challenge_row.id AS challenge_id FROM challenge_session session_row LEFT JOIN generation_attempt attempt ON attempt.challenge_session_id = session_row.id LEFT JOIN selection_voting_round voting ON voting.challenge_session_id = session_row.id LEFT JOIN challenge challenge_row ON challenge_row.generation_attempt_id = attempt.id ORDER BY session_row.id DESC, attempt.id DESC'

# Cooldown-only-Exposition eines freiwillig rerollten Offer-Sets
./deploy/mise-en-dice.sh acceptance sql \
  'SELECT challenge_session_id, curated_offer_set_id, created_at FROM reroll_offer_exposure ORDER BY created_at DESC LIMIT 20'
```

Die vierte Abfrage bestätigt nur die Existenz der Exposition und gibt keine Requirementtexte oder Providerpayloads aus.
Für tiefergehende, sensitive Audits existiert bewusst kein Kopierbefehl im Runbook.

## 7. Backup, Restore, Reset und Notfall

Ein Acceptance-Backup ist ein validiertes PostgreSQL-Custom-Archiv mit Prefix `mise-en-dice-acceptance-`:

```bash
./deploy/mise-en-dice.sh acceptance backup
./deploy/mise-en-dice.sh preview deploy main acceptance-restore
./deploy/mise-en-dice.sh preview restore acceptance-restore /opt/mise-en-dice/runtime/backups/mise-en-dice-acceptance-....dump --yes
```

Die Restore-Preview erhält weiterhin explizit deaktivierte Discord- und OpenAI-Adapter. Es gibt keinen
Produktionsrestore-Befehl. Vor dem irreversiblen Acceptance-Reset bei Bedarf zuerst ein Backup ausführen:

```bash
./deploy/mise-en-dice.sh acceptance backup
./deploy/mise-en-dice.sh acceptance reset
```

Interaktiv verlangt der Operator exakt `RESET-acceptance`; nichtinteraktiv ist ausschließlich `--yes` erlaubt. Der
Reset entfernt nur `med-acceptance`, dessen PostgreSQL-Volume und `runtime/instances/acceptance`. Die Providerdatei
bleibt absichtlich erhalten, damit sie nicht aus einer Shell-Historie rekonstruiert werden muss. Produktion und
Previews bleiben unangetastet.

Bei Secretverdacht, unerwarteter Provideraktivierung, Kostenanstieg, Datenverlust oder falscher
Challenge-Materialisierung: Acceptance sofort stoppen, keine weiteren Live-Interaktionen auslösen, relevante
secretfreie IDs und Zeitpunkte sichern, Secret beim Provider rotieren und einen P0/P1-Befund anlegen.

## 8. Evidenz- und Kostenprotokoll

Für jedes manuelle Szenario eine kopierbare, secretfreie Zeile ausfüllen:

```text
UTC:
Commit / Instanz: <sha> / acceptance
Szenario-ID:
Erwartung:
Beobachtung:
Session / Attempt / Offer-Set / Voting-Runde / Challenge:
OpenAI-Requests: <0|1|2>
Input / Output / Reasoning / Gesamt-Tokens:
Geschätzte Kosten / Projektlimit:
Latenz aus Nutzersicht:
Restart- oder Fehlerschritt:
Ergebnis: PASS | FAIL | BLOCKED
Folge-Issue / Priorität:
```

`PASS` bedeutet, dass die dokumentierte Erwartung einschließlich Requestbudget erfüllt ist. `FAIL` dokumentiert einen
reproduzierbaren Befund; `BLOCKED` hält eine externe oder organisatorische Blockade fest, ohne Erfolg zu behaupten.
Echte Discord- und OpenAI-Requests werden nur in diesen manuellen Szenarien ausgelöst — niemals in CI, Maven-Tests,
Previews oder Smokes.
