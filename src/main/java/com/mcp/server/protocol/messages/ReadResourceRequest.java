package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Solicitud MCP resources/read.
 */
public class ReadResourceRequest extends McpRequest {

    @JsonProperty("params")
    private ReadResourceParams params;

    public ReadResourceRequest() {
        super(null, "resources/read");
    }

    public ReadResourceRequest(Object id, ReadResourceParams params) {
        super(id, "resources/read");
        this.params = params;
    }

    public ReadResourceParams getParams() {
        return params;
    }

    public void setParams(ReadResourceParams params) {
        this.params = params;
    }

    public static class ReadResourceParams {
        @JsonProperty("uri")
        private String uri;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }
}
