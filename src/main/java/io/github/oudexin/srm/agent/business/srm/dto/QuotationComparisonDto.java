package io.github.oudexin.srm.agent.business.srm.dto;

import java.util.List;

/** Transparent comparison rules are returned together with the deterministic result. */
public record QuotationComparisonDto(
        String rfqId,
        int eligibleQuoteCount,
        int excludedQuoteCount,
        List<String> rules,
        List<QuotationComparisonItemDto> rankedQuotes) {
}
