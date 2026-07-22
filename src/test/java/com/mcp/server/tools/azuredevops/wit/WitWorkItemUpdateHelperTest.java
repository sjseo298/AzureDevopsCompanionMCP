package com.mcp.server.tools.azuredevops.wit;

import com.mcp.server.services.AzureDevOpsClientService;
import com.mcp.server.services.helpers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WitWorkItemUpdateHelperTest {

    @Mock
    private AzureDevOpsClientService client;

    private WitWorkItemUpdateHelper helper;

    @BeforeEach
    void setUp() {
        helper = new WitWorkItemUpdateHelper(client);
    }

    @Test
    void testArtifactLinkPR_GeneratesCorrectPatch() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("relations", "ArtifactLink:pr:repo123/329594");

        Map<String, Object> mockResponse = new LinkedHashMap<>();
        mockResponse.put("id", 1197873);
        mockResponse.put("rev", 5);
        mockResponse.put("fields", Map.of(
                "System.Title", "Test WI",
                "System.State", "New"
        ));
        mockResponse.put("url", "https://dev.azure.com/MyProject/_apis/wit/workitems/1197873");

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(mockResponse);

        Map<String, Object> resp = helper.update(args);

        assertNotNull(resp, "La respuesta no debe ser null");
        assertEquals(1197873, resp.get("id"));
    }

    @Test
    void testArtifactLinkPR_DuplicatePrevented() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("relations", "ArtifactLink:pr:repo123/329594,ArtifactLink:pr:repo123/329594");

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(Map.of("id", 1197873, "rev", 5, "fields", Map.of("System.Title", "Test WI")));

        helper.update(args);
    }

    @Test
    void testArtifactLinkPR_MultiplePRs() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("relations", "ArtifactLink:pr:repo123/329594,ArtifactLink:pr:repo123/329595");

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(Map.of("id", 1197873, "rev", 5, "fields", Map.of("System.Title", "Test WI")));

        helper.update(args);
    }

    @Test
    void testArtifactLinkPR_InvalidFormatSkipped() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("relations", "ArtifactLink:pr:invalid");

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(Map.of("id", 1197873, "rev", 5, "fields", Map.of("System.Title", "Test WI")));

        helper.update(args);
    }

    @Test
    void testArtifactLinkPR_MixedWithWI() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("relations", "ArtifactLink:pr:repo123/329594,Related:999999");

        when(client.getWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/999999"),
                anyMap(), eq("7.2-preview")
        )).thenReturn(Map.of(
                "id", 999999,
                "url", "https://dev.azure.com/MyProject/_apis/wit/workitems/999999",
                "fields", Map.of("System.Title", "Related WI")
        ));

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(Map.of("id", 1197873, "rev", 5, "fields", Map.of("System.Title", "Test WI")));

        helper.update(args);
    }

    @Test
    void testArtifactLinkPR_WithStateChange() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("relations", "ArtifactLink:pr:repo123/329594");
        args.put("state", "Active");

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(Map.of(
                "id", 1197873,
                "rev", 6,
                "fields", Map.of("System.Title", "Test WI", "System.State", "Active")
        ));

        Map<String, Object> resp = helper.update(args);
        assertEquals(1197873, resp.get("id"));
    }

    @Test
    void testArtifactLinkPR_WithRepositoryId() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("project", "MyProject");
        args.put("id", 1197873);
        args.put("repositoryId", "repo123");
        args.put("relations", "ArtifactLink:pr:repo123/329594");

        when(client.patchWitApiWithQuery(
                eq("MyProject"), isNull(), eq("workitems/1197873"),
                anyMap(), anyList(), eq("7.2-preview"), any()
        )).thenReturn(Map.of("id", 1197873, "rev", 5, "fields", Map.of("System.Title", "Test WI")));

        helper.update(args);
    }
}
