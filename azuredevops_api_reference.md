# Azure DevOps REST API Documentation (v7.2)

This document provides a comprehensive, English-language reference for the Azure DevOps REST API (version 7.2). Each section links to the official documentation for that API area. This structure will be used to iteratively expand with detailed documentation, cURL examples, input parameters, and request/response samples.

La documentacion de cada Seccion debe crearse en un archivo independiente referenciado por este en una carpeta que se llame api_doc. Y aqui se va marcando el progreso de lo que se va haciendo.

Se debe seguir el link de la seccion y documentar *Completamente* todas las operaciones y funcionalidades de esa seccion.

La documentacion debe tener una explicacion de para que sirve la funcionalidad.

**Cada vez que vayas a procesar una seccion, debes seguir la URL y leer el contenido de la pagina, para asegurarte de seguir todos los links de esa seccion, por que pueden tener subsecciones que deben ser documentados, al entrar en el link, debes seguir lo links para poder documentar todas las subsecciones, la documentacion de cada parametro debe estar tambien incluida, para poder saber que posibles valores deben enviarse, quiero que documentes en cada seccion, lo que sea tipo Filtros de propiedades a incluir, para que la documentacion quede completa, asi este sea a su vez un json, dejalo documentado, debe ser todos los posibles valores, no solo los valores comunes, todos los ejemplos de consumo deben ser con curl**


**Cuando una seccion sea muy grande, y contenga muchas subsecciones, se creara una carpeta interior para tener esas subsecciones separadas y evitar tener un archivo demasiado grande, el archivo de la seccion tendra una tabla con el indice del progreso documentado*


## Main API Areas (as seen in the official documentation)


