package com.example.expensetracker.common.mail;

import com.example.expensetracker.common.exception.ApiException;
import com.example.expensetracker.common.exception.ErrorCode;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/** Real email over SMTP ({@code spring.mail.*}). Active when {@code app.mail.enabled=true}. */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);

    private final JavaMailSender mailSender;
    private final MailSettings settings;

    public SmtpMailService(JavaMailSender mailSender, MailSettings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
        // Google shows App Passwords as "abcd efgh ijkl mnop"; SMTP needs them without spaces.
        if (mailSender instanceof JavaMailSenderImpl impl && impl.getPassword() != null) {
            impl.setPassword(impl.getPassword().replace(" ", ""));
        }
    }

    /** Checks the SMTP login once at startup so a bad config shows up in the log immediately. */
    @EventListener(ApplicationReadyEvent.class)
    public void checkConnection() {
        if (!(mailSender instanceof JavaMailSenderImpl impl)) {
            return;
        }
        try {
            impl.testConnection();
            log.info("Mail: SMTP login OK ({}:{} as {})", impl.getHost(), impl.getPort(), impl.getUsername());
        } catch (MessagingException e) {
            log.error("Mail: SMTP login FAILED ({}:{} as {}): {}. OTP emails will not be sent. "
                            + "Check MAIL_USERNAME / MAIL_PASSWORD (Gmail needs an App Password) in .env",
                    impl.getHost(), impl.getPort(), impl.getUsername(), e.getMessage());
        }
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("Mail sent to {} ({})", to, subject);
        } catch (MailException e) {
            log.error("Sending email to {} failed: {}", to, e.getMessage());
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "Could not send email. Please try again later");
        }
    }
}
