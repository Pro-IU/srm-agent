package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/** Supplier quotation state inside one RFQ and quotation version. */
public enum QuotationStatus {
    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    WITHDRAWN("WITHDRAWN", "已撤回"),
    EXPIRED("EXPIRED", "已过期"),
    REJECTED("REJECTED", "未中选"),
    ACCEPTED("ACCEPTED", "已中选");

    @EnumValue
    private final String code;
    private final String label;

    QuotationStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
