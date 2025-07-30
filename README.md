# 🚀 Azure DevOps MCP Server

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-green.svg)](https://spring.io/projects/spring-boot)
[![MCP Protocol](https://img.shields.io/badge/MCP-2025--06--18-blue.svg)](https://spec.modelcontextprotocol.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Servidor **Model Context Protocol (MCP)** especializado para **Azure DevOps**, implementado con Spring Boot 3.3.2 y Java 21. Proporciona acceso completo a work items, proyectos, equipos e iteraciones a través de una interfaz MCP estandardizada, optimizada para integración con VS Code y otros clientes MCP.

## ✨ Características Principales

- 🌐 **Protocolo MCP 2025-06-18**: Implementación completa del estándar Model Context Protocol
- 🔄 **Comunicación STDIO**: Integración directa con VS Code y otros clientes MCP
- 🌍 **API REST**: Endpoints HTTP alternativos para testing y desarrollo
- 🔗 **Integración VS Code**: Configuración automática incluida en `.vscode/settings.json`
- ⚡ **Alto Rendimiento**: Cliente HTTP asíncrono con Spring WebFlux
- 🛡️ **Manejo Robusto de Errores**: Logging detallado y recuperación automática
- 🎯 **Campos Personalizados**: Soporte completo para campos custom de Azure DevOps
- 📊 **Consultas WIQL**: Ejecución de consultas avanzadas Work Item Query Language

## 🛠️ Herramientas MCP Disponibles

### 📁 Gestión de Proyectos y Equipos
- `azuredevops_list_projects` - Lista todos los proyectos de la organización
- `azuredevops_list_teams` - Lista equipos de un proyecto específico
- `azuredevops_list_iterations` - Lista iteraciones/sprints con análisis de cadencia

### 📝 Work Items
- `azuredevops_create_workitem` - Crea work items (Tasks, User Stories, Bugs, Features)
- `azuredevops_get_workitem` - Obtiene detalles completos de un work item
- `azuredevops_update_workitem` - Actualiza campos de work items existentes
- `azuredevops_delete_workitem` - Elimina work items (con papelera de reciclaje)
- `azuredevops_get_workitem_types` - Lista tipos de work items disponibles

### 🔍 Consultas y Análisis
- `azuredevops_query_workitems` - Ejecuta consultas WIQL personalizadas
- `azuredevops_get_assigned_work` - Obtiene trabajo asignado al usuario actual
- `azuredevops_get_assigned_work_test` - Versión simplificada para testing

### 🆔 Utilidades
- `generate_uuid` - Genera UUIDs únicos para referencias
- `get_help` - Documentación completa y ejemplos de uso

## 🚀 Configuración Rápida

### 1. Prerrequisitos

- **Java 21+** (OpenJDK recomendado)
- **Azure DevOps** con acceso a una organización
- **Personal Access Token (PAT)** con permisos de work items

### 2. Variables de Entorno

```bash
# Requeridas
export AZURE_DEVOPS_ORGANIZATION="tu-organizacion"
export AZURE_DEVOPS_PAT="tu-personal-access-token"

# Opcionales
export LOG_LEVEL="INFO"
export SERVER_PORT="8080"
```

### 3. Compilación y Ejecución

```bash
# Clonar el repositorio
git clone https://github.com/sjseo298/YOUR_ORGANIZATIONAzureDevopsCompanionMCP.git
cd YOUR_ORGANIZATIONAzureDevopsCompanionMCP

# Compilar el proyecto
./gradlew clean build

# Ejecutar en modo STDIO (para VS Code)
./gradlew bootRun --args="--mcp.stdio=true"

# Ejecutar en modo HTTP (para desarrollo/testing)
./gradlew bootRun
```

## 🔧 Integración con VS Code

El proyecto incluye configuración automática para VS Code en `.vscode/settings.json`:

```json
{
    "mcp": {
        "servers": {
            "spring-mcp-server": {
                "command": "java",
                "args": [
                    "-jar",
                    "/workspaces/mcpazuredevops/target/mcp-server-1.0.0-SNAPSHOT.jar"
                ],
                "env": {
                    "SERVER_PORT": "8080",
                    "JAVA_OPTS": "-Xmx512m"
                }
            }
        }
    }
}
```

### Uso en VS Code

1. Instalar la extensión **GitHub Copilot Chat**
2. Configurar las variables de entorno
3. El servidor se conectará automáticamente
4. Usar comandos como:
   ```
   @spring-mcp-server List my assigned work items
   @spring-mcp-server Create a new user story for login feature
   @spring-mcp-server Show teams in Project Alpha
   ```

## 📊 Casos de Uso Principales

### 🗓️ Planificación Diaria
```javascript
// Consultar trabajo asignado
await mcp.call("azuredevops_get_assigned_work", {
    project: "MiProyecto",
    includeCompleted: false
});

// Ver estado de iteración actual
await mcp.call("azuredevops_list_iterations", {
    project: "MiProyecto",
    team: "Equipo-Alpha",
    timeFrame: "current"
});
```

### 📝 Gestión de Work Items
```javascript
// Crear una nueva user story
await mcp.call("azuredevops_create_workitem", {
    project: "MiProyecto",
    type: "User Story",
    title: "Como usuario quiero hacer login",
    description: "Implementar autenticación básica",
    assignedTo: "usuario@empresa.com",
    storyPoints: 5
});

// Crear tarea hijo
await mcp.call("azuredevops_create_workitem", {
    project: "MiProyecto",
    type: "Task",
    title: "Implementar API de autenticación",
    parentId: 1234,
    remainingWork: 8
});
```

### 🔍 Consultas Avanzadas
```javascript
// Buscar bugs críticos
await mcp.call("azuredevops_query_workitems", {
    project: "MiProyecto",
    query: `SELECT [System.Id], [System.Title], [System.AssignedTo] 
            FROM WorkItems 
            WHERE [System.WorkItemType] = 'Bug' 
            AND [Microsoft.VSTS.Common.Severity] = 'Critical'
            AND [System.State] <> 'Closed'`
});
```

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────┐    ┌─────────────────────────┐
│   Cliente MCP           │───▶│   Servidor MCP          │
│   (VS Code, etc.)       │    │   (Spring Boot)         │
└─────────────────────────┘    └─────────────────────────┘
             │                              │
             ▼                              ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│   Protocol Handler      │    │   Azure DevOps Client   │
│   (JSON-RPC over STDIO) │    │   (REST API WebClient)  │
└─────────────────────────┘    └─────────────────────────┘
             │                              │
             ▼                              ▼
┌─────────────────────────┐    ┌─────────────────────────┐
│   MCP Tools Registry    │    │   Azure DevOps API      │
│   (Herramientas Auto-   │    │   (v7.1)                │
│    registradas)         │    │                         │
└─────────────────────────┘    └─────────────────────────┘
```

### Stack Tecnológico

- **Java 21 LTS**: Features modernas (records, pattern matching, virtual threads)
- **Spring Boot 3.3.2**: Auto-configuración y gestión de dependencias
- **Spring WebFlux**: Cliente HTTP reactivo de alto rendimiento
- **Jackson**: Serialización JSON optimizada
- **SLF4J + Logback**: Logging estructurado y configurable
- **JUnit 5**: Testing moderno con Spring Boot Test

## 🤖 Prompts MCP Disponibles

El servidor incluye prompts inteligentes para asistir en tareas comunes:

### 🔧 `generar_configuracion_organizacional`
**Genera automáticamente la configuración organizacional completa**

```javascript
// Generación automática en nueva instalación
generar_configuracion_organizacional({})

// Regeneración con parámetros específicos
generar_configuracion_organizacional({
    "forzar_regeneracion": true,
    "proyecto_principal": "Mi_Proyecto",
    "incluir_campos_extendidos": true,
    "generar_backup": true
})
```

**Qué hace:**
- ✅ Detecta archivos de configuración faltantes
- ✅ Ejecuta descubrimiento automático de Azure DevOps
- ✅ Genera `discovered-organization.yml`, `organization-config.yml`, `field-mappings.yml`
- ✅ Incluye campos personalizados, tipos de work items y reglas de negocio
- ✅ Valida sintaxis YAML y optimiza configuración

### 🔍 `buscar_workitem`
**Busca work items por ID, título o criterios específicos**

```javascript
buscar_workitem({
    "criterio_busqueda": "login authentication",
    "tipo_workitem": "User Story",
    "proyecto": "MiProyecto",
    "incluir_detalles": true
})
```

### 📊 `consulta_proyectos_pertenencia`
**Consulta proyectos y estructura organizacional**

```javascript
consulta_proyectos_pertenencia({
    "incluir_equipos": true,
    "incluir_estadisticas": true
})
```

> 📖 **Documentación completa**: Ver [docs/PROMPT-GENERAR-CONFIGURACION.md](docs/PROMPT-GENERAR-CONFIGURACION.md) y [docs/EJEMPLO-USO-PROMPT.md](docs/EJEMPLO-USO-PROMPT.md)

## 📋 Campos Personalizados Soportados

El servidor maneja automáticamente los campos personalizados específicos:

### User Stories / Historias
- `tipoHistoria` - Tipo de historia de usuario
- `acceptanceCriteria` - Criterios de aceptación
- `migracionDatos` - Indica si involucra migración de datos
- `cumplimientoRegulatorio` - Cumplimiento regulatorio requerido
- `controlAutomatico` - Control automático habilitado

### Technical Stories / Historias Técnicas
- `tipoHistoriaTecnica` - Tipo de historia técnica
- Campos de historia estándar aplicables

### Tasks / Tareas
- `tipoTarea` - Categoría de la tarea
- `remainingWork` - Trabajo restante en horas

### Bugs
- `reproSteps` - Pasos para reproducir
- `datosPrueba` - Datos de prueba utilizados
- `bloqueante` - Si es un bug bloqueante
- `nivelPrueba` - Nivel de pruebas donde se encontró
- `origen` - Origen del defecto
- `etapaDescubrimiento` - Etapa donde se descubrió

### Campos Comunes
- `idSolucionAPM` - ID de solución en el APM
- `storyPoints` - Puntos de historia estimados
- `priority` - Prioridad (1-4)
- `tags` - Etiquetas separadas por punto y coma

## 🧪 Testing y Desarrollo

### Ejecutar Tests
```bash
# Tests unitarios
./gradlew test

# Tests de integración
./gradlew integrationTest

# Coverage report
./gradlew jacocoTestReport
```

### Modo Desarrollo
```bash
# Ejecutar con hot reload
./gradlew bootRun --args="--spring.profiles.active=development"

# Habilitar debugging
./gradlew bootRun --debug-jvm
```

### Testing de APIs
```bash
# Modo HTTP para testing manual
./gradlew bootRun --args="--spring.main.web-application-type=servlet"

# Endpoints disponibles:
# GET  /actuator/health
# GET  /actuator/info
# POST /api/mcp/tools/{toolName}
```

## 📚 Configuración Avanzada

### Variables de Entorno Completas

```bash
# Azure DevOps (Requeridas)
AZURE_DEVOPS_ORGANIZATION=mi-organizacion
AZURE_DEVOPS_PAT=mi-token-personal

# Servidor HTTP (Opcional)
SERVER_PORT=8080
JAVA_OPTS=-Xmx512m

# Logging (Opcional)
LOG_LEVEL=INFO              # ERROR, WARN, INFO, DEBUG
ROOT_LOG_LEVEL=WARN

# MCP (Opcional)
MCP_STDIO=true              # true para VS Code, false para desarrollo
MCP_TIMEOUT=30s

# Performance (Opcional)
ENABLE_METRICS=false        # Habilitar métricas Prometheus
```

### Configuración de Logging

```yaml
# application.yml
logging:
  level:
    com.mcp.server: INFO
    org.springframework: WARN
    reactor.netty: ERROR
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## 🚨 Troubleshooting

### Problemas Comunes

#### 1. Error de Autenticación
```
Error: 401 Unauthorized
```
**Solución**: Verificar PAT y permisos
```bash
# Verificar PAT con curl
curl -u :$AZURE_DEVOPS_PAT https://dev.azure.com/{org}/_apis/projects?api-version=7.1
```

#### 2. Organización No Encontrada
```
Error: Organization not found
```
**Solución**: Verificar nombre de organización
```bash
# La URL debe ser: https://dev.azure.com/{ORGANIZATION}
```

#### 3. Problemas de Conexión
```
Error: Connection timeout
```
**Solución**: Verificar proxy/firewall corporativo

#### 4. Charset/Encoding Issues
```
Error: Malformed JSON
```
**Solución**: El servidor configura automáticamente UTF-8

### Habilitar Debug
```bash
./gradlew bootRun --args="--logging.level.com.mcp.server=DEBUG"
```

## 🤝 Contribuir

### Agregar Nueva Herramienta MCP

1. **Crear la clase herramienta**:
```java
@Component
public class MiNuevaHerramienta implements McpTool {
    @Override
    public String getName() {
        return "azuredevops_mi_nueva_herramienta";
    }
    
    @Override
    public String getDescription() {
        return "Descripción de mi herramienta";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "param1", Map.of("type", "string", "description", "Descripción")
            )
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        // Implementación
        return Map.of("content", List.of(Map.of("type", "text", "text", "Resultado")));
    }
}
```

2. **La herramienta se registra automáticamente** por Spring Boot auto-discovery

3. **Agregar tests**:
```java
@SpringBootTest
class MiNuevaHerramientaTest {
    @Autowired
    private MiNuevaHerramienta herramienta;
    
    @Test
    void testExecute() {
        var result = herramienta.execute(Map.of("param1", "valor"));
        assertThat(result).isNotNull();
    }
}
```

### Guidelines de Contribución

1. **Fork** el repositorio
2. **Crear branch** para la feature: `git checkout -b feature/nueva-herramienta`
3. **Commits descriptivos** siguiendo [Conventional Commits](https://conventionalcommits.org/)
4. **Tests** para toda nueva funcionalidad
5. **Documentación** actualizada
6. **Pull Request** con descripción detallada

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 🔗 Enlaces Útiles

- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [Azure DevOps REST API](https://docs.microsoft.com/en-us/rest/api/azure/devops/)
- [WIQL Syntax Reference](https://docs.microsoft.com/en-us/azure/devops/boards/queries/wiql-syntax)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [GitHub Copilot Chat Extension](https://marketplace.visualstudio.com/items?itemName=GitHub.copilot-chat)

## 🏢 Estructura Organizacional

Este servidor está optimizado para trabajar con la estructura específica de work items, equipos y nomenclatura organizacional, pero puede adaptarse a cualquier implementación de Azure DevOps mediante configuración de campos personalizados.

---

<div align="center">

**Desarrollado con ❤️ para mejorar la productividad en Azure DevOps**

[⭐ Star en GitHub](https://github.com/sjseo298/YOUR_ORGANIZATIONAzureDevopsCompanionMCP) • [🐛 Reportar Bug](https://github.com/sjseo298/YOUR_ORGANIZATIONAzureDevopsCompanionMCP/issues) • [💡 Request Feature](https://github.com/sjseo298/YOUR_ORGANIZATIONAzureDevopsCompanionMCP/issues)

</div>
