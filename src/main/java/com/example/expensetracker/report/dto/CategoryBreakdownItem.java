package com.example.expensetracker.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryBreakdownItem(UUID categoryId, String categoryName, BigDecimal amount,
                                    BigDecimal percentage) {
}
