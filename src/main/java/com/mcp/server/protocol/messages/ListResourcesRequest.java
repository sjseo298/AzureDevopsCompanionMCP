package com.mcp.server.protocol.messages;

/**
 * Solicitud MCP resources/list.
 */
public class ListResourcesRequest extends McpRequest {

    public ListResourcesRequest() {
        super(null, "resources/list");
    }

    public ListResourcesRequest(Object id) {
        super(id, "resources/list");
    }
}
