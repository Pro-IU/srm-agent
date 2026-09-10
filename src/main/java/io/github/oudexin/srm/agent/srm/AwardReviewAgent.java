package io.github.oudexin.srm.agent.srm;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AwardReviewAgent implements SrmRoleAgent {
    private static final Set<String> TOOLS = Set.of("query_srm_rfq", "preview_srm_award");
    private final SrmStructuredDataTools tools;
    public AwardReviewAgent(SrmStructuredDataTools tools) { this.tools = tools; }
    @Override public SrmAgentName name() { return SrmAgentName.AWARD_REVIEW; }
    @Override public Set<String> toolPermissions() { return TOOLS; }
    @Override public SrmAgentTaskResult handle(SrmAgentTaskContext context) {
        String rfqId = context.request().parameter("rfqId");
        String quoteId = context.request().parameter("quoteId");
        if (rfqId == null || rfqId.isBlank()) return SrmAgentTaskResult.failure(name(), "rfqId缺失，未调用工具");
        if (quoteId == null || quoteId.isBlank()) {
            return SrmAgentTaskResult.success(name(), "query_srm_rfq", tools.queryRfq(rfqId),
                    List.of("授标复核仅返回RFQ/报价事实；缺少quoteId时不会生成预览。"));
        }
        return SrmAgentTaskResult.success(name(), "preview_srm_award",
                tools.previewAward(rfqId, quoteId, context.request().requestedBy()),
                List.of("仅生成授标预览，绝不调用确认；必须由人工显式回传一次性令牌。",
                        SrmAgentSystemPolicy.REQUIRED_CONSTRAINTS.get(3)));
    }
}
