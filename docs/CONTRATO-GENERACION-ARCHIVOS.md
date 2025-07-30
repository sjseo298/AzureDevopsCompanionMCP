# 📋 Contrato de Generación de Archivos de Configuración Organizacional

## 🎯 **Propósito del Contrato**

Este documento establece las **especificaciones técnicas obligatorias** que deben cumplir todos los archivos de configuración organizacional generados para garantizar **compatibilidad total** con el Azure DevOps MCP Server.

**Versión del Contrato:** 1.0  
**Fecha:** 2025-07-30  
**Aplicación Objetivo:** Azure DevOps MCP Server v1.0.0+  
**Compatibilidad:** OrganizationContextService, OrganizationConfigService

---

## 📄 **ARCHIVO 1: discovered-organization.yml**

### **🔸 Especificación del Contrato**

**Propósito:** Almacenar la estructura organizacional completa descubierta automáticamente desde Azure DevOps.  
**Consumidor:** `OrganizationContextService.loadConfiguration()`  
**Validación:** Debe parsear como YAML válido sin errores.

### **📋 Estructura Obligatoria (Schema)**

```yaml
# =============================================================================
# CABECERA OBLIGATORIA - REQUERIDA PARA VALIDACIÓN
# =============================================================================
discoveryDate: string                          # ISO 8601 format: "2025-07-30T12:30:00Z"
lastUpdated: string                            # ISO 8601 format: "2025-07-30T12:30:00Z" 
organizationUrl: string                        # URL: "https://dev.azure.com/nombre-org"
organizationName: string                       # Nombre legible: "Mi Organización"

# =============================================================================
# METADATA DE DESCUBRIMIENTO - REQUERIDA PARA ESTADÍSTICAS
# =============================================================================
discoveryMetadata:
  totalProjects: integer                       # Número total de proyectos
  totalWorkItemTypes: integer                  # Número total de tipos de work items
  customWorkItemTypes: integer                 # Número de tipos personalizados
  standardFieldsFound: integer                 # Campos estándar encontrados
  customFieldsFound: integer                   # Campos personalizados encontrados
  dateFieldsFound: integer                     # Campos de fecha encontrados

# =============================================================================
# PROYECTOS - ESTRUCTURA PRINCIPAL (ARRAY REQUERIDO)
# =============================================================================
projects:
  - id: string                                 # REQUERIDO: Identificador único kebab-case
    name: string                               # REQUERIDO: Nombre exacto en Azure DevOps
    description: string                        # REQUERIDO: Descripción del proyecto
    
    # -------------------------------------------------------------------------
    # EQUIPOS DEL PROYECTO (ARRAY OPCIONAL)
    # -------------------------------------------------------------------------
    teams:                                     # OPCIONAL: Lista de equipos
      - name: string                           # REQUERIDO: Nombre del equipo
        description: string                    # REQUERIDO: Descripción
        prefix: string                         # OPCIONAL: Prefijo de nomenclatura
        domain: string                         # OPCIONAL: Dominio funcional
        function: string                       # OPCIONAL: Función específica
    
    # -------------------------------------------------------------------------
    # ESTRUCTURA DE ÁREAS (ARRAY OPCIONAL)
    # -------------------------------------------------------------------------
    areaStructure:                             # OPCIONAL: Jerarquía de áreas
      - string                                 # Formato: "Proyecto" o "Proyecto\\SubArea"
    
    # -------------------------------------------------------------------------
    # TIPOS DE WORK ITEMS (ARRAY REQUERIDO)
    # -------------------------------------------------------------------------
    workItemTypes:                             # REQUERIDO: Al menos un tipo
      - name: string                           # REQUERIDO: Nombre del tipo
        baseType: string                       # REQUERIDO: Tipo base Azure DevOps
        description: string                    # REQUERIDO: Descripción
        
        # CAMPOS OBLIGATORIOS (ARRAY REQUERIDO)
        requiredFields:                        # REQUERIDO: Campos obligatorios
          - string                             # Formato: "System.Title" o "Custom.Campo"
        
        # CAMPOS DE FECHA (ARRAY CRÍTICO PARA COMPATIBILIDAD)
        dateFields:                            # CRÍTICO: Evita problema identificado
          - "Microsoft.VSTS.Scheduling.StartDate"    # OBLIGATORIO
          - "Microsoft.VSTS.Scheduling.FinishDate"   # OBLIGATORIO  
          - "Microsoft.VSTS.Scheduling.TargetDate"   # CRÍTICO para Features
          - "Microsoft.VSTS.Scheduling.DueDate"      # OBLIGATORIO
        
        # CAMPOS DE MÉTRICAS (ARRAY OPCIONAL)
        metricFields:                          # OPCIONAL: Campos de métricas
          - "Microsoft.VSTS.Scheduling.StoryPoints"
          - "Microsoft.VSTS.Scheduling.RemainingWork"
          - "Microsoft.VSTS.Common.Priority"
        
        # DEFINICIÓN DE CAMPOS PERSONALIZADOS (ARRAY OPCIONAL)
        fields:                                # OPCIONAL: Campos personalizados
          - name: string                       # REQUERIDO: Nombre legible
            referenceName: string              # REQUERIDO: Referencia Azure DevOps
            type: enum                         # REQUERIDO: Ver tipos válidos abajo
            required: boolean                  # REQUERIDO: true/false
            allowedValues: array[string]       # CONDICIONAL: Solo para picklist
            defaultValue: any                  # OPCIONAL: Valor por defecto
            helpText: string                   # OPCIONAL: Texto de ayuda

# =============================================================================
# CONFIGURACIÓN ORGANIZACIONAL POR DEFECTO (REQUERIDA)
# =============================================================================
organizationConfig:                           # REQUERIDO: Configuración base
  defaultProject: string                      # REQUERIDO: Proyecto por defecto
  defaultAreaPath: string                     # REQUERIDO: Área por defecto
  defaultIterationPath: string               # REQUERIDO: Iteración por defecto
  
  commonRequiredFields:                       # REQUERIDO: Campos comunes
    - "System.Title"                          # OBLIGATORIO
    - "System.AreaPath"                       # OBLIGATORIO
    - "System.IterationPath"                  # OBLIGATORIO
  
  defaultValues:                              # OPCIONAL: Valores por defecto
    string: any                               # Formato: "Custom.Campo": "Valor"
```

