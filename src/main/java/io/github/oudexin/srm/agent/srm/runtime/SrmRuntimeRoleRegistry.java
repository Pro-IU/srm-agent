package io.github.oudexin.srm.agent.srm.runtime;

import io.github.oudexin.srm.agent.srm.ProcurementMasterAgent;
import io.github.oudexin.srm.agent.srm.SrmAgentName;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Observable runtime assembly: the real Step-4 master and role/tool permission matrix are the execution chain. */
@Component
public class SrmRuntimeRoleRegistry {
    private final ProcurementMasterAgent master;
    public SrmRuntimeRoleRegistry(ProcurementMasterAgent master) { this.master = master; }
    public Map<SrmAgentName, Set<String>> allowedTools() { return master.toolPermissionMatrix(); }
    public Map<SrmAgentName, String> prompts() { return SrmRolePromptCatalog.prompts(); }
}
