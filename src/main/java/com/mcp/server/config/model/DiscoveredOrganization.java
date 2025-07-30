package com.mcp.server.config.model;

import java.util.*;

/**
 * Resultado del proceso de descubrimiento automático de la organización.
 */
public class DiscoveredOrganization {
    private OrganizationInfo organization;
    private List<Map<String, Object>> projects = new ArrayList<>();
    private Map<String, Object> workItemTypes = new HashMap<>();
    private Map<String, Object> customFields = new HashMap<>();
    private Map<String, Object> metadata = new HashMap<>();
    
    // Getters y setters
    public OrganizationInfo getOrganization() { return organization; }
    public void setOrganization(OrganizationInfo organization) { this.organization = organization; }
    
    public List<Map<String, Object>> getProjects() { return projects; }
    public void setProjects(List<Map<String, Object>> projects) { this.projects = projects; }
    
    public Map<String, Object> getWorkItemTypes() { return workItemTypes; }
    public void setWorkItemTypes(Map<String, Object> workItemTypes) { this.workItemTypes = workItemTypes; }
    
    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> customFields) { this.customFields = customFields; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

/**
 * Proyecto descubierto.
 */
class DiscoveredProject {
    private String id;
    private String name;
    private String description;
    private String state;
    private String visibility;
    private String lastUpdateTime;
    private String url;
    private List<DiscoveredTeam> teams = new ArrayList<>();
    private ProcessInfo process;
    private AreaStructure areaStructure;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    
    public String getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public List<DiscoveredTeam> getTeams() { return teams; }
    public void setTeams(List<DiscoveredTeam> teams) { this.teams = teams; }
    
    public ProcessInfo getProcess() { return process; }
    public void setProcess(ProcessInfo process) { this.process = process; }
    
    public AreaStructure getAreaStructure() { return areaStructure; }
    public void setAreaStructure(AreaStructure areaStructure) { this.areaStructure = areaStructure; }
}

/**
 * Equipo descubierto.
 */
class DiscoveredTeam {
    private String id;
    private String name;
    private String description;
    private String url;
    private String projectId;
    private TeamAnalysis analysis;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public TeamAnalysis getAnalysis() { return analysis; }
    public void setAnalysis(TeamAnalysis analysis) { this.analysis = analysis; }
}

/**
 * Análisis de equipo.
 */
class TeamAnalysis {
    private String detectedPrefix;
    private String category;
    private String description;
    
    public String getDetectedPrefix() { return detectedPrefix; }
    public void setDetectedPrefix(String detectedPrefix) { this.detectedPrefix = detectedPrefix; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

/**
 * Información del proceso.
 */
class ProcessInfo {
    private String id;
    private String name;
    private String description;
    private String type;
    private boolean isEnabled;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}

/**
 * Estructura de áreas.
 */
class AreaStructure {
    private String rootAreaPath;
    private List<AreaNode> children = new ArrayList<>();
    
    public String getRootAreaPath() { return rootAreaPath; }
    public void setRootAreaPath(String rootAreaPath) { this.rootAreaPath = rootAreaPath; }
    
    public List<AreaNode> getChildren() { return children; }
    public void setChildren(List<AreaNode> children) { this.children = children; }
}

/**
 * Nodo de área.
 */
class AreaNode {
    private int id;
    private String name;
    private String path;
    private boolean hasChildren;
    private List<AreaNode> children = new ArrayList<>();
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public boolean isHasChildren() { return hasChildren; }
    public void setHasChildren(boolean hasChildren) { this.hasChildren = hasChildren; }
    
    public List<AreaNode> getChildren() { return children; }
    public void setChildren(List<AreaNode> children) { this.children = children; }
}

/**
 * Tipos de work items descubiertos.
 */
class DiscoveredWorkItemTypes {
    private List<WorkItemTypeInfo> standard = new ArrayList<>();
    private List<WorkItemTypeInfo> custom = new ArrayList<>();
    
    public List<WorkItemTypeInfo> getStandard() { return standard; }
    public void setStandard(List<WorkItemTypeInfo> standard) { this.standard = standard; }
    
    public List<WorkItemTypeInfo> getCustom() { return custom; }
    public void setCustom(List<WorkItemTypeInfo> custom) { this.custom = custom; }
}

/**
 * Información de tipo de work item.
 */
class WorkItemTypeInfo {
    private String name;
    private String description;
    private String color;
    private String icon;
    private String url;
    private boolean isDisabled;
    private List<WorkItemField> fields = new ArrayList<>();
    private List<WorkItemState> states = new ArrayList<>();
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public boolean isDisabled() { return isDisabled; }
    public void setDisabled(boolean disabled) { isDisabled = disabled; }
    
    public List<WorkItemField> getFields() { return fields; }
    public void setFields(List<WorkItemField> fields) { this.fields = fields; }
    
    public List<WorkItemState> getStates() { return states; }
    public void setStates(List<WorkItemState> states) { this.states = states; }
}

/**
 * Campo de work item.
 */
class WorkItemField {
    private String name;
    private String referenceName;
    private String description;
    private String type;
    private boolean readOnly;
    private boolean required;
    private String helpText;
    private List<String> allowedValues = new ArrayList<>();
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getReferenceName() { return referenceName; }
    public void setReferenceName(String referenceName) { this.referenceName = referenceName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
    
    public List<String> getAllowedValues() { return allowedValues; }
    public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
}

/**
 * Estado de work item.
 */
class WorkItemState {
    private String name;
    private String category;
    private String color;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

/**
 * Metadatos del descubrimiento.
 */
class DiscoveryMetadata {
    private String discoveryDate;
    private String version;
    private DiscoveryStatistics statistics;
    private DiscoveryConfiguration configuration;
    
    public String getDiscoveryDate() { return discoveryDate; }
    public void setDiscoveryDate(String discoveryDate) { this.discoveryDate = discoveryDate; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public DiscoveryStatistics getStatistics() { return statistics; }
    public void setStatistics(DiscoveryStatistics statistics) { this.statistics = statistics; }
    
    public DiscoveryConfiguration getConfiguration() { return configuration; }
    public void setConfiguration(DiscoveryConfiguration configuration) { this.configuration = configuration; }
}

/**
 * Estadísticas del descubrimiento.
 */
class DiscoveryStatistics {
    private int totalProjects;
    private int totalTeams;
    private int totalWorkItemTypes;
    private int totalCustomFields;
    private String executionTimeMs;
    
    public int getTotalProjects() { return totalProjects; }
    public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }
    
    public int getTotalTeams() { return totalTeams; }
    public void setTotalTeams(int totalTeams) { this.totalTeams = totalTeams; }
    
    public int getTotalWorkItemTypes() { return totalWorkItemTypes; }
    public void setTotalWorkItemTypes(int totalWorkItemTypes) { this.totalWorkItemTypes = totalWorkItemTypes; }
    
    public int getTotalCustomFields() { return totalCustomFields; }
    public void setTotalCustomFields(int totalCustomFields) { this.totalCustomFields = totalCustomFields; }
    
    public String getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(String executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}

/**
 * Configuración del descubrimiento.
 */
class DiscoveryConfiguration {
    private boolean includeDeletedProjects;
    private boolean includeCustomFields;
    private boolean includeTeamAnalysis;
    private int maxProjectsToScan;
    
    public boolean isIncludeDeletedProjects() { return includeDeletedProjects; }
    public void setIncludeDeletedProjects(boolean includeDeletedProjects) { this.includeDeletedProjects = includeDeletedProjects; }
    
    public boolean isIncludeCustomFields() { return includeCustomFields; }
    public void setIncludeCustomFields(boolean includeCustomFields) { this.includeCustomFields = includeCustomFields; }
    
    public boolean isIncludeTeamAnalysis() { return includeTeamAnalysis; }
    public void setIncludeTeamAnalysis(boolean includeTeamAnalysis) { this.includeTeamAnalysis = includeTeamAnalysis; }
    
    public int getMaxProjectsToScan() { return maxProjectsToScan; }
    public void setMaxProjectsToScan(int maxProjectsToScan) { this.maxProjectsToScan = maxProjectsToScan; }
}
