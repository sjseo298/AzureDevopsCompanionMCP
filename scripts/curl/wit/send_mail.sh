#!/usr/bin/env bash
set -euo pipefail
# Script derivado de api_doc/wit_sections/send_mail.md (Send Mail)
# POST /{organization}/{project}/_apis/wit/sendmail?api-version=7.2-preview.1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/_env.sh"

usage(){ cat <<'USAGE'
Uso: send_mail.sh --project <proyecto> --to <lista> --subject <texto> --body <texto> --work-item-ids <ids> [--cc <lista>] [--reply-to <lista>] [--reason <texto>] [--body-file <path>] [--raw]

Fuente: api_doc/wit_sections/send_mail.md

Descripción:
  Envía un correo relacionado a uno o varios work items.
  Campos obligatorios: --project, --to, --subject, (--body o --body-file), --work-item-ids.
  api-version 7.2-preview.1.

Parámetros:
  --project         Proyecto (obligatorio)
  --to              Lista de destinatarios principal (separados por comas) (obligatorio)
  --subject         Asunto (obligatorio)
  --body            Cuerpo del mensaje (texto/HTML) (obligatorio si no se usa --body-file)
  --body-file       Archivo con el cuerpo (alternativa a --body)
  --work-item-ids   IDs separados por comas (obligatorio)
  --cc              Lista CC (opcional)
  --reply-to        Lista replyTo (opcional)
  --reason          Motivo/descripción (opcional)
  --raw             Muestra respuesta cruda JSON
  -h|--help         Ayuda

Ejemplos:
  scripts/curl/wit/send_mail.sh --project "Mi Proyecto" --to user@dom.com --subject "Aviso" --body "<b>Update</b>" --work-item-ids 123
  scripts/curl/wit/send_mail.sh --project "Mi Proyecto" --to a@x.com,b@y.com --cc c@z.com --subject "Multi" --body-file ./mensaje.html --work-item-ids 123,456 --reason "Notificación manual"
USAGE
}

PROJECT=""; TO=""; SUBJECT=""; BODY=""; BODY_FILE=""; WORK_ITEM_IDS=""; CC=""; REPLY_TO=""; REASON=""; RAW=false; DEBUG=false; HTML_WRAP=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project) PROJECT="$2"; shift 2;;
    --to) TO="$2"; shift 2;;
    --subject) SUBJECT="$2"; shift 2;;
    --body) BODY="$2"; shift 2;;
    --body-file) BODY_FILE="$2"; shift 2;;
    --work-item-ids) WORK_ITEM_IDS="$2"; shift 2;;
    --cc) CC="$2"; shift 2;;
    --reply-to) REPLY_TO="$2"; shift 2;;
    --reason) REASON="$2"; shift 2;;
    --html) HTML_WRAP=true; shift;;
    --raw) RAW=true; shift;;
    --debug) DEBUG=true; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Argumento no reconocido: $1" >&2; usage; exit 1;;
  esac
done

# Validaciones
if [[ -z "$PROJECT" || -z "$TO" || -z "$SUBJECT" || -z "$WORK_ITEM_IDS" ]]; then echo "Faltan parámetros obligatorios" >&2; usage; exit 2; fi
if [[ -z "$BODY" && -z "$BODY_FILE" ]]; then echo "Debe proporcionar --body o --body-file" >&2; exit 3; fi
if [[ -n "$BODY" && -n "$BODY_FILE" ]]; then echo "Use solo uno: --body o --body-file" >&2; exit 4; fi
if [[ -n "$BODY_FILE" && ! -f "$BODY_FILE" ]]; then echo "Archivo no encontrado: $BODY_FILE" >&2; exit 5; fi

if [[ -n "$BODY_FILE" ]]; then BODY=$(cat "$BODY_FILE"); fi
if $HTML_WRAP; then BODY="<html><body>${BODY}</body></html>"; fi

enc_csv_to_array_json(){
  local list="$1"; local force_number="${2:-false}";
  if [[ -z "$list" ]]; then jq -cn '[]'; return; fi
  if [[ "$force_number" == true ]]; then
    jq -cn --arg v "$list" '($v|split(",")| map(.|gsub("^\\s+|\\s+$";"")) | map(select(length>0)) | map(tonumber))'
  else
    jq -cn --arg v "$list" '($v|split(",")| map(.|gsub("^\\s+|\\s+$";"")) | map(select(length>0)))'
  fi
}

TO_JSON=$(enc_csv_to_array_json "$TO")
CC_JSON=$(enc_csv_to_array_json "$CC")
REPLY_JSON=$(enc_csv_to_array_json "$REPLY_TO")
WIS_JSON=$(enc_csv_to_array_json "$WORK_ITEM_IDS" true)

BODY_JSON=$(jq -n --arg subject "$SUBJECT" --arg body "$BODY" --argjson to "$TO_JSON" '{message:{subject:$subject, body:$body, to:$to}}')
if [[ $(jq -r '. | length' <<<"$CC_JSON") -gt 0 ]]; then BODY_JSON=$(echo "$BODY_JSON" | jq --argjson cc "$CC_JSON" '.message.cc = $cc'); fi
if [[ $(jq -r '. | length' <<<"$REPLY_JSON") -gt 0 ]]; then BODY_JSON=$(echo "$BODY_JSON" | jq --argjson rt "$REPLY_JSON" '.message.replyTo = $rt'); fi
BODY_JSON=$(echo "$BODY_JSON" | jq --argjson ids "$WIS_JSON" '. + {workItemIds: $ids}')
if [[ -z "$REASON" ]]; then REASON="Manual notification"; fi
BODY_JSON=$(echo "$BODY_JSON" | jq --arg r "$REASON" '. + {reason: $r}')

ENC_PROJECT=$(jq -rn --arg s "$PROJECT" '$s|@uri')
URL="${DEVOPS_BASE}/${ENC_PROJECT}/_apis/wit/sendmail?api-version=7.2-preview.1"

RESP=$(curl -sS -u ":${AZURE_DEVOPS_PAT}" -H "Accept: application/json;api-version=7.2-preview.1" -H "Content-Type: application/json" -X POST -d "$BODY_JSON" "$URL" || true)

if $DEBUG; then echo "Payload enviado:" >&2; echo "$BODY_JSON" | jq . >&2; fi

if ! jq -e . >/dev/null 2>&1 <<<"$RESP"; then
  echo "Respuesta no JSON:" >&2
  echo "$RESP" >&2
  exit 90
fi

if [[ $(echo "$RESP" | jq -r '.message? // empty') != "" && $(echo "$RESP" | jq -r '.typeKey? // empty') != "" ]]; then
  msg=$(echo "$RESP" | jq -r '.message')
  typek=$(echo "$RESP" | jq -r '.typeKey')
  if [[ "$typek" == "ArgumentNullException" ]]; then
    echo "Error remoto: $msg (type: $typek). Posibles causas: emails no válidos en la organización o cuerpo vacío tras sanitización." >&2
  else
    echo "Error remoto: $msg (type: $typek)" >&2
  fi
  exit 91
fi

if $RAW; then
  echo "$RESP" | jq .
else
  STATUS=$(echo "$RESP" | jq -r '.status? // .Status? // empty')
  if [[ -n "$STATUS" ]]; then
    echo "Estado: $STATUS"
  else
    echo "$RESP" | jq .
  fi
fi
