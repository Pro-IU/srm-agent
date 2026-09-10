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

/** Sourcing project / request for quotation. Award references are set only after evaluation. */
@TableName(value = "srm_rfq_project", autoResultMap = true)
public class RfqProject {
    @TableId(value = "rfq_id", type = IdType.INPUT)
    private String rfqId;
    private String rfqNo;
    private String projectName;
    private String materialCode;
    private String materialName;
    private BigDecimal requiredQuantity;
    private String unit;
    private LocalDate requiredDeliveryDate;
    private Instant quotationDeadline;
    private BigDecimal budgetAmount;
    private String currency;
    private RfqStatus status;
    private String ownerId;
    private String awardSupplierId;
    private String awardQuoteId;
    @TableField(fill = FieldFill.INSERT, typeHandler = InstantTypeHandler.class)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = InstantTypeHandler.class)
    private Instant updatedAt;

    public String getRfqId() { return rfqId; }
    public void setRfqId(String rfqId) { this.rfqId = rfqId; }
    public String getRfqNo() { return rfqNo; }
    public void setRfqNo(String rfqNo) { this.rfqNo = rfqNo; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public BigDecimal getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getRequiredDeliveryDate() { return requiredDeliveryDate; }
    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) { this.requiredDeliveryDate = requiredDeliveryDate; }
    public Instant getQuotationDeadline() { return quotationDeadline; }
    public void setQuotationDeadline(Instant quotationDeadline) { this.quotationDeadline = quotationDeadline; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public RfqStatus getStatus() { return status; }
    public void setStatus(RfqStatus status) { this.status = status; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getAwardSupplierId() { return awardSupplierId; }
    public void setAwardSupplierId(String awardSupplierId) { this.awardSupplierId = awardSupplierId; }
    public String getAwardQuoteId() { return awardQuoteId; }
    public void setAwardQuoteId(String awardQuoteId) { this.awardQuoteId = awardQuoteId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
