package com.mcp.server.protocol.messages;

/**
 * Solicitud para listar los prompts disponibles en el servidor MCP.
 * 
 * Esta solicitud no requiere parámetros y devuelve la lista completa
 * de prompts que el servidor puede ejecutar.
 */
public class ListPromptsRequest extends McpRequest {
    
    public ListPromptsRequest() {
        super(null, "prompts/list");
    }
    
    public ListPromptsRequest(Object id) {
        super(id, "prompts/list");
    }
}
