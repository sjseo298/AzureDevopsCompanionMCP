package com.mcp.server.prompts.base;

import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.List;
import java.util.Map;

/**
 * Clase base abstracta para implementar prompts MCP.
 * 
 * Proporciona funcionalidad común para todos los prompts y simplifica
 * la implementación de nuevos prompts definiendo métodos de conveniencia.
 */
public abstract class BasePrompt implements McpPrompt {
    
    protected final String name;
    protected final String title;
    protected final String description;
    protected final List<Prompt.PromptArgument> arguments;
    
    /**
     * Constructor para prompts básicos.
     * 
     * @param name El nombre único del prompt
     * @param title El título del prompt
     * @param description La descripción del prompt
     * @param arguments Los argumentos del prompt
     */
    protected BasePrompt(String name, String title, String description, List<Prompt.PromptArgument> arguments) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.arguments = arguments;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public Prompt getPromptDefinition() {
        return new Prompt(name, title, description, arguments);
    }
    
    /**
     * Método de conveniencia para crear un mensaje de sistema.
     * 
     * @param text El texto del mensaje
     * @return Un mensaje de sistema
     */
    protected PromptResult.PromptMessage systemMessage(String text) {
        return new PromptResult.PromptMessage(PromptResult.Role.SYSTEM, text);
    }
    
    /**
     * Método de conveniencia para crear un mensaje de usuario.
     * 
     * @param text El texto del mensaje
     * @return Un mensaje de usuario
     */
    protected PromptResult.PromptMessage userMessage(String text) {
        return new PromptResult.PromptMessage(PromptResult.Role.USER, text);
    }
    
    /**
     * Método de conveniencia para crear un mensaje de asistente.
     * 
     * @param text El texto del mensaje
     * @return Un mensaje de asistente
     */
    protected PromptResult.PromptMessage assistantMessage(String text) {
        return new PromptResult.PromptMessage(PromptResult.Role.ASSISTANT, text);
    }
    
    /**
     * Obtiene un argumento como String con valor por defecto.
     * 
     * @param arguments El mapa de argumentos
     * @param key La clave del argumento
     * @param defaultValue El valor por defecto
     * @return El valor del argumento o el valor por defecto
     */
    protected String getStringArgument(Map<String, Object> arguments, String key, String defaultValue) {
        Object value = arguments.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Obtiene un argumento como String requerido.
     * 
     * @param arguments El mapa de argumentos
     * @param key La clave del argumento
     * @return El valor del argumento
     * @throws IllegalArgumentException si el argumento no existe
     */
    protected String getRequiredStringArgument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required argument missing: " + key);
        }
        return value.toString();
    }
    
    /**
     * Obtiene un argumento como Boolean con valor por defecto.
     * 
     * @param arguments El mapa de argumentos
     * @param key La clave del argumento
     * @param defaultValue El valor por defecto
     * @return El valor del argumento o el valor por defecto
     */
    protected Boolean getBooleanArgument(Map<String, Object> arguments, String key, Boolean defaultValue) {
        Object value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
