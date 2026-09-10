package io.github.oudexin.srm.agent.srm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Structured SRM entrypoint. It routes and aggregates role outputs but never performs a high-risk write itself.
 * Confirmation is delegated only to the guarded Step-3 gateway and only with complete explicit human fields.
 */
@Component
public class ProcurementMasterAgent {
    private final SrmTaskRouter router;
    private final SrmControlledConfirmationGateway confirmationGateway;
    private final Map<SrmAgentName, SrmRoleAgent> agents;

    public ProcurementMasterAgent(SrmTaskRouter router, SrmControlledConfirmationGateway confirmationGateway,
            List<SrmRoleAgent> roleAgents) {
        this.router = router;
        this.confirmationGateway = confirmationGateway;
        Map<SrmAgentName, SrmRoleAgent> index = new EnumMap<>(SrmAgentName.class);
        for (SrmRoleAgent roleAgent : roleAgents) index.put(roleAgent.name(), roleAgent);
        this.agents = Map.copyOf(index);
    }

    public SrmWorkflowResult orchestrate(SrmTaskRequest request) {
        SrmRoutePlan plan = router.plan(request);
        if (plan.intent() == SrmTaskIntent.CONFIRMATION) return confirmOnly(request, plan);
        if (plan.fallback()) return new SrmWorkflowResult(plan, List.of(),
                "无法安全路由；请提供supplierId、rfqId、materialCode或purchaseOrderId等结构化参数。", false);
        SrmDelegationGuard guard = new SrmDelegationGuard();
        List<SrmAgentTaskResult> results = new ArrayList<>();
        for (SrmAgentName agentName : plan.participants()) {
            if (!guard.tryEnter(1)) {
                results.add(SrmAgentTaskResult.failure(agentName, "已达到最大委派次数/深度，停止委派以避免循环。"));
                break;
            }
            SrmRoleAgent agent = agents.get(agentName);
            if (agent == null) {
                results.add(SrmAgentTaskResult.failure(agentName, "角色未配置，安全跳过。"));
                continue;
            }
            try {
                results.add(agent.handle(new SrmAgentTaskContext(request, plan.intent(), 1, guard.count())));
            } catch (RuntimeException exception) {
                results.add(SrmAgentTaskResult.failure(agentName, "子Agent失败，已降级：" + exception.getMessage()));
            }
        }
        boolean needsConfirmation = plan.participants().contains(SrmAgentName.AWARD_REVIEW)
                || plan.participants().contains(SrmAgentName.PURCHASE_ORDER);
        return new SrmWorkflowResult(plan, results, summarize(plan, results), needsConfirmation);
    }

    /** Permission matrix entry for tests/observability; Master has no preview tools and only an explicit confirmation gateway. */
    public Map<SrmAgentName, Set<String>> toolPermissionMatrix() {
        Map<SrmAgentName, Set<String>> matrix = new EnumMap<>(SrmAgentName.class);
        matrix.put(SrmAgentName.PROCUREMENT_MASTER, confirmationGateway.permissions());
        agents.forEach((name, agent) -> matrix.put(name, agent.toolPermissions()));
        return Map.copyOf(matrix);
    }

    private SrmWorkflowResult confirmOnly(SrmTaskRequest request, SrmRoutePlan plan) {
        SrmExplicitConfirmation confirmation = request.explicitConfirmation();
        if (confirmation == null || !confirmation.isComplete()) {
            return new SrmWorkflowResult(plan, List.of(),
                    "未提供完整人工确认参数；不会调用确认工具，也不会执行任何高风险操作。", true);
        }
        String payload = confirmationGateway.confirm(confirmation);
        SrmAgentTaskResult result = SrmAgentTaskResult.success(SrmAgentName.PROCUREMENT_MASTER,
                SrmControlledConfirmationGateway.CONFIRM_TOOL, payload,
                List.of("确认调用仅因完整显式人工参数而发生；工具仍会校验令牌、目标、时效和重复使用。"));
        return new SrmWorkflowResult(plan, List.of(result), "受控确认入口已被显式调用；请以工具JSON结果为准。", false);
    }

    private String summarize(SrmRoutePlan plan, List<SrmAgentTaskResult> results) {
        String names = results.stream().map(result -> result.agent().getDisplayName()).collect(Collectors.joining(" → "));
        long failures = results.stream().filter(result -> !result.success()).count();
        return "路由=" + plan.intent() + "；参与角色=" + names + "；失败数=" + failures
                + "。所有结论仅来自结构化工具结果；高风险写入仍需预览后人工显式确认。";
    }
}
