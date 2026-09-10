package io.github.oudexin.srm.agent.srm;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class QuoteAnalysisAgent implements SrmRoleAgent {
    private static final Set<String> TOOLS = Set.of("compare_srm_rfq_quotations");
    private final SrmStructuredDataTools tools;
    public QuoteAnalysisAgent(SrmStructuredDataTools tools) { this.tools = tools; }
    @Override public SrmAgentName name() { return SrmAgentName.QUOTE_ANALYSIS; }
    @Override public Set<String> toolPermissions() { return TOOLS; }
    @Override public SrmAgentTaskResult handle(SrmAgentTaskContext context) {
        String rfqId = context.request().parameter("rfqId");
        if (rfqId == null || rfqId.isBlank()) return SrmAgentTaskResult.failure(name(), "rfqId缺失，未调用工具");
        return SrmAgentTaskResult.success(name(), "compare_srm_rfq_quotations", tools.compareRfqQuotations(rfqId),
                List.of("仅解释工具返回的价格、交期、账期、风险名次和固定权重；不自造事实或自动授标。",
                        SrmAgentSystemPolicy.REQUIRED_CONSTRAINTS.get(2)));
    }
}
