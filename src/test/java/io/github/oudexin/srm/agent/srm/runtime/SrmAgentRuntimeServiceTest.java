package io.github.oudexin.srm.agent.srm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.oudexin.srm.agent.srm.ProcurementMasterAgent;
import io.github.oudexin.srm.agent.srm.SrmAgentName;
import io.github.oudexin.srm.agent.srm.SrmAgentTaskResult;
import io.github.oudexin.srm.agent.srm.SrmRoutePlan;
import io.github.oudexin.srm.agent.srm.SrmTaskIntent;
import io.github.oudexin.srm.agent.srm.SrmTaskRequest;
import io.github.oudexin.srm.agent.srm.SrmWorkflowResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SrmAgentRuntimeServiceTest {
    @Mock private ProcurementMasterAgent master;
    @Mock private SrmLiveTaskInterpreter liveInterpreter;
    private SrmRuntimeProperties properties;
    private SrmRuntimeTraceStore traces;
    private SrmAgentRuntimeService runtime;

    @BeforeEach
    void setUp() {
        properties = new SrmRuntimeProperties();
        traces = new SrmRuntimeTraceStore();
        runtime = new SrmAgentRuntimeService(master, properties, liveInterpreter, traces);
    }

    @Test
    void demoChatUsesStructuredMasterChainAndReturnsSanitisedTrace() {
        when(master.orchestrate(any())).thenReturn(workflow(SrmTaskIntent.SUPPLIER,
                SrmAgentTaskResult.success(SrmAgentName.SUPPLIER, "query_srm_supplier", "{\"found\":true}", List.of())));

        SrmAgentChatResponse response = runtime.chat(new SrmAgentChatCommand("查询供应商风险", "c1", null,
                Map.of("supplierId", "sup_demo_001"), "buyer"));

        assertEquals("DEMO", response.mode());
        assertEquals("SUPPLIER", response.route());
        assertEquals(List.of("SUPPLIER"), response.participants());
        assertTrue(response.answer().contains("found"));
        assertEquals("SUCCESS", runtime.trace(response.traceId()).status());
        verify(master).orchestrate(any(SrmTaskRequest.class));
        verify(liveInterpreter, never()).interpret(any(), any(), any(), any());
    }

    @Test
    void previewDoesNotAutoConfirmAndTokenIsExcludedFromTraceAndAnswer() {
        String preview = "{\"actionType\":\"AWARD_RFQ\",\"targetId\":\"rfq_1\",\"confirmationToken\":\"secret-once\",\"riskWarnings\":[\"人工复核\"]}";
        when(master.orchestrate(any())).thenReturn(workflow(SrmTaskIntent.AWARD_REVIEW,
                SrmAgentTaskResult.success(SrmAgentName.AWARD_REVIEW, "preview_srm_award", preview, List.of())));

        SrmAgentChatResponse response = runtime.chat(new SrmAgentChatCommand("授标预览", null, "AWARD_REVIEW",
                Map.of("rfqId", "rfq_1", "quoteId", "quote_1"), "buyer"));

        assertEquals(1, response.requiredConfirmations().size());
        assertEquals("secret-once", response.requiredConfirmations().getFirst().confirmationToken());
        assertFalse(response.answer().contains("secret-once"));
        assertFalse(runtime.trace(response.traceId()).toString().contains("secret-once"));
        verify(master).orchestrate(any(SrmTaskRequest.class));
    }

    @Test
    void confirmationOnlyUsesDedicatedExplicitFields() {
        when(master.orchestrate(any())).thenReturn(workflow(SrmTaskIntent.CONFIRMATION,
                SrmAgentTaskResult.success(SrmAgentName.PROCUREMENT_MASTER, "confirm_srm_high_risk_action",
                        "{\"success\":true,\"executed\":true}", List.of())));

        assertThrows(SrmRuntimeException.class, () -> runtime.confirm(new SrmConfirmationCommand("AWARD_RFQ", "rfq", null, "buyer")));
        String result = runtime.confirm(new SrmConfirmationCommand("AWARD_RFQ", "rfq", "random-token", "buyer"));

        assertTrue(result.contains("success"));
        ArgumentCaptor<SrmTaskRequest> captured = ArgumentCaptor.forClass(SrmTaskRequest.class);
        verify(master).orchestrate(captured.capture());
        assertEquals(SrmTaskIntent.CONFIRMATION, captured.getValue().requestedIntent());
        assertEquals("random-token", captured.getValue().explicitConfirmation().confirmationToken());
    }

    @Test
    void liveModeWithoutCredentialsFailsBeforeAnyToolChain() {
        properties.setMode(SrmRuntimeProperties.Mode.LIVE);
        properties.setModel("synthetic-model-name");
        SrmRuntimeException exception = assertThrows(SrmRuntimeException.class, () -> runtime.chat(
                new SrmAgentChatCommand("查询供应商", null, null, Map.of("supplierId", "sup_1"), "buyer")));
        assertEquals("LIVE_MODE_NOT_CONFIGURED", exception.getCode());
        verify(master, never()).orchestrate(any());
    }

    @Test
    void rejectsNaturalLanguageConfirmationAndUnknownParameters() {
        assertThrows(SrmRuntimeException.class, () -> runtime.chat(new SrmAgentChatCommand("确认授标", null, "CONFIRMATION", Map.of(), "buyer")));
        assertThrows(SrmRuntimeException.class, () -> runtime.chat(new SrmAgentChatCommand("供应商", null, null, Map.of("tool", "anything"), "buyer")));
        verify(master, never()).orchestrate(any());
    }

    private static SrmWorkflowResult workflow(SrmTaskIntent intent, SrmAgentTaskResult result) {
        return new SrmWorkflowResult(new SrmRoutePlan(intent, List.of(result.agent()), "test", false), List.of(result), "合成结果", false);
    }
}
