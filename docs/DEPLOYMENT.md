# Deployment und Branch-Previews

Stand: 19. August 2026

Dieses Dokument beschreibt den Betrieb von Mise en Dice auf einem einzelnen Debian-/Docker-VPS. Der bestehende Gridwords-Stack bleibt ein vollständig getrenntes Compose-Projekt. Mise en Dice kennt weder dessen Dateien noch Container, Netzwerke oder Volumes.

## 1. Betriebsmodell

Jede Mise-en-Dice-Instanz besteht aus genau zwei Containern:

```text
Browser oder Caddy
        │
        │ 127.0.0.1:<Port>
        ▼
Mise-en-Dice-App ─────► PostgreSQL 17
                         kein Host-Port
```

Es gibt drei strikt getrennte Instanzarten:

- **Produktion** verwendet das feste Compose-Projekt `med-production`, den konfigurierten Produktionsport und ein dauerhaftes Datenbankvolume.
- **Acceptance** verwendet das feste Compose-Projekt `med-acceptance`, den eigenen konfigurierten Loopback-Port und ein eigenes dauerhaftes Datenbankvolume. Sie ist genau eine serverseitige Live-Testinstanz, kein frei benennbares Preview.
- **Preview** verwendet `med-preview-<name>`, einen automatisch gewählten Loopback-Port und ein eigenes Datenbankvolume. Ein Branch kann daher keine Produktionsdaten verändern.

Alle Webports werden ausschließlich an `127.0.0.1` gebunden. PostgreSQL wird überhaupt nicht auf dem Host veröffentlicht. Für Previews erfolgt der Zugriff per SSH-Tunnel; Produktion wird später über einen hostseitigen Reverse Proxy wie Caddy veröffentlicht.

Die Laufzeitdaten liegen bewusst außerhalb des Git-Checkouts:

```text
/opt/mise-en-dice/
├── repository/                 Git-Checkout und Deployment-Werkzeug
└── runtime/                    nicht versionierter Betriebszustand
    ├── admin.properties        BCrypt-Hash und Admin-Identität
    ├── discord.properties      optionales Bot-Secret, nur Produktion
    ├── openai.properties       optionales OpenAI-Secret, nur Produktion
    ├── acceptance.properties   Discord-Testbot und OpenAI-Testprojekt, nur Acceptance
    ├── operator.conf           Ports, SSH-Hinweis und Ressourcenlimits
    ├── instances/
    │   ├── production/
    │   ├── acceptance/
    │   └── previews/<name>/
    ├── backups/
    ├── worktrees/
    ├── locks/
    └── tmp/
```

## 2. Voraussetzungen

Auf dem VPS müssen vorhanden sein:

- Docker Engine mit Docker Compose v2,
- Git,
- `curl`, `openssl`, `flock` und `ss`,
- `apache2-utils` für die einmalige BCrypt-Erzeugung,
- ein normaler Betriebsbenutzer mit Zugriff auf den Docker-Daemon.

Das Werkzeug verweigert den Betrieb als `root`. `sudo` wird nur verwendet, um Verzeichnisse und Systemdienste einmalig einzurichten. Danach laufen Deployments als normaler Betriebsbenutzer.

Beispiel für Debian:

```bash
sudo apt update
sudo apt install --yes git curl openssl apache2-utils util-linux iproute2

docker --version
docker compose version
docker info
```

Schlägt `docker info` wegen fehlender Berechtigung fehl, muss der Betriebsbenutzer korrekt für Docker eingerichtet werden. Danach neu anmelden; ein halb aktualisiertes Gruppenmitglied ist ein erstaunlich zuverlässiger Lieferant sinnloser Fehlersuche.

## 3. Einmalige Installation

### 3.1 Verzeichnisse und Repository

Als vorhandener Betriebsbenutzer anmelden und nur das Elternverzeichnis mit `sudo` anlegen:

```bash
sudo install -d -m 0750 -o "$(id -un)" -g "$(id -gn)" /opt/mise-en-dice

git clone https://github.com/venomenon328/mise-en-dice.git \
  /opt/mise-en-dice/repository

cd /opt/mise-en-dice/repository
```

Das Laufzeitverzeichnis für die aktuelle Shell festlegen:

```bash
export MISE_EN_DICE_DEPLOY_ROOT=/opt/mise-en-dice/runtime
```

