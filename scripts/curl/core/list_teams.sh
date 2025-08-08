#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<'EOF'
Uso: $0 <projectIdOrName> [--top N] [--skip N]

Parámetros (api_doc/core.md › Teams/Get Teams):
  projectIdOrName  ID GUID o nombre del proyecto (obligatorio)
  --top            Número máximo de equipos a retornar (opcional)
  --skip           Número de equipos a omitir (opcional)

Ejemplos:
  $0 985807ad-7ff9-438d-849c-794c9bbc50f4 --top 50
  $0 Gerencia_Tecnologia --skip 100
EOF
  exit 1
}

PROJECT_ID=${1:-}; shift || true
[[ -z "$PROJECT_ID" ]] && usage
TOP=""; SKIP=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --top) TOP=${2:-}; shift 2;;
    --skip) SKIP=${2:-}; shift 2;;
    *) echo "Flag desconocida: $1" >&2; usage;;
  esac
done

urlencode() { jq -rn --arg s "$1" '$s|@uri'; }
PID_ENC=$(urlencode "$PROJECT_ID")

QP=("api-version=7.2-preview.3")
[[ -n "$TOP" ]] && QP+=("top=$TOP")
[[ -n "$SKIP" ]] && QP+=("skip=$SKIP")
QS="$(IFS='&'; echo "${QP[*]}")"
URL="${DEVOPS_BASE}/_apis/projects/${PID_ENC}/teams?${QS}"

RESPONSE=$(curl_json "$URL")
if echo "$RESPONSE" | jq . >/dev/null 2>&1; then
  echo "$RESPONSE" | jq
else
  echo "Respuesta no es JSON válido:" >&2
  echo "$RESPONSE"
  exit 1
fi
