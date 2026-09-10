package io.github.oudexin.srm.agent.srm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.oudexin.srm.agent.srm.SrmTaskIntent;
import io.agentscope.core.model.Model;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AgentScopeSrmLiveTaskInterpreterTest {
    @Test
    void onlyWhitelistedIntentAndParametersAreAccepted() {
        SrmRuntimeProperties properties = liveProperties();
        AgentScopeSrmLiveTaskInterpreter interpreter = new AgentScopeSrmLiveTaskInterpreter(Mockito.mock(Model.class), properties);

        assertEquals(SrmTaskIntent.QUOTE_ANALYSIS, interpreter.validate("t1",
                "{\"intent\":\"QUOTE_ANALYSIS\",\"parameters\":{\"rfqId\":\"rfq_1\"}}", Map.of(), "buyer").requestedIntent());
        assertThrows(SrmRuntimeException.class, () -> interpreter.validate("t2",
                "{\"intent\":\"CONFIRMATION\",\"parameters\":{}}", Map.of(), "buyer"));
        assertThrows(SrmRuntimeException.class, () -> interpreter.validate("t3",
                "{\"intent\":\"SUPPLIER\",\"parameters\":{\"tool\":\"confirm\"}}", Map.of(), "buyer"));
    }
    private static SrmRuntimeProperties liveProperties() {
        SrmRuntimeProperties properties = new SrmRuntimeProperties();
        properties.setMode(SrmRuntimeProperties.Mode.LIVE);
        properties.setProvider("dashscope"); properties.setModel("model"); properties.setApiKey("placeholder");
        return properties;
    }
}
