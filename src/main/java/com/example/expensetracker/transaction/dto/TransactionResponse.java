package com.example.expensetracker.transaction.dto;

import com.example.expensetracker.category.CategoryType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        CategoryType type,
        BigDecimal amount,
        UUID categoryId,
        String categoryName,
        String note,
        LocalDate transactionDate,
        Instant createdAt) {
}
