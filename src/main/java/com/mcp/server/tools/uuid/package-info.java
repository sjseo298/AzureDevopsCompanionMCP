/**
 * Herramienta de generación de UUIDs para el protocolo MCP.
 * 
 * <p>Este paquete implementa una herramienta MCP que proporciona capacidades
 * de generación de identificadores únicos universales (UUIDs) utilizando
 * la implementación nativa de Java. Es un ejemplo práctico de herramienta
 * MCP que demuestra las mejores prácticas de implementación.
 * 
 * <h2>Funcionalidades de la Herramienta:</h2>
 * <ul>
 *   <li><strong>UUID v4 (Random)</strong>: Generación de UUIDs aleatorios criptográficamente seguros</li>
 *   <li><strong>Sin Parámetros</strong>: No requiere argumentos de entrada</li>
 *   <li><strong>Formato Estándar</strong>: Output en formato RFC 4122 (8-4-4-4-12)</li>
 *   <li><strong>Thread-Safe</strong>: Seguro para uso concurrente</li>
 *   <li><strong>Alto Rendimiento</strong>: Implementación optimizada con {@link java.util.UUID}</li>
 * </ul>
 * 
 * <h2>Especificación de la Herramienta:</h2>
 * <table border="1">
 *   <tr><th>Propiedad</th><th>Valor</th></tr>
 *   <tr><td>Nombre</td><td><code>uuid_generator</code></td></tr>
 *   <tr><td>Descripción</td><td>Generates a random UUID using Java's UUID.randomUUID()</td></tr>
 *   <tr><td>Argumentos</td><td>Ninguno requerido</td></tr>
 *   <tr><td>Schema</td><td>Objeto vacío <code>{}</code></td></tr>
 *   <tr><td>Output</td><td><code>{"uuid": "string"}</code></td></tr>
 * </table>
 * 
 * <h2>Ejemplo de Uso:</h2>
 * <h3>Llamada JSON-RPC:</h3>
 * <pre>{@code
 * // Request
 * {
 *   "jsonrpc": "2.0",
 *   "id": "1",
 *   "method": "tools/call",
 *   "params": {
 *     "name": "uuid_generator",
 *     "arguments": {}
 *   }
 * }
 * 
 * // Response
 * {
 *   "jsonrpc": "2.0",
 *   "id": "1",
 *   "result": {
 *     "content": [
 *       {
 *         "type": "text",
 *         "text": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 * 
 * <h3>Código Java:</h3>
 * <pre>{@code
 * // Obtener instancia de la herramienta
 * McpTool uuidTool = new UuidGeneratorTool();
 * 
 * // Ejecutar con argumentos vacíos
 * McpResponse response = uuidTool.execute(Map.of());
 * 
 * // Extraer UUID generado
 * Map<String, Object> content = (Map<String, Object>) response.getResult().get("content");
 * String uuid = (String) content.get("text");
 * 
 * System.out.println("Generated UUID: " + uuid);
 * // Output: Generated UUID: 550e8400-e29b-41d4-a716-446655440000
 * }</pre>
 * 
 * <h2>Casos de Uso Comunes:</h2>
 * <ul>
 *   <li><strong>Identificadores de Sesión</strong>: Para tracking de sesiones de usuario</li>
 *   <li><strong>Request IDs</strong>: Identificadores únicos para trazabilidad</li>
 *   <li><strong>Database Keys</strong>: Claves primarias para entidades</li>
 *   <li><strong>File Names</strong>: Nombres únicos para archivos temporales</li>
 *   <li><strong>API Keys</strong>: Generación de tokens únicos</li>
 *   <li><strong>Testing</strong>: Datos de prueba únicos y predecibles</li>
 * </ul>
 * 
 * <h2>Características Técnicas:</h2>
 * <h3>Formato UUID v4:</h3>
 * <pre>
 * xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
 * 
 * Donde:
 * - x: dígito hexadecimal aleatorio [0-9a-f]
 * - 4: versión UUID (fijo)
 * - y: dígito en rango [8, 9, a, b]
 * 
 * Ejemplo: f47ac10b-58cc-4372-a567-0e02b2c3d479
 * </pre>
 * 
 * <h3>Propiedades de Calidad:</h3>
 * <ul>
 *   <li><strong>Unicidad</strong>: Probabilidad de colisión ≈ 5.3 × 10⁻³⁷</li>
 *   <li><strong>Aleatoriedad</strong>: 122 bits de entropía criptográfica</li>
 *   <li><strong>Performance</strong>: ~1M UUIDs/segundo en hardware moderno</li>
 *   <li><strong>Portabilidad</strong>: Compatible con todas las plataformas Java</li>
 * </ul>
 * 
 * <h2>Ejemplo - Integración con Spring:</h2>
 * <pre>{@code
 * @Component
 * public class UuidGeneratorTool implements McpTool {
 *     
 *     private static final Logger logger = LoggerFactory.getLogger(UuidGeneratorTool.class);
 *     
 *     @Override
 *     public McpResponse execute(Map<String, Object> arguments) {
 *         logger.debug("Generating new UUID");
 *         
 *         String uuid = UUID.randomUUID().toString();
 *         
 *         logger.debug("Generated UUID: {}", uuid);
 *         
 *         return McpResponse.success(Map.of(
 *             "content", List.of(Map.of(
 *                 "type", "text",
 *                 "text", uuid
 *             ))
 *         ));
 *     }
 * }
 * }</pre>
 * 
 * <h2>Testing y Validación:</h2>
 * <pre>{@code
 * @Test
 * void testUuidGeneration() {
 *     UuidGeneratorTool tool = new UuidGeneratorTool();
 *     
 *     // Ejecutar herramienta
 *     McpResponse response = tool.execute(Map.of());
 *     
 *     // Validar respuesta
 *     assertThat(response.isSuccess()).isTrue();
 *     
 *     // Extraer UUID
 *     String uuid = extractUuidFromResponse(response);
 *     
 *     // Validar formato
 *     assertThat(uuid).matches(UUID_REGEX_PATTERN);
 *     assertThat(UUID.fromString(uuid)).isNotNull(); // No lanza excepción
 * }
 * 
 * @Test
 * void testUuidUniqueness() {
 *     UuidGeneratorTool tool = new UuidGeneratorTool();
 *     Set<String> generatedUuids = new HashSet<>();
 *     
 *     // Generar 10,000 UUIDs
 *     for (int i = 0; i < 10_000; i++) {
 *         String uuid = extractUuidFromResponse(tool.execute(Map.of()));
 *         generatedUuids.add(uuid);
 *     }
 *     
 *     // Verificar que todos son únicos
 *     assertThat(generatedUuids).hasSize(10_000);
 * }
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see java.util.UUID
 * @see com.mcp.server.tools.base.McpTool
 * @see <a href="https://tools.ietf.org/html/rfc4122">RFC 4122 - UUID Specification</a>
 */
package com.mcp.server.tools.uuid;
