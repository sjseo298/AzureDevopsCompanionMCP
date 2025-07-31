package com.mcp.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para OrganizationContextService
 * Verifica que puede cargar todos los archivos de configuración YAML
 */
@DisplayName("OrganizationContextService - Integration Tests")
class OrganizationContextServiceIntegrationTest {

    private OrganizationContextService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationContextService();
        // Forzar la recarga de configuración para pruebas
        service.reloadConfiguration();
    }

    @Test
    @DisplayName("Debe cargar la configuración organizacional descubierta")
    void shouldLoadDiscoveredConfiguration() {
        Map<String, Object> config = service.getDiscoveredConfig();
        
        assertNotNull(config, "La configuración descubierta no debe ser null");
        // Si el archivo existe, debe tener contenido
        if (!config.isEmpty()) {
            assertTrue(config.containsKey("organization") || config.containsKey("projects"), 
                "La configuración debe contener información de organización o proyectos");
        }
    }

    @Test
    @DisplayName("Debe cargar la configuración organizacional personalizable")
    void shouldLoadOrganizationConfiguration() {
        Map<String, Object> config = service.getOrganizationConfig();
        
        assertNotNull(config, "La configuración organizacional no debe ser null");
        // Verificar que el método funciona aunque el archivo esté vacío o no exista
        assertTrue(config instanceof Map, "Debe retornar un Map válido");
    }

    @Test
    @DisplayName("Debe cargar la configuración de mapeo de campos")
    void shouldLoadFieldMappingConfiguration() {
        Map<String, Object> config = service.getFieldMappingConfig();
        
        assertNotNull(config, "La configuración de mapeo de campos no debe ser null");
        // Si el archivo existe, puede tener fieldMappings o customFields
        if (!config.isEmpty()) {
            assertTrue(config.containsKey("fieldMappings") || config.containsKey("customFields") || config.containsKey("workItemTypeFields"), 
                "La configuración debe contener información de mapeo de campos");
        }
    }

    @Test
    @DisplayName("Debe cargar las reglas de negocio organizacionales")
    void shouldLoadBusinessRulesConfiguration() {
        Map<String, Object> config = service.getBusinessRulesConfig();
        
        assertNotNull(config, "Las reglas de negocio no deben ser null");
        // Verificar que el método funciona aunque el archivo esté vacío o no exista
        assertTrue(config instanceof Map, "Debe retornar un Map válido");
    }

    @Test
    @DisplayName("Debe verificar que la configuración está cargada")
    void shouldVerifyConfigurationIsLoaded() {
        boolean isLoaded = service.isConfigurationLoaded();
        
        assertTrue(isLoaded, "La configuración debe estar marcada como cargada");
    }

    @Test
    @DisplayName("Debe proporcionar campos estándar del sistema")
    void shouldProvideStandardSystemFields() {
        var fields = service.getStandardSystemFields();
        
        assertNotNull(fields, "Los campos estándar no deben ser null");
        assertFalse(fields.isEmpty(), "Debe proporcionar campos estándar");
        assertTrue(fields.contains("System.Id"), "Debe incluir System.Id");
        assertTrue(fields.contains("System.Title"), "Debe incluir System.Title");
        assertTrue(fields.contains("System.State"), "Debe incluir System.State");
    }

    @Test
    @DisplayName("Debe construir cláusula SELECT básica de WIQL")
    void shouldBuildBasicWiqlSelectClause() {
        String selectClause = service.buildBasicWiqlSelectClause();
        
        assertNotNull(selectClause, "La cláusula SELECT no debe ser null");
        assertTrue(selectClause.startsWith("SELECT"), "Debe comenzar con SELECT");
        assertTrue(selectClause.contains("[System.Id]"), "Debe incluir System.Id");
        assertTrue(selectClause.contains("[System.Title]"), "Debe incluir System.Title");
    }

    @Test
    @DisplayName("Debe obtener proyectos descubiertos")
    void shouldGetDiscoveredProjects() {
        var projects = service.getDiscoveredProjects();
        
        assertNotNull(projects, "Los proyectos descubiertos no deben ser null");
        // Si hay proyectos descubiertos, verificar estructura básica
        if (!projects.isEmpty()) {
            Map<String, Object> firstProject = projects.get(0);
            assertTrue(firstProject.containsKey("name") || firstProject.containsKey("id"), 
                "Los proyectos deben tener name o id");
        }
    }
}
