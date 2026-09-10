package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceLibraryRecordDto(
        String priceRecordId,
        String supplierId,
        String supplierName,
        String materialCode,
        String materialName,
        BigDecimal unitPrice,
        String currency,
        BigDecimal minimumOrderQuantity,
        LocalDate effectiveStartDate,
        LocalDate effectiveEndDate,
        String status,
        boolean usableToday,
        String sourceQuoteId,
        String remark) {
}
