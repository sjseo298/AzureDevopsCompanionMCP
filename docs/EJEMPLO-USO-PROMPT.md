# 📋 Ejemplo Práctico: Uso del Prompt de Generación Automática

## 🎯 **Escenario**

Un administrador se conecta por primera vez a una nueva organización de Azure DevOps llamada "MiEmpresa" y necesita generar toda la configuración organizacional automáticamente.

## 🚀 **Paso a Paso**

### **1. Verificar Estado Inicial**
```bash
# Verificar que no existen archivos de configuración
ls -la config/
# Output: directorio vacío o archivos básicos solamente
```

### **2. Ejecutar el Prompt**
```javascript
// Llamada al prompt MCP
generar_configuracion_organizacional({
  "forzar_regeneracion": false,
  "proyecto_principal": null,        // Detección automática
  "incluir_campos_extendidos": true,
  "generar_backup": true
})
```

### **3. Respuesta del Asistente IA**

El asistente ejecutará automáticamente la siguiente secuencia:

#### **📁 Verificación de Archivos**
```bash
Verificando directorio config/...
❌ discovered-organization.yml - FALTANTE
❌ organization-config.yml - FALTANTE  
❌ field-mappings.yml - FALTANTE
❌ business-rules.yml - FALTANTE

✅ Regeneración necesaria iniciada...
```

#### **🔍 Descubrimiento de Proyectos**
```javascript
azuredevops_list_projects({})
```
```yaml
Proyectos encontrados:
- "Desarrollo-Principal" (234 work items, 12 equipos)
- "QA-Testing" (45 work items, 3 equipos)  
- "Infraestructura" (67 work items, 5 equipos)

🎯 Seleccionado como principal: "Desarrollo-Principal"
```

#### **📊 Descubrimiento Organizacional Completo**
```javascript
azuredevops_discover_organization({
  "project": "Desarrollo-Principal",
  "includeWorkItemTypes": true,
  "includeFields": true,
  "generateConfig": true
})
```

#### **🔧 Análisis Detallado por Proyecto**
```javascript
// Para cada proyecto encontrado:
azuredevops_get_workitem_types({
  "project": "Desarrollo-Principal",
  "includeExtendedInfo": true,
  "includeFieldDetails": true
})

azuredevops_list_teams({
  "project": "Desarrollo-Principal"
})

azuredevops_list_iterations({
  "project": "Desarrollo-Principal",
  "team": "equipo-backend"
})
```

### **4. Archivos Generados**

#### **`config/discovered-organization.yml`**
```yaml
# Estructura Organizacional Descubierta Automáticamente
# Generado el: 2025-07-30T15:45:00Z
# Organización: MiEmpresa

discoveryDate: "2025-07-30T15:45:00Z"
lastUpdated: "2025-07-30T15:45:00Z"
organizationUrl: "https://dev.azure.com/miempresa"
organizationName: "MiEmpresa"

# Resumen del descubrimiento
discoveryMetadata:
  totalProjects: 3
  totalWorkItemTypes: 8
  customWorkItemTypes: 3
  standardFieldsFound: 25
  customFieldsFound: 18
  dateFieldsFound: 6

# Proyectos descubiertos
projects:
  - id: "desarrollo-principal"
    name: "Desarrollo-Principal"
    description: "Proyecto principal de desarrollo"
    
    teams:
      - name: "equipo-backend"
        description: "Desarrollo backend"
        prefix: "be-"
        domain: "desarrollo"
        function: "backend"
      - name: "equipo-frontend" 
        description: "Desarrollo frontend"
        prefix: "fe-"
        domain: "desarrollo"
        function: "frontend"
    
    # Tipos de work items descubiertos
    workItemTypes:
      - name: "User Story"
        baseType: "User Story"
        description: "Historia de usuario estándar"
        requiredFields:
          - "System.Title"
          - "System.Description"
          - "Microsoft.VSTS.Common.AcceptanceCriteria"
        
        # Campos de fecha detectados
        dateFields:
          - "Microsoft.VSTS.Scheduling.StartDate"
          - "Microsoft.VSTS.Scheduling.FinishDate"
          - "Microsoft.VSTS.Scheduling.TargetDate"
        
        # Campos personalizados encontrados
        customFields:
          - name: "Prioridad Negocio"
            referenceName: "Custom.PrioridadNegocio"
            type: "picklistString"
            allowedValues: ["Alta", "Media", "Baja"]
            required: false
```

#### **`config/organization-config.yml`**
```yaml
# Configuración Organizacional - MiEmpresa
# Archivo personalizable por el usuario

organization:
  name: "MiEmpresa"
  displayName: "Mi Empresa Tecnológica"
  description: "Organización de desarrollo de software"
  
  azure:
    organization: "miempresa"
    baseUrl: "https://dev.azure.com/miempresa"
    defaultProject: "Desarrollo-Principal"

# Tipos de work items personalizados
workItemTypes:
  custom:
    - name: "User Story"
      baseType: "User Story"
      description: "Historia de usuario con campos de MiEmpresa"
      
      requiredFields:
        - name: "acceptanceCriteria"
          displayName: "Criterios de Aceptación"
          fieldName: "Microsoft.VSTS.Common.AcceptanceCriteria"
          type: "html"
          helpText: "Definir claramente los criterios de aceptación"
        
        - name: "prioridadNegocio"
          displayName: "Prioridad de Negocio"
          fieldName: "Custom.PrioridadNegocio"
          type: "picklist"
          values: ["Alta", "Media", "Baja"]
          defaultValue: "Media"
          helpText: "Prioridad desde perspectiva de negocio"
```

