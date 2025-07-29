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
 * como ID directo, búsqueda por título, o consultas WIQL avanzadas.
 */
public class BuscarWorkItemPrompt extends BasePrompt {
    
    public BuscarWorkItemPrompt() {
        super(
            "buscar_workitem",
            "Buscar Work Item",
            "Genera una consulta para buscar work items en Azure DevOps por ID, nombre o criterios específicos, utilizando las herramientas MCP disponibles para obtener información detallada.",
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
                    "Proyecto específico donde buscar. Si no se especifica, busca en todos los proyectos de YOUR_ORGANIZATION",
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
            Eres un asistente especializado en Azure DevOps para la organización YOUR_ORGANIZATION.
            
            Tu tarea es ayudar a buscar y localizar work items específicos en Azure DevOps.
            
            ESTRUCTURA ORGANIZACIONAL DE SURA:
            
            YOUR_ORGANIZATION maneja tres proyectos principales en Azure DevOps:
            1. **Gerencia_Tecnologia** - Proyecto principal de tecnología y desarrollo
            2. **Gerencia_Tecnologia_Egv_Aseguramiento** - Proyecto de aseguramiento y calidad
            3. **Portafolios** - Proyecto de gestión de portafolios e iniciativas estratégicas
            
            JERARQUÍA DE WORK ITEMS EN SURA:
            - **Proyecto**: Nivel más alto, agrupa iniciativas grandes (ej: "Remediación GW - 2025")
            - **Historia**: Funcionalidades del usuario y requerimientos de negocio
            - **Historia técnica**: Tareas técnicas de desarrollo e implementación
            - **Tarea**: Tareas generales y actividades específicas
            - **Subtarea**: Tareas granulares dentro de historias o tareas
            - **Bug**: Defectos y errores identificados
            - **Riesgo**: Gestión y seguimiento de riesgos
            
            ESTRUCTURA DE ÁREAS ORGANIZACIONALES:
            - Areas como "do-asegur-plan_de_remediacion" indican: dominio-función-iniciativa
            - Prefijos comunes: do- (dominio), plan_de_ (planes específicos)
            - Estructura jerárquica: Gerencia_Tecnologia\\{dominio}\\{función}\\{iniciativa}
            
            PATRONES DE NOMENCLATURA:
            - [PETID] y [NO PETID]: Clasificación de proyectos por tipo de demanda
            - Años en títulos (2024, 2025): Indican el período de ejecución
            - Estados comunes: New, En progreso, Cerrado, Planeado
            
            ESTRATEGIAS DE BÚSQUEDA:
            1. **Búsqueda por ID**: Si el criterio es numérico, usa get_workitem directamente
            2. **Búsqueda por título**: Usa query_workitems con WIQL para buscar en campos Title
            3. **Búsqueda textual**: Busca en múltiples campos usando consultas WIQL
            4. **Búsqueda jerárquica**: Considera la relación padre-hijo entre work items
            
            HERRAMIENTAS DISPONIBLES:
            - get_workitem: Obtiene un work item específico por ID
            - query_workitems: Ejecuta consultas WIQL para búsquedas complejas
            - list_projects: Lista proyectos disponibles
            """;
        
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Necesito buscar work items en Azure DevOps con el siguiente criterio: \"")
                  .append(criterioBusqueda).append("\"\n\n");
        
        // Determinar estrategia de búsqueda
        boolean esID = criterioBusqueda.matches("\\d+");
        
        userPrompt.append("ESTRATEGIA DE BÚSQUEDA:\n");
        
        if (esID) {
            userPrompt.append("1. El criterio parece ser un ID numérico (").append(criterioBusqueda).append(")\n");
            userPrompt.append("2. Intenta primero usar get_workitem con este ID\n");
            if (proyecto != null) {
                userPrompt.append("3. Busca en el proyecto: ").append(proyecto).append("\n");
            } else {
                userPrompt.append("3. Si no lo encuentra, intenta en todos los proyectos de YOUR_ORGANIZATION\n");
            }
        } else {
            userPrompt.append("1. El criterio es texto, usa query_workitems con consulta WIQL\n");
            userPrompt.append("2. Busca en los campos Title y Description\n");
            
            // Construir consulta WIQL sugerida
            userPrompt.append("3. Consulta WIQL sugerida:\n");
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
            
            userPrompt.append("   ```\n   ").append(wiqlQuery.toString()).append("\n   ```\n");
        }
        
        // Filtros adicionales
        if (tipoWorkItem != null) {
            userPrompt.append("4. Filtrar por tipo de work item: ").append(tipoWorkItem).append("\n");
        }
        
        if (estado != null) {
            userPrompt.append("5. Filtrar por estado: ").append(estado).append("\n");
        }
        
        if (proyecto != null) {
            userPrompt.append("6. Buscar solo en el proyecto: ").append(proyecto).append("\n");
        } else {
            userPrompt.append("6. Buscar en todos los proyectos de YOUR_ORGANIZATION\n");
        }
        
        // Nivel de detalle
        if (incluirDetalles) {
            userPrompt.append("\nNIVEL DE DETALLE REQUERIDO:\n");
            userPrompt.append("- Información completa del work item\n");
            userPrompt.append("- Estado actual y historial de cambios\n");
            userPrompt.append("- Asignaciones y comentarios\n");
            userPrompt.append("- Work items relacionados (padre/hijo)\n");
            userPrompt.append("- Campos personalizados de YOUR_ORGANIZATION\n");
        } else {
            userPrompt.append("\nNIVEL DE DETALLE: Información básica (ID, título, estado, tipo, asignado)\n");
        }
        
        userPrompt.append("\nPor favor, utiliza las herramientas MCP disponibles para realizar la búsqueda y proporciona los resultados encontrados.");
        
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