### **🔧 Tipos de Datos Válidos**

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| `string` | Texto simple | "Título del work item" |
| `html` | HTML enriquecido | `<p>Descripción <b>rica</b></p>` |
| `plainText` | Texto plano | "Texto sin formato" |
| `integer` | Número entero | 42 |
| `double` | Número decimal | 3.14 |
| `boolean` | Verdadero/Falso | true/false |
| `dateTime` | Fecha y hora | "2025-07-30T12:30:00Z" |
| `identity` | Usuario de Azure DevOps | "usuario@empresa.com" |
| `picklistString` | Lista de opciones texto | ["Opción1", "Opción2"] |
| `picklistInteger` | Lista de opciones numérica | [1, 2, 3] |

### **⚠️ Restricciones y Validaciones**

1. **Referencias de Campos** deben seguir formato exacto:
   - Sistema: `System.Campo`
   - VSTS: `Microsoft.VSTS.Categoria.Campo`
   - Personalizados: `Custom.ReferenceName` o `Custom.GUID`

2. **IDs de Proyectos** deben ser kebab-case: `proyecto-principal`

3. **Campos de Fecha** son **CRÍTICOS** - Su ausencia causa el problema identificado

4. **allowedValues** solo es válido para tipos `picklistString` y `picklistInteger`

5. **required** debe ser exactamente `true` o `false` (boolean, no string)

---

## 📄 **ARCHIVO 2: organization-config.yml**

### **🔸 Especificación del Contrato**

**Propósito:** Configuración personalizable por el usuario final.  
**Consumidor:** `OrganizationConfigService.getDefaultOrganizationConfig()`  
**Validación:** Debe contener configuración Azure DevOps válida.

### **📋 Estructura Obligatoria (Schema)**

```yaml
# =============================================================================
# INFORMACIÓN ORGANIZACIONAL (REQUERIDA)
# =============================================================================
organization:                                 # REQUERIDO: Raíz organizacional
  name: string                                # REQUERIDO: Nombre interno
  displayName: string                         # REQUERIDO: Nombre para mostrar
  description: string                         # REQUERIDO: Descripción
  
  azure:                                      # REQUERIDO: Configuración Azure DevOps
    organization: string                      # REQUERIDO: Nombre en Azure DevOps
    baseUrl: string                           # REQUERIDO: URL base
    defaultProject: string                    # REQUERIDO: Proyecto por defecto

# =============================================================================
# TIPOS DE WORK ITEMS PERSONALIZADOS (OPCIONAL)
# =============================================================================
workItemTypes:                               # OPCIONAL: Personalización de tipos
  custom:                                    # OPCIONAL: Array de tipos personalizados
    - name: string                           # REQUERIDO: Nombre del tipo
      baseType: enum                         # REQUERIDO: Ver tipos base válidos
      description: string                    # REQUERIDO: Descripción
      category: enum                         # REQUERIDO: Ver categorías válidas
      
      requiredFields:                        # OPCIONAL: Campos requeridos
        - name: string                       # REQUERIDO: Nombre interno camelCase
          displayName: string                # REQUERIDO: Nombre para mostrar
          fieldName: string                  # REQUERIDO: Campo Azure DevOps
          type: enum                         # REQUERIDO: Ver tipos válidos
          helpText: string                   # OPCIONAL: Texto de ayuda
          
          # CONDICIONAL: Solo para picklist
          values: array[string]              # Para type: "picklist"
          defaultValue: any                  # OPCIONAL: Valor por defecto
      
      businessRules:                         # OPCIONAL: Reglas de negocio
        - condition: string                  # REQUERIDO: Condición
          then: string                       # REQUERIDO: Acción
```

