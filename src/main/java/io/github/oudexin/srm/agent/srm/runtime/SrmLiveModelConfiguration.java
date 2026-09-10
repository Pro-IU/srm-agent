package io.github.oudexin.srm.agent.srm.runtime;

import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Dedicated optional model bean. Demo mode never calls it and it never receives domain-tool authority. */
@Configuration
public class SrmLiveModelConfiguration {
    @Bean("srmLiveModel")
    @ConditionalOnProperty(name = "srm.runtime.mode", havingValue = "LIVE")
    public Model srmLiveModel(SrmRuntimeProperties properties) {
        // AgentScope builds this bean at startup even in DEMO. A valid inert model name keeps DEMO key-free;
        // live calls are still refused unless the caller explicitly configures model + provider + key.
        String modelName = properties.getModel() == null || properties.getModel().isBlank()
                ? "qwen3.7-flash" : properties.getModel();
        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(modelName)
                .stream(false)
                .enableThinking(false);
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) builder.baseUrl(properties.getBaseUrl());
        return builder.build();
    }

    /** Keeps DEMO key-free; this path can never create a task or invoke a model. */
    @Bean
    @ConditionalOnMissingBean(SrmLiveTaskInterpreter.class)
    public SrmLiveTaskInterpreter unavailableLiveTaskInterpreter() {
        return (traceId, message, clientParameters, requestedBy) -> {
            throw new SrmRuntimeException("LIVE_MODE_NOT_CONFIGURED", "live 模式未配置 provider、model 或 API key");
        };
    }
}
