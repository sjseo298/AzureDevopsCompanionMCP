package com.mcp.server.utils.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utilidad especializada para generar archivos de configuración YAML basados en 
 * la investigación organizacional de Azure DevOps.
 */
public class AzureDevOpsConfigurationGenerator {
    
    private final AzureDevOpsOrganizationInvestigator organizationInvestigator;
    
    public AzureDevOpsConfigurationGenerator(AzureDevOpsOrganizationInvestigator organizationInvestigator) {
        this.organizationInvestigator = organizationInvestigator;
    }
    
    /**
     * Genera configuración completa para un proyecto específico con backup opcional.
     */
    public ConfigurationGenerationResult generateCompleteConfiguration(String project, boolean backupExistingFiles) {
        ConfigurationGenerationResult result = new ConfigurationGenerationResult();
        result.setProject(project);
        result.setGenerationDate(LocalDateTime.now());
        result.setBackupCreated(backupExistingFiles);
        
        try {
            // Paso 1: Realizar investigación completa
            OrganizationFieldInvestigation investigation = organizationInvestigator.performCompleteInvestigation(project);
            
            // Paso 2: Crear backup si se solicita
            if (backupExistingFiles) {
                createBackupFiles(result);
            }
            
            // Paso 3: Generar archivos YAML
            generateDiscoveredOrganizationYaml(investigation, result);
            generateOrganizationConfigYaml(investigation, result);
            generateFieldMappingsYaml(investigation, result);
            generateBusinessRulesYaml(investigation, result);
            
            result.setSuccess(true);
            result.setMessage("Configuración generada exitosamente");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error generando configuración: " + e.getMessage());
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Genera solo archivos específicos según el tipo de investigación.
     */
    public ConfigurationGenerationResult generateSpecificConfiguration(String project, String investigationType, boolean backupExistingFiles) {
        ConfigurationGenerationResult result = new ConfigurationGenerationResult();
        result.setProject(project);
        result.setGenerationDate(LocalDateTime.now());
        result.setBackupCreated(backupExistingFiles);
        
        try {
            OrganizationFieldInvestigation investigation = organizationInvestigator.performCompleteInvestigation(project);
            
            if (backupExistingFiles) {
                createBackupFiles(result);
            }
            
            switch (investigationType) {
                case "workitem-types":
                    generateWorkItemTypesConfiguration(investigation, result);
                    break;
                case "custom-fields":
                    generateCustomFieldsConfiguration(investigation, result);
                    break;
                case "picklist-values":
                    generatePicklistConfiguration(investigation, result);
                    break;
                case "full-configuration":
                    return generateCompleteConfiguration(project, backupExistingFiles);
                default:
                    throw new IllegalArgumentException("Tipo de investigación no reconocido: " + investigationType);
            }
            
            result.setSuccess(true);
            result.setMessage("Configuración específica generada exitosamente");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error generando configuración específica: " + e.getMessage());
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    // ========================================================================
    // MÉTODOS PRIVADOS DE GENERACIÓN
    // ========================================================================
    
    private void createBackupFiles(ConfigurationGenerationResult result) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupDir = Paths.get("config/backup_" + timestamp);
        Files.createDirectories(backupDir);
        
        List<String> configFiles = List.of(
            "config/discovered-organization.yml",
            "config/organization-config.yml", 
            "config/field-mappings.yml",
            "config/business-rules.yml"
        );
        
        List<String> backedUpFiles = new ArrayList<>();
        for (String configFile : configFiles) {
            Path originalFile = Paths.get(configFile);
            if (Files.exists(originalFile)) {
                Path backupFile = backupDir.resolve(originalFile.getFileName());
                Files.copy(originalFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                backedUpFiles.add(configFile);
            }
        }
        
        result.setBackupDirectory(backupDir.toString());
        result.setBackedUpFiles(backedUpFiles);
    }
    
    private void generateDiscoveredOrganizationYaml(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        StringBuilder yaml = new StringBuilder();
        
        // Header
        yaml.append("# Configuración descubierta automáticamente\n");
        yaml.append("# Generado el: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        yaml.append("# Proyecto: ").append(investigation.getProject()).append("\n\n");
        
        // Organization info
        yaml.append("organization:\n");
        yaml.append("  name: \"").append(investigation.getProject()).append("\"\n");
        yaml.append("  discoveryDate: \"").append(investigation.getInvestigationDate()).append("\"\n\n");
        
        // Work item types
        yaml.append("workItemTypes:\n");
        for (WorkItemTypeDefinition type : investigation.getWorkItemTypes()) {
            yaml.append("  - name: \"").append(type.getTypeName()).append("\"\n");
            if (type.getDescription() != null) {
                yaml.append("    description: \"").append(type.getDescription()).append("\"\n");
            }
            
            if (!type.getFieldsWithValues().isEmpty()) {
                yaml.append("    fields:\n");
                for (Map.Entry<String, List<String>> fieldEntry : type.getFieldsWithValues().entrySet()) {
                    yaml.append("      \"").append(fieldEntry.getKey()).append("\":\n");
                    yaml.append("        allowedValues:\n");
                    for (String value : fieldEntry.getValue()) {
                        yaml.append("          - \"").append(value).append("\"\n");
                    }
                }
            }
        }
        
        // Custom fields
        if (!investigation.getCustomFields().isEmpty()) {
            yaml.append("\ncustomFields:\n");
            for (FieldDefinition field : investigation.getCustomFields()) {
                yaml.append("  \"").append(field.getReferenceName()).append("\":\n");
                yaml.append("    name: \"").append(field.getName()).append("\"\n");
                yaml.append("    type: \"").append(field.getType()).append("\"\n");
                yaml.append("    readOnly: ").append(field.isReadOnly()).append("\n");
                
                if (investigation.getPicklistValues().containsKey(field.getReferenceName())) {
                    List<String> values = investigation.getPicklistValues().get(field.getReferenceName());
                    yaml.append("    allowedValues:\n");
                    for (String value : values) {
                        yaml.append("      - \"").append(value).append("\"\n");
                    }
                }
            }
        }
        
        // Write file
        Path outputFile = Paths.get("config/discovered-organization.yml");
        Files.createDirectories(outputFile.getParent());
        Files.write(outputFile, yaml.toString().getBytes());
        
        result.addGeneratedFile("config/discovered-organization.yml", "Configuración organizacional descubierta");
    }
    
    private void generateOrganizationConfigYaml(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# Configuración organizacional principal\n");
        yaml.append("# Generado automáticamente el: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        yaml.append("organization:\n");
        yaml.append("  name: \"").append(investigation.getProject()).append("\"\n");
        yaml.append("  defaultProject: \"").append(investigation.getProject()).append("\"\n");
        yaml.append("  defaultTeam: \"DefaultTeam\"\n");
        yaml.append("  timeZone: \"America/Bogota\"\n");
        yaml.append("  language: \"es-CO\"\n\n");
        
        yaml.append("workItemTypes:\n");
        for (WorkItemTypeDefinition type : investigation.getWorkItemTypes()) {
            yaml.append("  \"").append(type.getTypeName()).append("\":\n");
            yaml.append("    enabled: true\n");
            yaml.append("    fastCreation: true\n");
            
            if (type.getTypeName().toLowerCase().contains("historia")) {
                yaml.append("    requiredFields:\n");
                yaml.append("      - title\n");
                yaml.append("      - description\n");
                yaml.append("      - tipoHistoria\n");
            } else if (type.getTypeName().toLowerCase().contains("bug")) {
                yaml.append("    requiredFields:\n");
                yaml.append("      - title\n");
                yaml.append("      - reproSteps\n");
                yaml.append("      - origen\n");
            } else {
                yaml.append("    requiredFields:\n");
                yaml.append("      - title\n");
            }
        }
        
        Path outputFile = Paths.get("config/organization-config.yml");
        Files.write(outputFile, yaml.toString().getBytes());
        
        result.addGeneratedFile("config/organization-config.yml", "Configuración organizacional principal");
    }
    
    private void generateFieldMappingsYaml(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# Mapeo de campos personalizados\n");
        yaml.append("# Generado automáticamente el: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        yaml.append("fieldMappings:\n");
        yaml.append("  title:\n");
        yaml.append("    azureFieldName: \"System.Title\"\n");
        yaml.append("    helpText: \"Título descriptivo del work item\"\n");
        yaml.append("    required: true\n\n");
        
        yaml.append("  description:\n");
        yaml.append("    azureFieldName: \"System.Description\"\n");
        yaml.append("    helpText: \"Descripción detallada del work item\"\n");
        yaml.append("    required: false\n\n");
        
        // Agregar campos personalizados descubiertos
        for (FieldDefinition field : investigation.getCustomFields()) {
            String fieldKey = field.getName().toLowerCase().replaceAll("\\s+", "");
            yaml.append("  ").append(fieldKey).append(":\n");
            yaml.append("    azureFieldName: \"").append(field.getReferenceName()).append("\"\n");
            yaml.append("    helpText: \"").append(field.getName()).append(" - Campo personalizado\"\n");
            yaml.append("    required: false\n");
            
            if (investigation.getPicklistValues().containsKey(field.getReferenceName())) {
                List<String> values = investigation.getPicklistValues().get(field.getReferenceName());
                yaml.append("    allowedValues:\n");
                for (String value : values) {
                    yaml.append("      - \"").append(value).append("\"\n");
                }
            }
            yaml.append("\n");
        }
        
        Path outputFile = Paths.get("config/field-mappings.yml");
        Files.write(outputFile, yaml.toString().getBytes());
        
        result.addGeneratedFile("config/field-mappings.yml", "Mapeo de campos personalizados");
    }
    
    private void generateBusinessRulesYaml(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# Reglas de negocio organizacionales\n");
        yaml.append("# Generado automáticamente el: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        yaml.append("businessRules:\n");
        yaml.append("  validation:\n");
        yaml.append("    enabled: true\n");
        yaml.append("    strictMode: false\n\n");
        
        yaml.append("  workItemTypes:\n");
        for (WorkItemTypeDefinition type : investigation.getWorkItemTypes()) {
            yaml.append("    \"").append(type.getTypeName()).append("\":\n");
            yaml.append("      autoAssignment: false\n");
            yaml.append("      defaultPriority: 2\n");
            
            if (type.getTypeName().toLowerCase().contains("historia")) {
                yaml.append("      defaultValues:\n");
                yaml.append("        tipoHistoria: \"Funcional\"\n");
            } else if (type.getTypeName().toLowerCase().contains("bug")) {
                yaml.append("      defaultValues:\n");
                yaml.append("        origen: \"Manual\"\n");
                yaml.append("        nivelPrueba: \"Sistema\"\n");
            }
            yaml.append("\n");
        }
        
        Path outputFile = Paths.get("config/business-rules.yml");
        Files.write(outputFile, yaml.toString().getBytes());
        
        result.addGeneratedFile("config/business-rules.yml", "Reglas de negocio organizacionales");
    }
    
    private void generateWorkItemTypesConfiguration(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        generateOrganizationConfigYaml(investigation, result);
    }
    
    private void generateCustomFieldsConfiguration(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        generateFieldMappingsYaml(investigation, result);
    }
    
    private void generatePicklistConfiguration(OrganizationFieldInvestigation investigation, ConfigurationGenerationResult result) throws IOException {
        generateDiscoveredOrganizationYaml(investigation, result);
    }
    
    // ========================================================================
    // CLASE DE RESULTADO
    // ========================================================================
    
    public static class ConfigurationGenerationResult {
        private String project;
        private LocalDateTime generationDate;
        private boolean success;
        private String message;
        private String error;
        private boolean backupCreated;
        private String backupDirectory;
        private List<String> backedUpFiles = new ArrayList<>();
        private Map<String, String> generatedFiles = new HashMap<>();
        
        // Getters y setters
        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }
        
        public LocalDateTime getGenerationDate() { return generationDate; }
        public void setGenerationDate(LocalDateTime generationDate) { this.generationDate = generationDate; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public boolean isBackupCreated() { return backupCreated; }
        public void setBackupCreated(boolean backupCreated) { this.backupCreated = backupCreated; }
        
        public String getBackupDirectory() { return backupDirectory; }
        public void setBackupDirectory(String backupDirectory) { this.backupDirectory = backupDirectory; }
        
        public List<String> getBackedUpFiles() { return backedUpFiles; }
        public void setBackedUpFiles(List<String> backedUpFiles) { this.backedUpFiles = backedUpFiles; }
        
        public Map<String, String> getGeneratedFiles() { return generatedFiles; }
        public void setGeneratedFiles(Map<String, String> generatedFiles) { this.generatedFiles = generatedFiles; }
        
        public void addGeneratedFile(String filePath, String description) {
            this.generatedFiles.put(filePath, description);
        }
        
        /**
         * Genera un reporte del resultado de la generación.
         */
        public String generateReport() {
            StringBuilder report = new StringBuilder();
            
            report.append("🏗️ **RESULTADO GENERACIÓN DE CONFIGURACIÓN**\n");
            report.append("==========================================\n\n");
            
            report.append("📊 **Resumen:**\n");
            report.append("   • Proyecto: ").append(project).append("\n");
            report.append("   • Fecha: ").append(generationDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            report.append("   • Estado: ").append(success ? "✅ Exitoso" : "❌ Error").append("\n");
            report.append("   • Mensaje: ").append(message).append("\n");
            
            if (error != null) {
                report.append("   • Error: ").append(error).append("\n");
            }
            report.append("\n");
            
            if (backupCreated) {
                report.append("💾 **Backup creado:**\n");
                report.append("   • Directorio: ").append(backupDirectory).append("\n");
                report.append("   • Archivos respaldados: ").append(backedUpFiles.size()).append("\n");
                for (String file : backedUpFiles) {
                    report.append("     - ").append(file).append("\n");
                }
                report.append("\n");
            }
            
            if (!generatedFiles.isEmpty()) {
                report.append("📁 **Archivos generados:**\n");
                for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                    report.append("   • ").append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
                }
            }
            
            return report.toString();
        }
    }
}
