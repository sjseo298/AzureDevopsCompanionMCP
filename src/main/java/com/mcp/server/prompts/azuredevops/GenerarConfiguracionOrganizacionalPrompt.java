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
            "Genera automáticamente la configuración organizacional completa con descubrimiento exhaustivo de Azure DevOps, incluyendo todos los tipos de work items, campos personalizados, valores permitidos y estructura completa de equipos. Siempre incluye el máximo detalle disponible.",
            List.of(
                new Prompt.PromptArgument(
                    "generar_backup",
                    "Generar Backup de Archivos Existentes",
                    "Si se debe hacer backup de archivos de configuración existentes antes de regenerar. Recomendado: true para preservar configuraciones previas",
                    false
                ),
                new Prompt.PromptArgument(
                    "work_item_referencia",
                    "Work Item de Referencia del Usuario",
                    "URL completa o ID del work item que pertenece al equipo donde trabaja el usuario. Se usará para orientar la búsqueda hacia el área path correspondiente (ej: 'https://dev.azure.com/org/project/_workitems/edit/12345' o '12345')",
                    false
                )
            )
        );
    }
    
    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        validateArguments(arguments);
        
        // Solo necesitamos la configuración de backup, todo lo demás es automático con máximo detalle
        boolean generarBackup = getBooleanArgument(arguments, "generar_backup", true);
        String workItemReferencia = getStringArgument(arguments, "work_item_referencia", null);
        
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
            6. `azuredevops_analyze_workitem` - Análisis profundo de work items de referencia
            
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
        
        // Construir contexto específico optimizado para máximo detalle
        StringBuilder contextBuilder = new StringBuilder(systemPrompt);
        
        // Configuración automática optimizada
        contextBuilder.append("\n🔄 **MODO DETALLE COMPLETO**: Generación automática con descubrimiento exhaustivo activado.");
        contextBuilder.append("\n🔍 **DETECCIÓN AUTOMÁTICA**: Procesando automáticamente TODOS los proyectos disponibles.");
        contextBuilder.append("\n📋 **CAMPOS EXTENDIDOS**: Incluyendo información completa de todos los campos (allowedValues, fieldType, validaciones, etc.)");
        
        if (workItemReferencia != null && !workItemReferencia.trim().isEmpty()) {
            contextBuilder.append("\n🎯 **WORK ITEM DE REFERENCIA**: Se usará el work item ").append(workItemReferencia).append(" para orientar la búsqueda hacia el área path correspondiente del usuario.");
        }
        
        if (generarBackup) {
            contextBuilder.append("\n💾 **BACKUP**: Haciendo backup de archivos existentes antes de regenerar.");
        } else {
            contextBuilder.append("\n⚠️ **SIN BACKUP**: No se generará backup de archivos existentes (se sobrescribirán).");
        }
        
        // Prompt simplificado que siempre genera todo el detalle
        String userPrompt = """
            Ejecuta la generación automática COMPLETA de configuración organizacional para Azure DevOps MCP Server.
            
            **CONFIGURACIÓN AUTOMÁTICA - MÁXIMO DETALLE:**
            - ✅ Descubrimiento exhaustivo de TODOS los proyectos
            - ✅ Análisis completo de TODOS los tipos de work items 
            - ✅ Extracción de TODOS los campos personalizados y sus valores permitidos
            - ✅ Documentación completa de estructura organizacional
            - ✅ Generación de todos los archivos de configuración necesarios
            
            **PROCESO AUTOMATIZADO:**
            
            **SI SE PROPORCIONA WORK ITEM DE REFERENCIA:**
            1. 🔍 **ANÁLISIS PROFUNDO DEL WORK ITEM DE REFERENCIA:**
               - Usar `azuredevops_analyze_workitem` con el work item proporcionado
               - Analizar tipo del work item, tipo de su padre e hijos
               - Extraer valores de todos los campos personalizados
               - Identificar patrones de configuración específicos del equipo
               - Obtener información de proyecto, área path y equipo
               
            2. 🎯 **ORIENTAR DESCUBRIMIENTO CON DATOS REALES:**
               - Priorizar el proyecto donde está el work item de referencia
               - Validar campos personalizados descubiertos contra valores reales
               - Usar jerarquía del work item para identificar tipos más utilizados
               - Enfocar la documentación en patrones de configuración detectados
            
            **PROCESO PRINCIPAL:**
            
            1. **DESCUBRIMIENTO ORGANIZACIONAL COMPLETO**
               - Ejecutar `azuredevops_discover_organization` con configuración de máximo detalle:
                 * includeWorkItemTypes: true
                 * includeFieldDetails: true  
                 * includeExtendedInfo: true
                 * backupExistingFiles: """ + generarBackup + """
                 """ + (workItemReferencia != null && !workItemReferencia.trim().isEmpty() ? 
                     "* workItemReferencia: \"" + workItemReferencia + "\" (para orientar búsqueda hacia área path del usuario)" : 
                     "") + """
               - Procesar TODOS los proyectos disponibles automáticamente
               - Extraer TODOS los campos personalizados y valores permitidos
            
            2. **GENERACIÓN AUTOMÁTICA DE ARCHIVOS**
               Crear todos los archivos de configuración con información completa siguiendo 
               estrictamente los contratos técnicos definidos:
               
               **discovered-organization.yml** con:
               - Metadata de descubrimiento (fecha, versión, etc.)
               - Estructura real de proyectos y equipos descubierta dinámicamente
               - Tipos de work items reales con campos específicos de la organización
               - Campos de fecha críticos (StartDate, FinishDate, TargetDate, DueDate)
               - Patrones de nomenclatura reales detectados en la organización
               - Análisis de cadencia organizacional actual
               - **SI HAY WORK ITEM DE REFERENCIA:** Validación de configuración usando datos reales
               
               **organization-config.yml** con:
               - Configuración personalizable adaptada a la organización real
               - Mapeo de campos específicos de la organización descubierta
               - Reglas de negocio específicas
               - Estructura de equipos y proyectos
               - **SI HAY WORK ITEM DE REFERENCIA:** Priorización del proyecto/equipo del usuario
               
               **field-mappings.yml** con:
               - Mapeo detallado de campos personalizados
               - Tipos de datos y validaciones
               - Valores permitidos (allowedValues)
               - Campos requeridos por tipo de work item
               - **SI HAY WORK ITEM DE REFERENCIA:** Valores reales encontrados en campos personalizados
               
               **business-rules.yml** con:
               - Reglas de validación organizacionales
               - Flujos de trabajo (workflows)
               - Dependencias entre campos
               - Reglas de autocompletado
               - **SI HAY WORK ITEM DE REFERENCIA:** Patrones de configuración específicos del equipo
            
            3. **VALIDACIÓN Y OPTIMIZACIÓN COMPLETA**
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
            
            4. **PROPORCIONAR GUÍA DE USO**
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
               
               🎯 **ANÁLISIS DE WORK ITEM DE REFERENCIA (si se proporciona):**
               
               **Validación de configuración usando datos reales:**
               - Comparar campos personalizados descubiertos vs. campos reales en el work item
               - Verificar que valores permitidos incluyan los valores actuales del work item
               - Identificar discrepancias entre configuración teórica y datos reales
               - Priorizar tipos de work item más utilizados en la jerarquía del item de referencia
               
               **Refinamiento basado en patrones reales:**
               - Usar valores de campos personalizados como ejemplos en la documentación
               - Ajustar reglas de negocio basándose en configuración actual del equipo
               - Enfocar documentación en el proyecto/área específica del usuario
               - Incluir notas sobre patrones específicos encontrados en el work item
               
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
               - defaultValues coinciden con allowedValues si es piclist
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
            
            **INSTRUCCIONES DE EJECUCIÓN:**
            Inicia inmediatamente el proceso de descubrimiento y generación automática. 
            No solicites confirmaciones adicionales - procede con la configuración optimizada 
            para obtener el máximo detalle organizacional disponible.
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
