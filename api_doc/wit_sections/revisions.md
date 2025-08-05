# Revisions

Permite obtener la lista paginada de todas las revisiones de un work item, así como consultar una revisión específica. Cada revisión representa el estado completo del work item en un punto del tiempo.

**Documentación oficial:** [Revisions](https://learn.microsoft.com/en-us/rest/api/azure/devops/wit/revisions?view=azure-devops-rest-7.2)

## Operaciones principales

| Operación | Endpoint | Método |
|-----------|----------|--------|
| List      | /{organization}/{project}/_apis/wit/workItems/{id}/revisions?api-version=7.2-preview.3 | GET    |
| Get       | /{organization}/{project}/_apis/wit/workItems/{id}/revisions/{revisionNumber}?api-version=7.2-preview.3 | GET    |

---

## Listar revisiones de un work item

**Endpoint:**
```
GET https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{id}/revisions?api-version=7.2-preview.3
```

**Parámetros:**

| Nombre      | Ubicación | Tipo     | Requerido | Descripción                                                                 | Valores posibles / Ejemplo |
|-------------|-----------|----------|-----------|-----------------------------------------------------------------------------|----------------------------|
| organization| path      | string   | Sí        | Nombre de la organización de Azure DevOps                                   | fabrikam                  |
| project     | path      | string   | No        | Nombre o ID del proyecto                                                    | Fabrikam-Fiber-Git         |
| id          | path      | int32    | Sí        | ID del work item                                                            | 1                          |
| api-version | query     | string   | Sí        | Versión de la API                                                           | 7.2-preview.3              |
| $expand     | query     | enum     | No        | Expande atributos del work item                                             | none, relations, fields, links, all |
| $skip       | query     | int32    | No        | Número de revisiones a omitir (paginación)                                  | 0, 10, ...                 |
| $top        | query     | int32    | No        | Número máximo de revisiones a devolver                                      | 10, 100, ...               |

**Valores posibles para `$expand`:**
- `none`: Comportamiento por defecto
- `relations`: Expande relaciones
- `fields`: Expande campos
- `links`: Expande enlaces
- `all`: Expande todo

**Permisos requeridos:**
- `vso.work` (lectura de work items)

**Ejemplo de uso con curl:**
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/_apis/wit/workItems/1/revisions?$top=2&$skip=1&$expand=all&api-version=7.2-preview.3"
```

**Respuesta de ejemplo:**
```json
{
  "count": 2,
  "value": [
    {
      "id": 1,
      "rev": 2,
      "fields": {
        "System.WorkItemType": "Bug",
        "System.State": "New",
        "System.Reason": "New",
        "System.AssignedTo": {
          "displayName": "Jamal Hartnett",
          "url": "https://vssps.dev.azure.com/fabrikam/_apis/Identities/d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "_links": {
            "avatar": {
              "href": "https://dev.azure.com/mseng/_apis/GraphProfile/MemberAvatars/aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
            }
          },
          "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "uniqueName": "fabrikamfiber4@hotmail.com",
          "imageUrl": "https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "descriptor": "aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
        },
        "System.CreatedDate": "2017-09-04T02:08:16.6Z",
        "System.CreatedBy": {
          "displayName": "Jamal Hartnett",
          "url": "https://vssps.dev.azure.com/fabrikam/_apis/Identities/d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "_links": {
            "avatar": {
              "href": "https://dev.azure.com/mseng/_apis/GraphProfile/MemberAvatars/aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
            }
          },
          "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "uniqueName": "fabrikamfiber4@hotmail.com",
          "imageUrl": "https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "descriptor": "aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
        },
        "System.ChangedDate": "2017-09-04T02:28:56.253Z",
        "System.ChangedBy": {
          "displayName": "Jamal Hartnett",
          "url": "https://vssps.dev.azure.com/fabrikam/_apis/Identities/d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "_links": {
            "avatar": {
              "href": "https://dev.azure.com/mseng/_apis/GraphProfile/MemberAvatars/aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
            }
          },
          "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "uniqueName": "fabrikamfiber4@hotmail.com",
          "imageUrl": "https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
          "descriptor": "aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
        },
        "System.TeamProject": "MyAgilePro1",
        "System.AreaPath": "MyAgilePro1",
        "System.IterationPath": "MyAgilePro1\\Iteration 1",
        "System.Title": "Bug 1",
        "Microsoft.VSTS.Common.Priority": 2,
        "Microsoft.VSTS.Common.Severity": "3 - Medium",
        "Microsoft.VSTS.Common.ValueArea": "Business",
        "Microsoft.VSTS.Common.StateChangeDate": "2017-09-04T02:08:16.6Z",
        "System.Tags": ""
      },
      "url": "https://dev.azure.com/fabrikam/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c/_apis/wit/workItems/1/revisions/2"
    },
    {
      "id": 1,
      "rev": 3,
      "fields": {
        "System.WorkItemType": "Bug",
        "System.State": "Active"
      }
    }
  ]
}
```

---

## Obtener una revisión específica

**Endpoint:**
```
GET https://dev.azure.com/{organization}/{project}/_apis/wit/workItems/{id}/revisions/{revisionNumber}?api-version=7.2-preview.3
```

**Parámetros:**

| Nombre           | Ubicación | Tipo     | Requerido | Descripción                                                                 | Valores posibles / Ejemplo |
|------------------|-----------|----------|-----------|-----------------------------------------------------------------------------|----------------------------|
| organization     | path      | string   | Sí        | Nombre de la organización de Azure DevOps                                   | fabrikam                  |
| project          | path      | No        | Nombre o ID del proyecto                                                    | Fabrikam-Fiber-Git         |
| id               | path      | int32    | Sí        | ID del work item                                                            | 1                          |
| revisionNumber   | path      | int32    | Sí        | Número de la revisión                                                       | 2                          |
| api-version      | query     | string   | Sí        | Versión de la API                                                           | 7.2-preview.3              |
| $expand          | query     | enum     | No        | Expande atributos del work item                                             | none, relations, fields, links, all |

**Valores posibles para `$expand`:**
- `none`: Comportamiento por defecto
- `relations`: Expande relaciones
- `fields`: Expande campos
- `links`: Expande enlaces
- `all`: Expande todo

**Permisos requeridos:**
- `vso.work` (lectura de work items)

**Ejemplo de uso con curl:**
```bash
curl -X GET \
  -H "Authorization: Bearer <TOKEN>" \
  "https://dev.azure.com/fabrikam/Fabrikam-Fiber-Git/_apis/wit/workItems/1/revisions/2?$expand=all&api-version=7.2-preview.3"
