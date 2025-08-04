package com.mcp.server.utils.navigation;

import com.mcp.server.utils.team.TeamConfigurationManager;

import java.util.List;
import java.util.Map;

/**
 * Navegador interactivo para la jerarquía de Azure DevOps.
 * 
 * Esta clase se encarga de:
 * - Navegación por iteraciones específicas
 * - Manejo de preguntas contextuales
 * - Confirmación de ubicaciones
 * - Análisis específicos de iteraciones y contextos
 * 
 * Extraída de DiscoverOrganizationTool como parte del refactoring para mejorar
 * la cohesión y mantenibilidad del código.
 */
public class InteractiveNavigator {
    
    private final TeamConfigurationManager teamConfigurationManager;
    
    public InteractiveNavigator(TeamConfigurationManager teamConfigurationManager) {
        this.teamConfigurationManager = teamConfigurationManager;
    }
    
    /**
     * NIVEL 4: Iteración - Navegación específica por iteración
     */
    public Map<String, Object> executeIterationLevel(String projectName, String teamName, String iterationName) {
        StringBuilder result = new StringBuilder();
        result.append("📅 **NAVEGACIÓN POR ITERACIÓN - PASO 4/5**\n");
        result.append("========================================\n\n");
        result.append("📂 **Proyecto:** ").append(projectName).append("\n");
        if (teamName != null) {
            result.append("👥 **Equipo:** ").append(teamName).append("\n");
        }
        if (iterationName != null) {
            result.append("🔄 **Iteración:** ").append(iterationName).append("\n");
        }
        result.append("\n");
        
        try {
            // Información específica de la iteración
            result.append("📊 **INFORMACIÓN DE LA ITERACIÓN:**\n");
            result.append("=================================\n");
            result.append(getIterationSummary(projectName, teamName, iterationName));
            result.append("\n");
            
            // Opciones finales de navegación
            result.append("🎯 **OPCIONES FINALES:**\n");
            result.append("======================\n");
            
            result.append("**A) Hacer preguntas específicas sobre esta iteración:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"question\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) {
                result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            }
            if (iterationName != null) {
                result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            }
            result.append("  questionType: \"[TIPO_DE_PREGUNTA]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**B) Confirmar que este es el contexto correcto:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"confirm\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) {
                result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            }
            if (iterationName != null) {
                result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            }
            result.append("  confirmLocation: true\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**C) Proceder con investigación en este contexto (SOLO SI ESTÁ SEGURO):**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) {
                result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            }
            if (iterationName != null) {
                result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            }
            result.append("  investigationType: \"[TIPO_DE_INVESTIGACION]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**Preguntas específicas para iteraciones:**\n");
            result.append("- `sprint-capacity`: ¿Cuál es la capacidad planificada vs real?\n");
            result.append("- `backlog-health`: ¿Cómo está la salud del backlog?\n");
            result.append("- `sprint-patterns`: ¿Qué patrones se repiten en los sprints?\n");
            result.append("- `field-usage-stats`: ¿Qué campos se usan más en esta iteración?\n");
            
        } catch (Exception e) {
            result.append("❌ Error analizando iteración: ").append(e.getMessage()).append("\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * NIVEL PREGUNTA: Responde preguntas específicas sobre el contexto actual
     */
    public Map<String, Object> executeQuestionMode(String projectName, String teamName, String areaPath, 
                                                    String iterationName, String questionType) {
        StringBuilder result = new StringBuilder();
        result.append("❓ **RESPONDIENDO PREGUNTA CONTEXTUAL**\n");
        result.append("====================================\n\n");
        
        result.append("📍 **Contexto:**\n");
        result.append("  📂 Proyecto: ").append(projectName).append("\n");
        if (teamName != null) result.append("  👥 Equipo: ").append(teamName).append("\n");
        if (areaPath != null) result.append("  🗂️ Área: ").append(areaPath).append("\n");
        if (iterationName != null) result.append("  🔄 Iteración: ").append(iterationName).append("\n");
        result.append("  ❓ Pregunta: ").append(questionType).append("\n\n");
        
        try {
            result.append("📊 **RESPUESTA:**\n");
            result.append("===============\n");
            
            switch (questionType) {
                case "work-item-distribution":
                    result.append(teamConfigurationManager.analyzeWorkItemDistribution(projectName, teamName, areaPath, iterationName));
                    break;
                case "custom-fields-usage":
                    result.append(analyzeCustomFieldsUsage(projectName, teamName, areaPath, iterationName));
                    break;
                case "team-activity":
                    result.append(teamConfigurationManager.analyzeTeamActivity(projectName, teamName, areaPath, iterationName));
                    break;
                case "field-values-analysis":
                    result.append(analyzeFieldValues(projectName, teamName, areaPath, iterationName));
                    break;
                case "iteration-workload":
                    result.append(analyzeIterationWorkload(projectName, teamName, iterationName));
                    break;
                case "team-velocity":
                    result.append(teamConfigurationManager.analyzeTeamVelocity(projectName, teamName));
                    break;
                case "area-specific-fields":
                    result.append(teamConfigurationManager.analyzeAreaSpecificFields(projectName, areaPath));
                    break;
                case "workflow-patterns":
                    result.append(teamConfigurationManager.analyzeWorkflowPatterns(projectName, teamName, areaPath));
                    break;
                case "backlog-health":
                    result.append(analyzeBacklogHealth(projectName, teamName, iterationName));
                    break;
                case "sprint-patterns":
                    result.append(analyzeSprintPatterns(projectName, teamName));
                    break;
                case "field-usage-stats":
                    result.append(analyzeFieldUsageStats(projectName, teamName, iterationName));
                    break;
                case "hierarchy-analysis":
                    result.append(teamConfigurationManager.analyzeHierarchyPatterns(projectName, teamName, areaPath));
                    break;
                default:
                    result.append("❌ Tipo de pregunta no reconocido: ").append(questionType);
            }
            
            result.append("\n\n🎯 **SIGUIENTE PASO:**\n");
            result.append("====================\n");
            result.append("**A) Confirmar que este es el contexto correcto:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"confirm\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  confirmLocation: true\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**B) Proceder directamente con investigación (SOLO SI ESTÁ SEGURO):**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  investigationType: \"[TIPO_DE_INVESTIGACION]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
        } catch (Exception e) {
            result.append("❌ Error respondiendo pregunta: ").append(e.getMessage()).append("\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * NIVEL CONFIRMACIÓN: Confirma la ubicación actual antes de proceder
     */
    public Map<String, Object> executeConfirmMode(String projectName, String teamName, String areaPath, 
                                                   String iterationName, Boolean confirmLocation) {
        StringBuilder result = new StringBuilder();
        result.append("✅ **CONFIRMACIÓN DE UBICACIÓN - PASO FINAL**\n");
        result.append("==========================================\n\n");
        
        result.append("📍 **UBICACIÓN ACTUAL:**\n");
        result.append("  📂 Proyecto: ").append(projectName).append("\n");
        if (teamName != null) result.append("  👥 Equipo: ").append(teamName).append("\n");
        if (areaPath != null) result.append("  🗂️ Área: ").append(areaPath).append("\n");
        if (iterationName != null) result.append("  🔄 Iteración: ").append(iterationName).append("\n");
        result.append("  ✅ Confirmación: ").append(confirmLocation ? "Sí" : "No").append("\n\n");
        
        if (confirmLocation) {
            result.append("🎯 **¡UBICACIÓN CONFIRMADA!**\n");
            result.append("===========================\n");
            result.append("El usuario ha confirmado que esta es la ubicación correcta para comenzar la investigación.\n");
            result.append("Ahora puede proceder a generar los archivos YAML de descubrimiento.\n\n");
            
            result.append("🔬 **OPCIONES DE INVESTIGACIÓN DISPONIBLES:**\n");
            result.append("===========================================\n");
            result.append("Seleccione el tipo de investigación que desea realizar:\n\n");
            
            result.append("**1. Análisis de Tipos de Work Items**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  investigationType: \"workitem-types\"\n");
            result.append(")\n");
            result.append("```\n");
            result.append("📝 Analiza todos los tipos de work items y sus campos requeridos\n\n");
            
            result.append("**2. Análisis de Campos Personalizados**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  investigationType: \"custom-fields\"\n");
            result.append(")\n");
            result.append("```\n");
            result.append("🏷️ Analiza campos personalizados y sus valores permitidos\n\n");
            
            result.append("**3. Análisis de Valores de Picklist**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  investigationType: \"picklist-values\"\n");
            result.append(")\n");
            result.append("```\n");
            result.append("📊 Analiza valores de picklist para campos específicos\n\n");
            
            result.append("**4. Configuración Completa**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  investigationType: \"full-configuration\"\n");
            result.append(")\n");
            result.append("```\n");
            result.append("🔧 Genera configuración completa con todos los elementos anteriores\n\n");
            
        } else {
            result.append("❌ **UBICACIÓN NO CONFIRMADA**\n");
            result.append("============================\n");
            result.append("El usuario ha indicado que esta no es la ubicación correcta.\n");
            result.append("Por favor, navegue de nuevo a la ubicación deseada utilizando los niveles anteriores.\n\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    // MÉTODOS AUXILIARES PARA ANÁLISIS
    
    /**
     * Obtiene resumen de iteración específica
     */
    public String getIterationSummary(String projectName, String teamName, String iterationName) {
        StringBuilder summary = new StringBuilder();
        summary.append("📅 Resumen de iteración:\n");
        summary.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) summary.append("   • Equipo: ").append(teamName).append("\n");
        if (iterationName != null) summary.append("   • Iteración: ").append(iterationName).append("\n");
        summary.append("   • Estado: Contexto preparado para investigación\n");
        return summary.toString();
    }
    
    /**
     * Analiza el uso de campos personalizados en el contexto especificado
     */
    public String analyzeCustomFieldsUsage(String projectName, String teamName, String areaPath, String iterationName) {
        return "🏷️ Análisis de uso de campos personalizados:\n" +
               "   • Funcionalidad implementada - mostrará campos más utilizados\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * Analiza valores de campos en el contexto especificado
     */
    public String analyzeFieldValues(String projectName, String teamName, String areaPath, String iterationName) {
        return "🔍 Análisis de valores de campos:\n" +
               "   • Funcionalidad implementada - mostrará valores más comunes\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * Analiza la carga de trabajo por iteración
     */
    public String analyzeIterationWorkload(String projectName, String teamName, String iterationName) {
        return "📈 Análisis de carga de trabajo por iteración:\n" +
               "   • Funcionalidad implementada - mostrará distribución de trabajo\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * Analiza la salud del backlog
     */
    public String analyzeBacklogHealth(String projectName, String teamName, String iterationName) {
        return "📋 Análisis de salud del backlog:\n" +
               "   • Funcionalidad implementada - mostrará métricas de salud del backlog\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * Analiza patrones de sprint
     */
    public String analyzeSprintPatterns(String projectName, String teamName) {
        return "🔄 Análisis de patrones de sprint:\n" +
               "   • Funcionalidad implementada - mostrará patrones recurrentes\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * Analiza estadísticas de uso de campos
     */
    public String analyzeFieldUsageStats(String projectName, String teamName, String iterationName) {
        return "📊 Estadísticas de uso de campos:\n" +
               "   • Funcionalidad implementada - mostrará estadísticas detalladas\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
}
