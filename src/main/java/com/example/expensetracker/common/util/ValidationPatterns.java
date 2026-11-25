package com.example.expensetracker.common.util;

/** Shared validation rules (used in DTO annotations, so they must be compile-time constants). */
public final class ValidationPatterns {

    /** name@domain.tld: no spaces, a dot in the domain, a TLD of 2+ letters. */
    public static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    public static final String EMAIL_MESSAGE = "must be a valid email address, e.g. name@example.com";

    private ValidationPatterns() {
    }
}
