package com.mcp.server.config;

import org.springframework.context.annotation.Configuration;
import java.nio.charset.StandardCharsets;

/**
 * Configuración para asegurar que la aplicación use UTF-8 en todas las operaciones.
 * 
 * Esta configuración es especialmente importante para el protocolo MCP que debe
 * manejar correctamente caracteres especiales y acentos en español.
 */
@Configuration
public class EncodingConfig {
    
    // Constructor que se ejecuta cuando Spring crea la clase
    public EncodingConfig() {
        setDefaultEncoding();
    }
    
    private void setDefaultEncoding() {
        // Establecer UTF-8 como encoding por defecto del sistema
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        System.setProperty("stdout.encoding", "UTF-8");
        System.setProperty("stderr.encoding", "UTF-8");
        
        // Asegurar que Java use UTF-8 para operaciones de I/O
        System.setProperty("java.nio.charset.Charset.defaultCharset", "UTF-8");
        
        // Log para verificar la configuración
        System.err.println("Encoding configurado: " + System.getProperty("file.encoding"));
        System.err.println("Default Charset: " + StandardCharsets.UTF_8.name());
    }
}
