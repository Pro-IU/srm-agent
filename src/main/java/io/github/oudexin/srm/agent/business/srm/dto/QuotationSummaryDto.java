package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuotationSummaryDto(
        String quoteId,
        String quoteNo,
        String supplierId,
        String supplierName,
        String supplierStatus,
        String supplierRiskLevel,
        String status,
        BigDecimal unitPrice,
        BigDecimal quotedQuantity,
        BigDecimal totalAmount,
        String currency,
        Integer leadTimeDays,
        Integer paymentTermsDays,
        LocalDate validUntil,
        String riskNote) {
}
