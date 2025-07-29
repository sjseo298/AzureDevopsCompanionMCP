package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Solicitud para ejecutar una herramienta específica en el servidor MCP.
 * 
 * Incluye el nombre de la herramienta y los argumentos necesarios
 * para su ejecución según el esquema definido por la herramienta.
 */
public class CallToolRequest extends McpRequest {
    @JsonProperty("params")
    private CallToolParams params;
    
    public CallToolRequest() {
        super(null, "tools/call");
    }
    
    public CallToolRequest(Object id, CallToolParams params) {
        super(id, "tools/call");
        this.params = params;
    }
    
    public CallToolParams getParams() { return params; }
    public void setParams(CallToolParams params) { this.params = params; }
    
    /**
     * Parámetros para la ejecución de herramientas.
     */
    public static class CallToolParams {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("arguments")
        private Map<String, Object> arguments;
        
        public CallToolParams() {}
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Map<String, Object> getArguments() { return arguments; }
        public void setArguments(Map<String, Object> arguments) { this.arguments = arguments; }
    }
}
