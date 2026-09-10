package io.github.oudexin.srm.agent.srm.knowledge;

import java.util.Set;
import org.springframework.stereotype.Component;

/** Separate read-only support role keeps the original six business Agents and their write boundaries unchanged. */
@Component
public class SrmKnowledgeSupportAgent {
    public static final String NAME = "KNOWLEDGE_SUPPORT";
    private static final Set<String> PERMISSIONS = Set.of("query_srm_knowledge");
    private final KnowledgeEngineClient client;
    public SrmKnowledgeSupportAgent(KnowledgeEngineClient client) { this.client = client; }
    public Set<String> toolPermissions() { return PERMISSIONS; }
    public SrmKnowledgeResult answer(String message, String requestedBy, String authorization) {
        return client.search(new SrmKnowledgeQuery(message, requestedBy, authorization));
    }
}
