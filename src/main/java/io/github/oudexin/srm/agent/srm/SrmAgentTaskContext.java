package io.github.oudexin.srm.agent.srm;

/** Per-delegation context with explicit bounded depth/count supplied by the master. */
public record SrmAgentTaskContext(SrmTaskRequest request, SrmTaskIntent intent, int delegationDepth, int delegationCount) {
}
