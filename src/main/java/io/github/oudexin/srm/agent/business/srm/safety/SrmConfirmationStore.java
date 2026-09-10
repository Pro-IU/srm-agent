package io.github.oudexin.srm.agent.business.srm.safety;

import io.github.oudexin.srm.agent.business.srm.dto.SrmActionType;
import io.github.oudexin.srm.agent.business.srm.dto.SrmConfirmationPreview;
import io.github.oudexin.srm.agent.business.srm.service.SrmValidationException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/** In-memory pending confirmations. A restart clears them by design; never use this as production authorization. */
@Component
public class SrmConfirmationStore {
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private final ConcurrentHashMap<String, PendingConfirmation> pending = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Duration ttl;

    @Autowired
    public SrmConfirmationStore() {
        this(new SecureRandom(), Clock.systemUTC(), DEFAULT_TTL);
    }

    public SrmConfirmationStore(SecureRandom secureRandom, Clock clock, Duration ttl) {
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.ttl = ttl;
    }

    public SrmConfirmationPreview issue(SrmActionType actionType, String targetId, String requestedBy,
            List<String> changeSummary, List<String> riskWarnings, Map<String, String> attributes) {
        Instant now = Instant.now(clock);
        String token = nextToken();
        PendingConfirmation confirmation = new PendingConfirmation(actionType, targetId, requestedBy, now,
                now.plus(ttl), List.copyOf(changeSummary), List.copyOf(riskWarnings), Map.copyOf(attributes));
        pending.put(token, confirmation);
        return new SrmConfirmationPreview(actionType, targetId, token, requestedBy, confirmation.expiresAt,
                confirmation.changeSummary, confirmation.riskWarnings,
                "展示预览后，必须由人工显式调用 confirm_srm_high_risk_action 并回传完整 token；预览本身不会执行写入。");
    }

    public PendingConfirmation consume(SrmActionType actionType, String targetId, String token) {
        if (token == null || token.isBlank()) {
            throw new SrmValidationException("confirmationToken不能为空");
        }
        PendingConfirmation confirmation = pending.get(token);
        if (confirmation == null) {
            throw new SrmValidationException("确认请求不存在、已过期或已被使用");
        }
        if (confirmation.actionType != actionType || !confirmation.targetId.equals(targetId)) {
            throw new SrmValidationException("确认请求的操作类型或目标不匹配");
        }
        if (Instant.now(clock).isAfter(confirmation.expiresAt)) {
            pending.remove(token, confirmation);
            confirmation.used.set(true);
            throw new SrmValidationException("确认请求已过期");
        }
        if (!confirmation.used.compareAndSet(false, true)) {
            throw new SrmValidationException("确认请求已被使用");
        }
        pending.remove(token, confirmation);
        return confirmation;
    }

    private String nextToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return pending.containsKey(token) ? nextToken() : token;
    }

    public static final class PendingConfirmation {
        private final SrmActionType actionType;
        private final String targetId;
        private final String requestedBy;
        private final Instant createdAt;
        private final Instant expiresAt;
        private final List<String> changeSummary;
        private final List<String> riskWarnings;
        private final Map<String, String> attributes;
        private final AtomicBoolean used = new AtomicBoolean(false);

        private PendingConfirmation(SrmActionType actionType, String targetId, String requestedBy, Instant createdAt,
                Instant expiresAt, List<String> changeSummary, List<String> riskWarnings, Map<String, String> attributes) {
            this.actionType = actionType;
            this.targetId = targetId;
            this.requestedBy = requestedBy;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.changeSummary = changeSummary;
            this.riskWarnings = riskWarnings;
            this.attributes = attributes;
        }

        public SrmActionType actionType() { return actionType; }
        public String targetId() { return targetId; }
        public String requestedBy() { return requestedBy; }
        public Instant createdAt() { return createdAt; }
        public Instant expiresAt() { return expiresAt; }
        public List<String> changeSummary() { return changeSummary; }
        public Map<String, String> attributes() { return attributes; }
    }
}
