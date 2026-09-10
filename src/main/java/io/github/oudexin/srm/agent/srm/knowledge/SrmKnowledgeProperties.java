package io.github.oudexin.srm.agent.srm.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "srm.knowledge-engine")
public class SrmKnowledgeProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:8009";
    private int connectTimeoutMillis = 1000;
    private int readTimeoutMillis = 8000;
    private int maxResults = 5;
    private int maxContentLength = 1200;
    private int maxAttempts = 1;
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; } public void setConnectTimeoutMillis(int value) { this.connectTimeoutMillis = value; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; } public void setReadTimeoutMillis(int value) { this.readTimeoutMillis = value; }
    public int getMaxResults() { return maxResults; } public void setMaxResults(int value) { this.maxResults = value; }
    public int getMaxContentLength() { return maxContentLength; } public void setMaxContentLength(int value) { this.maxContentLength = value; }
    public int getMaxAttempts() { return maxAttempts; } public void setMaxAttempts(int value) { this.maxAttempts = value; }
    public int boundedResults() { return Math.max(1, Math.min(5, maxResults)); }
    public int boundedContentLength() { return Math.max(200, Math.min(4000, maxContentLength)); }
    public int boundedAttempts() { return 1; } // retries are intentionally disabled to avoid duplicate knowledge conversations.
}
