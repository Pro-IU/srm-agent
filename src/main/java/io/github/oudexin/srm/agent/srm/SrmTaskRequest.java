package io.github.oudexin.srm.agent.srm;

import java.util.Map;

/** Structured task envelope. Parameters carry identifiers rather than relying on natural-language extraction. */
public record SrmTaskRequest(
        String taskId,
        SrmTaskIntent requestedIntent,
        String userRequest,
        Map<String, String> parameters,
        String requestedBy,
        SrmExplicitConfirmation explicitConfirmation) {
    public SrmTaskRequest {
        requestedIntent = requestedIntent == null ? SrmTaskIntent.AUTO : requestedIntent;
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public String parameter(String name) { return parameters.get(name); }
}
