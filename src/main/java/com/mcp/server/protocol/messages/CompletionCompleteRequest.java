package com.mcp.server.protocol.messages;

import java.util.Map;

/**
 * Solicitud para completar argumentos de un prompt o referencia MCP.
 * 
 * Esta solicitud se utiliza para autocompletado de argumentos cuando
 * el cliente está construyendo una referencia a un prompt específico.
 */
public class CompletionCompleteRequest extends McpRequest {
    
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
     * Parámetros de la solicitud de completion.
     */
    public static class CompletionCompleteParams {
        private CompletionRef ref;
        private CompletionArgument argument;
        private CompletionContext context;
        
        public CompletionCompleteParams() {}
        
        public CompletionRef getRef() { return ref; }
        public void setRef(CompletionRef ref) { this.ref = ref; }
        
        public CompletionArgument getArgument() { return argument; }
        public void setArgument(CompletionArgument argument) { this.argument = argument; }
        
        public CompletionContext getContext() { return context; }
        public void setContext(CompletionContext context) { this.context = context; }
    }
    
    /**
     * Referencia al prompt o recurso para completar.
     */
    public static class CompletionRef {
        private String type;
        private String name;
        
        public CompletionRef() {}
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    /**
     * Argumento específico para completar.
     */
    public static class CompletionArgument {
        private String name;
        private String value;
        
        public CompletionArgument() {}
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
    
    /**
     * Contexto adicional para la completion.
     */
    public static class CompletionContext {
        private Map<String, Object> arguments;
        
        public CompletionContext() {}
        
        public Map<String, Object> getArguments() { return arguments; }
        public void setArguments(Map<String, Object> arguments) { this.arguments = arguments; }
    }
}
