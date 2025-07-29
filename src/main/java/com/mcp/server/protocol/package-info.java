/**
 * Definiciones del protocolo MCP (Model Context Protocol) siguiendo JSON-RPC 2.0.
 * 
 * <p>Este paquete implementa la especificación completa del protocolo MCP versión 2025-06-18,
 * que permite la comunicación estandarizada entre clientes de IA y servidores que
 * proporcionan herramientas y recursos.
 * 
 * <h2>Componentes del Protocolo:</h2>
 * <ul>
 *   <li><strong>Messages</strong> ({@link com.mcp.server.protocol.messages}): 
 *       Todas las solicitudes, respuestas y notificaciones JSON-RPC</li>
 *   <li><strong>Types</strong> ({@link com.mcp.server.protocol.types}): 
 *       Tipos de datos básicos como Tool, McpError</li>
 *   <li><strong>Handlers</strong> ({@link com.mcp.server.protocol.handlers}): 
 *       Procesadores que manejan la lógica del protocolo</li>
 * </ul>
 * 
 * <h2>Flujo del Protocolo MCP:</h2>
 * <pre>{@code
 * 1. Cliente → initialize → Servidor
 * 2. Cliente → tools/list → Servidor  
 * 3. Cliente → tools/call → Servidor
 * 4. [Repetir pasos 2-3 según necesidad]
 * }</pre>
 * 
 * <h2>Ejemplo de Mensaje MCP:</h2>
 * <pre>{@code
 * // Solicitud de inicialización
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "initialize",
 *   "params": {
 *     "protocolVersion": "2025-06-18",
 *     "capabilities": {},
 *     "clientInfo": {"name": "VSCode", "version": "1.0"}
 *   }
 * }
 * }</pre>
 * 
 * <h2>Características Implementadas:</h2>
 * <ul>
 *   <li>JSON-RPC 2.0 compliant</li>
 *   <li>Manejo robusto de errores</li>
 *   <li>Validación de esquemas JSON</li>
 *   <li>Soporte para notificaciones</li>
 *   <li>Deserialización polimórfica con Jackson</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://spec.modelcontextprotocol.io/">Especificación Oficial MCP</a>
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 * @see com.mcp.server.protocol.handlers.McpProtocolHandler
 */
package com.mcp.server.protocol;
