#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=deploy/lib.sh
source "$SCRIPT_DIR/../lib.sh"

failures=0

assert_equals() {
    local expected=$1
    local actual=$2
    local description=$3
    if [[ $expected != "$actual" ]]; then
        printf 'FAIL: %s\n  expected: %q\n  actual:   %q\n' "$description" "$expected" "$actual" >&2
        failures=$((failures + 1))
    fi
}

assert_true() {
    local description=$1
    shift
    if ! "$@"; then
        printf 'FAIL: %s\n' "$description" >&2
        failures=$((failures + 1))
    fi
}

assert_false() {
    local description=$1
    shift
    if "$@"; then
        printf 'FAIL: %s\n' "$description" >&2
        failures=$((failures + 1))
    fi
}

assert_equals 'feat-12-catalog-ui' "$(med_slugify 'feat/12-Catalog_UI')" 'Branch-Namen werden Compose-sicher normalisiert.'
assert_equals 'release-2026-08' "$(med_slugify '///Release 2026.08///')" 'Randzeichen und Trenner werden bereinigt.'
long_slug=$(med_slugify 'this-is-a-deliberately-ridiculously-long-preview-name-that-must-be-shortened')
assert_equals 42 "${#long_slug}" 'Preview-Namen sind höchstens 42 Zeichen lang.'

assert_true 'Gültiger actor_key wird akzeptiert.' med_valid_actor_key 'tobias_01'
assert_false 'Actor-Key mit Leerzeichen wird abgelehnt.' med_valid_actor_key 'Tobias Admin'
assert_true 'Gültiger BCrypt-Hash wird akzeptiert.' med_valid_bcrypt_hash '$2y$12$01234567890123456789012345678901234567890123456789012'
assert_false 'Offensichtlich ungültiger Hash wird abgelehnt.' med_valid_bcrypt_hash 'not-a-hash'

assert_true 'Normaler Branch-Ref wird akzeptiert.' med_valid_ref_input 'feat/14-deployment-package'
assert_true 'Commit-SHA wird akzeptiert.' med_valid_ref_input 'a4245d7d0023529ebfa8d8d087ce2d237bf94c6a'
assert_false 'Option-Injection als Ref wird abgelehnt.' med_valid_ref_input '--upload-pack=evil'
assert_false 'Ref mit Leerzeichen wird abgelehnt.' med_valid_ref_input 'feat/not safe'
assert_false 'Git-Revisionsausdruck wird nicht als Ref akzeptiert.' med_valid_ref_input 'main~1'
assert_false 'Ref mit Doppelpunkt wird abgelehnt.' med_valid_ref_input 'main:evil'

assert_true 'Absoluter Runtime-Pfad wird akzeptiert.' med_valid_absolute_runtime_path '/opt/mise-en-dice/runtime'
assert_false 'Relativer Runtime-Pfad wird abgelehnt.' med_valid_absolute_runtime_path 'runtime'
assert_false 'Dollarzeichen im Runtime-Pfad wird abgelehnt.' med_valid_absolute_runtime_path '/opt/$HOME/runtime'
assert_false 'Elternpfad im Runtime-Pfad wird abgelehnt.' med_valid_absolute_runtime_path '/opt/mise-en-dice/../runtime'
assert_false 'Doppelte Slashes im Runtime-Pfad werden abgelehnt.' med_valid_absolute_runtime_path '/opt//mise-en-dice/runtime'

assert_true 'Normales Speicherlimit wird akzeptiert.' med_valid_memory_limit 768m
assert_true 'Speicherlimit in Gigabyte wird akzeptiert.' med_valid_memory_limit 1g
assert_false 'Speicherlimit ohne Einheit wird abgelehnt.' med_valid_memory_limit 768
assert_false 'Null-Speicherlimit wird abgelehnt.' med_valid_memory_limit 0m
assert_true 'Normaler SSH-Benutzer wird akzeptiert.' med_valid_ssh_user gridwords
assert_false 'SSH-Benutzer mit Shellzeichen wird abgelehnt.' med_valid_ssh_user 'grid;words'
assert_true 'IPv4-Adresse wird als SSH-Host akzeptiert.' med_valid_ssh_host 159.195.217.58
assert_false 'SSH-Host mit Leerzeichen wird abgelehnt.' med_valid_ssh_host 'bad host'

assert_true 'Normaler unprivilegierter Port wird akzeptiert.' med_valid_port 18080
assert_false 'Privilegierter Port wird abgelehnt.' med_valid_port 443
assert_false 'Port oberhalb des gültigen Bereichs wird abgelehnt.' med_valid_port 70000

assert_equals 'Tobias\\Test' "$(med_properties_escape 'Tobias\Test')" 'Backslashes werden für Java Properties escaped.'

if (( failures > 0 )); then
    printf '%s test(s) failed.\n' "$failures" >&2
    exit 1
fi

printf 'All deployment helper tests passed.\n'
