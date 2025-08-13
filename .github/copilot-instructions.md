---
applyTo: "**"
description: Reglas de implementación para APIs de Azure DevOps (MCP Server)
---

## Principios de Arquitectura

### Separación de Responsabilidades
1. **Tools (Capa MCP)**: Solo adaptación de parámetros y formateo de respuestas
2. **Helpers (Lógica de Negocio)**: Encapsulan toda la funcionalidad específica
3. **Cliente Unificado**: `AzureDevOpsClientService` para todas las llamadas HTTP

### Enfoque Práctico
- Implementa conforme lees la documentación
- Crea archivos progresivamente para evitar sobrecarga de contexto
- Valida cada endpoint con cURL antes de implementar

## Stack Tecnológico
- **Backend**: Spring Boot (Java)
- **HTTP Client**: WebClient (ReactiveStreams)
- **Protocolo**: MCP (Model Context Protocol)
- **Autenticación**: Azure DevOps PAT (Personal Access Token)
- **Build**: Gradle
- **Testing**: JUnit 5
- **Configuración**: YAML/JSON
- **Scope**: Todo lo documentado en `api_doc/` (orden alfabético)

## Configuración del Entorno

### Variables Requeridas
```bash
AZURE_DEVOPS_ORGANIZATION    # Nombre de la organización
AZURE_DEVOPS_PAT            # Personal Access Token
```

### Variables Opcionales (con defaults)
```bash
AZURE_DEVOPS_API_VERSION=7.2-preview.1
AZURE_DEVOPS_VSSPS_API_VERSION=7.1
```

### URLs Base
- **DevOps**: `https://dev.azure.com/${AZURE_DEVOPS_ORGANIZATION}` (proyectos/organización)
- **VSSPS**: `https://app.vssps.visualstudio.com` (Accounts/Profiles)

### Seguridad
- **NUNCA** registrar el PAT en logs o archivos
- Usar variables de entorno para credenciales

## Flujo de Desarrollo Obligatorio

### 1. Selección de Endpoint
- Elegir de `api_doc/` y subsecciones referenciadas
- Seguir orden alfabético de implementación

### 2. Validación con cURL
- Crear script en `scripts/curl/<area>/`
- Usar `_env.sh` para configuración
- Ejecutar hasta validar (sin 4xx/5xx con datos válidos)

### 3. Implementación MCP
- Crear herramienta usando cliente unificado
- Implementar helper con lógica de negocio
- Tool solo como adaptador MCP

### 4. Validación Final
- Tests mínimos en verde
- Documentar uso
- Actualizar progreso

## Recarga del Servidor MCP
Siempre que se realice un cambio en el código (nuevos helpers, tools, ajustes de cliente, etc.):
1. Ejecutar build: `./gradlew clean build`.
2. Reiniciar el servidor MCP para que los cambios queden activos.
3. Volver a invocar el tool afectado y (si aplica) el script cURL correspondiente para validar.
Esta notificación debe recordarse en cada respuesta tras aplicar cambios.

## Reglas para scripts cURL
- Ubicación: `scripts/curl/<area>/<operacion>.sh`.
- Incluir `scripts/curl/_env.sh` (valida variables, define `curl_json`, `DEVOPS_BASE`, `VSSPS_BASE`).
- Aceptar parámetros por argumentos/flags (IDs, filtros) y construir URL correcta.
- Usar `curl_json "$URL"` y, si procede, formatear con `jq`.
- Para nombres con espacios o caracteres especiales (project/team), codificar en URL.

### Nivel de documentación requerido para cURL
- Cada script DEBE derivarse explícitamente del archivo en `api_doc/<area>/<endpoint>.md` correspondiente. No inventar rutas no documentadas localmente.
- Incluir un bloque `usage` que referencie el archivo MD fuente e indique:
  - Parámetros obligatorios/opcionales y flags admitidos.
  - Ejemplos de uso (incluyendo valores con espacios y caracteres especiales correctamente citados).
