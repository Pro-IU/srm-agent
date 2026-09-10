package io.github.oudexin.srm.agent.srm.knowledge;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter for knowledge-engine's actual authenticated SSE endpoint (/chat/send), followed by its actual
 * conversation-message endpoint (/chat/messages) to obtain persisted ragReferences. No undocumented endpoint is used.
 */
@Component
public class HttpKnowledgeEngineClient implements KnowledgeEngineClient {
    private final SrmKnowledgeProperties properties;
    private final KnowledgeEngineHttpTransport transport;
    public HttpKnowledgeEngineClient(SrmKnowledgeProperties properties, KnowledgeEngineHttpTransport transport) {
        this.properties = properties; this.transport = transport;
    }
    @Override
    public SrmKnowledgeResult search(SrmKnowledgeQuery query) {
        if (!properties.isEnabled()) return SrmKnowledgeResult.unavailable("知识库集成未启用；结构化数据链路未受影响。");
        if (query == null || query.query() == null || query.query().isBlank()) return SrmKnowledgeResult.noResults("知识问题为空，未调用知识库。");
        if (query.authorization() == null || query.authorization().isBlank())
            return SrmKnowledgeResult.unavailable("知识库需要其自身的登录会话；未提供可转发的 Authorization，未检索。");
        String conversationId = "srm-" + UUID.randomUUID();
        try {
            HttpResponse<String> stream = transport.send(post(conversationId, query));
            if (stream.statusCode() == 401 || stream.statusCode() == 403) return SrmKnowledgeResult.unavailable("知识库拒绝当前登录会话；可见性由 knowledge-engine 自身角色过滤决定。");
            if (stream.statusCode() < 200 || stream.statusCode() >= 300) return SrmKnowledgeResult.unavailable("知识库服务返回 " + stream.statusCode() + "；未影响结构化结果。");
            String answer = extractSseAnswer(stream.body());
            HttpResponse<String> messages = transport.send(messages(conversationId, query.authorization()));
            if (messages.statusCode() == 401 || messages.statusCode() == 403) return SrmKnowledgeResult.unavailable("知识答案已生成但引用读取被知识库会话拒绝；不展示不可核验结论。");
            if (messages.statusCode() < 200 || messages.statusCode() >= 300) return SrmKnowledgeResult.unavailable("知识库引用读取失败；不展示不可核验结论。");
            return fromMessages(messages.body(), answer);
        } catch (java.net.http.HttpTimeoutException exception) {
            return SrmKnowledgeResult.unavailable("知识库读取超时；请稍后重试，结构化数据结果仍可使用。");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SrmKnowledgeResult.unavailable("知识库读取被中断；未重试。");
        } catch (Exception exception) {
            return SrmKnowledgeResult.unavailable("知识库暂不可用或响应格式异常；未影响结构化数据结果。");
        }
    }
    private HttpRequest post(String conversationId, SrmKnowledgeQuery query) {
        String body = "content=" + encode(query.query()) + "&conversationId=" + encode(conversationId);
        return HttpRequest.newBuilder(uri("/chat/send")).timeout(Duration.ofMillis(Math.max(1, properties.getReadTimeoutMillis())))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/event-stream").header("Authorization", query.authorization())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private HttpRequest messages(String conversationId, String authorization) {
        return HttpRequest.newBuilder(uri("/chat/messages?conversationId=" + encode(conversationId)))
                .timeout(Duration.ofMillis(Math.max(1, properties.getReadTimeoutMillis())))
                .header("Accept", "application/json").header("Authorization", authorization).GET().build();
    }
    private SrmKnowledgeResult fromMessages(String body, String sseAnswer) {
        try {
            JSONArray messages = JSON.parseArray(body);
            if (messages == null || messages.isEmpty()) return SrmKnowledgeResult.noResults("知识库未返回可核验消息或引用。");
            JSONObject assistant = messages.stream().filter(JSONObject.class::isInstance).map(JSONObject.class::cast)
                    .filter(item -> "ASSISTANT".equalsIgnoreCase(item.getString("type")) || "assistant".equalsIgnoreCase(item.getString("type")))
                    .max(Comparator.comparing(item -> item.getString("createdAt"), Comparator.nullsLast(String::compareTo))).orElse(null);
            if (assistant == null) assistant = messages.getJSONObject(messages.size() - 1);
            List<SrmKnowledgeCitation> citations = citations(assistant.getJSONArray("ragReferences"));
            String answer = SrmKnowledgeContentSanitizer.snippet(assistant.getString("content"), properties.boundedContentLength());
            if (answer.isBlank()) answer = SrmKnowledgeContentSanitizer.snippet(sseAnswer, properties.boundedContentLength());
            if (answer.isBlank()) return SrmKnowledgeResult.noResults("知识库未返回可展示内容。");
            return new SrmKnowledgeResult(citations.isEmpty() ? "NO_CITATIONS" : "AVAILABLE", answer, citations,
                    citations.isEmpty() ? "knowledge-engine 当前响应没有可展示引用；未伪造文档来源。" : "文档内容仅作制度知识依据，可能过期，需人工核验。");
        } catch (RuntimeException exception) { return SrmKnowledgeResult.unavailable("知识库消息/引用格式异常；未展示不可信内容。"); }
    }
    private List<SrmKnowledgeCitation> citations(JSONArray references) {
        if (references == null) return List.of();
        List<SrmKnowledgeCitation> results = new ArrayList<>();
        for (int i = 0; i < references.size() && results.size() < properties.boundedResults(); i++) {
            JSONObject item = references.getJSONObject(i);
            if (item == null) continue;
            results.add(new SrmKnowledgeCitation(item.getString("documentId"), item.getString("documentTitle"),
                    item.getString("retrievalSource"), item.getString("url"),
                    SrmKnowledgeContentSanitizer.snippet(item.getString("chunkContent"), properties.boundedContentLength())));
        }
        return List.copyOf(results);
    }
    private String extractSseAnswer(String body) {
        if (body == null) return "";
        StringBuilder result = new StringBuilder();
        for (String line : body.split("\\R")) {
            String value = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
            if (!value.isBlank() && !value.startsWith("[PROGRESS]:") && !value.startsWith("[DONE]:")) result.append(value);
        }
        return result.toString();
    }
    private URI uri(String path) { return URI.create(properties.getBaseUrl().replaceAll("/$", "") + path); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
