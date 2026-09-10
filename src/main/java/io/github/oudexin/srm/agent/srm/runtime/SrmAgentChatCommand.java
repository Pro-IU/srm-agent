package io.github.oudexin.srm.agent.srm.runtime;

import java.util.Map;

public record SrmAgentChatCommand(String message, String conversationId, String intent,
        Map<String, String> parameters, String requestedBy, String knowledgeAuthorization) {
    public SrmAgentChatCommand(String message, String conversationId, String intent,
            Map<String, String> parameters, String requestedBy) {
        this(message, conversationId, intent, parameters, requestedBy, null);
    }
}
