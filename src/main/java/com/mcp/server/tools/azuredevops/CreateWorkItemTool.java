package com.mcp.server.tools.azuredevops;

import com.mcp.server.tools.azuredevops.client.AzureDevOpsClient;
import com.mcp.server.tools.azuredevops.client.AzureDevOpsException;
import com.mcp.server.tools.azuredevops.model.WorkItem;
import com.mcp.server.service.workitem.GenericWorkItemFieldsHandler;
import com.mcp.server.tools.base.McpTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Herramienta MCP para crear work items en Azure DevOps.
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class CreateWorkItemTool implements McpTool {
    
    private final AzureDevOpsClient azureDevOpsClient;
    private final GenericWorkItemFieldsHandler fieldsHandler;
    
    @Autowired
    public CreateWorkItemTool(AzureDevOpsClient azureDevOpsClient, GenericWorkItemFieldsHandler fieldsHandler) {
        this.azureDevOpsClient = azureDevOpsClient;
        this.fieldsHandler = fieldsHandler;
    }
    
    @Override
    public String getName() {
        return "azuredevops_create_workitem";
    }
    
    @Override
    public String getDescription() {
        return "Crea un nuevo work item en Azure DevOps. Soporta tipos como Task, User Story, Bug, Feature, etc.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> properties = new HashMap<>();
        
        // Campos básicos
        properties.put("project", Map.of(
            "type", "string",
            "description", "Nombre o ID del proyecto (requerido solo si no se proporciona parentId)"
        ));
        properties.put("type", Map.of(
            "type", "string",
            "description", "Tipo de work item (Historia, Historia técnica, Tarea, Bug, etc.)"
        ));
        properties.put("title", Map.of(
            "type", "string",
            "description", "Título del work item (obligatorio)"
        ));
        properties.put("description", Map.of(
            "type", "string",
            "description", "Descripción detallada del work item (obligatorio para Historia, Bug, Riesgo)"
        ));
        
        // Campos de sistema opcionales
        properties.put("assignedTo", Map.of(
            "type", "string",
            "description", "Email o nombre del usuario asignado (opcional)"
        ));
        properties.put("iterationPath", Map.of(
            "type", "string",
            "description", "Ruta de iteración (ej: 'MyProject\\Sprint 5') (opcional, se obtiene del padre si se proporciona parentId)"
        ));
        properties.put("areaPath", Map.of(
            "type", "string",
            "description", "Ruta de área (opcional, se obtiene del padre si se proporciona parentId)"
        ));
        properties.put("parentId", Map.of(
            "type", "number",
            "description", "ID del work item padre para crear jerarquía (opcional). Si se proporciona, el proyecto, área e iteración se obtendrán automáticamente del padre."
        ));
        
        // Campos organizacionales
        properties.put("acceptanceCriteria", Map.of(
            "type", "string",
            "description", "Criterios de aceptación (obligatorio para Historia/Historia técnica)"
        ));
        properties.put("tipoHistoria", Map.of(
            "type", "string",
            "description", "Tipo de historia (obligatorio para Historia)"
        ));
        properties.put("tipoHistoriaTecnica", Map.of(
            "type", "string",
            "description", "Tipo de historia técnica (obligatorio para Historia técnica)"
        ));
        properties.put("tipoTarea", Map.of(
            "type", "string",
            "description", "Tipo de tarea (obligatorio para Tarea)"
        ));
        properties.put("tipoSubtarea", Map.of(
            "type", "string",
            "description", "Tipo de subtarea (obligatorio para Subtarea)"
        ));
        properties.put("idSolucionAPM", Map.of(
            "type", "string",
            "description", "ID de la solución en el APM (obligatorio para Historia/Historia técnica/Bug)"
        ));
        properties.put("migracionDatos", Map.of(
            "type", "boolean",
            "description", "¿Hace parte de migración de datos? (obligatorio para Historia/Historia técnica)"
        ));
        properties.put("cumplimientoRegulatorio", Map.of(
            "type", "boolean",
            "description", "¿Es cumplimiento regulatorio? (obligatorio para Historia/Historia técnica)"
        ));
        properties.put("controlAutomatico", Map.of(
            "type", "boolean",
            "description", "¿Es control automático? (obligatorio para Historia/Historia técnica)"
        ));
        
        // Campos específicos para Bug
        properties.put("reproSteps", Map.of(
            "type", "string",
            "description", "Pasos para reproducir (obligatorio para Bug)"
        ));
        properties.put("datosPrueba", Map.of(
            "type", "string",
            "description", "Datos de prueba (obligatorio para Bug)"
        ));
        properties.put("bloqueante", Map.of(
            "type", "boolean",
            "description", "¿Es bloqueante? (obligatorio para Bug)"
        ));
        properties.put("nivelPrueba", Map.of(
            "type", "string",
            "description", "Nivel de prueba (obligatorio para Bug/Caso de prueba)"
        ));
        properties.put("origen", Map.of(
            "type", "string",
            "description", "Origen del bug (obligatorio para Bug)"
        ));
        properties.put("etapaDescubrimiento", Map.of(
            "type", "string",
            "description", "Etapa de descubrimiento (obligatorio para Bug)"
        ));
        
        // Campos adicionales
        properties.put("remainingWork", Map.of(
            "type", "number",
            "description", "Trabajo restante estimado en horas (opcional)"
        ));
        properties.put("storyPoints", Map.of(
            "type", "number",
            "description", "Story points para historias de usuario (opcional)"
        ));
        properties.put("priority", Map.of(
            "type", "number",
            "description", "Prioridad del trabajo (1-4, donde 1 es la más alta) (opcional)"
        ));
        properties.put("tags", Map.of(
            "type", "string",
            "description", "Tags separados por punto y coma (ej: 'urgent;bug-fix') (opcional)"
        ));
        
        return Map.of(
            "type", "object",
            "properties", properties,
            "required", List.of("type", "title")
        );
    }
    
    @Override
    public Map<String, Object> execute(Map<String, Object> arguments) {
        try {
            if (!azureDevOpsClient.isConfigured()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "Azure DevOps no está configurado. Necesita configurar AZURE_DEVOPS_ORGANIZATION y AZURE_DEVOPS_PAT."
                    )),
                    "isError", true
                );
            }
            
            // Extraer parámetros básicos
            String project = (String) arguments.get("project");
            String type = (String) arguments.get("type");
            String title = (String) arguments.get("title");
            String description = (String) arguments.get("description");
            String assignedTo = (String) arguments.get("assignedTo");
            String iterationPath = (String) arguments.get("iterationPath");
            String areaPath = (String) arguments.get("areaPath");
            Number parentIdNum = (Number) arguments.get("parentId");
            
            // Si se proporciona parentId, obtener información del work item padre
            if (parentIdNum != null) {
                try {
                    Map<String, Object> parentWorkItem = azureDevOpsClient.getWorkItem(parentIdNum.intValue());
                    if (parentWorkItem != null && parentWorkItem.containsKey("fields")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parentFields = (Map<String, Object>) parentWorkItem.get("fields");
                        
                        // Extraer proyecto del work item padre
                        String parentAreaPath = (String) parentFields.get("System.AreaPath");
                        if (parentAreaPath != null && !parentAreaPath.trim().isEmpty()) {
                            // El área path generalmente empieza con el nombre del proyecto
                            String[] pathParts = parentAreaPath.split("\\\\");
                            if (pathParts.length > 0) {
                                project = pathParts[0];
                                System.out.println("🔍 Proyecto obtenido del feature #" + parentIdNum + ": " + project);
                            }
                        }
                        
                        // Si no se especificó área path, usar la del padre
                        if (areaPath == null || areaPath.trim().isEmpty()) {
                            areaPath = parentAreaPath;
                            System.out.println("📁 Área path obtenida del padre: " + areaPath);
                        }
                        
                        // Si no se especificó iteración, usar la del padre
                        if (iterationPath == null || iterationPath.trim().isEmpty()) {
                            String parentIterationPath = (String) parentFields.get("System.IterationPath");
                            if (parentIterationPath != null && !parentIterationPath.trim().isEmpty()) {
                                iterationPath = parentIterationPath;
                                System.out.println("🔄 Iteración obtenida del padre: " + iterationPath);
                            }
                        }
                        
                        // Verificar que obtuvimos el proyecto
                        if (project == null || project.trim().isEmpty()) {
                            return Map.of(
                                "content", List.of(Map.of(
                                    "type", "text",
                                    "text", "❌ Error: No se pudo determinar el proyecto del work item padre #" + parentIdNum + ". El área path del padre es: " + parentAreaPath
                                )),
                                "isError", true
                            );
                        }
                    } else {
                        return Map.of(
                            "content", List.of(Map.of(
                                "type", "text",
                                "text", "❌ Error: No se pudo obtener información del work item padre #" + parentIdNum + ". Verifique que el work item existe y tiene permisos para accederlo."
                            )),
                            "isError", true
                        );
                    }
                } catch (Exception e) {
                    return Map.of(
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", "❌ Error al obtener información del work item padre #" + parentIdNum + ": " + e.getMessage() + "\n\n💡 Sugerencia: Proporcione el parámetro 'project' manualmente."
                        )),
                        "isError", true
                    );
                }
            }
            
            // Extraer campos organizacionales
            String acceptanceCriteria = (String) arguments.get("acceptanceCriteria");
            String tipoHistoria = (String) arguments.get("tipoHistoria");
            String tipoHistoriaTecnica = (String) arguments.get("tipoHistoriaTecnica");
            String tipoTarea = (String) arguments.get("tipoTarea");
            String tipoSubtarea = (String) arguments.get("tipoSubtarea");
            String idSolucionAPM = (String) arguments.get("idSolucionAPM");
            Boolean migracionDatos = (Boolean) arguments.get("migracionDatos");
            Boolean cumplimientoRegulatorio = (Boolean) arguments.get("cumplimientoRegulatorio");
            Boolean controlAutomatico = (Boolean) arguments.get("controlAutomatico");
            String reproSteps = (String) arguments.get("reproSteps");
            String datosPrueba = (String) arguments.get("datosPrueba");
            Boolean bloqueante = (Boolean) arguments.get("bloqueante");
            String nivelPrueba = (String) arguments.get("nivelPrueba");
            String origen = (String) arguments.get("origen");
            String etapaDescubrimiento = (String) arguments.get("etapaDescubrimiento");
            
            // Campos adicionales
            Number remainingWorkNum = (Number) arguments.get("remainingWork");
            Number storyPointsNum = (Number) arguments.get("storyPoints");
            Number priorityNum = (Number) arguments.get("priority");
            String tags = (String) arguments.get("tags");
            
            // Validaciones básicas
            if (parentIdNum == null && (project == null || project.trim().isEmpty())) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "❌ Error: Debe proporcionar 'project' O 'parentId'. Si proporciona 'parentId', el proyecto se obtendrá automáticamente del work item padre."
                    )),
                    "isError", true
                );
            }
            
            // Si tenemos parentId pero no proyecto, intentar obtenerlo del padre
            if (parentIdNum != null && (project == null || project.trim().isEmpty())) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "⚠️ Error: No se pudo obtener el proyecto del work item padre #" + parentIdNum + ". Esto no debería suceder si el código funcionó correctamente anteriormente."
                    )),
                    "isError", true
                );
            }
            
            if (type == null || type.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'type' es requerido"
                    )),
                    "isError", true
                );
            }
            
            if (title == null || title.trim().isEmpty()) {
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", "El parámetro 'title' es requerido"
                    )),
                    "isError", true
                );
            }
            
            // Construir operaciones JSON Patch básicas
            List<Map<String, Object>> operations = new ArrayList<>();
            
            // Campo obligatorio principal
            operations.add(Map.of(
                "op", "add",
                "path", "/fields/System.Title",
                "value", title
            ));
            
            // Preparar valores personalizados para campos obligatorios
            Map<String, Object> userProvidedValues = new HashMap<>();
            
            // Mapear campos básicos del sistema
            if (description != null && !description.trim().isEmpty()) {
                userProvidedValues.put("System.Description", description);
            }
            if (assignedTo != null && !assignedTo.trim().isEmpty()) {
                userProvidedValues.put("System.AssignedTo", assignedTo);
            }
            if (iterationPath != null && !iterationPath.trim().isEmpty()) {
                userProvidedValues.put("System.IterationPath", iterationPath);
            }
            if (areaPath != null && !areaPath.trim().isEmpty()) {
                userProvidedValues.put("System.AreaPath", areaPath);
            }
            
            // Mapear campos organizacionales
            if (acceptanceCriteria != null && !acceptanceCriteria.trim().isEmpty()) {
                userProvidedValues.put("Microsoft.VSTS.Common.AcceptanceCriteria", acceptanceCriteria);
            }
            if (tipoHistoria != null && !tipoHistoria.trim().isEmpty()) {
                userProvidedValues.put("Custom.TipoDeHistoria", tipoHistoria);
            }
            if (tipoHistoriaTecnica != null && !tipoHistoriaTecnica.trim().isEmpty()) {
                userProvidedValues.put("Custom.14858558-3edb-485a-9a52-a38c03c65c62", tipoHistoriaTecnica);
            }
            if (tipoTarea != null && !tipoTarea.trim().isEmpty()) {
                userProvidedValues.put("Custom.TipoDeTarea", tipoTarea);
            }
            if (tipoSubtarea != null && !tipoSubtarea.trim().isEmpty()) {
                userProvidedValues.put("Custom.TipoDeSubtarea", tipoSubtarea);
            }
            if (idSolucionAPM != null && !idSolucionAPM.trim().isEmpty()) {
                userProvidedValues.put("Custom.9fcf5e7b-aac8-44a0-9476-653d3ea45e14", idSolucionAPM);
            }
            if (migracionDatos != null) {
                userProvidedValues.put("Custom.78e00118-cbf0-42f1-bee1-269ea2a2dba3", migracionDatos ? "Si" : "No");
            }
            if (cumplimientoRegulatorio != null) {
                userProvidedValues.put("Custom.Lahistoriacorrespondeauncumplimientoregulatorio", cumplimientoRegulatorio ? "Si" : "No");
            }
            if (controlAutomatico != null) {
                userProvidedValues.put("Custom.5480ef11-38bf-4233-a94b-3fdd32107eb1", controlAutomatico ? "Si" : "No");
            }
            if (reproSteps != null && !reproSteps.trim().isEmpty()) {
                userProvidedValues.put("Microsoft.VSTS.TCM.ReproSteps", reproSteps);
            }
            if (datosPrueba != null && !datosPrueba.trim().isEmpty()) {
                userProvidedValues.put("Custom.DatosDePrueba", datosPrueba);
            }
            if (bloqueante != null) {
                userProvidedValues.put("Custom.Bloqueante", bloqueante);
            }
            if (nivelPrueba != null && !nivelPrueba.trim().isEmpty()) {
                userProvidedValues.put("Custom.NivelPrueba", nivelPrueba);
            }
            if (origen != null && !origen.trim().isEmpty()) {
                userProvidedValues.put("Custom.Origen", origen);
            }
            if (etapaDescubrimiento != null && !etapaDescubrimiento.trim().isEmpty()) {
                userProvidedValues.put("Custom.EtapaDescubrimiento", etapaDescubrimiento);
            }
            
            // Campos opcionales adicionales
            if (remainingWorkNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Scheduling.RemainingWork",
                    "value", remainingWorkNum.doubleValue()
                ));
            }
            
            if (storyPointsNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Scheduling.StoryPoints",
                    "value", storyPointsNum.doubleValue()
                ));
            }
            
            if (priorityNum != null) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/Microsoft.VSTS.Common.Priority",
                    "value", priorityNum.intValue()
                ));
            }
            
            if (tags != null && !tags.trim().isEmpty()) {
                operations.add(Map.of(
                    "op", "add",
                    "path", "/fields/System.Tags",
                    "value", tags
                ));
            }
            
            // Procesar campos con el manejador genérico
            Map<String, Object> allFields = new HashMap<>();
            for (Map<String, Object> op : operations) {
                allFields.put((String) op.get("path"), ((Map<String, Object>) op.get("value")).get("value"));
            }
            
            // Validar campos requeridos
            GenericWorkItemFieldsHandler.ValidationResult validation = 
                fieldsHandler.validateRequiredFields(type, allFields);
            
            if (!validation.isValid()) {
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("❌ Faltan campos obligatorios para el tipo '").append(type).append("':\n\n");
                
                List<String> errors = validation.getErrors();
                for (String error : errors) {
                    errorMsg.append("• ").append(error).append("\n");
                }
                
                errorMsg.append("\n📋 Use los parámetros correspondientes para proporcionar estos valores obligatorios.");
                
                return Map.of(
                    "content", List.of(Map.of(
                        "type", "text",
                        "text", errorMsg.toString()
                    )),
                    "isError", true
                );
            }
            
            // Relación padre-hijo si se especifica
            if (parentIdNum != null) {
                String parentUrl = String.format("https://dev.azure.com/%s/_apis/wit/workItems/%d", 
                    azureDevOpsClient.getOrganization(), parentIdNum.intValue());
                
                operations.add(Map.of(
                    "op", "add",
                    "path", "/relations/-",
                    "value", Map.of(
                        "rel", "System.LinkTypes.Hierarchy-Reverse",
                        "url", parentUrl,
                        "attributes", Map.of("comment", "Vinculado como trabajo hijo")
                    )
                ));
            }
            
            // Crear el work item
            WorkItem createdWorkItem = azureDevOpsClient.createWorkItem(project, type, operations);
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("✅ Work item creado exitosamente:%n%n"));
            result.append(createdWorkItem.toDetailedString());
            
            if (parentIdNum != null) {
                result.append(String.format("🔗 Vinculado como hijo del work item #%d%n", parentIdNum.intValue()));
            }
            
            // Agregar información sobre campos aplicados
            List<String> availableFields = fieldsHandler.getAvailableFields(type);
            if (availableFields.size() > 2) { // Más que los básicos
                result.append(String.format("%n📋 Campos aplicados para '%s'%n", type));
            }
            
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.toString()
                ))
            );
            
        } catch (AzureDevOpsException e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error de Azure DevOps: " + e.getMessage()
                )),
                "isError", true
            );
        } catch (Exception e) {
            return Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", "Error inesperado: " + e.getMessage()
                )),
                "isError", true
            );
        }
    }
}
