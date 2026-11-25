package com.example.expensetracker.transaction.dto;

import com.example.expensetracker.category.CategoryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull CategoryType type,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull UUID categoryId,
        @NotNull LocalDate transactionDate,
        @Size(max = 255) String note) {
}
