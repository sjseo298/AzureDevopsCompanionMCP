package com.mcp.server.tools.help;

import com.mcp.server.tools.base.McpTool;
import com.mcp.server.protocol.types.Tool;
import com.mcp.server.config.OrganizationContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Herramienta de ayuda del servidor MCP Azure DevOps.
 * 
 * <p>Proporciona información comprensiva sobre las capacidades del servidor,
 * adaptada dinámicamente a la estructura organizacional descubierta.
 * 
 * <p>Esta herramienta incluye:
 * <ul>
 *   <li>Descripción general del servidor y sus capacidades</li>
 *   <li>Lista completa de herramientas disponibles</li>
 *   <li>Contexto organizacional dinámico (jerarquía, dominios, nomenclatura)</li>
 *   <li>Ejemplos de uso para consultas básicas y avanzadas</li>
 *   <li>Mejores prácticas para el uso efectivo</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class HelpTool implements McpTool {
    
    private static final String TOOL_NAME = "get_help";
    private static final String DESCRIPTION = "Obtiene información completa de ayuda sobre el servidor MCP Azure DevOps, incluyendo contexto organizacional dinámico";
    
    private final OrganizationContextService organizationContextService;
    
    @Autowired
    public HelpTool(OrganizationContextService organizationContextService) {
        this.organizationContextService = organizationContextService;
    }
    
    @Override
    public Tool getToolDefinition() {
        return Tool.builder()
                .name(TOOL_NAME)
                .description(DESCRIPTION)
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "section", Map.of(
                            "type", "string",
                            "description", "Sección específica de ayuda (opcional): 'overview', 'tools', 'organization_context', 'examples', 'best_practices'",
                            "enum", List.of("overview", "tools", "organization_context", "examples", "best_practices")
                        )
                    ),
                    "required", List.of(),
                    "additionalProperties", false
                ))
                .build();
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        String section = arguments != null ? (String) arguments.get("section") : null;
        
        StringBuilder helpText = new StringBuilder();
        
        if (section == null) {
            // Mostrar toda la ayuda
            helpText.append(getOverview()).append("\n\n")
                   .append(getToolsHelp()).append("\n\n")
                   .append(getOrganizationContext()).append("\n\n")
                   .append(getExamples()).append("\n\n")
                   .append(getBestPractices());
        } else {
            // Mostrar sección específica
            switch (section) {
                case "overview":
                    helpText.append(getOverview());
                    break;
                case "tools":
                    helpText.append(getToolsHelp());
                    break;
                case "organization_context":
                    helpText.append(getOrganizationContext());
                    break;
                case "examples":
                    helpText.append(getExamples());
                    break;
                case "best_practices":
                    helpText.append(getBestPractices());
                    break;
                default:
                    throw new IllegalArgumentException("Sección no válida: " + section);
            }
        }
        
        // Crear respuesta en formato MCP
        return Map.of(
            "content", List.of(
                Map.of(
                    "type", "text",
                    "text", helpText.toString()
                )
            ),
            "isError", false
        );
    }
    
    private String getOverview() {
        Map<String, Object> orgConfig = organizationContextService.getOrganizationConfig();
        Map<String, Object> discoveredConfig = organizationContextService.getDiscoveredConfig();
        
        String organizationName = getOrganizationDisplayName(orgConfig, discoveredConfig);
        String organizationDescription = getOrganizationDescription(orgConfig);
        
        return String.format("""
# 🚀 Azure DevOps MCP Server - Guía Completa

## 📋 Descripción General
Servidor MCP (Model Context Protocol) especializado para Azure DevOps implementado con Spring Boot 3.3.2 y Java 21. 
Proporciona acceso completo a work items, proyectos, equipos e iteraciones a través de WebSocket y REST API.
%s

## ✨ Características Principales
- 🌐 **Protocol MCP 2024-11-05**: Implementación completa del protocolo estándar
- 🔄 **WebSocket Communication**: Comunicación en tiempo real bidireccional
- 🌍 **REST API**: Endpoints HTTP para integración fácil y testing
- 🔗 **Visual Studio Code**: Configuración automática para VS Code
- 🤖 **GitHub Copilot**: Integración nativa con chat de Copilot
- ☕ **Java 21 LTS**: Características modernas del lenguaje y rendimiento optimizado
- 🍃 **Spring Boot 3.3.2**: Framework robusto con autoconfiguración

## ✅ Funcionalidades Azure DevOps
- 📋 **Gestión Completa de Work Items**: Crear, actualizar, consultar y analizar work items
- 🔍 **Query Avanzado**: Ejecutar consultas WIQL personalizadas con macros
- 📊 **Análisis de Proyectos**: Ver estructura organizacional y equipos
- 🔄 **Iteraciones y Sprints**: Gestión de ciclos de desarrollo ágil
- 🏗️ **Tipos de Work Items**: Soporte para historias, tareas, bugs, features y épicas
- 🎯 **Contexto Organizacional**: Configuración adaptada dinámicamente a la organización

## 🔧 Tecnologías y Stack
- **Java**: 21 LTS con características modernas
- **Spring Boot**: 3.3.2 con autoconfiguración
- **Azure DevOps API**: v7.1 con JSON Patch (RFC 6902)
- **WebSocket**: Comunicación en tiempo real
- **JSON-RPC**: Protocolo de comunicación MCP
- **Maven/Gradle**: Gestión de dependencias y build
- **JUnit 5**: Testing unitario y de integración

## 🌐 Protocolo y Configuración
- **Protocolo MCP**: 2024-11-05 (JSON-RPC 2.0)
- **Azure DevOps API**: v7.1 REST API
- **Autenticación**: Personal Access Token (PAT) con scope vso.work_write
- **Organización**: %s
- **Content-Type**: application/json-patch+json (JSON Patch RFC 6902)
- **Formato**: Array de operaciones JSON Patch para create/update""", 
                organizationDescription, getAzureOrganizationName(orgConfig, discoveredConfig));
    }
    
    private String getOrganizationDisplayName(Map<String, Object> orgConfig, Map<String, Object> discoveredConfig) {
        if (orgConfig.containsKey("organization")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> org = (Map<String, Object>) orgConfig.get("organization");
            Object displayName = org.get("displayName");
            if (displayName != null) {
                return displayName.toString();
            }
        }
        
        Object orgName = discoveredConfig.get("organizationName");
        return orgName != null ? orgName.toString() : "Azure DevOps";
    }
    
    private String getOrganizationDescription(Map<String, Object> orgConfig) {
        if (orgConfig.containsKey("organization")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> org = (Map<String, Object>) orgConfig.get("organization");
            Object description = org.get("description");
            if (description != null) {
                String desc = description.toString();
                return "Optimizado específicamente para " + desc.toLowerCase() + ".";
            }
        }
        return "Configuración adaptada dinámicamente a la estructura organizacional.";
    }
    
    private String getAzureOrganizationName(Map<String, Object> orgConfig, Map<String, Object> discoveredConfig) {
        if (orgConfig.containsKey("organization")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> org = (Map<String, Object>) orgConfig.get("organization");
            @SuppressWarnings("unchecked")
            Map<String, Object> azure = (Map<String, Object>) org.get("azure");
            if (azure != null) {
                Object azureOrg = azure.get("organization");
                if (azureOrg != null) {
                    return azureOrg.toString();
                }
            }
        }
        
        Object orgName = discoveredConfig.get("organizationName");
        return orgName != null ? orgName.toString() : "azure-devops";
    }
    
    private String getToolsHelp() {
        return """
# 🛠️ Herramientas Disponibles (13 herramientas)

## 📋 Gestión de Proyectos y Organizacional
- **list_projects**: Lista todos los proyectos disponibles en la organización
  - Retorna: ID, nombre, descripción, estado, visibilidad
  - Útil para: Explorar estructura organizacional
  
- **list_teams**: Lista equipos de un proyecto específico
  - Parámetros: project (nombre o ID del proyecto)
  - Retorna: ID, nombre, descripción del equipo
  - Útil para: Identificar equipos por proyecto

- **list_iterations**: Lista iteraciones/sprints de un equipo con análisis de cadencia
  - Parámetros: project, team, timeFrame (current/past/future)
  - Retorna: Fechas, estado, análisis de cadencia ágil
  - Útil para: Planificación y seguimiento de sprints

## 📝 Gestión de Work Items
- **get_workitem**: Obtiene detalles completos de un work item por ID
  - Parámetros: project, workItemId, fields (opcional), expand (opcional)
  - Retorna: Todos los campos del work item incluyendo custom fields
  - Útil para: Inspección detallada de work items

- **create_workitem**: Crea nuevos work items con JSON Patch API
  - Soporta: Historia, Historia técnica, Tarea, Subtarea, Bug, Épica, etc.
  - Campos: Todos los campos estándar y personalizados de la organización
  - Relaciones: Soporte completo para jerarquías padre-hijo
  - Validación: Campos obligatorios por tipo según configuración organizacional
  
- **update_workitem**: Actualiza work items existentes
  - Operaciones: Cambio de estado, asignación, campos personalizados
  - Formato: JSON Patch operations (RFC 6902)
  - Control de concurrencia: Revisión opcional para evitar conflictos
  - 🛡️ **SEGURIDAD**: Validación automática para prevenir sobreescritura accidental

- **azuredevops_add_comment**: ✨ **NUEVA** - Agrega comentarios de forma SEGURA
  - Función: Solo agrega comentarios a la discusión sin riesgo de sobreescritura
  - Parámetros: project, workItemId, comment
  - Seguridad: NO puede sobreescribir la descripción original
  - Recomendado: Use esta herramienta en lugar de update_workitem para comentarios

- **delete_workitem**: Elimina work items de Azure DevOps
  - Modos: Papelera de reciclaje (por defecto) o eliminación permanente
  - Seguridad: Confirmación obligatoria para eliminación permanente
  - Parámetros: project, workItemId, destroy (opcional), confirmDestroy (requerido si destroy=true)
  - ⚠️ ADVERTENCIA: destroy=true es IRREVERSIBLE

- **get_assigned_work**: Obtiene work items asignados al usuario actual
  - Filtros: Por estado, tipo, iteración
  - Agrupación: Por state, type, iteration
  - Útil para: Planificación diaria y seguimiento personal

## 🔍 Consultas y Búsquedas Avanzadas
- **query_workitems**: Ejecuta consultas WIQL (Work Item Query Language)
  - Macros soportadas: @Me, @Today, @CurrentIteration, @Project
  - Operaciones: SELECT, WHERE, GROUP BY, ORDER BY
  - Límites: Hasta 50 resultados por defecto (configurable)
  - Incluye detalles completos de work items automáticamente

- **get_workitem_types**: Obtiene todos los tipos de work items de un proyecto
  - Información: Nombre, descripción, campos disponibles
  - Útil para: Validar tipos antes de crear work items

## 🛠️ Utilidades
- **generate_uuid**: Genera UUIDs únicos para identificadores
  - Formato: UUID estándar java.util.UUID
  - Útil para: Crear identificadores únicos en aplicaciones

- **get_help**: Muestra información de ayuda completa (esta herramienta)
  - Secciones: overview, tools, organization_context, examples, best_practices
  - Contexto: Documentación específica adaptada a la organización

## 🛡️ Características de Seguridad (NUEVO)
- **Validación Automática**: Prevención de sobreescritura accidental de contenido
- **Advertencias Proactivas**: Alertas cuando se intenta modificar contenido existente
- **Registro de Auditoría**: Logging de operaciones de riesgo para revisión
- **Herramientas Especializadas**: azuredevops_add_comment para comentarios seguros
- **Recomendaciones Inteligentes**: Sugerencias de mejores prácticas en tiempo real

## 📊 Características Especiales
- **JSON Patch Support**: Todas las operaciones de creación/actualización usan RFC 6902
- **Custom Fields**: Soporte completo para campos personalizados organizacionales
- **Work Item Types**: Tipos personalizados según configuración de la organización
- **Error Handling**: Manejo detallado de errores específicos de Azure DevOps
- **Validation**: Validación de campos obligatorios por tipo de work item
- **Safe Deletion**: Eliminación con papelera de reciclaje y confirmación para eliminación permanente""";
    }
    
    private String getOrganizationContext() {
        Map<String, Object> orgConfig = organizationContextService.getOrganizationConfig();
        Map<String, Object> discoveredConfig = organizationContextService.getDiscoveredConfig();
        List<Map<String, Object>> discoveredProjects = organizationContextService.getDiscoveredProjects();
        
        String organizationName = getOrganizationDisplayName(orgConfig, discoveredConfig);
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("# 🏢 Contexto Organizacional - ").append(organizationName).append("\n\n");
        
        // Información de proyectos
        if (!discoveredProjects.isEmpty()) {
            contextBuilder.append("## 📁 Proyectos Disponibles\n\n");
            for (Map<String, Object> project : discoveredProjects) {
                String projectName = (String) project.get("name");
                String description = (String) project.get("description");
                String azureProjectId = (String) project.get("azureProjectId");
                
                contextBuilder.append(String.format("### %s\n", projectName));
                if (description != null) {
                    contextBuilder.append(String.format("- **Descripción**: %s\n", description));
                }
                if (azureProjectId != null) {
                    contextBuilder.append(String.format("- **ID Azure**: %s\n", azureProjectId));
                }
                
                // Información de equipos
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> teams = (List<Map<String, Object>>) project.get("teams");
                if (teams != null && !teams.isEmpty()) {
                    contextBuilder.append("- **Equipos activos**: ").append(teams.size()).append("\n");
                    
                    // Agrupar equipos por dominio si tienen prefijo
                    Map<String, Long> domainCounts = teams.stream()
                        .map(team -> (String) team.get("name"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(
                            name -> {
                                String[] parts = name.split("-");
                                return parts.length >= 2 ? parts[0] + "-" + parts[1] : "otros";
                            },
                            Collectors.counting()
                        ));
                    
                    if (!domainCounts.isEmpty()) {
                        contextBuilder.append("- **Dominios principales**:\n");
                        domainCounts.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(5)
                            .forEach(entry -> 
                                contextBuilder.append(String.format("  - %s: %d equipos\n", entry.getKey(), entry.getValue()))
                            );
                    }
                }
                contextBuilder.append("\n");
            }
        }
        
        // Información de tipos de work items desde configuración
        Map<String, Object> fieldMappingConfig = organizationContextService.getFieldMappingConfig();
        if (fieldMappingConfig.containsKey("workItemTypes")) {
            contextBuilder.append("## 🔧 Tipos de Work Items Disponibles\n\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> workItemTypes = (Map<String, Object>) fieldMappingConfig.get("workItemTypes");
            
            workItemTypes.forEach((type, config) -> {
                contextBuilder.append(String.format("### %s\n", type));
                if (config instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typeConfig = (Map<String, Object>) config;
                    
                    Object description = typeConfig.get("description");
                    if (description != null) {
                        contextBuilder.append(String.format("- **Propósito**: %s\n", description));
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<String> requiredFields = (List<String>) typeConfig.get("requiredFields");
                    if (requiredFields != null && !requiredFields.isEmpty()) {
                        contextBuilder.append("- **Campos obligatorios**: ");
                        contextBuilder.append(String.join(", ", requiredFields));
                        contextBuilder.append("\n");
                    }
                }
                contextBuilder.append("\n");
            });
        }
        
        // Información de campos personalizados
        if (fieldMappingConfig.containsKey("customFields")) {
            contextBuilder.append("## ⚙️ Campos Personalizados\n\n");
            @SuppressWarnings("unchecked")
            Map<String, Object> customFields = (Map<String, Object>) fieldMappingConfig.get("customFields");
            
            customFields.forEach((fieldType, fields) -> {
                if (fields instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> fieldList = (List<Map<String, Object>>) fields;
                    
                    if (!fieldList.isEmpty()) {
                        contextBuilder.append(String.format("### %s\n", fieldType));
                        fieldList.forEach(field -> {
                            String name = (String) field.get("name");
                            String referenceName = (String) field.get("referenceName");
                            String type = (String) field.get("type");
                            
                            if (name != null) {
                                contextBuilder.append(String.format("- **%s**", name));
                                if (type != null) {
                                    contextBuilder.append(String.format(" (%s)", type));
                                }
                                if (referenceName != null) {
                                    contextBuilder.append(String.format(" - `%s`", referenceName));
                                }
                                contextBuilder.append("\n");
                            }
                        });
                        contextBuilder.append("\n");
                    }
                }
            });
        }
        
        // Reglas de negocio si están disponibles
        Map<String, Object> businessRules = organizationContextService.getBusinessRulesConfig();
        if (!businessRules.isEmpty()) {
            contextBuilder.append("## 📋 Reglas de Negocio\n\n");
            businessRules.forEach((rule, value) -> {
                contextBuilder.append(String.format("- **%s**: %s\n", rule, value.toString()));
            });
            contextBuilder.append("\n");
        }
        
        // Información de configuración organizacional
        if (orgConfig.containsKey("organization")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> org = (Map<String, Object>) orgConfig.get("organization");
            @SuppressWarnings("unchecked")
            Map<String, Object> azure = (Map<String, Object>) org.get("azure");
            
            if (azure != null) {
                contextBuilder.append("## 🔗 Configuración Azure DevOps\n\n");
                Object baseUrl = azure.get("baseUrl");
                Object defaultProject = azure.get("defaultProject");
                
                if (baseUrl != null) {
                    contextBuilder.append(String.format("- **URL Base**: %s\n", baseUrl));
                }
                if (defaultProject != null) {
                    contextBuilder.append(String.format("- **Proyecto por defecto**: %s\n", defaultProject));
                }
            }
        }
        
        return contextBuilder.toString();
    }
    
    private String getExamples() {
        Map<String, Object> orgConfig = organizationContextService.getOrganizationConfig();
        List<Map<String, Object>> discoveredProjects = organizationContextService.getDiscoveredProjects();
        
        // Obtener el primer proyecto disponible como ejemplo
        String exampleProject = "MiProyecto";
        String exampleTeam = "mi-equipo";
        String exampleAreaPath = "MiProyecto";
        String exampleIterationPath = "MiProyecto\\Sprint 1";
        
        if (!discoveredProjects.isEmpty()) {
            Map<String, Object> firstProject = discoveredProjects.get(0);
            exampleProject = (String) firstProject.get("name");
            exampleAreaPath = exampleProject;
            exampleIterationPath = exampleProject + "\\Sprint 1";
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> teams = (List<Map<String, Object>>) firstProject.get("teams");
            if (teams != null && !teams.isEmpty()) {
                exampleTeam = (String) teams.get(0).get("name");
            }
        }
        
        // Obtener configuración organizacional
        String defaultProject = exampleProject;
        if (orgConfig.containsKey("organization")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> org = (Map<String, Object>) orgConfig.get("organization");
            @SuppressWarnings("unchecked")
            Map<String, Object> azure = (Map<String, Object>) org.get("azure");
            if (azure != null && azure.get("defaultProject") != null) {
                defaultProject = azure.get("defaultProject").toString();
            }
        }
        
        return String.format("""
# 📚 Ejemplos de Uso

## 🔍 Consultas Básicas
```
# Listar todos los proyectos
list_projects

# Ver equipos del proyecto principal
list_teams(project: "%s")

# Ver work item específico
get_workitem(project: "%s", workItemId: 12345)

# Obtener mis tareas asignadas
get_assigned_work(project: "%s")

# Ver tipos de work items disponibles
get_workitem_types(project: "%s")

# Iteraciones del equipo 
list_iterations(project: "%s", team: "%s")
```

## 📝 Creación de Work Items

### Crear Work Item Básico:
```json
create_workitem({
  "project": "%s",
  "type": "Task",
  "title": "Implementar nueva funcionalidad",
  "description": "Descripción detallada de la funcionalidad",
  "assignedTo": "usuario@organizacion.com",
  "iterationPath": "%s",
  "areaPath": "%s"
})
```

### Crear Work Item con Relación Padre-Hijo:
```json
create_workitem({
  "project": "%s", 
  "type": "Task",
  "title": "Subtarea de desarrollo",
  "assignedTo": "desarrollador@organizacion.com",
  "remainingWork": 8,
  "parentId": 12345
})
```

## 🔍 Consultas WIQL Avanzadas

### Buscar Work Items Asignados:
```
query_workitems({
  "project": "%s",
  "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItems WHERE [System.AssignedTo] = @Me AND [System.State] <> 'Closed'"
})
```

### Buscar por Tipo y Estado:
```
query_workitems({
  "project": "%s",
  "query": "SELECT [System.Id], [System.Title], [System.AssignedTo] FROM WorkItems WHERE [System.WorkItemType] = 'Bug' AND [System.State] = 'Active'",
  "maxResults": 20
})
```

### Consulta por Iteración Actual:
```
query_workitems({
  "project": "%s",
  "query": "SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.IterationPath] = @CurrentIteration",
  "includeDetails": true
})
```

## 🔄 Actualización de Work Items

### Cambiar Estado:
```json
update_workitem({
  "project": "%s",
  "workItemId": 12345,
  "state": "Active"
})
```

### Actualizar Múltiples Campos:
```json
update_workitem({
  "project": "%s",
  "workItemId": 12345,
  "title": "Nuevo título actualizado",
  "assignedTo": "nuevo-usuario@organizacion.com",
  "remainingWork": 4,
  "state": "In Progress"
})
```

## 📊 Análisis y Reportes

### Trabajo Asignado con Agrupación:
```
get_assigned_work({
  "project": "%s",
  "groupBy": "state",
  "includeCompleted": false
})
```

### Análisis de Iteraciones:
```
list_iterations({
  "project": "%s",
  "team": "%s",
  "timeFrame": "current",
  "includeDetails": true
})
```

## 🛠️ Utilidades

### Generar UUID:
```
generate_uuid()
```

### Ayuda Específica:
```
get_help(section: "organization_context")
```

## ⚠️ Mejores Prácticas

1. **Siempre especifica el proyecto**: Todas las operaciones requieren el parámetro project
2. **Usa @Me en consultas**: Para filtrar work items asignados al usuario actual
3. **Limita resultados de consultas**: Usa maxResults para evitar timeouts
4. **Valida campos obligatorios**: Verifica get_workitem_types antes de crear work items
5. **Usa parentId para jerarquías**: Crear relaciones padre-hijo al crear work items""", 
            exampleProject, exampleProject, exampleProject, exampleProject, 
            exampleProject, exampleTeam, exampleProject, exampleIterationPath, 
            exampleAreaPath, exampleProject, exampleProject, exampleProject, 
            exampleProject, exampleProject, exampleProject, exampleProject, 
            exampleProject, exampleTeam);
    }
    
    private String getBestPractices() {
        return """
# 🎯 Mejores Prácticas

## 🛡️ Seguridad y Prevención de Errores (NUEVO)
- **Use azuredevops_add_comment para comentarios**: Evita sobreescritura accidental de contenido
- **Lea advertencias del sistema**: El sistema alerta sobre operaciones de riesgo automáticamente
- **Verifique antes de actualizar**: Siempre revise el contenido actual antes de actualizaciones masivas
- **Use 'comment' en lugar de 'description'**: Para agregar información sin sobreescribir
- **Respete las advertencias de seguridad**: El sistema registra operaciones de riesgo para auditoría
- **Confirme operaciones destructivas**: delete_workitem con destroy=true es IRREVERSIBLE

## ✅ Consultas Eficientes y Rendimiento
- **Usar WIQL para análisis complejos**: En lugar de múltiples llamadas individuales
- **Limitar resultados**: Usar maxResults para evitar timeouts en proyectos grandes  
- **Utilizar macros dinámicas**: @Me, @Today, @CurrentIteration para consultas reutilizables
- **Filtrar por estado**: Incluir [System.State] para excluir work items cerrados
- **Campos específicos**: Usar SELECT con campos específicos en lugar de SELECT *
- **Paginar resultados**: Para conjuntos de datos grandes usar maxResults apropiados

## 🏗️ Estructura Organizacional
- **Respetar jerarquía**: Proyecto > Épica > Historia > Tarea > Subtarea
- **Nomenclatura estándar**: Usar prefijos consistentes según dominios organizacionales
- **Contexto temporal**: Considerar sprints e iteraciones en planificación
- **AreaPath por dominio**: Usar estructura de área según dominio de negocio
- **Estados consistentes**: Seguir flujo definido por la organización

## 📝 Gestión de Work Items
- **Tipos organizacionales**: Usar tipos definidos por la organización
- **Campos obligatorios**: Validar todos los campos requeridos por tipo antes de crear
- **IterationPath completo**: Incluir ruta completa al crear work items
- **AreaPath apropiado**: Asignar según dominio de negocio correspondiente
- **Tags descriptivos**: Usar para categorización adicional
- **Emails organizacionales**: Mantener consistencia en assignedTo

## 🔧 Creación y Actualización
- **JSON Patch obligatorio**: Usar format RFC 6902 para create/update
- **Content-Type correcto**: Siempre usar 'application/json-patch+json'
- **Validación por tipo**: Verificar campos específicos según tipo de work item
- **Relaciones padre-hijo**: Usar parentId para jerarquías automáticas
- **Campos personalizados**: Incluir campos custom según configuración organizacional
- **Control de concurrencia**: Usar revision para updates seguros

## 🚨 Prevención de Errores Comunes (NUEVO)
1. **NO use 'description' para comentarios**: Use 'comment' o azuredevops_add_comment
2. **Verifique contenido antes de actualizar**: El sistema mostrará advertencias automáticamente
3. **Lea los logs de seguridad**: Operaciones de riesgo se registran para revisión
4. **Use herramientas especializadas**: azuredevops_add_comment es más seguro para comentarios
5. **Confirme operaciones destructivas**: delete con destroy=true no se puede deshacer
6. **Respete las validaciones**: El sistema previene errores comunes automáticamente

## ⚠️ Consideraciones de Seguridad y Permisos
- **PAT con scope correcto**: Usar vso.work_write para operaciones de escritura
- **Validar permisos**: Verificar acceso a proyectos antes de operaciones
- **Datos sensibles**: No incluir información confidencial en títulos/descripciones
- **Logs seguros**: Evitar logging de tokens o información personal
- **Auditoría**: El sistema registra operaciones de riesgo automáticamente

## 🎛️ Configuración y Entorno
- **Variables de entorno**: AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT correctas
- **Timeouts apropiados**: Configurar timeouts según red corporativa
- **Cache inteligente**: Cachear información de proyectos/equipos para uso repetitivo
- **Error handling**: Implementar manejo robusto de errores específicos Azure DevOps

## 📊 Monitoreo y Troubleshooting
- **Logs detallados**: Activar DEBUG para investigación de problemas
- **Response codes**: Monitorear 400, 401, 403, 404 para identificar patrones
- **Rate limiting**: Respetar límites de Azure DevOps API
- **Network issues**: Considerar proxy/firewall corporativo en troubleshooting
- **Security logs**: Revisar logs de seguridad para detectar patrones de riesgo""";
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
