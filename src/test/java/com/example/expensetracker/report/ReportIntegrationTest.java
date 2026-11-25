package com.example.expensetracker.report;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensetracker.support.AbstractIntegrationTest;
import com.example.expensetracker.support.TestApi.Session;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeEach;

class ReportIntegrationTest extends AbstractIntegrationTest {

    private Session a;

    @BeforeEach
    void seed() throws Exception {
        a = api.register("a@example.com");
        String t = a.accessToken();
        String food = api.defaultCategoryId(t, "Food", "EXPENSE");
        String rent = api.defaultCategoryId(t, "Rent", "EXPENSE");
        String salary = api.defaultCategoryId(t, "Salary", "INCOME");

        api.createTransaction(t, "INCOME", "5000.00", salary, "2026-01-01", null);
        api.createTransaction(t, "EXPENSE", "1000.00", rent, "2026-01-02", null);
        api.createTransaction(t, "EXPENSE", "250.25", food, "2026-01-10", null);
        api.createTransaction(t, "EXPENSE", "249.75", food, "2026-03-15", null);
        api.createTransaction(t, "INCOME", "5000.00", salary, "2026-03-01", null);
        api.createTransaction(t, "EXPENSE", "77.00", food, "2025-12-31", null); // previous year
    }

    @Test
    void summary() throws Exception {
        api.get("/api/v1/reports/summary?from=2026-01-01&to=2026-12-31", a.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").value(10000.00))
                .andExpect(jsonPath("$.data.totalExpense").value(1500.00))
                .andExpect(jsonPath("$.data.balance").value(8500.00));
    }

    @Test
    void byCategoryHasAmountsAndPercentagesSortedDesc() throws Exception {
        api.get("/api/v1/reports/by-category?from=2026-01-01&to=2026-12-31", a.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].categoryName").value("Rent"))
                .andExpect(jsonPath("$.data[0].amount").value(1000.00))
                .andExpect(jsonPath("$.data[0].percentage").value(66.67))
                .andExpect(jsonPath("$.data[1].categoryName").value("Food"))
                .andExpect(jsonPath("$.data[1].amount").value(500.00))
                .andExpect(jsonPath("$.data[1].percentage").value(33.33));
    }

    @Test
    void monthlyAlwaysHas12Months() throws Exception {
        api.get("/api/v1/reports/monthly?year=2026", a.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(12)))
                .andExpect(jsonPath("$.data[0].month").value(1))
                .andExpect(jsonPath("$.data[0].income").value(5000.00))
                .andExpect(jsonPath("$.data[0].expense").value(1250.25))
                .andExpect(jsonPath("$.data[1].income").value(0))
                .andExpect(jsonPath("$.data[2].expense").value(249.75));
    }

    @Test
    void reportsOnlyIncludeTheCallersData() throws Exception {
        Session b = api.register("b@example.com");
        api.get("/api/v1/reports/summary?from=2026-01-01&to=2026-12-31", b.accessToken())
                .andExpect(jsonPath("$.data.totalIncome").value(0))
                .andExpect(jsonPath("$.data.totalExpense").value(0));
        api.get("/api/v1/reports/by-category?from=2026-01-01&to=2026-12-31", b.accessToken())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void validation() throws Exception {
        api.get("/api/v1/reports/summary?from=2026-12-01&to=2026-01-01", a.accessToken())
                .andExpect(status().isBadRequest());
        api.get("/api/v1/reports/summary?from=2026-01-01", a.accessToken()).andExpect(status().isBadRequest());
        api.get("/api/v1/reports/monthly?year=1999", a.accessToken()).andExpect(status().isBadRequest());
    }
}
