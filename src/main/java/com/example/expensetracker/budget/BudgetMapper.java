package com.example.expensetracker.budget;

import com.example.expensetracker.budget.dto.BudgetResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget b, BigDecimal spent) {
        var category = b.getCategory();
        return new BudgetResponse(
                b.getId(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                b.getAmount(),
                spent.setScale(2, RoundingMode.HALF_UP),
                b.getAmount().subtract(spent).setScale(2, RoundingMode.HALF_UP),
                b.getYear(),
                b.getMonth());
    }
}
