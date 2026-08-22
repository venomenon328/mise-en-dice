#!/usr/bin/env bash

# Compose instance lifecycle, health checks, ports and rollback behavior.

properties_value() {
    local file=$1
    local wanted_key=$2
    awk -v wanted_key="$wanted_key" '
        {
            line = $0
            sub(/\r$/, "", line)
            sub(/^[[:space:]]+/, "", line)
            if (line == "" || line ~ /^[#!]/) next
            separator = index(line, "=")
            if (separator == 0) next
            key = substr(line, 1, separator - 1)
            value = substr(line, separator + 1)
            sub(/[[:space:]]+$/, "", key)
            sub(/^[[:space:]]+/, "", value)
            sub(/[[:space:]]+$/, "", value)
            if (key == wanted_key) {
                result = value
                found = 1
            }
        }
        END {
            if (found) printf "%s", result
        }
    ' "$file"
}

properties_has_key() {
    local file=$1
    local wanted_key=$2
    awk -v wanted_key="$wanted_key" '
        {
            line = $0
            sub(/\r$/, "", line)
            sub(/^[[:space:]]+/, "", line)
            if (line == "" || line ~ /^[#!]/) next
            separator = index(line, "=")
            if (separator == 0) next
            key = substr(line, 1, separator - 1)
            sub(/[[:space:]]+$/, "", key)
            if (key == wanted_key) found = 1
        }
        END { exit(found ? 0 : 1) }
    ' "$file"
}

validate_runtime_secret_file() {
    local file=$1
    local description=$2
    [[ -e $file || -L $file ]] || med_die "$description fehlt: $file"
    [[ -f $file && ! -L $file ]] || med_die "$description muss eine reguläre Datei und kein Symlink sein."

    local canonical_file canonical_repository mode permission_bits
    canonical_file=$(realpath -e -- "$file") || med_die "$description kann nicht kanonisch aufgelöst werden."
    canonical_repository=$(realpath -e -- "$REPOSITORY_ROOT") || med_die 'Das Repository kann nicht kanonisch aufgelöst werden.'
    case "$canonical_file/" in
        "$canonical_repository/"*) med_die "$description muss außerhalb des Git-Checkouts liegen." ;;
    esac

    mode=$(stat -c '%a' -- "$file") || med_die "$description-Dateirechte können nicht gelesen werden."
    [[ $mode =~ ^[0-7]{3,4}$ ]] || med_die "$description-Dateirechte sind ungültig."
    permission_bits=$((8#$mode))
    (( permission_bits == 0400 || permission_bits == 0600 )) \
        || med_die "$description muss die Dateirechte 0400 oder 0600 besitzen; Gruppen-, Other- und Execute-Rechte sind unzulässig."
}

provider_property_allowed() {
    local file_kind=$1
    local key=$2
    case "$file_kind:$key" in
        production-discord:mise-en-dice.discord.enabled|\
        production-discord:mise-en-dice.discord.token|\
        production-discord:mise-en-dice.discord.guild-id|\
        production-discord:mise-en-dice.discord.challenge-operator-role-id|\
        production-discord:mise-en-dice.discord.effective-date-zone|\
        production-discord:mise-en-dice.discord.participant-user-ids.GEORGIA|\
        production-discord:mise-en-dice.discord.participant-user-ids.TOBIAS|\
        production-openai:mise-en-dice.curation.openai.enabled|\
        production-openai:mise-en-dice.curation.openai.api-key|\
        production-openai:mise-en-dice.curation.openai.model|\
        production-openai:mise-en-dice.curation.openai.reasoning-effort|\
        production-openai:mise-en-dice.curation.openai.base-url|\
        production-openai:mise-en-dice.curation.openai.connect-timeout|\
        production-openai:mise-en-dice.curation.openai.request-timeout|\
        production-openai:mise-en-dice.curation.openai.recovery-window|\
        acceptance:mise-en-dice.discord.enabled|\
        acceptance:mise-en-dice.discord.token|\
        acceptance:mise-en-dice.discord.guild-id|\
        acceptance:mise-en-dice.discord.challenge-operator-role-id|\
        acceptance:mise-en-dice.discord.effective-date-zone|\
        acceptance:mise-en-dice.discord.participant-user-ids.GEORGIA|\
        acceptance:mise-en-dice.discord.participant-user-ids.TOBIAS|\
        acceptance:mise-en-dice.curation.openai.enabled|\
        acceptance:mise-en-dice.curation.openai.api-key|\
        acceptance:mise-en-dice.curation.openai.model|\
        acceptance:mise-en-dice.curation.openai.reasoning-effort|\
        acceptance:mise-en-dice.curation.openai.base-url|\
        acceptance:mise-en-dice.curation.openai.connect-timeout|\
        acceptance:mise-en-dice.curation.openai.request-timeout|\
        acceptance:mise-en-dice.curation.openai.recovery-window) return 0 ;;
        *) return 1 ;;
    esac
}

validate_provider_properties_file() {
    local file=$1
    local description=$2
    local file_kind=$3
    validate_runtime_secret_file "$file" "$description"

    local line trimmed key seen=$'\n'
    while IFS= read -r line || [[ -n $line ]]; do
        trimmed=${line%$'\r'}
        trimmed="${trimmed#"${trimmed%%[![:space:]]*}"}"
        [[ -z $trimmed || $trimmed == \#* || $trimmed == \!* ]] && continue
        [[ $trimmed == *=* ]] || med_die "$description enthält keine sichere key=value-Property."
        key=${trimmed%%=*}
        key="${key%"${key##*[![:space:]]}"}"
        [[ -n $key ]] || med_die "$description enthält einen leeren Property-Namen."
        provider_property_allowed "$file_kind" "$key" \
            || med_die "$description enthält eine nicht erlaubte Property: $key"
        [[ $seen != *$'\n'"$key"$'\n'* ]] \
            || med_die "$description enthält eine Property mehrfach: $key"
        seen+="$key"$'\n'
    done < "$file"
}

validate_discord_provider_configuration() {
    local file=$1
    local description=$2
    local require_explicit=$3
    local reject_disabled_secret=$4
    local enabled token guild_id operator_role_id georgia_id tobias_id

    if [[ $require_explicit == true ]]; then
        properties_has_key "$file" 'mise-en-dice.discord.enabled' \
            || med_die "$description muss mise-en-dice.discord.enabled explizit setzen."
    fi
    enabled=$(properties_value "$file" 'mise-en-dice.discord.enabled')
    enabled=${enabled:-false}
    [[ $enabled == true || $enabled == false ]] \
        || med_die "$description muss Discord klar mit true oder false konfigurieren."
    token=$(properties_value "$file" 'mise-en-dice.discord.token')
    if [[ $enabled == false ]]; then
        [[ $reject_disabled_secret != true || -z $token ]] \
            || med_die "$description deaktiviert Discord, enthält aber weiterhin einen Token."
        return
    fi

    [[ -n $token ]] || med_die "$description aktiviert Discord ohne Token."
    guild_id=$(properties_value "$file" 'mise-en-dice.discord.guild-id')
    operator_role_id=$(properties_value "$file" 'mise-en-dice.discord.challenge-operator-role-id')
    georgia_id=$(properties_value "$file" 'mise-en-dice.discord.participant-user-ids.GEORGIA')
    tobias_id=$(properties_value "$file" 'mise-en-dice.discord.participant-user-ids.TOBIAS')
    [[ $guild_id =~ ^[1-9][0-9]{4,31}$ ]] \
        || med_die "$description benötigt eine positive numerische Discord-Guild-ID."
    [[ $operator_role_id =~ ^[1-9][0-9]{4,31}$ ]] \
        || med_die "$description benötigt eine positive numerische Discord-Challenge-Operator-Rollen-ID."
    [[ ( -z $georgia_id || $georgia_id =~ ^[1-9][0-9]{4,31}$ ) && ( -z $tobias_id || $tobias_id =~ ^[1-9][0-9]{4,31}$ ) ]] \
        || med_die "$description enthält ungültige Legacy-Discord-User-IDs für GEORGIA oder TOBIAS."
    [[ -z $georgia_id || -z $tobias_id || $georgia_id != "$tobias_id" ]] \
        || med_die "$description darf GEORGIA und TOBIAS nicht dieselbe Discord-User-ID zuordnen."
}

validate_openai_provider_configuration() {
    local file=$1
    local description=$2
    local require_explicit=$3
    local reject_disabled_secret=$4
    local enabled api_key model reasoning_effort

    if [[ $require_explicit == true ]]; then
        properties_has_key "$file" 'mise-en-dice.curation.openai.enabled' \
            || med_die "$description muss mise-en-dice.curation.openai.enabled explizit setzen."
    fi
    enabled=$(properties_value "$file" 'mise-en-dice.curation.openai.enabled')
    enabled=${enabled:-false}
    [[ $enabled == true || $enabled == false ]] \
        || med_die "$description muss OpenAI klar mit true oder false konfigurieren."
    api_key=$(properties_value "$file" 'mise-en-dice.curation.openai.api-key')
    if [[ $enabled == false ]]; then
        [[ $reject_disabled_secret != true || -z $api_key ]] \
            || med_die "$description deaktiviert OpenAI, enthält aber weiterhin einen API-Key."
        return
    fi

    [[ -n $api_key ]] || med_die "$description aktiviert OpenAI ohne API-Key."
    model=$(properties_value "$file" 'mise-en-dice.curation.openai.model')
    reasoning_effort=$(properties_value "$file" 'mise-en-dice.curation.openai.reasoning-effort')
    [[ $model =~ ^[A-Za-z0-9._-]+$ ]] \
        || med_die "$description benötigt einen sicheren OpenAI-Modellnamen."
    [[ $reasoning_effort =~ ^(none|low|medium|high|xhigh|max)$ ]] \
        || med_die "$description benötigt eine unterstützte OpenAI-Reasoning-Stufe."
}

validate_production_provider_configuration() {
    if [[ -e $DISCORD_PROPERTIES || -L $DISCORD_PROPERTIES ]]; then
        validate_provider_properties_file "$DISCORD_PROPERTIES" 'Produktions-Discord-Konfiguration' production-discord
        validate_discord_provider_configuration "$DISCORD_PROPERTIES" 'Produktions-Discord-Konfiguration' false false
    fi
    if [[ -e $OPENAI_PROPERTIES || -L $OPENAI_PROPERTIES ]]; then
        validate_provider_properties_file "$OPENAI_PROPERTIES" 'Produktions-OpenAI-Konfiguration' production-openai
        validate_openai_provider_configuration "$OPENAI_PROPERTIES" 'Produktions-OpenAI-Konfiguration' false false
    fi
}

reject_matching_provider_secrets() {
    local acceptance_secret=$1
    local production_file=$2
    local production_key=$3
    local provider_name=$4
    [[ -e $production_file || -L $production_file ]] || return 0
    local production_secret
    production_secret=$(properties_value "$production_file" "$production_key")
    [[ -z $acceptance_secret || -z $production_secret || $acceptance_secret != "$production_secret" ]] \
        || med_die "Acceptance und Produktion dürfen nicht denselben $provider_name verwenden."
}

validate_acceptance_provider_configuration() {
    validate_provider_properties_file "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration' acceptance
    validate_discord_provider_configuration "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration' true true
    validate_openai_provider_configuration "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration' true true
    validate_production_provider_configuration

    reject_matching_provider_secrets \
        "$(properties_value "$ACCEPTANCE_PROPERTIES" 'mise-en-dice.discord.token')" \
        "$DISCORD_PROPERTIES" 'mise-en-dice.discord.token' 'Discord-Token'
    reject_matching_provider_secrets \
        "$(properties_value "$ACCEPTANCE_PROPERTIES" 'mise-en-dice.curation.openai.api-key')" \
        "$OPENAI_PROPERTIES" 'mise-en-dice.curation.openai.api-key' 'OpenAI-API-Key'
}

write_compose_env() {
    local destination=$1
    local image=$2
    local port=$3
    local db_password=$4
    local app_config_file=$5

    cat > "$destination" <<EOF_ENV
MISE_EN_DICE_IMAGE=$image
MISE_EN_DICE_APP_PORT=$port
MISE_EN_DICE_DB_NAME=mise_en_dice
MISE_EN_DICE_DB_USERNAME=mise_en_dice
MISE_EN_DICE_DB_PASSWORD=$db_password
MISE_EN_DICE_APP_CONFIG_FILE=$app_config_file
MISE_EN_DICE_RUNTIME_GID=$(id -g)
MISE_EN_DICE_APP_MEMORY_LIMIT=$APP_MEMORY_LIMIT
MISE_EN_DICE_POSTGRES_MEMORY_LIMIT=$POSTGRES_MEMORY_LIMIT
EOF_ENV
    chmod 0600 "$destination"
}

write_instance_application_properties() {
    local destination=$1
    local secure_cookie=$2
    local provider_source=${3:-none}

    # admin.properties is deliberately not a general Spring configuration source. Keeping only its own
    # namespace prevents it from becoming an accidental fallback for Discord, OpenAI, or profile settings.
    grep -E '^[[:space:]]*mise-en-dice\.administration\.' "$ADMIN_PROPERTIES" > "$destination" || true
    case "$provider_source" in
        none)
            # Previews and smoke systems are provider-free even when the operator shell has unrelated secrets.
            cat >> "$destination" <<'EOF_DISABLED_PROVIDERS'
mise-en-dice.discord.enabled=false
mise-en-dice.curation.openai.enabled=false
EOF_DISABLED_PROVIDERS
            ;;
        production)
            if [[ -f $DISCORD_PROPERTIES ]]; then
                cat "$DISCORD_PROPERTIES" >> "$destination"
            else
                echo 'mise-en-dice.discord.enabled=false' >> "$destination"
            fi
            if [[ -f $OPENAI_PROPERTIES ]]; then
                cat "$OPENAI_PROPERTIES" >> "$destination"
                if [[ $(properties_value "$OPENAI_PROPERTIES" 'mise-en-dice.curation.openai.enabled') == true ]]; then
                    echo 'spring.profiles.active=production' >> "$destination"
                fi
            else
                echo 'mise-en-dice.curation.openai.enabled=false' >> "$destination"
            fi
            ;;
        acceptance)
            cat "$ACCEPTANCE_PROPERTIES" >> "$destination"
            # The live Acceptance instance intentionally exercises the production-only OpenAI adapter gate.
            echo 'spring.profiles.active=production' >> "$destination"
            ;;
        *) med_die "Unbekannte Provider-Konfigurationsquelle: $provider_source" ;;
    esac
    cat >> "$destination" <<EOF_PROPERTIES
server.servlet.session.cookie.secure=$secure_cookie
server.forward-headers-strategy=native
management.endpoint.health.show-details=never
EOF_PROPERTIES
    # The container keeps the image's fixed non-root UID and receives only the
    # deployment user's primary group as a supplementary group for this read-only mount.
    chmod 0640 "$destination"
}

write_metadata() {
    local destination=$1
    local instance_type=$2
    local instance_name=$3
    local project_name=$4
    local source_ref=$5
    local source_sha=$6
    local image=$7
    local port=$8

    : > "$destination"
    med_write_shell_assignment "$destination" INSTANCE_TYPE "$instance_type"
    med_write_shell_assignment "$destination" INSTANCE_NAME "$instance_name"
    med_write_shell_assignment "$destination" PROJECT_NAME "$project_name"
    med_write_shell_assignment "$destination" SOURCE_REF "$source_ref"
    med_write_shell_assignment "$destination" SOURCE_SHA "$source_sha"
    med_write_shell_assignment "$destination" IMAGE "$image"
    med_write_shell_assignment "$destination" PORT "$port"
    med_write_shell_assignment "$destination" DEPLOYED_AT "$(med_utc_timestamp)"
    chmod 0600 "$destination"
}

load_metadata() {
    local instance_dir=$1
    [[ -f $instance_dir/metadata ]] || med_die "Instanzmetadaten fehlen: $instance_dir/metadata"
    # shellcheck disable=SC1090
    source "$instance_dir/metadata"
}

load_compose_env() {
    local instance_dir=$1
    [[ -f $instance_dir/compose.env ]] || med_die "Compose-Konfiguration fehlt: $instance_dir/compose.env"
    # The file is generated by this script and deliberately contains shell-safe simple values only.
    # shellcheck disable=SC1090
    source "$instance_dir/compose.env"
}

compose_instance() {
    local instance_dir=$1
    shift
    load_metadata "$instance_dir"
    docker compose \
        --project-name "$PROJECT_NAME" \
        --env-file "$instance_dir/compose.env" \
        -f "$COMPOSE_FILE" \
        "$@"
}

compose_project() {
    local project_name=$1
    local compose_env=$2
    shift 2
    docker compose \
        --project-name "$project_name" \
        --env-file "$compose_env" \
        -f "$COMPOSE_FILE" \
        "$@"
}

port_is_listening() {
    local port=$1
    ss -H -ltn "sport = :$port" 2>/dev/null | grep -q .
}

port_is_reserved_by_metadata() {
    local port=$1
    local metadata
    while IFS= read -r -d '' metadata; do
        if (
            unset PORT
            # shellcheck disable=SC1090
            source "$metadata"
            [[ ${PORT:-} == "$port" ]]
        ); then
            return 0
        fi
    done < <(find "$INSTANCES_DIR" -type f -name metadata -print0 2>/dev/null)
    return 1
}

allocate_preview_port() {
    local port
    for (( port = PREVIEW_PORT_START; port <= PREVIEW_PORT_END; port++ )); do
        if ! port_is_reserved_by_metadata "$port" && ! port_is_listening "$port"; then
            printf '%s' "$port"
            return 0
        fi
    done
    med_die "Kein freier Preview-Port im Bereich $PREVIEW_PORT_START-$PREVIEW_PORT_END."
}

acceptance_port_is_reserved_elsewhere() {
    local metadata
    while IFS= read -r -d '' metadata; do
        [[ $metadata == "$ACCEPTANCE_DIR/metadata" ]] && continue
        if (
            unset PORT
            # shellcheck disable=SC1090
            source "$metadata"
            [[ ${PORT:-} == "$ACCEPTANCE_PORT" ]]
        ); then
            return 0
        fi
    done < <(find "$INSTANCES_DIR" -type f -name metadata -print0 2>/dev/null)
    return 1
}

validate_acceptance_port() {
    acceptance_port_is_reserved_elsewhere \
        && med_die "Acceptance-Port $ACCEPTANCE_PORT ist bereits durch eine andere Mise-en-Dice-Instanz reserviert."

    if port_is_listening "$ACCEPTANCE_PORT"; then
        [[ -f $ACCEPTANCE_DIR/metadata ]] \
            || med_die "Acceptance-Port $ACCEPTANCE_PORT ist bereits durch einen fremden Prozess belegt."
        load_metadata "$ACCEPTANCE_DIR"
        [[ $INSTANCE_TYPE == acceptance && $INSTANCE_NAME == acceptance && $PROJECT_NAME == med-acceptance \
            && $PORT == "$ACCEPTANCE_PORT" ]] \
            || med_die 'Die vorhandenen Acceptance-Metadaten sind ungültig; Start wird verweigert.'
        [[ $(instance_container_status "$ACCEPTANCE_DIR") != stopped ]] \
            || med_die "Acceptance-Port $ACCEPTANCE_PORT ist durch einen fremden Prozess belegt."
    fi
}

validate_acceptance_preflight() {
    validate_acceptance_provider_configuration
    validate_acceptance_port
}

wait_for_health() {
    local port=$1
    local timeout_seconds=${2:-$HEALTH_TIMEOUT_SECONDS}
    local deadline=$((SECONDS + timeout_seconds))

    while (( SECONDS < deadline )); do
        if curl --fail --silent --show-error --max-time 3 \
            "http://127.0.0.1:${port}/actuator/health" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done
    return 1
}

verify_admin_entry() {
    local port=$1
    local status
    status=$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 5 \
        "http://127.0.0.1:${port}/admin" || true)
    [[ $status == 302 || $status == 303 ]]
}

instance_container_status() {
    local instance_dir=$1
    load_metadata "$instance_dir"
    local container_id
    container_id=$(docker compose \
        --project-name "$PROJECT_NAME" \
        --env-file "$instance_dir/compose.env" \
        -f "$COMPOSE_FILE" ps -q app 2>/dev/null || true)
    if [[ -z $container_id ]]; then
        printf 'stopped'
        return
    fi
    docker inspect --format '{{.State.Status}}{{if .State.Health}}/{{.State.Health.Status}}{{end}}' "$container_id" 2>/dev/null \
        || printf 'unknown'
}

prepare_instance_files() {
    local instance_dir=$1
    local image=$2
    local port=$3
    local secure_cookie=$4
    local provider_source=$5

    mkdir -p "$instance_dir"
    chmod 0750 "$instance_dir"

    local db_password
    if [[ -f $instance_dir/compose.env ]]; then
        load_compose_env "$instance_dir"
        db_password=$MISE_EN_DICE_DB_PASSWORD
    else
        db_password=$(med_random_hex 32)
    fi

    local compose_tmp properties_tmp
    compose_tmp=$(mktemp "$TEMP_DIR/compose.XXXXXX")
    properties_tmp=$(mktemp "$TEMP_DIR/application.XXXXXX")
    write_instance_application_properties "$properties_tmp" "$secure_cookie" "$provider_source"
    write_compose_env "$compose_tmp" "$image" "$port" "$db_password" "$instance_dir/application.properties"
    mv -f "$compose_tmp" "$instance_dir/compose.env"
    mv -f "$properties_tmp" "$instance_dir/application.properties"
    chmod 0600 "$instance_dir/compose.env"
    chmod 0640 "$instance_dir/application.properties"
}

recreate_app_container() {
    local instance_dir=$1
    # application.properties is atomically replaced on every deployment. Docker file bind mounts keep pointing at
    # the previously mounted inode if Compose reuses the existing container, so always recreate only the app after
    # the broad reconciliation. PostgreSQL remains untouched unless its own Compose configuration actually changed.
    compose_instance "$instance_dir" up -d --remove-orphans --no-deps --force-recreate app
}

rollback_instance_files() {
    local instance_dir=$1
    local had_previous=$2
    if [[ $had_previous == true ]]; then
        mv -f "$instance_dir/compose.env.previous" "$instance_dir/compose.env"
        mv -f "$instance_dir/application.properties.previous" "$instance_dir/application.properties"
        if [[ -f $instance_dir/metadata.previous ]]; then
            mv -f "$instance_dir/metadata.previous" "$instance_dir/metadata"
        fi
        if ! compose_instance "$instance_dir" up -d --remove-orphans >/dev/null \
            || ! recreate_app_container "$instance_dir" >/dev/null; then
            med_warn 'Der vorherige Anwendungsstand konnte nicht erneut gestartet werden. Logs und Datenbankzustand prüfen.'
            return 0
        fi
        if wait_for_health "$PORT"; then
            med_warn 'Der vorherige Anwendungsstand wurde wieder gestartet. Eine bereits ausgeführte Datenbankmigration wird dadurch nicht zurückgenommen.'
        else
            med_warn 'Auch der vorherige Anwendungsstand wurde nicht wieder gesund. Logs prüfen.'
        fi
    else
        if [[ -f $instance_dir/metadata ]]; then
            compose_instance "$instance_dir" down --volumes --remove-orphans >/dev/null 2>&1 || true
        fi
        rm -f "$instance_dir/compose.env" "$instance_dir/application.properties" "$instance_dir/metadata"
    fi
}

activate_instance() {
    local instance_dir=$1
    local instance_type=$2
    local instance_name=$3
    local project_name=$4
    local requested_ref=$5
    local sha=$6
    local image=$7
    local port=$8
    local secure_cookie=$9
    local provider_source=${10}

    local had_previous=false previous_port=$port
    if [[ -f $instance_dir/metadata ]]; then
        had_previous=true
        load_metadata "$instance_dir"
        previous_port=$PORT
        cp -f "$instance_dir/metadata" "$instance_dir/metadata.previous"
        cp -f "$instance_dir/compose.env" "$instance_dir/compose.env.previous"
        cp -f "$instance_dir/application.properties" "$instance_dir/application.properties.previous"
    elif port_is_listening "$port"; then
        med_die "Port $port ist bereits durch einen fremden Prozess belegt."
    fi

    [[ $previous_port == "$port" ]] || med_die 'Der Port einer bestehenden Instanz darf nicht stillschweigend wechseln.'

    prepare_instance_files "$instance_dir" "$image" "$port" "$secure_cookie" "$provider_source"
    write_metadata "$instance_dir/metadata" "$instance_type" "$instance_name" "$project_name" \
        "$requested_ref" "$sha" "$image" "$port"

    med_note "Starte Compose-Projekt $project_name ..."
    if ! compose_instance "$instance_dir" up -d --remove-orphans \
        || ! recreate_app_container "$instance_dir"; then
        med_warn 'Compose konnte die Instanz nicht starten.'
        rollback_instance_files "$instance_dir" "$had_previous"
        return 1
    fi

    if ! wait_for_health "$port"; then
        med_warn "Healthcheck auf Port $port ist fehlgeschlagen."
        compose_instance "$instance_dir" ps >&2 || true
        compose_instance "$instance_dir" logs --tail 200 app postgres >&2 || true
        rollback_instance_files "$instance_dir" "$had_previous"
        return 1
    fi

    if ! verify_admin_entry "$port"; then
        med_warn 'Der geschützte Admin-Einstieg liefert keinen Redirect auf das Login.'
        rollback_instance_files "$instance_dir" "$had_previous"
        return 1
    fi
    rm -f "$instance_dir/metadata.previous" "$instance_dir/compose.env.previous" "$instance_dir/application.properties.previous"
    med_note "Instanz ist gesund: http://127.0.0.1:${port}/admin"
}

create_temporary_instance_files() {
    local instance_dir=$1
    local image=$2
    local port=$3
    mkdir -p "$instance_dir"
    chmod 0750 "$instance_dir"
    write_instance_application_properties "$instance_dir/application.properties" false none
    write_compose_env "$instance_dir/compose.env" "$image" "$port" "$(med_random_hex 32)" "$instance_dir/application.properties"
}

smoke_image() {
    local image=$1
    local sha=$2
    local port project_name smoke_dir
    port=$(allocate_preview_port)
    project_name="med-smoke-${sha:0:10}-$$"
    smoke_dir="$TEMP_DIR/$project_name"
    create_temporary_instance_files "$smoke_dir" "$image" "$port"

    med_note 'Prüfe das Image vor dem Produktionswechsel gegen eine frische PostgreSQL-Datenbank ...'
    local result=0
    compose_project "$project_name" "$smoke_dir/compose.env" up -d --remove-orphans || result=$?
    if (( result == 0 )) && ! wait_for_health "$port"; then
        result=1
    fi
    if (( result == 0 )) && ! verify_admin_entry "$port"; then
        result=1
    fi

    if (( result != 0 )); then
        compose_project "$project_name" "$smoke_dir/compose.env" logs --tail 200 app postgres >&2 || true
    fi
    compose_project "$project_name" "$smoke_dir/compose.env" down --volumes --remove-orphans >/dev/null 2>&1 || true
    rm -rf "$smoke_dir"

    (( result == 0 )) || med_die 'Der Vorab-Smoke-Test des Images ist fehlgeschlagen.'
}
