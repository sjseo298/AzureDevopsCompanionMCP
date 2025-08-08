#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/list_queries_root_folders.md
# Operación: List (GET)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: list_queries_root_folders.sh --project <proj> [--expand <none|clauses|all|wiql>] [--depth <int>] [--include-deleted <true|false>] [--query-type <flat|tree|oneHop>] [--help]

Fuente: api_doc/wit_sections/list_queries_root_folders.md

Descripción:
  Lista las carpetas raíz de consultas (queries) en un proyecto y, opcionalmente, sus subcarpetas.

Parámetros:
  --project          Nombre/ID del proyecto (obligatorio)
  --expand           none|clauses|all|wiql (opcional)
  --depth            Entero (opcional)
  --include-deleted  true|false (opcional)
  --query-type       flat|tree|oneHop (opcional)

Ejemplos:
  scripts/curl/wit/list_queries_root_folders.sh --project "Mi Proyecto"
  scripts/curl/wit/list_queries_root_folders.sh --project "Mi Proyecto" --expand all --depth 2
  scripts/curl/wit/list_queries_root_folders.sh --project "Mi Proyecto" --query-type tree --include-deleted false
USAGE
}

PROJECT=""
EXPAND=""
DEPTH=""
INCLUDE_DELETED=""
QUERY_TYPE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    --depth) DEPTH="$2"; shift 2;;
    --include-deleted) INCLUDE_DELETED="$2"; shift 2;;
    --query-type) QUERY_TYPE="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then
  echo "Falta --project (obligatorio)" >&2; usage; exit 2
fi

# Validaciones simples
if [[ -n "$EXPAND" ]]; then
  case "$EXPAND" in none|clauses|all|wiql) ;; *) echo "--expand inválido" >&2; exit 3;; esac
fi
if [[ -n "$INCLUDE_DELETED" ]]; then
  case "$INCLUDE_DELETED" in true|false) ;; *) echo "--include-deleted debe ser true|false" >&2; exit 4;; esac
fi
if [[ -n "$QUERY_TYPE" ]]; then
  case "$QUERY_TYPE" in flat|tree|oneHop) ;; *) echo "--query-type inválido" >&2; exit 5;; esac
fi
if [[ -n "$DEPTH" && ! "$DEPTH" =~ ^[0-9]+$ ]]; then
  echo "--depth debe ser entero" >&2; exit 6
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
API_VER_OVERRIDE="7.2-preview"
BASE_URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/queries?api-version=${API_VER_OVERRIDE}"

QS=""
if [[ -n "${EXPAND}" ]]; then
  ENC=$(jq -rn --arg s "$EXPAND" '$s|@uri'); QS+="&\$expand=${ENC}"
fi
if [[ -n "${DEPTH}" ]]; then
  ENC=$(jq -rn --arg s "$DEPTH" '$s|@uri'); QS+="&depth=${ENC}"
fi
if [[ -n "${INCLUDE_DELETED}" ]]; then
  ENC=$(jq -rn --arg s "$INCLUDE_DELETED" '$s|@uri'); QS+="&includeDeleted=${ENC}"
fi
if [[ -n "${QUERY_TYPE}" ]]; then
  ENC=$(jq -rn --arg s "$QUERY_TYPE" '$s|@uri'); QS+="&queryType=${ENC}"
fi

URL="${BASE_URL}${QS}"

curl_json "${URL}" | jq .
