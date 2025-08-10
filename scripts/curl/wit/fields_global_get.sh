#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit.md (concepto de campos globales) y necesidad de inspeccionar metadata de campos
# Operación: Get Global Field (GET _apis/wit/fields/{referenceName})
# Nivel: Organización

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: fields_global_get.sh --field <referenceName> [--api-version <ver>] [--raw]

Fuente: api_doc/wit_sections/fields (global) (no hay archivo dedicado, se documenta aquí por necesidad de metadata)

Descripción:
  Obtiene definición global de un campo (tipo, picklistId, readOnly, usage, etc.)

Parámetros:
  --field         referenceName del campo (obligatorio)
  --api-version   Versión API (default 7.2-preview)
  --raw           Mostrar JSON crudo
  -h|--help       Ayuda

Ejemplos:
  scripts/curl/wit/fields_global_get.sh --field System.State
  scripts/curl/wit/fields_global_get.sh --field Custom.TipodeHistoria
USAGE
}

FIELD=""; API_VER="7.2-preview"; RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --field) FIELD="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Arg no reconocido: $1" >&2; usage; exit 1;;
  esac
done

[[ -z "$FIELD" ]] && echo "Falta --field" >&2 && usage && exit 2
ENC_FIELD=$(jq -rn --arg s "$FIELD" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/wit/fields/${ENC_FIELD}?api-version=${API_VER}"
RESP=$(curl_json "$URL") || { echo "Error endpoint" >&2; exit 3; }
if ! echo "$RESP" | jq empty >/dev/null 2>&1; then echo "Respuesta no JSON" >&2; printf '%s\n' "$RESP" >&2; exit 4; fi
if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq '{referenceName,name,type,usage,readOnly,picklistId,values?:(.values?)}'; fi
