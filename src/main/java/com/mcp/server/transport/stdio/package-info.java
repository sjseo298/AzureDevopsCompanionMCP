/**
 * Implementación del transporte STDIO para el protocolo MCP (Model Context Protocol).
 * 
 * <p>Este paquete proporciona la implementación del transporte basado en flujos
 * estándar (stdin/stdout) que permite la comunicación entre el servidor MCP y
 * clientes que ejecutan el servidor como proceso hijo. Es el método de transporte
 * preferido para integraciones con IDEs y editores de código.
 * 
 * <h2>Arquitectura de Transporte STDIO:</h2>
 * <pre>
 * ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 * │   Client        │───▶│   STDIO Pipes   │───▶│   MCP Server    │
 * │   (IDE/Editor)  │    │   (stdin/out)   │    │   (Java App)    │
 * │                 │◀───│                 │◀───│                 │
 * └─────────────────┘    └─────────────────┘    └─────────────────┘
 * </pre>
 * 
 * <h2>Características Principales:</h2>
 * <ul>
 *   <li><strong>Comunicación Bidireccional</strong>: Request/Response vía pipes</li>
 *   <li><strong>JSON-RPC 2.0</strong>: Un mensaje JSON por línea (line-delimited)</li>
 *   <li><strong>Process-Safe</strong>: Manejo robusto de señales y terminación</li>
 *   <li><strong>Buffering Inteligente</strong>: Optimización de I/O para performance</li>
 *   <li><strong>Error Recovery</strong>: Recuperación graceful de errores de stream</li>
 * </ul>
 * 
 * <h2>Protocolo de Comunicación:</h2>
 * <h3>Flujo de Inicio:</h3>
 * <pre>{@code
 * 1. Cliente ejecuta: java -jar mcp-server.jar --stdio
 * 2. Servidor detecta modo STDIO automáticamente
 * 3. Cliente envía mensaje de inicialización
 * 4. Servidor responde con capacidades disponibles
 * 5. Comunicación JSON-RPC establecida
 * }</pre>
 * 
 * <h3>Formato de Mensajes:</h3>
 * <pre>{@code
 * // Mensaje de entrada (stdin)
 * {"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}
 * 
 * // Respuesta de salida (stdout)
 * {"jsonrpc":"2.0","id":"1","result":{"tools":[...]}}
 * 
 * // Error de salida (stdout)
 * {"jsonrpc":"2.0","id":"1","error":{"code":-32601,"message":"Method not found"}}
 * }</pre>
 * 
 * <h2>Implementación de Components:</h2>
 * <dl>
 *   <dt><strong>StdioTransport</strong></dt>
 *   <dd>Coordinador principal del transporte STDIO</dd>
 *   
 *   <dt><strong>MessageReader</strong></dt>
 *   <dd>Lee y parsea mensajes JSON desde stdin</dd>
 *   
 *   <dt><strong>MessageWriter</strong></dt>
 *   <dd>Serializa y escribe respuestas a stdout</dd>
 *   
 *   <dt><strong>StreamHandler</strong></dt>
 *   <dd>Maneja el ciclo de vida de streams y buffers</dd>
 * </dl>
 * 
 * <h2>Ejemplo - Configuración STDIO:</h2>
 * <pre>{@code
 * // Detección automática en main()
 * public static void main(String[] args) {
 *     if (Arrays.asList(args).contains("--stdio")) {
 *         // Modo STDIO - no iniciar servidor web
 *         StdioTransport transport = new StdioTransport();
 *         transport.start();
 *     } else {
 *         // Modo servidor web normal
 *         SpringApplication.run(McpServerApplication.class, args);
 *     }
 * }
 * 
 * // Handler de mensajes
 * StdioMessageHandler handler = new StdioMessageHandler();
 * handler.handleMessage(inputLine)
 *        .thenAccept(response -> outputWriter.writeLine(response));
 * }</pre>
 * 
 * <h2>Manejo de Errores y Robustez:</h2>
 * <ul>
 *   <li><strong>Stream Interruption</strong>: Graceful shutdown cuando cliente termina</li>
 *   <li><strong>JSON Parsing Errors</strong>: Respuestas de error apropiadas</li>
 *   <li><strong>Buffer Overflow</strong>: Límites de memoria para mensajes grandes</li>
 *   <li><strong>Signal Handling</strong>: SIGTERM, SIGINT para terminación limpia</li>
 * </ul>
 * 
 * <h2>Ventajas del Transporte STDIO:</h2>
 * <ul>
 *   <li>✅ <strong>Simplicidad</strong>: No requiere configuración de red</li>
 *   <li>✅ <strong>Seguridad</strong>: Comunicación local, no expone puertos</li>
 *   <li>✅ <strong>Performance</strong>: Latencia mínima via pipes del OS</li>
 *   <li>✅ <strong>Compatibilidad</strong>: Funciona en todos los sistemas operativos</li>
 *   <li>✅ <strong>Aislamiento</strong>: Proceso aislado por cliente</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.mcp.server.transport
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Process.html">Java Process API</a>
 */
package com.mcp.server.transport.stdio;
