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
    @DisplayName("Debe tener argumentos definidos")
    void testArgumentos() {
        var definition = prompt.getPromptDefinition();
        assertNotNull(definition.getArguments());
        assertEquals(1, definition.getArguments().size());
        
        var argumentNames = definition.getArguments().stream()
            .map(arg -> arg.getName())
            .toList();
            
        assertTrue(argumentNames.contains("generar_backup"));
    }

    @Test
    @DisplayName("Debe ejecutar correctamente")
    void testEjecucion() {
        var result = prompt.execute(Map.of());
        
        assertNotNull(result);
        assertNotNull(result.getDescription());
        assertNotNull(result.getMessages());
        assertEquals(2, result.getMessages().size());
    }
}
