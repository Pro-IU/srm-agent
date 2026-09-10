package io.github.oudexin.srm.agent.srm;

import java.util.List;

/** Observable route plan; agents are serial by construction to preserve deterministic context ordering. */
public record SrmRoutePlan(SrmTaskIntent intent, List<SrmAgentName> participants, String reason, boolean fallback) {
    public SrmRoutePlan { participants = List.copyOf(participants); }
}
