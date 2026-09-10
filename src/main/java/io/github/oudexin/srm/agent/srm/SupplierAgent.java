package io.github.oudexin.srm.agent.srm;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SupplierAgent implements SrmRoleAgent {
    private static final Set<String> TOOLS = Set.of("query_srm_supplier");
    private final SrmStructuredDataTools tools;
    public SupplierAgent(SrmStructuredDataTools tools) { this.tools = tools; }
    @Override public SrmAgentName name() { return SrmAgentName.SUPPLIER; }
    @Override public Set<String> toolPermissions() { return TOOLS; }
    @Override public SrmAgentTaskResult handle(SrmAgentTaskContext context) {
        String supplierId = context.request().parameter("supplierId");
        if (supplierId == null || supplierId.isBlank()) return SrmAgentTaskResult.failure(name(), "supplierId缺失，未调用工具");
        return SrmAgentTaskResult.success(name(), "query_srm_supplier", tools.querySupplier(supplierId),
                List.of(SrmAgentSystemPolicy.REQUIRED_CONSTRAINTS.get(1)));
    }
}
