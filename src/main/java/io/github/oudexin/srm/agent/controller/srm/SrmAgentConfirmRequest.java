package io.github.oudexin.srm.agent.controller.srm;

/** Human must explicitly echo all preview-bound fields; conversational assent is deliberately unsupported. */
public record SrmAgentConfirmRequest(String actionType, String targetId, String confirmationToken, String confirmedBy) {
}
