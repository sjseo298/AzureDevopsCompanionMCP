/**
 * Manejadores de mensajes del protocolo MCP (Model Context Protocol).
 * 
 * <p>Este paquete implementa la capa de procesamiento de mensajes que actúa
 * como intermediario entre el transporte de datos y la lógica de negocio.
 * Los manejadores son responsables del ruteo, validación y transformación
 * de mensajes JSON-RPC según la especificación MCP.
 * 
 * <h2>Arquitectura de Manejadores:</h2>
 * <pre>
 * ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 * │   Transport     │───▶│   Handlers      │───▶│   Services      │
 * │   (STDIO/WS)    │    │   (Routing)     │    │   (Business)    │
 * └─────────────────┘    └─────────────────┘    └─────────────────┘
 * </pre>
 * 
 * <h2>Responsabilidades Principales:</h2>
 * <ul>
 *   <li><strong>Deserialización</strong>: JSON → Objetos Java strongly-typed</li>
 *   <li><strong>Ruteo</strong>: Dirigir mensajes a servicios apropiados</li>
 *   <li><strong>Validación</strong>: Verificar estructura y contenido de mensajes</li>
 *   <li><strong>Serialización</strong>: Objetos Java → JSON responses</li>
 *   <li><strong>Error Handling</strong>: Transformar excepciones en errores JSON-RPC</li>
 * </ul>
 * 
 * <h2>Tipos de Mensajes Soportados:</h2>
 * <dl>
 *   <dt><strong>Métodos Core</strong></dt>
 *   <dd>
 *     <ul>
 *       <li>initialize - Establecer conexión y capacidades</li>
 *       <li>ping - Health check y keep-alive</li>
 *       <li>shutdown - Terminación ordenada</li>
 *     </ul>
 *   </dd>
 *   
 *   <dt><strong>Métodos de Herramientas</strong></dt>
 *   <dd>
 *     <ul>
 *       <li>tools/list - Enumerar herramientas disponibles</li>
 *       <li>tools/call - Ejecutar herramienta específica</li>
 *     </ul>
 *   </dd>
 *   
 *   <dt><strong>Métodos de Recursos</strong> (futuro)</dt>
 *   <dd>
 *     <ul>
 *       <li>resources/list - Enumerar recursos disponibles</li>
 *       <li>resources/read - Leer contenido de recurso</li>
 *     </ul>
 *   </dd>
 * </dl>
 * 
 * <h2>Ejemplo - Procesamiento de Mensajes:</h2>
 * <pre>{@code
 * // Mensaje entrante JSON-RPC
 * {
 *   "jsonrpc": "2.0",
 *   "id": "123",
 *   "method": "tools/call",
 *   "params": {
 *     "name": "uuid_generator",
 *     "arguments": {}
 *   }
 * }
 * 
 * // Handler routing
 * String method = request.getMethod();
 * if (method.startsWith("tools/")) {
 *     return toolHandler.handle(request);
 * }
 * 
 * // Service execution
 * McpResponse response = toolService.callTool(
 *     request.getParams().getName(),
 *     request.getParams().getArguments()
 * );
 * }</pre>
 * 
 * <h2>Patrones de Implementación:</h2>
 * <ul>
 *   <li><strong>Strategy Pattern</strong>: Diferentes handlers para tipos de mensaje</li>
 *   <li><strong>Chain of Responsibility</strong>: Pipeline de procesamiento</li>
 *   <li><strong>Error Translation</strong>: Exception → JSON-RPC Error</li>
 *   <li><strong>Async Processing</strong>: Soporte para operaciones no bloqueantes</li>
 * </ul>
 * 
 * <h2>Manejo de Errores:</h2>
 * <pre>{@code
 * try {
 *     McpResponse response = serviceMethod(request);
 *     return successResponse(response);
 * } catch (ValidationException e) {
 *     return errorResponse(McpError.invalidParams(e.getMessage()));
 * } catch (ToolNotFoundException e) {
 *     return errorResponse(McpError.toolNotFound(e.getToolName()));
 * } catch (Exception e) {
 *     return errorResponse(McpError.internalError("Unexpected error"));
 * }
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see com.mcp.server.protocol.messages
 * @see com.mcp.server.service
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
package com.mcp.server.protocol.handlers;
