package com.example.expensetracker.report.dto;

import java.math.BigDecimal;

public record MonthlyTrendItem(int month, BigDecimal income, BigDecimal expense) {
}
