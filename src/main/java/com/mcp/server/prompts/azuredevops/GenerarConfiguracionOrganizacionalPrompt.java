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
            "Genera automáticamente la configuración organizacional completa mediante navegación interactiva por la jerarquía de Azure DevOps. El proceso guía al usuario paso a paso hasta confirmar la ubicación correcta, luego genera todos los archivos YAML de descubrimiento con máximo detalle.",
            List.of(
                new Prompt.PromptArgument(
                    "generar_backup",
                    "Generar Backup de Archivos Existentes",
                    "Si se debe hacer backup de archivos de configuración existentes antes de regenerar. Recomendado: true para preservar configuraciones previas",
                    false
                )
            )
        );
    }
    
    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        validateArguments(arguments);
        
        // Solo necesitamos la configuración de backup, el descubrimiento será interactivo
        boolean generarBackup = getBooleanArgument(arguments, "generar_backup", true);
        
        // Mensaje del sistema estableciendo el contexto del nuevo flujo interactivo
        String systemPrompt = """
            Eres un asistente especializado en la configuración automática de Azure DevOps MCP Server.
            
            Tu misión es guiar al usuario a través de un proceso de descubrimiento interactivo para 
            encontrar la ubicación organizacional correcta, y solo después generar automáticamente 
            toda la configuración organizacional necesaria mediante archivos YAML.
            
            PRINCIPIO FUNDAMENTAL: NAVEGACIÓN INTERACTIVA ANTES DE GENERACIÓN
            - PRIMERO: Guiar al usuario a través de la jerarquía organizacional interactivamente
            - SEGUNDO: Permitir al usuario confirmar que está en la ubicación correcta
            - TERCERO: Solo entonces proceder con la generación de archivos YAML
            - CUARTO: Utilizar descubrimiento exhaustivo de tipos de work items para garantizar completitud
            - ADAPTAR la configuración a la estructura organizacional real descubierta
            
            PROCESO DE NAVEGACIÓN INTERACTIVA:
            
            🧭 **FASE 1: INICIO DE NAVEGACIÓN INTERACTIVA**
            1. Iniciar con nivel organizacional para mostrar proyectos disponibles
            2. Guiar al usuario paso a paso por la jerarquía
            3. Explicar el propósito de cada nivel de navegación
            4. Permitir exploración libre hasta que el usuario esté satisfecho
            
            🔍 **FASE 2: EXPLORACIÓN GUIADA**
            Usar `azuredevops_discover_organization` con navigationLevel step-by-step:
            
            **Nivel 1 - Organización:** 
            - Mostrar todos los proyectos disponibles
            - Explicar qué representa cada proyecto
            - Guiar la selección del proyecto más representativo
            
            **Nivel 2 - Proyecto:**
            - Mostrar equipos y estructura del proyecto seleccionado
            - Analizar tipos de work items disponibles
            - Ofrecer opciones: continuar navegando, hacer preguntas, o confirmar ubicación
            
            **Nivel 3 - Equipo/Área (Opcional):**
            - Mostrar iteraciones y detalles específicos del equipo
            - Analizar contexto específico del equipo
            - Permitir navegación más profunda si es necesario
            
            **Nivel 4 - Iteración (Opcional):**
            - Mostrar detalles específicos de iteraciones
            - Analizar patrones de trabajo específicos
            - Ofrecer análisis contextual detallado
            
            **Nivel 5 - Preguntas (Opcional):**
            - Responder preguntas específicas sobre el contexto actual
            - Proporcionar análisis detallado del área seleccionada
            - Ayudar al usuario a entender mejor la estructura
            
            ✅ **FASE 3: CONFIRMACIÓN DE UBICACIÓN**
            - Usar navigationLevel: "confirm" para verificar que el usuario está satisfecho
            - Si confirmLocation: true → Proceder a generación de archivos
            - Si confirmLocation: false → Continuar navegación interactiva
            - NO generar archivos hasta recibir confirmación explícita
            
            🏗️ **FASE 4: GENERACIÓN DE ARCHIVOS (SOLO DESPUÉS DE CONFIRMACIÓN)**
            Una vez que el usuario confirme la ubicación correcta, usar navigationLevel: "investigation"
            con investigationType: "full-configuration" para generar todos los archivos YAML.
            
            🔧 **FASE 5: VALIDACIÓN Y OPTIMIZACIÓN**
            1. Validar sintaxis YAML generada
            2. Verificar completitud de campos obligatorios
            3. Optimizar configuración para mejores prácticas
            4. Proporcionar resumen de archivos generados
            
            ESPECIFICACIONES TÉCNICAS:
            """ + OrganizationalConfigurationContract.getCompleteContract() + """
            
            CONTEXTO DE EJECUCIÓN INTERACTIVA:
            """;
        
        // Construir contexto específico para navegación interactiva
        StringBuilder contextBuilder = new StringBuilder(systemPrompt);
        
        // Configuración para navegación interactiva
        contextBuilder.append("\n🧭 **MODO NAVEGACIÓN INTERACTIVA**: El usuario será guiado paso a paso por la jerarquía organizacional.");
        contextBuilder.append("\n✋ **CONFIRMACIÓN REQUERIDA**: NO generar archivos hasta que el usuario confirme la ubicación correcta.");
        contextBuilder.append("\n📋 **DETALLE COMPLETO**: Una vez confirmado, incluir información exhaustiva de todos los campos.");
        
        if (generarBackup) {
            contextBuilder.append("\n💾 **BACKUP**: Hacer backup de archivos existentes antes de regenerar.");
        } else {
            contextBuilder.append("\n⚠️ **SIN BACKUP**: No generar backup de archivos existentes (se sobrescribirán).");
        }
        
        // Prompt actualizado para flujo interactivo
        String userPrompt = """
            Inicia el proceso de generación de configuración organizacional mediante NAVEGACIÓN INTERACTIVA.
            
            **PROCESO INTERACTIVO PASO A PASO:**
            
            🧭 **PASO 1: INICIO DE NAVEGACIÓN**
            Comenzar con el nivel organizacional para mostrar la estructura disponible:
            
            ```
            azuredevops_discover_organization(
              navigationLevel: "organization"
            )
            ```
            
            **Objetivos del paso inicial:**
            - Mostrar todos los proyectos disponibles en la organización
            - Proporcionar información básica de cada proyecto
            - Explicar al usuario las opciones de navegación disponibles
            - Guiar la selección del proyecto más representativo para su trabajo
            
            🔍 **PASOS SIGUIENTES OPCIONALES:**
            
            **Si el usuario quiere explorar un proyecto específico:**
            ```
            azuredevops_discover_organization(
              navigationLevel: "project",
              selectedProject: "[NOMBRE_DEL_PROYECTO]"
            )
            ```
            
            **Si el usuario quiere navegar por equipos:**
            ```
            azuredevops_discover_organization(
              navigationLevel: "team",
              selectedProject: "[PROYECTO]",
              selectedTeam: "[EQUIPO]"
            )
            ```
            
            **Si el usuario quiere hacer preguntas contextuales:**
            ```
            azuredevops_discover_organization(
              navigationLevel: "question",
              selectedProject: "[PROYECTO]",
              questionType: "[TIPO_DE_PREGUNTA]"
            )
            ```
            
            ✅ **CONFIRMACIÓN DE UBICACIÓN (CRÍTICO):**
            Cuando el usuario indique que está en la ubicación correcta:
            ```
            azuredevops_discover_organization(
              navigationLevel: "confirm",
              selectedProject: "[PROYECTO]",
              selectedTeam: "[EQUIPO_SI_APLICA]",
              selectedAreaPath: "[AREA_SI_APLICA]",
              confirmLocation: true
            )
            ```
            
            🏗️ **GENERACIÓN DE ARCHIVOS (SOLO DESPUÉS DE CONFIRMACIÓN):**
            Una vez confirmado, generar la configuración completa:
            ```
            azuredevops_discover_organization(
              navigationLevel: "investigation",
              selectedProject: "[PROYECTO_CONFIRMADO]",
              selectedTeam: "[EQUIPO_CONFIRMADO]",
              selectedAreaPath: "[AREA_CONFIRMADA]",
              investigationType: "full-configuration",
              backupExistingFiles: """ + generarBackup + """
            )
            ```
            
            **INSTRUCCIONES ESPECÍFICAS:**
            
            1. **COMENZAR SIEMPRE** con navigationLevel: "organization"
            2. **GUIAR AL USUARIO** a través de cada nivel explicando las opciones
            3. **NO ASUMIR** qué proyecto/equipo/área es el correcto
            4. **PERMITIR EXPLORACIÓN LIBRE** hasta que el usuario esté satisfecho
            5. **REQUERIR CONFIRMACIÓN EXPLÍCITA** antes de generar archivos
            6. **GENERAR ARCHIVOS COMPLETOS** solo después de confirmación
            
            **MENSAJERÍA AL USUARIO:**
            
            📢 **Explicar el proceso:**
            "Vamos a navegar interactivamente por su organización de Azure DevOps para encontrar 
            la ubicación más representativa de su trabajo. Una vez que confirme la ubicación correcta, 
            generaremos automáticamente todos los archivos de configuración necesarios."
            
            🎯 **Propósito de cada nivel:**
            - **Organización**: Ver todos los proyectos disponibles
            - **Proyecto**: Analizar equipos y tipos de work items
            - **Equipo**: Explorar iteraciones y contexto específico
            - **Preguntas**: Obtener análisis detallado del contexto actual
            - **Confirmación**: Verificar que es la ubicación correcta
            - **Investigación**: Generar archivos YAML completos
            
            **VALIDACIONES CRÍTICAS:**
            
            ❌ **NO generar archivos sin confirmación explícita del usuario**
            ❌ **NO asumir que el primer proyecto es el correcto**  
            ❌ **NO proceder directamente a investigación sin navegación**
            ✅ **SÍ permitir exploración completa de la jerarquía**
            ✅ **SÍ explicar cada paso y sus opciones**
            ✅ **SÍ requerir confirmación antes de generar archivos**
            
            **INICIO INMEDIATO:**
            Comienza ahora con el nivel organizacional y guía al usuario a través del proceso interactivo.
            """;
        
        List<PromptResult.PromptMessage> messages = List.of(
            systemMessage(contextBuilder.toString()),
            userMessage(userPrompt)
        );
        
        return new PromptResult(
            "Generación interactiva de configuración organizacional para Azure DevOps MCP Server",
            messages
        );
    }
}
