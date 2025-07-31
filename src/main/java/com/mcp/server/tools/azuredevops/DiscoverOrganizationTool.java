package com.mcp.server.tools.azuredevops;

import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Herramienta MCP para descubrir y analizar la configuración de Azure DevOps de una organización.
 * Genera información sobre proyectos, equipos, tipos de work items y campos disponibles.
 * Incluye funcionalidad avanzada de investigación de campos personalizados y valores de picklist.
 */
@Component
public class DiscoverOrganizationTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationConfigService configService;
    private final HttpClient httpClient;
    private final GetWorkItemTypesTool getWorkItemTypesTool;
    
    @Autowired
    public DiscoverOrganizationTool(AzureDevOpsClient azureDevOpsClient, OrganizationConfigService configService, 
                                   GetWorkItemTypesTool getWorkItemTypesTool) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configService = configService;
        this.getWorkItemTypesTool = getWorkItemTypesTool;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public String getName() {
        return "azuredevops_discover_organization";
    }
    
    @Override
    public String getDescription() {
        return "Descubre y analiza la configuración de Azure DevOps de una organización, incluyendo proyectos, equipos, tipos de work items y campos disponibles.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        
        properties.put("project", Map.of(
            "type", "string",
            "description", "Nombre del proyecto de Azure DevOps para analizar (opcional, analiza toda la organización si se omite)"
        ));
        
        properties.put("backupExistingFiles", Map.of(
            "type", "boolean",
            "description", "Si hacer backup de archivos de configuración existentes antes de generar nuevos (por defecto: true)"
        ));
        
        properties.put("workItemReferencia", Map.of(
            "type", "string",
            "description", "URL completa o ID del work item de referencia del usuario para orientar la búsqueda hacia el área path correspondiente (ej: 'https://dev.azure.com/org/project/_workitems/edit/12345' o '12345')"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            String project = (String) arguments.get("project");
            Boolean backupExistingFiles = (Boolean) arguments.getOrDefault("backupExistingFiles", true);
            String workItemReferencia = (String) arguments.get("workItemReferencia");
            
            // NUEVO: Procesar work item de referencia para orientar la búsqueda
            String projectoReferencia = null;
            String areaPathReferencia = null;
            String equipoReferencia = null;
            
            if (workItemReferencia != null && !workItemReferencia.trim().isEmpty()) {
                try {
                    // Extraer información del work item de referencia
                    Map<String, Object> referenciaInfo = procesarWorkItemReferencia(workItemReferencia);
                    if (referenciaInfo != null) {
                        projectoReferencia = (String) referenciaInfo.get("project");
                        areaPathReferencia = (String) referenciaInfo.get("areaPath");
                        equipoReferencia = (String) referenciaInfo.get("team");
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando work item de referencia " + workItemReferencia + ": " + e.getMessage());
                }
            }
            
            // NUEVO: Siempre usar investigación detallada completa por defecto
            boolean includeWorkItemTypes = true;         // Siempre incluir tipos
            boolean includeFields = true;                // Siempre incluir campos
            boolean generateConfig = true;               // Siempre generar configuración
            boolean investigateCustomFields = true;      // Siempre investigar campos personalizados
            boolean exhaustiveTypeDiscovery = true;      // Siempre hacer descubrimiento exhaustivo
            
            StringBuilder result = new StringBuilder();
            result.append("🔍 ANÁLISIS COMPLETO DE AZURE DEVOPS\n");
            result.append("=====================================\n");
            result.append("📋 Modo: Investigación Detallada Completa (por defecto)\n");
            result.append("🔬 Enfoque: Documentación exhaustiva de campos y valores\n");
            
            if (workItemReferencia != null && !workItemReferencia.trim().isEmpty()) {
                result.append("🎯 Work Item Referencia: ").append(workItemReferencia).append("\n");
                if (projectoReferencia != null) {
                    result.append("   📁 Proyecto orientado: ").append(projectoReferencia).append("\n");
                }
                if (areaPathReferencia != null) {
                    result.append("   🗂️ Área path orientada: ").append(areaPathReferencia).append("\n");
                }
                if (equipoReferencia != null) {
                    result.append("   👥 Equipo orientado: ").append(equipoReferencia).append("\n");
                }
            }
            result.append("\n");
            
            // Información básica de la organización
            Map<String, Object> orgConfig = configService.getDefaultOrganizationConfig();
            result.append("🏢 Organización: ").append(orgConfig.get("organization")).append("\n");
            
            // Priorizar el proyecto de referencia si está disponible
            if (project == null && projectoReferencia != null) {
                project = projectoReferencia;
                result.append("📁 Proyecto (orientado por referencia): ").append(project).append("\n");
            } else if (project != null) {
                result.append("📁 Proyecto: ").append(project).append("\n");
            }
            result.append("\n");
            
            // PASO 1: Descubrimiento exhaustivo de TODOS los proyectos y tipos
            result.append("🔍 **FASE 1: DESCUBRIMIENTO EXHAUSTIVO**\n");
            result.append("=======================================\n");
            result.append(performExhaustiveWorkItemTypeDiscovery());
            result.append("\n");
            
            // PASO 2: Análisis detallado del proyecto específico (si se proporciona)
            if (project != null) {
                result.append("🔍 **FASE 2: ANÁLISIS DETALLADO DEL PROYECTO**\n");
                result.append("============================================\n");
                result.append(analyzeWorkItemTypesWithCompleteFieldDocumentation(project));
                result.append("\n");
            } else {
                // Si no se especifica proyecto, mostrar análisis general
                result.append("🔍 **FASE 2: ANÁLISIS GENERAL DE PROYECTOS**\n");
                result.append("==========================================\n");
                result.append(analyzeProjects());
            }
            
            // PASO 3: Generación de configuración sugerida
            result.append("🔍 **FASE 3: CONFIGURACIÓN SUGERIDA**\n");
            result.append("===================================\n");
            result.append(generateSuggestedConfiguration(project));
            result.append("\n");
            
            // PASO 4: Recomendaciones finales
            result.append("🔍 **FASE 4: RECOMENDACIONES Y PRÓXIMOS PASOS**\n");
            result.append("==============================================\n");
            result.append(getConfigurationRecommendations());
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "❌ Error durante el descubrimiento: " + e.getMessage()
                )),
                "isError", true
            );
        }
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
                    getOrganizationFromConfig());
            
            String response = makeDirectApiRequest(url);
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
    
    private String analyzeWorkItemTypes(String project) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📋 Tipos de Work Items\n");
        analysis.append("----------------------\n");
        
        try {
            // Obtener tipos de work items disponibles dinámicamente
            List<String> availableTypes = getAvailableWorkItemTypes(project);
            
            analysis.append("Tipos encontrados en el proyecto: ").append(availableTypes.size()).append("\n\n");
            
            for (String type : availableTypes) {
                analysis.append("• ").append(type);
                
                // Mostrar campos requeridos para este tipo si están configurados
                try {
                    List<String> requiredFields = configService.getRequiredFieldsForWorkItemType(type);
                    if (!requiredFields.isEmpty()) {
                        analysis.append(" (Campos requeridos: ");
                        analysis.append(String.join(", ", requiredFields));
                        analysis.append(")");
                    }
                } catch (Exception e) {
                    // Si no hay configuración para este tipo, continuar
                }
                analysis.append("\n");
                
                // NUEVO: Documentar valores válidos para campos obligatorios
                analysis.append(documentValidValuesForRequiredFields(project, type));
            }
            
            analysis.append("\n");
            
            // NUEVO: Agregar análisis jerárquico específico para el proyecto
            analysis.append("🏗️ **ANÁLISIS JERÁRQUICO**\n");
            analysis.append("========================\n");
            
            try {
                Map<String, Object> hierarchyData = analyzeWorkItemHierarchies(project);
                
                if (hierarchyData.containsKey("error")) {
                    analysis.append("⚠️ ").append(hierarchyData.get("error")).append("\n");
                } else if (hierarchyData.containsKey("message")) {
                    analysis.append("ℹ️ ").append(hierarchyData.get("message")).append("\n");
                } else {
                    // Mostrar estadísticas del análisis jerárquico
                    if (hierarchyData.containsKey("totalChildWorkItems")) {
                        int totalChildren = (Integer) hierarchyData.get("totalChildWorkItems");
                        analysis.append("📊 **Total work items hijos encontrados:** ").append(totalChildren).append("\n\n");
                    }
                    
                    if (hierarchyData.containsKey("parentChildRelations")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Set<String>> relations = (Map<String, Set<String>>) hierarchyData.get("parentChildRelations");
                        
                        if (!relations.isEmpty()) {
                            analysis.append("🔗 **Relaciones Padre-Hijo detectadas:**\n");
                            for (Map.Entry<String, Set<String>> relation : relations.entrySet()) {
                                analysis.append("   • **").append(relation.getKey()).append("** puede tener hijos de tipo:\n");
                                for (String childType : relation.getValue()) {
                                    analysis.append("     - ").append(childType).append("\n");
                                }
                            }
                            analysis.append("\n");
                        }
                    }
                    
                    if (hierarchyData.containsKey("mostCommonChildTypes")) {
                        @SuppressWarnings("unchecked")
                        List<Map.Entry<String, Integer>> commonTypes = (List<Map.Entry<String, Integer>>) hierarchyData.get("mostCommonChildTypes");
                        
                        if (!commonTypes.isEmpty()) {
                            analysis.append("📈 **Tipos de subtareas más utilizados:**\n");
                            for (Map.Entry<String, Integer> entry : commonTypes) {
                                analysis.append("   • **").append(entry.getKey()).append("**: ").append(entry.getValue()).append(" instancias\n");
                            }
                            analysis.append("\n");
                        }
                    }
                    
                    if (hierarchyData.containsKey("detailedStatistics")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Integer>> detailedStats = (Map<String, Map<String, Integer>>) hierarchyData.get("detailedStatistics");
                        
                        if (!detailedStats.isEmpty()) {
                            analysis.append("🎯 **Estadísticas detalladas por tipo padre:**\n");
                            for (Map.Entry<String, Map<String, Integer>> parentEntry : detailedStats.entrySet()) {
                                String parentType = parentEntry.getKey();
                                Map<String, Integer> childStats = parentEntry.getValue();
                                
                                analysis.append("   📁 **").append(parentType).append(":**\n");
                                for (Map.Entry<String, Integer> childEntry : childStats.entrySet()) {
                                    analysis.append("     - ").append(childEntry.getKey()).append(": ").append(childEntry.getValue()).append(" casos\n");
                                }
                            }
                            analysis.append("\n");
                        }
                    }
                }
                
                analysis.append("💡 **Interpretación de Resultados:**\n");
                analysis.append("• Los tipos listados arriba representan los patrones de subtareas reales utilizados en su organización\n");
                analysis.append("• Esta información es crucial para configurar correctamente los campos 'TipoSubtarea' en su organización\n");
                analysis.append("• Los tipos con mayor número de instancias son los más adoptados por los equipos\n\n");
                
            } catch (Exception e) {
                analysis.append("❌ Error en análisis jerárquico: ").append(e.getMessage()).append("\n\n");
            }
            
        } catch (Exception e) {
            analysis.append("❌ Error obteniendo tipos de work items: ").append(e.getMessage()).append("\n\n");
        }
        
        return analysis.toString();
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
    private List<String> getAvailableWorkItemTypes(String project) {
        List<String> workItemTypes = new ArrayList<>();
        
        try {
            System.out.println("🔍 Obteniendo tipos de work items usando GetWorkItemTypesTool...");
            
            // Usar GetWorkItemTypesTool que ya sabemos que funciona perfectamente
            Map<String, Object> arguments = Map.of(
                "project", project,
                "includeExtendedInfo", false  // Solo necesitamos los nombres
            );
            
            Map<String, Object> result = getWorkItemTypesTool.execute(arguments);
            
            if (result.containsKey("content")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                if (!content.isEmpty() && content.get(0).containsKey("text")) {
                    String response = (String) content.get(0).get("text");
                    
                    // Extraer nombres de tipos de la respuesta
                    List<String> extractedTypes = extractWorkItemTypeNames(response);
                    
                    // NUEVO: Aplicar validación mejorada para garantizar detección completa
                    Set<String> validatedTypes = validateAndEnhanceTypeDetection(response, new HashSet<>(extractedTypes));
                    workItemTypes = new ArrayList<>(validatedTypes);
                    
                    System.out.println("✅ Encontrados " + workItemTypes.size() + " tipos de work items");
                    System.out.println("📋 Tipos finales: " + String.join(", ", workItemTypes));
                }
            }
            
            if (workItemTypes.isEmpty()) {
                System.err.println("⚠️ No se pudieron obtener tipos de work items del proyecto: " + project);
                System.err.println("   Intentando método de fallback...");
                
                // NUEVO: método de fallback - consulta directa a la API
                workItemTypes = getWorkItemTypesDirectFromApi(project);
                
                if (workItemTypes.isEmpty()) {
                    System.err.println("   ❌ Método de fallback también falló");
                    System.err.println("   Verifique la conectividad y permisos de acceso al proyecto");
                } else {
                    System.out.println("   ✅ Método de fallback exitoso: " + workItemTypes.size() + " tipos encontrados");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo tipos de work items: " + e.getMessage());
            e.printStackTrace();
        }
        
        return workItemTypes;
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
        
        // Extraer de referencias personalizadas como "AgileSura.Historia" -> "Historia"
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
                    getOrganizationFromConfig(), project);
            
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
                    getOrganizationFromConfig(), project, encodedTypeName);
            
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
                    getOrganizationFromConfig(), project, encodedTypeName);
            
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
        List<String> validFields = new ArrayList<>();
        
        for (String field : candidateFields) {
            if (field != null && !field.trim().isEmpty() && isValidCustomField(field)) {
                // Solo incluir campos que parezcan válidos (Custom.GUID o Custom.Nombre)
                if (field.startsWith("Custom.") && 
                    (field.matches("Custom\\.[a-f0-9-]{36}") || field.matches("Custom\\.[A-Za-z][A-Za-z0-9]*"))) {
                    validFields.add(field.trim());
                }
            }
        }
        
        System.out.println("Campos validados para " + workItemType + ": " + validFields.size() + "/" + candidateFields.size());
        return validFields;
    }
    
    /**
     * Verifica si un campo personalizado tiene formato válido y no causa errores conocidos
     */
    private boolean isValidCustomField(String fieldName) {
        if (fieldName == null || !fieldName.startsWith("Custom.")) {
            return false;
        }
        
        // Validar formato básico (debe ser Custom.GUID o Custom.NombreValido)
        if (!fieldName.matches("Custom\\.[a-f0-9-]{36}") && 
            !fieldName.matches("Custom\\.[A-Za-z][A-Za-z0-9_]*")) {
            System.out.println("⚠️ Campo con formato inválido: " + fieldName);
            return false;
        }
        
        // Lista dinámica de campos problemáticos se puede expandir según errores encontrados
        // Estos campos se excluyen porque causan errores específicos de la API
        Set<String> knownProblematicFields = getKnownProblematicFields();
        
        if (knownProblematicFields.contains(fieldName)) {
            System.out.println("⚠️ Excluyendo campo problemático conocido: " + fieldName);
            return false;
        }
        
        return true;
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
        List<Integer> workItemIds = new ArrayList<>();
        
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/wiql?api-version=7.1", 
                    getOrganizationFromConfig(), project);
            
            // Reemplazar parámetros genéricos con valores específicos
            String processedQuery = processWIQLQueryParameters(wiqlQuery, project);
            
            // Crear el payload JSON para la consulta WIQL con límite explícito
            String limitedQuery = addLimitToWIQLQuery(processedQuery, 200); // Límite seguro muy por debajo de 20000
            String payload = String.format("{\"query\":\"%s\"}", limitedQuery.replace("\"", "\\\""));
            
            System.out.println("Ejecutando consulta WIQL: " + limitedQuery);
            
            String response = makeWIQLApiRequest(url, payload);
            if (response != null) {
                workItemIds = parseWorkItemIdsFromWIQLResponse(response);
                System.out.println("WIQL query retornó " + workItemIds.size() + " work items");
            }
            
        } catch (Exception e) {
            System.err.println("Error executing WIQL query: " + e.getMessage());
        }
        
        return workItemIds;
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
     * Hace una petición POST para WIQL con manejo robusto de errores
     */
    private String makeWIQLApiRequest(String url, String payload) {
        try {
            String pat = System.getenv("AZURE_DEVOPS_PAT");
            if (pat == null || pat.isEmpty()) {
                System.err.println("Azure DevOps PAT no configurado");
                return null;
            }
            
            String auth = Base64.getEncoder().encodeToString((":" + pat).getBytes());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                String errorBody = response.body();
                
                // Identificar y manejar errores específicos
                if (errorBody.contains("VS402337") || errorBody.contains("exceeds the size limit")) {
                    System.err.println("❌ ERROR: La consulta WIQL excede el límite de 20,000 work items");
                    System.err.println("   Solución: La consulta será automáticamente limitada a 200 items");
                    return null;
                } else if (errorBody.contains("TF51005") || errorBody.contains("field that does not exist")) {
                    System.err.println("❌ ERROR: Campo referenciado no existe en el proyecto");
                    System.err.println("   Detalle: " + errorBody);
                    return null;
                } else {
                    System.err.println("❌ WIQL query failed with status: " + response.statusCode() + " - " + errorBody);
                    return null;
                }
            }
            
            return response.body();
            
        } catch (Exception e) {
            System.err.println("Error making WIQL API request: " + e.getMessage());
            return null;
        }
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
                        getOrganizationFromConfig(), project, idsParam, fieldsParam);
                
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
     */
    private List<Map<String, Object>> getAllProjectFields(String project) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields?api-version=7.1", 
                    getOrganizationFromConfig(), project);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                fields = parseProjectFields(response);
            }
            
        } catch (Exception e) {
            System.err.println("Error obteniendo campos del proyecto: " + e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * Obtiene valores de picklist usando múltiples estrategias de endpoints
     */
    private List<String> getPicklistValues(String project, String fieldReferenceName, String picklistId) {
        // Estrategia 1: Endpoint de procesos organizacionales
        List<String> values = tryGetPicklistFromProcesses(picklistId);
        if (!values.isEmpty()) return values;
        
        // Estrategia 2: Endpoint de procesos con contexto de proyecto
        values = tryGetPicklistFromProjectContext(project, picklistId);
        if (!values.isEmpty()) return values;
        
        // Estrategia 3: Endpoint específico de campo
        values = tryGetPicklistFromFieldEndpoint(project, fieldReferenceName);
        if (!values.isEmpty()) return values;
        
        return Collections.emptyList();
    }
    
    // Métodos auxiliares para investigación avanzada
    
    private String makeDirectApiRequest(String url) {
        try {
            String pat = System.getenv("AZURE_DEVOPS_PAT");
            if (pat == null || pat.isEmpty()) {
                return null;
            }
            
            String auth = Base64.getEncoder().encodeToString((":" + pat).getBytes());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return null;
            }
            
            return response.body();
            
        } catch (Exception e) {
            System.err.println("Error making API request to " + url + ": " + e.getMessage());
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
                String picklistId = extractJsonValue(fieldBlock, "picklistId");
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
    
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    private List<String> tryGetPicklistFromProcesses(String picklistId) {
        try {
            String url = String.format("https://dev.azure.com/%s/_apis/work/processes/lists/%s?api-version=7.1", 
                    getOrganizationFromConfig(), picklistId);
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromProjectContext(String project, String picklistId) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/work/processes/lists/%s?api-version=7.1", 
                    getOrganizationFromConfig(), project, picklistId);
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"items\"")) {
                return extractArrayValues(response, "items");
            }
        } catch (Exception e) {
            // Continuar silenciosamente a la siguiente estrategia
        }
        return Collections.emptyList();
    }
    
    private List<String> tryGetPicklistFromFieldEndpoint(String project, String fieldReferenceName) {
        try {
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/fields/%s/allowedValues?api-version=7.1", 
                    getOrganizationFromConfig(), project, fieldReferenceName);
            
            String response = makeDirectApiRequest(url);
            if (response != null && response.contains("\"value\"")) {
                return extractArrayValues(response, "value");
            }
        } catch (Exception e) {
            // Continuar silenciosamente
        }
        return Collections.emptyList();
    }
    
    private List<String> extractArrayValues(String json, String arrayKey) {
        List<String> values = new ArrayList<>();
        
        Pattern arrayPattern = Pattern.compile("\"" + arrayKey + "\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(json);
        
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Pattern valuePattern = Pattern.compile("\"([^\"]+)\"");
            Matcher valueMatcher = valuePattern.matcher(arrayContent);
            
            while (valueMatcher.find()) {
                values.add(valueMatcher.group(1));
            }
        }
        
        return values;
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
     * Obtiene un work item como JSON usando llamada REST directa
     * Método auxiliar para el análisis jerárquico
     */
    private String getWorkItemAsJson(String project, Integer workItemId) {
        try {
            // Intentar usar el cliente existente primero
            Map<String, Object> orgConfig = configService.getDefaultOrganizationConfig();
            String organization = (String) orgConfig.get("organization");
            
            if (organization == null) {
                organization = System.getenv("AZURE_DEVOPS_ORGANIZATION");
            }
            
            if (organization == null) {
                return null;
            }
            
            String url = String.format("https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?api-version=7.1", 
                                      organization, project, workItemId);
            
            // Obtener PAT de variables de entorno
            String pat = System.getenv("AZURE_DEVOPS_PAT");
            if (pat == null) {
                return null;
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + 
                           Base64.getEncoder().encodeToString((":" + pat).getBytes(StandardCharsets.UTF_8)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.out.println("Error obteniendo work item " + workItemId + ": HTTP " + response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            System.out.println("Error en llamada REST para work item " + workItemId + ": " + e.getMessage());
            return null;
        }
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
    
    private String getOrganizationFromConfig() {
        // Obtener de la variable de entorno directamente
        String org = System.getenv("AZURE_DEVOPS_ORGANIZATION");
        if (org != null && !org.isEmpty()) {
            return org;
        }
        
        // Fallback al config service
        Map<String, Object> config = configService.getDefaultOrganizationConfig();
        return (String) config.get("organization");
    }
    
    private String getPersonalAccessTokenFromConfig() {
        // Obtener de la variable de entorno directamente
        String pat = System.getenv("AZURE_DEVOPS_PAT");
        if (pat != null && !pat.isEmpty()) {
            return pat;
        }
        
        // En una implementación real, esto vendría de configuración segura
        // Por ahora devolvemos null para usar la configuración del AzureDevOpsClient
        return null;
    }
    
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
                    getOrganizationFromConfig(), project, azureFieldName.replace(".", "%2E"));
            
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
                    getOrganizationFromConfig(), project, encodedType);
            
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
                    getOrganizationFromConfig(), project, encodedType);
            
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
                    getOrganizationFromConfig(), project);
            
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
                    getOrganizationFromConfig(), project, encodedFieldName);
            
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
            String picklistId = extractJsonValue(jsonResponse, "picklistId");
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
                    getOrganizationFromConfig(), project, encodedFieldName);
            
            String response = makeDirectApiRequest(url);
            if (response != null) {
                // Buscar valores permitidos en la respuesta
                allowedValues = parseFieldAllowedValues(response);
                
                // Si no se encontraron valores directos, buscar en picklistId
                if (allowedValues.isEmpty()) {
                    String picklistId = extractJsonValue(response, "picklistId");
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
                    getOrganizationFromConfig(), project);
            
            String requestBody = String.format("{\"query\":\"%s\"}", wiqlQuery.replace("\"", "\\\""));
            String response = makePostRequest(queryUrl, requestBody);
            
            if (response != null) {
                List<Integer> workItemIds = extractWorkItemIdsFromWiqlResponse(response);
                
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
                    getOrganizationFromConfig(), project, workItemId, referenceName);
            
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
     * Realiza una petición POST HTTP
     */
    private String makePostRequest(String url, String requestBody) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            
            String pat = getPAT();
            String auth = Base64.getEncoder().encodeToString((":" + pat).getBytes());
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("POST request failed: " + response.statusCode() + " - " + response.body());
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error realizando petición POST: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrae los IDs de work items de una respuesta WIQL
     */
    private List<Integer> extractWorkItemIdsFromWiqlResponse(String jsonResponse) {
        List<Integer> workItemIds = new ArrayList<>();
        
        try {
            // Buscar sección "workItems" en la respuesta
            Pattern workItemsPattern = Pattern.compile("\"workItems\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher workItemsMatcher = workItemsPattern.matcher(jsonResponse);
            
            if (workItemsMatcher.find()) {
                String workItemsSection = workItemsMatcher.group(1);
                
                // Extraer cada ID individual
                Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
                Matcher idMatcher = idPattern.matcher(workItemsSection);
                
                while (idMatcher.find()) {
                    workItemIds.add(Integer.parseInt(idMatcher.group(1)));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extrayendo IDs de work items de respuesta WIQL: " + e.getMessage());
        }
        
        return workItemIds;
    }
    
    /**
     * Realiza una petición HTTP GET
     */
    private String makeHttpGetRequest(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            
            String pat = getPAT();
            String auth = Base64.getEncoder().encodeToString((":" + pat).getBytes());
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("GET request failed: " + response.statusCode() + " - " + response.body());
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error realizando petición GET: " + e.getMessage());
            return null;
        }
    }

    /**
     * Procesa un work item de referencia para extraer información de contexto organizacional
     */
    private Map<String, Object> procesarWorkItemReferencia(String workItemReferencia) {
        try {
            // Extraer ID del work item
            Integer workItemId = extractWorkItemIdFromReference(workItemReferencia);
            if (workItemId == null) {
                System.err.println("No se pudo extraer ID válido del work item de referencia: " + workItemReferencia);
                return null;
            }
            
            // Buscar el proyecto del work item - intentar con proyectos conocidos
            Map<String, Object> workItemDetails = findWorkItemAcrossProjects(workItemId);
            if (workItemDetails == null) {
                System.err.println("No se pudo encontrar el work item " + workItemId + " en ningún proyecto accesible");
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) workItemDetails.get("fields");
            
            if (fields != null) {
                String areaPath = (String) fields.get("System.AreaPath");
                String project = null;
                String team = null;
                
                // Extraer proyecto del área path
                if (areaPath != null && !areaPath.trim().isEmpty()) {
                    String[] pathParts = areaPath.split("\\\\");
                    if (pathParts.length > 0) {
                        project = pathParts[0];
                    }
                    
                    // El equipo puede estar en el segundo nivel del área path
                    if (pathParts.length > 1) {
                        team = pathParts[1];
                    }
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("workItemId", workItemId);
                result.put("project", project);
                result.put("areaPath", areaPath);
                result.put("team", team);
                result.put("iterationPath", fields.get("System.IterationPath"));
                result.put("workItemType", fields.get("System.WorkItemType"));
                
                System.out.println("Work item de referencia procesado exitosamente: ID=" + workItemId + 
                                 ", Proyecto=" + project + ", Área=" + areaPath);
                
                return result;
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando work item de referencia " + workItemReferencia + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extrae el ID del work item de una URL o texto de referencia
     */
    private Integer extractWorkItemIdFromReference(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return null;
        }
        
        reference = reference.trim();
        
        // Si es solo un número, devolverlo directamente
        try {
            return Integer.parseInt(reference);
        } catch (NumberFormatException e) {
            // No es un número simple, intentar extraer de URL
        }
        
        // Patrones para URLs de Azure DevOps
        String[] patterns = {
            "/_workitems/edit/(\\d+)",  // https://dev.azure.com/org/project/_workitems/edit/12345
            "/workitems/(\\d+)",        // https://dev.azure.com/org/project/_workitems/12345  
            "workItemId=(\\d+)",        // Query parameter
            "#(\\d+)"                   // Referencia simple como #12345
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(reference);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException e) {
                    // Continuar con el siguiente patrón
                }
            }
        }
        
        return null;
    }
    
    /**
     * Busca un work item específico a través de múltiples proyectos
     */
    private Map<String, Object> findWorkItemAcrossProjects(Integer workItemId) {
        try {
            // Primero, intentar obtener la lista de proyectos
            String organization = getOrganizationFromConfig();
            String url = String.format("https://dev.azure.com/%s/_apis/projects?api-version=6.0", organization);
            String projectsResponse = makeHttpGetRequest(url);
            
            if (projectsResponse != null) {
                // Extraer nombres de proyectos de la respuesta
                List<String> projectNames = extractProjectNames(projectsResponse);
                
                // Intentar encontrar el work item en cada proyecto
                for (String projectName : projectNames) {
                    try {
                        String workItemUrl = String.format(
                            "https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?api-version=6.0",
                            organization, projectName, workItemId
                        );
                        
                        String workItemResponse = makeHttpGetRequest(workItemUrl);
                        if (workItemResponse != null && !workItemResponse.contains("does not exist")) {
                            // Parsear respuesta JSON básica
                            Map<String, Object> workItem = parseWorkItemResponse(workItemResponse);
                            if (workItem != null) {
                                return workItem;
                            }
                        }
                    } catch (Exception e) {
                        // Continuar con el siguiente proyecto
                        System.out.println("Work item " + workItemId + " no encontrado en proyecto " + projectName + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error buscando work item " + workItemId + " a través de proyectos: " + e.getMessage());
        }
        
        return null;
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
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Extraer campos usando regex (parsing básico)
            Pattern fieldsPattern = Pattern.compile("\"fields\"\\s*:\\s*\\{([^}]+(?:\\{[^}]*\\}[^}]*)*)\\}");
            Matcher fieldsMatcher = fieldsPattern.matcher(jsonResponse);
            
            if (fieldsMatcher.find()) {
                String fieldsSection = fieldsMatcher.group(1);
                Map<String, Object> fields = parseFieldsSection(fieldsSection);
                result.put("fields", fields);
                return result;
            }
        } catch (Exception e) {
            System.err.println("Error parseando respuesta de work item: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Parsea la sección de campos del JSON
     */
    private Map<String, Object> parseFieldsSection(String fieldsSection) {
        Map<String, Object> fields = new HashMap<>();
        
        try {
            // Patrones para diferentes tipos de campos
            Pattern stringFieldPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*?)\"");
            Matcher stringMatcher = stringFieldPattern.matcher(fieldsSection);
            
            while (stringMatcher.find()) {
                String fieldName = stringMatcher.group(1);
                String fieldValue = stringMatcher.group(2);
                
                // Solo procesar campos del sistema relevantes
                if (fieldName.startsWith("System.")) {
                    fields.put(fieldName, fieldValue);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parseando sección de campos: " + e.getMessage());
        }
        
        return fields;
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
}
