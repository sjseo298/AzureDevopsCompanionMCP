/**
 * Arquitectura base para herramientas MCP (Model Context Protocol).
 * 
 * <p>Este paquete define la infraestructura fundamental para la implementación
 * de herramientas en el servidor MCP. Proporciona interfaces, clases base y
 * utilidades que simplifican el desarrollo de nuevas herramientas mientras
 * garantizan consistencia y cumplimiento del protocolo.
 * 
 * <h2>Arquitectura de Herramientas:</h2>
 * <pre>
 * ┌─────────────────────────┐
 * │      McpTool            │  ← Interface principal
 * │   (Functional I/F)      │
 * └─────────────────────────┘
 *              ▲
 *              │ implements
 * ┌─────────────────────────┐
 * │   AbstractMcpTool       │  ← Clase base opcional
 * │   (Common functionality)│
 * └─────────────────────────┘
 *              ▲
 *              │ extends
 * ┌─────────────────────────┐
 * │   UuidGeneratorTool     │  ← Implementación concreta
 * │   CalendarTool, etc.    │
 * └─────────────────────────┘
 * </pre>
 * 
 * <h2>Componentes Principales:</h2>
 * <dl>
 *   <dt><strong>McpTool</strong> ({@link com.mcp.server.tools.base.McpTool})</dt>
 *   <dd>
 *     Interface funcional que define el contrato para todas las herramientas.
 *     Proporciona métodos de conveniencia y valores por defecto inteligentes.
 *   </dd>
 *   
 *   <dt><strong>AbstractMcpTool</strong> (futuro)</dt>
 *   <dd>
 *     Clase base opcional que implementa funcionalidad común como validación
 *     de argumentos, logging y métricas.
 *   </dd>
 *   
 *   <dt><strong>ToolValidator</strong> (futuro)</dt>
 *   <dd>
 *     Utilidades para validación de esquemas JSON y argumentos de entrada.
 *   </dd>
 * </dl>
 * 
 * <h2>Contrato de Herramientas:</h2>
 * <p>Toda herramienta MCP debe implementar:</p>
 * <ul>
 *   <li><strong>Nombre único</strong>: Identificador string sin espacios</li>
 *   <li><strong>Descripción</strong>: Texto legible explicando la funcionalidad</li>
 *   <li><strong>Esquema de entrada</strong>: JSON Schema validando argumentos</li>
 *   <li><strong>Lógica de ejecución</strong>: Implementación thread-safe</li>
 *   <li><strong>Manejo de errores</strong>: Transformación a McpResponse apropiada</li>
 * </ul>
 * 
 * <h2>Ejemplo - Implementación Básica:</h2>
 * <pre>{@code
 * @Component
 * public class CalculatorTool implements McpTool {
 *     
 *     @Override
 *     public String getName() {
 *         return "calculator";
 *     }
 *     
 *     @Override
 *     public String getDescription() {
 *         return "Performs basic mathematical operations";
 *     }
 *     
 *     @Override
 *     public Map<String, Object> getInputSchema() {
 *         return Map.of(
 *             "type", "object",
 *             "properties", Map.of(
 *                 "operation", Map.of("type", "string", "enum", List.of("+", "-", "*", "/")),
 *                 "a", Map.of("type", "number"),
 *                 "b", Map.of("type", "number")
 *             ),
 *             "required", List.of("operation", "a", "b")
 *         );
 *     }
 *     
 *     @Override
 *     public McpResponse execute(Map<String, Object> arguments) {
 *         // Validación automática via esquema
 *         String op = (String) arguments.get("operation");
 *         Number a = (Number) arguments.get("a");
 *         Number b = (Number) arguments.get("b");
 *         
 *         double result = switch (op) {
 *             case "+" -> a.doubleValue() + b.doubleValue();
 *             case "-" -> a.doubleValue() - b.doubleValue();
 *             case "*" -> a.doubleValue() * b.doubleValue();
 *             case "/" -> {
 *                 if (b.doubleValue() == 0) {
 *                     yield Double.NaN; // O lanzar excepción
 *                 }
 *                 yield a.doubleValue() / b.doubleValue();
 *             }
 *             default -> throw new IllegalArgumentException("Invalid operation: " + op);
 *         };
 *         
 *         return McpResponse.success(Map.of("result", result));
 *     }
 * }
 * }</pre>
 * 
 * <h2>Ejemplo - Con Validación Avanzada:</h2>
 * <pre>{@code
 * @Override
 * public McpResponse execute(Map<String, Object> arguments) {
 *     try {
 *         // Validación de entrada
 *         validateArguments(arguments);
 *         
 *         // Lógica de negocio
 *         Object result = performOperation(arguments);
 *         
 *         // Respuesta estructurada
 *         return McpResponse.success(Map.of(
 *             "result", result,
 *             "metadata", Map.of(
 *                 "executedAt", Instant.now(),
 *                 "toolVersion", "1.0.0"
 *             )
 *         ));
 *         
 *     } catch (IllegalArgumentException e) {
 *         return McpResponse.error(McpError.invalidParams(e.getMessage()));
 *     } catch (Exception e) {
 *         logger.error("Unexpected error in tool execution", e);
 *         return McpResponse.error(McpError.internalError("Tool execution failed"));
 *     }
 * }
 * }</pre>
 * 
 * <h2>Principios de Diseño:</h2>
 * <ul>
 *   <li><strong>Stateless</strong>: Herramientas no mantienen estado entre ejecuciones</li>
 *   <li><strong>Thread-Safe</strong>: Seguras para ejecución concurrente</li>
 *   <li><strong>Fail-Fast</strong>: Validación temprana y errores descriptivos</li>
 *   <li><strong>Composable</strong>: Fácil integración con Spring y otros frameworks</li>
 *   <li><strong>Testable</strong>: Interfaces claras facilitan testing unitario</li>
 * </ul>
 * 
 * <h2>Registro y Descubrimiento:</h2>
 * <pre>{@code
 * // Registro automático via Spring
 * @Configuration
 * public class ToolConfiguration {
 *     
 *     @Bean
 *     public ToolRegistry toolRegistry(List<McpTool> tools) {
 *         ToolRegistry registry = new ToolRegistry();
 *         tools.forEach(registry::register);
 *         return registry;
 *     }
 * }
 * 
 * // Uso en servicio
 * @Service
 * public class McpService {
 *     
 *     @Autowired
 *     private ToolRegistry toolRegistry;
 *     
 *     public List<Tool> listTools() {
 *         return toolRegistry.getAllTools()
 *                           .stream()
 *                           .map(McpTool::toTool)
 *                           .toList();
 *     }
 * }
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.mcp.server.protocol.types.Tool
 * @see com.mcp.server.service.McpService
 * @see <a href="https://json-schema.org/draft/2020-12/schema">JSON Schema Draft 2020-12</a>
 */
package com.mcp.server.tools.base;
