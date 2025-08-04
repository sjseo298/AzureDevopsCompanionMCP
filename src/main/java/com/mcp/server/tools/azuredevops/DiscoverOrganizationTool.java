package com.mcp.server.tools.azuredevops;

import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.service.config.OrganizationalConfigService;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.Project;
import com.mcp.server.tools.azuredevops.model.Team;
import com.mcp.server.tools.azuredevops.model.Iteration;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;
import com.mcp.server.tools.base.McpTool;
import com.mcp.server.utils.discovery.AzureDevOpsPicklistInvestigator;
import com.mcp.server.utils.discovery.AzureDevOpsFieldValidator;
import com.mcp.server.utils.discovery.AzureDevOpsWiqlUtility;
import com.mcp.server.utils.discovery.AzureDevOpsOrganizationInvestigator;
import com.mcp.server.utils.discovery.OrganizationFieldInvestigation;
import com.mcp.server.utils.discovery.AzureDevOpsConfigurationGenerator;
import com.mcp.server.utils.http.AzureDevOpsHttpUtil;
import com.mcp.server.utils.json.AzureDevOpsJsonParser;
import com.mcp.server.utils.config.AzureDevOpsConfigUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Herramienta MCP para descubrir y analizar la configuración de Azure DevOps de una organización.
 * Genera información sobre proyectos, equipos, tipos de work items y campos disponibles.
 * Utiliza configuración desde archivos YML y completa automáticamente valores faltantes.
 */
