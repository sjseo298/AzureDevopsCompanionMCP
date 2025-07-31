package com.mcp.server.protocol.handlers;

import com.mcp.server.protocol.messages.*;
import com.mcp.server.protocol.types.McpError;
import com.mcp.server.tools.base.McpTool;
import com.mcp.server.prompts.base.McpPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Manejador principal del protocolo MCP.
 * 
 * Procesa las solicitudes del protocolo y las dirige a los servicios apropiados.
 * Maneja la inicialización, listado de herramientas y ejecución de herramientas,
 * así como listado y ejecución de prompts.
 */
@Component
public class McpProtocolHandler {
    
    private final Map<String, McpTool> availableTools = new HashMap<>();
    private final Map<String, McpPrompt> availablePrompts = new HashMap<>();
    
    @Autowired
    public McpProtocolHandler(List<McpTool> tools, List<McpPrompt> prompts) {
        for (McpTool tool : tools) {
            availableTools.put(tool.getName(), tool);
        }
        
        for (McpPrompt prompt : prompts) {
            availablePrompts.put(prompt.getName(), prompt);
        }
    }
    
    /**
     * Procesa la solicitud de inicialización del protocolo MCP.
     */
    public Map<String, Object> handleInitialize(InitializeRequest.InitializeParams params) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2025-06-18");
        
        Map<String, Object> capabilities = new HashMap<>();
        
        // Server capabilities
        Map<String, Object> tools = new HashMap<>();
        tools.put("listChanged", true);
        capabilities.put("tools", tools);
        
        // Add prompts capability
        Map<String, Object> prompts = new HashMap<>();
        prompts.put("listChanged", true);
        capabilities.put("prompts", prompts);
        
        // Add the capabilities as requested by client
        Map<String, Object> roots = new HashMap<>();
        roots.put("listChanged", true);
        capabilities.put("roots", roots);
        
        Map<String, Object> sampling = new HashMap<>();
        capabilities.put("sampling", sampling);
        
        Map<String, Object> elicitation = new HashMap<>();
        capabilities.put("elicitation", elicitation);
        
        result.put("capabilities", capabilities);
        
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "MCP Spring Boot Server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        
        return result;
    }
    
    /**
     * Procesa la solicitud de listado de herramientas.
     */
    public Map<String, Object> handleListTools() {
        Map<String, Object> result = new HashMap<>();
        List<Object> tools = new ArrayList<>();
        
        for (McpTool tool : availableTools.values()) {
            tools.add(tool.getToolDefinition());
        }
        
        result.put("tools", tools);
        return result;
    }
    
    /**
     * Procesa la solicitud de listado de prompts.
     */
    public Map<String, Object> handleListPrompts() {
        Map<String, Object> result = new HashMap<>();
        List<Object> prompts = new ArrayList<>();
        
        for (McpPrompt prompt : availablePrompts.values()) {
            prompts.add(prompt.getPromptDefinition().toMap());
        }
        
        result.put("prompts", prompts);
        return result;
    }
    
    /**
     * Procesa la solicitud de ejecución de prompt.
     */
    public Map<String, Object> handleGetPrompt(String promptName, Map<String, Object> arguments) {
        McpPrompt prompt = availablePrompts.get(promptName);
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown prompt: " + promptName);
        }
        
        return prompt.execute(arguments).toMap();
    }
    
    /**
     * Procesa la solicitud de ejecución de herramienta.
     */
    public Map<String, Object> handleCallTool(String toolName, Map<String, Object> arguments) {
        McpTool tool = availableTools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        
        return tool.execute(arguments);
    }
    
    /**
     * Procesa la solicitud de completion para argumentos de prompt.
     */
    public Map<String, Object> handleCompletionComplete(CompletionCompleteRequest.CompletionCompleteParams params) {
        // Para completion de prompts, proporcionamos sugerencias básicas
        List<Map<String, Object>> completions = new ArrayList<>();
        
        // Analizar el tipo de referencia y el argumento para completion
        if (params.getRef() != null && "ref/prompt".equals(params.getRef().getType())) {
            String promptName = params.getRef().getName();
            String argumentName = params.getArgument() != null ? params.getArgument().getName() : "";
            String currentValue = params.getArgument() != null ? params.getArgument().getValue() : "";
            
            // Para el prompt de configuración organizacional
            if ("generar_configuracion_organizacional".equals(promptName)) {
                if ("generar_backup".equals(argumentName)) {
                    // Sugerencias para el argumento generar_backup
                    if (currentValue.isEmpty() || "s".startsWith(currentValue.toLowerCase()) || "y".startsWith(currentValue.toLowerCase())) {
                        completions.add(Map.of(
                            "label", "sí",
                            "insertText", "sí",
                            "documentation", "Generar backup de archivos existentes antes de sobreescribir"
                        ));
                        completions.add(Map.of(
                            "label", "yes", 
                            "insertText", "yes",
                            "documentation", "Generate backup of existing files before overwriting"
                        ));
                    }
                    if (currentValue.isEmpty() || "n".startsWith(currentValue.toLowerCase())) {
                        completions.add(Map.of(
                            "label", "no",
                            "insertText", "no", 
                            "documentation", "No generar backup - sobreescribir archivos directamente"
                        ));
                    }
                }
            }
        }
        
        // Si no hay completions específicas, devolver lista vacía
        Map<String, Object> result = new HashMap<>();
        result.put("completions", completions);
        return result;
    }
    
    /**
     * Procesa una solicitud MCP y retorna la respuesta apropiada.
     */
    public McpResponse processRequest(McpRequest request) {
        try {
            Object result = switch (request.getMethod()) {
                case "initialize" -> {
                    InitializeRequest initReq = (InitializeRequest) request;
                    yield handleInitialize(initReq.getParams());
                }
                case "tools/list" -> handleListTools();
                case "tools/call" -> {
                    CallToolRequest callReq = (CallToolRequest) request;
                    yield handleCallTool(
                        callReq.getParams().getName(),
                        callReq.getParams().getArguments()
                    );
                }
                case "prompts/list" -> handleListPrompts();
                case "prompts/get" -> {
                    GetPromptRequest getPromptReq = (GetPromptRequest) request;
                    yield handleGetPrompt(
                        getPromptReq.getParams().getName(),
                        getPromptReq.getParams().getArguments()
                    );
                }
                case "completion/complete" -> {
                    CompletionCompleteRequest completionReq = (CompletionCompleteRequest) request;
                    yield handleCompletionComplete(completionReq.getParams());
                }
                default -> throw new IllegalArgumentException("Unknown method: " + request.getMethod());
            };
            
            return McpResponse.success(request.getId(), result);
            
        } catch (Exception e) {
            McpError error = McpError.internalError("Error processing request: " + e.getMessage());
            return McpResponse.error(request.getId(), error);
        }
    }
}
