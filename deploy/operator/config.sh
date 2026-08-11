#!/usr/bin/env bash

# Runtime initialization and external operator configuration.

usage() {
    cat <<'USAGE'
Mise en Dice Deployment Operator

Einmalig:
  mise-en-dice.sh init [--non-interactive] [--force]
  mise-en-dice.sh doctor

Produktion:
  mise-en-dice.sh production deploy [git-ref]
  mise-en-dice.sh production status
  mise-en-dice.sh production logs [--follow]
  mise-en-dice.sh production stop
  mise-en-dice.sh production start
  mise-en-dice.sh production backup

Branch-/Commit-Previews:
  mise-en-dice.sh preview deploy <git-ref> [preview-name]
  mise-en-dice.sh preview list
  mise-en-dice.sh preview status <preview-name>
  mise-en-dice.sh preview logs <preview-name> [--follow]
  mise-en-dice.sh preview stop <preview-name>
  mise-en-dice.sh preview start <preview-name>
  mise-en-dice.sh preview sql <preview-name> <sql>
  mise-en-dice.sh preview restore <preview-name> <backup.dump> [--yes]
  mise-en-dice.sh preview remove <preview-name> [--yes]

Die Laufzeitwurzel ist standardmäßig /opt/mise-en-dice/runtime und kann mit
MISE_EN_DICE_DEPLOY_ROOT überschrieben werden. Sie muss außerhalb des Git-Checkouts liegen.
USAGE
}

acquire_lock() {
    mkdir -p "$LOCKS_DIR"
    exec 9>"$LOCKS_DIR/operator.lock"
    flock -w 60 9 || med_die 'Ein anderer Deployment-Vorgang läuft bereits.'
}

ensure_runtime_root_is_valid() {
    med_valid_absolute_runtime_path "$DEPLOY_ROOT" \
        || med_die 'MISE_EN_DICE_DEPLOY_ROOT muss ein normalisierter absoluter Pfad aus Buchstaben, Ziffern, Punkt, Unterstrich, Bindestrich und Slash sein.'

    command -v realpath >/dev/null 2>&1 || med_die 'Benötigtes Kommando fehlt: realpath'
    local canonical_root canonical_repository
    canonical_root=$(realpath -m -- "$DEPLOY_ROOT")
    canonical_repository=$(realpath -m -- "$REPOSITORY_ROOT")
    [[ $canonical_root == "$DEPLOY_ROOT" ]] \
        || med_die 'MISE_EN_DICE_DEPLOY_ROOT darf keine Symlink-, Punkt- oder Elternpfad-Abkürzungen enthalten.'

    case "$canonical_root/" in
        "$canonical_repository/"*)
            med_die 'Die Laufzeitwurzel muss außerhalb des Git-Checkouts liegen.'
            ;;
    esac
}

ensure_runtime_initialized() {
    ensure_runtime_root_is_valid
    [[ -f $OPERATOR_CONFIG ]] || med_die "Nicht initialisiert. Zuerst ausführen: $0 init"
    [[ -f $ADMIN_PROPERTIES ]] || med_die "Administrationskonfiguration fehlt: $ADMIN_PROPERTIES"
    # shellcheck disable=SC1090
    source "$OPERATOR_CONFIG"

    med_valid_port "$PRODUCTION_PORT" || med_die 'Ungültiger PRODUCTION_PORT in operator.conf.'
    med_valid_port "$PREVIEW_PORT_START" || med_die 'Ungültiger PREVIEW_PORT_START in operator.conf.'
    med_valid_port "$PREVIEW_PORT_END" || med_die 'Ungültiger PREVIEW_PORT_END in operator.conf.'
    (( PREVIEW_PORT_START <= PREVIEW_PORT_END )) || med_die 'Der Preview-Portbereich ist vertauscht.'
    (( PRODUCTION_PORT < PREVIEW_PORT_START || PRODUCTION_PORT > PREVIEW_PORT_END )) \
        || med_die 'Der Produktionsport darf nicht im Preview-Portbereich liegen.'
    [[ $BACKUP_RETENTION_DAYS =~ ^[0-9]+$ ]] || med_die 'BACKUP_RETENTION_DAYS muss eine Zahl sein.'
    [[ $HEALTH_TIMEOUT_SECONDS =~ ^[1-9][0-9]*$ ]] || med_die 'HEALTH_TIMEOUT_SECONDS muss eine positive Zahl sein.'
    med_valid_memory_limit "$APP_MEMORY_LIMIT" || med_die 'APP_MEMORY_LIMIT muss zum Beispiel 768m oder 1g sein.'
    med_valid_memory_limit "$POSTGRES_MEMORY_LIMIT" || med_die 'POSTGRES_MEMORY_LIMIT muss zum Beispiel 512m oder 1g sein.'
    med_valid_ssh_user "$SSH_USER" || med_die 'SSH_USER enthält ungültige Zeichen.'
    med_valid_ssh_host "$SSH_HOST" || med_die 'SSH_HOST enthält ungültige Zeichen.'
}

