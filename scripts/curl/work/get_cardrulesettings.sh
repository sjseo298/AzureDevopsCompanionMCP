#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  echo "Uso: $0 <project> <team> <board>" >&2
  echo "\nParámetros (api_doc/work/cardrulesettings.md):" >&2
  echo "  project   Nombre o ID del proyecto (obligatorio)" >&2
  echo "  team      Nombre o ID del equipo (obligatorio)" >&2
  echo "  board     ID o nombre del tablero (obligatorio)" >&2
  exit 1
}

PROJECT=${1:-}
TEAM=${2:-}
BOARD=${3:-}
[[ -z "$PROJECT" || -z "$TEAM" || -z "$BOARD" ]] && usage

# URL-encode helper
urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")
BOARD_ENC=$(urlencode "$BOARD")

# Según la documentación local, esta API usa 7.2-preview.2
URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/boards/${BOARD_ENC}/cardrulesettings?api-version=7.2-preview.2"

curl_json "$URL" | jq .
