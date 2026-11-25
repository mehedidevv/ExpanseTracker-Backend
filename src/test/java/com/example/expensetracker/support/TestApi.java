package com.example.expensetracker.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Small fluent helper + test data builders over MockMvc. */
public class TestApi {

    private final MockMvc mvc;
    private final CapturingMailService mail;

    public TestApi(MockMvc mvc, CapturingMailService mail) {
        this.mvc = mvc;
        this.mail = mail;
    }

    public record Session(String accessToken, String refreshToken) {
    }

    // ---- raw HTTP ----

    public ResultActions get(String url, String token) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token));
    }

    public ResultActions post(String url, String token, String json) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions put(String url, String token, String json) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions delete(String url, String token) throws Exception {
        return mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url), token));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    // ---- builders ----

    /** Registers and verifies the email with the OTP; returns the logged-in session. */
    public Session register(String email) throws Exception {
        post("/api/v1/auth/register", null, """
                {"email":"%s","password":"password123","fullName":"Test User","currencyCode":"BDT"}
                """.formatted(email));
        String body = post("/api/v1/auth/verify-email", null, otpJson(email, mail.lastCode(email)))
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.data.accessToken"), JsonPath.read(body, "$.data.refreshToken"));
    }

    public static String otpJson(String email, String otp) {
        return "{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}";
    }

    /** Id of a seeded default category, e.g. ("Food", "EXPENSE"). */
    public String defaultCategoryId(String token, String name, String type) throws Exception {
        String body = get("/api/v1/categories?type=" + type, token).andReturn().getResponse().getContentAsString();
        java.util.List<String> ids = JsonPath.read(body, "$.data[?(@.name=='" + name + "' && @.systemDefault==true)].id");
        return ids.getFirst();
    }

    public String createCategory(String token, String name, String type) throws Exception {
        return idOf(post("/api/v1/categories", token, """
                {"name":"%s","type":"%s"}""".formatted(name, type)));
    }

    public String createTransaction(String token, String type, String amount, String categoryId, String date,
                                    String note) throws Exception {
        return idOf(post("/api/v1/transactions", token, """
                {"type":"%s","amount":%s,"categoryId":"%s","transactionDate":"%s","note":%s}
                """.formatted(type, amount, categoryId, date, note == null ? "null" : "\"" + note + "\"")));
    }

    public String createBudget(String token, String categoryId, String amount, int year, int month)
            throws Exception {
        return idOf(post("/api/v1/budgets", token, """
                {"categoryId":%s,"amount":%s,"year":%d,"month":%d}
                """.formatted(categoryId == null ? "null" : "\"" + categoryId + "\"", amount, year, month)));
    }

    public static String idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        int status = result.andReturn().getResponse().getStatus();
        if (status != 201) {
            throw new AssertionError("Expected 201 but got " + status + ": " + body);
        }
        return JsonPath.read(body, "$.data.id");
    }
}
