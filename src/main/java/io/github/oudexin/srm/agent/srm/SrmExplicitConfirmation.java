package io.github.oudexin.srm.agent.srm;

/** Explicit values provided by a human after a preview; absent values must never trigger confirmation. */
public record SrmExplicitConfirmation(String actionType, String targetId, String confirmationToken, String confirmedBy) {
    public boolean isComplete() {
        return nonBlank(actionType) && nonBlank(targetId) && nonBlank(confirmationToken) && nonBlank(confirmedBy);
    }

    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }
}
