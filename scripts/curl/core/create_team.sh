#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> -n <name> [-d <description>]" >&2; exit 1; }
PROJECT_ID=${1:-}; shift || true
[[ -z "$PROJECT_ID" ]] && usage
NAME=""; DESC=""
while getopts ":n:d:" opt; do
  case $opt in
    n) NAME="$OPTARG" ;;
    d) DESC="$OPTARG" ;;
    *) usage ;;
  esac
done
[[ -z "$NAME" ]] && usage

DATA=$(jq -n --arg n "$NAME" --arg d "$DESC" '{name:$n, description: ($d|select(length>0))} | with_entries(select(.value != null))')
URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}/teams?api-version=7.2-preview.3"

curl -sS -X POST \
  -H "Authorization: Basic ${BASIC_AUTH}" \
  -H 'Content-Type: application/json' \
  -d "$DATA" \
  "$URL" | jq .
