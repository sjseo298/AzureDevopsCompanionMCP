/**
 * Tipos de datos fundamentales del protocolo MCP (Model Context Protocol).
 * 
 * <p>Este paquete define las estructuras de datos básicas que componen
 * el protocolo MCP, incluyendo definiciones de herramientas, manejo de errores
 * y otros tipos de datos comunes utilizados en la comunicación.
 * 
 * <h2>Tipos Principales:</h2>
 * <ul>
 *   <li><strong>Tool</strong> ({@link com.mcp.server.protocol.types.Tool}): 
 *       Definición completa de herramientas con esquemas JSON</li>
 *   <li><strong>McpError</strong> ({@link com.mcp.server.protocol.types.McpError}): 
 *       Manejo de errores compatible con JSON-RPC 2.0</li>
 *   <li><em>Resource</em> (futuro): Definición de recursos disponibles</li>
 *   <li><em>Capability</em> (futuro): Capacidades del servidor/cliente</li>
 * </ul>
 * 
 * <h2>Patrón de Diseño:</h2>
 * <p>Todas las clases en este paquete siguen los principios:
 * <ul>
 *   <li><strong>Inmutabilidad</strong>: Objects are immutable after creation</li>
 *   <li><strong>Builder Pattern</strong>: Construcción fluida y validada</li>
 *   <li><strong>Factory Methods</strong>: Métodos de conveniencia para casos comunes</li>
 *   <li><strong>JSON-first</strong>: Diseñados para serialización automática</li>
 * </ul>
 * 
 * <h2>Ejemplo - Definición de Tool:</h2>
 * <pre>{@code
 * Tool calculatorTool = Tool.builder()
 *     .name("calculator")
 *     .description("Realiza operaciones matemáticas básicas")
 *     .inputSchema(Map.of(
 *         "type", "object",
 *         "properties", Map.of(
 *             "operation", Map.of("type", "string", "enum", List.of("+", "-", "*", "/")),
 *             "a", Map.of("type", "number"),
 *             "b", Map.of("type", "number")
 *         ),
 *         "required", List.of("operation", "a", "b")
 *     ))
 *     .build();
 * }</pre>
 * 
 * <h2>Ejemplo - Manejo de Errores:</h2>
 * <pre>{@code
 * // Error estándar JSON-RPC
 * McpError parseError = McpError.parseError("Invalid JSON received");
 * 
 * // Error específico de herramienta
 * McpError toolError = McpError.toolNotFound("calculator");
 * 
 * // Error personalizado
 * McpError customError = new McpError(-32000, "Custom error message");
 * }</pre>
 * 
 * <h2>Validación y Seguridad:</h2>
 * <ul>
 *   <li>Validación de entrada en constructores</li>
 *   <li>Null-safety mediante Objects.requireNonNull</li>
 *   <li>Esquemas JSON Schema Draft 7 compliant</li>
 *   <li>Códigos de error estandarizados</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://json-schema.org/">JSON Schema Specification</a>
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Error Codes</a>
 * @see com.mcp.server.tools.base.McpTool
 */
package com.mcp.server.protocol.types;
