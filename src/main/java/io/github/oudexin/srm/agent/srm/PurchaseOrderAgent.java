package io.github.oudexin.srm.agent.srm;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderAgent implements SrmRoleAgent {
    private static final Set<String> TOOLS = Set.of("query_srm_purchase_order", "preview_srm_purchase_order_approval");
    private final SrmStructuredDataTools tools;
    public PurchaseOrderAgent(SrmStructuredDataTools tools) { this.tools = tools; }
    @Override public SrmAgentName name() { return SrmAgentName.PURCHASE_ORDER; }
    @Override public Set<String> toolPermissions() { return TOOLS; }
    @Override public SrmAgentTaskResult handle(SrmAgentTaskContext context) {
        String purchaseOrderId = context.request().parameter("purchaseOrderId");
        if (purchaseOrderId == null || purchaseOrderId.isBlank()) return SrmAgentTaskResult.failure(name(), "purchaseOrderId缺失，未调用工具");
        boolean approvalRequested = "true".equalsIgnoreCase(context.request().parameter("previewApproval"));
        if (!approvalRequested) {
            return SrmAgentTaskResult.success(name(), "query_srm_purchase_order", tools.queryPurchaseOrder(purchaseOrderId),
                    List.of("只读返回订单和履约状态；未设置previewApproval=true时不会生成审批预览。"));
        }
        return SrmAgentTaskResult.success(name(), "preview_srm_purchase_order_approval",
                tools.previewPurchaseOrderApproval(purchaseOrderId, context.request().requestedBy()),
                List.of("仅生成审批预览，绝不调用确认或下达订单。", SrmAgentSystemPolicy.REQUIRED_CONSTRAINTS.get(3)));
    }
}
