package com.mcp.server.services.helpers;

public class HtmlHelperTest {

    public static void testMarkdownRejection() {
        String[] markdownInputs = {
            "# Heading 1",
            "## Heading 2",
            "**bold text**",
            "__bold text__",
            "*italic text*",
            "- list item",
            "* list item",
            "+ list item",
            "1. numbered item",
            "`inline code`",
            "```code block```",
            "> blockquote",
            "[link](https://example.com)",
            "---",
            "***",
            "===\n---\n==="
        };

        System.out.println("=== Testing Markdown Rejection ===");
        int passed = 0;
        int failed = 0;
        for (String md : markdownInputs) {
            try {
                AzureDevOpsRichHtmlHelper.normalize(md, "test field");
                System.out.println("✗ FAIL - Markdown not rejected: " + md);
                failed++;
            } catch (IllegalArgumentException e) {
                System.out.println("✓ PASS - Markdown rejected: " + md);
                passed++;
            }
        }
        System.out.println("Markdown rejection: " + passed + " passed, " + failed + " failed\n");
    }

    public static void testPlainRejection() {
        String[] plainInputs = {
            "This is plain text",
            "Hello world",
            "Multiple\n\nlines\n\nhere",
            "Some text with numbers 123 and special chars @#$"
        };

        System.out.println("=== Testing Plain Text Rejection ===");
        int passed = 0;
        int failed = 0;
        for (String plain : plainInputs) {
            try {
                AzureDevOpsRichHtmlHelper.normalize(plain, "test field");
                System.out.println("✗ FAIL - Plain text not rejected: " + plain);
                failed++;
            } catch (IllegalArgumentException e) {
                System.out.println("✓ PASS - Plain text rejected: " + plain);
                passed++;
            }
        }
        System.out.println("Plain text rejection: " + passed + " passed, " + failed + " failed\n");
    }

    public static void testHtmlAcceptance() {
        String[] htmlInputs = {
            "<p>This is HTML</p>",
            "<p>Paragraph one</p><p>Paragraph two</p>",
            "<b>bold</b> and <i>italic</i>",
            "<ul><li>item 1</li><li>item 2</li></ul>",
            "<table><tr><td>cell</td></tr></table>",
            "<p>Description with <b>formatting</b></p>",
            "<p>Acceptance criteria: <ul><li>Criterion 1</li></ul></p>"
        };

        System.out.println("=== Testing HTML Acceptance ===");
        int passed = 0;
        int failed = 0;
        for (String html : htmlInputs) {
            try {
                String result = AzureDevOpsRichHtmlHelper.normalize(html, "test field");
                System.out.println("✓ PASS - HTML accepted: " + html);
                System.out.println("  Result: " + result);
                passed++;
            } catch (IllegalArgumentException e) {
                System.out.println("✗ FAIL - HTML rejected: " + html + " | Error: " + e.getMessage());
                failed++;
            }
        }
        System.out.println("HTML acceptance: " + passed + " passed, " + failed + " failed\n");
    }

    public static void testNullAndBlank() {
        System.out.println("=== Testing Null and Blank ===");
        try {
            String result1 = AzureDevOpsRichHtmlHelper.normalize(null, "test");
            System.out.println("✓ PASS - null accepted: " + result1);
        } catch (Exception e) {
            System.out.println("✗ FAIL - null rejected: " + e.getMessage());
        }
        try {
            String result2 = AzureDevOpsRichHtmlHelper.normalize("   ", "test");
            System.out.println("✓ PASS - blank accepted: " + result2);
        } catch (Exception e) {
            System.out.println("✗ FAIL - blank rejected: " + e.getMessage());
        }
        System.out.println();
    }

    public static void testEnrichIfHtmlField() {
        System.out.println("=== Testing enrichIfHtmlField ===");
        try {
            String html = "<p>Rich description</p>";
            Object result = AzureDevOpsRichHtmlHelper.enrichIfHtmlField("System.Description", html);
            System.out.println("✓ PASS - HTML field enriched: " + result);
        } catch (Exception e) {
            System.out.println("✗ FAIL - HTML field error: " + e.getMessage());
        }
        try {
            Object result2 = AzureDevOpsRichHtmlHelper.enrichIfHtmlField("System.Title", "Plain title");
            System.out.println("✓ PASS - Non-HTML field unchanged: " + result2);
        } catch (Exception e) {
            System.out.println("✗ FAIL - Non-HTML field error: " + e.getMessage());
        }
        System.out.println();
    }

    public static void testErrorMessage() {
        System.out.println("=== Testing Error Messages ===");
        try {
            AzureDevOpsRichHtmlHelper.normalize("# Heading", "System.Description");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ PASS - Error message: " + e.getMessage());
        }
        try {
            AzureDevOpsRichHtmlHelper.normalize("Plain text", "comment text");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ PASS - Error message: " + e.getMessage());
        }
        System.out.println();
    }

    public static void main(String[] args) {
        testMarkdownRejection();
        testPlainRejection();
        testHtmlAcceptance();
        testNullAndBlank();
        testEnrichIfHtmlField();
        testErrorMessage();
        System.out.println("=== All tests completed ===");
    }
}
