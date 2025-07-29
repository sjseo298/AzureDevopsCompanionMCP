package com.mcp.server;

import com.mcp.server.protocol.handlers.McpProtocolHandler;
import com.mcp.server.tools.base.McpTool;
import com.mcp.server.tools.azuredevops.DeleteWorkItemTool;
import com.mcp.server.tools.uuid.UuidGeneratorTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para la aplicación MCP Server.
 * 
 * <p>Estos tests verifican que:
 * <ul>
 *   <li>El contexto de Spring se carga correctamente</li>
 *   <li>Todos los componentes principales están configurados</li>
 *   <li>Las herramientas están registradas automáticamente</li>
 *   <li>El protocolo MCP está funcionando</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class McpServerApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private McpProtocolHandler protocolHandler;

    /**
     * Verifica que el contexto de Spring se carga correctamente.
     */
    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(protocolHandler).isNotNull();
    }
    
    /**
     * Verifica que el manejador del protocolo está configurado correctamente.
     */
    @Test
    void protocolHandlerIsConfigured() {
        assertThat(protocolHandler).isNotNull();
    }
    
    /**
     * Verifica que las herramientas están registradas automáticamente.
     */
    @Test
    void toolsAreAutoRegistered() {
        Collection<McpTool> tools = applicationContext.getBeansOfType(McpTool.class).values();
        
        assertThat(tools)
            .isNotEmpty()
            .hasAtLeastOneElementOfType(UuidGeneratorTool.class);
    }
    
    /**
     * Verifica que la herramienta UUID está configurada correctamente.
     */
    @Test
    void uuidToolIsConfigured() {
        UuidGeneratorTool uuidTool = applicationContext.getBean(UuidGeneratorTool.class);
        
        assertThat(uuidTool).isNotNull();
        assertThat(uuidTool.getName()).isEqualTo("generate_uuid");
        assertThat(uuidTool.getDescription()).isNotBlank();
        assertThat(uuidTool.getToolDefinition()).isNotNull();
    }
    
    /**
     * Verifica que la herramienta UUID ejecuta correctamente.
     */
    @Test
    void uuidToolExecutes() {
        UuidGeneratorTool uuidTool = applicationContext.getBean(UuidGeneratorTool.class);
        
        var result = uuidTool.execute(null);
        
        assertThat(result)
            .isNotNull()
            .containsKey("content")
            .containsKey("isError");
        
        assertThat(result.get("isError")).isEqualTo(false);
    }
}
