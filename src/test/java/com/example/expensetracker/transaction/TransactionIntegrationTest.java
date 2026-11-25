package com.example.expensetracker.transaction;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensetracker.support.AbstractIntegrationTest;
import com.example.expensetracker.support.TestApi.Session;
import org.junit.jupiter.api.Test;

class TransactionIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createAndGetKeepsExactMoneyPrecision() throws Exception {
        Session a = api.register("a@example.com");
        String food = api.defaultCategoryId(a.accessToken(), "Food", "EXPENSE");
        String id = api.createTransaction(a.accessToken(), "EXPENSE", "0.10", food, "2026-11-20", "Tea");

        api.get("/api/v1/transactions/" + id, a.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(0.10))
                .andExpect(jsonPath("$.data.categoryName").value("Food"))
                .andExpect(jsonPath("$.data.transactionDate").value("2026-11-20"))
                .andExpect(jsonPath("$.data.note").value("Tea"));

        String amount = jdbc.queryForObject("select amount::text from transactions where id = ?::uuid",
                String.class, id);
        org.assertj.core.api.Assertions.assertThat(amount).isEqualTo("0.10");
    }

    @Test
    void validationRules() throws Exception {
        Session a = api.register("a@example.com");
        String food = api.defaultCategoryId(a.accessToken(), "Food", "EXPENSE");
        String salary = api.defaultCategoryId(a.accessToken(), "Salary", "INCOME");

        // amount must be > 0
        api.post("/api/v1/transactions", a.accessToken(), body("EXPENSE", "0", food))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
        // max 2 decimals
        api.post("/api/v1/transactions", a.accessToken(), body("EXPENSE", "1.234", food))
                .andExpect(status().isBadRequest());
        // type must match category type
        api.post("/api/v1/transactions", a.accessToken(), body("EXPENSE", "5", salary))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match")));
        // malformed JSON / enum
        api.post("/api/v1/transactions", a.accessToken(), body("SPENDING", "5", food))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listIsPagedFilteredAndSortedByDateDescByDefault() throws Exception {
        Session a = api.register("a@example.com");
        String t = a.accessToken();
        String food = api.defaultCategoryId(t, "Food", "EXPENSE");
        String transport = api.defaultCategoryId(t, "Transport", "EXPENSE");
        String salary = api.defaultCategoryId(t, "Salary", "INCOME");
        api.createTransaction(t, "EXPENSE", "100.00", food, "2026-11-01", "Lunch with team");
        api.createTransaction(t, "EXPENSE", "50.00", transport, "2026-11-03", "Bus");
        api.createTransaction(t, "INCOME", "5000.00", salary, "2026-11-02", "November salary");
        api.createTransaction(t, "EXPENSE", "20.00", food, "2026-10-15", "Snacks");

        api.get("/api/v1/transactions", t)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.content[*].transactionDate",
                        contains("2026-11-03", "2026-11-02", "2026-11-01", "2026-10-15")));

        api.get("/api/v1/transactions?size=2&page=1", t)
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        api.get("/api/v1/transactions?type=EXPENSE&from=2026-11-01&to=2026-11-30", t)
                .andExpect(jsonPath("$.data.totalElements").value(2));
        api.get("/api/v1/transactions?categoryId=" + food, t)
                .andExpect(jsonPath("$.data.totalElements").value(2));
        api.get("/api/v1/transactions?minAmount=50&maxAmount=100", t)
                .andExpect(jsonPath("$.data.totalElements").value(2));
        api.get("/api/v1/transactions?q=LUNCH", t)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].note").value("Lunch with team"));
        api.get("/api/v1/transactions?sort=amount,asc", t)
                .andExpect(jsonPath("$.data.content[0].amount").value(20.00));
    }

    @Test
    void listRejectsBadSortAndBadRanges() throws Exception {
        Session a = api.register("a@example.com");
        api.get("/api/v1/transactions?sort=user.password,asc", a.accessToken()).andExpect(status().isBadRequest());
        api.get("/api/v1/transactions?from=2026-12-01&to=2026-11-01", a.accessToken())
                .andExpect(status().isBadRequest());
        api.get("/api/v1/transactions?from=not-a-date", a.accessToken()).andExpect(status().isBadRequest());
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        Session a = api.register("a@example.com");
        api.get("/api/v1/transactions?size=500", a.accessToken()).andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    void updateAndDelete() throws Exception {
        Session a = api.register("a@example.com");
        String food = api.defaultCategoryId(a.accessToken(), "Food", "EXPENSE");
        String id = api.createTransaction(a.accessToken(), "EXPENSE", "10.00", food, "2026-11-01", null);

        api.put("/api/v1/transactions/" + id, a.accessToken(), """
                {"type":"EXPENSE","amount":12.50,"categoryId":"%s","transactionDate":"2026-11-02","note":"fixed"}
                """.formatted(food))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(12.50))
                .andExpect(jsonPath("$.data.note").value("fixed"));

        api.delete("/api/v1/transactions/" + id, a.accessToken()).andExpect(status().isOk());
        api.get("/api/v1/transactions/" + id, a.accessToken()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersTransactions() throws Exception {
        Session a = api.register("a@example.com");
        Session b = api.register("b@example.com");
        String food = api.defaultCategoryId(a.accessToken(), "Food", "EXPENSE");
        String id = api.createTransaction(a.accessToken(), "EXPENSE", "10.00", food, "2026-11-01", null);

        api.get("/api/v1/transactions/" + id, b.accessToken()).andExpect(status().isNotFound());
        api.put("/api/v1/transactions/" + id, b.accessToken(), body("EXPENSE", "1", food))
                .andExpect(status().isNotFound());
        api.delete("/api/v1/transactions/" + id, b.accessToken()).andExpect(status().isNotFound());
        api.get("/api/v1/transactions", b.accessToken()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void cannotUseAnotherUsersCategory() throws Exception {
        Session a = api.register("a@example.com");
        Session b = api.register("b@example.com");
        String aCategory = api.createCategory(a.accessToken(), "Pets", "EXPENSE");
        api.post("/api/v1/transactions", b.accessToken(), body("EXPENSE", "5", aCategory))
                .andExpect(status().isNotFound());
    }

    private static String body(String type, String amount, String categoryId) {
        return """
                {"type":"%s","amount":%s,"categoryId":"%s","transactionDate":"2026-11-01"}
                """.formatted(type, amount, categoryId);
    }
}
