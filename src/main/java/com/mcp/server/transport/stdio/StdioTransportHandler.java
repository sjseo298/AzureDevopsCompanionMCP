package com.mcp.server.transport.stdio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.server.protocol.messages.*;
import com.mcp.server.protocol.types.McpError;
import com.mcp.server.protocol.handlers.McpProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Manejador del transporte STDIO para el protocolo MCP.
 * 
 * Este componente maneja la comunicación con el cliente MCP a través de
 * entrada y salida estándar, procesando mensajes JSON-RPC línea por línea.
 */
@Component
public class StdioTransportHandler implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StdioTransportHandler.class);

    @Autowired
    private McpProtocolHandler protocolHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        // Only run if stdio mode is enabled
        String stdioProperty = System.getProperty("mcp.stdio");
        if (!"true".equals(stdioProperty)) {
            System.err.println("STDIO mode not enabled, exiting handler");
            return;
        }
        
        System.err.println("Starting MCP stdio handler...");
        System.err.println("ObjectMapper configuration: " + objectMapper.getClass().getName());
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out), true)) {
            
            System.err.println("Input/Output streams established");
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    System.err.println("Received: " + line);
                    processMessage(line, writer);
                } catch (Exception e) {
                    System.err.println("Error processing message: " + line);
                    System.err.println("Exception details: " + e.getClass().getName() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    sendError(writer, "unknown", "Internal error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal error in stdio handler: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void processMessage(String message, PrintWriter writer) throws Exception {
        if (message.trim().isEmpty()) {
            return;
        }

        try {
            // Check if this is an HTTP request (health check)
            if (isHttpRequest(message)) {
                handleHttpRequest(message, writer);
                return;
            }
            
            // Check if this looks like JSON
            if (!isJsonMessage(message)) {
                System.err.println("Received non-JSON message, ignoring: " + 
                    (message.length() > 50 ? message.substring(0, 50) + "..." : message));
                return;
            }
            
            System.err.println("Processing JSON message: " + message);
            
            // First check if this is a notification or a request by looking for an ID
            boolean hasId = message.contains("\"id\":");
            
            if (hasId) {
                // Process as a request
                try {
                    McpRequest request = objectMapper.readValue(message, McpRequest.class);
                    System.err.println("Request parsed, id: " + request.getId() + ", method: " + request.getMethod());
                    
                    McpResponse response = protocolHandler.processRequest(request);
                    
                    String responseJson = objectMapper.writeValueAsString(response);
                    System.err.println("Sending response: " + responseJson);
                    writer.println(responseJson);
                    writer.flush();
                    System.err.println("Response sent");
                } catch (Exception e) {
                    // Handle deserialization error for requests
                    System.err.println("Error processing request: " + e.getMessage());
                    e.printStackTrace(System.err);
                    throw e;
                }
            } else {
                // Process as a notification - no response needed
                try {
                    McpNotification notification = objectMapper.readValue(message, McpNotification.class);
                    System.err.println("Notification parsed, method: " + notification.getMethod());
                    
                    // Process notification (no response needed)
                    processNotification(notification);
                } catch (Exception e) {
                    // Just log error for notifications, don't throw
                    System.err.println("Error processing notification: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in processMessage: " + e.getMessage());
            e.printStackTrace(System.err);
            throw e;
        }
    }
    
    private boolean isHttpRequest(String message) {
        return message.startsWith("GET ") || message.startsWith("POST ") || 
               message.startsWith("PUT ") || message.startsWith("DELETE ") ||
               message.startsWith("HEAD ") || message.startsWith("OPTIONS ") ||
               message.contains("HTTP/");
    }
    
    private boolean isJsonMessage(String message) {
        String trimmed = message.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    private void handleHttpRequest(String request, PrintWriter writer) {
        try {
            System.err.println("Handling HTTP request: " + 
                (request.length() > 100 ? request.substring(0, 100) + "..." : request));
            
            if (request.contains("GET /health") || request.contains("GET /mcp/health")) {
                // Simple health check response
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: application/json");
                writer.println("Content-Length: 25");
                writer.println();
                writer.println("{\"status\":\"healthy\"}");
                writer.flush();
                System.err.println("Health check response sent");
            } else {
                // For other HTTP requests, send a simple response
                String response = "HTTP/1.1 400 Bad Request\r\n" +
                                 "Content-Type: text/plain\r\n" +
                                 "Content-Length: 47\r\n\r\n" +
                                 "This is an MCP server, not an HTTP server.\r\n";
                writer.print(response);
                writer.flush();
                System.err.println("HTTP 400 response sent for non-health request");
            }
        } catch (Exception e) {
            System.err.println("Error handling HTTP request: " + e.getMessage());
        }
    }
    
    private void processNotification(McpNotification notification) {
        System.err.println("Processing notification: " + notification.getMethod());
        // Handle different notification types
        switch (notification.getMethod()) {
            case "notifications/initialized":
                System.err.println("Client initialized notification received");
                break;
            default:
                System.err.println("Unknown notification method: " + notification.getMethod());
                break;
        }
    }

    private void sendError(PrintWriter writer, Object id, String message) {
        try {
            McpError error = McpError.internalError(message);
            McpResponse errorResponse = McpResponse.error(id, error);
            String errorJson = objectMapper.writeValueAsString(errorResponse);
            System.err.println("Sending error response: " + errorJson);
            writer.println(errorJson);
            writer.flush();
        } catch (Exception e) {
            logger.error("Failed to send error response", e);
            e.printStackTrace(System.err);
        }
    }
}
