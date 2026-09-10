package io.github.oudexin.srm.agent.srm;

import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Deliberately separate from role agents: only ProcurementMasterAgent uses this after complete explicit confirmation. */
@Component
public class SrmControlledConfirmationGateway {
    public static final String CONFIRM_TOOL = "confirm_srm_high_risk_action";
    private final SrmStructuredDataTools tools;

    public SrmControlledConfirmationGateway(SrmStructuredDataTools tools) { this.tools = tools; }

    public String confirm(SrmExplicitConfirmation confirmation) {
        return tools.confirmHighRiskAction(confirmation.actionType(), confirmation.targetId(),
                confirmation.confirmationToken(), confirmation.confirmedBy());
    }

    public Set<String> permissions() { return Set.of(CONFIRM_TOOL, "query_srm_action_audit"); }
}
