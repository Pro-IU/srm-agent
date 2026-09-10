package io.github.oudexin.srm.agent.business.srm.dto;

import java.time.Instant;
import java.util.List;

/** Process-local audit event; Step 3 intentionally does not claim durable audit retention. */
public record SrmActionAuditRecord(
        String auditId,
        Instant occurredAt,
        SrmActionType actionType,
        String targetId,
        String requestedBy,
        String confirmedBy,
        boolean executed,
        String outcome,
        List<String> changeSummary) {
}
