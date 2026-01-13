package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;

/**
 * Clase base abstracta para todas las solicitudes del protocolo MCP.
 * 
 * <p>Las solicitudes son mensajes que requieren una respuesta y por lo tanto
 * incluyen un ID único para correlacionar la respuesta según JSON-RPC 2.0.
 * 
 * <p>Esta clase utiliza polimorfismo de Jackson para deserializar automáticamente
 * a la subclase correcta basándose en el campo "method".
 * 
 * <p>Formato JSON-RPC 2.0 según la especificación MCP:
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "initialize",
 *   "params": { ... }
 * }
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, 
    property = "method", 
    visible = true,
    defaultImpl = McpRequest.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InitializeRequest.class, name = "initialize"),
    @JsonSubTypes.Type(value = ListToolsRequest.class, name = "tools/list"),
    @JsonSubTypes.Type(value = CallToolRequest.class, name = "tools/call"),
    @JsonSubTypes.Type(value = ListPromptsRequest.class, name = "prompts/list"),
    @JsonSubTypes.Type(value = GetPromptRequest.class, name = "prompts/get"),
    @JsonSubTypes.Type(value = CompletionCompleteRequest.class, name = "completion/complete"),
    @JsonSubTypes.Type(value = PingRequest.class, name = "ping")
})
public abstract class McpRequest {
    
    private static final String JSON_RPC_VERSION = "2.0";
    
    @JsonProperty("jsonrpc")
    private final String jsonrpc;
    
    @JsonProperty("id")
    private final Object id;
    
    @JsonProperty("method")
    private final String method;
    
    /**
     * Constructor para crear una solicitud MCP.
     * 
     * @param id identificador único de la solicitud
     * @param method método/acción a ejecutar
     * @throws NullPointerException si method es null
     */
    protected McpRequest(Object id, String method) {
        this.jsonrpc = JSON_RPC_VERSION;
        this.id = id; // Puede ser null para notificaciones
        this.method = Objects.requireNonNull(method, "Method cannot be null");
    }
    
    /**
     * Constructor sin argumentos requerido por Jackson.
     */
    protected McpRequest() {
        this.jsonrpc = JSON_RPC_VERSION;
        this.id = null;
        this.method = null;
    }
    
    // Getters
    public final String getJsonrpc() { 
        return jsonrpc; 
    }
    
    public final Object getId() { 
        return id; 
    }
    
    public final String getMethod() { 
        return method; 
    }
    
    /**
     * Indica si esta solicitud es una notificación (sin respuesta esperada).
     * 
     * @return true si es una notificación (id es null)
     */
    public final boolean isNotification() {
        return id == null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        McpRequest that = (McpRequest) obj;
        return Objects.equals(jsonrpc, that.jsonrpc) &&
               Objects.equals(id, that.id) &&
               Objects.equals(method, that.method);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(jsonrpc, id, method);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "jsonrpc='" + jsonrpc + '\'' +
               ", id=" + id +
               ", method='" + method + '\'' +
               '}';
    }
}
