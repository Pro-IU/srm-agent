package io.github.oudexin.srm.agent.srm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.oudexin.srm.agent.srm.ProcurementMasterAgent;
import io.github.oudexin.srm.agent.srm.SrmControlledConfirmationGateway;
import io.github.oudexin.srm.agent.srm.SrmTaskRouter;
import io.github.oudexin.srm.agent.srm.SupplierAgent;
import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** No database, LLM or network: exercises the local chat entrypoint through the real Master and a real role. */
class SrmDemoRuntimeContractTest {
    @Test
    void demoChatRunsMasterThenRoleThenWhitelistedStructuredTool() {
        SrmStructuredDataTools tools = Mockito.mock(SrmStructuredDataTools.class);
        when(tools.querySupplier("sup_demo_001")).thenReturn("{\"found\":true,\"data\":{\"supplierId\":\"sup_demo_001\"}}");
        ProcurementMasterAgent master = new ProcurementMasterAgent(new SrmTaskRouter(),
                Mockito.mock(SrmControlledConfirmationGateway.class), List.of(new SupplierAgent(tools)));
        SrmAgentRuntimeService runtime = new SrmAgentRuntimeService(master, new SrmRuntimeProperties(),
                Mockito.mock(SrmLiveTaskInterpreter.class), new SrmRuntimeTraceStore());

        SrmAgentChatResponse response = runtime.chat(new SrmAgentChatCommand("查询供应商风险", null, null,
                Map.of("supplierId", "sup_demo_001"), "demo-buyer"));

        assertEquals("SUPPLIER", response.route());
        assertEquals(List.of("SUPPLIER"), response.participants());
        assertTrue(response.answer().contains("sup_demo_001"));
        verify(tools).querySupplier("sup_demo_001");
    }
}
