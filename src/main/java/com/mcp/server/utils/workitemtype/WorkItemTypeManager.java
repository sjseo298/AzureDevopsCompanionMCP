package com.mcp.server.utils.workitemtype;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.tools.azuredevops.GetWorkItemTypesTool;
import com.mcp.server.utils.discovery.AzureDevOpsConfigurationGenerator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manejador especializado para operaciones con tipos de work items en Azure DevOps.
 * Extraído de DiscoverOrganizationTool para mejorar la separación de responsabilidades.
 */
public class WorkItemTypeManager {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationConfigService configService;
    private final GetWorkItemTypesTool getWorkItemTypesTool;
    private final AzureDevOpsConfigurationGenerator configurationGenerator;
    
    public WorkItemTypeManager(
            AzureDevOpsClient azureDevOpsClient,
            OrganizationConfigService configService,
            GetWorkItemTypesTool getWorkItemTypesTool,
            AzureDevOpsConfigurationGenerator configurationGenerator) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configService = configService;
        this.getWorkItemTypesTool = getWorkItemTypesTool;
        this.configurationGenerator = configurationGenerator;
    }
    
    /**
     * Analiza tipos de work items en formato detallado
     */
    public String analyzeWorkItemTypesDetailed(String project) {
        StringBuilder analysis = new StringBuilder();
        
        try {
            Map<String, Object> workItemTypesResponse = azureDevOpsClient.getWorkItemTypes(project);
            Object valueObj = workItemTypesResponse.get("value");
            
            if (valueObj instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> workItemTypes = (List<Map<String, Object>>) valueObj;
                
                if (!workItemTypes.isEmpty()) {
                    analysis.append("Encontrados ").append(workItemTypes.size()).append(" tipos de work items:\n\n");
                    
                    for (Map<String, Object> type : workItemTypes) {
                        String typeName = (String) type.get("name");
                        analysis.append("• **").append(typeName).append("**\n");
                    }
                } else {
                    analysis.append("❌ No se encontraron tipos de work items en el proyecto.\n");
                }
            } else {
                analysis.append("❌ Respuesta inesperada de la API.\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ Error analizando tipos: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Obtiene la lista de tipos de work items disponibles en un proyecto
     */
    public List<String> getAvailableWorkItemTypes(String project) {
        List<String> workItemTypes = new ArrayList<>();
        
        try {
            System.out.println("🔍 Obteniendo tipos de work items usando GetWorkItemTypesTool...");
            
            // Usar GetWorkItemTypesTool que ya sabemos que funciona perfectamente
            Map<String, Object> arguments = Map.of(
                "project", project,
                "includeExtendedInfo", false  // Solo necesitamos los nombres
            );
            
            Map<String, Object> result = getWorkItemTypesTool.execute(arguments);
            
            if (result.containsKey("content")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                if (!content.isEmpty() && content.get(0).containsKey("text")) {
                    String response = (String) content.get(0).get("text");
                    
                    // Extraer nombres de tipos de la respuesta
                    List<String> extractedTypes = extractWorkItemTypeNames(response);
                    
                    // Aplicar validación mejorada para garantizar detección completa
                    Set<String> validatedTypes = validateAndEnhanceTypeDetection(response, new HashSet<>(extractedTypes));
                    workItemTypes = new ArrayList<>(validatedTypes);
                    
                    System.out.println("✅ Encontrados " + workItemTypes.size() + " tipos de work items");
                    System.out.println("📋 Tipos finales: " + String.join(", ", workItemTypes));
                }
            }
            
            if (workItemTypes.isEmpty()) {
                System.err.println("⚠️ No se pudieron obtener tipos de work items del proyecto: " + project);
                System.err.println("   Intentando método de fallback...");
                
                // Método de fallback - consulta directa a la API
                workItemTypes = getWorkItemTypesDirectFromApi(project);
                
                if (workItemTypes.isEmpty()) {
                    System.err.println("   ❌ Método de fallback también falló");
                    System.err.println("   Verifique la conectividad y permisos de acceso al proyecto");
                } else {
                    System.out.println("   ✅ Método de fallback exitoso: " + workItemTypes.size() + " tipos encontrados");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo tipos de work items: " + e.getMessage());
            e.printStackTrace();
        }
        
        return workItemTypes;
    }
    
    /**
     * Extrae nombres de tipos de work items de la respuesta formateada
     */
    private List<String> extractWorkItemTypeNames(String response) {
        List<String> typeNames = new ArrayList<>();
        
        try {
            // Buscar patrones específicos en la respuesta
            String[] lines = response.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // Patrones comunes en la respuesta de GetWorkItemTypesTool
                if (trimmedLine.startsWith("• ") || trimmedLine.startsWith("- ")) {
                    String typeName = trimmedLine.substring(2).trim();
                    
                    // Limpiar formato markdown si existe
                    if (typeName.startsWith("**") && typeName.endsWith("**")) {
                        typeName = typeName.substring(2, typeName.length() - 2);
                    }
                    
                    // Remover información adicional entre paréntesis
                    int parenIndex = typeName.indexOf(" (");
                    if (parenIndex > 0) {
                        typeName = typeName.substring(0, parenIndex);
                    }
                    
                    if (isValidWorkItemTypeName(typeName)) {
                        typeNames.add(typeName);
                    }
                }
            }
            
            System.out.println("📊 Tipos extraídos de respuesta: " + typeNames.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error extrayendo nombres de tipos: " + e.getMessage());
        }
        
        return typeNames;
    }
    
    /**
     * Valida y mejora la detección de tipos de work items
     */
    private Set<String> validateAndEnhanceTypeDetection(String response, Set<String> initialTypes) {
        Set<String> enhancedTypes = new HashSet<>(initialTypes);
        
        try {
            // Patrones adicionales para detectar tipos no capturados
            List<Pattern> additionalPatterns = Arrays.asList(
                Pattern.compile("tipo\\s+(?:de\\s+)?work\\s+item[s]?[:\\s]+([A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("work\\s+item\\s+type[s]?[:\\s]+([A-Za-z\\s]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\"([A-Za-z\\s]+)\"\\s+type", Pattern.CASE_INSENSITIVE)
            );
            
            for (Pattern pattern : additionalPatterns) {
                Matcher matcher = pattern.matcher(response);
                while (matcher.find()) {
                    String candidateType = matcher.group(1).trim();
                    if (isValidWorkItemTypeName(candidateType) && !enhancedTypes.contains(candidateType)) {
                        enhancedTypes.add(candidateType);
                        System.out.println("🔍 Tipo adicional detectado: '" + candidateType + "'");
                    }
                }
            }
            
            // Tipos comunes que pueden no ser detectados automáticamente
            List<String> commonTypes = Arrays.asList(
                "Task", "Bug", "User Story", "Feature", "Epic", "Issue", "Test Case",
                "Historia", "Historia técnica", "Tarea", "Subtarea", "Riesgo", "Caso de prueba"
            );
            
            for (String commonType : commonTypes) {
                if (response.toLowerCase().contains(commonType.toLowerCase()) && 
                    !enhancedTypes.contains(commonType)) {
                    enhancedTypes.add(commonType);
                    System.out.println("🎯 Tipo común agregado: '" + commonType + "'");
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error en validación mejorada: " + e.getMessage());
        }
        
        return enhancedTypes;
    }
    
    /**
     * Obtiene tipos de work items directamente desde la API como fallback
     */
    private List<String> getWorkItemTypesDirectFromApi(String project) {
        List<String> workItemTypes = new ArrayList<>();
        
        try {
            System.out.println("🔄 Ejecutando consulta directa a la API para: " + project);
            
            String path = String.format("%s/_apis/wit/workitemtypes", project);
            String response = makeDirectApiRequest(path);
            
            if (response != null) {
                workItemTypes = parseWorkItemTypesFromApiResponse(response);
                System.out.println("✅ API directa retornó " + workItemTypes.size() + " tipos");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en consulta directa a API: " + e.getMessage());
        }
        
        return workItemTypes;
    }
    
    /**
     * Parsea tipos de work items de la respuesta JSON directa de la API
     */
    private List<String> parseWorkItemTypesFromApiResponse(String jsonResponse) {
        List<String> typeNames = new ArrayList<>();
        
        try {
            // Buscar patrones de nombres en JSON: "name":"TipoWorkItem"
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = namePattern.matcher(jsonResponse);
            
            while (matcher.find()) {
                String typeName = matcher.group(1).trim();
                
                // Validar que sea un tipo válido y no duplicado
                if (isValidWorkItemTypeName(typeName) && !typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                    System.out.println("🎯 Tipo extraído de API: '" + typeName + "'");
                }
            }
            
            System.out.println("📊 Total tipos extraídos de API: " + typeNames.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error parseando respuesta de API: " + e.getMessage());
        }
        
        return typeNames;
    }
    
    /**
     * Valida si un nombre es un tipo de work item válido
     */
    private boolean isValidWorkItemTypeName(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return false;
        }
        
        typeName = typeName.trim();
        
        // Excluir valores que no son tipos válidos
        List<String> invalidPatterns = Arrays.asList(
            "project", "team", "area", "iteration", "field", "estado", "priority",
            "new", "active", "done", "closed", "removed", "resolved"
        );
        
        String lowerTypeName = typeName.toLowerCase();
        for (String invalid : invalidPatterns) {
            if (lowerTypeName.equals(invalid)) {
                return false;
            }
        }
        
        // Debe contener solo letras, espacios y algunos caracteres especiales
        return typeName.matches("^[A-Za-z\\sáéíóúÁÉÍÓÚñÑ\\-_]+$") && typeName.length() >= 2;
    }
    
    /**
     * Realiza una investigación específica de tipos de work items
     */
    public String performWorkItemTypesInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
        StringBuilder investigation = new StringBuilder();
        investigation.append("🔍 INVESTIGACIÓN CENTRALIZADA: Tipos de Work Items\n");
        investigation.append("===================================================\n\n");
        investigation.append("📍 Contexto específico:\n");
        investigation.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) investigation.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) investigation.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) investigation.append("   • Iteración: ").append(iterationName).append("\n");
        investigation.append("\n");
        
        try {
            // Generar configuración específica para work item types
            AzureDevOpsConfigurationGenerator.ConfigurationGenerationResult result = 
                configurationGenerator.generateSpecificConfiguration(projectName, "workitem-types", false);
            
            investigation.append("🏗️ **RESULTADO DE GENERACIÓN:**\n");
            investigation.append("==============================\n");
            investigation.append(result.generateReport());
            
            // Usar método existente como complemento para mostrar detalles
            investigation.append("\n📋 **DETALLES ADICIONALES:**\n");
            investigation.append("============================\n");
            investigation.append(analyzeWorkItemTypesDetailed(projectName));
        
        } catch (Exception e) {
            investigation.append("❌ Error durante investigación: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Realiza un análisis completo de tipos de work items con jerarquías
     */
    public String analyzeWorkItemTypes(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📋 Tipos de Work Items\n");
        analysis.append("----------------------\n");
        
        try {
            // Obtener tipos de work items disponibles dinámicamente
            List<String> availableTypes = getAvailableWorkItemTypes(project);
            
            analysis.append("Tipos encontrados en el proyecto: ").append(availableTypes.size()).append("\n\n");
            
            for (String type : availableTypes) {
                analysis.append("• ").append(type);
                
                // Mostrar campos requeridos para este tipo si están configurados
                try {
                    List<String> requiredFields = configService.getRequiredFieldsForWorkItemType(type);
                    if (!requiredFields.isEmpty()) {
                        analysis.append(" (Campos requeridos: ");
                        analysis.append(String.join(", ", requiredFields));
                        analysis.append(")");
                    }
                } catch (Exception e) {
                    // Si no hay configuración para este tipo, continuar
                }
                analysis.append("\n");
            }
            
            analysis.append("\n");
            
            // Agregar información adicional sobre jerarquías si está disponible
            analysis.append("🏗️ **ANÁLISIS JERÁRQUICO**\n");
            analysis.append("========================\n");
            analysis.append("Análisis de jerarquías disponible mediante otros métodos especializados.\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error analizando tipos de work items: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Realiza una solicitud HTTP directa para casos de fallback
     */
    private String makeDirectApiRequest(String path) {
        try {
            // Delegar al client existente que ya maneja autenticación
            return azureDevOpsClient.makeGenericApiRequest(path, null);
        } catch (Exception e) {
            System.err.println("❌ Error en solicitud directa: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae tipos de work items de una respuesta completa
     */
    public Set<String> extractWorkItemTypesFromResponse(String response) {
        Set<String> extractedTypes = new HashSet<>();
        
        try {
            // Combinar múltiples estrategias de extracción
            extractedTypes.addAll(extractWorkItemTypeNames(response));
            extractedTypes.addAll(validateAndEnhanceTypeDetection(response, extractedTypes));
            
        } catch (Exception e) {
            System.err.println("❌ Error extrayendo tipos de respuesta: " + e.getMessage());
        }
        
        return extractedTypes;
    }
}
