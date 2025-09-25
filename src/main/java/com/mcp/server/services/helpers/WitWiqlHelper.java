package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper para ejecución de queries WIQL en Azure DevOps.
 */
@Component
public class WitWiqlHelper {
    private final AzureDevOpsClientService azureService;
    private final WitWorkItemsListHelper workItemsHelper;

    public WitWiqlHelper(AzureDevOpsClientService svc, WitWorkItemsListHelper workItemsHelper) {
        this.azureService = svc;
        this.workItemsHelper = workItemsHelper;
    }

    public void validateId(Object id) {
        if (id == null || id.toString().trim().isEmpty()) throw new IllegalArgumentException("'id' es requerido");
    }
    public void validateWiql(Object wiql) {
        if (wiql == null || wiql.toString().trim().isEmpty()) throw new IllegalArgumentException("'wiql' es requerido");
    }
    public String resolveApiVersion(Object apiVersion, String defaultVersion) {
        return Optional.ofNullable(apiVersion).map(Object::toString).filter(s->!s.isBlank()).orElse(defaultVersion);
    }

    public Map<String,Object> fetchById(String project, String team, Object id, Object apiVersion) {
        String endpoint = "wiql/" + id;
        String version = resolveApiVersion(apiVersion, "7.2-preview");
        return azureService.getWitApiWithQuery(project, team, endpoint, null, version);
    }
    public Map<String,Object> fetchByQuery(String project, String team, Object wiql, Object apiVersion) {
        String version = resolveApiVersion(apiVersion, "7.2-preview");
        Map<String,Object> body = Map.of("query", wiql.toString());
        return azureService.postWitApi(project, team, "wiql", body, version);
    }

    /**
     * Ejecuta una consulta WIQL y obtiene los datos completos de los work items.
     * Según la documentación oficial de Azure DevOps, WIQL solo devuelve IDs de work items,
     * por lo que necesitamos una segunda llamada para obtener los datos de las columnas.
     */
    public Map<String,Object> fetchByQueryWithData(String project, String team, Object wiql, Object apiVersion) {
        // Primero ejecutamos la consulta WIQL para obtener los IDs
        Map<String,Object> wiqlResult = fetchByQuery(project, team, wiql, apiVersion);
        
        // Si hay error en la consulta WIQL, lo devolvemos
        if (wiqlResult.containsKey("isHttpError") || wiqlResult.isEmpty()) {
            return wiqlResult;
        }
        
        // Extraemos los IDs de los work items
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> workItems = (List<Map<String,Object>>) wiqlResult.get("workItems");
        
        if (workItems == null || workItems.isEmpty()) {
            return wiqlResult; // No hay work items, devolver resultado original
        }
        
        // Extraemos los IDs y convertimos a string separado por comas
        String ids = workItems.stream()
            .map(wi -> wi.get("id").toString())
            .collect(Collectors.joining(","));
        
        // Extraemos las columnas solicitadas en la consulta WIQL para filtrar los campos
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> columns = (List<Map<String,Object>>) wiqlResult.get("columns");
        String fields = null;
        if (columns != null && !columns.isEmpty()) {
            fields = columns.stream()
                .map(col -> col.get("referenceName").toString())
                .collect(Collectors.joining(","));
        }
        
        // Obtenemos los datos completos de los work items
        Map<String,Object> workItemsArgs = new HashMap<>();
        workItemsArgs.put("project", project);
        workItemsArgs.put("ids", ids);
        if (fields != null) {
            workItemsArgs.put("fields", fields);
        }
        workItemsArgs.put("apiVersion", resolveApiVersion(apiVersion, "7.2-preview"));
        
        Map<String,Object> workItemsData = workItemsHelper.list(workItemsArgs);
        
        // Si hay error al obtener los work items, devolvemos el resultado WIQL original
        if (workItemsData.containsKey("isHttpError")) {
            return wiqlResult;
        }
        
        // Combinamos el resultado WIQL con los datos de work items
        Map<String,Object> enrichedResult = new HashMap<>(wiqlResult);
        enrichedResult.put("workItemsData", workItemsData.get("value"));
        
        return enrichedResult;
    }

