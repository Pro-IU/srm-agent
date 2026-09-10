package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import java.util.EnumSet;

/** RFQ state machine. Awarding is a controlled action to be handled by a later step. */
public enum RfqStatus {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    QUOTING("QUOTING", "报价中"),
    QUOTE_CLOSED("QUOTE_CLOSED", "报价截止"),
    EVALUATING("EVALUATING", "评审中"),
    AWARDED("AWARDED", "已定点"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;
    private final String label;

    RfqStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public boolean canTransitionTo(RfqStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case DRAFT -> EnumSet.of(PUBLISHED, CANCELLED).contains(target);
            case PUBLISHED -> EnumSet.of(QUOTING, CANCELLED).contains(target);
            case QUOTING -> EnumSet.of(QUOTE_CLOSED, CANCELLED).contains(target);
            case QUOTE_CLOSED -> EnumSet.of(EVALUATING, CANCELLED).contains(target);
            case EVALUATING -> EnumSet.of(AWARDED, CANCELLED).contains(target);
            case AWARDED, CANCELLED -> false;
        };
    }
}
