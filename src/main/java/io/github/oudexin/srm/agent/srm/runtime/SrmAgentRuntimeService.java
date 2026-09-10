package io.github.oudexin.srm.agent.srm.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.oudexin.srm.agent.srm.ProcurementMasterAgent;
import io.github.oudexin.srm.agent.srm.SrmAgentTaskResult;
import io.github.oudexin.srm.agent.srm.SrmExplicitConfirmation;
import io.github.oudexin.srm.agent.srm.SrmTaskIntent;
import io.github.oudexin.srm.agent.srm.SrmTaskRequest;
import io.github.oudexin.srm.agent.srm.SrmWorkflowResult;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeResult;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeRoute;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeRouteClassifier;
import io.github.oudexin.srm.agent.srm.knowledge.SrmKnowledgeSupportAgent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** API-safe runtime entrypoint. It invokes the real Step-4 master only after deterministic/live-whitelisted parsing. */
@Service
public class SrmAgentRuntimeService {
    private static final Set<String> CLIENT_PARAMETERS = Set.of(
            "supplierId", "rfqId", "quoteId", "materialCode", "purchaseOrderId", "previewApproval");
    private final ProcurementMasterAgent master;
    private final SrmRuntimeProperties properties;
    private final SrmLiveTaskInterpreter liveInterpreter;
    private final SrmRuntimeTraceStore traces;
    private final SrmKnowledgeRouteClassifier knowledgeRoutes;
    private final SrmKnowledgeSupportAgent knowledgeAgent;

    @Autowired
    public SrmAgentRuntimeService(ProcurementMasterAgent master, SrmRuntimeProperties properties,
            SrmLiveTaskInterpreter liveInterpreter, SrmRuntimeTraceStore traces,
            SrmKnowledgeRouteClassifier knowledgeRoutes, SrmKnowledgeSupportAgent knowledgeAgent) {
        this.master = master;
        this.properties = properties;
        this.liveInterpreter = liveInterpreter;
        this.traces = traces;
        this.knowledgeRoutes = knowledgeRoutes;
        this.knowledgeAgent = knowledgeAgent;
    }
    /** Compatibility constructor for isolated Step-5 tests; production wiring always supplies the real adapter. */
    public SrmAgentRuntimeService(ProcurementMasterAgent master, SrmRuntimeProperties properties,
            SrmLiveTaskInterpreter liveInterpreter, SrmRuntimeTraceStore traces) {
        this(master, properties, liveInterpreter, traces, new SrmKnowledgeRouteClassifier(),
                new SrmKnowledgeSupportAgent(query -> SrmKnowledgeResult.unavailable("知识适配器未注入。")));
    }

