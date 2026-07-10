package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * Helper para crear work items replicando lógica avanzada del script bash.
 */
@Component
public class WitWorkItemCreateHelper {
    private final AzureDevOpsClientService client;
    private final WitWorkItemTypesFieldListHelper fieldListHelper;
    private final WitFieldsGlobalGetHelper fieldsGlobalHelper;
    private final WorkitemtrackingprocessPicklistsHelper picklistsHelper;
    private final WitWorkItemGetFieldsHelper getFieldsHelper;

    public WitWorkItemCreateHelper(AzureDevOpsClientService client) {
        this.client = client;
        this.fieldListHelper = new WitWorkItemTypesFieldListHelper(client);
        this.fieldsGlobalHelper = new WitFieldsGlobalGetHelper(client);
        this.picklistsHelper = new WorkitemtrackingprocessPicklistsHelper(client);
        this.getFieldsHelper = new WitWorkItemGetFieldsHelper(client);
    }

    public Map<String,Object> createWorkItem(Map<String,Object> args) {
        String project = args.getOrDefault("project","" ).toString();
        String type = args.getOrDefault("type","" ).toString();
        String title = args.getOrDefault("title","" ).toString();
        String apiVersion = args.getOrDefault("apiVersion","7.2-preview").toString();
        String area = args.getOrDefault("area",null) != null ? args.get("area").toString() : null;
        String iteration = args.getOrDefault("iteration",null) != null ? args.get("iteration").toString() : null;
        String description = args.getOrDefault("description",null) != null ? args.get("description").toString() : null;
        String acceptanceCriteria = args.getOrDefault("acceptanceCriteria",null) != null ? args.get("acceptanceCriteria").toString() : null;
        if ((acceptanceCriteria == null || acceptanceCriteria.isBlank()) && args.get("criteria") != null) {
            acceptanceCriteria = args.get("criteria").toString();
        }
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

        // Pre-validar campos requeridos antes de crear
        if (!bypassRules && !project.isEmpty() && !type.isEmpty()) {
            Map<String, Object> validation;
            try {
                validation = getFieldsHelper.validateRequiredFields(project, type, fields, apiVersion);
            } catch (Exception e) {
                validation = Map.of("missingDetails", List.of(), "missingRequired", true);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> missingDetails = validation != null ? (List<Map<String, Object>>) validation.get("missingDetails") : List.of();
            if (missingDetails != null && !missingDetails.isEmpty()) {
                StringBuilder diag = new StringBuilder();
                diag.append("## Campos requeridos faltantes\n\n");
                for (Map<String, Object> detail : missingDetails) {
                    String ref = Objects.toString(detail.get("referenceName"), "");
                    String name = Objects.toString(detail.get("name"), ref);
                    diag.append("### ").append(ref);
                    if (!name.equals(ref)) diag.append(" (").append(name).append(")");
                    diag.append("\n");

                    if (Boolean.TRUE.equals(detail.get("isPicklist"))) {
                        Object items = detail.get("picklistItems");
                        if (items instanceof List && !((List<?>) items).isEmpty()) {
                            List<String> itemStrs = ((List<?>) items).stream().map(Object::toString).toList();
                            diag.append("**Valores válidos:** ").append(String.join(", ", itemStrs)).append("\n");
                        } else if (detail.get("limitedToValues") instanceof List) {
                            List<String> vals = ((List<?>) detail.get("limitedToValues")).stream().map(Object::toString).toList();
                            diag.append("**Valores válidos:** ").append(String.join(", ", vals)).append("\n");
                        }
                    }

                    String projType = Objects.toString(detail.get("projectType"), "");
                    String globType = Objects.toString(detail.get("globalType"), "");
                    if (!projType.isEmpty() || !globType.isEmpty()) {
                        diag.append("**Tipo:** ").append(globType.isEmpty() ? projType : globType).append("\n");
                    }
                    diag.append("\n");
                }
                diag.append("Proporcione los campos faltantes usando `fields`:");
                for (Map<String, Object> detail : missingDetails) {
                    String ref = Objects.toString(detail.get("referenceName"), "");
                    diag.append("\n  `fields=").append(ref).append("=<valor>`");
                }

                return Map.of(
                    "isError", true,
                    "error", "Faltan campos requeridos para el tipo '" + type + "'",
                    "content", List.of(Map.of("type", "text", "text", diag.toString())),
                    "diagnostic", diag.toString(),
                    "missingFields", missingDetails
                );
            }

            // Si validateOnly=true, devolver diagnóstico sin crear
            if (validateOnly) {
                Map<String, Object> fieldsData;
                try {
                    fieldsData = getFieldsHelper.getFieldsForType(project, type, apiVersion, false, false);
                } catch (Exception e) {
                    return Map.of(
                        "isError", true,
                        "error", "Error al obtener campos para validación: " + e.getMessage(),
                        "content", List.of(Map.of("type", "text", "text", "Error al obtener campos para validación: " + e.getMessage()))
                    );
                }
                if (fieldsData == null) {
                    return Map.of(
                        "isError", true,
                        "error", "No se pudieron obtener los campos para el tipo '" + type + "'",
                        "content", List.of(Map.of("type", "text", "text", "No se pudieron obtener los campos para el tipo '" + type + "'"))
                    );
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reqFields = (List<Map<String, Object>>) fieldsData.get("requiredFields");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sugFields = (List<Map<String, Object>>) fieldsData.get("suggestedFields");
                @SuppressWarnings("unchecked")
                List<String> validStates = (List<String>) fieldsData.get("validStates");

                StringBuilder sb = new StringBuilder();
                sb.append("## Validación para tipo: ").append(type).append("\n\n");
                sb.append("Proyecto: ").append(project).append("\n");

                sb.append("\n### Campos Requeridos (").append(reqFields != null ? reqFields.size() : 0).append(")\n");
                if (reqFields != null && !reqFields.isEmpty()) {
                    for (Map<String, Object> f : reqFields) {
                        sb.append("- **").append(f.get("referenceName")).append("**");
                        if (Boolean.TRUE.equals(f.get("isPicklist"))) {
                            Object items = f.get("picklistItems");
                            if (items instanceof List && !((List<?>) items).isEmpty()) {
                                sb.append(" | Valores: ").append(String.join(", ", ((List<?>) items).stream().map(Object::toString).toList()));
                            }
                        }
                        sb.append("\n");
                    }
                } else {
                    sb.append("No hay campos estrictamente requeridos.\n");
                }

                if (sugFields != null && !sugFields.isEmpty()) {
                    sb.append("\n### Campos Sugeridos\n");
                    for (Map<String, Object> f : sugFields) {
                        sb.append("- ").append(f.get("referenceName")).append("\n");
                    }
                }

                if (validStates != null && !validStates.isEmpty()) {
                    sb.append("\n### Estados Válidos\n");
                    sb.append("System.State puede ser: ").append(String.join(", ", validStates)).append("\n");
                }

                sb.append("\n✅ No se detectaron problemas. El work item se puede crear.");
                return Map.of(
                    "isError", false,
                    "validated", true,
                    "type", type,
                    "project", project,
                    "content", List.of(Map.of("type", "text", "text", sb.toString())),
                    "diagnostic", sb.toString(),
                    "requiredFields", reqFields != null ? reqFields : List.of(),
                    "suggestedFields", sugFields != null ? sugFields : List.of(),
                    "validStates", validStates != null ? validStates : List.of()
                );
            }
        }

        java.util.List<Map<String,Object>> patch = new java.util.ArrayList<>();
        patch.add(Map.of("op","add","path","/fields/System.Title","value",title));
        if (state != null && !state.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.State","value",state));
        if (description != null && !description.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.Description","value",AzureDevOpsRichHtmlHelper.normalize(description, "System.Description")));
        if (acceptanceCriteria != null && !acceptanceCriteria.isEmpty()) patch.add(Map.of("op","add","path","/fields/Microsoft.VSTS.Common.AcceptanceCriteria","value",AzureDevOpsRichHtmlHelper.normalize(acceptanceCriteria, "Microsoft.VSTS.Common.AcceptanceCriteria")));
        if (area != null && !area.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.AreaPath","value",area));
        if (iteration != null && !iteration.isEmpty()) patch.add(Map.of("op","add","path","/fields/System.IterationPath","value",iteration));

        if (fields != null && !fields.isEmpty()) {
            List<String> fieldPairs = smartSplitByCommaOrSemicolon(fields);
            for (String pair : fieldPairs) {
                if (pair.isBlank() || !pair.contains("=")) continue;
                String[] kv = pair.split("=",2);
                String k = kv[0].trim();
                String v = kv.length > 1 ? kv[1].trim() : "";
                if (k.isEmpty()) continue;
                Object valObj = v;
                if (v.matches("^-?\\d+$")) { try { valObj = Integer.parseInt(v); } catch (NumberFormatException ignored) {} }
                else if (v.matches("^-?\\d+\\.\\d+$")) { try { valObj = Double.parseDouble(v); } catch (NumberFormatException ignored) {} }
                patch.add(Map.of("op","add","path","/fields/"+k,"value",AzureDevOpsRichHtmlHelper.enrichIfHtmlField(k, valObj)));
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
                    diag.append("## Error de validación de campos\n\n");

                    Map<String,Object> fieldsSummary = fieldListHelper.listFields(project, type, false, false, true, apiVersion);
                    java.util.Map<String,Object> globalCache = new java.util.HashMap<>();
                    java.util.Map<String,String> pickItemsCache = new java.util.HashMap<>();

                    for (Object errObj : (java.util.List<?>)ruleErrors) {
                        if (!(errObj instanceof Map)) continue;
                        Map<?,?> err = (Map<?,?>)errObj;
                        Object refObj = err.get("fieldReferenceName");
                        String ref = refObj == null ? "" : refObj.toString();
                        Object msgObj = err.get("errorMessage");
                        String msg = msgObj == null ? "(sin mensaje)" : msgObj.toString();
                        Object flagsObj = err.get("fieldStatusFlags");
                        String flags = flagsObj == null ? "-" : flagsObj.toString();

                        // Determinar si es un campo requerido
                        boolean isRequired = flags.toLowerCase().contains("required");

                        diag.append("### ").append(ref.isEmpty() ? "(campo desconocido)" : ref);
                        if (isRequired) diag.append(" ⚠️ Requerido");
                        diag.append("\n");
                        diag.append(msg);
                        diag.append("\n");

                        // Buscar valores permitidos (picklist)
                        String pickInfo = "";
                        if (!ref.isEmpty() && fieldsSummary != null && fieldsSummary.get("value") instanceof java.util.List) {
                            for (Object fObj : (java.util.List<?>)fieldsSummary.get("value")) {
                                if (!(fObj instanceof Map)) continue;
                                Map<?,?> f = (Map<?,?>)fObj;
                                Object fRefObj = f.get("ref");
                                if (ref.equals(fRefObj == null ? "" : fRefObj.toString())) {
                                    if (f.get("picklistItems") instanceof java.util.List && !((java.util.List<?>)f.get("picklistItems")).isEmpty()) {
                                        java.util.List<?> items = (java.util.List<?>)f.get("picklistItems");
                                        pickInfo = " | Valores: " + String.join(", ", items.stream().map(Object::toString).toArray(String[]::new));
                                    }
                                    break;
                                }
                            }
                        }
                        if (pickInfo.isEmpty() && !ref.isEmpty()) {
                            @SuppressWarnings("unchecked") Map<String,Object> global = (Map<String,Object>)globalCache.computeIfAbsent(ref, r -> fieldsGlobalHelper.getFieldGlobal(r, true));
                            if (global != null) {
                                if (Boolean.TRUE.equals(global.get("isPicklist")) && global.get("picklistId") != null) {
                                    String pid = global.get("picklistId").toString();
                                    if (pickItemsCache.get(pid) == null) {
                                        Map<String,Object> pick = picklistsHelper.fetchPicklist(pid);
                                        Object its = pick.get("items");
                                        if (its instanceof java.util.List && !((java.util.List<?>)its).isEmpty()) {
                                            String itemsJoined = String.join(", ", ((java.util.List<?>)its).stream().map(Object::toString).toArray(String[]::new));
                                            pickItemsCache.put(pid, itemsJoined);
                                            pickInfo = " | Valores: " + itemsJoined;
                                        }
                                    } else {
                                        pickInfo = " | Valores: " + pickItemsCache.get(pid);
                                    }
                                }
                            }
                        }
                        diag.append(pickInfo);
                        diag.append("\n\n");
                    }

                    diag.append("--- Diagnóstico completo ---\n");
                    resp.put("diagnostic", diag.toString());
                }
            }
        }
        return resp;
    }

    /**
     * Divide por coma o punto y coma, respetando valores con comas internas.
     * Soporta ambos formatos:
     *   fields="Field1=val1;Field2=val2" (punto y coma)
     *   fields="Field1=val1,Field2=val2" (coma)
     */
    private List<String> smartSplitByCommaOrSemicolon(String text) {
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
            } else if ((c == ',' || c == ';') && !inQuotes) {
                String remaining = text.substring(i + 1).trim();
                if (equalsCount > 0 && remaining.contains("=")) {
                    int nextEquals = remaining.indexOf('=');
                    String potentialField = remaining.substring(0, nextEquals).trim();
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
