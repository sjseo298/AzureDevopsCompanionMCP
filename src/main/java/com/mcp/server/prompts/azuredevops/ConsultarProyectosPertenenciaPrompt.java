package com.mcp.server.prompts.azuredevops;

import com.mcp.server.prompts.base.BasePrompt;
import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.List;
import java.util.Map;

/**
 * Prompt para consultar y analizar la pertenencia a proyectos en Azure DevOps.
 * 
 * Este prompt ayuda a determinar a qué proyectos y equipos pertenece un usuario
 * específico en la organización Sura, proporcionando un análisis detallado
 * de la estructura organizacional y permisos de acceso.
 */
public class ConsultarProyectosPertenenciaPrompt extends BasePrompt {
    
    public ConsultarProyectosPertenenciaPrompt() {
        super(
            "consultar_proyectos_pertenencia",
            "Consultar Pertenencia a Proyectos",
            "Consulta y analiza a qué proyectos y equipos pertenece un usuario específico en Azure DevOps de Sura, incluyendo análisis de permisos y estructura organizacional.",
            List.of(
                new Prompt.PromptArgument(
                    "usuario",
                    "Usuario",
                    "Nombre o email del usuario para consultar su pertenencia a proyectos (opcional - si no se especifica, consulta el usuario actual)",
                    false
                ),
                new Prompt.PromptArgument(
                    "incluir_equipos",
                    "Incluir Equipos",
                    "Si se deben listar también los equipos de cada proyecto donde el usuario tiene acceso",
                    false
                ),
                new Prompt.PromptArgument(
                    "incluir_work_items",
                    "Incluir Work Items",
                    "Si se deben mostrar los work items asignados al usuario en cada proyecto",
                    false
                )
            )
        );
    }
    
    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        validateArguments(arguments);
        
        String usuario = getStringArgument(arguments, "usuario", null);
        boolean incluirEquipos = getBooleanArgument(arguments, "incluir_equipos", true);
        boolean incluirWorkItems = getBooleanArgument(arguments, "incluir_work_items", false);
        
        // Mensaje del sistema estableciendo el contexto
        String systemPrompt = """
            Eres un asistente especializado en Azure DevOps para la organización Sura.
            
            Tu tarea es ayudar a consultar y analizar la pertenencia a proyectos en Azure DevOps.
            
            ESTRUCTURA ORGANIZACIONAL DE SURA:
            
            Sura maneja tres proyectos principales en Azure DevOps:
            1. **Gerencia_Tecnologia** - Proyecto principal de tecnología
            2. **Gerencia_Tecnologia_Egv_Aseguramiento** - Proyecto de aseguramiento y calidad
            3. **Portafolios** - Proyecto de gestión de portafolios

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
        
        StringBuilder userPrompt = new StringBuilder();
        
        if (usuario != null && !usuario.trim().isEmpty()) {
            userPrompt.append("Quiero consultar a qué proyectos pertenece el usuario: ")
                     .append(usuario).append(" en Azure DevOps de Sura.\n\n");
        } else {
            userPrompt.append("Quiero consultar a qué proyectos pertenezco en Azure DevOps de Sura.\n\n");
        }
        
        userPrompt.append("Por favor:\n");
        userPrompt.append("1. Lista todos los proyectos disponibles usando list_projects\n");
        
        if (incluirEquipos) {
            userPrompt.append("2. Para cada proyecto, lista los equipos usando list_teams\n");
            userPrompt.append("3. Identifica en qué equipos tiene permisos");
            if (usuario != null) {
                userPrompt.append(" el usuario ").append(usuario);
            }
            userPrompt.append("\n");
        }
        
        if (incluirWorkItems) {
            userPrompt.append("4. Busca work items asignados");
            if (usuario != null) {
                userPrompt.append(" al usuario ").append(usuario);
            }
            userPrompt.append(" usando query_workitems con consultas WIQL\n");
            userPrompt.append("5. Analiza la distribución de trabajo por proyecto\n");
        }
        
        userPrompt.append("4. Proporciona un resumen de los proyectos encontrados\n");
        
        // Estrategias de búsqueda específicas
        if (usuario != null && !usuario.trim().isEmpty()) {
            userPrompt.append("\nESTRATEGIAS DE BÚSQUEDA PARA EL USUARIO:\n");
            userPrompt.append("- Buscar work items asignados directamente\n");
            userPrompt.append("- Verificar en historial de creación y modificación\n");
            userPrompt.append("- Comprobar variantes del nombre (con/sin dominio, diferentes formatos)\n");
            userPrompt.append("- Analizar pertenencia a equipos específicos\n");
        }
        
        userPrompt.append("\nRecuerda usar las herramientas MCP disponibles para obtener información actualizada directamente de Azure DevOps.");
        
        List<PromptResult.PromptMessage> messages = List.of(
            systemMessage(systemPrompt),
            userMessage(userPrompt.toString())
        );
        
        return new PromptResult(
            "Consulta de pertenencia a proyectos en Azure DevOps de Sura" + 
            (usuario != null ? " para usuario: " + usuario : ""),
            messages
        );
    }
}
