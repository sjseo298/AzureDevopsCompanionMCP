package com.mcp.server.service.workitem;

import com.mcp.server.config.OrganizationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Manejador genérico de campos de work items que reemplaza la lógica hardcodeada específica de Sura.
 * Utiliza configuración dinámica para adaptarse a cualquier organización.
 */
@Component
public class GenericWorkItemFieldsHandler {
    
    private final OrganizationConfigService configService;
    
    @Autowired
    public GenericWorkItemFieldsHandler(OrganizationConfigService configService) {
        this.configService = configService;
    }
    
    /**
     * Procesa los campos de un work item según la configuración organizacional.
     */
    public Map<String, Object> processWorkItemFields(String workItemType, Map<String, Object> inputFields) {
        Map<String, Object> processedFields = new HashMap<>();
        
        // Procesar cada campo de entrada
        for (Map.Entry<String, Object> entry : inputFields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            // Buscar mapeo para este campo
            Map<String, Object> fieldMapping = configService.getFieldMapping(fieldName);
            
            if (!fieldMapping.isEmpty()) {
                // Campo mapeado - aplicar transformación
                String azureFieldName = (String) fieldMapping.get("azureFieldName");
                Object convertedValue = configService.convertFieldValue(fieldName, fieldValue);
                
                if (azureFieldName != null) {
                    processedFields.put(azureFieldName, convertedValue);
                } else {
                    processedFields.put(fieldName, convertedValue);
                }
            } else {
                // Campo no mapeado - usar tal como está
                processedFields.put(fieldName, fieldValue);
            }
        }
        
        // Agregar campos requeridos con valores por defecto si no están presentes
        addRequiredFields(workItemType, processedFields);
        
        return processedFields;
    }
    
    /**
     * Agrega campos requeridos con valores por defecto si no están presentes.
     */
    private void addRequiredFields(String workItemType, Map<String, Object> fields) {
        List<String> requiredFields = configService.getRequiredFieldsForWorkItemType(workItemType);
        
        for (String requiredField : requiredFields) {
            Map<String, Object> fieldMapping = configService.getFieldMapping(requiredField);
            
            if (!fieldMapping.isEmpty()) {
                String azureFieldName = (String) fieldMapping.get("azureFieldName");
                Object defaultValue = fieldMapping.get("defaultValue");
                
                String targetFieldName = azureFieldName != null ? azureFieldName : requiredField;
                
                if (!fields.containsKey(targetFieldName) && defaultValue != null) {
                    fields.put(targetFieldName, defaultValue);
                }
            }
        }
    }
    
    /**
     * Valida que todos los campos requeridos estén presentes.
     */
    public ValidationResult validateRequiredFields(String workItemType, Map<String, Object> fields) {
        ValidationResult result = new ValidationResult();
        List<String> requiredFields = configService.getRequiredFieldsForWorkItemType(workItemType);
        
        for (String requiredField : requiredFields) {
            Map<String, Object> fieldMapping = configService.getFieldMapping(requiredField);
            
            if (!fieldMapping.isEmpty()) {
                String azureFieldName = (String) fieldMapping.get("azureFieldName");
                String targetFieldName = azureFieldName != null ? azureFieldName : requiredField;
                
                if (!fields.containsKey(targetFieldName) || fields.get(targetFieldName) == null) {
                    result.addError("Required field missing: " + requiredField);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Obtiene la lista de campos disponibles para un tipo de work item.
     */
    public List<String> getAvailableFields(String workItemType) {
        return configService.getRequiredFieldsForWorkItemType(workItemType);
    }
    
    /**
     * Obtiene información de ayuda para un campo específico.
     */
    public String getFieldHelpText(String fieldName) {
        return configService.getFieldHelpText(fieldName);
    }
    
    /**
     * Obtiene los valores permitidos para un campo específico.
     */
    public List<String> getAllowedValues(String fieldName) {
        return configService.getAllowedValues(fieldName);
    }
    
    /**
     * Verifica si un campo es requerido para un tipo de work item específico.
     */
    public boolean isFieldRequired(String workItemType, String fieldName) {
        return configService.isFieldRequired(workItemType, fieldName);
    }
    
    /**
     * Convierte valores específicos para tipos de work items de Sura.
     * Mantiene compatibilidad con los campos existentes.
     */
    public Map<String, Object> processSuraCompatibleFields(String workItemType, Map<String, Object> inputFields) {
        Map<String, Object> processedFields = processWorkItemFields(workItemType, inputFields);
        
        // Agregar lógica específica para tipos de Sura si es necesario
        switch (workItemType.toLowerCase()) {
            case "historia":
                return processHistoriaFields(processedFields);
            case "historia técnica":
            case "historia_tecnica":
                return processHistoriaTecnicaFields(processedFields);
            case "tarea":
                return processTareaFields(processedFields);
            case "bug":
                return processBugFields(processedFields);
            default:
                return processedFields;
        }
    }
    
    private Map<String, Object> processHistoriaFields(Map<String, Object> fields) {
        // Agregar campos específicos de Historia si están disponibles en la configuración
        return fields;
    }
    
    private Map<String, Object> processHistoriaTecnicaFields(Map<String, Object> fields) {
        // Agregar campos específicos de Historia Técnica si están disponibles en la configuración
        return fields;
    }
    
    private Map<String, Object> processTareaFields(Map<String, Object> fields) {
        // Agregar campos específicos de Tarea si están disponibles en la configuración
        return fields;
    }
    
    private Map<String, Object> processBugFields(Map<String, Object> fields) {
        // Agregar campos específicos de Bug si están disponibles en la configuración
        if (!fields.containsKey("Microsoft.VSTS.TCM.ReproSteps") && fields.containsKey("reproSteps")) {
            fields.put("Microsoft.VSTS.TCM.ReproSteps", fields.get("reproSteps"));
        }
        return fields;
    }
    
    /**
     * Clase para representar el resultado de una validación.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
