# Plan de Implementación de APIs Azure DevOps (MCP Server)

Este documento describe el proceso estandarizado para implementar nuevas funcionalidades de la Azure DevOps REST API (v7.2) dentro de este proyecto, respetando el stack definido:

Backend: Spring Boot (Java)  | Cliente HTTP: WebClient  | Protocolo: MCP  | Auth: PAT  | Build: Gradle  | Tests: JUnit 5  | Config: YAML/JSON  | Dev Container: Ubuntu 24.04

---
## 1. Alcance y Objetivo
Establecer un flujo repetible, seguro y mantenible para agregar soporte a nuevos endpoints de Azure DevOps (documentados en `api_doc/`) exponiéndolos como herramientas MCP consumibles desde VS Code u otros clientes MCP.

IMPORTANTE: Este plan establece como alcance obligatorio implementar y documentar TODO lo que está en la carpeta `api_doc/` (archivos en la raíz). Cuando alguno de esos archivos de la raíz haga referencia a contenido ubicado en subcarpetas (por ejemplo, `work/`, `wit_sections/`, `workitemtrackingprocess/`), dichos contenidos también deben documentarse y contarán dentro del progreso general.

---
## 2. Estructura Actual Relevante
- Cliente HTTP central: `AzureDevOpsClientService` (único para TODO el consumo; maneja hosts dev.azure.com y app.vssps.visualstudio.com)
- Configuración: Variables de entorno y/o properties para org, PAT y versiones (`AZURE_DEVOPS_ORGANIZATION`, `AZURE_DEVOPS_PAT`, `AZURE_DEVOPS_API_VERSION`, `AZURE_DEVOPS_VSSPS_API_VERSION`)
- Clase base herramientas: `AbstractAzureDevOpsTool`
- Herramientas existentes de referencia: `BacklogsTool`, `BoardsTool`, `AccountsTool`, `GetMyMemberIdTool`
- Documentación de endpoints: carpeta `api_doc/`
- Configuración MCP (cliente VS Code): `mcp.json`

---
## 3. Principios de Diseño
1. Reutilización: Cualquier nueva herramienta debe extender `AbstractAzureDevOpsTool`.
2. Baja duplicación: Nada de lógica HTTP repetida en la herramienta.
3. Resiliencia: Manejo de errores consistente (mensajes claros al usuario MCP).
4. Formateo legible: Salida textual estructurada para cada endpoint.
5. Evolutivo: Posibilidad de extraer formateadores reutilizables si se repiten patrones.
6. Seguridad: Nunca registrar el PAT. Validar parámetros obligatorios temprano.
7. Testing mínimo: Cada herramienta nueva aporta tests de definición y validación de parámetros.

---
## 4. Flujo General (Checklist)
| Paso | Acción | Resultado |
|------|--------|-----------|
| 1 | Seleccionar endpoint en `api_doc/` (obligatorio: se implementan primero todos los endpoints ya documentados) | Alcance claro |
| 2 | Construir script cURL de validación (en `scripts/curl/<area>/<operacion>.sh`) que use `_env.sh` | Doc verificada con ejemplo ejecutable |
| 3 | Ejecutar el script cURL y ajustar doc si hay inconsistencias | Validación previa |
| 4 | (Si aplica) Implementar helper de soporte como Tool (ej. `azuredevops_get_my_memberid`) | Reutilización |
| 5 | Clasificar tipo de recurso (lista, detalle, mixto) | Estrategia de formateo |
| 6 | Definir parámetros requeridos/opcionales | Esquema JSON listo |
| 7 | (Opcional) Añadir helper en `AzureDevOpsClientService` si se repite patrón | Reutilización |
| 8 | Crear clase `*Tool` en paquete `com.mcp.server.tools.azuredevops` | Implementación base |
| 9 | Extender esquema con parámetros propios | Validación formal |
|10 | Implementar `executeInternal` usando `AzureDevOpsClientService` | Llamada HTTP |
|11 | Implementar formateo específico | Salida limpia |
|12 | Añadir tests JUnit (definición, esquema, validaciones) | Calidad mínima |
|13 | Actualizar progreso en `api_client_progress/...` | Seguimiento |
|14 | Documentar uso en `README` o doc específico | Consumibilidad |
|15 | Commit + PR | Integración |

