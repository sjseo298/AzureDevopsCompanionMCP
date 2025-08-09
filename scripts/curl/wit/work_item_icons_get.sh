#!/usr/bin/env bash
set -euo pipefail

# Script derivado de api_doc/wit_sections/work_item_icons.md
# Operación: Get (GET)
# Nivel: Organización

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage() {
  cat <<'USAGE'
Uso: work_item_icons_get.sh --icon <icon_id> [--no-precheck] [--help]

Fuente: api_doc/wit_sections/work_item_icons.md

Descripción:
  Obtiene un ícono de work item por su id.

Parámetros:
  --icon         Identificador del ícono (obligatorio, ej: icon_insect)
  --no-precheck  No validar previamente contra la lista (omite sugerencias locales)
  --help         Mostrar ayuda

Ejemplos:
  scripts/curl/wit/work_item_icons_get.sh --icon icon_insect
  scripts/curl/wit/work_item_icons_get.sh --icon icon_clipboard
  scripts/curl/wit/work_item_icons_get.sh --icon icon_insect --no-precheck
USAGE
}

ICON=""
PRECHECK=1
while [[ $# -gt 0 ]]; do
  case "$1" in
    --icon) ICON="$2"; shift 2;;
    --no-precheck) PRECHECK=0; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

if [[ -z "$ICON" ]]; then
  echo "Falta --icon" >&2; usage; exit 2
fi

API_VER="7.2-preview.1"

ICON_IDS=()
if [[ $PRECHECK -eq 1 ]]; then
  LIST_JSON="$(curl_json "${DEVOPS_BASE}/_apis/wit/workitemicons?api-version=${API_VER}")"
  # Si falla la obtención de la lista, continuamos sin abortar (para intentar el GET directo)
  if [[ -n "$LIST_JSON" ]]; then
    mapfile -t ICON_IDS < <(echo "$LIST_JSON" | jq -r '.value[].id') || true
    if [[ ${#ICON_IDS[@]} -gt 0 ]]; then
      if ! printf '%s\n' "${ICON_IDS[@]}" | grep -Fxq "$ICON"; then
        echo "Icono '$ICON' no encontrado en la lista local." >&2
        # Sugerencias por substring
        SUGERENCIAS=$(printf '%s\n' "${ICON_IDS[@]}" | grep -i "${ICON//_/.*}" || true)
        if [[ -z "$SUGERENCIAS" ]]; then
          # fallback: primeras 10
            SUGERENCIAS=$(printf '%s\n' "${ICON_IDS[@]}" | head -n 10)
        fi
        echo "Sugerencias:" >&2
        printf '  %s\n' $SUGERENCIAS >&2
        exit 3
      fi
    fi
  fi
fi

ENC_ICON=$(jq -rn --arg s "$ICON" '$s|@uri')
URL="${DEVOPS_BASE}/_apis/wit/workitemicons/${ENC_ICON}?api-version=${API_VER}"

# Capturamos respuesta para formatear posibles errores.
RESP="$(curl_json "$URL" 2>/dev/null || true)"
if [[ -z "$RESP" ]]; then
  echo "Respuesta vacía del servicio" >&2
  exit 4
fi

# Detectar error remoto específico
if echo "$RESP" | jq -e '.typeKey? == "WorkItemTrackingIconNotFoundException"' >/dev/null 2>&1; then
  MSG=$(echo "$RESP" | jq -r '.message // "Icono no encontrado"')
  echo "Error remoto: $MSG (type: WorkItemTrackingIconNotFoundException)" >&2
  exit 5
fi

echo "$RESP" | jq .