#### **`config/field-mappings.yml`**
```yaml
# Mapeo Detallado de Campos Personalizados
# Generado automáticamente desde descubrimiento

fieldMappings:
  # Campos básicos estándar
  title:
    azureFieldName: "System.Title"
    required: true
    type: "string"
    helpText: "Título descriptivo del work item"
  
  description:
    azureFieldName: "System.Description"
    required: false
    type: "html"
    helpText: "Descripción detallada del work item"
  
  # Campos personalizados descubiertos
  prioridadNegocio:
    azureFieldName: "Custom.PrioridadNegocio"
    required: false
    type: "picklistString"
    allowedValues: ["Alta", "Media", "Baja"]
    defaultValue: "Media"
    helpText: "Prioridad asignada por el área de negocio"

# Validaciones detectadas
validations:
  prioridadNegocio:
    rule: "required_if_type"
    condition: "workItemType == 'User Story'"
    message: "Prioridad de negocio es obligatoria para historias de usuario"
```

#### **`config/business-rules.yml`**
```yaml
# Reglas de Negocio Organizacionales
# Detectadas automáticamente durante el descubrimiento

businessRules:
  validation:
    # Reglas para User Stories
    userStory:
      - rule: "Criterios de aceptación son obligatorios"
        field: "acceptanceCriteria"
        condition: "not_empty"
        
      - rule: "Prioridad debe estar definida"
        field: "prioridadNegocio"
        condition: "in_list"
        values: ["Alta", "Media", "Baja"]
  
  workflow:
    userStory:
      states: ["New", "Active", "Resolved", "Closed"]
      transitions:
        New: ["Active"]
        Active: ["Resolved", "Closed"]
        Resolved: ["Active", "Closed"]
        Closed: []
  
  automation:
    autoAssignment:
      enabled: true
      rules:
        - condition: "prioridadNegocio == 'Alta'"
          action: "assign_to_lead"
```

### **5. Validación y Optimización**

```yaml
✅ VALIDACIÓN COMPLETADA:
- Sintaxis YAML: VÁLIDA
- Campos obligatorios: PRESENTES
- Referencias cruzadas: VÁLIDAS
- Valores permitidos: CONSISTENTES

✅ OPTIMIZACIÓN APLICADA:
- Campos duplicados: REMOVIDOS
- Estructura: ORGANIZADA
- Comentarios: AGREGADOS
- Mejores prácticas: APLICADAS
```

### **6. Reporte Final**

```markdown
🎉 **GENERACIÓN AUTOMÁTICA COMPLETADA**

📊 **Estadísticas:**
- Proyectos analizados: 3
- Equipos descubiertos: 20
- Tipos de work items: 8
- Campos personalizados: 18
- Reglas de negocio: 12

📁 **Archivos creados:**
- config/discovered-organization.yml (1,234 líneas)
- config/organization-config.yml (456 líneas)
- config/field-mappings.yml (189 líneas)
- config/business-rules.yml (67 líneas)

✅ **Estado:** LISTO PARA USO
⚙️ **Próximo paso:** Personalizar organization-config.yml según necesidades
```

## 🔧 **Personalización Post-Generación**

### **Editar Configuración Organizacional**
```yaml
# Personalizar config/organization-config.yml
organization:
  displayName: "Mi Empresa - División Tecnológica"  # ✏️ Personalizado
  
workItemTypes:
  custom:
    - name: "User Story"
      requiredFields:
        - name: "componente"  # ✏️ Campo adicional personalizado
          displayName: "Componente Afectado"
          fieldName: "Custom.ComponenteAfectado"
          type: "picklist"
          values: ["Frontend", "Backend", "Database", "API"]
```

### **Validar Configuración**
```bash
# Probar la nueva configuración
azuredevops_get_workitem_types({
  "project": "Desarrollo-Principal"
})

# Verificar que los campos personalizados se muestran
azuredevops_create_workitem({
  "project": "Desarrollo-Principal",
  "type": "User Story",
  "title": "Test de configuración",
  "prioridadNegocio": "Alta"  # ✅ Campo personalizado disponible
})
```

## 🏆 **Resultado**

- ✅ **Configuración completa** generada automáticamente
- ✅ **Campos personalizados** completamente mapeados  
- ✅ **Reglas de negocio** detectadas y configuradas
- ✅ **Lista para producción** sin intervención manual
- ✅ **Documentación automática** de la estructura organizacional

Este proceso transforma una instalación "en blanco" en un sistema completamente configurado y listo para uso productivo en cuestión de minutos.
