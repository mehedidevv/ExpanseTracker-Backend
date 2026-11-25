package com.example.expensetracker.common.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.NOT_FOUND, resource + " not found: " + id);
    }
}
