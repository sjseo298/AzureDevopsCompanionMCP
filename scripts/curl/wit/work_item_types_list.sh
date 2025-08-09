#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_types.md
# Operación: List (GET)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_types_list.sh --project <project> [--api-version <ver>] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_types.md

Descripción:
  Lista los tipos de work item del proyecto.

Parámetros:
  --project      Nombre o ID del proyecto (obligatorio)
  --api-version  Override de versión API (opcional, default 7.2-preview)
  --raw          Mostrar JSON crudo sin jq
  --help         Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_types_list.sh --project MiProyecto
  scripts/curl/wit/work_item_types_list.sh --project Gerencia_Tecnologia
USAGE
}

PROJECT=""
API_VER="7.2-preview"
RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypes?api-version=${API_VER}"

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
