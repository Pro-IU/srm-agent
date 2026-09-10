package io.github.oudexin.srm.agent.srm;

/** Router intent. AUTO is classified deterministically from the structured request. */
public enum SrmTaskIntent {
    AUTO,
    SUPPLIER,
    SOURCING,
    QUOTE_ANALYSIS,
    AWARD_REVIEW,
    PRICE_LIBRARY,
    PURCHASE_ORDER,
    CROSS_DOMAIN,
    CONFIRMATION,
    UNKNOWN
}