- Validar y exigir los parámetros marcados como obligatorios en la doc. Si en nuestra organización un parámetro opcional es de facto requerido, indicarlo (“obligatorio en nuestra org”).
- Respetar el nivel del recurso según la doc local: nivel proyecto, equipo o tablero. Si existen variantes, crear scripts separados y con nombres inequívocos (p.ej., `get_boardrows_project.sh` vs `get_boardrows.sh`).
- Codificar SIEMPRE todos los segmentos de ruta con `jq -rn --arg s "$val" '$s|@uri'`.
- Usar `"${AZURE_DEVOPS_API_VERSION}"` por defecto. Donde la doc local especifique una preview distinta, usar esa versión (ej.: capacities `7.2-preview.3`, cardsettings/cardrulesettings `7.2-preview.2`).
- Evitar endpoints no documentados en la doc local (p.ej., no usar `/work/columns` si `columns.md` exige board).
- Consideraciones de salida/seguridad:
  - Mostrar JSON formateado con `jq` cuando aplique; si la respuesta es binaria, mostrar headers para validar.
  - No imprimir secretos (PAT) ni variables sensibles.
  - El script debe mostrar `usage` y salir con código != 0 si faltan argumentos requeridos.

## Reglas para herramientas MCP
- Todas extienden `AbstractAzureDevOpsTool` y usan SIEMPRE `AzureDevOpsClientService` (sin lógica HTTP duplicada).
- Esquema de entrada: incluir `project` (requerido) y `team` (opcional). Parámetros específicos mínimos necesarios.
- Cliente HTTP:
  - Endpoints VSSPS: `getVsspsApi(pathWithQuery)`.
  - Endpoints de proyecto (work/core/etc.): helpers como `getWorkApi(project, team, path)`.
- Convenciones de nombres:
  - Formato: `azuredevops_<area>_<operacion>`.
  - Ejemplos: `azuredevops_core_get_projects`, `azuredevops_core_create_project`, `azuredevops_work_get_boards`, `azuredevops_accounts_get_accounts`, `azuredevops_profile_get_my_memberid`.
  - La clase Java mantiene el sufijo `Tool` (ej. `ProjectsTool`), y los tests `<Clase>Test`.
- Manejo de errores consistente y mensajes claros; no exponer secretos.
- Formateo de salida:
  - Listas (`count + value[]`): enumerar elementos.
  - Respuestas unitarias: mostrar campos clave.
  - Fallback a JSON/string si es complejo.

## Estándar de nombres por área (nuevo)
- Para identificar claramente el área de la API, los tools deben seguir `azuredevops_<area>_<operacion>`.
- Áreas comunes: `core`, `work`, `accounts`, `profile`, `wit`, `build`, etc.
- Ejemplos:
  - Core: `azuredevops_core_get_projects`, `azuredevops_core_get_teams`, `azuredevops_core_create_project`.
  - Work: `azuredevops_work_get_boards`, `azuredevops_work_get_backlogs`.
  - Accounts: `azuredevops_accounts_get_accounts`.
  - Profile: `azuredevops_profile_get_my_memberid`.
- Migración: renombrar herramientas existentes que no cumplan el estándar y actualizar tests/documentación.

## Manejo de Errores HTTP
- El cliente `AzureDevOpsClientService` captura respuestas HTTP de error con estructura enriquecida:
  - `isHttpError: true`, `httpStatus`, `httpReason` y cuerpo JSON original
- `AbstractAzureDevOpsTool` expone `tryFormatRemoteError(Map)` para formatear errores
- Tras cada llamada a la API, invocar `tryFormatRemoteError(resp)` para mantener formato consistente

## Testing y Validación
- **NO crear pruebas unitarias** a menos que sea explícitamente solicitado
- Validar funcionamiento mediante scripts cURL y pruebas manuales
- Verificar definición (nombre, descripción, schema) y parámetros obligatorios

## Criterios de Completitud
- Script(s) cURL creados y validados (sin 4xx/5xx con datos válidos)
- Herramienta MCP implementa la misma ruta y parámetros que el script validado
- Tests mínimos en verde y documentación/progreso actualizados

