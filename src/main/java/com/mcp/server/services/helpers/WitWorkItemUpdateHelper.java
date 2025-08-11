package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.http.MediaType;

import java.util.*;

/**
 * Helper para actualizar work items replicando la lógica avanzada del script bash work_items_update.sh
 * Características soportadas:
 *  - Operaciones JSON Patch: add / replace / remove
 *  - Atajos: state, title, description, area, iteration
 *  - Herencia de AreaPath / IterationPath desde el padre (--parent) si no se especifican
 *  - Re-parenting: relación System.LinkTypes.Hierarchy-Reverse
 *  - Relaciones adicionales tipo:id[:comentario] con de-duplicación
 *  - Numerización de valores (int/double) cuando aplica
 *  - Flags: validateOnly, bypassRules, suppressNotifications, debug, noDiagnostic, raw
 *  - Diagnóstico enriquecido de RuleValidationErrors (tipo de campo, picklists, estados válidos para System.State, sugerencias)
 */
public class WitWorkItemUpdateHelper {
    private final AzureDevOpsClientService client;
    private final WitWorkItemGetHelper getHelper;
    private final WitWorkItemTypesFieldListHelper fieldListHelper;
    private final WitFieldsGlobalGetHelper fieldsGlobalHelper;
    private final WorkitemtrackingprocessPicklistsHelper picklistsHelper;

    public WitWorkItemUpdateHelper(AzureDevOpsClientService client) {
        this.client = client;
        this.getHelper = new WitWorkItemGetHelper(client);
        this.fieldListHelper = new WitWorkItemTypesFieldListHelper(client);
        this.fieldsGlobalHelper = new WitFieldsGlobalGetHelper(client);
        this.picklistsHelper = new WorkitemtrackingprocessPicklistsHelper(client);
    }

    public Map<String,Object> update(Map<String,Object> args) {
        String project = Objects.toString(args.get("project"), "");
        // Alias de parámetros: parent / parentId, api-version / apiVersion
        if (!args.containsKey("parentId") && args.get("parent") != null) {
            args.put("parentId", args.get("parent"));
        }
        Object apiVerAlias = args.get("api-version");
        if (apiVerAlias != null && !args.containsKey("apiVersion")) args.put("apiVersion", apiVerAlias);
        int id = Integer.parseInt(Objects.toString(args.get("id")));
        String apiVersion = Objects.toString(args.getOrDefault("apiVersion", "7.2-preview"));
        String add = opt(args, "add");
        String replace = opt(args, "replace");
        String remove = opt(args, "remove");
        String state = opt(args, "state");
        String title = opt(args, "title");
        String description = opt(args, "description");
        String area = opt(args, "area");
        String iteration = opt(args, "iteration");
        Integer parentId = args.get("parentId") != null ? Integer.valueOf(args.get("parentId").toString()) : null;
        String relations = opt(args, "relations");
        boolean validateOnly = bool(args, "validateOnly");
        boolean bypassRules = bool(args, "bypassRules");
        boolean suppressNotifications = bool(args, "suppressNotifications");
        boolean debug = bool(args, "debug");
        boolean noDiagnostic = bool(args, "noDiagnostic");

        List<Map<String,Object>> patch = new ArrayList<>();

        // Atajos (script usa op add para todos)
        if (state != null && !state.isEmpty()) patch.add(addField("System.State", state));
        if (title != null && !title.isEmpty()) patch.add(addField("System.Title", title));
        if (description != null && !description.isEmpty()) patch.add(addField("System.Description", description));
        if (area != null && !area.isEmpty()) patch.add(addField("System.AreaPath", area));
        if (iteration != null && !iteration.isEmpty()) patch.add(addField("System.IterationPath", iteration));

        // add / replace lists
        if (add != null && !add.isBlank()) parseKvList(add, "add", patch);
        if (replace != null && !replace.isBlank()) parseKvList(replace, "replace", patch);

        // remove list
        if (remove != null && !remove.isBlank()) {
            for (String f : remove.split(",")) {
                if (f == null || f.isBlank()) continue;
                patch.add(Map.of("op", "remove", "path", "/fields/" + f.trim()));
            }
        }

        // Parent inheritance + relation
        if (parentId != null) {
            Map<String,Object> parent = getHelper.getWorkItem(project, parentId, "System.AreaPath,System.IterationPath", apiVersion);
            if (parent != null && parent.get("fields") instanceof Map) {
                Map<?,?> fm = (Map<?,?>)parent.get("fields");
                if ((area == null || area.isBlank()) && fm.get("System.AreaPath") != null) {
                    patch.add(addField("System.AreaPath", fm.get("System.AreaPath").toString()));
                }
                if ((iteration == null || iteration.isBlank()) && fm.get("System.IterationPath") != null) {
                    patch.add(addField("System.IterationPath", fm.get("System.IterationPath").toString()));
                }
            }
            if (parent != null && parent.get("url") != null) {
                String pUrl = parent.get("url").toString();
                patch.add(Map.of(
                    "op","add",
                    "path","/relations/-",
                    "value", Map.of("rel","System.LinkTypes.Hierarchy-Reverse","url", pUrl)
                ));
            }
        }

        // Additional relations
        if (relations != null && !relations.isBlank()) {
            Set<String> seen = new HashSet<>();
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
                Map<String,Object> relWi = getHelper.getWorkItem(project, Integer.parseInt(relId), null, apiVersion);
                if (relWi == null || relWi.get("url") == null) continue;
                Map<String,Object> relObj = new LinkedHashMap<>();
                relObj.put("rel", relType);
                relObj.put("url", relWi.get("url"));
                if (comment != null && !comment.isEmpty()) relObj.put("attributes", Map.of("comment", comment));
                patch.add(Map.of("op","add","path","/relations/-","value", relObj));
            }
        }

