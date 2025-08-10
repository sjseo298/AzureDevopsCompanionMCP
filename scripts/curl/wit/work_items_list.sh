#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_items.md
# Operación: List (GET multiple by ids)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_list.sh --project <project> --ids <listaIds> [--fields <listaCampos>] [--expand <None|Relations|Links|All>] [--as-of <fechaISO>] [--api-version <ver>] [--raw]

Fuente: api_doc/wit_sections/work_items.md

Parámetros:
  --project       Proyecto (obligatorio)
  --ids           Lista de IDs separados por coma (máx 200) (obligatorio)
  --fields        Lista separada por comas de referenceNames a devolver
  --expand        None|Relations|Links|All (opcional)
  --as-of         Fecha/hora ISO (opcional)
  --api-version   Override versión API (default 7.2-preview)
  --raw           Mostrar JSON crudo
  -h|--help       Ayuda

Ejemplos:
  scripts/curl/wit/work_items_list.sh --project Proj --ids 10,20,30 --fields System.Id,System.Title
  scripts/curl/wit/work_items_list.sh --project Proj --ids 10,11 --expand Relations
USAGE
}

PROJECT=""
IDS=""
FIELDS=""
EXPAND=""
AS_OF=""
API_VER="7.2-preview"
RAW=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --ids) IDS="$2"; shift 2;;
    --fields) FIELDS="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    --as-of) AS_OF="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

[[ -z "$PROJECT" ]] && echo "Falta --project" >&2 && usage && exit 2
[[ -z "$IDS" ]] && echo "Falta --ids" >&2 && usage && exit 2

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitems?ids=$(jq -rn --arg s "$IDS" '$s|@uri')&api-version=${API_VER}"

if [[ -n "$FIELDS" ]]; then URL+="&fields=$(jq -rn --arg s "$FIELDS" '$s|@uri')"; fi
if [[ -n "$EXPAND" ]]; then URL+="&expand=$(jq -rn --arg s "$EXPAND" '$s|@uri')"; fi
if [[ -n "$AS_OF" ]]; then URL+="&asOf=$(jq -rn --arg s "$AS_OF" '$s|@uri')"; fi

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
