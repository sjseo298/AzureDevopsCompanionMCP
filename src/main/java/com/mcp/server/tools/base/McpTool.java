package com.mcp.server.tools.base;

import com.mcp.server.protocol.types.Tool;
import java.util.List;
import java.util.Map;

/**
 * Interfaz funcional base para todas las herramientas MCP.
 * 
 * <p>Define el contrato que deben cumplir las implementaciones de herramientas
 * para ser registradas y ejecutadas por el servidor MCP.
 * 
 * <p>Todas las implementaciones deben:
 * <ul>
 *   <li>Ser marcadas con {@code @Component} para auto-registro</li>
 *   <li>Proporcionar una definición completa de herramienta</li>
 *   <li>Manejar errores de forma consistente</li>
 *   <li>Validar argumentos de entrada</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@FunctionalInterface
public interface McpTool {
    
    /**
     * Ejecuta la herramienta con los argumentos proporcionados.
     * 
     * <p>Esta es la operación principal que debe implementar cada herramienta.
     * El resultado debe seguir el formato estándar de respuesta MCP.
     * 
     * @param arguments argumentos para la ejecución de la herramienta
     * @return resultado de la ejecución en formato MCP
     * @throws IllegalArgumentException si los argumentos son inválidos
     * @throws RuntimeException si ocurre un error durante la ejecución
     */
    Map<String, Object> execute(Map<String, Object> arguments);
    
    /**
     * Retorna la definición de la herramienta incluyendo nombre, descripción y esquema.
     * 
     * <p>Esta implementación por defecto requiere que la clase implemente
     * también el método {@link #getName()}.
     * 
     * @return definición de la herramienta
     */
    default Tool getToolDefinition() {
        return Tool.builder()
                .name(getName())
                .description(getDescription())
                .inputSchema(getInputSchema())
                .build();
    }
    
    /**
     * Retorna el nombre único de la herramienta.
     * 
     * <p>El nombre debe ser único en todo el servidor y seguir la convención
     * snake_case (ej: "generate_uuid", "calculate_sum").
     * 
     * @return nombre único de la herramienta
     */
    default String getName() {
        // Por defecto, usar el nombre de la clase en snake_case
        String className = this.getClass().getSimpleName();
        return className.replaceAll("([a-z])([A-Z])", "$1_$2")
                       .replace("Tool", "")
                       .toLowerCase();
    }
    
    /**
     * Retorna la descripción de la herramienta.
     * 
     * <p>La descripción debe ser clara y concisa, explicando qué hace
     * la herramienta y cuándo usarla.
     * 
     * @return descripción de la herramienta
     */
    default String getDescription() {
        return "Herramienta MCP: " + getName();
    }
    
    /**
     * Retorna el esquema JSON para validar los argumentos de entrada.
     * 
     * <p>El esquema debe seguir JSON Schema Draft 7 y definir todos
     * los parámetros requeridos y opcionales.
     * 
     * @return esquema JSON para validación de argumentos
     */
    default Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", List.of()
        );
    }
}
