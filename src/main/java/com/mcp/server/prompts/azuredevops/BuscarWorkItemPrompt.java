package com.mcp.server.prompts.azuredevops;

import com.mcp.server.prompts.base.BasePrompt;
import com.mcp.server.protocol.types.Prompt;
import com.mcp.server.protocol.types.PromptResult;

import java.util.List;
import java.util.Map;

/**
 * Prompt para buscar work items en Azure DevOps por nombre o ID.
 * 
 * Este prompt genera una consulta estructurada para encontrar work items
 * específicos en Azure DevOps, utilizando diferentes estrategias de búsqueda
 * y adaptándose dinámicamente al contexto organizacional mediante las
 * herramientas MCP disponibles.
 */
public class BuscarWorkItemPrompt extends BasePrompt {
    
    public BuscarWorkItemPrompt() {
        super(
            "buscar_workitem",
            "Buscar Work Item",
            "Genera una consulta dinámica para buscar work items en Azure DevOps por ID, nombre o criterios específicos, adaptándose al contexto organizacional mediante las herramientas MCP disponibles.",
            List.of(
                new Prompt.PromptArgument(
                    "criterio_busqueda",
                    "Criterio de Búsqueda",
                    "El criterio de búsqueda: puede ser un ID numérico, texto del título, o descripción del work item",
                    true
                ),
                new Prompt.PromptArgument(
                    "tipo_workitem",
                    "Tipo de Work Item",
                    "Tipo específico de work item a buscar (Historia, Tarea, Bug, etc.). Opcional - si no se especifica, busca en todos los tipos",
                    false
                ),
                new Prompt.PromptArgument(
                    "proyecto",
                    "Proyecto",
                    "Proyecto específico donde buscar. Si no se especifica, busca en todos los proyectos disponibles",
                    false
                ),
                new Prompt.PromptArgument(
                    "estado",
                    "Estado",
                    "Estado del work item (Active, Done, Closed, etc.). Opcional",
                    false
                ),
                new Prompt.PromptArgument(
                    "incluir_detalles",
                    "Incluir Detalles",
                    "Si se deben incluir todos los detalles del work item encontrado",
                    false
                )
            )
        );
    }
    
    @Override
    public PromptResult execute(Map<String, Object> arguments) {
        validateArguments(arguments);
        
        String criterioBusqueda = getRequiredStringArgument(arguments, "criterio_busqueda");
        String tipoWorkItem = getStringArgument(arguments, "tipo_workitem", null);
        String proyecto = getStringArgument(arguments, "proyecto", null);
        String estado = getStringArgument(arguments, "estado", null);
        boolean incluirDetalles = getBooleanArgument(arguments, "incluir_detalles", false);
        
        // Mensaje del sistema estableciendo el contexto
        String systemPrompt = """
            Eres un asistente especializado en Azure DevOps para organizaciones.
            
            Tu tarea es ayudar a buscar y localizar work items específicos en Azure DevOps.
            
            INFORMACIÓN DINÁMICA:
            Antes de realizar cualquier búsqueda, utiliza las herramientas MCP disponibles para obtener
            información actualizada sobre la organización:
            
            1. **Estructura organizacional**: Usa get_help() para obtener contexto organizacional
            2. **Proyectos disponibles**: Usa list_projects() para ver todos los proyectos
            3. **Tipos de work items**: Usa get_workitem_types() para cada proyecto según sea necesario
            
            ESTRATEGIAS DE BÚSQUEDA:
            1. **Búsqueda por ID**: Si el criterio es numérico, usa get_workitem directamente
            2. **Búsqueda por título**: Usa query_workitems con WIQL para buscar en campos Title
            3. **Búsqueda textual**: Busca en múltiples campos usando consultas WIQL
            4. **Búsqueda jerárquica**: Considera la relación padre-hijo entre work items
            
            HERRAMIENTAS DISPONIBLES:
            - get_help: Obtiene información contextual sobre la organización
            - get_workitem: Obtiene un work item específico por ID
            - query_workitems: Ejecuta consultas WIQL para búsquedas complejas
            - list_projects: Lista proyectos disponibles
            - get_workitem_types: Obtiene tipos de work items disponibles por proyecto
            """;
        
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Necesito buscar work items en Azure DevOps con el siguiente criterio: \"")
                  .append(criterioBusqueda).append("\"\n\n");
        
