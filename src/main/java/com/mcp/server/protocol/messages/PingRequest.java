package com.mcp.server.protocol.messages;

/**
 * Solicitud de ping para verificar estado del servidor.
 */
public class PingRequest extends McpRequest {
    
    public PingRequest() {
        super(null, "ping");
    }
    
    public PingRequest(Object id) {
        super(id, "ping");
    }
}
