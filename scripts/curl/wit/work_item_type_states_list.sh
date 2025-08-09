#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_type_states.md
# Operación: List (GET)
# Nivel: Proyecto (requiere project) + tipo (path)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_type_states_list.sh --project <project> --type <tipo> [--api-version <ver>] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_type_states.md

Descripción:
  Lista los estados definidos para un tipo de work item (name, color, order) en un proyecto.

Parámetros:
  --project      Nombre o ID del proyecto (obligatorio)
  --type         Nombre del tipo de work item (obligatorio, respeta mayúsculas y espacios)
  --api-version  Override de versión API (opcional, default 7.2-preview)
  --raw          Mostrar respuesta JSON sin formatear (sin jq)
  --help         Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_type_states_list.sh --project MiProyecto --type Bug
  scripts/curl/wit/work_item_type_states_list.sh --project Gerencia_Tecnologia --type "User Story"
  scripts/curl/wit/work_item_type_states_list.sh --project Gerencia_Tecnologia --type Tarea --api-version 7.2-preview
USAGE
}

PROJECT=""
TYPE=""
API_VER="7.2-preview"
RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "$TYPE" ]]; then echo "Falta --type" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TYPE=$(jq -rn --arg s "$TYPE" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypes/${ENC_TYPE}/states?api-version=${API_VER}"

set +e
RESP=$(curl_json "$URL")
CURL_EXIT=$?
set -e
if [[ $CURL_EXIT -ne 0 || -z "$RESP" ]]; then
  echo "Error al invocar endpoint (exit=$CURL_EXIT)." >&2
  echo "Raw:" >&2
  printf '%s\n' "$RESP" >&2
  exit 3
fi

if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then
    printf '%s\n' "$RESP"
  else
    echo "$RESP" | jq .
  fi
else
  echo "Respuesta no es JSON válido." >&2
  printf '%s\n' "$RESP" >&2
  exit 4
fi
