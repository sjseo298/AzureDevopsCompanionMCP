package com.mcp.server.prompts.azuredevops;

import com.mcp.server.prompts.base.BasePrompt;
import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.List;
import java.util.Map;

/**
 * Prompt para generar automáticamente los archivos de configuración organizacional
 * cuando no existen en el directorio config/.
 * 
 * Este prompt actúa como un asistente inteligente que:
 * 1. Detecta si faltan archivos de configuración
 * 2. Ejecuta descubrimiento automático de la organización
 * 3. Genera y guarda los archivos de configuración necesarios
 * 4. Proporciona validación y mejores prácticas
 */
public class GenerarConfiguracionOrganizacionalPrompt extends BasePrompt {
    
    public GenerarConfiguracionOrganizacionalPrompt() {
        super(
            "generar_configuracion_organizacional",
            "Generar Configuración Organizacional Automática",
            "Detecta archivos de configuración faltantes y genera automáticamente la configuración organizacional completa mediante descubrimiento de Azure DevOps, incluyendo tipos de work items, campos personalizados y estructura de equipos.",
            List.of(
                new Prompt.PromptArgument(
                    "forzar_regeneracion",
                    "Forzar Regeneración",
                    "Si se debe regenerar la configuración aunque ya existan archivos (útil para actualizaciones). Por defecto: false",
                    false
                ),
                new Prompt.PromptArgument(
                    "proyecto_principal",
                    "Proyecto Principal",
                    "Nombre del proyecto principal a usar como base para el descubrimiento. Si no se especifica, intentará detectar automáticamente",
                    false
                ),
                new Prompt.PromptArgument(
                    "incluir_campos_extendidos",
                    "Incluir Campos Extendidos",
                    "Si se debe incluir información extendida de campos (valores permitidos, tipos de datos, etc.). Por defecto: true",
                    false
                ),
                new Prompt.PromptArgument(
                    "generar_backup",
                    "Generar Backup",
                    "Si se debe hacer backup de archivos existentes antes de regenerar. Por defecto: true",
                    false
                )
            )
        );
    }
    
    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        validateArguments(arguments);
        
        boolean forzarRegeneracion = getBooleanArgument(arguments, "forzar_regeneracion", false);
        String proyectoPrincipal = getStringArgument(arguments, "proyecto_principal", null);
        boolean incluirCamposExtendidos = getBooleanArgument(arguments, "incluir_campos_extendidos", true);
        boolean generarBackup = getBooleanArgument(arguments, "generar_backup", true);
        
