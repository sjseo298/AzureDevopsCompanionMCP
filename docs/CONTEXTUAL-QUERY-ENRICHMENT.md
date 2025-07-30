# 🔍 Enriquecimiento Contextual de Consultas WIQL

## Descripción General

El enriquecimiento contextual de consultas WIQL es una funcionalidad que permite que la aplicación se mantenga genérica mientras automáticamente incluye campos relevantes basándose en el contexto organizacional descubierto.

## Problema Identificado

Durante el desarrollo se descubrió que las consultas WIQL básicas no incluían campos críticos de fecha, causando:

- ❌ Features sin fechas estimadas de finalización
- ❌ Información incompleta de planificación
- ❌ Consultas que requerían especificar manualmente todos los campos
- ❌ Inconsistencia entre diferentes herramientas

## Solución Implementada

### 1. OrganizationContextService

```java
@Service
public class OrganizationContextService {
    // Carga configuración organizacional desde YAML
    // Proporciona campos contextuales para consultas WIQL
    // Mantiene compatibilidad con múltiples organizaciones
}
```

**Funcionalidades:**
- Carga dinámica de configuración organizacional
- Construcción de cláusulas SELECT enriquecidas
- Mapeo de campos específicos por tipo de work item
- Validación de campos existentes en la organización

### 2. Enriquecimiento Automático de Consultas

Las consultas WIQL simples se enriquecen automáticamente:

**Consulta Original:**
```sql
SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.WorkItemType] = 'Feature'
```

**Consulta Enriquecida:**
```sql
SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType], 
       [System.AssignedTo], [System.AreaPath], [System.IterationPath], 
       [System.CreatedDate], [System.ChangedDate], 
       [Microsoft.VSTS.Scheduling.StartDate], [Microsoft.VSTS.Scheduling.FinishDate],
       [Microsoft.VSTS.Scheduling.TargetDate], [Microsoft.VSTS.Scheduling.DueDate]
FROM WorkItems WHERE [System.WorkItemType] = 'Feature'
```

### 3. Estrategia de Fallback

Si la consulta enriquecida falla:
1. **Intenta con la consulta original**
2. **Si falla, intenta con consulta básica garantizada**
3. **Si todo falla, reporta el error**

## Configuración Organizacional

### Estructura de Archivos

```
config/
├── discovered-organization.yml    # Estructura organizacional completa
├── sura-field-mapping.yml        # Mapeo de campos personalizados
├── organization-config.yml       # Configuración personalizable
└── README.md                     # Documentación de configuración
```

### Campos de Fecha Críticos

Los siguientes campos se identificaron como faltantes y ahora se incluyen automáticamente:

| Campo | Descripción | Impacto |
|-------|-------------|---------|
| `Microsoft.VSTS.Scheduling.TargetDate` | Fecha objetivo | **CRÍTICO** - Features sin fechas estimadas |
| `Microsoft.VSTS.Scheduling.StartDate` | Fecha de inicio | ALTO - Planificación incompleta |
| `Microsoft.VSTS.Scheduling.FinishDate` | Fecha de finalización | ALTO - Seguimiento deficiente |
| `Microsoft.VSTS.Scheduling.DueDate` | Fecha de vencimiento | MEDIO - Priorización afectada |

## Uso en las Herramientas

### GetAssignedWorkTool
```java
// Construcción de consulta con contexto organizacional
String selectClause = organizationContextService.buildWiqlSelectClause(
    null,   // workItemType
    true,   // includeDates
    true,   // includeMetrics  
    false   // includeCustomFields
);
```

### QueryWorkItemsTool
```java
// Enriquecimiento automático de consultas simples
String enrichedQuery = enrichWiqlQuery(query);
WiqlQueryResult result = executeWiqlQueryWithFallback(project, team, enrichedQuery, query);
```

## Beneficios

### Para el Usuario
- ✅ **Información completa automáticamente** - No necesita especificar todos los campos
- ✅ **Fechas de planificación siempre disponibles** - Crítico para seguimiento de Features
- ✅ **Consultas consistentes** - Mismo nivel de detalle en todas las herramientas
- ✅ **Compatibilidad con organizaciones diferentes** - Se adapta automáticamente