Optional dauerhaft für diesen Benutzer:

```bash
printf '%s\n' \
  'export MISE_EN_DICE_DEPLOY_ROOT=/opt/mise-en-dice/runtime' \
  >> "$HOME/.profile"
```

### 3.2 SSH-Hinweis und Ports setzen

Diese Werte werden nur verwendet, um nach einem Preview-Deployment einen kopierbaren Tunnelbefehl auszugeben. Sie öffnen selbst keinerlei Port.

```bash
export MISE_EN_DICE_SSH_USER=dein-server-benutzer
export MISE_EN_DICE_SSH_HOST=deine-server-ip-oder-domain
export MISE_EN_DICE_PRODUCTION_PORT=18080
export MISE_EN_DICE_ACCEPTANCE_PORT=18090
export MISE_EN_DICE_PREVIEW_PORT_START=18100
export MISE_EN_DICE_PREVIEW_PORT_END=18199
```

Die Bereiche dürfen geändert werden, müssen aber frei und unprivilegiert sein. Produktions- und Acceptance-Port müssen verschieden sein und dürfen nicht im Preview-Bereich liegen. Das Werkzeug prüft sowohl bereits reservierte Preview-Ports als auch tatsächlich lauschende Hostprozesse.

### 3.3 Runtime initialisieren

```bash
./deploy/mise-en-dice.sh init
```

Das Werkzeug fragt nach:

1. einer stabilen Admin-Benutzerkennung, beispielsweise `tobias`,
2. einem Anzeigenamen,
3. dem Admin-Passwort mit Wiederholung.

Nur der BCrypt-Hash wird gespeichert. Klartextpasswort, Datenbankpasswörter und Laufzeitdateien landen nicht im Repository.

Danach die vollständige Vorprüfung ausführen:

```bash
./deploy/mise-en-dice.sh doctor
```

`doctor` prüft unter anderem Docker, Compose, das Git-Remote, die Runtime-Konfiguration und die syntaktische Gültigkeit des Deployment-Compose.

## 4. Zuerst gefahrlos die aktuelle Website ansehen

Die erste Besichtigung sollte als Preview von `main` erfolgen. Dabei bleiben Produktion und deren Secure-Cookie-Konfiguration vollständig außen vor.

```bash
cd /opt/mise-en-dice/repository
export MISE_EN_DICE_DEPLOY_ROOT=/opt/mise-en-dice/runtime

./deploy/mise-en-dice.sh preview deploy main
```

Das Werkzeug erledigt dabei automatisch:

1. `origin` aktualisieren und den exakten Commit auflösen,
2. einen temporären Git-Worktree erzeugen,
3. das Java-21-Image bauen oder ein identisches bereits gebautes Commit-Image wiederverwenden,
4. einen freien Port aus dem Preview-Bereich wählen,
5. ein separates Compose-Projekt mit eigener PostgreSQL-Datenbank starten,
6. Liquibase gegen die leere Datenbank laufen lassen,
7. Healthcheck und geschützten Admin-Einstieg prüfen,
8. Port, URL und SSH-Tunnel ausgeben.

Beispielausgabe:

```text
PREVIEW_NAME=main
APP_PORT=18100
URL=http://localhost:18100/admin
SSH_TUNNEL=ssh -N -L 18100:127.0.0.1:18100 user@example.org
```

Auf dem eigenen Rechner den ausgegebenen SSH-Tunnel öffnen und dieses Fenster geöffnet lassen:

```bash
ssh -N -L 18100:127.0.0.1:18100 user@example.org
```

Danach im Browser öffnen:

```text
http://localhost:18100/admin
```

Unter Windows PowerShell mit explizitem Schlüssel beispielsweise:

```powershell
ssh -N `
  -L 18100:127.0.0.1:18100 `
  -i $env:USERPROFILE\.ssh\dein_server_key `
  user@example.org
