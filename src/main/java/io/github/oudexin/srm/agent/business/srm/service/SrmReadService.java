package io.github.oudexin.srm.agent.business.srm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.oudexin.srm.agent.business.srm.dto.PriceLibraryRecordDto;
import io.github.oudexin.srm.agent.business.srm.dto.PurchaseOrderDetailDto;
import io.github.oudexin.srm.agent.business.srm.dto.PurchaseOrderLineDto;
import io.github.oudexin.srm.agent.business.srm.dto.QuotationComparisonDto;
import io.github.oudexin.srm.agent.business.srm.dto.QuotationComparisonItemDto;
import io.github.oudexin.srm.agent.business.srm.dto.QuotationSummaryDto;
import io.github.oudexin.srm.agent.business.srm.dto.RfqDetailDto;
import io.github.oudexin.srm.agent.business.srm.dto.SrmReadResult;
import io.github.oudexin.srm.agent.business.srm.dto.SupplierProfileDto;
import io.github.oudexin.srm.agent.business.srm.entity.PriceLibraryRecord;
import io.github.oudexin.srm.agent.business.srm.entity.PriceRecordStatus;
import io.github.oudexin.srm.agent.business.srm.entity.PurchaseOrder;
import io.github.oudexin.srm.agent.business.srm.entity.PurchaseOrderLine;
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
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Read-only SRM projections and deterministic quote comparison. It never returns persistence entities. */
@Service
public class SrmReadService {
    private static final List<String> COMPARISON_RULES = List.of(
            "仅比较状态为 SUBMITTED 或 ACCEPTED、未过有效期、且供应商未暂停/取消资格的报价。",
            "价格排名权重 45%（单价越低越好）；交期排名权重 25%（天数越少越好）。",
            "账期排名权重 15%（天数越多越好）；风险排名权重 15%（LOW 优于 MEDIUM 优于 HIGH/CRITICAL）。",
            "weightedRank 为各维名次的加权和，数值越小越靠前；相同结果按 quoteNo、quoteId 固定排序，非模型结论。"
    );

    private final SupplierMapper supplierMapper;
    private final RfqProjectMapper rfqProjectMapper;
    private final SupplierQuotationMapper quotationMapper;
    private final PriceLibraryRecordMapper priceLibraryRecordMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderLineMapper purchaseOrderLineMapper;
    private final Clock clock;

    @Autowired
    public SrmReadService(SupplierMapper supplierMapper, RfqProjectMapper rfqProjectMapper,
            SupplierQuotationMapper quotationMapper, PriceLibraryRecordMapper priceLibraryRecordMapper,
            PurchaseOrderMapper purchaseOrderMapper, PurchaseOrderLineMapper purchaseOrderLineMapper) {
        this(supplierMapper, rfqProjectMapper, quotationMapper, priceLibraryRecordMapper,
                purchaseOrderMapper, purchaseOrderLineMapper, Clock.systemUTC());
    }

    SrmReadService(SupplierMapper supplierMapper, RfqProjectMapper rfqProjectMapper,
            SupplierQuotationMapper quotationMapper, PriceLibraryRecordMapper priceLibraryRecordMapper,
            PurchaseOrderMapper purchaseOrderMapper, PurchaseOrderLineMapper purchaseOrderLineMapper, Clock clock) {
        this.supplierMapper = supplierMapper;
        this.rfqProjectMapper = rfqProjectMapper;
        this.quotationMapper = quotationMapper;
        this.priceLibraryRecordMapper = priceLibraryRecordMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderLineMapper = purchaseOrderLineMapper;
        this.clock = clock;
    }

    public SrmReadResult<SupplierProfileDto> findSupplier(String supplierId) {
        Supplier supplier = supplierMapper.selectById(SrmInputValidator.requiredId(supplierId, "supplierId"));
        return supplier == null ? SrmReadResult.notFound("供应商") : SrmReadResult.found(toSupplierDto(supplier));
    }

