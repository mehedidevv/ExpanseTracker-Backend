package com.example.expensetracker.budget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        BigDecimal spent,
        BigDecimal remaining,
        int year,
        int month) {
}
