#!/usr/bin/env bash

# Fixed #293 post-036 corridor. No caller-selected SQL, ranges or changesets.
production_editorial_upgrade_state() {
    local instance_dir=$1
    load_compose_env "$instance_dir"
    {
        cat "$REPOSITORY_ROOT/deploy/reconciliation/293/history.sql"
        printf '\nSELECT pg_temp.issue_293_state();\n'
    } | compose_instance "$instance_dir" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" postgres psql \
        --host=127.0.0.1 --username="$MISE_EN_DICE_DB_USERNAME" --dbname="$MISE_EN_DICE_DB_NAME" \
        --no-psqlrc --no-align --tuples-only --quiet --set ON_ERROR_STOP=1
}

# Assemble completely before connecting for writes. SHA-256 binds each approved
# source/range to its reviewed file; CRLF checkouts normalize to canonical LF.
assemble_editorial_upgrade() {
    local destination=$1
    local directory="$REPOSITORY_ROOT/deploy/reconciliation/293"
    local relative first last expected actual source
    cat "$directory/history.sql" "$directory/setup.sql" > "$destination"
    while IFS=$'\t' read -r relative first last expected; do
        expected=${expected%$'\r'}
        [[ $relative =~ ^(src/main/resources/db/changelog/|deploy/reconciliation/293/)[A-Za-z0-9_./-]+$ \
            && $relative != *..* && $first =~ ^[1-9][0-9]*$ && $last =~ ^[1-9][0-9]*$ \
            && $expected =~ ^[a-f0-9]{64}$ ]] || med_die 'Ungültiger #293-Ausführungsplan.'
        source="$REPOSITORY_ROOT/$relative"
        [[ -f $source && ! -L $source ]] || med_die "#293-Quelldatei fehlt: $relative"
        actual=$(sed 's/\r$//' "$source" | sha256sum | cut -d ' ' -f1)
        [[ $actual == "$expected" ]] || med_die "#293-Quellprüfung fehlgeschlagen: $relative"
        sed 's/\r$//' "$source" | sed -n "${first},${last}p" >> "$destination"
        printf '\n' >> "$destination"
    done < "$directory/parts.tsv"
    cat "$directory/record.sql" >> "$destination"
}

command_production_reconcile_editorial_upgrade() (
    ensure_runtime_initialized
    check_base_commands
    acquire_lock
    ensure_production_deployed

    local state bundle backup_output backup_file app_container
    state=$(production_editorial_upgrade_state "$PRODUCTION_DIR")
    case "$state" in
        APPLIED)
            printf 'RECONCILIATION=already-applied\n'
            return 0 ;;
        PENDING) ;;
        *) med_die '#293 benötigt den exakt dokumentierten Incidentstand nach 036; keine Änderungen.' ;;
    esac

    bundle=$(mktemp "$TEMP_DIR/issue-293-XXXXXX.sql")
    trap 'rm -f -- "$bundle"' EXIT
    assemble_editorial_upgrade "$bundle"
    compose_instance "$PRODUCTION_DIR" stop app
    app_container=$(compose_instance "$PRODUCTION_DIR" ps -q app)
    if [[ -n $app_container ]] \
        && [[ $(docker inspect --format '{{.State.Running}}' "$app_container") != false ]]; then
        med_die '#293: Produktionsanwendung ist nicht sicher gestoppt.'
    fi
    [[ $(production_editorial_upgrade_state "$PRODUCTION_DIR") == PENDING ]] \
        || med_die '#293: Datenbankzustand hat sich nach App-Stopp verändert.'
    backup_output=$(backup_instance "$PRODUCTION_DIR")
    backup_file=$(printf '%s\n' "$backup_output" | sed -n 's/^BACKUP_FILE=//p')
    [[ -n $backup_file && -s $backup_file && -s $backup_file.sha256 ]] \
        || med_die '#293: validiertes Produktionsbackup fehlt.'
    (cd "$(dirname "$backup_file")" && sha256sum --check "$(basename "$backup_file").sha256") >&2

    load_compose_env "$PRODUCTION_DIR"
    compose_instance "$PRODUCTION_DIR" exec -T \
        -e "PGPASSWORD=$MISE_EN_DICE_DB_PASSWORD" postgres psql \
        --host=127.0.0.1 --username="$MISE_EN_DICE_DB_USERNAME" --dbname="$MISE_EN_DICE_DB_NAME" \
        --no-psqlrc --set ON_ERROR_STOP=1 < "$bundle"
    [[ $(production_editorial_upgrade_state "$PRODUCTION_DIR") == APPLIED ]] \
        || med_die '#293: vollständiger Abschlussmarker fehlt.'
    med_note '#293 abgeschlossen. App bleibt gestoppt; anschließend production deploy main ausführen.'
    printf 'RECONCILIATION=applied\nBACKUP_FILE=%s\n' "$backup_file"
)
