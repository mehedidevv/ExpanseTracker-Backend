package com.example.expensetracker.category;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensetracker.support.AbstractIntegrationTest;
import com.example.expensetracker.support.TestApi.Session;
import org.junit.jupiter.api.Test;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listReturnsDefaultsPlusOwnFilteredByType() throws Exception {
        Session a = api.register("a@example.com");
        Session b = api.register("b@example.com");
        api.createCategory(a.accessToken(), "Pets", "EXPENSE");
        api.createCategory(b.accessToken(), "B only", "EXPENSE");

        api.get("/api/v1/categories", a.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(14)))
                .andExpect(jsonPath("$.data[*].name", hasItem("Pets")))
                .andExpect(jsonPath("$.data[*].name", not(hasItem("B only"))));

        api.get("/api/v1/categories?type=INCOME", a.accessToken())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[*].type", everyItem(is("INCOME"))));
    }

    @Test
    void duplicateNamesAreRejectedCaseInsensitively() throws Exception {
        Session a = api.register("a@example.com");
        api.post("/api/v1/categories", a.accessToken(), """
                {"name":"food","type":"EXPENSE"}""").andExpect(status().isConflict());
        api.createCategory(a.accessToken(), "Pets", "EXPENSE");
        api.post("/api/v1/categories", a.accessToken(), """
                {"name":"PETS","type":"EXPENSE"}""").andExpect(status().isConflict());
        // same name with a different type is fine
        api.post("/api/v1/categories", a.accessToken(), """
                {"name":"Pets","type":"INCOME"}""").andExpect(status().isCreated());
    }

    @Test
    void systemDefaultsAreReadOnly() throws Exception {
        Session a = api.register("a@example.com");
        String food = api.defaultCategoryId(a.accessToken(), "Food", "EXPENSE");
        api.put("/api/v1/categories/" + food, a.accessToken(), """
                {"name":"Groceries","type":"EXPENSE"}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        api.delete("/api/v1/categories/" + food, a.accessToken()).andExpect(status().isForbidden());
    }

    @Test
    void updateAndDeleteOwnCategory() throws Exception {
        Session a = api.register("a@example.com");
        String id = api.createCategory(a.accessToken(), "Pets", "EXPENSE");
        api.put("/api/v1/categories/" + id, a.accessToken(), """
                {"name":"Pet care","type":"EXPENSE","icon":"pets","color":"#FFAA00"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Pet care"))
                .andExpect(jsonPath("$.data.systemDefault").value(false));
        api.delete("/api/v1/categories/" + id, a.accessToken()).andExpect(status().isOk());
    }

    @Test
    void categoryInUseCannotBeDeletedOrChangeType() throws Exception {
        Session a = api.register("a@example.com");
        String id = api.createCategory(a.accessToken(), "Pets", "EXPENSE");
        api.createTransaction(a.accessToken(), "EXPENSE", "10.00", id, "2026-11-01", null);

        api.delete("/api/v1/categories/" + id, a.accessToken()).andExpect(status().isConflict());
        api.put("/api/v1/categories/" + id, a.accessToken(), """
                {"name":"Pets","type":"INCOME"}""").andExpect(status().isConflict());
    }

    @Test
    void otherUsersCategoryIsNotFound() throws Exception {
        Session a = api.register("a@example.com");
        Session b = api.register("b@example.com");
        String id = api.createCategory(a.accessToken(), "Pets", "EXPENSE");

        api.put("/api/v1/categories/" + id, b.accessToken(), """
                {"name":"Hacked","type":"EXPENSE"}""").andExpect(status().isNotFound());
        api.delete("/api/v1/categories/" + id, b.accessToken()).andExpect(status().isNotFound());
    }
}
