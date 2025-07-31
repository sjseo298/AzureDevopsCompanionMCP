package com.mcp.server.config;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Servicio de validación de seguridad para prevenir errores comunes como
 * sobreescritura accidental de contenido importante.
 * 
 * <p>Este servicio implementa validaciones proactivas para:
 * <ul>
 *   <li>Detectar intentos de sobreescritura de descripciones existentes</li>
 *   <li>Validar campos críticos antes de actualizar</li>
 *   <li>Proporcionar advertencias y sugerencias al usuario</li>
 *   <li>Registrar operaciones de riesgo para auditoría</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class SecurityValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityValidationService.class);
    
    // Campos considerados críticos que requieren validación especial
    private static final List<String> CRITICAL_FIELDS = List.of(
        "System.Description",
        "System.Title", 
        "Microsoft.VSTS.Common.AcceptanceCriteria",
        "Microsoft.VSTS.TCM.ReproSteps"
    );
    
    /**
     * Valida si una operación de actualización de descripción es segura.
     * 
     * @param currentDescription descripción actual del work item
     * @param newDescription nueva descripción propuesta
     * @param workItemId ID del work item being updated
     * @return resultado de la validación con advertencias y recomendaciones
     */
    public ValidationResult validateDescriptionUpdate(String currentDescription, String newDescription, Integer workItemId) {
        ValidationResult result = new ValidationResult();
        
        // Si no hay descripción actual, la actualización es segura
        if (currentDescription == null || currentDescription.trim().isEmpty()) {
            result.setSafe(true);
            result.addMessage("✅ Actualización segura: No hay descripción existente");
            return result;
        }
        
        // Si la nueva descripción es igual a la actual, no hay problema
        if (currentDescription.equals(newDescription)) {
            result.setSafe(true);
            result.addMessage("✅ No hay cambios en la descripción");
            return result;
        }
        
        // VALIDACIÓN DE RIESGO: Descripción existente será sobreescrita
        result.setSafe(false);
        result.setHighRisk(true);
        
        // Generar advertencias detalladas
        result.addWarning("⚠️  ADVERTENCIA CRÍTICA: Se va a SOBREESCRIBIR la descripción existente");
        result.addWarning("📝 Descripción actual: " + truncateText(currentDescription, 100));
        result.addWarning("🔄 Nueva descripción: " + truncateText(newDescription, 100));
        result.addWarning("");
        result.addWarning("🚨 IMPACTO: La descripción original se perderá completamente");
        result.addWarning("💡 RECOMENDACIÓN: Use 'azuredevops_add_comment' para agregar comentarios sin sobreescribir");
        result.addWarning("🔗 Alternativa: Use el parámetro 'comment' en update_workitem");
        
        // Logging para auditoría
        logger.warn("SECURITY ALERT: Description overwrite attempt for WorkItem #{} - Original: {} chars, New: {} chars", 
                   workItemId, currentDescription.length(), newDescription.length());
        
        return result;
    }
    
    /**
     * Valida si un campo crítico está siendo modificado de forma segura.
     * 
     * @param fieldName nombre del campo Azure DevOps
     * @param currentValue valor actual del campo
     * @param newValue nuevo valor propuesto
     * @param workItemId ID del work item
     * @return resultado de la validación
     */
    public ValidationResult validateCriticalFieldUpdate(String fieldName, Object currentValue, Object newValue, Integer workItemId) {
        ValidationResult result = new ValidationResult();
        
        if (!CRITICAL_FIELDS.contains(fieldName)) {
            result.setSafe(true);
            return result;
        }
        
        // Si no hay valor actual, la actualización es segura
        if (currentValue == null) {
            result.setSafe(true);
            result.addMessage("✅ Campo crítico seguro: No hay valor existente para " + fieldName);
            return result;
        }
        
        // Si los valores son iguales, no hay cambio
        if (Objects.equals(currentValue, newValue)) {
            result.setSafe(true);
            return result;
        }
        
        // Advertencia para campos críticos
        result.setSafe(false);
        result.addWarning("⚠️  Modificando campo crítico: " + fieldName);
        result.addWarning("📝 Valor actual: " + truncateText(String.valueOf(currentValue), 100));
        result.addWarning("🔄 Nuevo valor: " + truncateText(String.valueOf(newValue), 100));
        
        logger.info("Critical field update: WorkItem #{}, Field: {}, User initiated change", workItemId, fieldName);
        
        return result;
    }
    
    /**
     * Verifica si una operación debe ser bloqueada por políticas de seguridad.
     * 
     * @param operation tipo de operación (update, delete, etc.)
     * @param workItemId ID del work item
     * @param fields campos being modified
     * @return true si la operación debe ser bloqueada
     */
    public boolean shouldBlockOperation(String operation, Integer workItemId, Map<String, Object> fields) {
        // Por ahora, no bloqueamos ninguna operación, solo advertimos
        // En el futuro se pueden implementar políticas más estrictas
        return false;
    }
    
    /**
     * Registra una operación de riesgo para auditoría.
     */
    public void logRiskOperation(String operation, Integer workItemId, String details) {
        logger.warn("RISK OPERATION: {} on WorkItem #{} - {}", operation, workItemId, details);
    }
    
    /**
     * Trunca texto para mostrar en advertencias.
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * Clase para encapsular resultados de validación.
     */
    public static class ValidationResult {
        private boolean safe = true;
        private boolean highRisk = false;
        private List<String> messages = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public boolean isSafe() { return safe; }
        public void setSafe(boolean safe) { this.safe = safe; }
        
        public boolean isHighRisk() { return highRisk; }
        public void setHighRisk(boolean highRisk) { this.highRisk = highRisk; }
        
        public List<String> getMessages() { return messages; }
        public void addMessage(String message) { this.messages.add(message); }
        
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        public String getFormattedWarnings() {
            return String.join("\n", warnings);
        }
    }
}

// Necesario para compilation
class Objects {
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}

class ArrayList<T> extends java.util.ArrayList<T> {
    public ArrayList() {
        super();
    }
}
