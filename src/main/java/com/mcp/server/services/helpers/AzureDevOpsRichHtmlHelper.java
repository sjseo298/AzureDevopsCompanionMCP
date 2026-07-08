package com.mcp.server.services.helpers;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normaliza HTML enriquecido al subconjunto que Azure DevOps conserva bien en campos HTML.
 */
public final class AzureDevOpsRichHtmlHelper {
    private static final Set<String> HTML_FIELDS = Set.of(
            "System.Description",
            "Microsoft.VSTS.Common.AcceptanceCriteria",
            "Microsoft.VSTS.TCM.ReproSteps",
            "Microsoft.VSTS.TCM.SystemInfo",
            "Microsoft.VSTS.Common.Resolution"
    );

    private static final String TABLE_STYLE = "border-collapse:collapse;width:100%;";
    private static final String TH_STYLE = "border:1px solid #d0d7de;background-color:#f6f8fa;padding:6px 8px;text-align:left;font-weight:600;";
    private static final String TD_STYLE = "border:1px solid #d0d7de;padding:6px 8px;";

    private AzureDevOpsRichHtmlHelper() {}

    public static boolean isKnownHtmlField(String referenceName) {
        return referenceName != null && HTML_FIELDS.contains(referenceName);
    }

    public static Object enrichIfHtmlField(String referenceName, Object value) {
        if (!isKnownHtmlField(referenceName) || !(value instanceof String text)) {
            return value;
        }
        return normalize(text);
    }

    public static String normalize(String input) {
        if (input == null || input.isBlank()) return input;
        String html = looksLikeHtml(input) ? input : plainTextToHtml(input);
        html = sanitizeDangerousHtml(html);
        html = applyAzureDevOpsTableStyle(html);
        return html;
    }

    private static boolean looksLikeHtml(String input) {
        return Pattern.compile("<\\s*/?\\s*[a-zA-Z][a-zA-Z0-9]*(\\s|>|/>)").matcher(input).find();
    }

    private static String plainTextToHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        String[] paragraphs = escaped.split("\\R\\s*\\R");
        StringBuilder sb = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim().replaceAll("\\R", "<br/>");
            if (!p.isEmpty()) sb.append("<p>").append(p).append("</p>");
        }
        return sb.toString();
    }

    private static String sanitizeDangerousHtml(String html) {
        String out = html;
        out = out.replaceAll("(?is)<\\s*(script|iframe|object|embed|style)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>", "");
        out = out.replaceAll("(?is)<\\s*(script|iframe|object|embed|style)\\b[^>]*/\\s*>", "");
        out = out.replaceAll("(?i)\\s+on[a-zA-Z]+\\s*=\\s*\"[^\"]*\"", "");
        out = out.replaceAll("(?i)\\s+on[a-zA-Z]+\\s*=\\s*'[^']*'", "");
        out = out.replaceAll("(?i)href\\s*=\\s*\"\\s*javascript:[^\"]*\"", "href=\"#\"");
        out = out.replaceAll("(?i)href\\s*=\\s*'\\s*javascript:[^']*'", "href=\"#\"");
        return out;
    }

    private static String applyAzureDevOpsTableStyle(String html) {
        String out = replaceTagWithStyle(html, "table", TABLE_STYLE);
        out = replaceTagWithStyle(out, "th", TH_STYLE);
        out = replaceTagWithStyle(out, "td", TD_STYLE);
        return out;
    }

    private static String replaceTagWithStyle(String html, String tagName, String style) {
        Pattern p = Pattern.compile("(?i)<" + tagName + "\\b([^>]*)>");
        Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String attrs = m.group(1) == null ? "" : m.group(1);
            attrs = attrs.replaceAll("(?i)\\sstyle\\s*=\\s*\"[^\"]*\"", "");
            attrs = attrs.replaceAll("(?i)\\sstyle\\s*=\\s*'[^']*'", "");
            String replacement = "<" + tagName + " style=\"" + style + "\"" + attrs + ">";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
