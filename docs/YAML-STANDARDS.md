# 📋 Estándares de Configuración YAML - Actualizado Julio 2025

## 🚨 Actualización Crítica

**Durante el desarrollo se descubrió información crítica sobre campos faltantes que ha sido incorporada en estos estándares.**

**Problema Identificado:** Campos de fecha esenciales no se incluían en consultas WIQL automáticas, causando que Features aparecieran sin fechas estimadas de finalización.

## 📐 Estándares de Formato

### **1. Indentación**
- **USAR:** 2 espacios para indentación
- **NO USAR:** Tabs o 4 espacios

```yaml
# ✅ CORRECTO
parent:
  child:
    grandchild: value
```

### **2. Encabezados de Archivo**
Todos los archivos YAML deben incluir un encabezado con fecha de actualización:

```yaml
# ====================================================================
# [TÍTULO DEL ARCHIVO]
# Actualizado: 2025-07-30 - Campos de fecha agregados
# ====================================================================
```
# Descripción: [Propósito del archivo]
# Autor: [Equipo/Persona responsable]
# Última actualización: [Fecha]
# ====================================================================
```

### **3. Comentarios**
- Usar `#` para comentarios
- Comentarios en líneas separadas antes del elemento que describen
- Comentarios en español para consistencia con el contexto de YOUR_ORGANIZATION

```yaml
# Configuración de Azure DevOps
azure:
  # Organización principal de YOUR_ORGANIZATION
  organization: "sura"
  # URL base para APIs
  baseUrl: "https://dev.azure.com/sura"
```

### **4. Cadenas de Texto**
- **USAR:** Comillas dobles para todos los valores string
- **EVITAR:** Comillas simples o valores sin comillas

```yaml
# ✅ CORRECTO
name: "YOUR_ORGANIZATION Colombia"
description: "Organización líder en seguros"

# ❌ INCORRECTO
name: YOUR_ORGANIZATION Colombia
description: 'Organización líder en seguros'
```

## 📁 Estructura de Archivos por Tipo

### **1. Archivos de Configuración Organizacional**
```yaml
# organization-config.yml
organization:
  name: "Nombre de la Organización"
  displayName: "Nombre para Mostrar"
  description: "Descripción detallada"
  
  azure:
    organization: "nombre-org"
    baseUrl: "https://dev.azure.com/nombre-org"
    defaultProject: "Proyecto-Principal"
  
  projects:
    - id: "proyecto-id"
      name: "Nombre_Proyecto"
      displayName: "Nombre del Proyecto"
      description: "Descripción del proyecto"
      isDefault: true
```

### **2. Archivos de Mapeo de Campos**
```yaml
# field-mapping.yml
fieldMappings:
  # Campos básicos
  fieldName:
    azureFieldName: "System.FieldName"
    required: true|false
    type: "string|html|integer|boolean|identity"
    defaultValue: "valor"
    allowedValues: ["valor1", "valor2"]
    helpText: "Texto de ayuda descriptivo"
```

### **3. Archivos de Configuración de Aplicación**
```yaml
# application.yml
# Configuración de Spring Boot
spring:
  application:
    name: "nombre-aplicacion"
  main:
    web-application-type: "none"
    
# Configuración específica del dominio
dominio:
  configuracion:
    propiedad: "valor"
```

## 🔧 Validaciones Obligatorias

### **1. Sintaxis YAML**
- Todos los archivos deben ser YAML válido
- Usar herramientas de validación: `yamllint`

### **2. Estructura Consistente**
- Los archivos del mismo tipo deben seguir la misma estructura
- Los archivos de test deben replicar la estructura de producción

### **3. Nomenclatura**
- **Archivos:** kebab-case (`organization-config.yml`)
- **Propiedades:** camelCase (`defaultProject`)
- **IDs:** kebab-case (`gerencia-tecnologia`)

## 📋 Lista de Verificación

Antes de confirmar cambios en archivos YAML, verificar:

- [ ] Indentación de 2 espacios consistente
- [ ] Encabezado descriptivo incluido
- [ ] Todas las cadenas entre comillas dobles
- [ ] Comentarios en español cuando corresponda
- [ ] Estructura consistente con archivos similares
- [ ] Sintaxis YAML válida
- [ ] Nomenclatura siguiendo convenciones

## 🛠️ Herramientas Recomendadas

### **Validación:**
```bash
# Instalar yamllint
pip install yamllint

# Validar archivo
yamllint config/organization-config.yml

# Validar con configuración personalizada
yamllint --config-data '{extends: default, rules: {line-length: {max: 120}, indentation: {spaces: 2}}}' archivo.yml
```

