#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_items.md
# Operación: Update (PATCH)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_update.sh --project <project> --id <id> \
  [--add k=v,...] [--replace k=v,...] [--remove field1,field2] \
  [--state <estado>] [--title <titulo>] [--description <texto>] \
  [--area <AreaPath>] [--iteration <IterationPath>] \
  [--parent <idPadre>] [--relations rel1,rel2] \
  [--validate-only] [--bypass-rules] [--suppress-notifications] \
  [--api-version <ver>] [--raw] [--debug] [--no-diagnostic]

Fuente: api_doc/wit_sections/work_items.md (Update PATCH)

Parámetros clave:
  --project                Proyecto (obligatorio)
  --id                     ID del work item (obligatorio)
  --add k=v,...            Operaciones JSON Patch op=add (fields)
  --replace k=v,...        Operaciones JSON Patch op=replace
  --remove f1,f2           Operaciones op=remove (solo nombre de campo)
  --state / --title / --description  Atajos para System.State / System.Title / System.Description
  --area / --iteration     Atajos para System.AreaPath / System.IterationPath
  --parent <id>            Agrega relación Hierarchy-Reverse (re-parenting)
  --relations              Lista tipo:id[:comentario] (se agregan relaciones adicionales)
  --validate-only          validateOnly=true (no persiste)
  --bypass-rules           bypassRules=true
  --suppress-notifications suppressNotifications=true
  --debug                  Imprime JSON Patch generado
  --no-diagnostic          No ejecutar diagnóstico enriquecido si hay error
  --raw                    Muestra JSON crudo de respuesta
  --api-version            Override (default 7.2-preview)

Relaciones ejemplo:
  System.LinkTypes.Related:870057,ArtifactLink:1234:Referencia
  System.LinkTypes.Hierarchy-Forward:12345  (añade hijo existente)

Diagnóstico de errores:
  Si la respuesta incluye RuleValidationErrors, se enriquece mostrando tipo global, picklists y sugerencias de --add/--replace.
USAGE
}

PROJECT=""; ID=""; ADD=""; REPLACE=""; REMOVE=""; STATE=""; TITLE=""; DESC=""; AREA_PATH=""; ITER_PATH=""; PARENT=""; RELATIONS_SPEC=""; API_VER="7.2-preview"; RAW=0; VALIDATE=0; BYPASS=0; SUPPRESS=0; DEBUG=0; NO_DIAG=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --id) ID="$2"; shift 2;;
    --add) ADD="$2"; shift 2;;
    --replace) REPLACE="$2"; shift 2;;
    --remove) REMOVE="$2"; shift 2;;
  --state) STATE="$2"; shift 2;;
  --title) TITLE="$2"; shift 2;;
  --description) DESC="$2"; shift 2;;
  --area) AREA_PATH="$2"; shift 2;;
  --iteration) ITER_PATH="$2"; shift 2;;
  --parent) PARENT="$2"; shift 2;;
  --relations) RELATIONS_SPEC="$2"; shift 2;;
    --validate-only) VALIDATE=1; shift;;
    --bypass-rules) BYPASS=1; shift;;
    --suppress-notifications) SUPPRESS=1; shift;;
    --api-version) API_VER="$2"; shift 2;;
    --raw) RAW=1; shift;;
  --debug) DEBUG=1; shift;;
  --no-diagnostic) NO_DIAG=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Arg desconocido: $1" >&2; usage; exit 1;;
  esac
done

PATCH=()

add_field_patch() {
  local mode="$1"; local ref="$2"; local val="$3"; local jsonVal
  if [[ "$val" =~ ^-?[0-9]+([.][0-9]+)?$ ]]; then
    jsonVal="$val"
  else
    jsonVal=$(jq -Rn --arg v "$val" '$v')
  fi
  PATCH+=("{\"op\":\"$mode\",\"path\":\"/fields/${ref}\",\"value\":${jsonVal}}")
}

# Atajos
[[ -n "$STATE" ]] && add_field_patch add "System.State" "$STATE"
[[ -n "$TITLE" ]] && add_field_patch add "System.Title" "$TITLE"
[[ -n "$DESC" ]] && add_field_patch add "System.Description" "$DESC"
[[ -n "$AREA_PATH" ]] && add_field_patch add "System.AreaPath" "$AREA_PATH"
[[ -n "$ITER_PATH" ]] && add_field_patch add "System.IterationPath" "$ITER_PATH"

