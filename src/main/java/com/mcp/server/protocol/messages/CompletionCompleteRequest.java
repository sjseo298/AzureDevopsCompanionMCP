package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Solicitud para completar un prompt específico del servidor MCP.
 * 
 * Esta solicitud es parte del protocolo de completion y permite
 * completar argumentos de prompts dinámicamente.
 */
public class CompletionCompleteRequest extends McpRequest {
    
    @JsonProperty("params")
    private CompletionCompleteParams params;
    
    public CompletionCompleteRequest() {
        super(null, "completion/complete");
    }
    
    public CompletionCompleteRequest(Object id, CompletionCompleteParams params) {
        super(id, "completion/complete");
        this.params = params;
    }
    
    public CompletionCompleteParams getParams() {
        return params;
    }
    
    public void setParams(CompletionCompleteParams params) {
        this.params = params;
    }
    
    /**
     * Parámetros para la solicitud de completion.
     */
    public static class CompletionCompleteParams {
        
        @JsonProperty("ref")
        private CompletionRef ref;
        
        @JsonProperty("argument")
        private CompletionArgument argument;
        
        @JsonProperty("context")
        private Map<String, Object> context;
        
        public CompletionCompleteParams() {}
        
        public CompletionCompleteParams(CompletionRef ref, CompletionArgument argument, Map<String, Object> context) {
            this.ref = ref;
            this.argument = argument;
            this.context = context;
        }
        
        public CompletionRef getRef() {
            return ref;
        }
        
        public void setRef(CompletionRef ref) {
            this.ref = ref;
        }
        
        public CompletionArgument getArgument() {
            return argument;
        }
        
        public void setArgument(CompletionArgument argument) {
            this.argument = argument;
        }
        
        public Map<String, Object> getContext() {
            return context;
        }
        
        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }
    
    /**
     * Referencia al recurso para completion.
     */
    public static class CompletionRef {
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("name")
        private String name;
        
        public CompletionRef() {}
        
        public CompletionRef(String type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    /**
     * Argumento para completion.
     */
    public static class CompletionArgument {
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("value")
        private String value;
        
        public CompletionArgument() {}
        
        public CompletionArgument(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
}
