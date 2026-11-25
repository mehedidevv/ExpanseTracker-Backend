package com.example.expensetracker.report;

import com.example.expensetracker.category.CategoryType;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.report.ReportRepository.MonthTypeTotal;
import com.example.expensetracker.report.ReportRepository.TypeTotal;
import com.example.expensetracker.report.dto.CategoryBreakdownItem;
import com.example.expensetracker.report.dto.MonthlyTrendItem;
import com.example.expensetracker.report.dto.SummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-R1..R3. All amounts are scaled to 2 decimals. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public SummaryResponse summary(UUID userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        BigDecimal income = ZERO;
        BigDecimal expense = ZERO;
        for (TypeTotal t : reportRepository.totalsByType(userId, from, to)) {
            if (t.type() == CategoryType.INCOME) {
                income = scale(t.total());
            } else {
                expense = scale(t.total());
            }
        }
        return new SummaryResponse(from, to, income, expense, income.subtract(expense));
    }

    public List<CategoryBreakdownItem> byCategory(UUID userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        var rows = reportRepository.expenseByCategory(userId, from, to);
        BigDecimal total = rows.stream().map(ReportRepository.CategoryTotal::total).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        return rows.stream()
                .map(r -> new CategoryBreakdownItem(r.categoryId(), r.categoryName(), scale(r.total()),
                        total.signum() == 0 ? ZERO
                                : r.total().multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP)))
                .toList();
    }

    /** Always returns 12 entries (months without data are zero). */
    public List<MonthlyTrendItem> monthly(UUID userId, int year) {
        BigDecimal[] income = new BigDecimal[13];
        BigDecimal[] expense = new BigDecimal[13];
        for (MonthTypeTotal row : reportRepository.monthlyTotals(userId, year)) {
            if (row.type() == CategoryType.INCOME) {
                income[row.month()] = scale(row.total());
            } else {
                expense[row.month()] = scale(row.total());
            }
        }
        List<MonthlyTrendItem> result = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            result.add(new MonthlyTrendItem(m, income[m] == null ? ZERO : income[m],
                    expense[m] == null ? ZERO : expense[m]));
        }
        return result;
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
