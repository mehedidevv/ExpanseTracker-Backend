package com.example.expensetracker.common.mail;

/** Sends plain-text emails. Implementation is chosen by {@code app.mail.enabled}. */
public interface MailService {

    /** @throws com.example.expensetracker.common.exception.ApiException 503 if the email could not be sent */
    void send(String to, String subject, String body);
}
