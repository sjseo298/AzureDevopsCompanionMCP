package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Clase para representar notificaciones del protocolo MCP.
 * 
 * Las notificaciones son mensajes unidireccionales que no requieren respuesta.
 * No incluyen un ID ya que no se espera correlación con respuestas.
 * 
 * Formato JSON-RPC 2.0 según la especificación MCP.
 */
public class McpNotification {
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Object params;
    
    public McpNotification() {}
    
    public McpNotification(String method, Object params) {
        this.method = method;
        this.params = params;
    }
    
    // Getters and setters
    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public Object getParams() { return params; }
    public void setParams(Object params) { this.params = params; }
}
