package com.mcp.server.utils;

import java.util.*;

/**
 * Utilidad para validación rápida de work items antes de creación
 * Evita errores comunes y acelera el proceso de creación
 */
public class WorkItemValidationUtils {
    
    // Campos obligatorios por tipo de work item
    private static final Map<String, List<String>> REQUIRED_FIELDS = new HashMap<String, List<String>>() {{
        put("Historia técnica", List.of("title", "acceptanceCriteria", "tipoHistoriaTecnica", 
                                      "idSolucionAPM", "migracionDatos", "cumplimientoRegulatorio", "controlAutomatico"));
        put("Historia", List.of("title", "acceptanceCriteria", "tipoHistoria", 
                               "idSolucionAPM", "migracionDatos", "cumplimientoRegulatorio", "controlAutomatico"));
        put("Bug", List.of("title", "reproSteps", "idSolucionAPM", "bloqueante", 
                          "nivelPrueba", "origen", "etapaDescubrimiento"));
        put("Proyecto", List.of("title", "tipoProyecto", "trenesYDominios"));
        put("Tarea", List.of("title", "tipoTarea"));
        put("Caso de prueba", List.of("title", "tipoEjecucion", "fase", "nivelPrueba"));
        put("Subtarea", List.of("title", "tipoSubtarea"));
        put("Revisión post implantación", List.of("title", "nroOrdenCambio"));
        put("Riesgo", List.of("title", "description", "planAccion"));
        put("Épica", List.of("title"));
        put("Feature", List.of("title"));
    }};
    
    // Valores por defecto para campos comunes
    private static final Map<String, Object> DEFAULT_VALUES = new HashMap<String, Object>() {{
        put("priority", 2);
        put("state", "New");
        put("migracionDatos", false);
        put("cumplimientoRegulatorio", false);
        put("controlAutomatico", false);
        put("bloqueante", false);
        put("idSolucionAPM", "APM-TEMP-001");
    }};
    
    // Tipos de work item recomendados para pruebas rápidas (menos campos obligatorios)
    private static final List<String> FAST_TYPES = List.of("Feature", "Épica", "Tarea");
    
    /**
     * Valida que un work item tiene todos los campos obligatorios
     */
    public static ValidationResult validateWorkItem(String type, Map<String, Object> fields) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validar tipo
        if (!REQUIRED_FIELDS.containsKey(type)) {
            errors.add("Tipo de work item no válido: " + type);
            return new ValidationResult(false, errors, warnings);
        }
        
        // Validar campos obligatorios
        List<String> requiredFields = REQUIRED_FIELDS.get(type);
        for (String field : requiredFields) {
            if (!fields.containsKey(field) || fields.get(field) == null || 
                fields.get(field).toString().trim().isEmpty()) {
                errors.add("Campo obligatorio faltante: " + field);
            }
        }
        
        // Validaciones específicas
        validateSpecificRules(type, fields, errors, warnings);
        
        // Sugerir optimizaciones
        if (!FAST_TYPES.contains(type)) {
            warnings.add("Considera usar '" + FAST_TYPES.get(0) + "' para pruebas rápidas (menos campos obligatorios)");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Aplica valores por defecto a campos faltantes
     */
    public static Map<String, Object> applyDefaults(String type, Map<String, Object> fields) {
        Map<String, Object> result = new HashMap<>(fields);
        
        // Aplicar valores por defecto comunes
        for (Map.Entry<String, Object> entry : DEFAULT_VALUES.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Aplicar valores por defecto específicos por tipo
        applyTypeSpecificDefaults(type, result);
        
        return result;
    }
    
    /**
     * Sugiere el tipo de work item más rápido para crear
     */
    public static String suggestFastType(String purpose) {
        if (purpose != null && purpose.toLowerCase().contains("prueba")) {
            return "Feature";
        } else if (purpose != null && purpose.toLowerCase().contains("agrup")) {
            return "Épica";
        } else {
            return "Tarea";
        }
    }
    
    private static void validateSpecificRules(String type, Map<String, Object> fields, 
                                            List<String> errors, List<String> warnings) {
        switch (type) {
            case "Historia técnica":
                validateHistoriaTecnica(fields, errors, warnings);
                break;
            case "Bug":
                validateBug(fields, errors, warnings);
                break;
            case "Revisión post implantación":
                validateRevisionPost(fields, errors, warnings);
                break;
        }
    }
    
    private static void validateHistoriaTecnica(Map<String, Object> fields, 
                                              List<String> errors, List<String> warnings) {
        // Validar ID APM para remediación
        String tipo = (String) fields.get("tipoHistoriaTecnica");
        if ("Remediación".equals(tipo)) {
            String idAPM = (String) fields.get("idSolucionAPM");
            if (idAPM == null || "APM-TEMP-001".equals(idAPM)) {
                warnings.add("Para remediaciones, especifica un ID APM real en lugar del temporal");
            }
        }
    }
    
    private static void validateBug(Map<String, Object> fields, List<String> errors, List<String> warnings) {
        // Validar nivel de prueba para bugs de testing
        String etapa = (String) fields.get("etapaDescubrimiento");
        if ("Testing".equals(etapa) && !fields.containsKey("nivelPrueba")) {
            errors.add("Los bugs encontrados en testing deben especificar el nivel de prueba");
        }
    }
    
    private static void validateRevisionPost(Map<String, Object> fields, List<String> errors, List<String> warnings) {
        // Validar formato de orden de cambio
        String nroOrden = (String) fields.get("nroOrdenCambio");
        if (nroOrden != null && !nroOrden.matches("^CHG\\d{6}$")) {
            errors.add("El número de orden de cambio debe seguir el formato CHG######");
        }
    }
    
    private static void applyTypeSpecificDefaults(String type, Map<String, Object> fields) {
        switch (type) {
            case "Historia técnica":
                if (!fields.containsKey("tipoHistoriaTecnica")) {
                    fields.put("tipoHistoriaTecnica", "Remediación");
                }
                break;
            case "Historia":
                if (!fields.containsKey("tipoHistoria")) {
                    fields.put("tipoHistoria", "Funcional");
                }
                break;
            case "Bug":
                if (!fields.containsKey("origen")) {
                    fields.put("origen", "Manual");
                }
                if (!fields.containsKey("nivelPrueba")) {
                    fields.put("nivelPrueba", "Sistema");
                }
                if (!fields.containsKey("etapaDescubrimiento")) {
                    fields.put("etapaDescubrimiento", "Testing");
                }
                break;
            case "Proyecto":
                if (!fields.containsKey("tipoProyecto")) {
                    fields.put("tipoProyecto", "Remediación");
                }
                if (!fields.containsKey("trenesYDominios")) {
                    fields.put("trenesYDominios", "Aseguramiento");
                }
                break;
            case "Tarea":
                if (!fields.containsKey("tipoTarea")) {
                    fields.put("tipoTarea", "Desarrollo");
                }
                break;
        }
    }
    
    /**
     * Resultado de validación
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!valid) {
                sb.append("❌ ERRORES:\n");
                errors.forEach(error -> sb.append("  • ").append(error).append("\n"));
            }
            if (!warnings.isEmpty()) {
                sb.append("⚠️ ADVERTENCIAS:\n");
                warnings.forEach(warning -> sb.append("  • ").append(warning).append("\n"));
            }
            if (valid && warnings.isEmpty()) {
                sb.append("✅ Validación exitosa");
            }
            return sb.toString();
        }
    }
}