        // Mensaje del sistema estableciendo el contexto
        String systemPrompt = """
            Eres un asistente especializado en la configuración automática de Azure DevOps MCP Server.
            
            Tu misión es detectar archivos de configuración faltantes y generar automáticamente 
            toda la configuración organizacional necesaria mediante el uso inteligente de las 
            herramientas MCP disponibles, adaptándose dinámicamente a cualquier organización.
            
            PRINCIPIO FUNDAMENTAL: DESCUBRIMIENTO DINÁMICO EXHAUSTIVO
            - NO uses información hardcodeada de organizaciones específicas
            - SIEMPRE obtén información actual mediante herramientas MCP
            - UTILIZA descubrimiento exhaustivo de tipos de work items para garantizar completitud
            - ADAPTA la configuración a la estructura organizacional real descubierta
            
            ⚠️ **CRÍTICO**: Utilizar exhaustiveTypeDiscovery=true en azuredevops_discover_organization
            para obtener TODOS los tipos de work items de TODOS los proyectos, evitando perder
            tipos personalizados críticos que podrían estar en proyectos específicos.
            
            PROCESO DE GENERACIÓN AUTOMÁTICA:
            
            🔍 **FASE 1: DETECCIÓN**
            1. Verificar existencia de archivos de configuración en config/
            2. Identificar archivos faltantes o desactualizados
            3. Evaluar si se necesita regeneración completa o parcial
            
            📊 **FASE 2: DESCUBRIMIENTO DINÁMICO**
            1. Usar `azuredevops_discover_organization` para análisis completo de la organización real
            2. Ejecutar con todos los parámetros extendidos habilitados
            3. Capturar información específica de la organización sin asumir estructura previa
            4. Adaptar configuración basada en los datos reales descubiertos
            
            📁 **FASE 3: GENERACIÓN DE ARCHIVOS ADAPTATIVA**
            Generar archivos de configuración basados en la estructura organizacional real:
            
            ▪️ **config/discovered-organization.yml** - Estructura real descubierta dinámicamente
            ▪️ **config/organization-config.yml** - Configuración adaptada a la organización
            ▪️ **config/field-mappings.yml** - Mapeo de campos personalizados
            ▪️ **config/business-rules.yml** - Reglas de negocio organizacionales
            
            🔧 **FASE 4: VALIDACIÓN Y OPTIMIZACIÓN**
            1. Validar sintaxis YAML generada
            2. Verificar completitud de campos obligatorios
            3. Optimizar configuración para mejores prácticas
            
            HERRAMIENTAS MCP DISPONIBLES:
            
            1. `azuredevops_discover_organization` - Descubrimiento organizacional
            2. `azuredevops_list_projects` - Listar proyectos disponibles
            3. `azuredevops_get_workitem_types` - Obtener tipos de work items detallados
            4. `azuredevops_list_teams` - Listar equipos por proyecto
            5. `azuredevops_list_iterations` - Analizar cadencia de entregas
            
            📋 **CONTRATO TÉCNICO COMPLETO DE GENERACIÓN DE ARCHIVOS**
            
            **ESPECIFICACIONES TÉCNICAS OBLIGATORIAS - CUMPLIMIENTO TOTAL REQUERIDO**
            
            Este contrato establece las especificaciones técnicas que DEBEN cumplir todos los 
            archivos generados para garantizar compatibilidad 100% con la aplicación.
            
            📄 **ARCHIVO 1: discovered-organization.yml**
            
            **Propósito:** Estructura organizacional completa descubierta automáticamente
            **Consumidor:** OrganizationContextService.loadConfiguration()
            **Validación:** Debe parsear como YAML válido sin errores
            
            ```yaml
            # =============================================================================
            # CABECERA OBLIGATORIA - REQUERIDA PARA VALIDACIÓN
            # =============================================================================
            discoveryDate: string                          # ISO 8601: "2025-07-30T12:30:00Z"
            lastUpdated: string                            # ISO 8601: "2025-07-30T12:30:00Z" 
            organizationUrl: string                        # URL: "https://dev.azure.com/nombre-org"
            organizationName: string                       # Nombre legible: "Mi Organización"
            
            # =============================================================================
            # METADATA DE DESCUBRIMIENTO - REQUERIDA PARA ESTADÍSTICAS
            # =============================================================================
            discoveryMetadata:
              totalProjects: integer                       # Número total de proyectos
              totalWorkItemTypes: integer                  # Número total de tipos
              customWorkItemTypes: integer                 # Número de tipos personalizados
              standardFieldsFound: integer                 # Campos estándar encontrados
              customFieldsFound: integer                   # Campos personalizados encontrados
              dateFieldsFound: integer                     # Campos de fecha encontrados
            
            # =============================================================================
            # PROYECTOS - ESTRUCTURA PRINCIPAL (ARRAY REQUERIDO)
            # =============================================================================
            projects:
              - id: string                                 # REQUERIDO: Identificador kebab-case
                name: string                               # REQUERIDO: Nombre exacto Azure DevOps
                description: string                        # REQUERIDO: Descripción del proyecto
                
                # EQUIPOS DEL PROYECTO (ARRAY OPCIONAL)
                teams:                                     # OPCIONAL: Lista de equipos
                  - name: string                           # REQUERIDO: Nombre del equipo
                    description: string                    # REQUERIDO: Descripción
                    prefix: string                         # OPCIONAL: Prefijo nomenclatura
                    domain: string                         # OPCIONAL: Dominio funcional
                    function: string                       # OPCIONAL: Función específica
                
                # ESTRUCTURA DE ÁREAS (ARRAY OPCIONAL)
                areaStructure:                             # OPCIONAL: Jerarquía de áreas
                  - string                                 # Formato: "Proyecto\\SubArea"
                
                # TIPOS DE WORK ITEMS (ARRAY REQUERIDO)
                workItemTypes:                             # REQUERIDO: Al menos un tipo
                  - name: string                           # REQUERIDO: Nombre del tipo
                    baseType: string                       # REQUERIDO: Tipo base Azure DevOps
                    description: string                    # REQUERIDO: Descripción
                    
                    # CAMPOS OBLIGATORIOS (ARRAY REQUERIDO)
                    requiredFields:                        # REQUERIDO: Campos obligatorios
                      - string                             # Formato: "System.Title"
                    
                    # CAMPOS DE FECHA (ARRAY CRÍTICO PARA COMPATIBILIDAD)
                    dateFields:                            # CRÍTICO: Evita problema identificado
                      - "Microsoft.VSTS.Scheduling.StartDate"    # OBLIGATORIO
                      - "Microsoft.VSTS.Scheduling.FinishDate"   # OBLIGATORIO  
                      - "Microsoft.VSTS.Scheduling.TargetDate"   # CRÍTICO Features
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
                        type: enum                         # REQUERIDO: Ver tipos válidos
                        required: boolean                  # REQUERIDO: true/false
                        allowedValues: array[string]       # CONDICIONAL: Solo picklist
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
            
            📄 **ARCHIVO 2: organization-config.yml**
            
            **Propósito:** Configuración personalizable por el usuario final
            **Consumidor:** OrganizationConfigService.getDefaultOrganizationConfig()
            **Validación:** Debe contener configuración Azure DevOps válida
            
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
              custom:                                    # OPCIONAL: Array tipos personalizados
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
            
            📄 **ARCHIVO 3: field-mappings.yml**
            
            **Propósito:** Mapeo detallado entre campos internos y campos Azure DevOps
            **Consumidor:** OrganizationConfigService.getFieldMapping()
            **Validación:** Todos los campos básicos deben estar presentes
            
            ```yaml
            # =============================================================================
            # MAPEO DE CAMPOS (REQUERIDO)
            # =============================================================================
            fieldMappings:                               # REQUERIDO: Raíz de mapeos
              
              # CAMPOS BÁSICOS OBLIGATORIOS
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
              
              # CAMPOS PERSONALIZADOS (OPCIONAL)
              # Cualquier campo adicional siguiendo el mismo patrón
            
            # =============================================================================
            # VALIDACIONES DE CAMPOS (OPCIONAL)
            # =============================================================================
            validations:                                # OPCIONAL: Reglas de validación
              nombreCampo:                              # Nombre del campo a validar
                rule: enum                              # REQUERIDO: Ver reglas válidas
                condition: string                       # CONDICIONAL: Según la regla
                values: array                           # CONDICIONAL: Para regla in_list
                pattern: string                         # CONDICIONAL: Para matches_pattern
                message: string                         # REQUERIDO: Mensaje de error
            ```
            
            📄 **ARCHIVO 4: business-rules.yml**
            
            **Propósito:** Reglas de negocio, flujos de trabajo y automatización
            **Consumidor:** Sistema de validación y automatización
            **Validación:** Estructura válida para motor de reglas
            
            ```yaml
            # =============================================================================
            # REGLAS DE NEGOCIO (REQUERIDO)
            # =============================================================================
            businessRules:                              # REQUERIDO: Raíz de reglas
              
              # VALIDACIONES POR TIPO (OPCIONAL)
              validation:                               # OPCIONAL: Reglas de validación
                tipoWorkItem:                           # Nombre del tipo (camelCase)
                  - rule: string                        # REQUERIDO: Descripción de la regla
                    field: string                       # REQUERIDO: Campo a validar
                    condition: enum                     # REQUERIDO: Ver condiciones válidas
                    values: array                       # CONDICIONAL: Para in_list
                    pattern: string                     # CONDICIONAL: matches_pattern
                    message: string                     # OPCIONAL: Mensaje personalizado
              
              # FLUJOS DE TRABAJO (OPCIONAL)
              workflow:                                 # OPCIONAL: Definición de workflows
                tipoWorkItem:                           # Nombre del tipo (camelCase)
                  states: array[string]                 # REQUERIDO: Estados válidos
                  transitions:                          # REQUERIDO: Transiciones válidas
                    estadoOrigen: array[string]         # Estados destino permitidos
              
              # AUTOMATIZACIÓN (OPCIONAL)
              automation:                               # OPCIONAL: Reglas automatización
                nombreAutomatizacion:                   # Nombre de la automatización
                  enabled: boolean                      # REQUERIDO: true/false
                  rules:                                # REQUERIDO: Array de reglas
                    - condition: string                 # REQUERIDO: Condición de disparo
                      action: enum                      # REQUERIDO: Ver acciones válidas
                      target: string                    # CONDICIONAL: Según la acción
            ```
            
            🔧 **TIPOS DE DATOS VÁLIDOS (TABLA DE REFERENCIA)**
            
            | Tipo | Descripción | Ejemplo | Notas |
            |------|-------------|---------|-------|
            | string | Texto simple | "Título" | Texto sin formato |
            | html | HTML enriquecido | "<p>Desc</p>" | Para descripciones |
            | plainText | Texto plano | "Texto" | Sin formato HTML |
            | integer | Número entero | 42 | Solo números enteros |
            | double | Número decimal | 3.14 | Números con decimales |
            | boolean | Verdadero/Falso | true/false | Solo true o false |
            | dateTime | Fecha y hora | "2025-07-30T12:30:00Z" | ISO 8601 |
            | identity | Usuario Azure DevOps | "user@company.com" | Email usuario |
            | picklistString | Lista opciones texto | ["Op1", "Op2"] | Requiere allowedValues |
            | picklistInteger | Lista opciones numérica | [1, 2, 3] | Requiere allowedValues |
            
            🔧 **VALORES ENUM VÁLIDOS**
            
            **baseType** válidos:
            - "User Story", "Task", "Bug", "Feature", "Epic", "Issue", "Test Case"
            
            **category** válidos:
            - "functional", "technical", "defect", "implementation"
            
            **condition** para validaciones válidos:
            - "required", "not_empty", "in_list", "matches_pattern", "required_if_type"
            
            **action** para automatización válidos:
            - "assign_to_lead", "transition_to_resolved", "add_tag", "send_notification"
            
            ⚠️ **RESTRICCIONES Y VALIDACIONES CRÍTICAS**
            
            1. **Referencias de Campos** - FORMATO EXACTO OBLIGATORIO:
               - Sistema: "System.Campo"
               - VSTS: "Microsoft.VSTS.Categoria.Campo"
               - Personalizados: "Custom.ReferenceName" o "Custom.GUID"
            
            2. **IDs de Proyectos** - FORMATO KEBAB-CASE:
               - Correcto: "proyecto-principal", "mi-proyecto-especial"
               - Incorrecto: "Proyecto Principal", "miProyectoEspecial"
            
            3. **Campos de Fecha** - CRÍTICOS PARA COMPATIBILIDAD:
               - Su ausencia causa el problema identificado en la aplicación
               - OBLIGATORIOS: StartDate, FinishDate, TargetDate, DueDate
               - FORMATO: "Microsoft.VSTS.Scheduling.NombreCampo"
            
            4. **allowedValues** - CONDICIONAL:
               - SOLO válido para tipos "picklistString" y "picklistInteger"
               - Para otros tipos, NO incluir este campo
            
            5. **Valores Boolean** - FORMATO NATIVO YAML:
               - Correcto: true, false (sin comillas)
               - Incorrecto: "true", "false" (con comillas - son strings)
            
            6. **Nomenclatura de Campos**:
               - Internos: camelCase ("campoPersonalizado")
               - Azure DevOps: formato exacto ("System.Title")
               - IDs: kebab-case ("proyecto-principal")
            
            ✅ **CHECKLIST DE VALIDACIÓN OBLIGATORIO**
            
            ANTES DE GENERAR:
            - [ ] Conexión Azure DevOps funcional
            - [ ] Permisos para leer metadatos organizacionales
            - [ ] Proyectos accesibles por el usuario actual
            - [ ] Herramientas MCP disponibles y funcionales
            
            DESPUÉS DE GENERAR:
            - [ ] Sintaxis YAML válida en TODOS los archivos
            - [ ] Estructura obligatoria presente según contrato
            - [ ] Campos de fecha críticos en TODOS los workItemTypes
            - [ ] Referencias de campos en formato correcto
            - [ ] Tipos de datos válidos según tabla de referencia
            - [ ] IDs en formato kebab-case para proyectos
            - [ ] Valores boolean como true/false (no strings)
            - [ ] allowedValues SOLO en campos picklist
            
            VALIDACIÓN DE COMPATIBILIDAD:
            - [ ] OrganizationContextService.loadConfiguration() sin errores
            - [ ] OrganizationConfigService.getFieldMapping() retorna datos
            - [ ] OrganizationConfigService.getDefaultOrganizationConfig() funcional
            - [ ] Parsing YAML exitoso con ObjectMapper
            - [ ] Campos referenciales existen en Azure DevOps real
            
            🚨 **ERRORES CRÍTICOS A EVITAR**
            
            ❌ "Cannot parse YAML" → Validar sintaxis YAML antes de guardar
            ❌ "Missing required field: discoveryDate" → Incluir cabecera obligatoria
            ❌ "Invalid field reference" → Usar formato exacto de referencias
            ❌ "Date fields missing" → Incluir StartDate, FinishDate, TargetDate, DueDate
            ❌ "Boolean expected, string found" → Usar true/false (no "true"/"false")
            ❌ "Field not found in Azure DevOps" → Verificar que campos existan realmente
            
            📋 **EJEMPLOS DE REFERENCIAS REALES COMUNES**
            
            **Ejemplos de Campos Personalizados Comunes:**
            - "Custom.FieldName" → Campo personalizado con nombre descriptivo
            - "Custom.{GUID}" → Campo personalizado con identificador único
            - "Custom.Category" → Campo de categorización organizacional
            - "Custom.Status" → Campo de estado personalizado
            - "Custom.BooleanField" → Campo de tipo verdadero/falso
            - "Custom.Priority" → Campo de prioridad personalizada
            - "Custom.Department" → Campo de departamento o área
            - "Custom.BusinessValue" → Campo de valor de negocio
            
            **Patrones de Equipos Organizacionales:**
            - Prefijos funcionales → Equipos por función específica
            - Prefijos de dominio → Equipos por área de negocio
            - Prefijos técnicos → Equipos por especialización técnica
            - Patrón común: "{prefijo}-{area}-{funcion}"
            
            **Tipos de Work Items Estándar:**
            - "User Story" → Historias de usuario estándar
            - "Task" → Tareas de implementación
            - "Bug" → Defectos del sistema
            - "Feature" → Características de producto
            - "Epic" → Épicas de alto nivel
            
            CONTEXTO DE EJECUCIÓN:
            """;
        
