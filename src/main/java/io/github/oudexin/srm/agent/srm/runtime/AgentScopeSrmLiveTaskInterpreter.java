package io.github.oudexin.srm.agent.srm.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.oudexin.srm.agent.srm.SrmTaskIntent;
import io.github.oudexin.srm.agent.srm.SrmTaskRequest;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.Model;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Actual AgentScope model adapter for explicitly configured live mode. The model can classify only a whitelisted
 * task envelope; Step-4 roles still perform all domain reads and Step-3 remains the only write gateway.
 */
@Component
@ConditionalOnProperty(name = "srm.runtime.mode", havingValue = "LIVE")
public class AgentScopeSrmLiveTaskInterpreter implements SrmLiveTaskInterpreter {
    private static final Set<String> PARAMETER_WHITELIST = Set.of(
            "supplierId", "rfqId", "quoteId", "materialCode", "purchaseOrderId", "previewApproval");
    private final Model model;
    private final SrmRuntimeProperties properties;

    public AgentScopeSrmLiveTaskInterpreter(@Qualifier("srmLiveModel") Model model, SrmRuntimeProperties properties) {
        this.model = model;
        this.properties = properties;
    }

    @Override
    public SrmTaskRequest interpret(String traceId, String message, Map<String, String> clientParameters, String requestedBy) {
        if (!properties.isLiveConfigured()) throw new SrmRuntimeException("LIVE_MODE_NOT_CONFIGURED", "live 模式未配置 provider、model 或 API key");
        if (!"dashscope".equalsIgnoreCase(properties.getProvider().trim()))
            throw new SrmRuntimeException("LIVE_PROVIDER_NOT_SUPPORTED", "当前 SRM live 模式仅支持 dashscope provider");
        List<Msg> input = List.of(
                Msg.builder().role(MsgRole.SYSTEM).name("srm-runtime").content(TextBlock.builder()
                        .text(SrmRolePromptCatalog.RUNTIME_GUARDRAIL).build()).build(),
                Msg.builder().role(MsgRole.USER).name("user").content(TextBlock.builder().text(message).build()).build());
        List<ChatResponse> responses;
        try {
            responses = model.stream(input, null, null).collectList()
                    .block(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())));
        } catch (RuntimeException exception) {
            throw new SrmRuntimeException("LIVE_MODEL_UNAVAILABLE", "live 模型调用失败；未执行任何 SRM 操作");
        }
        return validate(traceId, extractText(responses), clientParameters, requestedBy);
    }

    SrmTaskRequest validate(String traceId, String output, Map<String, String> clientParameters, String requestedBy) {
        try {
            JSONObject root = JSON.parseObject(output);
            SrmTaskIntent intent = SrmTaskIntent.valueOf(root.getString("intent"));
            if (intent == SrmTaskIntent.CONFIRMATION || intent == SrmTaskIntent.AUTO || intent == SrmTaskIntent.UNKNOWN) {
                throw new IllegalArgumentException("intent is not allowed in live output");
            }
            Map<String, String> parameters = new LinkedHashMap<>();
            JSONObject generated = root.getJSONObject("parameters");
            if (generated != null) {
                for (String key : generated.keySet()) {
                    if (!PARAMETER_WHITELIST.contains(key)) throw new IllegalArgumentException("unknown parameter");
                    Object value = generated.get(key);
                    if (!(value instanceof String) && !(value instanceof Boolean)) throw new IllegalArgumentException("invalid parameter value");
                    parameters.put(key, String.valueOf(value));
                }
            }
            if (clientParameters != null) {
                for (Map.Entry<String, String> entry : clientParameters.entrySet()) {
                    if (!PARAMETER_WHITELIST.contains(entry.getKey())) throw new IllegalArgumentException("unknown client parameter");
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
            return new SrmTaskRequest(traceId, intent, "live-validated", parameters, requestedBy, null);
        } catch (RuntimeException exception) {
            throw new SrmRuntimeException("LIVE_OUTPUT_REJECTED", "live 模型输出未通过 SRM 白名单校验；未执行任何操作");
        }
    }

    private static String extractText(List<ChatResponse> responses) {
        if (responses == null) return "";
        StringBuilder text = new StringBuilder();
        for (ChatResponse response : responses) for (ContentBlock block : response.getContent())
            if (block instanceof TextBlock textBlock) text.append(textBlock.getText());
        return text.toString();
    }
}
