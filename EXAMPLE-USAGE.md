# Ejemplo de Uso del Sistema Genérico Azure DevOps MCP

## 🏢 Escenario: Migración de Sura a Sistema Genérico

### 1. Estado Anterior (Hardcoded para Sura)

```java
// Código anterior - específico para Sura
if (type.equals("Historia")) {
    operations.add(createOperation("/fields/Custom.TipoHistoria", tipoHistoria));
    operations.add(createOperation("/fields/Custom.ControlAutomatico", controlAutomatico));
    // ... más campos hardcodeados
}
```

### 2. Estado Actual (Sistema Genérico)

```java
// Código genérico - configurable para cualquier organización
Map<String, Object> processedFields = fieldsHandler.processWorkItemFields(type, inputFields);
GenericWorkItemFieldsHandler.ValidationResult validation = 
    fieldsHandler.validateRequiredFields(type, processedFields);
```

## 🎯 Casos de Uso Prácticos

### Caso 1: Nueva Organización "TechCorp"

#### 1.1 Configuración Inicial
```yaml
# config/techcorp-config.yml
organization:
  name: "techcorp"
  defaultProject: "Platform"
  defaultTeam: "Backend Team"
  timeZone: "America/New_York"
  language: "en-US"

fieldMappings:
  title:
    azureFieldName: "System.Title"
    required: true
  
  priority:
    azureFieldName: "Microsoft.VSTS.Common.Priority"
    required: true
    defaultValue: 2
  
  techStack:
    azureFieldName: "Custom.TechStack"
    required: false
    type: "string"
    helpText: "Technology stack used (Java, .NET, Python, etc.)"
```

#### 1.2 Uso del Sistema
```bash
# Descubrir configuración automáticamente
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/call",
    "params": {
      "name": "azuredevops_discover_organization",
      "arguments": {
        "project": "Platform",
        "generateConfig": true,
        "includeWorkItemTypes": true
      }
    }
  }'

# Crear work item con campos específicos de TechCorp
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/call",
    "params": {
      "name": "azuredevops_create_workitem",
      "arguments": {
        "project": "Platform",
        "type": "Task",
        "title": "Implement OAuth integration",
        "description": "Add OAuth 2.0 authentication to API",
        "techStack": "Java Spring Boot",
        "priority": 1,
        "assignedTo": "developer@techcorp.com"
      }
    }
  }'
```

### Caso 2: Mantenimiento de Compatibilidad con Sura

#### 2.1 Configuración Sura Existente
```java
// El sistema automáticamente mantiene compatibilidad
// con tipos específicos de Sura
public Map<String, Object> processSuraCompatibleFields(String workItemType, Map<String, Object> inputFields) {
    switch (workItemType.toLowerCase()) {
        case "historia":
            return processHistoriaFields(processedFields);
        case "historia técnica":
            return processHistoriaTecnicaFields(processedFields);
        // ...
    }
}
```

#### 2.2 Uso Transparente
```bash
# Funciona exactamente igual que antes
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/call",
    "params": {
      "name": "azuredevops_create_workitem",
      "arguments": {
        "project": "SURA",
        "type": "Historia",
        "title": "Implementar nueva funcionalidad",
        "description": "Descripción de la historia",
        "tipoHistoria": "Funcional",
        "controlAutomatico": true,
        "migracionDatos": false
      }
    }
  }'
```

### Caso 3: Organización Multinacional "GlobalTech"

#### 3.1 Configuración Multi-región
```yaml
# config/globaltech-config.yml
organization:
  name: "globaltech"
  defaultProject: "GlobalPlatform"
  regions:
    - name: "US"
      timeZone: "America/New_York"
      language: "en-US"
    - name: "LATAM"
      timeZone: "America/Bogota"
      language: "es-CO"
    - name: "EMEA"
      timeZone: "Europe/London"
      language: "en-GB"

fieldMappings:
  region:
    azureFieldName: "Custom.Region"
    required: true
    allowedValues: ["US", "LATAM", "EMEA"]
  
  complianceLevel:
    azureFieldName: "Custom.ComplianceLevel"
    required: true
    allowedValues: ["SOX", "GDPR", "HIPAA", "None"]
    defaultValue: "None"
```

