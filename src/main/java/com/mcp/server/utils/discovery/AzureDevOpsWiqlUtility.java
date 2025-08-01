package com.mcp.server.utils.discovery;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.model.WiqlQueryResult;

import java.util.*;

/**
 * Utilidad especializada para construcción y ejecución de consultas WIQL en Azure DevOps.
 * Centraliza patrones comunes de consulta y generación dinámica de WIQL.
 */
public class AzureDevOpsWiqlUtility {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    public AzureDevOpsWiqlUtility(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    /**
     * Ejecuta una consulta WIQL y retorna los resultados.
     */
    public WiqlQueryResult executeWiqlQuery(String project, String team, String query) {
        try {
            return azureDevOpsClient.executeWiqlQuery(project, team, query);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Construye consulta WIQL para descubrir valores únicos de un campo específico.
     */
    public String buildWiqlQueryForFieldDiscovery(String workItemType, String fieldReferenceName, int maxResults) {
        if (workItemType == null || fieldReferenceName == null) {
            return null;
        }
        
        return String.format(
            "SELECT [System.Id], [%s] FROM WorkItems WHERE [System.WorkItemType] = '%s' AND [%s] <> '' ORDER BY [System.ChangedDate] DESC",
            fieldReferenceName, 
            workItemType, 
            fieldReferenceName
        );
    }
    
    /**
     * Construye consulta WIQL para obtener work items de muestra de un tipo específico.
     */
    public String buildSampleWorkItemsQuery(String workItemType, int maxResults) {
        if (workItemType == null) {
            return "SELECT [System.Id] FROM WorkItems ORDER BY [System.Id] DESC";
        }
        
        return String.format(
            "SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType] FROM WorkItems WHERE [System.WorkItemType] = '%s' ORDER BY [System.ChangedDate] DESC",
            workItemType
        );
    }
    
    /**
     * Construye consulta WIQL para buscar work items con campos personalizados específicos.
     */
    public String buildCustomFieldSearchQuery(String workItemType, List<String> customFields) {
        if (customFields == null || customFields.isEmpty()) {
            return buildSampleWorkItemsQuery(workItemType, 50);
        }
        
        StringBuilder query = new StringBuilder("SELECT [System.Id], [System.Title]");
        
        // Agregar campos personalizados al SELECT
        for (String field : customFields) {
            query.append(", [").append(field).append("]");
        }
        
        query.append(" FROM WorkItems WHERE ");
        
        // Agregar filtro por tipo si se especifica
        if (workItemType != null) {
            query.append("[System.WorkItemType] = '").append(workItemType).append("'");
        } else {
            query.append("1 = 1");
        }
        
        // Agregar condiciones para que al menos uno de los campos personalizados tenga valor
        if (customFields.size() == 1) {
            query.append(" AND [").append(customFields.get(0)).append("] <> ''");
        } else if (customFields.size() > 1) {
            query.append(" AND (");
            for (int i = 0; i < customFields.size(); i++) {
                if (i > 0) query.append(" OR ");
                query.append("[").append(customFields.get(i)).append("] <> ''");
            }
            query.append(")");
        }
        
        query.append(" ORDER BY [System.ChangedDate] DESC");
        
        return query.toString();
    }
    
    /**
     * Construye consulta WIQL para validar la existencia de campos en work items.
     */
    public String buildFieldValidationQuery(String workItemType, List<String> fieldsToValidate) {
        if (fieldsToValidate == null || fieldsToValidate.isEmpty()) {
            return buildSampleWorkItemsQuery(workItemType, 10);
        }
        
        StringBuilder query = new StringBuilder("SELECT [System.Id]");
        
        // Agregar todos los campos a validar
        for (String field : fieldsToValidate) {
            query.append(", [").append(field).append("]");
        }
        
        query.append(" FROM WorkItems WHERE ");
        
        if (workItemType != null) {
            query.append("[System.WorkItemType] = '").append(workItemType).append("'");
        } else {
            query.append("1 = 1");
        }
        
        query.append(" ORDER BY [System.Id] DESC");
        
        return query.toString();
    }
    
    /**
     * Construye consulta WIQL para obtener distribución de estados por tipo de work item.
     */
    public String buildStateDistributionQuery(String workItemType) {
        if (workItemType == null) {
            return "SELECT [System.Id], [System.State], [System.WorkItemType] FROM WorkItems ORDER BY [System.State]";
        }
        
        return String.format(
            "SELECT [System.Id], [System.State], [System.Title] FROM WorkItems WHERE [System.WorkItemType] = '%s' ORDER BY [System.State], [System.ChangedDate] DESC",
            workItemType
        );
    }
    
    /**
     * Construye consulta WIQL para análisis de campos con valores nulos/vacíos.
     */
    public String buildEmptyFieldAnalysisQuery(String workItemType, String fieldReferenceName) {
        if (fieldReferenceName == null) {
            return buildSampleWorkItemsQuery(workItemType, 20);
        }
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT [System.Id], [System.Title], [").append(fieldReferenceName).append("]");
        query.append(" FROM WorkItems WHERE ");
        
        if (workItemType != null) {
            query.append("[System.WorkItemType] = '").append(workItemType).append("' AND ");
        }
        
        query.append("([").append(fieldReferenceName).append("] = '' OR [").append(fieldReferenceName).append("] IS NULL)");
        query.append(" ORDER BY [System.ChangedDate] DESC");
        
        return query.toString();
    }
    
    /**
     * Construye consulta WIQL para obtener historial de cambios recientes.
     */
    public String buildRecentChangesQuery(String workItemType, int daysBack) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT [System.Id], [System.Title], [System.ChangedBy], [System.ChangedDate]");
        query.append(" FROM WorkItems WHERE ");
        
        if (workItemType != null) {
            query.append("[System.WorkItemType] = '").append(workItemType).append("' AND ");
        }
        
        query.append("[System.ChangedDate] >= @Today - ").append(daysBack);
        query.append(" ORDER BY [System.ChangedDate] DESC");
        
        return query.toString();
    }
    
    /**
     * Construye consulta WIQL flexible basada en criterios múltiples.
     */
    public String buildFlexibleQuery(WiqlQueryBuilder builder) {
        if (builder == null) {
            return buildSampleWorkItemsQuery(null, 50);
        }
        
        StringBuilder query = new StringBuilder("SELECT ");
        
        // Construir SELECT
        if (builder.selectFields.isEmpty()) {
            query.append("[System.Id], [System.Title]");
        } else {
            for (int i = 0; i < builder.selectFields.size(); i++) {
                if (i > 0) query.append(", ");
                query.append("[").append(builder.selectFields.get(i)).append("]");
            }
        }
        
        query.append(" FROM WorkItems");
        
        // Construir WHERE
        if (!builder.conditions.isEmpty()) {
            query.append(" WHERE ");
            for (int i = 0; i < builder.conditions.size(); i++) {
                if (i > 0) query.append(" AND ");
                query.append(builder.conditions.get(i));
            }
        }
        
        // Construir ORDER BY
        if (!builder.orderBy.isEmpty()) {
            query.append(" ORDER BY ");
            for (int i = 0; i < builder.orderBy.size(); i++) {
                if (i > 0) query.append(", ");
                query.append(builder.orderBy.get(i));
            }
        } else {
            query.append(" ORDER BY [System.ChangedDate] DESC");
        }
        
        return query.toString();
    }
    
    /**
     * Sanitiza valores para uso seguro en consultas WIQL.
     */
    public String sanitizeWiqlValue(String value) {
        if (value == null) {
            return "''";
        }
        
        // Escapar comillas simples duplicándolas
        String sanitized = value.replace("'", "''");
        
        // Si contiene espacios o caracteres especiales, envolver en comillas
        if (sanitized.contains(" ") || sanitized.contains("'") || sanitized.contains("\"")) {
            return "'" + sanitized + "'";
        }
        
        return sanitized;
    }
    
    /**
     * Valida sintaxis básica de una consulta WIQL.
     */
    public WiqlValidationResult validateWiqlQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new WiqlValidationResult(false, "Consulta vacía", Collections.emptyList());
        }
        
        List<String> warnings = new ArrayList<>();
        String upperQuery = query.toUpperCase();
        
        // Validaciones básicas
        if (!upperQuery.contains("SELECT")) {
            return new WiqlValidationResult(false, "Consulta debe contener SELECT", warnings);
        }
        
        if (!upperQuery.contains("FROM WORKITEMS")) {
            return new WiqlValidationResult(false, "Consulta debe contener FROM WorkItems", warnings);
        }
        
        // Advertencias comunes
        if (!upperQuery.contains("ORDER BY")) {
            warnings.add("Se recomienda incluir ORDER BY para resultados consistentes");
        }
        
        if (upperQuery.contains("SELECT *")) {
            warnings.add("SELECT * puede ser lento, considere especificar campos específicos");
        }
        
        if (upperQuery.contains("WHERE") && !upperQuery.contains("[SYSTEM.WORKITEMTYPE]")) {
            warnings.add("Considere filtrar por tipo de work item para mejor rendimiento");
        }
        
        return new WiqlValidationResult(true, "Consulta válida", warnings);
    }
    
