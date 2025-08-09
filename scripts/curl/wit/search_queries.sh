#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/search_queries.md
# y fallback según api_doc/wit_sections/list_queries_root_folders.md (searchText/top por querystring)
# Operación: Search (POST preferente, GET fallback)
# Nivel: Proyecto

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: search_queries.sh --project <proj> --search-text <texto> [--team <team>] [--expand <none|clauses|all|wiql>] [--top <n>] [--api-version <ver>] [--help]

Fuentes:
  - api_doc/wit_sections/search_queries.md (POST /queries/$search)
  - api_doc/wit_sections/list_queries_root_folders.md (GET /queries?searchText=...&top=...)

Descripción:
  Busca queries por texto en un proyecto. Intenta POST $search; si no está disponible,
  realiza búsqueda vía GET con parámetros searchText/top.

Parámetros:
  --project      Nombre/ID del proyecto (obligatorio)
  --team         Nombre/ID del equipo (opcional; ignorado en fallback GET)
  --search-text  Texto a buscar (obligatorio)
  --expand       none|clauses|all|wiql (opcional)
  --top          Límite de resultados (opcional)
  --api-version  Override de api-version (opcional, por defecto 7.2-preview)

Ejemplos:
  scripts/curl/wit/search_queries.sh --project "Mi Proyecto" --search-text "Bugs" --expand all --top 5
USAGE
}

PROJECT=""
TEAM=""
SEARCH_TEXT=""
EXPAND=""
TOP=""
API_VERSION_OVERRIDE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --team) TEAM="$2"; shift 2;;
    --search-text) SEARCH_TEXT="$2"; shift 2;;
    --expand) EXPAND="$2"; shift 2;;
    --top) TOP="$2"; shift 2;;
    --api-version) API_VERSION_OVERRIDE="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" || -z "$SEARCH_TEXT" ]]; then
  echo "Faltan --project y/o --search-text (obligatorios)" >&2; usage; exit 2
fi

if [[ -n "$EXPAND" ]]; then
  case "$EXPAND" in none|clauses|all|wiql) ;; *) echo "--expand inválido" >&2; exit 3;; esac
fi
if [[ -n "$TOP" && ! "$TOP" =~ ^[0-9]+$ ]]; then
  echo "--top debe ser entero" >&2; exit 4
fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TEAM=""
if [[ -n "$TEAM" ]]; then
  ENC_TEAM=$(jq -rn --arg s "$TEAM" '$s|@uri')
fi

VERSIONS_TO_TRY=()
if [[ -n "$API_VERSION_OVERRIDE" ]]; then
  VERSIONS_TO_TRY+=("$API_VERSION_OVERRIDE")
else
  VERSIONS_TO_TRY+=("7.2-preview" "7.2-preview.1" "7.1-preview.1")
fi

# Construir body JSON incremental
BODY=$(jq -n --arg searchText "$SEARCH_TEXT" '{searchText: $searchText}')
if [[ -n "$EXPAND" ]]; then
  BODY=$(echo "$BODY" | jq --arg expand "$EXPAND" '. + {"$expand": $expand}')
fi
if [[ -n "$TOP" ]]; then
  BODY=$(echo "$BODY" | jq --argjson top "$TOP" '. + {top: $top}')
fi

build_post_url() {
  local ver="$1"
  if [[ -n "$ENC_TEAM" ]]; then
    printf '%s' "${DEVOPS_BASE}/${ENC_PROJECT}/${ENC_TEAM}/_apis/wit/queries/\$search?api-version=${ver}"
  else
    printf '%s' "${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/queries/\$search?api-version=${ver}"
  fi
}

# 1) Intento POST $search (según doc search_queries.md)
for VER in "${VERSIONS_TO_TRY[@]}"; do
  URL=$(build_post_url "$VER")
  HTTP_RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" -H "Content-Type: application/json" -X POST -d "$BODY" -w "\n%{http_code}" "$URL")
  HTTP_CODE=$(printf '%s\n' "$HTTP_RESP" | tail -n1)
  RESP_BODY=$(printf '%s\n' "$HTTP_RESP" | sed '$d')

  if [[ "$HTTP_CODE" =~ ^2 ]]; then
    if jq -e . >/dev/null 2>&1 <<<"$RESP_BODY"; then jq . <<<"$RESP_BODY"; else printf '%s\n' "$RESP_BODY"; fi
    exit 0
  fi

  MSG=$(jq -r '.message? // empty' <<<"$RESP_BODY" 2>/dev/null || true)
  TYPE=$(jq -r '.typeKey? // empty' <<<"$RESP_BODY" 2>/dev/null || true)
  if [[ "$TYPE" == "VssPropertyValidationException" ]] && \
     printf '%s' "$MSG" | grep -qiE 'Parameter name: (Name|Wiql|isFolder)'; then
    # El backend está interpretando como creación de query -> pasar a fallback GET
    break
  fi

  # Otros errores: continuar probando versión siguiente
  continue
done

# 2) Fallback GET /queries?searchText=... (según list_queries_root_folders.md)
ENC_SEARCH=$(jq -rn --arg s "$SEARCH_TEXT" '$s|@uri')
QS="searchText=${ENC_SEARCH}"
if [[ -n "$EXPAND" ]]; then
  ENC_EXPAND=$(jq -rn --arg s "$EXPAND" '$s|@uri')
  QS="%24expand=${ENC_EXPAND}&${QS}"
fi
if [[ -n "$TOP" ]]; then
  QS="${QS}&top=${TOP}"
fi
VER_FOR_GET="${API_VERSION_OVERRIDE:-7.2-preview}"
GET_URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/queries?${QS}&api-version=${VER_FOR_GET}"

HTTP_RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json" "$GET_URL" -w "\n%{http_code}")
HTTP_CODE=$(printf '%s\n' "$HTTP_RESP" | tail -n1)
RESP_BODY=$(printf '%s\n' "$HTTP_RESP" | sed '$d')

if [[ "$HTTP_CODE" =~ ^2 ]]; then
  if jq -e . >/dev/null 2>&1 <<<"$RESP_BODY"; then jq . <<<"$RESP_BODY"; else printf '%s\n' "$RESP_BODY"; fi
  exit 0
fi

# Fallback: imprimir cuerpo de error
if jq -e . >/dev/null 2>&1 <<<"$RESP_BODY"; then jq . <<<"$RESP_BODY" >&2; else printf '%s\n' "$RESP_BODY" >&2; fi
exit 1
