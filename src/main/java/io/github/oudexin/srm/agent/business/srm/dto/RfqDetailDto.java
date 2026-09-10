package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RfqDetailDto(
        String rfqId,
        String rfqNo,
        String projectName,
        String materialCode,
        String materialName,
        BigDecimal requiredQuantity,
        String unit,
        LocalDate requiredDeliveryDate,
        Instant quotationDeadline,
        BigDecimal budgetAmount,
        String currency,
        String status,
        String ownerId,
        String awardSupplierId,
        String awardQuoteId,
        List<QuotationSummaryDto> quotations) {
}
