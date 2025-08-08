#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<'EOF'
Uso: list_projects.sh [--state WellFormed|Creating|Deleting|New] [--top N] [--continuation TOKEN]

Parámetros (api_doc/core.md › Projects/List Projects):
  --state           Filtro de estado (opcional)
  --top             Número máximo de proyectos (opcional)
  --continuation    Continuation token para paginación (opcional)

Ejemplos:
  ./list_projects.sh --state WellFormed --top 10
  ./list_projects.sh --continuation abc123
EOF
  exit 1
}

STATE=""; TOP=""; CONT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --state) STATE=${2:-}; shift 2;;
    --top) TOP=${2:-}; shift 2;;
    --continuation) CONT=${2:-}; shift 2;;
    *) echo "Flag desconocida: $1" >&2; usage;;
  esac
done

QP=("api-version=7.2-preview.4")
[[ -n "$STATE" ]] && QP+=("stateFilter=$STATE")
[[ -n "$TOP" ]] && QP+=("top=$TOP")
[[ -n "$CONT" ]] && QP+=("continuationToken=$CONT")
QS="$(IFS='&'; echo "${QP[*]}")"
URL="${DEVOPS_BASE}/_apis/projects?${QS}"

RESPONSE=$(curl_json "$URL")
if echo "$RESPONSE" | jq . >/dev/null 2>&1; then
  echo "$RESPONSE" | jq
else
  echo "Respuesta no es JSON válido:" >&2
  echo "$RESPONSE"
  exit 1
fi