```

## 5. Beliebige Branches und Commits prüfen

Ein Remote-Branch wird mit genau einem Befehl ausgerollt:

```bash
./deploy/mise-en-dice.sh preview deploy feat/12-catalog-ui-polish
```

Auch Tags und Commit-SHAs funktionieren:

```bash
./deploy/mise-en-dice.sh preview deploy v0.1.0
./deploy/mise-en-dice.sh preview deploy a4245d7d0023529ebfa8d8d087ce2d237bf94c6a
```

Der automatisch gebildete Preview-Name wird aus dem Ref normalisiert. Ein expliziter kurzer Name ist möglich:

```bash
./deploy/mise-en-dice.sh preview deploy feat/12-catalog-ui-polish ui12
```

Wird derselbe Preview-Name erneut deployt, bleiben dessen Port und Datenbankvolume erhalten; nur das Anwendungsimage wird auf den neu aufgelösten Commit aktualisiert. Verschiedene Previews teilen niemals ein Datenbankvolume.

Alle Previews anzeigen:

```bash
./deploy/mise-en-dice.sh preview list
```

Status und Logs:

```bash
./deploy/mise-en-dice.sh preview status ui12
./deploy/mise-en-dice.sh preview logs ui12
./deploy/mise-en-dice.sh preview logs ui12 --follow
```

Stoppen und später wieder starten:

```bash
./deploy/mise-en-dice.sh preview stop ui12
./deploy/mise-en-dice.sh preview start ui12
```

Eine Preview einschließlich Datenbankvolume vollständig entfernen:

```bash
./deploy/mise-en-dice.sh preview remove ui12
```

Interaktiv muss zur Sicherheit `REMOVE-ui12` bestätigt werden. In CI ist stattdessen ausdrücklich `--yes` nötig:

```bash
./deploy/mise-en-dice.sh preview remove ui12 --yes
```

## 6. Produktion deployen

Produktion erst ausrollen, wenn die Domain beziehungsweise der Reverse Proxy vorbereitet werden soll:

```bash
./deploy/mise-en-dice.sh production deploy main
```

### 6.1 Produktive Provider konfigurieren

Der Bot bleibt deaktiviert, bis eine nur für den Betriebsbenutzer lesbare Datei
`/opt/mise-en-dice/runtime/discord.properties` angelegt wird (`chmod 0600`). Sie wird ausschließlich in die
Produktionsinstanz übernommen, niemals in Acceptance, Previews oder den Produktions-Smoke-Test:

```properties
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=BOT_TOKEN_AUSSERHALB_DES_REPOSITORIES
mise-en-dice.discord.guild-id=DEINE_PRIVATE_GUILD_ID
mise-en-dice.discord.challenge-operator-role-id=CHALLENGE_OPERATOR_ROLE_ID
mise-en-dice.discord.effective-date-zone=Europe/Berlin
# Optionaler, einmaliger Legacy-Bootstrap. Nach der erfolgreichen Übernahme dürfen diese
# Einträge entfernt werden; PostgreSQL bleibt dann die alleinige Runtime-Autorität.
mise-en-dice.discord.participant-user-ids.GEORGIA=DISCORD_USER_ID
mise-en-dice.discord.participant-user-ids.TOBIAS=DISCORD_USER_ID
```

`challenge-operator-role-id` ist die ID einer normalen Discord-Rolle, deren Mitglieder `/challenge` starten und `/teilnehmer` verwalten dürfen. Sie benötigt keine Discord-`Administrator`-Berechtigung und ist fachlich unabhängig von Teilnehmern und Standard-Elektorat: Ein Teilnehmer darf die Commands ohne diese Rolle nicht ausführen; ein Rollenmitglied muss umgekehrt weder Teilnehmer noch Elektoratsmitglied sein. `/zutat` bleibt für jedes Mitglied der konfigurierten Guild nutzbar.

Zusätzlich kann `/opt/mise-en-dice/runtime/openai.properties` angelegt werden (`chmod 0600`):

```properties
mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.api-key=OPENAI_PRODUKTIVKEY_AUSSERHALB_DES_REPOSITORIES
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
```

Die Dateien sind optional; vorhandene Installationen benötigen weder eine neue Datei noch `init --force`. Ohne sie
bleiben die jeweiligen Adapter deaktiviert. Sobald `discord.properties` Discord aktiviert, sind Token, Guild-ID und
Challenge-Operator-Rollen-ID vollständig zu setzen. Die Georgia-/Tobias-Participant-Mappings sind ausschließlich ein
optional kontrollierter Bootstrap: passende DB-Zuordnungen bleiben unverändert, widersprüchliche Zuordnungen verhindern
den Start sichtbar und überschreiben nichts. Nach erfolgreicher Übernahme können sie entfernt werden; neue Discord-
Teilnehmer benötigen niemals einen Property-Eintrag.
Aktiviert `openai.properties` den Kurator, setzt der Operator das `production`-Profil explizit.
`acceptance.properties` wird nie in Produktion übernommen. Tokens, API-Keys und IDs gehören weder in `.env`, Commit,
Statusausgabe noch Logs.

Vor dem eigentlichen Umschalten führt der Operator einen vollständigen Smoke-Test des gebauten Images mit einer frischen temporären PostgreSQL-Datenbank aus. Existiert bereits eine Produktion, wird danach automatisch ein validiertes Backup erzeugt. Erst dann wird das feste Projekt `med-production` aktualisiert.

Bei einem fehlgeschlagenen Healthcheck werden Logs ausgegeben und der vorherige App-Stand wieder gestartet. Eine bereits ausgeführte Datenbankmigration kann dabei bewusst nicht automatisch zurückgerollt werden. Das wäre keine Sicherheitsfunktion, sondern Datenbankroulette mit hübscher Konsolenausgabe. Deshalb gibt es vor dem Wechsel das frische Smoke-System und das Produktionsbackup.

Status, Logs, Stop und Start:

```bash
./deploy/mise-en-dice.sh production status
./deploy/mise-en-dice.sh production logs
./deploy/mise-en-dice.sh production logs --follow
./deploy/mise-en-dice.sh production stop
./deploy/mise-en-dice.sh production start
```

Ein automatisches `production remove --volumes` existiert absichtlich nicht.

## 7. Feste Live-Acceptance

Acceptance ist die einzige erlaubte Live-Testinstanz. Sie erhält einen eigenen PostgreSQL-Datenträger, das feste
Compose-Projekt `med-acceptance`, Port `18090` (oder `ACCEPTANCE_PORT` aus `operator.conf`) und keine öffentliche
Domain. Der Zugriff erfolgt ausschließlich über einen SSH-Tunnel; die Session-Cookies sind dort bewusst nicht
`Secure`.

Die Datei `/opt/mise-en-dice/runtime/acceptance.properties` ist verpflichtend, regulär, kein Symlink und höchstens
`0600`. Sie enthält nur die folgenden Providerproperties, beispielsweise für den echten manuellen Live-Test:

```properties
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=ACCEPTANCE_TESTBOT_TOKEN
mise-en-dice.discord.guild-id=ACCEPTANCE_GUILD_ID
mise-en-dice.discord.challenge-operator-role-id=ACCEPTANCE_CHALLENGE_OPERATOR_ROLE_ID
mise-en-dice.discord.effective-date-zone=Europe/Berlin
# Optionaler Legacy-Bootstrap, nicht Teil der Laufzeitautorisierung.
mise-en-dice.discord.participant-user-ids.GEORGIA=ACCEPTANCE_GEORGIA_ID
mise-en-dice.discord.participant-user-ids.TOBIAS=ACCEPTANCE_TOBIAS_ID

mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.api-key=ACCEPTANCE_OPENAI_PROJECT_KEY
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
```

Die Discord-Guild- und Rollen-IDs müssen positiv sein. Falls Legacy-User-IDs für `GEORGIA` oder `TOBIAS` angegeben
werden, müssen sie positiv und bei zwei Einträgen verschieden sein. Die Operator-Rolle steuert ausschließlich
`/challenge` und `/teilnehmer`, nicht das Electorate. Für
einen rein technischen, providerfreien Betriebscheck können beide Adapter explizit deaktiviert werden; Token und
API-Key dürfen dann nicht in der Datei stehen:

```properties
mise-en-dice.discord.enabled=false
mise-en-dice.curation.openai.enabled=false
```

Vor jedem Live-Deployment führt der Operator die secretfreie Prüfung aus. Sie lehnt unsichere Dateien, unvollständige
Aktivierung, ungültige Guild-/Rollen-/User-IDs, Portkollisionen und gleiche Acceptance-/Produktions-Token beziehungsweise API-Keys ab,
bevor Docker gestartet wird:

```bash
chmod 0600 /opt/mise-en-dice/runtime/acceptance.properties
./deploy/mise-en-dice.sh acceptance preflight
./deploy/mise-en-dice.sh acceptance deploy main
```

Status, Logs, Stop und Start:

```bash
./deploy/mise-en-dice.sh acceptance status
./deploy/mise-en-dice.sh acceptance logs
./deploy/mise-en-dice.sh acceptance logs --follow
./deploy/mise-en-dice.sh acceptance stop
./deploy/mise-en-dice.sh acceptance start
```

`status` zeigt nur Aktivierungszustände sowie Modell und Reasoning-Stufe, niemals Secretwerte. Eine Acceptance-DB
kann mit genau einer read-only Query ohne Semikolon inspiziert werden; schreibende SQL-Befehle werden abgewiesen:

```bash
./deploy/mise-en-dice.sh acceptance sql \
  'SELECT status, count(*) FROM curation_round GROUP BY status'
