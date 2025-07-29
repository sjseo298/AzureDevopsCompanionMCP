package com.mcp.server.tools.azuredevops.model;

import java.util.*;

/**
 * Manejador de campos obligatorios específicos de Sura para work items.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class SuraWorkItemFieldsHandler {
    
    // Campos universales obligatorios para todos los tipos
    private static final Set<String> UNIVERSAL_REQUIRED_FIELDS = Set.of(
        "System.Title",
        "System.State",
        "System.AreaPath",
        "System.IterationPath"
    );
    
    // Mapeo de tipos de work items a sus campos obligatorios específicos
    private static final Map<String, Set<String>> TYPE_SPECIFIC_REQUIRED_FIELDS = new HashMap<>();
    
    static {
        // Historia
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Historia", Set.of(
            "System.Description",
            "Microsoft.VSTS.Common.AcceptanceCriteria",
            "Custom.TipoDeHistoria",
            "Custom.MigracionDatos",
            "Custom.CumplimientoRegulatorio", 
            "Custom.ControlAutomatico",
            "Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14" // ID de la solución en el APM
        ));
        
                // Historia técnica
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Historia técnica", Set.of(
            "System.Description",
            "Microsoft.VSTS.Common.AcceptanceCriteria",
            "Custom.14858558-3edb-485a-9a52-a38c03c65c62", // Tipo de Historia Técnica
            "Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3", // Migración de datos
            "Custom.Lahistoriacorrespondeauncumplimientoregulatorio", // Cumplimiento regulatorio
            "Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1", // Control automático 
            "Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14" // ID de la solución en el APM
        ));
        
        // Tarea
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Tarea", Set.of(
            "Custom.TipoDeTarea"
        ));
        
        // Subtarea
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Subtarea", Set.of(
            "Custom.TipoDeSubtarea"
        ));
        
        // Bug
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Bug", Set.of(
            "System.Description",
            "Microsoft.VSTS.TCM.ReproSteps",
            "Microsoft.VSTS.Common.ValueArea",
            "Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14", // ID de la solución en el APM
            "Custom.DatosDePrueba",
            "Custom.Bloqueante",
            "Custom.NivelPrueba",
            "Custom.Origen",
            "Custom.EtapaDescubrimiento"
        ));
        
        // Caso de prueba
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Caso de prueba", Set.of(
            "Custom.TipoEjecucion",
            "Custom.Fase",
            "Custom.NivelPrueba"
        ));
        
        // Riesgo
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Riesgo", Set.of(
            "System.Description",
            "Custom.PlanAccion"
        ));
        
        // Proyecto  
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Proyecto", Set.of(
            "Custom.TipoProyecto",
            "Custom.TrenesDominios"
        ));
        
        // Épica
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Épica", Set.of(
            // Solo campos universales
        ));
        
        // Revisión post implantación
        TYPE_SPECIFIC_REQUIRED_FIELDS.put("Revisión post implantación", Set.of(
            "Custom.NumeroOrdenCambio"
        ));
    }
    
    // Valores por defecto para campos específicos basados en los valores exactos de Sura
    private static final Map<String, Object> DEFAULT_VALUES = Map.of(
        "System.State", "New",
        "Microsoft.VSTS.Common.ValueArea", "Business",
        "Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3", "No", // Migración de datos (Si/No)
        "Custom.Lahistoriacorrespondeauncumplimientoregulatorio", "No", // Cumplimiento regulatorio (Si/No)
        "Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1", "No", // Control automático (Si/No)
        "Custom.Bloqueante", "No" // Bloqueante (Si/No)
    );
    
    // Valores válidos para las listas desplegables de Sura
    private static final Map<String, Set<String>> PICKLIST_VALUES = new HashMap<>();
    
    static {
        // Tipo de Historia Técnica
        PICKLIST_VALUES.put("Custom.14858558-3edb-485a-9a52-a38c03c65c62", Set.of(
            "Bug", "Historia Técnica", "Plan de pruebas", "Plan migración de datos", "Pruebas automatizadas"
        ));
        
        // Valores booleanos (Si/No)
        Set<String> booleanValues = Set.of("Si", "No");
        PICKLIST_VALUES.put("Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3", booleanValues); // Migración de datos
        PICKLIST_VALUES.put("Custom.Lahistoriacorrespondeauncumplimientoregulatorio", booleanValues); // Cumplimiento regulatorio
        PICKLIST_VALUES.put("Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1", booleanValues); // Control automático
        PICKLIST_VALUES.put("Custom.Bloqueante", booleanValues); // Bloqueante
        
        // Origen
        PICKLIST_VALUES.put("Custom.Origen", Set.of("Automatizado", "Híbrido", "Manual"));
        
        // Etapa de descubrimiento
        PICKLIST_VALUES.put("Custom.EtapaDescubrimiento", Set.of(
            "Certificación", "Exploración", "Post-implantación", "Regresión"
        ));
    }
    
    /**
     * Valida si un valor es válido para un campo de lista específico.
     * 
     * @param fieldReference nombre de referencia del campo
     * @param value valor a validar
     * @return true si es válido, false en caso contrario
     */
    public static boolean isValidPicklistValue(String fieldReference, String value) {
        // Validación especial para ID de solución en APM (debe ser numérico)
        if (fieldReference.equals("Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14")) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        Set<String> validValues = PICKLIST_VALUES.get(fieldReference);
        
        // Si no es un campo de lista, cualquier valor es válido
        if (validValues == null) {
            return true;
        }
        
        return validValues.contains(value);
    }    /**
     * Obtiene los valores válidos para un campo de lista desplegable.
     * 
     * @param fieldReference nombre de referencia del campo
     * @return conjunto de valores válidos o null si no es un campo de lista
     */
    public static Set<String> getValidPicklistValues(String fieldReference) {
        return PICKLIST_VALUES.get(fieldReference);
    }
    
    /**
     * Valida todos los valores de campos en las operaciones JSON Patch.
     * 
     * @param operations operaciones JSON Patch
     * @return resultado de validación de valores
     */
    public static ValidationResult validateFieldValues(List<Map<String, Object>> operations) {
        Set<String> invalidFields = new HashSet<>();
        
        for (Map<String, Object> operation : operations) {
            String op = (String) operation.get("op");
            String path = (String) operation.get("path");
            Object value = operation.get("value");
            
            if ("add".equals(op) && path != null && path.startsWith("/fields/") && value != null) {
                String fieldName = path.substring("/fields/".length());
                String valueStr = value.toString();
                
                if (fieldName.equals("Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14") && !isValidPicklistValue(fieldName, valueStr)) {
                    invalidFields.add("ID de la solución en el APM (debe ser un valor numérico, valor inválido: " + valueStr + ")");
                } else if (!isValidPicklistValue(fieldName, valueStr)) {
                    invalidFields.add(fieldName + " (valor inválido: " + valueStr + ")");
                }
            }
        }
        
        return new ValidationResult(invalidFields.isEmpty(), invalidFields);
    }

    /**
     * Obtiene todos los campos obligatorios para un tipo de work item.
     * 
     * @param workItemType tipo de work item
     * @return conjunto de campos obligatorios
     */
    public static Set<String> getRequiredFields(String workItemType) {
        Set<String> requiredFields = new HashSet<>(UNIVERSAL_REQUIRED_FIELDS);
        
        Set<String> typeSpecific = TYPE_SPECIFIC_REQUIRED_FIELDS.get(workItemType);
        if (typeSpecific != null) {
            requiredFields.addAll(typeSpecific);
        }
        
        return requiredFields;
    }
    
    /**
     * Valida si un conjunto de operaciones JSON Patch incluye todos los campos obligatorios.
     * 
     * @param workItemType tipo de work item
     * @param operations operaciones JSON Patch
     * @return resultado de validación
     */
    public static ValidationResult validateRequiredFields(String workItemType, List<Map<String, Object>> operations) {
        Set<String> requiredFields = getRequiredFields(workItemType);
        Set<String> providedFields = new HashSet<>();
        
        // Extraer campos de las operaciones
        for (Map<String, Object> operation : operations) {
            String op = (String) operation.get("op");
            String path = (String) operation.get("path");
            
            if ("add".equals(op) && path != null && path.startsWith("/fields/")) {
                String fieldName = path.substring("/fields/".length());
                providedFields.add(fieldName);
            }
        }
        
        // Encontrar campos faltantes
        Set<String> missingFields = new HashSet<>(requiredFields);
        missingFields.removeAll(providedFields);
        
        return new ValidationResult(missingFields.isEmpty(), missingFields);
    }
    
    /**
     * Agrega campos obligatorios faltantes con valores por defecto.
     * 
     * @param workItemType tipo de work item
     * @param operations lista de operaciones a modificar
     * @param userProvidedValues valores específicos del usuario
     * @return operaciones actualizadas con campos obligatorios
     */
    public static List<Map<String, Object>> addMissingRequiredFields(
            String workItemType, 
            List<Map<String, Object>> operations,
            Map<String, Object> userProvidedValues) {
        
        List<Map<String, Object>> updatedOperations = new ArrayList<>(operations);
        ValidationResult validation = validateRequiredFields(workItemType, operations);
        
        if (!validation.isValid()) {
            for (String missingField : validation.getMissingFields()) {
                Object value = userProvidedValues.getOrDefault(missingField, 
                                DEFAULT_VALUES.get(missingField));
                
                if (value != null) {
                    updatedOperations.add(Map.of(
                        "op", "add",
                        "path", "/fields/" + missingField,
                        "value", value
                    ));
                }
            }
        }
        
        return updatedOperations;
    }
    
    /**
     * Obtiene un mapa de campos obligatorios con sus descripciones.
     * 
     * @param workItemType tipo de work item
     * @return mapa de campo a descripción
     */
    public static Map<String, String> getRequiredFieldsWithDescriptions(String workItemType) {
        Map<String, String> fieldDescriptions = new HashMap<>();
        
        fieldDescriptions.put("System.Title", "Título del work item (obligatorio)");
        fieldDescriptions.put("System.State", "Estado del work item (por defecto: New)");
        fieldDescriptions.put("System.AreaPath", "Ruta de área del proyecto");
        fieldDescriptions.put("System.IterationPath", "Ruta de iteración/sprint");
        
        // Descripciones específicas por tipo
        switch (workItemType) {
            case "Historia":
            case "Historia técnica":
                fieldDescriptions.put("System.Description", "Descripción detallada de la historia");
                fieldDescriptions.put("Microsoft.VSTS.Common.AcceptanceCriteria", "Criterios de aceptación");
                fieldDescriptions.put("Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14", "ID de la solución en el APM");
                fieldDescriptions.put("Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3", "¿Hace parte de migración de datos? (Si/No)");
                fieldDescriptions.put("Custom.Lahistoriacorrespondeauncumplimientoregulatorio", "¿Es cumplimiento regulatorio? (Si/No)");
                fieldDescriptions.put("Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1", "¿Es control automático? (Si/No)");
                
                if ("Historia".equals(workItemType)) {
                    fieldDescriptions.put("Custom.TipoDeHistoria", "Tipo de historia (requerido)");
                } else {
                    fieldDescriptions.put("Custom.14858558-3edb-485a-9a52-a38c03c65c62", 
                        "Tipo de historia técnica (Bug, Historia Técnica, Plan de pruebas, Plan migración de datos, Pruebas automatizadas)");
                }
                break;
                
            case "Tarea":
                fieldDescriptions.put("Custom.TipoDeTarea", "Tipo de tarea (requerido)");
                break;
                
            case "Bug":
                fieldDescriptions.put("System.Description", "Descripción del bug");
                fieldDescriptions.put("Microsoft.VSTS.TCM.ReproSteps", "Pasos para reproducir");
                fieldDescriptions.put("Custom.DatosDePrueba", "Datos de prueba utilizados");
                fieldDescriptions.put("Custom.Bloqueante", "¿Es bloqueante? (Si/No)");
                fieldDescriptions.put("Custom.NivelPrueba", "Nivel de prueba donde se encontró");
                fieldDescriptions.put("Custom.Origen", "Origen del bug (Automatizado, Híbrido, Manual)");
                fieldDescriptions.put("Custom.EtapaDescubrimiento", "Etapa de descubrimiento (Certificación, Exploración, Post-implantación, Regresión)");
                break;
        }
        
        return fieldDescriptions;
    }
    
    /**
     * Resultado de validación de campos obligatorios.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final Set<String> missingFields;
        
        public ValidationResult(boolean valid, Set<String> missingFields) {
            this.valid = valid;
            this.missingFields = missingFields;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public Set<String> getMissingFields() {
            return missingFields;
        }
        
        public String getErrorMessage() {
            if (valid) {
                return null;
            }
            return "Campos obligatorios faltantes: " + String.join(", ", missingFields);
        }
    }
}
