package io.github.oudexin.srm.agent.business.srm.service;

import io.github.oudexin.srm.agent.business.srm.dto.SrmActionAuditRecord;
import io.github.oudexin.srm.agent.business.srm.dto.SrmActionResult;
import io.github.oudexin.srm.agent.business.srm.dto.SrmActionType;
import io.github.oudexin.srm.agent.business.srm.dto.SrmConfirmationPreview;
import io.github.oudexin.srm.agent.business.srm.entity.PriceLibraryRecord;
import io.github.oudexin.srm.agent.business.srm.entity.PriceRecordStatus;
import io.github.oudexin.srm.agent.business.srm.entity.PurchaseOrder;
import io.github.oudexin.srm.agent.business.srm.entity.PurchaseOrderStatus;
import io.github.oudexin.srm.agent.business.srm.entity.QuotationStatus;
import io.github.oudexin.srm.agent.business.srm.entity.RfqProject;
import io.github.oudexin.srm.agent.business.srm.entity.RfqStatus;
import io.github.oudexin.srm.agent.business.srm.entity.Supplier;
import io.github.oudexin.srm.agent.business.srm.entity.SupplierQuotation;
import io.github.oudexin.srm.agent.business.srm.entity.SupplierStatus;
import io.github.oudexin.srm.agent.business.srm.mapper.PriceLibraryRecordMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.PurchaseOrderMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.RfqProjectMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.SupplierMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.SupplierQuotationMapper;
import io.github.oudexin.srm.agent.business.srm.safety.SrmConfirmationStore;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controlled SRM writes. There is deliberately no public execute-without-confirmation method.
 * Spring transaction rollback protects multi-row award changes when a guarded write fails.
 */
@Service
public class SrmControlledActionService {
    private final RfqProjectMapper rfqProjectMapper;
    private final SupplierQuotationMapper quotationMapper;
    private final SupplierMapper supplierMapper;
    private final PriceLibraryRecordMapper priceLibraryRecordMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SrmConfirmationStore confirmationStore;
    private final Clock clock;
    private final CopyOnWriteArrayList<SrmActionAuditRecord> auditLog = new CopyOnWriteArrayList<>();

    @Autowired
    public SrmControlledActionService(RfqProjectMapper rfqProjectMapper, SupplierQuotationMapper quotationMapper,
            SupplierMapper supplierMapper, PriceLibraryRecordMapper priceLibraryRecordMapper,
            PurchaseOrderMapper purchaseOrderMapper, SrmConfirmationStore confirmationStore) {
        this(rfqProjectMapper, quotationMapper, supplierMapper, priceLibraryRecordMapper, purchaseOrderMapper,
                confirmationStore, Clock.systemUTC());
    }

    SrmControlledActionService(RfqProjectMapper rfqProjectMapper, SupplierQuotationMapper quotationMapper,
            SupplierMapper supplierMapper, PriceLibraryRecordMapper priceLibraryRecordMapper,
            PurchaseOrderMapper purchaseOrderMapper, SrmConfirmationStore confirmationStore, Clock clock) {
        this.rfqProjectMapper = rfqProjectMapper;
        this.quotationMapper = quotationMapper;
        this.supplierMapper = supplierMapper;
        this.priceLibraryRecordMapper = priceLibraryRecordMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.confirmationStore = confirmationStore;
        this.clock = clock;
    }

    /** Generate a non-mutating award preview for an RFQ in EVALUATING state. */
    public SrmConfirmationPreview previewAward(String rfqId, String quoteId, String requestedBy) {
        String targetId = SrmInputValidator.requiredId(rfqId, "rfqId");
        String validQuoteId = SrmInputValidator.requiredId(quoteId, "quoteId");
        String operator = requiredOperator(requestedBy, "requestedBy");
        AwardContext context = validateAward(targetId, validQuoteId);
        return confirmationStore.issue(SrmActionType.AWARD_RFQ, targetId, operator,
                List.of("RFQ " + context.rfq.getRfqNo() + " 状态：EVALUATING → AWARDED",
                        "中选供应商：" + context.supplier.getSupplierName(),
                        "中选报价：" + context.quote.getQuoteNo() + "，未税单价=" + context.quote.getUnitPrice()),
                List.of("授标会影响后续价格库与采购订单来源，请由授权人员复核报价、风险与业务决策。"),
                Map.of("quoteId", validQuoteId));
    }

    /** Generate a non-mutating preview to re-enable a currently suspended price record. */
    public SrmConfirmationPreview previewEnablePriceRecord(String priceRecordId, String requestedBy) {
        String targetId = SrmInputValidator.requiredId(priceRecordId, "priceRecordId");
        String operator = requiredOperator(requestedBy, "requestedBy");
        PriceLibraryRecord record = validatePriceEnable(targetId);
        return confirmationStore.issue(SrmActionType.ENABLE_PRICE_RECORD, targetId, operator,
                List.of("价格记录 " + record.getPriceRecordId() + " 状态：SUSPENDED → VALID",
                        "物料=" + record.getMaterialCode() + "，单价=" + record.getUnitPrice() + " " + record.getCurrency()),
                List.of("启用价格将允许后续受控流程引用；需复核供应商资格和有效期。"), Map.of());
    }

