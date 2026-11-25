package com.example.expensetracker.transaction.dto;

import com.example.expensetracker.category.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/** Query params for GET /transactions. All optional. */
public record TransactionFilter(
        CategoryType type,
        UUID categoryId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String q) {
}
