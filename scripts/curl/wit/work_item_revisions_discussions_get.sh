#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/work_item_revisions_discussions.md (Read Reporting Discussions)
# GET /{organization}/{project}/_apis/wit/reporting/workitemrevisionsdiscussions?api-version=7.2-preview

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: work_item_revisions_discussions_get.sh [--project <proyecto>] [--raw]

Fuente: api_doc/wit_sections/work_item_revisions_discussions.md (Read Reporting Discussions)

Descripción:
  Lista discusiones de revisiones de work items (reporting). Si no se pasa --project, consulta a nivel organización (si soportado).
  api-version 7.2-preview.

Parámetros:
  --project   Proyecto (opcional según doc local, recomendado)
  --raw       Muestra JSON completo
  -h|--help   Ayuda

Ejemplos:
  scripts/curl/wit/work_item_revisions_discussions_get.sh --project "Mi Proyecto"
  scripts/curl/wit/work_item_revisions_discussions_get.sh --raw
USAGE
}

PROJECT=""; RAW=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --raw) RAW=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

BASE_PATH="_apis/wit/reporting/workitemrevisionsdiscussions"; API_VER="7.2-preview"
if [[ -n "$PROJECT" ]]; then
  ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
  URL="${DEVOPS_BASE}/${ENC_PROJECT}/${BASE_PATH}?api-version=${API_VER}"
else
  URL="${DEVOPS_BASE}/${BASE_PATH}?api-version=${API_VER}"
fi

RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" "$URL" || true)
# Nueva detección de ausencia de endpoint (controller not found)
if ! jq -e . >/dev/null 2>&1 <<<"$RESP"; then
  if grep -qi "The controller for path" <<<"$RESP"; then
    echo "Endpoint no disponible en esta organización (controller not found): $URL" >&2
    exit 92
  fi
  echo "Respuesta no JSON" >&2; echo "$RESP" | head -c400 >&2; exit 90
fi

if jq -e '(.message? != null) and (.typeKey? != null)' >/dev/null 2>&1 <<<"$RESP"; then
  echo "Error remoto: $(echo "$RESP" | jq -r '.message') (type: $(echo "$RESP" | jq -r '.typeKey'))" >&2
  exit 91
fi

if $RAW; then
  echo "$RESP" | jq .
else
  echo "$RESP" | jq '{count, sample: (.value[0:3] | map({workItemId, revision, comments: (.discussion.comments|length) }))}'
fi
