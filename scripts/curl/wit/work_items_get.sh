#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_items.md
# Operación: Get Work Item (GET)
# Nivel: Proyecto (nota: también funciona a nivel organización si se omite --project)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_get.sh [--project <project>] --id <id> [--fields <listaCampos>] [--expand <None|Relations|Links|All>] [--as-of <fechaISO>] [--api-version <ver>] [--raw]

Fuente: api_doc/wit_sections/work_items.md

Parámetros:
  --project       Proyecto (opcional; si se omite, consulta por ID a nivel organización)
  --id            ID del work item (obligatorio)
  --fields        Lista separada por comas de referenceNames a devolver
  --expand        None|Relations|Links|All (opcional) (se envía como $expand en la URL)
  --as-of         Fecha/hora ISO (opcional)
  --api-version   Override versión API (default 7.2-preview)
  --raw           Mostrar JSON crudo
  -h|--help       Mostrar ayuda

Ejemplos:
  scripts/curl/wit/work_items_get.sh --project Proj --id 123
  scripts/curl/wit/work_items_get.sh --id 123                               # nivel organización (sin proyecto)
  scripts/curl/wit/work_items_get.sh --project "Proj con espacios" --id 123 --fields System.Id,System.Title,System.State --expand Relations
USAGE
}

PROJECT=""
ID=""
FIELDS=""
EXPAND=""
AS_OF=""
API_VER="7.2-preview"
RAW=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --fields) FIELDS="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    --as-of) AS_OF="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

[[ -z "$ID" ]] && echo "Falta --id" >&2 && usage && exit 2

if [[ -n "$PROJECT" ]]; then
  ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
  URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitems/${ID}?api-version=${API_VER}"
else
  # Modo organización: sin segmento de proyecto
  URL="${DEVOPS_BASE}/_apis/wit/workitems/${ID}?api-version=${API_VER}"
fi

if [[ -n "$FIELDS" ]]; then
  ENC_FIELDS=$(jq -rn --arg s "$FIELDS" '$s|@uri')
  URL+="&fields=${ENC_FIELDS}"
fi
if [[ -n "$EXPAND" ]]; then
  ENC_EXPAND=$(jq -rn --arg s "$EXPAND" '$s|@uri')
  URL+="&\$expand=${ENC_EXPAND}"
fi
if [[ -n "$AS_OF" ]]; then
  ENC_ASOF=$(jq -rn --arg s "$AS_OF" '$s|@uri')
  URL+="&asOf=${ENC_ASOF}"
fi

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
