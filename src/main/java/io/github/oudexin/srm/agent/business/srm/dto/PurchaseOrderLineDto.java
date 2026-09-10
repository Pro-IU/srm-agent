package io.github.oudexin.srm.agent.business.srm.dto;

import java.math.BigDecimal;

public record PurchaseOrderLineDto(
        int lineNo,
        String materialCode,
        String materialName,
        String unit,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal openQuantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        String status,
        String remark) {
}
