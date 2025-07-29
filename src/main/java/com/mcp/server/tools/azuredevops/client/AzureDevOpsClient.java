package com.mcp.server.tools.azuredevops.client;

import com.mcp.server.tools.azuredevops.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para interactuar con la API REST de Azure DevOps.
 * 
 * <p>Proporciona métodos para todas las operaciones principales:
 * proyectos, equipos, work items, consultas WIQL e iteraciones.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class AzureDevOpsClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureDevOpsClient.class);
    private static final String API_VERSION = "7.1";
    
    private final WebClient webClient;
    private final String organization;
    private final String baseUrl;
    
    /**
     * Constructor que configura el cliente web con autenticación.
     */
    public AzureDevOpsClient(
            @Value("${azure.devops.organization:#{null}}") String organization,
            @Value("${azure.devops.pat:#{null}}") String pat) {
        
        this.organization = organization;
        this.baseUrl = String.format("https://dev.azure.com/%s", organization);
        
        // Crear headers de autenticación Basic Auth con PAT
        String auth = ":" + (pat != null ? pat : "");
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
        
        logger.info("Azure DevOps client initialized for organization: {}", organization);
    }
    
    /**
     * Lista todos los proyectos disponibles en la organización.
     * 
     * @return lista de proyectos
     * @throws AzureDevOpsException si hay error en la API
     */
    public List<Project> listProjects() {
        logger.debug("Listing projects for organization: {}", organization);
        
        try {
            ApiResponse.ListResponse<Project> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/_apis/projects")
                    .queryParam("api-version", API_VERSION)
                    .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse.ListResponse<Project>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
            
            List<Project> projects = response != null ? response.getItems() : List.of();
            logger.debug("Found {} projects", projects.size());
            return projects;
            
        } catch (WebClientResponseException e) {
            logger.error("Error listing projects: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to list projects: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error listing projects", e);
            throw new AzureDevOpsException("Unexpected error listing projects", e);
        }
    }
    
    /**
     * Lista todos los equipos de un proyecto.
     * 
     * @param project nombre o ID del proyecto
     * @return lista de equipos
     * @throws AzureDevOpsException si hay error en la API
     */
    public List<Team> listTeams(String project) {
        logger.debug("Listing teams for project: {}", project);
        
        try {
            ApiResponse.ListResponse<Team> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/_apis/projects/{project}/teams")
                    .queryParam("api-version", API_VERSION)
                    .build(project))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse.ListResponse<Team>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
            
            List<Team> teams = response != null ? response.getItems() : List.of();
            logger.debug("Found {} teams in project {}", teams.size(), project);
            return teams;
            
        } catch (WebClientResponseException e) {
            logger.error("Error listing teams for project {}: {} - {}", project, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to list teams: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error listing teams for project {}", project, e);
            throw new AzureDevOpsException("Unexpected error listing teams", e);
        }
    }
    
    /**
     * Obtiene un work item por su ID.
     * 
     * @param project nombre o ID del proyecto
     * @param workItemId ID del work item
     * @param fields campos específicos a obtener (opcional)
     * @param expand expansiones adicionales (opcional)
     * @return work item encontrado
     * @throws AzureDevOpsException si hay error en la API
     */
    public WorkItem getWorkItem(String project, Integer workItemId, List<String> fields, String expand) {
        logger.debug("Getting work item {} from project {}", workItemId, project);
        
        try {
            var uriBuilder = webClient.get()
                .uri(uriBuilder2 -> {
                    var builder = uriBuilder2
                        .path("/{project}/_apis/wit/workitems/{id}")
                        .queryParam("api-version", API_VERSION);
                    
                    if (fields != null && !fields.isEmpty()) {
                        builder.queryParam("fields", String.join(",", fields));
                    }
                    
                    if (expand != null && !expand.trim().isEmpty()) {
                        builder.queryParam("$expand", expand);
                    }
                    
                    return builder.build(project, workItemId);
                });
            
            WorkItem workItem = uriBuilder
                .retrieve()
                .bodyToMono(WorkItem.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            logger.debug("Retrieved work item: {}", workItem != null ? workItem.toDisplayString() : "null");
            return workItem;
            
        } catch (WebClientResponseException e) {
            logger.error("Error getting work item {} from project {}: {} - {}", 
                workItemId, project, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to get work item: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting work item {} from project {}", workItemId, project, e);
            throw new AzureDevOpsException("Unexpected error getting work item", e);
        }
    }
    
    /**
     * Obtiene un work item por su ID sin especificar proyecto.
     * 
     * @param workItemId ID del work item
     * @return mapa con los datos del work item
     * @throws AzureDevOpsException si hay error en la API
     */
    public Map<String, Object> getWorkItem(Integer workItemId) {
        logger.debug("Getting work item {} without project specification", workItemId);
        
        try {
            var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/_apis/wit/workitems/{id}")
                    .queryParam("api-version", API_VERSION)
                    .build(workItemId))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            logger.debug("Retrieved work item: {}", response != null ? "success" : "null");
            return response;
            
        } catch (WebClientResponseException e) {
            logger.error("Error getting work item {}: {} - {}", 
                workItemId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to get work item: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting work item {}", workItemId, e);
            throw new AzureDevOpsException("Unexpected error getting work item", e);
        }
    }

        /**
     * Obtiene múltiples work items por sus IDs usando la API batch.
     * 
     * @param project Nombre o ID del proyecto
     * @param workItemIds Lista de IDs de work items
     * @param fields Lista de campos específicos a obtener (opcional)
     * @return Lista de work items
     * @throws AzureDevOpsException si hay error en la API
     */
    public List<WorkItem> getWorkItems(String project, List<Integer> workItemIds, List<String> fields) {
        if (workItemIds == null || workItemIds.isEmpty()) {
            return List.of();
        }
        
        logger.debug("Getting {} work items from project {}", workItemIds.size(), project);
        
        try {
            // Crear el body de la petición batch según la documentación de Microsoft
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ids", workItemIds);
            
            if (fields != null && !fields.isEmpty()) {
                requestBody.put("fields", fields);
            }
            
            var response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/{project}/_apis/wit/workitemsbatch")
                    .queryParam("api-version", API_VERSION)
                    .build(project))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse.ListResponse<WorkItem>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
            
            List<WorkItem> workItems = response != null ? response.getItems() : List.of();
            logger.debug("Retrieved {} work items", workItems.size());
            return workItems;
            
        } catch (WebClientResponseException e) {
            logger.error("Error getting work items from project {}: {} - {}", 
                project, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to get work items: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting work items from project {}", project, e);
            throw new AzureDevOpsException("Unexpected error getting work items", e);
        }
    }
    
    /**
     * Ejecuta una consulta WIQL.
     * 
     * @param project nombre o ID del proyecto
     * @param team nombre o ID del equipo (opcional)
     * @param wiqlQuery consulta WIQL
     * @return resultado de la consulta
     * @throws AzureDevOpsException si hay error en la API
     */
    public WiqlQueryResult executeWiqlQuery(String project, String team, String wiqlQuery) {
        logger.debug("Executing WIQL query in project {} team {}: {}", project, team, wiqlQuery);
        
        try {
            String path = team != null && !team.trim().isEmpty()
                ? "/{project}/{team}/_apis/wit/wiql"
                : "/{project}/_apis/wit/wiql";
            
            Map<String, String> requestBody = Map.of("query", wiqlQuery);
            
            var request = webClient.post()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path(path)
                        .queryParam("api-version", API_VERSION);
                    
                    if (team != null && !team.trim().isEmpty()) {
                        return builder.build(project, team);
                    } else {
                        return builder.build(project);
                    }
                })
                .bodyValue(requestBody);
            
            WiqlQueryResult result = request
                .retrieve()
                .bodyToMono(WiqlQueryResult.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            logger.debug("WIQL query returned {} work items", result != null ? result.getResultCount() : 0);
            return result;
            
        } catch (WebClientResponseException e) {
            logger.error("Error executing WIQL query in project {} team {}: {} - {}", 
                project, team, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to execute WIQL query: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error executing WIQL query in project {} team {}", project, team, e);
            throw new AzureDevOpsException("Unexpected error executing WIQL query", e);
        }
    }
    
    /**
     * Crea un nuevo work item.
     * 
     * @param project nombre o ID del proyecto
     * @param workItemType tipo de work item (Task, User Story, Bug, etc.)
     * @param operations operaciones JSON Patch para crear el work item
     * @return work item creado
     * @throws AzureDevOpsException si hay error en la API
     */
    public WorkItem createWorkItem(String project, String workItemType, List<Map<String, Object>> operations) {
        logger.debug("Creating work item of type {} in project {}", workItemType, project);
        logger.debug("Operations to send: {}", operations);
        
        try {
            String uri = "/" + project + "/_apis/wit/workitems/$" + workItemType;
            logger.debug("Request URI: {}", uri);
            
            WorkItem createdItem = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path(uri)
                    .queryParam("api-version", API_VERSION)
                    .build())
                .header(HttpHeaders.CONTENT_TYPE, "application/json-patch+json")
                .bodyValue(operations)
                .retrieve()
                .bodyToMono(WorkItem.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            logger.debug("Created work item: {}", createdItem != null ? createdItem.toDisplayString() : "null");
            return createdItem;
            
        } catch (WebClientResponseException e) {
            String errorDetails = e.getResponseBodyAsString();
            String requestUri = "/" + project + "/_apis/wit/workitems/$" + workItemType;
            logger.error("Error creating work item of type {} in project {}: {} - {}", 
                workItemType, project, e.getStatusCode(), errorDetails);
            logger.error("Request URI was: {}", requestUri);
            logger.error("Operations sent: {}", operations);
            throw new AzureDevOpsException("Failed to create work item: " + e.getMessage() + " - " + errorDetails, e);
        } catch (Exception e) {
            logger.error("Unexpected error creating work item of type {} in project {}", workItemType, project, e);
            throw new AzureDevOpsException("Unexpected error creating work item", e);
        }
    }
    
    /**
     * Actualiza un work item existente.
     * 
     * @param project nombre o ID del proyecto
     * @param workItemId ID del work item
     * @param operations operaciones JSON Patch para actualizar
     * @return work item actualizado
     * @throws AzureDevOpsException si hay error en la API
     */
    public WorkItem updateWorkItem(String project, Integer workItemId, List<Map<String, Object>> operations) {
        logger.debug("Updating work item {} in project {}", workItemId, project);
        
        try {
            WorkItem updatedItem = webClient.patch()
                .uri(uriBuilder -> uriBuilder
                    .path("/{project}/_apis/wit/workitems/{id}")
                    .queryParam("api-version", API_VERSION)
                    .build(project, workItemId))
                .header(HttpHeaders.CONTENT_TYPE, "application/json-patch+json")
                .bodyValue(operations)
                .retrieve()
                .bodyToMono(WorkItem.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            logger.debug("Updated work item: {}", updatedItem != null ? updatedItem.toDisplayString() : "null");
            return updatedItem;
            
        } catch (WebClientResponseException e) {
            logger.error("Error updating work item {} in project {}: {} - {}", 
                workItemId, project, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to update work item: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error updating work item {} in project {}", workItemId, project, e);
            throw new AzureDevOpsException("Unexpected error updating work item", e);
        }
    }
    
    /**
     * Lista las iteraciones de un equipo.
     * 
     * @param project nombre o ID del proyecto
     * @param team nombre o ID del equipo
     * @param timeFrame filtro temporal (current, past, future - opcional)
     * @return lista de iteraciones
     * @throws AzureDevOpsException si hay error en la API
     */
    public List<Iteration> listIterations(String project, String team, String timeFrame) {
        logger.debug("Listing iterations for project {} team {} timeframe {}", project, team, timeFrame);
        
        try {
            var uriBuilder = webClient.get()
                .uri(uriBuilder2 -> {
                    var builder = uriBuilder2
                        .path("/{project}/{team}/_apis/work/teamsettings/iterations")
                        .queryParam("api-version", API_VERSION);
                    
                    if (timeFrame != null && !timeFrame.trim().isEmpty()) {
                        builder.queryParam("$timeframe", timeFrame);
                    }
                    
                    return builder.build(project, team);
                });
            
            ApiResponse.ListResponse<Iteration> response = uriBuilder
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse.ListResponse<Iteration>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
            
            List<Iteration> iterations = response != null ? response.getItems() : List.of();
            logger.debug("Found {} iterations", iterations.size());
            return iterations;
            
        } catch (WebClientResponseException e) {
            logger.error("Error listing iterations for project {} team {}: {} - {}", 
                project, team, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to list iterations: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error listing iterations for project {} team {}", project, team, e);
            throw new AzureDevOpsException("Unexpected error listing iterations", e);
        }
    }
    
    /**
     * Obtiene los tipos de work items disponibles en un proyecto.
     * 
     * @param project nombre del proyecto
     * @return mapa con información de los tipos de work items
     */
    public Map<String, Object> getWorkItemTypes(String project) {
        try {
            logger.debug("Getting work item types for project: {}", project);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + project + "/_apis/wit/workitemtypes")
                    .queryParam("api-version", API_VERSION)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            logger.debug("Retrieved work item types for project {}", project);
            return response != null ? response : new HashMap<>();
            
        } catch (WebClientResponseException e) {
            logger.error("Error getting work item types for project {}: {} - {}", 
                project, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AzureDevOpsException("Failed to get work item types: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error getting work item types for project {}", project, e);
            throw new AzureDevOpsException("Unexpected error getting work item types", e);
        }
    }
    
    /**
     * Elimina un work item específico.
     * 
     * <p>Por defecto, el work item se envía a la papelera de reciclaje y puede ser restaurado.
     * Si se especifica destroy=true, el work item se elimina permanentemente.
     * 
     * @param project nombre o ID del proyecto
     * @param workItemId ID del work item a eliminar
     * @param destroy si true, elimina permanentemente (IRREVERSIBLE)
     * @return información del work item eliminado
     * @throws AzureDevOpsException si hay error en la API
     */
    public Map<String, Object> deleteWorkItem(String project, Integer workItemId, Boolean destroy) {
        logger.debug("Deleting work item {} in project {} (destroy: {})", workItemId, project, destroy);
        
        try {
            // Usar retrieve().toBodilessEntity() para DELETE según el patrón de otros métodos
            var responseEntity = webClient.delete()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path("/{project}/_apis/wit/workitems/{id}")
                        .queryParam("api-version", API_VERSION);
                    
                    if (destroy != null && destroy) {
                        builder = builder.queryParam("destroy", "true");
                    }
                    
                    return builder.build(project, workItemId);
                })
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(30))
                .block();
            
            Map<String, Object> response = responseEntity != null ? responseEntity.getBody() : null;
            logger.debug("Deleted work item {}: status={}, hasBody={}", 
                workItemId, 
                responseEntity != null ? responseEntity.getStatusCode() : "null",
                response != null);
            
            return response != null ? response : Map.of("success", true, "workItemId", workItemId);
            
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("Error deleting work item {} in project {}: {} - {}", 
                workItemId, project, e.getStatusCode(), errorBody);
            
            // Incluir el cuerpo del error para mejor diagnóstico
            String errorMessage = String.format("Failed to delete work item (HTTP %d): %s", 
                e.getStatusCode().value(), 
                errorBody.isEmpty() ? e.getMessage() : errorBody);
            
            throw new AzureDevOpsException(errorMessage, e);
        } catch (Exception e) {
            logger.error("Unexpected error deleting work item {} in project {}", workItemId, project, e);
            throw new AzureDevOpsException("Unexpected error deleting work item", e);
        }
    }
    
    /**
     * Verifica la configuración del cliente.
     * 
     * @return true si la configuración es válida
     */
    public boolean isConfigured() {
        return organization != null && !organization.trim().isEmpty();
    }
    
    /**
     * Obtiene la organización configurada.
     * 
     * @return nombre de la organización
     */
    public String getOrganization() {
        return organization;
    }
}
