package io.github.oudexin.srm.agent.srm;

import java.util.Set;

/** AgentScope-compatible role contract: each role is a Spring component with an explicit least-privilege tool set. */
public interface SrmRoleAgent {
    SrmAgentName name();
    Set<String> toolPermissions();
    SrmAgentTaskResult handle(SrmAgentTaskContext context);
}
