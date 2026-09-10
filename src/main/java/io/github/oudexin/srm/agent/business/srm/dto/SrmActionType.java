package io.github.oudexin.srm.agent.business.srm.dto;

/** High-risk operations that can only run through an issued, one-time confirmation. */
public enum SrmActionType {
    AWARD_RFQ,
    ENABLE_PRICE_RECORD,
    SUSPEND_PRICE_RECORD,
    APPROVE_PURCHASE_ORDER
}