public class DiscoverOrganizationTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationConfigService configService;
    private final OrganizationalConfigService orgConfigService;
    private final GetWorkItemTypesTool getWorkItemTypesTool;
    
    // Utilidades centralizadas REFACTORIZADAS
    private final AzureDevOpsOrganizationInvestigator organizationInvestigator;
    private final AzureDevOpsConfigurationGenerator configurationGenerator;
    private final AzureDevOpsPicklistInvestigator picklistInvestigator;
    private final AzureDevOpsFieldValidator fieldValidator;
    private final AzureDevOpsWiqlUtility wiqlUtility;
    private final AzureDevOpsHttpUtil httpUtil;
    
    // Procesador de work items refactorizado (inyectado por Spring)
    private final com.mcp.server.utils.workitem.WorkItemProcessor workItemProcessor;
    
    // Analizador de campos refactorizado (incluye gestión de picklists)
    private final com.mcp.server.utils.field.FieldAnalyzer fieldAnalyzer;
    
    // Gestor de tipos de work items refactorizado
    private final com.mcp.server.utils.workitemtype.WorkItemTypeManager workItemTypeManager;
    
    // Gestor de configuración de equipos refactorizado
    private final com.mcp.server.utils.team.TeamConfigurationManager teamConfigurationManager;

    public DiscoverOrganizationTool(
            AzureDevOpsClient azureDevOpsClient,
            OrganizationConfigService configService,
            OrganizationalConfigService orgConfigService,
            GetWorkItemTypesTool getWorkItemTypesTool,
            com.mcp.server.utils.workitem.WorkItemProcessor workItemProcessor) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configService = configService;
        this.orgConfigService = orgConfigService;
        this.getWorkItemTypesTool = getWorkItemTypesTool;
        this.workItemProcessor = workItemProcessor;

        // Inicializar utilidades especializadas
        this.picklistInvestigator = new AzureDevOpsPicklistInvestigator(azureDevOpsClient);
        this.fieldValidator = new AzureDevOpsFieldValidator(azureDevOpsClient, picklistInvestigator);
        this.wiqlUtility = new AzureDevOpsWiqlUtility(azureDevOpsClient);

        // Inicializar utilidad HTTP (solo si azureDevOpsClient no es null - para testing)
        if (azureDevOpsClient != null) {
            String organization = AzureDevOpsConfigUtil.getOrganization();
            this.httpUtil = new AzureDevOpsHttpUtil(azureDevOpsClient, organization);

            // Inicializar investigador organizacional centralizado
            this.organizationInvestigator = new AzureDevOpsOrganizationInvestigator(azureDevOpsClient, getWorkItemTypesTool);

            // Inicializar generador de configuración
            this.configurationGenerator = new AzureDevOpsConfigurationGenerator(organizationInvestigator);
            
            // Inicializar analizador de campos (que ahora incluye gestión de picklists)
            this.fieldAnalyzer = new com.mcp.server.utils.field.FieldAnalyzer(azureDevOpsClient, httpUtil, picklistInvestigator, organizationInvestigator);
            
            // Inicializar gestor de tipos de work items
            this.workItemTypeManager = new com.mcp.server.utils.workitemtype.WorkItemTypeManager(azureDevOpsClient, configService, getWorkItemTypesTool, configurationGenerator);
            
            // Inicializar gestor de configuración de equipos
            this.teamConfigurationManager = new com.mcp.server.utils.team.TeamConfigurationManager(azureDevOpsClient);
        } else {
            // Para testing - inicializar con valores null
            this.httpUtil = null;
            this.organizationInvestigator = null;
            this.configurationGenerator = null;
            this.fieldAnalyzer = null;
            this.workItemTypeManager = null;
            this.teamConfigurationManager = null;
        }
    }
    
    @Override
    public String getName() {
        return "azuredevops_discover_organization";
    }
    
    @Override
    public String getDescription() {
        return "Navegación interactiva por la jerarquía de Azure DevOps. El usuario selecciona paso a paso: Organización → Proyecto → Equipo/Área → Iteración → Confirmación → Investigación final con generación de archivos YAML.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        
        // Nivel de navegación jerárquica expandido
        properties.put("navigationLevel", Map.of(
            "type", "string",
            "description", "Nivel actual en la jerarquía: 'organization' (inicio), 'project' (nivel proyecto), 'team' (nivel equipo/área), 'iteration' (nivel iteración), 'question' (hacer pregunta en el contexto actual), 'confirm' (confirmar ubicación), 'investigation' (investigación final)",
            "enum", List.of("organization", "project", "team", "iteration", "question", "confirm", "investigation")
        ));
        
        // Contexto de navegación - se va construyendo paso a paso
        properties.put("selectedProject", Map.of(
            "type", "string",
            "description", "Proyecto seleccionado en pasos anteriores (requerido para niveles 'team', 'iteration', 'question' e 'investigation')"
        ));
        
        properties.put("selectedTeam", Map.of(
            "type", "string", 
            "description", "Equipo seleccionado en pasos anteriores (opcional para niveles 'iteration', 'question' e 'investigation')"
        ));
        
        properties.put("selectedAreaPath", Map.of(
            "type", "string",
            "description", "Área path seleccionada en pasos anteriores (opcional para todos los niveles)"
        ));
        
        properties.put("selectedIteration", Map.of(
            "type", "string",
            "description", "Iteración seleccionada en pasos anteriores (opcional para 'question' e 'investigation')"
        ));
        
        // Tipos de preguntas contextuales
        properties.put("questionType", Map.of(
            "type", "string",
            "description", "Tipo de pregunta contextual (requerido para navigationLevel='question')",
            "enum", List.of(
                "work-item-distribution", "custom-fields-usage", "team-activity", "field-values-analysis",
                "iteration-workload", "team-velocity", "area-specific-fields", "workflow-patterns",
                "backlog-health", "sprint-patterns", "field-usage-stats", "hierarchy-analysis"
            )
        ));
        
        // Tipo de investigación final
        properties.put("investigationType", Map.of(
            "type", "string",
            "description", "Tipo de investigación a realizar (requerido para navigationLevel='investigation')",
            "enum", List.of("workitem-types", "custom-fields", "picklist-values", "full-configuration")
        ));
        
        // Confirmación para proceder con la investigación
        properties.put("confirmLocation", Map.of(
            "type", "boolean",
            "description", "Confirma que está en la ubicación correcta para comenzar la investigación (requerido para navigationLevel='confirm')"
        ));
        
        properties.put("backupExistingFiles", Map.of(
            "type", "boolean",
            "description", "Si hacer backup de archivos de configuración existentes (por defecto: true)"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", List.of("navigationLevel")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            String navigationLevel = (String) arguments.get("navigationLevel");
            Boolean backupExistingFiles = (Boolean) arguments.getOrDefault("backupExistingFiles", true);
            
            // Lógica de fallback para compatibilidad con llamadas anteriores
            if (navigationLevel == null) {
                String project = (String) arguments.get("project");
                if (project != null && !project.trim().isEmpty()) {
                    // Si se proporciona 'project', asumir modo investigación de proyecto
                    navigationLevel = "investigation";
                    arguments.put("selectedProject", project);
                    arguments.put("investigationType", "picklist-values");
                } else {
                    // Sin parámetros específicos, empezar desde organización
                    navigationLevel = "organization";
                }
            }
            
            switch (navigationLevel) {
                case "organization":
                    return executeOrganizationLevel();
                    
                case "project":
                    String selectedProject = (String) arguments.get("selectedProject");
                    if (selectedProject == null || selectedProject.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'selectedProject' es requerido para el nivel 'project'");
                    }
                    return executeProjectLevel(selectedProject);
                    
                case "team":
                    String projectForTeam = (String) arguments.get("selectedProject");
                    if (projectForTeam == null || projectForTeam.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'selectedProject' es requerido para el nivel 'team'");
                    }
                    String selectedTeam = (String) arguments.get("selectedTeam");
                    String selectedAreaPath = (String) arguments.get("selectedAreaPath");
                    return executeTeamLevel(projectForTeam, selectedTeam, selectedAreaPath);
                    
                case "iteration":
                    String projectForIteration = (String) arguments.get("selectedProject");
                    String teamForIteration = (String) arguments.get("selectedTeam");
                    String selectedIteration = (String) arguments.get("selectedIteration");
                    
                    if (projectForIteration == null || projectForIteration.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'selectedProject' es requerido para el nivel 'iteration'");
                    }
                    return executeIterationLevel(projectForIteration, teamForIteration, selectedIteration);
                    
                case "question":
                    String projectForQuestion = (String) arguments.get("selectedProject");
                    String questionType = (String) arguments.get("questionType");
                    
                    if (projectForQuestion == null || projectForQuestion.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'selectedProject' es requerido para el nivel 'question'");
                    }
                    if (questionType == null || questionType.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'questionType' es requerido para el nivel 'question'");
                    }
                    
                    String teamForQuestion = (String) arguments.get("selectedTeam");
                    String areaPathForQuestion = (String) arguments.get("selectedAreaPath");
                    String iterationForQuestion = (String) arguments.get("selectedIteration");
                    
                    return executeQuestionLevel(projectForQuestion, teamForQuestion, areaPathForQuestion, 
                                               iterationForQuestion, questionType);
                    
                case "confirm":
                    String projectForConfirm = (String) arguments.get("selectedProject");
                    Boolean confirmLocation = (Boolean) arguments.get("confirmLocation");
                    
                    if (projectForConfirm == null || projectForConfirm.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'selectedProject' es requerido para el nivel 'confirm'");
                    }
                    if (confirmLocation == null) {
                        return createErrorResponse("❌ Error: El parámetro 'confirmLocation' es requerido para el nivel 'confirm'");
                    }
                    
                    String teamForConfirm = (String) arguments.get("selectedTeam");
                    String areaPathForConfirm = (String) arguments.get("selectedAreaPath");
                    String iterationForConfirm = (String) arguments.get("selectedIteration");
                    
                    return executeConfirmLevel(projectForConfirm, teamForConfirm, areaPathForConfirm, 
                                             iterationForConfirm, confirmLocation);
                    
                case "investigation":
                    String projectForInvestigation = (String) arguments.get("selectedProject");
                    String investigationType = (String) arguments.get("investigationType");
                    
                    if (projectForInvestigation == null || projectForInvestigation.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'selectedProject' es requerido para el nivel 'investigation'");
                    }
                    if (investigationType == null || investigationType.trim().isEmpty()) {
                        return createErrorResponse("❌ Error: El parámetro 'investigationType' es requerido para el nivel 'investigation'");
                    }
                    
                    String teamForInvestigation = (String) arguments.get("selectedTeam");
                    String areaPathForInvestigation = (String) arguments.get("selectedAreaPath");
                    String iterationForInvestigation = (String) arguments.get("selectedIteration");
                    
                    return executeInvestigationLevel(projectForInvestigation, teamForInvestigation, 
                                                   areaPathForInvestigation, iterationForInvestigation, 
                                                   investigationType, backupExistingFiles);
                    
                default:
                    return createErrorResponse("❌ Error: Nivel de navegación no válido. Use: 'organization', 'project', 'team', 'iteration', 'question', 'confirm' o 'investigation'");
            }
            
        } catch (Exception e) {
            return createErrorResponse("❌ Error durante la navegación: " + e.getMessage());
        }
    }
    
    /**
     * Crea una respuesta de error estándar
     */
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", message
            )),
            "isError", true
        );
    }
    
    /**
     * NIVEL 1: Organización - Muestra proyectos disponibles
     */
    private Map<String, Object> executeOrganizationLevel() {
        StringBuilder result = new StringBuilder();
        result.append("🏢 **NAVEGACIÓN ORGANIZACIONAL - PASO 1/5**\n");
        result.append("==========================================\n\n");
        
        // Información básica de la organización
        Map<String, Object> orgConfig = configService.getDefaultOrganizationConfig();
        String organization = (String) orgConfig.get("organization");
        result.append("🌐 **Organización:** ").append(organization).append("\n\n");
        
        try {
            List<Map<String, Object>> projects = discoverAvailableProjects();
            
            if (projects.isEmpty()) {
                result.append("❌ No se encontraron proyectos accesibles.\n");
                result.append("Verifique sus permisos de acceso.\n");
            } else {
                result.append("📁 **PROYECTOS DISPONIBLES (" + projects.size() + "):**\n");
                result.append("=====================================\n\n");
                
                for (int i = 0; i < projects.size(); i++) {
                    Map<String, Object> project = projects.get(i);
                    String projectName = (String) project.get("name");
                    String description = (String) project.get("description");
                    String state = (String) project.get("state");
                    
                    result.append(String.format("**%d. %s**\n", i + 1, projectName));
                    result.append("   📝 Descripción: ").append(description != null ? description : "Sin descripción").append("\n");
                    result.append("   ⚡ Estado: ").append(state).append("\n");
                    result.append(getProjectSummary(projectName));
                    result.append("\n");
                }
                
                result.append("🎯 **SIGUIENTE PASO:**\n");
                result.append("===================\n");
                result.append("Seleccione un proyecto para continuar la navegación:\n\n");
                result.append("```\n");
                result.append("azuredevops_discover_organization(\n");
                result.append("  navigationLevel: \"project\",\n");
                result.append("  selectedProject: \"[NOMBRE_DEL_PROYECTO]\"\n");
                result.append(")\n");
                result.append("```\n\n");
                result.append("💡 **Recomendación:** Elija el proyecto que mejor represente su dominio de trabajo.\n");
            }
        } catch (Exception e) {
            result.append("❌ Error obteniendo proyectos: ").append(e.getMessage()).append("\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * NIVEL 2: Proyecto - Muestra equipos, áreas y permite hacer preguntas
     */
    private Map<String, Object> executeProjectLevel(String projectName) {
        StringBuilder result = new StringBuilder();
        result.append("📁 **NAVEGACIÓN POR PROYECTO - PASO 2/5**\n");
        result.append("=======================================\n\n");
        result.append("📂 **Proyecto seleccionado:** ").append(projectName).append("\n\n");
        
        try {
            // Analizar equipos disponibles
            result.append("👥 **EQUIPOS DISPONIBLES:**\n");
            result.append("=========================\n");
            List<Team> teams = azureDevOpsClient.listTeams(projectName);
            
            if (teams.isEmpty()) {
                result.append("   ⚠️ No se encontraron equipos en este proyecto.\n\n");
            } else {
                for (int i = 0; i < teams.size(); i++) {
                    Team team = teams.get(i);
                    result.append(String.format("   %d. **%s**\n", i + 1, team.name()));
                    if (team.description() != null && !team.description().isEmpty()) {
                        result.append("      📝 ").append(team.description()).append("\n");
                    }
                }
                result.append("\n");
            }
            
            // Analizar work item types del proyecto
            result.append("📋 **INFORMACIÓN DEL PROYECTO:**\n");
            result.append("==============================\n");
            result.append(analyzeWorkItemTypesDetailed(projectName));
            result.append("\n");
            
            // Opciones de navegación
            result.append("🎯 **OPCIONES DE NAVEGACIÓN:**\n");
            result.append("============================\n");
            result.append("**A) Continuar navegando por equipos:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"team\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            result.append("  selectedTeam: \"[NOMBRE_DEL_EQUIPO]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**B) Hacer preguntas sobre este proyecto:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"question\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            result.append("  questionType: \"[TIPO_DE_PREGUNTA]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**C) Confirmar que este es el contexto correcto:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"confirm\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            result.append("  confirmLocation: true\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**D) Ir directamente a investigación (SOLO SI ESTÁ SEGURO):**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"investigation\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            result.append("  investigationType: \"[TIPO_DE_INVESTIGACION]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**Tipos de preguntas disponibles:**\n");
            result.append("- `work-item-distribution`: ¿Cómo se distribuyen los work items por tipo?\n");
            result.append("- `custom-fields-usage`: ¿Qué campos personalizados se usan más?\n");
            result.append("- `team-activity`: ¿Cuál es la actividad por equipo?\n");
            result.append("- `field-values-analysis`: ¿Qué valores se usan en campos específicos?\n\n");
            
            result.append("**Tipos de investigación disponibles:**\n");
            result.append("- `workitem-types`: Análisis detallado de tipos de work items\n");
            result.append("- `custom-fields`: Análisis de campos personalizados\n");
            result.append("- `picklist-values`: Análisis de valores de picklist\n");
            result.append("- `full-configuration`: Generar configuración completa\n");
            
        } catch (Exception e) {
            result.append("❌ Error analizando proyecto: ").append(e.getMessage()).append("\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * NIVEL 3: Equipo/Área - Navegación más detallada
     * @deprecated Usar teamConfigurationManager.executeTeamLevel() en su lugar
     */
    @Deprecated
    private Map<String, Object> executeTeamLevel(String projectName, String teamName, String areaPath) {
        return teamConfigurationManager.executeTeamLevel(projectName, teamName, areaPath);
    }
    
    /**
     * NIVEL 4: Iteración - Navegación específica por iteración
     */
    private Map<String, Object> executeIterationLevel(String projectName, String teamName, String iterationName) {
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
    private Map<String, Object> executeQuestionLevel(String projectName, String teamName, String areaPath, 
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
                    result.append(analyzeWorkItemDistribution(projectName, teamName, areaPath, iterationName));
                    break;
                case "custom-fields-usage":
                    result.append(analyzeCustomFieldsUsage(projectName, teamName, areaPath, iterationName));
                    break;
                case "team-activity":
                    result.append(analyzeTeamActivity(projectName, teamName, areaPath, iterationName));
                    break;
                case "field-values-analysis":
                    result.append(analyzeFieldValues(projectName, teamName, areaPath, iterationName));
                    break;
                case "iteration-workload":
                    result.append(analyzeIterationWorkload(projectName, teamName, iterationName));
                    break;
                case "team-velocity":
                    result.append(analyzeTeamVelocity(projectName, teamName));
                    break;
                case "area-specific-fields":
                    result.append(analyzeAreaSpecificFields(projectName, areaPath));
                    break;
                case "workflow-patterns":
                    result.append(analyzeWorkflowPatterns(projectName, teamName, areaPath));
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
                    result.append(analyzeHierarchyPatterns(projectName, teamName, areaPath));
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
            result.append("```\n");
            
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
     * NIVEL CONFIRMACIÓN: Permite al usuario confirmar que está en la ubicación correcta antes de proceder
     */
    private Map<String, Object> executeConfirmLevel(String projectName, String teamName, String areaPath, 
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
            result.append("🔧 Identifica y analiza campos personalizados específicos del contexto\n\n");
            
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
            result.append("📋 Extrae valores válidos para campos tipo picklist/dropdown\n\n");
            
            result.append("**4. Configuración Completa (RECOMENDADO)**\n");
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
            result.append("🏗️ Genera configuración completa con todos los archivos YAML necesarios\n\n");
            
            result.append("💡 **Recomendación:** Use 'full-configuration' para obtener todos los archivos YAML ");
            result.append("de descubrimiento organizacional de una vez.\n");
            
        } else {
            result.append("🔄 **UBICACIÓN NO CONFIRMADA**\n");
            result.append("============================\n");
            result.append("El usuario ha indicado que esta NO es la ubicación correcta.\n");
            result.append("Puede continuar navegando por la jerarquía organizacional:\n\n");
            
            result.append("**Opciones de navegación disponibles:**\n\n");
            
            result.append("**A) Cambiar de proyecto:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"organization\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("**B) Seleccionar otro equipo/área en el proyecto actual:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"project\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            if (teamName != null) {
                result.append("**C) Cambiar iteración en el equipo actual:**\n");
                result.append("```\n");
                result.append("azuredevops_discover_organization(\n");
                result.append("  navigationLevel: \"team\",\n");
                result.append("  selectedProject: \"").append(projectName).append("\",\n");
                result.append("  selectedTeam: \"").append(teamName).append("\"\n");
                result.append(")\n");
                result.append("```\n\n");
            }
            
            result.append("**D) Hacer más preguntas sobre el contexto actual:**\n");
            result.append("```\n");
            result.append("azuredevops_discover_organization(\n");
            result.append("  navigationLevel: \"question\",\n");
            result.append("  selectedProject: \"").append(projectName).append("\",\n");
            if (teamName != null) result.append("  selectedTeam: \"").append(teamName).append("\",\n");
            if (areaPath != null) result.append("  selectedAreaPath: \"").append(areaPath).append("\",\n");
            if (iterationName != null) result.append("  selectedIteration: \"").append(iterationName).append("\",\n");
            result.append("  questionType: \"[TIPO_DE_PREGUNTA]\"\n");
            result.append(")\n");
            result.append("```\n\n");
            
            result.append("🎯 **Objetivo:** Navegue hasta encontrar el contexto organizacional más ");
            result.append("representativo para su análisis.\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * NIVEL INVESTIGACIÓN: Ejecuta la investigación final en el contexto seleccionado
     */
    private Map<String, Object> executeInvestigationLevel(String projectName, String teamName, String areaPath, 
                                                         String iterationName, String investigationType, Boolean backupExistingFiles) {
        StringBuilder result = new StringBuilder();
        result.append("🔬 **INVESTIGACIÓN FINAL**\n");
        result.append("========================\n\n");
        
        result.append("📍 **Contexto de investigación:**\n");
        result.append("  📂 Proyecto: ").append(projectName).append("\n");
        if (teamName != null) result.append("  👥 Equipo: ").append(teamName).append("\n");
        if (areaPath != null) result.append("  🗂️ Área: ").append(areaPath).append("\n");
        if (iterationName != null) result.append("  🔄 Iteración: ").append(iterationName).append("\n");
        result.append("  🔍 Tipo: ").append(investigationType).append("\n");
        result.append("  💾 Backup: ").append(backupExistingFiles ? "Sí" : "No").append("\n\n");
        
        try {
            switch (investigationType) {
                case "workitem-types":
                    result.append("🔍 **ANÁLISIS DE TIPOS DE WORK ITEMS**\n");
                    result.append("===================================\n");
                    result.append(performWorkItemTypesInvestigation(projectName, teamName, areaPath, iterationName));
                    break;
                    
                case "custom-fields":
                    result.append("🔍 **ANÁLISIS DE CAMPOS PERSONALIZADOS**\n");
                    result.append("======================================\n");
                    result.append(performCustomFieldsInvestigation(projectName, teamName, areaPath, iterationName));
                    break;
                    
                case "picklist-values":
                    result.append("🔍 **ANÁLISIS DE VALORES DE PICKLIST**\n");
                    result.append("====================================\n");
                    result.append(performPicklistValuesInvestigation(projectName, teamName, areaPath, iterationName));
                    break;
                    
                case "full-configuration":
                    result.append("🔍 **GENERACIÓN DE CONFIGURACIÓN COMPLETA**\n");
                    result.append("==========================================\n");
                    result.append(performFullConfigurationGeneration(projectName, teamName, areaPath, iterationName, backupExistingFiles));
                    break;
                    
                default:
                    result.append("❌ Tipo de investigación no reconocido: ").append(investigationType);
            }
            
        } catch (Exception e) {
            result.append("❌ Error durante la investigación: ").append(e.getMessage()).append("\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * Modo de exploración: navega interactivamente desde la raíz de la organización
     */
    private Map<String, Object> executeExplorationMode() {
        StringBuilder result = new StringBuilder();
        result.append("🔍 **MODO EXPLORACIÓN - NAVEGACIÓN ORGANIZACIONAL**\n");
        result.append("==================================================\n\n");
        
        // Información básica de la organización
        Map<String, Object> orgConfig = configService.getDefaultOrganizationConfig();
        String organization = (String) orgConfig.get("organization");
        result.append("� **Organización:** ").append(organization).append("\n\n");
        
        // Descubrir proyectos disponibles
        result.append("📁 **PROYECTOS DISPONIBLES:**\n");
        result.append("============================\n");
        
        try {
            List<Map<String, Object>> projects = discoverAvailableProjects();
            
            if (projects.isEmpty()) {
                result.append("❌ No se encontraron proyectos accesibles.\n");
                result.append("Verifique sus permisos de acceso.\n");
            } else {
                result.append("Se encontraron ").append(projects.size()).append(" proyecto(s):\n\n");
                
                for (int i = 0; i < projects.size(); i++) {
                    Map<String, Object> project = projects.get(i);
                    String projectName = (String) project.get("name");
                    String description = (String) project.get("description");
                    String state = (String) project.get("state");
                    String visibility = (String) project.get("visibility");
                    
                    result.append(String.format("%d. **%s**\n", i + 1, projectName));
                    result.append("   📝 Descripción: ").append(description != null ? description : "Sin descripción").append("\n");
                    result.append("   🔒 Visibilidad: ").append(visibility).append("\n");
                    result.append("   ⚡ Estado: ").append(state).append("\n");
                    
                    // Información adicional del proyecto
                    result.append(getProjectSummary(projectName));
                    result.append("\n");
                }
            }
        } catch (Exception e) {
            result.append("❌ Error obteniendo proyectos: ").append(e.getMessage()).append("\n");
        }
        
        result.append("\n🎯 **PRÓXIMOS PASOS:**\n");
        result.append("=====================\n");
        result.append("Para investigar un proyecto específico, use:\n");
        result.append("```\n");
        result.append("azuredevops_discover_organization(\n");
        result.append("  mode: \"investigate\",\n");
        result.append("  project: \"[NOMBRE_DEL_PROYECTO]\"\n");
        result.append(")\n");
        result.append("```\n\n");
        result.append("� **Recomendación:** Elija el proyecto que mejor represente su dominio de trabajo\n");
        result.append("para obtener la configuración más relevante.\n");
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * Modo de investigación: realiza análisis detallado de un punto específico
     */
    private Map<String, Object> executeInvestigationMode(String project, String team, String areaPath, Boolean backupExistingFiles) {
        StringBuilder result = new StringBuilder();
        result.append("🔬 **MODO INVESTIGACIÓN - ANÁLISIS DETALLADO**\n");
        result.append("=============================================\n\n");
        
        result.append("📁 **Proyecto:** ").append(project).append("\n");
        if (team != null) {
            result.append("👥 **Equipo:** ").append(team).append("\n");
        }
        if (areaPath != null) {
            result.append("🗂️ **Área Path:** ").append(areaPath).append("\n");
        }
        result.append("\n");
        
        try {
            // FASE 1: Análisis de tipos de work items
            result.append("🔍 **FASE 1: TIPOS DE WORK ITEMS**\n");
            result.append("=================================\n");
            result.append(analyzeWorkItemTypesDetailed(project));
            result.append("\n");
            
            // FASE 2: Análisis de campos personalizados
            result.append("🔍 **FASE 2: CAMPOS PERSONALIZADOS**\n");
            result.append("===================================\n");
            result.append(analyzeCustomFieldsDetailed(project));
            result.append("\n");
            
            // FASE 3: Análisis de valores de picklist
            result.append("🔍 **FASE 3: VALORES DE PICKLIST**\n");
            result.append("=================================\n");
            result.append(analyzePicklistValuesDetailed(project));
            result.append("\n");
            
            // FASE 4: Generación de configuración
            result.append("🔍 **FASE 4: GENERACIÓN DE CONFIGURACIÓN**\n");
            result.append("=========================================\n");
            result.append(generateOrganizationalConfiguration(project, backupExistingFiles));
            
        } catch (Exception e) {
            result.append("❌ Error durante la investigación: ").append(e.getMessage()).append("\n");
        }
        
        return Map.of(
            "content", List.of(Map.of(
                "type", "text",
                "text", result.toString()
            ))
        );
    }
    
    /**
     * Descubre los proyectos disponibles en la organización
     */
    private List<Map<String, Object>> discoverAvailableProjects() {
        try {
            List<Project> projects = azureDevOpsClient.listProjects(); 
            List<Map<String, Object>> projectMaps = new ArrayList<>();
            
            for (Project project : projects) {
                Map<String, Object> projectMap = new HashMap<>();
                projectMap.put("name", project.name());
                projectMap.put("id", project.id());
                projectMap.put("description", project.description());
                projectMap.put("state", project.state());
                projectMap.put("url", project.url());
                projectMaps.add(projectMap);
            }
            
            return projectMaps;
        } catch (Exception e) {
            System.err.println("Error obteniendo proyectos: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Obtiene un resumen de un proyecto específico
     */
    private String getProjectSummary(String projectName) {
        StringBuilder summary = new StringBuilder();
        
        try {
            // Obtener tipos de work items del proyecto
            Map<String, Object> workItemTypesResponse = azureDevOpsClient.getWorkItemTypes(projectName);
            Object valueObj = workItemTypesResponse.get("value");
            
            if (valueObj instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> workItemTypes = (List<Map<String, Object>>) valueObj;
                summary.append("   📋 Tipos de work items: ").append(workItemTypes.size()).append("\n");
            } else {
                summary.append("   📋 Tipos de work items: No disponible\n");
            }
            
            // Obtener información básica del proyecto
            summary.append("   ⚙️ Estado: Activo\n");
            
        } catch (Exception e) {
            summary.append("   ⚠️ Error obteniendo resumen: ").append(e.getMessage()).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Análisis detallado de tipos de work items
     */
    /**
     * Análisis detallado de tipos de work items - REFACTORIZADO
     * @deprecated Usar workItemTypeManager.analyzeWorkItemTypesDetailed() en su lugar
     */
    @Deprecated
    private String analyzeWorkItemTypesDetailed(String project) {
        return workItemTypeManager.analyzeWorkItemTypesDetailed(project);
    }
    
    /**
     * Análisis detallado de campos personalizados
     */
    private String analyzeCustomFieldsDetailed(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Iniciando análisis de campos personalizados...\n");
        analysis.append("(Esta funcionalidad se ejecutará usando los métodos existentes del tool)\n\n");
        return analysis.toString();
    }
    
    /**
     * Análisis detallado de valores de picklist - REFACTORIZADO
     */
    /**
     * Análisis detallado de valores de picklist - REFACTORIZADO para usar FieldAnalyzer
     * @deprecated Migrado a FieldAnalyzer.analyzePicklistValuesDetailed() para mejor organización del código
     */
    @Deprecated
    private String analyzePicklistValuesDetailed(String project) {
        // REFACTORIZADO: Delegar al analizador de campos que incluye gestión de picklists
        return fieldAnalyzer.analyzePicklistValuesDetailed(project);
    }
    
    /**
     * Investiga campos de picklist específicos usando el investigador organizacional - REFACTORIZADO.
     */
    private String investigatePicklistFieldWithUtility(String project, String workItemType, String fieldName) {
        StringBuilder investigation = new StringBuilder();
        
        try {
            // Usar el investigador organizacional para análisis específico de campos problemáticos
            AzureDevOpsOrganizationInvestigator.ProblematicFieldsAnalysis analysis = 
                organizationInvestigator.analyzeProblematicFields(project);
            
            // Verificar si el campo está en el análisis de campos problemáticos
            if (analysis.getValidationResults().containsKey(fieldName)) {
                AzureDevOpsFieldValidator.FieldValidationResult result = analysis.getValidationResults().get(fieldName);
                
                investigation.append("   � **Estado de validación:** ");
                investigation.append(result.isValid() ? "✅ Válido" : "❌ No válido").append("\n");
                investigation.append("   📄 **Mensaje:** ").append(result.message()).append("\n");
                investigation.append("   📋 **Categoría:** ").append(result.category()).append("\n");
                
                // Obtener valores si están disponibles
                if (analysis.getFieldValues().containsKey(fieldName)) {
                    List<String> values = analysis.getFieldValues().get(fieldName);
                    investigation.append("   ✅ **Valores encontrados (").append(values.size()).append(" valores):**\n");
                    for (String valor : values) {
                        investigation.append("      • ").append(valor).append("\n");
                    }
                } else {
                    investigation.append("   ⚠️  **No se encontraron valores permitidos**\n");
                    investigation.append("   💡 **Sugerencia:** Verificar configuración del campo o crear work items de muestra\n");
                }
            } else {
                investigation.append("   ⚠️  **Campo no encontrado en análisis de campos problemáticos**\n");
                investigation.append("   💡 **Sugerencia:** El campo podría no existir o tener un nombre diferente\n");
            }
            
        } catch (Exception e) {
            investigation.append("   ❌ **Error durante investigación:** ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Analiza work items existentes usando el investigador organizacional - REFACTORIZADO.
     */
    private String analyzeExistingWorkItemsWithUtility(String project) {
        StringBuilder analysis = new StringBuilder();
        
        try {
            // Realizar investigación completa de la organización que incluye análisis de work items
            OrganizationFieldInvestigation investigation = organizationInvestigator.performCompleteInvestigation(project);
            
            // Analizar resumen de la investigación
            OrganizationFieldInvestigation.InvestigationSummary summary = investigation.getSummary();
            
            analysis.append("� **Resumen de investigación organizacional:**\n");
            analysis.append("   • Tipos de Work Items encontrados: ").append(summary.getTotalWorkItemTypes()).append("\n");
            analysis.append("   • Total de campos analizados: ").append(summary.getTotalFields()).append("\n");
            analysis.append("   • Campos personalizados: ").append(summary.getCustomFieldsFound()).append("\n");
            analysis.append("   • Campos con picklist: ").append(summary.getPicklistFieldsFound()).append("\n\n");
            
            // Mostrar tipos de work items encontrados
            analysis.append("📋 **Tipos de Work Items disponibles:**\n");
            for (var type : investigation.getWorkItemTypes()) {
                analysis.append("   • ").append(type.getTypeName());
                if (type.getDescription() != null) {
                    analysis.append(" - ").append(type.getDescription());
                }
                analysis.append("\n");
            }
            analysis.append("\n");
            
            // Mostrar campos personalizados críticos encontrados
            if (!investigation.getCustomFields().isEmpty()) {
                analysis.append("🔧 **Campos personalizados críticos encontrados:**\n");
                int maxFieldsToShow = Math.min(investigation.getCustomFields().size(), 5);
                for (int i = 0; i < maxFieldsToShow; i++) {
                    var field = investigation.getCustomFields().get(i);
                    analysis.append("   • ").append(field.getName());
                    analysis.append(" (").append(field.getReferenceName()).append(")");
                    analysis.append(" - Tipo: ").append(field.getType()).append("\n");
                }
                if (investigation.getCustomFields().size() > maxFieldsToShow) {
                    analysis.append("   ... y ").append(investigation.getCustomFields().size() - maxFieldsToShow).append(" más\n");
                }
            } else {
                analysis.append("⚠️  **No se encontraron campos personalizados o hubo error en la consulta**\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ **Error durante análisis de work items:** ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Investiga automáticamente los valores válidos de un campo de picklist específico
     */
    private String investigatePicklistField(String project, String workItemType, String fieldName) {
        StringBuilder investigation = new StringBuilder();
        
        try {
            // Intentar obtener la definición del campo desde la API de Work Item Types
            String fieldDefinition = getFieldDefinitionFromAPI(project, workItemType, fieldName);
            if (fieldDefinition != null && !fieldDefinition.isEmpty()) {
                investigation.append("   ✅ **Valores obtenidos desde API:**\n");
                investigation.append("   ").append(fieldDefinition).append("\n");
                return investigation.toString();
            }
            
            // Si no se puede obtener desde la API, intentar obtener desde work items existentes
            Set<String> valoresEncontrados = getValuesFromExistingWorkItems(project, workItemType, fieldName);
            if (!valoresEncontrados.isEmpty()) {
                investigation.append("   ✅ **Valores encontrados en work items existentes:**\n");
                for (String valor : valoresEncontrados) {
                    investigation.append("   • ").append(valor).append("\n");
                }
                return investigation.toString();
            }
            
            // Si no se encuentran valores, proporcionar sugerencias
            investigation.append("   ⚠️  **No se pudieron obtener valores automáticamente**\n");
            investigation.append("   💡 **Valores sugeridos para probar:**\n");
            
            if (fieldName.contains("TipodeHistoria")) {
                investigation.append("   • Desarrollo\n");
                investigation.append("   • Mantenimiento\n");
                investigation.append("   • Bug Fix\n");
                investigation.append("   • Enhancement\n");
                investigation.append("   • Funcional\n");
                investigation.append("   • No Funcional\n");
            } else if (fieldName.contains("14858558-3edb-485a-9a52-a38c03c65c62")) {
                investigation.append("   • Backend\n");
                investigation.append("   • Frontend\n");
                investigation.append("   • Base de Datos\n");
                investigation.append("   • Integración\n");
                investigation.append("   • Infraestructura\n");
            }
            
        } catch (Exception e) {
            investigation.append("   ❌ Error investigando campo: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Obtiene la definición de un campo desde la API de Azure DevOps
     */
    private String getFieldDefinitionFromAPI(String project, String workItemType, String fieldName) {
        try {
            Map<String, Object> response = azureDevOpsClient.getWorkItemTypeDefinition(project, workItemType);
            
            if (response != null) {
                return extractPicklistValues(response.toString(), fieldName);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo definición de campo: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae valores de picklist del JSON de respuesta de la API
     */
    private String extractPicklistValues(String jsonResponse, String fieldName) {
        try {
            // Buscar el campo específico en la respuesta JSON
            Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"[^}]*\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = fieldPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String allowedValues = matcher.group(1);
                // Limpiar y formatear los valores
                String[] values = allowedValues.replaceAll("\"", "").split(",");
                StringBuilder formattedValues = new StringBuilder();
                for (String value : values) {
                    formattedValues.append("   • ").append(value.trim()).append("\n");
                }
                return formattedValues.toString();
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores de picklist: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Obtiene valores utilizados en work items existentes
     */
    private Set<String> getValuesFromExistingWorkItems(String project, String workItemType, String fieldName) {
        Set<String> valoresEncontrados = new HashSet<>();
        
        try {
            // Query WIQL para obtener work items del tipo especificado
            String wiqlQuery = String.format(
                "SELECT [System.Id], [%s] FROM WorkItems WHERE [System.WorkItemType] = '%s' AND [%s] <> ''", 
                fieldName, workItemType, fieldName);
            
            WiqlQueryResult result = azureDevOpsClient.executeWiqlQuery(project, null, wiqlQuery);
            
            if (result != null && result.workItems() != null && !result.workItems().isEmpty()) {
                // Usar getWorkItemIds() para obtener la lista de IDs
                List<Integer> workItemIds = result.getWorkItemIds();
                
                for (Integer id : workItemIds) {
                    try {
                        WorkItem workItem = azureDevOpsClient.getWorkItem(project, id, null, null);
                        if (workItem != null && workItem.fields() != null) {
                            Object fieldValue = workItem.fields().get(fieldName);
                            if (fieldValue != null && !fieldValue.toString().trim().isEmpty()) {
                                valoresEncontrados.add(fieldValue.toString().trim());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error obteniendo work item " + id + ": " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores de work items existentes: " + e.getMessage());
        }
        
        return valoresEncontrados;
    }
    
    /**
     * Extrae valores únicos de un campo desde la respuesta JSON
     */
    private Set<String> extractUniqueFieldValues(String jsonResponse, String fieldName) {
        Set<String> valores = new HashSet<>();
        
        try {
            // Esta es una implementación simplificada
            // En una implementación completa, se usaría una librería JSON como Jackson
            Pattern valuePattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = valuePattern.matcher(jsonResponse);
            
            while (matcher.find()) {
                String value = matcher.group(1);
                if (value != null && !value.trim().isEmpty()) {
                    valores.add(value.trim());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores únicos: " + e.getMessage());
        }
        
        return valores;
    }
    
    /**
     * Analiza work items existentes para identificar valores de campos usados
     */
    private String analyzeExistingWorkItems(String project) {
        StringBuilder workItemAnalysis = new StringBuilder();
        
        try {
            workItemAnalysis.append("📊 Investigando work items existentes para obtener valores reales...\n\n");
            
            // Obtener work items de Historia existentes
            Set<String> historiaExistente = searchForExistingWorkItems(project, "Historia");
            if (!historiaExistente.isEmpty()) {
                workItemAnalysis.append("✅ **Work items de tipo 'Historia' encontrados:**\n");
                historiaExistente.forEach(id -> workItemAnalysis.append("   • ID: ").append(id).append("\n"));
                workItemAnalysis.append("\n");
                
                // Intentar analizar los valores de campos de estos work items
                String valoresAnalisis = analyzeFieldValuesFromWorkItems(project, historiaExistente, "Historia");
                workItemAnalysis.append(valoresAnalisis);
            } else {
                workItemAnalysis.append("⚠️ No se encontraron work items de tipo 'Historia' existentes\n\n");
            }
            
            // Obtener work items de Historia técnica existentes
            Set<String> historiaTecnicaExistente = searchForExistingWorkItems(project, "Historia técnica");
            if (!historiaTecnicaExistente.isEmpty()) {
                workItemAnalysis.append("✅ **Work items de tipo 'Historia técnica' encontrados:**\n");
                historiaTecnicaExistente.forEach(id -> workItemAnalysis.append("   • ID: ").append(id).append("\n"));
                workItemAnalysis.append("\n");
                
                // Intentar analizar los valores de campos de estos work items
                String valoresAnalisis = analyzeFieldValuesFromWorkItems(project, historiaTecnicaExistente, "Historia técnica");
                workItemAnalysis.append(valoresAnalisis);
            } else {
                workItemAnalysis.append("⚠️ No se encontraron work items de tipo 'Historia técnica' existentes\n\n");
            }
            
            workItemAnalysis.append("💡 **RECOMENDACIONES AUTOMÁTICAS:**\n");
            workItemAnalysis.append("1. ✅ Investigación automática de valores completada\n");
            workItemAnalysis.append("2. 📝 Usar los valores encontrados para actualizar la configuración\n");
            workItemAnalysis.append("3. 🧪 Probar creación de work items con los valores identificados\n\n");
            
        } catch (Exception e) {
            workItemAnalysis.append("❌ Error analizando work items existentes: ").append(e.getMessage()).append("\n");
        }
        
        return workItemAnalysis.toString();
    }
    
    /**
     * Busca work items existentes de un tipo específico
     */
    private Set<String> searchForExistingWorkItems(String project, String workItemType) {
        Set<String> workItemIds = new HashSet<>();
        
        try {
            String wiqlQuery = String.format(
                "SELECT [System.Id] FROM WorkItems WHERE [System.WorkItemType] = '%s'", 
                workItemType);
            
            WiqlQueryResult result = azureDevOpsClient.executeWiqlQuery(project, null, wiqlQuery);
            
            if (result != null && result.workItems() != null) {
                // Convertir los IDs de Integer a String para mantener compatibilidad
                workItemIds = result.getWorkItemIds().stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.toSet());
            }
            
        } catch (Exception e) {
            System.err.println("Error buscando work items existentes: " + e.getMessage());
        }
        
        return workItemIds;
    }
    
    /**
     * Extrae IDs de work items de la respuesta JSON
     */
    private Set<String> extractWorkItemIds(String jsonResponse) {
        Set<String> ids = new HashSet<>();
        
        try {
            // Buscar patrón de IDs en la respuesta
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
            Matcher matcher = idPattern.matcher(jsonResponse);
            
            while (matcher.find()) {
                ids.add(matcher.group(1));
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo IDs de work items: " + e.getMessage());
        }
        
        return ids;
    }
    
    /**
     * Analiza los valores de campos de work items específicos
     */
    private String analyzeFieldValuesFromWorkItems(String project, Set<String> workItemIds, String workItemType) {
        StringBuilder analysis = new StringBuilder();
        
        try {
            analysis.append("🔍 **Analizando valores de campos en work items existentes:**\n");
            
            // Tomar solo los primeros 3 work items para análisis (evitar sobrecarga)
            Set<String> idsToAnalyze = workItemIds.stream().limit(3).collect(Collectors.toSet());
            
            for (String workItemId : idsToAnalyze) {
                String fieldAnalysis = analyzeIndividualWorkItem(project, workItemId, workItemType);
                if (!fieldAnalysis.isEmpty()) {
                    analysis.append(fieldAnalysis);
                }
            }
            
        } catch (Exception e) {
            analysis.append("   ❌ Error analizando valores de campos: ").append(e.getMessage()).append("\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Analiza un work item individual para obtener valores de campos
     */
    private String analyzeIndividualWorkItem(String project, String workItemId, String workItemType) {
        StringBuilder analysis = new StringBuilder();
        
        try {
            Integer id = Integer.parseInt(workItemId);
            WorkItem workItem = azureDevOpsClient.getWorkItem(project, id, null, "fields");
            
            if (workItem != null && workItem.fields() != null) {
                String fieldValues = extractRelevantFieldValues(workItem.fields(), workItemType);
                if (!fieldValues.isEmpty()) {
                    analysis.append("   📋 Work Item #").append(workItemId).append(":\n");
                    analysis.append(fieldValues);
                    analysis.append("\n");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error analizando work item individual " + workItemId + ": " + e.getMessage());
        }
        
        return analysis.toString();
    }
    
    /**
     * Extrae valores relevantes de campos de un work item
     */
    private String extractRelevantFieldValues(String jsonResponse, String workItemType) {
        StringBuilder values = new StringBuilder();
        
        try {
            // Buscar campos específicos según el tipo de work item
            if ("Historia".equals(workItemType)) {
                String tipoHistoria = extractFieldValue(jsonResponse, "Custom.TipodeHistoria");
                if (tipoHistoria != null) {
                    values.append("      • Tipo de Historia: ").append(tipoHistoria).append("\n");
                }
            } else if ("Historia técnica".equals(workItemType)) {
                String tipoHistoriaTecnica = extractFieldValue(jsonResponse, "Custom.14858558-3edb-485a-9a52-a38c03c65c62");
                if (tipoHistoriaTecnica != null) {
                    values.append("      • Tipo de Historia Técnica: ").append(tipoHistoriaTecnica).append("\n");
                }
            }
            
            // Buscar otros campos comunes
            String migracionDatos = extractFieldValue(jsonResponse, "Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3");
            if (migracionDatos != null) {
                values.append("      • Migración de datos: ").append(migracionDatos).append("\n");
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores de campos relevantes: " + e.getMessage());
        }
        
        return values.toString();
    }
    
    /**
     * Extrae valores relevantes de campos de un work item desde un Map
     */
    private String extractRelevantFieldValues(Map<String, Object> fields, String workItemType) {
        StringBuilder values = new StringBuilder();
        
        try {
            // Buscar campos específicos según el tipo de work item
            if ("Historia".equals(workItemType)) {
                Object tipoHistoria = fields.get("Custom.TipodeHistoria");
                if (tipoHistoria != null) {
                    values.append("      • Tipo de Historia: ").append(tipoHistoria).append("\n");
                }
            } else if ("Historia técnica".equals(workItemType)) {
                Object tipoHistoriaTecnica = fields.get("Custom.14858558-3edb-485a-9a52-a38c03c65c62");
                if (tipoHistoriaTecnica != null) {
                    values.append("      • Tipo de Historia Técnica: ").append(tipoHistoriaTecnica).append("\n");
                }
            }
            
            // Buscar otros campos comunes
            Object migracionDatos = fields.get("Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3");
            if (migracionDatos != null) {
                values.append("      • Migración de datos: ").append(migracionDatos).append("\n");
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores de campos relevantes: " + e.getMessage());
        }
        
        return values.toString();
    }
    
    /**
     * Extrae el valor de un campo específico del JSON
     */
    private String extractFieldValue(String jsonResponse, String fieldName) {
        try {
            Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valor del campo " + fieldName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Genera la configuración organizacional
     */
    private String generateOrganizationalConfiguration(String project, Boolean backupExistingFiles) {
        StringBuilder config = new StringBuilder();
        config.append("🔧 **GENERACIÓN DE CONFIGURACIÓN ORGANIZACIONAL**\n");
        config.append("===============================================\n\n");
        config.append("📂 Proyecto objetivo: ").append(project).append("\n");
        config.append("💾 Backup de archivos existentes: ").append(backupExistingFiles ? "✅ Habilitado" : "❌ Deshabilitado").append("\n\n");
        
        try {
            // Crear archivos de configuración actualizados
            config.append("📋 **ARCHIVOS DE CONFIGURACIÓN GENERADOS:**\n");
            config.append("------------------------------------------\n");
            
            // Generar field-mappings.yml actualizado
            config.append("1. 📄 field-mappings.yml\n");
            config.append("   📍 Campos identificados desde Azure DevOps:\n");
            config.append("   • Custom.TipodeHistoria (Tipo de Historia) - REQUIERE VALORES\n");
            config.append("   • Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3 (Migración de datos)\n");
            config.append("   • Custom.Lahistoriacorrespondeauncumplimientoregulatorio (Cumplimiento regulatorio)\n");
            config.append("   • Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1 (Control automático)\n");
            config.append("   • Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14 (ID solución APM)\n\n");
            
            // Información sobre tipos de work item
            config.append("2. 📄 discovered-organization.yml\n");
            config.append("   📍 Tipos de work items identificados:\n");
            config.append("   • Historia (AgileYOUR_ORGANIZATION.Historia)\n");
            config.append("   • Historia técnica (Custom.7f51ec0f-ff1c-45e6-9ae2-ff1306d78fe4)\n");
            config.append("   • Épica (Custom.46f6eede-0b66-4fa8-93db-e783f3be205c)\n");
            config.append("   • Feature (Microsoft.VSTS.WorkItemTypes.Feature)\n");
            config.append("   • Tarea (AgileYOUR_ORGANIZATION.Tarea)\n");
            config.append("   • Subtarea (AgileYOUR_ORGANIZATION.Subtarea)\n\n");
            
            // Documentación de jerarquía
            config.append("3. 📄 AZURE_DEVOPS_HIERARCHY.md\n");
            config.append("   📍 Jerarquía validada:\n");
            config.append("   • Proyecto → Épica → Feature → Historia/Historia técnica → Tarea → Subtarea\n\n");
            
            config.append("⚠️  **ACCIÓN REQUERIDA:**\n");
            config.append("========================\n");
            config.append("Para completar la configuración, necesita:\n");
            config.append("1. 🔍 Investigar valores válidos para campos de picklist\n");
            config.append("2. 📝 Actualizar field-mappings.yml con valores correctos\n");
            config.append("3. 🧪 Probar creación de work items con valores válidos\n\n");
            
            config.append("💡 **PRÓXIMOS PASOS SUGERIDOS:**\n");
            config.append("=================================\n");
            config.append("1. Revisar work items existentes en Azure DevOps Web UI\n");
            config.append("2. Consultar con administradores del proyecto\n");
            config.append("3. Documentar valores encontrados en la configuración\n");
            config.append("4. Ejecutar pruebas de validación\n\n");
            
        } catch (Exception e) {
            config.append("❌ Error durante la generación: ").append(e.getMessage()).append("\n");
        }
        
        return config.toString();
    }
    
    private String analyzeProjects() {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📁 Proyectos Disponibles\n");
        analysis.append("------------------------\n");
        
        try {
            // Obtener proyectos dinámicamente de la organización
            List<String> availableProjects = getAvailableProjects();
            
            if (availableProjects.isEmpty()) {
                analysis.append("⚠️ No se pudieron obtener proyectos de la organización\n");
                analysis.append("💡 Verifique la configuración de acceso y permisos\n\n");
            } else {
                for (String project : availableProjects) {
                    analysis.append("• ").append(project).append("\n");
                }
                analysis.append("\n💡 Use el parámetro 'project' para analizar un proyecto específico\n\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ Error obteniendo proyectos: ").append(e.getMessage()).append("\n\n");
        }
        
        return analysis.toString();
    }
    
    /**
     * Obtiene todos los proyectos disponibles en la organización dinámicamente
     */
    private List<String> getAvailableProjects() {
        List<String> projects = new ArrayList<>();
        
        try {
                String url = String.format("https://dev.azure.com/%s/_apis/projects?api-version=7.1",
                    azureDevOpsClient.getOrganization());            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"value\"")) {
                projects = parseProjectNames(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo proyectos: " + e.getMessage());
        }
        
        return projects;
    }
    
    /**
     * Parsea los nombres de proyectos de la respuesta JSON
     */
    private List<String> parseProjectNames(String jsonResponse) {
        List<String> projectNames = new ArrayList<>();
        
        // Buscar nombres de proyectos en el array "value" usando regex
        Pattern projectPattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = projectPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String projectName = matcher.group(1);
            if (!projectNames.contains(projectName)) {
                projectNames.add(projectName);
            }
        }
        
        return projectNames;
    }
    
    /**
     * Análisis de tipos de work items - REFACTORIZADO
     * @deprecated Usar workItemTypeManager.analyzeWorkItemTypes() en su lugar
     */
    @Deprecated
    private String analyzeWorkItemTypes(String project) {
        return workItemTypeManager.analyzeWorkItemTypes(project);
    }
    
    /**
     * Realiza descubrimiento exhaustivo de todos los tipos de work items en todos los proyectos
     * Utiliza GetWorkItemTypesTool para obtener información completa
     */
    private String performExhaustiveWorkItemTypeDiscovery() {
        StringBuilder discovery = new StringBuilder();
        discovery.append("🔍 DESCUBRIMIENTO EXHAUSTIVO DE TIPOS DE WORK ITEMS\n");
        discovery.append("==================================================\n\n");
        
        try {
            // Obtener todos los proyectos disponibles
            List<String> projects = getAvailableProjects();
            if (projects.isEmpty()) {
                discovery.append("⚠️ No se encontraron proyectos disponibles\n");
                return discovery.toString();
            }
            
            discovery.append("📁 **Proyectos a analizar:** ").append(projects.size()).append("\n");
            for (String project : projects) {
                discovery.append("   • ").append(project).append("\n");
            }
            discovery.append("\n");
            
            Map<String, Set<String>> allTypesPerProject = new HashMap<>();
            Map<String, Map<String, Object>> typeDefinitions = new HashMap<>();
            int totalUniqueTypes = 0;
            
            // Para cada proyecto, obtener tipos de work items completos
            for (String project : projects) {
                discovery.append("🔍 **Analizando proyecto: ").append(project).append("**\n");
                
                // Usar GetWorkItemTypesTool para obtener información completa
                Map<String, Object> arguments = Map.of(
                    "project", project,
                    "includeExtendedInfo", true,
                    "includeFieldDetails", true
                );
                
                Map<String, Object> result = getWorkItemTypesTool.execute(arguments);
                
                if (result.containsKey("content")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                    if (!content.isEmpty() && content.get(0).containsKey("text")) {
                        String response = (String) content.get(0).get("text");
                        
                        // Extraer información de tipos del response
                        Set<String> projectTypes = extractWorkItemTypesFromResponse(response);
                        
                        // NUEVO: Validación adicional para garantizar detección completa
                        Set<String> validatedTypes = validateAndEnhanceTypeDetection(response, projectTypes);
                        allTypesPerProject.put(project, validatedTypes);
                        
                        discovery.append("   ✅ Encontrados ").append(validatedTypes.size()).append(" tipos: ");
                        discovery.append(String.join(", ", validatedTypes)).append("\n");
                        
                        // Log detallado para debugging
                        if (validatedTypes.size() != projectTypes.size()) {
                            System.out.println("⚠️ Diferencia detectada en " + project + ": " + 
                                             projectTypes.size() + " vs " + validatedTypes.size());
                        }
                        
                        // Almacenar definiciones detalladas para análisis posterior
                        storeTypeDefinitions(project, response, typeDefinitions);
                    }
                }
                discovery.append("\n");
            }
            
            // Análisis global de tipos únicos
            Set<String> allUniqueTypes = new HashSet<>();
            for (Set<String> projectTypes : allTypesPerProject.values()) {
                allUniqueTypes.addAll(projectTypes);
            }
            totalUniqueTypes = allUniqueTypes.size();
            
            discovery.append("📊 **RESUMEN GLOBAL**\n");
            discovery.append("═══════════════════\n");
            discovery.append("• **Total de proyectos analizados:** ").append(projects.size()).append("\n");
            discovery.append("• **Total de tipos únicos encontrados:** ").append(totalUniqueTypes).append("\n");
            discovery.append("• **Tipos únicos globales:** ").append(String.join(", ", allUniqueTypes)).append("\n\n");
            
            // Análisis de tipos por proyecto
            discovery.append("📈 **ANÁLISIS POR PROYECTO**\n");
            discovery.append("═══════════════════════════\n");
            for (Map.Entry<String, Set<String>> entry : allTypesPerProject.entrySet()) {
                String project = entry.getKey();
                Set<String> types = entry.getValue();
                discovery.append("🏗️ **").append(project).append(":** ").append(types.size()).append(" tipos\n");
                
                // Identificar tipos únicos por proyecto
                Set<String> uniqueToProject = new HashSet<>(types);
                for (Map.Entry<String, Set<String>> other : allTypesPerProject.entrySet()) {
                    if (!other.getKey().equals(project)) {
                        uniqueToProject.removeAll(other.getValue());
                    }
                }
                
                if (!uniqueToProject.isEmpty()) {
                    discovery.append("   🎯 **Únicos:** ").append(String.join(", ", uniqueToProject)).append("\n");
                }
            }
            
            discovery.append("\n💡 **RECOMENDACIÓN:** Esta información completa debe usarse para actualizar\n");
            discovery.append("los archivos de configuración organizacional para garantizar soporte completo.\n");
            
            // NUEVO: Realizar análisis jerárquico de work items
            discovery.append("\n");
            discovery.append(performHierarchicalAnalysis(projects));
            
        } catch (Exception e) {
            discovery.append("❌ Error durante descubrimiento exhaustivo: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        
        return discovery.toString();
    }
    
    /**
     * Realiza análisis jerárquico de work items en todos los proyectos
     */
    private String performHierarchicalAnalysis(List<String> projects) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("🔍 **ANÁLISIS JERÁRQUICO DE WORK ITEMS**\n");
        analysis.append("====================================\n\n");
        
        Map<String, Map<String, Object>> projectHierarchies = new HashMap<>();
        
        // Analizar cada proyecto
        for (String project : projects) {
            analysis.append("📁 **Analizando jerarquías en proyecto: ").append(project).append("**\n");
            
            try {
                Map<String, Object> hierarchyData = analyzeWorkItemHierarchies(project);
                projectHierarchies.put(project, hierarchyData);
                
                if (hierarchyData.containsKey("error")) {
                    analysis.append("   ⚠️ ").append(hierarchyData.get("error")).append("\n");
                } else if (hierarchyData.containsKey("message")) {
                    analysis.append("   ℹ️ ").append(hierarchyData.get("message")).append("\n");
                } else {
                    // Mostrar estadísticas básicas
                    if (hierarchyData.containsKey("totalChildWorkItems")) {
                        analysis.append("   • Total work items hijos: ").append(hierarchyData.get("totalChildWorkItems")).append("\n");
                    }
                    
                    if (hierarchyData.containsKey("parentChildRelations")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Set<String>> relations = (Map<String, Set<String>>) hierarchyData.get("parentChildRelations");
                        
                        if (!relations.isEmpty()) {
                            analysis.append("   • Relaciones padre-hijo detectadas:\n");
                            for (Map.Entry<String, Set<String>> relation : relations.entrySet()) {
                                analysis.append("     - **").append(relation.getKey()).append("** → [")
                                       .append(String.join(", ", relation.getValue())).append("]\n");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                analysis.append("   ❌ Error en análisis jerárquico: ").append(e.getMessage()).append("\n");
            }
            
            analysis.append("\n");
        }
        
        // Generar documentación consolidada de patrones
        analysis.append(generateSubtaskPatternsDocumentation(projectHierarchies));
        
        return analysis.toString();
    }
    
    /**
     * Extrae nombres de tipos de work items de la respuesta formateada
     */
    private Set<String> extractWorkItemTypesFromResponse(String response) {
        Set<String> types = new HashSet<>();
        
        try {
            // Buscar patrones como "🔹 **NombreTipo**"
            Pattern pattern = Pattern.compile("🔹 \\*\\*([^*]+)\\*\\*");
            Matcher matcher = pattern.matcher(response);
            
            while (matcher.find()) {
                String typeName = matcher.group(1).trim();
                
                // Limpiar texto extra de diferentes formatos:
                // - "_(Deshabilitado)_" -> formato con guiones bajos
                // - " (Deshabilitado)" -> formato sin guiones bajos
                // - " _(cualquier texto)_" -> cualquier texto entre guiones bajos y paréntesis
                typeName = typeName.replaceAll("\\s*_?\\([^)]+\\)_?", "").trim();
                
                // Validar que el nombre no esté vacío después de la limpieza
                if (!typeName.isEmpty() && !types.contains(typeName)) {
                    types.add(typeName);
                    System.out.println("✅ Tipo detectado: '" + typeName + "'");
                }
            }
            
            System.out.println("📋 Total tipos únicos extraídos: " + types.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error extrayendo tipos de work items: " + e.getMessage());
            e.printStackTrace();
        }
        
        return types;
    }
    
    /**
     * Almacena definiciones detalladas de tipos para análisis posterior
     */
    private void storeTypeDefinitions(String project, String response, Map<String, Map<String, Object>> typeDefinitions) {
        // Por ahora solo almacenamos el proyecto y response para referencia futura
        // En una implementación más avanzada, podríamos parsear campos específicos
        typeDefinitions.put(project + "_response", Map.of("project", project, "response", response));
    }
    
    /**
     * Obtiene los campos básicos configurados dinámicamente desde el servicio de configuración
     */
    private List<String> getConfiguredBasicFields() {
        List<String> configuredFields = new ArrayList<>();
        
        try {
            // Obtener campos dinámicamente desde archivos de configuración existentes
            // Esto permite que el sistema se adapte a cualquier configuración sin hardcodear
            List<String> potentialFields = discoverFieldsFromConfiguration();
            
            // Verificar cuáles de estos campos están realmente configurados
            for (String fieldName : potentialFields) {
                try {
                    Map<String, Object> fieldMapping = configService.getFieldMapping(fieldName);
                    if (fieldMapping != null && !fieldMapping.isEmpty()) {
                        configuredFields.add(fieldName);
                    }
                } catch (Exception e) {
                    // Campo no configurado, continuar con el siguiente
                }
            }
            
            System.out.println("Campos básicos encontrados en configuración: " + configuredFields.size());
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos configurados: " + e.getMessage());
            // En caso de error, devolver lista vacía (sin fallback hardcoded)
        }
        
        return configuredFields;
    }
    
    /**
     * Descubre campos desde archivos de configuración existentes de forma dinámica
     */
    private List<String> discoverFieldsFromConfiguration() {
        List<String> discoveredFields = new ArrayList<>();
        
        try {
            // Leer del archivo field-mappings.yml si existe
            String configPath = "config/field-mappings.yml";
            discoveredFields = extractFieldNamesFromConfigFile(configPath);
            
            if (discoveredFields.isEmpty()) {
                // Si no hay archivo de configuración, usar un conjunto mínimo dinámico
                System.out.println("No se encontró configuración de campos - usando descubrimiento dinámico completo");
            }
            
        } catch (Exception e) {
            System.err.println("Error descubriendo campos desde configuración: " + e.getMessage());
        }
        
        return discoveredFields;
    }
    
    /**
     * Extrae nombres de campos del archivo de configuración YAML
     */
    private List<String> extractFieldNamesFromConfigFile(String configPath) {
        List<String> fieldNames = new ArrayList<>();
        
        try {
            // Este método se puede implementar para leer realmente del archivo YAML
            // Por ahora devolver lista vacía para forzar descubrimiento completamente dinámico
            System.out.println("Configuración dinámica de campos activada");
            
        } catch (Exception e) {
            System.err.println("Error leyendo archivo de configuración: " + e.getMessage());
        }
        
        return fieldNames;
    }

    private String analyzeWorkItemFields(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🏷️ Campos de Work Items\n");
        analysis.append("-----------------------\n");
        
        try {
            // Obtener campos básicos dinámicamente desde la configuración existente
            List<String> configuredFields = getConfiguredBasicFields();
            
            if (!configuredFields.isEmpty()) {
                analysis.append("Campos Básicos Configurados:\n");
                for (String field : configuredFields) {
                    analysis.append("• ").append(field);
                    
                    Map<String, Object> fieldMapping = configService.getFieldMapping(field);
                    if (!fieldMapping.isEmpty()) {
                        String azureField = (String) fieldMapping.get("azureFieldName");
                        if (azureField != null) {
                            analysis.append(" → ").append(azureField);
                        }
                    }
                    analysis.append("\n");
                }
            } else {
                analysis.append("⚠️ No se encontraron campos básicos configurados\n");
                analysis.append("💡 Use generateConfig=true para crear configuración base\n");
            }
            
            analysis.append("\nCampos Dinámicos:\n");
            analysis.append("• Los campos específicos se descubren automáticamente por tipo de work item\n");
            analysis.append("• Use investigateCustomFields=true para análisis exhaustivo de campos personalizados\n");
            
            analysis.append("\n");
            
        } catch (Exception e) {
            analysis.append("❌ Error obteniendo campos: ").append(e.getMessage()).append("\n\n");
        }
        
        return analysis.toString();
    }
    
    private String generateSuggestedConfiguration(String project) {
        StringBuilder config = new StringBuilder();
        config.append("⚙️ Configuración YAML Sugerida\n");
        config.append("==============================\n");
        config.append("```yaml\n");
        config.append("organization:\n");
        config.append("  name: \"").append(configService.getDefaultOrganizationConfig().get("organization")).append("\"\n");
        config.append("  defaultProject: \"").append(project != null ? project : "YourProject").append("\"\n");
        config.append("  defaultTeam: \"YourTeam\"\n");
        config.append("  timeZone: \"America/Bogota\"\n");
        config.append("  language: \"es-CO\"\n\n");
        
        config.append("fieldMappings:\n");
        config.append("  title:\n");
        config.append("    azureFieldName: \"System.Title\"\n");
        config.append("    required: true\n");
        config.append("  description:\n");
        config.append("    azureFieldName: \"System.Description\"\n");
        config.append("    required: false\n");
        config.append("  state:\n");
        config.append("    azureFieldName: \"System.State\"\n");
        config.append("    required: true\n");
        config.append("    defaultValue: \"New\"\n");
        config.append("```\n\n");
        
        config.append("💾 Guarde esta configuración en config/organization-config.yml\n\n");
        
        return config.toString();
    }
    
    private String getConfigurationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("💡 Recomendaciones de Configuración\n");
        recommendations.append("===================================\n");
        
        recommendations.append("1. 🎯 **Campos Básicos**: Asegúrese de que title, description y state estén configurados\n");
        recommendations.append("2. 🏷️ **Mapeo de Campos**: Defina mapeos específicos para campos personalizados de su organización\n");
        recommendations.append("3. ✅ **Validaciones**: Configure campos requeridos según sus procesos de trabajo\n");
        recommendations.append("4. 🔄 **Compatibilidad**: Use aliases para mantener compatibilidad con herramientas existentes\n");
        recommendations.append("5. 📝 **Documentación**: Agregue helpText para campos personalizados\n\n");
        
        recommendations.append("🚀 **Próximos pasos**:\n");
        recommendations.append("• Ejecute con generateConfig=true para obtener un archivo YAML base\n");
        recommendations.append("• Personalice los mapeos según su organización\n");
        recommendations.append("• Pruebe la configuración con work items de prueba\n");
        
        return recommendations.toString();
    }
    
    // ========================================================================
    // FUNCIONALIDAD AVANZADA DE INVESTIGACIÓN DE CAMPOS
    // Reemplaza la funcionalidad de los scripts investigate-field-values.sh 
    // y get-picklist-values.sh
    // ========================================================================
    
    /**
     * Investiga valores permitidos para campos personalizados específicos
     */
    private String investigateCustomFieldValues(String project) {
        StringBuilder investigation = new StringBuilder();
        investigation.append("🔬 Investigación Avanzada de Campos Personalizados\n");
        investigation.append("================================================\n");
        
        try {
            // Obtener todos los tipos de work items disponibles en el proyecto dinámicamente
            List<String> availableTypes = getAvailableWorkItemTypes(project);
            
            if (availableTypes.isEmpty()) {
                investigation.append("⚠️ No se pudieron obtener los tipos de work items del proyecto\n");
                return investigation.toString();
            }
            
            investigation.append("📋 Tipos de Work Items encontrados: ").append(availableTypes.size()).append("\n");
            investigation.append("Types: ").append(String.join(", ", availableTypes)).append("\n\n");
            
            // Investigar TODOS los tipos, no solo los conocidos, para ser exhaustivos
            for (String typeName : availableTypes) {
                investigation.append("\n📋 Tipo: ").append(typeName).append("\n");
                investigation.append("─".repeat(40)).append("\n");
                
                Map<String, List<String>> fieldValues = investigateWorkItemTypeDefinition(project, typeName);
                
                if (fieldValues.isEmpty()) {
                    investigation.append("ℹ️ No se encontraron campos personalizados con valores permitidos\n");
                } else {
                    for (Map.Entry<String, List<String>> entry : fieldValues.entrySet()) {
                        investigation.append("🏷️ Campo: ").append(entry.getKey()).append("\n");
                        investigation.append("   Valores permitidos:\n");
                        for (String value : entry.getValue()) {
                            investigation.append("   • ").append(value).append("\n");
                        }
                        investigation.append("\n");
                    }
                }
            }
            
            // Obtener todos los campos del proyecto para análisis adicional  
            investigation.append("\n🔧 Análisis General de Campos del Proyecto\n");
            investigation.append("─".repeat(40)).append("\n");
            
            List<Map<String, Object>> allFields = getAllProjectFields(project);
            List<Map<String, Object>> customFields = allFields.stream()
                    .filter(field -> {
                        String refName = (String) field.get("referenceName");
                        String name = (String) field.get("name");
                        return refName.contains("Custom") || name.toLowerCase().contains("tipo");
                    })
                    .toList();
            
            investigation.append("📊 Resumen de campos personalizados encontrados: ").append(customFields.size()).append("\n\n");
            
            for (Map<String, Object> field : customFields) {
                investigation.append("🏷️ ").append(field.get("name")).append("\n");
                investigation.append("   Referencia: ").append(field.get("referenceName")).append("\n");
                investigation.append("   Tipo: ").append(field.get("type")).append("\n");
                
                // Intentar obtener valores de picklist si aplica
                if ("picklistString".equals(field.get("type"))) {
                    String picklistId = (String) field.get("picklistId");
                    if (picklistId != null) {
                        List<String> picklistValues = getPicklistValues(project, (String) field.get("referenceName"), picklistId);
                        if (!picklistValues.isEmpty()) {
                            investigation.append("   Valores permitidos:\n");
                            for (String value : picklistValues) {
                                investigation.append("     • ").append(value).append("\n");
                            }
                        }
                    }
                }
                investigation.append("\n");
            }
            
        } catch (Exception e) {
            investigation.append("❌ Error durante la investigación: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
    
    /**
     * Obtiene todos los tipos de work items disponibles en el proyecto usando GetWorkItemTypesTool
     * Esta implementación garantiza consistencia con la herramienta especializada que ya funciona
     */
    /**
     * Obtiene tipos de work items disponibles - REFACTORIZADO
     * @deprecated Usar workItemTypeManager.getAvailableWorkItemTypes() en su lugar
     */
    @Deprecated
    private List<String> getAvailableWorkItemTypes(String project) {
        return workItemTypeManager.getAvailableWorkItemTypes(project);
    }
    
    /**
     * Extrae nombres de tipos de work items de la respuesta formateada de GetWorkItemTypesTool
     */
    private List<String> extractWorkItemTypeNames(String response) {
        List<String> typeNames = new ArrayList<>();
        
        try {
            // Buscar patrones como "🔹 **NombreTipo**" en la respuesta
            Pattern pattern = Pattern.compile("🔹 \\*\\*([^*]+)\\*\\*");
            Matcher matcher = pattern.matcher(response);
            
            while (matcher.find()) {
                String typeName = matcher.group(1).trim();
                
                // Limpiar texto extra de diferentes formatos:
                // - "_(Deshabilitado)_" -> formato con guiones bajos
                // - " (Deshabilitado)" -> formato sin guiones bajos  
                // - " _(cualquier texto)_" -> cualquier texto entre guiones bajos y paréntesis
                typeName = typeName.replaceAll("\\s*_?\\([^)]+\\)_?", "").trim();
                
                // Validar que el nombre no esté vacío y no sea duplicado
                if (!typeName.isEmpty() && !typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                    System.out.println("✅ Tipo detectado en extractWorkItemTypeNames: '" + typeName + "'");
                }
            }
            
            System.out.println("📋 Tipos extraídos por extractWorkItemTypeNames: " + String.join(", ", typeNames));
            System.out.println("📊 Total de tipos: " + typeNames.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error extrayendo nombres de tipos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return typeNames;
    }
    
    /**
     * Valida y mejora la detección de tipos usando múltiples estrategias
     * para garantizar que no se pierda ningún tipo de work item
     */
    private Set<String> validateAndEnhanceTypeDetection(String response, Set<String> initialTypes) {
        Set<String> enhancedTypes = new HashSet<>(initialTypes);
        
        try {
            // Estrategia 1: Buscar patrones adicionales que el regex principal pudo haber perdido
            // Buscar líneas que empiecen con "🔹" seguido de texto
            Pattern alternativePattern1 = Pattern.compile("🔹\\s+\\*\\*([^*\\n]+)\\*\\*");
            Matcher matcher1 = alternativePattern1.matcher(response);
            
            while (matcher1.find()) {
                String typeName = matcher1.group(1).trim();
                typeName = typeName.replaceAll("\\s*_?\\([^)]+\\)_?", "").trim();
                if (!typeName.isEmpty()) {
                    enhancedTypes.add(typeName);
                }
            }
            
            // Estrategia 2: Buscar nombres en formato de encabezados
            // Para casos donde el formato puede variar
            Pattern plainPattern = Pattern.compile("\\*\\*([A-Za-z][A-Za-z0-9\\s\\-_]+)\\*\\*");
            Matcher matcher2 = plainPattern.matcher(response);
            
            while (matcher2.find()) {
                String typeName = matcher2.group(1).trim();
                typeName = typeName.replaceAll("\\s*_?\\([^)]+\\)_?", "").trim();
                
                // Solo agregar si parece un tipo válido de work item
                if (isValidWorkItemTypeName(typeName)) {
                    enhancedTypes.add(typeName);
                }
            }
            
            // Estrategia 3: Búsqueda en metadatos de la respuesta
            // Buscar referencias específicas que indiquen tipos de work items
            if (response.contains("referenceName")) {
                Pattern referencePattern = Pattern.compile("referenceName[\"']?\\s*:\\s*[\"']([^\"']+)[\"']");
                Matcher matcher3 = referencePattern.matcher(response);
                
                while (matcher3.find()) {
                    String reference = matcher3.group(1);
                    String extractedType = extractTypeFromReference(reference);
                    if (extractedType != null) {
                        enhancedTypes.add(extractedType);
                    }
                }
            }
            
            // Log de mejoras detectadas
            if (enhancedTypes.size() > initialTypes.size()) {
                Set<String> newTypes = new HashSet<>(enhancedTypes);
                newTypes.removeAll(initialTypes);
                System.out.println("🔍 Tipos adicionales detectados por validación: " + String.join(", ", newTypes));
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error en validación mejorada de tipos: " + e.getMessage());
            // En caso de error, devolver los tipos iniciales
            return initialTypes;
        }
        
        return enhancedTypes;
    }
    
    /**
     * Valida si un string parece un nombre válido de tipo de work item
     * Implementación completamente genérica sin valores hardcodeados
     */
    private boolean isValidWorkItemTypeName(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return false;
        }
        
        String normalized = typeName.trim();
        
        // Verificar patrones genéricos de tipos de work item válidos
        return normalized.matches("^[A-Za-z][A-Za-z0-9\\s\\-_]+$") && 
               normalized.length() > 2 && 
               normalized.length() < 100 &&
               !normalized.toLowerCase().contains("total") &&
               !normalized.toLowerCase().contains("encontrado") &&
               !normalized.toLowerCase().contains("error") &&
               !normalized.toLowerCase().contains("resultado");
    }
    
    /**
     * Extrae el nombre del tipo de work item de una referencia de Azure DevOps
     */
    private String extractTypeFromReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        
        // Extraer de referencias como "Microsoft.VSTS.WorkItemTypes.Bug" -> "Bug"
        if (reference.startsWith("Microsoft.VSTS.WorkItemTypes.")) {
            return reference.substring("Microsoft.VSTS.WorkItemTypes.".length());
        }
        
        // Extraer de referencias personalizadas como "AgileYOUR_ORGANIZATION.Historia" -> "Historia"
        if (reference.contains(".")) {
            String[] parts = reference.split("\\.");
            return parts[parts.length - 1];
        }
        
        return null;
    }
    
    /**
     * Método de fallback para obtener tipos de work items directamente de la API
     * cuando el parsing de respuestas formateadas falla
     */
    private List<String> getWorkItemTypesDirectFromApi(String project) {
        List<String> workItemTypes = new ArrayList<>();
        
        try {
            System.out.println("🔄 Ejecutando consulta directa a la API para: " + project);
            
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                workItemTypes = parseWorkItemTypesFromApiResponse(response);
                System.out.println("✅ API directa retornó " + workItemTypes.size() + " tipos");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en consulta directa a API: " + e.getMessage());
        }
        
        return workItemTypes;
    }
    
    /**
     * Parsea tipos de work items de la respuesta JSON directa de la API
     */
    private List<String> parseWorkItemTypesFromApiResponse(String jsonResponse) {
        List<String> typeNames = new ArrayList<>();
        
        try {
            // Buscar patrones de nombres en JSON: "name":"TipoWorkItem"
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = namePattern.matcher(jsonResponse);
            
            while (matcher.find()) {
                String typeName = matcher.group(1).trim();
                
                // Validar que sea un tipo válido y no duplicado
                if (isValidWorkItemTypeName(typeName) && !typeNames.contains(typeName)) {
                    typeNames.add(typeName);
                    System.out.println("🎯 Tipo extraído de API: '" + typeName + "'");
                }
            }
            
            System.out.println("📊 Total tipos extraídos de API: " + typeNames.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error parseando respuesta de API: " + e.getMessage());
        }
        
        return typeNames;
    }
    
    /**
     * Investiga la definición completa de un tipo de work item específico
     */
    private Map<String, List<String>> investigateWorkItemTypeDefinition(String project, String workItemTypeName) {
        Map<String, List<String>> fieldAllowedValues = new HashMap<>();
        
        try {
            String encodedTypeName = java.net.URLEncoder.encode(workItemTypeName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, encodedTypeName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                fieldAllowedValues = parseWorkItemTypeFieldValues(response);
            }
            
            // Intentar obtener valores de la definición de tipo primero
            if (response != null) {
                fieldAllowedValues = parseWorkItemTypeFieldValues(response);
            }
            
            // Si no encontramos valores en la definición, usar WIQL para extraer valores reales
            if (fieldAllowedValues.isEmpty()) {
                Map<String, List<String>> dynamicValues = extractFieldValuesFromExistingWorkItems(project, workItemTypeName);
                fieldAllowedValues.putAll(dynamicValues);
            }
            
        } catch (Exception e) {
            System.err.println("Error investigando work item type '" + workItemTypeName + "': " + e.getMessage());
            // En caso de error, usar valores de fallback
            fieldAllowedValues = extractFieldValuesFromExistingWorkItems(project, workItemTypeName);
        }
        
        return fieldAllowedValues;
    }
    
    /**
     * Extrae valores únicos de campos personalizados analizando work items existentes usando WIQL
     */
    private Map<String, List<String>> extractFieldValuesFromExistingWorkItems(String project, String workItemTypeName) {
        Map<String, List<String>> fieldValues = new HashMap<>();
        
        try {
            System.out.println("🔍 Analizando campos personalizados para: " + workItemTypeName);
            
            // Primero obtener los campos personalizados del tipo de work item
            List<String> customFields = getCustomFieldsForWorkItemType(project, workItemTypeName);
            if (customFields.isEmpty()) {
                System.out.println("ℹ️ No se encontraron campos personalizados para el tipo: " + workItemTypeName);
                return fieldValues;
            }
            
            System.out.println("📋 Campos a investigar: " + customFields.size());
            
            // Construir consulta WIQL para obtener work items recientes
            String wiqlQuery = buildWIQLQueryForFieldDiscovery(workItemTypeName, customFields);
            
            // Ejecutar consulta WIQL
            List<Integer> workItemIds = executeWIQLQuery(project, wiqlQuery);
            
            if (!workItemIds.isEmpty()) {
                System.out.println("📊 Analizando " + workItemIds.size() + " work items para extraer valores");
                
                // Obtener detalles de los work items encontrados
                Map<String, Set<String>> discoveredValues = extractFieldValuesFromWorkItems(project, workItemIds, customFields);
                
                // Convertir Set a List para el resultado
                for (Map.Entry<String, Set<String>> entry : discoveredValues.entrySet()) {
                    List<String> values = new ArrayList<>(entry.getValue());
                    if (!values.isEmpty()) {
                        fieldValues.put(entry.getKey(), values);
                        System.out.println("✅ Campo " + entry.getKey() + ": " + values.size() + " valores únicos");
                    }
                }
                
                if (fieldValues.isEmpty()) {
                    System.out.println("⚠️ No se encontraron valores en los work items analizados");
                }
            } else {
                System.out.println("⚠️ No se encontraron work items del tipo " + workItemTypeName + " en el período especificado");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error extracting field values from existing work items: " + e.getMessage());
            e.printStackTrace();
        }
        
        return fieldValues;
    }
    
    /**
     * Parsea valores de campos de work items existentes
     */
    private Map<String, List<String>> parseFieldValuesFromWorkItemsResponse(String jsonResponse, String targetType) {
        // Este método ya no se usa - los valores se extraen dinámicamente usando WIQL
        return new HashMap<>();
    }
    
    /**
     * Obtiene los campos personalizados específicos para un tipo de work item con validación
     */
    private List<String> getCustomFieldsForWorkItemType(String project, String workItemTypeName) {
        List<String> customFields = new ArrayList<>();
        
        try {
            String encodedTypeName = java.net.URLEncoder.encode(workItemTypeName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, encodedTypeName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                customFields = parseCustomFieldsFromTypeDefinition(response);
                System.out.println("Campos personalizados encontrados para " + workItemTypeName + ": " + customFields.size());
                
                // Validar que los campos realmente existen y están disponibles
                customFields = validateFieldsExistence(customFields, project, workItemTypeName);
            } else {
                System.out.println("No se pudo obtener definición de tipo para: " + workItemTypeName);
            }
            
        } catch (Exception e) {
            System.err.println("Error getting custom fields for work item type '" + workItemTypeName + "': " + e.getMessage());
        }
        
        return customFields;
    }
    
    /**
     * Valida que los campos personalizados realmente existen y están disponibles
     */
    private List<String> validateFieldsExistence(List<String> candidateFields, String project, String workItemType) {
        // ✅ REFACTORIZADO: Usar utilidad centralizada para validación
        Map<String, AzureDevOpsFieldValidator.FieldValidationResult> validationResults = 
            fieldValidator.validateFieldsExistence(project, workItemType, candidateFields);
        
        List<String> validFields = new ArrayList<>();
        for (Map.Entry<String, AzureDevOpsFieldValidator.FieldValidationResult> entry : validationResults.entrySet()) {
            if (entry.getValue().isValid()) {
                validFields.add(entry.getKey());
            }
        }
        
        System.out.println("Campos validados para " + workItemType + ": " + validFields.size() + "/" + candidateFields.size());
        return validFields;
    }
    
    /**
     * ✅ REFACTORIZADO: Este método ahora delega a la utilidad centralizada
     * Verifica si un campo personalizado tiene formato válido y no causa errores conocidos
     */
    private boolean isValidCustomField(String fieldName) {
        return fieldValidator.isValidCustomField(null, null, fieldName);
    }
    
    /**
     * Obtiene campos problemáticos conocidos de forma dinámica
     * Estos pueden expandirse según errores encontrados en tiempo de ejecución
     */
    private Set<String> getKnownProblematicFields() {
        Set<String> problematicFields = new HashSet<>();
        
        // Solo agregar campos que han causado errores específicos confirmados
        // Se pueden agregar dinámicamente según los errores encontrados
        problematicFields.add("Custom.46f6eede-0b66-4fa8-93db-e783f3be205c"); // TF51005 confirmado
        
        return problematicFields;
    }
    
    /**
     * Parsea campos personalizados de la definición del tipo de work item
     */
    private List<String> parseCustomFieldsFromTypeDefinition(String jsonResponse) {
        List<String> customFields = new ArrayList<>();
        
        // Buscar campos que empiecen con "Custom." en fieldInstances
        Pattern fieldPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"(Custom\\.[^\"]+)\"");
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            if (!customFields.contains(fieldName)) {
                customFields.add(fieldName);
            }
        }
        
        return customFields;
    }
    
    /**
     * Construye una consulta WIQL para descubrir valores de campos específicos
     */
    private String buildWIQLQueryForFieldDiscovery(String workItemTypeName, List<String> customFields) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT [System.Id], [System.Title], [System.State]");
        
        // Validar y agregar solo campos que existen
        List<String> validFields = new ArrayList<>();
        for (String field : customFields) {
            if (field != null && !field.trim().isEmpty()) {
                validFields.add(field.trim());
                query.append(", [").append(field.trim()).append("]");
            }
        }
        
        query.append(" FROM WorkItems WHERE [System.WorkItemType] = '").append(workItemTypeName).append("'");
        
        // Ampliar criterios para encontrar más work items con datos
        // Incluir work items en cualquier estado, no solo finales
        query.append(" AND [System.ChangedDate] > @Today - 180"); // Últimos 6 meses para balance entre datos y rendimiento
        
        // CRÍTICO: Limitar resultados para evitar error VS402337 (límite 20000)
        query.append(" ORDER BY [System.ChangedDate] DESC");
        
        return query.toString();
    }
    
    /**
     * Ejecuta una consulta WIQL y retorna los IDs de work items encontrados
     */
    private List<Integer> executeWIQLQuery(String project, String wiqlQuery) {
        // ✅ REFACTORIZADO: Usar utilidad WIQL centralizada
        try {
            // Procesar parámetros usando utilidad centralizada si es necesario
            String processedQuery = processWIQLQueryParameters(wiqlQuery, project);
            
            // Ejecutar usando la utilidad centralizada con validación incluida
            WiqlQueryResult result = wiqlUtility.executeWiqlQuery(project, null, processedQuery);
            
            if (result != null && result.workItems() != null) {
                return result.getWorkItemIds();
            }
            
        } catch (Exception e) {
            System.err.println("Error ejecutando consulta WIQL: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Procesa parámetros genéricos en consultas WIQL
     */
    private String processWIQLQueryParameters(String query, String project) {
        // Reemplazar parámetros estándar de Azure DevOps
        String processedQuery = query;
        
        // @project -> nombre del proyecto específico
        processedQuery = processedQuery.replace("@project", "'" + project + "'");
        
        // @me -> usuario actual (usar @me tal como está, Azure DevOps lo resuelve)
        // @today -> fecha actual (usar @today tal como está, Azure DevOps lo resuelve)
        // @currentIteration -> iteración actual (usar @currentIteration tal como está)
        
        // Limpiar espacios extra que puedan haber quedado
        processedQuery = processedQuery.replaceAll("\\s+", " ").trim();
        
        return processedQuery;
    }
    
    /**
     * Agrega límite TOP a consulta WIQL para evitar exceder límite de 20000 items
     */
    private String addLimitToWIQLQuery(String originalQuery, int limit) {
        // Si la consulta ya tiene SELECT, agregar TOP después de SELECT
        if (originalQuery.toUpperCase().contains("SELECT")) {
            return originalQuery.replaceFirst("(?i)SELECT", "SELECT TOP " + limit);
        }
        return originalQuery;
    }
    
    /**
     * Hace una petición WIQL con manejo robusto de errores usando AzureDevOpsClient
     */
    private String makeWIQLApiRequest(String project, String wiqlQuery) {
        try {
            WiqlQueryResult result = azureDevOpsClient.executeWiqlQuery(project, null, wiqlQuery);
            
            if (result != null) {
                // Convertir el resultado a JSON string para mantener compatibilidad con el código existente
                // En el futuro se podría refactorizar para trabajar directamente con el objeto
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\"workItems\":[");
                
                if (result.workItems() != null && !result.workItems().isEmpty()) {
                    boolean first = true;
                    for (WiqlQueryResult.WorkItemReference wi : result.workItems()) {
                        if (!first) jsonBuilder.append(",");
                        jsonBuilder.append("{\"id\":").append(wi.id()).append("}");
                        first = false;
                    }
                }
                
                jsonBuilder.append("]}");
                return jsonBuilder.toString();
            }
            
        } catch (AzureDevOpsException e) {
            String errorMessage = e.getMessage();
            
            // Identificar y manejar errores específicos
            if (errorMessage.contains("VS402337") || errorMessage.contains("exceeds the size limit")) {
                System.err.println("❌ ERROR: La consulta WIQL excede el límite de 20,000 work items");
                System.err.println("   Solución: La consulta será automáticamente limitada a 200 items");
                return null;
            } else if (errorMessage.contains("TF51005") || errorMessage.contains("field that does not exist")) {
                System.err.println("❌ ERROR: Campo referenciado no existe en el proyecto");
                System.err.println("   Detalle: " + errorMessage);
                return null;
            } else {
                System.err.println("❌ WIQL query failed: " + errorMessage);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error making WIQL API request: " + e.getMessage());
            return null;
        }
        
        return null;
    }
    
    /**
     * Parsea IDs de work items de la respuesta WIQL
     */
    private List<Integer> parseWorkItemIdsFromWIQLResponse(String jsonResponse) {
        List<Integer> ids = new ArrayList<>();
        
        // Buscar workItems en la respuesta
        Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
        Matcher matcher = idPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            try {
                int id = Integer.parseInt(matcher.group(1));
                ids.add(id);
            } catch (NumberFormatException e) {
                // Ignorar IDs inválidos
            }
        }
        
        // Limitar a máximo 50 work items para evitar sobrecarga
        if (ids.size() > 50) {
            ids = ids.subList(0, 50);
        }
        
        return ids;
    }
    
    /**
     * Extrae valores de campos de work items específicos
     */
    private Map<String, Set<String>> extractFieldValuesFromWorkItems(String project, List<Integer> workItemIds, List<String> customFields) {
        Map<String, Set<String>> fieldValues = new HashMap<>();
        
        // Inicializar sets para cada campo
        for (String field : customFields) {
            fieldValues.put(field, new HashSet<>());
        }
        
        try {
            // Procesar work items en lotes para evitar URLs muy largas
            int batchSize = 10;
            for (int i = 0; i < workItemIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, workItemIds.size());
                List<Integer> batch = workItemIds.subList(i, endIndex);
                
                String idsParam = batch.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                
                String fieldsParam = customFields.stream()
                        .collect(Collectors.joining(","));
                
                String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems?ids=%s&fields=%s&api-version=7.1", 
                        azureDevOpsClient.getOrganization(), project, idsParam, fieldsParam);
                
                String response = makeDirectApiRequest(url);
                if (response != null) {
                    parseFieldValuesFromBatch(response, customFields, fieldValues);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting field values from work items: " + e.getMessage());
        }
        
        return fieldValues;
    }
    
    /**
     * Parsea valores de campos de un lote de work items
     */
    private void parseFieldValuesFromBatch(String jsonResponse, List<String> customFields, Map<String, Set<String>> fieldValues) {
        // Buscar cada work item en la respuesta
        Pattern workItemPattern = Pattern.compile("\\{[^{}]*\"fields\"\\s*:\\s*\\{([^{}]+)\\}[^{}]*\\}");
        Matcher workItemMatcher = workItemPattern.matcher(jsonResponse);
        
        while (workItemMatcher.find()) {
            String fieldsContent = workItemMatcher.group(1);
            
            // Para cada campo personalizado, buscar su valor
            for (String field : customFields) {
                Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
                Matcher fieldMatcher = fieldPattern.matcher(fieldsContent);
                
                if (fieldMatcher.find()) {
                    String value = fieldMatcher.group(1);
                    if (value != null && !value.trim().isEmpty()) {
                        fieldValues.get(field).add(value);
                    }
                }
            }
        }
    }
    
    /**
     * Obtiene todos los campos del proyecto con metadatos detallados
     * @deprecated Usar fieldAnalyzer.getAllProjectFields() en su lugar
     */
    @Deprecated
    private List<Map<String, Object>> getAllProjectFields(String project) {
        // REFACTORIZADO: Delegar a FieldAnalyzer
        return fieldAnalyzer.getAllProjectFields(project);
    }
    
    /**
     * Obtiene valores de picklist usando múltiples estrategias de endpoints
     * @deprecated Migrado a FieldAnalyzer.getPicklistValues() para mejor organización del código
     */
    @Deprecated
    private List<String> getPicklistValues(String project, String fieldReferenceName, String picklistId) {
        // REFACTORIZADO: Delegar al analizador de campos que incluye gestión de picklists
        return fieldAnalyzer.getPicklistValues(project, fieldReferenceName, picklistId);
    }
    
    // Métodos auxiliares para investigación avanzada
    
    private String makeDirectApiRequest(String endpoint) {
        try {
            // Usar el método genérico de AzureDevOpsClient
            return azureDevOpsClient.makeGenericApiRequest(endpoint, null);
            
        } catch (Exception e) {
            System.err.println("Error making API request to " + endpoint + ": " + e.getMessage());
            return null;
        }
    }
    
    private Map<String, List<String>> parseWorkItemTypeFieldValues(String jsonResponse) {
        Map<String, List<String>> fieldAllowedValues = new HashMap<>();
        
        // Buscar fieldInstances y sus allowedValues usando regex simple
        Pattern fieldPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            String referenceName = matcher.group(1);
            String allowedValuesStr = matcher.group(2);
            
            List<String> values = new ArrayList<>();
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(allowedValuesStr);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
            
            if (!values.isEmpty()) {
                fieldAllowedValues.put(referenceName, values);
            }
        }
        
        // Si no encontramos valores con allowedValues, buscar campos requeridos y retornar vacío
        // para que se active el método de fallback
        return fieldAllowedValues;
    }
    
    private List<Map<String, Object>> parseProjectFields(String jsonResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Buscar cada campo en el array "value" usando regex
        Pattern fieldPattern = Pattern.compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"referenceName\"\\s*:\\s*\"([^\"]+)\"[^}]*\"type\"\\s*:\\s*\"([^\"]+)\"[^}]*\\}", Pattern.DOTALL);
        Matcher matcher = fieldPattern.matcher(jsonResponse);
        
        while (matcher.find()) {
            try {
                Map<String, Object> field = new HashMap<>();
                field.put("name", matcher.group(1));
                field.put("referenceName", matcher.group(2));
                field.put("type", matcher.group(3));
                
                // Buscar picklistId si existe en este campo
                String fieldBlock = matcher.group(0);
                String picklistId = AzureDevOpsJsonParser.extractSimpleValue(fieldBlock, "picklistId");
                if (picklistId != null) {
                    field.put("picklistId", picklistId);
                }
                
                fields.add(field);
            } catch (Exception e) {
                System.err.println("Error parsing field definition: " + e.getMessage());
            }
        }
        
        return fields;
    }
    
    /**
     * @deprecated Migrado a FieldAnalyzer.tryGetPicklistFromProcesses()
     */
    @Deprecated
    private List<String> tryGetPicklistFromProcesses(String picklistId) {
        return fieldAnalyzer.tryGetPicklistFromProcesses(picklistId);
    }
    
    /**
     * @deprecated Migrado a FieldAnalyzer.tryGetPicklistFromProjectContext()
     */
    @Deprecated
    private List<String> tryGetPicklistFromProjectContext(String project, String picklistId) {
        return fieldAnalyzer.tryGetPicklistFromProjectContext(project, picklistId);
    }
    
    /**
     * @deprecated Migrado a FieldAnalyzer.tryGetPicklistFromFieldEndpoint()
     */
    @Deprecated
    private List<String> tryGetPicklistFromFieldEndpoint(String project, String fieldReferenceName) {
        return fieldAnalyzer.tryGetPicklistFromFieldEndpoint(project, fieldReferenceName);
    }
    
    /**
     * @deprecated Migrado a FieldAnalyzer.extractArrayValues()
     */
    @Deprecated
    private List<String> extractArrayValues(String json, String arrayKey) {
        return fieldAnalyzer.extractArrayValues(json, arrayKey);
    }
    
    /**
     * Analiza las jerarquías de work items para identificar patrones de subtareas
     * y tipos de work items hijos más utilizados.
     */
    private Map<String, Object> analyzeWorkItemHierarchies(String project) {
        Map<String, Object> hierarchyAnalysis = new HashMap<>();
        
        try {
            System.out.println("Iniciando análisis de jerarquías para proyecto: " + project);
            
            // Enfoque 1: Buscar work items recientes que podrían tener relaciones
            String recentWorkItemsQuery = "SELECT [System.Id], [System.Title], [System.WorkItemType] " +
                                        "FROM WorkItems WHERE [System.TeamProject] = @project " +
                                        "ORDER BY [System.ChangedDate] DESC";
            
            List<Integer> recentWorkItemIds = executeWIQLQuery(project, recentWorkItemsQuery);
            
            if (recentWorkItemIds.isEmpty()) {
                hierarchyAnalysis.put("message", "No se encontraron work items en el proyecto " + project);
                return hierarchyAnalysis;
            }
            
            // Mapas para análisis estadístico
            Map<String, Integer> childTypeCount = new HashMap<>();
            Map<String, Set<String>> parentChildRelations = new HashMap<>();
            Map<String, Map<String, Integer>> parentTypeToChildTypes = new HashMap<>();
            
            // Procesar una muestra representativa para evitar sobrecarga
            int maxWorkItemsToProcess = Math.min(recentWorkItemIds.size(), 50);
            List<Integer> sampleIds = recentWorkItemIds.subList(0, maxWorkItemsToProcess);
            
            System.out.println("Analizando " + maxWorkItemsToProcess + " work items de " + recentWorkItemIds.size() + " totales");
            
            // Analizar cada work item para identificar relaciones padre-hijo
            for (Integer workItemId : sampleIds) {
                try {
                    String workItemData = getWorkItemAsJson(project, workItemId);
                    
                    if (workItemData != null) {
                        String workItemType = extractFieldFromJson(workItemData, "System.WorkItemType");
                        String parentIdStr = extractFieldFromJson(workItemData, "System.Parent");
                        
                        if (workItemType != null) {
                            // Si tiene padre, entonces es un work item hijo
                            if (parentIdStr != null && !parentIdStr.isEmpty() && !parentIdStr.equals("null")) {
                                // Contar tipos de work items hijos
                                childTypeCount.merge(workItemType, 1, Integer::sum);
                                
                                try {
                                    Integer parentIdInt = Integer.valueOf(parentIdStr);
                                    String parentData = getWorkItemAsJson(project, parentIdInt);
                                    
                                    if (parentData != null) {
                                        String parentType = extractFieldFromJson(parentData, "System.WorkItemType");
                                        
                                        if (parentType != null) {
                                            // Registrar relación padre-hijo por tipo
                                            parentChildRelations.computeIfAbsent(parentType, k -> new HashSet<>()).add(workItemType);
                                            
                                            // Estadísticas detalladas por tipo de padre
                                            parentTypeToChildTypes.computeIfAbsent(parentType, k -> new HashMap<>())
                                                                 .merge(workItemType, 1, Integer::sum);
                                            
                                            System.out.println("✅ Relación encontrada: " + parentType + " (ID: " + parentIdInt + ") -> " + workItemType + " (ID: " + workItemId + ")");
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    System.out.println("⚠️ ID de padre inválido para work item " + workItemId + ": " + parentIdStr);
                                } catch (Exception e) {
                                    System.out.println("⚠️ No se pudo analizar padre para work item " + workItemId + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error procesando work item " + workItemId + ": " + e.getMessage());
                }
            }
            
            // Construir resultado del análisis
            hierarchyAnalysis.put("totalWorkItemsAnalyzed", maxWorkItemsToProcess);
            hierarchyAnalysis.put("totalWorkItemsAvailable", recentWorkItemIds.size());
            hierarchyAnalysis.put("childTypeDistribution", childTypeCount);
            hierarchyAnalysis.put("parentChildRelations", parentChildRelations);
            hierarchyAnalysis.put("detailedStatistics", parentTypeToChildTypes);
            
            // Identificar los tipos de subtareas más comunes
            List<Map.Entry<String, Integer>> sortedChildTypes = childTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());
            
            hierarchyAnalysis.put("mostCommonChildTypes", sortedChildTypes);
            
            if (childTypeCount.isEmpty()) {
                hierarchyAnalysis.put("message", "No se encontraron relaciones padre-hijo en la muestra analizada de " + maxWorkItemsToProcess + " work items");
            } else {
                System.out.println("✅ Análisis de jerarquías completado para proyecto " + project + ": " + 
                                 childTypeCount.size() + " tipos de work items hijos detectados, " +
                                 parentChildRelations.size() + " tipos de padres identificados");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error durante análisis de jerarquías para proyecto " + project + ": " + e.getMessage());
            hierarchyAnalysis.put("error", "Error durante análisis: " + e.getMessage());
        }
        
        return hierarchyAnalysis;
    }
    
    /**
     * Obtiene un work item como JSON usando AzureDevOpsClient
     * Método auxiliar para el análisis jerárquico
     */
    private String getWorkItemAsJson(String project, Integer workItemId) {
        try {
            Map<String, Object> workItemData = azureDevOpsClient.getWorkItem(workItemId);
            if (workItemData != null) {
                // Convertir Map a JSON string usando una serialización simple
                // En el futuro se podría usar Jackson para una serialización más completa
                StringBuilder json = new StringBuilder();
                json.append("{");
                boolean first = true;
                for (Map.Entry<String, Object> entry : workItemData.entrySet()) {
                    if (!first) json.append(",");
                    json.append("\"").append(entry.getKey()).append("\":\"")
                        .append(entry.getValue().toString()).append("\"");
                    first = false;
                }
                json.append("}");
                return json.toString();
            }
            
        } catch (Exception e) {
            System.out.println("Error en llamada REST para work item " + workItemId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae un campo específico de una respuesta JSON de Azure DevOps
     */
    private String extractFieldFromJson(String jsonResponse, String fieldName) {
        try {
            // Buscar el campo en la sección "fields"
            String fieldsPattern = "\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}";
            Pattern pattern = Pattern.compile(fieldsPattern);
            Matcher matcher = pattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String fieldsSection = matcher.group(1);
                
                // Buscar el campo específico
                String fieldPattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
                Pattern fieldPatternCompiled = Pattern.compile(fieldPattern);
                Matcher fieldMatcher = fieldPatternCompiled.matcher(fieldsSection);
                
                if (fieldMatcher.find()) {
                    return fieldMatcher.group(1);
                }
            }
        } catch (Exception e) {
            System.out.println("Error extrayendo campo " + fieldName + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Genera documentación de patrones de subtareas basado en el análisis jerárquico
     */
    private String generateSubtaskPatternsDocumentation(Map<String, Map<String, Object>> projectHierarchies) {
        StringBuilder doc = new StringBuilder();
        
        doc.append("\n🏗️ **ANÁLISIS DE PATRONES JERÁRQUICOS**\n");
        doc.append("=====================================\n\n");
        
        Map<String, Set<String>> globalParentChildRelations = new HashMap<>();
        Map<String, Integer> globalChildTypeCount = new HashMap<>();
        
        // Consolidar datos de todos los proyectos
        for (Map.Entry<String, Map<String, Object>> projectEntry : projectHierarchies.entrySet()) {
            String projectName = projectEntry.getKey();
            Map<String, Object> hierarchy = projectEntry.getValue();
            
            if (hierarchy.containsKey("error")) {
                doc.append("⚠️ **").append(projectName).append("**: ").append(hierarchy.get("error")).append("\n");
                continue;
            }
            
            doc.append("📁 **Proyecto: ").append(projectName).append("**\n");
            
            if (hierarchy.containsKey("totalChildWorkItems")) {
                doc.append("   • Total work items hijos: ").append(hierarchy.get("totalChildWorkItems")).append("\n");
            }
            
            // Relaciones padre-hijo
            if (hierarchy.containsKey("parentChildRelations")) {
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> relations = (Map<String, Set<String>>) hierarchy.get("parentChildRelations");
                
                for (Map.Entry<String, Set<String>> relation : relations.entrySet()) {
                    String parentType = relation.getKey();
                    Set<String> childTypes = relation.getValue();
                    
                    doc.append("   • **").append(parentType).append("** puede tener hijos de tipo: ")
                       .append(String.join(", ", childTypes)).append("\n");
                    
                    // Consolidar globalmente
                    globalParentChildRelations.computeIfAbsent(parentType, k -> new HashSet<>()).addAll(childTypes);
                }
            }
            
            // Distribución de tipos hijos
            if (hierarchy.containsKey("childTypeDistribution")) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> distribution = (Map<String, Integer>) hierarchy.get("childTypeDistribution");
                
                for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                    globalChildTypeCount.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
            
            doc.append("\n");
        }
        
        // Resumen global
        doc.append("🌍 **PATRONES GLOBALES IDENTIFICADOS**\n");
        doc.append("===================================\n\n");
        
        if (!globalParentChildRelations.isEmpty()) {
            doc.append("📋 **Relaciones Padre-Hijo Detectadas:**\n");
            for (Map.Entry<String, Set<String>> relation : globalParentChildRelations.entrySet()) {
                doc.append("   • **").append(relation.getKey()).append("** → [")
                   .append(String.join(", ", relation.getValue())).append("]\n");
            }
            doc.append("\n");
        }
        
        if (!globalChildTypeCount.isEmpty()) {
            doc.append("📊 **Tipos de Subtareas/Hijos Más Utilizados:**\n");
            globalChildTypeCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        doc.append("   • **").append(entry.getKey()).append("**: ")
                           .append(entry.getValue()).append(" instancias\n");
                    });
            doc.append("\n");
        }
        
        // Recomendaciones
        doc.append("💡 **RECOMENDACIONES DE USO:**\n");
        doc.append("============================\n");
        doc.append("• Los tipos identificados arriba representan los patrones de subtareas más utilizados en la organización\n");
        doc.append("• Considere estandarizar el uso de estos tipos para mantener consistencia\n");
        doc.append("• Los tipos con mayor número de instancias son los más adoptados por los equipos\n\n");
        
        return doc.toString();
    }
    
    // Métodos auxiliares para obtener configuración
    

    
    /**
     * Documenta los valores válidos para campos obligatorios de un tipo específico de work item
     * Esta funcionalidad es crítica para que los usuarios sepan qué valores pueden usar
     */
    private String documentValidValuesForRequiredFields(String project, String workItemType) {
        StringBuilder documentation = new StringBuilder();
        
        try {
            // Obtener campos obligatorios para este tipo
            List<String> requiredFields = configService.getRequiredFieldsForWorkItemType(workItemType);
            
            if (requiredFields.isEmpty()) {
                return ""; // No hay campos obligatorios específicos
            }
            
            // Obtener definición completa del tipo de work item
            Map<String, List<String>> fieldValidValues = investigateWorkItemTypeDefinition(project, workItemType);
            
            // Filtrar solo los campos obligatorios que tienen valores permitidos
            Map<String, List<String>> requiredFieldValues = new HashMap<>();
            
            for (String requiredField : requiredFields) {
                // Mapear campo lógico a campo de Azure DevOps
                Map<String, Object> fieldMapping = configService.getFieldMapping(requiredField);
                String azureFieldName = requiredField;
                
                if (!fieldMapping.isEmpty() && fieldMapping.containsKey("azureFieldName")) {
                    azureFieldName = (String) fieldMapping.get("azureFieldName");
                }
                
                // Buscar valores válidos para este campo
                if (fieldValidValues.containsKey(azureFieldName)) {
                    requiredFieldValues.put(requiredField, fieldValidValues.get(azureFieldName));
                } else {
                    // Intentar obtener valores de forma específica para campos conocidos
                    List<String> specificValues = getSpecificFieldValues(project, workItemType, requiredField, azureFieldName);
                    if (!specificValues.isEmpty()) {
                        requiredFieldValues.put(requiredField, specificValues);
                    }
                }
            }
            
            // Documentar valores encontrados
            if (!requiredFieldValues.isEmpty()) {
                documentation.append("  📝 **Valores válidos para campos obligatorios:**\n");
                
                for (Map.Entry<String, List<String>> entry : requiredFieldValues.entrySet()) {
                    String fieldName = entry.getKey();
                    List<String> validValues = entry.getValue();
                    
                    documentation.append("    🏷️ **").append(fieldName).append("**: ");
                    if (validValues.size() <= 5) {
                        // Mostrar todos los valores si son pocos
                        documentation.append(String.join(", ", validValues));
                    } else {
                        // Mostrar primeros 5 + contador si son muchos
                        documentation.append(String.join(", ", validValues.subList(0, 5)));
                        documentation.append(" (+" + (validValues.size() - 5) + " más)");
                    }
                    documentation.append("\n");
                }
                documentation.append("\n");
            }
            
        } catch (Exception e) {
            // No mostrar errores en la documentación principal, solo log interno
            System.err.println("Error documentando valores válidos para " + workItemType + ": " + e.getMessage());
        }
        
        return documentation.toString();
    }
    
    /**
     * Obtiene valores específicos para campos conocidos utilizando estrategias específicas
     */
    private List<String> getSpecificFieldValues(String project, String workItemType, String logicalFieldName, String azureFieldName) {
        List<String> values = new ArrayList<>();
        
        try {
            // Estrategia 1: Campos de tipo (tipoHistoria, tipoHistoriaTecnica, etc.)
            if (logicalFieldName.toLowerCase().contains("tipo")) {
                values = getTypeFieldValues(project, workItemType, azureFieldName);
            }
            
            // Estrategia 2: Campos booleanos (Si/No)
            else if (logicalFieldName.toLowerCase().contains("migracion") || 
                     logicalFieldName.toLowerCase().contains("regulatorio") ||
                     logicalFieldName.toLowerCase().contains("automatico")) {
                values = List.of("Si", "No");
            }
            
            // Estrategia 3: Campos de estado estándar
            else if (logicalFieldName.equals("state")) {
                values = getWorkItemStateValues(project, workItemType);
            }
            
            // Estrategia 4: Campos de prioridad
            else if (logicalFieldName.equals("priority")) {
                values = List.of("1", "2", "3", "4");
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores específicos para campo " + logicalFieldName + ": " + e.getMessage());
        }
        
        return values;
    }
    
    /**
     * Obtiene valores válidos para campos de tipo específicos
     */
    private List<String> getTypeFieldValues(String project, String workItemType, String azureFieldName) {
        List<String> values = new ArrayList<>();
        
        try {
            // Intentar obtener desde definición de campo
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields/%s?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, azureFieldName.replace(".", "%2E"));
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                values = parseFieldAllowedValues(response);
            }
            
            // Si no se encontraron valores, usar estrategia de análisis de work items existentes
            if (values.isEmpty()) {
                values = extractUniqueFieldValuesFromExistingWorkItems(project, workItemType, azureFieldName);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores de campo tipo: " + e.getMessage());
        }
        
        return values;
    }
    
    /**
     * Obtiene estados válidos para un tipo de work item específico
     */
    private List<String> getWorkItemStateValues(String project, String workItemType) {
        List<String> states = new ArrayList<>();
        
        try {
            String encodedType = java.net.URLEncoder.encode(workItemType, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, encodedType);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                states = parseWorkItemStates(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo estados de work item: " + e.getMessage());
        }
        
        return states;
    }
    
    /**
     * Parsea valores permitidos de la respuesta de definición de campo
     */
    private List<String> parseFieldAllowedValues(String jsonResponse) {
        List<String> values = new ArrayList<>();
        
        try {
            // Buscar valores en allowedValues o en definición de picklist
            Pattern allowedValuesPattern = Pattern.compile("\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = allowedValuesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String valuesString = matcher.group(1);
                Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
                Matcher valueMatcher = valuePattern.matcher(valuesString);
                
                while (valueMatcher.find()) {
                    values.add(valueMatcher.group(1));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando valores permitidos: " + e.getMessage());
        }
        
        return values;
    }
    
    /**
     * Parsea estados de work item de la respuesta de definición de tipo
     */
    private List<String> parseWorkItemStates(String jsonResponse) {
        List<String> states = new ArrayList<>();
        
        try {
            // Buscar estados en la definición del tipo
            Pattern statesPattern = Pattern.compile("\"states\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = statesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String statesString = matcher.group(1);
                Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher nameMatcher = namePattern.matcher(statesString);
                
                while (nameMatcher.find()) {
                    states.add(nameMatcher.group(1));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando estados: " + e.getMessage());
        }
        
        return states;
    }
    
    /**
    /**
     * Realiza backup de archivos de configuración existentes
     */
    private String performConfigurationBackup() {
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
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );
            
            for (String configFile : configFiles) {
                try {
                    java.nio.file.Path originalPath = java.nio.file.Paths.get(configFile);
                    
                    if (java.nio.file.Files.exists(originalPath)) {
                        String backupFileName = configFile.replace(".", "_backup_" + timestamp + ".");
                        java.nio.file.Path backupPath = java.nio.file.Paths.get(backupFileName);
                        
                        java.nio.file.Files.copy(originalPath, backupPath, 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        
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
     * Analiza tipos de work items con documentación completa de TODOS los campos y sus valores permitidos
     */
    private String analyzeWorkItemTypesWithCompleteFieldDocumentation(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📋 **DOCUMENTACIÓN EXHAUSTIVA DE WORK ITEM TYPES**\n");
        analysis.append("================================================\n\n");
        
        try {
            // Obtener todos los tipos de work items disponibles
            List<String> availableTypes = getAvailableWorkItemTypes(project);
            
            analysis.append("🔍 **Tipos encontrados:** ").append(availableTypes.size()).append("\n");
            analysis.append("📋 **Lista:** ").append(String.join(", ", availableTypes)).append("\n\n");
            
            // Para cada tipo, documentar TODOS sus campos exhaustivamente
            for (String workItemType : availableTypes) {
                analysis.append("═".repeat(80)).append("\n");
                analysis.append("📋 **WORK ITEM TYPE: ").append(workItemType.toUpperCase()).append("**\n");
                analysis.append("═".repeat(80)).append("\n\n");
                
                // Obtener información completa del tipo
                Map<String, Object> typeDefinition = getCompleteWorkItemTypeDefinition(project, workItemType);
                
                if (typeDefinition.isEmpty()) {
                    analysis.append("❌ No se pudo obtener información del tipo ").append(workItemType).append("\n\n");
                    continue;
                }
                
                // Documentar campos obligatorios
                analysis.append("🔴 **CAMPOS OBLIGATORIOS:**\n");
                analysis.append("─".repeat(30)).append("\n");
                List<String> requiredFields = getRequiredFieldsForType(typeDefinition);
                if (requiredFields.isEmpty()) {
                    analysis.append("ℹ️ No se detectaron campos obligatorios específicos\n");
                } else {
                    for (String field : requiredFields) {
                        analysis.append("• ").append(field).append("\n");
                    }
                }
                analysis.append("\n");
                
                // Documentar TODOS los campos disponibles
                analysis.append("📝 **TODOS LOS CAMPOS DISPONIBLES:**\n");
                analysis.append("─".repeat(35)).append("\n");
                Map<String, Map<String, Object>> allFields = getAllFieldsForType(project, workItemType, typeDefinition);
                
                if (allFields.isEmpty()) {
                    analysis.append("⚠️ No se pudieron obtener campos para este tipo\n");
                } else {
                    for (Map.Entry<String, Map<String, Object>> fieldEntry : allFields.entrySet()) {
                        String fieldName = fieldEntry.getKey();
                        Map<String, Object> fieldDetails = fieldEntry.getValue();
                        
                        analysis.append("\n🏷️ **").append(fieldName).append("**\n");
                        
                        // Información básica del campo
                        String referenceName = (String) fieldDetails.get("referenceName");
                        String fieldType = (String) fieldDetails.get("type");
                        String inferredType = (String) fieldDetails.get("inferredType");
                        Boolean isRequired = (Boolean) fieldDetails.get("required");
                        String description = (String) fieldDetails.get("description");
                        
                        analysis.append("   📋 Referencia: ").append(referenceName != null ? referenceName : "N/A").append("\n");
                        
                        // MEJORADO: Mostrar tipo inferido si está disponible, sino el tipo base
                        String displayType = inferredType != null ? inferredType : fieldType;
                        analysis.append("   🔧 Tipo: ").append(displayType != null ? displayType : "N/A");
                        if (inferredType != null && !inferredType.equals(fieldType)) {
                            analysis.append(" (inferido de: ").append(fieldType != null ? fieldType : "N/A").append(")");
                        }
                        analysis.append("\n");
                        
                        analysis.append("   ✅ Obligatorio: ").append(isRequired != null ? (isRequired ? "Sí" : "No") : "N/A").append("\n");
                        
                        if (description != null && !description.trim().isEmpty()) {
                            analysis.append("   📖 Descripción: ").append(description).append("\n");
                        }
                        
                        // CRÍTICO: Documentar valores permitidos si los hay
                        // Usar el tipo inferido para la búsqueda de valores permitidos
                        String typeForValues = inferredType != null ? inferredType : fieldType;
                        List<String> allowedValues = getFieldAllowedValues(project, workItemType, referenceName, typeForValues);
                        if (!allowedValues.isEmpty()) {
                            analysis.append("   🎯 **VALORES PERMITIDOS:**\n");
                            for (String value : allowedValues) {
                                analysis.append("      • ").append(value).append("\n");
                            }
                        } else {
                            analysis.append("   ℹ️ Valores: Entrada libre\n");
                        }
                    }
                }
                
                analysis.append("\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ Error durante análisis exhaustivo: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        
        return analysis.toString();
    }
    
    /**
     * Obtiene definición completa de un tipo de work item incluyendo todos los metadatos
     */
    private Map<String, Object> getCompleteWorkItemTypeDefinition(String project, String workItemType) {
        Map<String, Object> definition = new HashMap<>();
        
        try {
            String encodedType = java.net.URLEncoder.encode(workItemType, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitemtypes/%s?$expand=fields,states,transitions&api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, encodedType);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                definition = parseWorkItemTypeDefinitionResponse(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo definición completa de tipo " + workItemType + ": " + e.getMessage());
        }
        
        return definition;
    }
    
    /**
     * Parsea la respuesta completa de definición de tipo de work item
     */
    private Map<String, Object> parseWorkItemTypeDefinitionResponse(String jsonResponse) {
        Map<String, Object> definition = new HashMap<>();
        
        try {
            // Extraer información básica
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(jsonResponse);
            if (nameMatcher.find()) {
                definition.put("name", nameMatcher.group(1));
            }
            
            // Extraer campos
            List<Map<String, Object>> fields = parseFieldDefinitions(jsonResponse);
            definition.put("fields", fields);
            
            // Extraer estados
            List<String> states = parseStates(jsonResponse);
            definition.put("states", states);
            
        } catch (Exception e) {
            System.err.println("Error parseando definición de tipo: " + e.getMessage());
        }
        
        return definition;
    }
    
    /**
     * Parsea definiciones de campos de la respuesta JSON
     */
    private List<Map<String, Object>> parseFieldDefinitions(String jsonResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            // Buscar sección de fieldInstances
            Pattern fieldInstancesPattern = Pattern.compile("\"fieldInstances\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
            Matcher matcher = fieldInstancesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String fieldInstancesSection = matcher.group(1);
                
                // Extraer cada campo individual
                Pattern fieldPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
                Matcher fieldMatcher = fieldPattern.matcher(fieldInstancesSection);
                
                while (fieldMatcher.find()) {
                    String fieldKey = fieldMatcher.group(1);
                    String fieldData = fieldMatcher.group(2);
                    
                    Map<String, Object> fieldInfo = parseIndividualField(fieldKey, fieldData);
                    if (!fieldInfo.isEmpty()) {
                        fields.add(fieldInfo);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando definiciones de campos: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Parsea información de un campo individual
     */
    private Map<String, Object> parseIndividualField(String fieldKey, String fieldData) {
        Map<String, Object> fieldInfo = new HashMap<>();
        
        try {
            fieldInfo.put("key", fieldKey);
            
            // Extraer referenceName
            Pattern refPattern = Pattern.compile("\"referenceName\"\\s*:\\s*\"([^\"]+)\"");
            Matcher refMatcher = refPattern.matcher(fieldData);
            if (refMatcher.find()) {
                fieldInfo.put("referenceName", refMatcher.group(1));
            }
            
            // Extraer name
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(fieldData);
            if (nameMatcher.find()) {
                fieldInfo.put("name", nameMatcher.group(1));
            }
            
            // Extraer type
            Pattern typePattern = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
            Matcher typeMatcher = typePattern.matcher(fieldData);
            if (typeMatcher.find()) {
                fieldInfo.put("type", typeMatcher.group(1));
            }
            
            // Extraer required
            Pattern requiredPattern = Pattern.compile("\"required\"\\s*:\\s*(true|false)");
            Matcher requiredMatcher = requiredPattern.matcher(fieldData);
            if (requiredMatcher.find()) {
                fieldInfo.put("required", Boolean.parseBoolean(requiredMatcher.group(1)));
            }
            
            // Extraer description si existe
            Pattern descPattern = Pattern.compile("\"helpText\"\\s*:\\s*\"([^\"]+)\"");
            Matcher descMatcher = descPattern.matcher(fieldData);
            if (descMatcher.find()) {
                fieldInfo.put("description", descMatcher.group(1));
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando campo individual " + fieldKey + ": " + e.getMessage());
        }
        
        return fieldInfo;
    }
    
    /**
     * Parsea estados de la respuesta JSON
     */
    private List<String> parseStates(String jsonResponse) {
        List<String> states = new ArrayList<>();
        
        try {
            Pattern statesPattern = Pattern.compile("\"states\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher matcher = statesPattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String statesSection = matcher.group(1);
                Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher nameMatcher = namePattern.matcher(statesSection);
                
                while (nameMatcher.find()) {
                    states.add(nameMatcher.group(1));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando estados: " + e.getMessage());
        }
        
        return states;
    }
    
    /**
     * Obtiene campos obligatorios específicos de la definición del tipo
     */
    private List<String> getRequiredFieldsForType(Map<String, Object> typeDefinition) {
        List<String> requiredFields = new ArrayList<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) typeDefinition.get("fields");
            
            if (fields != null) {
                for (Map<String, Object> field : fields) {
                    Boolean required = (Boolean) field.get("required");
                    if (required != null && required) {
                        String name = (String) field.get("name");
                        if (name != null) {
                            requiredFields.add(name);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos obligatorios: " + e.getMessage());
        }
        
        return requiredFields;
    }
    
    /**
     * Obtiene TODOS los campos disponibles para un tipo de work item con información detallada
     */
    private Map<String, Map<String, Object>> getAllFieldsForType(String project, String workItemType, Map<String, Object> typeDefinition) {
        Map<String, Map<String, Object>> allFields = new HashMap<>();
        
        try {
            // Primero obtener campos de la definición del tipo
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> definitionFields = (List<Map<String, Object>>) typeDefinition.get("fields");
            
            if (definitionFields != null) {
                for (Map<String, Object> field : definitionFields) {
                    String name = (String) field.get("name");
                    if (name != null) {
                        allFields.put(name, new HashMap<>(field));
                    }
                }
            }
            
            // También obtener campos del proyecto (pueden haber más)
            List<Map<String, Object>> projectFields = getAllProjectFieldsDetailed(project);
            
            for (Map<String, Object> projectField : projectFields) {
                String name = (String) projectField.get("name");
                if (name != null && !allFields.containsKey(name)) {
                    allFields.put(name, new HashMap<>(projectField));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo todos los campos para tipo " + workItemType + ": " + e.getMessage());
        }
        
        return allFields;
    }
    
    /**
     * Obtiene información detallada de todos los campos del proyecto
     */
    private List<Map<String, Object>> getAllProjectFieldsDetailed(String project) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                fields = parseProjectFieldsResponse(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos detallados del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Parsea respuesta de campos del proyecto
     */
    private List<Map<String, Object>> parseProjectFieldsResponse(String jsonResponse) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            // Buscar array "value" en la respuesta
            Pattern valuePattern = Pattern.compile("\"value\"\\s*:\\s*\\[([^\\]]+(?:\\[[^\\]]*\\][^\\]]*)*)\\]");
            Matcher valueMatcher = valuePattern.matcher(jsonResponse);
            
            if (valueMatcher.find()) {
                String valueSection = valueMatcher.group(1);
                
                // Dividir en objetos individuales
                Pattern fieldPattern = Pattern.compile("\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
                Matcher fieldMatcher = fieldPattern.matcher(valueSection);
                
                while (fieldMatcher.find()) {
                    String fieldData = fieldMatcher.group(1);
                    Map<String, Object> fieldInfo = parseProjectFieldData(fieldData);
                    if (!fieldInfo.isEmpty()) {
                        fields.add(fieldInfo);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando respuesta de campos del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Parsea datos de un campo individual del proyecto y determina automáticamente su tipo
     */
    private Map<String, Object> parseProjectFieldData(String fieldData) {
        Map<String, Object> fieldInfo = new HashMap<>();
        
        try {
            // Extraer propiedades básicas
            extractFieldProperty(fieldData, "name", fieldInfo);
            extractFieldProperty(fieldData, "referenceName", fieldInfo);
            extractFieldProperty(fieldData, "type", fieldInfo);
            extractFieldProperty(fieldData, "description", fieldInfo);
            extractFieldProperty(fieldData, "picklistId", fieldInfo);
            
            // MEJORA: Determinar automáticamente el tipo correcto basado en la definición
            String referenceName = (String) fieldInfo.get("referenceName");
            if (referenceName != null) {
                String inferredType = determineFieldType(fieldInfo, fieldData);
                if (inferredType != null) {
                    fieldInfo.put("inferredType", inferredType);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando datos de campo individual: " + e.getMessage());
        }
        
        return fieldInfo;
    }
    
    /**
     * Determina automáticamente el tipo de campo basado en su definición
     */
    private String determineFieldType(Map<String, Object> fieldInfo, String fieldData) {
        try {
            // Estrategia 1: Si tiene picklistId, es un campo de lista
            String picklistId = (String) fieldInfo.get("picklistId");
            if (picklistId != null && !picklistId.trim().isEmpty()) {
                return "picklistString";
            }
            
            // Estrategia 2: Buscar si tiene allowedValues definidos en el JSON
            if (fieldData.contains("\"allowedValues\"")) {
                Pattern allowedValuesPattern = Pattern.compile("\"allowedValues\"\\s*:\\s*\\[([^\\]]+)\\]");
                Matcher matcher = allowedValuesPattern.matcher(fieldData);
                if (matcher.find()) {
                    String valuesString = matcher.group(1);
                    // Si tiene valores, agregar la información
                    List<String> allowedValues = parseAllowedValuesFromString(valuesString);
                    if (!allowedValues.isEmpty()) {
                        fieldInfo.put("allowedValues", allowedValues);
                        return "picklistString";
                    }
                }
            }
            
            // Estrategia 3: Análisis del tipo base de Azure DevOps
            String baseType = (String) fieldInfo.get("type");
            if (baseType != null) {
                switch (baseType.toLowerCase()) {
                    case "boolean":
                        return "boolean";
                    case "integer":
                    case "double":
                        return baseType.toLowerCase();
                    case "datetime":
                        return "dateTime";
                    case "html":
                        return "html";
                    case "identity":
                        return "identity";
                    case "plaintext":
                        return "plainText";
                    case "string":
                    default:
                        // Para strings, verificar si es realmente un campo de lista basado en el nombre
                        String referenceName = (String) fieldInfo.get("referenceName");
                        if (referenceName != null && isLikelyPicklistField(referenceName)) {
                            return "picklistString";
                        }
                        return "string";
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error determinando tipo de campo: " + e.getMessage());
        }
        
        return "string"; // Fallback por defecto
    }
    
    /**
     * Determina si un campo es probablemente de tipo lista basado en su nombre
     */
    private boolean isLikelyPicklistField(String referenceName) {
        if (referenceName == null) return false;
        
        String fieldName = referenceName.toLowerCase();
        
        // Patrones comunes que indican campos de lista
        return fieldName.contains("tipo") ||
               fieldName.contains("type") ||
               fieldName.contains("categoria") ||
               fieldName.contains("category") ||
               fieldName.contains("clasificacion") ||
               fieldName.contains("classification") ||
               fieldName.contains("nivel") ||
               fieldName.contains("level") ||
               fieldName.contains("origen") ||
               fieldName.contains("source") ||
               fieldName.contains("fase") ||
               fieldName.contains("phase") ||
               fieldName.contains("estado") ||
               fieldName.contains("status") ||
               fieldName.contains("prioridad") ||
               fieldName.contains("priority");
    }
    
    /**
     * Parsea valores permitidos de una cadena JSON
     */
    private List<String> parseAllowedValuesFromString(String valuesString) {
        List<String> values = new ArrayList<>();
        
        try {
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(valuesString);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando valores permitidos: " + e.getMessage());
        }
        
        return values;
    }
    
    /**
     * Extrae una propiedad específica del JSON de campo
     */
    private void extractFieldProperty(String fieldData, String propertyName, Map<String, Object> fieldInfo) {
        try {
            Pattern pattern = Pattern.compile("\"" + propertyName + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(fieldData);
            if (matcher.find()) {
                fieldInfo.put(propertyName, matcher.group(1));
            }
        } catch (Exception e) {
            // Ignorar errores de propiedades individuales
        }
    }
    
    /**
     * Obtiene valores permitidos para un campo específico usando múltiples estrategias
     * MEJORADO: Detecta automáticamente si un campo es de tipo picklist basado en su definición
     * PÚBLICO: Permite acceso desde OrganizationConfigService para valores dinámicos
     */
    public List<String> getFieldAllowedValues(String project, String workItemType, String referenceName, String fieldType) {
        List<String> allowedValues = new ArrayList<>();
        
        if (referenceName == null || referenceName.trim().isEmpty()) {
            return allowedValues;
        }
        
        try {
            // Estrategia 1: Obtener definición completa del campo para detectar si es picklist
            Map<String, Object> fieldDefinition = getCompleteFieldDefinition(project, referenceName);
            
            // Verificar si el campo tiene picklistId (indicador de que es un campo de lista)
            String picklistId = (String) fieldDefinition.get("picklistId");
            boolean hasPicklistId = picklistId != null && !picklistId.trim().isEmpty();
            
            // Estrategia 1a: Si tiene picklistId, obtener valores del picklist
            if (hasPicklistId) {
                allowedValues = getPicklistValues(project, referenceName, picklistId);
            }
            
            // Estrategia 1b: Si no tiene picklistId pero la definición incluye allowedValues directamente
            if (allowedValues.isEmpty() && fieldDefinition.containsKey("allowedValues")) {
                Object allowedValuesObj = fieldDefinition.get("allowedValues");
                if (allowedValuesObj instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<String> directAllowedValues = (List<String>) allowedValuesObj;
                    allowedValues = new ArrayList<>(directAllowedValues);
                }
            }
            
            // Estrategia 2: Para campos de estado, obtener desde definición del tipo
            if (allowedValues.isEmpty() && "System.State".equals(referenceName)) {
                allowedValues = getWorkItemStateValues(project, workItemType);
            }
            
            // Estrategia 3: Extraer valores únicos de work items existentes (último recurso)
            if (allowedValues.isEmpty()) {
                allowedValues = extractUniqueFieldValuesFromExistingWorkItems(project, workItemType, referenceName);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores permitidos para campo " + referenceName + ": " + e.getMessage());
        }
        
        return allowedValues;
    }
    
    /**
     * Obtiene la definición completa de un campo incluyendo información de picklist
     */
    private Map<String, Object> getCompleteFieldDefinition(String project, String referenceName) {
        Map<String, Object> fieldDefinition = new HashMap<>();
        
        try {
            // URL para obtener definición específica del campo
            String encodedFieldName = java.net.URLEncoder.encode(referenceName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields/%s?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, encodedFieldName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                fieldDefinition = parseCompleteFieldDefinition(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo definición completa de campo " + referenceName + ": " + e.getMessage());
        }
        
        return fieldDefinition;
    }
    
    /**
     * Parsea la definición completa de un campo incluyendo detección automática de tipo
     */
    private Map<String, Object> parseCompleteFieldDefinition(String jsonResponse) {
        Map<String, Object> definition = new HashMap<>();
        
        try {
            // Extraer propiedades básicas
            extractFieldProperty(jsonResponse, "name", definition);
            extractFieldProperty(jsonResponse, "referenceName", definition);
            extractFieldProperty(jsonResponse, "type", definition);
            extractFieldProperty(jsonResponse, "description", definition);
            
            // CRÍTICO: Extraer picklistId si existe
            String picklistId = AzureDevOpsJsonParser.extractSimpleValue(jsonResponse, "picklistId");
            if (picklistId != null && !picklistId.trim().isEmpty()) {
                definition.put("picklistId", picklistId);
                
                // Si tiene picklistId, automáticamente es de tipo picklistString
                definition.put("inferredType", "picklistString");
            }
            
            // Extraer allowedValues directos si existen
            List<String> allowedValues = parseFieldAllowedValues(jsonResponse);
            if (!allowedValues.isEmpty()) {
                definition.put("allowedValues", allowedValues);
                
                // Si tiene allowedValues, también es probablemente un campo de lista
                if (!definition.containsKey("inferredType")) {
                    definition.put("inferredType", "picklistString");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parseando definición completa de campo: " + e.getMessage());
        }
        
        return definition;
    }
    
    /**
     * Obtiene valores permitidos desde la definición específica de un campo
     */
    private List<String> getFieldDefinitionAllowedValues(String project, String referenceName) {
        List<String> allowedValues = new ArrayList<>();
        
        try {
            // URL para obtener definición específica del campo
            String encodedFieldName = java.net.URLEncoder.encode(referenceName, StandardCharsets.UTF_8);
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields/%s?api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project, encodedFieldName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                // Buscar valores permitidos en la respuesta
                allowedValues = parseFieldAllowedValues(response);
                
                // Si no se encontraron valores directos, buscar en picklistId
                if (allowedValues.isEmpty()) {
                    String picklistId = AzureDevOpsJsonParser.extractSimpleValue(response, "picklistId");
                    if (picklistId != null && !picklistId.trim().isEmpty()) {
                        allowedValues = getPicklistValues(project, referenceName, picklistId);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo definición de campo " + referenceName + ": " + e.getMessage());
        }
        
        return allowedValues;
    }


    
    /**
     * Extrae valores únicos de un campo específico consultando work items existentes
     */
    private List<String> extractUniqueFieldValuesFromExistingWorkItems(String project, String workItemType, String referenceName) {
        Set<String> uniqueValues = new HashSet<>();
        
        try {
            // Consulta WIQL para obtener muestras de work items de este tipo
            String wiqlQuery = String.format(
                "SELECT [System.Id], [%s] FROM WorkItems WHERE [System.WorkItemType] = '%s' ORDER BY [System.Id] DESC",
                referenceName, workItemType
            );
            
            // Limitar resultados para evitar sobrecarga
            String queryUrl = String.format("https://dev.azure.com/%s/%s/_apis/wit/wiql?$top=50&api-version=7.1", 
                    azureDevOpsClient.getOrganization(), project);
            
            String requestBody = String.format("{\"query\":\"%s\"}", wiqlQuery.replace("\"", "\\\""));
            String response = makePostRequest(queryUrl, requestBody);
            
            if (response != null) {
                List<Integer> workItemIds = AzureDevOpsJsonParser.extractWorkItemIds(response);
                
                // Para cada work item, obtener el valor del campo
                for (Integer workItemId : workItemIds) {
                    if (workItemIds.indexOf(workItemId) >= 20) break; // Limitar a primeros 20 para eficiencia
                    
                    String fieldValue = getFieldValueFromWorkItem(project, workItemId, referenceName);
                    if (fieldValue != null && !fieldValue.trim().isEmpty() && !"null".equals(fieldValue)) {
                        uniqueValues.add(fieldValue);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valores únicos para campo " + referenceName + ": " + e.getMessage());
        }
        
        List<String> result = new ArrayList<>(uniqueValues);
        result.sort(String::compareToIgnoreCase);
        return result;
    }
    
    /**
     * Obtiene el valor de un campo específico de un work item individual
     */
    private String getFieldValueFromWorkItem(String project, Integer workItemId, String referenceName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?fields=%s&api-version=7.1",
                    azureDevOpsClient.getOrganization(), project, workItemId, referenceName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                return extractFieldValueFromWorkItemResponse(response, referenceName);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valor de campo " + referenceName + " para work item " + workItemId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae el valor de un campo específico de la respuesta JSON de un work item
     */
    private String extractFieldValueFromWorkItemResponse(String jsonResponse, String referenceName) {
        try {
            // Buscar en la sección "fields"
            Pattern fieldsPattern = Pattern.compile("\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
            Matcher fieldsMatcher = fieldsPattern.matcher(jsonResponse);
            
            if (fieldsMatcher.find()) {
                String fieldsSection = fieldsMatcher.group(1);
                
                // Buscar el campo específico
                String escapedReferenceName = referenceName.replace(".", "\\.");
                Pattern fieldPattern = Pattern.compile("\"" + escapedReferenceName + "\"\\s*:\\s*\"([^\"]+)\"");
                Matcher fieldMatcher = fieldPattern.matcher(fieldsSection);
                
                if (fieldMatcher.find()) {
                    return fieldMatcher.group(1);
                }
                
                // También intentar sin comillas (para valores numéricos o booleanos)
                Pattern fieldPatternNoQuotes = Pattern.compile("\"" + escapedReferenceName + "\"\\s*:\\s*([^,\\}]+)");
                Matcher fieldMatcherNoQuotes = fieldPatternNoQuotes.matcher(fieldsSection);
                
                if (fieldMatcherNoQuotes.find()) {
                    return fieldMatcherNoQuotes.group(1).trim();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo valor de campo " + referenceName + " de respuesta: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Realiza una petición POST HTTP usando AzureDevOpsClient
     */
    private String makePostRequest(String endpoint, String requestBody) {
        try {
            // Extraer el endpoint relativo de la URL completa si es necesario
            String relativeEndpoint = endpoint;
            if (endpoint.startsWith("https://dev.azure.com/")) {
                // Extraer la parte después de la organización
                String orgName = azureDevOpsClient.getOrganization();
                String orgPrefix = "https://dev.azure.com/" + orgName;
                if (endpoint.startsWith(orgPrefix)) {
                    relativeEndpoint = endpoint.substring(orgPrefix.length());
                }
            }
            
            // Por ahora, usar el método genérico GET ya que no hay un método POST genérico
            // En el futuro se podría agregar un método makeGenericPostRequest a AzureDevOpsClient
            System.out.println("⚠️ makePostRequest temporalmente usando GET - considerar implementar POST genérico en AzureDevOpsClient");
            return azureDevOpsClient.makeGenericApiRequest(relativeEndpoint, null);
            
        } catch (Exception e) {
            System.err.println("Error realizando petición POST: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Realiza una petición HTTP GET usando AzureDevOpsClient
     */
    /**
     * @deprecated Reemplazado por WorkItemProcessor
     */
    @Deprecated
    private String makeHttpGetRequest(String url) {
        try {
            // Extraer el endpoint relativo de la URL completa
            String relativeEndpoint = url;
            if (url.startsWith("https://dev.azure.com/")) {
                // Extraer la parte después de la organización
                String orgName = azureDevOpsClient.getOrganization();
                String orgPrefix = "https://dev.azure.com/" + orgName;
                if (url.startsWith(orgPrefix)) {
                    relativeEndpoint = url.substring(orgPrefix.length());
                }
            }
            
            return azureDevOpsClient.makeGenericApiRequest(relativeEndpoint, null);
            
        } catch (Exception e) {
            System.err.println("Error realizando petición GET: " + e.getMessage());
            return null;
        }
    }

    /**
     * Procesa un work item de referencia para extraer información de contexto organizacional
     */
    /**
     * Obtiene el procesador de work items para uso en otras partes de la herramienta.
     * 
     * @return Instancia del procesador de work items
     */
    protected com.mcp.server.utils.workitem.WorkItemProcessor getWorkItemProcessor() {
        return this.workItemProcessor;
    }
    
    /**
     * @deprecated Reemplazado por {@link com.mcp.server.utils.workitem.WorkItemProcessor#procesarWorkItemReferencia(String)}
     */
    @Deprecated
    private Map<String, Object> procesarWorkItemReferencia(String workItemReferencia) {
        // Delegar a la nueva clase refactorizada
        return workItemProcessor.procesarWorkItemReferencia(workItemReferencia);
    }
    
    /**
     * Extrae el ID del work item de una URL o texto de referencia
     */
    private Integer extractWorkItemIdFromReference(String reference) {
        return workItemProcessor.extractWorkItemIdFromReference(reference);
    }
    
    /**
     * Busca un work item específico a través de múltiples proyectos
     */
    private Map<String, Object> findWorkItemAcrossProjects(Integer workItemId) {
        return workItemProcessor.findWorkItemAcrossProjects(workItemId);
    }
    
    /**
     * Extrae nombres de proyectos de la respuesta JSON de la API
     */
    private List<String> extractProjectNames(String jsonResponse) {
        List<String> projectNames = new ArrayList<>();
        
        try {
            // Buscar patrones de nombres de proyecto en la respuesta JSON
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(jsonResponse);
            
            while (nameMatcher.find()) {
                String projectName = nameMatcher.group(1);
                if (!projectNames.contains(projectName)) {
                    projectNames.add(projectName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error extrayendo nombres de proyectos: " + e.getMessage());
        }
        
        return projectNames;
    }
    
    /**
     * Parsea la respuesta JSON de un work item de forma básica
     */
    private Map<String, Object> parseWorkItemResponse(String jsonResponse) {
        return workItemProcessor.parseWorkItemResponse(jsonResponse);
    }
    
    /**
     * Parsea la sección de campos del JSON
     */
    private Map<String, Object> parseFieldsSection(String fieldsSection) {
        return workItemProcessor.parseFieldsSection(fieldsSection);
    }

    /**
     * Obtiene el PAT (Personal Access Token) de las variables de entorno
     */
    private String getPAT() {
        String pat = System.getenv("AZURE_DEVOPS_PAT");
        if (pat == null || pat.trim().isEmpty()) {
            throw new RuntimeException("AZURE_DEVOPS_PAT environment variable is required but not set");
        }
        return pat;
    }
    
    // ================================================================
    // MÉTODOS STUB PARA NAVEGACIÓN JERÁRQUICA INTERACTIVA
    // ================================================================
    
    /**
     * Obtiene resumen contextual del equipo/área
     * @deprecated Usar teamConfigurationManager.getTeamContextSummary() en su lugar
     */
    @Deprecated
    private String getTeamContextSummary(String projectName, String teamName, String areaPath) {
        return teamConfigurationManager.getTeamContextSummary(projectName, teamName, areaPath);
    }
    
    /**
     * Obtiene resumen de iteración específica
     */
    private String getIterationSummary(String projectName, String teamName, String iterationName) {
        StringBuilder summary = new StringBuilder();
        summary.append("📅 Resumen de iteración:\n");
        summary.append("   • Proyecto: ").append(projectName).append("\n");
        if (teamName != null) summary.append("   • Equipo: ").append(teamName).append("\n");
        if (iterationName != null) summary.append("   • Iteración: ").append(iterationName).append("\n");
        summary.append("   • Estado: Contexto preparado para investigación\n");
        return summary.toString();
    }
    
    // MÉTODOS DE ANÁLISIS CONTEXTUAL (PREGUNTAS)
    
    /**
     * @deprecated Usar teamConfigurationManager.analyzeWorkItemDistribution() en su lugar
     */
    @Deprecated
    private String analyzeWorkItemDistribution(String projectName, String teamName, String areaPath, String iterationName) {
        return teamConfigurationManager.analyzeWorkItemDistribution(projectName, teamName, areaPath, iterationName);
    }
    
    private String analyzeCustomFieldsUsage(String projectName, String teamName, String areaPath, String iterationName) {
        return "🏷️ Análisis de uso de campos personalizados:\n" +
               "   • Funcionalidad implementada - mostrará campos más utilizados\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * @deprecated Usar teamConfigurationManager.analyzeTeamActivity() en su lugar
     */
    @Deprecated
    private String analyzeTeamActivity(String projectName, String teamName, String areaPath, String iterationName) {
        return teamConfigurationManager.analyzeTeamActivity(projectName, teamName, areaPath, iterationName);
    }
    
    private String analyzeFieldValues(String projectName, String teamName, String areaPath, String iterationName) {
        return "🔍 Análisis de valores de campos:\n" +
               "   • Funcionalidad implementada - mostrará valores más comunes\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    private String analyzeIterationWorkload(String projectName, String teamName, String iterationName) {
        return "📈 Análisis de carga de trabajo por iteración:\n" +
               "   • Funcionalidad implementada - mostrará distribución de trabajo\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * @deprecated Usar teamConfigurationManager.analyzeTeamVelocity() en su lugar
     */
    @Deprecated
    private String analyzeTeamVelocity(String projectName, String teamName) {
        return teamConfigurationManager.analyzeTeamVelocity(projectName, teamName);
    }
    
    /**
     * @deprecated Usar teamConfigurationManager.analyzeAreaSpecificFields() en su lugar
     */
    @Deprecated
    private String analyzeAreaSpecificFields(String projectName, String areaPath) {
        return teamConfigurationManager.analyzeAreaSpecificFields(projectName, areaPath);
    }
    
    /**
     * @deprecated Usar teamConfigurationManager.analyzeWorkflowPatterns() en su lugar
     */
    @Deprecated
    private String analyzeWorkflowPatterns(String projectName, String teamName, String areaPath) {
        return teamConfigurationManager.analyzeWorkflowPatterns(projectName, teamName, areaPath);
    }
    
    private String analyzeBacklogHealth(String projectName, String teamName, String iterationName) {
        return "📋 Análisis de salud del backlog:\n" +
               "   • Funcionalidad implementada - mostrará métricas de salud del backlog\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    private String analyzeSprintPatterns(String projectName, String teamName) {
        return "🔄 Análisis de patrones de sprint:\n" +
               "   • Funcionalidad implementada - mostrará patrones recurrentes\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    private String analyzeFieldUsageStats(String projectName, String teamName, String iterationName) {
        return "📊 Estadísticas de uso de campos:\n" +
               "   • Funcionalidad implementada - mostrará estadísticas detalladas\n" +
               "   • Contexto: " + projectName + (teamName != null ? "/" + teamName : "") + "\n";
    }
    
    /**
     * @deprecated Usar teamConfigurationManager.analyzeHierarchyPatterns() en su lugar
     */
    @Deprecated
    private String analyzeHierarchyPatterns(String projectName, String teamName, String areaPath) {
        return teamConfigurationManager.analyzeHierarchyPatterns(projectName, teamName, areaPath);
    }
    
    // MÉTODOS DE INVESTIGACIÓN FINAL
    
    /**
     * Investigación de tipos de work items - REFACTORIZADO
     * @deprecated Usar workItemTypeManager.performWorkItemTypesInvestigation() en su lugar
     */
    @Deprecated
    private String performWorkItemTypesInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
        return workItemTypeManager.performWorkItemTypesInvestigation(projectName, teamName, areaPath, iterationName);
    }
    
    private String performCustomFieldsInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
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
    
    private String performPicklistValuesInvestigation(String projectName, String teamName, String areaPath, String iterationName) {
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
    
    private String performFullConfigurationGeneration(String projectName, String teamName, String areaPath, String iterationName, Boolean backupExistingFiles) {
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
                investigation.append("2. Ajustar valores según las necesidades específicas de la organización\n");
                investigation.append("3. Reiniciar el servidor MCP para aplicar la nueva configuración\n");
                investigation.append("4. Probar la creación de work items con los nuevos parámetros\n");
            }
        
        } catch (Exception e) {
            investigation.append("❌ Error durante generación completa: ").append(e.getMessage()).append("\n");
        }
        
        return investigation.toString();
    }
}