```

Backup und vollständiger Reset betreffen nur Acceptance. Vor einem Reset bei Bedarf zuerst sichern; der Reset behält
`acceptance.properties` für ein späteres Neuaufsetzen, entfernt aber App, Datenbankvolume und Instanzmetadaten:

```bash
./deploy/mise-en-dice.sh acceptance backup
./deploy/mise-en-dice.sh acceptance reset
# Nichtinteraktiv ausschließlich bewusst:
./deploy/mise-en-dice.sh acceptance reset --yes
```

Ein Acceptance-Backup darf ausschließlich in eine normale Preview restauriert werden. Dort bleiben Discord und
OpenAI ausdrücklich deaktiviert. Es gibt keinen Restore-Befehl mit Produktionsziel.

Die vollständigen manuellen Testfälle, Kostenprotokolle und der Notfallablauf stehen in
[`PRODUCTION_VALIDATION.md`](PRODUCTION_VALIDATION.md).

## 8. Domain und Caddy

Das Repository enthält unter [`deploy/Caddyfile.example`](../deploy/Caddyfile.example) ein minimales Beispiel:

```caddyfile
dice.example.de {
    encode zstd gzip
    reverse_proxy 127.0.0.1:18080
}
```

Vorgehen:

1. A-Record der gewünschten Subdomain auf die öffentliche VPS-IP setzen.
2. Caddy auf dem Host installieren beziehungsweise in die vorhandene Proxy-Konfiguration integrieren.
3. Den Hostnamen im Caddyfile ersetzen.
4. Öffentlich nur HTTP/HTTPS freigeben; Port 18080 bleibt loopback-only.
5. Caddy-Konfiguration prüfen und neu laden.

Die Produktionsinstanz verwendet `Secure`-Session-Cookies und ist deshalb für die echte Anmeldung über HTTPS gedacht. Für einen reinen SSH-/HTTP-Test weiterhin eine Preview von `main` verwenden.

## 9. Produktions- und Acceptance-Backups

Manuelles, konsistentes PostgreSQL-Backup im Custom-Format für Produktion:

```bash
./deploy/mise-en-dice.sh production backup
```

Der Operator:

- schreibt zunächst eine versteckte `.partial`-Datei,
- erzeugt `pg_dump --format=custom` innerhalb des PostgreSQL-Containers,
- prüft das Archiv mit `pg_restore --list`,
- verschiebt es erst danach atomar auf den endgültigen Namen,
- erzeugt eine SHA-256-Prüfsumme,
- entfernt nach einem erfolgreichen Backup ältere Archive gemäß Aufbewahrungsfrist.

Ablage:

```text
/opt/mise-en-dice/runtime/backups/
```

Acceptance verwendet denselben validierten Ablauf, kennzeichnet die Datei aber eindeutig mit
`mise-en-dice-acceptance-...`:

```bash
./deploy/mise-en-dice.sh acceptance backup
```

Die lokale Sicherung schützt nicht gegen Verlust des gesamten VPS. Ein verschlüsselter Offsite-Transfer muss zusätzlich eingerichtet werden.

## 10. Backup sicher prüfen oder wiederherstellen

Der Operator erlaubt Restore ausschließlich in eine ausdrücklich benannte Preview. Ein versehentliches Überschreiben der laufenden Produktion wird dadurch ausgeschlossen.

Ziel-Preview anlegen:

```bash
./deploy/mise-en-dice.sh preview deploy main restore-test
```

Backup einspielen:

```bash
./deploy/mise-en-dice.sh preview restore \
  restore-test \
  /opt/mise-en-dice/runtime/backups/mise-en-dice-YYYYMMDDTHHMMSSZ-COMMIT.dump
