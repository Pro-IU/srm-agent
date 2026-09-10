package io.github.oudexin.srm.agent.srm.knowledge;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
class JdkKnowledgeEngineHttpTransport implements KnowledgeEngineHttpTransport {
    private final HttpClient client;
    JdkKnowledgeEngineHttpTransport(SrmKnowledgeProperties properties) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMillis()))).build();
    }
    @Override public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
