# 🚀 Scripts Interactivos MCP Azure DevOps

Esta carpeta contiene scripts interactivos para gestionar las imágenes Docker del servidor MCP Azure DevOps.

## 📋 Scripts Disponibles

### 🎯 Script Principal
- **`mcp-docker-helper.sh`** - Menú principal interactivo con todas las opciones

### 🔨 Scripts de Construcción
- **`build-docker-image.sh`** - Construcción interactiva de imágenes Docker
- **`test-docker-image.sh`** - Testing interactivo de imágenes construidas

## 🚀 Uso Rápido

```bash
# Ejecutar el helper principal (recomendado)
./scripts/mcp-docker-helper.sh

# O ejecutar scripts individuales de forma interactiva
./scripts/build-docker-image.sh
./scripts/test-docker-image.sh
```

## 📊 Tipos de Imágenes Disponibles

| Imagen | Tamaño | Optimización | Descripción |
|--------|--------|--------------|-------------|
| `mcp-azure-devops:latest` | ~473MB | Estándar | Versión base con todas las dependencias |
| `mcp-azure-devops:slim` | ~358MB | Alpine Linux | Versión optimizada (↓24%) |
| `mcp-azure-devops:ultra` | ~191MB | JRE Customizado | Ultra-optimizada (↓60%) |

## �� Modo Interactivo

Todos los scripts funcionan en **modo interactivo** cuando se ejecutan sin parámetros:

### 🔨 Build Interactivo
```bash
./scripts/build-docker-image.sh
```
- Selección visual de Dockerfile (estándar/slim/ultra)
- Configuración de tag automática
- Opciones de limpieza y cache
- Test post-construcción opcional

### 🧪 Test Interactivo
```bash
./scripts/test-docker-image.sh
```
- Detección automática de imágenes disponibles
- Selección de modos de test (STDIO/HTTP/ALL)
- Configuración de puertos y timeouts

### 🎯 Helper Principal
```bash
./scripts/mcp-docker-helper.sh
```
- Menú completo con todas las operaciones
- Vista general de imágenes disponibles
- Ejecución rápida de contenedores
- Limpieza y documentación

## 📖 Ejemplos de Uso

### Construir imagen ultra-optimizada:
```bash
$ ./scripts/build-docker-image.sh
# Seleccionar opción 3 (Dockerfile.ultra)
# Tag automático: mcp-azure-devops:ultra
```

### Probar imagen con credenciales:
```bash
$ ./scripts/test-docker-image.sh  
# Seleccionar imagen ultra
# Modo STDIO con archivo .env
```

### Ejecutar servidor rápidamente:
```bash
$ ./scripts/mcp-docker-helper.sh
# Opción 6: Ejecutar imagen rápidamente
# Seleccionar ultra + modo STDIO/HTTP
```

## ⚙️ Configuración

Los scripts requieren un archivo `.env` en la raíz del proyecto:

```env
AZURE_DEVOPS_ORGANIZATION=tu-organizacion
AZURE_DEVOPS_PAT=tu-personal-access-token
```

## 🔧 Funcionalidades Avanzadas

- **Auto-detección** de imágenes disponibles
- **Validación** de configuración antes de construir
- **Limpieza inteligente** de imágenes obsoletas
- **Tests automáticos** post-construcción
- **Documentación integrada** con ejemplos
- **Ejecución rápida** para desarrollo

## 🎨 Características Visuales

- ✅ **Colores** para mejor legibilidad
- 📊 **Información de tamaños** en tiempo real  
- 🎯 **Menús numerados** fáciles de usar
- ⏱️ **Timestamps** en todas las operaciones
- 🚀 **Banners** y separadores visuales

## 📜 Scripts Adicionales

### 🐚 Shell Scripts (curl/)
- `work_item_attachment_add.sh` - Test de adjuntar archivos
- `attachments_delete.sh` - Test de eliminación de attachments  

### 🐍 Python Scripts (python/)
- `list_work_items.py` - Listar work items con filtros
- `export_work_items.py` - Exportar work items a CSV/JSON

¡Disfruta del desarrollo con MCP Azure DevOps! 🎉