parse_kv_list() {
  local mode="$1"; local list="$2"; IFS=',' read -r -a arr <<<"$list"
  for p in "${arr[@]}"; do
    [[ -z "$p" ]] && continue
    local k="${p%%=*}"; local v="${p#*=}"; [[ -z "$k" ]] && continue
    add_field_patch "$mode" "$k" "$v"
  done
}
[[ -n "$ADD" ]] && parse_kv_list add "$ADD"
[[ -n "$REPLACE" ]] && parse_kv_list replace "$REPLACE"
if [[ -n "$REMOVE" ]]; then
  IFS=',' read -r -a rm <<<"$REMOVE"
  for f in "${rm[@]}"; do
    [[ -z "$f" ]] && continue
    PATCH+=("{\"op\":\"remove\",\"path\":\"/fields/${f}\"}")
  done
fi

escape_json() { echo -n "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

# Re-parenting (Hierarchy-Reverse) y herencia opcional de Area/Iteration si no se pasaron
if [[ -n "$PARENT" ]]; then
  if [[ ! "$PARENT" =~ ^[0-9]+$ ]]; then echo "--parent debe ser numérico" >&2; exit 2; fi
  if PARENT_JSON=$(bash "${SCRIPT_DIR}/work_items_get.sh" --project "$PROJECT" --id "$PARENT" --fields "System.AreaPath,System.IterationPath" --raw --api-version "$API_VER" 2>/dev/null); then
    if [[ -z "$AREA_PATH" ]]; then
      parentArea=$(echo "$PARENT_JSON" | jq -r '.fields[\"System.AreaPath\"] // empty')
      [[ -n "$parentArea" ]] && add_field_patch add "System.AreaPath" "$parentArea"
    fi
    if [[ -z "$ITER_PATH" ]]; then
      parentIter=$(echo "$PARENT_JSON" | jq -r '.fields[\"System.IterationPath\"] // empty')
      [[ -n "$parentIter" ]] && add_field_patch add "System.IterationPath" "$parentIter"
    fi
    PARENT_URL=$(echo "$PARENT_JSON" | jq -r '.url')
    if [[ -n "$PARENT_URL" ]]; then
      PATCH+=("{\"op\":\"add\",\"path\":\"/relations/-\",\"value\":{\"rel\":\"System.LinkTypes.Hierarchy-Reverse\",\"url\":\"$PARENT_URL\"}}")
    fi
  else
    echo "No se pudo obtener datos del padre $PARENT" >&2
  fi
fi

# Helper fetch_wi_url (usa GET directo minimal)
fetch_wi_url() {
  local id="$1"; [[ -z "$id" ]] && return 1
  local pjEnc=$(jq -rn --arg s "$PROJECT" '$s|@uri')
  local url="${DEVOPS_BASE}/${pjEnc}/_apis/wit/workitems/${id}?api-version=${API_VER}&fields=System.Id"
  local resp
  resp=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H 'Accept: application/json' "$url" 2>/dev/null || true)
  echo "$resp" | jq -r '.url // empty'
}

# Relaciones adicionales (fuera del bloque de padre)
if [[ -n "$RELATIONS_SPEC" ]]; then
  IFS=',' read -r -a RELARR <<<"$RELATIONS_SPEC"
  declare -A REL_UNIQ=()
  for spec in "${RELARR[@]}"; do
    [[ -z "$spec" ]] && continue
    relType="${spec%%:*}"; rest="${spec#*:}"; [[ "$relType" == "$rest" ]] && { echo "Relación inválida: $spec" >&2; continue; }
    relId="${rest%%:*}"; comment=""
    [[ "$rest" == *:* ]] && comment="${rest#*:}"
    [[ ! "$relId" =~ ^[0-9]+$ ]] && { echo "ID relación no numérico: $spec" >&2; continue; }
    key="$relType|$relId|$comment"; [[ -n "${REL_UNIQ[$key]:-}" ]] && continue; REL_UNIQ[$key]=1
    if TARGET_URL=$(fetch_wi_url "$relId"); then
      if [[ -n "$comment" ]]; then
        esc=$(escape_json "$comment")
        PATCH+=("{\"op\":\"add\",\"path\":\"/relations/-\",\"value\":{\"rel\":\"$relType\",\"url\":\"$TARGET_URL\",\"attributes\":{\"comment\":\"$esc\"}}}")
      else
        PATCH+=("{\"op\":\"add\",\"path\":\"/relations/-\",\"value\":{\"rel\":\"$relType\",\"url\":\"$TARGET_URL\"}}")
      fi
    else
      echo "No se agregó relación (no URL) spec=$spec" >&2
    fi
  done
fi

BODY="[$(IFS=,; echo "${PATCH[*]}")]"
if [[ $DEBUG -eq 1 ]]; then
  >&2 echo "--- JSON PATCH (debug) ---"
  if command -v jq >/dev/null 2>&1; then >&2 echo "$BODY" | jq . || >&2 echo "$BODY"; else >&2 echo "$BODY"; fi
  >&2 echo "--------------------------"