### **🔧 Valores Enum Válidos**

**baseType** válidos:
- `"User Story"`, `"Task"`, `"Bug"`, `"Feature"`, `"Epic"`, `"Issue"`, `"Test Case"`

**category** válidos:
- `"functional"`, `"technical"`, `"defect"`, `"implementation"`

**type** válidos:
- `"string"`, `"html"`, `"picklist"`, `"boolean"`, `"integer"`

---

## 📄 **ARCHIVO 3: field-mappings.yml**

### **🔸 Especificación del Contrato**

**Propósito:** Mapeo detallado entre campos internos y campos de Azure DevOps.  
**Consumidor:** `OrganizationConfigService.getFieldMapping()`  
**Validación:** Todos los campos básicos deben estar presentes.

### **📋 Estructura Obligatoria (Schema)**

```yaml
# =============================================================================
# MAPEO DE CAMPOS (REQUERIDO)
# =============================================================================
fieldMappings:                               # REQUERIDO: Raíz de mapeos
  
  # -------------------------------------------------------------------------
  # CAMPOS BÁSICOS OBLIGATORIOS
  # -------------------------------------------------------------------------
  title:                                     # OBLIGATORIO
    azureFieldName: "System.Title"           # REQUERIDO: Campo Azure DevOps
    required: true                           # REQUERIDO: boolean
    type: "string"                           # REQUERIDO: Tipo de dato
    helpText: string                         # OPCIONAL: Texto de ayuda
  
  description:                               # OBLIGATORIO
    azureFieldName: "System.Description"     # REQUERIDO
    required: false                          # REQUERIDO
    type: "html"                             # REQUERIDO
    helpText: string                         # OPCIONAL
  
  state:                                     # OBLIGATORIO
    azureFieldName: "System.State"           # REQUERIDO
    required: true                           # REQUERIDO
    type: "string"                           # REQUERIDO
    defaultValue: "New"                      # OPCIONAL
    allowedValues: array[string]             # OPCIONAL: Estados válidos
  
  assignedTo:                                # OBLIGATORIO
    azureFieldName: "System.AssignedTo"      # REQUERIDO
    required: false                          # REQUERIDO
    type: "identity"                         # REQUERIDO
    helpText: string                         # OPCIONAL
  
  # -------------------------------------------------------------------------
  # CAMPOS PERSONALIZADOS (OPCIONAL)
  # -------------------------------------------------------------------------
  # Cualquier campo adicional siguiendo el mismo patrón

# =============================================================================
# VALIDACIONES DE CAMPOS (OPCIONAL)
# =============================================================================
validations:                                # OPCIONAL: Reglas de validación
  nombreCampo:                              # Nombre del campo a validar
    rule: enum                              # REQUERIDO: Ver reglas válidas
    condition: string                       # CONDICIONAL: Según la regla
    values: array                           # CONDICIONAL: Para regla in_list
    pattern: string                         # CONDICIONAL: Para regla matches_pattern
    message: string                         # REQUERIDO: Mensaje de error
```

### **🔧 Reglas de Validación Válidas**

| Regla | Descripción | Campos Requeridos |
|-------|-------------|-------------------|
| `required` | Campo obligatorio | `message` |
| `not_empty` | No puede estar vacío | `message` |
| `in_list` | Debe estar en lista | `values`, `message` |
| `matches_pattern` | Debe coincidir con patrón | `pattern`, `message` |
| `required_if_type` | Obligatorio si tipo específico | `condition`, `message` |

---

## 📄 **ARCHIVO 4: business-rules.yml**

### **🔸 Especificación del Contrato**

**Propósito:** Reglas de negocio, flujos de trabajo y automatización.  
**Consumidor:** Sistema de validación y automatización.  
**Validación:** Estructura válida para motor de reglas.

### **📋 Estructura Obligatoria (Schema)**

