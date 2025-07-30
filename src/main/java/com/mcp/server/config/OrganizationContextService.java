package com.mcp.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para cargar y proporcionar contexto organizacional dinámico.
 * Lee archivos de configuración YAML para adaptar queries WIQL según la organización.
 */
@Service
public class OrganizationContextService {
    
    @Value("${app.config.path:config/}")
    private String configPath;
    
    private final ObjectMapper yamlMapper;
    private Map<String, Object> discoveredConfig;
    private Map<String, Object> fieldMappingConfig;
    private boolean configLoaded = false;
    
    public OrganizationContextService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * Carga la configuración organizacional desde archivos YAML.
     */
    private void loadConfiguration() {
        if (configLoaded) {
            return;
        }
        
        try {
            // Cargar configuración descubierta
            File discoveredFile = new File(configPath + "discovered-organization.yml");
            if (discoveredFile.exists()) {
                discoveredConfig = yamlMapper.readValue(discoveredFile, Map.class);
            } else {
                discoveredConfig = new HashMap<>();
            }
            
            // Cargar mapeo de campos
            File fieldMappingFile = new File(configPath + "sura-field-mapping.yml");
            if (fieldMappingFile.exists()) {
                fieldMappingConfig = yamlMapper.readValue(fieldMappingFile, Map.class);
            } else {
                fieldMappingConfig = new HashMap<>();
            }
            
            configLoaded = true;
        } catch (IOException e) {
            System.err.println("Error loading organization configuration: " + e.getMessage());
            discoveredConfig = new HashMap<>();
            fieldMappingConfig = new HashMap<>();
            configLoaded = true;
        }
    }
    
    /**
     * Obtiene los campos estándar del sistema que deben incluirse en queries WIQL.
     */
    public List<String> getStandardSystemFields() {
        loadConfiguration();
        
        List<String> standardFields = new ArrayList<>();
        standardFields.add("System.Id");
        standardFields.add("System.Title");
        standardFields.add("System.State");
        standardFields.add("System.WorkItemType");
        standardFields.add("System.AssignedTo");
        standardFields.add("System.AreaPath");
        standardFields.add("System.IterationPath");
        standardFields.add("System.CreatedDate");
        standardFields.add("System.ChangedDate");
        
        return standardFields;
    }
    
    /**
     * Obtiene los campos de fechas de planificación que deben incluirse en queries.
     * Versión conservadora que solo incluye campos estándar conocidos.
     */
    public List<String> getSchedulingDateFields() {
        loadConfiguration();
        
        List<String> dateFields = new ArrayList<>();
        
        // Solo campos estándar de fechas de Azure DevOps que se sabe que existen
        dateFields.add("Microsoft.VSTS.Scheduling.TargetDate");
        dateFields.add("Microsoft.VSTS.Scheduling.DueDate");
        
        // Nota: StartDate y FinishDate pueden no existir en todas las configuraciones
        // Se pueden agregar después de validación
        
        return dateFields;
    }
    
    /**
     * Obtiene los campos de métricas y estimación que deben incluirse en queries.
     * Versión conservadora que solo incluye campos estándar conocidos.
     */
    public List<String> getSchedulingMetricFields() {
        loadConfiguration();
        
        List<String> metricFields = new ArrayList<>();
        metricFields.add("Microsoft.VSTS.Scheduling.RemainingWork");
        metricFields.add("Microsoft.VSTS.Scheduling.StoryPoints");
        metricFields.add("Microsoft.VSTS.Common.Priority");
        
        // Campos que pueden no existir en todas las configuraciones:
        // - Microsoft.VSTS.Scheduling.OriginalEstimate
        // - Microsoft.VSTS.Scheduling.CompletedWork
        
        return metricFields;
    }
    
