#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
# shellcheck source=deploy/lib.sh
source "$SCRIPT_DIR/../lib.sh"
# shellcheck source=deploy/operator/editorial-upgrade.sh
source "$SCRIPT_DIR/../operator/editorial-upgrade.sh"

test_root=$(mktemp -d)
trap 'rm -rf -- "$test_root"' EXIT
TEMP_DIR=$test_root
PRODUCTION_DIR="$test_root/production"
events="$test_root/events"

ensure_runtime_initialized() { :; }
check_base_commands() { :; }
acquire_lock() { echo lock >> "$events"; }
ensure_production_deployed() { :; }
load_compose_env() {
    MISE_EN_DICE_DB_PASSWORD='test'
    MISE_EN_DICE_DB_USERNAME='test'
    MISE_EN_DICE_DB_NAME='test'
}
production_editorial_upgrade_state() {
    if [[ -f $test_root/applied ]]; then echo APPLIED; else echo "$initial_state"; fi
}
compose_instance() {
    case "$2" in
        stop) echo stop >> "$events" ;;
        ps) : ;;
        exec)
            test -s "$test_root/backup.dump.sha256"
            cat > "$test_root/executed.sql"
            echo write >> "$events"
            touch "$test_root/applied" ;;
        *) exit 1 ;;
    esac
}
backup_instance() {
    echo backup >> "$events"
    [[ $backup_valid == true ]] || return 1
    echo test-backup > "$test_root/backup.dump"
    (cd "$test_root" && sha256sum backup.dump > backup.dump.sha256)
    printf 'BACKUP_FILE=%s/backup.dump\n' "$test_root"
}

# Real assembler is exercised, including every pinned original and replacement.
assemble_editorial_upgrade "$test_root/bundle.sql"
test -s "$test_root/bundle.sql"
grep -q '293-editorial-upgrade-corridor' "$test_root/bundle.sql"
initial_state=INVALID
backup_valid=true
if (command_production_reconcile_editorial_upgrade); then
    med_die 'Unknown history was accepted.'
fi
test "$(cat "$events")" = lock
rm "$events"

initial_state=PENDING
backup_valid=false
if (command_production_reconcile_editorial_upgrade); then
    med_die 'Failed backup was accepted.'
fi
test ! -e "$test_root/executed.sql"
test "$(cat "$events")" = $'lock\nstop\nbackup'
rm "$events"

backup_valid=true
command_production_reconcile_editorial_upgrade > "$test_root/result"
test "$(cat "$events")" = $'lock\nstop\nbackup\nwrite'
grep -qx 'RECONCILIATION=applied' "$test_root/result"
rm "$events"
command_production_reconcile_editorial_upgrade > "$test_root/repeat"
test "$(cat "$events")" = lock
grep -qx 'RECONCILIATION=already-applied' "$test_root/repeat"
echo 'All editorial upgrade operator tests passed.'
