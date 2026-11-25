package com.example.expensetracker.report;

import com.example.expensetracker.common.security.CurrentUser;
import com.example.expensetracker.report.dto.CategoryBreakdownItem;
import com.example.expensetracker.report.dto.MonthlyTrendItem;
import com.example.expensetracker.report.dto.SummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.expensetracker.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Total income, expense and balance for a date range (inclusive)")
    @GetMapping("/summary")
    public ApiResponse<SummaryResponse> summary(@AuthenticationPrincipal CurrentUser user,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Summary report generated successfully", reportService.summary(user.id(), from, to));
    }

    @Operation(summary = "Expense breakdown by category (amount + percentage)")
    @GetMapping("/by-category")
    public ApiResponse<List<CategoryBreakdownItem>> byCategory(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Category breakdown generated successfully", reportService.byCategory(user.id(), from, to));
    }

    @Operation(summary = "Income and expense per month for a year (12 entries)")
    @GetMapping("/monthly")
    public ApiResponse<List<MonthlyTrendItem>> monthly(@AuthenticationPrincipal CurrentUser user,
                                          @RequestParam @Min(2000) @Max(2100) int year) {
        return ApiResponse.ok("Monthly trend generated successfully", reportService.monthly(user.id(), year));
    }
}
