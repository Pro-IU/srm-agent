package io.github.oudexin.srm.agent.tools;

import com.alibaba.fastjson2.JSON;
import io.github.oudexin.srm.agent.business.srm.dto.SrmActionType;
import io.github.oudexin.srm.agent.business.srm.service.SrmControlledActionService;
import io.github.oudexin.srm.agent.business.srm.service.SrmReadService;
import io.github.oudexin.srm.agent.business.srm.service.SrmValidationException;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Future Agent-callable SRM tool facade. It is not registered with, or used by, any SRM Agent in Step 3.
 * High-risk methods only create/consume explicit human confirmation requests; they never mutate on preview.
 */
@Component
public class SrmStructuredDataTools {
    private final SrmReadService readService;
    private final SrmControlledActionService controlledActionService;

    public SrmStructuredDataTools(SrmReadService readService, SrmControlledActionService controlledActionService) {
        this.readService = readService;
        this.controlledActionService = controlledActionService;
    }

    @Tool(name = "query_srm_supplier", description = "只读查询合成SRM供应商档案、资格状态和风险等级。不存在时返回 found=false/NOT_FOUND。")
    public String querySupplier(@ToolParam(name = "supplier_id", description = "内部供应商ID") String supplierId) {
        return response(() -> readService.findSupplier(supplierId));
    }

    @Tool(name = "query_srm_rfq", description = "只读查询合成SRM RFQ/寻源项目详情及参与报价摘要。不会执行授标。")
    public String queryRfq(@ToolParam(name = "rfq_id", description = "内部RFQ ID") String rfqId) {
        return response(() -> readService.findRfq(rfqId));
    }

    @Tool(name = "compare_srm_rfq_quotations", description = "按透明固定规则横向比较有效报价：价格45%、交期25%、账期15%、风险15%。结果不是模型建议或自动授标。")
    public String compareRfqQuotations(@ToolParam(name = "rfq_id", description = "内部RFQ ID") String rfqId) {
        return response(() -> readService.compareEligibleQuotations(rfqId));
    }

    @Tool(name = "query_srm_price_library", description = "只读查询物料价格库。include_inactive=false 仅返回VALID/EXPIRING；返回记录状态与当日可用标识。")
    public String queryPriceLibrary(
            @ToolParam(name = "material_code", description = "物料编码") String materialCode,
            @ToolParam(name = "include_inactive", description = "是否包含EXPIRED/SUSPENDED记录", required = false) Boolean includeInactive) {
        return response(() -> readService.findPriceLibrary(materialCode, Boolean.TRUE.equals(includeInactive)));
    }

    @Tool(name = "query_srm_purchase_order", description = "只读查询采购订单头、供应商状态、订单明细及履约数量。")
    public String queryPurchaseOrder(@ToolParam(name = "purchase_order_id", description = "内部采购订单ID") String purchaseOrderId) {
        return response(() -> readService.findPurchaseOrder(purchaseOrderId));
    }

    @Tool(name = "preview_srm_award", description = "生成RFQ授标预览和一次性确认令牌；不会授标。仅EVALUATING RFQ、有效SUBMITTED报价和QUALIFIED供应商可预览。")
    public String previewAward(
            @ToolParam(name = "rfq_id", description = "内部RFQ ID") String rfqId,
            @ToolParam(name = "quote_id", description = "拟中选报价ID") String quoteId,
            @ToolParam(name = "requested_by", description = "发起预览的人工操作人标识") String requestedBy) {
        return response(() -> controlledActionService.previewAward(rfqId, quoteId, requestedBy));
    }

    @Tool(name = "preview_srm_enable_price_record", description = "生成已停用价格记录的启用预览和一次性确认令牌；不会启用。")
    public String previewEnablePriceRecord(
            @ToolParam(name = "price_record_id", description = "价格库记录ID") String priceRecordId,
            @ToolParam(name = "requested_by", description = "发起预览的人工操作人标识") String requestedBy) {
        return response(() -> controlledActionService.previewEnablePriceRecord(priceRecordId, requestedBy));
    }

    @Tool(name = "preview_srm_suspend_price_record", description = "生成有效/临期价格记录的停用预览和一次性确认令牌；不会停用。")
    public String previewSuspendPriceRecord(
            @ToolParam(name = "price_record_id", description = "价格库记录ID") String priceRecordId,
            @ToolParam(name = "requested_by", description = "发起预览的人工操作人标识") String requestedBy) {
        return response(() -> controlledActionService.previewSuspendPriceRecord(priceRecordId, requestedBy));
    }

    @Tool(name = "preview_srm_purchase_order_approval", description = "生成采购订单审批预览和一次性确认令牌；不会审批或下达订单。")
    public String previewPurchaseOrderApproval(
            @ToolParam(name = "purchase_order_id", description = "内部采购订单ID") String purchaseOrderId,
            @ToolParam(name = "requested_by", description = "发起预览的人工操作人标识") String requestedBy) {
        return response(() -> controlledActionService.previewPurchaseOrderApproval(purchaseOrderId, requestedBy));
    }

    @Tool(name = "confirm_srm_high_risk_action", description = "唯一的SRM高风险写入入口。必须由人工在查看对应预览后显式回传 action_type、target_id 和一次性 confirmation_token；令牌过期、错配或重复使用会被拒绝。")
    public String confirmHighRiskAction(
            @ToolParam(name = "action_type", description = "AWARD_RFQ/ENABLE_PRICE_RECORD/SUSPEND_PRICE_RECORD/APPROVE_PURCHASE_ORDER") String actionType,
            @ToolParam(name = "target_id", description = "预览中返回的目标ID") String targetId,
            @ToolParam(name = "confirmation_token", description = "预览中返回的随机一次性令牌") String confirmationToken,
            @ToolParam(name = "confirmed_by", description = "执行显式人工确认的操作人标识") String confirmedBy) {
        return response(() -> controlledActionService.confirm(parseActionType(actionType), targetId, confirmationToken, confirmedBy));
    }

    @Tool(name = "query_srm_action_audit", description = "查询当前进程内的SRM受控写操作审计记录。仅用于演示；重启后会清空。")
    public String queryActionAudit() {
        return response(controlledActionService::listAuditRecords);
    }

    private SrmActionType parseActionType(String value) {
        if (value == null || value.isBlank()) {
            throw new SrmValidationException("actionType不能为空");
        }
        try {
            return SrmActionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new SrmValidationException("不支持的actionType");
        }
    }

    private String response(Supplier<Object> call) {
        try {
            return JSON.toJSONString(call.get());
        } catch (SrmValidationException exception) {
            return JSON.toJSONString(new ToolError(false, "VALIDATION_ERROR", exception.getMessage()));
        }
    }

    private record ToolError(boolean success, String code, String message) {
    }
}