### **Formateo:**
```bash
# Usar prettier para formateo automático
npx prettier --write "**/*.yml"
```

## � Campos de Fecha Críticos - Descubrimiento de Julio 2025

### **Problema Identificado**
Durante el desarrollo se descubrió que varios campos de fecha esenciales no se incluían automáticamente en las consultas WIQL, causando:

- ❌ Features sin fechas estimadas de finalización
- ❌ Información de planificación incompleta
- ❌ Seguimiento deficiente de progreso
- ❌ Priorización afectada

### **Campos Identificados como Faltantes**

| Campo | Impacto | Problema |
|-------|---------|----------|
| `Microsoft.VSTS.Scheduling.TargetDate` | **CRÍTICO** | Features sin fechas estimadas |
| `Microsoft.VSTS.Scheduling.StartDate` | ALTO | Planificación incompleta |
| `Microsoft.VSTS.Scheduling.FinishDate` | ALTO | Seguimiento deficiente |
| `Microsoft.VSTS.Scheduling.DueDate` | MEDIO | Priorización afectada |

### **Solución Implementada**

**1. Configuración en `discovered-organization.yml`:**
```yaml
dateFieldsAnalysis:
  missingFieldsIdentified:
    - field: "Microsoft.VSTS.Scheduling.TargetDate"
      impact: "CRITICAL"
      description: "Fecha objetivo no se incluía, causando Features sin fechas estimadas"

wiqlContextConfiguration:
  defaultFields:
    withDates:
      - "Microsoft.VSTS.Scheduling.StartDate"
      - "Microsoft.VSTS.Scheduling.FinishDate"
      - "Microsoft.VSTS.Scheduling.TargetDate"
      - "Microsoft.VSTS.Scheduling.DueDate"
```

**2. Configuración en `sura-field-mapping.yml`:**
```yaml
missingDateFieldsDiscovered:
  discoveryDate: "2025-07-30"
  impact: "CRITICAL"
  identifiedMissingFields:
    - referenceName: "Microsoft.VSTS.Scheduling.TargetDate"
      impact: "CRITICAL"
      issue: "Features sin fechas estimadas de finalización"
      solution: "Agregado al enriquecimiento automático de consultas"
```

## 🎯 Aplicación Genérica con Contexto Organizacional

### **Principios de Diseño Actualizados**

1. **Código Genérico:** No hardcoding organizacional en Java
2. **Configuración Dinámica:** Todo en archivos YAML  
3. **Enriquecimiento Automático:** Consultas contextuales con campos de fecha
4. **Fallback Resiliente:** Si falla enriquecido, usar original

### **Implementación Técnica**

```java
// OrganizationContextService.java
@Service
public class OrganizationContextService {
    
    public List<String> getSchedulingDateFields() {
        // CRÍTICO: Incluir campos de fecha identificados como faltantes
        return Arrays.asList(
            "Microsoft.VSTS.Scheduling.StartDate",
            "Microsoft.VSTS.Scheduling.FinishDate", 
            "Microsoft.VSTS.Scheduling.TargetDate",  // CRÍTICO para Features
            "Microsoft.VSTS.Scheduling.DueDate"
        );
    }
}
```

## 📝 Validación Actualizada

### **Checklist de Configuración (ACTUALIZADO)**

- ✅ Campos de fecha incluidos en `wiqlContextConfiguration`
- ✅ `dateFieldsAnalysis` documentado con campos faltantes
- ✅ `missingDateFieldsDiscovered` completado
- ✅ Enriquecimiento contextual configurado
- ✅ Fallback strategy definida
- ✅ **NUEVO:** Campos críticos validados en producción

### **Testing de Campos de Fecha**

```java
@Test
public void testCriticalDateFieldsIncluded() {
    List<String> fields = contextService.getSchedulingDateFields();
    
    // CRÍTICO: TargetDate debe estar presente
    assertTrue("TargetDate es crítico para Features", 
               fields.contains("Microsoft.VSTS.Scheduling.TargetDate"));
    
    // Otros campos importantes
    assertTrue(fields.contains("Microsoft.VSTS.Scheduling.StartDate"));
    assertTrue(fields.contains("Microsoft.VSTS.Scheduling.FinishDate"));
}
```

## �📚 Referencias

- [YAML Specification](https://yaml.org/spec/)
- [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Azure DevOps REST API](https://docs.microsoft.com/en-us/rest/api/azure/devops/)
- **NUEVO:** [Contextual Query Enrichment Documentation](./CONTEXTUAL-QUERY-ENRICHMENT.md)

---
**Nota Crítica:** Esta actualización incluye campos de fecha esenciales identificados como faltantes. Debe implementarse para asegurar información completa de planificación.
