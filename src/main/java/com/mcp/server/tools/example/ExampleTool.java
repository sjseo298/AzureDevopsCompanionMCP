package com.mcp.server.tools.example;

import com.mcp.server.protocol.types.Tool;
import com.mcp.server.tools.base.McpTool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Herramienta de ejemplo para la plantilla MCP.
 * Genera un UUID simple como ejemplo.
 */
@Component
public class ExampleTool implements McpTool {

    @Override
    public String getName() {
        return "generate_uuid";
    }

    @Override
    public Tool getToolDefinition() {
        return Tool.builder()
            .name(getName())
            .description("Genera un UUID simple como ejemplo de herramienta MCP.")
            .inputSchema(Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", Map.of()
            ))
            .build();
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        String uuid = java.util.UUID.randomUUID().toString();
        
        return Map.of(
            "uuid", uuid,
            "message", "UUID generado exitosamente: " + uuid,
            "timestamp", System.currentTimeMillis()
        );
    }
}
