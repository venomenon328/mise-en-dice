#!/usr/bin/env bash

# Shared helpers for the Mise en Dice deployment operator.
# This file is sourced by deploy/mise-en-dice.sh and its small unit test.

med_die() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

med_note() {
    printf '==> %s\n' "$*" >&2
}

med_warn() {
    printf 'WARN: %s\n' "$*" >&2
}

med_require_command() {
    local command_name=$1
    command -v "$command_name" >/dev/null 2>&1 \
        || med_die "Benötigtes Kommando fehlt: ${command_name}"
}

med_slugify() {
    local raw=$1
    local slug

    slug=$(printf '%s' "$raw" \
        | LC_ALL=C tr '[:upper:]' '[:lower:]' \
        | LC_ALL=C sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//; s/-+/-/g' \
        | cut -c1-42 \
        | LC_ALL=C sed -E 's/-+$//')

    printf '%s' "$slug"
}

med_valid_actor_key() {
    [[ $1 =~ ^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$ ]]
}

med_valid_bcrypt_hash() {
    [[ $1 =~ ^\$2[aby]\$(0[4-9]|[12][0-9]|3[01])\$[./A-Za-z0-9]{53}$ ]]
}

med_valid_ref_input() {
    local ref=$1
    [[ -n $ref ]] || return 1
    [[ $ref != -* ]] || return 1
    [[ $ref != *$'\n'* && $ref != *$'\r'* && $ref != *$'\t'* && $ref != *' '* ]] || return 1
    [[ $ref =~ ^[A-Za-z0-9._/+@-]+$ ]]
}

med_valid_absolute_runtime_path() {
    local path=$1
    [[ $path =~ ^/[A-Za-z0-9._/-]+$ ]] || return 1
    [[ $path != *'//'* && $path != */./* && $path != */../* && $path != */. && $path != */.. ]]
}

med_valid_memory_limit() {
    [[ $1 =~ ^[1-9][0-9]*[kKmMgG]$ ]]
}

med_valid_ssh_user() {
    [[ $1 =~ ^[A-Za-z0-9._-]+$ ]]
}

med_valid_ssh_host() {
    [[ $1 =~ ^[A-Za-z0-9._:-]+$ ]]
}

med_valid_port() {
    local port=$1
    [[ $port =~ ^[0-9]+$ ]] || return 1
    (( port >= 1024 && port <= 65535 ))
}

med_properties_escape() {
    local value=$1
    [[ $value != *$'\n'* && $value != *$'\r'* ]] \
        || med_die 'Property-Werte dürfen keine Zeilenumbrüche enthalten.'
    value=${value//\\/\\\\}
    printf '%s' "$value"
}

med_write_shell_assignment() {
    local destination=$1
    local key=$2
    local value=$3
    printf '%s=%q\n' "$key" "$value" >> "$destination"
}

med_random_hex() {
    local bytes=${1:-32}
    openssl rand -hex "$bytes"
}

med_utc_timestamp() {
    date -u '+%Y-%m-%dT%H:%M:%SZ'
}

med_compact_timestamp() {
    date -u '+%Y%m%dT%H%M%SZ'
}
