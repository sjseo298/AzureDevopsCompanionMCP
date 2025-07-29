package com.mcp.server.tools.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Representa una iteración (sprint) de Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Iteration(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("attributes") Attributes attributes,
    @JsonProperty("url") String url,
    @JsonProperty("timeFrame") String timeFrame
) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Attributes(
        @JsonProperty("startDate") String startDate,
        @JsonProperty("finishDate") String finishDate
    ) {}
    
    /**
     * Obtiene la fecha de inicio de la iteración.
     * 
     * @return fecha de inicio o null si no está definida
     */
    public OffsetDateTime getStartDate() {
        return attributes != null && attributes.startDate != null 
            ? OffsetDateTime.parse(attributes.startDate) 
            : null;
    }
    
    /**
     * Obtiene la fecha de fin de la iteración.
     * 
     * @return fecha de fin o null si no está definida
     */
    public OffsetDateTime getFinishDate() {
        return attributes != null && attributes.finishDate != null 
            ? OffsetDateTime.parse(attributes.finishDate) 
            : null;
    }
    
    /**
     * Calcula la duración de la iteración en días.
     * 
     * @return duración en días o 0 si las fechas no están disponibles
     */
    public long getDurationInDays() {
        OffsetDateTime start = getStartDate();
        OffsetDateTime finish = getFinishDate();
        
        if (start != null && finish != null) {
            return ChronoUnit.DAYS.between(start.toLocalDate(), finish.toLocalDate()) + 1;
        }
        return 0;
    }
    
    /**
     * Verifica si la iteración es la actual.
     * 
     * @return true si es la iteración actual
     */
    public boolean isCurrent() {
        return "current".equalsIgnoreCase(timeFrame);
    }
    
    /**
     * Verifica si la iteración está en el pasado.
     * 
     * @return true si es una iteración pasada
     */
    public boolean isPast() {
        return "past".equalsIgnoreCase(timeFrame);
    }
    
    /**
     * Verifica si la iteración está en el futuro.
     * 
     * @return true si es una iteración futura
     */
    public boolean isFuture() {
        return "future".equalsIgnoreCase(timeFrame);
    }
    
    /**
     * Convierte la iteración a una representación de texto.
     * 
     * @return representación textual de la iteración
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s (%s)", name, id));
        
        if (isCurrent()) {
            sb.append(" [ACTUAL]");
        } else if (isPast()) {
            sb.append(" [PASADA]");
        } else if (isFuture()) {
            sb.append(" [FUTURA]");
        }
        
        OffsetDateTime start = getStartDate();
        OffsetDateTime finish = getFinishDate();
        
        if (start != null && finish != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            sb.append(String.format(" - %s al %s (%d días)", 
                start.format(formatter),
                finish.format(formatter),
                getDurationInDays()
            ));
        }
        
        return sb.toString();
    }
    
    /**
     * Obtiene una representación detallada de la iteración.
     * 
     * @return información detallada de la iteración
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Iteración: %s%n", name));
        sb.append(String.format("ID: %s%n", id));
        sb.append(String.format("Estado: %s%n", timeFrame != null ? timeFrame.toUpperCase() : "DESCONOCIDO"));
        
        OffsetDateTime start = getStartDate();
        OffsetDateTime finish = getFinishDate();
        
        if (start != null && finish != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            sb.append(String.format("Inicio: %s%n", start.format(formatter)));
            sb.append(String.format("Fin: %s%n", finish.format(formatter)));
            sb.append(String.format("Duración: %d días%n", getDurationInDays()));
            
            // Calcular cadencia típica (asumiendo sprints quincenales o semanales)
            long duration = getDurationInDays();
            if (duration == 7) {
                sb.append("Cadencia: Semanal%n");
            } else if (duration == 14) {
                sb.append("Cadencia: Quincenal%n");
            } else if (duration == 21) {
                sb.append("Cadencia: Tri-semanal%n");
            } else if (duration == 28 || duration == 30) {
                sb.append("Cadencia: Mensual%n");
            } else {
                sb.append(String.format("Cadencia: Personalizada (%d días)%n", duration));
            }
        }
        
        return sb.toString();
    }
}
