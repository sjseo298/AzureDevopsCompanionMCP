package com.mcp.server.services.helpers;

import com.mcp.server.services.AzureDevOpsClientService;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Helper para la operación get_fields del router wit_work_items.
 * Descubre campos requeridos, picklists, campos calculados y tipos disponibles.
 */
@Component
public class WitWorkItemGetFieldsHelper {

    private final AzureDevOpsClientService client;
    private final WitWorkItemTypesListHelper typesListHelper;
    private final WitWorkItemTypesFieldListHelper fieldListHelper;
    private final WitFieldsGlobalGetHelper fieldsGlobalHelper;
    private final WorkitemtrackingprocessPicklistsHelper picklistsHelper;
    private final WitWorkItemTypeStatesListHelper statesListHelper;

    public WitWorkItemGetFieldsHelper(AzureDevOpsClientService client) {
        this.client = client;
        this.typesListHelper = new WitWorkItemTypesListHelper(client);
        this.fieldListHelper = new WitWorkItemTypesFieldListHelper(client);
        this.fieldsGlobalHelper = new WitFieldsGlobalGetHelper(client);
        this.picklistsHelper = new WorkitemtrackingprocessPicklistsHelper(client);
        this.statesListHelper = new WitWorkItemTypeStatesListHelper(client);
    }

    /**
     * Lista los tipos de work item disponibles en un proyecto.
     */
    public Map<String, Object> listTypesForProject(String project, String apiVersion) {
        return typesListHelper.listTypes(project, apiVersion);
    }

    /**
     * Extrae solo los nombres de los tipos de work item de la respuesta de la API.
     */
    public List<String> getTypeNames(Map<String, Object> response) {
        return typesListHelper.getTypeNames(response);
    }

    /**
     * Obtiene campos de un tipo de work item específico.
     */
    public Map<String, Object> getFieldsForType(String project, String type, String apiVersion,
                                                 boolean showPicklistItems, boolean raw) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Listar tipos disponibles
        Map<String, Object> typesResp = typesListHelper.listTypes(project, apiVersion);
        List<String> typeNames = typesListHelper.getTypeNames(typesResp);
        result.put("workItemTypes", typeNames);

        // 2. Obtener campos del tipo específico
        Map<String, Object> fieldsResp = fieldListHelper.listFields(project, type, false, false, showPicklistItems, apiVersion);
        List<Map<String, Object>> allFields = new ArrayList<>();
        List<Map<String, Object>> requiredFields = new ArrayList<>();
        List<Map<String, Object>> picklistFields = new ArrayList<>();
        List<Map<String, Object>> readOnlyFields = new ArrayList<>();
        List<Map<String, Object>> suggestedFields = new ArrayList<>();

        Set<String> requiredRefs = new HashSet<>();
        Set<String> picklistRefs = new HashSet<>();
        Set<String> readOnlyRefs = new HashSet<>();