        // Construir contexto específico basado en argumentos
        StringBuilder contextBuilder = new StringBuilder(systemPrompt);
        
        if (forzarRegeneracion) {
            contextBuilder.append("\n🔄 **REGENERACIÓN FORZADA**: Se debe regenerar toda la configuración incluso si ya existe.");
        }
        
        if (proyectoPrincipal != null) {
            contextBuilder.append("\n🎯 **PROYECTO PRINCIPAL**: ").append(proyectoPrincipal);
        } else {
            contextBuilder.append("\n🔍 **DETECCIÓN AUTOMÁTICA**: Detectar automáticamente el proyecto principal.");
        }
        
        if (incluirCamposExtendidos) {
            contextBuilder.append("\n📋 **CAMPOS EXTENDIDOS**: Incluir información completa de campos (allowedValues, fieldType, etc.)");
        }
        
        if (generarBackup) {
            contextBuilder.append("\n💾 **BACKUP**: Hacer backup de archivos existentes antes de regenerar.");
        }
        
        // Prompt principal para el usuario
        String userPrompt = """
            Ejecuta el proceso de generación automática de configuración organizacional para Azure DevOps MCP Server.
            
            **INSTRUCCIONES ESPECÍFICAS:**
            
            1. **VERIFICAR ARCHIVOS EXISTENTES**
               - Listar contenido del directorio config/
               - Identificar qué archivos existen y cuáles faltan
               - Evaluar si los archivos existentes están completos
            
            2. **OBTENER CONTEXTO ORGANIZACIONAL DINÁMICO**
               - PRIMER PASO: Ejecutar `get_help()` para obtener contexto organizacional actual
               - Usar `azuredevops_list_projects` para obtener proyectos reales disponibles
               
               - **DESCUBRIMIENTO EXHAUSTIVO DE TIPOS DE WORK ITEMS (CRÍTICO):**
                 Ejecutar `azuredevops_discover_organization` con parámetros:
                 * exhaustiveTypeDiscovery: true
                 * includeWorkItemTypes: true
                 * includeFields: true (si incluir_campos_extendidos = true)
                 * generateConfig: true
                 
                 Este paso es FUNDAMENTAL porque descubre TODOS los tipos de work items 
                 en TODOS los proyectos, garantizando que no se pierda ningún tipo 
                 personalizado crítico para la organización.
               
               - Si proyecto_principal está especificado, úsalo; sino detecta automáticamente
               - ADAPTAR toda la configuración basada en información real descubierta
            
            3. **GENERAR ESTRUCTURA COMPLETA**
               Para cada proyecto descubierto, ejecutar:
               - `azuredevops_get_workitem_types` con includeExtendedInfo: true y includeFieldDetails: true
               - `azuredevops_list_teams` para estructura organizacional
               - `azuredevops_list_iterations` para análisis de cadencia
            
            4. **CREAR ARCHIVOS DE CONFIGURACIÓN**
               Generar y guardar los siguientes archivos YAML válidos:
               
               **discovered-organization.yml** con:
               - Metadata de descubrimiento (fecha, versión, etc.)
               - Estructura real de proyectos y equipos descubierta dinámicamente
               - Tipos de work items reales con campos específicos de la organización
               - Campos de fecha críticos (StartDate, FinishDate, TargetDate, DueDate)
               - Patrones de nomenclatura reales detectados en la organización
               - Análisis de cadencia organizacional actual
               
               **organization-config.yml** con:
               - Configuración personalizable adaptada a la organización real
               - Mapeo de campos específicos de la organización descubierta
               - Reglas de negocio específicas
               - Estructura de equipos y proyectos
               
               **field-mappings.yml** con:
               - Mapeo detallado de campos personalizados
               - Tipos de datos y validaciones
               - Valores permitidos (allowedValues)
               - Campos requeridos por tipo de work item
               
               **business-rules.yml** con:
               - Reglas de validación organizacionales
               - Flujos de trabajo (workflows)
               - Dependencias entre campos
               - Reglas de autocompletado
            
            5. **VALIDAR Y OPTIMIZAR**
               - Verificar sintaxis YAML válida usando herramientas como yamllint
               - Validar que todos los campos obligatorios estén presentes
               - Verificar que las referencias de campos sean correctas
               - Optimizar para mejores prácticas de MCP
               - Generar reporte de archivos creados/actualizados
               
               **VALIDACIONES CRÍTICAS DE COMPATIBILIDAD:**
               
               ✅ **Verificar estructura obligatoria**:
               - discovered-organization.yml debe tener: discoveryDate, organizationName, projects[]
               - organization-config.yml debe tener: organization.name, organization.azure
               - field-mappings.yml debe tener: fieldMappings con campos básicos
               - business-rules.yml debe tener: businessRules.validation
               
               ✅ **Verificar campos de fecha críticos**:
               - Todos los workItemTypes deben incluir dateFields[]
               - StartDate, FinishDate, TargetDate, DueDate deben estar presentes
               - Referencias deben ser exactas: "Microsoft.VSTS.Scheduling.StartDate"
               
               ✅ **Verificar tipos de datos**:
               - Solo usar tipos válidos: string, html, boolean, integer, picklistString
               - allowedValues solo para tipos picklist
               - required debe ser boolean (true/false)
               
               ✅ **Verificar nomenclatura**:
               - IDs de proyectos en kebab-case
               - Nombres de campos internos en camelCase
               - Referencias de Azure DevOps exactas
               
               ✅ **Verificar integridad referencial**:
               - Todos los campos mencionados en requiredFields deben tener definición
               - defaultValues deben coincidir con allowedValues si es picklist
               - workItemTypes deben tener baseType válido
            
            6. **PROPORCIONAR GUÍA DE USO**
               - Explicar qué archivos se generaron y su propósito
               - Indicar cómo personalizar la configuración generada
               - Sugerir próximos pasos de configuración
               - Documentar campos específicos encontrados
               - Proporcionar ejemplos de uso de los archivos generados
               
               **EJEMPLOS DE PATRONES ORGANIZACIONALES COMUNES:**
               
               🔍 **Tipos de campos personalizados encontrados típicamente:**
               - "Custom.WorkItemType" → Clasificaciones específicas
               - "Custom.{GUID-unico}" → Campos con identificadores únicos
               - "Custom.DescriptiveField" → Campos con nombres descriptivos
               - "Custom.CustomStatus" → Estados específicos de la organización
               - "Custom.BooleanField" → Campos de validación verdadero/falso
               - "Custom.Priority" → Campos de prioridad personalizada
               - "Custom.Department" → Campos de departamento o área
               - "Custom.BusinessValue" → Campos de valor de negocio
               
               📋 **Patrones de nomenclatura de equipos típicos:**
               - Prefijos funcionales → Equipos por función específica
               - Prefijos de dominio → Equipos por área de negocio
               - Prefijos técnicos → Equipos por especialización
               - Patrón común: "{prefijo}-{area}-{funcion}"
               
               🏗️ **Tipos de work items estándar a incluir:**
               - "User Story" → Historias de usuario estándar
               - "Task" → Tareas de implementación
               - "Bug" → Defectos del sistema
               - "Feature" → Características de producto
               - "Epic" → Épicas de alto nivel
               
               **VERIFICACIÓN FINAL DE ARCHIVOS GENERADOS:**
               
               Después de generar todos los archivos, OBLIGATORIAMENTE verificar:
               
               🔍 **VALIDACIÓN DE ESTRUCTURA:**
               - discovered-organization.yml tiene: discoveryDate, organizationName, projects[]
               - organization-config.yml tiene: organization.name, organization.azure
               - field-mappings.yml tiene: fieldMappings con campos básicos (title, description, state)
               - business-rules.yml tiene: businessRules.validation (puede estar vacío pero presente)
               
               🔍 **VALIDACIÓN DE CAMPOS DE FECHA:**
               - TODOS los workItemTypes incluyen dateFields[] con los 4 campos críticos
               - StartDate, FinishDate, TargetDate, DueDate están presentes
               - Referencias son exactas: "Microsoft.VSTS.Scheduling.StartDate"
               
               🔍 **VALIDACIÓN DE TIPOS DE DATOS:**
               - Solo usar tipos de la tabla de referencia
               - allowedValues SOLO para tipos picklistString/picklistInteger
               - required es boolean (true/false) no string
               
               🔍 **VALIDACIÓN DE NOMENCLATURA:**
               - IDs proyectos en kebab-case
               - Nombres campos internos en camelCase
               - Referencias Azure DevOps exactas
               
               🔍 **VALIDACIÓN DE INTEGRIDAD REFERENCIAL:**
               - Campos en requiredFields tienen definición completa
               - defaultValues coinciden con allowedValues si es picklist
               - workItemTypes tienen baseType de la lista válida
               
               📊 **REPORTE FINAL:**
               Generar reporte con:
               - Número de archivos creados/actualizados
               - Proyectos procesados
               - Tipos de work items encontrados
               - Campos personalizados detectados
               - Errores encontrados y corregidos
               - Próximos pasos recomendados
               
               Este contrato técnico es OBLIGATORIO y garantiza compatibilidad 100% con la aplicación.
            
            **INICIO DEL PROCESO:**
            
            Comienza verificando qué archivos de configuración existen actualmente en config/ 
            y procede con el descubrimiento y generación automática según las especificaciones.
            """;
        
        List<PromptResult.PromptMessage> messages = List.of(
            systemMessage(contextBuilder.toString()),
            userMessage(userPrompt)
        );
        
        return new PromptResult(
            "Generación automática de configuración organizacional para Azure DevOps MCP Server",
            messages
        );
    }
}
