#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_relation_types.md
# Operación: Get (GET)
# Nivel: Organización (no requiere project en ruta)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_relation_types_get.sh --reference <referenceName> [--help]

Fuente: api_doc/wit_sections/work_item_relation_types.md

Descripción:
  Obtiene un tipo de relación específico por su referenceName.

Parámetros:
  --reference  referenceName del tipo de relación (obligatorio, ej: System.LinkTypes.Hierarchy-Forward)
  --help       Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_relation_types_get.sh --reference System.LinkTypes.Hierarchy-Forward
USAGE
}

REFERENCE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --reference) REFERENCE="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$REFERENCE" ]]; then
  echo "Falta --reference" >&2; usage; exit 2
fi

ENC_REF=$(jq -rn --arg s "$REFERENCE" '$s|@uri')
API_VER="7.2-preview"
URL="${DEVOPS_BASE}/_apis/wit/workitemrelationtypes/${ENC_REF}?api-version=${API_VER}"

curl_json "$URL" | jq .