```

Interaktiv muss `RESTORE-restore-test` bestätigt werden. Der Operator prüft eine vorhandene SHA-256-Datei, validiert das Archiv, stoppt nur die Preview-App, baut deren Datenbank neu auf, spielt das Archiv ein und startet die App mit Healthcheck wieder.

Für CI beziehungsweise bewusst nichtinteraktive Läufe:

```bash
./deploy/mise-en-dice.sh preview restore restore-test /pfad/backup.dump --yes
```

Nach erfolgreicher Prüfung kann die Restore-Preview wieder vollständig entfernt werden.

## 11. Regelmäßiges Backup per systemd

Beispiele liegen unter [`deploy/systemd`](../deploy/systemd). Benutzer und Pfade prüfen, dann installieren:

```bash
sudo cp deploy/systemd/mise-en-dice-backup.service /etc/systemd/system/
sudo cp deploy/systemd/mise-en-dice-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now mise-en-dice-backup.timer
```

Prüfen:

```bash
systemctl list-timers mise-en-dice-backup.timer
sudo systemctl start mise-en-dice-backup.service
journalctl -u mise-en-dice-backup.service --since today
```

Der Timer startet standardmäßig nachts um 03:20 Uhr mit kleiner zufälliger Verzögerung.

## 12. Repository und Deployment-Werkzeug aktualisieren

Das Operator-Skript baut beliebige Remote-Refs, stammt selbst aber aus dem lokalen Checkout. Änderungen am Deployment-Werkzeug werden daher zuerst per Fast-Forward übernommen:

```bash
cd /opt/mise-en-dice/repository
git switch main
git pull --ff-only
./deploy/mise-en-dice.sh doctor
```

Danach können erneut `main`, Branches oder Commits deployt werden. Das Werkzeug verändert den aktuell ausgecheckten Branch nicht; Builds erfolgen in temporären, detached Worktrees.

## 13. Direkte Preview-Datenbankdiagnose

Nur für Diagnose und CI steht ein direkter SQL-Befehl auf Preview-Datenbanken bereit:

```bash
./deploy/mise-en-dice.sh preview sql ui12 \
  'SELECT count(*) FROM ingredient_concept;'
```

Es gibt bewusst keinen entsprechenden Produktionsbefehl. Redaktionelle Produktionsänderungen gehören in die Anwendung, nicht in improvisierte Shell-SQL-Sitzungen um 02:17 Uhr.

## 14. Fehlerdiagnose

### Preview startet nicht

```bash
./deploy/mise-en-dice.sh preview status <name>
./deploy/mise-en-dice.sh preview logs <name>
```

Bei einem fehlgeschlagenen neuen Deployment gibt der Operator bereits die letzten App- und PostgreSQL-Logs aus und verbucht die Instanz nicht als erfolgreich.

### Portbereich ist voll

```bash
./deploy/mise-en-dice.sh preview list
sudo ss -ltnp
```

Nicht mehr benötigte Previews vollständig entfernen oder den Portbereich in `/opt/mise-en-dice/runtime/operator.conf` bewusst erweitern.

### Docker-Berechtigung fehlt

```bash
docker info
id
```

Nicht das Deployment mit `sudo` ausführen. Die Docker-Berechtigung des Betriebsbenutzers korrigieren und neu anmelden.

### Konfiguration beschädigt

```bash
./deploy/mise-en-dice.sh doctor
```

`init --force` ersetzt Admin- und Operator-Konfiguration und sollte nur bewusst eingesetzt werden. Datenbankvolumes werden dadurch nicht gelöscht, vorhandene Instanzen benötigen danach aber beim nächsten Deployment die neue Admin-Konfiguration.

### Speicher prüfen

```bash
free -h
docker stats --no-stream
```

Die Standardlimits betragen 768 MB für die App und 512 MB für PostgreSQL je laufender Instanz. Viele parallele Previews sind auf einem kleinen VPS daher weniger „Cloud“ als „warum swappt das jetzt?“. Nicht benötigte Previews stoppen oder entfernen.
