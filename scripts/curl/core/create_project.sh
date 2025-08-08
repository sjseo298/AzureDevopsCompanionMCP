#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() {
  cat >&2 <<EOF
Uso: $0 -n <name> [-d <description>] [-p <processType>] [-v <visibility>] [-s <sourceControlType>]
  -n  Nombre del proyecto (requerido)
  -d  Descripción (opcional)
  -p  Proceso (Agile|Scrum|CMMI) u otro processTypeId GUID (requerido)
  -v  Visibilidad (private|public) (requerido)
  -s  Tipo de control de versiones (Git|TFVC) (requerido)
EOF
  exit 1
}

NAME=""; DESC=""; PROCESS=""; VISIBILITY=""; SOURCE=""
while getopts ":n:d:p:v:s:" opt; do
  case $opt in
    n) NAME="$OPTARG" ;;
    d) DESC="$OPTARG" ;;
    p) PROCESS="$OPTARG" ;;
    v) VISIBILITY="$OPTARG" ;;
    s) SOURCE="$OPTARG" ;;
    *) usage ;;
  esac
done
[[ -z "$NAME" || -z "$PROCESS" || -z "$VISIBILITY" || -z "$SOURCE" ]] && usage

DATA=$(jq -n --arg name "$NAME" --arg desc "$DESC" --arg proc "$PROCESS" --arg vis "$VISIBILITY" --arg src "$SOURCE" '
{
  name: $name,
  description: ($desc | select(length>0)),
  visibility: $vis,
  capabilities: {
    versioncontrol: { sourceControlType: $src },
    processTemplate: { templateTypeId: $proc }
  }
} | with_entries(select(.value != null))')

URL="${DEVOPS_BASE}/_apis/projects?api-version=7.2-preview.4"

curl -sS -X POST \
  -H "Authorization: Basic ${BASIC_AUTH}" \
  -H 'Content-Type: application/json' \
  -d "$DATA" \
  "$URL" | jq .
