/**
 * Mensajes del protocolo MCP siguiendo la especificación JSON-RPC 2.0.
 * 
 * <p>Este paquete contiene todas las implementaciones de mensajes que componen
 * la comunicación entre cliente y servidor MCP. Cada mensaje está diseñado para
 * ser serializado/deserializado automáticamente por Jackson.
 * 
 * <h2>Jerarquía de Mensajes:</h2>
 * <pre>{@code
 * McpRequest (abstract)
 * ├── InitializeRequest        # Inicialización de sesión
 * ├── ListToolsRequest         # Solicitar lista de herramientas
 * └── CallToolRequest          # Ejecutar herramienta específica
 * 
 * McpResponse                  # Respuesta a cualquier solicitud
 * McpNotification              # Notificaciones sin respuesta
 * }</pre>
 * 
 * <h2>Tipos de Mensajes:</h2>
 * <ul>
 *   <li><strong>Requests</strong> ({@link com.mcp.server.protocol.messages.McpRequest}): 
 *       Solicitudes que requieren respuesta (incluyen ID único)</li>
 *   <li><strong>Responses</strong> ({@link com.mcp.server.protocol.messages.McpResponse}): 
 *       Respuestas a solicitudes (mismo ID que la request)</li>
 *   <li><strong>Notifications</strong> ({@link com.mcp.server.protocol.messages.McpNotification}): 
 *       Notificaciones unidireccionales (sin ID, no requieren respuesta)</li>
 * </ul>
 * 
 * <h2>Ejemplo de Request:</h2>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "tools/call",
 *   "params": {
 *     "name": "generate_uuid",
 *     "arguments": {}
 *   }
 * }
 * }</pre>
 * 
 * <h2>Ejemplo de Response:</h2>
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "result": {
 *     "content": [
 *       {"type": "text", "text": "Generated UUID: 123e4567-e89b-12d3-a456-426614174000"}
 *     ]
 *   }
 * }
 * }</pre>
 * 
 * <h2>Características Implementadas:</h2>
 * <ul>
 *   <li>Deserialización polimórfica basada en campo "method"</li>
 *   <li>Validación automática de formato JSON-RPC 2.0</li>
 *   <li>Inmutabilidad para thread-safety</li>
 *   <li>Manejo robusto de errores</li>
 *   <li>Soporte completo para Jackson annotations</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 * @see <a href="https://spec.modelcontextprotocol.io/">MCP Protocol Specification</a>
 * @see com.mcp.server.protocol.handlers.McpProtocolHandler
 */
package com.mcp.server.protocol.messages;
