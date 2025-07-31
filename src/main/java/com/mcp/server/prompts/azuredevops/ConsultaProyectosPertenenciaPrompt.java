package com.mcp.server.prompts.azuredevops;

import com.mcp.server.prompts.base.BasePrompt;
import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.List;
import java.util.Map;

/**
 * Prompt para consultar a qué proyectos pertenece el usuario en Azure DevOps.
 * 
 * Este prompt genera una consulta estructurada para identificar todos los
 * proyectos de Azure DevOps a los que el usuario tiene acceso, adaptándose
 * a la estructura organizacional configurada.
 */
public class ConsultaProyectosPertenenciaPrompt extends BasePrompt {
    
    public ConsultaProyectosPertenenciaPrompt() {
        super(
            "consulta_proyectos_pertenencia",
            "Consulta de Proyectos de Pertenencia",
            "Genera una consulta para identificar todos los proyectos de Azure DevOps a los que pertenece el usuario, adaptándose a la estructura organizacional configurada.",
            List.of(
                new Prompt.PromptArgument(
                    "usuario",
                    "Usuario",
                    "Nombre del usuario para consultar sus proyectos (opcional, se usa @Me si no se especifica)",
                    false
                ),
                new Prompt.PromptArgument(
                    "incluir_equipos",
                    "Incluir Equipos",
                    "Si se deben incluir los equipos de cada proyecto en la respuesta",
                    false
                ),
                new Prompt.PromptArgument(
                    "formato_detallado",
                    "Formato Detallado",
                    "Si se debe proporcionar información detallada de cada proyecto",
                    false
                )
            )
        );
    }
    
    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        validateArguments(arguments);
        
        String usuario = getStringArgument(arguments, "usuario", "@Me");
        boolean incluirEquipos = getBooleanArgument(arguments, "incluir_equipos", false);
        boolean formatoDetallado = getBooleanArgument(arguments, "formato_detallado", false);
        
        StringBuilder promptText = new StringBuilder();
        
        // Mensaje del sistema estableciendo el contexto
        String systemPrompt = """
            Eres un asistente especializado en Azure DevOps para organizaciones.
            
            Tu tarea es ayudar a consultar y analizar la pertenencia a proyectos en Azure DevOps.
            
            ESTRUCTURA ORGANIZACIONAL:
            
            La organización puede manejar múltiples proyectos en Azure DevOps con diferentes
            estructuras y equipos según su configuración específica.
            
            PREFIJOS DE EQUIPOS:
            - EQP_xxx: Equipos de desarrollo específicos
            - PROJ_xxx: Equipos de proyecto
            - AREA_xxx: Equipos por área funcional
            
            HERRAMIENTAS DISPONIBLES:
            - list_projects: Lista todos los proyectos disponibles
            - list_teams: Lista equipos de un proyecto específico
            - query_workitems: Ejecuta consultas WIQL para work items
            - get_assigned_work: Obtiene work items asignados al usuario
            """;
        
        // Mensaje del usuario con la consulta específica
        promptText.append("Quiero consultar a qué proyectos pertenece ");
        if ("@Me".equals(usuario)) {
            promptText.append("mi usuario");
        } else {
            promptText.append("el usuario: ").append(usuario);
        }
        promptText.append(" en Azure DevOps.\n\n");
        
        promptText.append("Por favor:\n");
        promptText.append("1. Lista todos los proyectos disponibles usando list_projects\n");
        
        if (incluirEquipos) {
            promptText.append("2. Para cada proyecto encontrado, lista los equipos usando list_teams\n");
            promptText.append("3. Identifica en qué equipos está el usuario\n");
        }
        
        if (formatoDetallado) {
            promptText.append("4. Proporciona información detallada de cada proyecto:\n");
            promptText.append("   - Nombre y descripción del proyecto\n");
            promptText.append("   - Equipos a los que pertenece el usuario\n");
            promptText.append("   - Rol o permisos en cada equipo\n");
            promptText.append("5. Analiza qué tipo de trabajo realiza el usuario en cada proyecto\n");
        } else {
            promptText.append("4. Proporciona un resumen de los proyectos encontrados\n");
        }
        
        promptText.append("\nRecuerda usar las herramientas MCP disponibles para obtener información actualizada directamente de Azure DevOps.");
        
        List<PromptResult.PromptMessage> messages = List.of(
            systemMessage(systemPrompt),
            userMessage(promptText.toString())
        );
        
        return new PromptResult(
            "Consulta estructurada para identificar proyectos de pertenencia del usuario en Azure DevOps",
            messages
        );
    }
}
