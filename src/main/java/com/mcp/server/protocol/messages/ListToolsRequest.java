package com.mcp.server.protocol.messages;

/**
 * Solicitud para listar las herramientas disponibles en el servidor MCP.
 * 
 * Esta solicitud no requiere parámetros y devuelve la lista completa
 * de herramientas que el servidor puede ejecutar.
 */
public class ListToolsRequest extends McpRequest {
    
    public ListToolsRequest() {
        super(null, "tools/list");
    }
    
    public ListToolsRequest(Object id) {
        super(id, "tools/list");
    }
}
