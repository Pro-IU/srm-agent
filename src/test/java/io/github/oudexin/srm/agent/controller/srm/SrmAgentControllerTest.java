package io.github.oudexin.srm.agent.controller.srm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.oudexin.srm.agent.srm.runtime.SrmAgentChatResponse;
import io.github.oudexin.srm.agent.srm.runtime.SrmAgentRuntimeService;
import io.github.oudexin.srm.agent.srm.runtime.SrmConfirmationCommand;
import io.github.oudexin.srm.agent.srm.runtime.SrmRuntimeException;
import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SrmAgentControllerTest {
    @Test
    void delegatesStableChatRequestAndDoesNotTreatNullAsConfirmation() {
        SrmAgentRuntimeService runtime = Mockito.mock(SrmAgentRuntimeService.class);
        SrmAgentController controller = new SrmAgentController(runtime, Mockito.mock(SrmStructuredDataTools.class));
        SrmAgentChatResponse expected = new SrmAgentChatResponse("answer", "DEMO", "SUPPLIER", List.of(), List.of(), List.of(), "trace", "SUCCESS");
        when(runtime.chat(any())).thenReturn(expected);

        assertEquals(expected, controller.chat(new SrmAgentChatRequest("供应商", "c", null, Map.of("supplierId", "s"), "buyer")));
        when(runtime.confirm(any())).thenThrow(new SrmRuntimeException("CONFIRMATION_FIELDS_REQUIRED", "fields required"));
        assertThrows(SrmRuntimeException.class, () -> controller.confirm(null));
    }
}
