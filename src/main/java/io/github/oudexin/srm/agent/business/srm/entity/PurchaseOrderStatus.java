package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import java.util.EnumSet;

/** Purchase-order lifecycle; release and cancellation require later human-controlled actions. */
public enum PurchaseOrderStatus {
    DRAFT("DRAFT", "草稿"),
    PENDING_APPROVAL("PENDING_APPROVAL", "待审批"),
    APPROVED("APPROVED", "已审批"),
    RELEASED("RELEASED", "已下达"),
    PARTIALLY_RECEIVED("PARTIALLY_RECEIVED", "部分收货"),
    RECEIVED("RECEIVED", "已收货"),
    CLOSED("CLOSED", "已关闭"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;
    private final String label;

    PurchaseOrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public boolean canTransitionTo(PurchaseOrderStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case DRAFT -> EnumSet.of(PENDING_APPROVAL, CANCELLED).contains(target);
            case PENDING_APPROVAL -> EnumSet.of(APPROVED, DRAFT, CANCELLED).contains(target);
            case APPROVED -> EnumSet.of(RELEASED, CANCELLED).contains(target);
            case RELEASED -> EnumSet.of(PARTIALLY_RECEIVED, RECEIVED, CANCELLED).contains(target);
            case PARTIALLY_RECEIVED -> EnumSet.of(RECEIVED, CLOSED).contains(target);
            case RECEIVED -> EnumSet.of(CLOSED).contains(target);
            case CLOSED, CANCELLED -> false;
        };
    }
}
