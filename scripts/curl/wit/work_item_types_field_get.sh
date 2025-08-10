#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_types_field.md
# Operación: Get (GET)
# Nivel: Proyecto + tipo + campo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_types_field_get.sh --project <project> --type <tipo> --field <referenceName> \
  [--api-version <ver>] [--summary] [--show-picklist-items] [--no-enrich] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_types_field.md

Descripción:
  Obtiene el detalle de un campo específico de un tipo de work item (scoped field).
  Por defecto ENRIQUECE con metadata global (tipo global, usage, readOnly, picklistId) y si aplica los valores del picklist.
  Con --summary se muestra una vista condensada; --show-picklist-items agrega los valores (en modo resumen) para picklists.
  Con --no-enrich se devuelve solo el JSON del endpoint scoped (a menos que se combine con --raw que devuelve sin procesar).

Parámetros:
  --project              Nombre o ID del proyecto (obligatorio)
  --type                 Nombre del tipo de work item (obligatorio)
  --field                referenceName del campo (obligatorio)
  --api-version          Override de versión API (default 7.2-preview)
  --summary              Vista resumida (ref, name, required, projectType, globalType, picklistCount)
  --show-picklist-items  En modo --summary incluye arreglo de valores de picklist
  --no-enrich            Desactiva enriquecimiento (omite llamadas global/picklist)
  --raw                  Mostrar JSON crudo del endpoint scoped sin modificaciones
  --help                 Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_types_field_get.sh --project Gerencia_Tecnologia --type Historia --field System.Title
  scripts/curl/wit/work_item_types_field_get.sh --project Gerencia_Tecnologia --type Historia --field Custom.TipodeHistoria --summary --show-picklist-items
  scripts/curl/wit/work_item_types_field_get.sh --project Gerencia_Tecnologia --type Historia --field System.State --no-enrich
USAGE
}

PROJECT=""
TYPE=""
FIELD=""
API_VER="7.2-preview"
RAW=0
SUMMARY=0
SHOW_PICK_ITEMS=0
ENRICH=1
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    --field) FIELD="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --summary) SUMMARY=1; shift;;
    --show-picklist-items) SHOW_PICK_ITEMS=1; shift;;
    --no-enrich) ENRICH=0; shift;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "$TYPE" ]]; then echo "Falta --type" >&2; usage; exit 2; fi
if [[ -z "$FIELD" ]]; then echo "Falta --field" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TYPE=$(jq -rn --arg s "$TYPE" '$s|@uri')
ENC_FIELD=$(jq -rn --arg s "$FIELD" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypes/${ENC_TYPE}/fields/${ENC_FIELD}?api-version=${API_VER}"

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if ! echo "$RESP" | jq empty >/dev/null 2>&1; then
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi

if [[ $RAW -eq 1 ]]; then
  printf '%s\n' "$RESP"
  exit 0
fi

if [[ $ENRICH -eq 0 ]]; then
  # Mostrar directamente (posible summary ligero)
  if [[ $SUMMARY -eq 1 ]]; then
    echo "$RESP" | jq '{ref:.referenceName,name:.name,required:.alwaysRequired,projectType:.type}'
  else
    echo "$RESP" | jq '.'
  fi
  exit 0
fi

# Enriquecimiento global
GLOBAL=$(bash "${SCRIPT_DIR}/fields_global_get.sh" --field "$FIELD" --raw 2>/dev/null || echo '{}')
pickId=$(echo "$GLOBAL" | jq -r '.picklistId // ""')
PICK_JSON='null'
if [[ -n "$pickId" ]]; then
  PICK_JSON=$(bash "${SCRIPT_DIR}/../workitemtrackingprocess/picklists_get.sh" --id "$pickId" --raw 2>/dev/null || echo '{}')
fi

MERGED=$(jq -n --argjson scoped "$RESP" --argjson g "$GLOBAL" --argjson p "$PICK_JSON" '
  ($scoped + {globalType:($g.type//null), globalUsage:($g.usage//null), globalReadOnly:($g.readOnly//null), picklistId:($g.picklistId//null)}) as $m
  | if ($p!=null and ($p|type=="object") and ($p|has("items"))) then $m + {picklistName:($p.name//null), picklistType:($p.type//null), picklistItems:($p.items//[])} else $m end
')

if [[ $SUMMARY -eq 1 ]]; then
  if [[ $SHOW_PICK_ITEMS -eq 1 ]]; then
    echo "$MERGED" | jq '{ref:.referenceName,name:.name,required:.alwaysRequired,projectType:.type,globalType,globalUsage,globalReadOnly,hasPicklist:(.picklistItems|type=="array"),picklistCount:(.picklistItems|length//0),picklistItems:(.picklistItems//[])}'
  else
    echo "$MERGED" | jq '{ref:.referenceName,name:.name,required:.alwaysRequired,projectType:.type,globalType,globalUsage,globalReadOnly,hasPicklist:(.picklistItems|type=="array"),picklistCount:(.picklistItems|length//0)}'
  fi
else
  echo "$MERGED" | jq '.'
fi
