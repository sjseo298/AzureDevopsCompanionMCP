package com.mcp.server.protocol.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "azure.devops.organization=test",
    "azure.devops.pat=test"
})
class CompletionCompleteRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserializeCompletionCompleteRequest() throws Exception {
        String json = """
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "completion/complete",
                "params": {
                    "ref": {
                        "type": "ref/prompt",
                        "name": "generar_configuracion_organizacional"
                    },
                    "argument": {
                        "name": "generar_backup",
                        "value": ""
                    },
                    "context": {
                        "arguments": {}
                    }
                }
            }
            """;
        
        // Deserializar como McpRequest base
        McpRequest request = objectMapper.readValue(json, McpRequest.class);
        
        // Verificar que se deserializó correctamente como CompletionCompleteRequest
        assertInstanceOf(CompletionCompleteRequest.class, request);
        
        CompletionCompleteRequest completionRequest = (CompletionCompleteRequest) request;
        assertEquals("completion/complete", completionRequest.getMethod());
        assertEquals(4, completionRequest.getId());
        
        // Verificar parámetros
        assertNotNull(completionRequest.getParams());
        assertNotNull(completionRequest.getParams().getRef());
        assertEquals("ref/prompt", completionRequest.getParams().getRef().getType());
        assertEquals("generar_configuracion_organizacional", completionRequest.getParams().getRef().getName());
        
        assertNotNull(completionRequest.getParams().getArgument());
        assertEquals("generar_backup", completionRequest.getParams().getArgument().getName());
        assertEquals("", completionRequest.getParams().getArgument().getValue());
        
        assertNotNull(completionRequest.getParams().getContext());
    }
    
    @Test
    void testDeserializeCompletionWithValue() throws Exception {
        String json = """
            {
                "jsonrpc": "2.0",
                "id": 5,
                "method": "completion/complete",
                "params": {
                    "ref": {
                        "type": "ref/prompt",
                        "name": "generar_configuracion_organizacional"
                    },
                    "argument": {
                        "name": "generar_backup",
                        "value": "no"
                    },
                    "context": {
                        "arguments": {}
                    }
                }
            }
            """;
        
        McpRequest request = objectMapper.readValue(json, McpRequest.class);
        assertInstanceOf(CompletionCompleteRequest.class, request);
        
        CompletionCompleteRequest completionRequest = (CompletionCompleteRequest) request;
        assertEquals("no", completionRequest.getParams().getArgument().getValue());
    }
}
