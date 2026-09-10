package io.github.oudexin.srm.agent.srm;

import java.util.List;

/** Master output includes the plan and participant results for observability. */
public record SrmWorkflowResult(
        SrmRoutePlan routePlan,
        List<SrmAgentTaskResult> participantResults,
        String summary,
        boolean requiresHumanConfirmation) {
    public SrmWorkflowResult { participantResults = List.copyOf(participantResults); }
}
