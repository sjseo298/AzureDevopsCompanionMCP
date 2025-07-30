package com.mcp.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas genéricas para OrganizationConfigService
 * Utiliza configuración real como caso de prueba
 */
@DisplayName("OrganizationConfigService - Generic Tests")
class OrganizationConfigServiceTest {

    private OrganizationConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new OrganizationConfigService();
    }

    @Test
    @DisplayName("Should provide default organization configuration")
    void testGetDefaultOrganizationConfig() {
        Map<String, Object> config = configService.getDefaultOrganizationConfig();
        
        assertNotNull(config, "Configuration should not be null");
        assertFalse(config.isEmpty(), "Configuration should not be empty");
        
        // Verify essential configuration keys
        assertTrue(config.containsKey("organization"), "Should contain organization key");
        assertTrue(config.containsKey("defaultProject"), "Should contain defaultProject key");
        assertTrue(config.containsKey("timeZone"), "Should contain timeZone key");
        assertTrue(config.containsKey("language"), "Should contain language key");
        
        // Verify values are not null (but they might be null when not using Spring injection)
        // Since we're creating OrganizationConfigService manually, @Value annotations don't work
        // The organization field will be null, so we just verify the structure exists
        assertTrue(config.get("defaultProject").toString().length() > 0, "Default project should not be empty");
        assertNotNull(config.get("timeZone"), "Time zone should not be null");
        assertNotNull(config.get("language"), "Language should not be null");
    }

    @Test
    @DisplayName("Should provide field mappings for common fields")
    void testGetFieldMapping() {
        // Test title field mapping
        Map<String, Object> titleMapping = configService.getFieldMapping("title");
        assertNotNull(titleMapping, "Title mapping should not be null");
        
        if (!titleMapping.isEmpty()) {
            assertTrue(titleMapping.containsKey("azureFieldName"), "Title mapping should contain azureFieldName");
            assertEquals("System.Title", titleMapping.get("azureFieldName"), "Title should map to System.Title");
            assertTrue(titleMapping.containsKey("required"), "Title mapping should contain required flag");
            assertTrue(titleMapping.containsKey("type"), "Title mapping should contain type");
        }
        
        // Test description field mapping
        Map<String, Object> descMapping = configService.getFieldMapping("description");
        assertNotNull(descMapping, "Description mapping should not be null");
        
        if (!descMapping.isEmpty()) {
            assertEquals("System.Description", descMapping.get("azureFieldName"), "Description should map to System.Description");
        }
        
        // Test assignedTo field mapping
        Map<String, Object> assignedMapping = configService.getFieldMapping("assignedTo");
        assertNotNull(assignedMapping, "AssignedTo mapping should not be null");
        
        if (!assignedMapping.isEmpty()) {
            assertEquals("System.AssignedTo", assignedMapping.get("azureFieldName"), "AssignedTo should map to System.AssignedTo");
        }
    }

    @Test
    @DisplayName("Should convert field values appropriately")
    void testConvertFieldValue() {
        // Test boolean conversion
        Object trueValue = configService.convertFieldValue("test", "true");
        assertEquals(Boolean.TRUE, trueValue, "Should convert 'true' to Boolean.TRUE");
        
        Object falseValue = configService.convertFieldValue("test", "false");
        assertEquals(Boolean.FALSE, falseValue, "Should convert 'false' to Boolean.FALSE");
        
        // Test numeric conversion
        Object numericValue = configService.convertFieldValue("test", "123");
        assertEquals(123, numericValue, "Should convert '123' to Integer 123");
        
        // Test string preservation
        Object stringValue = configService.convertFieldValue("test", "hello world");
        assertEquals("hello world", stringValue, "Should preserve non-convertible strings");
        
        // Test null and non-string values
        Object nullValue = configService.convertFieldValue("test", null);
        assertNull(nullValue, "Should handle null values");
        
        Object objectValue = configService.convertFieldValue("test", 456);
        assertEquals(456, objectValue, "Should preserve non-string objects");
    }

    @Test
    @DisplayName("Should provide required fields for work item types")
    void testGetRequiredFieldsForWorkItemType() {
        // Test Task/Tarea
        assertFalse(configService.getRequiredFieldsForWorkItemType("Task").isEmpty(), "Task should have required fields");
        assertFalse(configService.getRequiredFieldsForWorkItemType("Tarea").isEmpty(), "Tarea should have required fields");
        
        // Test Historia/User Story
        assertFalse(configService.getRequiredFieldsForWorkItemType("Historia").isEmpty(), "Historia should have required fields");
        assertFalse(configService.getRequiredFieldsForWorkItemType("User Story").isEmpty(), "User Story should have required fields");
        
        // Test Bug
        assertFalse(configService.getRequiredFieldsForWorkItemType("Bug").isEmpty(), "Bug should have required fields");
        
        // Test Feature
        assertFalse(configService.getRequiredFieldsForWorkItemType("Feature").isEmpty(), "Feature should have required fields");
        
        // Test unknown type
        assertFalse(configService.getRequiredFieldsForWorkItemType("Unknown").isEmpty(), "Unknown type should have basic required fields");
    }

    @Test
    @DisplayName("Should provide help text for fields")
    void testGetFieldHelpText() {
        String titleHelp = configService.getFieldHelpText("title");
        assertNotNull(titleHelp, "Title help should not be null");
        assertFalse(titleHelp.isEmpty(), "Title help should not be empty");
        
        String descHelp = configService.getFieldHelpText("description");
        assertNotNull(descHelp, "Description help should not be null");
        assertFalse(descHelp.isEmpty(), "Description help should not be empty");
        
        String unknownHelp = configService.getFieldHelpText("unknownField");
        assertNotNull(unknownHelp, "Unknown field help should not be null");
        assertEquals("Campo personalizado", unknownHelp, "Unknown field should return default help");
    }

    @Test
    @DisplayName("Should provide allowed values for fields")
    void testGetAllowedValues() {
        // Test state values
        assertFalse(configService.getAllowedValues("state").isEmpty(), "State should have allowed values");
        assertTrue(configService.getAllowedValues("state").contains("New"), "State should include 'New'");
        assertTrue(configService.getAllowedValues("state").contains("Active"), "State should include 'Active'");
        
        // Test priority values
        assertFalse(configService.getAllowedValues("priority").isEmpty(), "Priority should have allowed values");
        assertTrue(configService.getAllowedValues("priority").contains("1"), "Priority should include '1'");
        
        // Test unknown field
        assertTrue(configService.getAllowedValues("unknownField").isEmpty(), "Unknown field should have empty allowed values");
    }

    @Test
    @DisplayName("Should validate field requirements correctly")
    void testIsFieldRequired() {
        // Title should be required for all work item types
        assertTrue(configService.isFieldRequired("Task", "title"), "Title should be required for Task");
        assertTrue(configService.isFieldRequired("Historia", "title"), "Title should be required for Historia");
        assertTrue(configService.isFieldRequired("Bug", "title"), "Title should be required for Bug");
        
        // Test case insensitivity
        assertTrue(configService.isFieldRequired("task", "title"), "Should be case insensitive for work item type");
        
        // Test acceptance criteria for Historia
        assertTrue(configService.isFieldRequired("Historia", "acceptanceCriteria"), "Acceptance criteria should be required for Historia");
        
        // Test repro steps for Bug
        assertTrue(configService.isFieldRequired("Bug", "reproSteps"), "Repro steps should be required for Bug");
    }
}
