#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_items.md
# Operación: Create (POST)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_items_create.sh --project <project> --type <tipo> --title <titulo> \
  [--state <estado>] [--description <texto>] [--fields k1=v1,k2=v2] \
  [--area <AreaPath>] [--iteration <IterationPath>] \
  [--parent <idPadre>] [--relations rel1,rel2] \
  [--bypass-rules] [--suppress-notifications] [--api-version <ver>] [--raw]
  [--no-diagnostic]

Crea un work item usando JSON Patch. Permite opcionalmente vincularlo a un padre existente mediante relación Hierarchy-Reverse.
    [--debug]               Modo debug para imprimir el JSON Patch generado

Parámetros obligatorios:
  --project                Proyecto
  --type                   Tipo (ej: Bug, Task, 'User Story', Épica, etc.)
  --title                  Título

Opcionales:
  --state                  Estado inicial
  --description            Descripción (Markdown) -> System.Description
  --fields                 Lista adicional k=v separada por comas (usa referenceNames)
  --area                   Establece System.AreaPath
  --iteration              Establece System.IterationPath
  --parent                 ID de work item padre (agrega relación Hierarchy-Reverse)
                           Si no se especifican --area / --iteration se intentan heredar del padre
  --relations              Lista separada por comas de relaciones adicionales. Formato por item:
                           tipo:id[:comentario]
                           Ej: System.LinkTypes.Related:870057,ArtifactLink:1234:Referencia
                           Para hijos existentes (nuevo ítem como padre): System.LinkTypes.Hierarchy-Forward:<id>
  --bypass-rules           bypassRules=true
  --suppress-notifications suppressNotifications=true
  --api-version            Override (default 7.2-preview)
    --no-diagnostic          No ejecutar diagnóstico de campos cuando hay error
    --raw                    JSON crudo completo
    --debug                  Imprime el JSON Patch generado (stderr)
  -h|--help                Ayuda

Ejemplos:
  scripts/curl/wit/work_items_create.sh --project Gerencia_Tecnologia --type Épica --title "Nueva épica" --parent 869848
  scripts/curl/wit/work_items_create.sh --project Gerencia_Tecnologia --type Proyecto --title "Nuevo Proyecto" --fields Custom.Tipodeproyecto=Gestión --validate-only
USAGE
}

# Inicialización única
PROJECT=""; TYPE=""; TITLE=""; STATE=""; DESC=""; EXTRA_FIELDS=""; AREA_PATH=""; ITER_PATH=""; PARENT=""; RELATIONS_SPEC=""; API_VER="7.2-preview"; RAW=0; BYPASS=0; SUPPRESS=0; DEBUG=0; NO_DIAG=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --type) TYPE="$2"; shift 2;;
    --title) TITLE="$2"; shift 2;;
    --state) STATE="$2"; shift 2;;
    --description) DESC="$2"; shift 2;;
    --fields) EXTRA_FIELDS="$2"; shift 2;;
    --bypass-rules) BYPASS=1; shift;;
    --suppress-notifications) SUPPRESS=1; shift;;
  --area) AREA_PATH="$2"; shift 2;;
  --iteration) ITER_PATH="$2"; shift 2;;
  --parent) PARENT="$2"; shift 2;;
  --relations) RELATIONS_SPEC="$2"; shift 2;;
  --api-version) API_VER="$2"; shift 2;;
  --raw) RAW=1; shift;;
  --debug) DEBUG=1; shift;;
    --no-diagnostic) NO_DIAG=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

[[ -z "$PROJECT" || -z "$TYPE" || -z "$TITLE" ]] && echo "Faltan obligatorios" >&2 && usage && exit 2
if [[ -n "$PARENT" && ! "$PARENT" =~ ^[0-9]+$ ]]; then echo "--parent debe ser numérico" >&2; exit 2; fi

escape_json() { sed -e 's/\\\\/\\\\\\\\/g' -e 's/"/\\\\"/g'; }
PATCH=()
add_field_patch() {
  local ref="$1"; local val="$2"; local jsonVal
  if [[ "$val" =~ ^-?[0-9]+([.][0-9]+)?$ ]]; then
    jsonVal="$val"
  else
    # Usar jq para serializar string de forma segura (maneja backslashes, comillas, UTF-8)
    jsonVal=$(jq -Rn --arg v "$val" '$v')
  fi
  PATCH+=("{\"op\":\"add\",\"path\":\"/fields/${ref}\",\"value\":${jsonVal}}")
}
add_field_patch "System.Title" "$TITLE"
[[ -n "$STATE" ]] && add_field_patch "System.State" "$STATE"
[[ -n "$DESC" ]] && add_field_patch "System.Description" "$DESC"
[[ -n "$AREA_PATH" ]] && add_field_patch "System.AreaPath" "$AREA_PATH"
[[ -n "$ITER_PATH" ]] && add_field_patch "System.IterationPath" "$ITER_PATH"
if [[ -n "$EXTRA_FIELDS" ]]; then
  IFS=',' read -r -a KV <<< "$EXTRA_FIELDS"
  for pair in "${KV[@]}"; do
    [[ -z "$pair" ]] && continue
    key="${pair%%=*}"; val="${pair#*=}"
    [[ -z "$key" ]] && continue
    add_field_patch "$key" "$val"
  done
