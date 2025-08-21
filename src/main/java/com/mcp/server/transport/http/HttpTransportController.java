package com.mcp.server.transport.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.protocol.handlers.McpProtocolHandler;
import com.mcp.server.protocol.messages.McpRequest;
import com.mcp.server.protocol.messages.McpResponse;
import com.mcp.server.protocol.types.McpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador HTTP para el protocolo MCP.
 * 
 * Implementa el transporte "Streamable HTTP" según la especificación MCP 2025-06-18.
 * Permite acceso remoto al servidor MCP a través de HTTP POST/GET requests.
 * 
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http">MCP Streamable HTTP Transport</a>
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "mcp.http", havingValue = "true")
public class HttpTransportController {

    private static final Logger logger = LoggerFactory.getLogger(HttpTransportController.class);
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    @Autowired
    private McpProtocolHandler protocolHandler;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Maneja requests JSON-RPC vía HTTP POST.
     * 
     * @param request el request MCP en formato JSON-RPC
     * @param protocolVersion versión del protocolo MCP
     * @param sessionId ID de sesión (opcional)
     * @return respuesta MCP
     */
    @PostMapping
    public ResponseEntity<?> handleMcpRequest(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "MCP-Protocol-Version", defaultValue = "2025-03-26") String protocolVersion,
            @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
            @RequestHeader(value = "Accept", defaultValue = "application/json") String accept) {
        
        try {
            logger.debug("Received MCP request: {}", request);
            
            // Validar versión del protocolo
            if (!isValidProtocolVersion(protocolVersion)) {
                logger.warn("Invalid protocol version: {}", protocolVersion);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid protocol version: " + protocolVersion));
            }
            
            // Convertir el request map a McpRequest
            McpRequest mcpRequest = objectMapper.convertValue(request, McpRequest.class);
            
            // Procesar el request
            McpResponse response = protocolHandler.processRequest(mcpRequest);
            
            // Preparar headers de respuesta
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (sessionId != null) {
                headers.set("Mcp-Session-Id", sessionId);
            }
            
            logger.debug("Sending MCP response: {}", response);
            return ResponseEntity.ok().headers(headers).body(response);
            
        } catch (Exception e) {
            logger.error("Error processing MCP request", e);
            McpError error = McpError.internalError("Error processing request: " + e.getMessage());
            McpResponse errorResponse = McpResponse.error(
                request.get("id"), 
                error
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Maneja health check y información del servidor.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "protocol", "MCP",
            "version", MCP_PROTOCOL_VERSION,
            "transport", "HTTP",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Maneja CORS preflight requests.
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handlePreflight() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, MCP-Protocol-Version, Mcp-Session-Id, Accept");
        return ResponseEntity.ok().headers(headers).build();
    }

    private boolean isValidProtocolVersion(String version) {
        // Lista de versiones soportadas
        return "2025-06-18".equals(version) || "2025-03-26".equals(version);
    }

    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
            "jsonrpc", "2.0",
            "error", Map.of(
                "code", -32600,
                "message", message
            )
        );
    }
}
