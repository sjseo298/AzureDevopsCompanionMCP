package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Herramienta para analizar work items de referencia en profundidad.
 * Obtiene información completa del work item, incluyendo tipo, padre, hijos y campos personalizados.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class WorkItemAnalyzerTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public WorkItemAnalyzerTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_analyze_workitem_reference";
    }
    
    @Override
    public String getDescription() {
        return "Analiza un work item de referencia en profundidad, incluyendo su tipo, padre, hijos, campos personalizados y valores. Útil para entender la estructura organizacional basada en un ejemplo real.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "workItemReference", Map.of(
                    "type", "string",
                    "description", "URL completa o ID del work item a analizar (ej: 'https://dev.azure.com/org/project/_workitems/edit/12345' o '12345')"
                ),
                "project", Map.of(
                    "type", "string", 
                    "description", "Nombre del proyecto (opcional si se puede extraer de la URL)"
                ),
                "includeHierarchy", Map.of(
                    "type", "boolean",
                    "description", "Si incluir análisis de jerarquía (padre/hijos). Por defecto: true"
                ),
                "includeCustomFields", Map.of(
                    "type", "boolean",
                    "description", "Si incluir análisis detallado de campos personalizados. Por defecto: true"
                )
            ),
            "required", List.of("workItemReference")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            if (!azureDevOpsClient.isConfigured()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                    ))
                );
            }
            
            String workItemReference = (String) arguments.get("workItemReference");
            String project = (String) arguments.get("project");
            Boolean includeHierarchy = (Boolean) arguments.getOrDefault("includeHierarchy", true);
            Boolean includeCustomFields = (Boolean) arguments.getOrDefault("includeCustomFields", true);
            
            if (workItemReference == null || workItemReference.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'workItemReference' es requerido"
                    )),
                    "isError", true
                );
            }
            
            // Extraer ID del work item
            Integer workItemId = extractWorkItemIdFromReference(workItemReference);
            if (workItemId == null) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "No se pudo extraer el ID del work item de la referencia: " + workItemReference
                    )),
                    "isError", true
                );
            }
            
            // Si no se proporcionó proyecto, intentar encontrarlo
            if (project == null || project.trim().isEmpty()) {
                project = findProjectForWorkItem(workItemId);
                if (project == null) {
                    return Map.of(
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", "No se pudo encontrar el proyecto para el work item #" + workItemId + ". Proporcione el parámetro 'project'."
                        )),
                        "isError", true
                    );
                }
            }
            
            // Analizar el work item
            WorkItemAnalysisResult analysis = analyzeWorkItem(project, workItemId, includeHierarchy, includeCustomFields);
            
            StringBuilder result = new StringBuilder();
            result.append("🔍 **ANÁLISIS COMPLETO DEL WORK ITEM DE REFERENCIA**\n");
            result.append("=====================================================\n\n");
            
            // Información básica
            result.append("📋 **INFORMACIÓN BÁSICA**\n");
            result.append("ID: ").append(workItemId).append("\n");
            result.append("Proyecto: ").append(project).append("\n");
            result.append("Tipo: ").append(analysis.workItemType).append("\n");
            result.append("Estado: ").append(analysis.state).append("\n");
            result.append("Título: ").append(analysis.title).append("\n");
            result.append("Área: ").append(analysis.areaPath).append("\n");
            result.append("Iteración: ").append(analysis.iterationPath).append("\n");
            if (analysis.assignedTo != null) {
                result.append("Asignado a: ").append(analysis.assignedTo).append("\n");
            }
            result.append("\n");
            
            // Análisis de jerarquía
            if (includeHierarchy && (analysis.parentInfo != null || !analysis.childrenInfo.isEmpty())) {
                result.append("🏗️ **ANÁLISIS DE JERARQUÍA**\n");
                
                if (analysis.parentInfo != null) {
                    result.append("👆 **Work Item Padre:**\n");
                    result.append("   • ID: ").append(analysis.parentInfo.get("id")).append("\n");
                    result.append("   • Tipo: ").append(analysis.parentInfo.get("workItemType")).append("\n");
                    result.append("   • Título: ").append(analysis.parentInfo.get("title")).append("\n");
                    result.append("\n");
                }
                
                if (!analysis.childrenInfo.isEmpty()) {
                    result.append("👇 **Work Items Hijos (").append(analysis.childrenInfo.size()).append("):**\n");
                    for (Map<String, Object> child : analysis.childrenInfo) {
                        result.append("   • #").append(child.get("id"))
                              .append(" (").append(child.get("workItemType")).append("): ")
                              .append(child.get("title")).append("\n");
                    }
                    result.append("\n");
                }
                
                // Patrones identificados
                result.append("🎯 **PATRONES IDENTIFICADOS:**\n");
                if (analysis.parentInfo != null) {
                    result.append("   • Relación: ").append(analysis.parentInfo.get("workItemType"))
                          .append(" → ").append(analysis.workItemType).append("\n");
                }
                if (!analysis.childrenInfo.isEmpty()) {
                    Set<String> childTypes = new HashSet<>();
                    for (Map<String, Object> child : analysis.childrenInfo) {
                        childTypes.add((String) child.get("workItemType"));
                    }
                    result.append("   • El tipo '").append(analysis.workItemType)
                          .append("' puede tener hijos: [").append(String.join(", ", childTypes)).append("]\n");
                }
                result.append("\n");
            }
            
            // Análisis de campos personalizados
            if (includeCustomFields && !analysis.customFields.isEmpty()) {
                result.append("🔧 **CAMPOS PERSONALIZADOS DETECTADOS**\n");
                for (Map.Entry<String, Object> field : analysis.customFields.entrySet()) {
                    String fieldName = field.getKey();
                    Object fieldValue = field.getValue();
                    
                    result.append("   • **").append(fieldName).append("**\n");
                    result.append("     - Valor actual: ").append(fieldValue != null ? fieldValue.toString() : "null").append("\n");
                    
                    // Intentar determinar el tipo de dato
                    String dataType = determineFieldDataType(fieldValue);
                    result.append("     - Tipo detectado: ").append(dataType).append("\n");
                    
                    // Sugerir mapeo
                    String suggestedMapping = suggestFieldMapping(fieldName, fieldValue);
                    if (suggestedMapping != null) {
                        result.append("     - Mapeo sugerido: ").append(suggestedMapping).append("\n");
                    }
                    result.append("\n");
                }
            }
            
            // Recomendaciones para configuración
            result.append("💡 **RECOMENDACIONES PARA CONFIGURACIÓN**\n");
            result.append("=====================================\n");
            
            result.append("**Para discovered-organization.yml:**\n");
            result.append("- Incluir tipo '").append(analysis.workItemType).append("' en workItemTypes\n");
            if (analysis.parentInfo != null || !analysis.childrenInfo.isEmpty()) {
                result.append("- Documentar relaciones jerárquicas encontradas\n");
            }
            result.append("\n");
            
            result.append("**Para organization-config.yml:**\n");
            result.append("- Configurar área path por defecto: ").append(analysis.areaPath).append("\n");
            result.append("- Configurar iteración por defecto: ").append(analysis.iterationPath).append("\n");
            result.append("\n");
            
            if (!analysis.customFields.isEmpty()) {
                result.append("**Para field-mappings.yml:**\n");
                for (String fieldName : analysis.customFields.keySet()) {
                    result.append("- Mapear campo personalizado: ").append(fieldName).append("\n");
                }
                result.append("\n");
            }
            
            result.append("**Para business-rules.yml:**\n");
            result.append("- Definir validaciones específicas para tipo '").append(analysis.workItemType).append("'\n");
            if (analysis.parentInfo != null || !analysis.childrenInfo.isEmpty()) {
                result.append("- Configurar reglas de jerarquía según patrones detectados\n");
            }
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                )),
                "analysisData", analysis.toMap() // Datos estructurados para uso programático
            );
            
        } catch (AzureDevOpsException e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error de Azure DevOps: " + e.getMessage()
                )),
                "isError", true
            );
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error inesperado: " + e.getMessage()
                )),
                "isError", true
            );
        }
    }
    
    /**
     * Extrae el ID del work item de una referencia (URL o ID directo)
     */
    private Integer extractWorkItemIdFromReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return null;
        }
        
        // Intentar como número directo primero
        try {
            return Integer.valueOf(reference.trim());
        } catch (NumberFormatException e) {
            // No es un número directo, intentar extraer de URL
        }
        
        // Patrones de URL comunes
        String[] patterns = {
            "/_workitems/edit/(\\d+)",  // https://dev.azure.com/org/project/_workitems/edit/12345
            "/workitems/(\\d+)",        // https://dev.azure.com/org/project/_workitems/12345  
            "workItemId=(\\d+)",        // Query parameter
            "#(\\d+)"                   // Referencia simple como #12345
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(reference);
            if (m.find()) {
                try {
                    return Integer.valueOf(m.group(1));
                } catch (NumberFormatException e) {
                    // Continuar con el siguiente patrón
                }
            }
        }
        
        return null;
    }
    
    /**
     * Encuentra el proyecto para un work item dado su ID
     */
    private String findProjectForWorkItem(Integer workItemId) {
        try {
            // Usar la API global de work items
            Map<String, Object> workItem = azureDevOpsClient.getWorkItem(workItemId);
            if (workItem != null && workItem.containsKey("fields")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = (Map<String, Object>) workItem.get("fields");
                String areaPath = (String) fields.get("System.AreaPath");
                if (areaPath != null && !areaPath.trim().isEmpty()) {
                    String[] pathParts = areaPath.split("\\\\");
                    if (pathParts.length > 0) {
                        return pathParts[0];
                    }
                }
            }
        } catch (Exception e) {
            // No se pudo encontrar automáticamente
        }
        return null;
    }
    
    /**
     * Analiza un work item en profundidad
     */
    private WorkItemAnalysisResult analyzeWorkItem(String project, Integer workItemId, 
                                                  boolean includeHierarchy, boolean includeCustomFields) {
        WorkItemAnalysisResult result = new WorkItemAnalysisResult();
        
        // Obtener work item con expansiones si se requiere jerarquía
        String expand = includeHierarchy ? "Relations" : null;
        WorkItem workItem = azureDevOpsClient.getWorkItem(project, workItemId, null, expand);
        
        if (workItem == null) {
            throw new AzureDevOpsException("Work item not found: " + workItemId);
        }
        
        // Información básica
        result.workItemId = workItemId;
        result.project = project;
        result.workItemType = workItem.getWorkItemType();
        result.state = workItem.getState();
        result.title = workItem.getTitle();
        result.areaPath = workItem.getAreaPath();
        result.iterationPath = workItem.getIterationPath();
        result.assignedTo = workItem.getAssignedToName();
        
        // Analizar campos personalizados
        if (includeCustomFields) {
            analyzeCustomFields(workItem, result);
        }
        
        // Analizar jerarquía
        if (includeHierarchy) {
            analyzeHierarchy(project, workItem, result);
        }
        
        return result;
    }
    
    /**
     * Analiza campos personalizados del work item
     */
    private void analyzeCustomFields(WorkItem workItem, WorkItemAnalysisResult result) {
        Map<String, Object> fields = workItem.fields();
        
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            // Identificar campos personalizados (no estándar de Azure DevOps)
            if (isCustomField(fieldName)) {
                result.customFields.put(fieldName, fieldValue);
            }
        }
    }
    
    /**
     * Determina si un campo es personalizado
     */
    private boolean isCustomField(String fieldName) {
        // Campos estándar de Azure DevOps
        Set<String> standardFields = Set.of(
            "System.Id", "System.Title", "System.Description", "System.State",
            "System.WorkItemType", "System.AssignedTo", "System.CreatedBy",
            "System.CreatedDate", "System.ChangedBy", "System.ChangedDate",
            "System.AreaPath", "System.IterationPath", "System.Tags",
            "System.Reason", "System.CommentCount", "System.History",
            "Microsoft.VSTS.Common.Priority", "Microsoft.VSTS.Common.StateChangeDate",
            "Microsoft.VSTS.Common.ActivatedBy", "Microsoft.VSTS.Common.ActivatedDate",
            "Microsoft.VSTS.Common.ResolvedBy", "Microsoft.VSTS.Common.ResolvedDate",
            "Microsoft.VSTS.Common.ClosedBy", "Microsoft.VSTS.Common.ClosedDate",
            "Microsoft.VSTS.Scheduling.StoryPoints", "Microsoft.VSTS.Scheduling.Effort",
            "Microsoft.VSTS.Scheduling.OriginalEstimate", "Microsoft.VSTS.Scheduling.RemainingWork",
            "Microsoft.VSTS.Scheduling.CompletedWork", "Microsoft.VSTS.Scheduling.StartDate",
            "Microsoft.VSTS.Scheduling.FinishDate", "Microsoft.VSTS.Scheduling.TargetDate",
            "Microsoft.VSTS.Scheduling.DueDate", "Microsoft.VSTS.Common.AcceptanceCriteria",
            "Microsoft.VSTS.TCM.ReproSteps", "Microsoft.VSTS.TCM.SystemInfo"
        );
        
        return !standardFields.contains(fieldName);
    }
    
    /**
     * Analiza la jerarquía del work item (padre e hijos)
     */
    private void analyzeHierarchy(String project, WorkItem workItem, WorkItemAnalysisResult result) {
        // Analizar parent usando System.Parent field si está disponible
        Map<String, Object> fields = workItem.fields();
        Object parentId = fields.get("System.Parent");
        
        if (parentId != null) {
            try {
                Integer parentWorkItemId = Integer.valueOf(parentId.toString());
                WorkItem parentWorkItem = azureDevOpsClient.getWorkItem(project, parentWorkItemId, null, null);
                if (parentWorkItem != null) {
                    result.parentInfo = Map.of(
                        "id", parentWorkItem.id(),
                        "workItemType", parentWorkItem.getWorkItemType(),
                        "title", parentWorkItem.getTitle() != null ? parentWorkItem.getTitle() : "",
                        "state", parentWorkItem.getState() != null ? parentWorkItem.getState() : ""
                    );
                }
            } catch (Exception e) {
                // Error obteniendo padre, continuar sin él
            }
        }
        
        // Buscar hijos mediante consulta WIQL
        try {
            String childrenQuery = String.format(
                "SELECT [System.Id], [System.Title], [System.WorkItemType], [System.State] " +
                "FROM WorkItems WHERE [System.Parent] = %d", workItem.id()
            );
            
            var queryResult = azureDevOpsClient.executeWiqlQuery(project, null, childrenQuery);
            if (queryResult.hasResults()) {
                List<WorkItem> children = azureDevOpsClient.getWorkItems(project, queryResult.getWorkItemIds(), 
                    List.of("System.Id", "System.Title", "System.WorkItemType", "System.State"));
                
                for (WorkItem child : children) {
                    result.childrenInfo.add(Map.of(
                        "id", child.id(),
                        "workItemType", child.getWorkItemType(),
                        "title", child.getTitle() != null ? child.getTitle() : "",
                        "state", child.getState() != null ? child.getState() : ""
                    ));
                }
            }
        } catch (Exception e) {
            // Error buscando hijos, continuar sin ellos
        }
    }
    
    /**
     * Determina el tipo de dato de un campo basado en su valor
     */
    private String determineFieldDataType(Object value) {
        if (value == null) {
            return "unknown";
        }
        
        if (value instanceof String) {
            String str = (String) value;
            if (str.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                return "datetime";
            } else if (str.matches("\\d+")) {
                return "integer";
            } else if (str.matches("\\d*\\.\\d+")) {
                return "double";
            } else if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
                return "boolean";
            } else if (str.contains("<") && str.contains(">")) {
                return "html";
            } else {
                return "string";
            }
        } else if (value instanceof Number) {
            return value instanceof Integer ? "integer" : "double";
        } else if (value instanceof Boolean) {
            return "boolean";
        }
        
        return "object";
    }
    
    /**
     * Sugiere un mapeo para un campo personalizado
     */
    private String suggestFieldMapping(String fieldName, Object value) {
        String lowerName = fieldName.toLowerCase();
        
        if (lowerName.contains("tipo") || lowerName.contains("type")) {
            return "tipoHistoria o tipoTarea";
        } else if (lowerName.contains("priority") || lowerName.contains("prioridad")) {
            return "priority";
        } else if (lowerName.contains("department") || lowerName.contains("departamento")) {
            return "departamento";
        } else if (lowerName.contains("business") || lowerName.contains("negocio")) {
            return "valorNegocio";
        } else if (lowerName.contains("risk") || lowerName.contains("riesgo")) {
            return "nivelRiesgo";
        } else if (lowerName.contains("acceptance") || lowerName.contains("criterio")) {
            return "acceptanceCriteria";
        }
        
        return null;
    }
    
    /**
     * Clase para almacenar el resultado del análisis
     */
    private static class WorkItemAnalysisResult {
        public Integer workItemId;
        public String project;
        public String workItemType;
        public String state;
        public String title;
        public String areaPath;
        public String iterationPath;
        public String assignedTo;
        public Map<String, Object> customFields = new HashMap<>();
        public Map<String, Object> parentInfo;
        public List<Map<String, Object>> childrenInfo = new ArrayList<>();
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("workItemId", workItemId);
            map.put("project", project);
            map.put("workItemType", workItemType);
            map.put("state", state);
            map.put("title", title);
            map.put("areaPath", areaPath);
            map.put("iterationPath", iterationPath);
            map.put("assignedTo", assignedTo);
            map.put("customFields", customFields);
            map.put("parentInfo", parentInfo);
            map.put("childrenInfo", childrenInfo);
            return map;
        }
    }
}
