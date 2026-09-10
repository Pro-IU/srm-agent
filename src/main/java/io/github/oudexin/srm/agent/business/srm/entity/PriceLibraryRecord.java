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

/** Approved or historical material price reference, optionally traceable to a supplier quotation. */
@TableName(value = "srm_price_library_record", autoResultMap = true)
public class PriceLibraryRecord {
    @TableId(value = "price_record_id", type = IdType.INPUT)
    private String priceRecordId;
    private String supplierId;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal unitPrice;
    private String currency;
    private BigDecimal minimumOrderQuantity;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private PriceRecordStatus status;
    private String sourceQuoteId;
    private String remark;
    @TableField(fill = FieldFill.INSERT, typeHandler = InstantTypeHandler.class)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = InstantTypeHandler.class)
    private Instant updatedAt;

    public String getPriceRecordId() { return priceRecordId; }
    public void setPriceRecordId(String priceRecordId) { this.priceRecordId = priceRecordId; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getMinimumOrderQuantity() { return minimumOrderQuantity; }
    public void setMinimumOrderQuantity(BigDecimal minimumOrderQuantity) { this.minimumOrderQuantity = minimumOrderQuantity; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public void setEffectiveStartDate(LocalDate effectiveStartDate) { this.effectiveStartDate = effectiveStartDate; }
    public LocalDate getEffectiveEndDate() { return effectiveEndDate; }
    public void setEffectiveEndDate(LocalDate effectiveEndDate) { this.effectiveEndDate = effectiveEndDate; }
    public PriceRecordStatus getStatus() { return status; }
    public void setStatus(PriceRecordStatus status) { this.status = status; }
    public String getSourceQuoteId() { return sourceQuoteId; }
    public void setSourceQuoteId(String sourceQuoteId) { this.sourceQuoteId = sourceQuoteId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
