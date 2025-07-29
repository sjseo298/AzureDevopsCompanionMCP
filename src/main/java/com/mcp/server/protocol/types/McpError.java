package com.mcp.server.protocol.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Clase inmutable para representar errores en el protocolo MCP.
 * 
 * <p>Sigue el formato de errores JSON-RPC 2.0 con códigos de error estándar
 * y mensajes descriptivos. Proporciona métodos de conveniencia para crear
 * errores comunes.
 * 
 * <p>Códigos de error estándar según JSON-RPC 2.0:
 * <ul>
 *   <li>-32700: Parse error</li>
 *   <li>-32600: Invalid Request</li>
 *   <li>-32601: Method not found</li>
 *   <li>-32602: Invalid params</li>
 *   <li>-32603: Internal error</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class McpError {
    
    // Códigos de error estándar JSON-RPC 2.0
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // Códigos de error específicos de MCP (rango de aplicación: -32000 a -32099)
    public static final int TOOL_NOT_FOUND = -32000;
    public static final int TOOL_EXECUTION_ERROR = -32001;
    public static final int RESOURCE_NOT_FOUND = -32002;
    public static final int PERMISSION_DENIED = -32003;
    
    @JsonProperty("code")
    private final int code;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("data")
    private final Object data;
    
    /**
     * Constructor principal para crear un error.
     * 
     * @param code código de error JSON-RPC
     * @param message mensaje descriptivo del error
     * @param data datos adicionales del error (opcional)
     */
    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = Objects.requireNonNull(message, "Error message cannot be null");
        this.data = data;
    }
    
    /**
     * Constructor para crear un error sin datos adicionales.
     * 
     * @param code código de error JSON-RPC
     * @param message mensaje descriptivo del error
     */
    public McpError(int code, String message) {
        this(code, message, null);
    }
    
    /**
     * Constructor sin argumentos requerido por Jackson.
     */
    McpError() {
        this.code = 0;
        this.message = null;
        this.data = null;
    }
    
    // Métodos de conveniencia para errores comunes
    
    /**
     * Crea un error de parseo JSON.
     * 
     * @param message mensaje del error
     * @return nuevo McpError de parseo
     */
    public static McpError parseError(String message) {
        return new McpError(PARSE_ERROR, message);
    }
    
    /**
     * Crea un error de solicitud inválida.
     * 
     * @param message mensaje del error
     * @return nuevo McpError de solicitud inválida
     */
    public static McpError invalidRequest(String message) {
        return new McpError(INVALID_REQUEST, message);
    }
    
    /**
     * Crea un error de método no encontrado.
     * 
     * @param method método que no fue encontrado
     * @return nuevo McpError de método no encontrado
     */
    public static McpError methodNotFound(String method) {
        return new McpError(METHOD_NOT_FOUND, "Method not found: " + method);
    }
    
    /**
     * Crea un error de parámetros inválidos.
     * 
     * @param message mensaje del error
     * @return nuevo McpError de parámetros inválidos
     */
    public static McpError invalidParams(String message) {
        return new McpError(INVALID_PARAMS, message);
    }
    
    /**
     * Crea un error interno del servidor.
     * 
     * @param message mensaje del error
     * @return nuevo McpError interno
     */
    public static McpError internalError(String message) {
        return new McpError(INTERNAL_ERROR, message);
    }
    
    /**
     * Crea un error de herramienta no encontrada.
     * 
     * @param toolName nombre de la herramienta
     * @return nuevo McpError de herramienta no encontrada
     */
    public static McpError toolNotFound(String toolName) {
        return new McpError(TOOL_NOT_FOUND, "Tool not found: " + toolName);
    }
    
    /**
     * Crea un error de ejecución de herramienta.
     * 
     * @param toolName nombre de la herramienta
     * @param cause causa del error
     * @return nuevo McpError de ejecución
     */
    public static McpError toolExecutionError(String toolName, String cause) {
        return new McpError(TOOL_EXECUTION_ERROR, "Tool execution failed: " + toolName, cause);
    }
    
    // Getters
    public int getCode() { 
        return code; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public Object getData() { 
        return data; 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        McpError mcpError = (McpError) obj;
        return code == mcpError.code &&
               Objects.equals(message, mcpError.message) &&
               Objects.equals(data, mcpError.data);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(code, message, data);
    }
    
    @Override
    public String toString() {
        return "McpError{" +
               "code=" + code +
               ", message='" + message + '\'' +
               ", data=" + data +
               '}';
    }
}