---
## 5. Plantilla para Nueva Herramienta
```java
@Component
public class ExampleTool extends AbstractAzureDevOpsTool {
    private static final String TOOL_NAME = "azuredevops_get_example";
    private static final String DESCRIPTION = "Describe la función";

    @Autowired
    public ExampleTool(AzureDevOpsClientService service) { super(service); }

    @Override public String getName() { return TOOL_NAME; }
    @Override public String getDescription() { return DESCRIPTION; }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String,Object> base = createBaseSchema(); // incluye project (req) y team (opt)
        @SuppressWarnings("unchecked")
        Map<String,Object> props = (Map<String,Object>) base.get("properties");
        props.put("exampleId", Map.of(
            "type", "string",
            "description", "ID opcional del recurso"
        ));
        return base;
    }

    @Override
    protected Map<String, Object> executeInternal(Map<String, Object> args) {
        String project = getProject(args);
        String team = getTeam(args); // opcional
        String exampleId = args.get("exampleId") == null ? null : args.get("exampleId").toString().trim();
        Map<String,Object> response;
        if (exampleId != null && !exampleId.isEmpty()) {
            response = azureService.getWorkApi(project, team, "examples/" + exampleId);
        } else {
            response = azureService.getWorkApi(project, team, "examples");
        }
        return success(format(response));
    }

    private String format(Map<String,Object> data) {
        // Implementar formateo específico; fallback a toString
        return data == null ? "(Respuesta vacía)" : data.toString();
    }
}
```

---
## 6. Ampliaciones en AzureDevOpsClientService
Solo agregar métodos helper cuando:
- Se repite un patrón de ruta (ej: `work/...`, `core/...`)
- Se requieran query params recurrentes (ej: `?$expand=...`)

Política de cliente único:
- Todas las herramientas deben usar SIEMPRE `AzureDevOpsClientService`.
- Este cliente centraliza credenciales mediante variables de entorno y/o properties:
  - AZURE_DEVOPS_ORGANIZATION (fallback `azure.devops.organization`)
  - AZURE_DEVOPS_PAT (fallback `azure.devops.pat`)
  - AZURE_DEVOPS_API_VERSION (fallback `azure.devops.api-version`)
  - AZURE_DEVOPS_VSSPS_API_VERSION (fallback `azure.devops.vssps-api-version`)
- Para endpoints VSSPS (Accounts/Profiles) usar `getVsspsApi(pathWithQuery)`.
- Para endpoints del proyecto (work/core/etc.) usar helpers específicos (ej: `getWorkApi(project, team, path)`).

Ejemplo:
```java
public Map<String,Object> getWorkItem(String project, String id) {
    return getWorkApi("workitems/" + id, project, null, null, null);
}
```

---
## 7. Convenciones de Nombres
| Elemento | Convención | Ejemplo |
|----------|------------|---------|
| Nombre herramienta | `azuredevops_get_<recurso>` | azuredevops_get_boards |
| Clase Java | `<Recurso>Tool` | BoardsTool |
| Test | `<Recurso>ToolTest` | BoardsToolTest |
| Parámetro ID | `<recurso>Id` | boardId |

---
## 8. Esquema de Entrada (JSON Schema simplificado)
Siempre incluir:
- `project` (string, requerido)
- `team` (string, opcional)
- Parámetros específicos (solo los necesarios, sin sobrecargar)

Validaciones adicionales (si se requieren) se implementan en `executeInternal` lanzando `IllegalArgumentException`.

---
## 9. Estrategia de Formateo
1. Respuestas con `count + value[]`: listar elementos numerados.
2. Respuestas unitarias: mostrar campos clave ordenados lógicamente (ID, nombre, estado, URL, etc.).
3. Listas anidadas: indentar + numerar.
4. Evitar volcar JSON crudo excepto en casos complejos (fallback: `super.formatData`).

Si el formateo se repite entre recursos, considerar una utilidad futura (no prematuro ahora).

