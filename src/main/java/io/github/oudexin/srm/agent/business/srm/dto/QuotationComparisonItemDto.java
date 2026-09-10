package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Deterministic comparison row. Lower weightedRank is better; it is not an AI/model recommendation. */
public record QuotationComparisonItemDto(
        int rank,
        String quoteId,
        String supplierId,
        String supplierName,
        BigDecimal unitPrice,
        Integer leadTimeDays,
        Integer paymentTermsDays,
        String riskLevel,
        LocalDate validUntil,
        BigDecimal weightedRank,
        List<String> ruleExplanation) {
}
