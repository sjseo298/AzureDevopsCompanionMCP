package com.mcp.server.tools.uuid;

import com.mcp.server.tools.base.McpTool;
import com.mcp.server.protocol.types.Tool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implementación de la herramienta de generación de UUID.
 * 
 * <p>Esta herramienta genera un UUID aleatorio utilizando {@link UUID#randomUUID()}.
 * No requiere parámetros de entrada y devuelve el UUID como texto en formato estándar.
 * 
 * <p>Ejemplo de respuesta:
 * <pre>{@code
 * {
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "Generated UUID: 550e8400-e29b-41d4-a716-446655440000"
 *     }
 *   ]
 * }
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class UuidGeneratorTool implements McpTool {
    
    private static final String TOOL_NAME = "generate_uuid";
    private static final String DESCRIPTION = "Generate a random UUID using java.util.UUID";
    
    @Override
    public Tool getToolDefinition() {
        Map<String, Object> schemaMap = new HashMap<>();
        schemaMap.put("type", "object");
        schemaMap.put("properties", new HashMap<>());
        schemaMap.put("required", new ArrayList<>());
        
        return Tool.builder()
                .name(TOOL_NAME)
                .description(DESCRIPTION)
                .inputSchema(Tool.Schema.fromMap(schemaMap))
                .build();
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        // Validar argumentos (no deberían haber)
        if (arguments != null && !arguments.isEmpty()) {
            throw new IllegalArgumentException(
                "La herramienta generate_uuid no acepta argumentos, pero se recibieron: " + arguments.keySet()
            );
        }
        
        // Generar UUID
        String uuid = UUID.randomUUID().toString();
        
        // Crear respuesta en formato MCP usando HashMap y ArrayList para evitar inmutables
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        contentItem.put("text", "Generated UUID: " + uuid);
        
        List<Map<String, Object>> contentList = new ArrayList<>();
        contentList.add(contentItem);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", contentList);
        result.put("isError", false);
        
        return result;
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
