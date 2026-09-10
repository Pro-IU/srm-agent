package io.github.oudexin.srm.agent.srm;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Deterministic router: no LLM is used to choose a role, which keeps the Step-4 contract testable offline. */
@Component
public class SrmTaskRouter {
    public SrmRoutePlan plan(SrmTaskRequest request) {
        SrmTaskIntent explicit = request.requestedIntent();
        if (explicit != SrmTaskIntent.AUTO) {
            return forIntent(explicit, "调用方指定的结构化意图");
        }
        String text = normalized(request.userRequest());
        boolean award = contains(text, "授标", "定点", "中选");
        boolean purchaseOrder = contains(text, "采购订单", "订单", "po", "履约", "收货", "审批");
        boolean quote = contains(text, "报价", "比价", "价格比较", "评分");
        boolean rfq = contains(text, "rfq", "寻源", "询价");
        boolean price = contains(text, "价格库", "临期价格", "停用价格", "启用价格");
        boolean supplier = contains(text, "供应商", "准入", "风险");
        int domains = bool(award) + bool(purchaseOrder) + bool(quote) + bool(rfq) + bool(price) + bool(supplier);
        if (domains > 1) {
            List<SrmAgentName> participants = new ArrayList<>();
            if (supplier) participants.add(SrmAgentName.SUPPLIER);
            if (rfq || price) participants.add(SrmAgentName.SOURCING);
            if (quote) participants.add(SrmAgentName.QUOTE_ANALYSIS);
            if (award) participants.add(SrmAgentName.AWARD_REVIEW);
            if (purchaseOrder) participants.add(SrmAgentName.PURCHASE_ORDER);
            return new SrmRoutePlan(SrmTaskIntent.CROSS_DOMAIN,
                    participants.stream().limit(SrmDelegationGuard.MAX_DELEGATION_COUNT).toList(),
                    "检测到多个SRM业务域，按固定顺序串行委派；超出上限的域要求拆分任务。", false);
        }
        if (award) return forIntent(SrmTaskIntent.AWARD_REVIEW, "检测到授标/定点关键词");
        if (purchaseOrder) return forIntent(SrmTaskIntent.PURCHASE_ORDER, "检测到采购订单/履约/审批关键词");
        if (quote) return forIntent(SrmTaskIntent.QUOTE_ANALYSIS, "检测到报价比较关键词");
        if (rfq) return forIntent(SrmTaskIntent.SOURCING, "检测到RFQ/寻源关键词");
        if (price) return forIntent(SrmTaskIntent.PRICE_LIBRARY, "检测到价格库关键词");
        if (supplier) return forIntent(SrmTaskIntent.SUPPLIER, "检测到供应商/准入/风险关键词");
        return forIntent(SrmTaskIntent.UNKNOWN, "无法从请求识别SRM结构化意图，安全回退到Master说明。");
    }

    private SrmRoutePlan forIntent(SrmTaskIntent intent, String reason) {
        return switch (intent) {
            case SUPPLIER -> new SrmRoutePlan(intent, List.of(SrmAgentName.SUPPLIER), reason, false);
            case SOURCING, PRICE_LIBRARY -> new SrmRoutePlan(intent, List.of(SrmAgentName.SOURCING), reason, false);
            case QUOTE_ANALYSIS -> new SrmRoutePlan(intent, List.of(SrmAgentName.QUOTE_ANALYSIS), reason, false);
            case AWARD_REVIEW -> new SrmRoutePlan(intent, List.of(SrmAgentName.AWARD_REVIEW), reason, false);
            case PURCHASE_ORDER -> new SrmRoutePlan(intent, List.of(SrmAgentName.PURCHASE_ORDER), reason, false);
            case CONFIRMATION -> new SrmRoutePlan(intent, List.of(), reason, false);
            case CROSS_DOMAIN -> new SrmRoutePlan(intent, List.of(SrmAgentName.SUPPLIER, SrmAgentName.SOURCING,
                    SrmAgentName.QUOTE_ANALYSIS), reason + "；默认最多三个角色。", false);
            case AUTO, UNKNOWN -> new SrmRoutePlan(SrmTaskIntent.UNKNOWN, List.of(), reason, true);
        };
    }

    private static String normalized(String text) { return text == null ? "" : text.toLowerCase(); }
    private static boolean contains(String text, String... tokens) {
        for (String token : tokens) if (text.contains(token)) return true;
        return false;
    }
    private static int bool(boolean value) { return value ? 1 : 0; }
}
