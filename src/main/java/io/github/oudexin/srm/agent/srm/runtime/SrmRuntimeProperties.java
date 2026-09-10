package io.github.oudexin.srm.agent.srm.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Runtime switches for the portfolio-only SRM entrypoint. Demo is intentionally the safe default. */
@ConfigurationProperties(prefix = "srm.runtime")
public class SrmRuntimeProperties {
    public enum Mode { DEMO, LIVE }

    private Mode mode = Mode.DEMO;
    private String provider = "dashscope";
    private String model = "";
    private String baseUrl = "";
    private String apiKey = "";
    private int timeoutSeconds = 15;
    private int maxMessageLength = 2000;

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode == null ? Mode.DEMO : mode; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxMessageLength() { return maxMessageLength; }
    public void setMaxMessageLength(int maxMessageLength) { this.maxMessageLength = maxMessageLength; }

    public boolean isLiveConfigured() {
        return mode == Mode.LIVE && nonBlank(provider) && nonBlank(model) && nonBlank(apiKey);
    }
    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }
}
