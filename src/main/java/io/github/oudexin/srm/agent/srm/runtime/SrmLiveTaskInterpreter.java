package io.github.oudexin.srm.agent.srm.runtime;

import io.github.oudexin.srm.agent.srm.SrmTaskRequest;
import java.util.Map;

/** Optional live-mode boundary. Its output is validated before it can reach any role or tool. */
public interface SrmLiveTaskInterpreter {
    SrmTaskRequest interpret(String traceId, String message, Map<String, String> clientParameters, String requestedBy);
}
