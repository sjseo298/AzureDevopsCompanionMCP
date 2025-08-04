package com.mcp.server.utils.hierarchy;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.utils.discovery.AzureDevOpsWiqlUtility;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;
import com.mcp.server.utils.workitem.WorkItemProcessor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analizador de jerarquías de work items de Azure DevOps.
 * 
 * Esta clase se encarga de:
 * - Análisis de jerarquías padre-hijo en work items
 * - Generación de documentación de patrones de subtareas
 * - Búsqueda de work items a través de múltiples proyectos
 * - Extracción de datos jerárquicos y relaciones
 * 
 * Extraída de DiscoverOrganizationTool como parte del refactoring para mejorar
 * la cohesión y mantenibilidad del código.
 */
public class WorkItemHierarchyAnalyzer {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsWiqlUtility wiqlUtility;
    private final WorkItemProcessor workItemProcessor;
    
    public WorkItemHierarchyAnalyzer(AzureDevOpsClient azureDevOpsClient, 
                                   AzureDevOpsWiqlUtility wiqlUtility,
                                   WorkItemProcessor workItemProcessor) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.wiqlUtility = wiqlUtility;
        this.workItemProcessor = workItemProcessor;
    }
    
    /**
     * Analiza las jerarquías de work items para identificar patrones de subtareas
     * y tipos de work items hijos más utilizados.
     */
    public Map<String, Object> analyzeWorkItemHierarchies(String project) {
        Map<String, Object> hierarchyAnalysis = new HashMap<>();
        
        try {
            System.out.println("Iniciando análisis de jerarquías para proyecto: " + project);
            
            // Enfoque 1: Buscar work items recientes que podrían tener relaciones
            String recentWorkItemsQuery = "SELECT [System.Id], [System.Title], [System.WorkItemType] " +
                                        "FROM WorkItems WHERE [System.TeamProject] = @project " +
                                        "ORDER BY [System.ChangedDate] DESC";
            
            List<Integer> recentWorkItemIds = executeWIQLQuery(project, recentWorkItemsQuery);
            
            if (recentWorkItemIds.isEmpty()) {
                hierarchyAnalysis.put("message", "No se encontraron work items en el proyecto " + project);
                return hierarchyAnalysis;
            }
            
            // Mapas para análisis estadístico
            Map<String, Integer> childTypeCount = new HashMap<>();
            Map<String, Set<String>> parentChildRelations = new HashMap<>();
            Map<String, Map<String, Integer>> parentTypeToChildTypes = new HashMap<>();
            
            // Procesar una muestra representativa para evitar sobrecarga
            int maxWorkItemsToProcess = Math.min(recentWorkItemIds.size(), 50);
            List<Integer> sampleIds = recentWorkItemIds.subList(0, maxWorkItemsToProcess);
            
            System.out.println("Analizando " + maxWorkItemsToProcess + " work items de " + recentWorkItemIds.size() + " totales");
            
            // Analizar cada work item para identificar relaciones padre-hijo
            for (Integer workItemId : sampleIds) {
                try {
                    String workItemData = getWorkItemAsJson(project, workItemId);
                    
                    if (workItemData != null) {
                        String workItemType = extractFieldFromJson(workItemData, "System.WorkItemType");
                        String parentIdStr = extractFieldFromJson(workItemData, "System.Parent");
                        
                        if (workItemType != null) {
                            // Si tiene padre, entonces es un work item hijo
                            if (parentIdStr != null && !parentIdStr.isEmpty() && !parentIdStr.equals("null")) {
                                // Contar tipos de work items hijos
                                childTypeCount.merge(workItemType, 1, Integer::sum);
                                
                                try {
                                    Integer parentIdInt = Integer.valueOf(parentIdStr);
                                    String parentData = getWorkItemAsJson(project, parentIdInt);
                                    
                                    if (parentData != null) {
                                        String parentType = extractFieldFromJson(parentData, "System.WorkItemType");
                                        
                                        if (parentType != null) {
                                            // Registrar relación padre-hijo por tipo
                                            parentChildRelations.computeIfAbsent(parentType, k -> new HashSet<>()).add(workItemType);
                                            
                                            // Estadísticas detalladas por tipo de padre
                                            parentTypeToChildTypes.computeIfAbsent(parentType, k -> new HashMap<>())
                                                                 .merge(workItemType, 1, Integer::sum);
                                            
                                            System.out.println("✅ Relación encontrada: " + parentType + " (ID: " + parentIdInt + ") -> " + workItemType + " (ID: " + workItemId + ")");
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("⚠️ ID de padre inválido para work item " + workItemId + ": " + parentIdStr);
                                } catch (Exception e) {
                                    System.out.println("⚠️ No se pudo analizar padre para work item " + workItemId + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error procesando work item " + workItemId + ": " + e.getMessage());
                }
            }
            
            // Construir resultado del análisis
            hierarchyAnalysis.put("totalWorkItemsAnalyzed", maxWorkItemsToProcess);
            hierarchyAnalysis.put("totalWorkItemsAvailable", recentWorkItemIds.size());
            hierarchyAnalysis.put("childTypeDistribution", childTypeCount);
            hierarchyAnalysis.put("parentChildRelations", parentChildRelations);
            hierarchyAnalysis.put("detailedStatistics", parentTypeToChildTypes);
            
            // Identificar los tipos de subtareas más comunes
            List<Map.Entry<String, Integer>> sortedChildTypes = childTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());
            
            hierarchyAnalysis.put("mostCommonChildTypes", sortedChildTypes);
            
            if (childTypeCount.isEmpty()) {
                hierarchyAnalysis.put("message", "No se encontraron relaciones padre-hijo en la muestra analizada de " + maxWorkItemsToProcess + " work items");
            } else {
                System.out.println("✅ Análisis de jerarquías completado para proyecto " + project + ": " + 
                                 childTypeCount.size() + " tipos de work items hijos detectados, " +
                                 parentChildRelations.size() + " tipos de padres identificados");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error durante análisis de jerarquías para proyecto " + project + ": " + e.getMessage());
            hierarchyAnalysis.put("error", "Error durante análisis: " + e.getMessage());
        }
        
        return hierarchyAnalysis;
    }
    
    /**
     * Genera documentación de patrones de subtareas basado en el análisis jerárquico
     */
    public String generateSubtaskPatternsDocumentation(Map<String, Map<String, Object>> projectHierarchies) {
        StringBuilder doc = new StringBuilder();
        
        doc.append("\n🏗️ **ANÁLISIS DE PATRONES JERÁRQUICOS**\n");
        doc.append("=====================================\n\n");
        
        Map<String, Set<String>> globalParentChildRelations = new HashMap<>();
        Map<String, Integer> globalChildTypeCount = new HashMap<>();
        
        // Consolidar datos de todos los proyectos
        for (Map.Entry<String, Map<String, Object>> projectEntry : projectHierarchies.entrySet()) {
            String projectName = projectEntry.getKey();
            Map<String, Object> hierarchy = projectEntry.getValue();
            
            if (hierarchy.containsKey("error")) {
                doc.append("⚠️ **").append(projectName).append("**: ").append(hierarchy.get("error")).append("\n");
                continue;
            }
            
            doc.append("📁 **Proyecto: ").append(projectName).append("**\n");
            
            if (hierarchy.containsKey("totalChildWorkItems")) {
                doc.append("   • Total work items hijos: ").append(hierarchy.get("totalChildWorkItems")).append("\n");
            }
            
            // Relaciones padre-hijo
            if (hierarchy.containsKey("parentChildRelations")) {
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> relations = (Map<String, Set<String>>) hierarchy.get("parentChildRelations");
                
                for (Map.Entry<String, Set<String>> relation : relations.entrySet()) {
                    String parentType = relation.getKey();
                    Set<String> childTypes = relation.getValue();
                    
                    doc.append("   • **").append(parentType).append("** puede tener hijos de tipo: ")
                       .append(String.join(", ", childTypes)).append("\n");
                    
                    // Consolidar globalmente
                    globalParentChildRelations.computeIfAbsent(parentType, k -> new HashSet<>()).addAll(childTypes);
                }
            }
            
            // Distribución de tipos hijos
            if (hierarchy.containsKey("childTypeDistribution")) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> distribution = (Map<String, Integer>) hierarchy.get("childTypeDistribution");
                
                for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                    globalChildTypeCount.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
            
            doc.append("\n");
        }
        
        // Resumen global
        doc.append("🌍 **PATRONES GLOBALES IDENTIFICADOS**\n");
        doc.append("===================================\n\n");
        
        if (!globalParentChildRelations.isEmpty()) {
            doc.append("📋 **Relaciones Padre-Hijo Detectadas:**\n");
            for (Map.Entry<String, Set<String>> relation : globalParentChildRelations.entrySet()) {
                doc.append("   • **").append(relation.getKey()).append("** → [")
                   .append(String.join(", ", relation.getValue())).append("]\n");
            }
            doc.append("\n");
        }
        
        if (!globalChildTypeCount.isEmpty()) {
            doc.append("📊 **Tipos de Subtareas/Hijos Más Utilizados:**\n");
            globalChildTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        doc.append("   • **").append(entry.getKey()).append("**: ")
                           .append(entry.getValue()).append(" instancias\n");
                    });
            doc.append("\n");
        }
        
        // Recomendaciones
        doc.append("💡 **RECOMENDACIONES DE USO:**\n");
        doc.append("============================\n");
        doc.append("• Los tipos identificados arriba representan los patrones de subtareas más utilizados en la organización\n");
        doc.append("• Considere estandarizar el uso de estos tipos para mantener consistencia\n");
        doc.append("• Los tipos con mayor número de instancias son los más adoptados por los equipos\n\n");
        
        return doc.toString();
    }
    
    /**
     * Busca un work item específico a través de múltiples proyectos
     */
    public Map<String, Object> findWorkItemAcrossProjects(Integer workItemId) {
        return workItemProcessor.findWorkItemAcrossProjects(workItemId);
    }
    
    /**
     * Extrae ID de work item de una referencia textual
     */
    public Integer extractWorkItemIdFromReference(String reference) {
        return workItemProcessor.extractWorkItemIdFromReference(reference);
    }
    
    /**
     * Extrae nombres de proyectos de la respuesta JSON de la API
     */
    public List<String> extractProjectNames(String jsonResponse) {
        List<String> projectNames = new ArrayList<>();
        
        try {
            // Buscar patrones de nombres de proyecto en la respuesta JSON
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(jsonResponse);
            
            while (nameMatcher.find()) {
                String projectName = nameMatcher.group(1);
                if (!projectNames.contains(projectName)) {
                    projectNames.add(projectName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extrayendo nombres de proyectos: " + e.getMessage());
        }
        
        return projectNames;
    }
    
    // MÉTODOS AUXILIARES PRIVADOS
    
    /**
     * Ejecuta una consulta WIQL y retorna los IDs de work items encontrados
     */
    private List<Integer> executeWIQLQuery(String project, String wiqlQuery) {
        try {
            // Procesar parámetros usando utilidad centralizada si es necesario
            String processedQuery = processWIQLQueryParameters(wiqlQuery, project);
            
            // Ejecutar usando la utilidad centralizada con validación incluida
            WiqlQueryResult result = wiqlUtility.executeWiqlQuery(project, null, processedQuery);
            
            if (result != null && result.workItems() != null) {
                return result.getWorkItemIds();
            }
            
        } catch (Exception e) {
            System.err.println("Error ejecutando consulta WIQL: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Procesa parámetros genéricos en consultas WIQL
     */
    private String processWIQLQueryParameters(String query, String project) {
        // Reemplazar parámetros estándar de Azure DevOps
        String processedQuery = query;
        
        // @project -> nombre del proyecto específico
        processedQuery = processedQuery.replace("@project", "'" + project + "'");
        
        // @me -> usuario actual (usar @me tal como está, Azure DevOps lo resuelve)
        // @today -> fecha actual (usar @today tal como está, Azure DevOps lo resuelve)
        // @currentIteration -> iteración actual (usar @currentIteration tal como está)
        
        // Limpiar espacios extra que puedan haber quedado
        processedQuery = processedQuery.replaceAll("\\s+", " ").trim();
        
        return processedQuery;
    }
    
    /**
     * Obtiene un work item como JSON usando AzureDevOpsClient
     * Método auxiliar para el análisis jerárquico
     */
    private String getWorkItemAsJson(String project, Integer workItemId) {
        try {
            Map<String, Object> workItemData = azureDevOpsClient.getWorkItem(workItemId);
            if (workItemData != null) {
                // Convertir Map a JSON string usando una serialización simple
                // En el futuro se podría usar Jackson para una serialización más completa
                StringBuilder json = new StringBuilder();
                json.append("{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : workItemData.entrySet()) {
                    if (!first) json.append(",");
                    json.append("\"").append(entry.getKey()).append("\":\"")
                        .append(entry.getValue().toString()).append("\"");
                    first = false;
                }
                json.append("}");
                return json.toString();
            }
            
        } catch (Exception e) {
            System.out.println("Error en llamada REST para work item " + workItemId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae un campo específico de una respuesta JSON de Azure DevOps
     */
    private String extractFieldFromJson(String jsonResponse, String fieldName) {
        try {
            // Buscar el campo en la sección "fields"
            String fieldsPattern = "\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}";
            Pattern pattern = Pattern.compile(fieldsPattern);
            Matcher matcher = pattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String fieldsSection = matcher.group(1);
                
                // Buscar el campo específico
                String fieldPattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
                Pattern fieldPatternCompiled = Pattern.compile(fieldPattern);
                Matcher fieldMatcher = fieldPatternCompiled.matcher(fieldsSection);
                
                if (fieldMatcher.find()) {
                    return fieldMatcher.group(1);
                }
            }
        } catch (Exception e) {
            System.out.println("Error extrayendo campo " + fieldName + ": " + e.getMessage());
        }
        return null;
    }
}
