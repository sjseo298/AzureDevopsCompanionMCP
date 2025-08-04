package com.mcp.server.utils.picklist;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.utils.http.AzureDevOpsHttpUtil;
import com.mcp.server.utils.discovery.AzureDevOpsPicklistInvestigator;
import com.mcp.server.utils.discovery.AzureDevOpsOrganizationInvestigator;
import com.mcp.server.utils.discovery.OrganizationFieldInvestigation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Componente responsable de la gestión de picklists y sus valores en Azure DevOps.
 * Encapsula toda la funcionalidad relacionada con:
 * - Obtención de valores de picklist desde múltiples fuentes
 * - Análisis detallado de picklists organizacionales
 * - Estrategias de fallback para obtener valores
 * - Extracción y parseo de arrays de valores
 */
public class PicklistManager {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsHttpUtil httpUtil;
    private final AzureDevOpsPicklistInvestigator picklistInvestigator;
    private final AzureDevOpsOrganizationInvestigator organizationInvestigator;
    
    public PicklistManager(AzureDevOpsClient azureDevOpsClient, 
                          AzureDevOpsHttpUtil httpUtil,
                          AzureDevOpsPicklistInvestigator picklistInvestigator,
                          AzureDevOpsOrganizationInvestigator organizationInvestigator) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.httpUtil = httpUtil;
        this.picklistInvestigator = picklistInvestigator;
        this.organizationInvestigator = organizationInvestigator;
    }
    
    /**
     * Obtiene valores de picklist usando múltiples estrategias de endpoints
     * Delega al investigador especializado para mantener consistencia
     */
    public List<String> getPicklistValues(String project, String fieldReferenceName, String picklistId) {
        // REFACTORIZADO: Usar utilidad centralizada en lugar de implementación duplicada
        return picklistInvestigator.getPicklistValues(project, fieldReferenceName, picklistId);
    }
    
    /**
     * Intenta obtener valores de picklist desde el endpoint de procesos globales
     */
    public List<String> tryGetPicklistFromProcesses(String picklistId) {
        try {
            String endpoint = String.format("/_apis/work/processes/lists/%s", picklistId);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    /**
     * Intenta obtener valores de picklist desde el contexto específico del proyecto
     */
    public List<String> tryGetPicklistFromProjectContext(String project, String picklistId) {
        try {
            String endpoint = String.format("/%s/_apis/work/processes/lists/%s", project, picklistId);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    /**
     * Intenta obtener valores de picklist desde el endpoint específico del campo
     */
    public List<String> tryGetPicklistFromFieldEndpoint(String project, String fieldReferenceName) {
        try {
            String endpoint = String.format("/%s/_apis/wit/fields/%s/allowedValues", project, fieldReferenceName);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            if (response != null && response.contains("\"value\"")) {
                return extractArrayValues(response, "value");
            }
        } catch (Exception e) {
            // Continuar silenciosamente
        }
        return Collections.emptyList();
    }
    
    /**
     * Extrae valores de un array JSON específico
     */
    public List<String> extractArrayValues(String json, String arrayKey) {
        List<String> values = new ArrayList<>();
        
        Pattern arrayPattern = Pattern.compile("\"" + arrayKey + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(arrayContent);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
        }
        
        return values;
    }
    
    /**
     * Análisis detallado de valores de picklist - REFACTORIZADO
     */
    public String analyzePicklistValuesDetailed(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🔍 Iniciando análisis detallado de valores de picklist para proyecto: ").append(project).append("\n\n");
        
        try {
            // **USAR INVESTIGADOR ORGANIZACIONAL CENTRALIZADO**
            analysis.append("🔧 **INVESTIGACIÓN USANDO INVESTIGADOR ORGANIZACIONAL**\n");
            analysis.append("====================================================\n");
            analysis.append("Utilizando AzureDevOpsOrganizationInvestigator para análisis completo y optimizado\n\n");
            
            // Realizar investigación completa de la organización
            OrganizationFieldInvestigation investigation = organizationInvestigator.performCompleteInvestigation(project);
            
            // Generar reporte detallado usando el investigador
            String detailedReport = organizationInvestigator.generateDetailedReport(investigation);
            analysis.append(detailedReport);
            
            // Análisis específico de campos problemáticos conocidos
            analysis.append("\n🔍 **ANÁLISIS DE CAMPOS PROBLEMÁTICOS ESPECÍFICOS**\n");
            analysis.append("==================================================\n");
            AzureDevOpsOrganizationInvestigator.ProblematicFieldsAnalysis problematicAnalysis = 
                organizationInvestigator.analyzeProblematicFields(project);
            
            if (problematicAnalysis != null) {
                analysis.append("📋 Campos problemáticos identificados: ").append(problematicAnalysis.getValidationResults().size()).append("\n");
                
                // Contar campos válidos e inválidos
                long validFields = problematicAnalysis.getValidationResults().values().stream()
                    .mapToLong(result -> result.isValid() ? 1 : 0)
                    .sum();
                long invalidFields = problematicAnalysis.getValidationResults().size() - validFields;
                
                analysis.append("✅ Campos resueltos: ").append(validFields).append("\n");
                analysis.append("❌ Campos sin resolver: ").append(invalidFields).append("\n\n");
                
                // Detalles de campos sin resolver
                List<String> unresolvedFields = problematicAnalysis.getValidationResults().entrySet().stream()
                    .filter(entry -> !entry.getValue().isValid())
                    .map(Map.Entry::getKey)
                    .toList();
                
                if (!unresolvedFields.isEmpty()) {
                    analysis.append("**Campos sin resolver:**\n");
                    for (String unresolvedField : unresolvedFields) {
                        analysis.append("  - ").append(unresolvedField).append("\n");
                    }
                    analysis.append("\n");
                }
                
                // Generar reporte usando el método disponible
                analysis.append("**Reporte detallado:**\n");
                analysis.append(problematicAnalysis.generateReport());
            }
            
            return analysis.toString();
            
        } catch (Exception e) {
            analysis.append("❌ Error durante el análisis detallado de picklists: ").append(e.getMessage()).append("\n");
            analysis.append("Se continuará con métodos de fallback...\n\n");
            
            // Fallback a análisis manual básico
            return performBasicPicklistAnalysis(project, analysis);
        }
    }
    
    /**
     * Análisis básico de picklists como fallback cuando el investigador organizacional falla
     */
    private String performBasicPicklistAnalysis(String project, StringBuilder analysis) {
        try {
            analysis.append("🔄 **ANÁLISIS BÁSICO DE PICKLISTS (FALLBACK)**\n");
            analysis.append("============================================\n");
            
            // Obtener campos del proyecto con información básica
            Map<String, String> picklistFields = discoverPicklistFields(project);
            
            if (picklistFields.isEmpty()) {
                analysis.append("⚠️ No se encontraron campos tipo picklist en el proyecto.\n");
                return analysis.toString();
            }
            
            analysis.append("📋 Campos tipo picklist encontrados: ").append(picklistFields.size()).append("\n\n");
            
            int successfulAnalysis = 0;
            int failedAnalysis = 0;
            
            for (Map.Entry<String, String> entry : picklistFields.entrySet()) {
                String fieldName = entry.getKey();
                String picklistId = entry.getValue();
                
                analysis.append("🔍 **Campo: ").append(fieldName).append("**\n");
                analysis.append("   📋 Picklist ID: ").append(picklistId != null ? picklistId : "No disponible").append("\n");
                
                try {
                    List<String> values = getPicklistValues(project, fieldName, picklistId);
                    if (!values.isEmpty()) {
                        analysis.append("   ✅ Valores encontrados: ").append(values.size()).append("\n");
                        analysis.append("   📝 Valores: ").append(String.join(", ", values.subList(0, Math.min(5, values.size()))));
                        if (values.size() > 5) {
                            analysis.append(" ... (y ").append(values.size() - 5).append(" más)");
                        }
                        analysis.append("\n");
                        successfulAnalysis++;
                    } else {
                        analysis.append("   ❌ No se pudieron obtener valores\n");
                        failedAnalysis++;
                    }
                } catch (Exception e) {
                    analysis.append("   ❌ Error: ").append(e.getMessage()).append("\n");
                    failedAnalysis++;
                }
                
                analysis.append("\n");
            }
            
            // Resumen final
            analysis.append("📊 **RESUMEN DEL ANÁLISIS**\n");
            analysis.append("=========================\n");
            analysis.append("✅ Campos analizados exitosamente: ").append(successfulAnalysis).append("\n");
            analysis.append("❌ Campos con errores: ").append(failedAnalysis).append("\n");
            analysis.append("📈 Tasa de éxito: ").append(String.format("%.1f%%", 
                (successfulAnalysis * 100.0) / (successfulAnalysis + failedAnalysis))).append("\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error crítico en análisis básico: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Descubre campos tipo picklist en el proyecto
     */
    private Map<String, String> discoverPicklistFields(String project) {
        Map<String, String> picklistFields = new HashMap<>();
        
        try {
            String endpoint = httpUtil.buildFieldsEndpoint(project);
            String response = azureDevOpsClient.makeGenericApiRequest(endpoint, httpUtil.getDefaultQueryParams());
            
            if (response != null) {
                // Buscar campos con picklistId
                Pattern fieldPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"picklistId\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
                Matcher matcher = fieldPattern.matcher(response);
                
                while (matcher.find()) {
                    String fieldName = matcher.group(1);
                    String picklistId = matcher.group(2);
                    picklistFields.put(fieldName, picklistId);
                }
                
                // También buscar campos que podrían ser picklists por su nombre
                Pattern namePattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"type\"\\s*:\\s*\"string\"", Pattern.DOTALL);
                Matcher nameMatcher = namePattern.matcher(response);
                
                while (nameMatcher.find()) {
                    String fieldName = nameMatcher.group(1);
                    if (isLikelyPicklistField(fieldName) && !picklistFields.containsKey(fieldName)) {
                        picklistFields.put(fieldName, null); // Sin picklistId específico
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error descubriendo campos picklist: " + e.getMessage());
        }
        
        return picklistFields;
    }
    
    /**
     * Determina si un campo es probablemente de tipo lista basado en su nombre
     */
    private boolean isLikelyPicklistField(String referenceName) {
        if (referenceName == null) return false;
        
        String fieldName = referenceName.toLowerCase();
        
        // Patrones comunes que indican campos de lista
        return fieldName.contains("tipo") ||
               fieldName.contains("type") ||
               fieldName.contains("categoria") ||
               fieldName.contains("category") ||
               fieldName.contains("clasificacion") ||
               fieldName.contains("classification") ||
               fieldName.contains("nivel") ||
               fieldName.contains("level") ||
               fieldName.contains("origen") ||
               fieldName.contains("source") ||
               fieldName.contains("fase") ||
               fieldName.contains("phase") ||
               fieldName.contains("estado") ||
               fieldName.contains("status") ||
               fieldName.contains("prioridad") ||
               fieldName.contains("priority");
    }
}
