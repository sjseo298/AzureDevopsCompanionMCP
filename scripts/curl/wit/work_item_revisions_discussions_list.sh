#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_revisions_discussions.md
# Operación: Read Reporting Discussions (GET)
# Nivel: Proyecto (requiere project en ruta)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_revisions_discussions_list.sh --project <project> [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_revisions_discussions.md

Descripción:
  Recupera discusiones (comentarios por revisión) de work items vía endpoint reporting.

Parámetros:
  --project  Nombre o ID del proyecto (obligatorio)
  --raw      Imprime la respuesta sin formatear (sin jq)
  --help     Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_revisions_discussions_list.sh --project MiProyecto
  scripts/curl/wit/work_item_revisions_discussions_list.sh --project "Gerencia_Tecnologia"
USAGE
}

PROJECT=""
RAW=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then
  echo "Falta --project" >&2; usage; exit 2
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
API_VER="7.2-preview"
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/reporting/workitemrevisionsdiscussions?api-version=${API_VER}"

# Capturamos la respuesta (aunque sea error) sin abortar por pipefail en jq posterior
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

# Intentar detectar JSON válido
if echo "$RESP" | jq empty >/dev/null 2>&1; then
  if [[ $RAW -eq 1 ]]; then
    printf '%s\n' "$RESP"
  else
    echo "$RESP" | jq .
  fi
else
  echo "Respuesta no es JSON válido (posible HTML o texto de error)." >&2
  printf '%s\n' "$RESP" >&2
  exit 4
fi
