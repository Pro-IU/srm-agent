package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderDetailDto(
        String purchaseOrderId,
        String poNo,
        String supplierId,
        String supplierName,
        String supplierStatus,
        String supplierRiskLevel,
        String sourceRfqId,
        String selectedQuoteId,
        String status,
        String purchaserId,
        LocalDate orderDate,
        LocalDate expectedDeliveryDate,
        String currency,
        BigDecimal totalAmount,
        boolean highRiskReviewRequired,
        String remark,
        List<PurchaseOrderLineDto> lines) {
}
