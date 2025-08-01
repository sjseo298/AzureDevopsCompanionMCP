package com.mcp.server.config;

import com.mcp.server.tools.azuredevops.DiscoverOrganizationTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio para manejar la configuración organizacional de manera genérica.
 * Lee configuración dinámica desde archivos YAML para adaptarse a cualquier organización.
 */
@Service
public class OrganizationConfigService {
    
    @Value("${app.config.path:config/}")
    private String configPath;
    
    @Value("${AZUREDEVOPS_ORGANIZATION:organization}")
    private String azureDevOpsOrganization;
    
    private final Map<String, Map<String, Object>> fieldMappings;
    private final OrganizationContextService contextService;
    private final DiscoverOrganizationTool discoverTool;
    
    // Constructor para uso en producción con inyección de dependencias
    @Autowired
    public OrganizationConfigService(OrganizationContextService contextService, @Lazy DiscoverOrganizationTool discoverTool) {
        this.contextService = contextService;
        this.discoverTool = discoverTool;
        this.fieldMappings = createDefaultFieldMappings();
    }
    
    // Constructor sin parámetros para mantener compatibilidad con tests existentes
    public OrganizationConfigService() {
        this.contextService = null;
        this.discoverTool = null;
        this.fieldMappings = createDefaultFieldMappings();
    }
    
    /**
     * Crea mapeos de campos por defecto.
     */
    private Map<String, Map<String, Object>> createDefaultFieldMappings() {
        Map<String, Map<String, Object>> mappings = new HashMap<>();
        
        // Campos básicos
        mappings.put("title", Map.of(
            "azureFieldName", "System.Title",
            "required", true,
            "type", "string"
        ));
        
        mappings.put("description", Map.of(
            "azureFieldName", "System.Description",
            "required", false,
            "type", "html"
        ));
        
        mappings.put("assignedTo", Map.of(
            "azureFieldName", "System.AssignedTo",
            "required", false,
            "type", "identity"
        ));
        
        mappings.put("state", Map.of(
            "azureFieldName", "System.State",
            "required", true,
            "type", "string",
            "defaultValue", "New"
        ));
        
        mappings.put("priority", Map.of(
            "azureFieldName", "Microsoft.VSTS.Common.Priority",
            "required", false,
            "type", "integer",
            "defaultValue", 2
        ));
        
        mappings.put("tags", Map.of(
            "azureFieldName", "System.Tags",
            "required", false,
            "type", "plainText"
        ));
        
        // Campos organizacionales (compatibilidad)
        mappings.put("tipoHistoria", Map.of(
            "azureFieldName", "Custom.TipoHistoria",
            "required", false,
            "type", "string"
        ));
        
        mappings.put("acceptanceCriteria", Map.of(
            "azureFieldName", "Microsoft.VSTS.Common.AcceptanceCriteria",
            "required", false,
            "type", "html"
        ));
        
        mappings.put("reproSteps", Map.of(
            "azureFieldName", "Microsoft.VSTS.TCM.ReproSteps",
            "required", false,
            "type", "html"
        ));
        
        return mappings;
    }
    
