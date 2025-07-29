/**
 * Servidor MCP (Model Context Protocol) implementado con Spring Boot.
 * 
 * <p>Este paquete contiene la implementación completa de un servidor MCP que cumple
 * con la especificación oficial del protocolo. Está diseñado siguiendo principios
 * de Clean Architecture y SOLID.
 * 
 * <h2>Características Principales:</h2>
 * <ul>
 *   <li><strong>Protocolo MCP 2025-06-18</strong>: Implementación completa de la especificación</li>
 *   <li><strong>Transporte STDIO</strong>: Comunicación via entrada/salida estándar</li>
 *   <li><strong>Auto-descubrimiento</strong>: Las herramientas se registran automáticamente</li>
 *   <li><strong>Extensible</strong>: Fácil agregar nuevas herramientas sin modificar código</li>
 *   <li><strong>Robusto</strong>: Manejo de errores y validaciones completas</li>
 * </ul>
 * 
 * <h2>Arquitectura de Paquetes:</h2>
 * <ul>
 *   <li>{@link com.mcp.server.protocol} - Definición del protocolo MCP</li>
 *   <li>{@link com.mcp.server.tools} - Implementaciones de herramientas</li>
 *   <li>{@link com.mcp.server.transport} - Capa de transporte</li>
 *   <li>{@link com.mcp.server.config} - Configuración de la aplicación</li>
 * </ul>
 * 
 * <h2>Ejemplo de Uso:</h2>
 * <pre>{@code
 * // Ejecutar en modo STDIO
 * java -jar mcp-server.jar --mcp.stdio=true
 * 
 * // Comunicación JSON-RPC 2.0
 * {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {...}}
 * }</pre>
 * 
 * <h2>Herramientas Disponibles:</h2>
 * <ul>
 *   <li><strong>generate_uuid</strong>: Genera UUIDs aleatorios</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://spec.modelcontextprotocol.io/">Especificación MCP</a>
 * @see com.mcp.server.tools.base.McpTool
 * @see com.mcp.server.protocol.handlers.McpProtocolHandler
 */
package com.mcp.server;
