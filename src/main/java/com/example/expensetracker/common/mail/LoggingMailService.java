package com.example.expensetracker.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Dev fallback when {@code app.mail.enabled=false}: prints the email (including OTP codes) to the log
 * instead of sending it. Never use in production.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailService.class);

    @EventListener(ApplicationReadyEvent.class)
    public void announce() {
        log.warn("Mail: MAIL_ENABLED=false, emails are NOT sent. OTP codes are printed in this log.");
    }

    @Override
    public void send(String to, String subject, String body) {
        log.warn("MAIL DISABLED (app.mail.enabled=false). Email to {} | {} | {}", to, subject, body);
    }
}
