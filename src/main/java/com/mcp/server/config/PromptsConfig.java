package com.mcp.server.config;

import com.mcp.server.prompts.azuredevops.BuscarWorkItemPrompt;
import com.mcp.server.prompts.azuredevops.ConsultaProyectosPertenenciaPrompt;
import com.mcp.server.prompts.azuredevops.GenerarConfiguracionOrganizacionalPrompt;
import com.mcp.server.prompts.base.McpPrompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de prompts MCP.
 * 
 * Registra todos los prompts disponibles en el servidor MCP como beans de Spring,
 * permitiendo que sean inyectados automáticamente en el protocolo handler.
 */
@Configuration
public class PromptsConfig {
    
    /**
     * Registra el prompt para consultar proyectos de pertenencia.
     */
    @Bean
    public McpPrompt consultaProyectosPertenenciaPrompt() {
        return new ConsultaProyectosPertenenciaPrompt();
    }
    
    /**
     * Registra el prompt para buscar work items.
     */
    @Bean
    public McpPrompt buscarWorkItemPrompt() {
        return new BuscarWorkItemPrompt();
    }
    
    /**
     * Registra el prompt para generar configuración organizacional automáticamente.
     */
    @Bean
    public McpPrompt generarConfiguracionOrganizacionalPrompt() {
        return new GenerarConfiguracionOrganizacionalPrompt();
    }
}
