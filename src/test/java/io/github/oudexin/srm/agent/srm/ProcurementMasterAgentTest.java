package io.github.oudexin.srm.agent.srm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcurementMasterAgentTest {
    @Mock private SrmControlledConfirmationGateway confirmationGateway;
    @Mock private SrmStructuredDataTools tools;

    @Test
    void crossDomainDelegatesSeriallyAndAggregatesStructuredResults() {
        List<SrmAgentName> invoked = new ArrayList<>();
        ProcurementMasterAgent master = master(new FakeRole(SrmAgentName.SUPPLIER, invoked, false),
                new FakeRole(SrmAgentName.SOURCING, invoked, false),
                new FakeRole(SrmAgentName.QUOTE_ANALYSIS, invoked, false));
        SrmTaskRequest request = new SrmTaskRequest("task_1", SrmTaskIntent.CROSS_DOMAIN,
                "跨域", Map.of("supplierId", "sup_1", "purchaseOrderId", "po_1"), "buyer_1", null);

        SrmWorkflowResult result = master.orchestrate(request);

        assertEquals(List.of(SrmAgentName.SUPPLIER, SrmAgentName.SOURCING, SrmAgentName.QUOTE_ANALYSIS), result.routePlan().participants());
        assertEquals(List.of(SrmAgentName.SUPPLIER, SrmAgentName.SOURCING, SrmAgentName.QUOTE_ANALYSIS), invoked);
        assertEquals(3, result.participantResults().size());
        assertTrue(result.participantResults().stream().allMatch(SrmAgentTaskResult::success));
        assertTrue(result.summary().contains("失败数=0"));
    }

    @Test
    void failedSubAgentDegradesSafelyAndNextRoleStillRuns() {
        List<SrmAgentName> invoked = new ArrayList<>();
        ProcurementMasterAgent master = master(new FakeRole(SrmAgentName.SUPPLIER, invoked, true),
                new FakeRole(SrmAgentName.PURCHASE_ORDER, invoked, false));
        SrmTaskRequest request = new SrmTaskRequest("task_2", SrmTaskIntent.AUTO,
                "供应商风险与采购订单履约", Map.of("supplierId", "sup_1", "purchaseOrderId", "po_1"), "buyer_1", null);

        SrmWorkflowResult result = master.orchestrate(request);

        assertEquals(List.of(SrmAgentName.SUPPLIER, SrmAgentName.PURCHASE_ORDER), invoked);
        assertFalse(result.participantResults().getFirst().success());
        assertTrue(result.participantResults().get(1).success());
    }

    @Test
    void awardReviewCanOnlyPreviewAndNeverAutoConfirm() {
        when(tools.previewAward("rfq_1", "quote_1", "buyer_1")).thenReturn("{\"preview\":true}");
        ProcurementMasterAgent master = master(new AwardReviewAgent(tools));
        SrmTaskRequest request = new SrmTaskRequest("task_3", SrmTaskIntent.AWARD_REVIEW, "授标",
                Map.of("rfqId", "rfq_1", "quoteId", "quote_1"), "buyer_1", null);

        SrmWorkflowResult result = master.orchestrate(request);

        assertEquals("preview_srm_award", result.participantResults().getFirst().toolName());
        assertTrue(result.requiresHumanConfirmation());
        verify(tools).previewAward("rfq_1", "quote_1", "buyer_1");
        verify(confirmationGateway, never()).confirm(any());
    }

    @Test
    void incompleteConfirmationCannotCallGatewayAndGuardBoundsDelegation() {
        ProcurementMasterAgent master = master();
        SrmTaskRequest request = new SrmTaskRequest("task_4", SrmTaskIntent.CONFIRMATION, "确认", Map.of(),
                "buyer_1", new SrmExplicitConfirmation("AWARD_RFQ", "rfq_1", null, "approver_1"));

        SrmWorkflowResult result = master.orchestrate(request);

        assertTrue(result.requiresHumanConfirmation());
        verify(confirmationGateway, never()).confirm(any());
        SrmDelegationGuard guard = new SrmDelegationGuard();
        assertTrue(guard.tryEnter(1));
        assertTrue(guard.tryEnter(1));
        assertTrue(guard.tryEnter(1));
        assertFalse(guard.tryEnter(1));
        assertFalse(guard.tryEnter(SrmDelegationGuard.MAX_DELEGATION_DEPTH + 1));
    }

    @Test
    void permissionMatrixKeepsConfirmAwayFromSubAgents() {
        when(confirmationGateway.permissions()).thenReturn(Set.of("confirm_srm_high_risk_action", "query_srm_action_audit"));
        ProcurementMasterAgent master = master(new SupplierAgent(tools), new QuoteAnalysisAgent(tools), new AwardReviewAgent(tools));

        Map<SrmAgentName, Set<String>> permissions = master.toolPermissionMatrix();

        assertTrue(permissions.get(SrmAgentName.PROCUREMENT_MASTER).contains("confirm_srm_high_risk_action"));
        assertFalse(permissions.get(SrmAgentName.SUPPLIER).contains("confirm_srm_high_risk_action"));
        assertFalse(permissions.get(SrmAgentName.QUOTE_ANALYSIS).contains("preview_srm_award"));
        assertFalse(permissions.get(SrmAgentName.AWARD_REVIEW).contains("confirm_srm_high_risk_action"));
    }

    private ProcurementMasterAgent master(SrmRoleAgent... roles) {
        return new ProcurementMasterAgent(new SrmTaskRouter(), confirmationGateway, List.of(roles));
    }

    private static final class FakeRole implements SrmRoleAgent {
        private final SrmAgentName name;
        private final List<SrmAgentName> invoked;
        private final boolean throwFailure;
        private FakeRole(SrmAgentName name, List<SrmAgentName> invoked, boolean throwFailure) {
            this.name = name; this.invoked = invoked; this.throwFailure = throwFailure;
        }
        @Override public SrmAgentName name() { return name; }
        @Override public Set<String> toolPermissions() { return Set.of("fake_" + name); }
        @Override public SrmAgentTaskResult handle(SrmAgentTaskContext context) {
            invoked.add(name);
            if (throwFailure) throw new IllegalStateException("synthetic failure");
            return SrmAgentTaskResult.success(name, "fake", "{}", List.of());
        }
    }
}
