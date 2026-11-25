package com.example.expensetracker.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests against a REAL PostgreSQL.
 * <ul>
 *   <li>Docker available (CI): a shared Testcontainers {@code postgres:17-alpine} is started.</li>
 *   <li>No Docker (local dev): uses {@code TEST_DB_URL}, default
 *       {@code jdbc:postgresql://localhost:5432/expense_tracker_test} (see scripts/setup-local-db.sh).</li>
 * </ul>
 * All user data is wiped before each test; seeded default categories are kept.
 * Emails are captured in memory by {@link CapturingMailService} (read OTP codes from there).
 */
@SpringBootTest
@Import(CapturingMailService.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = startContainerIfDockerAvailable();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected CapturingMailService mail;

    protected TestApi api;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> env("TEST_DB_URL",
                    "jdbc:postgresql://localhost:5432/expense_tracker_test"));
            registry.add("spring.datasource.username", () -> env("TEST_DB_USERNAME", "expense"));
            registry.add("spring.datasource.password", () -> env("TEST_DB_PASSWORD", "expense"));
        }
    }

    @BeforeEach
    void cleanDatabaseAndInitApi() {
        jdbc.execute("TRUNCATE transactions, budgets, refresh_tokens, otp_codes, pending_registrations");
        jdbc.execute("DELETE FROM categories WHERE user_id IS NOT NULL");
        jdbc.execute("DELETE FROM users");
        mail.clear();
        api = new TestApi(mockMvc, mail);
    }

    private static PostgreSQLContainer startContainerIfDockerAvailable() {
        boolean docker;
        try {
            docker = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable e) {
            docker = false;
        }
        if (!docker) {
            return null;
        }
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine");
        container.start();
        return container;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
