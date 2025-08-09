#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/revisions.md (List Revisions)
# GET /{organization}/{project}/_apis/wit/workItems/{id}/revisions?api-version=7.2-preview.3

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: revisions_list.sh --project <proyecto> --id <workItemId> [--expand <none|relations|fields|links|all>] [--skip <n>] [--top <n>] [--raw]

Fuente: api_doc/wit_sections/revisions.md (List)

Descripción:
  Lista revisiones de un work item. Permite paginar con $skip y $top y expand opcional.
  api-version 7.2-preview.3.

Parámetros:
  --project  Proyecto (obligatorio)
  --id       ID del work item (obligatorio)
  --expand   none|relations|fields|links|all (opcional)
  --skip     Número de revisiones a omitir (opcional)
  --top      Número máximo a devolver (opcional)
  --raw      Muestra JSON sin filtrar (debug)
  -h|--help  Ayuda

Ejemplos:
  scripts/curl/wit/revisions_list.sh --project "Mi Proyecto" --id 123
  scripts/curl/wit/revisions_list.sh --project "Mi Proyecto" --id 123 --expand fields --top 5
USAGE
}

PROJECT=""; ID=""; EXPAND=""; SKIP=""; TOP=""; RAW=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    --skip) SKIP="$2"; shift 2;;
    --top) TOP="$2"; shift 2;;
    --raw) RAW=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$ID" ]]; then echo "--project y --id son obligatorios" >&2; usage; exit 2; fi
if ! [[ "$ID" =~ ^[0-9]+$ ]]; then echo "--id debe ser numérico" >&2; exit 3; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ID}/revisions?api-version=7.2-preview.3"
if [[ -n "$EXPAND" ]]; then URL+="&%24expand=$(jq -rn --arg s "$EXPAND" '$s|@uri')"; fi
if [[ -n "$SKIP" ]]; then URL+="&%24skip=$(jq -rn --arg s "$SKIP" '$s|@uri')"; fi
if [[ -n "$TOP" ]]; then URL+="&%24top=$(jq -rn --arg s "$TOP" '$s|@uri')"; fi

RESP=$(curl_json "$URL")

# Validar si parece JSON
if ! printf '%s' "$RESP" | head -c1 | grep -q '[{]'; then
  echo "Respuesta no JSON recibida (mostrar primeras 400 chars):" >&2
  printf '%s' "$RESP" | head -c400 >&2
  exit 91
fi

# Manejo de error remoto (message + typeKey)
if printf '%s' "$RESP" | jq -e '(.value? == null) and (.message? != null) and (.typeKey? != null)' >/dev/null 2>&1; then
  printf '%s' "$RESP" | jq -r '"Error remoto: \(.message) (type: \(.typeKey))"' >&2
  exit 92
fi

if $RAW; then
  printf '%s' "$RESP" | jq .
else
  printf '%s' "$RESP" | jq '{count, value: ( .value[0:5] | map({id, rev, title: .fields["System.Title"], state: .fields["System.State"]}) )}'
fi
