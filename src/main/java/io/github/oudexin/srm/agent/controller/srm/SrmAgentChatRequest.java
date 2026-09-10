package io.github.oudexin.srm.agent.controller.srm;

import java.util.Map;

/** Public request intentionally contains no prompt, tool selection, token or arbitrary method fields. */
public record SrmAgentChatRequest(String message, String conversationId, String intent,
        Map<String, String> parameters, String requestedBy) {
}
