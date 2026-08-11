#!/usr/bin/env bash

# User-facing production and preview commands.

print_tunnel_hint() {
    local port=$1
    printf 'URL=http://localhost:%s/admin\n' "$port"
    printf 'SSH_TUNNEL=ssh -N -L %s:127.0.0.1:%s %s@%s\n' "$port" "$port" "$SSH_USER" "$SSH_HOST"
}

command_doctor() {
    ensure_runtime_initialized
    check_base_commands

    local origin_url
    origin_url=$(git -C "$REPOSITORY_ROOT" remote get-url origin 2>/dev/null) \
        || med_die 'Das Repository besitzt kein origin-Remote.'
    git -C "$REPOSITORY_ROOT" ls-remote --exit-code origin HEAD >/dev/null 2>&1 \
        || med_die 'Das origin-Remote ist nicht erreichbar oder besitzt keinen HEAD.'

    local doctor_dir="$TEMP_DIR/doctor-$$"
    mkdir -p "$doctor_dir"
    write_instance_application_properties "$doctor_dir/application.properties" false
    write_compose_env "$doctor_dir/compose.env" 'mise-en-dice:doctor' "$PRODUCTION_PORT" 'doctor-password' "$doctor_dir/application.properties"
    docker compose --project-name med-doctor --env-file "$doctor_dir/compose.env" -f "$COMPOSE_FILE" config --quiet
    rm -rf "$doctor_dir"

    med_note 'Deployment-Voraussetzungen sind erfüllt.'
    printf 'REPOSITORY=%s\n' "$origin_url"
    printf 'DEPLOY_ROOT=%s\n' "$DEPLOY_ROOT"
    printf 'PRODUCTION_PORT=%s\n' "$PRODUCTION_PORT"
    printf 'PREVIEW_PORT_RANGE=%s-%s\n' "$PREVIEW_PORT_START" "$PREVIEW_PORT_END"
}

command_production_deploy() {
    local requested_ref=${1:-main}
    ensure_runtime_initialized
    check_base_commands
    acquire_lock

    local build_data sha image
    build_data=$(build_image "$requested_ref")
    IFS=$'\t' read -r sha image <<< "$build_data"
    smoke_image "$image" "$sha"

    if [[ -f $PRODUCTION_DIR/metadata ]]; then
        backup_instance "$PRODUCTION_DIR" >/dev/null
    fi

    activate_instance "$PRODUCTION_DIR" production production med-production \
        "$requested_ref" "$sha" "$image" "$PRODUCTION_PORT" true

    printf 'INSTANCE=production\n'
    printf 'SOURCE_REF=%s\n' "$requested_ref"
    printf 'SOURCE_SHA=%s\n' "$sha"
    printf 'APP_PORT=%s\n' "$PRODUCTION_PORT"
    printf 'CADDY_UPSTREAM=http://127.0.0.1:%s\n' "$PRODUCTION_PORT"
}

normalise_preview_name() {
    local raw_name=$1
    local slug
    slug=$(med_slugify "$raw_name")
    [[ -n $slug ]] || med_die "Aus '$raw_name' lässt sich kein sicherer Preview-Name bilden."
    printf '%s' "$slug"
}