fi
### Si se especifica padre, obtenemos su URL para usar formato oficial (con GUID de proyecto)
# Nota: Para obtener datos del padre reutilizamos el script work_items_get.sh (principio DRY)

if [[ -n "$PARENT" ]]; then
  # Invocar script existente solicitando solo campos necesarios para herencia
  if PARENT_WI_JSON=$(bash "${SCRIPT_DIR}/work_items_get.sh" --project "$PROJECT" --id "$PARENT" --fields "System.AreaPath,System.IterationPath" --api-version "$API_VER" --raw 2>/dev/null); then
    if ! echo "$PARENT_WI_JSON" | jq -e '.url' >/dev/null 2>&1; then
      echo "No se pudo obtener URL del padre (JSON sin .url)" >&2; exit 3; fi
    # Heredar primero (antes de relación) para mantener un orden lógico del patch
    if [[ -z "$AREA_PATH" || -z "$ITER_PATH" ]]; then
      parentArea=$(echo "$PARENT_WI_JSON" | jq -r '.fields["System.AreaPath"] // empty' 2>/dev/null || true)
      parentIter=$(echo "$PARENT_WI_JSON" | jq -r '.fields["System.IterationPath"] // empty' 2>/dev/null || true)
      if [[ -z "$AREA_PATH" && -n "$parentArea" ]]; then
        add_field_patch "System.AreaPath" "$parentArea"
        [[ $DEBUG -eq 1 ]] && >&2 echo "[debug] Heredado System.AreaPath='$parentArea' del padre $PARENT"
      fi
      if [[ -z "$ITER_PATH" && -n "$parentIter" ]]; then
        add_field_patch "System.IterationPath" "$parentIter"
        [[ $DEBUG -eq 1 ]] && >&2 echo "[debug] Heredado System.IterationPath='$parentIter' del padre $PARENT"
      fi
    fi
    PARENT_WI_URL=$(echo "$PARENT_WI_JSON" | jq -r '.url')
    # Fallback: si la URL no contiene GUID de proyecto, refetch completo para URL canonical
    if [[ "$PARENT_WI_URL" =~ dev.azure.com/[^/]+/_apis/wit/workItems/ ]]; then
      FULL_PARENT_JSON=$(bash "${SCRIPT_DIR}/work_items_get.sh" --project "$PROJECT" --id "$PARENT" --api-version "$API_VER" --raw 2>/dev/null || true)
      if echo "$FULL_PARENT_JSON" | jq -e '.url' >/dev/null 2>&1; then
        FULL_URL=$(echo "$FULL_PARENT_JSON" | jq -r '.url')
        if [[ "$FULL_URL" =~ dev.azure.com/.+/.+/_apis/wit/workItems/ ]]; then
          PARENT_WI_URL="$FULL_URL"
          [[ $DEBUG -eq 1 ]] && >&2 echo "[debug] Reemplazada URL de relación por canonical con GUID de proyecto"
        fi
      fi
    fi
    PATCH+=("{\"op\":\"add\",\"path\":\"/relations/-\",\"value\":{\"rel\":\"System.LinkTypes.Hierarchy-Reverse\",\"url\":\"${PARENT_WI_URL}\"}}")
  else
    echo "Fallo obteniendo datos del padre (ID=$PARENT)" >&2; exit 3
  fi
fi