### Para el Desarrollo
- ✅ **Código genérico mantenido** - No hardcoding organizacional
- ✅ **Configuración dinámica** - Cambios sin recompilar
- ✅ **Extensibilidad** - Fácil agregar nuevas organizaciones
- ✅ **Mantenibilidad** - Lógica centralizada en configuración

## Configuración por Organización

### Para Sura Colombia
```yaml
wiqlContextConfiguration:
  defaultFields:
    basic: [campos estándar del sistema]
    withDates: [campos de fechas de planificación]
    withMetrics: [campos de métricas y estimación]
  
  organizationSpecificFields:
    features:
      - "Microsoft.VSTS.Scheduling.TargetDate"  # Crítico para Sura
    historias:
      - "Custom.TipoDeHistoria"
      - "Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14"  # ID Solución APM
```

### Para Otras Organizaciones
El mismo patrón se puede aplicar creando archivos `discovered-[organization].yml` con:
- Tipos de work items específicos
- Campos personalizados
- Reglas de negocio
- Patrones de nomenclatura

## Implementación Técnica

### 1. Carga de Configuración
```java
private void loadConfiguration() {
    File discoveredFile = new File(configPath + "discovered-organization.yml");
    if (discoveredFile.exists()) {
        discoveredConfig = yamlMapper.readValue(discoveredFile, Map.class);
    }
}
```

### 2. Construcción Contextual
```java
public List<String> buildContextualFieldList(String workItemType, 
                                           boolean includeDates, 
                                           boolean includeMetrics) {
    List<String> fields = getStandardSystemFields();
    if (includeDates) fields.addAll(getSchedulingDateFields());
    if (includeMetrics) fields.addAll(getSchedulingMetricFields());
    return fields.stream().distinct().collect(Collectors.toList());
}
```

### 3. Enriquecimiento de Consultas
```java
private String enrichWiqlQuery(String originalQuery) {
    Pattern simpleSelectPattern = Pattern.compile(
        "SELECT\\s+\\[System\\.Id\\].*FROM\\s+WorkItems", 
        Pattern.CASE_INSENSITIVE
    );
    
    if (simpleSelectPattern.matcher(originalQuery).find()) {
        return buildEnrichedQuery(originalQuery);
    }
    return originalQuery;
}
```

## Casos de Uso

### 1. Consulta de Features sin Fechas
**Problema:** `SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.WorkItemType] = 'Feature'`
**Resultado:** Features aparecían sin información de fechas de finalización

**Solución:** Enriquecimiento automático incluye campos de fecha
**Resultado:** Features con fechas objetivo, inicio, y finalización visibles

### 2. Análisis de Trabajo Asignado
**Problema:** Información limitada en trabajo asignado
**Solución:** Contexto organizacional incluye métricas y fechas automáticamente

### 3. Adaptación a Nueva Organización
**Problema:** Código hardcodeado para Sura
**Solución:** Configuración YAML específica por organización

## Monitoreo y Debugging

### Logs de Consultas
```
Consulta original: SELECT [System.Id], [System.Title] FROM WorkItems...
Consulta enriquecida: SELECT [System.Id], [System.Title], [System.State]...
```

### Métricas de Fallback
- Consultas enriquecidas exitosas
- Consultas que requirieron fallback
- Errores de campos no existentes

## Próximos Pasos

1. **Activar enriquecimiento en producción**
2. **Crear herramienta de descubrimiento automático real**
3. **Desarrollar validación de campos organizacionales**
4. **Implementar análisis de campos faltantes**
5. **Crear marketplace de configuraciones organizacionales**

## Referencias

- `src/main/java/com/mcp/server/config/OrganizationContextService.java` - Servicio principal
- `config/discovered-organization.yml` - Configuración de Sura
- `config/sura-field-mapping.yml` - Mapeo de campos personalizados
- `src/main/java/com/mcp/server/tools/azuredevops/QueryWorkItemsTool.java` - Implementación de enriquecimiento
