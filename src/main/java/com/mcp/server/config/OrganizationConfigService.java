package com.mcp.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio para manejar la configuración organizacional de manera genérica.
 * Reemplaza la lógica hardcodeada específica de Sura con configuración dinámica.
 */
@Service
public class OrganizationConfigService {
    
    @Value("${app.config.path:config/}")
    private String configPath;
    
    @Value("${AZUREDEVOPS_ORGANIZATION:suramericana}")
    private String azureDevOpsOrganization;
    
    private final Map<String, Map<String, Object>> fieldMappings;
    
    public OrganizationConfigService() {
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
        
        // Campos específicos de Sura (compatibilidad)
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
     */
    public Object convertFieldValue(String fieldName, Object value) {
        // Por ahora, conversión básica - se puede extender
        if (value instanceof String) {
            String stringValue = (String) value;
            
            // Conversiones comunes
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                return Boolean.parseBoolean(stringValue);
            }
            
            // Intentar conversión numérica
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                // Mantener como string
                return value;
            }
        }
        
        return value;
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
                return List.of("title", "description", "state");
            default:
                return List.of("title", "state");
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
     */
    public List<String> getAllowedValues(String fieldName) {
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
     * Verifica si un campo es requerido para un tipo específico de work item.
     */
    public boolean isFieldRequired(String workItemType, String fieldName) {
        List<String> requiredFields = getRequiredFieldsForWorkItemType(workItemType);
        return requiredFields.stream()
            .anyMatch(field -> field.equalsIgnoreCase(fieldName));
    }
}