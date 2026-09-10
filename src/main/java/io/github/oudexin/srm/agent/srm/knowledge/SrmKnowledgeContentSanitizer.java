package io.github.oudexin.srm.agent.srm.knowledge;

/** Presentation-only normalisation. Retrieved text has no authority to alter routing, permissions or confirmations. */
final class SrmKnowledgeContentSanitizer {
    private SrmKnowledgeContentSanitizer() { }
    static String snippet(String text, int limit) {
        if (text == null) return "";
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").replaceAll("(?i)</?(system|tool|assistant)[^>]*>", "").trim();
        return cleaned.length() <= limit ? cleaned : cleaned.substring(0, limit) + "…";
    }
}
