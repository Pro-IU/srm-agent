package io.github.oudexin.srm.agent.srm;

import java.util.List;

/** Structured sub-agent result; toolPayload is the JSON emitted by Step-3 tool facade. */
public record SrmAgentTaskResult(
        SrmAgentName agent,
        boolean success,
        String toolName,
        String toolPayload,
        List<String> safetyNotes,
        String failureReason) {
    public static SrmAgentTaskResult success(SrmAgentName agent, String toolName, String payload, List<String> notes) {
        return new SrmAgentTaskResult(agent, true, toolName, payload, List.copyOf(notes), null);
    }

    public static SrmAgentTaskResult failure(SrmAgentName agent, String reason) {
        return new SrmAgentTaskResult(agent, false, null, null, List.of(), reason);
    }
}
