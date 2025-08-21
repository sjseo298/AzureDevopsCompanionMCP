package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.WitAttachmentsHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkItemAttachmentAddToolTest {

    @Mock
    private AzureDevOpsClientService azureService;

    @Mock
    private WitAttachmentsHelper attachmentsHelper;

    private WorkItemAttachmentAddTool tool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new WorkItemAttachmentAddTool(azureService, attachmentsHelper);
    }

    @Test
    void testGetName() {
        assertEquals("azuredevops_wit_work_item_attachment_add", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().contains("Adjunta"));
    }

    @Test
    void testValidateCommon_MissingProject() {
        Map<String, Object> args = Map.of(
            "workItemId", 123,
            "fileName", "test.txt",
            "dataBase64", "dGVzdA=="  // "test" en base64
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            tool.validateCommon(args)
        );
        assertTrue(exception.getMessage().contains("'project' es requerido"));
    }

    @Test
    void testValidateCommon_MissingWorkItemId() {
        Map<String, Object> args = Map.of(
            "project", "TestProject",
            "fileName", "test.txt",
            "dataBase64", "dGVzdA=="
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            tool.validateCommon(args)
        );
        assertTrue(exception.getMessage().contains("'workItemId' es requerido"));
    }

    @Test
    void testValidateCommon_InvalidWorkItemId() {
        Map<String, Object> args = Map.of(
            "project", "TestProject",
            "workItemId", "invalid",
            "fileName", "test.txt",
            "dataBase64", "dGVzdA=="
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            tool.validateCommon(args)
        );
        assertTrue(exception.getMessage().contains("'workItemId' inválido"));
    }

    @Test
    void testValidateCommon_InvalidWorkItemIdZero() {
        Map<String, Object> args = Map.of(
            "project", "TestProject",
            "workItemId", 0,
            "fileName", "test.txt",
            "dataBase64", "dGVzdA=="
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            tool.validateCommon(args)
        );
        assertTrue(exception.getMessage().contains("debe ser > 0"));
    }

    @Test
    void testValidateCommon_ValidArgs() {
        Map<String, Object> args = Map.of(
            "project", "TestProject",
            "workItemId", 123,
            "fileName", "test.txt",
            "dataBase64", "dGVzdA=="
        );

        // Mock the helper validation
        doNothing().when(attachmentsHelper).validateCreate("test.txt", "dGVzdA==");

        // Should not throw exception
        assertDoesNotThrow(() -> tool.validateCommon(args));
    }

    @Test
    void testGetInputSchema() {
        Map<String, Object> schema = tool.getInputSchema();
        
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties);
        
        assertTrue(properties.containsKey("project"));
        assertTrue(properties.containsKey("workItemId"));
        assertTrue(properties.containsKey("fileName"));
        assertTrue(properties.containsKey("dataBase64"));
        assertTrue(properties.containsKey("contentType"));
        assertTrue(properties.containsKey("comment"));
        assertTrue(properties.containsKey("apiVersion"));
        assertTrue(properties.containsKey("raw"));
    }

    @Test
    void testExecuteInternal_SuccessfulOperation() {
        // Preparar argumentos
        Map<String, Object> args = Map.of(
            "project", "TestProject",
            "workItemId", 123,
            "fileName", "test.txt",
            "dataBase64", Base64.getEncoder().encodeToString("test content".getBytes()),
            "comment", "Test comment"
        );

        // Mock helper methods
        byte[] testData = "test content".getBytes();
        when(attachmentsHelper.decodeBase64(anyString())).thenReturn(testData);
        when(attachmentsHelper.sanitizeContentType(null)).thenReturn("application/octet-stream");
        
        Map<String, Object> createResponse = Map.of(
            "id", "att-123",
            "url", "https://dev.azure.com/org/_apis/wit/attachments/att-123"
        );
        when(attachmentsHelper.createAttachment(eq("test.txt"), eq(testData), eq("application/octet-stream")))
            .thenReturn(createResponse);
        
        Map<String, Object> linkResponse = Map.of("id", 123, "rev", 2);
        when(attachmentsHelper.linkAttachmentToWorkItem(
            eq("TestProject"), 
            eq(123), 
            eq("https://dev.azure.com/org/_apis/wit/attachments/att-123"), 
            eq("Test comment"), 
            eq("7.2-preview")))
            .thenReturn(linkResponse);

        // Mock validation
        doNothing().when(attachmentsHelper).validateCreate(anyString(), anyString());

        // Ejecutar
        Map<String, Object> result = tool.executeInternal(args);

        // Verificar
        assertNotNull(result);
        assertFalse((Boolean) result.getOrDefault("isError", false));
        assertTrue(result.get("data").toString().contains("WorkItem: 123"));
        assertTrue(result.get("data").toString().contains("Archivo: test.txt"));
    }

    @Test
    void testExecuteInternal_CreateAttachmentFails() {
        // Preparar argumentos
        Map<String, Object> args = Map.of(
            "project", "TestProject",
            "workItemId", 123,
            "fileName", "test.txt",
            "dataBase64", Base64.getEncoder().encodeToString("test content".getBytes())
        );

        // Mock helper methods
        byte[] testData = "test content".getBytes();
        when(attachmentsHelper.decodeBase64(anyString())).thenReturn(testData);
        when(attachmentsHelper.sanitizeContentType(null)).thenReturn("application/octet-stream");
        
        Map<String, Object> createResponse = Map.of(
            "isHttpError", true,
            "httpStatus", 400,
            "message", "Bad Request"
        );
        when(attachmentsHelper.createAttachment(eq("test.txt"), eq(testData), eq("application/octet-stream")))
            .thenReturn(createResponse);

        // Mock validation
        doNothing().when(attachmentsHelper).validateCreate(anyString(), anyString());

        // Ejecutar
        Map<String, Object> result = tool.executeInternal(args);

        // Verificar error
        assertNotNull(result);
        assertTrue((Boolean) result.getOrDefault("isError", false));
    }
}
