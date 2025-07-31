package com.mcp.server.test;

import com.mcp.server.config.OrganizationConfigService;

import java.util.List;

/**
 * Test manual para verificar la funcionalidad de valores dinámicos
 */
public class DynamicValuesManualTest {

    public static void main(String[] args) {
        System.out.println("=== Test Manual de Valores Dinámicos ===");
        
        // Test 1: OrganizationConfigService sin dependencias (fallback a valores hardcodeados)
        System.out.println("\n1. Test con constructor sin dependencias:");
        OrganizationConfigService configServiceBasic = new OrganizationConfigService();
        
        List<String> stateValues = configServiceBasic.getAllowedValues("state");
        List<String> priorityValues = configServiceBasic.getAllowedValues("priority");
        List<String> unknownValues = configServiceBasic.getAllowedValues("unknownField");
        
        System.out.println("state values: " + stateValues);
        System.out.println("priority values: " + priorityValues);
        System.out.println("unknown field values: " + unknownValues);
        
        // Test 2: Probar con campos que no existen en YAML (debería usar fallback)
        System.out.println("\n2. Test de campos no configurados:");
        
        List<String> reasonValues = configServiceBasic.getAllowedValues("reason");
        List<String> customFieldValues = configServiceBasic.getAllowedValues("tipoDeHistoria");
        
        System.out.println("reason values: " + reasonValues);
        System.out.println("tipoDeHistoria values (sin configuración YAML): " + customFieldValues);
        
        System.out.println("\n=== Test Básico Completado ===");
        System.out.println("\nNOTA: Para probar valores dinámicos reales, es necesario:");
        System.out.println("1. Tener configuración YAML con @DYNAMIC_FROM_AZURE_DEVOPS");
        System.out.println("2. Tener conexión a Azure DevOps configurada");
        System.out.println("3. Ejecutar en contexto de Spring con dependencias inyectadas");
    }
}