    public SrmReadResult<RfqDetailDto> findRfq(String rfqId) {
        RfqProject rfq = rfqProjectMapper.selectById(SrmInputValidator.requiredId(rfqId, "rfqId"));
        if (rfq == null) {
            return SrmReadResult.notFound("RFQ");
        }
        List<SupplierQuotation> quotes = quotationMapper.selectList(new LambdaQueryWrapper<SupplierQuotation>()
                .eq(SupplierQuotation::getRfqId, rfq.getRfqId()));
        Map<String, Supplier> suppliers = suppliersById(quotes);
        List<QuotationSummaryDto> summaries = quotes.stream()
                .map(quote -> toQuotationDto(quote, suppliers.get(quote.getSupplierId())))
                .sorted(Comparator.comparing(QuotationSummaryDto::quoteNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(QuotationSummaryDto::quoteId))
                .toList();
        return SrmReadResult.found(new RfqDetailDto(rfq.getRfqId(), rfq.getRfqNo(), rfq.getProjectName(),
                rfq.getMaterialCode(), rfq.getMaterialName(), rfq.getRequiredQuantity(), rfq.getUnit(),
                rfq.getRequiredDeliveryDate(), rfq.getQuotationDeadline(), rfq.getBudgetAmount(), rfq.getCurrency(),
                enumCode(rfq.getStatus()), rfq.getOwnerId(), rfq.getAwardSupplierId(), rfq.getAwardQuoteId(), summaries));
    }

    public SrmReadResult<QuotationComparisonDto> compareEligibleQuotations(String rfqId) {
        String validRfqId = SrmInputValidator.requiredId(rfqId, "rfqId");
        if (rfqProjectMapper.selectById(validRfqId) == null) {
            return SrmReadResult.notFound("RFQ");
        }
        List<SupplierQuotation> allQuotes = quotationMapper.selectList(new LambdaQueryWrapper<SupplierQuotation>()
                .eq(SupplierQuotation::getRfqId, validRfqId));
        Map<String, Supplier> suppliers = suppliersById(allQuotes);
        LocalDate today = LocalDate.now(clock);
        List<ComparableQuote> eligible = allQuotes.stream()
                .filter(quote -> isComparable(quote, suppliers.get(quote.getSupplierId()), today))
                .map(quote -> new ComparableQuote(quote, suppliers.get(quote.getSupplierId())))
                .toList();
        if (eligible.isEmpty()) {
            return SrmReadResult.noData(new QuotationComparisonDto(validRfqId, 0, allQuotes.size(), COMPARISON_RULES, List.of()));
        }
        List<QuotationComparisonItemDto> ranked = rank(eligible);
        return SrmReadResult.found(new QuotationComparisonDto(validRfqId, ranked.size(),
                allQuotes.size() - ranked.size(), COMPARISON_RULES, ranked));
    }

    public SrmReadResult<List<PriceLibraryRecordDto>> findPriceLibrary(String materialCode, boolean includeInactive) {
        String validMaterialCode = SrmInputValidator.requiredMaterialCode(materialCode);
        LambdaQueryWrapper<PriceLibraryRecord> query = new LambdaQueryWrapper<PriceLibraryRecord>()
                .eq(PriceLibraryRecord::getMaterialCode, validMaterialCode);
        if (!includeInactive) {
            query.in(PriceLibraryRecord::getStatus, PriceRecordStatus.VALID, PriceRecordStatus.EXPIRING);
        }
        LocalDate today = LocalDate.now(clock);
        List<PriceLibraryRecordDto> records = priceLibraryRecordMapper.selectList(query).stream()
                .map(record -> toPriceDto(record, supplierMapper.selectById(record.getSupplierId()), today))
                .sorted(Comparator.comparing(PriceLibraryRecordDto::effectiveEndDate).thenComparing(PriceLibraryRecordDto::priceRecordId))
                .toList();
        return records.isEmpty() ? SrmReadResult.noData(records) : SrmReadResult.found(records);
    }

    public SrmReadResult<PurchaseOrderDetailDto> findPurchaseOrder(String purchaseOrderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(SrmInputValidator.requiredId(purchaseOrderId, "purchaseOrderId"));
        if (order == null) {
            return SrmReadResult.notFound("采购订单");
        }
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        List<PurchaseOrderLineDto> lines = purchaseOrderLineMapper.selectList(new LambdaQueryWrapper<PurchaseOrderLine>()
                        .eq(PurchaseOrderLine::getPurchaseOrderId, order.getPurchaseOrderId()))
                .stream().map(this::toPurchaseOrderLineDto)
                .sorted(Comparator.comparingInt(PurchaseOrderLineDto::lineNo)).toList();
        return SrmReadResult.found(new PurchaseOrderDetailDto(order.getPurchaseOrderId(), order.getPoNo(),
                order.getSupplierId(), supplier == null ? null : supplier.getSupplierName(),
                supplier == null ? null : enumCode(supplier.getStatus()),
                supplier == null ? null : enumCode(supplier.getRiskLevel()), order.getSourceRfqId(),
                order.getSelectedQuoteId(), enumCode(order.getStatus()), order.getPurchaserId(), order.getOrderDate(),
                order.getExpectedDeliveryDate(), order.getCurrency(), order.getTotalAmount(),
                order.isHighRiskReviewRequired(), order.getRemark(), lines));
    }

    private Map<String, Supplier> suppliersById(List<SupplierQuotation> quotes) {
        return quotes.stream().map(SupplierQuotation::getSupplierId).distinct()
                .map(id -> Map.entry(id, supplierMapper.selectById(id))).filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<QuotationComparisonItemDto> rank(List<ComparableQuote> quotes) {
        Map<String, Integer> priceRanks = ranks(quotes, Comparator.comparing(item -> item.quote().getUnitPrice(), Comparator.nullsLast(BigDecimal::compareTo)));
        Map<String, Integer> deliveryRanks = ranks(quotes, Comparator.comparing(item -> item.quote().getLeadTimeDays(), Comparator.nullsLast(Integer::compareTo)));
        Map<String, Integer> termsRanks = ranks(quotes, Comparator.comparing((ComparableQuote item) -> item.supplier().getPaymentTermsDays(), Comparator.nullsLast(Comparator.reverseOrder())));
        Map<String, Integer> riskRanks = ranks(quotes, Comparator.comparingInt(item -> riskRank(item.supplier().getRiskLevel())));
        List<QuotationComparisonItemDto> result = new ArrayList<>();
        for (ComparableQuote item : quotes) {
            String id = item.quote().getQuoteId();
            BigDecimal score = BigDecimal.valueOf(priceRanks.get(id)).multiply(BigDecimal.valueOf(0.45))
                    .add(BigDecimal.valueOf(deliveryRanks.get(id)).multiply(BigDecimal.valueOf(0.25)))
                    .add(BigDecimal.valueOf(termsRanks.get(id)).multiply(BigDecimal.valueOf(0.15)))
                    .add(BigDecimal.valueOf(riskRanks.get(id)).multiply(BigDecimal.valueOf(0.15)))
                    .setScale(2, RoundingMode.HALF_UP);
            result.add(new QuotationComparisonItemDto(0, id, item.quote().getSupplierId(), item.supplier().getSupplierName(),
                    item.quote().getUnitPrice(), item.quote().getLeadTimeDays(), item.supplier().getPaymentTermsDays(),
                    enumCode(item.supplier().getRiskLevel()), item.quote().getValidUntil(), score,
                    List.of("价格名次=" + priceRanks.get(id) + " ×45%", "交期名次=" + deliveryRanks.get(id) + " ×25%",
                            "账期名次=" + termsRanks.get(id) + " ×15%", "风险名次=" + riskRanks.get(id) + " ×15%")));
        }
        result.sort(Comparator.comparing(QuotationComparisonItemDto::weightedRank)
                .thenComparing(QuotationComparisonItemDto::quoteId));
        List<QuotationComparisonItemDto> ranked = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            QuotationComparisonItemDto item = result.get(i);
            ranked.add(new QuotationComparisonItemDto(i + 1, item.quoteId(), item.supplierId(), item.supplierName(),
                    item.unitPrice(), item.leadTimeDays(), item.paymentTermsDays(), item.riskLevel(), item.validUntil(),
                    item.weightedRank(), item.ruleExplanation()));
        }
        return List.copyOf(ranked);
    }

    private Map<String, Integer> ranks(List<ComparableQuote> quotes, Comparator<ComparableQuote> comparator) {
        List<ComparableQuote> sorted = quotes.stream().sorted(comparator
                .thenComparing(item -> item.quote().getQuoteNo(), Comparator.nullsLast(String::compareTo))
                .thenComparing(item -> item.quote().getQuoteId())).toList();
        return java.util.stream.IntStream.range(0, sorted.size()).boxed()
                .collect(Collectors.toMap(index -> sorted.get(index).quote().getQuoteId(), index -> index + 1));
    }

    private boolean isComparable(SupplierQuotation quote, Supplier supplier, LocalDate today) {
        return supplier != null
                && (quote.getStatus() == QuotationStatus.SUBMITTED || quote.getStatus() == QuotationStatus.ACCEPTED)
                && (quote.getValidUntil() == null || !quote.getValidUntil().isBefore(today))
                && supplier.getStatus() != SupplierStatus.SUSPENDED
                && supplier.getStatus() != SupplierStatus.DISQUALIFIED;
    }

    private int riskRank(SupplierRiskLevel riskLevel) {
        if (riskLevel == null) return 4;
        return switch (riskLevel) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private SupplierProfileDto toSupplierDto(Supplier supplier) {
        return new SupplierProfileDto(supplier.getSupplierId(), supplier.getSupplierCode(), supplier.getSupplierName(),
                supplier.getSupplyCategory(), enumCode(supplier.getStatus()), enumCode(supplier.getRiskLevel()),
                supplier.getQualityScore(), supplier.getDeliveryScore(), supplier.getPaymentTermsDays(),
                supplier.getCountryRegion(), supplier.getRiskNote());
    }

    private QuotationSummaryDto toQuotationDto(SupplierQuotation quote, Supplier supplier) {
        return new QuotationSummaryDto(quote.getQuoteId(), quote.getQuoteNo(), quote.getSupplierId(),
                supplier == null ? null : supplier.getSupplierName(), supplier == null ? null : enumCode(supplier.getStatus()),
                supplier == null ? null : enumCode(supplier.getRiskLevel()), enumCode(quote.getStatus()), quote.getUnitPrice(),
                quote.getQuotedQuantity(), quote.getTotalAmount(), quote.getCurrency(), quote.getLeadTimeDays(),
                supplier == null ? null : supplier.getPaymentTermsDays(), quote.getValidUntil(), quote.getRiskNote());
    }

    private PriceLibraryRecordDto toPriceDto(PriceLibraryRecord record, Supplier supplier, LocalDate today) {
        boolean usable = record.getStatus() == PriceRecordStatus.VALID
                && !record.getEffectiveStartDate().isAfter(today) && !record.getEffectiveEndDate().isBefore(today);
        return new PriceLibraryRecordDto(record.getPriceRecordId(), record.getSupplierId(),
                supplier == null ? null : supplier.getSupplierName(), record.getMaterialCode(), record.getMaterialName(),
                record.getUnitPrice(), record.getCurrency(), record.getMinimumOrderQuantity(), record.getEffectiveStartDate(),
                record.getEffectiveEndDate(), enumCode(record.getStatus()), usable, record.getSourceQuoteId(), record.getRemark());
    }

    private PurchaseOrderLineDto toPurchaseOrderLineDto(PurchaseOrderLine line) {
        BigDecimal received = line.getReceivedQuantity() == null ? BigDecimal.ZERO : line.getReceivedQuantity();
        BigDecimal ordered = line.getOrderedQuantity() == null ? BigDecimal.ZERO : line.getOrderedQuantity();
        return new PurchaseOrderLineDto(line.getLineNo(), line.getMaterialCode(), line.getMaterialName(), line.getUnit(),
                ordered, received, ordered.subtract(received), line.getUnitPrice(), line.getTaxRate(),
                enumCode(line.getStatus()), line.getRemark());
    }

    private static String enumCode(Object value) {
        if (value == null) return null;
        if (value instanceof SupplierStatus status) return status.getCode();
        if (value instanceof SupplierRiskLevel level) return level.getCode();
        if (value instanceof PriceRecordStatus status) return status.getCode();
        if (value instanceof QuotationStatus status) return status.getCode();
        if (value instanceof io.github.oudexin.srm.agent.business.srm.entity.RfqStatus status) return status.getCode();
        if (value instanceof io.github.oudexin.srm.agent.business.srm.entity.PurchaseOrderStatus status) return status.getCode();
        if (value instanceof io.github.oudexin.srm.agent.business.srm.entity.PurchaseOrderLineStatus status) return status.getCode();
        return value.toString();
    }

    private record ComparableQuote(SupplierQuotation quote, Supplier supplier) {
    }
}
