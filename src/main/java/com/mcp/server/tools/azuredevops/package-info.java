/**
 * Cliente Azure DevOps para el protocolo MCP (Model Context Protocol).
 * 
 * <p>Este paquete implementa un cliente completo para la API REST de Azure DevOps
 * que permite interactuar con work items, proyectos, equipos e iteraciones a través
 * de herramientas MCP. Proporciona una interfaz natural para desarrolladores que
 * necesitan consultar, crear y actualizar elementos de trabajo en Azure DevOps.
 * 
 * <h2>Funcionalidades Principales:</h2>
 * <ul>
 *   <li><strong>Work Items</strong>: Consulta, creación y actualización de elementos de trabajo</li>
 *   <li><strong>WIQL Queries</strong>: Ejecución de consultas personalizadas con Work Item Query Language</li>
 *   <li><strong>Proyectos y Equipos</strong>: Listado de proyectos y equipos disponibles</li>
 *   <li><strong>Iteraciones</strong>: Gestión de sprints y cadencia de entregas</li>
 *   <li><strong>Jerarquías</strong>: Creación de relaciones padre-hijo entre work items</li>
 * </ul>
 * 
 * <h2>Herramientas MCP Disponibles:</h2>
 * <dl>
 *   <dt><strong>azuredevops_list_projects</strong></dt>
 *   <dd>Lista todos los proyectos disponibles en la organización</dd>
 *   
 *   <dt><strong>azuredevops_list_teams</strong></dt>
 *   <dd>Lista equipos dentro de un proyecto específico</dd>
 *   
 *   <dt><strong>azuredevops_query_workitems</strong></dt>
 *   <dd>Ejecuta consultas WIQL para buscar work items</dd>
 *   
 *   <dt><strong>azuredevops_get_workitem</strong></dt>
 *   <dd>Obtiene detalles de un work item específico por ID</dd>
 *   
 *   <dt><strong>azuredevops_create_workitem</strong></dt>
 *   <dd>Crea un nuevo work item (Task, User Story, Bug, etc.)</dd>
 *   
 *   <dt><strong>azuredevops_update_workitem</strong></dt>
 *   <dd>Actualiza campos de un work item existente</dd>
 *   
 *   <dt><strong>azuredevops_list_iterations</strong></dt>
 *   <dd>Lista iteraciones (sprints) de un equipo</dd>
 *   
 *   <dt><strong>azuredevops_get_assigned_work</strong></dt>
 *   <dd>Obtiene work items asignados al usuario actual</dd>
 * </dl>
 * 
 * <h2>Configuración de Autenticación:</h2>
 * <p>El cliente utiliza Personal Access Token (PAT) para autenticación:</p>
 * <pre>{@code
 * # Variables de entorno
 * AZURE_DEVOPS_ORGANIZATION=mi-organizacion
 * AZURE_DEVOPS_PAT=mi-token-personal
 * 
 * # O en application.yml
 * azure:
 *   devops:
 *     organization: mi-organizacion
 *     pat: mi-token-personal
 *     apiVersion: "7.1"
 * }</pre>
 * 
 * <h2>Ejemplo de Uso - Consultar Trabajo Asignado:</h2>
 * <pre>{@code
 * // JSON-RPC request
 * {
 *   "jsonrpc": "2.0",
 *   "id": "1",
 *   "method": "tools/call",
 *   "params": {
 *     "name": "azuredevops_get_assigned_work",
 *     "arguments": {
 *       "project": "MiProyecto",
 *       "includeCompleted": false
 *     }
 *   }
 * }
 * 
 * // Response
 * {
 *   "jsonrpc": "2.0",
 *   "id": "1",
 *   "result": {
 *     "content": [
 *       {
 *         "type": "text",
 *         "text": "Work Items asignados:\n1. [Task #123] Implementar API - Estado: Active\n2. [Bug #124] Corregir validación - Estado: New"
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 * 
 * <h2>Ejemplo de Uso - Crear Work Item:</h2>
 * <pre>{@code
 * // JSON-RPC request
 * {
 *   "jsonrpc": "2.0",
 *   "id": "2", 
 *   "method": "tools/call",
 *   "params": {
 *     "name": "azuredevops_create_workitem",
 *     "arguments": {
 *       "project": "MiProyecto",
 *       "type": "Task",
 *       "title": "Implementar nueva funcionalidad",
 *       "description": "Detalles de la tarea...",
 *       "assignedTo": "usuario@empresa.com",
 *       "iterationPath": "MiProyecto\\Sprint 5",
 *       "parentId": 100
 *     }
 *   }
 * }
 * }</pre>
 * 
 * <h2>Arquitectura del Cliente:</h2>
 * <pre>
 * ┌─────────────────────────┐    ┌─────────────────────────┐
 * │   MCP Tools             │───▶│   Azure DevOps Client   │
 * │   (azuredevops_*)       │    │   (REST API)            │
 * └─────────────────────────┘    └─────────────────────────┘
 *              │                              │
 *              ▼                              ▼
 * ┌─────────────────────────┐    ┌─────────────────────────┐
 * │   Model Classes         │    │   HTTP Client           │
 * │   (WorkItem, Project)   │    │   (Spring WebClient)    │
 * └─────────────────────────┘    └─────────────────────────┘
 * </pre>
 * 
 * <h2>Casos de Uso Soportados:</h2>
 * <h3>Planificación Diaria:</h3>
 * <ul>
 *   <li>Consultar trabajo asignado al usuario</li>
 *   <li>Revisar estado de tareas pendientes</li>
 *   <li>Priorizar trabajo según iteración actual</li>
 * </ul>
 * 
 * <h3>Gestión de Work Items:</h3>
 * <ul>
 *   <li>Crear nuevas tareas a partir de historias</li>
 *   <li>Actualizar progreso y estado</li>
 *   <li>Establecer relaciones jerárquicas</li>
 * </ul>
 * 
 * <h3>Análisis y Reporting:</h3>
 * <ul>
 *   <li>Consultas WIQL personalizadas</li>
 *   <li>Análisis de cadencia de entregas</li>
 *   <li>Métricas de velocidad del equipo</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.mcp.server.tools.base.McpTool
 * @see <a href="https://docs.microsoft.com/en-us/rest/api/azure/devops/">Azure DevOps REST API</a>
 * @see <a href="https://docs.microsoft.com/en-us/azure/devops/boards/queries/wiql-syntax">WIQL Syntax Reference</a>
 */
package com.mcp.server.tools.azuredevops;
