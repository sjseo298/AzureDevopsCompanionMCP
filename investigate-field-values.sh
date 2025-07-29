#!/bin/bash

# Script para investigar los valores permitidos en campos personalizados de YOUR_ORGANIZATION
export AZURE_DEVOPS_PAT="dxlchhaa5assxmppanr2l47dxyqx7uixvp7gj6o2fmix7gnkdl2q"
export AZURE_DEVOPS_ORGANIZATION="YOUR_ORGANIZATION"

echo "🔍 Investigando campos personalizados de YOUR_ORGANIZATION..."
echo "=============================================="

# Función para hacer requests a la API
make_request() {
    local url="$1"
    local auth_header="Authorization: Basic $(echo -n ":$AZURE_DEVOPS_PAT" | base64)"
    
    curl -s -H "$auth_header" -H "Accept: application/json" "$url"
}

# 1. Obtener definición completa del work item type "Historia técnica"
echo -e "\n📋 1. Investigando definición de 'Historia técnica'..."
HISTORIA_TECNICA_URL="https://dev.azure.com/YOUR_ORGANIZATION/Gerencia_Tecnologia/_apis/wit/workitemtypes/Historia%20t%C3%A9cnica?api-version=7.1"
make_request "$HISTORIA_TECNICA_URL" | jq '.' > historia_tecnica_definition.json

# 2. Obtener definición de "Historia"
echo -e "\n📋 2. Investigando definición de 'Historia'..."
HISTORIA_URL="https://dev.azure.com/YOUR_ORGANIZATION/Gerencia_Tecnologia/_apis/wit/workitemtypes/Historia?api-version=7.1"
make_request "$HISTORIA_URL" | jq '.' > historia_definition.json

# 3. Obtener definición de "Tarea"
echo -e "\n📋 3. Investigando definición de 'Tarea'..."
TAREA_URL="https://dev.azure.com/YOUR_ORGANIZATION/Gerencia_Tecnologia/_apis/wit/workitemtypes/Tarea?api-version=7.1"
make_request "$TAREA_URL" | jq '.' > tarea_definition.json

# 4. Obtener todos los campos del proyecto para ver los valores permitidos
echo -e "\n🔧 4. Obteniendo todos los campos del proyecto..."
FIELDS_URL="https://dev.azure.com/YOUR_ORGANIZATION/Gerencia_Tecnologia/_apis/wit/fields?api-version=7.1"
make_request "$FIELDS_URL" | jq '.' > project_fields.json

# 5. Buscar campos específicos de YOUR_ORGANIZATION
echo -e "\n🎯 5. Filtrando campos personalizados de YOUR_ORGANIZATION..."
echo "Campos que contienen 'Custom' o 'Tipo':"
jq -r '.value[] | select(.referenceName | contains("Custom") or contains("Tipo")) | "\(.referenceName): \(.name)"' project_fields.json

# 6. Intentar obtener valores permitidos para campos específicos
echo -e "\n📝 6. Buscando valores permitidos en campos clave..."

# Buscar campo "Tipo de Historia Técnica"
echo "🔍 Buscando 'Tipo de Historia Técnica'..."
jq -r '.value[] | select(.name | contains("Tipo de Historia") and contains("Técnica")) | {name: .name, referenceName: .referenceName, type: .type}' project_fields.json

# Buscar campo "Tipo de Historia"
echo "🔍 Buscando 'Tipo de Historia'..."
jq -r '.value[] | select(.name | contains("Tipo de Historia") and (contains("Técnica") | not)) | {name: .name, referenceName: .referenceName, type: .type}' project_fields.json

# Buscar campo "Tipo de Tarea"
echo "🔍 Buscando 'Tipo de Tarea'..."
jq -r '.value[] | select(.name | contains("Tipo") and contains("Tarea")) | {name: .name, referenceName: .referenceName, type: .type}' project_fields.json

# 7. Examinar los archivos JSON generados para valores permitidos
echo -e "\n📊 7. Analizando definiciones de work items..."

if [ -f "historia_tecnica_definition.json" ]; then
    echo "✅ Definición de Historia técnica guardada en: historia_tecnica_definition.json"
    # Buscar allowedValues en la definición
    if jq -e '.fieldInstances[] | select(.referenceName | contains("Tipo")) | .allowedValues' historia_tecnica_definition.json > /dev/null 2>&1; then
        echo "🎯 Valores permitidos encontrados en Historia técnica:"
        jq -r '.fieldInstances[] | select(.referenceName | contains("Tipo")) | "\(.referenceName): \(.allowedValues)"' historia_tecnica_definition.json
    fi
fi

if [ -f "historia_definition.json" ]; then
    echo "✅ Definición de Historia guardada en: historia_definition.json"
    # Buscar allowedValues en la definición
    if jq -e '.fieldInstances[] | select(.referenceName | contains("Tipo")) | .allowedValues' historia_definition.json > /dev/null 2>&1; then
        echo "🎯 Valores permitidos encontrados en Historia:"
        jq -r '.fieldInstances[] | select(.referenceName | contains("Tipo")) | "\(.referenceName): \(.allowedValues)"' historia_definition.json
    fi
fi

if [ -f "tarea_definition.json" ]; then
    echo "✅ Definición de Tarea guardada en: tarea_definition.json"
    # Buscar allowedValues en la definición
    if jq -e '.fieldInstances[] | select(.referenceName | contains("Tipo")) | .allowedValues' tarea_definition.json > /dev/null 2>&1; then
        echo "🎯 Valores permitidos encontrados en Tarea:"
        jq -r '.fieldInstances[] | select(.referenceName | contains("Tipo")) | "\(.referenceName): \(.allowedValues)"' tarea_definition.json
    fi
fi

echo -e "\n✨ Investigación completada!"
echo "📁 Archivos generados:"
echo "   - historia_tecnica_definition.json"
echo "   - historia_definition.json" 
echo "   - tarea_definition.json"
echo "   - project_fields.json"
echo -e "\n💡 Revisa estos archivos para encontrar los valores exactos permitidos en los campos personalizados."
