package com.mcp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

/**
 * Aplicación principal del servidor MCP.
 * 
 * <p>Este servidor implementa el Model Context Protocol (MCP) usando Spring Boot.
 * Soporta comunicación via STDIO para integración con clientes como VS Code.
 * 
 * <p>Modos de operación:
 * <ul>
 *   <li>STDIO: Para comunicación directa con clientes MCP</li>
 *   <li>WebSocket: Para aplicaciones web (futuro)</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
public class McpServerApplication {

    private static final String STDIO_ARG = "--mcp.stdio=true";
    private static final String STDIO_PROPERTY = "mcp.stdio";
    private static final String WEB_APP_TYPE_PROPERTY = "spring.main.web-application-type";

    /**
     * Punto de entrada principal de la aplicación.
     * 
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // Configurar UTF-8 como encoding por defecto
        configureDefaultEncoding();
        
        var application = new SpringApplication(McpServerApplication.class);
        
        // Configurar modo STDIO si está habilitado
        if (isStdioModeEnabled(args)) {
            configureStdioMode();
        }
        
        application.run(args);
    }

    /**
     * Verifica si el modo STDIO está habilitado en los argumentos.
     * 
     * @param args argumentos de línea de comandos
     * @return true si el modo STDIO está habilitado
     */
    private static boolean isStdioModeEnabled(String[] args) {
        return Arrays.asList(args).contains(STDIO_ARG);
    }

    /**
     * Configura las propiedades del sistema para el modo STDIO.
     */
    private static void configureStdioMode() {
        System.setProperty(STDIO_PROPERTY, "true");
        System.setProperty(WEB_APP_TYPE_PROPERTY, "none");
    }
    
    /**
     * Configura UTF-8 como encoding por defecto para toda la aplicación.
     */
    private static void configureDefaultEncoding() {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("console.encoding", "UTF-8");
        System.setProperty("stdout.encoding", "UTF-8");
        System.setProperty("stderr.encoding", "UTF-8");
        System.setProperty("java.nio.charset.Charset.defaultCharset", "UTF-8");
        
        // Verificar que la configuración se aplicó correctamente
        System.err.println("Default encoding configured: " + System.getProperty("file.encoding"));
    }
}
