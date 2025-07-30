package com.mcp.server.tools.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Representa un Work Item de Azure DevOps.
 * 
 * <p>Modelo de datos inmutable que encapsula la información completa
 * de un elemento de trabajo, incluyendo campos del sistema, campos
 * personalizados y metadatos de versionado.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkItem(
    @JsonProperty("id") Integer id,
    @JsonProperty("rev") Integer rev,
    @JsonProperty("fields") Map<String, Object> fields,
    @JsonProperty("url") String url
) {
    
    /**
     * Obtiene el título del work item.
     * 
     * @return título del work item o null si no está definido
     */
    public String getTitle() {
        return (String) fields.get("System.Title");
    }
    
    /**
     * Obtiene el estado actual del work item.
     * 
     * @return estado del work item o null si no está definido
     */
    public String getState() {
        return (String) fields.get("System.State");
    }
    
    /**
     * Obtiene el tipo de work item.
     * 
     * @return tipo del work item (Task, User Story, Bug, etc.)
     */
    public String getWorkItemType() {
        return (String) fields.get("System.WorkItemType");
    }
    
    /**
     * Obtiene la persona asignada al work item.
     * 
     * @return información del usuario asignado o null si no está asignado
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAssignedTo() {
        return (Map<String, Object>) fields.get("System.AssignedTo");
    }
    
    /**
     * Obtiene el nombre del usuario asignado.
     * 
     * @return nombre del usuario asignado o "Unassigned" si no está asignado
     */
    public String getAssignedToName() {
        var assignedTo = getAssignedTo();
        if (assignedTo != null) {
            return (String) assignedTo.get("displayName");
        }
        return "Unassigned";
    }
    
    /**
     * Obtiene la descripción del work item.
     * 
     * @return descripción o null si no está definida
     */
    public String getDescription() {
        return (String) fields.get("System.Description");
    }
    
    /**
     * Obtiene la ruta del área del work item.
     * 
     * @return ruta del área
     */
    public String getAreaPath() {
        return (String) fields.get("System.AreaPath");
    }
    
    /**
     * Obtiene la ruta de iteración del work item.
     * 
     * @return ruta de iteración
     */
    public String getIterationPath() {
        return (String) fields.get("System.IterationPath");
    }
    
    /**
     * Obtiene la fecha de creación del work item.
     * 
     * @return fecha de creación
     */
    public OffsetDateTime getCreatedDate() {
        String dateStr = (String) fields.get("System.CreatedDate");
        return dateStr != null ? OffsetDateTime.parse(dateStr) : null;
    }
    
    /**
     * Obtiene la fecha de última modificación del work item.
     * 
     * @return fecha de modificación
     */
    public OffsetDateTime getChangedDate() {
        String dateStr = (String) fields.get("System.ChangedDate");
        return dateStr != null ? OffsetDateTime.parse(dateStr) : null;
    }
    
    /**
     * Obtiene la fecha de vencimiento (Due Date) del work item.
     * 
     * @return fecha de vencimiento o null si no está definida
     */
    public OffsetDateTime getDueDate() {
        String dateStr = (String) fields.get("Microsoft.VSTS.Scheduling.DueDate");
        return dateStr != null ? OffsetDateTime.parse(dateStr) : null;
    }
    
    /**
     * Obtiene la fecha objetivo (Target Date) del work item.
     * 
     * @return fecha objetivo o null si no está definida
     */
    public OffsetDateTime getTargetDate() {
        String dateStr = (String) fields.get("Microsoft.VSTS.Scheduling.TargetDate");
        return dateStr != null ? OffsetDateTime.parse(dateStr) : null;
    }
    
    /**
     * Obtiene la fecha de inicio (Start Date) del work item.
     * 
     * @return fecha de inicio o null si no está definida
     */
    public OffsetDateTime getStartDate() {
        String dateStr = (String) fields.get("Microsoft.VSTS.Scheduling.StartDate");
        return dateStr != null ? OffsetDateTime.parse(dateStr) : null;
    }
    
    /**
     * Obtiene la fecha de finalización (Finish Date) del work item.
     * 
     * @return fecha de finalización o null si no está definida
     */
    public OffsetDateTime getFinishDate() {
        String dateStr = (String) fields.get("Microsoft.VSTS.Scheduling.FinishDate");
        return dateStr != null ? OffsetDateTime.parse(dateStr) : null;
    }
    
    /**
     * Obtiene las etiquetas del work item.
     * 
     * @return etiquetas separadas por punto y coma o null si no hay etiquetas
     */
    public String getTags() {
        return (String) fields.get("System.Tags");
    }
    
    /**
     * Obtiene el trabajo restante estimado.
     * 
     * @return horas de trabajo restante o null si no está definido
     */
    public Double getRemainingWork() {
        Object value = fields.get("Microsoft.VSTS.Scheduling.RemainingWork");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
    
    /**
     * Obtiene el trabajo original estimado.
     * 
     * @return horas de trabajo original o null si no está definido
     */
    public Double getOriginalEstimate() {
        Object value = fields.get("Microsoft.VSTS.Scheduling.OriginalEstimate");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
    
    /**
     * Obtiene el trabajo completado.
     * 
     * @return horas de trabajo completado o null si no está definido
     */
    public Double getCompletedWork() {
        Object value = fields.get("Microsoft.VSTS.Scheduling.CompletedWork");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
    
    /**
     * Obtiene el story points del work item.
     * 
     * @return story points o null si no está definido
     */
    public Double getStoryPoints() {
        Object value = fields.get("Microsoft.VSTS.Scheduling.StoryPoints");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
    
    /**
     * Obtiene la prioridad del work item.
     * 
     * @return prioridad o null si no está definida
     */
    public Integer getPriority() {
        Object value = fields.get("Microsoft.VSTS.Common.Priority");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
    
    /**
     * Obtiene un campo personalizado del work item.
     * 
     * @param fieldName nombre del campo
     * @return valor del campo o null si no existe
     */
    public Object getField(String fieldName) {
        return fields.get(fieldName);
    }
    
    /**
     * Verifica si el work item está cerrado.
     * 
     * @return true si el estado indica que está cerrado
     */
    public boolean isClosed() {
        String state = getState();
        return state != null && (
            state.equalsIgnoreCase("Closed") ||
            state.equalsIgnoreCase("Done") ||
            state.equalsIgnoreCase("Resolved") ||
            state.equalsIgnoreCase("Completed")
        );
    }
    
    /**
     * Verifica si el work item está activo/en progreso.
     * 
     * @return true si el estado indica que está activo
     */
    public boolean isActive() {
        String state = getState();
        return state != null && (
            state.equalsIgnoreCase("Active") ||
            state.equalsIgnoreCase("In Progress") ||
            state.equalsIgnoreCase("Committed")
        );
    }
    
    /**
     * Convierte el work item a una representación de texto legible.
     * 
     * @return representación textual del work item
     */
    public String toDisplayString() {
        return String.format("[%s #%d] %s - Estado: %s - Asignado: %s",
            getWorkItemType(),
            id,
            getTitle(),
            getState(),
            getAssignedToName()
        );
    }
    
    /**
     * Convierte el work item a una representación detallada de texto.
     * 
     * @return representación detallada del work item
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ID: %d | Tipo: %s | Estado: %s%n", id, getWorkItemType(), getState()));
        sb.append(String.format("Título: %s%n", getTitle()));
        sb.append(String.format("Asignado: %s%n", getAssignedToName()));
        sb.append(String.format("Área: %s%n", getAreaPath()));
        sb.append(String.format("Iteración: %s%n", getIterationPath()));
        
        if (getDescription() != null && !getDescription().trim().isEmpty()) {
            sb.append(String.format("Descripción: %s%n", getDescription()));
        }
        
        // Fechas de planificación
        if (getStartDate() != null) {
            sb.append(String.format("Fecha de inicio: %s%n", getStartDate().toLocalDate()));
        }
        
        if (getDueDate() != null) {
            sb.append(String.format("Fecha de vencimiento: %s%n", getDueDate().toLocalDate()));
        }
        
        if (getTargetDate() != null) {
            sb.append(String.format("Fecha objetivo: %s%n", getTargetDate().toLocalDate()));
        }
        
        if (getFinishDate() != null) {
            sb.append(String.format("Fecha de finalización: %s%n", getFinishDate().toLocalDate()));
        }
        
        if (getRemainingWork() != null) {
            sb.append(String.format("Trabajo restante: %.1f horas%n", getRemainingWork()));
        }
        
        if (getStoryPoints() != null) {
            sb.append(String.format("Story Points: %.0f%n", getStoryPoints()));
        }
        
        if (getTags() != null && !getTags().trim().isEmpty()) {
            sb.append(String.format("Etiquetas: %s%n", getTags()));
        }
        
        return sb.toString();
    }
}
