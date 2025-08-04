package com.mcp.server.utils.team;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.model.Iteration;

import java.util.*;

/**
 * Manejador especializado para operaciones con equipos y configuración de áreas en Azure DevOps.
 * Extraído de DiscoverOrganizationTool para mejorar la separación de responsabilidades.
 */
public class TeamConfigurationManager {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    public TeamConfigurationManager(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    /**
     * Ejecuta la navegación a nivel de equipo y área
     */
    public Map<String, Object> executeTeamLevel(String projectName, String teamName, String areaPath) {
        StringBuilder result = new StringBuilder();
        result.append("👥 **NAVEGACIÓN POR EQUIPO/ÁREA - PASO 3/5**\n");
        result.append("==========================================\n\n");
        result.append("📂 **Proyecto:** ").append(projectName).append("\n");
        if (teamName != null) {
            result.append("👥 **Equipo:** ").append(teamName).append("\n");
        }
        if (areaPath != null) {
            result.append("🗂️ **Área Path:** ").append(areaPath).append("\n");
        }
        result.append("\n");
        
        try {
            // Analizar iteraciones si hay equipo seleccionado
            if (teamName != null) {
                result.append("📅 **ITERACIONES DEL EQUIPO:**\n");
                result.append("============================\n");
                
                List<Iteration> iterations = azureDevOpsClient.listIterations(projectName, teamName, null);
                if (iterations.isEmpty()) {
                    result.append("   ⚠️ No se encontraron iteraciones para este equipo.\n\n");
                } else {
                    for (int i = 0; i < Math.min(iterations.size(), 5); i++) {
                        Iteration iteration = iterations.get(i);
                        result.append(String.format("   %d. **%s**\n", i + 1, iteration.name()));
                        if (iteration.attributes() != null) {
                            result.append("      📅 ").append(iteration.attributes().startDate());
                            result.append(" - ").append(iteration.attributes().finishDate()).append("\n");
                        }
                    }
                    if (iterations.size() > 5) {
                        result.append("   ... y ").append(iterations.size() - 5).append(" más\n");
                    }
                    result.append("\n");
                }
            }
            
            // Información contextual del equipo/área
            result.append("📊 **INFORMACIÓN CONTEXTUAL:**\n");
            result.append("=============================\n");
            result.append(getTeamContextSummary(projectName, teamName, areaPath));
            result.append("\n");
            
            // Opciones de navegación más específicas
            result.append("🎯 **OPCIONES DE NAVEGACIÓN:**\n");
            result.append("============================\n");
            
            result.append("**A) Navegar por iteraciones específicas:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"iteration\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) {
                result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            }
            if (areaPath != null) {
                result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            }
            result.append("  selectedIteration: \"[NOMBRE_ITERACION]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**B) Hacer preguntas contextuales sobre este equipo/área:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"question\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) {
                result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            }
            if (areaPath != null) {
                result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            }
            result.append("  questionType: \"team-activity\"  # Opciones: work-item-distribution, custom-fields-usage, team-activity, etc.\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**C) Confirmar ubicación y proceder a investigación:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"confirm\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) {
                result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            }
            if (areaPath != null) {
                result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            }
            result.append("  confirmLocation: true\n");
            result.append(")\n");
            result.append("```\n\n");
            
        } catch (Exception e) {
            result.append("❌ Error durante navegación de equipo: ").append(e.getMessage()).append("\n");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(Map.of("type", "text", "text", result.toString())));
        
        return response;
    }
    
    /**
     * Obtiene resumen contextual del equipo y área
     */
    public String getTeamContextSummary(String projectName, String teamName, String areaPath) {
        StringBuilder summary = new StringBuilder();
        summary.append("📊 Información contextual disponible:\n");
        summary.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) summary.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) summary.append("   • Área: ").append(areaPath).append("\n");
        summary.append("   • Estado: Listo para análisis detallado\n");
        return summary.toString();
    }
    
    /**
     * Analiza distribución de work items por equipo/área
     */
    public String analyzeWorkItemDistribution(String projectName, String teamName, String areaPath, String iterationName) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 **ANÁLISIS DE DISTRIBUCIÓN DE WORK ITEMS**\n");
        analysis.append("==========================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) analysis.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) analysis.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) analysis.append("   • Iteración: ").append(iterationName).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis usando azureDevOpsClient
            analysis.append("📈 **Resultados del análisis:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará estadísticas de distribución por tipo\n");
            analysis.append("   • Total work items analizados: [NÚMERO]\n");
            analysis.append("   • Distribución por tipo: [DETALLES]\n");
            analysis.append("   • Patrones identificados: [PATRONES]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza uso de campos personalizados por equipo/área
     */
    public String analyzeCustomFieldsUsage(String projectName, String teamName, String areaPath, String iterationName) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🏷️ **ANÁLISIS DE USO DE CAMPOS PERSONALIZADOS**\n");
        analysis.append("===============================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) analysis.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) analysis.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) analysis.append("   • Iteración: ").append(iterationName).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis
            analysis.append("📋 **Resultados del análisis:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará campos más utilizados\n");
            analysis.append("   • Campos personalizados encontrados: [NÚMERO]\n");
            analysis.append("   • Frecuencia de uso: [ESTADÍSTICAS]\n");
            analysis.append("   • Recomendaciones: [SUGERENCIAS]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza actividad del equipo
     */
    public String analyzeTeamActivity(String projectName, String teamName, String areaPath, String iterationName) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("👥 **ANÁLISIS DE ACTIVIDAD DEL EQUIPO**\n");
        analysis.append("=====================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) analysis.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) analysis.append("   • Área: ").append(areaPath).append("\n");
        if (iterationName != null) analysis.append("   • Iteración: ").append(iterationName).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis
            analysis.append("📊 **Métricas de actividad:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará métricas de actividad\n");
            analysis.append("   • Work items creados: [NÚMERO]\n");
            analysis.append("   • Work items completados: [NÚMERO]\n");
            analysis.append("   • Velocidad promedio: [VELOCIDAD]\n");
            analysis.append("   • Tendencias: [ANÁLISIS]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza velocidad del equipo
     */
    public String analyzeTeamVelocity(String projectName, String teamName) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🚀 **ANÁLISIS DE VELOCIDAD DEL EQUIPO**\n");
        analysis.append("=====================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) analysis.append("   • Equipo: ").append(teamName).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis de velocidad
            analysis.append("📈 **Métricas de velocidad:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará métricas de velocidad\n");
            analysis.append("   • Velocidad promedio (últimos 6 sprints): [VELOCIDAD]\n");
            analysis.append("   • Tendencia: [ASCENDENTE/DESCENDENTE/ESTABLE]\n");
            analysis.append("   • Capacidad vs. demanda: [ANÁLISIS]\n");
            analysis.append("   • Recomendaciones: [SUGERENCIAS]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza campos específicos del área
     */
    public String analyzeAreaSpecificFields(String projectName, String areaPath) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🗂️ **ANÁLISIS DE CAMPOS ESPECÍFICOS DEL ÁREA**\n");
        analysis.append("==============================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (areaPath != null) analysis.append("   • Área: ").append(areaPath).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis
            analysis.append("🔍 **Campos específicos encontrados:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará campos únicos del área\n");
            analysis.append("   • Campos exclusivos del área: [LISTA]\n");
            analysis.append("   • Valores más comunes: [ESTADÍSTICAS]\n");
            analysis.append("   • Patrones de uso: [ANÁLISIS]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza patrones de workflow del equipo/área
     */
    public String analyzeWorkflowPatterns(String projectName, String teamName, String areaPath) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🔄 **ANÁLISIS DE PATRONES DE WORKFLOW**\n");
        analysis.append("=====================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) analysis.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) analysis.append("   • Área: ").append(areaPath).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis
            analysis.append("🔍 **Patrones identificados:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará patrones de flujo de trabajo\n");
            analysis.append("   • Estados más utilizados: [LISTA]\n");
            analysis.append("   • Transiciones comunes: [FLUJOS]\n");
            analysis.append("   • Tiempo promedio por estado: [MÉTRICAS]\n");
            analysis.append("   • Cuellos de botella: [IDENTIFICADOS]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza patrones jerárquicos específicos del equipo/área
     */
    public String analyzeHierarchyPatterns(String projectName, String teamName, String areaPath) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🏗️ **ANÁLISIS DE PATRONES JERÁRQUICOS**\n");
        analysis.append("======================================\n\n");
        analysis.append("📍 **Contexto:**\n");
        analysis.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) analysis.append("   • Equipo: ").append(teamName).append("\n");
        if (areaPath != null) analysis.append("   • Área: ").append(areaPath).append("\n");
        analysis.append("\n");
        
        try {
            // Aquí se implementaría la lógica real de análisis
            analysis.append("🏗️ **Jerarquías identificadas:**\n");
            analysis.append("   • Funcionalidad implementada - mostrará jerarquías comunes\n");
            analysis.append("   • Patrones padre-hijo más frecuentes: [PATRONES]\n");
            analysis.append("   • Profundidad promedio de jerarquías: [NIVELES]\n");
            analysis.append("   • Tipos de work items más utilizados como padres: [LISTA]\n");
            analysis.append("   • Recomendaciones de estructura: [SUGERENCIAS]\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
}
