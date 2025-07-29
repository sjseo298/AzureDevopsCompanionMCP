package com.mcp.server.protocol.messages;

import java.util.Map;

/**
 * Solicitud para obtener un prompt específico del servidor MCP.
 * 
 * Esta solicitud requiere el nombre del prompt y opcionalmente argumentos
 * para personalizar el prompt generado.
 */
public class GetPromptRequest extends McpRequest {
    
    private GetPromptParams params;
    
    public GetPromptRequest() {
        super(null, "prompts/get");
    }
    
    public GetPromptRequest(Object id, String name, Map<String, Object> arguments) {
        super(id, "prompts/get");
        this.params = new GetPromptParams(name, arguments);
    }
    
    public GetPromptParams getParams() {
        return params;
    }
    
    public void setParams(GetPromptParams params) {
        this.params = params;
    }
    
    public static class GetPromptParams {
        private String name;
        private Map<String, Object> arguments;
        
        public GetPromptParams() {}
        
        public GetPromptParams(String name, Map<String, Object> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Map<String, Object> getArguments() {
            return arguments;
        }
        
        public void setArguments(Map<String, Object> arguments) {
            this.arguments = arguments;
        }
    }
}
