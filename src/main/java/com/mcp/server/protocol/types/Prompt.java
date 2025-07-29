package com.mcp.server.protocol.types;

import java.util.List;
import java.util.Map;

/**
 * Representa un prompt MCP que puede ser ejecutado por el servidor.
 * 
 * Los prompts proporcionan plantillas estructuradas para interacciones
 * con modelos de IA, soportando:
 * - Formateo consistente de mensajes
 * - Sustitución de parámetros
 * - Inyección de contexto
 * - Formateo de respuestas
 * - Plantillas de instrucciones
 */
public class Prompt {
    
    private String name;
    private String title;
    private String description;
    private List<PromptArgument> arguments;
    
    public Prompt() {}
    
    public Prompt(String name, String title, String description, List<PromptArgument> arguments) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.arguments = arguments;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<PromptArgument> getArguments() {
        return arguments;
    }
    
    public void setArguments(List<PromptArgument> arguments) {
        this.arguments = arguments;
    }
    
    /**
     * Convierte el prompt a un Map para serialización JSON.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "name", name,
            "title", title != null ? title : name,
            "description", description != null ? description : "",
            "arguments", arguments != null ? arguments : List.of()
        );
    }
    
    /**
     * Argumento de un prompt MCP.
     */
    public static class PromptArgument {
        private String name;
        private String title;
        private String description;
        private boolean required;
        
        public PromptArgument() {}
        
        public PromptArgument(String name, String title, String description, boolean required) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.required = required;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public void setRequired(boolean required) {
            this.required = required;
        }
        
        /**
         * Convierte el argumento a un Map para serialización JSON.
         */
        public Map<String, Object> toMap() {
            return Map.of(
                "name", name,
                "title", title != null ? title : name,
                "description", description != null ? description : "",
                "required", required
            );
        }
    }
}