```yaml
# =============================================================================
# REGLAS DE NEGOCIO (REQUERIDO)
# =============================================================================
businessRules:                              # REQUERIDO: Raíz de reglas
  
  # -------------------------------------------------------------------------
  # VALIDACIONES POR TIPO (OPCIONAL)
  # -------------------------------------------------------------------------
  validation:                               # OPCIONAL: Reglas de validación
    tipoWorkItem:                           # Nombre del tipo (camelCase)
      - rule: string                        # REQUERIDO: Descripción de la regla
        field: string                       # REQUERIDO: Campo a validar
        condition: enum                     # REQUERIDO: Ver condiciones válidas
        values: array                       # CONDICIONAL: Para in_list
        pattern: string                     # CONDICIONAL: Para matches_pattern
        message: string                     # OPCIONAL: Mensaje personalizado
  
  # -------------------------------------------------------------------------
  # FLUJOS DE TRABAJO (OPCIONAL)
  # -------------------------------------------------------------------------
  workflow:                                 # OPCIONAL: Definición de workflows
    tipoWorkItem:                           # Nombre del tipo (camelCase)
      states: array[string]                 # REQUERIDO: Estados válidos
      transitions:                          # REQUERIDO: Transiciones válidas
        estadoOrigen: array[string]         # Estados destino permitidos
  
  # -------------------------------------------------------------------------
  # AUTOMATIZACIÓN (OPCIONAL)
  # -------------------------------------------------------------------------
  automation:                               # OPCIONAL: Reglas de automatización
    nombreAutomatizacion:                   # Nombre de la automatización
      enabled: boolean                      # REQUERIDO: true/false
      rules:                                # REQUERIDO: Array de reglas
        - condition: string                 # REQUERIDO: Condición de disparo
          action: enum                      # REQUERIDO: Ver acciones válidas
          target: string                    # CONDICIONAL: Según la acción
```

### **🔧 Valores Enum para Automatización**

**condition** ejemplos válidos:
- `"priority == 'High'"`, `"state == 'Resolved'"`, `"assignedTo == null"`

**action** válidos:
- `"assign_to_lead"`, `"transition_to_resolved"`, `"add_tag"`, `"send_notification"`

---

## ✅ **CHECKLIST DE VALIDACIÓN**

### **📋 Validación Pre-Generación**

Antes de generar archivos, verificar:

- [ ] **Conexión Azure DevOps** funcional
- [ ] **Permisos adecuados** para leer metadatos
- [ ] **Proyectos accesibles** por el usuario
- [ ] **Herramientas MCP** disponibles y funcionales

### **📋 Validación Post-Generación**

Después de generar archivos, verificar:

- [ ] **Sintaxis YAML válida** en todos los archivos
- [ ] **Estructura obligatoria** presente según contrato
- [ ] **Campos de fecha críticos** incluidos en todos los tipos
- [ ] **Referencias de campos** en formato correcto
- [ ] **Tipos de datos** válidos según especificación
- [ ] **IDs en formato correcto** (kebab-case para proyectos)
- [ ] **Valores boolean** como true/false (no strings)
- [ ] **allowedValues** solo en campos picklist

### **📋 Validación de Compatibilidad**

Verificar que los archivos pueden ser procesados por:

- [ ] **OrganizationContextService.loadConfiguration()** sin errores
- [ ] **OrganizationConfigService.getFieldMapping()** retorna datos
- [ ] **OrganizationConfigService.getDefaultOrganizationConfig()** funcional
- [ ] **Parsing YAML** exitoso con ObjectMapper
- [ ] **Campos referenciales** existen en Azure DevOps

---

## 🚨 **ERRORES COMUNES Y SOLUCIONES**

### **❌ Error: "Cannot parse YAML"**
**Causa:** Sintaxis YAML inválida  
**Solución:** Validar con yamllint antes de guardar

### **❌ Error: "Missing required field: discoveryDate"**
**Causa:** Cabecera obligatoria faltante  
**Solución:** Incluir todos los campos obligatorios del contrato

### **❌ Error: "Invalid field reference"**
**Causa:** Referencia de campo en formato incorrecto  
**Solución:** Usar formato exacto: "System.Campo" o "Custom.Campo"

### **❌ Error: "Date fields missing"**
**Causa:** Campos de fecha críticos ausentes  
**Solución:** Incluir StartDate, FinishDate, TargetDate, DueDate

### **❌ Error: "Boolean expected, string found"**
**Causa:** Valor boolean como string ("true" en lugar de true)  
**Solución:** Usar valores boolean nativos de YAML

---

## 📞 **SOPORTE Y CONTACTO**

**Versión del Contrato:** 1.0  
**Última Actualización:** 2025-07-30  
**Responsable:** Equipo de Desarrollo MCP Server  
**Documento de Referencia:** [PROMPT-GENERAR-CONFIGURACION.md](PROMPT-GENERAR-CONFIGURACION.md)

---

> **⚠️ IMPORTANTE**: El cumplimiento de este contrato es **OBLIGATORIO** para garantizar la compatibilidad con la aplicación. Archivos que no cumplan estas especificaciones pueden causar errores de runtime o funcionalidad limitada.
