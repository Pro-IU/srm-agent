package io.github.oudexin.srm.agent.srm.runtime;

import java.util.List;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeResult;

public record SrmAgentChatResponse(String answer, String mode, String route, List<String> participants,
        List<SrmToolCallSummary> toolCalls, List<SrmRequiredConfirmation> requiredConfirmations,
        String traceId, String status, List<String> dataFacts, SrmKnowledgeResult knowledgeBasis) {
    public SrmAgentChatResponse {
        participants = List.copyOf(participants);
        toolCalls = List.copyOf(toolCalls);
        requiredConfirmations = List.copyOf(requiredConfirmations);
        dataFacts = dataFacts == null ? List.of() : List.copyOf(dataFacts);
    }
    public SrmAgentChatResponse(String answer, String mode, String route, List<String> participants,
            List<SrmToolCallSummary> toolCalls, List<SrmRequiredConfirmation> requiredConfirmations,
            String traceId, String status) {
        this(answer, mode, route, participants, toolCalls, requiredConfirmations, traceId, status, List.of(), null);
    }
}
