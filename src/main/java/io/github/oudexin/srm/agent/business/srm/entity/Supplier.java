package io.github.oudexin.srm.agent.business.srm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.oudexin.srm.agent.config.InstantTypeHandler;
import java.math.BigDecimal;
import java.time.Instant;

/** Manufacturing supplier master. All values in the Step-2 demo are synthetic. */
@TableName(value = "srm_supplier", autoResultMap = true)
public class Supplier {
    @TableId(value = "supplier_id", type = IdType.INPUT)
    private String supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplyCategory;
    private SupplierStatus status;
    private SupplierRiskLevel riskLevel;
    private BigDecimal qualityScore;
    private BigDecimal deliveryScore;
    /** Agreed account period in days, used as a transparent quotation-comparison factor. */
    private Integer paymentTermsDays;
    private String countryRegion;
    private String contactName;
    private String contactEmail;
    private String riskNote;
    @TableField(fill = FieldFill.INSERT, typeHandler = InstantTypeHandler.class)
    private Instant createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE, typeHandler = InstantTypeHandler.class)
    private Instant updatedAt;

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplyCategory() { return supplyCategory; }
    public void setSupplyCategory(String supplyCategory) { this.supplyCategory = supplyCategory; }
    public SupplierStatus getStatus() { return status; }
    public void setStatus(SupplierStatus status) { this.status = status; }
    public SupplierRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(SupplierRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    public BigDecimal getDeliveryScore() { return deliveryScore; }
    public void setDeliveryScore(BigDecimal deliveryScore) { this.deliveryScore = deliveryScore; }
    public Integer getPaymentTermsDays() { return paymentTermsDays; }
    public void setPaymentTermsDays(Integer paymentTermsDays) { this.paymentTermsDays = paymentTermsDays; }
    public String getCountryRegion() { return countryRegion; }
    public void setCountryRegion(String countryRegion) { this.countryRegion = countryRegion; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getRiskNote() { return riskNote; }
    public void setRiskNote(String riskNote) { this.riskNote = riskNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
