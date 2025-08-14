# Análisis de Scripts cURL e Implementaciones Faltantes

**Fecha:** 13 de agosto de 2025  
**Análisis basado en:** Revisión del codebase completo y documentación API

## 📊 Resumen Ejecutivo

| Área | APIs Doc. | Scripts cURL | Tools MCP | Scripts Faltantes | Tools MCP Faltantes |
|------|-----------|--------------|-----------|-------------------|---------------------|
| **accounts** | 2 | 2 | 1 | ✅ 0 | ⚠️ 2 |
| **core** | 19 | 19 | 19 | ✅ 0 | ⚠️ 2 |
| **profile** | 1 | 1 | 1 | ✅ 0 | ✅ 0 |
| **work** | 26 | 20 | 2 | ❌ 8 | ⚠️ 19 |
| **wit** | 27 | 60 | 49 | ❌ 21 | ⚠️ 26 |
| **workitemtrackingprocess** | 14 | 1 | 1 | ❌ 14 | ✅ 0 |
| **TOTAL** | **89** | **103** | **73** | **43** | **49** |

## 🎯 Prioridades de Implementación

### 1. Scripts cURL Faltantes (43 total)

#### Área Work (8 scripts) - ALTA PRIORIDAD
```bash
scripts/curl/work/get_predefined-queries.sh     # Requiere api_doc/work/predefined-queries.md
scripts/curl/work/get_rows.sh                   # Requiere api_doc/work/rows.md
scripts/curl/work/get_taskboard-columns.sh      # Requiere api_doc/work/taskboard-columns.md
scripts/curl/work/get_taskboard-work-items.sh   # Requiere api_doc/work/taskboard-work-items.md
scripts/curl/work/get_teamdaysoff.sh            # Requiere api_doc/work/teamdaysoff.md
scripts/curl/work/get_teamfieldvalues.sh        # Requiere api_doc/work/teamfieldvalues.md
scripts/curl/work/get_teamsettings.sh           # Requiere api_doc/work/teamsettings.md
scripts/curl/work/get_workitemsorder.sh         # Requiere api_doc/work/workitemsorder.md
```

#### Área WorkItemTrackingProcess (14 scripts) - ALTA PRIORIDAD
```bash
scripts/curl/workitemtrackingprocess/behaviors.sh
scripts/curl/workitemtrackingprocess/controls.sh
scripts/curl/workitemtrackingprocess/fields.sh
scripts/curl/workitemtrackingprocess/groups.sh
scripts/curl/workitemtrackingprocess/layout.sh
scripts/curl/workitemtrackingprocess/lists.sh
scripts/curl/workitemtrackingprocess/pages.sh
scripts/curl/workitemtrackingprocess/processes.sh
scripts/curl/workitemtrackingprocess/rules.sh
scripts/curl/workitemtrackingprocess/states.sh
scripts/curl/workitemtrackingprocess/system_controls.sh
scripts/curl/workitemtrackingprocess/work_item_types.sh
scripts/curl/workitemtrackingprocess/work_item_types_behaviors.sh
scripts/curl/workitemtrackingprocess/workitemtrackingprogress.sh
```

#### Área WIT (21 scripts) - MEDIA PRIORIDAD
**Nota:** Para WIT, los scripts están implementados de forma granular (ej: `attachments_create.sh`, `attachments_get.sh`, `attachments_delete.sh` en lugar de `attachments.sh`). La mayoría de la funcionalidad ya está cubierta.

```bash
# Scripts consolidados que faltan:
scripts/curl/wit/attachments.sh                        # Wrapper para create/get/delete
scripts/curl/wit/classification_nodes.sh               # Wrapper para CRUD operations
scripts/curl/wit/comment_reactions_engaged_users.sh    # Wrapper
scripts/curl/wit/comments.sh                          # Wrapper para CRUD operations
scripts/curl/wit/comments_reactions.sh                # Wrapper
scripts/curl/wit/comments_versions.sh                 # Wrapper
scripts/curl/wit/recycle_bin.sh                       # Wrapper para operations
scripts/curl/wit/reporting_work_item_links.sh         # Wrapper
scripts/curl/wit/reporting_work_item_revisions.sh     # Wrapper
scripts/curl/wit/revisions.sh                         # Wrapper
scripts/curl/wit/update_query_folder_rename_undelete.sh
scripts/curl/wit/wiql.sh                               # Wrapper para by_id/by_query
scripts/curl/wit/work_item_icons.sh                   # Wrapper
scripts/curl/wit/work_item_relation_types.sh          # Wrapper
scripts/curl/wit/work_item_revisions_discussions.sh   # Wrapper
scripts/curl/wit/work_item_transitions.sh
scripts/curl/wit/work_item_type_categories.sh         # Wrapper
scripts/curl/wit/work_item_type_states.sh
scripts/curl/wit/work_item_types.sh                   # Wrapper
scripts/curl/wit/work_item_types_field.sh             # Wrapper
scripts/curl/wit/work_items.sh                        # Wrapper para CRUD operations
```

