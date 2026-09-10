package io.github.oudexin.srm.agent.business.srm.dto;

import java.time.Instant;
import java.util.List;

/** A pending, non-executing high-risk change. The token is random and single-use. */
public record SrmConfirmationPreview(
        SrmActionType actionType,
        String targetId,
        String confirmationToken,
        String requestedBy,
        Instant expiresAt,
        List<String> changeSummary,
        List<String> riskWarnings,
        String confirmationInstruction) {
}
