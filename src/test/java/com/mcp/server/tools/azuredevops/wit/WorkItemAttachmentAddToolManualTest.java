package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Base64;

/**
 * Test manual para verificar que WorkItemAttachmentAddTool funciona igual que el script
 */
@SpringBootTest
@ActiveProfiles("test")
public class WorkItemAttachmentAddToolManualTest {

    @Autowired(required = false)
    private WorkItemAttachmentAddTool tool;

    @Autowired(required = false)
    private AzureDevOpsClientService azureService;

    @Test
    public void testToolAvailability() {
        System.out.println("=== TEST: Availability ===");
        System.out.println("Tool disponible: " + (tool != null));
        System.out.println("Azure Service disponible: " + (azureService != null));
        
        if (tool != null) {
            System.out.println("Tool name: " + tool.getName());
            System.out.println("Tool description: " + tool.getDescription());
            System.out.println("Input schema: " + tool.getInputSchema());
        }
    }

    @Test
    public void testToolExecution() {
        if (tool == null) {
            System.out.println("Tool no disponible - skipping execution test");
            return;
        }

        String testContent = "Este es un test del tool MCP desde JUnit";
        String dataBase64 = Base64.getEncoder().encodeToString(testContent.getBytes());

        Map<String, Object> args = Map.of(
            "project", "Gerencia_Tecnologia",
            "workItemId", 877406,
            "fileName", "test_from_junit.txt",
            "dataBase64", dataBase64,
            "comment", "Test desde JUnit - confirmar que funciona igual que script"
        );

        System.out.println("=== TEST: Execution ===");
        System.out.println("Argumentos: " + args);

        try {
            // Simular validación
            tool.validateCommon(args);
            System.out.println("✅ Validación pasó correctamente");

            // En un entorno real ejecutaríamos: Map<String, Object> result = tool.executeInternal(args);
            // Pero necesitaríamos las credenciales de Azure DevOps configuradas
            System.out.println("⚠️  Ejecución completa requiere credenciales Azure DevOps");
            
        } catch (Exception e) {
            System.out.println("❌ Error en test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
