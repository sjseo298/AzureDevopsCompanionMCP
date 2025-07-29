package com.mcp.server.protocol.types;

import java.util.List;
import java.util.Map;

/**
 * Resultado de un prompt MCP que contiene los mensajes generados.
 * 
 * Representa la respuesta cuando se ejecuta un prompt, proporcionando
 * los mensajes formateados que pueden ser utilizados por un modelo de IA.
 */
public class PromptResult {
    
    private String description;
    private List<PromptMessage> messages;
    
    public PromptResult() {}
    
    public PromptResult(String description, List<PromptMessage> messages) {
        this.description = description;
        this.messages = messages;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<PromptMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<PromptMessage> messages) {
        this.messages = messages;
    }
    
    /**
     * Convierte el resultado a un Map para serialización JSON.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "description", description != null ? description : "",
            "messages", messages != null ? messages.stream().map(PromptMessage::toMap).toList() : List.of()
        );
    }
    
    /**
     * Mensaje individual dentro de un prompt MCP.
     */
    public static class PromptMessage {
        private Role role;
        private Content content;
        
        public PromptMessage() {}
        
        public PromptMessage(Role role, Content content) {
            this.role = role;
            this.content = content;
        }
        
        public PromptMessage(Role role, String text) {
            this.role = role;
            this.content = new TextContent(text);
        }
        
        public Role getRole() {
            return role;
        }
        
        public void setRole(Role role) {
            this.role = role;
        }
        
        public Content getContent() {
            return content;
        }
        
        public void setContent(Content content) {
            this.content = content;
        }
        
        /**
         * Convierte el mensaje a un Map para serialización JSON.
         */
        public Map<String, Object> toMap() {
            return Map.of(
                "role", role.toString().toLowerCase(),
                "content", content.toMap()
            );
        }
    }
    
    /**
     * Roles posibles para los mensajes de prompt.
     */
    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
    
    /**
     * Contenido base para mensajes de prompt.
     */
    public static abstract class Content {
        protected String type;
        
        public Content(String type) {
            this.type = type;
        }
        
        public String getType() {
            return type;
        }
        
        public abstract Map<String, Object> toMap();
    }
    
    /**
     * Contenido de texto para mensajes de prompt.
     */
    public static class TextContent extends Content {
        private String text;
        
        public TextContent() {
            super("text");
        }
        
        public TextContent(String text) {
            super("text");
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "type", type,
                "text", text != null ? text : ""
            );
        }
    }
}