    // ========================================================================
    // CLASES DE APOYO
    // ========================================================================
    
    /**
     * Builder para construir consultas WIQL complejas de forma fluida.
     */
    public static class WiqlQueryBuilder {
        private final List<String> selectFields = new ArrayList<>();
        private final List<String> conditions = new ArrayList<>();
        private final List<String> orderBy = new ArrayList<>();
        
        public WiqlQueryBuilder select(String... fields) {
            Collections.addAll(selectFields, fields);
            return this;
        }
        
        public WiqlQueryBuilder where(String condition) {
            conditions.add(condition);
            return this;
        }
        
        public WiqlQueryBuilder whereWorkItemType(String workItemType) {
            if (workItemType != null) {
                conditions.add("[System.WorkItemType] = '" + workItemType + "'");
            }
            return this;
        }
        
        public WiqlQueryBuilder whereFieldNotEmpty(String fieldName) {
            if (fieldName != null) {
                conditions.add("[" + fieldName + "] <> ''");
            }
            return this;
        }
        
        public WiqlQueryBuilder whereFieldEquals(String fieldName, String value) {
            if (fieldName != null && value != null) {
                conditions.add("[" + fieldName + "] = '" + value.replace("'", "''") + "'");
            }
            return this;
        }
        
        public WiqlQueryBuilder orderBy(String field, String direction) {
            if (field != null) {
                String orderClause = "[" + field + "]";
                if (direction != null && (direction.equalsIgnoreCase("DESC") || direction.equalsIgnoreCase("ASC"))) {
                    orderClause += " " + direction.toUpperCase();
                }
                orderBy.add(orderClause);
            }
            return this;
        }
        
        public WiqlQueryBuilder orderByDescending(String field) {
            return orderBy(field, "DESC");
        }
        
        public WiqlQueryBuilder orderByAscending(String field) {
            return orderBy(field, "ASC");
        }
    }
    
    public record WiqlValidationResult(boolean isValid, String message, List<String> warnings) {}
}
