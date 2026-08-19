#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=deploy/lib.sh
source "$SCRIPT_DIR/../lib.sh"
# shellcheck source=deploy/operator/runtime.sh
source "$SCRIPT_DIR/../operator/runtime.sh"

failures=0

fail() {
    printf 'FAIL: %s\n' "$1" >&2
    failures=$((failures + 1))
}

assert_failure_without_secret() {
    local description=$1
    local secret=$2
    shift 2
    local output
    if output=$("$@" 2>&1); then
        fail "$description sollte fehlschlagen."
        return
    fi
    [[ $output != *"$secret"* ]] || fail "$description darf keinen Secretwert ausgeben."
}

test_root=$(mktemp -d)
trap 'rm -rf "$test_root"' EXIT
REPOSITORY_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
DEPLOY_ROOT="$test_root/runtime"
INSTANCES_DIR="$DEPLOY_ROOT/instances"
ACCEPTANCE_DIR="$INSTANCES_DIR/acceptance"
DISCORD_PROPERTIES="$DEPLOY_ROOT/discord.properties"
OPENAI_PROPERTIES="$DEPLOY_ROOT/openai.properties"
ACCEPTANCE_PROPERTIES="$DEPLOY_ROOT/acceptance.properties"
mkdir -p "$ACCEPTANCE_DIR"

cat > "$DISCORD_PROPERTIES" <<'EOF_DISCORD'
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=production-discord-secret
mise-en-dice.discord.guild-id=123456789
mise-en-dice.discord.challenge-operator-role-id=555555555
mise-en-dice.discord.participant-user-ids.GEORGIA=111111111
mise-en-dice.discord.participant-user-ids.TOBIAS=222222222
EOF_DISCORD
cat > "$OPENAI_PROPERTIES" <<'EOF_OPENAI'
mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.api-key=production-openai-secret
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
EOF_OPENAI
cat > "$ACCEPTANCE_PROPERTIES" <<'EOF_ACCEPTANCE'
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=acceptance-discord-secret
mise-en-dice.discord.guild-id=987654321
mise-en-dice.discord.challenge-operator-role-id=666666666
mise-en-dice.discord.participant-user-ids.GEORGIA=333333333
mise-en-dice.discord.participant-user-ids.TOBIAS=444444444
mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.api-key=acceptance-openai-secret
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
EOF_ACCEPTANCE
chmod 0600 "$DISCORD_PROPERTIES" "$OPENAI_PROPERTIES" "$ACCEPTANCE_PROPERTIES"

