#!/bin/bash
export AZURE_DEVOPS_ORGANIZATION="YOUR_ORGANIZATION"
export AZURE_DEVOPS_PAT="dxlchhaa5assxmppanr2l47dxyqx7uixvp7gj6o2fmix7gnkdl2q"

# Obtener todos los campos personalizados de Historia técnica que contienen "solucion" o "apm" en su nombre
curl -s -X GET "https://dev.azure.com/$AZURE_DEVOPS_ORGANIZATION/Gerencia_Tecnologia/_apis/wit/workitemtypes/Historia%20t%C3%A9cnica/fields?api-version=7.1-preview.2" \
  -H "Authorization: Basic $(echo -n :$AZURE_DEVOPS_PAT | base64)" | \
  jq -r ".value[] | select(.name | test(\"(?i)solucion|apm\")) | {name, referenceName}"
