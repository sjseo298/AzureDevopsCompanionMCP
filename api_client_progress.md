8. **Gestión de Progreso y Tamaño de Archivos**
   - El progreso de cada documento debe mantenerse en archivos pequeños y claros.
   - Si para registrar el progreso se necesita almacenar más información, crea subcarpetas específicas para cada área o funcionalidad.
   - Cada subcarpeta debe contener archivos de avance detallado y ser referenciada desde este archivo principal, asegurando trazabilidad y organización.
# Progreso de Implementación del Cliente Azure DevOps REST API
# Instrucciones para la Implementación del Cliente de APIs

Para mantener la implementación organizada y evitar código duplicado, sigue estas pautas al desarrollar el cliente de consumo de las APIs en este proyecto:

1. **Patrón de Organización**
   - Cada área de la API debe tener su propio módulo o clase, ubicado en una estructura de carpetas clara (por ejemplo, `src/main/java/api/azuredevops/<area>`).
   - Los métodos de consumo deben ser reutilizables y parametrizables, evitando duplicidad de lógica entre áreas.

2. **Exposición como Tools MCP**
   - Cada funcionalidad implementada debe exponerse como un "tool" para el servidor MCP, siguiendo la convención de tools ya definida en el proyecto.
   - Los tools deben tener nombres descriptivos y documentar claramente los parámetros de entrada y salida.

3. **Documentación y Ejemplos**
   - Actualiza el archivo de documentación local correspondiente (`api_doc/<area>.md`) con ejemplos de uso, parámetros y posibles respuestas.
   - Incluye ejemplos de consumo con cURL y describe los posibles valores de los parámetros.

4. **Evitar Código Duplicado**
   - Centraliza la lógica común (autenticación, manejo de errores, formateo de peticiones) en utilidades compartidas.
   - Revisa los módulos existentes antes de crear nuevos métodos para evitar duplicidad.

5. **Pruebas y Validación**
   - Implementa pruebas unitarias para cada tool expuesto.
   - Valida que los tools funcionen correctamente con los endpoints documentados.

6. **Convenciones de Nombres y Estructura**
   - Usa nombres consistentes y descriptivos para clases, métodos y tools.
   - Mantén la estructura de carpetas alineada con la documentación y el índice de progreso.

7. **Actualización de Progreso**
   - Marca el avance en este archivo cada vez que se implemente un nuevo cliente, cambiando el estado de ⏳ a ✅.

Estas instrucciones aseguran que el desarrollo sea escalable, mantenible y fácil de integrar con el servidor MCP.

Este archivo resume el estado de avance de la implementación de un cliente para cada área de la API que ya tiene documentación local creada en el proyecto.

| Sección | Archivo de Documentación | Cliente Implementado |
|---------|-------------------------|---------------------|
| Accounts | [api_doc/accounts.md](api_doc/accounts.md) | ⏳ |
| Advanced Security | [api_doc/advanced_security.md](api_doc/advanced_security.md) | ⏳ |
| Approvals And Checks | [api_doc/approvals_and_checks.md](api_doc/approvals_and_checks.md) | ⏳ |
| Artifacts | [api_doc/artifacts.md](api_doc/artifacts.md) | ⏳ |
| Artifacts Package Types | [api_doc/artifactspackagetypes.md](api_doc/artifactspackagetypes.md) | ⏳ |
| Audit | [api_doc/audit.md](api_doc/audit.md) | ⏳ |
| Build | [api_doc/build.md](api_doc/build.md) | ⏳ |
| Core | [api_doc/core.md](api_doc/core.md) | ⏳ |
| Dashboard | [api_doc/dashboard.md](api_doc/dashboard.md) | ⏳ |
| Work | [api_doc/work.md](api_doc/work.md) | ⏳ |
| Work Item Tracking | [api_doc/wit.md](api_doc/wit.md) | ⏳ |
