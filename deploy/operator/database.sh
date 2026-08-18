#!/usr/bin/env bash

# PostgreSQL backup, restore and preview-only SQL diagnostics.

backup_instance() {
    local instance_dir=$1
    load_metadata "$instance_dir"
    load_compose_env "$instance_dir"

    local container_id
    container_id=$(compose_instance "$instance_dir" ps -q postgres)
    [[ -n $container_id ]] || med_die 'Die PostgreSQL-Instanz läuft nicht; Backup abgebrochen.'

    mkdir -p "$BACKUPS_DIR"
    local timestamp short_sha backup_prefix base_name temporary_file final_file checksum_tmp
    timestamp=$(med_compact_timestamp)
    short_sha=${SOURCE_SHA:0:12}
    case "$INSTANCE_TYPE" in
        production) backup_prefix='mise-en-dice' ;;
        acceptance) backup_prefix='mise-en-dice-acceptance' ;;
        *) med_die 'Backups sind nur für Produktion oder Acceptance vorgesehen.' ;;
    esac
    base_name="${backup_prefix}-${timestamp}-${short_sha}.dump"
    if [[ -e $BACKUPS_DIR/$base_name || -e $BACKUPS_DIR/.$base_name.partial ]]; then
        base_name="${backup_prefix}-${timestamp}-${short_sha}-${RANDOM}.dump"
    fi
    temporary_file="$BACKUPS_DIR/.${base_name}.partial"
    final_file="$BACKUPS_DIR/$base_name"
    checksum_tmp="$BACKUPS_DIR/.${base_name}.sha256.partial"

    med_note "Erzeuge PostgreSQL-Backup $base_name ..."
    rm -f "$temporary_file" "$checksum_tmp"
    if ! compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" \
        postgres pg_dump \
        --host=127.0.0.1 \
        --username="$MISE_EN_DICE_DB_USERNAME" \
        --dbname="$MISE_EN_DICE_DB_NAME" \
        --format=custom \
        --compress=6 \
        --no-owner \
        --no-privileges > "$temporary_file"; then
        rm -f "$temporary_file"
        med_die 'pg_dump ist fehlgeschlagen; es wurde kein Backup veröffentlicht.'
    fi

    if [[ ! -s $temporary_file ]]; then
        rm -f "$temporary_file"
        med_die 'Das erzeugte Backup ist leer.'
    fi
    if ! compose_instance "$instance_dir" exec -T postgres pg_restore --list < "$temporary_file" >/dev/null; then
        rm -f "$temporary_file"
        med_die 'Das erzeugte Backup konnte nicht als PostgreSQL-Archiv validiert werden.'
    fi

    mv -f "$temporary_file" "$final_file"
    (
        cd "$BACKUPS_DIR" || exit 1
        sha256sum "$base_name" > "$(basename "$checksum_tmp")"
    )
    mv -f "$checksum_tmp" "$final_file.sha256"
    chmod 0600 "$final_file" "$final_file.sha256"

    if (( BACKUP_RETENTION_DAYS > 0 )); then
        find "$BACKUPS_DIR" -maxdepth 1 -type f -name 'mise-en-dice-*.dump' -mtime "+$BACKUP_RETENTION_DAYS" -print0 \
            | while IFS= read -r -d '' old_backup; do
                rm -f "$old_backup" "$old_backup.sha256"
            done
    fi
    find "$BACKUPS_DIR" -maxdepth 1 -type f -name '.*.partial' -mtime +1 -delete

    med_note "Backup validiert: $final_file"
    printf 'BACKUP_FILE=%s\n' "$final_file"
}

confirm_destructive_action() {
    local expected=$1
    local supplied_yes=$2
    [[ $supplied_yes == true ]] && return 0
    [[ -t 0 ]] || med_die "Nicht-interaktiv ist --yes erforderlich. Erwartete Bestätigung: $expected"
    local answer
    read -r -p "Zur Bestätigung exakt '$expected' eingeben: " answer
    [[ $answer == "$expected" ]] || med_die 'Abgebrochen.'
}

restore_preview() {
    local preview_name=$1
    local backup_file=$2
    local supplied_yes=$3
    local instance_dir="$PREVIEWS_DIR/$preview_name"

    [[ -f $backup_file ]] || med_die "Backup nicht gefunden: $backup_file"
    load_metadata "$instance_dir"
    [[ $INSTANCE_TYPE == preview ]] || med_die 'Restore ist ausschließlich in eine Preview erlaubt.'
    load_compose_env "$instance_dir"

    confirm_destructive_action "RESTORE-$preview_name" "$supplied_yes"

    if [[ -f $backup_file.sha256 ]]; then
        (cd "$(dirname "$backup_file")" && sha256sum --check "$(basename "$backup_file").sha256")
    fi
    compose_instance "$instance_dir" exec -T postgres pg_restore --list < "$backup_file" >/dev/null

    med_note "Stoppe die Preview-Anwendung und stelle $backup_file wieder her ..."
    compose_instance "$instance_dir" stop app
    compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" \
        postgres dropdb --host=127.0.0.1 --username="$MISE_EN_DICE_DB_USERNAME" --if-exists --force "$MISE_EN_DICE_DB_NAME"
    compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" \
        postgres createdb --host=127.0.0.1 --username="$MISE_EN_DICE_DB_USERNAME" "$MISE_EN_DICE_DB_NAME"
    compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" \
        postgres pg_restore \
        --host=127.0.0.1 \
        --username="$MISE_EN_DICE_DB_USERNAME" \
        --dbname="$MISE_EN_DICE_DB_NAME" \
        --no-owner \
        --no-privileges \
        --exit-on-error < "$backup_file"

    compose_instance "$instance_dir" start app
    if ! wait_for_health "$PORT" || ! verify_admin_entry "$PORT"; then
        compose_instance "$instance_dir" logs --tail 200 app postgres >&2 || true
        med_die 'Restore abgeschlossen, aber die Anwendung wurde nicht vollständig gesund. Die Preview-App bleibt zur Prüfung im aktuellen Zustand.'
    fi
    med_note "Restore erfolgreich; Preview ist wieder gesund auf Port $PORT."
}

command_preview_sql() {
    local instance_dir=$1
    local sql=$2
    load_compose_env "$instance_dir"
    compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" \
        postgres psql \
        --host=127.0.0.1 \
        --username="$MISE_EN_DICE_DB_USERNAME" \
        --dbname="$MISE_EN_DICE_DB_NAME" \
        --no-align \
        --tuples-only \
        --set ON_ERROR_STOP=1 \
        --command "$sql"
}

validate_read_only_sql() {
    local sql=$1
    local upper_sql=${sql^^}
    [[ -n $sql && $sql != *';'* ]] \
        || med_die 'acceptance sql erwartet genau eine Query ohne Semikolon.'
    [[ $upper_sql =~ ^[[:space:]]*(SELECT|WITH|EXPLAIN|SHOW)[[:space:]] ]] \
        || med_die 'acceptance sql erlaubt nur read-only SELECT-, WITH-, EXPLAIN- oder SHOW-Queries.'
}

command_acceptance_sql() {
    local instance_dir=$1
    local sql=$2
    validate_read_only_sql "$sql"
    load_compose_env "$instance_dir"
    compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" \
        postgres psql \
        --host=127.0.0.1 \
        --username="$MISE_EN_DICE_DB_USERNAME" \
        --dbname="$MISE_EN_DICE_DB_NAME" \
        --no-align \
        --tuples-only \
        --quiet \
        --set ON_ERROR_STOP=1 \
        --command "BEGIN TRANSACTION READ ONLY; $sql; ROLLBACK;"
}
