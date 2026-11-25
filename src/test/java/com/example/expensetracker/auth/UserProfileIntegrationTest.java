package com.example.expensetracker.auth;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensetracker.support.AbstractIntegrationTest;
import com.example.expensetracker.support.TestApi.Session;
import org.junit.jupiter.api.Test;

class UserProfileIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getAndUpdateProfile() throws Exception {
        Session s = api.register("gina@example.com");
        api.get("/api/v1/users/me", s.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("gina@example.com"))
                .andExpect(jsonPath("$.data.currencyCode").value("BDT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        api.put("/api/v1/users/me", s.accessToken(), """
                        {"fullName":"Gina R","currencyCode":"USD"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Gina R"))
                .andExpect(jsonPath("$.data.currencyCode").value("USD"));
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndRevokesSessions() throws Exception {
        Session s = api.register("hank@example.com");
        api.put("/api/v1/users/me/password", s.accessToken(), """
                        {"currentPassword":"wrong-one","newPassword":"newpassword1"}""")
                .andExpect(status().isBadRequest());

        api.put("/api/v1/users/me/password", s.accessToken(), """
                        {"currentPassword":"password123","newPassword":"newpassword1"}""")
                .andExpect(status().isOk());

        api.post("/api/v1/auth/login", null, """
                        {"email":"hank@example.com","password":"password123"}""")
                .andExpect(status().isUnauthorized());
        api.post("/api/v1/auth/login", null, """
                        {"email":"hank@example.com","password":"newpassword1"}""")
                .andExpect(status().isOk());
        api.post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + s.refreshToken() + "\"}")
                .andExpect(status().isUnauthorized());
    }
}
