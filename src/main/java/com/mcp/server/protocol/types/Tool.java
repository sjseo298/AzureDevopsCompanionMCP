package com.mcp.server.protocol.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Representación inmutable de una herramienta en el protocolo MCP.
 * 
 * <p>Una herramienta define una funcionalidad que el servidor puede ejecutar
 * para el cliente, incluyendo su esquema de validación de parámetros.
 * 
 * <p>Ejemplo de uso:
 * <pre>{@code
 * Tool tool = Tool.builder()
 *     .name("generate_uuid")
 *     .description("Genera un UUID aleatorio")
 *     .inputSchema(Map.of("type", "object"))
 *     .build();
 * }</pre>
 * 
 * @author MCP Server Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Tool {
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("inputSchema")
    private final Schema inputSchema;
    
    private Tool(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Tool name cannot be null");
        this.description = Objects.requireNonNull(builder.description, "Tool description cannot be null");
        this.inputSchema = builder.inputSchema != null ? builder.inputSchema : new Schema();
    }
    
    // Constructor para Jackson
    Tool() {
        this.name = null;
        this.description = null;
        this.inputSchema = null;
    }
    
    /**
     * Crea un nuevo builder para construir una herramienta.
     * 
     * @return nuevo builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public String getName() { 
        return name; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public Schema getInputSchema() { 
        return inputSchema; 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tool tool = (Tool) obj;
        return Objects.equals(name, tool.name) &&
               Objects.equals(description, tool.description) &&
               Objects.equals(inputSchema, tool.inputSchema);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, description, inputSchema);
    }
    
    @Override
    public String toString() {
        return "Tool{" +
               "name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", inputSchema=" + inputSchema +
               '}';
    }
    
    /**
     * Builder para construir instancias de Tool de forma fluida.
     */
    public static final class Builder {
        private String name;
        private String description;
        private Schema inputSchema;
        
        private Builder() {}
        
        /**
         * Establece el nombre de la herramienta.
         * 
         * @param name nombre único de la herramienta
         * @return este builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Establece la descripción de la herramienta.
         * 
         * @param description descripción clara de la funcionalidad
         * @return este builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Establece el esquema de entrada usando un Map.
         * 
         * @param schema esquema JSON como Map
         * @return este builder
         */
        public Builder inputSchema(Map<String, Object> schema) {
            this.inputSchema = Schema.fromMap(schema);
            return this;
        }
        
        /**
         * Establece el esquema de entrada usando un objeto Schema.
         * 
         * @param schema objeto Schema
         * @return este builder
         */
        public Builder inputSchema(Schema schema) {
            this.inputSchema = schema;
            return this;
        }
        
        /**
         * Construye la instancia de Tool.
         * 
         * @return nueva instancia de Tool
         * @throws NullPointerException si name o description son null
         */
        public Tool build() {
            return new Tool(this);
        }
    }
    
    /**
     * Esquema JSON para validar los parámetros de entrada de la herramienta.
     */
    public static final class Schema {
        
        @JsonProperty("type")
        private final String type;
        
        @JsonProperty("properties")
        private final Map<String, Object> properties;
        
        @JsonProperty("required")
        private final List<String> required;
        
        public Schema() {
            this.type = "object";
            this.properties = Map.of();
            this.required = List.of();
        }
        
        public Schema(String type, Map<String, Object> properties, List<String> required) {
            this.type = Objects.requireNonNull(type, "Schema type cannot be null");
            this.properties = Objects.requireNonNull(properties, "Schema properties cannot be null");
            this.required = Objects.requireNonNull(required, "Schema required cannot be null");
        }
        
        /**
         * Crea un Schema desde un Map.
         * 
         * @param schemaMap mapa con los datos del schema
         * @return nuevo Schema
         */
        @SuppressWarnings("unchecked")
        public static Schema fromMap(Map<String, Object> schemaMap) {
            String type = (String) schemaMap.getOrDefault("type", "object");
            Map<String, Object> properties = (Map<String, Object>) schemaMap.getOrDefault("properties", Map.of());
            List<String> required = (List<String>) schemaMap.getOrDefault("required", List.of());
            
            return new Schema(type, properties, required);
        }
        
        // Getters
        public String getType() { 
            return type; 
        }
        
        public Map<String, Object> getProperties() { 
            return properties; 
        }
        
        public List<String> getRequired() { 
            return required; 
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Schema schema = (Schema) obj;
            return Objects.equals(type, schema.type) &&
                   Objects.equals(properties, schema.properties) &&
                   Objects.equals(required, schema.required);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, properties, required);
        }
        
        @Override
        public String toString() {
            return "Schema{" +
                   "type='" + type + '\'' +
                   ", properties=" + properties +
                   ", required=" + required +
                   '}';
        }
    }
}