if [[ -n "$RELATIONS_SPEC" ]]; then
  IFS=',' read -r -a REL_ARR <<< "$RELATIONS_SPEC"
  declare -A REL_UNIQ=()
  for spec in "${REL_ARR[@]}"; do
    [[ -z "$spec" ]] && continue
    # Formato tipo:id[:comentario]
    relType="${spec%%:*}"; rest="${spec#*:}"; [[ "$relType" == "$rest" ]] && { echo "Relación inválida (falta id): $spec" >&2; exit 4; }
    relId="${rest%%:*}"; comment=""
    if [[ "$rest" == *:* ]]; then comment="${rest#*:}"; fi
    if ! [[ "$relId" =~ ^[0-9]+$ ]]; then echo "ID de relación no numérico en: $spec" >&2; exit 4; fi
    key="$relType|$relId|$comment"; [[ -n "${REL_UNIQ[$key]:-}" ]] && continue; REL_UNIQ[$key]=1
    if TARGET_URL=$(fetch_wi_url "$relId"); then
      if [[ -n "$comment" ]]; then
        escComment=$(echo -n "$comment" | escape_json)
        PATCH+=("{\"op\":\"add\",\"path\":\"/relations/-\",\"value\":{\"rel\":\"${relType}\",\"url\":\"${TARGET_URL}\",\"attributes\":{\"comment\":\"${escComment}\"}}}")
      else
        PATCH+=("{\"op\":\"add\",\"path\":\"/relations/-\",\"value\":{\"rel\":\"${relType}\",\"url\":\"${TARGET_URL}\"}}")
      fi
    else
      echo "No se agregó relación (no se obtuvo URL) spec=$spec" >&2
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
ENC_TYPE=$(jq -rn --arg s "$TYPE" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/workitems/\$${ENC_TYPE}?api-version=${API_VER}"
[[ $BYPASS -eq 1 ]] && URL+="&bypassRules=true"
[[ $SUPPRESS -eq 1 ]] && URL+="&suppressNotifications=true"

RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Content-Type: application/json-patch+json" -H "Accept: application/json" -X POST "$URL" -d "$BODY") || { echo "Error endpoint" >&2; exit 3; }
if ! echo "$RESP" | jq empty >/dev/null 2>&1; then
  echo "Respuesta no JSON" >&2; printf '%s\n' "$RESP" >&2; exit 4
fi

# Detectar éxito
if jq -e '.id and .fields' >/dev/null 2>&1 <<<"$RESP"; then
  if [[ $RAW -eq 1 ]]; then
    printf '%s\n' "$RESP"
  else
    echo "$RESP" | jq '{id:.id, rev:.rev, title:.fields["System.Title"], state:.fields["System.State"], url:.url}'
  fi
  exit 0
fi

# Error: formatear primero mensaje principal breve
ERR_MSG=$(jq -r '.message? // empty' <<<"$RESP")
ERR_TYPE=$(jq -r '.typeKey? // .typeName? // empty' <<<"$RESP")
if [[ -n "$ERR_MSG" ]]; then
  if [[ -n "$ERR_TYPE" ]]; then
    echo "ERROR: $ERR_MSG (type: $ERR_TYPE)" >&2
  else
    echo "ERROR: $ERR_MSG" >&2
  fi
fi

# Detalle de validaciones específicas (RuleValidationErrors)
if jq -e '.customProperties.RuleValidationErrors | length > 0' >/dev/null 2>&1 <<<"$RESP"; then
  echo "--- Validación de campos con error ---" >&2
  # Obtener resumen completo una sola vez (incluye picklists si existen)
  FIELDS_SUMMARY=$(bash "${SCRIPT_DIR}/work_item_types_field_list.sh" --project "$PROJECT" --type "$TYPE" --summary --show-picklist-items 2>/dev/null || echo '[]')
  # Caches para no repetir llamadas costosas
  declare -A CACHE_GLOBAL_FIELD_JSON=()
  declare -A CACHE_PICKLIST_ITEMS=()
  # Recorremos cada error y enriquecemos
  while IFS= read -r line; do
    ref=$(jq -r '.fieldReferenceName // empty' <<<"$line")
    msg=$(jq -r '.errorMessage // "(sin mensaje)"' <<<"$line")
    flags=$(jq -r '.fieldStatusFlags // "-"' <<<"$line")
    typeInfo=""; pickInfo="";
    if [[ -n "$ref" ]]; then
      # Buscar en el summary
      entry=$(echo "$FIELDS_SUMMARY" | jq -r --arg r "$ref" 'map(select(.ref==$r)) | .[0] | select(.!=null) | @base64' 2>/dev/null || true)
      if [[ -n "$entry" ]]; then
        decoded=$(echo "$entry" | base64 -d 2>/dev/null || true)
        gType=$(echo "$decoded" | jq -r '.globalType // .projectType // empty' 2>/dev/null || true)
        pItems=$(echo "$decoded" | jq -r 'if (has("picklistItems") and (.picklistItems|type=="array") and (.picklistItems|length)>0) then (.picklistItems|join(" | ")) else empty end' 2>/dev/null || true)
        [[ -n "$gType" && "$gType" != "null" ]] && typeInfo=" | type: $gType"
        [[ -n "$pItems" ]] && pickInfo=" | vals: $pItems"
      fi
      # Fallback individual si falta tipo o picklist
      if [[ -z "$pickInfo" || -z "$typeInfo" ]]; then
        IND_JSON=$(bash "${SCRIPT_DIR}/work_item_types_field_get.sh" --project "$PROJECT" --type "$TYPE" --field "$ref" --summary --show-picklist-items 2>/dev/null || echo '{}')
        indType=$(echo "$IND_JSON" | jq -r '.globalType // .projectType // empty' 2>/dev/null || true)
        indPick=$(echo "$IND_JSON" | jq -r 'if (.picklistItems|type=="array" and (.picklistItems|length)>0) then (.picklistItems|join(" | ")) else empty end' 2>/dev/null || true)
        [[ -n "$indType" && -z "$typeInfo" ]] && typeInfo=" | type: $indType"
        [[ -n "$indPick" && -z "$pickInfo" ]] && pickInfo=" | vals: $indPick"
      fi
      # NUEVO: Fallback global para obtener picklistId y valores reales (campos custom no listan items en summary)
      if [[ -z "$pickInfo" ]]; then
        # Recuperar definición global (cache)
        if [[ -z "${CACHE_GLOBAL_FIELD_JSON[$ref]:-}" ]]; then
          CACHE_GLOBAL_FIELD_JSON[$ref]=$(bash "${SCRIPT_DIR}/fields_global_get.sh" --field "$ref" --raw 2>/dev/null || echo '{}')
          [[ $DEBUG -eq 1 ]] && >&2 echo "[debug] global_get $ref -> $(echo "${CACHE_GLOBAL_FIELD_JSON[$ref]}" | jq -r '.picklistId? // "(sin picklistId)"')"
        fi
        gjson="${CACHE_GLOBAL_FIELD_JSON[$ref]}"
        gPicklistId=$(echo "$gjson" | jq -r '.picklistId? // empty')
        gIsPick=$(echo "$gjson" | jq -r '.isPicklist? // false')
        gType2=$(echo "$gjson" | jq -r '.type? // empty')
        if [[ -n "$gType2" && -z "$typeInfo" ]]; then
          typeInfo=" | type: $gType2"
        fi
        if [[ "$gIsPick" == "true" && -n "$gPicklistId" ]]; then
          # Obtener items del picklist (cache)
            if [[ -z "${CACHE_PICKLIST_ITEMS[$gPicklistId]:-}" ]]; then
              rawPick=$(bash "${SCRIPT_DIR}/../workitemtrackingprocess/picklists_get.sh" --id "$gPicklistId" --raw 2>/dev/null || echo '{}')
              itemsJoined=$(echo "$rawPick" | jq -r 'if (.items|type=="array" and (.items|length)>0) then (.items|join(" | ")) else empty end')
              CACHE_PICKLIST_ITEMS[$gPicklistId]="$itemsJoined"
              [[ $DEBUG -eq 1 ]] && >&2 echo "[debug] picklist_get $gPicklistId items=$(echo "$rawPick" | jq -r '.items|length // 0')"
            fi
            if [[ -n "${CACHE_PICKLIST_ITEMS[$gPicklistId]}" ]]; then
              pickInfo=" | vals: ${CACHE_PICKLIST_ITEMS[$gPicklistId]}"
            fi
        fi
      fi
    fi
    echo "* ${ref:-'(sin ref)'} -> $msg | flags: $flags$typeInfo$pickInfo" >&2
  done < <(jq -c '.customProperties.RuleValidationErrors[]' <<<"$RESP")
  echo >&2
  echo "Sugerencias (--fields):" >&2
  jq -r '.customProperties.RuleValidationErrors[] | select((.fieldStatusFlags//"" )|test("required")) | "  --fields " + (.fieldReferenceName//"REF") + "=<valor>"' <<<"$RESP" | sort -u >&2 || true
  echo "--- Fin validación detallada ---" >&2
fi

# Mostrar JSON completo (stdout) salvo que se pida solo resumen
if [[ $RAW -eq 1 ]]; then
  printf '%s\n' "$RESP"
else
  echo "$RESP" | jq .
fi

# Diagnóstico adicional (solo si hay mensaje de error y no se desactiva)
if [[ $NO_DIAG -eq 0 && -n "$ERR_MSG" ]]; then
  {
    echo "--- Diagnóstico de campos para el tipo '$TYPE' (proyecto '$PROJECT') ---"
    echo "Mostrando campos requeridos y picklists (vista resumen)."
    bash "${SCRIPT_DIR}/work_item_types_field_list.sh" --project "$PROJECT" --type "$TYPE" --required-only --summary --show-picklist-items 2>&1 || echo "(Fallo obteniendo listado de campos)"
    echo "--- Fin diagnóstico ---"
  } >&2
fi

exit 5