check_base_commands() {
    (( $(id -u) != 0 )) || med_die 'Das Deployment-Werkzeug darf nicht als root laufen. Verzeichnisrechte einmalig mit sudo setzen, danach als Betriebsbenutzer arbeiten.'
    med_require_command git
    med_require_command docker
    med_require_command curl
    med_require_command openssl
    med_require_command flock
    med_require_command ss
    med_require_command sha256sum
    docker compose version >/dev/null 2>&1 || med_die 'Docker Compose v2 ist nicht verfügbar.'
    docker info >/dev/null 2>&1 || med_die 'Der Docker-Daemon ist nicht erreichbar.'
}

write_operator_config() {
    local temporary_file=$1
    : > "$temporary_file"
    med_write_shell_assignment "$temporary_file" SSH_USER "$MISE_EN_DICE_SSH_USER_VALUE"
    med_write_shell_assignment "$temporary_file" SSH_HOST "$MISE_EN_DICE_SSH_HOST_VALUE"
    med_write_shell_assignment "$temporary_file" PRODUCTION_PORT "$MISE_EN_DICE_PRODUCTION_PORT_VALUE"
    med_write_shell_assignment "$temporary_file" PREVIEW_PORT_START "$MISE_EN_DICE_PREVIEW_PORT_START_VALUE"
    med_write_shell_assignment "$temporary_file" PREVIEW_PORT_END "$MISE_EN_DICE_PREVIEW_PORT_END_VALUE"
    med_write_shell_assignment "$temporary_file" BACKUP_RETENTION_DAYS "$MISE_EN_DICE_BACKUP_RETENTION_DAYS_VALUE"
    med_write_shell_assignment "$temporary_file" HEALTH_TIMEOUT_SECONDS "$MISE_EN_DICE_HEALTH_TIMEOUT_SECONDS_VALUE"
    med_write_shell_assignment "$temporary_file" APP_MEMORY_LIMIT "${MISE_EN_DICE_APP_MEMORY_LIMIT_VALUE:-768m}"
    med_write_shell_assignment "$temporary_file" POSTGRES_MEMORY_LIMIT "${MISE_EN_DICE_POSTGRES_MEMORY_LIMIT_VALUE:-512m}"
    chmod 0600 "$temporary_file"
}

write_admin_properties() {
    local temporary_file=$1
    local actor_key=$2
    local display_name=$3
    local password_hash=$4

    cat > "$temporary_file" <<EOF_PROPERTIES
mise-en-dice.administration.enabled=true
mise-en-dice.administration.accounts[0].actor-key=$(med_properties_escape "$actor_key")
mise-en-dice.administration.accounts[0].display-name=$(med_properties_escape "$display_name")
mise-en-dice.administration.accounts[0].password-hash=$(med_properties_escape "$password_hash")
EOF_PROPERTIES
    chmod 0600 "$temporary_file"
}

