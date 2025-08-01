package com.mcp.server.service.workitem;

import com.mcp.server.config.OrganizationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Manejador genérico de campos de work items con configuración dinámica.
 * Utiliza configuración organizacional para adaptarse a cualquier organización.
 */
@Component
public class GenericWorkItemFieldsHandler {
    
    private final OrganizationConfigService configService;
    
    @Autowired
    public GenericWorkItemFieldsHandler(OrganizationConfigService configService) {
        this.configService = configService;
    }
    
        /**
     * Procesa campos de work item aplicando configuración organizacional.
     */
    public Map<String, Object> processWorkItemFields(String workItemType, Map<String, Object> inputFields) {
        System.out.println("DEBUG GenericWorkItemFieldsHandler: processWorkItemFields called with workItemType=" + workItemType);
        System.out.println("DEBUG GenericWorkItemFieldsHandler: inputFields=" + inputFields);
        
        Map<String, Object> processedFields = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : inputFields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            System.out.println("DEBUG GenericWorkItemFieldsHandler: Processing field=" + fieldName + ", value=" + fieldValue + " (type: " + (fieldValue != null ? fieldValue.getClass().getSimpleName() : "null") + ")");
            
            // Buscar mapeo para este campo
            Map<String, Object> fieldMapping = configService.getFieldMapping(fieldName);
            
            if (!fieldMapping.isEmpty()) {
                // Campo mapeado - aplicar transformación
                String azureFieldName = (String) fieldMapping.get("azureFieldName");
                Object convertedValue = configService.convertFieldValue(fieldName, fieldValue);
                
                System.out.println("DEBUG GenericWorkItemFieldsHandler: Field mapped. azureFieldName=" + azureFieldName + ", convertedValue=" + convertedValue + " (type: " + (convertedValue != null ? convertedValue.getClass().getSimpleName() : "null") + ")");
                
                if (azureFieldName != null) {
                    processedFields.put(azureFieldName, convertedValue);
                } else {
                    processedFields.put(fieldName, convertedValue);
                }
            } else {
                // Campo no mapeado - usar tal como está
                System.out.println("DEBUG GenericWorkItemFieldsHandler: Field not mapped, using as-is");
                processedFields.put(fieldName, fieldValue);
            }
        }
        
        // Agregar campos requeridos con valores por defecto
        addRequiredFields(workItemType, processedFields);
        
