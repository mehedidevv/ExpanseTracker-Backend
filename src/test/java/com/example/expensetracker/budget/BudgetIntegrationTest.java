package com.example.expensetracker.budget;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensetracker.support.AbstractIntegrationTest;
import com.example.expensetracker.support.TestApi.Session;
import org.junit.jupiter.api.Test;

class BudgetIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listComputesSpentAndRemainingForTheMonth() throws Exception {
        Session a = api.register("a@example.com");
        String t = a.accessToken();
        String food = api.defaultCategoryId(t, "Food", "EXPENSE");
        String transport = api.defaultCategoryId(t, "Transport", "EXPENSE");
        String salary = api.defaultCategoryId(t, "Salary", "INCOME");

        api.createBudget(t, null, "1000.00", 2026, 11);
        api.createBudget(t, food, "300.00", 2026, 11);

        api.createTransaction(t, "EXPENSE", "120.50", food, "2026-11-05", null);
        api.createTransaction(t, "EXPENSE", "79.50", food, "2026-11-30", null);
        api.createTransaction(t, "EXPENSE", "400.00", transport, "2026-11-10", null);
        api.createTransaction(t, "EXPENSE", "999.00", food, "2026-10-31", null);   // other month
        api.createTransaction(t, "INCOME", "5000.00", salary, "2026-11-01", null); // income ignored

        api.get("/api/v1/budgets?year=2026&month=11", t)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                // overall first
                .andExpect(jsonPath("$.data[0].categoryId").doesNotExist())
                .andExpect(jsonPath("$.data[0].spent").value(600.00))
                .andExpect(jsonPath("$.data[0].remaining").value(400.00))
                .andExpect(jsonPath("$.data[1].categoryName").value("Food"))
                .andExpect(jsonPath("$.data[1].spent").value(200.00))
                .andExpect(jsonPath("$.data[1].remaining").value(100.00));
    }

    @Test
    void oneBudgetPerCategoryPerMonth() throws Exception {
        Session a = api.register("a@example.com");
        String t = a.accessToken();
        String food = api.defaultCategoryId(t, "Food", "EXPENSE");
        api.createBudget(t, null, "1000", 2026, 11);
        api.createBudget(t, food, "300", 2026, 11);

        api.post("/api/v1/budgets", t, """
                {"categoryId":null,"amount":5,"year":2026,"month":11}""").andExpect(status().isConflict());
        api.post("/api/v1/budgets", t, """
                {"categoryId":"%s","amount":5,"year":2026,"month":11}""".formatted(food))
                .andExpect(status().isConflict());
        api.post("/api/v1/budgets", t, """
                {"categoryId":"%s","amount":5,"year":2026,"month":12}""".formatted(food))
                .andExpect(status().isCreated());
    }

    @Test
    void validation() throws Exception {
        Session a = api.register("a@example.com");
        String t = a.accessToken();
        String salary = api.defaultCategoryId(t, "Salary", "INCOME");
        api.post("/api/v1/budgets", t, """
                {"categoryId":"%s","amount":5,"year":2026,"month":11}""".formatted(salary))
                .andExpect(status().isBadRequest());
        api.post("/api/v1/budgets", t, """
                {"amount":0,"year":2026,"month":13}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("amount", "month")));
        api.get("/api/v1/budgets?year=2026&month=13", t).andExpect(status().isBadRequest());
        api.get("/api/v1/budgets?year=2026", t).andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete() throws Exception {
        Session a = api.register("a@example.com");
        String t = a.accessToken();
        String id = api.createBudget(t, null, "1000", 2026, 11);
        api.put("/api/v1/budgets/" + id, t, """
                {"categoryId":null,"amount":1500.00,"year":2026,"month":11}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(1500.00))
                .andExpect(jsonPath("$.data.remaining").value(1500.00));
        api.delete("/api/v1/budgets/" + id, t).andExpect(status().isOk());
        api.get("/api/v1/budgets?year=2026&month=11", t).andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersBudgets() throws Exception {
        Session a = api.register("a@example.com");
        Session b = api.register("b@example.com");
        String id = api.createBudget(a.accessToken(), null, "1000", 2026, 11);

        api.get("/api/v1/budgets?year=2026&month=11", b.accessToken()).andExpect(jsonPath("$.data", hasSize(0)));
        api.put("/api/v1/budgets/" + id, b.accessToken(), """
                {"amount":1,"year":2026,"month":11}""").andExpect(status().isNotFound());
        api.delete("/api/v1/budgets/" + id, b.accessToken()).andExpect(status().isNotFound());
    }
}