| Sección | Documentación Oficial | Documentación Local | Cliente Implementado |
|---------|----------------------|---------------------|---------------------|
| Get started with Azure DevOps & REST | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/?view=azure-devops-rest-7.2) | - | ⏳ |
| Accounts | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/accounts/?view=azure-devops-rest-7.2) | [api_doc/accounts.md](api_doc/accounts.md) | ✅ |
| Advanced Security | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/advanced-security/?view=azure-devops-rest-7.2) | [api_doc/advanced_security.md](api_doc/advanced_security.md) | ✅ |
| Approvals And Checks | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/approvalsandchecks/?view=azure-devops-rest-7.2) | [api_doc/approvals_and_checks.md](api_doc/approvals_and_checks.md) | ✅ |
| Artifacts | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/?view=azure-devops-rest-7.2) | [api_doc/artifacts.md](api_doc/artifacts.md) | ✅ |
| Artifact Details | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/artifact-details?view=azure-devops-rest-7.2) | - | ⏳ |
| Change Tracking | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/change-tracking?view=azure-devops-rest-7.2) | - | ⏳ |
| Feed Management | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/feed-management?view=azure-devops-rest-7.2) | - | ⏳ |
| Feed Recycle Bin | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/feed-recycle-bin?view=azure-devops-rest-7.2) | - | ⏳ |
| Provenance | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/provenance?view=azure-devops-rest-7.2) | - | ⏳ |
| Recycle Bin | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/recycle-bin?view=azure-devops-rest-7.2) | - | ⏳ |
| Retention Policies | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/retention-policies?view=azure-devops-rest-7.2) | - | ⏳ |
| Service Settings | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifacts/service-settings?view=azure-devops-rest-7.2) | - | ⏳ |
| Artifacts Package Types | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/artifactspackagetypes/?view=azure-devops-rest-7.2) | [api_doc/artifactspackagetypes.md](api_doc/artifactspackagetypes.md) | ✅ |
| Audit | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/audit/?view=azure-devops-rest-7.2) | [api_doc/audit.md](api_doc/audit.md) | ✅ |
| Build | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/build/?view=azure-devops-rest-7.2) | [api_doc/build.md](api_doc/build.md) | ✅ |
| Core | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/core/?view=azure-devops-rest-7.2) | [api_doc/core.md](api_doc/core.md) | ✅ |
| Dashboard | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/dashboard/?view=azure-devops-rest-7.2) | [api_doc/dashboard.md](api_doc/dashboard.md) | ✅ |
| Delegated Authorization | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/delegatedauthorization/?view=azure-devops-rest-7.2) | - | ⏳ |
| Distributed Task | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/distributedtask/?view=azure-devops-rest-7.2) | - | ⏳ |
| Environments | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/environments/?view=azure-devops-rest-7.2) | - | ⏳ |
| Extension Management | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/extensionmanagement/?view=azure-devops-rest-7.2) | - | ⏳ |
| Favorite | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/favorite/?view=azure-devops-rest-7.2) | - | ⏳ |
| Git | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/git/?view=azure-devops-rest-7.2) | - | ⏳ |
| Graph | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/graph/?view=azure-devops-rest-7.2) | - | ⏳ |
| Identities | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/identities/?view=azure-devops-rest-7.2) | - | ⏳ |
| Member Entitlement Management | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/memberentitlementmanagement/?view=azure-devops-rest-7.2) | - | ⏳ |
| Notification | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/notification/?view=azure-devops-rest-7.2) | - | ⏳ |
| Operations | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/operations/?view=azure-devops-rest-7.2) | - | ⏳ |
| Permissions Report | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/permissionsreport/?view=azure-devops-rest-7.2) | - | ⏳ |
| Pipelines | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/pipelines/?view=azure-devops-rest-7.2) | - | ⏳ |
| Policy | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/policy/?view=azure-devops-rest-7.2) | - | ⏳ |
| Profile | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/profile/?view=azure-devops-rest-7.2) | - | ⏳ |
| Release | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/release/?view=azure-devops-rest-7.2) | - | ⏳ |
| Resource Usage | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/resourceusage/?view=azure-devops-rest-7.2) | - | ⏳ |
| Search | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/search/?view=azure-devops-rest-7.2) | - | ⏳ |
| Security | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/security/?view=azure-devops-rest-7.2) | - | ⏳ |
| Security Roles | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/securityroles/?view=azure-devops-rest-7.2) | - | ⏳ |
| Service Endpoint | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/serviceendpoint/?view=azure-devops-rest-7.2) | - | ⏳ |
| Service Hooks | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/servicehooks/?view=azure-devops-rest-7.2) | - | ⏳ |
| Status | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/status/?view=azure-devops-rest-7.2) | - | ⏳ |
| Symbol | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/symbol/?view=azure-devops-rest-7.2) | - | ⏳ |
| Test | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/test/?view=azure-devops-rest-7.2) | - | ⏳ |
| Test Plan | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/testplan/?view=azure-devops-rest-7.2) | - | ⏳ |
| Test Results | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/testresults/?view=azure-devops-rest-7.2) | - | ⏳ |
| Tfvc | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/tfvc/?view=azure-devops-rest-7.2) | - | ⏳ |
| Token Admin | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/tokenadmin/?view=azure-devops-rest-7.2) | - | ⏳ |
| Tokens | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/tokens/?view=azure-devops-rest-7.2) | - | ⏳ |
| Wiki | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/wiki/?view=azure-devops-rest-7.2) | - | ⏳ |
| Work | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/?view=azure-devops-rest-7.2) | [api_doc/work.md](api_doc/work.md) | ⏳ |
| Work Item Tracking | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/?view=azure-devops-rest-7.2) | [api_doc/wit.md](api_doc/wit.md) | ✅ |
| Work Item Tracking Process | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/workitemtrackingprocess/?view=azure-devops-rest-7.2) | - | ⏳ |
| Work Item Tracking Process Template | [Enlace](https://learn.microsoft.com/en-us/rest/api/azure/devops/workitemtrackingprocesstemplate/?view=azure-devops-rest-7.2) | - | ⏳ |


a
