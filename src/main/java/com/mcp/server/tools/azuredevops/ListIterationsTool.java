package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.Iteration;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Herramienta MCP para listar iteraciones (sprints) de un equipo.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class ListIterationsTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    
    @Autowired
    public ListIterationsTool(AzureDevOpsClient azureDevOpsClient) {
        this.azureDevOpsClient = azureDevOpsClient;
    }
    
    @Override
    public String getName() {
        return "azuredevops_list_iterations";
    }
    
    @Override
    public String getDescription() {
        return "Lista las iteraciones (sprints) de un equipo en Azure DevOps. " +
               "Permite analizar la cadencia de entregas y planificación ágil.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "project", Map.of(
                    "type", "string",
                    "description", "Nombre o ID del proyecto"
                ),
                "team", Map.of(
                    "type", "string",
                    "description", "Nombre o ID del equipo"
                ),
                "timeFrame", Map.of(
                    "type", "string",
                    "description", "Filtro temporal: 'current' (actual), 'past' (pasadas), 'future' (futuras) - opcional",
                    "enum", List.of("current", "past", "future")
                ),
                "includeDetails", Map.of(
                    "type", "boolean",
                    "description", "Si incluir análisis detallado de cadencia (por defecto: true)"
                )
            ),
            "required", List.of("project", "team")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            if (!azureDevOpsClient.isConfigured()) {
                return Map.of("error",
                    "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                );
            }
            
            String project = (String) arguments.get("project");
            String team = (String) arguments.get("team");
            String timeFrame = (String) arguments.get("timeFrame");
            Boolean includeDetails = (Boolean) arguments.getOrDefault("includeDetails", true);
            
            if (project == null || project.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'project' es requerido");
            }
            
            if (team == null || team.trim().isEmpty()) {
                return Map.of("error", "El parámetro 'team' es requerido");
            }
            
            List<Iteration> iterations = azureDevOpsClient.listIterations(project, team, timeFrame);
            
            if (iterations.isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", String.format("No se encontraron iteraciones para el equipo '%s' en el proyecto '%s'.", team, project)
                    ))
                );
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Iteraciones del equipo '%s' en proyecto '%s'", team, project));
            if (timeFrame != null && !timeFrame.trim().isEmpty()) {
                result.append(String.format(" (filtro: %s)", timeFrame.toUpperCase()));
            }
            result.append(":%n%n");
            
            // Listar iteraciones
            for (int i = 0; i < iterations.size(); i++) {
                Iteration iteration = iterations.get(i);
                result.append(String.format("%d. %s%n", i + 1, iteration.toDisplayString()));
                
                if (includeDetails) {
                    result.append("%n");
                    String[] detailLines = iteration.toDetailedString().split("%n");
                    for (String line : detailLines) {
                        if (!line.trim().isEmpty() && !line.startsWith("Iteración:")) {
                            result.append(String.format("   %s%n", line));
                        }
                    }
                    result.append("%n");
                }
            }
            
            // Análisis de cadencia si hay múltiples iteraciones
            if (includeDetails && iterations.size() > 1) {
                result.append(analyzeCadence(iterations));
            }
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (AzureDevOpsException e) {
            return Map.of("error", "Error de Azure DevOps: " + e.getMessage());
        } catch (Exception e) {
            return Map.of("error", "Error inesperado: " + e.getMessage());
        }
    }
    
    /**
     * Analiza la cadencia de las iteraciones para extraer patrones.
     */
    private String analyzeCadence(List<Iteration> iterations) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 Análisis de Cadencia:%n%n");
        
        // Análisis de duración
        Map<Long, Long> durationCounts = iterations.stream()
            .filter(i -> i.getDurationInDays() > 0)
            .collect(java.util.stream.Collectors.groupingBy(
                Iteration::getDurationInDays,
                java.util.stream.Collectors.counting()
            ));
        
        if (!durationCounts.isEmpty()) {
            analysis.append("Duraciones de iteraciones:%n");
            durationCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    long days = entry.getKey();
                    long count = entry.getValue();
                    String cadenceType = getCadenceType(days);
                    analysis.append(String.format("  • %d días (%s): %d iteración(es)%n", 
                        days, cadenceType, count));
                });
        }
        
        // Iteración actual
        iterations.stream()
            .filter(Iteration::isCurrent)
            .findFirst()
            .ifPresent(current -> {
                analysis.append("%n🎯 Iteración Actual:%n");
                analysis.append(String.format("  • %s%n", current.name()));
                if (current.getDurationInDays() > 0) {
                    analysis.append(String.format("  • Duración: %d días (%s)%n", 
                        current.getDurationInDays(), getCadenceType(current.getDurationInDays())));
                }
            });
        
        // Estadísticas generales
        long totalIterations = iterations.size();
        long currentCount = iterations.stream().mapToLong(i -> i.isCurrent() ? 1 : 0).sum();
        long pastCount = iterations.stream().mapToLong(i -> i.isPast() ? 1 : 0).sum();
        long futureCount = iterations.stream().mapToLong(i -> i.isFuture() ? 1 : 0).sum();
        
        analysis.append("%n📈 Estadísticas:%n");
        analysis.append(String.format("  • Total de iteraciones: %d%n", totalIterations));
        analysis.append(String.format("  • Pasadas: %d | Actual: %d | Futuras: %d%n", 
            pastCount, currentCount, futureCount));
        
        return analysis.toString();
    }
    
    /**
     * Determina el tipo de cadencia basado en la duración.
     */
    private String getCadenceType(long days) {
        return switch ((int) days) {
            case 7 -> "Semanal";
            case 14 -> "Quincenal";
            case 21 -> "Tri-semanal";
            case 28, 30 -> "Mensual";
            default -> "Personalizada";
        };
    }
}
