package com.mcp.server.service.config;

import com.mcp.server.config.OrganizationConfigService;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Servicio para manejar la configuración organizacional desde archivos YML
 * y completar automáticamente los valores faltantes usando la API de Azure DevOps.
 * Todas las llamadas HTTP se centralizan en AzureDevOpsClient.
 */
@Service
public class OrganizationalConfigService {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final OrganizationConfigService configService;
    private final Yaml yaml;
    
    private static final String CONFIG_PATH = "src/test/resources/config/discovered-organization.yml";
    private static final String FIELD_MAPPINGS_PATH = "src/test/resources/config/field-mappings.yml";
    
    @Autowired
    public OrganizationalConfigService(AzureDevOpsClient azureDevOpsClient, OrganizationConfigService configService) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.configService = configService;
        this.yaml = new Yaml();
    }
    
    /**
     * Obtiene la configuración organizacional, completando automáticamente los valores faltantes
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrganizationalConfig(String project) {
        try {
            Map<String, Object> config = loadConfigFromFile();
            
            if (config == null || config.isEmpty()) {
                // Si no hay configuración, crear una básica
                config = createBasicConfig(project);
            }
            
            // Completar valores faltantes automáticamente
            completeMissingValues(config, project);
            
            // Guardar la configuración actualizada
            saveConfigToFile(config);
            
            return config;
            
        } catch (Exception e) {
            System.err.println("Error obteniendo configuración organizacional: " + e.getMessage());
            return createBasicConfig(project);
        }
    }
    
    /**
     * Obtiene valores válidos para un campo de picklist específico
     */
    public List<String> getValidValuesForField(String project, String fieldName) {
        try {
            Map<String, Object> config = loadConfigFromFile();
            if (config == null) return Collections.emptyList();
            
            // Buscar en customFields
            @SuppressWarnings("unchecked")
            Map<String, Object> customFields = (Map<String, Object>) config.get("customFields");
            if (customFields != null) {
                for (Map.Entry<String, Object> entry : customFields.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldConfig = (Map<String, Object>) entry.getValue();
                    String technicalName = (String) fieldConfig.get("technicalName");
                    
                    if (fieldName.equals(technicalName)) {
                        String status = (String) fieldConfig.get("status");
                        
                        // Si los valores están marcados como funcionales, retornarlos
                        if ("✅ FUNCIONAL".equals(status)) {
                            @SuppressWarnings("unchecked")
                            List<String> validValues = (List<String>) fieldConfig.get("validValues");
                            return validValues != null ? validValues : Collections.emptyList();
                        }
                        
                        // Si requiere investigación, intentar obtener valores de la API
                        if ("⚠️ REQUIERE INVESTIGACIÓN".equals(status)) {
                            List<String> apiValues = getFieldValuesFromAPI(project, fieldName);
                            if (!apiValues.isEmpty()) {
                                fieldConfig.put("validValues", apiValues);
                                fieldConfig.put("status", "✅ FUNCIONAL");
                                saveConfigToFile(config);
                                return apiValues;
                            }
                        }
                    }
                }
            }
            
            // Si no se encontró en la config, intentar obtener de la API directamente
            return getFieldValuesFromAPI(project, fieldName);
            
        } catch (Exception e) {
            System.err.println("Error obteniendo valores para campo " + fieldName + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Carga la configuración desde el archivo YML
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfigFromFile() {
        try {
            Path configPath = Paths.get(CONFIG_PATH);
            if (Files.exists(configPath)) {
                try (FileInputStream inputStream = new FileInputStream(configPath.toFile())) {
                    return yaml.load(inputStream);
                }
            }
        } catch (Exception e) {
            System.err.println("Error cargando configuración desde archivo: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Guarda la configuración en el archivo YML
     */
    private void saveConfigToFile(Map<String, Object> config) {
        try {
            Path configPath = Paths.get(CONFIG_PATH);
            Files.createDirectories(configPath.getParent());
            
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                yaml.dump(config, writer);
            }
        } catch (IOException e) {
            System.err.println("Error guardando configuración en archivo: " + e.getMessage());
        }
    }
    
    /**
     * Crea una configuración básica para empezar
     */
    private Map<String, Object> createBasicConfig(String project) {
        Map<String, Object> config = new HashMap<>();
        config.put("organization", azureDevOpsClient.getOrganization());
        config.put("project", project);
        config.put("lastUpdated", new Date().toString());
        config.put("customFields", new HashMap<>());
        config.put("workItemTypes", new HashMap<>());
        return config;
    }
    
    /**
     * Completa valores faltantes en la configuración
     */
    @SuppressWarnings("unchecked")
    private void completeMissingValues(Map<String, Object> config, String project) {
        try {
            Map<String, Object> customFields = (Map<String, Object>) config.get("customFields");
            if (customFields != null) {
                for (Map.Entry<String, Object> entry : customFields.entrySet()) {
                    Map<String, Object> fieldConfig = (Map<String, Object>) entry.getValue();
                    String status = (String) fieldConfig.get("status");
                    
                    if ("⚠️ REQUIERE INVESTIGACIÓN".equals(status) || "❌ VALORES DESCONOCIDOS".equals(status)) {
                        String technicalName = (String) fieldConfig.get("technicalName");
                        if (technicalName != null) {
                            List<String> apiValues = getFieldValuesFromAPI(project, technicalName);
                            if (!apiValues.isEmpty()) {
                                fieldConfig.put("validValues", apiValues);
                                fieldConfig.put("status", "✅ FUNCIONAL");
                                System.out.println("✅ Completado campo: " + technicalName + " con " + apiValues.size() + " valores");
                            }
                        }
                    }
                }
            }
            
            config.put("lastUpdated", new Date().toString());
            
        } catch (Exception e) {
            System.err.println("Error completando valores faltantes: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene valores de campo usando AzureDevOpsClient
     */
    private List<String> getFieldValuesFromAPI(String project, String fieldName) {
        try {
            Map<String, Object> response = azureDevOpsClient.getFieldAllowedValues(project, fieldName);
            return extractAllowedValuesFromResponse(response);
            
        } catch (AzureDevOpsException e) {
            System.err.println("Error obteniendo valores de campo desde API: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Extrae valores permitidos de la respuesta de la API de campos
     */
    private List<String> extractAllowedValuesFromResponse(Map<String, Object> response) {
        List<String> values = new ArrayList<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<String> allowedValues = (List<String>) response.get("value");
            if (allowedValues != null) {
                values.addAll(allowedValues);
            }
        } catch (Exception e) {
            System.err.println("Error parsing allowed values from response: " + e.getMessage());
        }
        
        return values;
    }
}
