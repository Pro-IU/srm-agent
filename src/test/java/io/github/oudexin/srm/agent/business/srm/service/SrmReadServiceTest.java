package io.github.oudexin.srm.agent.business.srm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.oudexin.srm.agent.business.srm.dto.QuotationComparisonDto;
import io.github.oudexin.srm.agent.business.srm.dto.SrmReadResult;
import io.github.oudexin.srm.agent.business.srm.entity.QuotationStatus;
import io.github.oudexin.srm.agent.business.srm.entity.RfqProject;
import io.github.oudexin.srm.agent.business.srm.entity.Supplier;
import io.github.oudexin.srm.agent.business.srm.entity.SupplierQuotation;
import io.github.oudexin.srm.agent.business.srm.entity.SupplierRiskLevel;
import io.github.oudexin.srm.agent.business.srm.entity.SupplierStatus;
import io.github.oudexin.srm.agent.business.srm.mapper.PriceLibraryRecordMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.PurchaseOrderLineMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.PurchaseOrderMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.RfqProjectMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.SupplierMapper;
import io.github.oudexin.srm.agent.business.srm.mapper.SupplierQuotationMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SrmReadServiceTest {
    @Mock private SupplierMapper supplierMapper;
    @Mock private RfqProjectMapper rfqProjectMapper;
    @Mock private SupplierQuotationMapper quotationMapper;
    @Mock private PriceLibraryRecordMapper priceLibraryRecordMapper;
    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private PurchaseOrderLineMapper purchaseOrderLineMapper;

    @Test
    void comparisonUsesDeterministicTransparentRanks() {
        RfqProject rfq = new RfqProject();
        rfq.setRfqId("rfq_1");
        when(rfqProjectMapper.selectById("rfq_1")).thenReturn(rfq);
        Supplier alpha = supplier("sup_a", "Alpha", SupplierStatus.QUALIFIED, SupplierRiskLevel.LOW, 45);
        Supplier beta = supplier("sup_b", "Beta", SupplierStatus.POTENTIAL, SupplierRiskLevel.MEDIUM, 30);
        when(supplierMapper.selectById("sup_a")).thenReturn(alpha);
        when(supplierMapper.selectById("sup_b")).thenReturn(beta);
        when(quotationMapper.selectList(any())).thenReturn(List.of(
                quote("q_a", "SQ-A", "sup_a", "100.00", 20), quote("q_b", "SQ-B", "sup_b", "90.00", 10)));
        SrmReadService service = service();

        SrmReadResult<QuotationComparisonDto> result = service.compareEligibleQuotations("rfq_1");

        assertTrue(result.found());
        assertEquals("q_b", result.data().rankedQuotes().getFirst().quoteId());
        assertEquals(1, result.data().rankedQuotes().getFirst().rank());
        assertEquals(4, result.data().rules().size());
    }

    @Test
    void expiredOrSuspendedQuotesAreExcludedWithoutPretendingNoRfqExists() {
        RfqProject rfq = new RfqProject();
        rfq.setRfqId("rfq_1");
        when(rfqProjectMapper.selectById("rfq_1")).thenReturn(rfq);
        Supplier suspended = supplier("sup_s", "Suspended", SupplierStatus.SUSPENDED, SupplierRiskLevel.HIGH, 30);
        when(supplierMapper.selectById("sup_s")).thenReturn(suspended);
        SupplierQuotation quote = quote("q_s", "SQ-S", "sup_s", "80.00", 5);
        quote.setValidUntil(LocalDate.of(2026, 8, 30));
        when(quotationMapper.selectList(any())).thenReturn(List.of(quote));

        SrmReadResult<QuotationComparisonDto> result = service().compareEligibleQuotations("rfq_1");

        assertTrue(result.found());
        assertEquals("NO_DATA", result.code());
        assertFalse(result.data().rankedQuotes().iterator().hasNext());
        assertEquals(1, result.data().excludedQuoteCount());
    }

    private SrmReadService service() {
        return new SrmReadService(supplierMapper, rfqProjectMapper, quotationMapper, priceLibraryRecordMapper,
                purchaseOrderMapper, purchaseOrderLineMapper,
                Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC));
    }

    private static Supplier supplier(String id, String name, SupplierStatus status, SupplierRiskLevel risk, int terms) {
        Supplier supplier = new Supplier();
        supplier.setSupplierId(id);
        supplier.setSupplierName(name);
        supplier.setStatus(status);
        supplier.setRiskLevel(risk);
        supplier.setPaymentTermsDays(terms);
        return supplier;
    }

    private static SupplierQuotation quote(String id, String number, String supplierId, String price, int leadDays) {
        SupplierQuotation quote = new SupplierQuotation();
        quote.setQuoteId(id);
        quote.setQuoteNo(number);
        quote.setRfqId("rfq_1");
        quote.setSupplierId(supplierId);
        quote.setStatus(QuotationStatus.SUBMITTED);
        quote.setUnitPrice(new BigDecimal(price));
        quote.setLeadTimeDays(leadDays);
        quote.setValidUntil(LocalDate.of(2026, 9, 30));
        return quote;
    }
}
