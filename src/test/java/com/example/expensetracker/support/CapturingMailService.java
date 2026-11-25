package com.example.expensetracker.support;

import com.example.expensetracker.common.mail.MailService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Primary;

/** Test replacement for real email: keeps the last email per recipient so tests can read OTP codes. */
@Primary
public class CapturingMailService implements MailService {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    private final Map<String, String> lastBodyByRecipient = new ConcurrentHashMap<>();

    @Override
    public void send(String to, String subject, String body) {
        lastBodyByRecipient.put(to, body);
    }

    /** The 6-digit code from the last email sent to this address. */
    public String lastCode(String email) {
        String body = lastBodyByRecipient.get(email);
        if (body == null) {
            throw new AssertionError("No email was sent to " + email);
        }
        Matcher m = CODE.matcher(body);
        if (!m.find()) {
            throw new AssertionError("No code in email: " + body);
        }
        return m.group(1);
    }

    public boolean hasMailFor(String email) {
        return lastBodyByRecipient.containsKey(email);
    }

    public void clear() {
        lastBodyByRecipient.clear();
    }
}
