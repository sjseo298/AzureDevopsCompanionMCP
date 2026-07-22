package com.mcp.server.services.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureDevOpsRichHtmlHelperTest {

    @Test
    void rejectsMarkdownWhenNoHtmlTagsPresent() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AzureDevOpsRichHtmlHelper.normalize("# Heading", "System.Description")
        );

        assertTrue(ex.getMessage().contains("markdown"));
    }

    @Test
    void rejectsPlainTextWhenNoHtmlAndNoMarkdown() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AzureDevOpsRichHtmlHelper.normalize("This is plain text", "System.Description")
        );

        assertTrue(ex.getMessage().contains("texto plano"));
    }

    @Test
    void acceptsHtmlEvenIfTextContainsMarkdownLikeTokens() {
        String input = "<p>Texto con **bold** y _italics_ literales</p>";
        String result = AzureDevOpsRichHtmlHelper.normalize(input, "System.Description");

        assertEquals(input, result);
    }

    @Test
    void sanitizesDangerousHtml() {
        String input = "<p onclick=\"alert(1)\">Hola</p><script>alert('x')</script>";
        String result = AzureDevOpsRichHtmlHelper.normalize(input, "System.Description");

        assertFalse(result.toLowerCase().contains("<script"));
        assertFalse(result.toLowerCase().contains("onclick="));
        assertTrue(result.contains("<p"));
    }

    @Test
    void appliesTableStyles() {
        String input = "<table><tr><th>H</th><td>V</td></tr></table>";
        String result = AzureDevOpsRichHtmlHelper.normalize(input, "System.Description");

        assertTrue(result.contains("<table style=\"border-collapse:collapse;width:100%;\""));
        assertTrue(result.contains("<th style=\"border:1px solid #d0d7de;background-color:#f6f8fa;padding:6px 8px;text-align:left;font-weight:600;\""));
        assertTrue(result.contains("<td style=\"border:1px solid #d0d7de;padding:6px 8px;\""));
    }

    @Test
    void keepsNullAndBlankUntouched() {
        assertEquals(null, AzureDevOpsRichHtmlHelper.normalize(null, "System.Description"));
        assertEquals("   ", AzureDevOpsRichHtmlHelper.normalize("   ", "System.Description"));
    }
}
