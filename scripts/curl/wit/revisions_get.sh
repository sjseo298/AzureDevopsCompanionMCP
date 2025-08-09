#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/revisions.md (Get Revision)
# GET /{organization}/{project}/_apis/wit/workItems/{id}/revisions/{rev}?api-version=7.2-preview.3

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: revisions_get.sh --project <proyecto> --id <workItemId> --rev <revision> [--expand <none|relations|fields|links|all>]

Fuente: api_doc/wit_sections/revisions.md (Get)

Descripción:
  Obtiene una revisión específica de un work item.
  api-version 7.2-preview.3.

Parámetros:
  --project  Proyecto (obligatorio)
  --id       ID del work item (obligatorio)
  --rev      Número de la revisión (obligatorio)
  --expand   none|relations|fields|links|all (opcional)
  -h|--help  Ayuda

Ejemplos:
  scripts/curl/wit/revisions_get.sh --project "Mi Proyecto" --id 123 --rev 2
  scripts/curl/wit/revisions_get.sh --project "Mi Proyecto" --id 123 --rev 2 --expand fields
USAGE
}

PROJECT=""; ID=""; REV=""; EXPAND=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --rev) REV="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$ID" || -z "$REV" ]]; then echo "--project, --id y --rev son obligatorios" >&2; usage; exit 2; fi
if ! [[ "$ID" =~ ^[0-9]+$ && "$REV" =~ ^[0-9]+$ ]]; then echo "--id y --rev deben ser numéricos" >&2; exit 3; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workItems/${ID}/revisions/${REV}?api-version=7.2-preview.3"
if [[ -n "$EXPAND" ]]; then URL+="&%24expand=$(jq -rn --arg s "$EXPAND" '$s|@uri')"; fi

RESP=$(curl_json "$URL")
if ! printf '%s' "$RESP" | head -c1 | grep -q '[{]'; then
  echo "Respuesta no JSON recibida (primeros 400 chars):" >&2
  printf '%s' "$RESP" | head -c400 >&2
  exit 91
fi

printf '%s' "$RESP" | jq '{id, rev, title: .fields["System.Title"], state: .fields["System.State"], type: .fields["System.WorkItemType"]}'
