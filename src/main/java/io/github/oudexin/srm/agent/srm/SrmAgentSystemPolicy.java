package io.github.oudexin.srm.agent.srm;

import java.util.List;

/** Shared system constraints for later ReActAgent wrappers; Step 4 keeps execution deterministic and offline. */
public final class SrmAgentSystemPolicy {
    private SrmAgentSystemPolicy() { }

    public static final List<String> REQUIRED_CONSTRAINTS = List.of(
            "本项目是个人作品集，所有SRM数据均为合成数据，不得声称来自原公司或生产环境。",
            "只可根据获授权工具返回的结构化数据回答，不得编造供应商、价格、订单或状态事实。",
            "报价比较是固定透明规则，不能表述为LLM自主决策或自动授标。",
            "授标、价格启停、订单审批默认不执行；先返回预览与风险，等待人工显式确认。",
            "任何角色都不得声称已完成未经confirm_srm_high_risk_action确认的高风险操作。"
    );
}
