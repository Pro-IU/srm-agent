package io.github.oudexin.srm.agent.business.srm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import io.github.oudexin.srm.agent.business.srm.entity.SupplierRiskLevel;
import io.github.oudexin.srm.agent.business.srm.entity.SupplierStatus;
import io.github.oudexin.srm.agent.business.srm.mapper.PriceLibraryRecordMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.PurchaseOrderMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.RfqProjectMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.SupplierMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.SupplierQuotationMapper;
import io.github.oudexin.srm.agent.business.srm.safety.SrmConfirmationStore;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SrmControlledActionServiceTest {
    @Mock private RfqProjectMapper rfqProjectMapper;
    @Mock private SupplierQuotationMapper quotationMapper;
    @Mock private SupplierMapper supplierMapper;
    @Mock private PriceLibraryRecordMapper priceLibraryRecordMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;

    private MutableClock clock;
    private SrmControlledActionService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-31T00:00:00Z"));
        SrmConfirmationStore store = new SrmConfirmationStore(new SecureRandom(), clock, Duration.ofMinutes(5));
        service = new SrmControlledActionService(rfqProjectMapper, quotationMapper, supplierMapper,
                priceLibraryRecordMapper, purchaseOrderMapper, store, clock);
    }

    @Test
    void previewNeverWritesAndWrongTargetCannotConsumeIt() {
        stubAwardableContext();
        SrmConfirmationPreview preview = service.previewAward("rfq_1", "quote_1", "buyer_1");

        verify(quotationMapper, never()).updateById(org.mockito.ArgumentMatchers.any(SupplierQuotation.class));
        verify(rfqProjectMapper, never()).updateById(org.mockito.ArgumentMatchers.any(RfqProject.class));
        assertThrows(SrmValidationException.class, () -> service.confirm(SrmActionType.AWARD_RFQ,
                "rfq_other", preview.confirmationToken(), "approver_1"));
        verify(quotationMapper, never()).updateById(org.mockito.ArgumentMatchers.any(SupplierQuotation.class));
    }

    @Test
    void validOneTimeConfirmationWritesAndAuditsThenRejectsReuse() {
        stubAwardableContext();
        when(quotationMapper.updateById(org.mockito.ArgumentMatchers.any(SupplierQuotation.class))).thenReturn(1);
        when(rfqProjectMapper.updateById(org.mockito.ArgumentMatchers.any(RfqProject.class))).thenReturn(1);
        SrmConfirmationPreview preview = service.previewAward("rfq_1", "quote_1", "buyer_1");

        SrmActionResult result = service.confirm(SrmActionType.AWARD_RFQ, "rfq_1", preview.confirmationToken(), "approver_1");

        assertTrue(result.executed());
        assertEquals(1, service.listAuditRecords().size());
        assertTrue(service.listAuditRecords().getFirst().executed());
        verify(quotationMapper).updateById(org.mockito.ArgumentMatchers.any(SupplierQuotation.class));
        verify(rfqProjectMapper).updateById(org.mockito.ArgumentMatchers.any(RfqProject.class));
        assertThrows(SrmValidationException.class, () -> service.confirm(SrmActionType.AWARD_RFQ,
                "rfq_1", preview.confirmationToken(), "approver_1"));
    }

    @Test
    void expiredConfirmationAndInvalidStateAreRejectedWithoutWrites() {
        stubAwardableContext();
        SrmConfirmationPreview preview = service.previewAward("rfq_1", "quote_1", "buyer_1");
        clock.advance(Duration.ofMinutes(6));
        assertThrows(SrmValidationException.class, () -> service.confirm(SrmActionType.AWARD_RFQ,
                "rfq_1", preview.confirmationToken(), "approver_1"));

        RfqProject draft = new RfqProject();
        draft.setRfqId("rfq_draft");
        draft.setStatus(RfqStatus.DRAFT);
        when(rfqProjectMapper.selectById("rfq_draft")).thenReturn(draft);
        assertThrows(SrmValidationException.class, () -> service.previewAward("rfq_draft", "quote_1", "buyer_1"));
        verify(quotationMapper, never()).updateById(org.mockito.ArgumentMatchers.any(SupplierQuotation.class));
    }

    @Test
    void suspendedSupplierPurchaseOrderCannotEnterApprovalFlow() {
        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseOrderId("po_risk");
        order.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        order.setSupplierId("sup_risk");
        when(purchaseOrderMapper.selectById("po_risk")).thenReturn(order);
        Supplier supplier = supplier("sup_risk", SupplierStatus.SUSPENDED);
        when(supplierMapper.selectById("sup_risk")).thenReturn(supplier);

        assertThrows(SrmValidationException.class, () -> service.previewPurchaseOrderApproval("po_risk", "buyer_1"));
        verify(purchaseOrderMapper, never()).updateById(org.mockito.ArgumentMatchers.any(PurchaseOrder.class));
    }

    private void stubAwardableContext() {
        RfqProject rfq = new RfqProject();
        rfq.setRfqId("rfq_1");
        rfq.setRfqNo("RFQ-1");
        rfq.setStatus(RfqStatus.EVALUATING);
        when(rfqProjectMapper.selectById("rfq_1")).thenReturn(rfq);
        SupplierQuotation quote = new SupplierQuotation();
        quote.setQuoteId("quote_1");
        quote.setQuoteNo("SQ-1");
        quote.setRfqId("rfq_1");
        quote.setSupplierId("sup_1");
        quote.setStatus(QuotationStatus.SUBMITTED);
        quote.setUnitPrice(new BigDecimal("100.00"));
        quote.setValidUntil(LocalDate.of(2026, 9, 30));
        when(quotationMapper.selectById("quote_1")).thenReturn(quote);
        when(supplierMapper.selectById("sup_1")).thenReturn(supplier("sup_1", SupplierStatus.QUALIFIED));
    }

    private static Supplier supplier(String id, SupplierStatus status) {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(id);
        supplier.setSupplierName("Synthetic Supplier");
        supplier.setStatus(status);
        supplier.setRiskLevel(SupplierRiskLevel.LOW);
        return supplier;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