---
## 10. Testing Mínimo (por herramienta)
- `testGetToolDefinition`: nombre, descripción, inputSchema no nulos.
- `testInputSchemaStructure`: contiene `project` y parámetros propios.
- `testExecuteWithMissingProject`: error.
- `testExecuteWithEmptyProject`: error.
- (Opcional) Test de formateo con datos simulados (si la lógica es no trivial).

Mocks avanzados (WireMock / MockWebServer) se pueden introducir posteriormente para pruebas integradas HTTP.

---
## 11. Configuración y Secrets
- `azure.devops.organization` y `azure.devops.pat` provienen de variables de entorno o `application.yml` (no commitear PAT reales).
- El `pat` nunca se escribe en logs.
- Validación temprana en `AzureDevOpsConfig.validate()`.

---
## 12. Seguridad
- Usar siempre HTTPS (ya cubierto por base URL).
- No construir rutas con datos sin sanitizar (trim + validar obligatorios).
- Manejo uniforme de errores: no exponer stack traces internos al usuario MCP.

---
## 13. Rendimiento
- WebClient es no bloqueante, pero actualmente uso simple (bloqueo con `.block()`). Escalar a reactivo completo sólo si hay necesidad real.
- Evitar llamadas repetidas dentro de un mismo `executeInternal` salvo sea imprescindible.

---
## 14. Errores y Observabilidad
- Captura genérica en `AbstractAzureDevOpsTool.execute`.
- Diferenciar claramente: parámetros inválidos vs. fallo remoto.
- (Futuro) Añadir logging estructurado y métricas (Micrometer) si se requiere.

---
## 15. Flujo de Commit / PR
1. Crear rama: `feature/<api>-<recurso>`
2. Implementar herramienta + tests + doc de progreso.
3. Actualizar `api_client_progress/<area>/<recurso>.md` marcando estado.
4. Ejecutar tests: `./gradlew test`.
5. PR con descripción: endpoint, parámetros, decisiones de formateo.

---
## 16. Ejemplo Concreto: Implementar Backlogs
1. Revisar doc en `api_doc/work/backlogs.md`.
2. Parámetros: `project` (req), `team` (opt), `backlogId` (opt), `includeWorkItems` (boolean opt para endpoint `/workItems`).
3. Lógica:
   - Si `backlogId` vacío => listar backlog levels.
   - Si `backlogId` y `includeWorkItems=true` => `backlogs/{id}/workItems`.
   - Si `backlogId` y no flag => `backlogs/{id}`.
4. Formateo:
   - Lista: tabla simple (ID, Name, Rank, Tipos)
   - Detalle: encabezado + workItemTypes
   - WorkItems: enumerar IDs o campos clave devueltos.
5. Tests: validar ramas de parámetros.

---
## 17. Roadmap Futuro (Opcional)
- Unificar formateadores para listas (`ListFormatter`).
- Cache ligero para catálogos poco cambiantes (work item types).
- Añadir soporte de paginación donde aplique.
- Incorporar mocks HTTP para tests de integración.

---
## 18. Criterios de Hecho por Herramienta
- [ ] Clase Tool implementada (para cada endpoint documentado en `api_doc/`)
- [ ] Esquema input correcto
- [ ] Formateo útil
- [ ] Tests mínimos verdes
- [ ] Documento progreso actualizado
- [ ] Sin secretos en commits
- [ ] Nombre consistente y registrado automáticamente (Spring Component)

---
## 19. Problemas Comunes y Soluciones
| Problema | Causa | Solución |
|----------|-------|----------|
| Error: PAT requerido | Falta variable entorno | Exportar / configurar en `application.yml` temporal local |
| 401 Unauthorized | PAT inválido | Regenerar PAT con scopes correctos (Work) |
| NullPointer en servicio | Inyección nula en tests | Usar constructor con `null` solo para tests que no ejecutan HTTP |
| Lista sin formatear | Faltó override `formatData` | Implementar formateo específico |

---
## 20. Ejecución de Pruebas
```bash
./gradlew clean test
```
Para inspeccionar un test específico:
```bash
./gradlew test --tests *BoardsToolTest
```

---
## 21. Referencias
- Azure DevOps REST Docs: https://learn.microsoft.com/en-us/rest/api/azure/devops/
- MCP Especificación: (consultar documentación oficial MCP)

