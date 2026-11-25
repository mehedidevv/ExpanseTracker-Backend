package com.example.expensetracker.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SummaryResponse(LocalDate from, LocalDate to, BigDecimal totalIncome, BigDecimal totalExpense,
                              BigDecimal balance) {
}
