package io.github.oudexin.srm.agent.srm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.oudexin.srm.agent.srm.ProcurementMasterAgent;
import io.github.oudexin.srm.agent.srm.SrmAgentName;
import io.github.oudexin.srm.agent.srm.SrmAgentTaskResult;
import io.github.oudexin.srm.agent.srm.SrmRoutePlan;
import io.github.oudexin.srm.agent.srm.SrmTaskIntent;
import io.github.oudexin.srm.agent.srm.SrmWorkflowResult;
import io.github.oudexin.srm.agent.srm.knowledge.KnowledgeEngineClient;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeResult;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeRouteClassifier;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeSupportAgent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class SrmKnowledgeRoutingTest {
    @Test
    void dataOnlyNeverCallsKnowledgeAndHybridRunsDataBeforeKnowledge() {
        ProcurementMasterAgent master = Mockito.mock(ProcurementMasterAgent.class);
        KnowledgeEngineClient client = Mockito.mock(KnowledgeEngineClient.class);
        when(master.orchestrate(any())).thenReturn(workflow());
        when(client.search(any())).thenReturn(new SrmKnowledgeResult("AVAILABLE", "合成制度依据", List.of(), ""));
        SrmAgentRuntimeService runtime = runtime(master, client);

        assertEquals("SUPPLIER", runtime.chat(new SrmAgentChatCommand("查询供应商风险", null, null, Map.of("supplierId", "sup_demo_alpha"), "buyer")).route());
        verify(client, never()).search(any());
        Mockito.clearInvocations(master, client);
        runtime.chat(new SrmAgentChatCommand("查询供应商风险并说明准入制度", null, null, Map.of("supplierId", "sup_demo_alpha"), "buyer", "Bearer scoped"));
        InOrder order = Mockito.inOrder(master, client);
        order.verify(master).orchestrate(any()); order.verify(client).search(any());
    }
    @Test
    void knowledgeOnlyPropagatesAuthorizationAndNeverCallsMaster() {
        ProcurementMasterAgent master = Mockito.mock(ProcurementMasterAgent.class);
        KnowledgeEngineClient client = Mockito.mock(KnowledgeEngineClient.class);
        when(client.search(any())).thenReturn(SrmKnowledgeResult.noResults("合成知识为空"));
        SrmAgentRuntimeService runtime = runtime(master, client);

        SrmAgentChatResponse result = runtime.chat(new SrmAgentChatCommand("价格库制度 FAQ", null, null, Map.of(), "buyer", "Bearer scoped"));

        assertEquals("KNOWLEDGE_ONLY", result.route()); assertEquals("NO_RESULTS", result.knowledgeBasis().status());
        verify(master, never()).orchestrate(any()); verify(client).search(Mockito.argThat(query -> "Bearer scoped".equals(query.authorization())));
    }
    private static SrmAgentRuntimeService runtime(ProcurementMasterAgent master, KnowledgeEngineClient client) {
        return new SrmAgentRuntimeService(master, new SrmRuntimeProperties(), Mockito.mock(SrmLiveTaskInterpreter.class), new SrmRuntimeTraceStore(),
                new SrmKnowledgeRouteClassifier(), new SrmKnowledgeSupportAgent(client));
    }
    private static SrmWorkflowResult workflow() {
        SrmAgentTaskResult result = SrmAgentTaskResult.success(SrmAgentName.SUPPLIER, "query_srm_supplier", "{\"found\":true}", List.of());
        return new SrmWorkflowResult(new SrmRoutePlan(SrmTaskIntent.SUPPLIER, List.of(SrmAgentName.SUPPLIER), "test", false), List.of(result), "合成结构化结果", false);
    }
}
