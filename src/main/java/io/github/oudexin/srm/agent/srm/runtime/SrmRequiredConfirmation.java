package io.github.oudexin.srm.agent.srm.runtime;

import java.time.Instant;
import java.util.List;

/** Returned only to the caller that requested a preview; it is never copied into trace/audit telemetry. */
public record SrmRequiredConfirmation(String actionType, String targetId, String confirmationToken,
        Instant expiresAt, List<String> riskWarnings) {
    public SrmRequiredConfirmation { riskWarnings = riskWarnings == null ? List.of() : List.copyOf(riskWarnings); }
}
