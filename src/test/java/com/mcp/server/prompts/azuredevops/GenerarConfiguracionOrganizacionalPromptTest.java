package com.mcp.server.prompts.azuredevops;

import com.mcp.server.prompts.azuredevops.GenerarConfiguracionOrganizacionalPrompt;
import com.mcp.server.protocol.types.PromptResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas para GenerarConfiguracionOrganizacionalPrompt
 */
class GenerarConfiguracionOrganizacionalPromptTest {

    private GenerarConfiguracionOrganizacionalPrompt prompt;

    @BeforeEach
    void setUp() {
        prompt = new GenerarConfiguracionOrganizacionalPrompt();
    }

    @Test
    @DisplayName("Debe tener el nombre correcto")
    void testNombre() {
        assertEquals("generar_configuracion_organizacional", prompt.getName());
    }

    @Test
    @DisplayName("Debe tener título descriptivo")
    void testTitulo() {
        assertEquals("Generar Configuración Organizacional Automática", prompt.getTitle());
    }

    @Test
    @DisplayName("Debe tener descripción detallada")
    void testDescripcion() {
        String descripcion = prompt.getDescription();
        assertTrue(descripcion.contains("descubrimiento"));
        assertTrue(descripcion.contains("automáticamente"));
        assertTrue(descripcion.contains("configuración organizacional"));
    }

    @Test
    @DisplayName("Debe tener argumentos opcionales definidos")
    void testArgumentos() {
        var definition = prompt.getPromptDefinition();
        assertNotNull(definition.getArguments());
        assertEquals(2, definition.getArguments().size());
        
        // Verificar nombres de argumentos
        var argumentNames = definition.getArguments().stream()
            .map(arg -> arg.getName())
            .toList();
            
        assertTrue(argumentNames.contains("generar_backup"));
        assertTrue(argumentNames.contains("work_item_referencia"));
    }

    @Test
    @DisplayName("Debe ejecutar con argumentos por defecto")
    void testEjecucionSinArgumentos() {
        var result = prompt.execute(Map.of());
        
        assertNotNull(result);
        assertNotNull(result.getDescription());
        assertNotNull(result.getMessages());
        assertEquals(2, result.getMessages().size());
        
        // Verificar que tiene mensaje de sistema y usuario
        assertEquals("system", result.getMessages().get(0).getRole().toString().toLowerCase());
        assertEquals("user", result.getMessages().get(1).getRole().toString().toLowerCase());
    }

    @Test
    @DisplayName("Debe manejar argumentos personalizados")
    void testEjecucionConArgumentos() {
        Map<String, Object> args = Map.of(
            "generar_backup", false
        );
        
        var result = prompt.execute(args);
        
        assertNotNull(result);
        assertTrue(result.getDescription().contains("automática"));
        
        // Verificar que el contexto incluye el argumento de backup
        String systemMessage = ((PromptResult.TextContent) result.getMessages().get(0).getContent()).getText();
        assertTrue(systemMessage.contains("SIN BACKUP"));
    }

    @Test
    @DisplayName("Debe generar mensajes detallados para el proceso")
    void testContenidoMensajes() {
        var result = prompt.execute(Map.of());
        
        String systemMessage = ((PromptResult.TextContent) result.getMessages().get(0).getContent()).getText();
        String userMessage = ((PromptResult.TextContent) result.getMessages().get(1).getContent()).getText();
        
        // Verificar contenido del mensaje de sistema
        assertTrue(systemMessage.contains("FASE 1: DETECCIÓN"));
        assertTrue(systemMessage.contains("FASE 2: DESCUBRIMIENTO"));
        assertTrue(systemMessage.contains("FASE 3: GENERACIÓN"));
        assertTrue(systemMessage.contains("FASE 4: VALIDACIÓN"));
        assertTrue(systemMessage.contains("discovered-organization.yml"));
        assertTrue(systemMessage.contains("organization-config.yml"));
        
        // Verificar contenido del mensaje de usuario
        assertTrue(userMessage.contains("DESCUBRIMIENTO ORGANIZACIONAL"));
        assertTrue(userMessage.contains("azuredevops_discover_organization"));
        assertTrue(userMessage.contains("GENERACIÓN AUTOMÁTICA"));
        assertTrue(userMessage.contains("VALIDACIÓN Y OPTIMIZACIÓN"));
        assertTrue(userMessage.contains("field-mappings.yml"));
        assertTrue(userMessage.contains("business-rules.yml"));
    }
}