    public boolean isFolderQueryError(Map<String,Object> resp) {
        if (resp == null) return false;
        Object typeKey = resp.get("typeKey");
        Object message = resp.get("message");
        return typeKey != null && "QueryException".equals(typeKey.toString()) && message != null && message.toString().contains("Querying folders is not supported");
    }

    public String formatResponse(Map<String,Object> data) {
        if (data == null || data.isEmpty()) return "(Sin datos)";
        StringBuilder sb = new StringBuilder();
        
        Object queryType = data.get("queryType");
        if (queryType != null) sb.append("Tipo: ").append(queryType).append('\n');
        
        @SuppressWarnings("unchecked") 
        List<Map<String,Object>> cols = (List<Map<String,Object>>) data.get("columns");
        if (cols != null && !cols.isEmpty()) {
            sb.append("Columnas (").append(cols.size()).append("): ");
            cols.stream().limit(20).forEach(c -> sb.append(c.getOrDefault("referenceName","?")).append(" "));
            sb.append('\n');
        }
        
        // Verificar si tenemos datos completos de work items
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> workItemsData = (List<Map<String,Object>>) data.get("workItemsData");
        
        Object workItems = data.get("workItems");
        if (workItems instanceof List) {
            List<?> list = (List<?>) workItems;
            sb.append("WorkItems: ").append(list.size()).append('\n');
            
            if (workItemsData != null && !workItemsData.isEmpty()) {
                // Mostrar datos completos
                int i = 1;
                for (Map<String,Object> workItem : workItemsData) {
                    if (i > 25) { 
                        sb.append("... (").append(workItemsData.size() - 25).append(" más)\n"); 
                        break; 
                    }
                    
                    sb.append(i++).append(") ");
                    
                    @SuppressWarnings("unchecked")
                    Map<String,Object> fields = (Map<String,Object>) workItem.get("fields");
                    if (fields != null) {
                        // Mostrar campos principales
                        Object id = workItem.get("id");
                        Object title = fields.get("System.Title");
                        Object state = fields.get("System.State");
                        Object workItemType = fields.get("System.WorkItemType");
                        Object assignedTo = fields.get("System.AssignedTo");
                        
                        sb.append("ID=").append(id);
                        if (workItemType != null) sb.append(", Tipo=").append(workItemType);
                        if (title != null) sb.append(", Título=\"").append(title).append("\"");
                        if (state != null) sb.append(", Estado=").append(state);
                        if (assignedTo != null) {
                            if (assignedTo instanceof Map) {
                                Object displayName = ((Map<?,?>)assignedTo).get("displayName");
                                sb.append(", Asignado=").append(displayName != null ? displayName : assignedTo);
                            } else {
                                sb.append(", Asignado=").append(assignedTo);
                            }
                        }
                        
                        // Mostrar otros campos solicitados en la consulta
                        if (cols != null) {
                            for (Map<String,Object> col : cols) {
                                String refName = (String) col.get("referenceName");
                                if (refName != null && !refName.equals("System.Id") && 
                                    !refName.equals("System.Title") && !refName.equals("System.State") && 
                                    !refName.equals("System.WorkItemType") && !refName.equals("System.AssignedTo")) {
                                    Object value = fields.get(refName);
                                    if (value != null) {
                                        String friendlyName = (String) col.get("name");
                                        sb.append(", ").append(friendlyName != null ? friendlyName : refName).append("=").append(value);
                                    }
                                }
                            }
                        }
                    } else {
                        // Formato simple si no hay fields
                        Object wid = workItem.get("id");
                        sb.append("ID=").append(wid);
                    }
                    sb.append('\n');
                }
            } else {
                // Formato simple (solo IDs) - comportamiento anterior
                int i = 1;
                for (Object o : list) {
                    if (i > 25) { 
                        sb.append("... (").append(list.size() - 25).append(" más)\n"); 
                        break; 
                    }
                    if (o instanceof Map) {
                        Object wid = ((Map<?,?>)o).get("id");
                        sb.append(i++).append(") ID=").append(wid).append('\n');
                    }
                }
            }
        }
        
        return sb.toString().isBlank() ? data.toString() : sb.toString();
    }
}
