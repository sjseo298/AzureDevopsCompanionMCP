package com.mcp.server.tools.help;

import com.mcp.server.tools.base.McpTool;
import com.mcp.server.protocol.types.Tool;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Herramienta de ayuda del servidor MCP Azure DevOps.
 * 
 * <p>Proporciona información comprensiva sobre las capacidades del servidor,
 * incluyendo contexto específico para la estructura organizacional de Sura Colombia.
 * 
 * <p>Esta herramienta incluye:
 * <ul>
 *   <li>Descripción general del servidor y sus capacidades</li>
 *   <li>Lista completa de herramientas disponibles</li>
 *   <li>Contexto específico de Sura (jerarquía, dominios, nomenclatura)</li>
 *   <li>Ejemplos de uso para consultas básicas y avanzadas</li>
 *   <li>Mejores prácticas para el uso efectivo</li>
 * </ul>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class HelpTool implements McpTool {
    
    private static final String TOOL_NAME = "get_help";
    private static final String DESCRIPTION = "Obtiene información completa de ayuda sobre el servidor MCP Azure DevOps, incluyendo contexto específico para Sura Colombia";
    
    @Override
    public Tool getToolDefinition() {
        return Tool.builder()
                .name(TOOL_NAME)
                .description(DESCRIPTION)
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "section", Map.of(
                            "type", "string",
                            "description", "Sección específica de ayuda (opcional): 'overview', 'tools', 'sura_context', 'examples', 'best_practices'",
                            "enum", List.of("overview", "tools", "sura_context", "examples", "best_practices")
                        )
                    ),
                    "required", List.of(),
                    "additionalProperties", false
                ))
                .build();
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        String section = arguments != null ? (String) arguments.get("section") : null;
        
        StringBuilder helpText = new StringBuilder();
        
        if (section == null) {
            // Mostrar toda la ayuda
            helpText.append(getOverview()).append("\n\n")
                   .append(getToolsHelp()).append("\n\n")
                   .append(getSuraContext()).append("\n\n")
                   .append(getExamples()).append("\n\n")
                   .append(getBestPractices());
        } else {
            // Mostrar sección específica
            switch (section) {
                case "overview":
                    helpText.append(getOverview());
                    break;
                case "tools":
                    helpText.append(getToolsHelp());
                    break;
                case "sura_context":
                    helpText.append(getSuraContext());
                    break;
                case "examples":
                    helpText.append(getExamples());
                    break;
                case "best_practices":
                    helpText.append(getBestPractices());
                    break;
                default:
                    throw new IllegalArgumentException("Sección no válida: " + section);
            }
        }
        
        // Crear respuesta en formato MCP
        return Map.of(
            "content", List.of(
                Map.of(
                    "type", "text",
                    "text", helpText.toString()
                )
            ),
            "isError", false
        );
    }
    
    private String getOverview() {
        return """
# 🚀 Azure DevOps MCP Server - Guía Completa

## 📋 Descripción General
Servidor MCP (Model Context Protocol) especializado para Azure DevOps implementado con Spring Boot 3.3.2 y Java 21. 
Proporciona acceso completo a work items, proyectos, equipos e iteraciones a través de WebSocket y REST API.
Optimizado específicamente para la estructura organizacional y metodología de Sura Colombia.

## ✨ Características Principales
- 🌐 **Protocol MCP 2024-11-05**: Implementación completa del protocolo estándar
- 🔄 **WebSocket Communication**: Comunicación en tiempo real bidireccional
- 🌍 **REST API**: Endpoints HTTP para integración fácil y testing
- 🔗 **Visual Studio Code**: Configuración automática para VS Code
- 🤖 **GitHub Copilot**: Integración nativa con chat de Copilot
- ☕ **Java 21 LTS**: Características modernas del lenguaje y rendimiento optimizado
- 🍃 **Spring Boot 3.3.2**: Framework robusto con autoconfiguración

## ✅ Funcionalidades Azure DevOps
- 📋 **Gestión Completa de Work Items**: Crear, actualizar, consultar y analizar work items
- 🔍 **Query Avanzado**: Ejecutar consultas WIQL personalizadas con macros
- 📊 **Análisis de Proyectos**: Ver estructura organizacional y equipos
- 🔄 **Iteraciones y Sprints**: Gestión de ciclos de desarrollo ágil
- 🏗️ **Tipos de Work Items**: Soporte para historias, tareas, bugs, features y épicas
- 🎯 **Contexto Sura**: Configuración especializada para el entorno de Sura Colombia

## 🔧 Tecnologías y Stack
- **Java**: 21 LTS con características modernas
- **Spring Boot**: 3.3.2 con autoconfiguración
- **Azure DevOps API**: v7.1 con JSON Patch (RFC 6902)
- **WebSocket**: Comunicación en tiempo real
- **JSON-RPC**: Protocolo de comunicación MCP
- **Maven/Gradle**: Gestión de dependencias y build
- **JUnit 5**: Testing unitario y de integración

## 🌐 Protocolo y Configuración
- **Protocolo MCP**: 2024-11-05 (JSON-RPC 2.0)
- **Azure DevOps API**: v7.1 REST API
- **Autenticación**: Personal Access Token (PAT) con scope vso.work_write
- **Organización**: sura (Azure DevOps)
- **Content-Type**: application/json-patch+json (JSON Patch RFC 6902)
- **Formato**: Array de operaciones JSON Patch para create/update""";
    }
    
    private String getToolsHelp() {
        return """
# 🛠️ Herramientas Disponibles (12 herramientas)

## 📋 Gestión de Proyectos y Organizacional
- **list_projects**: Lista todos los proyectos disponibles en la organización
  - Retorna: ID, nombre, descripción, estado, visibilidad
  - Útil para: Explorar estructura organizacional
  
- **list_teams**: Lista equipos de un proyecto específico
  - Parámetros: project (nombre o ID del proyecto)
  - Retorna: ID, nombre, descripción del equipo
  - Útil para: Identificar equipos por proyecto

- **list_iterations**: Lista iteraciones/sprints de un equipo con análisis de cadencia
  - Parámetros: project, team, timeFrame (current/past/future)
  - Retorna: Fechas, estado, análisis de cadencia ágil
  - Útil para: Planificación y seguimiento de sprints

## 📝 Gestión de Work Items
- **get_workitem**: Obtiene detalles completos de un work item por ID
  - Parámetros: project, workItemId, fields (opcional), expand (opcional)
  - Retorna: Todos los campos del work item incluyendo custom fields
  - Útil para: Inspección detallada de work items

- **create_workitem**: Crea nuevos work items con JSON Patch API
  - Soporta: Historia, Historia técnica, Tarea, Subtarea, Bug, Épica, etc.
  - Campos: Todos los campos estándar y personalizados de Sura
  - Relaciones: Soporte completo para jerarquías padre-hijo
  - Validación: Campos obligatorios por tipo según configuración Sura
  
- **update_workitem**: Actualiza work items existentes
  - Operaciones: Cambio de estado, asignación, campos personalizados
  - Formato: JSON Patch operations (RFC 6902)
  - Control de concurrencia: Revisión opcional para evitar conflictos

- **delete_workitem**: Elimina work items de Azure DevOps
  - Modos: Papelera de reciclaje (por defecto) o eliminación permanente
  - Seguridad: Confirmación obligatoria para eliminación permanente
  - Parámetros: project, workItemId, destroy (opcional), confirmDestroy (requerido si destroy=true)
  - ⚠️ ADVERTENCIA: destroy=true es IRREVERSIBLE

- **get_assigned_work**: Obtiene work items asignados al usuario actual
  - Filtros: Por estado, tipo, iteración
  - Agrupación: Por state, type, iteration
  - Útil para: Planificación diaria y seguimiento personal

## 🔍 Consultas y Búsquedas Avanzadas
- **query_workitems**: Ejecuta consultas WIQL (Work Item Query Language)
  - Macros soportadas: @Me, @Today, @CurrentIteration, @Project
  - Operaciones: SELECT, WHERE, GROUP BY, ORDER BY
  - Límites: Hasta 50 resultados por defecto (configurable)
  - Incluye detalles completos de work items automáticamente

- **get_workitem_types**: Obtiene todos los tipos de work items de un proyecto
  - Información: Nombre, descripción, campos disponibles
  - Útil para: Validar tipos antes de crear work items

## 🛠️ Utilidades
- **generate_uuid**: Genera UUIDs únicos para identificadores
  - Formato: UUID estándar java.util.UUID
  - Útil para: Crear identificadores únicos en aplicaciones

- **get_help**: Muestra información de ayuda completa (esta herramienta)
  - Secciones: overview, tools, sura_context, examples, best_practices
  - Contexto: Documentación específica para Sura Colombia

## 📊 Características Especiales
- **JSON Patch Support**: Todas las operaciones de creación/actualización usan RFC 6902
- **Sura Custom Fields**: Soporte completo para campos personalizados de Sura
- **Work Item Types**: Tipos personalizados en español (Historia, Tarea, etc.)
- **Error Handling**: Manejo detallado de errores específicos de Azure DevOps
- **Validation**: Validación de campos obligatorios por tipo de work item
- **Safe Deletion**: Eliminación con papelera de reciclaje y confirmación para eliminación permanente""";
    }
    
    private String getSuraContext() {
        return """
# 🏢 Contexto Específico para Sura Colombia

## 🔧 Tipos de Work Items Personalizados (OBLIGATORIO USAR EN ESPAÑOL)

### ✅ Tipos Habilitados en Sura:
1. **Historia** - Funcionalidades de negocio
   - Campo clave: `Tipo de Historia` (Bug, Historia, Plan de pruebas, Plan migración de datos, Pruebas automatizadas)
   - Campos obligatorios: Title, State, Description, AcceptanceCriteria, TipoDeHistoria, MigracionDatos, CumplimientoRegulatorio, ControlAutomatico, ID_APM

2. **Historia técnica** - Trabajo técnico, infraestructura, bugs
   - Campo clave: `Tipo de Historia Técnica` (Bug, Historia Técnica, Plan de pruebas, etc.)
   - Campos obligatorios: Title, State, Description, AcceptanceCriteria, TipoDeHistoriaTecnica, MigracionDatos, CumplimientoRegulatorio, ControlAutomatico, ID_APM

3. **Tarea** - Tareas específicas de desarrollo
   - Campo clave: `Tipo de tarea` (Spike, Tarea)
   - Campos obligatorios: Title, State, TipoDeTarea

4. **Subtarea** - Subdivisiones de tareas
   - Campo clave: `Tipo de subtarea` (Análisis de impacto, Aprobación arquitecto CAI, Pruebas de aceptación, etc.)
   - Campos obligatorios: Title, State, TipoDeSubtarea

5. **Bug** - ⚠️ USAR "Historia técnica" CON TIPO "Bug" EN SU LUGAR
6. **Caso de prueba** - Casos de prueba específicos
7. **Riesgo** - Gestión de riesgos del proyecto
8. **Proyecto** - Iniciativas de alto nivel
9. **Épica** - Épicas de producto
10. **Revisión post implantación** - Revisiones después de despliegues

### ❌ Tipos Deshabilitados (NO USAR):
- Task → Usar **"Tarea"**
- User Story → Usar **"Historia"**
- Epic → Usar **"Épica"**
- Issue → Usar **"Riesgo"**

## 🏗️ Estructura Organizacional

### Jerarquía de Work Items:
```
PROYECTO (400+ activos)
├── ÉPICA/FEATURE (Capacidades de negocio)
├── HISTORIA/HISTORIA TÉCNICA (Requerimientos funcionales)
├── TAREA (Actividades específicas)
├── SUBTAREA (Subdivisiones de tareas)
├── BUG (como Historia técnica con tipo Bug)
└── CASO DE PRUEBA (Validaciones de calidad)
```

### Dominios de Negocio:
- **do-asegur-**: Aseguramiento (pólizas, reclamaciones, suscripción, ARL)
- **egv-salud-**: Salud (PAC, atención virtual, inclusión asegurados)
- **do-prestacion_distribucion-**: Prestación (salud en casa, medicamentos)
- **do-acceso_clientes-**: Acceso (portales, canales masivos, sostenibilidad)
- **do-infraestructura-**: Infraestructura (Kyndryl: core, DevOps, SRE, DBA)
- **do-ciencia_analitica-**: Analítica (BI, IA aplicada, arquitectura)

### Nomenclatura de Proyectos:
- **[PETID] + año**: Proyectos con identificador formal
- **[NO PETID] + año**: Proyectos sin identificador específico
- **Archivar-**: Proyectos en proceso de cierre
- **H_1_2025, T2 2025**: Hitos y trimestres específicos

### Estructura de Equipos (100+ equipos):
- **Prefijo "do-"**: Dominios operativos
- **Prefijo "egv-"**: Evolución y gobierno
- **Prefijo "t-"**: Equipos transversales
- **Prefijo "mod-"**: Modificaciones operativas
- **Sufijo "inactivo-"**: Equipos deshabilitados

## 🏢 Jerarquía de Proyectos en Sura (ACTUALIZADA 2025)

### Estructura de Proyectos (3 niveles principales):

#### 1. **Gerencia_Tecnologia** (Proyecto Principal)
- **ID**: 985807ad-7ff9-438d-849c-794c9bbc50f4
- **Descripción**: Agrupa todos los proyectos de desarrollo
- **Equipos**: 100+ equipos activos
- **Work Items Activos**: 400+ proyectos de alto nivel
- **Dominios incluidos**:
  - `do-asegur-*`: Aseguramiento y pólizas
  - `do-prestacion_distribucion-*`: Prestación y distribución
  - `do-acceso_clientes-*`: Acceso de clientes
  - `do-infraestructura-*`: Infraestructura (Kyndryl)
  - `do-ciencia_analitica-*`: Analítica e IA
  - `egv-salud-*`: Evolución en salud
  - `t-*`: Equipos transversales

#### 2. **Gerencia_Tecnologia_Egv_Aseguramiento** (Especializado)
- **ID**: d4897a90-a850-48a6-8dd2-be1a1742067a
- **Descripción**: Agrupa los proyectos de aseguramiento
- **Equipos**: 74+ equipos especializados
- **Enfoque**: Evolución y gobierno de aseguramiento
- **Dominios incluidos**:
  - `egv-asegur-*`: Evolución de aseguramiento
  - `do-asegur-*`: Desarrollo operativo de aseguramiento
  - `t-core_*`: Equipos transversales del core

#### 3. **Portafolios** (Iniciativas Estratégicas)
- **ID**: 4261b86e-ac42-449b-a360-113821717ccf
- **Descripción**: Proyecto que agrupa las iniciativas de los portafolios
- **Equipos**: 100+ equipos de iniciativas
- **Enfoque**: Nuevos desarrollos y modernización
- **Dominios incluidos**:
  - `mod-operativo_*`: Modificaciones operativas
  - `dllo-*`: Desarrollo de nuevos productos
  - `alianza-ecosistemas_*`: Alianzas estratégicas
  - `relev-pers-empr_*`: Relevancia personal empresarial

### 🎯 JERARQUÍA DE WORK ITEMS EN SURA (ACTUALIZADA)

#### Nivel 1: **PROYECTO** (Más alto - Iniciativas estratégicas)
- **Ejemplo**: "Remediación GW - 2025" (ID: 695480)
- **Área**: `Gerencia_Tecnologia\\do-asegur-plan_de_remediacion`
- **Propósito**: Agrupa iniciativas completas de gran envergadura
- **Estados**: New, En progreso, Cerrado, Planeado
- **Duración**: Típicamente anuales o multi-anuales

#### Nivel 2: **ÉPICA/FEATURE** (Capacidades de negocio)
- Agrupa funcionalidades relacionadas
- Duración: Trimestral o semestral

#### Nivel 3: **HISTORIA / HISTORIA TÉCNICA** (Requerimientos funcionales)
- **Historia**: Funcionalidades de usuario final
- **Historia técnica**: Trabajo técnico, infraestructura, desarrollo
- Duración: Sprint (1-4 semanas)

#### Nivel 4: **TAREA** (Actividades específicas)
- Trabajo granular dentro de historias
- Duración: Días o una semana

#### Nivel 5: **SUBTAREA** (Más granular)
- Subdivisiones de tareas específicas
- Duración: Horas o días

### 📁 ESTRUCTURA DE ÁREAS ORGANIZACIONALES

#### Patrón de Nomenclatura de Áreas:
```
Gerencia_Tecnologia\\{dominio}-{función}-{iniciativa}
```

**Ejemplo Real**: 
- Área: `Gerencia_Tecnologia\\do-asegur-plan_de_remediacion`
- Interpretación: dominio_aseguramiento - función_aseguramiento - iniciativa_plan_de_remediacion

#### Prefijos de Dominio:
- **do-**: Dominios operativos (desarrollo operativo)
- **egv-**: Evolución y gobierno
- **t-**: Transversales
- **mod-**: Modificaciones operativas
- **dllo-**: Desarrollo de nuevos productos

#### Sufijos de Función:
- **-asegur-**: Aseguramiento (pólizas, siniestros, suscripción)
- **-salud-**: Salud (PAC, consultas médicas)
- **-prestacion_distribucion-**: Prestación y distribución
- **-acceso_clientes-**: Portales y canales
- **-infraestructura-**: Infraestructura tecnológica
- **-ciencia_analitica-**: Analítica de datos e IA

### 🏷️ PATRONES DE NOMENCLATURA DE PROYECTOS

#### Clasificación por Tipo de Demanda:
1. **[PETID] + año**: Proyectos con identificador formal en sistema PETID
   - Ejemplo: "[PETID] 2025", "[PETID] H_1_2025"
   - Representan demandas oficiales registradas

2. **[NO PETID] + año**: Proyectos sin identificador específico
   - Ejemplo: "[NO PETID] 2025"
   - Proyectos internos o de mantenimiento

3. **Archivar-**: Proyectos en proceso de cierre o archivados
   - Ejemplo: "Archivar-[PETID] 2025"
   - Estado transitorio antes del cierre definitivo

#### Indicadores Temporales:
- **H_1_2025**: Hito 1 del año 2025
- **T2 2025, T3 2025, T4 2025**: Trimestre específico
- **Q1 2025, Q2 2025**: Quarter específico
- **Sprint X**: Iteraciones específicas de desarrollo

#### Proyectos Temáticos (Ejemplos Reales):
- **"Remediación GW - 2025"**: Plan de remediación de Guidewire
- **"Sostenibilidad de dominio"**: Iniciativas de sostenibilidad
- **"Demandas cruzadas 2025"**: Demandas entre múltiples dominios
- **"PILAR DE GESTIÓN DEL CONOCIMIENTO"**: Gestión del conocimiento organizacional
- **"Transformación Plan Vive"**: Transformación digital del Plan Vive
- **"IFRS17 - FeniX"**: Implementación de normativas contables

### 🔍 Cómo Determinar a Qué Proyectos Pertenezco

Para identificar los proyectos donde tienes acceso, usa estas herramientas:

1. **Lista todos los proyectos disponibles**:
   ```
   azuredevops_list_projects
   ```
   - Muestra los 3 proyectos principales de Sura
   - Solo verás proyectos donde tengas permisos

2. **Lista equipos de un proyecto específico**:
   ```
   azuredevops_list_teams project="Gerencia_Tecnologia"
   ```
   - Muestra todos los equipos donde puedes trabajar
   - Solo aparecen equipos con permisos de acceso

3. **Consulta tu trabajo asignado**:
   ```
   azuredevops_get_assigned_work project="Gerencia_Tecnologia"
   ```
   - Lista work items asignados a tu usuario
   - Indica automáticamente los proyectos activos

### 📊 Distribución por Dominio de Negocio

| Prefijo | Dominio | Proyecto Principal | Ejemplos de Equipos |
|---------|---------|-------------------|---------------------|
| `do-asegur-` | Aseguramiento | Gerencia_Tecnologia | Pólizas, Reclamaciones, ARL |
| `egv-asegur-` | Evolución Aseguramiento | Gerencia_Tecnologia_Egv_Aseguramiento | OIPA, Kinesis, Reaseguro |
| `do-prestacion-` | Prestación | Gerencia_Tecnologia | Salud en casa, Medicamentos |
| `do-acceso-` | Acceso Clientes | Gerencia_Tecnologia | Portales, Canales masivos |
| `mod-operativo-` | Modificaciones | Portafolios | IFRS17, Core, Nuevos modelos |
| `dllo-` | Desarrollo | Portafolios | Nuevos productos, Canales |

## ⚙️ Metodología Ágil y Iteraciones
- **Sprints**: Trimestrales (Q1, Q2, Q3, Q4)
- **Iteraciones**: Por días hábiles [11 días hábiles]
- **Jerarquía**: Gerencia_Tecnologia > Año > Sprint
- **Estados**: New → Active → Resolved → Closed

## 📋 Campos Obligatorios por Tipo

### Campos Universales (TODOS los tipos):
- **System.Title** - Título descriptivo
- **System.State** - Estado del work item (New por defecto)
- **System.AreaPath** - Ruta de área (dominio de negocio)
- **System.IterationPath** - Ruta de iteración (sprint)

### Campos Específicos Sura:
- **ID de la solución en el APM**: Campo Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14 (valor numérico, ej: 448)
- **Migración de datos**: Custom.MigracionDatos (Si/No)
- **Cumplimiento regulatorio**: Custom.CumplimientoRegulatorio (Si/No)
- **Control automático**: Custom.ControlAutomatico (Si/No)

## 🔧 Azure DevOps API - Formato JSON Patch

### Estructura Obligatoria (RFC 6902):
```json
[
  {
    "op": "add",
    "path": "/fields/System.Title",
    "value": "Título del work item"
  },
  {
    "op": "add", 
    "path": "/fields/Custom.TipoDeHistoria",
    "value": "Historia"
  }
]
```

### Content-Type Requerido:
- **OBLIGATORIO**: `application/json-patch+json`
- **NO usar**: `application/json`

### Relaciones Padre-Hijo:
```json
{
  "op": "add",
  "path": "/relations/-", 
  "value": {
    "rel": "System.LinkTypes.Hierarchy-Reverse",
    "url": "https://dev.azure.com/sura/_apis/wit/workItems/{parentId}"
  }
}
```""";
    }
    
    private String getExamples() {
        return """
# 📚 Ejemplos de Uso

## 🔍 Consultas Básicas
```
# Listar todos los proyectos
list_projects

# Ver equipos de Gerencia_Tecnologia
list_teams(project: "Gerencia_Tecnologia")

# Ver work item específico
get_workitem(project: "Gerencia_Tecnologia", workItemId: 695480)

# Obtener mis tareas asignadas
get_assigned_work(project: "Gerencia_Tecnologia")

# Ver tipos de work items disponibles
get_workitem_types(project: "Gerencia_Tecnologia")

# Iteraciones del equipo 
list_iterations(project: "Gerencia_Tecnologia", team: "do-asegur-arl")
```

## 📝 Creación de Work Items (Formato Sura)

### Crear Historia de Negocio:
```json
create_workitem({
  "project": "Gerencia_Tecnologia",
  "type": "Historia",
  "title": "Como usuario quiero poder consultar mis pólizas",
  "description": "Descripción detallada de la funcionalidad",
  "acceptanceCriteria": "Criterios de aceptación específicos",
  "tipoHistoria": "Historia",
  "migracionDatos": false,
  "cumplimientoRegulatorio": true,
  "controlAutomatico": false,
  "idSolucionAPM": "448",
  "assignedTo": "usuario@sura.com.co",
  "iterationPath": "Gerencia_Tecnologia\\2025\\Sprint 3 Q2 2025",
  "areaPath": "Gerencia_Tecnologia\\do-asegur-arl"
})
```

### Crear Tarea de Desarrollo:
```json
create_workitem({
  "project": "Gerencia_Tecnologia", 
  "type": "Tarea",
  "title": "Implementar endpoint REST para consulta de pólizas",
  "tipoTarea": "Tarea",
  "assignedTo": "desarrollador@sura.com.co",
  "remainingWork": 16,
  "parentId": 695480
})
```

### Crear Bug (usando Historia técnica):
```json
create_workitem({
  "project": "Gerencia_Tecnologia",
  "type": "Historia técnica", 
  "title": "Error en validación de campos de póliza",
  "description": "Descripción del error encontrado",
  "tipoHistoriaTecnica": "Bug",
  "migracionDatos": false,
  "cumplimientoRegulatorio": false,
  "controlAutomatico": true,
  "idSolucionAPM": "448",
  "priority": 1
})
```

## 🔍 Consultas WIQL Avanzadas

### Análisis Organizacional:
```sql
-- Proyectos activos
query_workitems({
  "project": "Gerencia_Tecnologia",
  "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItems WHERE [System.WorkItemType] = 'Proyecto' AND [System.State] <> 'Closed'"
})

-- Distribution por dominios  
query_workitems({
  "project": "Gerencia_Tecnologia",
  "query": "SELECT [System.AreaPath], COUNT() FROM WorkItems WHERE [System.WorkItemType] IN ('Historia', 'Historia técnica') GROUP BY [System.AreaPath]"
})
```

### Seguimiento Personal:
```sql
-- Mis work items asignados
query_workitems({
  "project": "Gerencia_Tecnologia", 
  "query": "SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType] FROM WorkItems WHERE [System.AssignedTo] = @Me AND [System.State] <> 'Closed'"
})

-- Work items de la iteración actual
query_workitems({
  "project": "Gerencia_Tecnologia",
  "query": "SELECT * FROM WorkItems WHERE [System.IterationPath] UNDER @CurrentIteration"
})
```

### Análisis por Dominio:
```sql
-- Proyectos de aseguramiento (ARL)
query_workitems({
  "project": "Gerencia_Tecnologia",
  "query": "SELECT [System.Id], [System.Title] FROM WorkItems WHERE [System.AreaPath] UNDER 'Gerencia_Tecnologia\\do-asegur-arl'"
})

-- Trabajo de Kyndryl (infraestructura)
query_workitems({
  "project": "Gerencia_Tecnologia", 
  "query": "SELECT * FROM WorkItems WHERE [System.AreaPath] CONTAINS 'kyndryl' AND [System.State] = 'Active'"
})

-- Análisis trimestral Q2 2025
query_workitems({
  "project": "Gerencia_Tecnologia",
  "query": "SELECT [System.WorkItemType], [System.State], COUNT() FROM WorkItems WHERE [System.IterationPath] CONTAINS 'Q2 2025' GROUP BY [System.WorkItemType], [System.State]"
})
```

### Reportes de Calidad:
```sql
-- Bugs activos por área
query_workitems({
  "project": "Gerencia_Tecnologia",
  "query": "SELECT [System.AreaPath], COUNT() FROM WorkItems WHERE [System.WorkItemType] = 'Historia técnica' AND [Custom.TipoDeHistoriaTecnica] = 'Bug' AND [System.State] <> 'Closed' GROUP BY [System.AreaPath]"
})

-- Items de cumplimiento regulatorio
query_workitems({
  "project": "Gerencia_Tecnologia", 
  "query": "SELECT [System.Id], [System.Title] FROM WorkItems WHERE [Custom.CumplimientoRegulatorio] = 'Si'"
})
```

## 🔄 Actualización de Work Items
```json
# Cambiar estado y asignación
update_workitem({
  "project": "Gerencia_Tecnologia",
  "workItemId": 695480,
  "state": "Active", 
  "assignedTo": "nuevo.usuario@sura.com.co",
  "remainingWork": 8
})

# Mover a nueva iteración
update_workitem({
  "project": "Gerencia_Tecnologia",
  "workItemId": 695480,
  "iterationPath": "Gerencia_Tecnologia\\2025\\Sprint 4 Q2 2025"
})
```

## 🧪 Testing y Validación

### Verificar configuración:
```json
# Validar conexión
list_projects

# Verificar tipos disponibles
get_workitem_types(project: "Gerencia_Tecnologia")

# Probar work item existente
get_workitem(project: "Gerencia_Tecnologia", workItemId: 1)
```

### Crear work item de prueba:
```json
create_workitem({
  "project": "Gerencia_Tecnologia",
  "type": "Tarea",
  "title": "Prueba de creación de work item",
  "tipoTarea": "Tarea"
})
```""";
    }
    
    private String getBestPractices() {
        return """
# 🎯 Mejores Prácticas

## ✅ Consultas Eficientes y Rendimiento
- **Usar WIQL para análisis complejos**: En lugar de múltiples llamadas individuales
- **Limitar resultados**: Usar maxResults para evitar timeouts en proyectos grandes  
- **Utilizar macros dinámicas**: @Me, @Today, @CurrentIteration para consultas reutilizables
- **Filtrar por estado**: Incluir [System.State] para excluir work items cerrados
- **Campos específicos**: Usar SELECT con campos específicos en lugar de SELECT *
- **Paginar resultados**: Para conjuntos de datos grandes usar maxResults apropiados

## 🏗️ Estructura Organizacional Sura
- **Respetar jerarquía**: Proyecto > Épica > Historia/Historia técnica > Tarea > Subtarea
- **Nomenclatura estándar**: Usar prefijos correctos (do-, egv-, t-, mod-) según dominios
- **Contexto temporal**: Considerar Q1/Q2/Q3/Q4 en iteraciones y planificación
- **AreaPath por dominio**: Usar estructura de área según dominio de negocio
- **Estados consistentes**: Seguir flujo New → Active → Resolved → Closed

## � Gestión de Work Items
- **Tipos en español**: OBLIGATORIO usar "Historia", "Tarea", etc. (no Task, User Story)
- **Campos obligatorios**: Validar todos los campos requeridos por tipo antes de crear
- **IterationPath completo**: Incluir ruta completa al crear work items
- **AreaPath apropiado**: Asignar según dominio de negocio correspondiente
- **Tags descriptivos**: Usar para categorización adicional (urgent, bug-fix, etc.)
- **Emails corporativos**: Mantener consistencia en assignedTo con @sura.com.co

## 🔧 Creación y Actualización
- **JSON Patch obligatorio**: Usar format RFC 6902 para create/update
- **Content-Type correcto**: Siempre usar 'application/json-patch+json'
- **Validación por tipo**: Verificar campos específicos según tipo de work item
- **Relaciones padre-hijo**: Usar parentId para jerarquías automáticas
- **Campos Sura**: Incluir campos personalizados (ID APM, migración datos, etc.)
- **Control de concurrencia**: Usar revision para updates seguros

## 🏢 Trabajando con Proyectos Sura

### Identificar Mis Proyectos:
```bash
# 1. Listar todos los proyectos disponibles
list_projects()

# 2. Ver equipos de proyecto principal
list_teams(project: "Gerencia_Tecnologia")

# 3. Consultar mi trabajo asignado por proyecto
get_assigned_work(project: "Gerencia_Tecnologia")
get_assigned_work(project: "Gerencia_Tecnologia_Egv_Aseguramiento")  
get_assigned_work(project: "Portafolios")
```

### Determinar el Proyecto Correcto para un Work Item:
- **Desarrollo operativo** → `Gerencia_Tecnologia`
- **Evolución de aseguramiento** → `Gerencia_Tecnologia_Egv_Aseguramiento`
- **Nuevas iniciativas/portafolios** → `Portafolios`

### Consultas Cross-Proyecto:
```sql
# Buscar trabajo asignado en todos mis proyectos
SELECT [System.Id], [System.Title], [System.TeamProject]
FROM WorkItems
WHERE [System.AssignedTo] = @Me
AND [System.State] <> 'Closed'
```

### 🎯 Prompt: "¿A qué proyectos pertenezco?"

Para determinar tus proyectos de pertenencia, sigue esta secuencia:

1. **Lista proyectos disponibles**:
   ```
   azuredevops_list_projects
   ```

2. **Verifica trabajo asignado por proyecto**:
   ```
   azuredevops_query_workitems({
     "project": "Gerencia_Tecnologia",
     "query": "SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType] FROM WorkItems WHERE [System.AssignedTo] = @Me",
     "maxResults": 10
   })
   ```
   Repetir para: `Gerencia_Tecnologia_Egv_Aseguramiento` y `Portafolios`

3. **Analiza trabajo activo**:
   ```
   azuredevops_query_workitems({
     "project": "PROYECTO_PRINCIPAL",
     "query": "SELECT [System.Id], [System.Title], [System.State] FROM WorkItems WHERE [System.AssignedTo] = @Me AND [System.State] <> 'Cerrado'",
     "maxResults": 20
   })
   ```

**Criterios de pertenencia**:
- ✅ **Acceso Activo**: Work items asignados (especialmente no cerrados)
- 📋 **Acceso Histórico**: Solo work items cerrados
- ❌ **Sin Acceso**: No aparecen work items

**Indicadores de tu rol**:
- **Cantidad de work items** por proyecto
- **Tipos de trabajo**: Historia, Tarea, Feature, etc.
- **Áreas de negocio**: System.AreaPath (do-*, egv-*, t-*)
- **Proyecto principal**: Donde tienes más actividad

## 🔍 Análisis y Reportes
- **Agrupar por AreaPath**: Para análisis por dominio de negocio
- **Filtrar por WorkItemType**: Para análisis específicos por tipo
- **Usar ChangedDate**: Para seguimiento temporal y tendencias
- **Combinar State + IterationPath**: Para seguimiento ágil efectivo
- **Macros en consultas**: Aprovechar @CurrentIteration para reportes dinámicos

## ⚠️ Consideraciones de Seguridad y Permisos
- **PAT con scope correcto**: Usar vso.work_write para operaciones de escritura
- **Validar permisos**: Verificar acceso a proyectos antes de operaciones
- **Datos sensibles**: No incluir información confidencial en títulos/descripciones
- **Logs seguros**: Evitar logging de tokens o información personal

## 🎛️ Configuración y Entorno
- **Variables de entorno**: AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT correctas
- **Timeouts apropiados**: Configurar timeouts según red corporativa
- **Cache inteligente**: Cachear información de proyectos/equipos para uso repetitivo
- **Error handling**: Implementar manejo robusto de errores específicos Azure DevOps

## 📊 Monitoreo y Troubleshooting
- **Logs detallados**: Activar DEBUG para investigación de problemas
- **Response codes**: Monitorear 400, 401, 403, 404 para identificar patrones
- **Rate limiting**: Respetar límites de Azure DevOps API
- **Network issues**: Considerar proxy/firewall corporativo en troubleshooting

## 🚀 Arquitectura del Servidor MCP

### Stack Tecnológico:
- **Java 21 LTS**: Aprovechar características modernas (pattern matching, records)
- **Spring Boot 3.3.2**: Configuración automática y gestión de dependencias
- **WebSocket**: Para comunicación en tiempo real con VS Code
- **JSON-RPC 2.0**: Protocolo estándar MCP para interoperabilidad
- **Maven/Gradle**: Gestión de dependencias y build reproducible

### Patrones de Diseño Implementados:
- **Builder Pattern**: Para construcción de objetos complejos (Tool, McpResponse)
- **Strategy Pattern**: Diferentes herramientas implementan estrategias específicas
- **Factory Pattern**: Creación de respuestas success/error
- **Template Method**: Comportamiento común en McpTool interface

### Extensibilidad:
- **Plugin Architecture**: Nuevas herramientas via @Component sin código adicional
- **Configuration Driven**: Configuración externa via application.yml
- **Dependency Injection**: Spring IoC para gestión automática de dependencias
- **Interface Based**: Extensión fácil mediante interfaces bien definidas

## 📚 Referencias y Recursos

### Documentación Oficial:
- **Azure DevOps REST API v7.1**: https://docs.microsoft.com/en-us/rest/api/azure/devops/
- **Model Context Protocol**: https://spec.modelcontextprotocol.io/
- **JSON Patch RFC 6902**: https://datatracker.ietf.org/doc/html/rfc6902
- **Spring Boot Reference**: https://docs.spring.io/spring-boot/docs/current/reference/html/

### Guías del Proyecto:
- **README.md**: Configuración completa e instalación
- **ARCHITECTURE.md**: Arquitectura detallada del servidor
- **DEVELOPMENT.md**: Guía de desarrollo y testing
- **JAVA_MCP_GUIDE.md**: Guía completa de servidores MCP en Java""";
    }
    
    @Override
    public String getName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