        if (debug) {
            System.err.println("--- JSON PATCH (debug) ---");
            System.err.println(patch);
            System.err.println("--------------------------");
        }

        Map<String,String> query = new LinkedHashMap<>();
        query.put("api-version", apiVersion);
        if (validateOnly) query.put("validateOnly", "true");
        if (bypassRules) query.put("bypassRules", "true");
        if (suppressNotifications) query.put("suppressNotifications", "true");

    Map<String,Object> resp = client.patchWitApiWithQuery(project, null, "workitems/"+id, query, patch, apiVersion, MediaType.valueOf("application/json-patch+json"));

    if (!noDiagnostic && resp != null && resp.containsKey("customProperties")) {
            Object cp = resp.get("customProperties");
            if (cp instanceof Map) {
                Object ruleErrors = ((Map<?,?>)cp).get("RuleValidationErrors");
                if (ruleErrors instanceof List && !((List<?>)ruleErrors).isEmpty()) {
                    String workItemType = extractWorkItemType(resp);
                    if (workItemType == null || workItemType.isBlank()) {
                        Map<String,Object> existing = getHelper.getWorkItem(project, id, "System.WorkItemType", apiVersion);
                        if (existing != null && existing.get("fields") instanceof Map) {
                            Object wt = ((Map<?,?>)existing.get("fields")).get("System.WorkItemType");
                            if (wt != null) workItemType = wt.toString();
                        }
                    }
                    StringBuilder diag = new StringBuilder();
                    diag.append("--- Validación de campos con error ---\n");
                    Map<String,Object> fieldsSummary = workItemType != null ? fieldListHelper.listFields(project, workItemType, true, true, true, apiVersion) : null;
                    List<String> stateValues = workItemType != null ? fetchStates(project, workItemType, apiVersion) : List.of();
                    Map<String,Object> globalCache = new HashMap<>();
                    Map<String,String> pickCache = new HashMap<>();
                    Set<String> requiredSuggestions = new HashSet<>();
                    for (Object errObj : (List<?>)ruleErrors) {
                        if (!(errObj instanceof Map)) continue;
                        Map<?,?> err = (Map<?,?>)errObj;
                        String ref = Objects.toString(err.get("fieldReferenceName"), "");
                        String msg = Objects.toString(err.get("errorMessage"), "(sin mensaje)");
                        String flags = Objects.toString(err.get("fieldStatusFlags"), "-");
                        String typeInfo = ""; String pickInfo = ""; String validStatesInfo = "";
                        if (!ref.isEmpty() && fieldsSummary != null && fieldsSummary.get("value") instanceof List) {
                            for (Object fObj : (List<?>)fieldsSummary.get("value")) {
                                if (!(fObj instanceof Map)) continue;
                                Map<?,?> f = (Map<?,?>)fObj;
                                if (ref.equals(Objects.toString(f.get("ref"), ""))) {
                                    Object gt = f.get("globalType"); if (gt == null) gt = f.get("projectType");
                                    if (gt != null) typeInfo = " | type: " + gt;
                                    Object pickItems = f.get("picklistItems");
                                    if (pickItems instanceof List && !((List<?>)pickItems).isEmpty()) {
                                        pickInfo = " | vals: " + String.join(" | ", ((List<?>)pickItems).stream().map(Object::toString).toArray(String[]::new));
                                    }
                                    break;
                                }
                            }
                        }
                        if ((typeInfo.isEmpty() || pickInfo.isEmpty()) && !ref.isEmpty()) {
                            Map<String,Object> global = (Map<String,Object>)globalCache.computeIfAbsent(ref, r -> fieldsGlobalHelper.getFieldGlobal(r, true));
                            if (global != null) {
                                if (typeInfo.isEmpty() && global.get("type") != null) typeInfo = " | type: " + global.get("type");
                                if (pickInfo.isEmpty() && Boolean.TRUE.equals(global.get("isPicklist")) && global.get("picklistId") != null) {
                                    String pid = global.get("picklistId").toString();
                                    String itemsJoined = pickCache.computeIfAbsent(pid, idp -> {
                                        Map<String,Object> pick = picklistsHelper.fetchPicklist(pid);
                                        Object its = pick.get("items");
                                        if (its instanceof List && !((List<?>)its).isEmpty()) {
                                            return String.join(" | ", ((List<?>)its).stream().map(Object::toString).toArray(String[]::new));
                                        }
                                        return "";
                                    });
                                    if (!itemsJoined.isBlank()) pickInfo = " | vals: " + itemsJoined;
                                }
                            }
                        }
                        if ("System.State".equals(ref) && (flags.contains("limitedToValues") || flags.contains("invalidListValue") || flags.contains("hasValues"))) {
                            if (!stateValues.isEmpty()) {
                                validStatesInfo = " | valid: " + String.join(" | ", stateValues);
                            }
                        }
                        if (flags.toLowerCase().contains("required")) requiredSuggestions.add(ref);
                        diag.append("* ").append(ref.isEmpty()?"(sin ref)":ref).append(" -> ").append(msg)
                            .append(" | flags: ").append(flags).append(typeInfo).append(pickInfo).append(validStatesInfo).append("\n");
                    }
                    if (!requiredSuggestions.isEmpty()) {
                        diag.append("Sugerencias (--add/--replace):\n");
                        for (String r : requiredSuggestions) diag.append("  --add ").append(r).append("=<valor>\n");
                    }
                    diag.append("--- Fin validación detallada ---\n");
                    // Si ya existe algún diagnóstico previo, concatenar
                    Object existing = resp.get("diagnostic");
                    if (existing instanceof String && !((String) existing).isBlank()) {
                        resp.put("diagnostic", existing + diag.toString());
                    } else {
                        resp.put("diagnostic", diag.toString());
                    }
                }
            }
        }
        // Diagnóstico extendido adicional (lista de campos requeridos) si hubo mensaje de error aunque no haya RuleValidationErrors
        if (!noDiagnostic && resp != null && resp.get("message") != null) {
            String workItemType = extractWorkItemType(resp);
            if ((workItemType == null || workItemType.isBlank())) {
                try {
                    Map<String,Object> existing = getHelper.getWorkItem(project, id, "System.WorkItemType", apiVersion);
                    if (existing != null && existing.get("fields") instanceof Map) {
                        Object wt = ((Map<?,?>)existing.get("fields")).get("System.WorkItemType");
                        if (wt != null) workItemType = wt.toString();
                    }
                } catch (Exception ignored) {}
            }
            if (workItemType != null && !workItemType.isBlank()) {
                try {
                    Map<String,Object> fieldsSummary = fieldListHelper.listFields(project, workItemType, false, false, true, apiVersion);
                    Object val = fieldsSummary != null ? fieldsSummary.get("value") : null;
                    if (val instanceof List<?> list && !list.isEmpty()) {
                        StringBuilder extra = new StringBuilder();
                        extra.append("--- Diagnóstico de campos requeridos (resumen) ---\n");
                        for (Object fo : list) {
                            if (!(fo instanceof Map<?,?> fm)) continue;
                            Object always = fm.get("alwaysRequired");
                            if (Boolean.TRUE.equals(always)) {
                                Object ref = fm.get("referenceName");
                                Object name = fm.get("name");
                                Object help = fm.get("helpText");
                                extra.append("* ").append(ref!=null?ref:"(sin ref)");
                                if (name != null) extra.append(" - ").append(name);
                                if (help != null) {
                                    String hs = help.toString();
                                    if (hs.length() > 90) hs = hs.substring(0,87) + "...";
                                    extra.append(" | ").append(hs);
                                }
                                Object pick = fm.get("picklistItems");
                                if (pick instanceof List<?> pl && !pl.isEmpty()) {
                                    extra.append(" | vals: ").append(String.join(" | ", pl.stream().map(Object::toString).toArray(String[]::new)));
                                }
                                extra.append("\n");
                            }
                        }
                        extra.append("--- Fin diagnóstico ---\n");
                        Object existing = resp.get("diagnostic");
                        if (existing instanceof String && !((String) existing).isBlank()) {
                            resp.put("diagnostic", existing + extra.toString());
                        } else {
                            resp.put("diagnostic", extra.toString());
                        }
                    }
                } catch (Exception ignored) { /* best effort */ }
            }
        }
        return resp;
    }

    private static String opt(Map<String,Object> m, String k) { Object v = m.get(k); return v == null? null : v.toString(); }
    private static boolean bool(Map<String,Object> m, String k) { return Boolean.TRUE.equals(m.get(k)); }

    private Map<String,Object> addField(String ref, Object value) {
        return Map.of("op","add","path","/fields/"+ref,"value", value);
    }

    private void parseKvList(String list, String op, List<Map<String,Object>> patch) {
        for (String entry : list.split(",")) {
            if (entry.isBlank() || !entry.contains("=")) continue;
            String[] kv = entry.split("=",2);
            String k = kv[0].trim(); String v = kv[1].trim(); if (k.isEmpty()) continue;
            Object val = v;
            if (v.matches("^-?\\d+$")) { try { val = Integer.parseInt(v); } catch (NumberFormatException ignored) {} }
            else if (v.matches("^-?\\d+\\.\\d+$")) { try { val = Double.parseDouble(v); } catch (NumberFormatException ignored) {} }
            patch.add(Map.of("op", op, "path", "/fields/"+k, "value", val));
        }
    }

    private String extractWorkItemType(Map<String,Object> resp) {
        Object fields = resp.get("fields");
        if (fields instanceof Map<?,?> fm) {
            Object t = fm.get("System.WorkItemType");
            if (t != null) return t.toString();
        }
        return null;
    }

    private List<String> fetchStates(String project, String type, String apiVersion) {
        try {
            Map<String,Object> states = client.getWitApi(project, null, "workitemtypes/"+type+"/states");
            Object val = states.get("value");
            if (val instanceof List<?> list) {
                List<String> names = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof Map<?,?> m) {
                        Object name = m.get("name");
                        if (name != null) names.add(name.toString());
                    }
                }
                return names;
            }
        } catch (Exception ignored) {}
        return List.of();
    }
}
