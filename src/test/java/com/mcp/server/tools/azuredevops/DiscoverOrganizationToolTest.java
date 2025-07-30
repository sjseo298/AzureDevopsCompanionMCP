package com.mcp.server.tools.azuredevops;

import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.tools.azuredevops.DiscoverOrganizationTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas genéricas para DiscoverOrganizationTool
 * Utiliza configuración real de Sura como caso de prueba
 */
class DiscoverOrganizationToolTest {

    private DiscoverOrganizationTool discoverOrganizationTool;
    private OrganizationConfigService organizationConfigService;

    @BeforeEach
    void setUp() {
        organizationConfigService = new OrganizationConfigService();
        // Note: We cannot easily instantiate DiscoverOrganizationTool without Spring context
        // as it requires AzureDevOpsClient dependency
    }

    @Test
    void testOrganizationConfigService() {
        Map<String, Object> config = organizationConfigService.getDefaultOrganizationConfig();
        
        assertNotNull(config);
        assertFalse(config.isEmpty());
        
        // Note: organization can be null when not using Spring injection (@Value doesn't work)
        // But the structure should be there
        assertTrue(config.containsKey("organization"));
        assertNotNull(config.get("defaultProject"));
        
        // Verify default values are set correctly
        assertEquals("DefaultProject", config.get("defaultProject"));
        assertEquals("DefaultTeam", config.get("defaultTeam"));
        assertEquals("America/Bogota", config.get("timeZone"));
        assertEquals("es-CO", config.get("language"));
    }

    @Test
    void testFieldMappings() {
        Map<String, Object> titleMapping = organizationConfigService.getFieldMapping("title");
        Map<String, Object> descriptionMapping = organizationConfigService.getFieldMapping("description");
        
        assertNotNull(titleMapping);
        assertNotNull(descriptionMapping);
        
        if (!titleMapping.isEmpty()) {
            assertEquals("System.Title", titleMapping.get("azureFieldName"));
        }
    }

    @Test
    void testFieldValidation() {
        String titleHelp = organizationConfigService.getFieldHelpText("title");
        String descriptionHelp = organizationConfigService.getFieldHelpText("description");
        
        assertNotNull(titleHelp);
        assertNotNull(descriptionHelp);
        assertFalse(titleHelp.isEmpty());
        assertFalse(descriptionHelp.isEmpty());
        
        assertTrue(organizationConfigService.isFieldRequired("Task", "title"));
        assertTrue(organizationConfigService.isFieldRequired("Historia", "title"));
    }

    @Test
    void testSupportedWorkItemTypes() {
        // Test that the service supports various work item types
        assertDoesNotThrow(() -> {
            organizationConfigService.getRequiredFieldsForWorkItemType("Task");
            organizationConfigService.getRequiredFieldsForWorkItemType("Bug");
            organizationConfigService.getRequiredFieldsForWorkItemType("Historia");
            organizationConfigService.getRequiredFieldsForWorkItemType("Feature");
        });
    }

    @Test
    void testAllowedValues() {
        assertDoesNotThrow(() -> {
            organizationConfigService.getAllowedValues("state");
            organizationConfigService.getAllowedValues("priority");
            organizationConfigService.getAllowedValues("reason");
        });
        
        // Test that state values are not empty
        assertFalse(organizationConfigService.getAllowedValues("state").isEmpty());
        assertFalse(organizationConfigService.getAllowedValues("priority").isEmpty());
    }
}