    /**
     * Obtiene la configuración organizacional por defecto.
     */
    public Map<String, Object> getDefaultOrganizationConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("organization", azureDevOpsOrganization);
        config.put("defaultProject", "DefaultProject");
        config.put("defaultTeam", "DefaultTeam");
        config.put("timeZone", "America/Bogota");
        config.put("language", "es-CO");
        return config;
    }
    
    /**
     * Obtiene el mapeo de un campo específico.
     */
    public Map<String, Object> getFieldMapping(String fieldName) {
        return fieldMappings.getOrDefault(fieldName, Map.of());
    }
    
    /**
     * Convierte un valor de campo según la configuración.
     * IMPORTANTE: Azure DevOps API requiere que todos los valores sean Strings,
     * por lo que convertimos a String después de validar el tipo.
     */
    public Object convertFieldValue(String fieldName, Object value) {
        System.out.println("DEBUG: convertFieldValue called with fieldName=" + fieldName + ", value=" + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
        
        if (value == null) {
            return null;
        }
        
        // Obtener información del mapeo de campo para determinar el tipo esperado
        Map<String, Object> fieldMapping = getFieldMapping(fieldName);
        String expectedType = (String) fieldMapping.get("type");
        
        System.out.println("DEBUG: expectedType=" + expectedType + " for field=" + fieldName);
        
        // Si no hay mapeo o tipo definido, devolver como String
        if (expectedType == null) {
            String result = value.toString();
            System.out.println("DEBUG: No expectedType, returning toString(): " + result);
            return result;
        }
        
        // Conversiones seguras basadas en el tipo esperado, pero siempre devolviendo String
        switch (expectedType.toLowerCase()) {
            case "boolean":
                boolean boolValue;
                if (value instanceof String) {
                    boolValue = Boolean.parseBoolean((String) value);
                } else if (value instanceof Boolean) {
                    boolValue = (Boolean) value;
                } else {
                    boolValue = value != null && !value.toString().isEmpty() && !"0".equals(value.toString());
                }
                String boolResult = String.valueOf(boolValue);
                System.out.println("DEBUG: Boolean conversion result: " + boolResult);
                return boolResult;
                
            case "integer":
            case "number":
                if (value instanceof Number) {
                    String intResult = String.valueOf(((Number) value).intValue());
                    System.out.println("DEBUG: Integer conversion result: " + intResult);
                    return intResult;
                } else if (value instanceof String) {
                    try {
                        int intValue = Integer.parseInt((String) value);
                        String intResult = String.valueOf(intValue);
                        System.out.println("DEBUG: String to Integer conversion result: " + intResult);
                        return intResult;
                    } catch (NumberFormatException e) {
                        String fallbackResult = value.toString();
                        System.out.println("DEBUG: Integer parse failed, fallback result: " + fallbackResult);
                        return fallbackResult;
                    }
                }
                String intFallbackResult = value.toString();
                System.out.println("DEBUG: Integer fallback result: " + intFallbackResult);
                return intFallbackResult;
                
            case "double":
                if (value instanceof Number) {
                    String doubleResult = String.valueOf(((Number) value).doubleValue());
                    System.out.println("DEBUG: Double conversion result: " + doubleResult);
                    return doubleResult;
                } else if (value instanceof String) {
                    try {
                        double doubleValue = Double.parseDouble((String) value);
                        String doubleResult = String.valueOf(doubleValue);
                        System.out.println("DEBUG: String to Double conversion result: " + doubleResult);
                        return doubleResult;
                    } catch (NumberFormatException e) {
                        String fallbackResult = value.toString();
                        System.out.println("DEBUG: Double parse failed, fallback result: " + fallbackResult);
                        return fallbackResult;
                    }
                }
                String doubleFallbackResult = value.toString();
                System.out.println("DEBUG: Double fallback result: " + doubleFallbackResult);
                return doubleFallbackResult;
                
            case "string":
            case "html":
            case "pickliststring":
            case "datetime":
            case "identity":
                // Para todos los tipos de texto, convertir a String
                String stringResult = value.toString();
                System.out.println("DEBUG: String conversion result: " + stringResult);
                return stringResult;
                
            default:
                // Para tipos desconocidos, convertir a String
                String defaultResult = value.toString();
                System.out.println("DEBUG: Default conversion result: " + defaultResult);
                return defaultResult;
        }
    }
    
    /**
     * Obtiene los campos requeridos para un tipo específico de work item.
     */
    public List<String> getRequiredFieldsForWorkItemType(String workItemType) {
        switch (workItemType.toLowerCase()) {
            case "task":
            case "tarea":
                return List.of("title", "description", "state");
            case "user story":
            case "historia":
                return List.of("title", "description", "acceptanceCriteria", "state");
            case "historia técnica":
            case "historia_tecnica":
                return List.of("title", "description", "state");
            case "bug":
                return List.of("title", "description", "reproSteps", "state");
            case "feature":
                return List.of("title");  // Feature solo requiere título
            default:
                return List.of("title");  // Campos mínimos para tipos desconocidos
        }
    }
    
    /**
     * Obtiene texto de ayuda para un campo específico.
     */
    public String getFieldHelpText(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "title":
                return "Título descriptivo del work item";
            case "description":
                return "Descripción detallada del work item";
            case "acceptancecriteria":
                return "Criterios de aceptación para la historia";
            case "reprosteps":
                return "Pasos para reproducir el problema";
            case "assignedto":
                return "Usuario asignado al work item";
            case "state":
                return "Estado actual del work item (New, Active, Done, etc.)";
            case "priority":
                return "Prioridad del work item (1=High, 4=Low)";
            case "tags":
                return "Etiquetas separadas por punto y coma";
            default:
                return "Campo personalizado";
        }
    }
    
    /**
     * Obtiene los valores permitidos para un campo específico.
     * Soporta valores dinámicos desde Azure DevOps usando @DYNAMIC_FROM_AZURE_DEVOPS.
     */
    public List<String> getAllowedValues(String fieldName) {
        // Si tenemos dependencias inyectadas, intentar obtener desde la configuración YAML
        if (contextService != null) {
            Map<String, Object> fieldMappingConfig = contextService.getFieldMappingConfig();
            
            if (fieldMappingConfig.containsKey("fieldMappings")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldMappings = (Map<String, Object>) fieldMappingConfig.get("fieldMappings");
                
                if (fieldMappings.containsKey(fieldName)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldConfig = (Map<String, Object>) fieldMappings.get(fieldName);
                    
                    if (fieldConfig.containsKey("allowedValues")) {
                        Object allowedValuesObj = fieldConfig.get("allowedValues");
                        
                        // Si es la directiva especial, obtener valores dinámicamente
                        if ("@DYNAMIC_FROM_AZURE_DEVOPS".equals(allowedValuesObj) && discoverTool != null) {
                            return getDynamicAllowedValues(fieldName, fieldConfig);
                        }
                        
                        // Si es una lista estática
                        if (allowedValuesObj instanceof List<?>) {
                            @SuppressWarnings("unchecked")
                            List<?> rawValues = (List<?>) allowedValuesObj;
                            List<String> result = new ArrayList<>();
                            
                            // Convertir todos los valores a String para evitar problemas de casting
                            for (Object value : rawValues) {
                                if (value != null) {
                                    result.add(value.toString());
                                }
                            }
                            
                            return result;
                        }
                    }
                }
            }
        }
        
        // Fallback a valores hardcodeados para campos básicos
        switch (fieldName.toLowerCase()) {
            case "state":
                return List.of("New", "Active", "Resolved", "Closed", "Done");
            case "priority":
                return List.of("1", "2", "3", "4");
            case "reason":
                return List.of("New", "Work finished", "Fixed", "Deferred");
            default:
                return List.of();
        }
    }
    
    /**
     * Obtiene valores permitidos dinámicamente desde Azure DevOps.
     */
    private List<String> getDynamicAllowedValues(String fieldName, Map<String, Object> fieldConfig) {
        if (contextService == null || discoverTool == null) {
            System.err.println("No se pueden obtener valores dinámicos - dependencias no inyectadas");
            return List.of();
        }
        
        try {
            String azureFieldName = (String) fieldConfig.get("azureFieldName");
            if (azureFieldName == null) {
                return List.of();
            }
            
            // Obtener el proyecto primario desde la configuración
            String primaryProject = getPrimaryProject();
            if (primaryProject == null) {
                return List.of();
            }
            
            // Determinar el tipo de work item basado en el campo
            String workItemType = determineWorkItemTypeFromField(azureFieldName);
            
            // Llamar al DiscoverOrganizationTool para obtener valores dinámicos
            return getFieldAllowedValuesFromAzure(primaryProject, workItemType, azureFieldName);
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores dinámicos para campo " + fieldName + ": " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtiene el proyecto primario desde la configuración organizacional.
     */
    private String getPrimaryProject() {
        if (contextService == null) {
            return "Gerencia_Tecnologia"; // Fallback
        }
        
        Map<String, Object> discoveredConfig = contextService.getDiscoveredConfig();
        
        if (discoveredConfig.containsKey("projects")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projects = (List<Map<String, Object>>) discoveredConfig.get("projects");
            
            if (!projects.isEmpty()) {
                // Buscar el proyecto "Gerencia_Tecnologia" o tomar el primero
                return projects.stream()
                    .map(project -> (String) project.get("name"))
                    .filter(name -> "Gerencia_Tecnologia".equals(name))
                    .findFirst()
                    .orElse((String) projects.get(0).get("name"));
            }
        }
        
        return "Gerencia_Tecnologia"; // Fallback
    }
    
    /**
     * Determina el tipo de work item más apropiado basado en el campo.
     */
    private String determineWorkItemTypeFromField(String azureFieldName) {
        // Mapeo basado en los campos más comunes por tipo
        if (azureFieldName.contains("TipoDeHistoria")) {
            return "Historia";
        } else if (azureFieldName.contains("TipoDeHistoriaTecnica")) {
            return "Historia técnica";
        } else if (azureFieldName.contains("TipoDeTarea")) {
            return "Tarea";
        } else if (azureFieldName.contains("TipoDeSubtarea")) {
            return "Subtarea";
        } else if (azureFieldName.contains("NivelPrueba") || azureFieldName.contains("Origen") || azureFieldName.contains("EtapaDescubrimiento")) {
            return "Bug";
        } else if (azureFieldName.contains("TipoEjecucion") || azureFieldName.contains("Fase")) {
            return "Caso de prueba";
        } else if (azureFieldName.contains("Probabilidad") || azureFieldName.contains("Impacto")) {
            return "Riesgo";
        } else if (azureFieldName.contains("TipoDeProyecto")) {
            return "Proyecto";
        } else if (azureFieldName.contains("ResultadoRevision")) {
            return "Revisión post implantación";
        }
        
        return "Task"; // Fallback genérico
    }
    
    /**
     * Obtiene valores permitidos desde Azure DevOps usando el DiscoverOrganizationTool.
     */
    private List<String> getFieldAllowedValuesFromAzure(String project, String workItemType, String azureFieldName) {
        try {
            // Determinar el tipo de campo basado en el nombre del campo
            String fieldType = determineFieldType(azureFieldName);
            
            // Llamar al método público del DiscoverOrganizationTool
            return discoverTool.getFieldAllowedValues(project, workItemType, azureFieldName, fieldType);
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores desde Azure DevOps para campo " + azureFieldName + ": " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Determina el tipo de campo basado en el nombre del campo de Azure DevOps.
     */
    private String determineFieldType(String azureFieldName) {
        if (azureFieldName.startsWith("Custom.") && 
            (azureFieldName.contains("Tipo") || azureFieldName.contains("Nivel") || 
             azureFieldName.contains("Origen") || azureFieldName.contains("Etapa") ||
             azureFieldName.contains("Fase") || azureFieldName.contains("Resultado"))) {
            return "picklistString";
        } else if (azureFieldName.equals("System.State")) {
            return "string";
        } else if (azureFieldName.contains("Priority")) {
            return "integer";
        }
        
        return "string"; // Fallback
    }
    
    /**
     * Verifica si un campo es requerido para un tipo específico de work item.
     */
    public boolean isFieldRequired(String workItemType, String fieldName) {
        List<String> requiredFields = getRequiredFieldsForWorkItemType(workItemType);
        return requiredFields.stream()
            .anyMatch(field -> field.equalsIgnoreCase(fieldName));
    }
    
    /**
     * Obtiene todos los mapeos de campos desde la configuración organizacional.
     */
    public Map<String, Object> getAllFieldMappings() {
        try {
            Map<String, Object> config = contextService.getFieldMappingConfig();
            if (config != null && config.containsKey("fieldMappings")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldMappings = (Map<String, Object>) config.get("fieldMappings");
                return fieldMappings != null ? fieldMappings : new HashMap<>();
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo mapeos de campos: " + e.getMessage());
        }
        
        return new HashMap<>();
    }
}