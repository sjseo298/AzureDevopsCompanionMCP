package com.mcp.server.utils.investigation;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.utils.discovery.AzureDevOpsConfigurationGenerator;
import com.mcp.server.utils.workitemtype.WorkItemTypeManager;
import com.mcp.server.utils.field.FieldAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Investigador organizacional para Azure DevOps.
 * 
 * Esta clase se encarga de:
 * - Backup de configuraciones existentes
 * - Investigación completa de configuraciones organizacionales
 * - Generación de archivos YAML de configuración
 * - Documentación exhaustiva de tipos de work items
 * - Análisis de campos personalizados y picklists
 * 
 * Extraída de DiscoverOrganizationTool como parte del refactoring para mejorar
 * la cohesión y mantenibilidad del código.
 */
public class OrganizationInvestigator {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final AzureDevOpsConfigurationGenerator configurationGenerator;
    private final WorkItemTypeManager workItemTypeManager;
    private final FieldAnalyzer fieldAnalyzer;
    
    public OrganizationInvestigator(AzureDevOpsClient azureDevOpsClient,
                                   AzureDevOpsConfigurationGenerator configurationGenerator,
                                   WorkItemTypeManager workItemTypeManager,
                                   FieldAnalyzer fieldAnalyzer) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configurationGenerator = configurationGenerator;
        this.workItemTypeManager = workItemTypeManager;
        this.fieldAnalyzer = fieldAnalyzer;
    }
    
    /**
     * Realiza backup de archivos de configuración existentes
     */
    public String performConfigurationBackup() {
        StringBuilder backupReport = new StringBuilder();
        backupReport.append("💾 **Backup de Archivos de Configuración**\n");
        backupReport.append("========================================\n");
        
        try {
            List<String> configFiles = List.of(
                "config/organization-config.yml",
                "config/field-mappings.yml", 
                "config/discovered-organization.yml",
                "src/main/resources/application.yml"
            );
            
            int backedUpFiles = 0;
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );
            
            for (String configFile : configFiles) {
                try {
                    Path originalPath = Paths.get(configFile);
                    
                    if (Files.exists(originalPath)) {
                        String backupFileName = configFile.replace(".", "_backup_" + timestamp + ".");
                        Path backupPath = Paths.get(backupFileName);
                        
                        Files.copy(originalPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                        
                        backupReport.append("✅ ").append(configFile).append(" → ").append(backupFileName).append("\n");
                        backedUpFiles++;
                    } else {
                        backupReport.append("ℹ️ ").append(configFile).append(" (no existe, no requiere backup)\n");
                    }
                    
                } catch (Exception e) {
                    backupReport.append("❌ Error con ").append(configFile).append(": ").append(e.getMessage()).append("\n");
                }
            }
            
            backupReport.append("\n📊 **Resumen:** ").append(backedUpFiles).append(" archivos respaldados exitosamente\n");
            backupReport.append("🕒 **Timestamp:** ").append(timestamp).append("\n\n");
            
        } catch (Exception e) {
            backupReport.append("❌ Error general durante backup: ").append(e.getMessage()).append("\n\n");
        }
        
        return backupReport.toString();
    }
    
    /**
     * Realiza investigación específica de tipos de work items
     */
    public String performWorkItemTypesInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
        return workItemTypeManager.performWorkItemTypesInvestigation(projectName, teamName, areaPath, iterationName);
    }
    
    /**
     * Realiza investigación específica de campos personalizados
     */
    public String performCustomFieldsInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
        StringBuilder investigation = new StringBuilder();
        investigation.append("🔍 INVESTIGACIÓN CENTRALIZADA: Campos Personalizados\n");
        investigation.append("====================================================\n\n");
        investigation.append("📍 Contexto específico:\n");
        investigation.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) investigation.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) investigation.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) investigation.append("   • Iteración: ").append(iterationName).append("\n");
        investigation.append("\n");
        
        try {
            // Generar configuración específica para campos personalizados
            AzureDevOpsConfigurationGenerator.ConfigurationGenerationResult result = 
                configurationGenerator.generateSpecificConfiguration(projectName, "custom-fields", false);
            
            investigation.append("🏗️ **RESULTADO DE GENERACIÓN:**\n");
            investigation.append("==============================\n");
            investigation.append(result.generateReport());
            
            // Usar método existente como complemento
            investigation.append("\n🔧 **DETALLES ADICIONALES:**\n");
            investigation.append("============================\n");
            investigation.append(analyzeCustomFieldsDetailed(projectName));
        
        } catch (Exception e) {
            investigation.append("❌ Error durante investigación: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Realiza investigación específica de valores de picklist
     */
    public String performPicklistValuesInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
        StringBuilder investigation = new StringBuilder();
        investigation.append("🔍 INVESTIGACIÓN CENTRALIZADA: Valores de Picklist\n");
        investigation.append("===================================================\n\n");
        investigation.append("📍 Contexto específico:\n");
        investigation.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) investigation.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) investigation.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) investigation.append("   • Iteración: ").append(iterationName).append("\n");
        investigation.append("\n");
        
        try {
            // Generar configuración específica para valores de picklist
            AzureDevOpsConfigurationGenerator.ConfigurationGenerationResult result = 
                configurationGenerator.generateSpecificConfiguration(projectName, "picklist-values", false);
            
            investigation.append("🏗️ **RESULTADO DE GENERACIÓN:**\n");
            investigation.append("==============================\n");
            investigation.append(result.generateReport());
            
            // Usar método existente refactorizado como complemento
            investigation.append("\n📋 **ANÁLISIS DETALLADO DE PICKLIST:**\n");
            investigation.append("=====================================\n");
            investigation.append(analyzePicklistValuesDetailed(projectName));
        
        } catch (Exception e) {
            investigation.append("❌ Error durante investigación: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Realiza generación completa de configuración organizacional
     */
    public String performFullConfigurationGeneration(String projectName, String teamName, String areaPath, String iterationName, Boolean backupExistingFiles) {
        StringBuilder investigation = new StringBuilder();
        investigation.append("🏗️ GENERACIÓN COMPLETA DE CONFIGURACIÓN\n");
        investigation.append("======================================\n\n");
        investigation.append("📍 Contexto específico:\n");
        investigation.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) investigation.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) investigation.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) investigation.append("   • Iteración: ").append(iterationName).append("\n");
        investigation.append("   • Backup: ").append(backupExistingFiles ? "Sí" : "No").append("\n");
        investigation.append("\n");
        
        try {
            // Realizar backup si se solicita
            if (backupExistingFiles) {
                investigation.append("💾 **BACKUP PREVIO:**\n");
                investigation.append("===================\n");
                investigation.append(performConfigurationBackup());
            }
            
            // Generar configuración completa
            AzureDevOpsConfigurationGenerator.ConfigurationGenerationResult result = 
                configurationGenerator.generateCompleteConfiguration(projectName, backupExistingFiles);
            
            investigation.append("✅ **CONFIGURACIÓN COMPLETA GENERADA**\n");
            investigation.append("======================================\n");
            investigation.append(result.generateReport());
            
            if (result.isSuccess()) {
                investigation.append("\n🎉 **¡PROCESO COMPLETADO EXITOSAMENTE!**\n");
                investigation.append("========================================\n");
                investigation.append("La configuración organizacional ha sido generada y está lista para usar.\n");
                investigation.append("Los archivos YAML contienen toda la información descubierta automáticamente.\n\n");
                
                investigation.append("📁 **PRÓXIMOS PASOS:**\n");
                investigation.append("1. Revisar los archivos generados en el directorio config/\n");
                investigation.append("2. Personalizar los valores según necesidades específicas\n");
                investigation.append("3. Usar la configuración en las herramientas MCP\n");
                investigation.append("4. Ejecutar pruebas de integración con la configuración\n\n");
            } else {
                investigation.append("\n⚠️ **PROCESO COMPLETADO CON ADVERTENCIAS**\n");
                investigation.append("=========================================\n");
                investigation.append("La configuración se generó pero con algunos problemas.\n");
                investigation.append("Revisar los detalles arriba para identificar los elementos que requieren atención manual.\n\n");
            }
        
        } catch (Exception e) {
            investigation.append("❌ Error durante generación: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Realiza análisis exhaustivo de tipos de work items con documentación completa de campos
     */
    public String analyzeWorkItemTypesWithCompleteFieldDocumentation(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📋 **DOCUMENTACIÓN EXHAUSTIVA DE WORK ITEM TYPES**\n");
        analysis.append("================================================\n\n");
        
        try {
            // Obtener todos los tipos de work items disponibles
            List<String> availableTypes = workItemTypeManager.getAvailableWorkItemTypes(project);
            
            analysis.append("🔍 **Tipos encontrados:** ").append(availableTypes.size()).append("\n");
            analysis.append("📋 **Lista:** ").append(String.join(", ", availableTypes)).append("\n\n");
            
            // Para cada tipo, documentar TODOS sus campos exhaustivamente
            for (String workItemType : availableTypes) {
                analysis.append("═".repeat(80)).append("\n");
                analysis.append("📋 **WORK ITEM TYPE: ").append(workItemType.toUpperCase()).append("**\n");
                analysis.append("═".repeat(80)).append("\n\n");
                
                // Obtener información completa del tipo usando WorkItemTypeManager
                String typeAnalysis = workItemTypeManager.analyzeWorkItemTypesDetailed(project);
                analysis.append(typeAnalysis);
                
                // Obtener información detallada de campos usando FieldAnalyzer
                List<Map<String, Object>> fieldData = fieldAnalyzer.getAllProjectFieldsDetailed(project);
                analysis.append("\n🔧 **ANÁLISIS DETALLADO DE CAMPOS:**\n");
                analysis.append("====================================\n");
                analysis.append("📊 Total de campos encontrados: ").append(fieldData.size()).append("\n");
                
                // Procesar y mostrar información resumida de campos
                for (Map<String, Object> field : fieldData) {
                    String fieldName = (String) field.get("name");
                    String fieldType = (String) field.get("type");
                    if (fieldName != null && fieldType != null) {
                        analysis.append("• ").append(fieldName).append(" (").append(fieldType).append(")\n");
                    }
                }
                
                analysis.append("\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    // MÉTODOS AUXILIARES PRIVADOS
    
    /**
     * Analiza campos personalizados de manera detallada
     */
    private String analyzeCustomFieldsDetailed(String projectName) {
        try {
            List<Map<String, Object>> fieldData = fieldAnalyzer.getAllProjectFieldsDetailed(projectName);
            StringBuilder result = new StringBuilder();
            result.append("📊 Total de campos encontrados: ").append(fieldData.size()).append("\n");
            
            for (Map<String, Object> field : fieldData) {
                String fieldName = (String) field.get("name");
                String fieldType = (String) field.get("type");
                Boolean isCustom = (Boolean) field.get("isCustom");
                
                if (Boolean.TRUE.equals(isCustom) && fieldName != null && fieldType != null) {
                    result.append("• ").append(fieldName).append(" (").append(fieldType).append(")\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            return "❌ Error analizando campos personalizados: " + e.getMessage();
        }
    }
    
    /**
     * Analiza valores de picklist de manera detallada
     */
    private String analyzePicklistValuesDetailed(String projectName) {
        try {
            return fieldAnalyzer.analyzePicklistValuesDetailed(projectName);
        } catch (Exception e) {
            return "❌ Error analizando valores de picklist: " + e.getMessage();
        }
    }
}