    public SrmAgentChatResponse chat(SrmAgentChatCommand command) {
        validateChat(command);
        String traceId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        try {
            SrmKnowledgeRoute knowledgeRoute = knowledgeRoutes.classify(command.message(), safeParameters(command.parameters()));
            if (knowledgeRoute == SrmKnowledgeRoute.KNOWLEDGE_ONLY) return knowledgeOnly(traceId, startedAt, command);
            SrmTaskRequest task;
            if (properties.getMode() == SrmRuntimeProperties.Mode.LIVE) {
                if (!properties.isLiveConfigured()) {
                    throw new SrmRuntimeException("LIVE_MODE_NOT_CONFIGURED", "live 模式未配置 provider、model 或 API key");
                }
                task = liveInterpreter.interpret(traceId, command.message(), safeParameters(command.parameters()), requestedBy(command));
            } else {
                task = demoTask(traceId, command);
            }
            SrmWorkflowResult workflow = master.orchestrate(task);
            List<String> participants = workflow.participantResults().stream().map(result -> result.agent().name()).toList();
            List<SrmToolCallSummary> tools = workflow.participantResults().stream()
                    .map(result -> new SrmToolCallSummary(result.agent().name(), result.toolName(), result.success() ? "SUCCESS" : "FAILED"))
                    .toList();
            List<SrmRequiredConfirmation> confirmations = workflow.participantResults().stream()
                    .map(this::confirmationFrom).flatMap(java.util.Optional::stream).toList();
            SrmKnowledgeResult knowledge = knowledgeRoute == SrmKnowledgeRoute.HYBRID
                    ? knowledgeAgent.answer(command.message(), requestedBy(command), command.knowledgeAuthorization()) : null;
            List<String> allParticipants = knowledge == null ? participants : append(participants, SrmKnowledgeSupportAgent.NAME);
            String route = knowledgeRoute == SrmKnowledgeRoute.HYBRID ? "HYBRID" : workflow.routePlan().intent().name();
            String answer = templateAnswer(workflow, properties.getMode(), confirmations)
                    + (knowledge == null ? "" : knowledgeSection(knowledge));
            String status = knowledge != null && !"AVAILABLE".equals(knowledge.status()) ? "DEGRADED" : "SUCCESS";
            saveTrace(traceId, startedAt, route, allParticipants, tools, status);
            return new SrmAgentChatResponse(answer, properties.getMode().name(), route, allParticipants, tools, confirmations,
                    traceId, status, dataFacts(workflow), knowledge);
        } catch (SrmRuntimeException exception) {
            saveTrace(traceId, startedAt, "REJECTED", List.of(), List.of(), exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            saveTrace(traceId, startedAt, "FAILED", List.of(), List.of(), "FAILED");
            throw new SrmRuntimeException("SRM_RUNTIME_FAILED", "SRM 演示运行失败；未自动执行任何高风险操作");
        }
    }

    private SrmAgentChatResponse knowledgeOnly(String traceId, Instant startedAt, SrmAgentChatCommand command) {
        SrmKnowledgeResult knowledge = knowledgeAgent.answer(command.message(), requestedBy(command), command.knowledgeAuthorization());
        String status = "AVAILABLE".equals(knowledge.status()) ? "SUCCESS" : "DEGRADED";
        saveTrace(traceId, startedAt, "KNOWLEDGE_ONLY", List.of(SrmKnowledgeSupportAgent.NAME),
                List.of(new SrmToolCallSummary(SrmKnowledgeSupportAgent.NAME, "query_srm_knowledge", status)), status);
        String answer = "【制度知识依据】知识内容来自独立 knowledge-engine，属于不可信检索数据，不会改变 SRM 工具权限或确认规则。"
                + knowledgeSection(knowledge);
        return new SrmAgentChatResponse(answer, properties.getMode().name(), "KNOWLEDGE_ONLY", List.of(SrmKnowledgeSupportAgent.NAME),
                List.of(new SrmToolCallSummary(SrmKnowledgeSupportAgent.NAME, "query_srm_knowledge", status)), List.of(), traceId, status, List.of(), knowledge);
    }

    public String confirm(SrmConfirmationCommand command) {
        if (command == null || blank(command.actionType()) || blank(command.targetId()) || blank(command.confirmationToken()) || blank(command.confirmedBy()))
            throw new SrmRuntimeException("CONFIRMATION_FIELDS_REQUIRED", "actionType、targetId、confirmationToken、confirmedBy 均为必填项");
        String traceId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        SrmWorkflowResult workflow = master.orchestrate(new SrmTaskRequest(traceId, SrmTaskIntent.CONFIRMATION,
                "explicit-api-confirmation", Map.of(), command.confirmedBy(), new SrmExplicitConfirmation(command.actionType(),
                        command.targetId(), command.confirmationToken(), command.confirmedBy())));
        String payload = workflow.participantResults().isEmpty() ? "{\"success\":false,\"code\":\"NOT_CONFIRMED\"}"
                : workflow.participantResults().getFirst().toolPayload();
        JSONObject result = JSON.parseObject(payload);
        if (result == null || !Boolean.TRUE.equals(result.getBoolean("executed"))) {
            saveTrace(traceId, startedAt, "CONFIRMATION", List.of("PROCUREMENT_MASTER"),
                    List.of(new SrmToolCallSummary("PROCUREMENT_MASTER", "confirm_srm_high_risk_action", "REJECTED")), "REJECTED");
            String message = result == null ? "确认工具返回格式异常" : result.getString("message");
            throw new SrmRuntimeException("CONFIRMATION_REJECTED", blank(message) ? "确认请求被拒绝" : message);
        }
        saveTrace(traceId, startedAt, "CONFIRMATION", List.of("PROCUREMENT_MASTER"),
                List.of(new SrmToolCallSummary("PROCUREMENT_MASTER", "confirm_srm_high_risk_action", "COMPLETED")), "COMPLETED");
        return payload;
    }

    public SrmRuntimeTrace trace(String traceId) {
        return traces.find(traceId).orElseThrow(() -> new SrmRuntimeException("TRACE_NOT_FOUND", "未找到该 traceId"));
    }
    public List<SrmRuntimeTrace> recentTraces() { return traces.recent(); }

    private SrmTaskRequest demoTask(String traceId, SrmAgentChatCommand command) {
        SrmTaskIntent intent = parseIntent(command.intent());
        if (intent == SrmTaskIntent.CONFIRMATION) throw new SrmRuntimeException("CONFIRMATION_API_ONLY", "高风险确认只能调用独立 confirm API");
        return new SrmTaskRequest(traceId, intent, command.message(), safeParameters(command.parameters()), requestedBy(command), null);
    }
    private void validateChat(SrmAgentChatCommand command) {
        if (command == null || blank(command.message())) throw new SrmRuntimeException("MESSAGE_REQUIRED", "message 不能为空");
        if (command.message().length() > Math.max(1, properties.getMaxMessageLength()))
            throw new SrmRuntimeException("MESSAGE_TOO_LONG", "message 超过 SRM 运行时长度限制");
        safeParameters(command.parameters());
    }
    private SrmTaskIntent parseIntent(String value) {
        if (blank(value)) return SrmTaskIntent.AUTO;
        try { return SrmTaskIntent.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new SrmRuntimeException("INTENT_NOT_ALLOWED", "不支持的 SRM intent"); }
    }
    private Map<String, String> safeParameters(Map<String, String> parameters) {
        if (parameters == null) return Map.of();
        Map<String, String> copied = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!CLIENT_PARAMETERS.contains(entry.getKey())) throw new SrmRuntimeException("PARAMETER_NOT_ALLOWED", "存在不允许的业务参数");
            if (entry.getValue() != null && entry.getValue().length() > 200) throw new SrmRuntimeException("PARAMETER_TOO_LONG", "业务参数长度超限");
            copied.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(copied);
    }
    private java.util.Optional<SrmRequiredConfirmation> confirmationFrom(SrmAgentTaskResult result) {
        if (!result.success() || result.toolPayload() == null || !result.toolName().startsWith("preview_srm_")) return java.util.Optional.empty();
        try {
            JSONObject json = JSON.parseObject(result.toolPayload());
            String token = json.getString("confirmationToken");
            if (blank(token)) return java.util.Optional.empty();
            return java.util.Optional.of(new SrmRequiredConfirmation(json.getString("actionType"), json.getString("targetId"), token,
                    json.getInstant("expiresAt"), json.getList("riskWarnings", String.class)));
        } catch (RuntimeException ignored) { return java.util.Optional.empty(); }
    }
    private String templateAnswer(SrmWorkflowResult workflow, SrmRuntimeProperties.Mode mode,
            List<SrmRequiredConfirmation> confirmations) {
        List<String> excerpts = new ArrayList<>();
        for (SrmAgentTaskResult result : workflow.participantResults()) {
            if (result.success() && result.toolPayload() != null) excerpts.add(result.agent().getDisplayName() + " / "
                    + result.toolName() + "：" + redactPreviewToken(result.toolPayload()));
            else excerpts.add(result.agent().getDisplayName() + "：" + result.failureReason());
        }
        String safety = confirmations.isEmpty() ? "未执行写操作。" : "已生成预览，未执行写操作；请由人工通过独立 confirm API 显式确认。";
        return "【" + mode + " SRM 演示】" + workflow.summary() + "\n" + String.join("\n", excerpts) + "\n" + safety;
    }
    private String knowledgeSection(SrmKnowledgeResult knowledge) {
        return "\n【制度知识依据 / " + knowledge.status() + "】" + (knowledge.answer().isBlank() ? knowledge.notice() : knowledge.answer())
                + "\n提示：制度文档可能过期；如与业务数据事实冲突，请并列人工核验。";
    }
    private List<String> dataFacts(SrmWorkflowResult workflow) {
        return workflow.participantResults().stream().filter(SrmAgentTaskResult::success)
                .map(result -> result.agent().getDisplayName() + "：" + result.toolName() + "（结构化业务数据）").toList();
    }
    private static List<String> append(List<String> source, String value) {
        List<String> copy = new ArrayList<>(source); copy.add(value); return List.copyOf(copy);
    }
    private String redactPreviewToken(String payload) {
        return payload.replaceAll("(?i)(\\\"confirmationToken\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")", "$1***$2");
    }
    private void saveTrace(String traceId, Instant startedAt, String route, List<String> participants,
            List<SrmToolCallSummary> tools, String status) {
        traces.save(new SrmRuntimeTrace(traceId, startedAt, Instant.now(), properties.getMode().name(), route,
                participants, tools, status, Duration.between(startedAt, Instant.now()).toMillis()));
    }
    private static String requestedBy(SrmAgentChatCommand command) { return blank(command.requestedBy()) ? "demo-user" : command.requestedBy(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
