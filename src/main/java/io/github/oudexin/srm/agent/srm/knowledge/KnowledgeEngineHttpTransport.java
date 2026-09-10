package io.github.oudexin.srm.agent.srm.knowledge;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

interface KnowledgeEngineHttpTransport {
    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
