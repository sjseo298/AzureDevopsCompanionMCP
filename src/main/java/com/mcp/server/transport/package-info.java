/**
 * Implementaciones de transporte para el protocolo MCP (Model Context Protocol).
 * 
 * <p>Este paquete define la capa de transporte que maneja la comunicación entre
 * el cliente y servidor MCP. Proporciona abstracción para diferentes protocolos
 * de comunicación manteniendo compatibilidad con la especificación MCP.
 * 
 * <h2>Transportes Disponibles:</h2>
 * <ul>
 *   <li><strong>STDIO</strong> ({@link com.mcp.server.transport.stdio}): 
 *       Comunicación vía entrada/salida estándar (pipes)</li>
 *   <li><em>WebSocket</em> (futuro): Comunicación bidireccional para aplicaciones web</li>
 *   <li><em>HTTP</em> (futuro): API REST para integraciones síncronas</li>
 * </ul>
 * 
 * <h2>Arquitectura de Transporte:</h2>
 * <pre>{@code
 * Cliente MCP
 *     ↓
 * [Transport Layer]  ← Este paquete
 *     ↓
 * Protocol Handler
 *     ↓
 * Tool Execution
 * }</pre>
 * 
 * <h2>Implementar Nuevo Transporte:</h2>
 * <ol>
 *   <li>Crear subpaquete (ej: {@code websocket/})</li>
 *   <li>Implementar handler que procese mensajes JSON-RPC 2.0</li>
 *   <li>Integrar con {@link com.mcp.server.protocol.handlers.McpProtocolHandler}</li>
 *   <li>Configurar en {@code application.yml}</li>
 * </ol>
 * 
 * <h2>Características Comunes:</h2>
 * <ul>
 *   <li>Formato JSON-RPC 2.0 según especificación MCP</li>
 *   <li>Manejo de errores consistente</li>
 *   <li>Logging estructurado para debugging</li>
 *   <li>Validación de mensajes de entrada</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://spec.modelcontextprotocol.io/">Especificación MCP</a>
 * @see com.mcp.server.protocol.handlers.McpProtocolHandler
 * @see com.mcp.server.transport.stdio.StdioTransportHandler
 */
package com.mcp.server.transport;
