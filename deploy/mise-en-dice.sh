#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null) \
    || { printf 'ERROR: Das Deployment-Werkzeug muss aus einem Git-Checkout des Repositories laufen.\n' >&2; exit 1; }
# shellcheck source=deploy/lib.sh
source "$SCRIPT_DIR/lib.sh"

COMPOSE_FILE="$SCRIPT_DIR/compose.yaml"
DEPLOY_ROOT=${MISE_EN_DICE_DEPLOY_ROOT:-/opt/mise-en-dice/runtime}
OPERATOR_CONFIG="$DEPLOY_ROOT/operator.conf"
ADMIN_PROPERTIES="$DEPLOY_ROOT/admin.properties"
DISCORD_PROPERTIES="$DEPLOY_ROOT/discord.properties"
INSTANCES_DIR="$DEPLOY_ROOT/instances"
PREVIEWS_DIR="$INSTANCES_DIR/previews"
PRODUCTION_DIR="$INSTANCES_DIR/production"
WORKTREES_DIR="$DEPLOY_ROOT/worktrees"
BACKUPS_DIR="$DEPLOY_ROOT/backups"
TEMP_DIR="$DEPLOY_ROOT/tmp"
LOCKS_DIR="$DEPLOY_ROOT/locks"

# shellcheck source=deploy/operator/config.sh
source "$SCRIPT_DIR/operator/config.sh"
# shellcheck source=deploy/operator/runtime.sh
source "$SCRIPT_DIR/operator/runtime.sh"
# shellcheck source=deploy/operator/git-image.sh
source "$SCRIPT_DIR/operator/git-image.sh"
# shellcheck source=deploy/operator/database.sh
source "$SCRIPT_DIR/operator/database.sh"
# shellcheck source=deploy/operator/commands.sh
source "$SCRIPT_DIR/operator/commands.sh"

main "$@"
