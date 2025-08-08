#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=../_env.sh
source "${SCRIPT_DIR}/../_env.sh"

usage() { echo "Uso: $0 <projectId> <teamId> [-n <newName>] [-d <newDescription>]" >&2; exit 1; }
PROJECT_ID=${1:-}; TEAM_ID=${2:-}; shift 2 || true
[[ -z "$PROJECT_ID" || -z "$TEAM_ID" ]] && usage
NEW_NAME=""; NEW_DESC=""
while getopts ":n:d:" opt; do
  case $opt in
    n) NEW_NAME="$OPTARG" ;;
    d) NEW_DESC="$OPTARG" ;;
    *) usage ;;
  esac
done

DATA=$(jq -n --arg n "$NEW_NAME" --arg d "$NEW_DESC" '{ name: ($n|select(length>0)), description: ($d|select(length>0)) } | with_entries(select(.value != null))')
URL="${DEVOPS_BASE}/_apis/projects/${PROJECT_ID}/teams/${TEAM_ID}?api-version=7.2-preview.3"

curl -sS -X PATCH \
  -H "Authorization: Basic ${BASIC_AUTH}" \
  -H 'Content-Type: application/json' \
  -d "$DATA" \
  "$URL" | jq .
