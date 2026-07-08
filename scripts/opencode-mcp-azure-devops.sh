#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

IMAGE="${AZURE_DEVOPS_MCP_IMAGE:-mcp-azure-devops:latest}"
ENV_FILE="${AZURE_DEVOPS_MCP_ENV_FILE:-${PROJECT_ROOT}/.env}"
HOST="${AZURE_DEVOPS_MCP_HOST:-127.0.0.1}"
PORT="${AZURE_DEVOPS_MCP_HTTP_PORT:-9091}"

docker_args=(
  run --rm -i
  --pull never
  --env "MCP_PUBLIC_BASE_URL=http://${HOST}:${PORT}"
)

if [[ -f "${ENV_FILE}" ]]; then
  docker_args+=(--env-file "${ENV_FILE}")
fi

port_available=true
if docker ps --format '{{.Ports}}' | grep -Eq "(^|, )(${HOST}|0\.0\.0\.0|:::|\[::\]):${PORT}->|(^|, )${PORT}->"; then
  port_available=false
fi

if [[ "${port_available}" == true ]]; then
  docker_args+=(
    -p "${HOST}:${PORT}:8080"
  )
else
  echo "WARN: ${HOST}:${PORT} is already in use; starting MCP stdio-http without publishing HTTP port. Upload URLs will use the existing service on that port." >&2
fi

exec docker "${docker_args[@]}" "${IMAGE}" stdio-http
