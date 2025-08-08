#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<EOF
Uso: $0 <project> <team> <board>

Parámetros (requeridos según api_doc/work/rows.md):
  project   Nombre o ID del proyecto
  team      Nombre o ID del equipo
  board     ID o nombre del tablero

Ejemplos:
  $0 MiProyecto MiEquipo MiTablero
  $0 MiProyecto "Equipo con espacios" "Tablero Principal"
EOF
  exit 1
}

PROJECT=${1:-}
TEAM=${2:-}
BOARD=${3:-}
[[ -z "$PROJECT" || -z "$TEAM" || -z "$BOARD" ]] && usage

# URL-encode helper usando jq
urlencode() { jq -rn --arg s "$1" '$s|@uri'; }

PROJECT_ENC=$(urlencode "$PROJECT")
TEAM_ENC=$(urlencode "$TEAM")
BOARD_ENC=$(urlencode "$BOARD")

URL="${DEVOPS_BASE}/${PROJECT_ENC}/${TEAM_ENC}/_apis/work/boards/${BOARD_ENC}/rows?api-version=${AZURE_DEVOPS_API_VERSION}"

curl_json "$URL" | jq .
