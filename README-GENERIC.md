# Azure DevOps MCP Server - Sistema Genérico

## 🎯 Visión General

Este servidor MCP (Model Context Protocol) para Azure DevOps ha sido transformado de una implementación específica para Sura a un **sistema genérico y configurable** que puede adaptarse a cualquier organización.

## 🏗️ Arquitectura Genérica

### Componentes Principales

#### 1. **OrganizationConfigService**
- **Propósito**: Gestión centralizada de configuración organizacional
- **Ubicación**: `src/main/java/com/mcp/server/config/OrganizationConfigService.java`
- **Características**:
  - Mapeo de campos configurable
  - Valores por defecto organizacionales
  - Conversión automática de tipos
  - Soporte para múltiples tipos de work items

#### 2. **GenericWorkItemFieldsHandler**
- **Propósito**: Procesamiento genérico de campos de work items
- **Ubicación**: `src/main/java/com/mcp/server/service/workitem/GenericWorkItemFieldsHandler.java`
- **Características**:
  - Validación configurable de campos requeridos
  - Transformación automática de valores
  - Compatibilidad retroactiva con sistemas existentes
  - Procesamiento específico por tipo de work item

#### 3. **DiscoverOrganizationTool**
- **Propósito**: Herramienta de descubrimiento automático de configuración
- **Ubicación**: `src/main/java/com/mcp/server/tools/azuredevops/DiscoverOrganizationTool.java`
- **Características**:
  - Análisis de proyectos disponibles
  - Detección de tipos de work items
  - Generación automática de configuración YAML
  - Recomendaciones de mejores prácticas

## 🔧 Configuración

### Variables de Entorno

```bash
# Configuración de Azure DevOps
AZUREDEVOPS_ORGANIZATION=tu-organizacion
AZUREDEVOPS_TOKEN=tu-personal-access-token

# Configuración de la aplicación
APP_CONFIG_PATH=config/
```

### Estructura de Configuración

```
config/
├── organization-config.yml      # Configuración principal
├── discovered-organization.yml  # Configuración descubierta automáticamente
└── sura-field-mapping.yml      # Ejemplo de mapeo específico
```

### Ejemplo de Configuración YAML

```yaml
organization:
  name: "mi-organizacion"
  defaultProject: "MiProyecto"
  defaultTeam: "MiEquipo"
  timeZone: "America/Bogota"
  language: "es-CO"

fieldMappings:
  title:
    azureFieldName: "System.Title"
    required: true
    type: "string"
  
  description:
    azureFieldName: "System.Description"
    required: false
    type: "html"
  
  customField:
    azureFieldName: "Custom.MiCampo"
    required: true
    type: "string"
    defaultValue: "Valor por defecto"
    helpText: "Descripción del campo personalizado"
```

## 🚀 Herramientas Disponibles

### 1. Crear Work Item
```bash
azuredevops_create_workitem
```
- Crea work items usando configuración organizacional
- Validación automática de campos requeridos
- Soporte para jerarquías padre-hijo

### 2. Descubrir Organización
```bash
azuredevops_discover_organization
```
- Analiza la configuración actual de Azure DevOps
- Genera configuración YAML automáticamente
- Proporciona recomendaciones de mejores prácticas

### 3. Herramientas Existentes
- `azuredevops_get_workitem`
- `azuredevops_update_workitem`
- `azuredevops_delete_workitem`
- `azuredevops_query_workitems`
- `azuredevops_list_projects`
- `azuredevops_list_teams`
- `azuredevops_list_iterations`
- `azuredevops_get_assigned_work`

## 💡 Migración de Sura a Sistema Genérico

### ✅ Cambios Implementados

1. **Configuración Dinámica**
   - Reemplazado hardcoding específico de Sura con configuración basada en HashMap
   - Implementado sistema de mapeo de campos configurable
   - Agregado soporte para organizaciones múltiples

2. **Manejo Genérico de Campos**
   - Creado `GenericWorkItemFieldsHandler` que reemplaza `SuraWorkItemFieldsHandler`
   - Implementada validación configurable de campos requeridos
   - Mantenida compatibilidad retroactiva con tipos de Sura

3. **Herramientas de Descubrimiento**
   - Nueva herramienta `DiscoverOrganizationTool` para análisis automático
   - Generación automática de configuración YAML
   - Recomendaciones específicas por organización

### 🔄 Compatibilidad Retroactiva

El sistema mantiene compatibilidad completa con:
- Tipos de work items específicos de Sura (Historia, Historia Técnica, Tarea)
- Campos personalizados existentes
- Flujos de trabajo actuales

## 🏃‍♂️ Inicio Rápido

### 1. Configuración Inicial
```bash
# Configurar variables de entorno
export AZUREDEVOPS_ORGANIZATION="mi-organizacion"
export AZUREDEVOPS_TOKEN="mi-token"

# Ejecutar el servidor
./gradlew bootRun
```

### 2. Descubrir Configuración
```bash
# Usar MCP client para descubrir configuración
azuredevops_discover_organization --project="MiProyecto" --generateConfig=true
```

### 3. Crear Work Item Genérico
```bash
azuredevops_create_workitem \
  --project="MiProyecto" \
  --type="Task" \
  --title="Mi tarea" \
  --description="Descripción de la tarea"
```

## 🧪 Testing

### Ejecutar Tests
```bash
./gradlew clean test
```

### Build Completo
```bash
./gradlew clean build
```

### Tests de Configuración
- `MockOrganizationConfigService`: Mock para testing
- Configuración de prueba en `src/test/resources/`
- Tests automatizados de validación de campos

## 📈 Extensibilidad

### Agregar Nueva Organización

1. **Crear configuración YAML**:
```yaml
# config/nueva-organizacion-config.yml
organization:
  name: "nueva-organizacion"
  # ... configuración específica
```

2. **Implementar mapeos específicos**:
```java
// En OrganizationConfigService
private Map<String, Object> createCustomMappings() {
    // Mapeos específicos para la nueva organización
}
```

3. **Usar herramienta de descubrimiento**:
```bash
azuredevops_discover_organization --generateConfig=true
```

### Agregar Nuevos Tipos de Work Item

```java
// En GenericWorkItemFieldsHandler
public List<String> getRequiredFieldsForWorkItemType(String workItemType) {
    switch (workItemType.toLowerCase()) {
        case "mi-tipo-personalizado":
            return List.of("campo1", "campo2", "campo3");
        // ...
    }
}
```

## 🔒 Seguridad

- Tokens de acceso personal encriptados
- Validación de entrada para todos los campos
- Logging de auditoría para operaciones críticas
- Manejo seguro de errores sin exposición de datos sensibles

## 📖 Documentación Adicional

- [Configuración Avanzada](docs/advanced-configuration.md)
- [Mejores Prácticas](docs/best-practices.md)
- [Guía de Troubleshooting](docs/troubleshooting.md)
- [API Reference](docs/api-reference.md)

## 🤝 Contribución

1. Fork del repositorio
2. Crear branch para nueva funcionalidad
3. Implementar cambios con tests
4. Enviar Pull Request con documentación actualizada

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## 📞 Soporte

Para soporte técnico o preguntas sobre implementación:
- Crear issue en GitHub
- Contactar al equipo de desarrollo
- Consultar documentación en `/docs`

---

**✨ ¡El sistema genérico está listo para ser usado por cualquier organización!**
