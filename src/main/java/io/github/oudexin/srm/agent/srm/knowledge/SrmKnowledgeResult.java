package io.github.oudexin.srm.agent.srm.knowledge;

import java.util.List;

/** Normalised read-only result. Content remains untrusted evidence and never becomes a tool instruction. */
public record SrmKnowledgeResult(String status, String answer, List<SrmKnowledgeCitation> citations, String notice) {
    public SrmKnowledgeResult { citations = citations == null ? List.of() : List.copyOf(citations); }
    public static SrmKnowledgeResult unavailable(String notice) { return new SrmKnowledgeResult("UNAVAILABLE", "", List.of(), notice); }
    public static SrmKnowledgeResult noResults(String notice) { return new SrmKnowledgeResult("NO_RESULTS", "", List.of(), notice); }
}
