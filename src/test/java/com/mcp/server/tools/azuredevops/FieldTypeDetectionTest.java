package com.mcp.server.tools.azuredevops;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas específicas para la detección automática de tipos de campo
 */
class FieldTypeDetectionTest {

    @Test
    @DisplayName("Test field type inference for picklist fields")
    void testPicklistFieldDetection() throws Exception {
        // Crear una instancia mínima para poder llamar los métodos privados
        DiscoverOrganizationTool tool = createTestInstance();
        
        // Acceder al método privado determineFieldType usando reflexión
        Method determineFieldTypeMethod = DiscoverOrganizationTool.class.getDeclaredMethod("determineFieldType", Map.class, String.class);
        determineFieldTypeMethod.setAccessible(true);
        
        // Test 1: Campo con picklistId debería ser detectado como picklistString
        Map<String, Object> fieldWithPicklist = new HashMap<>();
        fieldWithPicklist.put("referenceName", "Custom.TipoHistoriaTecnica");
        fieldWithPicklist.put("type", "string");
        fieldWithPicklist.put("picklistId", "12345");
        
        String inferredType = (String) determineFieldTypeMethod.invoke(tool, fieldWithPicklist, "");
        assertEquals("picklistString", inferredType);
        
        // Test 2: Campo con allowedValues en JSON debería ser detectado como picklistString
        Map<String, Object> fieldWithAllowedValues = new HashMap<>();
        fieldWithAllowedValues.put("referenceName", "Custom.TipoHistoria");
        fieldWithAllowedValues.put("type", "string");
        
        String fieldDataWithAllowedValues = "\"allowedValues\": [\"Valor1\", \"Valor2\", \"Valor3\"]";
        String inferredType2 = (String) determineFieldTypeMethod.invoke(tool, fieldWithAllowedValues, fieldDataWithAllowedValues);
        assertEquals("picklistString", inferredType2);
        
        // Test 3: Campo con nombre que sugiere lista debería ser detectado como picklistString
        Map<String, Object> fieldWithSuggestiveName = new HashMap<>();
        fieldWithSuggestiveName.put("referenceName", "Custom.TipoDeHistoria");
        fieldWithSuggestiveName.put("type", "string");
        
        String inferredType3 = (String) determineFieldTypeMethod.invoke(tool, fieldWithSuggestiveName, "");
        assertEquals("picklistString", inferredType3);
        
        // Test 4: Campo normal string debería mantenerse como string
        Map<String, Object> normalStringField = new HashMap<>();
        normalStringField.put("referenceName", "System.Description");
        normalStringField.put("type", "string");
        
        String inferredType4 = (String) determineFieldTypeMethod.invoke(tool, normalStringField, "");
        assertEquals("string", inferredType4);
    }
    
    @Test
    @DisplayName("Test likely picklist field name detection")
    void testLikelyPicklistFieldNameDetection() throws Exception {
        DiscoverOrganizationTool tool = createTestInstance();
        
        // Acceder al método privado isLikelyPicklistField usando reflexión
        Method isLikelyPicklistFieldMethod = DiscoverOrganizationTool.class.getDeclaredMethod("isLikelyPicklistField", String.class);
        isLikelyPicklistFieldMethod.setAccessible(true);
        
        // Campos que DEBERÍAN ser detectados como picklist
        assertTrue((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.TipoHistoriaTecnica"));
        assertTrue((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.TipoDeHistoria"));
        assertTrue((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.Categoria"));
        assertTrue((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.Estado"));
        assertTrue((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.Prioridad"));
        assertTrue((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.Origen"));
        
        // Campos que NO deberían ser detectados como picklist
        assertFalse((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "System.Title"));
        assertFalse((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "System.Description"));
        assertFalse((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.Comentarios"));
        assertFalse((Boolean) isLikelyPicklistFieldMethod.invoke(tool, "Custom.NotasAdicionales"));
    }
    
    /**
     * Crea una instancia mínima de DiscoverOrganizationTool para testing
     * Nota: Algunos métodos pueden fallar si requieren dependencias reales
     */
    private DiscoverOrganizationTool createTestInstance() {
        // Para testing, creamos una instancia con dependencias nulas
        // Esto funcionará para métodos que no requieren estas dependencias
        return new DiscoverOrganizationTool(null, null, null);
    }
}
