package io.github.oudexin.srm.agent.srm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SrmTaskRouterTest {
    private final SrmTaskRouter router = new SrmTaskRouter();

    @Test
    void routesEachSingleDomainToItsLeastPrivilegeRole() {
        assertEquals(SrmAgentName.SUPPLIER, plan("查询供应商准入风险").participants().getFirst());
        assertEquals(SrmAgentName.SOURCING, plan("查询RFQ寻源进度").participants().getFirst());
        assertEquals(SrmAgentName.QUOTE_ANALYSIS, plan("比较报价评分").participants().getFirst());
        assertEquals(SrmAgentName.AWARD_REVIEW, plan("授标候选复核").participants().getFirst());
        assertEquals(SrmAgentName.SOURCING, plan("查询价格库临期记录").participants().getFirst());
        assertEquals(SrmAgentName.PURCHASE_ORDER, plan("采购订单履约状态").participants().getFirst());
    }

    @Test
    void crossDomainIsOrderedAndBounded() {
        SrmRoutePlan plan = plan("供应商风险与采购订单履约一起查看");
        assertEquals(SrmTaskIntent.CROSS_DOMAIN, plan.intent());
        assertEquals(java.util.List.of(SrmAgentName.SUPPLIER, SrmAgentName.PURCHASE_ORDER), plan.participants());
        assertTrue(plan.participants().size() <= SrmDelegationGuard.MAX_DELEGATION_COUNT);
    }

    @Test
    void unknownIntentFallsBackWithoutAnyRole() {
        SrmRoutePlan plan = plan("今天天气好吗");
        assertTrue(plan.fallback());
        assertTrue(plan.participants().isEmpty());
    }

    private SrmRoutePlan plan(String text) {
        return router.plan(new SrmTaskRequest("task_1", SrmTaskIntent.AUTO, text, Map.of(), "buyer_1", null));
    }
}
