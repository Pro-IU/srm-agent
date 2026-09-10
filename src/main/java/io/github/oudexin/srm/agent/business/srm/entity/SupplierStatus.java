package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** Supplier lifecycle used by the synthetic SRM baseline. */
public enum SupplierStatus {
    POTENTIAL("POTENTIAL", "潜在供应商"),
    QUALIFIED("QUALIFIED", "合格"),
    SUSPENDED("SUSPENDED", "暂停合作"),
    DISQUALIFIED("DISQUALIFIED", "取消资格");

    @EnumValue
    private final String code;
    private final String label;

    SupplierStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