fi
ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitems/${ID}?api-version=${API_VER}"
[[ $VALIDATE -eq 1 ]] && URL+="&validateOnly=true"
[[ $BYPASS -eq 1 ]] && URL+="&bypassRules=true"
[[ $SUPPRESS -eq 1 ]] && URL+="&suppressNotifications=true"

RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Content-Type: application/json-patch+json" -H "Accept: application/json" -X PATCH "$URL" -d "$BODY") || { echo "Error endpoint" >&2; exit 3; }
if ! echo "$RESP" | jq empty >/dev/null 2>&1; then
  echo "Respuesta no JSON" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi

# Éxito directo
if jq -e '.id and .fields' >/dev/null 2>&1 <<<"$RESP"; then
  if [[ $RAW -eq 1 ]]; then
    printf '%s\n' "$RESP";
  else
    echo "$RESP" | jq '{id:.id, rev:.rev, title:.fields["System.Title"], state:.fields["System.State"], url:.url}'
  fi
  exit 0
fi

# Error enriquecido
ERR_MSG=$(jq -r '.message? // empty' <<<"$RESP")
ERR_TYPE=$(jq -r '.typeKey? // .typeName? // empty' <<<"$RESP")
if [[ -n "$ERR_MSG" ]]; then
  if [[ -n "$ERR_TYPE" ]]; then echo "ERROR: $ERR_MSG (type: $ERR_TYPE)" >&2; else echo "ERROR: $ERR_MSG" >&2; fi
fi

