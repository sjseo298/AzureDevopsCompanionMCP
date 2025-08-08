#!/usr/bin/env bash
set -euo pipefail

export AZURE_DEVOPS_ORGANIZATION="YOUR_ORGANIZATION"
export AZURE_DEVOPS_PAT="dxlchhaa5assxmppanr2l47dxyqx7uixvp7gj6o2fmix7gnkdl2q"

# Variables requeridas
: "${AZURE_DEVOPS_PAT:?AZURE_DEVOPS_PAT es requerido}"
: "${AZURE_DEVOPS_ORGANIZATION:?AZURE_DEVOPS_ORGANIZATION es requerido}"

# Versiones por defecto (se pueden sobrescribir desde el entorno)
: "${AZURE_DEVOPS_API_VERSION:=7.2-preview.1}"
: "${AZURE_DEVOPS_VSSPS_API_VERSION:=7.1}"

# Bases
DEVOPS_BASE="https://dev.azure.com/${AZURE_DEVOPS_ORGANIZATION}"
VSSPS_BASE="https://app.vssps.visualstudio.com"

# Header Authorization precomputado (Basic base64(":PAT"))
BASIC_AUTH=$(printf ":%s" "${AZURE_DEVOPS_PAT}" | base64 -w0)

curl_json() {
  local url="$1"
  curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" "$url"
}
