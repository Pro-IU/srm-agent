package io.github.oudexin.srm.agent.srm.knowledge;

/** Authorization is an opaque caller-provided credential forwarded to knowledge-engine, never stored or logged. */
public record SrmKnowledgeQuery(String query, String requestedBy, String authorization) {
}
