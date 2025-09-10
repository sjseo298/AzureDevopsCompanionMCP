package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Helper para crear work items replicando lógica avanzada del script bash.
 */
@Component
public class WitWorkItemCreateHelper {
    private final AzureDevOpsClientService client;
    private final WitWorkItemTypesFieldListHelper fieldListHelper;
    private final WitFieldsGlobalGetHelper fieldsGlobalHelper;
    private final WorkitemtrackingprocessPicklistsHelper picklistsHelper;

    public WitWorkItemCreateHelper(AzureDevOpsClientService client) {
        this.client = client;
        this.fieldListHelper = new WitWorkItemTypesFieldListHelper(client);
        this.fieldsGlobalHelper = new WitFieldsGlobalGetHelper(client);
        this.picklistsHelper = new WorkitemtrackingprocessPicklistsHelper(client);
    }

    public Map<String,Object> createWorkItem(Map<String,Object> args) {
    String project = args.getOrDefault("project","" ).toString();
        String type = args.getOrDefault("type","" ).toString();
        String title = args.getOrDefault("title","" ).toString();
        String apiVersion = args.getOrDefault("apiVersion","7.2-preview").toString();
        String area = args.getOrDefault("area",null) != null ? args.get("area").toString() : null;
        String iteration = args.getOrDefault("iteration",null) != null ? args.get("iteration").toString() : null;
        String description = args.getOrDefault("description",null) != null ? args.get("description").toString() : null;
        String state = args.getOrDefault("state",null) != null ? args.get("state").toString() : null;
    Integer parentId = args.getOrDefault("parentId",null) != null ? Integer.valueOf(args.get("parentId").toString()) : null;
        String fields = args.getOrDefault("fields",null) != null ? args.get("fields").toString() : null;
        String relations = args.getOrDefault("relations",null) != null ? args.get("relations").toString() : null;
        boolean raw = Boolean.TRUE.equals(args.get("raw"));
        boolean validateOnly = Boolean.TRUE.equals(args.get("validateOnly"));
        boolean bypassRules = Boolean.TRUE.equals(args.get("bypassRules"));
        boolean suppressNotifications = Boolean.TRUE.equals(args.get("suppressNotifications"));
        boolean debug = Boolean.TRUE.equals(args.get("debug"));
        boolean noDiagnostic = Boolean.TRUE.equals(args.get("noDiagnostic"));

        java.util.List<Map<String,Object>> patch = new java.util.ArrayList<>();
        patch.add(Map.of("op","add","path","/fields/System.Title","value",title));
        if (state != null && !state.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.State","value",state));
        if (description != null && !description.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.Description","value",description));
        if (area != null && !area.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.AreaPath","value",area));
        if (iteration != null && !iteration.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.IterationPath","value",iteration));

        if (fields != null && !fields.isEmpty()) {
            // CORREGIDO: Usar parsing inteligente para campos que pueden contener comas
            List<String> fieldPairs = smartSplitByComma(fields);
            for (String pair : fieldPairs) {
                if (pair.isBlank() || !pair.contains("=")) continue;
                String[] kv = pair.split("=",2);
                String k = kv[0].trim(); 
                String v = kv.length > 1 ? kv[1].trim() : "";
                if (k.isEmpty()) continue;
                Object valObj = v;
                if (v.matches("^-?\\d+$")) { try { valObj = Integer.parseInt(v); } catch (NumberFormatException ignored) {} }
                else if (v.matches("^-?\\d+\\.\\d+$")) { try { valObj = Double.parseDouble(v); } catch (NumberFormatException ignored) {} }
                patch.add(Map.of("op","add","path","/fields/"+k,"value",valObj));
            }
        }

        String parentArea = null, parentIter = null, parentUrl = null;
        if (parentId != null) {
            WitWorkItemGetHelper getHelper = new WitWorkItemGetHelper(client);
            // Si no se especificó project intentar una inferencia mínima: necesitamos proyecto para la ruta scoped.
            // Estrategia: Intentar consultar sin project (cliente añadirá solo /_apis/wit/... y Azure DevOps permite /_apis/wit/workitems/{id})
            Map<String,Object> parentJson = getHelper.getWorkItem(project, parentId, "System.AreaPath,System.IterationPath", apiVersion);
            if ((project == null || project.isBlank()) && parentJson != null) {
                // Intentar extraer project guid o name desde URL canonical si disponible
                Object pUrl = parentJson.get("url");
                if (pUrl instanceof String) {
                    String url = (String)pUrl;
                    // Ej: https://dev.azure.com/org/PROJECTGUID/_apis/wit/workItems/123
                    // Tomar el segmento anterior a _apis
                    int idxApis = url.indexOf("/_apis/wit/workItems/");
                    if (idxApis > 0) {
                        String prefix = url.substring(0, idxApis); // https://dev.azure.com/org/PROJECTGUID
                        String[] segs = prefix.split("/");
                        if (segs.length >= 5) { // .. dev.azure.com org PROJECTGUID
                            project = segs[segs.length-1];
                        }
                    }
                }
            }
            if (parentJson != null && parentJson.get("fields") instanceof Map) {
                Map<?,?> fm = (Map<?,?>)parentJson.get("fields");
                Object paObj = fm.get("System.AreaPath");
                Object piObj = fm.get("System.IterationPath");
                parentArea = (area == null || area.isEmpty()) && paObj != null ? paObj.toString() : null;
                parentIter = (iteration == null || iteration.isEmpty()) && piObj != null ? piObj.toString() : null;
            }
            if (parentArea != null && !parentArea.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.AreaPath","value",parentArea));
            if (parentIter != null && !parentIter.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.IterationPath","value",parentIter));
            if (parentJson != null && parentJson.get("url") != null) parentUrl = parentJson.get("url").toString();
            if (parentUrl != null && parentUrl.matches(".*dev.azure.com/[^/]+/_apis/wit/workItems/.*")) {
                Map<String,Object> parentFull = getHelper.getWorkItem(project, parentId, null, apiVersion);
                if (parentFull != null && parentFull.get("url") != null) parentUrl = parentFull.get("url").toString();
            }
            if (parentUrl != null && !parentUrl.isBlank()) {
                patch.add(Map.of("op","add","path","/relations/-","value",Map.of("rel","System.LinkTypes.Hierarchy-Reverse","url",parentUrl)));
            }
        }

        // Si aún no tenemos project (no se infirió) no podemos construir endpoint scoped -> devolver error consistente
        if (project == null || project.isBlank()) {
            return Map.of("isError", true, "message", "No se pudo inferir el proyecto a partir del parentId; especifique 'project'.");
        }

        if (relations != null && !relations.isEmpty()) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (String spec : relations.split(",")) {
                if (spec.isBlank() || !spec.contains(":")) continue;
                String[] parts = spec.split(":",3);
                if (parts.length < 2) continue;
                String relType = parts[0].trim();
                String relId = parts[1].trim();
                String comment = parts.length == 3 ? parts[2].trim() : null;
                if (!relId.matches("^\\d+$")) continue;
                String key = relType + "|" + relId + "|" + (comment==null?"":comment);
                if (!seen.add(key)) continue;
                WitWorkItemGetHelper getHelper = new WitWorkItemGetHelper(client);
                Map<String,Object> relJson = getHelper.getWorkItem(project, Integer.parseInt(relId), null, apiVersion);
                String relUrl = relJson != null && relJson.get("url") != null ? relJson.get("url").toString() : null;
                if (relUrl == null || relUrl.isBlank()) continue;
                java.util.Map<String,Object> relObj = new java.util.LinkedHashMap<>();
                relObj.put("rel", relType);
                relObj.put("url", relUrl);
                if (comment != null && !comment.isEmpty()) relObj.put("attributes", Map.of("comment", comment));
                patch.add(Map.of("op","add","path","/relations/-","value", relObj));
            }
        }

        if (debug) {
            System.err.println("--- JSON PATCH (debug) ---");
            System.err.println(patch);
            System.err.println("--------------------------");
        }

        java.util.Map<String,String> query = new java.util.LinkedHashMap<>();
        query.put("api-version", apiVersion);
        if (bypassRules) query.put("bypassRules", "true");
        if (suppressNotifications) query.put("suppressNotifications", "true");
        if (validateOnly) query.put("validateOnly", "true");
    String path = "workitems/$" + type;
    Map<String,Object> resp = client.postWitApiWithQuery(project, null, path, query, patch, apiVersion, MediaType.valueOf("application/json-patch+json"));

        if (!noDiagnostic && resp != null && resp.containsKey("customProperties")) {
            Object cp = resp.get("customProperties");
            if (cp instanceof Map) {
                Map<?,?> cpMap = (Map<?,?>)cp;
                Object ruleErrors = cpMap.get("RuleValidationErrors");
                if (ruleErrors instanceof java.util.List && !((java.util.List<?>)ruleErrors).isEmpty()) {
                    StringBuilder diag = new StringBuilder();
                    diag.append("--- Validación de campos con error ---\n");
                    Map<String,Object> fieldsSummary = fieldListHelper.listFields(project, type, true, true, true, apiVersion);
                    java.util.Map<String,Object> globalCache = new java.util.HashMap<>();
                    java.util.Map<String,String> pickItemsCache = new java.util.HashMap<>();
                    java.util.Set<String> requiredSuggestions = new java.util.HashSet<>();
                    for (Object errObj : (java.util.List<?>)ruleErrors) {
                        if (!(errObj instanceof Map)) continue;
                        Map<?,?> err = (Map<?,?>)errObj;
                        Object refObj = err.get("fieldReferenceName");
                        String ref = refObj == null ? "" : refObj.toString();
                        Object msgObj = err.get("errorMessage");
                        String msg = msgObj == null ? "(sin mensaje)" : msgObj.toString();
                        Object flagsObj = err.get("fieldStatusFlags");
                        String flags = flagsObj == null ? "-" : flagsObj.toString();
                        String typeInfo = ""; String pickInfo = "";
                        if (!ref.isEmpty() && fieldsSummary != null && fieldsSummary.get("value") instanceof java.util.List) {
                            for (Object fObj : (java.util.List<?>)fieldsSummary.get("value")) {
                                if (!(fObj instanceof Map)) continue;
                                Map<?,?> f = (Map<?,?>)fObj;
                                Object fRefObj = f.get("ref");
                                if (ref.equals(fRefObj == null ? "" : fRefObj.toString())) {
                                    Object gt = f.get("globalType"); if (gt == null) gt = f.get("projectType"); if (gt != null) typeInfo = " | type: " + gt;
                                    if (f.get("picklistItems") instanceof java.util.List && !((java.util.List<?>)f.get("picklistItems")).isEmpty()) {
                                        java.util.List<?> items = (java.util.List<?>)f.get("picklistItems");
                                        pickInfo = " | vals: " + String.join(" | ", items.stream().map(Object::toString).toArray(String[]::new));
                                    }
                                    break;
                                }
                            }
                        }
                        if ((pickInfo.isEmpty() || typeInfo.isEmpty()) && !ref.isEmpty()) {
                            @SuppressWarnings("unchecked") Map<String,Object> global = (Map<String,Object>)globalCache.computeIfAbsent(ref, r -> fieldsGlobalHelper.getFieldGlobal(r, true));
                            if (global != null) {
                                if (typeInfo.isEmpty() && global.get("type") != null) typeInfo = " | type: " + global.get("type");
                                if (pickInfo.isEmpty() && Boolean.TRUE.equals(global.get("isPicklist")) && global.get("picklistId") != null) {
                                    String pid = global.get("picklistId").toString();
                                    String itemsJoined = pickItemsCache.computeIfAbsent(pid, id -> {
                                        Map<String,Object> pick = picklistsHelper.fetchPicklist(id);
                                        Object its = pick.get("items");
                                        if (its instanceof java.util.List && !((java.util.List<?>)its).isEmpty()) {
                                            return String.join(" | ", ((java.util.List<?>)its).stream().map(Object::toString).toArray(String[]::new));
                                        }
                                        return "";
                                    });
                                    if (!itemsJoined.isBlank()) pickInfo = " | vals: " + itemsJoined;
                                }
                            }
                        }
                        if (flags.toLowerCase().contains("required")) requiredSuggestions.add(ref);
                        diag.append("* ").append(ref.isEmpty()?"(sin ref)":ref).append(" -> ").append(msg).append(" | flags: ").append(flags).append(typeInfo).append(pickInfo).append("\n");
                    }
                    if (!requiredSuggestions.isEmpty()) {
                        diag.append("Sugerencias (--fields):\n");
                        for (String r : requiredSuggestions) diag.append("  --fields ").append(r).append("=<valor>\n");
                    }
                    diag.append("--- Fin validación detallada ---\n");
                    resp.put("diagnostic", diag.toString());
                }
            }
        }
        return resp;
    }
    
    /**
     * Divide inteligentemente por comas, respetando valores que pueden contener comas internas
     */
    private List<String> smartSplitByComma(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int equalsCount = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '=' && !inQuotes) {
                equalsCount++;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                // Solo dividir en coma si ya hemos visto un '=' en el segmento actual
                // y el siguiente segmento parece ser otro campo=valor
                String remaining = text.substring(i + 1).trim();
                if (equalsCount > 0 && remaining.contains("=")) {
                    // Verificar si lo que sigue parece ser otro campo
                    int nextEquals = remaining.indexOf('=');
                    String potentialField = remaining.substring(0, nextEquals).trim();
                    // Si el potencial campo no contiene espacios excesivos, probablemente es un campo
                    if (potentialField.length() > 0 && potentialField.length() < 100 && 
                        !potentialField.contains("\n") && !potentialField.contains("#")) {
                        result.add(current.toString().trim());
                        current = new StringBuilder();
                        equalsCount = 0;
                        continue;
                    }
                }
                current.append(c);
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        
        return result;
    }
}
