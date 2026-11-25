package com.example.expensetracker.common.exception;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
