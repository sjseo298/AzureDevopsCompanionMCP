package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcp.server.protocol.types.McpError;
import java.util.Objects;

/**
 * Clase inmutable para representar respuestas del protocolo MCP.
 * 
 * <p>Una respuesta puede contener un resultado exitoso o un error,
 * pero nunca ambos según la especificación JSON-RPC 2.0.
 * El ID debe coincidir con el de la solicitud original.
 * 
 * <p>Ejemplos de uso:
 * <pre>{@code
 * // Respuesta exitosa
 * McpResponse success = McpResponse.success(1, data);
 * 
 * // Respuesta con error
 * McpResponse error = McpResponse.error(1, McpError.invalidParams("Missing parameter"));
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class McpResponse {
    
    private static final String JSON_RPC_VERSION = "2.0";
    
    @JsonProperty("jsonrpc")
    private final String jsonrpc;
    
    @JsonProperty("id")
    private final Object id;
    
    @JsonProperty("result")
    private final Object result;
    
    @JsonProperty("error")
    private final McpError error;
    
    /**
     * Constructor privado para construcción controlada.
     */
    private McpResponse(Object id, Object result, McpError error) {
        this.jsonrpc = JSON_RPC_VERSION;
        this.id = id;
        this.result = result;
        this.error = error;
    }
    
    /**
     * Constructor sin argumentos requerido por Jackson.
     */
    McpResponse() {
        this.jsonrpc = JSON_RPC_VERSION;
        this.id = null;
        this.result = null;
        this.error = null;
    }
    
    /**
     * Crea una respuesta exitosa con resultado.
     * 
     * @param id identificador de la solicitud original
     * @param result resultado de la operación
     * @return nueva respuesta exitosa
     */
    public static McpResponse success(Object id, Object result) {
        Objects.requireNonNull(result, "Result cannot be null for success response");
        return new McpResponse(id, result, null);
    }
    
    /**
     * Crea una respuesta de error.
     * 
     * @param id identificador de la solicitud original
     * @param error información del error
     * @return nueva respuesta de error
     */
    public static McpResponse error(Object id, McpError error) {
        Objects.requireNonNull(error, "Error cannot be null for error response");
        return new McpResponse(id, null, error);
    }
    
    /**
     * Crea una respuesta de error con código y mensaje.
     * 
     * @param id identificador de la solicitud original
     * @param code código de error JSON-RPC
     * @param message mensaje de error
     * @return nueva respuesta de error
     */
    public static McpResponse error(Object id, int code, String message) {
        return error(id, new McpError(code, message));
    }
    
    // Getters
    public String getJsonrpc() { 
        return jsonrpc; 
    }
    
    public Object getId() { 
        return id; 
    }
    
    @JsonIgnore
    public Object getResult() { 
        return result; 
    }
    
    public McpError getError() { 
        return error; 
    }
    
    @JsonIgnore
    /**
     * Indica si esta respuesta representa un éxito.
     * 
     * @return true si la respuesta es exitosa (tiene result y no error)
     */
    public boolean isSuccess() {
        return error == null && result != null;
    }
    
    /**
     * Indica si esta respuesta representa un error.
     * 
     * @return true si la respuesta es de error (tiene error y no result)
     */
    public boolean isError() {
        return error != null && result == null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        McpResponse that = (McpResponse) obj;
        return Objects.equals(jsonrpc, that.jsonrpc) &&
               Objects.equals(id, that.id) &&
               Objects.equals(result, that.result) &&
               Objects.equals(error, that.error);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(jsonrpc, id, result, error);
    }
    
    @Override
    public String toString() {
        return "McpResponse{" +
               "jsonrpc='" + jsonrpc + '\'' +
               ", id=" + id +
               ", result=" + result +
               ", error=" + error +
               '}';
    }
}
