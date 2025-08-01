package com.mcp.server.utils.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad centralizada para obtener configuración de Azure DevOps.
 * Proporciona acceso uniforme a configuración desde variables de entorno y propiedades del sistema.
 */
public class AzureDevOpsConfigUtil {
    
    // Constantes para nombres de variables de entorno
    private static final String ENV_ORGANIZATION = "AZURE_DEVOPS_ORGANIZATION";
    private static final String ENV_PAT = "AZURE_DEVOPS_PAT";
    private static final String ENV_API_VERSION = "AZURE_DEVOPS_API_VERSION";
    
    // Constantes para propiedades del sistema
    private static final String PROP_ORGANIZATION = "azure.devops.organization";
    private static final String PROP_PAT = "azure.devops.pat";
    private static final String PROP_API_VERSION = "azure.devops.apiVersion";
    
    // Valores por defecto
    private static final String DEFAULT_API_VERSION = "7.1";
    
    /**
     * Obtiene el nombre de la organización de Azure DevOps.
     * 
     * @return nombre de la organización
     * @throws IllegalStateException si no se encuentra configuración
     */
    public static String getOrganization() {
        String organization = getConfigValue(ENV_ORGANIZATION, PROP_ORGANIZATION, null);
        if (organization == null || organization.trim().isEmpty()) {
            throw new IllegalStateException("Azure DevOps organization not configured. Set " + ENV_ORGANIZATION + " environment variable or " + PROP_ORGANIZATION + " system property.");
        }
        return organization.trim();
    }
    
    /**
     * Obtiene el Personal Access Token de Azure DevOps.
     * 
     * @return PAT de Azure DevOps
     * @throws IllegalStateException si no se encuentra configuración
     */
    public static String getPersonalAccessToken() {
        String pat = getConfigValue(ENV_PAT, PROP_PAT, null);
        if (pat == null || pat.trim().isEmpty()) {
            throw new IllegalStateException("Azure DevOps PAT not configured. Set " + ENV_PAT + " environment variable or " + PROP_PAT + " system property.");
        }
        return pat.trim();
    }
    
    /**
     * Obtiene la versión de la API de Azure DevOps.
     * 
     * @return versión de la API
     */
    public static String getApiVersion() {
        return getConfigValue(ENV_API_VERSION, PROP_API_VERSION, DEFAULT_API_VERSION);
    }
    
    /**
     * Verifica si la configuración básica está disponible.
     * 
     * @return true si organización y PAT están configurados
     */
    public static boolean isConfigurationAvailable() {
        try {
            getOrganization();
            getPersonalAccessToken();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * Obtiene toda la configuración disponible como un mapa.
     * 
     * @return mapa con configuración disponible
     */
    public static Map<String, String> getAllConfiguration() {
        Map<String, String> config = new HashMap<>();
        
        try {
            config.put("organization", getOrganization());
        } catch (IllegalStateException e) {
            config.put("organization", "NOT_CONFIGURED");
        }
        
        try {
            config.put("pat", getPersonalAccessToken().substring(0, Math.min(4, getPersonalAccessToken().length())) + "****");
        } catch (IllegalStateException e) {
            config.put("pat", "NOT_CONFIGURED");
        }
        
        config.put("apiVersion", getApiVersion());
        
        return config;
    }
    
    /**
     * Obtiene información de configuración para diagnóstico (sin exponer el PAT completo).
     * 
     * @return string con información de configuración
     */
    public static String getConfigurationInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Azure DevOps Configuration:\n");
        
        Map<String, String> config = getAllConfiguration();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            info.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Construye la URL base para la organización de Azure DevOps.
     * 
     * @return URL base de la organización
     */
    public static String getOrganizationBaseUrl() {
        return "https://dev.azure.com/" + getOrganization();
    }
    
    /**
     * Construye la URL base para APIs de la organización.
     * 
     * @return URL base para APIs
     */
    public static String getApiBaseUrl() {
        return getOrganizationBaseUrl();
    }
    
    /**
     * Obtiene un valor de configuración probando múltiples fuentes en orden de preferencia.
     * 
     * @param envVar nombre de la variable de entorno
     * @param sysProp nombre de la propiedad del sistema
     * @param defaultValue valor por defecto si no se encuentra en ninguna fuente
     * @return valor de configuración
     */
    private static String getConfigValue(String envVar, String sysProp, String defaultValue) {
        // 1. Intentar variable de entorno
        String value = System.getenv(envVar);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        
        // 2. Intentar propiedad del sistema
        value = System.getProperty(sysProp);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        
        // 3. Usar valor por defecto
        return defaultValue;
    }
    
    /**
     * Valida que la organización tenga un formato válido.
     * 
     * @param organization nombre de organización a validar
     * @return true si el formato es válido
     */
    public static boolean isValidOrganizationName(String organization) {
        if (organization == null || organization.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = organization.trim();
        
        // Validaciones básicas del formato de nombre de organización de Azure DevOps
        return trimmed.length() >= 2 && 
               trimmed.length() <= 50 &&
               trimmed.matches("^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]$");
    }
    
    /**
     * Valida que un PAT tenga un formato básico válido.
     * 
     * @param pat PAT a validar
     * @return true si el formato es básicamente válido
     */
    public static boolean isValidPatFormat(String pat) {
        if (pat == null || pat.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = pat.trim();
        
        // Validaciones básicas: longitud típica de PAT y caracteres base64
        return trimmed.length() >= 20 && 
               trimmed.length() <= 100 &&
               trimmed.matches("^[A-Za-z0-9+/=]+$");
    }
}
