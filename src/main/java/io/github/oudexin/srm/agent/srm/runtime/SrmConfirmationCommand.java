package io.github.oudexin.srm.agent.srm.runtime;

public record SrmConfirmationCommand(String actionType, String targetId, String confirmationToken, String confirmedBy) {
}
