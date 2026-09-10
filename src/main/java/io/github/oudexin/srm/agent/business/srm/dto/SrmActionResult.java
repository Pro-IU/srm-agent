package io.github.oudexin.srm.agent.business.srm.dto;

import java.time.Instant;

public record SrmActionResult(
        boolean executed,
        SrmActionType actionType,
        String targetId,
        String auditId,
        Instant executedAt,
        String message) {
}