command_init() {
    local non_interactive=false
    local force=false
    local argument

    for argument in "$@"; do
        case "$argument" in
            --non-interactive) non_interactive=true ;;
            --force) force=true ;;
            *) med_die "Unbekannte init-Option: $argument" ;;
        esac
    done

    ensure_runtime_root_is_valid
    check_base_commands

    if ! mkdir -p "$DEPLOY_ROOT" 2>/dev/null; then
        med_die "Kann $DEPLOY_ROOT nicht anlegen. Einmalig ausführen: sudo install -d -m 0750 -o \"$(id -un)\" -g \"$(id -gn)\" \"$DEPLOY_ROOT\""
    fi
    acquire_lock

    if [[ -e $OPERATOR_CONFIG || -e $ADMIN_PROPERTIES ]] && [[ $force != true ]]; then
        med_die 'Die Laufzeitwurzel ist bereits initialisiert. --force würde die Operator- und Admin-Konfiguration ersetzen.'
    fi

    mkdir -p "$PREVIEWS_DIR" "$PRODUCTION_DIR" "$WORKTREES_DIR" "$BACKUPS_DIR" "$TEMP_DIR"
    chmod 0750 "$DEPLOY_ROOT" "$INSTANCES_DIR" "$PREVIEWS_DIR" "$PRODUCTION_DIR" "$WORKTREES_DIR" "$BACKUPS_DIR" "$TEMP_DIR" "$LOCKS_DIR"

    local actor_key display_name password_hash password password_repeat
    if [[ $non_interactive == true ]]; then
        actor_key=${MISE_EN_DICE_ADMIN_ACTOR_KEY:-}
        display_name=${MISE_EN_DICE_ADMIN_DISPLAY_NAME:-}
        password_hash=${MISE_EN_DICE_ADMIN_PASSWORD_HASH:-}
        [[ -n $actor_key && -n $display_name && -n $password_hash ]] \
            || med_die 'Für --non-interactive sind MISE_EN_DICE_ADMIN_ACTOR_KEY, _DISPLAY_NAME und _PASSWORD_HASH erforderlich.'
    else
        med_require_command htpasswd
        read -r -p 'Admin-Benutzerkennung [tobias]: ' actor_key
        actor_key=${actor_key:-tobias}
        read -r -p 'Anzeigename [Tobias]: ' display_name
        display_name=${display_name:-Tobias}
        while true; do
            read -r -s -p 'Admin-Passwort: ' password
            printf '\n' >&2
            read -r -s -p 'Admin-Passwort wiederholen: ' password_repeat
            printf '\n' >&2
            [[ -n $password && $password == "$password_repeat" ]] && break
            med_warn 'Die Passwörter sind leer oder stimmen nicht überein.'
        done
        password_hash=$(printf '%s\n' "$password" | htpasswd -n -i -B -C 12 "$actor_key" | cut -d: -f2-)
        unset password password_repeat
    fi

    med_valid_actor_key "$actor_key" \
        || med_die 'Die Admin-Benutzerkennung darf nur Buchstaben, Ziffern, Unterstrich und Bindestrich enthalten.'
    [[ -n $display_name && $display_name != *$'\n'* && $display_name != *$'\r'* ]] \
        || med_die 'Der Anzeigename darf nicht leer sein oder Zeilenumbrüche enthalten.'
    med_valid_bcrypt_hash "$password_hash" || med_die 'Der Admin-Passworthash ist kein gültiger BCrypt-Hash.'

    MISE_EN_DICE_SSH_USER_VALUE=${MISE_EN_DICE_SSH_USER:-$(id -un)}
    MISE_EN_DICE_SSH_HOST_VALUE=${MISE_EN_DICE_SSH_HOST:-SERVER_IP}
    MISE_EN_DICE_PRODUCTION_PORT_VALUE=${MISE_EN_DICE_PRODUCTION_PORT:-18080}
    MISE_EN_DICE_PREVIEW_PORT_START_VALUE=${MISE_EN_DICE_PREVIEW_PORT_START:-18100}
    MISE_EN_DICE_PREVIEW_PORT_END_VALUE=${MISE_EN_DICE_PREVIEW_PORT_END:-18199}
    MISE_EN_DICE_BACKUP_RETENTION_DAYS_VALUE=${MISE_EN_DICE_BACKUP_RETENTION_DAYS:-14}
    MISE_EN_DICE_HEALTH_TIMEOUT_SECONDS_VALUE=${MISE_EN_DICE_HEALTH_TIMEOUT_SECONDS:-180}
    MISE_EN_DICE_APP_MEMORY_LIMIT_VALUE=${MISE_EN_DICE_APP_MEMORY_LIMIT:-768m}
    MISE_EN_DICE_POSTGRES_MEMORY_LIMIT_VALUE=${MISE_EN_DICE_POSTGRES_MEMORY_LIMIT:-512m}

    med_valid_port "$MISE_EN_DICE_PRODUCTION_PORT_VALUE" || med_die 'Ungültiger Produktionsport.'
    med_valid_port "$MISE_EN_DICE_PREVIEW_PORT_START_VALUE" || med_die 'Ungültiger Start des Preview-Portbereichs.'
    med_valid_port "$MISE_EN_DICE_PREVIEW_PORT_END_VALUE" || med_die 'Ungültiges Ende des Preview-Portbereichs.'
    (( MISE_EN_DICE_PREVIEW_PORT_START_VALUE <= MISE_EN_DICE_PREVIEW_PORT_END_VALUE )) \
        || med_die 'Der Preview-Portbereich ist vertauscht.'
    (( MISE_EN_DICE_PRODUCTION_PORT_VALUE < MISE_EN_DICE_PREVIEW_PORT_START_VALUE \
        || MISE_EN_DICE_PRODUCTION_PORT_VALUE > MISE_EN_DICE_PREVIEW_PORT_END_VALUE )) \
        || med_die 'Der Produktionsport darf nicht im Preview-Portbereich liegen.'
    [[ $MISE_EN_DICE_BACKUP_RETENTION_DAYS_VALUE =~ ^[0-9]+$ ]] \
        || med_die 'Die Backup-Aufbewahrung muss eine Zahl von Tagen sein.'
    [[ $MISE_EN_DICE_HEALTH_TIMEOUT_SECONDS_VALUE =~ ^[1-9][0-9]*$ ]] \
        || med_die 'Das Health-Timeout muss eine positive Zahl sein.'
    med_valid_memory_limit "$MISE_EN_DICE_APP_MEMORY_LIMIT_VALUE" \
        || med_die 'Das App-Speicherlimit muss zum Beispiel 768m oder 1g sein.'
    med_valid_memory_limit "$MISE_EN_DICE_POSTGRES_MEMORY_LIMIT_VALUE" \
        || med_die 'Das PostgreSQL-Speicherlimit muss zum Beispiel 512m oder 1g sein.'
    med_valid_ssh_user "$MISE_EN_DICE_SSH_USER_VALUE" || med_die 'Der SSH-Benutzer enthält ungültige Zeichen.'
    med_valid_ssh_host "$MISE_EN_DICE_SSH_HOST_VALUE" || med_die 'Der SSH-Host enthält ungültige Zeichen.'

    local operator_tmp admin_tmp
    operator_tmp=$(mktemp "$TEMP_DIR/operator.XXXXXX")
    admin_tmp=$(mktemp "$TEMP_DIR/admin.XXXXXX")
    write_operator_config "$operator_tmp"
    write_admin_properties "$admin_tmp" "$actor_key" "$display_name" "$password_hash"
    mv -f "$operator_tmp" "$OPERATOR_CONFIG"
    mv -f "$admin_tmp" "$ADMIN_PROPERTIES"
    chmod 0600 "$OPERATOR_CONFIG" "$ADMIN_PROPERTIES"

    med_note "Laufzeitwurzel initialisiert: $DEPLOY_ROOT"
    med_note "Produktionsport: $MISE_EN_DICE_PRODUCTION_PORT_VALUE"
    med_note "Preview-Ports: $MISE_EN_DICE_PREVIEW_PORT_START_VALUE-$MISE_EN_DICE_PREVIEW_PORT_END_VALUE"
    med_note "Nächster Schritt: $0 doctor"
}

