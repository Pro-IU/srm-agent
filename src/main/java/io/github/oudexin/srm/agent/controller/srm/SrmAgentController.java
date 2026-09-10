package io.github.oudexin.srm.agent.controller.srm;

import io.github.oudexin.srm.agent.srm.runtime.SrmAgentChatCommand;
import io.github.oudexin.srm.agent.srm.runtime.SrmAgentChatResponse;
import io.github.oudexin.srm.agent.srm.runtime.SrmAgentRuntimeService;
import io.github.oudexin.srm.agent.srm.runtime.SrmConfirmationCommand;
import io.github.oudexin.srm.agent.srm.runtime.SrmRuntimeTrace;
import io.github.oudexin.srm.agent.tools.SrmStructuredDataTools;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Unified, non-streaming API for the SRM local demonstration runtime. */
@RestController
@RequestMapping("/api/srm/agent")
public class SrmAgentController {
    private final SrmAgentRuntimeService runtime;
    private final SrmStructuredDataTools tools;
    public SrmAgentController(SrmAgentRuntimeService runtime, SrmStructuredDataTools tools) {
        this.runtime = runtime;
        this.tools = tools;
    }
    @PostMapping("/chat")
    public SrmAgentChatResponse chat(@RequestBody(required = false) SrmAgentChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (request == null) return runtime.chat(new SrmAgentChatCommand(null, null, null, null, null));
        return runtime.chat(new SrmAgentChatCommand(request.message(), request.conversationId(), request.intent(),
                request.parameters(), request.requestedBy(), authorization));
    }
    /** Direct-controller compatibility for contract tests; HTTP calls use the header-aware endpoint above. */
    public SrmAgentChatResponse chat(SrmAgentChatRequest request) { return chat(request, null); }
    @PostMapping("/confirm")
    public String confirm(@RequestBody(required = false) SrmAgentConfirmRequest request) {
        if (request == null) return runtime.confirm(new SrmConfirmationCommand(null, null, null, null));
        return runtime.confirm(new SrmConfirmationCommand(request.actionType(), request.targetId(),
                request.confirmationToken(), request.confirmedBy()));
    }
    @GetMapping("/traces/{traceId}")
    public SrmRuntimeTrace trace(@PathVariable String traceId) { return runtime.trace(traceId); }
    @GetMapping("/traces")
    public List<SrmRuntimeTrace> traces() { return runtime.recentTraces(); }
    @GetMapping("/audit")
    public String audit() { return tools.queryActionAudit(); }
}