```

**Respuesta de ejemplo:**
```json
{
  "id": 1,
  "rev": 2,
  "fields": {
    "System.WorkItemType": "Bug",
    "System.State": "New",
    "System.Reason": "New",
    "System.AssignedTo": {
      "displayName": "Jamal Hartnett",
      "url": "https://vssps.dev.azure.com/fabrikam/_apis/Identities/d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "_links": {
        "avatar": {
          "href": "https://dev.azure.com/mseng/_apis/GraphProfile/MemberAvatars/aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
        }
      },
      "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "uniqueName": "fabrikamfiber4@hotmail.com",
      "imageUrl": "https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "descriptor": "aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
    },
    "System.CreatedDate": "2017-09-04T02:08:16.6Z",
    "System.CreatedBy": {
      "displayName": "Jamal Hartnett",
      "url": "https://vssps.dev.azure.com/fabrikam/_apis/Identities/d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "_links": {
        "avatar": {
          "href": "https://dev.azure.com/mseng/_apis/GraphProfile/MemberAvatars/aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
        }
      },
      "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "uniqueName": "fabrikamfiber4@hotmail.com",
      "imageUrl": "https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "descriptor": "aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
    },
    "System.ChangedDate": "2017-09-04T02:28:56.253Z",
    "System.ChangedBy": {
      "displayName": "Jamal Hartnett",
      "url": "https://vssps.dev.azure.com/fabrikam/_apis/Identities/d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "_links": {
        "avatar": {
          "href": "https://dev.azure.com/mseng/_apis/GraphProfile/MemberAvatars/aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
        }
      },
      "id": "d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "uniqueName": "fabrikamfiber4@hotmail.com",
      "imageUrl": "https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d291b0c4-a05c-4ea6-8df1-4b41d5f39eff",
      "descriptor": "aad.YTkzODFkODYtNTYxYS03ZDdiLWJjM2QtZDUzMjllMjM5OTAz"
    },
    "System.TeamProject": "MyAgilePro1",
    "System.AreaPath": "MyAgilePro1",
    "System.IterationPath": "MyAgilePro1\\Iteration 1",
    "System.Title": "Bug 1",
    "Microsoft.VSTS.Common.Priority": 2,
    "Microsoft.VSTS.Common.Severity": "3 - Medium",
    "Microsoft.VSTS.Common.ValueArea": "Business",
    "Microsoft.VSTS.Common.StateChangeDate": "2017-09-04T02:08:16.6Z",
    "System.Tags": ""
  },
  "_links": {
    "self": {
      "href": "https://dev.azure.com/fabrikam/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c/_apis/wit/workItems/1/revisions/2"
    },
    "workItemRevisions": {
      "href": "https://dev.azure.com/fabrikam/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c/_apis/wit/workItems/1/revisions"
    },
    "parent": {
      "href": "https://dev.azure.com/fabrikam/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c/_apis/wit/workItems/1"
    }
  },
  "url": "https://dev.azure.com/fabrikam/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c/_apis/wit/workItems/1/revisions/2"
}
```