command_preview_deploy() {
    local requested_ref=${1:-}
    local requested_name=${2:-}
    [[ -n $requested_ref ]] || med_die 'preview deploy benötigt einen Branch, Tag oder Commit.'
    ensure_runtime_initialized
    check_base_commands
    acquire_lock

    local preview_name
    if [[ -n $requested_name ]]; then
        preview_name=$(normalise_preview_name "$requested_name")
    else
        preview_name=$(normalise_preview_name "$requested_ref")
    fi

    local instance_dir="$PREVIEWS_DIR/$preview_name"
    if [[ -f $instance_dir/metadata ]]; then
        load_metadata "$instance_dir"
        if [[ $SOURCE_REF != "$requested_ref" && -z $requested_name ]]; then
            med_die "Der automatisch gebildete Preview-Name '$preview_name' gehört bereits zu '$SOURCE_REF'. Bitte einen expliziten Namen angeben."
        fi
    fi

    local build_data sha image port
    build_data=$(build_image "$requested_ref")
    IFS=$'\t' read -r sha image <<< "$build_data"

    if [[ -f $instance_dir/metadata ]]; then
        load_metadata "$instance_dir"
        port=$PORT
    else
        port=$(allocate_preview_port)
    fi

    activate_instance "$instance_dir" preview "$preview_name" "med-preview-$preview_name" \
        "$requested_ref" "$sha" "$image" "$port" false

    printf 'PREVIEW_NAME=%s\n' "$preview_name"
    printf 'SOURCE_REF=%s\n' "$requested_ref"
    printf 'SOURCE_SHA=%s\n' "$sha"
    printf 'APP_PORT=%s\n' "$port"
    print_tunnel_hint "$port"
}

find_preview_dir() {
    local requested_name=$1
    local preview_name
    preview_name=$(normalise_preview_name "$requested_name")
    local instance_dir="$PREVIEWS_DIR/$preview_name"
    [[ -f $instance_dir/metadata ]] || med_die "Preview nicht gefunden: $preview_name"
    printf '%s' "$instance_dir"
}

command_preview_list() {
    ensure_runtime_initialized
    check_base_commands
    printf '%-42s %-24s %-7s %-24s %s\n' NAME STATUS PORT REF COMMIT
    local metadata instance_dir status
    while IFS= read -r -d '' metadata; do
        instance_dir=$(dirname "$metadata")
        load_metadata "$instance_dir"
        status=$(instance_container_status "$instance_dir")
        printf '%-42s %-24s %-7s %-24s %s\n' "$INSTANCE_NAME" "$status" "$PORT" "$SOURCE_REF" "${SOURCE_SHA:0:12}"
    done < <(find "$PREVIEWS_DIR" -mindepth 2 -maxdepth 2 -type f -name metadata -print0 | sort -z)
}

command_instance_status() {
    local instance_dir=$1
    load_metadata "$instance_dir"
    printf 'INSTANCE=%s\n' "$INSTANCE_NAME"
    printf 'STATUS=%s\n' "$(instance_container_status "$instance_dir")"
    printf 'SOURCE_REF=%s\n' "$SOURCE_REF"
    printf 'SOURCE_SHA=%s\n' "$SOURCE_SHA"
    printf 'APP_PORT=%s\n' "$PORT"
    compose_instance "$instance_dir" ps
}

command_instance_logs() {
    local instance_dir=$1
    local follow=${2:-false}
    if [[ $follow == true ]]; then
        compose_instance "$instance_dir" logs --tail 200 --follow app postgres
    else
        compose_instance "$instance_dir" logs --tail 200 app postgres
    fi
}

command_instance_stop() {
    local instance_dir=$1
    acquire_lock
    compose_instance "$instance_dir" stop
    med_note 'Instanz gestoppt.'
}

command_instance_start() {
    local instance_dir=$1
    acquire_lock
    load_metadata "$instance_dir"
    compose_instance "$instance_dir" start
    if ! wait_for_health "$PORT" || ! verify_admin_entry "$PORT"; then
        compose_instance "$instance_dir" logs --tail 200 app postgres >&2 || true
        med_die 'Instanz wurde gestartet, aber nicht vollständig gesund.'
    fi
    med_note "Instanz ist gesund auf Port $PORT."
}

command_preview_remove() {
    local instance_dir=$1
    local supplied_yes=$2
    acquire_lock
    load_metadata "$instance_dir"
    confirm_destructive_action "REMOVE-$INSTANCE_NAME" "$supplied_yes"
    compose_instance "$instance_dir" down --volumes --remove-orphans
    rm -rf "$instance_dir"
    med_note "Preview $INSTANCE_NAME einschließlich Datenbankvolume entfernt."
}

