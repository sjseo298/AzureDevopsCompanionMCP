package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Solicitud de inicialización del protocolo MCP.
 * 
 * Esta es la primera solicitud que debe enviar el cliente para establecer
 * la sesión MCP. Incluye información sobre el cliente y sus capacidades.
 */
public class InitializeRequest extends McpRequest {
    @JsonProperty("params")
    private InitializeParams params;
    
    public InitializeRequest() {
        super(null, "initialize");
    }
    
    public InitializeRequest(Object id, InitializeParams params) {
        super(id, "initialize");
        this.params = params;
    }
    
    public InitializeParams getParams() { return params; }
    public void setParams(InitializeParams params) { this.params = params; }
    
    /**
     * Parámetros de la solicitud de inicialización.
     */
    public static class InitializeParams {
        @JsonProperty("protocolVersion")
        private String protocolVersion;
        
        @JsonProperty("capabilities")
        private Map<String, Object> capabilities;
        
        @JsonProperty("clientInfo")
        private ClientInfo clientInfo;
        
        public InitializeParams() {}
        
        public String getProtocolVersion() { return protocolVersion; }
        public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }
        
        public Map<String, Object> getCapabilities() { return capabilities; }
        public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
        
        public ClientInfo getClientInfo() { return clientInfo; }
        public void setClientInfo(ClientInfo clientInfo) { this.clientInfo = clientInfo; }
    }
    
    /**
     * Información sobre el cliente MCP.
     */
    public static class ClientInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("version")
        private String version;
        
        public ClientInfo() {}
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
