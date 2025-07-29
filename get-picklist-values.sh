#!/bin/bash

# Script para obtener valores de picklists importantes de Sura
export AZURE_DEVOPS_PAT="dxlchhaa5assxmppanr2l47dxyqx7uixvp7gj6o2fmix7gnkdl2q"
export AZURE_DEVOPS_ORGANIZATION="SuraColombia"

echo "🎯 Obteniendo valores de picklists importantes de Sura..."
echo "========================================================"

# Función para obtener valores de picklist
get_picklist_values() {
    local name="$1"
    local picklist_id="$2"
    
    echo -e "\n📋 $name:"
    echo "   ID: $picklist_id"
    
    # Intentar diferentes endpoints para obtener los valores
    echo "   🔍 Intentando obtener valores..."
    
    # Endpoint 1: Proceso de trabajo
    local url1="https://dev.azure.com/SuraColombia/_apis/work/processes/lists/$picklist_id?api-version=7.1"
    local result1=$(curl -s -H "Authorization: Basic $(echo -n ":$AZURE_DEVOPS_PAT" | base64)" "$url1")
    
    if echo "$result1" | jq -e '.items' > /dev/null 2>&1; then
        echo "   ✅ Valores encontrados:"
        echo "$result1" | jq -r '.items[]' | sed 's/^/      - /'
        return 0
    fi
    
    # Endpoint 2: Contexto del proyecto
    local url2="https://dev.azure.com/SuraColombia/Gerencia_Tecnologia/_apis/work/processes/lists/$picklist_id?api-version=7.1"
    local result2=$(curl -s -H "Authorization: Basic $(echo -n ":$AZURE_DEVOPS_PAT" | base64)" "$url2")
    
    if echo "$result2" | jq -e '.items' > /dev/null 2>&1; then
        echo "   ✅ Valores encontrados:"
        echo "$result2" | jq -r '.items[]' | sed 's/^/      - /'
        return 0
    fi
    
    # Endpoint 3: Campo específico
    local field_ref=$(echo "$name" | sed 's/ /_/g')
    local url3="https://dev.azure.com/SuraColombia/Gerencia_Tecnologia/_apis/wit/fields/$field_ref/allowedValues?api-version=7.1"
    local result3=$(curl -s -H "Authorization: Basic $(echo -n ":$AZURE_DEVOPS_PAT" | base64)" "$url3")
    
    if echo "$result3" | jq -e '.value' > /dev/null 2>&1; then
        echo "   ✅ Valores encontrados:"
        echo "$result3" | jq -r '.value[]' | sed 's/^/      - /'
        return 0
    fi
    
    echo "   ❌ No se pudieron obtener los valores de esta picklist"
    echo "   📝 Respuesta: $result1"
}

# Obtener valores para los campos más importantes
get_picklist_values "Tipo de Historia Técnica" "b520383e-10b5-4fea-b2b5-5a9d35311994"
get_picklist_values "Tipo de Historia" "7a4308b0-57b5-44a1-a68d-ac29caf42123"
get_picklist_values "Tipo de tarea" "a15062db-4fc1-4418-afc5-ed0772fa8f37"
get_picklist_values "Tipo de subtarea" "8eaa9ab8-8496-4ade-9f55-eaa7727f605d"
get_picklist_values "Bloqueante" "c7871ed5-d8b1-4d45-b0dc-2e1eed6e0635"
get_picklist_values "Origen" "38776ac6-9249-4886-9dbe-3ecc6211388f"
get_picklist_values "Etapa de descubrimiento" "92c3c570-48b6-45de-83ef-858ed5afaeaa"
get_picklist_values "Nivel prueba" "dd0bbd16-e48e-42f1-a2f7-0c6934567012"
get_picklist_values "La historia hace parte de una migración de datos" "7f5cbac9-9bde-47b7-ae83-a5053414ce90"
get_picklist_values "La historia corresponde a un cumplimiento regulatorio" "8e90af44-31b1-4d33-8273-689cacb33e41"
get_picklist_values "La historia hace parte de un control automático o control de aplicación" "91844484-b1de-40b6-bd59-2986a1d969f8"

echo -e "\n✨ Investigación de picklists completada!"