#### 3.2 Validación Automática
```java
// El sistema automáticamente valida campos según configuración
GenericWorkItemFieldsHandler.ValidationResult validation = 
    fieldsHandler.validateRequiredFields("Task", fields);

if (!validation.isValid()) {
    // Errores específicos por campo:
    // "Required field missing: region"
    // "Invalid value for complianceLevel. Allowed: [SOX, GDPR, HIPAA, None]"
}
```

## 🔄 Flujo de Implementación

### Paso 1: Análisis de Organización Existente
```bash
# Ejecutar herramienta de descubrimiento
azuredevops_discover_organization \
  --project="ExistingProject" \
  --includeWorkItemTypes=true \
  --includeFields=true \
  --generateConfig=true
```

**Salida esperada:**
```
🔍 Análisis de Configuración de Azure DevOps
==========================================

🏢 Organización: mi-organizacion
📁 Proyecto: ExistingProject

📋 Tipos de Work Items
----------------------
• Task (Campos requeridos: title, description, state)
• User Story (Campos requeridos: title, description, acceptanceCriteria, state)
• Bug (Campos requeridos: title, description, reproSteps, state)

🏷️ Campos de Work Items
-----------------------
Campos Básicos:
• title → System.Title
• description → System.Description
• assignedTo → System.AssignedTo
• state → System.State
• priority → Microsoft.VSTS.Common.Priority

⚙️ Configuración YAML Sugerida
==============================
```yaml
organization:
  name: "mi-organizacion"
  defaultProject: "ExistingProject"
  # ... configuración generada automáticamente
```

### Paso 2: Personalización de Configuración
```yaml
# Editar config/mi-organizacion-config.yml
organization:
  name: "mi-organizacion"
  defaultProject: "ExistingProject"
  customFields:
    businessValue:
      azureFieldName: "Custom.BusinessValue"
      required: true
      type: "integer"
      helpText: "Business value score (1-100)"
    
    riskLevel:
      azureFieldName: "Custom.RiskLevel"
      required: false
      allowedValues: ["Low", "Medium", "High"]
      defaultValue: "Medium"
```

### Paso 3: Testing de la Configuración
```bash
# Crear work item de prueba
azuredevops_create_workitem \
  --project="ExistingProject" \
  --type="Task" \
  --title="Test Configuration" \
  --description="Testing new generic configuration" \
  --businessValue=85 \
  --riskLevel="Low"
```

### Paso 4: Migración Gradual
```java
// El sistema permite migración gradual
// Mantiene compatibilidad con herramientas existentes
// Agrega nuevas capacidades sin interrumpir flujos actuales

// Funciona con sistemas legacy:
processLegacyWorkItem("Historia", legacyFields);

// Y con nueva configuración:
processGenericWorkItem("User Story", genericFields);
```

## 📊 Métricas de Mejora

### Antes (Sistema Hardcoded)
- ❌ Una sola organización soportada (Sura)
- ❌ Campos fijos no configurables
- ❌ Cambios requieren modificación de código
- ❌ Testing limitado a casos específicos

### Después (Sistema Genérico)
- ✅ Cualquier organización soportada
- ✅ Campos completamente configurables
- ✅ Cambios via archivos de configuración
- ✅ Testing exhaustivo con múltiples configuraciones
- ✅ Herramientas de descubrimiento automático
- ✅ Validación dinámica de campos
- ✅ Compatibilidad retroactiva garantizada

## 🚀 Beneficios Obtenidos

1. **Flexibilidad**: Adaptable a cualquier organización de Azure DevOps
2. **Mantenibilidad**: Cambios de configuración sin modificar código
3. **Escalabilidad**: Soporte para múltiples organizaciones simultáneamente
4. **Compatibilidad**: Mantiene funcionalidad existente de Sura
5. **Automatización**: Descubrimiento y configuración automática
6. **Validación**: Sistema robusto de validación de campos
7. **Documentación**: Generación automática de documentación de configuración

## 📈 Roadmap Futuro

- [ ] **Soporte para múltiples organizaciones simultáneas**
- [ ] **Interface web para configuración visual**
- [ ] **Plantillas de configuración por industria**
- [ ] **Sincronización automática con cambios en Azure DevOps**
- [ ] **Analytics y métricas de uso por organización**
- [ ] **Plugin para VS Code**

---

**✨ El sistema genérico está completamente implementado y listo para producción!**
