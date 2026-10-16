package com.alumni.alumni_management_system.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException {

    public static final String MESSAGE = "This email address is already registered.";

    public DuplicateEmailException() {
        super(HttpStatus.CONFLICT, MESSAGE, Map.of("email", MESSAGE));
    }
}