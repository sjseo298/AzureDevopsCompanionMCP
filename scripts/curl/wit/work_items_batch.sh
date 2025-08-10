#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_items.md
# Operación: Get Work Items Batch (POST workitemsbatch)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_batch.sh --project <project> --ids <listaIds> [--fields <listaCampos>] [--as-of <fechaISO>] [--error-policy <Omit|Fail>] [--api-version <ver>] [--raw]

Fuente: api_doc/wit_sections/work_items.md

Parámetros:
  --project       Proyecto (obligatorio)
  --ids           Lista de IDs separados por coma (máx 200) (obligatorio)
  --fields        Lista separada por comas de referenceNames a devolver
  --as-of         Fecha/hora ISO (opcional)
  --error-policy  Omit|Fail (default Omit)
  --api-version   Override versión API (default 7.2-preview)
  --raw           Mostrar JSON crudo
  -h|--help       Ayuda

Ejemplos:
  scripts/curl/wit/work_items_batch.sh --project Proj --ids 10,20,30 --fields System.Id,System.Title
  scripts/curl/wit/work_items_batch.sh --project Proj --ids 10,20 --error-policy Fail
USAGE
}

PROJECT=""
IDS=""
FIELDS=""
AS_OF=""
ERROR_POLICY="Omit"
API_VER="7.2-preview"
RAW=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --ids) IDS="$2"; shift 2;;
    --fields) FIELDS="$2"; shift 2;;
    --as-of) AS_OF="$2"; shift 2;;
    --error-policy) ERROR_POLICY="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

[[ -z "$PROJECT" ]] && echo "Falta --project" >&2 && usage && exit 2
[[ -z "$IDS" ]] && echo "Falta --ids" >&2 && usage && exit 2

JSON_BODY=$(jq -n \
  --arg ids "$IDS" \
  --arg fields "$FIELDS" \
  --arg asof "$AS_OF" \
  --arg policy "$ERROR_POLICY" '
    ( {ids: ($ids|split(",")|map(.|gsub(" ";""))|map(select(length>0))|map(tonumber))} )
    | ( if ($fields|length)>0 then .+{fields: ($fields|split(",")|map(.|gsub(" ";""))|map(select(length>0)))} else . end )
    | ( if ($asof|length)>0 then .+{asOf: $asof} else . end )
    | ( .+{errorPolicy: ( if ($policy|length)>0 then $policy else "Omit" end )} )
')

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemsbatch?api-version=${API_VER}"
RESP=$(curl -sS -H "Content-Type: application/json" -H "Accept: application/json" -u ":${AZURE_DEVOPS_PAT}" -X POST "$URL" -d "$JSON_BODY") || { echo "Error al invocar endpoint" >&2; exit 3; }

if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then printf '%s\n' "$RESP"; else echo "$RESP" | jq .; fi
else
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi
