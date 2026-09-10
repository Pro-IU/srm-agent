package io.github.oudexin.srm.agent.srm;

/** Named SRM roles for structured delegation and observability. */
public enum SrmAgentName {
    PROCUREMENT_MASTER("ProcurementMasterAgent"),
    SUPPLIER("SupplierAgent"),
    SOURCING("SourcingAgent"),
    QUOTE_ANALYSIS("QuoteAnalysisAgent"),
    AWARD_REVIEW("AwardReviewAgent"),
    PURCHASE_ORDER("PurchaseOrderAgent");

    private final String displayName;

    SrmAgentName(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
