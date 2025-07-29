package com.mcp.server.prompts.base;

import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.Map;

/**
 * Interfaz base para todos los prompts MCP.
 * 
 * Define el contrato que deben cumplir todos los prompts implementados
 * en el servidor, proporcionando métodos para definir la estructura
 * del prompt y ejecutarlo con argumentos específicos.
 */
public interface McpPrompt {
    
    /**
     * Obtiene el nombre único del prompt.
     * 
     * @return El nombre del prompt
     */
    String getName();
    
    /**
     * Obtiene el título del prompt para mostrar en la UI.
     * 
     * @return El título del prompt
     */
    String getTitle();
    
    /**
     * Obtiene la descripción del prompt.
     * 
     * @return La descripción del prompt
     */
    String getDescription();
    
    /**
     * Obtiene la definición completa del prompt incluyendo argumentos.
     * 
     * @return La definición del prompt
     */
    Prompt getPromptDefinition();
    
    /**
     * Ejecuta el prompt con los argumentos proporcionados.
     * 
     * @param arguments Los argumentos para el prompt
     * @return El resultado del prompt con mensajes generados
     */
    PromptResult execute(Map<String, Object> arguments);
    
    /**
     * Valida que los argumentos proporcionados son válidos para este prompt.
     * 
     * @param arguments Los argumentos a validar
     * @throws IllegalArgumentException si los argumentos no son válidos
     */
    default void validateArguments(Map<String, Object> arguments) {
        // Implementación por defecto - puede ser sobrescrita
        if (arguments == null) {
            arguments = Map.of();
        }
        
        Prompt definition = getPromptDefinition();
        if (definition.getArguments() != null) {
            for (Prompt.PromptArgument arg : definition.getArguments()) {
                if (arg.isRequired() && !arguments.containsKey(arg.getName())) {
                    throw new IllegalArgumentException("Required argument missing: " + arg.getName());
                }
            }
        }
    }
}