if jq -e '.customProperties.RuleValidationErrors | length > 0' >/dev/null 2>&1 <<<"$RESP"; then
  echo "--- Validación de campos con error ---" >&2
  wType=$(jq -r '.fields["System.WorkItemType"] // empty' <<<"$RESP")
  # Fallback: si no se obtuvo el tipo (respuesta de error sin .fields) consultamos el work item existente
  if [[ -z "$wType" && "$ID" =~ ^[0-9]+$ ]]; then
    EXISTING_WI_JSON=$(bash "${SCRIPT_DIR}/work_items_get.sh" --project "$PROJECT" --id "$ID" --fields "System.WorkItemType" --raw --api-version "$API_VER" 2>/dev/null || echo '{}')
    wType=$(echo "$EXISTING_WI_JSON" | jq -r '.fields["System.WorkItemType"] // empty')
  fi
  FIELDS_SUMMARY=$(bash "${SCRIPT_DIR}/work_item_types_field_list.sh" --project "$PROJECT" --type "$wType" --raw 2>/dev/null || echo '[]')
  FIELDS_SUMMARY_IS_JSON=1
  if ! echo "$FIELDS_SUMMARY" | jq -e 'type=="array"' >/dev/null 2>&1; then
    FIELDS_SUMMARY_IS_JSON=0
  fi
  # Cache de estados válidos (solo se obtiene si se requiere para System.State)
  STATE_VALUES=""
  declare -A CACHE_GLOBAL_FIELD_JSON=()
  declare -A CACHE_PICKLIST_ITEMS=()
  while IFS= read -r line; do
    ref=$(jq -r '.fieldReferenceName // empty' <<<"$line")
    msg=$(jq -r '.errorMessage // "(sin mensaje)"' <<<"$line")
    flags=$(jq -r '.fieldStatusFlags // "-"' <<<"$line")
    typeInfo=""; pickInfo=""
    if [[ -n "$ref" ]]; then
      if [[ $FIELDS_SUMMARY_IS_JSON -eq 1 ]]; then
        entry=$(echo "$FIELDS_SUMMARY" | jq -r --arg r "$ref" 'map(select(.ref==$r)) | .[0] | select(.!=null) | @base64' 2>/dev/null || true)
        if [[ -n "$entry" ]]; then
          decoded=$(echo "$entry" | base64 -d 2>/dev/null || true)
          gType=$(echo "$decoded" | jq -r '.globalType // .projectType // empty')
          pItems=$(echo "$decoded" | jq -r 'if (has("picklistItems") and (.picklistItems|type=="array") and (.picklistItems|length)>0) then (.picklistItems|join(" | ")) else empty end')
          [[ -n "$gType" && "$gType" != "null" ]] && typeInfo=" | type: $gType"
          [[ -n "$pItems" ]] && pickInfo=" | vals: $pItems"
        fi
      fi
      if [[ -z "$pickInfo" || -z "$typeInfo" ]]; then
        IND_JSON=$(bash "${SCRIPT_DIR}/work_item_types_field_get.sh" --project "$PROJECT" --type "$wType" --field "$ref" --raw 2>/dev/null || echo '{}')
        if echo "$IND_JSON" | jq empty >/dev/null 2>&1; then
          indType=$(echo "$IND_JSON" | jq -r '.globalType // .projectType // empty')
          indPick=$(echo "$IND_JSON" | jq -r 'if (.picklistItems|type=="array" and (.picklistItems|length)>0) then (.picklistItems|join(" | ")) else empty end')
          [[ -n "$indType" && -z "$typeInfo" ]] && typeInfo=" | type: $indType"
          [[ -n "$indPick" && -z "$pickInfo" ]] && pickInfo=" | vals: $indPick"
        fi
      fi
      if [[ -z "$pickInfo" ]]; then
        if [[ -z "${CACHE_GLOBAL_FIELD_JSON[$ref]:-}" ]]; then
          CACHE_GLOBAL_FIELD_JSON[$ref]=$(bash "${SCRIPT_DIR}/fields_global_get.sh" --field "$ref" --raw 2>/dev/null || echo '{}')
        fi
        gjson="${CACHE_GLOBAL_FIELD_JSON[$ref]}"
        gPicklistId=$(echo "$gjson" | jq -r '.picklistId? // empty')
        gIsPick=$(echo "$gjson" | jq -r '.isPicklist? // false')
        gType2=$(echo "$gjson" | jq -r '.type? // empty')
        if [[ -n "$gType2" && -z "$typeInfo" ]]; then typeInfo=" | type: $gType2"; fi
        if [[ "$gIsPick" == "true" && -n "$gPicklistId" ]]; then
          if [[ -z "${CACHE_PICKLIST_ITEMS[$gPicklistId]:-}" ]]; then
            rawPick=$(bash "${SCRIPT_DIR}/../workitemtrackingprocess/picklists_get.sh" --id "$gPicklistId" --raw 2>/dev/null || echo '{}')
            CACHE_PICKLIST_ITEMS[$gPicklistId]=$(echo "$rawPick" | jq -r 'if (.items|type=="array" and (.items|length)>0) then (.items|join(" | ")) else empty end')
          fi
          if [[ -n "${CACHE_PICKLIST_ITEMS[$gPicklistId]}" ]]; then pickInfo=" | vals: ${CACHE_PICKLIST_ITEMS[$gPicklistId]}"; fi
        fi
      fi
    fi
    # Si es System.State y los flags indican valores limitados o inválidos, agregar lista de válidos
    validVals=""
    if [[ "$ref" == "System.State" && "$flags" =~ (limitedToValues|invalidListValue|hasValues) ]]; then
      if [[ -z "$STATE_VALUES" ]]; then
        RAW_STATES=$(bash "${SCRIPT_DIR}/work_item_type_states_list.sh" --project "$PROJECT" --type "$wType" --raw 2>/dev/null || echo '{}')
        if echo "$RAW_STATES" | jq -e '.value' >/dev/null 2>&1; then
          STATE_VALUES=$(echo "$RAW_STATES" | jq -r '.value[]?.name' | paste -sd ' | ' - 2>/dev/null || true)
        fi
      fi
      if [[ -n "$STATE_VALUES" ]]; then
        validVals=" | valid: $STATE_VALUES"
      fi
    fi
    echo "* ${ref:-'(sin ref)'} -> $msg | flags: $flags$typeInfo$pickInfo$validVals" >&2
  done < <(jq -c '.customProperties.RuleValidationErrors[]' <<<"$RESP")
  echo >&2
  echo "Sugerencias (--add/--replace):" >&2
  jq -r '.customProperties.RuleValidationErrors[] | select((.fieldStatusFlags//"" )|test("required")) | "  --add " + (.fieldReferenceName//"REF") + "=<valor>"' <<<"$RESP" | sort -u >&2 || true
  echo "--- Fin validación detallada ---" >&2
fi

if [[ $RAW -eq 1 ]]; then
  printf '%s\n' "$RESP"
else
  echo "$RESP" | jq .
fi

if [[ $NO_DIAG -eq 0 && -n "$ERR_MSG" ]]; then
  {
    wType=$(jq -r '.fields["System.WorkItemType"] // empty' <<<"$RESP")
    if [[ -n "$wType" ]]; then
      echo "--- Diagnóstico de campos para el tipo '$wType' (proyecto '$PROJECT') ---"
      bash "${SCRIPT_DIR}/work_item_types_field_list.sh" --project "$PROJECT" --type "$wType" --required-only --summary --show-picklist-items 2>/dev/null || echo '(Fallo obteniendo listado de campos)'
      echo "--- Fin diagnóstico ---"
    fi
  } >&2
fi

exit 5
