package com.mcp.server.utils.discovery;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utilidad especializada para validar campos personalizados y de sistema en Azure DevOps.
 * Centraliza toda la lógica de validación de campos y tipos de datos.
 */
public class AzureDevOpsFieldValidator {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsPicklistInvestigator picklistInvestigator;
    
    // Patrones para validación de campos
    private static final Pattern CUSTOM_FIELD_PATTERN = Pattern.compile("^Custom\\.[A-Za-z0-9_]+$");
    private static final Pattern SYSTEM_FIELD_PATTERN = Pattern.compile("^System\\.[A-Za-z0-9_]+$");
    private static final Pattern MICROSOFT_FIELD_PATTERN = Pattern.compile("^Microsoft\\.[A-Za-z0-9_\\.]+$");
    
    // Campos de sistema comunes conocidos
    private static final Set<String> KNOWN_SYSTEM_FIELDS = Set.of(
        "System.Id", "System.Title", "System.Description", "System.State", 
        "System.AssignedTo", "System.CreatedBy", "System.CreatedDate",
        "System.ChangedBy", "System.ChangedDate", "System.WorkItemType",
        "System.AreaPath", "System.IterationPath", "System.Tags",
        "System.Priority", "System.Severity", "System.Activity",
        "System.OriginalEstimate", "System.RemainingWork", "System.CompletedWork",
        "Microsoft.VSTS.Common.Priority", "Microsoft.VSTS.Common.Severity",
        "Microsoft.VSTS.Common.StateChangeDate", "Microsoft.VSTS.Common.ActivatedDate",
        "Microsoft.VSTS.Common.ResolvedDate", "Microsoft.VSTS.Common.ClosedDate",
        "Microsoft.VSTS.Scheduling.StoryPoints", "Microsoft.VSTS.Scheduling.Effort",
        "Microsoft.VSTS.Scheduling.RemainingWork", "Microsoft.VSTS.Scheduling.CompletedWork"
    );
    
