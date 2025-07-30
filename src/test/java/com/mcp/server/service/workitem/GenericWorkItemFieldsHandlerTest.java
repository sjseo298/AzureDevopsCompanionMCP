package com.mcp.server.service.workitem;

import com.mcp.server.config.OrganizationConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas genéricas para GenericWorkItemFieldsHandler
 * Utiliza configuración real de YOUR_ORGANIZATION como caso de prueba
 */
class GenericWorkItemFieldsHandlerTest {

    private GenericWorkItemFieldsHandler fieldsHandler;
    private OrganizationConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new OrganizationConfigService();
        fieldsHandler = new GenericWorkItemFieldsHandler(configService);
    }

    @Test
    @DisplayName("Debe procesar campos de work item genéricamente")
    void testProcessWorkItemFields() {
        Map<String, Object> inputFields = new HashMap<>();
        inputFields.put("title", "Test Task");
        inputFields.put("description", "Test Description");
        inputFields.put("priority", 1);

        Map<String, Object> processedFields = fieldsHandler.processWorkItemFields("Task", inputFields);

        assertNotNull(processedFields);
        // El método convierte los nombres de campo a los nombres de Azure DevOps
        assertTrue(processedFields.containsKey("System.Title"));
        assertEquals("Test Task", processedFields.get("System.Title"));
    }

    @Test
    @DisplayName("Debe validar campos requeridos")
    void testValidateRequiredFields() {
        Map<String, Object> inputFields = new HashMap<>();
        inputFields.put("title", "Test Task");
        inputFields.put("description", "Test Description");

        // Primero procesar los campos para convertir a nombres Azure
        Map<String, Object> processedFields = fieldsHandler.processWorkItemFields("Task", inputFields);
        
        // Luego validar los campos procesados
        GenericWorkItemFieldsHandler.ValidationResult result = 
            fieldsHandler.validateRequiredFields("Task", processedFields);

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Debe detectar campos faltantes")
    void testValidateRequiredFieldsMissingFields() {
        Map<String, Object> incompleteFields = new HashMap<>();
        incompleteFields.put("description", "Test Description");
        // Falta title que es requerido

        GenericWorkItemFieldsHandler.ValidationResult result = 
            fieldsHandler.validateRequiredFields("Task", incompleteFields);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Debe obtener campos disponibles")
    void testGetAvailableFields() {
        List<String> availableFields = fieldsHandler.getAvailableFields("Task");

        assertNotNull(availableFields);
        assertFalse(availableFields.isEmpty());
        assertTrue(availableFields.contains("title"));
    }

    @Test
    @DisplayName("Debe manejar tipos de work item específicos de YOUR_ORGANIZATION")
    void testYOUR_ORGANIZATIONSpecificWorkItemTypes() {
        // Test Historia
        Map<String, Object> historiaFields = new HashMap<>();
        historiaFields.put("title", "Test Historia");
        historiaFields.put("description", "Test Description");
        
        Map<String, Object> processedHistoria = fieldsHandler.processWorkItemFields("Historia", historiaFields);
        assertNotNull(processedHistoria);

        // Test Historia Técnica
        Map<String, Object> historiaTecnicaFields = new HashMap<>();
        historiaTecnicaFields.put("title", "Test Historia Técnica");
        historiaTecnicaFields.put("description", "Test Description");
        
        Map<String, Object> processedHistoriaTecnica = fieldsHandler.processWorkItemFields("Historia Técnica", historiaTecnicaFields);
        assertNotNull(processedHistoriaTecnica);
    }
}