    /**
     * Obtiene campos personalizados relevantes según el tipo de work item.
     */
    public List<String> getCustomFieldsForWorkItemType(String workItemType) {
        loadConfiguration();
        
        List<String> customFields = new ArrayList<>();
        
        if (fieldMappingConfig.containsKey("customFields")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> customFieldsConfig = (Map<String, Object>) fieldMappingConfig.get("customFields");
            
            // Buscar campos específicos del tipo de work item
            String workItemKey = normalizeWorkItemType(workItemType);
            if (customFieldsConfig.containsKey(workItemKey)) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> typeFields = (List<Map<String, Object>>) customFieldsConfig.get(workItemKey);
                
                customFields.addAll(typeFields.stream()
                    .map(field -> (String) field.get("referenceName"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            }
        }
        
        return customFields;
    }
    
    /**
     * Construye una lista completa de campos para incluir en consultas WIQL
     * basándose en el contexto organizacional y tipo de work item.
     */
    public List<String> buildContextualFieldList(String workItemType, boolean includeDates, boolean includeMetrics, boolean includeCustomFields) {
        loadConfiguration();
        
        List<String> fields = new ArrayList<>();
        
        // Siempre incluir campos estándar
        fields.addAll(getStandardSystemFields());
        
        // Incluir campos de fecha si se solicita
        if (includeDates) {
            fields.addAll(getSchedulingDateFields());
        }
        
        // Incluir métricas si se solicita
        if (includeMetrics) {
            fields.addAll(getSchedulingMetricFields());
        }
        
        // Incluir campos personalizados si se solicita
        if (includeCustomFields && workItemType != null) {
            fields.addAll(getCustomFieldsForWorkItemType(workItemType));
        }
        
        // Remover duplicados y devolver
        return fields.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * Construye la cláusula SELECT de una consulta WIQL con campos contextuales.
     * Versión segura que valida campos progresivamente.
     */
    public String buildWiqlSelectClause(String workItemType, boolean includeDates, boolean includeMetrics, boolean includeCustomFields) {
        List<String> fields = buildContextualFieldList(workItemType, includeDates, includeMetrics, includeCustomFields);
        
        return "SELECT " + fields.stream()
            .map(field -> "[" + field + "]")
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Construye una cláusula SELECT conservadora solo con campos básicos garantizados.
     */
    public String buildBasicWiqlSelectClause() {
        List<String> basicFields = new ArrayList<>();
        basicFields.add("System.Id");
        basicFields.add("System.Title");
        basicFields.add("System.State");
        basicFields.add("System.WorkItemType");
        basicFields.add("System.AssignedTo");
        basicFields.add("System.AreaPath");
        basicFields.add("System.IterationPath");
        basicFields.add("System.CreatedDate");
        basicFields.add("System.ChangedDate");
        
        return "SELECT " + basicFields.stream()
            .map(field -> "[" + field + "]")
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Construye una cláusula SELECT con campos de fecha solamente si se solicita.
     */
    public String buildWiqlSelectClauseWithDates() {
        List<String> fields = new ArrayList<>();
        fields.addAll(getStandardSystemFields());
        fields.addAll(getSchedulingDateFields());
        
        return "SELECT " + fields.stream()
            .distinct()
            .map(field -> "[" + field + "]")
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Obtiene campos requeridos específicos de la organización para un tipo de work item.
     */
    public List<String> getRequiredFieldsFromConfig(String workItemType) {
        loadConfiguration();
        
        if (fieldMappingConfig.containsKey("requiredFieldsByType")) {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> requiredFields = (Map<String, List<String>>) fieldMappingConfig.get("requiredFieldsByType");
            
            return requiredFields.getOrDefault(workItemType, Collections.emptyList());
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Obtiene la configuración de proyectos descubiertos.
     */
    public List<Map<String, Object>> getDiscoveredProjects() {
        loadConfiguration();
        
        if (discoveredConfig.containsKey("projects")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projects = (List<Map<String, Object>>) discoveredConfig.get("projects");
            return projects;
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Obtiene información de equipos para un proyecto específico.
     */
    public List<Map<String, Object>> getTeamsForProject(String projectName) {
        loadConfiguration();
        
        return getDiscoveredProjects().stream()
            .filter(project -> projectName.equals(project.get("name")))
            .findFirst()
            .map(project -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> teams = (List<Map<String, Object>>) project.get("teams");
                return teams != null ? teams : Collections.<Map<String, Object>>emptyList();
            })
            .orElse(Collections.emptyList());
    }
    
    /**
     * Normaliza el nombre del tipo de work item para búsqueda en configuración.
     */
    private String normalizeWorkItemType(String workItemType) {
        if (workItemType == null) {
            return "";
        }
        
        String normalized = workItemType.toLowerCase().trim();
        
        // Mapeos comunes
        switch (normalized) {
            case "user story":
            case "historia de usuario":
                return "historia";
            case "historia técnica":
            case "technical story":
                return "historiaTecnica";
            case "task":
                return "tarea";
            case "subtask":
                return "subtarea";
            default:
                return normalized;
        }
    }
    
    /**
     * Verifica si la configuración está cargada correctamente.
     */
    public boolean isConfigurationLoaded() {
        loadConfiguration();
        return configLoaded && (discoveredConfig != null || fieldMappingConfig != null);
    }
    
    /**
     * Recarga la configuración desde archivos (útil para desarrollo/testing).
     */
    public void reloadConfiguration() {
        configLoaded = false;
        loadConfiguration();
    }
}