    public AzureDevOpsFieldValidator(AzureDevOpsClient azureDevOpsClient, AzureDevOpsPicklistInvestigator picklistInvestigator) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.picklistInvestigator = picklistInvestigator;
    }
    
    /**
     * Valida la existencia y accesibilidad de múltiples campos en un proyecto.
     */
    public Map<String, FieldValidationResult> validateFieldsExistence(String project, String workItemType, List<String> fieldNames) {
        Map<String, FieldValidationResult> results = new HashMap<>();
        
        if (fieldNames == null || fieldNames.isEmpty()) {
            return results;
        }
        
        try {
            // Obtener definición del tipo de work item para validación contextual
            Map<String, Object> workItemTypeDefinition = null;
            if (workItemType != null) {
                workItemTypeDefinition = azureDevOpsClient.getWorkItemTypeDefinition(project, workItemType);
            }
            
            for (String fieldName : fieldNames) {
                if (fieldName == null || fieldName.trim().isEmpty()) {
                    results.put(fieldName, new FieldValidationResult(false, "Campo vacío o nulo", null));
                    continue;
                }
                
                FieldValidationResult result = validateSingleField(project, workItemType, fieldName, workItemTypeDefinition);
                results.put(fieldName, result);
            }
            
        } catch (Exception e) {
            // Si hay error general, marcar todos como no válidos
            for (String fieldName : fieldNames) {
                results.put(fieldName, new FieldValidationResult(false, "Error durante validación: " + e.getMessage(), null));
            }
        }
        
        return results;
    }
    
    /**
     * Valida si un campo personalizado tiene formato y acceso válidos.
     */
    public boolean isValidCustomField(String project, String workItemType, String fieldReferenceName) {
        if (fieldReferenceName == null || fieldReferenceName.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Verificar formato del campo personalizado
            if (!CUSTOM_FIELD_PATTERN.matcher(fieldReferenceName).matches()) {
                return false;
            }
            
            // Intentar obtener definición del campo
            Map<String, Object> fieldDefinition = picklistInvestigator.getCompleteFieldDefinition(project, fieldReferenceName);
            
            // Si obtuvimos definición, el campo existe
            return fieldDefinition != null && !fieldDefinition.isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Obtiene información detallada sobre campos personalizados para un tipo de work item.
     */
    public List<CustomFieldInfo> getCustomFieldsForWorkItemType(String project, String workItemType) {
        List<CustomFieldInfo> customFields = new ArrayList<>();
        
        try {
            Map<String, Object> typeDefinition = azureDevOpsClient.getWorkItemTypeDefinition(project, workItemType);
            
            if (typeDefinition != null) {
                customFields = extractCustomFieldsFromTypeDefinition(project, typeDefinition);
            }
            
        } catch (Exception e) {
            // Error silencioso - retornar lista vacía
        }
        
        return customFields;
    }
    
    /**
     * Detecta el tipo de datos de un campo basado en su definición y valores.
     */
    public FieldTypeInfo detectFieldType(String project, String workItemType, String fieldReferenceName) {
        try {
            Map<String, Object> fieldDefinition = picklistInvestigator.getCompleteFieldDefinition(project, fieldReferenceName);
            
            if (fieldDefinition.isEmpty()) {
                return new FieldTypeInfo("unknown", "Campo no encontrado", Collections.emptyList());
            }
            
            String systemType = (String) fieldDefinition.get("type");
            String inferredType = (String) fieldDefinition.get("inferredType");
            
            // Obtener valores permitidos si los hay
            List<String> allowedValues = picklistInvestigator.getFieldAllowedValues(project, workItemType, fieldReferenceName);
            
            // Determinar tipo final
            String finalType = determineFieldType(systemType, inferredType, allowedValues);
            String description = buildFieldTypeDescription(systemType, inferredType, allowedValues);
            
            return new FieldTypeInfo(finalType, description, allowedValues);
            
        } catch (Exception e) {
            return new FieldTypeInfo("error", "Error detectando tipo: " + e.getMessage(), Collections.emptyList());
        }
    }
    
    /**
     * Verifica si un campo es requerido para un tipo de work item específico.
     */
    public boolean isFieldRequired(String project, String workItemType, String fieldReferenceName) {
        try {
            Map<String, Object> typeDefinition = azureDevOpsClient.getWorkItemTypeDefinition(project, workItemType);
            
            if (typeDefinition != null) {
                String typeDefString = typeDefinition.toString();
                
                // Buscar el campo en la definición y verificar si es requerido
                String fieldPattern = "\"referenceName\":\"" + fieldReferenceName.replace(".", "\\.") + "\"";
                if (typeDefString.contains(fieldPattern)) {
                    // Buscar en el contexto del campo si es requerido
                    int fieldIndex = typeDefString.indexOf(fieldPattern);
                    String fieldContext = typeDefString.substring(Math.max(0, fieldIndex - 200), Math.min(typeDefString.length(), fieldIndex + 500));
                    
                    return fieldContext.contains("\"required\":true") || fieldContext.contains("\"alwaysRequired\":true");
                }
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return false;
    }
    
    // ========================================================================
    // MÉTODOS PRIVADOS DE APOYO
    // ========================================================================
    
    private FieldValidationResult validateSingleField(String project, String workItemType, String fieldName, Map<String, Object> workItemTypeDefinition) {
        try {
            // Verificar si es campo de sistema conocido
            if (KNOWN_SYSTEM_FIELDS.contains(fieldName)) {
                return new FieldValidationResult(true, "Campo de sistema conocido", "system");
            }
            
            // Verificar formato de campo personalizado
            if (CUSTOM_FIELD_PATTERN.matcher(fieldName).matches()) {
                boolean exists = isValidCustomField(project, workItemType, fieldName);
                return new FieldValidationResult(exists, 
                    exists ? "Campo personalizado válido" : "Campo personalizado no encontrado", 
                    "custom");
            }
            
            // Verificar formato de campo de sistema
            if (SYSTEM_FIELD_PATTERN.matcher(fieldName).matches() || MICROSOFT_FIELD_PATTERN.matcher(fieldName).matches()) {
                Map<String, Object> fieldDefinition = picklistInvestigator.getCompleteFieldDefinition(project, fieldName);
                boolean exists = !fieldDefinition.isEmpty();
                return new FieldValidationResult(exists, 
                    exists ? "Campo de sistema válido" : "Campo de sistema no encontrado", 
                    "system");
            }
            
            // Campo con formato no reconocido
            return new FieldValidationResult(false, "Formato de campo no reconocido", "unknown");
            
        } catch (Exception e) {
            return new FieldValidationResult(false, "Error validando campo: " + e.getMessage(), "error");
        }
    }
    
    private List<CustomFieldInfo> extractCustomFieldsFromTypeDefinition(String project, Map<String, Object> typeDefinition) {
        List<CustomFieldInfo> customFields = new ArrayList<>();
        
        try {
            String typeDefString = typeDefinition.toString();
            
            // Buscar patrones de campos personalizados en la definición
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"referenceName\":\"(Custom\\.[^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(typeDefString);
            
            Set<String> foundFields = new HashSet<>();
            
            while (matcher.find()) {
                String fieldName = matcher.group(1);
                if (!foundFields.contains(fieldName)) {
                    foundFields.add(fieldName);
                    
                    // Obtener información adicional del campo
                    FieldTypeInfo typeInfo = detectFieldType(project, null, fieldName);
                    boolean required = isFieldRequired(project, null, fieldName);
                    
                    customFields.add(new CustomFieldInfo(fieldName, typeInfo.type(), typeInfo.description(), required, typeInfo.allowedValues()));
                }
            }
            
        } catch (Exception e) {
            // Error silencioso
        }
        
        return customFields;
    }
    
    private String determineFieldType(String systemType, String inferredType, List<String> allowedValues) {
        if (inferredType != null && !inferredType.isEmpty()) {
            return inferredType;
        }
        
        if (!allowedValues.isEmpty()) {
            return "stringWithAllowedValues";
        }
        
        if (systemType != null) {
            return mapSystemTypeToFinalType(systemType);
        }
        
        return "string";
    }
    
    private String mapSystemTypeToFinalType(String systemType) {
        return switch (systemType.toLowerCase()) {
            case "string" -> "string";
            case "integer" -> "integer";
            case "double", "decimal" -> "double";
            case "datetime" -> "dateTime";
            case "boolean" -> "boolean";
            case "pickliststring" -> "picklistString";
            case "html" -> "html";
            case "plaintext" -> "plainText";
            case "identity" -> "identity";
            case "treepath" -> "treePath";
            default -> systemType;
        };
    }
    
    private String buildFieldTypeDescription(String systemType, String inferredType, List<String> allowedValues) {
        StringBuilder description = new StringBuilder();
        
        if (systemType != null) {
            description.append("Tipo sistema: ").append(systemType);
        }
        
        if (inferredType != null && !inferredType.equals(systemType)) {
            if (description.length() > 0) description.append(", ");
            description.append("Tipo inferido: ").append(inferredType);
        }
        
        if (!allowedValues.isEmpty()) {
            if (description.length() > 0) description.append(", ");
            description.append("Valores permitidos: ").append(allowedValues.size()).append(" opciones");
        }
        
        return description.length() > 0 ? description.toString() : "Campo de texto genérico";
    }
    
    // ========================================================================
    // CLASES DE RESULTADO
    // ========================================================================
    
    public record FieldValidationResult(boolean isValid, String message, String category) {}
    
    public record FieldTypeInfo(String type, String description, List<String> allowedValues) {}
    
    public record CustomFieldInfo(String referenceName, String type, String description, boolean required, List<String> allowedValues) {}
}
