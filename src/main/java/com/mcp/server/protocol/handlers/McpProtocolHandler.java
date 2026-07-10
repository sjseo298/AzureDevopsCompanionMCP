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
    private final Map<String, String> availableResources = new LinkedHashMap<>();

    private static final Set<String> EXPOSED_TOOL_NAMES = Set.of(
            // Azure DevOps consolidated router tools
            "azuredevops_profile_identity",
            "azuredevops_core_projects",
            "azuredevops_core_teams",
            "azuredevops_core_processes",
            "azuredevops_core_avatars",
            "azuredevops_work_planning",
            "azuredevops_wit_work_items",
            "azuredevops_wit_comments",
            "azuredevops_wit_attachments",
            "azuredevops_wit_classification_nodes",
            "azuredevops_wit_queries",
            "azuredevops_wit_reporting",
            "azuredevops_git_api",
            "azuredevops_git_repositories",
            "azuredevops_git_pull_requests",
            "azuredevops_git_local"
    );
    
    @Autowired
    public McpProtocolHandler(List<McpTool> tools, List<McpPrompt> prompts) {
        for (McpTool tool : tools) {
            if (tool == null) continue;
            String name = null;
            try {
                name = tool.getName();
            } catch (Exception ignored) {
                // ignore
            }
            if (name == null || name.isBlank()) continue;
            if (!EXPOSED_TOOL_NAMES.contains(name)) continue;
            availableTools.put(name, tool);
        }
        
        for (McpPrompt prompt : prompts) {
            availablePrompts.put(prompt.getName(), prompt);
        }

        availableResources.put("azuredevops-mcp://config/index", configIndexResource());
        availableResources.put("azuredevops-mcp://config/opencode", opencodeConfigResource());
        availableResources.put("azuredevops-mcp://config/vscode", vscodeConfigResource());
        availableResources.put("azuredevops-mcp://config/docker-script", dockerScriptConfigResource());
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

        Map<String, Object> resources = new HashMap<>();
        resources.put("listChanged", false);
        capabilities.put("resources", resources);
        
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
            try {
                if (tool == null) {
                    System.err.println("[MCP][tools/list] Bean McpTool nulo encontrado - se ignora");
                    continue;
                }
                var def = tool.getToolDefinition();
                if (def == null) {
                    System.err.println("[MCP][tools/list] getToolDefinition() devolvió null para: " + tool.getClass().getName());
                    continue; // evitar NPE
                }
                tools.add(def);
            } catch (NullPointerException npe) {
                System.err.println("[MCP][tools/list] NullPointer al procesar tool: " + tool.getClass().getName());
                npe.printStackTrace();
            } catch (Exception ex) {
                System.err.println("[MCP][tools/list] Error al registrar tool " + tool.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
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
     * Procesa la solicitud de listado de recursos MCP.
     */
    public Map<String, Object> handleListResources() {
        Map<String, Object> result = new HashMap<>();
        List<Object> resources = new ArrayList<>();

        resources.add(resourceDefinition(
                "azuredevops-mcp://config/index",
                "configuration-index",
                "Azure DevOps MCP Configuration Index",
                "Indice de recursos para configurar este MCP en otros proyectos.",
                0.8
        ));
        resources.add(resourceDefinition(
                "azuredevops-mcp://config/opencode",
                "opencode-configuration",
                "opencode MCP Configuration",
                "Plantilla Markdown para configurar .opencode/opencode.json y el script local.",
                1.0
        ));
        resources.add(resourceDefinition(
                "azuredevops-mcp://config/vscode",
                "vscode-configuration",
                "VS Code MCP Configuration",
                "Plantilla Markdown para configurar .vscode/mcp.json.",
                1.0
        ));
        resources.add(resourceDefinition(
                "azuredevops-mcp://config/docker-script",
                "docker-script-configuration",
                "Docker stdio-http Startup Script",
                "Script recomendado para arrancar el MCP en opencode sin fallar si el puerto HTTP esperado ya esta ocupado.",
                0.9
        ));

        result.put("resources", resources);
        return result;
    }

    /**
     * Procesa la solicitud de lectura de un recurso MCP.
     */
    public Map<String, Object> handleReadResource(String uri) {
        String text = availableResources.get(uri);
        if (text == null) {
            throw new IllegalArgumentException("Unknown resource: " + uri);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "text/markdown",
                "text", text
        )));
        return result;
    }

    private Map<String, Object> resourceDefinition(String uri, String name, String title, String description, double priority) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("uri", uri);
        resource.put("name", name);
        resource.put("title", title);
        resource.put("description", description);
        resource.put("mimeType", "text/markdown");
        resource.put("annotations", Map.of(
                "audience", List.of("user", "assistant"),
                "priority", priority
        ));
        return resource;
    }

    private String configIndexResource() {
        return """
                # Azure DevOps MCP Configuration Resources

                Use these resources when configuring this Azure DevOps MCP server in another project.

                Available resources:

                - `azuredevops-mcp://config/opencode`: opencode project configuration using `.opencode/opencode.json`.
                - `azuredevops-mcp://config/vscode`: VS Code MCP configuration using `.vscode/mcp.json`.
                - `azuredevops-mcp://config/docker-script`: optional startup script for opencode that avoids Docker failures when the expected HTTP upload port is already in use.

                The recommended Docker image tag is `mcp-azure-devops:latest`.

                The expected HTTP upload base URL for local `stdio-http` usage is:

                ```text
                http://127.0.0.1:9091
                ```
                """;
    }

    private String opencodeConfigResource() {
        return """
                # opencode MCP Configuration

                Use this resource when configuring this Azure DevOps MCP server in an opencode project.

                ## Target File

                `.opencode/opencode.json`

                ## Requirements

                - Docker image: `mcp-azure-devops:latest`
                - Env file in the project, usually `.env`
                - Required env values:
                  - `AZURE_DEVOPS_ORGANIZATION`
                  - `AZURE_DEVOPS_PAT`
                - Optional startup script: `scripts/opencode-mcp-azure-devops.sh`

                ## Recommended Configuration

                Replace `/path/to/project` with the target project absolute path.

                ```json
                {
                  "$schema": "https://opencode.ai/config.json",
                  "mcp": {
                    "azure-devops": {
                      "type": "local",
                      "cwd": "/path/to/project",
                      "command": [
                        "/usr/bin/env",
                        "bash",
                        "/path/to/project/scripts/opencode-mcp-azure-devops.sh"
                      ],
                      "enabled": true,
                      "timeout": 30000
                    }
                  }
                }
                ```

                ## Behavior

                The script starts the Docker container in `stdio-http` mode.

                It expects `127.0.0.1:9091` for HTTP upload URLs. If that port is free, the script publishes it with `-p 127.0.0.1:9091:8080`. If the port is already in use, the script does not publish a new port and assumes the existing service on `9091` is a compatible instance of this same image.

                Restart opencode after changing `.opencode/opencode.json`.
                """;
    }

    private String vscodeConfigResource() {
        return """
                # VS Code MCP Configuration

                Use this resource when configuring this Azure DevOps MCP server in VS Code.

                ## Target File

                `.vscode/mcp.json`

                ## Docker stdio-http Configuration

                This configuration starts the server through Docker, uses MCP over STDIO, and publishes HTTP uploads on `127.0.0.1:9091`.

                ```json
                {
                  "inputs": [
                    {
                      "id": "azure_devops_org",
                      "type": "promptString",
                      "description": "Azure DevOps organization name (e.g. 'contoso')"
                    },
                    {
                      "id": "azure_devops_pat",
                      "type": "promptString",
                      "description": "Azure DevOps Personal Access Token (PAT)"
                    }
                  ],
                  "servers": {
                    "azure-devops": {
                      "command": "docker",
                      "args": [
                        "run",
                        "--rm",
                        "-i",
                        "-p",
                        "127.0.0.1:9091:8080",
                        "--env",
                        "MCP_PUBLIC_BASE_URL=http://127.0.0.1:9091",
                        "--env",
                        "AZURE_DEVOPS_ORGANIZATION=${input:azure_devops_org}",
                        "--env",
                        "AZURE_DEVOPS_PAT=${input:azure_devops_pat}",
                        "mcp-azure-devops:latest",
                        "stdio-http"
                      ]
                    }
                  }
                }
                ```

                If another local instance already publishes `127.0.0.1:9091`, do not start a second VS Code configuration with the same `-p` mapping. Either reuse the existing compatible service for uploads, choose a different host port, or use the opencode startup script pattern from `azuredevops-mcp://config/docker-script`.
                """;
    }

    private String dockerScriptConfigResource() {
        return """
                # Docker stdio-http Startup Script

                Use this script in projects where Docker should not fail when the expected upload port is already in use.

                ## Target File

                `scripts/opencode-mcp-azure-devops.sh`

                ## Script

                ```bash
                #!/usr/bin/env bash
                set -euo pipefail

                SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
                PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

                IMAGE="${AZURE_DEVOPS_MCP_IMAGE:-mcp-azure-devops:latest}"
                ENV_FILE="${AZURE_DEVOPS_MCP_ENV_FILE:-${PROJECT_ROOT}/.env}"
                HOST="${AZURE_DEVOPS_MCP_HOST:-127.0.0.1}"
                PORT="${AZURE_DEVOPS_MCP_HTTP_PORT:-9091}"

                docker_args=(
                  run --rm -i
                  --pull never
                  --env "MCP_PUBLIC_BASE_URL=http://${HOST}:${PORT}"
                )

                if [[ -f "${ENV_FILE}" ]]; then
                  docker_args+=(--env-file "${ENV_FILE}")
                fi

                port_available=true
                if docker ps --format '{{.Ports}}' | grep -Eq "(^|, )(${HOST}|0\\.0\\.0\\.0|:::|\\[::\\]):${PORT}->|(^|, )${PORT}->"; then
                  port_available=false
                fi

                if [[ "${port_available}" == true ]]; then
                  docker_args+=(-p "${HOST}:${PORT}:8080")
                else
                  echo "WARN: ${HOST}:${PORT} is already in use; starting MCP stdio-http without publishing HTTP port. Upload URLs will use the existing service on that port." >&2
                fi

                exec docker "${docker_args[@]}" "${IMAGE}" stdio-http
                ```

                ## Environment Variables

                - `AZURE_DEVOPS_MCP_IMAGE`: Docker image to run. Default: `mcp-azure-devops:latest`.
                - `AZURE_DEVOPS_MCP_ENV_FILE`: env file passed to Docker. Default: `<project>/.env`.
                - `AZURE_DEVOPS_MCP_HOST`: host for HTTP publication. Default: `127.0.0.1`.
                - `AZURE_DEVOPS_MCP_HTTP_PORT`: expected HTTP upload port. Default: `9091`.

                The script always passes `MCP_PUBLIC_BASE_URL=http://127.0.0.1:9091` by default. If `9091` is already in use, it does not publish a new port and assumes the existing service on that port is a compatible instance of this image.
                """;
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
                case "resources/list" -> handleListResources();
                case "resources/read" -> {
                    ReadResourceRequest readResourceReq = (ReadResourceRequest) request;
                    if (readResourceReq.getParams() == null || readResourceReq.getParams().getUri() == null) {
                        throw new IllegalArgumentException("Missing resource uri");
                    }
                    yield handleReadResource(readResourceReq.getParams().getUri());
                }
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
                case "ping" -> "pong";
                default -> throw new IllegalArgumentException("Unknown method: " + request.getMethod());
            };
            
            return McpResponse.success(request.getId(), result);
            
        } catch (Exception e) {
            McpError error = McpError.internalError("Error processing request: " + e.getMessage());
            return McpResponse.error(request.getId(), error);
        }
    }
}
