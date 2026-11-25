package com.example.expensetracker.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.mail.*}. SMTP server settings themselves live under {@code spring.mail.*}. */
@ConfigurationProperties(prefix = "app.mail")
/** @param verifyDomain reject sign-up emails whose domain has no mail server (DNS check) */
public record MailSettings(boolean enabled, String from, boolean verifyDomain) {
}
