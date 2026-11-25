package com.example.expensetracker.common.exception;

/** Business-rule validation failure (400 VALIDATION_ERROR). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
