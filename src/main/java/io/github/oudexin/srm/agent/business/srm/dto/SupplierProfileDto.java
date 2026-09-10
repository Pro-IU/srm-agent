package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;

/** Safe supplier projection; it intentionally omits persistence/audit implementation details. */
public record SupplierProfileDto(
        String supplierId,
        String supplierCode,
        String supplierName,
        String supplyCategory,
        String status,
        String riskLevel,
        BigDecimal qualityScore,
        BigDecimal deliveryScore,
        Integer paymentTermsDays,
        String countryRegion,
        String riskNote) {
}
