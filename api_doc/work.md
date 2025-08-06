# Work API

This section documents the Azure DevOps Work API and all its subsections. Each subsection is documented in its own file for clarity and maintainability.

**Cada vez que vayas a procesar una seccion, debes seguir la URL y leer el contenido de la pagina, para asegurarte de seguir todos los links de esa seccion, por que pueden tener subsecciones que deben ser documentados, al entrar en el link, debes seguir lo links para poder documentar todas las subsecciones, la documentacion de cada parametro debe estar tambien incluida, para poder saber que posibles valores deben enviarse, quiero que documentes en cada seccion, lo que sea tipo Filtros de propiedades a incluir, para que la documentacion quede completa, asi este sea a su vez un json, dejalo documentado, debe ser todos los posibles valores, no solo los valores comunes, todos los ejemplos de consumo deben ser con curl**

## Official Documentation Links

- [BacklogConfiguration](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/backlogconfiguration?view=azure-devops-rest-7.2)
- [Backlogs](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/backlogs?view=azure-devops-rest-7.2)
- [BoardColumns](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardcolumns?view=azure-devops-rest-7.2)
- [BoardParents](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardparents?view=azure-devops-rest-7.2)
- [BoardRows](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardrows?view=azure-devops-rest-7.2)
- [Boards](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boards?view=azure-devops-rest-7.2)
- [BoardUserSettings](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/boardusersettings?view=azure-devops-rest-7.2)
- [Capacities](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/capacities?view=azure-devops-rest-7.2)
- [CardRuleSettings](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/cardrulesettings?view=azure-devops-rest-7.2)
- [CardSettings](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/cardsettings?view=azure-devops-rest-7.2)
- [ChartImages](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/chartimages?view=azure-devops-rest-7.2)
- [Charts](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/charts?view=azure-devops-rest-7.2)
- [Columns](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/columns?view=azure-devops-rest-7.2)
- [DeliveryTimeline](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/deliverytimeline?view=azure-devops-rest-7.2)
- [IterationCapacities](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/iterationcapacities?view=azure-devops-rest-7.2)
- [Iterations](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/iterations?view=azure-devops-rest-7.2)
- [Plans](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/plans?view=azure-devops-rest-7.2)
- [Predefined Queries](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/predefined-queries?view=azure-devops-rest-7.2)
- [ProcessConfiguration](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/processconfiguration?view=azure-devops-rest-7.2)
- [Rows](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/rows?view=azure-devops-rest-7.2)
- [Taskboard Columns](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/taskboard-columns?view=azure-devops-rest-7.2)
- [Taskboard Work Items](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/taskboard-work-items?view=azure-devops-rest-7.2)
- [TeamDaysOff](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/teamdaysoff?view=azure-devops-rest-7.2)
- [TeamFieldValues](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/teamfieldvalues?view=azure-devops-rest-7.2)
- [TeamSettings](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/teamsettings?view=azure-devops-rest-7.2)
- [WorkItemsOrder](https://learn.microsoft.com/en-us/rest/api/azure/devops/work/workitemsorder?view=azure-devops-rest-7.2)

## Index of Subsections and Progress

| Subsection                | Documentation Link                                      | Status         |
|---------------------------|--------------------------------------------------------|----------------|
| BacklogConfiguration      | [backlogconfiguration.md](work/backlogconfiguration.md) | ✅ Documented  |
| Backlogs                  | [backlogs.md](work/backlogs.md)                         | ✅ Documented  |
| BoardColumns              | [boardcolumns.md](work/boardcolumns.md)                 | ✅ Documented  |
| BoardParents              | [boardparents.md](work/boardparents.md)                 | ✅ Documented  |
| BoardRows                 | [boardrows.md](work/boardrows.md)                       | ✅ Documentado |
| Boards                    | [boards.md](work/boards.md)                             | ✅ Documentado |
| BoardUserSettings         | [boardusersettings.md](work/boardusersettings.md)       | ✅ Documentado |
| Capacities                | [capacities.md](work/capacities.md)                     | ✅ Documentado |
| CardRuleSettings          | [cardrulesettings.md](work/cardrulesettings.md)         | ✅ Documentado |
| CardSettings              | [cardsettings.md](work/cardsettings.md)                 | ✅ Documentado |
| ChartImages               | [chartimages.md](work/chartimages.md)                   | ✅ Documentado |
| Charts                    | [charts.md](work/charts.md)                             | ✅ Documentado |
| Columns                   | [columns.md](work/columns.md)                           | ✅ Documentado |
| DeliveryTimeline          | [deliverytimeline.md](work/deliverytimeline.md)         | ✅ Documentado |
| IterationCapacities       | [iterationcapacities.md](work/iterationcapacities.md)   | ✅ Documentado |
| Iterations                | [iterations.md](work/iterations.md)                     | ✅ Documentado |
| Plans                     | [plans.md](work/plans.md)                               | ✅ Documentado |
| Predefined Queries        | [predefined-queries.md](work/predefined-queries.md)     | ✅ Documentado |
| ProcessConfiguration      | [processconfiguration.md](work/processconfiguration.md) | ✅ Documentado |
| Rows                      | [rows.md](work/rows.md)                                 | ✅ Documentado |
| Taskboard Columns         | [taskboard-columns.md](work/taskboard-columns.md)       | ✅ Documentado |
| Taskboard Work Items      | [taskboard-work-items.md](work/taskboard-work-items.md) | ✅ Documentado |
| TeamDaysOff               | [teamdaysoff.md](work/teamdaysoff.md)                   | ✅ Documentado |
| TeamFieldValues           | [teamfieldvalues.md](work/teamfieldvalues.md)           | ✅ Documentado |
| TeamSettings              | [teamsettings.md](work/teamsettings.md)                 | ✅ Documentado |
| WorkItemsOrder            | [workitemsorder.md](work/workitemsorder.md)             | ✅ Documentado |

---

Each file will be updated with detailed documentation, cURL examples, parameters, and all required information as per the project guidelines.
