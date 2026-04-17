package com.smartpos.backend.reports;

import com.smartpos.backend.common.error.BusinessRuleException;
import com.smartpos.backend.reports.dto.DailySummaryResponse;
import com.smartpos.backend.reports.dto.TopProductsResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final int MAX_TOP_PRODUCTS = 100;

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily-summary")
    @PreAuthorize("hasRole('OWNER')")
    public DailySummaryResponse dailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate target = date == null ? LocalDate.now() : date;
        return reportService.dailySummary(target);
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasRole('OWNER')")
    public TopProductsResponse topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("'to' must be on or after 'from'");
        }
        if (limit <= 0 || limit > MAX_TOP_PRODUCTS) {
            throw new BusinessRuleException("limit must be between 1 and " + MAX_TOP_PRODUCTS);
        }
        return reportService.topProducts(from, to, limit);
    }
}
