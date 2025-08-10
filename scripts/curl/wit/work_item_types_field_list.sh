#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_types_field.md
# Operación: List (GET)
# Nivel: Proyecto + tipo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_types_field_list.sh --project <project> --type <tipo> [--api-version <ver>] [--required-only] [--filter-ref <substring>] [--filter-name <substring>] [--summary] [--show-picklist-items] [--raw] [--help]

Fuente: api_doc/wit_sections/work_item_types_field.md

Descripción:
  Lista los campos de un tipo de work item en un proyecto.
  Por defecto entrega JSON ENRIQUECIDO COMPLETO (máximo detalle: metadata global + picklists con items).
  Usar --summary para vista condensada (ref, nombre, required, tipos, conteo picklist). Añadir --show-picklist-items para incluir los valores en modo resumen.

Parámetros:
  --project       Nombre o ID del proyecto (obligatorio)
  --type          Nombre del tipo de work item (obligatorio)
  --api-version   Override de versión API (opcional, default 7.2-preview)
  --required-only Solo campos marcados como alwaysRequired=true
  --filter-ref    Substring (case-insensitive) a buscar en referenceName
  --filter-name   Substring (case-insensitive) a buscar en name
  --summary       Activa modo resumen condensado
  --show-picklist-items  Con --summary incluye arreglo completo de valores de picklist (puede ser largo)
  --raw           Mostrar JSON crudo sin jq
  --help          Muestra esta ayuda.

Ejemplos:
  scripts/curl/wit/work_item_types_field_list.sh --project MiProyecto --type Bug
  scripts/curl/wit/work_item_types_field_list.sh --project Gerencia_Tecnologia --type "User Story" --required-only --summary
  scripts/curl/wit/work_item_types_field_list.sh --project Gerencia_Tecnologia --type Proyecto --filter-ref Custom. --summary
USAGE
}

PROJECT=""
TYPE=""
API_VER="7.2-preview"
RAW=0
REQUIRED_ONLY=0
FILTER_REF=""
FILTER_NAME=""
SUMMARY=0
ENRICH=1 # Enriquecido por defecto (bandera --enrich mantenida por retrocompatibilidad, ya no necesaria)
SHOW_PICK_ITEMS=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    --api-version) API_VER="$2"; shift 2;;
    --required-only) REQUIRED_ONLY=1; shift;;
    --filter-ref) FILTER_REF="$2"; shift 2;;
    --filter-name) FILTER_NAME="$2"; shift 2;;
  --summary) SUMMARY=1; shift;;
  --enrich) ENRICH=1; shift;; # no-op (default ya es enriquecido)
  --show-picklist-items) SHOW_PICK_ITEMS=1; shift;;
    --raw) RAW=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$PROJECT" ]]; then echo "Falta --project" >&2; usage; exit 2; fi
if [[ -z "$TYPE" ]]; then echo "Falta --type" >&2; usage; exit 2; fi

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
ENC_TYPE=$(jq -rn --arg s "$TYPE" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitemtypes/${ENC_TYPE}/fields?api-version=${API_VER}"

RESP=$(curl_json "$URL") || { echo "Error al invocar endpoint" >&2; exit 3; }
if ! echo "$RESP" | jq empty >/dev/null 2>&1; then
  echo "Respuesta no es JSON válido" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi

if [[ $RAW -eq 1 && $REQUIRED_ONLY -eq 0 && -z "$FILTER_REF" && -z "$FILTER_NAME" && $SUMMARY -eq 0 && $ENRICH -eq 0 ]]; then
  printf '%s\n' "$RESP"
  exit 0
fi

JQ_FILTER='.value[]'
if [[ $REQUIRED_ONLY -eq 1 ]]; then
  JQ_FILTER+=" | select(.alwaysRequired==true)"
fi
if [[ -n "$FILTER_REF" ]]; then
  escRef=$(printf '%s' "$FILTER_REF" | sed 's/\//\\\//g')
  JQ_FILTER+=" | select(.referenceName|test(\"$escRef\"; "
  JQ_FILTER+='"i"))'
fi
if [[ -n "$FILTER_NAME" ]]; then
  escName=$(printf '%s' "$FILTER_NAME" | sed 's/\//\\\//g')
  JQ_FILTER+=" | select(.name|test(\"$escName\"; "
  JQ_FILTER+='"i"))'
fi

BASE_LIST=$(echo "$RESP" | jq -r "$JQ_FILTER | {referenceName:.referenceName,name:.name,alwaysRequired:.alwaysRequired,helpText:(.helpText//\"\"),projectType:.type}" | jq -s '.')

ENRICHED='[]'
COUNT=$(echo "$BASE_LIST" | jq 'length')
for ((i=0; i<COUNT; i++)); do
  fJson=$(echo "$BASE_LIST" | jq ".[$i]")
  ref=$(echo "$fJson" | jq -r '.referenceName')
  # Llamar script de campo global
  G_RESP=$(bash "${SCRIPT_DIR}/fields_global_get.sh" --field "$ref" --raw 2>/dev/null || echo '{}')
  pickId=$(echo "$G_RESP" | jq -r '.picklistId // ""' 2>/dev/null || echo '')
  PICK_JSON='null'
  if [[ -n "$pickId" ]]; then
    PICK_JSON=$(bash "${SCRIPT_DIR}/../workitemtrackingprocess/picklists_get.sh" --id "$pickId" --raw 2>/dev/null || echo '{}')
  fi
  merged=$(jq -n --argjson base "$fJson" --argjson g "$G_RESP" --argjson p "$PICK_JSON" '($base + {globalType: ($g.type//null), usage: ($g.usage//null), readOnly: ($g.readOnly//null), picklistId: ($g.picklistId//null)}) as $m | if ($p!=null and ($p|type=="object") and ($p|has("items"))) then $m + {picklistName: ($p.name//null), picklistType: ($p.type//null), picklistItems: ($p.items // [])} else $m end')
  ENRICHED=$(jq --argjson item "$merged" '. + [$item]' <<< "$ENRICHED")
done
RESULT="$ENRICHED"

if [[ $SUMMARY -eq 1 ]]; then
  if [[ $SHOW_PICK_ITEMS -eq 1 ]]; then
    echo "$RESULT" | jq '[ .[] | {ref:.referenceName,name:.name,required:.alwaysRequired,projectType,globalType,readOnly,hasPicklist:(.picklistItems|type=="array"),picklistCount:(.picklistItems| length? // 0),picklistItems:(.picklistItems // empty)} ]'
  else
    echo "$RESULT" | jq '[ .[] | {ref:.referenceName,name:.name,required:.alwaysRequired,projectType,globalType,readOnly,hasPicklist:(.picklistItems|type=="array"),picklistCount:(.picklistItems| length? // 0)} ]'
  fi
else
  echo "$RESULT" | jq '.'
fi
