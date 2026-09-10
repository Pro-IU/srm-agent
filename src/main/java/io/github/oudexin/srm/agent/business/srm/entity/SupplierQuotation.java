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

/** One versioned supplier response to an RFQ. */
@TableName(value = "srm_supplier_quote", autoResultMap = true)
public class SupplierQuotation {
    @TableId(value = "quote_id", type = IdType.INPUT)
    private String quoteId;
    private String quoteNo;
    private String rfqId;
    private String supplierId;
    private Integer quoteVersion;
    private QuotationStatus status;
    private BigDecimal unitPrice;
    private BigDecimal quotedQuantity;
    private BigDecimal taxRate;
    private BigDecimal totalAmount;
    private String currency;
    private Integer leadTimeDays;
    private LocalDate validUntil;
    private Instant submittedAt;
    private String riskNote;
    @TableField(fill = FieldFill.INSERT, typeHandler = InstantTypeHandler.class)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = InstantTypeHandler.class)
    private Instant updatedAt;

    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }
    public String getQuoteNo() { return quoteNo; }
    public void setQuoteNo(String quoteNo) { this.quoteNo = quoteNo; }
    public String getRfqId() { return rfqId; }
    public void setRfqId(String rfqId) { this.rfqId = rfqId; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public Integer getQuoteVersion() { return quoteVersion; }
    public void setQuoteVersion(Integer quoteVersion) { this.quoteVersion = quoteVersion; }
    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getQuotedQuantity() { return quotedQuantity; }
    public void setQuotedQuantity(BigDecimal quotedQuantity) { this.quotedQuantity = quotedQuantity; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getRiskNote() { return riskNote; }
    public void setRiskNote(String riskNote) { this.riskNote = riskNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