    /** Generate a non-mutating preview to suspend an otherwise usable price record. */
    public SrmConfirmationPreview previewSuspendPriceRecord(String priceRecordId, String requestedBy) {
        String targetId = SrmInputValidator.requiredId(priceRecordId, "priceRecordId");
        String operator = requiredOperator(requestedBy, "requestedBy");
        PriceLibraryRecord record = validatePriceSuspend(targetId);
        return confirmationStore.issue(SrmActionType.SUSPEND_PRICE_RECORD, targetId, operator,
                List.of("价格记录 " + record.getPriceRecordId() + " 状态：" + record.getStatus().getCode() + " → SUSPENDED",
                        "物料=" + record.getMaterialCode() + "，单价=" + record.getUnitPrice() + " " + record.getCurrency()),
                List.of("停用后该价格记录不应再作为采购下单依据。"), Map.of());
    }

    /** Generate a non-mutating preview for a PO approval. Approval does not release the order. */
    public SrmConfirmationPreview previewPurchaseOrderApproval(String purchaseOrderId, String requestedBy) {
        String targetId = SrmInputValidator.requiredId(purchaseOrderId, "purchaseOrderId");
        String operator = requiredOperator(requestedBy, "requestedBy");
        PurchaseOrder order = validatePurchaseOrderApproval(targetId);
        List<String> warnings = order.isHighRiskReviewRequired()
                ? List.of("该订单标识为高风险复核；本次人工确认即为审批责任留痕，审批后仍不会自动下达。")
                : List.of("审批后订单仅变更为 APPROVED，不会自动释放或发送给供应商。");
        return confirmationStore.issue(SrmActionType.APPROVE_PURCHASE_ORDER, targetId, operator,
                List.of("采购订单 " + order.getPoNo() + " 状态：PENDING_APPROVAL → APPROVED",
                        "金额=" + order.getTotalAmount() + " " + order.getCurrency(), "供应商ID=" + order.getSupplierId()),
                warnings, Map.of());
    }

    /**
     * The only public mutation gateway. The token binds the caller to the previewed action and target.
     */
    @Transactional
    public SrmActionResult confirm(SrmActionType actionType, String targetId, String confirmationToken, String confirmedBy) {
        if (actionType == null) {
            throw new SrmValidationException("actionType不能为空");
        }
        String validTargetId = SrmInputValidator.requiredId(targetId, "targetId");
        String operator = requiredOperator(confirmedBy, "confirmedBy");
        SrmConfirmationStore.PendingConfirmation pending = confirmationStore.consume(actionType, validTargetId, confirmationToken);
        try {
            executeConfirmed(actionType, validTargetId, pending.attributes());
            SrmActionAuditRecord audit = audit(pending, operator, true, "EXECUTED", pending.changeSummary());
            return new SrmActionResult(true, actionType, validTargetId, audit.auditId(), audit.occurredAt(), "人工确认后执行成功");
        } catch (RuntimeException exception) {
            audit(pending, operator, false, "REJECTED: " + exception.getMessage(), pending.changeSummary());
            throw exception;
        }
    }

    /** Current-process audit query for the Step-3 demonstration boundary. */
    public List<SrmActionAuditRecord> listAuditRecords() {
        return List.copyOf(auditLog);
    }

    private void executeConfirmed(SrmActionType actionType, String targetId, Map<String, String> attributes) {
        switch (actionType) {
            case AWARD_RFQ -> executeAward(targetId, attributes.get("quoteId"));
            case ENABLE_PRICE_RECORD -> executePriceStatus(targetId, PriceRecordStatus.VALID);
            case SUSPEND_PRICE_RECORD -> executePriceStatus(targetId, PriceRecordStatus.SUSPENDED);
            case APPROVE_PURCHASE_ORDER -> executePurchaseOrderApproval(targetId);
        }
    }

    private void executeAward(String rfqId, String quoteId) {
        AwardContext context = validateAward(rfqId, quoteId);
        context.quote.setStatus(QuotationStatus.ACCEPTED);
        if (quotationMapper.updateById(context.quote) != 1) {
            throw new SrmValidationException("报价状态更新失败，已回滚");
        }
        context.rfq.setStatus(RfqStatus.AWARDED);
        context.rfq.setAwardSupplierId(context.supplier.getSupplierId());
        context.rfq.setAwardQuoteId(context.quote.getQuoteId());
        if (rfqProjectMapper.updateById(context.rfq) != 1) {
            throw new SrmValidationException("RFQ授标更新失败，已回滚");
        }
    }

    private void executePriceStatus(String priceRecordId, PriceRecordStatus targetStatus) {
        PriceLibraryRecord record = targetStatus == PriceRecordStatus.VALID
                ? validatePriceEnable(priceRecordId) : validatePriceSuspend(priceRecordId);
        record.setStatus(targetStatus);
        if (priceLibraryRecordMapper.updateById(record) != 1) {
            throw new SrmValidationException("价格记录状态更新失败，已回滚");
        }
    }

