package com.mcp.server.utils.discovery;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.GetWorkItemTypesTool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilidad completa para realizar investigaciones organizacionales de Azure DevOps.
 * Combina todas las utilidades especializadas para generar informes completos.
 */
public class AzureDevOpsOrganizationInvestigator {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsPicklistInvestigator picklistInvestigator;
    private final AzureDevOpsFieldValidator fieldValidator;
    private final AzureDevOpsWiqlUtility wiqlUtility;
    private final GetWorkItemTypesTool workItemTypesTool;
    
    public AzureDevOpsOrganizationInvestigator(AzureDevOpsClient azureDevOpsClient, GetWorkItemTypesTool workItemTypesTool) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.workItemTypesTool = workItemTypesTool;
        
        // Inicializar utilidades especializadas
        this.picklistInvestigator = new AzureDevOpsPicklistInvestigator(azureDevOpsClient);
        this.fieldValidator = new AzureDevOpsFieldValidator(azureDevOpsClient, picklistInvestigator);
        this.wiqlUtility = new AzureDevOpsWiqlUtility(azureDevOpsClient);
    }
    
    /**
     * Realiza una investigación completa de la organización para un proyecto específico.
     */
    public OrganizationFieldInvestigation performCompleteInvestigation(String project) {
        OrganizationFieldInvestigation investigation = new OrganizationFieldInvestigation();
        investigation.setProject(project);
        investigation.setInvestigationDate(new Date());
        
        try {
            // Paso 1: Obtener tipos de work items
            List<WorkItemTypeDefinition> workItemTypes = investigateWorkItemTypes(project);
            investigation.setWorkItemTypes(workItemTypes);
            
            // Paso 2: Investigar todos los campos
            List<FieldDefinition> allFields = investigateAllFields(project, workItemTypes);
            investigation.setAllFields(allFields);
            
            // Paso 3: Filtrar campos personalizados  
            List<FieldDefinition> customFields = allFields.stream()
                .filter(field -> field.getReferenceName() != null && field.getReferenceName().startsWith("Custom."))
                .collect(Collectors.toList());
            investigation.setCustomFields(customFields);
            
            // Paso 4: Investigar valores de picklist
            Map<String, List<String>> picklistValues = investigatePicklistValues(project, customFields);
            investigation.setPicklistValues(picklistValues);
            
        } catch (Exception e) {
            // En caso de error, retornar investigación parcial
            System.err.println("Error durante investigación organizacional: " + e.getMessage());
        }
        
        return investigation;
    }
    
    /**
     * Genera un reporte detallado de la investigación.
     */
    public String generateDetailedReport(OrganizationFieldInvestigation investigation) {
        StringBuilder report = new StringBuilder();
        
        report.append("🏢 **REPORTE INVESTIGACIÓN ORGANIZACIONAL**\n");
        report.append("==========================================\n\n");
        
        // Información general
        report.append("📊 **RESUMEN EJECUTIVO**\n");
        OrganizationFieldInvestigation.InvestigationSummary summary = investigation.getSummary();
        report.append("   • Proyecto: ").append(investigation.getProject()).append("\n");
        report.append("   • Fecha: ").append(investigation.getInvestigationDate()).append("\n");
        report.append("   • Tipos de Work Items: ").append(summary.getTotalWorkItemTypes()).append("\n");
        report.append("   • Total de Campos: ").append(summary.getTotalFields()).append("\n");
        report.append("   • Campos Personalizados: ").append(summary.getCustomFieldsFound()).append("\n");
        report.append("   • Campos con Picklist: ").append(summary.getPicklistFieldsFound()).append("\n");
        report.append("   • Campos de Fecha: ").append(summary.getDateFieldsFound()).append("\n\n");
        
        // Detalles de tipos de work items
        report.append("📋 **TIPOS DE WORK ITEMS ENCONTRADOS**\n");
        report.append("=====================================\n");
        for (WorkItemTypeDefinition type : investigation.getWorkItemTypes()) {
            report.append("   • ").append(type.getTypeName());
            if (type.getDescription() != null) {
                report.append(" - ").append(type.getDescription());
            }
            report.append("\n");
        }
        report.append("\n");
        
        // Campos personalizados críticos
        report.append("🔧 **CAMPOS PERSONALIZADOS CRÍTICOS**\n");
        report.append("====================================\n");
        for (FieldDefinition field : investigation.getCustomFields()) {
            report.append("   • ").append(field.getName()).append("\n");
            report.append("     - Referencia: ").append(field.getReferenceName()).append("\n");
            report.append("     - Tipo: ").append(field.getType()).append("\n");
            report.append("     - Solo lectura: ").append(field.isReadOnly() ? "Sí" : "No").append("\n");
            
            // Buscar valores en el mapa de picklist
            if (investigation.getPicklistValues().containsKey(field.getReferenceName())) {
                List<String> values = investigation.getPicklistValues().get(field.getReferenceName());
                report.append("     - Valores: ").append(String.join(", ", values)).append("\n");
            }
            report.append("\n");
        }
        
        // Valores de picklist encontrados
        if (!investigation.getPicklistValues().isEmpty()) {
            report.append("📝 **VALORES DE PICKLIST DETECTADOS**\n");
            report.append("====================================\n");
            for (Map.Entry<String, List<String>> entry : investigation.getPicklistValues().entrySet()) {
                report.append("   • ").append(entry.getKey()).append(":\n");
                for (String value : entry.getValue()) {
                    report.append("     - ").append(value).append("\n");
                }
                report.append("\n");
            }
        }
        
        return report.toString();
    }
    
    /**
     * Realiza investigación específica de campos problemáticos conocidos.
     */
    public ProblematicFieldsAnalysis analyzeProblematicFields(String project) {
        ProblematicFieldsAnalysis analysis = new ProblematicFieldsAnalysis();
        
        // Lista de campos conocidos como problemáticos
        List<String> problematicFields = List.of(
            "Custom.TipodeHistoria",
            "Custom.14858558-3edb-485a-9a52-a38c03c65c62", 
            "Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3",
            "Custom.Lahistoriacorrespondeauncumplimientoregulatorio",
            "Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1",
            "Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14"
        );
        
        // Validar cada campo problemático
        Map<String, AzureDevOpsFieldValidator.FieldValidationResult> validationResults = 
            fieldValidator.validateFieldsExistence(project, null, problematicFields);
            
        analysis.setValidationResults(validationResults);
        
        // Obtener valores para campos válidos
        Map<String, List<String>> fieldValues = new HashMap<>();
        for (Map.Entry<String, AzureDevOpsFieldValidator.FieldValidationResult> entry : validationResults.entrySet()) {
            if (entry.getValue().isValid()) {
                String fieldName = entry.getKey();
                List<String> values = picklistInvestigator.getFieldAllowedValues(project, null, fieldName);
                if (!values.isEmpty()) {
                    fieldValues.put(fieldName, values);
                }
            }
        }
        analysis.setFieldValues(fieldValues);
        
        return analysis;
    }
    
    // ========================================================================
    // MÉTODOS PRIVADOS DE APOYO
    // ========================================================================
    
    private List<WorkItemTypeDefinition> investigateWorkItemTypes(String project) {
        List<WorkItemTypeDefinition> types = new ArrayList<>();
        
        try {
            Map<String, Object> arguments = Map.of(
                "project", project,
                "includeExtendedInfo", true,
                "includeFieldDetails", true
            );
            
            Map<String, Object> result = workItemTypesTool.execute(arguments);
            
            if (!result.containsKey("isError") || !(Boolean) result.get("isError")) {
                // Procesar resultado exitoso
                // Nota: Este método necesitaría ser implementado según la estructura exacta
                // del resultado de GetWorkItemTypesTool
                types = parseWorkItemTypesFromToolResult(result);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo tipos de work items: " + e.getMessage());
        }
        
        return types;
    }
    
    private List<FieldDefinition> investigateAllFields(String project, List<WorkItemTypeDefinition> workItemTypes) {
        Set<FieldDefinition> allFieldsSet = new HashSet<>();
        
        for (WorkItemTypeDefinition type : workItemTypes) {
            try {
                // Obtener campos personalizados para este tipo
                List<AzureDevOpsFieldValidator.CustomFieldInfo> customFields = 
                    fieldValidator.getCustomFieldsForWorkItemType(project, type.getTypeName());
                
                for (AzureDevOpsFieldValidator.CustomFieldInfo customField : customFields) {
                    FieldDefinition fieldDef = new FieldDefinition();
                    fieldDef.setName(extractFieldDisplayName(customField.referenceName()));
                    fieldDef.setReferenceName(customField.referenceName());
                    fieldDef.setType(customField.type());
                    // Solo usar métodos que existen en FieldDefinition
                    fieldDef.setPicklistId(customField.referenceName()); // Usar como identificador
                    
                    allFieldsSet.add(fieldDef);
                }
                
            } catch (Exception e) {
                System.err.println("Error investigando campos para tipo " + type.getTypeName() + ": " + e.getMessage());
            }
        }
        
        return new ArrayList<>(allFieldsSet);
    }
    
    private Map<String, List<String>> investigatePicklistValues(String project, List<FieldDefinition> customFields) {
        Map<String, List<String>> picklistValues = new HashMap<>();
        
        for (FieldDefinition field : customFields) {
            // Intentar obtener valores usando la utilidad especializada
            List<String> values = picklistInvestigator.getFieldAllowedValues(project, null, field.getReferenceName());
            if (!values.isEmpty()) {
                picklistValues.put(field.getReferenceName(), values);
            }
        }
        
        return picklistValues;
    }
    
    private List<WorkItemTypeDefinition> parseWorkItemTypesFromToolResult(Map<String, Object> result) {
        // Este método necesitaría implementación específica basada en el formato
        // exacto del resultado de GetWorkItemTypesTool
        List<WorkItemTypeDefinition> types = new ArrayList<>();
        
        // Implementación placeholder - necesitaría ser completada
        // según la estructura real del resultado
        
        return types;
    }
    
    private String extractFieldDisplayName(String referenceName) {
        if (referenceName.startsWith("Custom.")) {
            return referenceName.substring(7); // Remover "Custom."
        }
        return referenceName;
    }
    
    // ========================================================================
    // CLASES DE RESULTADO
    // ========================================================================
    
    public static class ProblematicFieldsAnalysis {
        private Map<String, AzureDevOpsFieldValidator.FieldValidationResult> validationResults;
        private Map<String, List<String>> fieldValues;
        
        public ProblematicFieldsAnalysis() {
            this.validationResults = new HashMap<>();
            this.fieldValues = new HashMap<>();
        }
        
        public Map<String, AzureDevOpsFieldValidator.FieldValidationResult> getValidationResults() {
            return validationResults;
        }
        
        public void setValidationResults(Map<String, AzureDevOpsFieldValidator.FieldValidationResult> validationResults) {
            this.validationResults = validationResults;
        }
        
        public Map<String, List<String>> getFieldValues() {
            return fieldValues;
        }
        
        public void setFieldValues(Map<String, List<String>> fieldValues) {
            this.fieldValues = fieldValues;
        }
        
        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("🚨 **ANÁLISIS DE CAMPOS PROBLEMÁTICOS**\n");
            report.append("======================================\n\n");
            
            for (Map.Entry<String, AzureDevOpsFieldValidator.FieldValidationResult> entry : validationResults.entrySet()) {
                String field = entry.getKey();
                AzureDevOpsFieldValidator.FieldValidationResult result = entry.getValue();
                
                report.append("🔧 **").append(field).append("**\n");
                report.append("   Estado: ").append(result.isValid() ? "✅ Válido" : "❌ No válido").append("\n");
                report.append("   Mensaje: ").append(result.message()).append("\n");
                report.append("   Categoría: ").append(result.category()).append("\n");
                
                if (fieldValues.containsKey(field)) {
                    List<String> values = fieldValues.get(field);
                    report.append("   Valores encontrados (").append(values.size()).append("):\n");
                    for (String value : values) {
                        report.append("      • ").append(value).append("\n");
                    }
                }
                report.append("\n");
            }
            
            return report.toString();
        }
    }
}
