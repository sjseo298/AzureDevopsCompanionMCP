#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_icons.md
# Operación: List (GET)
# Nivel: Organización (no requiere project en ruta)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_icons_list.sh [--help]

Fuente: api_doc/wit_sections/work_item_icons.md

Descripción:
  Lista los íconos de work item disponibles en la organización.

Ejemplos:
  scripts/curl/wit/work_item_icons_list.sh
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

API_VER="7.2-preview.1"
URL="${DEVOPS_BASE}/_apis/wit/workitemicons?api-version=${API_VER}"

curl_json "$URL" | jq .
