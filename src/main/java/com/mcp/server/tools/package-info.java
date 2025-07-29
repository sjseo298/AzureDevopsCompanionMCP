/**
 * Implementaciones de herramientas que expone el servidor MCP a los clientes.
 * 
 * <p>Este paquete contiene todas las herramientas ejecutables que el servidor
 * proporciona a los clientes MCP. Cada herramienta implementa funcionalidad específica
 * y se registra automáticamente mediante Spring's component scanning.
 * 
 * <h2>Arquitectura de Herramientas:</h2>
 * <ul>
 *   <li><strong>Base</strong> ({@link com.mcp.server.tools.base}): 
 *       Interfaces y clases base para implementar herramientas</li>
 *   <li><strong>UUID</strong> ({@link com.mcp.server.tools.uuid}): 
 *       Generador de identificadores únicos universales</li>
 *   <li><em>Calculator</em> (futuro): Operaciones matemáticas</li>
 *   <li><em>FileSystem</em> (futuro): Manipulación de archivos</li>
 * </ul>
 * 
 * <h2>Crear Nueva Herramienta:</h2>
 * <pre>{@code
 * @Component
 * public class CalculatorTool implements McpTool {
 *     
 *     @Override
 *     public Map<String, Object> execute(Map<String, Object> arguments) {
 *         // Implementar lógica de la calculadora
 *         Integer a = (Integer) arguments.get("a");
 *         Integer b = (Integer) arguments.get("b");
 *         return Map.of(
 *             "content", List.of(Map.of(
 *                 "type", "text", 
 *                 "text", "Resultado: " + (a + b)
 *             ))
 *         );
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return "calculator";
 *     }
 * }
 * }</pre>
 * 
 * <h2>Requisitos para Herramientas:</h2>
 * <ul>
 *   <li>Implementar {@link com.mcp.server.tools.base.McpTool}</li>
 *   <li>Anotar con {@code @Component} para auto-registro</li>
 *   <li>Definir esquema JSON Schema para validación</li>
 *   <li>Manejar errores de forma consistente</li>
 *   <li>Proporcionar documentación clara</li>
 * </ul>
 * 
 * <h2>Auto-descubrimiento:</h2>
 * <p>Las herramientas se registran automáticamente cuando:
 * <ol>
 *   <li>Están anotadas con {@code @Component}</li>
 *   <li>Implementan {@link com.mcp.server.tools.base.McpTool}</li>
 *   <li>Están en el classpath de Spring Boot</li>
 * </ol>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.mcp.server.tools.base.McpTool
 * @see com.mcp.server.tools.uuid.UuidGeneratorTool
 * @see com.mcp.server.protocol.handlers.McpProtocolHandler
 */
package com.mcp.server.tools;