# Git for Windows can emulate chmod as a read-only flag rather than POSIX mode bits. The deployment
# operator itself runs on Debian, where the real stat-based check remains active. Locally, substitute
# only that mode read so the remaining isolation checks still execute.
posix_modes_supported=true
if (( (8#$(stat -c '%a' "$ACCEPTANCE_PROPERTIES") & 0077) != 0 )); then
    posix_modes_supported=false
    printf 'NOTE: POSIX-Dateirechte werden von dieser Shell nicht abgebildet; simuliere 0600 für Isolationstests.\n'
    stat() {
        if [[ ${1:-} == -c && ${2:-} == %a ]]; then
            printf '600\n'
        else
            command stat "$@"
        fi
    }
fi

if ! validate_acceptance_provider_configuration; then
    fail 'Vollständige, getrennte Acceptance-Konfiguration wird akzeptiert.'
fi

sed -i 's/acceptance-discord-secret/production-discord-secret/' "$ACCEPTANCE_PROPERTIES"
assert_failure_without_secret \
    'Gleiche Discord-Secrets zwischen Acceptance und Produktion' \
    'production-discord-secret' \
    validate_acceptance_provider_configuration

cat > "$ACCEPTANCE_PROPERTIES" <<'EOF_DISABLED'
mise-en-dice.discord.enabled=false
mise-en-dice.curation.openai.enabled=false
EOF_DISABLED
chmod 0600 "$ACCEPTANCE_PROPERTIES"
if ! validate_acceptance_provider_configuration; then
    fail 'Explizit deaktivierte Acceptance-Provider werden akzeptiert.'
fi

# These calls intentionally run at top level under set -e. A missing optional production provider file
# must therefore return success rather than inheriting the failed [[ -e ... ]] status.
mv "$DISCORD_PROPERTIES" "$DISCORD_PROPERTIES.saved"
mv "$OPENAI_PROPERTIES" "$OPENAI_PROPERTIES.saved"
validate_acceptance_provider_configuration
mv "$DISCORD_PROPERTIES.saved" "$DISCORD_PROPERTIES"
validate_acceptance_provider_configuration
mv "$DISCORD_PROPERTIES" "$DISCORD_PROPERTIES.saved"
mv "$OPENAI_PROPERTIES.saved" "$OPENAI_PROPERTIES"
validate_acceptance_provider_configuration
mv "$DISCORD_PROPERTIES.saved" "$DISCORD_PROPERTIES"

cat > "$ACCEPTANCE_PROPERTIES" <<'EOF_INCOMPLETE_DISCORD'
mise-en-dice.discord.enabled=true
mise-en-dice.discord.guild-id=987654321
mise-en-dice.discord.challenge-operator-role-id=666666666
mise-en-dice.discord.participant-user-ids.GEORGIA=333333333
mise-en-dice.discord.participant-user-ids.TOBIAS=444444444
mise-en-dice.curation.openai.enabled=false
EOF_INCOMPLETE_DISCORD
chmod 0600 "$ACCEPTANCE_PROPERTIES"
assert_failure_without_secret \
    'Aktiviertes Discord ohne Token' \
    'acceptance-discord-secret' \
    validate_acceptance_provider_configuration

cat > "$ACCEPTANCE_PROPERTIES" <<'EOF_MISSING_OPERATOR_ROLE'
mise-en-dice.discord.enabled=true
mise-en-dice.discord.token=acceptance-discord-secret
mise-en-dice.discord.guild-id=987654321
mise-en-dice.discord.participant-user-ids.GEORGIA=333333333
mise-en-dice.discord.participant-user-ids.TOBIAS=444444444
mise-en-dice.curation.openai.enabled=false
EOF_MISSING_OPERATOR_ROLE
chmod 0600 "$ACCEPTANCE_PROPERTIES"
assert_failure_without_secret \
    'Aktiviertes Discord ohne Challenge-Operator-Rolle' \
    'acceptance-discord-secret' \
    validate_acceptance_provider_configuration

cat > "$ACCEPTANCE_PROPERTIES" <<'EOF_INCOMPLETE_OPENAI'
mise-en-dice.discord.enabled=false
mise-en-dice.curation.openai.enabled=true
mise-en-dice.curation.openai.model=gpt-5.6-terra
mise-en-dice.curation.openai.reasoning-effort=medium
EOF_INCOMPLETE_OPENAI
chmod 0600 "$ACCEPTANCE_PROPERTIES"
assert_failure_without_secret \
    'Aktiviertes OpenAI ohne API-Key' \
    'acceptance-openai-secret' \
    validate_acceptance_provider_configuration

cat > "$ACCEPTANCE_PROPERTIES" <<'EOF_DISABLED_AGAIN'
mise-en-dice.discord.enabled=false
mise-en-dice.curation.openai.enabled=false
EOF_DISABLED_AGAIN
chmod 0600 "$ACCEPTANCE_PROPERTIES"

if [[ $posix_modes_supported == true ]]; then
    chmod 0400 "$ACCEPTANCE_PROPERTIES"
    if ! validate_runtime_secret_file "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration'; then
        fail 'Secretdatei mit 0400 wird akzeptiert.'
    fi
    chmod 0600 "$ACCEPTANCE_PROPERTIES"
    if ! validate_runtime_secret_file "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration'; then
        fail 'Secretdatei mit 0600 wird akzeptiert.'
    fi
    chmod 0700 "$ACCEPTANCE_PROPERTIES"
    assert_failure_without_secret \
        'Secretdatei mit Owner-Execute-Recht 0700' \
        'acceptance-openai-secret' \
        validate_runtime_secret_file "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration'
    chmod 0640 "$ACCEPTANCE_PROPERTIES"
    assert_failure_without_secret \
        'Secretdatei mit Gruppenrecht 0640' \
        'acceptance-openai-secret' \
        validate_runtime_secret_file "$ACCEPTANCE_PROPERTIES" 'Acceptance-Provider-Konfiguration'
    chmod 0600 "$ACCEPTANCE_PROPERTIES"
else
    printf 'NOTE: POSIX-Modustests 0400/0600/0640/0700 werden nur auf Linux-CI ausgeführt.\n'
fi

if ln -s "$DISCORD_PROPERTIES" "$ACCEPTANCE_PROPERTIES.link" 2>/dev/null \
    && [[ -L $ACCEPTANCE_PROPERTIES.link ]]; then
    assert_failure_without_secret \
        'Symlink-Providerdatei' \
        'production-discord-secret' \
        validate_runtime_secret_file "$ACCEPTANCE_PROPERTIES.link" 'Acceptance-Provider-Konfiguration'
else
    printf 'NOTE: Symlinks werden von dieser Shell nicht abgebildet; Linux-CI prueft diesen Fall.\n'
fi

mkdir -p "$INSTANCES_DIR/preview-collision"
cat > "$INSTANCES_DIR/preview-collision/metadata" <<'EOF_METADATA'
PORT=18090
EOF_METADATA
ACCEPTANCE_PORT=18090
if ! acceptance_port_is_reserved_elsewhere; then
    fail 'Acceptance-Portkollision mit Preview-Metadaten wird erkannt.'
fi

if (( failures > 0 )); then
    printf '%s Runtime-Test(s) failed.\n' "$failures" >&2
    exit 1
fi

printf 'All deployment runtime tests passed.\n'
