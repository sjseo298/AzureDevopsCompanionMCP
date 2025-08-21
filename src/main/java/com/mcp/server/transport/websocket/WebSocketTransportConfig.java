package com.mcp.server.transport.websocket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración para el transporte WebSocket del protocolo MCP.
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "mcp.websocket", havingValue = "true")
public class WebSocketTransportConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketTransportHandler(), "/mcp/ws")
                .setAllowedOrigins("*"); // En producción, configurar origins específicos
    }

    @Bean
    public WebSocketTransportHandler webSocketTransportHandler() {
        return new WebSocketTransportHandler();
    }
}
