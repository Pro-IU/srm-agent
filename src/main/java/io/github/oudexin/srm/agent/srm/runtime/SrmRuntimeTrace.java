package io.github.oudexin.srm.agent.srm.runtime;

import java.time.Instant;
import java.util.List;

/** Sanitised observability projection: no original message, prompt, API key or confirmation token. */
public record SrmRuntimeTrace(String traceId, Instant startedAt, Instant finishedAt, String mode,
        String route, List<String> participants, List<SrmToolCallSummary> toolCalls, String status, long durationMs) {
    public SrmRuntimeTrace { participants = List.copyOf(participants); toolCalls = List.copyOf(toolCalls); }
}
