package com.smartpos.backend.reports;

import com.smartpos.backend.domain.enums.PaymentMethod;
import com.smartpos.backend.domain.enums.SaleStatus;
import com.smartpos.backend.products.ProductEntity;
import com.smartpos.backend.products.ProductRepository;
import com.smartpos.backend.reports.dto.DailySummaryResponse;
import com.smartpos.backend.reports.dto.PaymentMethodBreakdown;
import com.smartpos.backend.reports.dto.TopProductRow;
import com.smartpos.backend.reports.dto.TopProductsResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;

    public ReportService(ReportRepository reportRepository, ProductRepository productRepository) {
        this.reportRepository = reportRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public DailySummaryResponse dailySummary(LocalDate date) {
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Object[] totals = firstRow(reportRepository.salesTotals(SaleStatus.COMPLETED, from, to));
        long salesCount = ((Number) totals[0]).longValue();
        BigDecimal netRevenue = toBigDecimal(totals[1]);
        BigDecimal taxRevenue = toBigDecimal(totals[2]);
        BigDecimal totalRevenue = toBigDecimal(totals[3]);

        Object[] cancelled = firstRow(reportRepository.cancelledTotals(SaleStatus.CANCELLED, from, to));
        long cancelledCount = ((Number) cancelled[0]).longValue();
        BigDecimal cancelledAmount = toBigDecimal(cancelled[1]);

        Long itemsSold = reportRepository.totalItemsSold(SaleStatus.COMPLETED, from, to);

        List<PaymentMethodBreakdown> breakdown = new ArrayList<>();
        for (Object[] row : reportRepository.salesAggregateByPaymentMethod(SaleStatus.COMPLETED, from, to)) {
            breakdown.add(new PaymentMethodBreakdown(
                    (PaymentMethod) row[0],
                    ((Number) row[1]).longValue(),
                    toBigDecimal(row[2])
            ));
        }

        return new DailySummaryResponse(
                date, salesCount, netRevenue, taxRevenue, totalRevenue,
                itemsSold == null ? 0L : itemsSold,
                cancelledCount, cancelledAmount,
                breakdown
        );
    }

    @Transactional(readOnly = true)
    public TopProductsResponse topProducts(LocalDate from, LocalDate to, int limit) {
        Instant fromI = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toI   = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> rows = reportRepository.topProducts(
                SaleStatus.COMPLETED, fromI, toI, PageRequest.of(0, limit));

        Set<UUID> productIds = rows.stream().map(r -> (UUID) r[0]).collect(Collectors.toSet());
        Map<UUID, ProductEntity> products = loadProducts(productIds);

        List<TopProductRow> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            long qty = ((Number) r[1]).longValue();
            BigDecimal revenue = toBigDecimal(r[2]);
            ProductEntity p = products.get(id);
            out.add(new TopProductRow(id,
                    p == null ? null : p.getSku(),
                    p == null ? null : p.getName(),
                    qty, revenue));
        }
        return new TopProductsResponse(from, to, limit, out);
    }

    private Object[] firstRow(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return new Object[] {0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        return rows.get(0);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    private Map<UUID, ProductEntity> loadProducts(Collection<UUID> ids) {
        Map<UUID, ProductEntity> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        productRepository.findAllById(ids).forEach(p -> map.put(p.getId(), p));
        return map;
    }
}
