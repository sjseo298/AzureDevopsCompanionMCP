8. **Gestión de Progreso y Tamaño de Archivos**
9. **Actualización y Lectura de Archivos de Progreso**
   - Para cada área y subfuncionalidad, consulta el archivo de avance correspondiente en la subcarpeta.
   - Antes de implementar o modificar una funcionalidad, revisa el archivo .md de progreso para conocer el estado actual y las instrucciones específicas.
   - Al avanzar en la implementación, actualiza el archivo .md de la subfuncionalidad, cambiando el estado, agregando notas, ejemplos, fechas y responsables si aplica.
   - Si una funcionalidad se completa, marca el estado como ✅ y documenta los endpoints, parámetros y ejemplos de uso.
   - Mantén la trazabilidad referenciando los archivos de avance desde este archivo principal y desde la documentación local.
   - El progreso de cada documento debe mantenerse en archivos pequeños y claros.
   - Si para registrar el progreso se necesita almacenar más información, crea subcarpetas específicas para cada área o funcionalidad.
   - Cada subcarpeta debe contener archivos de avance detallado y ser referenciada desde este archivo principal, asegurando trazabilidad y organización.
# Progreso de Implementación del Cliente Azure DevOps REST API
# Instrucciones para la Implementación del Cliente de APIs

Para mantener la implementación organizada y evitar código duplicado, sigue estas pautas al desarrollar el cliente de consumo de las APIs en este proyecto:

1. **Patrón de Organización**
   - Cada área de la API debe tener su propio módulo o clase, ubicado en una estructura de carpetas clara (por ejemplo, `src/main/java/api/azuredevops/<area>`).
   - Los métodos de consumo deben ser reutilizables y parametrizables, evitando duplicidad de lógica entre áreas.

2. **Exposición como Tools MCP**
   - Cada funcionalidad implementada debe exponerse como un "tool" para el servidor MCP, siguiendo la convención de tools ya definida en el proyecto.
   - Los tools deben tener nombres descriptivos y documentar claramente los parámetros de entrada y salida.

3. **Documentación y Ejemplos**
   - Actualiza el archivo de documentación local correspondiente (`api_doc/<area>.md`) con ejemplos de uso, parámetros y posibles respuestas.
   - Incluye ejemplos de consumo con cURL y describe los posibles valores de los parámetros.

4. **Evitar Código Duplicado**
   - Centraliza la lógica común (autenticación, manejo de errores, formateo de peticiones) en utilidades compartidas.
   - Revisa los módulos existentes antes de crear nuevos métodos para evitar duplicidad.

5. **Pruebas y Validación**
   - Implementa pruebas unitarias para cada tool expuesto.
   - Valida que los tools funcionen correctamente con los endpoints documentados.

6. **Convenciones de Nombres y Estructura**
   - Usa nombres consistentes y descriptivos para clases, métodos y tools.
   - Mantén la estructura de carpetas alineada con la documentación y el índice de progreso.

7. **Actualización de Progreso**
   - Marca el avance en este archivo cada vez que se implemente un nuevo cliente, cambiando el estado de ⏳ a ✅.

Estas instrucciones aseguran que el desarrollo sea escalable, mantenible y fácil de integrar con el servidor MCP.

Este archivo resume el estado de avance de la implementación de un cliente para cada área de la API que ya tiene documentación local creada en el proyecto.

| Sección | Archivo de Documentación | Cliente Implementado |
|---------|-------------------------|---------------------|
| Accounts | [api_doc/accounts.md](api_doc/accounts.md) | ⏳ |
| Advanced Security | [api_doc/advanced_security.md](api_doc/advanced_security.md) | ⏳ |
| Approvals And Checks | [api_doc/approvals_and_checks.md](api_doc/approvals_and_checks.md) | ⏳ |
| Artifacts | [api_doc/artifacts.md](api_doc/artifacts.md) | ⏳ |
| Artifacts Package Types | [api_doc/artifactspackagetypes.md](api_doc/artifactspackagetypes.md) | ⏳ |
| Audit | [api_doc/audit.md](api_doc/audit.md) | ⏳ |
| Build | [api_doc/build.md](api_doc/build.md) | ⏳ |
| Core | [api_doc/core.md](api_doc/core.md) | ✅ |
| Dashboard | [api_doc/dashboard.md](api_doc/dashboard.md) | ⏳ |
| Work | [api_doc/work.md](api_doc/work.md) | ⏳ |
| Work Item Tracking | [api_doc/wit.md](api_doc/wit.md) | ⏳ (iniciado) |

---

Notas de avance recientes
- Core (✅):
  - Tools MCP:
    - Projects: `azuredevops_core_get_projects`, `azuredevops_core_get_project`, `azuredevops_core_create_project`, `azuredevops_core_update_project`, `azuredevops_core_delete_project`, `azuredevops_core_get_project_properties`, `azuredevops_core_set_project_properties`.
    - Teams: `azuredevops_core_get_teams` (por proyecto), `azuredevops_core_get_all_teams` (organización), `azuredevops_core_get_team`, `azuredevops_core_create_team`, `azuredevops_core_update_team`, `azuredevops_core_delete_team`, `azuredevops_core_get_team_members`.
  - Cliente: `AzureDevOpsClientService` amplíado con `postCoreApi`, `patchCoreApi`, `deleteCoreApi` y soporte de api-version específica.
  - Scripts cURL:
    - Projects: `scripts/curl/core/list_projects.sh`, `scripts/curl/core/get_project.sh`, `scripts/curl/core/create_project.sh`, `scripts/curl/core/update_project.sh`, `scripts/curl/core/delete_project.sh`, `scripts/curl/core/get_project_properties.sh`, `scripts/curl/core/set_project_properties.sh`.
    - Teams: `scripts/curl/core/list_teams.sh`, `scripts/curl/core/get_all_teams.sh`, `scripts/curl/core/get_team.sh`, `scripts/curl/core/create_team.sh`, `scripts/curl/core/update_team.sh`, `scripts/curl/core/delete_team.sh`, `scripts/curl/core/get_team_members.sh`.
  - Tests mínimos: definición y esquema de entrada para Projects y Teams (se ampliarán para nuevas tools).

- Work Item Tracking (iniciado ⏳):
  - Cliente: agregado soporte `getWitApi` y `postWitApi` en `AzureDevOpsClientService`.
  - Scripts cURL (derivados 1:1 de `api_doc/wit_sections/*.md`):
    - `scripts/curl/wit/account_my_work_recent_activity.sh`
    - `scripts/curl/wit/artifact_link_types.sh`
    - `scripts/curl/wit/artifact_uri_query.sh`
    - `scripts/curl/wit/attachments_create.sh`
  - Tools MCP:
    - `azuredevops_wit_get_account_my_work_recent_activity` (`AccountMyWorkRecentActivityTool`)
    - `azuredevops_wit_get_artifact_link_types` (`ArtifactLinkTypesTool`)
    - `azuredevops_wit_artifact_uri_query` (`ArtifactUriQueryTool`)
  - Tests mínimos: definición básica de tools creados.
  - Próximo: completar Attachments (get/delete), `classification_nodes`, `comments` y siguientes, siguiendo orden alfabético.
