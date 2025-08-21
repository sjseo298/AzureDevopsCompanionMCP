package com.mcp.server.transport.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.protocol.handlers.McpProtocolHandler;
import com.mcp.server.protocol.messages.McpRequest;
import com.mcp.server.protocol.messages.McpResponse;
import com.mcp.server.protocol.types.McpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manejador de transporte WebSocket para el protocolo MCP.
 * 
 * Proporciona comunicación bidireccional en tiempo real para clientes web
 * y aplicaciones que requieren notificaciones del servidor.
 */
@Component
@ConditionalOnProperty(name = "mcp.websocket", havingValue = "true")
public class WebSocketTransportHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTransportHandler.class);
    
    @Autowired
    private McpProtocolHandler protocolHandler;

    @Autowired
    private ObjectMapper objectMapper;
    
    // Mantener sesiones activas
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        activeSessions.put(session.getId(), session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            if (message instanceof TextMessage textMessage) {
                String payload = textMessage.getPayload();
                logger.debug("Received message from {}: {}", session.getId(), payload);
                
                // Parsear el mensaje JSON-RPC
                McpRequest request = objectMapper.readValue(payload, McpRequest.class);
                
                // Procesar el request
                McpResponse response = protocolHandler.processRequest(request);
                
                // Enviar respuesta
                String responseJson = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(responseJson));
                
                logger.debug("Sent response to {}: {}", session.getId(), responseJson);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message from " + session.getId(), e);
            
            // Enviar error de vuelta al cliente
            McpError error = McpError.internalError("Error processing message: " + e.getMessage());
            McpResponse errorResponse = McpResponse.error("unknown", error);
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(errorJson));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session " + session.getId(), exception);
        activeSessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket connection closed: {} ({})", session.getId(), closeStatus);
        activeSessions.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Envía una notificación a todas las sesiones activas.
     */
    public void broadcastNotification(Object notification) {
        try {
            String message = objectMapper.writeValueAsString(notification);
            activeSessions.values().parallelStream().forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    logger.warn("Failed to send notification to session {}", session.getId(), e);
                }
            });
        } catch (Exception e) {
            logger.error("Error broadcasting notification", e);
        }
    }
}
