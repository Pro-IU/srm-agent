package io.github.oudexin.srm.agent.srm.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Process-local capped trace store for local demos. */
@Component
public class SrmRuntimeTraceStore {
    private static final int MAX_TRACES = 100;
    private final ConcurrentHashMap<String, SrmRuntimeTrace> traces = new ConcurrentHashMap<>();
    public void save(SrmRuntimeTrace trace) {
        traces.put(trace.traceId(), trace);
        if (traces.size() > MAX_TRACES) traces.values().stream().min(Comparator.comparing(SrmRuntimeTrace::startedAt))
                .ifPresent(oldest -> traces.remove(oldest.traceId()));
    }
    public Optional<SrmRuntimeTrace> find(String traceId) { return Optional.ofNullable(traces.get(traceId)); }
    public List<SrmRuntimeTrace> recent() {
        return traces.values().stream().sorted(Comparator.comparing(SrmRuntimeTrace::startedAt).reversed())
                .limit(20).toList();
    }
    void clear() { traces.clear(); }
}
