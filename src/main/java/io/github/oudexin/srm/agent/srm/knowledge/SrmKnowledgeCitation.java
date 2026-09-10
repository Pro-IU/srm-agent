package io.github.oudexin.srm.agent.srm.knowledge;

/** Fields are copied only when knowledge-engine actually returns them; no page number or URL is invented. */
public record SrmKnowledgeCitation(String documentId, String title, String source, String url, String snippet) {
}