---
## 22. Última Actualización
Generado automáticamente: (mantener fecha manual al actualizar)

- Fecha: 2025-08-08
- Autor: Plan Generator

---
## 23. Progreso de Implementación
- Infraestructura base (MCP + Spring Boot + WebClient): 70%
- Servicio HTTP AzureDevOpsClientService: 80%
- Clase base AbstractAzureDevOpsTool: 80%
- Herramienta Backlogs (listar/detalle/workItems): 70%
- Herramienta Boards (listar/detalle): 60%
- Pruebas unitarias mínimas: 40%
- Documentación y plan: 90%

Progreso global estimado: 70%

---
## 24. Flujo de implementación (orden exacto de api_doc/)
Todos los elementos documentados tienen la misma prioridad. Para garantizar un flujo determinista, se abordarán en el orden alfabético exacto de los archivos en la carpeta `api_doc/`. Para cada archivo raíz, se deben seguir todos los enlaces y documentar/implementar sus subsecciones referenciadas (incluyendo parámetros y ejemplos cURL completos), antes de pasar al siguiente archivo.

Orden de trabajo (inventario sincronizado con el árbol actual):

1) `api_doc/accounts.md`
   - Procesar todas las operaciones documentadas en el archivo.
2) `api_doc/advanced_security.md`
   - Procesar todas las operaciones documentadas en el archivo.
3) `api_doc/approvals_and_checks.md`
   - Procesar todas las operaciones documentadas en el archivo.
4) `api_doc/artifacts.md`
   - Procesar todas las operaciones documentadas en el archivo.
5) `api_doc/artifactspackagetypes.md`
   - Procesar todas las operaciones documentadas en el archivo.
6) `api_doc/audit.md`
   - Procesar todas las operaciones documentadas en el archivo.
7) `api_doc/build.md`
   - Procesar todas las operaciones documentadas en el archivo.
8) `api_doc/core.md`
   - Procesar todas las operaciones documentadas en el archivo.
9) `api_doc/dashboard.md`
   - Subtemas esperados (según doc oficial): Dashboards (listar/obtener/reemplazar/crear) y Widgets (listar/obtener/crear/actualizar/reemplazar/eliminar, mover y redimensionar).
10) `api_doc/wit.md`
    - Seguir y documentar TODAS las subsecciones referenciadas en `wit_sections/` (lista exacta actual):
      - `wit_sections/account_my_work_recent_activity.md`
      - `wit_sections/artifact_link_types.md`
      - `wit_sections/artifact_uri_query.md`
      - `wit_sections/attachments.md`
      - `wit_sections/classification_nodes.md`
      - `wit_sections/comment_reactions_engaged_users.md`
      - `wit_sections/comments.md`
      - `wit_sections/comments_reactions.md`
      - `wit_sections/comments_versions.md`
      - `wit_sections/list_queries_root_folders.md`
      - `wit_sections/recycle_bin.md`
      - `wit_sections/reporting_work_item_links.md`
      - `wit_sections/reporting_work_item_revisions.md`
      - `wit_sections/revisions.md`
      - `wit_sections/search_queries.md`
      - `wit_sections/send_mail.md`
      - `wit_sections/update_query_folder_rename_undelete.md`
      - `wit_sections/wiql.md`
      - `wit_sections/work_item_icons.md`
      - `wit_sections/work_item_relation_types.md`
      - `wit_sections/work_item_revisions_discussions.md`
      - `wit_sections/work_item_transitions.md`
      - `wit_sections/work_item_type_categories.md`
      - `wit_sections/work_item_type_states.md`
      - `wit_sections/work_item_types.md`
      - `wit_sections/work_item_types_field.md`
      - `wit_sections/work_items.md`
    - Si este archivo referencia otras subcarpetas (p. ej., `workitemtrackingprocess/`), también deben procesarse todas sus subsecciones (inventario abajo en el punto 12).
