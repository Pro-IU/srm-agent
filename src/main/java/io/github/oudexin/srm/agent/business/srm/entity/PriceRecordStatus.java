package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** Usability of a price-library record; validity dates remain authoritative. */
public enum PriceRecordStatus {
    VALID("VALID", "有效"),
    EXPIRING("EXPIRING", "即将到期"),
    EXPIRED("EXPIRED", "已过期"),
    SUSPENDED("SUSPENDED", "暂停使用");

    @EnumValue
    private final String code;
    private final String label;

    PriceRecordStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