        if (fieldsResp != null && fieldsResp.get("value") instanceof List) {
            Map<String, Object> globalCache = new HashMap<>();
            Map<String, String> pickItemsCache = new HashMap<>();

            for (Object fObj : (List<?>) fieldsResp.get("value")) {
                if (!(fObj instanceof Map)) continue;
                Map<?, ?> f = (Map<?, ?>) fObj;

                Map<String, Object> enriched = new LinkedHashMap<>();
                Object refObj = f.get("ref");
                String ref = refObj != null ? refObj.toString() : "";
                Object nameObj = f.get("name");
                String name = nameObj != null ? nameObj.toString() : ref;

                enriched.put("referenceName", ref);
                enriched.put("name", name);

                // alwaysRequired
                Object alwaysReqObj = f.get("alwaysRequired");
                boolean alwaysRequired = Boolean.TRUE.equals(alwaysReqObj);
                enriched.put("alwaysRequired", alwaysRequired);
                if (alwaysRequired) requiredRefs.add(ref);

                // readOnly
                Object readOnlyObj = f.get("readOnly");
                boolean readOnly = Boolean.TRUE.equals(readOnlyObj);
                enriched.put("readOnly", readOnly);
                if (readOnly) readOnlyRefs.add(ref);

                // Project type
                enriched.put("projectType", f.get("type"));

                // Enrich with global
                if (!ref.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> global = (Map<String, Object>) globalCache.computeIfAbsent(ref,
                        r -> fieldsGlobalHelper.getFieldGlobal(r, true));
                    if (global != null) {
                        enriched.put("globalType", global.get("type"));
                        enriched.put("globalUsage", global.get("usage"));
                        enriched.put("globalReadOnly", global.get("readOnly"));

                        // Picklist detection
                        if (Boolean.TRUE.equals(global.get("isPicklist")) && global.get("picklistId") != null) {
                            String pid = global.get("picklistId").toString();
                            picklistRefs.add(ref);
                            enriched.put("isPicklist", true);
                            enriched.put("picklistId", pid);

                            if (showPicklistItems) {
                                String itemsStr = pickItemsCache.computeIfAbsent(pid, id -> {
                                    Map<String, Object> pick = picklistsHelper.fetchPicklist(id);
                                    Object items = pick.get("items");
                                    if (items instanceof List && !((List<?>) items).isEmpty()) {
                                        return String.join(" | ", ((List<?>) items).stream().map(Object::toString).toArray(String[]::new));
                                    }
                                    return "";
                                });
                                if (!itemsStr.isBlank()) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> items = (List<Object>) picklistsHelper.fetchPicklist(pid).get("items");
                                    if (items != null) {
                                        enriched.put("picklistItems", items);
                                    }
                                }
                            }
                        }
                    }
                }

                // Check for limitedToValues (inline picklist)
                Object limitedToValues = f.get("limitedToValues");
                if (limitedToValues instanceof List && !((List<?>) limitedToValues).isEmpty()) {
                    if (!picklistRefs.contains(ref)) {
                        picklistRefs.add(ref);
                    }
                    enriched.put("limitedToValues", limitedToValues);
                    enriched.put("isPicklist", true);
                }

                // Suggested fields (system fields that should typically be set)
                if (isSuggestedField(ref)) {
                    suggestedFields.add(enriched);
                }

                allFields.add(enriched);
                if (alwaysRequired) requiredFields.add(enriched);
                if (picklistRefs.contains(ref) && !readOnly) picklistFields.add(enriched);
                if (readOnly) readOnlyFields.add(enriched);
            }
        }

        result.put("type", type);
        result.put("allFields", raw ? allFields : null);
        result.put("requiredFields", requiredFields);
        result.put("requiredFieldRefs", new ArrayList<>(requiredRefs));
        result.put("picklistFields", picklistFields);
        result.put("picklistFieldRefs", new ArrayList<>(picklistRefs));
        result.put("readOnlyFields", readOnlyFields);
        result.put("readOnlyFieldRefs", new ArrayList<>(readOnlyRefs));
        result.put("suggestedFields", suggestedFields);

        // 3. Obtener estados válidos para el tipo
        Map<String, Object> statesResp = statesListHelper.listStates(project, type, apiVersion);
        List<String> validStates = new ArrayList<>();
        if (statesResp != null && statesResp.get("value") instanceof List) {
            for (Object sObj : (List<?>) statesResp.get("value")) {
                if (sObj instanceof Map) {
                    Map<?, ?> s = (Map<?, ?>) sObj;
                    Object stateName = s.get("name");
                    if (stateName != null) {
                        validStates.add(stateName.toString());
                    }
                }
            }
        }
        result.put("validStates", validStates);

        return result;
    }

    /**
     * Campos del sistema que se sugieren configurar al crear un work item.
     */
    private boolean isSuggestedField(String ref) {
        if (ref == null) return false;
        return ref.equals("System.AreaPath") ||
               ref.equals("System.IterationPath") ||
               ref.equals("System.Tags") ||
               ref.equals("System.Description") ||
               ref.equals("Microsoft.VSTS.Common.AcceptanceCriteria");
    }

    /**
     * Verifica si campos proporcionados cubren los requeridos.
     * Devuelve lista de campos faltantes con sus detalles.
     */
    public Map<String, Object> validateRequiredFields(String project, String type, String providedFields, String apiVersion) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> missingRefs = new ArrayList<>();
        List<Map<String, Object>> missingDetails = new ArrayList<>();

        // Obtener campos requeridos
        Map<String, Object> fieldsResp = fieldListHelper.listFields(project, type, true, false, false, apiVersion);
        Set<String> requiredRefs = new HashSet<>();

        if (fieldsResp != null && fieldsResp.get("value") instanceof List) {
            for (Object fObj : (List<?>) fieldsResp.get("value")) {
                if (!(fObj instanceof Map)) continue;
                Map<?, ?> f = (Map<?, ?>) fObj;
                Object alwaysReqObj = f.get("alwaysRequired");
                if (Boolean.TRUE.equals(alwaysReqObj)) {
                    Object refObj = f.get("ref");
                    if (refObj != null) requiredRefs.add(refObj.toString());
                }
            }
        }

        // Extraer campos proporcionados
        Set<String> providedRefNames = new HashSet<>();
        if (providedFields != null && !providedFields.isBlank()) {
            List<String> pairs = smartSplitByCommaOrSemicolon(providedFields);
            for (String pair : pairs) {
                if (pair.contains("=")) {
                    String key = pair.split("=", 2)[0].trim();
                    providedRefNames.add(key);
                }
            }
        }

        // Campos System.Title siempre se proporcionan (son obligatorios en el tool)
        providedRefNames.add("System.Title");

        // Encontrar faltantes
        for (String ref : requiredRefs) {
            if (!providedRefNames.contains(ref)) {
                missingRefs.add(ref);
            }
        }

        // Si hay faltantes, obtener detalles
        if (!missingRefs.isEmpty()) {
            Map<String, Object> fullFieldsResp = fieldListHelper.listFields(project, type, false, false, true, apiVersion);
            Map<String, Object> globalCache = new HashMap<>();
            Map<String, String> pickItemsCache = new HashMap<>();

            if (fullFieldsResp != null && fullFieldsResp.get("value") instanceof List) {
                for (String missingRef : missingRefs) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("referenceName", missingRef);
                    detail.put("missing", true);

                    // Buscar en la lista de campos
                    for (Object fObj : (List<?>) fullFieldsResp.get("value")) {
                        if (!(fObj instanceof Map)) continue;
                        Map<?, ?> f = (Map<?, ?>) fObj;
                        Object refObj = f.get("ref");
                        if (missingRef.equals(refObj == null ? "" : refObj.toString())) {
                            detail.put("name", f.get("name"));
                            detail.put("projectType", f.get("type"));

                            // Enrich with global
                            @SuppressWarnings("unchecked")
                            Map<String, Object> global = (Map<String, Object>) globalCache.computeIfAbsent(missingRef,
                                r -> fieldsGlobalHelper.getFieldGlobal(r, true));
                            if (global != null) {
                                detail.put("globalType", global.get("type"));
                                if (Boolean.TRUE.equals(global.get("isPicklist")) && global.get("picklistId") != null) {
                                    String pid = global.get("picklistId").toString();
                                    detail.put("isPicklist", true);
                                    detail.put("picklistId", pid);
                                    if (pickItemsCache.get(pid) == null) {
                                        Map<String, Object> pick = picklistsHelper.fetchPicklist(pid);
                                        Object items = pick.get("items");
                                        if (items instanceof List && !((List<?>) items).isEmpty()) {
                                            List<String> itemStrs = ((List<?>) items).stream().map(Object::toString).toList();
                                            detail.put("picklistItems", itemStrs);
                                            pickItemsCache.put(pid, String.join(" | ", itemStrs));
                                        }
                                    } else {
                                        detail.put("picklistItems", pickItemsCache.get(pid));
                                    }
                                }
                            }

                            // Check limitedToValues
                            Object limitedToValues = f.get("limitedToValues");
                            if (limitedToValues instanceof List && !((List<?>) limitedToValues).isEmpty()) {
                                detail.put("isPicklist", true);
                                detail.put("limitedToValues", limitedToValues);
                            }
                            break;
                        }
                    }
                    missingDetails.add(detail);
                }
            }
        }

        result.put("type", type);
        result.put("requiredFieldRefs", new ArrayList<>(requiredRefs));
        result.put("providedFieldRefs", new ArrayList<>(providedRefNames));
        result.put("missingRequired", missingRefs.isEmpty());
        result.put("missingFieldRefs", missingRefs);
        result.put("missingDetails", missingDetails);

        return result;
    }

    /**
     * Divide por coma o punto y coma, respetando valores con comas internas.
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
