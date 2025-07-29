package com.mcp.server.tools.azuredevops.client;

/**
 * Excepción específica para errores de Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AzureDevOpsException extends RuntimeException {
    
    /**
     * Construye una nueva excepción con el mensaje especificado.
     * 
     * @param message mensaje de error
     */
    public AzureDevOpsException(String message) {
        super(message);
    }
    
    /**
     * Construye una nueva excepción con el mensaje y causa especificados.
     * 
     * @param message mensaje de error
     * @param cause causa de la excepción
     */
    public AzureDevOpsException(String message, Throwable cause) {
        super(message, cause);
    }
}
