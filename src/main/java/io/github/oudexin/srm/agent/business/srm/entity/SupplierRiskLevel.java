package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** Current supply risk assessment, intentionally separate from qualification status. */
public enum SupplierRiskLevel {
    LOW("LOW", "低风险"),
    MEDIUM("MEDIUM", "中风险"),
    HIGH("HIGH", "高风险"),
    CRITICAL("CRITICAL", "重大风险");

    @EnumValue
    private final String code;
    private final String label;

    SupplierRiskLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
