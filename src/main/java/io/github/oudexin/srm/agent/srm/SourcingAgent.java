package io.github.oudexin.srm.agent.srm;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SourcingAgent implements SrmRoleAgent {
    private static final Set<String> TOOLS = Set.of("query_srm_rfq", "query_srm_price_library");
    private final SrmStructuredDataTools tools;
    public SourcingAgent(SrmStructuredDataTools tools) { this.tools = tools; }
    @Override public SrmAgentName name() { return SrmAgentName.SOURCING; }
    @Override public Set<String> toolPermissions() { return TOOLS; }
    @Override public SrmAgentTaskResult handle(SrmAgentTaskContext context) {
        if (context.intent() == SrmTaskIntent.PRICE_LIBRARY || context.request().parameter("materialCode") != null) {
            String materialCode = context.request().parameter("materialCode");
            if (materialCode == null || materialCode.isBlank()) return SrmAgentTaskResult.failure(name(), "materialCode缺失，未调用工具");
            return SrmAgentTaskResult.success(name(), "query_srm_price_library", tools.queryPriceLibrary(materialCode, true),
                    List.of("价格库状态来自结构化工具；临期/停用记录不会被表述为可自动下单。"));
        }
        String rfqId = context.request().parameter("rfqId");
        if (rfqId == null || rfqId.isBlank()) return SrmAgentTaskResult.failure(name(), "rfqId缺失，未调用工具");
        return SrmAgentTaskResult.success(name(), "query_srm_rfq", tools.queryRfq(rfqId),
                List.of(SrmAgentSystemPolicy.REQUIRED_CONSTRAINTS.get(1)));
    }
}