    private void executePurchaseOrderApproval(String purchaseOrderId) {
        PurchaseOrder order = validatePurchaseOrderApproval(purchaseOrderId);
        order.setStatus(PurchaseOrderStatus.APPROVED);
        if (purchaseOrderMapper.updateById(order) != 1) {
            throw new SrmValidationException("采购订单审批更新失败，已回滚");
        }
    }

    private AwardContext validateAward(String rfqId, String quoteId) {
        RfqProject rfq = required(rfqProjectMapper.selectById(rfqId), "RFQ不存在");
        if (rfq.getStatus() != RfqStatus.EVALUATING || !rfq.getStatus().canTransitionTo(RfqStatus.AWARDED)
                || rfq.getAwardQuoteId() != null || rfq.getAwardSupplierId() != null) {
            throw new SrmValidationException("仅允许未授标且处于EVALUATING状态的RFQ授标");
        }
        SupplierQuotation quote = required(quotationMapper.selectById(quoteId), "报价不存在");
        if (!rfqId.equals(quote.getRfqId()) || quote.getStatus() != QuotationStatus.SUBMITTED) {
            throw new SrmValidationException("报价不属于该RFQ，或当前不是可授标的SUBMITTED状态");
        }
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now(clock))) {
            throw new SrmValidationException("报价已过有效期，不能授标");
        }
        Supplier supplier = required(supplierMapper.selectById(quote.getSupplierId()), "报价供应商不存在");
        if (supplier.getStatus() != SupplierStatus.QUALIFIED) {
            throw new SrmValidationException("仅合格供应商可以授标");
        }
        return new AwardContext(rfq, quote, supplier);
    }

    private PriceLibraryRecord validatePriceEnable(String priceRecordId) {
        PriceLibraryRecord record = required(priceLibraryRecordMapper.selectById(priceRecordId), "价格记录不存在");
        if (record.getStatus() != PriceRecordStatus.SUSPENDED) {
            throw new SrmValidationException("仅SUSPENDED价格记录可以启用");
        }
        LocalDate today = LocalDate.now(clock);
        if (record.getEffectiveStartDate().isAfter(today) || record.getEffectiveEndDate().isBefore(today)) {
            throw new SrmValidationException("价格记录不在有效期内，不能启用");
        }
        Supplier supplier = required(supplierMapper.selectById(record.getSupplierId()), "价格记录供应商不存在");
        if (supplier.getStatus() != SupplierStatus.QUALIFIED) {
            throw new SrmValidationException("仅合格供应商的价格记录可以启用");
        }
        return record;
    }

    private PriceLibraryRecord validatePriceSuspend(String priceRecordId) {
        PriceLibraryRecord record = required(priceLibraryRecordMapper.selectById(priceRecordId), "价格记录不存在");
        if (record.getStatus() != PriceRecordStatus.VALID && record.getStatus() != PriceRecordStatus.EXPIRING) {
            throw new SrmValidationException("仅VALID或EXPIRING价格记录可以停用");
        }
        return record;
    }

    private PurchaseOrder validatePurchaseOrderApproval(String purchaseOrderId) {
        PurchaseOrder order = required(purchaseOrderMapper.selectById(purchaseOrderId), "采购订单不存在");
        if (order.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL
                || !order.getStatus().canTransitionTo(PurchaseOrderStatus.APPROVED)) {
            throw new SrmValidationException("仅PENDING_APPROVAL采购订单可以审批");
        }
        Supplier supplier = required(supplierMapper.selectById(order.getSupplierId()), "采购订单供应商不存在");
        if (supplier.getStatus() != SupplierStatus.QUALIFIED) {
            throw new SrmValidationException("采购订单供应商不是合格状态，禁止审批");
        }
        if (order.getSourceRfqId() != null) {
            RfqProject rfq = required(rfqProjectMapper.selectById(order.getSourceRfqId()), "订单来源RFQ不存在");
            if (rfq.getStatus() != RfqStatus.AWARDED || !order.getSupplierId().equals(rfq.getAwardSupplierId())
                    || order.getSelectedQuoteId() == null || !order.getSelectedQuoteId().equals(rfq.getAwardQuoteId())) {
                throw new SrmValidationException("来源RFQ尚未有效授标，或订单供应商/报价与授标结果不一致");
            }
        }
        return order;
    }

    private SrmActionAuditRecord audit(SrmConfirmationStore.PendingConfirmation pending, String confirmedBy,
            boolean executed, String outcome, List<String> summary) {
        SrmActionAuditRecord record = new SrmActionAuditRecord(UUID.randomUUID().toString(), Instant.now(clock),
                pending.actionType(), pending.targetId(), pending.requestedBy(), confirmedBy, executed, outcome, summary);
        auditLog.add(record);
        return record;
    }

    private String requiredOperator(String value, String fieldName) {
        return SrmInputValidator.requiredId(value, fieldName);
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new SrmValidationException(message);
        }
        return value;
    }

    private record AwardContext(RfqProject rfq, SupplierQuotation quote, Supplier supplier) {
    }
}
