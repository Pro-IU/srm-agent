package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** Fulfilment state of an individual purchase-order line. */
public enum PurchaseOrderLineStatus {
    OPEN("OPEN", "待收货"),
    PARTIALLY_RECEIVED("PARTIALLY_RECEIVED", "部分收货"),
    RECEIVED("RECEIVED", "已收货"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;
    private final String label;

    PurchaseOrderLineStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
