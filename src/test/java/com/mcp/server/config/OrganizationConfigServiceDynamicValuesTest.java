package com.mcp.server.config;

import com.mcp.server.tools.azuredevops.DiscoverOrganizationTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests para la funcionalidad de valores dinámicos en OrganizationConfigService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationConfigService - Dynamic Values")
class OrganizationConfigServiceDynamicValuesTest {

    @Mock
    private OrganizationContextService contextService;
    
    @Mock
    private DiscoverOrganizationTool discoverTool;
    
    private OrganizationConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new OrganizationConfigService(contextService, discoverTool);
    }

    @Test
    @DisplayName("Should return dynamic values when @DYNAMIC_FROM_AZURE_DEVOPS is configured")
    void testDynamicValuesFromAzureDevOps() {
        // Configurar mock del contexto para devolver configuración con @DYNAMIC_FROM_AZURE_DEVOPS
        Map<String, Object> fieldMappingConfig = new HashMap<>();
        Map<String, Object> fieldMappings = new HashMap<>();
        Map<String, Object> tipoHistoriaConfig = new HashMap<>();
        
        tipoHistoriaConfig.put("azureFieldName", "Custom.TipoDeHistoria");
        tipoHistoriaConfig.put("allowedValues", "@DYNAMIC_FROM_AZURE_DEVOPS");
        
        fieldMappings.put("tipoDeHistoria", tipoHistoriaConfig);
        fieldMappingConfig.put("fieldMappings", fieldMappings);
        
        when(contextService.getFieldMappingConfig()).thenReturn(fieldMappingConfig);
        
        // Mock de la configuración de proyectos
        Map<String, Object> discoveredConfig = new HashMap<>();
        List<Map<String, Object>> projects = new ArrayList<>();
        Map<String, Object> project = new HashMap<>();
        project.put("name", "Gerencia_Tecnologia");
        projects.add(project);
        discoveredConfig.put("projects", projects);
        
        when(contextService.getDiscoveredConfig()).thenReturn(discoveredConfig);
        
        // Mock del DiscoverOrganizationTool para devolver valores dinámicos
        List<String> expectedValues = Arrays.asList("Funcional", "No Funcional", "Técnica");
        when(discoverTool.getFieldAllowedValues(
            eq("Gerencia_Tecnologia"), 
            eq("Historia"), 
            eq("Custom.TipoDeHistoria"), 
            eq("picklistString")
        )).thenReturn(expectedValues);
        
        // Ejecutar
        List<String> result = configService.getAllowedValues("tipoDeHistoria");
        
        // Verificar
        assertEquals(expectedValues, result);
    }
    
    @Test
    @DisplayName("Should return static values when list is configured")
    void testStaticValuesFromConfig() {
        // Configurar mock del contexto para devolver valores estáticos
        Map<String, Object> fieldMappingConfig = new HashMap<>();
        Map<String, Object> fieldMappings = new HashMap<>();
        Map<String, Object> priorityConfig = new HashMap<>();
        
        List<String> staticValues = Arrays.asList("1", "2", "3", "4");
        priorityConfig.put("allowedValues", staticValues);
        
        fieldMappings.put("priority", priorityConfig);
        fieldMappingConfig.put("fieldMappings", fieldMappings);
        
        when(contextService.getFieldMappingConfig()).thenReturn(fieldMappingConfig);
        
        // Ejecutar
        List<String> result = configService.getAllowedValues("priority");
        
        // Verificar
        assertEquals(staticValues, result);
    }
    
    @Test
    @DisplayName("Should fallback to hardcoded values for basic fields")
    void testFallbackToHardcodedValues() {
        // Mock contexto vacío
        when(contextService.getFieldMappingConfig()).thenReturn(new HashMap<>());
        
        // Ejecutar
        List<String> stateValues = configService.getAllowedValues("state");
        List<String> priorityValues = configService.getAllowedValues("priority");
        
        // Verificar
        assertTrue(stateValues.contains("New"));
        assertTrue(stateValues.contains("Active"));
        assertTrue(priorityValues.contains("1"));
        assertTrue(priorityValues.contains("4"));
    }
    
    @Test
    @DisplayName("Should return empty list for unknown fields")
    void testUnknownFieldReturnsEmptyList() {
        // Mock contexto vacío
        when(contextService.getFieldMappingConfig()).thenReturn(new HashMap<>());
        
        // Ejecutar
        List<String> result = configService.getAllowedValues("unknownField");
        
        // Verificar
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("Should handle errors gracefully when Azure DevOps call fails")
    void testErrorHandlingForDynamicValues() {
        // Configurar mock del contexto
        Map<String, Object> fieldMappingConfig = new HashMap<>();
        Map<String, Object> fieldMappings = new HashMap<>();
        Map<String, Object> fieldConfig = new HashMap<>();
        
        fieldConfig.put("azureFieldName", "Custom.TipoDeHistoria");
        fieldConfig.put("allowedValues", "@DYNAMIC_FROM_AZURE_DEVOPS");
        
        fieldMappings.put("tipoDeHistoria", fieldConfig);
        fieldMappingConfig.put("fieldMappings", fieldMappings);
        
        when(contextService.getFieldMappingConfig()).thenReturn(fieldMappingConfig);
        when(contextService.getDiscoveredConfig()).thenReturn(new HashMap<>());
        
        // Mock que el DiscoverOrganizationTool lance excepción
        when(discoverTool.getFieldAllowedValues(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("Azure DevOps connection failed"));
        
        // Ejecutar
        List<String> result = configService.getAllowedValues("tipoDeHistoria");
        
        // Verificar que devuelve lista vacía en caso de error
        assertTrue(result.isEmpty());
    }
}
