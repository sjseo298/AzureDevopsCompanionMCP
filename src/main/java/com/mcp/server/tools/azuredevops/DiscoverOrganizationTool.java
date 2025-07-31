package com.mcp.server.tools.azuredevops;

import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Herramienta MCP para descubrir y analizar la configuración de Azure DevOps de una organización.
 * Genera información sobre proyectos, equipos, tipos de work items y campos disponibles.
 * Incluye funcionalidad avanzada de investigación de campos personalizados y valores de picklist.
 */
@Component
public class DiscoverOrganizationTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationConfigService configService;
    private final HttpClient httpClient;
    
    @Autowired
    public DiscoverOrganizationTool(AzureDevOpsClient azureDevOpsClient, OrganizationConfigService configService) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configService = configService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public String getName() {
        return "azuredevops_discover_organization";
    }
    
    @Override
    public String getDescription() {
        return "Descubre y analiza la configuración de Azure DevOps de una organización, incluyendo proyectos, equipos, tipos de work items y campos disponibles.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        
        properties.put("project", Map.of(
            "type", "string",
            "description", "Nombre del proyecto de Azure DevOps para analizar (opcional, analiza toda la organización si se omite)"
        ));
        
        properties.put("includeWorkItemTypes", Map.of(
            "type", "boolean",
            "description", "Si incluir información detallada de tipos de work items (por defecto: true)"
        ));
        
        properties.put("includeFields", Map.of(
            "type", "boolean", 
            "description", "Si incluir información detallada de campos disponibles (por defecto: false)"
        ));
        
        properties.put("generateConfig", Map.of(
            "type", "boolean",
            "description", "Si generar un archivo de configuración YAML sugerido (por defecto: false)"
        ));
        
        properties.put("investigateCustomFields", Map.of(
            "type", "boolean",
            "description", "Si realizar investigación avanzada de campos personalizados y valores de picklist (por defecto: false)"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            String project = (String) arguments.get("project");
            boolean includeWorkItemTypes = (Boolean) arguments.getOrDefault("includeWorkItemTypes", true);
            boolean includeFields = (Boolean) arguments.getOrDefault("includeFields", false);
            boolean generateConfig = (Boolean) arguments.getOrDefault("generateConfig", false);
            boolean investigateCustomFields = (Boolean) arguments.getOrDefault("investigateCustomFields", false);
            
            StringBuilder result = new StringBuilder();
            result.append("🔍 Análisis de Configuración de Azure DevOps\n");
            result.append("==========================================\n\n");
            
            // Información básica de la organización
            Map<String, Object> orgConfig = configService.getDefaultOrganizationConfig();
            result.append("🏢 Organización: ").append(orgConfig.get("organization")).append("\n");
            
            if (project != null) {
                result.append("📁 Proyecto: ").append(project).append("\n");
            }
            result.append("\n");
            
            // Descubrir proyectos si no se especifica uno
            if (project == null) {
                result.append(analyzeProjects());
            }
            
            // Analizar tipos de work items
            if (includeWorkItemTypes) {
                if (project != null) {
                    result.append(analyzeWorkItemTypes(project));
                } else {
                    result.append("⚠️ Especifique un proyecto para analizar tipos de work items\n\n");
                }
            }
            
            // Analizar campos disponibles
            if (includeFields) {
                if (project != null) {
                    result.append(analyzeWorkItemFields(project));
                } else {
                    result.append("⚠️ Especifique un proyecto para analizar campos de work items\n\n");
                }
            }
            
            // Generar configuración sugerida
            if (generateConfig) {
                result.append(generateSuggestedConfiguration(project));
            }
            
            // Investigación avanzada de campos personalizados (funcionalidad de los scripts)
            if (investigateCustomFields) {
                if (project != null) {
                    result.append(investigateCustomFieldValues(project));
                } else {
                    result.append("⚠️ Especifique un proyecto para investigación avanzada de campos personalizados\n\n");
                }
            }
            
            // Recomendaciones de configuración
            result.append(getConfigurationRecommendations());
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "❌ Error durante el descubrimiento: " + e.getMessage()
                )),
                "isError", true
            );
        }
    }
    
    private String analyzeProjects() {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📁 Proyectos Disponibles\n");
        analysis.append("------------------------\n");
        
        try {
            // Obtener proyectos dinámicamente de la organización
            List<String> availableProjects = getAvailableProjects();
            
            if (availableProjects.isEmpty()) {
                analysis.append("⚠️ No se pudieron obtener proyectos de la organización\n");
                analysis.append("💡 Verifique la configuración de acceso y permisos\n\n");
            } else {
                for (String project : availableProjects) {
                    analysis.append("• ").append(project).append("\n");
                }
                analysis.append("\n💡 Use el parámetro 'project' para analizar un proyecto específico\n\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ Error obteniendo proyectos: ").append(e.getMessage()).append("\n\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Obtiene todos los proyectos disponibles en la organización dinámicamente
     */
    private List<String> getAvailableProjects() {
        List<String> projects = new ArrayList<>();
        
        try {
            String url = String.format("https://dev.azure.com/%s/_apis/projects?api-version=7.1", 
                    getOrganizationFromConfig());
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"value\"")) {
                projects = parseProjectNames(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo proyectos: " + e.getMessage());
        }
        
        return projects;
    }
    
    /**
     * Parsea los nombres de proyectos de la respuesta JSON
     */
    private List<String> parseProjectNames(String jsonResponse) {
        List<String> projectNames = new ArrayList<>();
        
        // Buscar nombres de proyectos en el array "value" usando regex
        Pattern projectPattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = projectPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String projectName = matcher.group(1);
            if (!projectNames.contains(projectName)) {
                projectNames.add(projectName);
            }
        }
        
        return projectNames;
    }
    
    private String analyzeWorkItemTypes(String project) {
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
            
        } catch (Exception e) {
            analysis.append("❌ Error obteniendo tipos de work items: ").append(e.getMessage()).append("\n\n");
        }
        
        return analysis.toString();
    }
    
    private String analyzeWorkItemFields(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🏷️ Campos de Work Items\n");
        analysis.append("-----------------------\n");
        
        try {
            // Mostrar campos básicos soportados
            List<String> basicFields = List.of("title", "description", "assignedTo", "state", "priority", "tags");
            
            analysis.append("Campos Básicos:\n");
            for (String field : basicFields) {
                analysis.append("• ").append(field);
                
                Map<String, Object> fieldMapping = configService.getFieldMapping(field);
                if (!fieldMapping.isEmpty()) {
                    String azureField = (String) fieldMapping.get("azureFieldName");
                    if (azureField != null) {
                        analysis.append(" → ").append(azureField);
                    }
                }
                analysis.append("\n");
            }
            
            analysis.append("\nCampos Específicos (disponibles según tipo):\n");
            analysis.append("• acceptanceCriteria (User Story)\n");
            analysis.append("• reproSteps (Bug)\n");
            analysis.append("• Campos personalizados se descubren dinámicamente con investigateCustomFields=true\n");
            
            analysis.append("\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error obteniendo campos: ").append(e.getMessage()).append("\n\n");
        }
        
        return analysis.toString();
    }
    
    private String generateSuggestedConfiguration(String project) {
        StringBuilder config = new StringBuilder();
        config.append("⚙️ Configuración YAML Sugerida\n");
        config.append("==============================\n");
        config.append("```yaml\n");
        config.append("organization:\n");
        config.append("  name: \"").append(configService.getDefaultOrganizationConfig().get("organization")).append("\"\n");
        config.append("  defaultProject: \"").append(project != null ? project : "YourProject").append("\"\n");
        config.append("  defaultTeam: \"YourTeam\"\n");
        config.append("  timeZone: \"America/Bogota\"\n");
        config.append("  language: \"es-CO\"\n\n");
        
        config.append("fieldMappings:\n");
        config.append("  title:\n");
        config.append("    azureFieldName: \"System.Title\"\n");
        config.append("    required: true\n");
        config.append("  description:\n");
        config.append("    azureFieldName: \"System.Description\"\n");
        config.append("    required: false\n");
        config.append("  state:\n");
        config.append("    azureFieldName: \"System.State\"\n");
        config.append("    required: true\n");
        config.append("    defaultValue: \"New\"\n");
        config.append("```\n\n");
        
        config.append("💾 Guarde esta configuración en config/organization-config.yml\n\n");
        
        return config.toString();
    }
    
    private String getConfigurationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("💡 Recomendaciones de Configuración\n");
        recommendations.append("===================================\n");
        
        recommendations.append("1. 🎯 **Campos Básicos**: Asegúrese de que title, description y state estén configurados\n");
        recommendations.append("2. 🏷️ **Mapeo de Campos**: Defina mapeos específicos para campos personalizados de su organización\n");
        recommendations.append("3. ✅ **Validaciones**: Configure campos requeridos según sus procesos de trabajo\n");
        recommendations.append("4. 🔄 **Compatibilidad**: Use aliases para mantener compatibilidad con herramientas existentes\n");
        recommendations.append("5. 📝 **Documentación**: Agregue helpText para campos personalizados\n\n");
        
        recommendations.append("🚀 **Próximos pasos**:\n");
        recommendations.append("• Ejecute con generateConfig=true para obtener un archivo YAML base\n");
        recommendations.append("• Personalice los mapeos según su organización\n");
        recommendations.append("• Pruebe la configuración con work items de prueba\n");
        
        return recommendations.toString();
    }
    
    // ========================================================================
    // FUNCIONALIDAD AVANZADA DE INVESTIGACIÓN DE CAMPOS
    // Reemplaza la funcionalidad de los scripts investigate-field-values.sh 
    // y get-picklist-values.sh
    // ========================================================================
    
    /**
     * Investiga valores permitidos para campos personalizados específicos
     */
    private String investigateCustomFieldValues(String project) {
        StringBuilder investigation = new StringBuilder();
        investigation.append("🔬 Investigación Avanzada de Campos Personalizados\n");
        investigation.append("================================================\n");
        
        try {
            // Obtener todos los tipos de work items disponibles en el proyecto dinámicamente
            List<String> availableTypes = getAvailableWorkItemTypes(project);
            
            if (availableTypes.isEmpty()) {
                investigation.append("⚠️ No se pudieron obtener los tipos de work items del proyecto\n");
                return investigation.toString();
            }
            
            investigation.append("📋 Tipos de Work Items encontrados: ").append(availableTypes.size()).append("\n");
            investigation.append("Types: ").append(String.join(", ", availableTypes)).append("\n\n");
            
            for (String typeName : availableTypes) {
                investigation.append("\n📋 Tipo: ").append(typeName).append("\n");
                investigation.append("─".repeat(40)).append("\n");
                
                Map<String, List<String>> fieldValues = investigateWorkItemTypeDefinition(project, typeName);
                
                if (fieldValues.isEmpty()) {
                    investigation.append("ℹ️ No se encontraron campos personalizados con valores permitidos\n");
                } else {
                    for (Map.Entry<String, List<String>> entry : fieldValues.entrySet()) {
                        investigation.append("🏷️ Campo: ").append(entry.getKey()).append("\n");
                        investigation.append("   Valores permitidos:\n");
                        for (String value : entry.getValue()) {
                            investigation.append("   • ").append(value).append("\n");
                        }
                        investigation.append("\n");
                    }
                }
            }
            
            // Obtener todos los campos del proyecto para análisis adicional  
            investigation.append("\n🔧 Análisis General de Campos del Proyecto\n");
            investigation.append("─".repeat(40)).append("\n");
            
            List<Map<String, Object>> allFields = getAllProjectFields(project);
            List<Map<String, Object>> customFields = allFields.stream()
                    .filter(field -> {
                        String refName = (String) field.get("referenceName");
                        String name = (String) field.get("name");
                        return refName.contains("Custom") || name.toLowerCase().contains("tipo");
                    })
                    .toList();
            
            investigation.append("📊 Resumen de campos personalizados encontrados: ").append(customFields.size()).append("\n\n");
            
            for (Map<String, Object> field : customFields) {
                investigation.append("🏷️ ").append(field.get("name")).append("\n");
                investigation.append("   Referencia: ").append(field.get("referenceName")).append("\n");
                investigation.append("   Tipo: ").append(field.get("type")).append("\n");
                
                // Intentar obtener valores de picklist si aplica
                if ("picklistString".equals(field.get("type"))) {
                    String picklistId = (String) field.get("picklistId");
                    if (picklistId != null) {
                        List<String> picklistValues = getPicklistValues(project, (String) field.get("referenceName"), picklistId);
                        if (!picklistValues.isEmpty()) {
                            investigation.append("   Valores permitidos:\n");
                            for (String value : picklistValues) {
                                investigation.append("     • ").append(value).append("\n");
                            }
                        }
                    }
                }
                investigation.append("\n");
            }
            
        } catch (Exception e) {
            investigation.append("❌ Error durante la investigación: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Obtiene todos los tipos de work items disponibles en el proyecto dinámicamente
     */
    private List<String> getAvailableWorkItemTypes(String project) {
        List<String> workItemTypes = new ArrayList<>();
        
        try {
            // Usar llamada directa para obtener todos los tipos
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes?api-version=7.1", 
                    getOrganizationFromConfig(), project);
            
            String response = makeDirectApiRequest(url);
            
            if (response != null && response.contains("\"value\"")) {
                workItemTypes = parseWorkItemTypeNamesImproved(response);
            }
            
            // Si aún no tenemos tipos o tenemos pocos, intentar con el método original
            if (workItemTypes.size() < 10) {
                if (response != null) {
                    List<String> originalMethod = parseWorkItemTypeNames(response);
                    if (originalMethod.size() > workItemTypes.size()) {
                        workItemTypes = originalMethod;
                    }
                }
            }
            
            // Si no pudimos obtener tipos dinámicamente, usar tipos comunes como fallback
            if (workItemTypes.isEmpty()) {
                workItemTypes = List.of("Task", "User Story", "Bug", "Feature", "Epic");
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo tipos de work items: " + e.getMessage());
            e.printStackTrace();
            // Fallback a tipos comunes
            workItemTypes = List.of("Task", "User Story", "Bug", "Feature", "Epic");
        }
        
        return workItemTypes;
    }
    
    /**
     * Parsea los nombres de tipos de work items de la respuesta JSON
     */
    private List<String> parseWorkItemTypeNames(String jsonResponse) {
        List<String> typeNames = new ArrayList<>();
        
        // Buscar nombres de tipos en el array "value" usando regex más específico
        Pattern valueArrayPattern = Pattern.compile("\"value\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher valueArrayMatcher = valueArrayPattern.matcher(jsonResponse);
        
        if (valueArrayMatcher.find()) {
            String valueArrayContent = valueArrayMatcher.group(1);
            
            // Buscar objetos individuales de work item types
            Pattern typePattern = Pattern.compile("\\{[^{}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}");
            Matcher matcher = typePattern.matcher(valueArrayContent);
            
            while (matcher.find()) {
                String typeName = matcher.group(1);
                if (!typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                }
            }
        }
        
        return typeNames;
    }
    
    /**
     * Parsea los nombres de tipos de work items de la respuesta JSON usando un enfoque más robusto
     */
    private List<String> parseWorkItemTypeNamesImproved(String jsonResponse) {
        List<String> typeNames = new ArrayList<>();
        
        try {
            // Buscar el array "value" primero
            int valueStart = jsonResponse.indexOf("\"value\"");
            if (valueStart == -1) {
                return typeNames;
            }
            
            // Encontrar el inicio del array
            int arrayStart = jsonResponse.indexOf("[", valueStart);
            if (arrayStart == -1) {
                return typeNames;
            }
            
            // Encontrar el final del array
            int arrayEnd = jsonResponse.lastIndexOf("]");
            if (arrayEnd == -1 || arrayEnd <= arrayStart) {
                return typeNames;
            }
            
            // Extraer el contenido del array
            String arrayContent = jsonResponse.substring(arrayStart + 1, arrayEnd);
            
            // Buscar objetos individuales de work item types
            Pattern objectPattern = Pattern.compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}");
            Matcher matcher = objectPattern.matcher(arrayContent);
            
            while (matcher.find()) {
                String typeName = matcher.group(1);
                if (!typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando work item types: " + e.getMessage());
            // Fallback al método original si falla
            return parseWorkItemTypeNames(jsonResponse);
        }
        
        return typeNames;
    }
    
    /**
     * Investiga la definición completa de un tipo de work item específico
     */
    private Map<String, List<String>> investigateWorkItemTypeDefinition(String project, String workItemTypeName) {
        Map<String, List<String>> fieldAllowedValues = new HashMap<>();
        
        try {
            String encodedTypeName = java.net.URLEncoder.encode(workItemTypeName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?api-version=7.1", 
                    getOrganizationFromConfig(), project, encodedTypeName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                fieldAllowedValues = parseWorkItemTypeFieldValues(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error investigando work item type '" + workItemTypeName + "': " + e.getMessage());
        }
        
        return fieldAllowedValues;
    }
    
    /**
     * Obtiene todos los campos del proyecto con metadatos detallados
     */
    private List<Map<String, Object>> getAllProjectFields(String project) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields?api-version=7.1", 
                    getOrganizationFromConfig(), project);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                fields = parseProjectFields(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Obtiene valores de picklist usando múltiples estrategias de endpoints
     */
    private List<String> getPicklistValues(String project, String fieldReferenceName, String picklistId) {
        // Estrategia 1: Endpoint de procesos organizacionales
        List<String> values = tryGetPicklistFromProcesses(picklistId);
        if (!values.isEmpty()) return values;
        
        // Estrategia 2: Endpoint de procesos con contexto de proyecto
        values = tryGetPicklistFromProjectContext(project, picklistId);
        if (!values.isEmpty()) return values;
        
        // Estrategia 3: Endpoint específico de campo
        values = tryGetPicklistFromFieldEndpoint(project, fieldReferenceName);
        if (!values.isEmpty()) return values;
        
        return Collections.emptyList();
    }
    
    // Métodos auxiliares para investigación avanzada
    
    private String makeDirectApiRequest(String url) {
        try {
            String pat = System.getenv("AZURE_DEVOPS_PAT");
            if (pat == null || pat.isEmpty()) {
                return null;
            }
            
            String auth = Base64.getEncoder().encodeToString((":" + pat).getBytes());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return null;
            }
            
            return response.body();
            
        } catch (Exception e) {
            System.err.println("Error making API request to " + url + ": " + e.getMessage());
            return null;
        }
    }
    
    private Map<String, List<String>> parseWorkItemTypeFieldValues(String jsonResponse) {
        Map<String, List<String>> fieldAllowedValues = new HashMap<>();
        
        // Buscar fieldInstances y sus allowedValues usando regex simple
        Pattern fieldPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String referenceName = matcher.group(1);
            String allowedValuesStr = matcher.group(2);
            
            List<String> values = new ArrayList<>();
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(allowedValuesStr);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
            
            if (!values.isEmpty()) {
                fieldAllowedValues.put(referenceName, values);
            }
        }
        
        return fieldAllowedValues;
    }
    
    private List<Map<String, Object>> parseProjectFields(String jsonResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Buscar cada campo en el array "value" usando regex
        Pattern fieldPattern = Pattern.compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"type\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            try {
                Map<String, Object> field = new HashMap<>();
                field.put("name", matcher.group(1));
                field.put("referenceName", matcher.group(2));
                field.put("type", matcher.group(3));
                
                // Buscar picklistId si existe en este campo
                String fieldBlock = matcher.group(0);
                String picklistId = extractJsonValue(fieldBlock, "picklistId");
                if (picklistId != null) {
                    field.put("picklistId", picklistId);
                }
                
                fields.add(field);
            } catch (Exception e) {
                System.err.println("Error parsing field definition: " + e.getMessage());
            }
        }
        
        return fields;
    }
    
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private List<String> tryGetPicklistFromProcesses(String picklistId) {
        try {
            String url = String.format("https://dev.azure.com/%s/_apis/work/processes/lists/%s?api-version=7.1", 
                    getOrganizationFromConfig(), picklistId);
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromProjectContext(String project, String picklistId) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/work/processes/lists/%s?api-version=7.1", 
                    getOrganizationFromConfig(), project, picklistId);
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromFieldEndpoint(String project, String fieldReferenceName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields/%s/allowedValues?api-version=7.1", 
                    getOrganizationFromConfig(), project, fieldReferenceName);
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"value\"")) {
                return extractArrayValues(response, "value");
            }
        } catch (Exception e) {
            // Continuar silenciosamente
        }
        return Collections.emptyList();
    }
    
    private List<String> extractArrayValues(String json, String arrayKey) {
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
    
    // Métodos auxiliares para obtener configuración
    
    private String getOrganizationFromConfig() {
        // Obtener de la variable de entorno directamente
        String org = System.getenv("AZURE_DEVOPS_ORGANIZATION");
        if (org != null && !org.isEmpty()) {
            return org;
        }
        
        // Fallback al config service
        Map<String, Object> config = configService.getDefaultOrganizationConfig();
        return (String) config.get("organization");
    }
    
    private String getPersonalAccessTokenFromConfig() {
        // En una implementación real, esto vendría de configuración segura
        // Por ahora devolvemos null para usar la configuración del AzureDevOpsClient
        return null;
    }
}