11) `api_doc/work.md`
    - Seguir y documentar TODAS las subsecciones referenciadas en `work/` (lista exacta actual):
      - `work/backlogconfiguration.md`
      - `work/backlogs.md`
      - `work/boardcolumns.md`
      - `work/boardparents.md`
      - `work/boardrows.md`
      - `work/boards.md`
      - `work/boardusersettings.md`
      - `work/capacities.md`
      - `work/cardrulesettings.md`
      - `work/cardsettings.md`
      - `work/chartimages.md`
      - `work/charts.md`
      - `work/columns.md`
      - `work/deliverytimeline.md`
      - `work/iterationcapacities.md`
      - `work/iterations.md`
      - `work/plans.md`
      - `work/predefined-queries.md`
      - `work/processconfiguration.md`
      - `work/rows.md`
      - `work/taskboard-columns.md`
      - `work/taskboard-work-items.md`
      - `work/teamdaysoff.md`
      - `work/teamfieldvalues.md`
      - `work/teamsettings.md`
      - `work/workitemsorder.md`
12) Subcarpetas adicionales referenciadas explícitamente por cualquiera de los archivos anteriores
    - `workitemtrackingprocess/` (inventario actual; procesar todas sus subsecciones existentes cuando sea referenciada por el archivo raíz correspondiente):
      - `workitemtrackingprocess/behaviors.md`
      - `workitemtrackingprocess/controls.md`
      - `workitemtrackingprocess/fields.md`
      - `workitemtrackingprocess/groups.md`
      - `workitemtrackingprocess/layout.md`
      - `workitemtrackingprocess/lists.md`
      - `workitemtrackingprocess/pages.md`
      - `workitemtrackingprocess/processes.md`
      - `workitemtrackingprocess/progreso.md`
      - `workitemtrackingprocess/rules.md`
      - `workitemtrackingprocess/states.md`
      - `workitemtrackingprocess/system-controls.md`
      - `workitemtrackingprocess/work-item-types-behaviors.md`
      - `workitemtrackingprocess/work-item-types.md`
      - `workitemtrackingprocess/workitemtrackingprogress.md`

Nota: Mantener esta lista sincronizada con el contenido real de `api_doc/`. Si se agregan o eliminan archivos en `api_doc/`, actualizar el orden y el detalle aquí. No se avanza al siguiente archivo hasta completar el archivo actual y todas sus subsecciones referenciadas.

---
Fin del documento.

## 25. Scripts de validación (cURL) reutilizables
Para cada API documentada, antes de implementar la herramienta MCP se debe crear un script cURL que:
- Resida en `scripts/curl/<area>/` (ej.: `scripts/curl/accounts/`).
- Incluya el archivo común `scripts/curl/_env.sh` para obtener variables y funciones.
- Reciba parámetros por flags o argumentos posicionales cuando aplique (IDs, filtros), y construya la URL correctamente.
- Use autenticación básica con `AZURE_DEVOPS_PAT` y base URLs apropiadas:
  - `https://dev.azure.com/${AZURE_DEVOPS_ORGANIZATION}` para APIs de proyecto/organización.
  - `https://app.vssps.visualstudio.com` para Accounts/Profiles.
- Imprima respuesta JSON (formateable con `jq` si está disponible).

Archivo común (`scripts/curl/_env.sh`):
- Define y valida variables requeridas:
  - `AZURE_DEVOPS_ORGANIZATION` y `AZURE_DEVOPS_PAT` (obligatorias)
  - `AZURE_DEVOPS_API_VERSION` (por defecto `7.2-preview.1`)
  - `AZURE_DEVOPS_VSSPS_API_VERSION` (por defecto `7.1`)
- Expone helpers:
  - `curl_json <URL>`: realiza GET con headers JSON y auth.
  - Variables `DEVOPS_BASE` y `VSSPS_BASE` para construir URLs.

Criterio de Hecho adicional por API:
- [ ] Script(s) cURL creados y probados sin error 4xx/5xx con datos válidos.
- [ ] Documentación actualizada con ejemplo cURL real usado por el script.
- [ ] La herramienta MCP implementa la misma ruta y parámetros que el script validado.

Ejemplo (Accounts):
- `scripts/curl/accounts/list_accounts.sh`: lista cuentas con filtros opcionales `ownerId`, `memberId`, `properties`.
- `scripts/curl/accounts/get_account.sh <accountId>`: obtiene detalle de una cuenta por GUID.