        userPrompt.append("PASOS A SEGUIR:\n");
        userPrompt.append("1. OBTENER CONTEXTO ORGANIZACIONAL:\n");
        userPrompt.append("   - Ejecuta get_help() para obtener información sobre la organización\n");
        userPrompt.append("   - Si es necesario, ejecuta list_projects() para ver proyectos disponibles\n\n");
        
        // Determinar estrategia de búsqueda
        boolean esID = criterioBusqueda.matches("\\d+");
        
        userPrompt.append("2. ESTRATEGIA DE BÚSQUEDA:\n");
        
        if (esID) {
            userPrompt.append("   - El criterio parece ser un ID numérico (").append(criterioBusqueda).append(")\n");
            userPrompt.append("   - Intenta usar get_workitem con este ID\n");
            if (proyecto != null) {
                userPrompt.append("   - Busca en el proyecto especificado: ").append(proyecto).append("\n");
            } else {
                userPrompt.append("   - Si no se especificó proyecto, determina el proyecto apropiado\n");
            }
        } else {
            userPrompt.append("   - El criterio es texto, usa query_workitems con consulta WIQL\n");
            userPrompt.append("   - Busca en los campos Title y Description\n");
            
            // Construir consulta WIQL sugerida
            userPrompt.append("   - Consulta WIQL sugerida:\n");
            StringBuilder wiqlQuery = new StringBuilder();
            wiqlQuery.append("SELECT [System.Id], [System.Title], [System.State], [System.WorkItemType] ");
            wiqlQuery.append("FROM WorkItems WHERE ");
            wiqlQuery.append("([System.Title] CONTAINS '").append(criterioBusqueda).append("' ");
            wiqlQuery.append("OR [System.Description] CONTAINS '").append(criterioBusqueda).append("')");
            
            if (tipoWorkItem != null) {
                wiqlQuery.append(" AND [System.WorkItemType] = '").append(tipoWorkItem).append("'");
            }
            
            if (estado != null) {
                wiqlQuery.append(" AND [System.State] = '").append(estado).append("'");
            }
            
            userPrompt.append("     ```\n     ").append(wiqlQuery.toString()).append("\n     ```\n");
        }
        
        // Filtros adicionales
        userPrompt.append("\n3. FILTROS ADICIONALES:\n");
        if (tipoWorkItem != null) {
            userPrompt.append("   - Filtrar por tipo de work item: ").append(tipoWorkItem).append("\n");
        } else {
            userPrompt.append("   - Sin filtro de tipo específico (buscar en todos los tipos)\n");
        }
        
        if (estado != null) {
            userPrompt.append("   - Filtrar por estado: ").append(estado).append("\n");
        } else {
            userPrompt.append("   - Sin filtro de estado específico\n");
        }
        
        if (proyecto != null) {
            userPrompt.append("   - Buscar solo en el proyecto: ").append(proyecto).append("\n");
        } else {
            userPrompt.append("   - Determinar proyecto(s) apropiado(s) basado en contexto organizacional\n");
        }
        
        // Nivel de detalle
        userPrompt.append("\n4. NIVEL DE DETALLE:\n");
        if (incluirDetalles) {
            userPrompt.append("   - Información completa del work item encontrado\n");
            userPrompt.append("   - Estado actual y historial si está disponible\n");
            userPrompt.append("   - Asignaciones y comentarios\n");
            userPrompt.append("   - Work items relacionados (padre/hijo)\n");
            userPrompt.append("   - Campos personalizados según configuración organizacional\n");
        } else {
            userPrompt.append("   - Información básica (ID, título, estado, tipo, asignado)\n");
        }
        
        userPrompt.append("\n5. PRESENTACIÓN DE RESULTADOS:\n");
        userPrompt.append("   - Proporciona un resumen claro de los work items encontrados\n");
        userPrompt.append("   - Si no se encuentran resultados, sugiere búsquedas alternativas\n");
        userPrompt.append("   - Incluye enlaces a Azure DevOps cuando sea posible\n");
        
        userPrompt.append("\nPor favor, sigue estos pasos utilizando las herramientas MCP disponibles para realizar la búsqueda.");
        
        List<PromptResult.PromptMessage> messages = List.of(
            systemMessage(systemPrompt),
            userMessage(userPrompt.toString())
        );
        
        return new PromptResult(
            "Búsqueda estructurada de work items en Azure DevOps con criterio: " + criterioBusqueda,
            messages
        );
    }
}
