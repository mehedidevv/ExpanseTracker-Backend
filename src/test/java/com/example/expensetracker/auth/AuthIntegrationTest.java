package com.example.expensetracker.auth;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.expensetracker.support.AbstractIntegrationTest;
import com.example.expensetracker.support.TestApi.Session;
import static com.example.expensetracker.support.TestApi.otpJson;
import org.junit.jupiter.api.Test;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private AuthCleanupJob cleanupJob;

    @Test
    void registerSendsOtpAndVerifyEmailReturnsTokens() throws Exception {
        api.post("/api/v1/auth/register", null, """
                        {"email":"Alice@Example.COM","password":"password123","fullName":" Alice ","currencyCode":"BDT"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        String firstCode = mail.lastCode("alice@example.com"); // stored + mailed lowercase
        assertUserCount(0); // no account before the code is verified

        // cannot log in before verifying; login emails a fresh code
        api.post("/api/v1/auth/login", null, LOGIN_ALICE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        String code = mail.lastCode("alice@example.com");
        org.junit.jupiter.api.Assumptions.assumeFalse(firstCode.equals(code));
        api.post("/api/v1/auth/verify-email", null, otpJson("alice@example.com", firstCode))
                .andExpect(status().isBadRequest()); // replaced by the newer code

        api.post("/api/v1/auth/verify-email", null, otpJson("alice@example.com", wrong(code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect code. 3 attempts left"));
        api.post("/api/v1/auth/verify-email", null, otpJson("ALICE@example.com", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));

        // code is single use: the account now exists
        api.post("/api/v1/auth/verify-email", null, otpJson("alice@example.com", code))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email is already verified. Please log in"));
        api.post("/api/v1/auth/login", null, LOGIN_ALICE).andExpect(status().isOk());
        assertUserCount(1);
    }

    @Test
    void invalidEmailFormatsAreRejectedAndNothingIsSaved() throws Exception {
        for (String bad : new String[] {"nope", "a@b", "a@b.c", "a b@example.com", "a@example.", "@example.com"}) {
            api.post("/api/v1/auth/register", null, """
                            {"email":"%s","password":"password123","fullName":"X","currencyCode":"BDT"}""".formatted(bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                    .andExpect(jsonPath("$.message").value("email must be a valid email address, e.g. name@example.com"));
        }
        // wrong field name ("name" instead of "fullName") is rejected too
        api.post("/api/v1/auth/register", null, """
                        {"email":"ok@example.com","password":"password123","name":"X","currencyCode":"BDT"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fullName"));
        assertUserCount(0);
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("select count(*) from pending_registrations", Integer.class));
    }

    @Test
    void unverifiedSignUpCannotLogInWithWrongPassword() throws Exception {
        register("liam@example.com");
        api.post("/api/v1/auth/login", null, """
                        {"email":"liam@example.com","password":"wrong-password"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect password"));
    }

    private void assertUserCount(int expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected,
                jdbc.queryForObject("select count(*) from users", Integer.class));
    }

    @Test
    void otpIsBurnedAfterTooManyWrongAttempts() throws Exception {
        register("gina@example.com");
        String code = mail.lastCode("gina@example.com");
        for (int i = 1; i < 5; i++) {
            api.post("/api/v1/auth/verify-email", null, otpJson("gina@example.com", wrong(code)))
                    .andExpect(status().isBadRequest());
        }
        api.post("/api/v1/auth/verify-email", null, otpJson("gina@example.com", wrong(code)))
                .andExpect(status().isTooManyRequests());
        // even the right code no longer works
        api.post("/api/v1/auth/verify-email", null, otpJson("gina@example.com", code))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtpInvalidatesThePreviousCode() throws Exception {
        register("ivan@example.com");
        String first = mail.lastCode("ivan@example.com");
        api.post("/api/v1/auth/resend-otp", null, """
                        {"email":"ivan@example.com"}""")
                .andExpect(status().isOk());
        String second = mail.lastCode("ivan@example.com");
        org.junit.jupiter.api.Assumptions.assumeFalse(first.equals(second)); // 1-in-a-million collision

        api.post("/api/v1/auth/verify-email", null, otpJson("ivan@example.com", first))
                .andExpect(status().isBadRequest());
        api.post("/api/v1/auth/verify-email", null, otpJson("ivan@example.com", second))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPasswordFlowResetsPasswordAndSignsOutEverywhere() throws Exception {
        Session s = api.register("judy@example.com");
        api.post("/api/v1/auth/forgot-password", null, """
                        {"email":"judy@example.com"}""")
                .andExpect(status().isOk());
        String code = mail.lastCode("judy@example.com");

        api.post("/api/v1/auth/verify-reset-otp", null, otpJson("judy@example.com", wrong(code)))
                .andExpect(status().isBadRequest());
        String body = api.post("/api/v1/auth/verify-reset-otp", null, otpJson("judy@example.com", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        String resetToken = com.jayway.jsonpath.JsonPath.read(body, "$.data.resetToken");

        String reset = "{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"brand-new-pass\"}";
        api.post("/api/v1/auth/reset-password", null, reset).andExpect(status().isOk());
        // reset token is single use
        api.post("/api/v1/auth/reset-password", null, reset).andExpect(status().isBadRequest());

        api.post("/api/v1/auth/login", null, """
                        {"email":"judy@example.com","password":"password123"}""")
                .andExpect(status().isUnauthorized());
        api.post("/api/v1/auth/login", null, """
                        {"email":"judy@example.com","password":"brand-new-pass"}""")
                .andExpect(status().isOk());
        // old sessions were revoked
        api.post("/api/v1/auth/refresh", null, refresh(s.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownEmailGetsExactMessages() throws Exception {
        api.post("/api/v1/auth/forgot-password", null, """
                        {"email":"nobody@example.com"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No account found with this email"));
        api.post("/api/v1/auth/login", null, """
                        {"email":"nobody@example.com","password":"password123"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No account found with this email"));
        api.post("/api/v1/auth/resend-otp", null, """
                        {"email":"nobody@example.com"}""")
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertFalse(mail.hasMailFor("nobody@example.com"));
    }

    @Test
    void unverifiedSignUpIsRemovedAfterOneHourAndCanRegisterAgain() throws Exception {
        register("mona@example.com");
        // pretend the code was sent 61 minutes ago
        jdbc.update("update pending_registrations set last_sent_at = now() - interval '61 minutes'");

        api.post("/api/v1/auth/verify-email", null, otpJson("mona@example.com", mail.lastCode("mona@example.com")))
                .andExpect(status().isNotFound());
        api.post("/api/v1/auth/login", null, """
                        {"email":"mona@example.com","password":"password123"}""")
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertEquals(1, cleanupJob.purgeUnverifiedSignUps());

        register("mona@example.com"); // fresh sign-up works, no cooldown
        api.post("/api/v1/auth/verify-email", null, otpJson("mona@example.com", mail.lastCode("mona@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void unverifiedEmailCanRegisterAgainAndNewDetailsWin() throws Exception {
        register("kate@example.com");
        api.post("/api/v1/auth/register", null, """
                        {"email":"kate@example.com","password":"second-pass1","fullName":"Kate","currencyCode":"USD"}""")
                .andExpect(status().isCreated());
        api.post("/api/v1/auth/verify-email", null, otpJson("kate@example.com", mail.lastCode("kate@example.com")))
                .andExpect(status().isOk());
        api.post("/api/v1/auth/login", null, """
                        {"email":"kate@example.com","password":"password123"}""")
                .andExpect(status().isUnauthorized());
        api.post("/api/v1/auth/login", null, """
                        {"email":"kate@example.com","password":"second-pass1"}""")
                .andExpect(status().isOk());
    }

    @Test
    void everyResponseUsesTheStandardEnvelope() throws Exception {
        api.register("nina@example.com");
        api.post("/api/v1/auth/login", null, """
                        {"email":"nina@example.com","password":"password123"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
        api.post("/api/v1/auth/login", null, """
                        {"email":"nina@example.com","password":"wrong-pass"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Incorrect password"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void duplicateEmailIsCaseInsensitiveConflict() throws Exception {
        api.register("bob@example.com");
        api.post("/api/v1/auth/register", null, """
                        {"email":"BOB@example.com","password":"password123","fullName":"Bob","currencyCode":"USD"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void registerValidationReturnsFieldErrors() throws Exception {
        api.post("/api/v1/auth/register", null, """
                        {"email":"nope","password":"short","fullName":"","currencyCode":"bdt"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("email", "password", "fullName", "currencyCode")));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        api.register("carol@example.com");
        api.post("/api/v1/auth/login", null, """
                        {"email":"carol@example.com","password":"wrong-password"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpointsRequireValidToken() throws Exception {
        api.get("/api/v1/users/me", null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        api.get("/api/v1/users/me", "garbage.token.value").andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokensAndReuseRevokesEverything() throws Exception {
        Session s = api.register("dave@example.com");
        String body = api.post("/api/v1/auth/refresh", null, refresh(s.refreshToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newRefresh = com.jayway.jsonpath.JsonPath.read(body, "$.data.refreshToken");

        // old token is now revoked; re-using it is treated as theft
        api.post("/api/v1/auth/refresh", null, refresh(s.refreshToken())).andExpect(status().isUnauthorized());
        // ...which also revoked the new token
        api.post("/api/v1/auth/refresh", null, refresh(newRefresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Session s = api.register("erin@example.com");
        api.post("/api/v1/auth/logout", s.accessToken(), refresh(s.refreshToken()))
                .andExpect(status().isOk());
        api.post("/api/v1/auth/refresh", null, refresh(s.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        Session s = api.register("frank@example.com");
        api.post("/api/v1/auth/logout", null, refresh(s.refreshToken())).andExpect(status().isUnauthorized());
    }

    private static final String LOGIN_ALICE = """
            {"email":"alice@example.com","password":"password123"}""";

    private void register(String email) throws Exception {
        api.post("/api/v1/auth/register", null, """
                        {"email":"%s","password":"password123","fullName":"T","currencyCode":"BDT"}""".formatted(email))
                .andExpect(status().isCreated());
    }

    /** A different valid 6-digit code. */
    private static String wrong(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    private static String refresh(String token) {
        return "{\"refreshToken\":\"" + token + "\"}";
    }
}
