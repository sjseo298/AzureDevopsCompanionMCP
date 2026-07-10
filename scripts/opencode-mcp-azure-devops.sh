#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

IMAGE="${AZURE_DEVOPS_MCP_IMAGE:-mcp-azure-devops:latest}"
ENV_FILE="${AZURE_DEVOPS_MCP_ENV_FILE:-${PROJECT_ROOT}/.env}"
HOST="${AZURE_DEVOPS_MCP_HOST:-127.0.0.1}"
PORT="${AZURE_DEVOPS_MCP_HTTP_PORT:-9091}"
GIT_PERSIST="${AZURE_DEVOPS_MCP_GIT_PERSIST:-false}"
GIT_VOLUME="${AZURE_DEVOPS_MCP_GIT_VOLUME:-mcp-azure-devops-git-workspace}"
GIT_BIND="${AZURE_DEVOPS_MCP_GIT_BIND:-}"
GIT_ROOT="${AZURE_DEVOPS_MCP_GIT_ROOT:-/tmp/mcp-git}"

docker_args=(
  run --rm -i
  --pull never
  --env "MCP_PUBLIC_BASE_URL=http://${HOST}:${PORT}"
  --env "MCP_GIT_WORKSPACE_ROOT=${GIT_ROOT}"
)

if [[ -f "${ENV_FILE}" ]]; then
  docker_args+=(--env-file "${ENV_FILE}")
fi

if [[ "${GIT_PERSIST}" == "true" ]]; then
  docker_args+=(--env "MCP_GIT_PERSISTENT=true")
  if [[ -n "${GIT_BIND}" ]]; then
    mkdir -p "${GIT_BIND}"
    docker_args+=(-v "${GIT_BIND}:${GIT_ROOT}")
  else
    docker_args+=(-v "${GIT_VOLUME}:${GIT_ROOT}")
  fi
else
  docker_args+=(--env "MCP_GIT_PERSISTENT=false")
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
