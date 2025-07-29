package com.mcp.server.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración de Jackson para serialización/deserialización JSON.
 * 
 * <p>Esta configuración está optimizada para el protocolo MCP que requiere:
 * <ul>
 *   <li>Manejo flexible de propiedades desconocidas</li>
 *   <li>Serialización de objetos vacíos</li>
 *   <li>Formato JSON-RPC 2.0 estricto</li>
 *   <li>Soporte para tipos de fecha/hora Java 8+</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Configura el ObjectMapper principal para toda la aplicación.
     * 
     * <p>Características:
     * <ul>
     *   <li>Ignora propiedades desconocidas durante deserialización</li>
     *   <li>Permite serialización de beans vacíos</li>
     *   <li>Usa snake_case para nombres de propiedades JSON</li>
     *   <li>Soporte completo para Java Time API</li>
     *   <li>Formato ISO-8601 para fechas</li>
     * </ul>
     * 
     * @return ObjectMapper configurado para MCP
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // Configuración de deserialización
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                
                // Configuración de serialización
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.INDENT_OUTPUT, false)
                
                // Soporte para Java Time API
                .registerModule(new JavaTimeModule())
                
                // Estrategia de nombres (snake_case para MCP)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
