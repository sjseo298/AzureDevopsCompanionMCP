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
    private static final int DEFAULT_TOP = 25;
    private static final int MAX_TOP = 200;

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

    public Map<String,Object> fetchByQuery(String project, String team, Object wiql, Object top, Object apiVersion) {
        String version = resolveApiVersion(apiVersion, "7.2-preview");
        Map<String,Object> body = Map.of("query", wiql.toString());
        Map<String,String> query = new LinkedHashMap<>();
        query.put("api-version", version);
        Integer topInt = normalizeTop(top);
        query.put("$top", topInt.toString());
        return azureService.postWitApiWithQuery(project, team, "wiql", query, body, version, org.springframework.http.MediaType.APPLICATION_JSON);
    }

    /**
     * Ejecuta una consulta WIQL y obtiene los datos completos de los work items.
     * Según la documentación oficial de Azure DevOps, WIQL solo devuelve IDs de work items,
     * por lo que necesitamos una segunda llamada para obtener los datos de las columnas.
     */
    public Map<String,Object> fetchByQueryWithData(String project, String team, Object wiql, Object apiVersion) {
        return fetchByQueryWithData(project, team, wiql, null, null, apiVersion);
    }

    public Map<String,Object> fetchByQueryWithData(String project, String team, Object wiql, Object top, Object fieldsOverride, Object apiVersion) {
        // Primero ejecutamos la consulta WIQL para obtener los IDs
        Map<String,Object> wiqlResult = fetchByQuery(project, team, wiql, top, apiVersion);
        
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
        
        Integer topInt = normalizeTop(top);
        List<Map<String,Object>> orderedWorkItems = workItems;
        if (topInt != null && topInt < orderedWorkItems.size()) {
            orderedWorkItems = orderedWorkItems.subList(0, topInt);
        }

        List<Integer> idsInOrder = orderedWorkItems.stream()
            .map(wi -> Integer.valueOf(wi.get("id").toString()))
            .toList();

        // Extraemos los IDs y convertimos a string separado por comas
        String ids = orderedWorkItems.stream()
            .map(wi -> wi.get("id").toString())
            .collect(Collectors.joining(","));
        
        // Extraemos las columnas solicitadas en la consulta WIQL para filtrar los campos
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> columns = (List<Map<String,Object>>) wiqlResult.get("columns");
        String fields = null;
        if (fieldsOverride != null && !fieldsOverride.toString().isBlank()) {
            fields = fieldsOverride.toString();
        } else if (columns != null && !columns.isEmpty()) {
            fields = columns.stream()
                .map(col -> col.get("referenceName").toString())
                .collect(Collectors.joining(","));
            if (isOnlyIdField(fields)) fields = defaultUsefulFields();
        } else {
            fields = defaultUsefulFields();
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
        
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> fetchedData = (List<Map<String,Object>>) workItemsData.get("value");
        List<Map<String,Object>> orderedData = preserveWiqlOrder(fetchedData, idsInOrder);

        // Combinamos el resultado WIQL con los datos de work items
        Map<String,Object> enrichedResult = new HashMap<>(wiqlResult);
        enrichedResult.put("workItems", orderedWorkItems);
        enrichedResult.put("workItemsData", orderedData);
        enrichedResult.put("items", buildStructuredItems(project, orderedData));
        enrichedResult.put("count", orderedData.size());
        enrichedResult.put("wiqlOrderPreserved", true);
        enrichedResult.put("idsInWiqlOrder", idsInOrder);
        enrichedResult.put("topApplied", topInt);
        enrichedResult.put("maxTop", MAX_TOP);
        enrichedResult.put("fieldsUsed", fields);
        
        return enrichedResult;
    }

    private Integer normalizeTop(Object value) {
        Integer parsed = parsePositiveInt(value);
        if (parsed == null) return DEFAULT_TOP;
        return Math.min(parsed, MAX_TOP);
    }

    private Integer parsePositiveInt(Object value) {
        if (value == null) return null;
        try {
            int n = Integer.parseInt(value.toString());
            return n > 0 ? n : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isOnlyIdField(String fields) {
        if (fields == null) return false;
        String normalized = fields.replace(" ", "").toLowerCase(Locale.ROOT);
        return "system.id".equals(normalized) || "id".equals(normalized);
    }

    private String defaultUsefulFields() {
        return "System.Id,System.Title,System.State,System.WorkItemType,System.AssignedTo,System.CreatedDate,System.CreatedBy,System.ChangedDate";
    }

    private List<Map<String,Object>> buildStructuredItems(String project, List<Map<String,Object>> workItemsData) {
        if (workItemsData == null || workItemsData.isEmpty()) return List.of();
        List<Map<String,Object>> out = new ArrayList<>();
        for (Map<String,Object> item : workItemsData) {
            Map<String,Object> row = new LinkedHashMap<>();
            Object id = item.get("id");
            row.put("id", id);
            @SuppressWarnings("unchecked")
            Map<String,Object> fields = item.get("fields") instanceof Map ? (Map<String,Object>) item.get("fields") : Map.of();
            putIfPresent(row, "title", fields.get("System.Title"));
            putIfPresent(row, "type", fields.get("System.WorkItemType"));
            putIfPresent(row, "state", fields.get("System.State"));
            putIfPresent(row, "createdDate", fields.get("System.CreatedDate"));
            putIfPresent(row, "changedDate", fields.get("System.ChangedDate"));
            Object assignedTo = compactIdentity(fields.get("System.AssignedTo"));
            if (assignedTo != null) row.put("assignedTo", assignedTo);
            Object createdBy = compactIdentity(fields.get("System.CreatedBy"));
            if (createdBy != null) row.put("createdBy", createdBy);
            Object changedBy = compactIdentity(fields.get("System.ChangedBy"));
            if (changedBy != null) row.put("changedBy", changedBy);
            String url = buildWorkItemUrl(project, id, item.get("url"));
            if (url != null) row.put("url", url);
            row.put("fields", compactFields(fields));
            out.add(row);
        }
        return out;
    }

    private void putIfPresent(Map<String,Object> row, String key, Object value) {
        if (value != null) row.put(key, value);
    }

    private Map<String,Object> compactFields(Map<String,Object> fields) {
        Map<String,Object> compact = new LinkedHashMap<>();
        for (Map.Entry<String,Object> entry : fields.entrySet()) {
            Object value = compactIdentity(entry.getValue());
            compact.put(entry.getKey(), value != null ? value : entry.getValue());
        }
        return compact;
    }

    private Object compactIdentity(Object value) {
        if (!(value instanceof Map<?,?> identity)) return null;
        Map<String,Object> compact = new LinkedHashMap<>();
        Object displayName = identity.get("displayName");
        Object uniqueName = identity.get("uniqueName");
        Object id = identity.get("id");
        Object descriptor = identity.get("descriptor");
        if (displayName != null) compact.put("displayName", displayName);
        if (uniqueName != null) compact.put("uniqueName", uniqueName);
        if (id != null) compact.put("id", id);
        if (descriptor != null) compact.put("descriptor", descriptor);
        return compact.isEmpty() ? null : compact;
    }

    private String buildWorkItemUrl(String project, Object id, Object apiUrl) {
        if (id == null) return null;
        String org = extractOrganization(apiUrl);
        if (org == null) return null;
        String projectSegment = project == null || project.isBlank() ? "_workitems" : project;
        return "https://dev.azure.com/" + org + "/" + projectSegment + "/_workitems/edit/" + id;
    }

    private String extractOrganization(Object apiUrl) {
        if (apiUrl == null) return null;
        String url = apiUrl.toString();
        String marker = "https://dev.azure.com/";
        int start = url.indexOf(marker);
        if (start < 0) return null;
        String rest = url.substring(start + marker.length());
        int slash = rest.indexOf('/');
        return slash > 0 ? rest.substring(0, slash) : null;
    }

    private List<Map<String,Object>> preserveWiqlOrder(List<Map<String,Object>> fetchedData, List<Integer> idsInOrder) {
        if (fetchedData == null || fetchedData.isEmpty()) return List.of();
        Map<Integer, Map<String,Object>> byId = new HashMap<>();
        for (Map<String,Object> item : fetchedData) {
            Object idObj = item.get("id");
            if (idObj == null) continue;
            try {
                byId.put(Integer.valueOf(idObj.toString()), item);
            } catch (Exception ignored) {}
        }
        List<Map<String,Object>> ordered = new ArrayList<>();
        for (Integer id : idsInOrder) {
            Map<String,Object> item = byId.get(id);
            if (item != null) ordered.add(item);
        }
        return ordered;
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
        Object topApplied = data.get("topApplied");
        if (topApplied != null) sb.append("Top aplicado: ").append(topApplied).append('\n');
        Object orderPreserved = data.get("wiqlOrderPreserved");
        if (orderPreserved != null) sb.append("Orden WIQL preservado: ").append(orderPreserved).append('\n');
        
        @SuppressWarnings("unchecked") 
        List<Map<String,Object>> cols = (List<Map<String,Object>>) data.get("columns");
        if (cols != null && !cols.isEmpty()) {
            sb.append("Columnas (").append(cols.size()).append("): ");
            cols.stream().limit(20).forEach(c -> sb.append(c.getOrDefault("referenceName","?")).append(" "));
            sb.append('\n');
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> structuredItems = (List<Map<String,Object>>) data.get("items");
        if (structuredItems != null && !structuredItems.isEmpty()) {
            sb.append("WorkItems: ").append(structuredItems.size()).append('\n');
            int i = 1;
            for (Map<String,Object> item : structuredItems) {
                if (i > 25) {
                    sb.append("... (").append(structuredItems.size() - 25).append(" más)\n");
                    break;
                }
                sb.append(i++).append(") ID=").append(item.get("id"));
                appendIfPresent(sb, ", Tipo=", item.get("type"));
                appendQuotedIfPresent(sb, ", Título=", item.get("title"));
                appendIfPresent(sb, ", Estado=", item.get("state"));
                appendIfPresent(sb, ", Created Date=", item.get("createdDate"));
                appendIfPresent(sb, ", Changed Date=", item.get("changedDate"));
                Object assignedTo = item.get("assignedTo");
                if (assignedTo != null) sb.append(", Asignado=").append(formatIdentity(assignedTo));
                Object createdBy = item.get("createdBy");
                if (createdBy != null) sb.append(", Created By=").append(formatIdentity(createdBy));
                Object changedBy = item.get("changedBy");
                if (changedBy != null) sb.append(", Changed By=").append(formatIdentity(changedBy));
                Object url = item.get("url");
                if (url != null) sb.append("\n   URL: ").append(url);
                sb.append('\n');
            }
            return sb.toString().isBlank() ? data.toString() : sb.toString();
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

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) sb.append(label).append(value);
    }

    private void appendQuotedIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) sb.append(label).append('"').append(value).append('"');
    }

    private String formatIdentity(Object value) {
        if (value instanceof Map<?,?> identity) {
            Object displayName = identity.get("displayName");
            Object uniqueName = identity.get("uniqueName");
            if (displayName != null && uniqueName != null) return displayName + " <" + uniqueName + ">";
            if (displayName != null) return displayName.toString();
            if (uniqueName != null) return uniqueName.toString();
        }
        return value.toString();
    }
}