handle_production() {
    local action=${1:-}
    shift || true
    case "$action" in
        deploy) command_production_deploy "$@" ;;
        status)
            ensure_runtime_initialized; check_base_commands
            [[ -f $PRODUCTION_DIR/metadata ]] || med_die 'Produktion wurde noch nicht deployt.'
            command_instance_status "$PRODUCTION_DIR"
            ;;
        logs)
            ensure_runtime_initialized; check_base_commands
            [[ -f $PRODUCTION_DIR/metadata ]] || med_die 'Produktion wurde noch nicht deployt.'
            local follow=false
            [[ ${1:-} == --follow ]] && follow=true
            command_instance_logs "$PRODUCTION_DIR" "$follow"
            ;;
        stop)
            ensure_runtime_initialized; check_base_commands
            [[ -f $PRODUCTION_DIR/metadata ]] || med_die 'Produktion wurde noch nicht deployt.'
            command_instance_stop "$PRODUCTION_DIR"
            ;;
        start)
            ensure_runtime_initialized; check_base_commands
            [[ -f $PRODUCTION_DIR/metadata ]] || med_die 'Produktion wurde noch nicht deployt.'
            command_instance_start "$PRODUCTION_DIR"
            ;;
        backup)
            ensure_runtime_initialized; check_base_commands; acquire_lock
            [[ -f $PRODUCTION_DIR/metadata ]] || med_die 'Produktion wurde noch nicht deployt.'
            backup_instance "$PRODUCTION_DIR"
            ;;
        *) usage; med_die "Unbekannte production-Aktion: ${action:-<leer>}" ;;
    esac
}

handle_preview() {
    local action=${1:-}
    shift || true
    case "$action" in
        deploy) command_preview_deploy "$@" ;;
        list) command_preview_list ;;
        status)
            ensure_runtime_initialized; check_base_commands
            command_instance_status "$(find_preview_dir "${1:-}")"
            ;;
        logs)
            ensure_runtime_initialized; check_base_commands
            local instance_dir follow=false
            instance_dir=$(find_preview_dir "${1:-}")
            [[ ${2:-} == --follow ]] && follow=true
            command_instance_logs "$instance_dir" "$follow"
            ;;
        stop)
            ensure_runtime_initialized; check_base_commands
            command_instance_stop "$(find_preview_dir "${1:-}")"
            ;;
        start)
            ensure_runtime_initialized; check_base_commands
            command_instance_start "$(find_preview_dir "${1:-}")"
            ;;
        sql)
            ensure_runtime_initialized; check_base_commands
            [[ -n ${1:-} && -n ${2:-} ]] || med_die 'preview sql benötigt Preview-Name und SQL als ein Argument.'
            command_preview_sql "$(find_preview_dir "$1")" "$2"
            ;;
        restore)
            ensure_runtime_initialized; check_base_commands; acquire_lock
            [[ -n ${1:-} && -n ${2:-} ]] || med_die 'preview restore benötigt Preview-Name und Backup-Datei.'
            local restore_name restore_file restore_yes=false
            restore_name=$(normalise_preview_name "$1")
            restore_file=$2
            [[ ${3:-} == --yes ]] && restore_yes=true
            restore_preview "$restore_name" "$restore_file" "$restore_yes"
            ;;
        remove)
            ensure_runtime_initialized; check_base_commands
            [[ -n ${1:-} ]] || med_die 'preview remove benötigt einen Preview-Namen.'
            local remove_yes=false
            [[ ${2:-} == --yes ]] && remove_yes=true
            command_preview_remove "$(find_preview_dir "$1")" "$remove_yes"
            ;;
        *) usage; med_die "Unbekannte preview-Aktion: ${action:-<leer>}" ;;
    esac
}

main() {
    local command=${1:-}
    shift || true
    case "$command" in
        init) command_init "$@" ;;
        doctor) command_doctor ;;
        production) handle_production "$@" ;;
        preview) handle_preview "$@" ;;
        help|-h|--help|'') usage ;;
        *) usage; med_die "Unbekanntes Kommando: $command" ;;
    esac
}
