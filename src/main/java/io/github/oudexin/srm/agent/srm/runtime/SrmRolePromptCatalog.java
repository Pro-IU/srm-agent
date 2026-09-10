package io.github.oudexin.srm.agent.srm.runtime;

import io.github.oudexin.srm.agent.srm.SrmAgentName;
import java.util.EnumMap;
import java.util.Map;

/** Versioned role constraints supplied to the optional AgentScope model adapter; never exposed by APIs. */
public final class SrmRolePromptCatalog {
    private SrmRolePromptCatalog() { }
    public static final String RUNTIME_GUARDRAIL = """
            这是个人作品集的 SRM 演示，全部数据均为合成数据。只能根据受控结构化工具返回事实作答，
            不得虚构供应商、价格或原公司/生产落地。报价比较采用固定透明规则（价格45%、交期25%、账期15%、风险15%），
            不是模型决策。授标、价格启停和订单审批只能生成预览；必须等待独立 API 的人工显式确认。
            输出只允许一个 JSON 对象：intent 和 parameters；不得选择工具、Bean、方法或确认操作。
            """;
    public static Map<SrmAgentName, String> prompts() {
        Map<SrmAgentName, String> prompts = new EnumMap<>(SrmAgentName.class);
        prompts.put(SrmAgentName.PROCUREMENT_MASTER, "统一入口：路由、串行汇总和安全提示，绝不自动确认。");
        prompts.put(SrmAgentName.SUPPLIER, "供应商档案、准入和风险查询解释。");
        prompts.put(SrmAgentName.SOURCING, "RFQ、寻源进度、参与报价和价格库查询。");
        prompts.put(SrmAgentName.QUOTE_ANALYSIS, "解释确定性报价比较及业务权衡，不生成新事实。");
        prompts.put(SrmAgentName.AWARD_REVIEW, "授标候选复核，只能生成预览，禁止确认。");
        prompts.put(SrmAgentName.PURCHASE_ORDER, "采购订单履约查询和审批预览建议，禁止确认。");
        return Map.copyOf(prompts);
    }
}
