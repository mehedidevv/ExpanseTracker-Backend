package com.example.expensetracker.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** categoryId null = overall budget. */
public record BudgetRequest(
        UUID categoryId,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(12) Integer month) {
}
