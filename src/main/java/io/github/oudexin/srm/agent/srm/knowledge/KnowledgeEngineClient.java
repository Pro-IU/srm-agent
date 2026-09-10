package io.github.oudexin.srm.agent.srm.knowledge;

/** Narrow read-only adapter boundary for the separately owned knowledge-engine. */
public interface KnowledgeEngineClient {
    SrmKnowledgeResult search(SrmKnowledgeQuery query);
}
