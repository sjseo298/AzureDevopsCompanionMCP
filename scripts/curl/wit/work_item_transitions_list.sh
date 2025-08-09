#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_transitions.md
# Operación: List (GET)
# Nivel: Organización (NO requiere project en la ruta)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_transitions_list.sh --ids <id1,id2,...> [--api-version <ver>] [--action checkin] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_transitions.md

Descripción:
  Lista el siguiente estado posible (stateOnTransition) para cada work item indicado.
  Endpoint organization-level: /_apis/wit/workitemtransitions

Parámetros:
  --ids          Lista de IDs separados por coma (obligatorio)
  --api-version  Override de versión API (opcional, default 7.1)
  --action       Acción opcional (actualmente sólo 'checkin' soportado oficialmente)
  --raw          Mostrar respuesta JSON sin pipe a jq
  --help         Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_transitions_list.sh --ids 123
  scripts/curl/wit/work_item_transitions_list.sh --ids 877023,875205
  scripts/curl/wit/work_item_transitions_list.sh --ids 877023 --action checkin
  scripts/curl/wit/work_item_transitions_list.sh --ids 877023 --api-version 7.1
USAGE
}

IDS=""
API_VER="7.1"
RAW=0
ACTION=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --ids) IDS="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --action) ACTION="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$IDS" ]]; then echo "Falta --ids" >&2; usage; exit 2; fi

ENC_IDS=$(jq -rn --arg s "$IDS" '$s|@uri')
BASE_URL="${DEVOPS_BASE}/_apis/wit/workitemtransitions?ids=${ENC_IDS}&api-version=${API_VER}"
if [[ -n "$ACTION" ]]; then
  ENC_ACTION=$(jq -rn --arg s "$ACTION" '$s|@uri')
  BASE_URL+="&action=${ENC_ACTION}"
fi
URL="$BASE_URL"

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
  echo "Respuesta no es JSON válido (posible HTML o endpoint no habilitado)." >&2
  printf '%s\n' "$RESP" >&2
  exit 4
fi
