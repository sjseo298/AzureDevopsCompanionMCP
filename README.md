# Plantilla MCP

Este proyecto es una plantilla base para servidores MCP (Model Context Protocol).

## ¿Qué incluye?
- Infraestructura MCP lista para extender
- Un prompt de ejemplo (EchoPrompt)
- Herramienta de ejemplo (UuidGeneratorTool)

## ¿Cómo extender la plantilla?
1. **Agregar un nuevo prompt**
   - Crear una clase que extienda `BasePrompt` en el paquete `prompts`.
   - Registrar el bean en `PromptsConfig`.
2. **Agregar una nueva herramienta**
   - Implementar la interfaz `McpTool` en el paquete `tools`.
   - Registrar el bean correspondiente (crear un archivo de configuración si es necesario).
3. **Actualizar pruebas**
   - Agregar pruebas unitarias/integración para los nuevos prompts y herramientas.

## Ejemplo de prompt
Ver `EchoPrompt` en `src/main/java/com/mcp/server/prompts/example/EchoPrompt.java`.

---

Para más información, consulte la documentación MCP incluida en el proyecto.
