package com.mcp.server.prompts.azuredevops;

/**
 * Especificaciones técnicas obligatorias para los archivos de configuración organizacional.
 * 
 * Esta clase contiene el contrato técnico completo que DEBE cumplir la generación automática
 * de archivos YAML para garantizar compatibilidad 100% con la aplicación.
 * 
 * IMPORTANTE: Este contrato raramente se modifica y define la estructura exacta requerida
 * por OrganizationContextService, OrganizationConfigService y el motor de validación.
 */
public class OrganizationalConfigurationContract {
    
    /**
     * Obtiene las especificaciones técnicas completas para todos los archivos YAML.
     * 
     * @return String con el contrato técnico completo incluyendo:
     *         - Especificaciones de discovered-organization.yml
     *         - Especificaciones de organization-config.yml  
     *         - Especificaciones de field-mappings.yml
     *         - Especificaciones de business-rules.yml
     *         - Tablas de referencia de tipos válidos
     *         - Validaciones críticas y restricciones
     *         - Checklist de validación obligatorio
     */
    public static String getTechnicalContract() {
        return """
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
            """;
    }
    
    /**
     * Obtiene las tablas de referencia para tipos de datos y valores enum válidos.
     * 
     * @return String con las tablas de referencia completas
     */
    public static String getReferenceTablesAndValidations() {
        return """
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
            """;
    }
    
    /**
     * Obtiene el checklist de validación obligatorio y errores críticos a evitar.
     * 
     * @return String con el checklist completo y errores críticos
     */
    public static String getValidationChecklistAndCriticalErrors() {
        return """
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
            """;
    }
    
    /**
     * Obtiene el contrato técnico completo incluyendo especificaciones, tablas de referencia
     * y validaciones críticas.
     * 
     * @return String con todo el contrato técnico para incluir en prompts
     */
    public static String getCompleteContract() {
        return getTechnicalContract() + "\n\n" + 
               getReferenceTablesAndValidations() + "\n\n" + 
               getValidationChecklistAndCriticalErrors();
    }
}
