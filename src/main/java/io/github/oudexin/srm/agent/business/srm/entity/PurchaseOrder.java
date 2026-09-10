package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.oudexin.srm.agent.config.InstantTypeHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Purchase order header. Lines are persisted separately in {@link PurchaseOrderLine}. */
@TableName(value = "srm_purchase_order", autoResultMap = true)
public class PurchaseOrder {
    @TableId(value = "purchase_order_id", type = IdType.INPUT)
    private String purchaseOrderId;
    private String poNo;
    private String supplierId;
    private String sourceRfqId;
    private String selectedQuoteId;
    private PurchaseOrderStatus status;
    private String purchaserId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String currency;
    private BigDecimal totalAmount;
    private boolean highRiskReviewRequired;
    private String remark;
    @TableField(fill = FieldFill.INSERT, typeHandler = InstantTypeHandler.class)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = InstantTypeHandler.class)
    private Instant updatedAt;

    public String getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(String purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public String getPoNo() { return poNo; }
    public void setPoNo(String poNo) { this.poNo = poNo; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSourceRfqId() { return sourceRfqId; }
    public void setSourceRfqId(String sourceRfqId) { this.sourceRfqId = sourceRfqId; }
    public String getSelectedQuoteId() { return selectedQuoteId; }
    public void setSelectedQuoteId(String selectedQuoteId) { this.selectedQuoteId = selectedQuoteId; }
    public PurchaseOrderStatus getStatus() { return status; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public String getPurchaserId() { return purchaserId; }
    public void setPurchaserId(String purchaserId) { this.purchaserId = purchaserId; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public boolean isHighRiskReviewRequired() { return highRiskReviewRequired; }
    public void setHighRiskReviewRequired(boolean highRiskReviewRequired) { this.highRiskReviewRequired = highRiskReviewRequired; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