        System.out.println("DEBUG GenericWorkItemFieldsHandler: Final processedFields=" + processedFields);
        return processedFields;
    }
    
    /**
     * Agrega campos requeridos con valores por defecto si no están presentes.
     */
    private void addRequiredFields(String workItemType, Map<String, Object> fields) {
        System.out.println("DEBUG GenericWorkItemFieldsHandler: addRequiredFields called with workItemType=" + workItemType);
        
        List<String> requiredFields = configService.getRequiredFieldsForWorkItemType(workItemType);
        System.out.println("DEBUG GenericWorkItemFieldsHandler: requiredFields=" + requiredFields);
        
        for (String requiredField : requiredFields) {
            System.out.println("DEBUG GenericWorkItemFieldsHandler: Processing required field=" + requiredField);
            
            Map<String, Object> fieldMapping = configService.getFieldMapping(requiredField);
            System.out.println("DEBUG GenericWorkItemFieldsHandler: fieldMapping=" + fieldMapping);
            
            if (!fieldMapping.isEmpty()) {
                String azureFieldName = (String) fieldMapping.get("azureFieldName");
                Object defaultValue = fieldMapping.get("defaultValue");
                
                System.out.println("DEBUG GenericWorkItemFieldsHandler: azureFieldName=" + azureFieldName + ", defaultValue=" + defaultValue + " (type: " + (defaultValue != null ? defaultValue.getClass().getSimpleName() : "null") + ")");
                
                String targetFieldName = azureFieldName != null ? azureFieldName : requiredField;
                
                if (!fields.containsKey(targetFieldName) && defaultValue != null) {
                    System.out.println("DEBUG GenericWorkItemFieldsHandler: Adding default value for field=" + targetFieldName);
                    // Aplicar conversión de tipo al valor por defecto
                    Object convertedDefaultValue = configService.convertFieldValue(requiredField, defaultValue);
                    System.out.println("DEBUG GenericWorkItemFieldsHandler: convertedDefaultValue=" + convertedDefaultValue + " (type: " + (convertedDefaultValue != null ? convertedDefaultValue.getClass().getSimpleName() : "null") + ")");
                    fields.put(targetFieldName, convertedDefaultValue);
                } else {
                    System.out.println("DEBUG GenericWorkItemFieldsHandler: Field already exists or no default value. fields.containsKey=" + fields.containsKey(targetFieldName) + ", defaultValue=" + defaultValue);
                }
            } else {
                System.out.println("DEBUG GenericWorkItemFieldsHandler: No fieldMapping found for required field=" + requiredField);
            }
        }
        
        System.out.println("DEBUG GenericWorkItemFieldsHandler: Final fields after addRequiredFields=" + fields);
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
     * Obtiene todos los campos disponibles con sus detalles para el schema dinámico.
     */
    public Map<String, Object> getAvailableFieldsWithDetails() {
        Map<String, Object> fieldsWithDetails = new HashMap<>();
        
        // Obtener todos los mapeos de campos desde la configuración
        Map<String, Object> allFieldMappings = configService.getAllFieldMappings();
        
        for (Map.Entry<String, Object> entry : allFieldMappings.entrySet()) {
            String fieldName = entry.getKey();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldConfig = (Map<String, Object>) entry.getValue();
            
            // Crear la definición del campo para el schema
            Map<String, Object> fieldDefinition = new HashMap<>();
            
            // Determinar el tipo del campo
            String fieldType = (String) fieldConfig.getOrDefault("type", "string");
            if ("boolean".equals(fieldType)) {
                fieldDefinition.put("type", "boolean");
            } else if ("integer".equals(fieldType) || "number".equals(fieldType)) {
                fieldDefinition.put("type", "number");
            } else {
                fieldDefinition.put("type", "string");
            }
            
            // Construir la descripción dinámicamente
            StringBuilder description = new StringBuilder();
            String helpText = (String) fieldConfig.get("helpText");
            if (helpText != null && !helpText.isEmpty()) {
                description.append(helpText);
            }
            
            // Agregar información sobre si es requerido
            Boolean required = (Boolean) fieldConfig.get("required");
            if (Boolean.TRUE.equals(required)) {
                if (description.length() > 0) {
                    description.append(" ");
                }
                description.append("(Obligatorio)");
            }
            
            // Agregar valores permitidos si existen
            List<String> allowedValues = getAllowedValues(fieldName);
            if (!allowedValues.isEmpty()) {
                if (description.length() > 0) {
                    description.append(". ");
                }
                description.append("Valores permitidos: ").append(String.join(", ", allowedValues));
            }
            
            if (description.length() > 0) {
                fieldDefinition.put("description", description.toString());
            } else {
                fieldDefinition.put("description", fieldName);
            }
            
            fieldsWithDetails.put(fieldName, fieldDefinition);
        }
        
        return fieldsWithDetails;
    }

    /**
     * Verifica si un campo es requerido para un tipo de work item específico.
     */
    public boolean isFieldRequired(String workItemType, String fieldName) {
        return configService.isFieldRequired(workItemType, fieldName);
    }
    
    /**
     * Convierte valores específicos para tipos de work items organizacionales.
     * Mantiene compatibilidad con los campos existentes.
     */
    public Map<String, Object> processOrganizationCompatibleFields(String workItemType, Map<String, Object> inputFields) {
        Map<String, Object> processedFields = processWorkItemFields(workItemType, inputFields);
        
        // Agregar lógica específica para tipos organizacionales si es necesario
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