### 2. Herramientas MCP Faltantes (49 total)

#### Accounts (2 herramientas)
```java
azuredevops_accounts_get_account      // GetAccountTool
azuredevops_accounts_list_accounts    // ListAccountsTool
```

#### Core (2 herramientas)
```java
azuredevops_core_list_projects        // ListProjectsTool (diferente de get_projects)
azuredevops_core_list_teams          // ListTeamsTool (diferente de get_teams)
```

#### Work (19 herramientas)
```java
azuredevops_work_get_backlogconfiguration
azuredevops_work_get_boardcolumns
azuredevops_work_get_boardparents
azuredevops_work_get_boardrows
azuredevops_work_get_boardrows_project
azuredevops_work_get_boardusersettings
azuredevops_work_get_capacities
azuredevops_work_get_cardrulesettings
azuredevops_work_get_cardsettings
azuredevops_work_get_chartimages
azuredevops_work_get_charts
azuredevops_work_get_columns
azuredevops_work_get_deliverytimeline
azuredevops_work_get_iterationcapacities
azuredevops_work_get_iterations
azuredevops_work_get_plans
azuredevops_work_get_predefined_queries
azuredevops_work_get_processconfiguration
azuredevops_work_list_boards
```

#### WIT (26 herramientas)
```java
azuredevops_wit_account_my_work_recent_activity
azuredevops_wit_artifact_link_types
azuredevops_wit_classification_nodes_patch
azuredevops_wit_classification_nodes_put
azuredevops_wit_recycle_bin_destroy
azuredevops_wit_recycle_bin_get
azuredevops_wit_recycle_bin_get_batch
azuredevops_wit_recycle_bin_list
azuredevops_wit_recycle_bin_restore
azuredevops_wit_reporting_work_item_links_get
azuredevops_wit_reporting_work_item_revisions_get
azuredevops_wit_reporting_work_item_revisions_post
azuredevops_wit_work_item_icons_get
azuredevops_wit_work_item_icons_list
azuredevops_wit_work_item_relation_types_get
azuredevops_wit_work_item_revisions_discussions_get
azuredevops_wit_work_item_revisions_discussions_list
azuredevops_wit_work_item_transitions_list
azuredevops_wit_work_item_type_categories_get
azuredevops_wit_work_item_types_field_list
azuredevops_wit_work_item_types_list
azuredevops_wit_work_items_create
azuredevops_wit_work_items_delete
azuredevops_wit_work_items_get
azuredevops_wit_work_items_get_template
azuredevops_wit_work_items_update
```

## 🔄 Flujo de Trabajo Recomendado

### Fase 1: Scripts cURL (Orden de prioridad)
1. **Work** - 8 scripts (completar área más simple)
2. **WorkItemTrackingProcess** - 14 scripts (área con mayor déficit)
3. **WIT** - 21 scripts wrapper (opcional, funcionalidad ya cubierta)

### Fase 2: Herramientas MCP
1. **Accounts** - 2 tools (área más simple)
2. **Core** - 2 tools (completar área crítica)
3. **Work** - 19 tools (basado en scripts validados)
4. **WIT** - 26 tools (basado en scripts validados)

## 📋 Estado Actual por Área

### ✅ **COMPLETO**
- **Profile**: 100% scripts y tools implementados

### 🟡 **PARCIALMENTE COMPLETO**
- **Accounts**: Scripts completos, faltan 2 tools MCP
- **Core**: Scripts completos, faltan 2 tools MCP

### ❌ **INCOMPLETO**
- **Work**: Faltan 8 scripts + 19 tools MCP
- **WIT**: Funcionalidad cubierta con scripts granulares, faltan wrappers + 26 tools MCP
- **WorkItemTrackingProcess**: Área menos desarrollada, faltan 14 scripts

## 🎯 Próximos Pasos Inmediatos

1. **Implementar scripts Work faltantes** siguiendo documentación en `api_doc/work/`
2. **Crear scripts WorkItemTrackingProcess** basados en `api_doc/workitemtrackingprocess/`
3. **Desarrollar herramientas MCP** para scripts ya validados con cURL
4. **Seguir flujo estándar**: Script cURL → Validación → Herramienta MCP → Test

---
**Nota**: Este análisis se basa en la revisión completa del código fuente Java y scripts existentes. Los nombres de herramientas MCP se extrajeron directamente de las constantes `NAME` en los archivos `*Tool.java`.
