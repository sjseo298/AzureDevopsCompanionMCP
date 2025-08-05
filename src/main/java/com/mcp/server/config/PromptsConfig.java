package com.mcp.server.config;

import com.mcp.server.prompts.example.EchoPrompt;
import com.mcp.server.prompts.base.McpPrompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de prompts MCP para la plantilla.
 * 
 * Registra los prompts de ejemplo disponibles en el servidor MCP como beans de Spring,
 * permitiendo que sean inyectados automáticamente en el protocolo handler.
 * 
 * Para agregar nuevos prompts, simplemente agregue métodos @Bean que retornen
 * implementaciones de McpPrompt.
 */
@Configuration
public class PromptsConfig {
    
    /**
     * Registra el prompt de ejemplo (Echo) para la plantilla MCP.
     */
    @Bean
    public McpPrompt echoPrompt() {
        return new EchoPrompt();
    }
}
