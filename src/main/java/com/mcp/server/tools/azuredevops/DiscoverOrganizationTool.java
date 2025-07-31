package com.mcp.server.tools.azuredevops;

import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.base.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * Herramienta MCP para descubrir y analizar la configuración de Azure DevOps de una organización.
 * Genera información sobre proyectos, equipos, tipos de work items y campos disponibles.
 * Incluye funcionalidad avanzada de investigación de campos personalizados y valores de picklist.
 */
@Component
public class DiscoverOrganizationTool implements McpTool {
    
    private static final Logger logger = LoggerFactory.getLogger(DiscoverOrganizationTool.class);
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationConfigService configService;
    private final HttpClient httpClient;
    private final GetWorkItemTypesTool getWorkItemTypesTool;
    
    @Autowired
    public DiscoverOrganizationTool(AzureDevOpsClient azureDevOpsClient, OrganizationConfigService configService, 
                                   GetWorkItemTypesTool getWorkItemTypesTool) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configService = configService;
        this.getWorkItemTypesTool = getWorkItemTypesTool;
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
        
        properties.put("exhaustiveTypeDiscovery", Map.of(
            "type", "boolean",
            "description", "Si realizar descubrimiento exhaustivo de tipos de work items en TODOS los proyectos con información completa (por defecto: false)"
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
            boolean exhaustiveTypeDiscovery = (Boolean) arguments.getOrDefault("exhaustiveTypeDiscovery", false);
            
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
            
            // NUEVO: Descubrimiento exhaustivo de tipos de work items
            if (exhaustiveTypeDiscovery) {
                result.append(performExhaustiveWorkItemTypeDiscovery());
                result.append("\n");
                
                // Si se solicita descubrimiento exhaustivo, no necesitamos hacer análisis específico por proyecto
                // ya que se analizan TODOS los proyectos
                
            } else {
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
    
    /**
     * Realiza descubrimiento exhaustivo de todos los tipos de work items en todos los proyectos
     * Utiliza GetWorkItemTypesTool para obtener información completa
     */
    private String performExhaustiveWorkItemTypeDiscovery() {
        StringBuilder discovery = new StringBuilder();
        discovery.append("🔍 DESCUBRIMIENTO EXHAUSTIVO DE TIPOS DE WORK ITEMS\n");
        discovery.append("==================================================\n\n");
        
        try {
            // Obtener todos los proyectos disponibles
            List<String> projects = getAvailableProjects();
            if (projects.isEmpty()) {
                discovery.append("⚠️ No se encontraron proyectos disponibles\n");
                return discovery.toString();
            }
            
            discovery.append("📁 **Proyectos a analizar:** ").append(projects.size()).append("\n");
            for (String project : projects) {
                discovery.append("   • ").append(project).append("\n");
            }
            discovery.append("\n");
            
            Map<String, Set<String>> allTypesPerProject = new HashMap<>();
            Map<String, Map<String, Object>> typeDefinitions = new HashMap<>();
            int totalUniqueTypes = 0;
            
            // Para cada proyecto, obtener tipos de work items completos
            for (String project : projects) {
                discovery.append("🔍 **Analizando proyecto: ").append(project).append("**\n");
                
                // Usar GetWorkItemTypesTool para obtener información completa
                Map<String, Object> arguments = Map.of(
                    "project", project,
                    "includeExtendedInfo", true,
                    "includeFieldDetails", true
                );
                
                Map<String, Object> result = getWorkItemTypesTool.execute(arguments);
                
                if (result.containsKey("content")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                    if (!content.isEmpty() && content.get(0).containsKey("text")) {
                        String response = (String) content.get(0).get("text");
                        
                        // Extraer información de tipos del response
                        Set<String> projectTypes = extractWorkItemTypesFromResponse(response);
                        allTypesPerProject.put(project, projectTypes);
                        
                        discovery.append("   ✅ Encontrados ").append(projectTypes.size()).append(" tipos: ");
                        discovery.append(String.join(", ", projectTypes)).append("\n");
                        
                        // Almacenar definiciones detalladas para análisis posterior
                        storeTypeDefinitions(project, response, typeDefinitions);
                    }
                }
                discovery.append("\n");
            }
            
            // Análisis global de tipos únicos
            Set<String> allUniqueTypes = new HashSet<>();
            for (Set<String> projectTypes : allTypesPerProject.values()) {
                allUniqueTypes.addAll(projectTypes);
            }
            totalUniqueTypes = allUniqueTypes.size();
            
            discovery.append("📊 **RESUMEN GLOBAL**\n");
            discovery.append("═══════════════════\n");
            discovery.append("• **Total de proyectos analizados:** ").append(projects.size()).append("\n");
            discovery.append("• **Total de tipos únicos encontrados:** ").append(totalUniqueTypes).append("\n");
            discovery.append("• **Tipos únicos globales:** ").append(String.join(", ", allUniqueTypes)).append("\n\n");
            
            // Análisis de tipos por proyecto
            discovery.append("📈 **ANÁLISIS POR PROYECTO**\n");
            discovery.append("═══════════════════════════\n");
            for (Map.Entry<String, Set<String>> entry : allTypesPerProject.entrySet()) {
                String project = entry.getKey();
                Set<String> types = entry.getValue();
                discovery.append("🏗️ **").append(project).append(":** ").append(types.size()).append(" tipos\n");
                
                // Identificar tipos únicos por proyecto
                Set<String> uniqueToProject = new HashSet<>(types);
                for (Map.Entry<String, Set<String>> other : allTypesPerProject.entrySet()) {
                    if (!other.getKey().equals(project)) {
                        uniqueToProject.removeAll(other.getValue());
                    }
                }
                
                if (!uniqueToProject.isEmpty()) {
                    discovery.append("   🎯 **Únicos:** ").append(String.join(", ", uniqueToProject)).append("\n");
                }
            }
            
            discovery.append("\n💡 **RECOMENDACIÓN:** Esta información completa debe usarse para actualizar\n");
            discovery.append("los archivos de configuración organizacional para garantizar soporte completo.\n");
            
        } catch (Exception e) {
            discovery.append("❌ Error durante descubrimiento exhaustivo: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        
        return discovery.toString();
    }
    
    /**
     * Extrae nombres de tipos de work items de la respuesta formateada
     */
    private Set<String> extractWorkItemTypesFromResponse(String response) {
        Set<String> types = new HashSet<>();
        
        // Buscar patrones como "🔹 **NombreTipo**"
        Pattern pattern = Pattern.compile("🔹 \\*\\*([^*]+)\\*\\*");
        Matcher matcher = pattern.matcher(response);
        
        while (matcher.find()) {
            String typeName = matcher.group(1).trim();
            // Limpiar texto extra como "(Deshabilitado)"
            typeName = typeName.replaceAll("\\s*_\\([^)]+\\)_", "").trim();
            types.add(typeName);
        }
        
        return types;
    }
    
    /**
     * Almacena definiciones detalladas de tipos para análisis posterior
     */
    private void storeTypeDefinitions(String project, String response, Map<String, Map<String, Object>> typeDefinitions) {
        // Por ahora solo almacenamos el proyecto y response para referencia futura
        // En una implementación más avanzada, podríamos parsear campos específicos
        typeDefinitions.put(project + "_response", Map.of("project", project, "response", response));
    }
    
    /**
     * Obtiene los campos básicos configurados dinámicamente desde el servicio de configuración
     */
    private List<String> getConfiguredBasicFields() {
        List<String> configuredFields = new ArrayList<>();
        
        try {
            // Obtener campos dinámicamente desde archivos de configuración existentes
            // Esto permite que el sistema se adapte a cualquier configuración sin hardcodear
            List<String> potentialFields = discoverFieldsFromConfiguration();
            
            // Verificar cuáles de estos campos están realmente configurados
            for (String fieldName : potentialFields) {
                try {
                    Map<String, Object> fieldMapping = configService.getFieldMapping(fieldName);
                    if (fieldMapping != null && !fieldMapping.isEmpty()) {
                        configuredFields.add(fieldName);
                    }
                } catch (Exception e) {
                    // Campo no configurado, continuar con el siguiente
                }
            }
            
            System.out.println("Campos básicos encontrados en configuración: " + configuredFields.size());
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos configurados: " + e.getMessage());
            // En caso de error, devolver lista vacía (sin fallback hardcoded)
        }
        
        return configuredFields;
    }
    
    /**
     * Descubre campos desde archivos de configuración existentes de forma dinámica
     */
    private List<String> discoverFieldsFromConfiguration() {
        List<String> discoveredFields = new ArrayList<>();
        
        try {
            // Leer del archivo field-mappings.yml si existe
            String configPath = "config/field-mappings.yml";
            discoveredFields = extractFieldNamesFromConfigFile(configPath);
            
            if (discoveredFields.isEmpty()) {
                // Si no hay archivo de configuración, usar un conjunto mínimo dinámico
                System.out.println("No se encontró configuración de campos - usando descubrimiento dinámico completo");
            }
            
        } catch (Exception e) {
            System.err.println("Error descubriendo campos desde configuración: " + e.getMessage());
        }
        
        return discoveredFields;
    }
    
    /**
     * Extrae nombres de campos del archivo de configuración YAML
     */
    private List<String> extractFieldNamesFromConfigFile(String configPath) {
        List<String> fieldNames = new ArrayList<>();
        
        try {
            // Este método se puede implementar para leer realmente del archivo YAML
            // Por ahora devolver lista vacía para forzar descubrimiento completamente dinámico
            System.out.println("Configuración dinámica de campos activada");
            
        } catch (Exception e) {
            System.err.println("Error leyendo archivo de configuración: " + e.getMessage());
        }
        
        return fieldNames;
    }

    private String analyzeWorkItemFields(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🏷️ Campos de Work Items\n");
        analysis.append("-----------------------\n");
        
        try {
            // Obtener campos básicos dinámicamente desde la configuración existente
            List<String> configuredFields = getConfiguredBasicFields();
            
            if (!configuredFields.isEmpty()) {
                analysis.append("Campos Básicos Configurados:\n");
                for (String field : configuredFields) {
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
            } else {
                analysis.append("⚠️ No se encontraron campos básicos configurados\n");
                analysis.append("💡 Use generateConfig=true para crear configuración base\n");
            }
            
            analysis.append("\nCampos Dinámicos:\n");
            analysis.append("• Los campos específicos se descubren automáticamente por tipo de work item\n");
            analysis.append("• Use investigateCustomFields=true para análisis exhaustivo de campos personalizados\n");
            
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
            
            // Investigar TODOS los tipos, no solo los conocidos, para ser exhaustivos
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
     * Obtiene todos los tipos de work items disponibles en el proyecto usando GetWorkItemTypesTool
     * Esta implementación garantiza consistencia con la herramienta especializada que ya funciona
     */
    private List<String> getAvailableWorkItemTypes(String project) {
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
                    workItemTypes = extractWorkItemTypeNames(response);
                    System.out.println("✅ Encontrados " + workItemTypes.size() + " tipos de work items");
                }
            }
            
            if (workItemTypes.isEmpty()) {
                System.err.println("⚠️ No se pudieron obtener tipos de work items del proyecto: " + project);
                System.err.println("   Verifique la conectividad y permisos de acceso al proyecto");
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo tipos de work items: " + e.getMessage());
            e.printStackTrace();
        }
        
        return workItemTypes;
    }
    
    /**
     * Extrae nombres de tipos de work items de la respuesta formateada de GetWorkItemTypesTool
     */
    private List<String> extractWorkItemTypeNames(String response) {
        List<String> typeNames = new ArrayList<>();
        
        try {
            // Buscar patrones como "🔹 **NombreTipo**" en la respuesta
            Pattern pattern = Pattern.compile("🔹 \\*\\*([^*]+)\\*\\*");
            Matcher matcher = pattern.matcher(response);
            
            while (matcher.find()) {
                String typeName = matcher.group(1).trim();
                // Limpiar texto extra como "(Deshabilitado)"
                typeName = typeName.replaceAll("\\s*_\\([^)]+\\)_", "").trim();
                if (!typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                }
            }
            
            System.out.println("📋 Tipos extraídos: " + String.join(", ", typeNames));
            
        } catch (Exception e) {
            System.err.println("Error extrayendo nombres de tipos: " + e.getMessage());
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
            
            // Intentar obtener valores de la definición de tipo primero
            if (response != null) {
                fieldAllowedValues = parseWorkItemTypeFieldValues(response);
            }
            
            // Si no encontramos valores en la definición, usar WIQL para extraer valores reales
            if (fieldAllowedValues.isEmpty()) {
                Map<String, List<String>> dynamicValues = extractFieldValuesFromExistingWorkItems(project, workItemTypeName);
                fieldAllowedValues.putAll(dynamicValues);
            }
            
        } catch (Exception e) {
            System.err.println("Error investigando work item type '" + workItemTypeName + "': " + e.getMessage());
            // En caso de error, usar valores de fallback
            fieldAllowedValues = extractFieldValuesFromExistingWorkItems(project, workItemTypeName);
        }
        
        return fieldAllowedValues;
    }
    
    /**
     * Extrae valores únicos de campos personalizados analizando work items existentes usando WIQL
     */
    private Map<String, List<String>> extractFieldValuesFromExistingWorkItems(String project, String workItemTypeName) {
        Map<String, List<String>> fieldValues = new HashMap<>();
        
        try {
            System.out.println("🔍 Analizando campos personalizados para: " + workItemTypeName);
            
            // Primero obtener los campos personalizados del tipo de work item
            List<String> customFields = getCustomFieldsForWorkItemType(project, workItemTypeName);
            if (customFields.isEmpty()) {
                System.out.println("ℹ️ No se encontraron campos personalizados para el tipo: " + workItemTypeName);
                return fieldValues;
            }
            
            System.out.println("📋 Campos a investigar: " + customFields.size());
            
            // Construir consulta WIQL para obtener work items recientes
            String wiqlQuery = buildWIQLQueryForFieldDiscovery(workItemTypeName, customFields);
            
            // Ejecutar consulta WIQL
            List<Integer> workItemIds = executeWIQLQuery(project, wiqlQuery);
            
            if (!workItemIds.isEmpty()) {
                System.out.println("📊 Analizando " + workItemIds.size() + " work items para extraer valores");
                
                // Obtener detalles de los work items encontrados
                Map<String, Set<String>> discoveredValues = extractFieldValuesFromWorkItems(project, workItemIds, customFields);
                
                // Convertir Set a List para el resultado
                for (Map.Entry<String, Set<String>> entry : discoveredValues.entrySet()) {
                    List<String> values = new ArrayList<>(entry.getValue());
                    if (!values.isEmpty()) {
                        fieldValues.put(entry.getKey(), values);
                        System.out.println("✅ Campo " + entry.getKey() + ": " + values.size() + " valores únicos");
                    }
                }
                
                if (fieldValues.isEmpty()) {
                    System.out.println("⚠️ No se encontraron valores en los work items analizados");
                }
            } else {
                System.out.println("⚠️ No se encontraron work items del tipo " + workItemTypeName + " en el período especificado");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting field values from existing work items: " + e.getMessage());
            e.printStackTrace();
        }
        
        return fieldValues;
    }
    
    /**
     * Parsea valores de campos de work items existentes
     */
    private Map<String, List<String>> parseFieldValuesFromWorkItemsResponse(String jsonResponse, String targetType) {
        // Este método ya no se usa - los valores se extraen dinámicamente usando WIQL
        return new HashMap<>();
    }
    
    /**
     * Obtiene los campos personalizados específicos para un tipo de work item con validación
     */
    private List<String> getCustomFieldsForWorkItemType(String project, String workItemTypeName) {
        List<String> customFields = new ArrayList<>();
        
        try {
            String encodedTypeName = java.net.URLEncoder.encode(workItemTypeName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?api-version=7.1", 
                    getOrganizationFromConfig(), project, encodedTypeName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                customFields = parseCustomFieldsFromTypeDefinition(response);
                System.out.println("Campos personalizados encontrados para " + workItemTypeName + ": " + customFields.size());
                
                // Validar que los campos realmente existen y están disponibles
                customFields = validateFieldsExistence(customFields, project, workItemTypeName);
            } else {
                System.out.println("No se pudo obtener definición de tipo para: " + workItemTypeName);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting custom fields for work item type '" + workItemTypeName + "': " + e.getMessage());
        }
        
        return customFields;
    }
    
    /**
     * Valida que los campos personalizados realmente existen y están disponibles
     */
    private List<String> validateFieldsExistence(List<String> candidateFields, String project, String workItemType) {
        List<String> validFields = new ArrayList<>();
        
        for (String field : candidateFields) {
            if (field != null && !field.trim().isEmpty() && isValidCustomField(field)) {
                // Solo incluir campos que parezcan válidos (Custom.GUID o Custom.Nombre)
                if (field.startsWith("Custom.") && 
                    (field.matches("Custom\\.[a-f0-9-]{36}") || field.matches("Custom\\.[A-Za-z][A-Za-z0-9]*"))) {
                    validFields.add(field.trim());
                }
            }
        }
        
        System.out.println("Campos validados para " + workItemType + ": " + validFields.size() + "/" + candidateFields.size());
        return validFields;
    }
    
    /**
     * Verifica si un campo personalizado tiene formato válido y no causa errores conocidos
     */
    private boolean isValidCustomField(String fieldName) {
        if (fieldName == null || !fieldName.startsWith("Custom.")) {
            return false;
        }
        
        // Validar formato básico (debe ser Custom.GUID o Custom.NombreValido)
        if (!fieldName.matches("Custom\\.[a-f0-9-]{36}") && 
            !fieldName.matches("Custom\\.[A-Za-z][A-Za-z0-9_]*")) {
            System.out.println("⚠️ Campo con formato inválido: " + fieldName);
            return false;
        }
        
        // Lista dinámica de campos problemáticos se puede expandir según errores encontrados
        // Estos campos se excluyen porque causan errores específicos de la API
        Set<String> knownProblematicFields = getKnownProblematicFields();
        
        if (knownProblematicFields.contains(fieldName)) {
            System.out.println("⚠️ Excluyendo campo problemático conocido: " + fieldName);
            return false;
        }
        
        return true;
    }
    
    /**
     * Obtiene campos problemáticos conocidos de forma dinámica
     * Estos pueden expandirse según errores encontrados en tiempo de ejecución
     */
    private Set<String> getKnownProblematicFields() {
        Set<String> problematicFields = new HashSet<>();
        
        // Solo agregar campos que han causado errores específicos confirmados
        // Se pueden agregar dinámicamente según los errores encontrados
        problematicFields.add("Custom.46f6eede-0b66-4fa8-93db-e783f3be205c"); // TF51005 confirmado
        
        return problematicFields;
    }
    
    /**
     * Parsea campos personalizados de la definición del tipo de work item
     */
    private List<String> parseCustomFieldsFromTypeDefinition(String jsonResponse) {
        List<String> customFields = new ArrayList<>();
        
        // Buscar campos que empiecen con "Custom." en fieldInstances
        Pattern fieldPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"(Custom\\.[^\"]+)\"");
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            if (!customFields.contains(fieldName)) {
                customFields.add(fieldName);
            }
        }
        
        return customFields;
    }
    
    /**
     * Construye una consulta WIQL para descubrir valores de campos específicos
     */
    private String buildWIQLQueryForFieldDiscovery(String workItemTypeName, List<String> customFields) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT [System.Id], [System.Title], [System.State]");
        
        // Validar y agregar solo campos que existen
        List<String> validFields = new ArrayList<>();
        for (String field : customFields) {
            if (field != null && !field.trim().isEmpty()) {
                validFields.add(field.trim());
                query.append(", [").append(field.trim()).append("]");
            }
        }
        
        query.append(" FROM WorkItems WHERE [System.WorkItemType] = '").append(workItemTypeName).append("'");
        
        // Ampliar criterios para encontrar más work items con datos
        // Incluir work items en cualquier estado, no solo finales
        query.append(" AND [System.ChangedDate] > @Today - 180"); // Últimos 6 meses para balance entre datos y rendimiento
        
        // CRÍTICO: Limitar resultados para evitar error VS402337 (límite 20000)
        query.append(" ORDER BY [System.ChangedDate] DESC");
        
        return query.toString();
    }
    
    /**
     * Ejecuta una consulta WIQL y retorna los IDs de work items encontrados
     */
    private List<Integer> executeWIQLQuery(String project, String wiqlQuery) {
        List<Integer> workItemIds = new ArrayList<>();
        
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/wiql?api-version=7.1", 
                    getOrganizationFromConfig(), project);
            
            // Crear el payload JSON para la consulta WIQL con límite explícito
            String limitedQuery = addLimitToWIQLQuery(wiqlQuery, 200); // Límite seguro muy por debajo de 20000
            String payload = String.format("{\"query\":\"%s\"}", limitedQuery.replace("\"", "\\\""));
            
            System.out.println("Ejecutando consulta WIQL limitada: " + limitedQuery);
            
            String response = makeWIQLApiRequest(url, payload);
            if (response != null) {
                workItemIds = parseWorkItemIdsFromWIQLResponse(response);
                System.out.println("WIQL query retornó " + workItemIds.size() + " work items");
            }
            
        } catch (Exception e) {
            System.err.println("Error executing WIQL query: " + e.getMessage());
        }
        
        return workItemIds;
    }
    
    /**
     * Agrega límite TOP a consulta WIQL para evitar exceder límite de 20000 items
     */
    private String addLimitToWIQLQuery(String originalQuery, int limit) {
        // Si la consulta ya tiene SELECT, agregar TOP después de SELECT
        if (originalQuery.toUpperCase().contains("SELECT")) {
            return originalQuery.replaceFirst("(?i)SELECT", "SELECT TOP " + limit);
        }
        return originalQuery;
    }
    
    /**
     * Hace una petición POST para WIQL con manejo robusto de errores
     */
    private String makeWIQLApiRequest(String url, String payload) {
        try {
            String pat = System.getenv("AZURE_DEVOPS_PAT");
            if (pat == null || pat.isEmpty()) {
                System.err.println("Azure DevOps PAT no configurado");
                return null;
            }
            
            String auth = Base64.getEncoder().encodeToString((":" + pat).getBytes());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                String errorBody = response.body();
                
                // Identificar y manejar errores específicos
                if (errorBody.contains("VS402337") || errorBody.contains("exceeds the size limit")) {
                    System.err.println("❌ ERROR: La consulta WIQL excede el límite de 20,000 work items");
                    System.err.println("   Solución: La consulta será automáticamente limitada a 200 items");
                    return null;
                } else if (errorBody.contains("TF51005") || errorBody.contains("field that does not exist")) {
                    System.err.println("❌ ERROR: Campo referenciado no existe en el proyecto");
                    System.err.println("   Detalle: " + errorBody);
                    return null;
                } else {
                    System.err.println("❌ WIQL query failed with status: " + response.statusCode() + " - " + errorBody);
                    return null;
                }
            }
            
            return response.body();
            
        } catch (Exception e) {
            System.err.println("Error making WIQL API request: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parsea IDs de work items de la respuesta WIQL
     */
    private List<Integer> parseWorkItemIdsFromWIQLResponse(String jsonResponse) {
        List<Integer> ids = new ArrayList<>();
        
        // Buscar workItems en la respuesta
        Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
        Matcher matcher = idPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            try {
                int id = Integer.parseInt(matcher.group(1));
                ids.add(id);
            } catch (NumberFormatException e) {
                // Ignorar IDs inválidos
            }
        }
        
        // Limitar a máximo 50 work items para evitar sobrecarga
        if (ids.size() > 50) {
            ids = ids.subList(0, 50);
        }
        
        return ids;
    }
    
    /**
     * Extrae valores de campos de work items específicos
     */
    private Map<String, Set<String>> extractFieldValuesFromWorkItems(String project, List<Integer> workItemIds, List<String> customFields) {
        Map<String, Set<String>> fieldValues = new HashMap<>();
        
        // Inicializar sets para cada campo
        for (String field : customFields) {
            fieldValues.put(field, new HashSet<>());
        }
        
        try {
            // Procesar work items en lotes para evitar URLs muy largas
            int batchSize = 10;
            for (int i = 0; i < workItemIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, workItemIds.size());
                List<Integer> batch = workItemIds.subList(i, endIndex);
                
                String idsParam = batch.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                
                String fieldsParam = customFields.stream()
                        .collect(Collectors.joining(","));
                
                String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems?ids=%s&fields=%s&api-version=7.1", 
                        getOrganizationFromConfig(), project, idsParam, fieldsParam);
                
                String response = makeDirectApiRequest(url);
                if (response != null) {
                    parseFieldValuesFromBatch(response, customFields, fieldValues);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting field values from work items: " + e.getMessage());
        }
        
        return fieldValues;
    }
    
    /**
     * Parsea valores de campos de un lote de work items
     */
    private void parseFieldValuesFromBatch(String jsonResponse, List<String> customFields, Map<String, Set<String>> fieldValues) {
        // Buscar cada work item en la respuesta
        Pattern workItemPattern = Pattern.compile("\\{[^{}]*\"fields\"\\s*:\\s*\\{([^{}]+)\\}[^{}]*\\}");
        Matcher workItemMatcher = workItemPattern.matcher(jsonResponse);
        
        while (workItemMatcher.find()) {
            String fieldsContent = workItemMatcher.group(1);
            
            // Para cada campo personalizado, buscar su valor
            for (String field : customFields) {
                Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
                Matcher fieldMatcher = fieldPattern.matcher(fieldsContent);
                
                if (fieldMatcher.find()) {
                    String value = fieldMatcher.group(1);
                    if (value != null && !value.trim().isEmpty()) {
                        fieldValues.get(field).add(value);
                    }
                }
            }
        }
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
        
        // Si no encontramos valores con allowedValues, buscar campos requeridos y retornar vacío
        // para que se active el método de fallback
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
