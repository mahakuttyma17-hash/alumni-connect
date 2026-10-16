package com.alumni.alumni_management_system.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class FieldValidationException extends ApiException {

    public FieldValidationException(Map<String, String> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields.", fieldErrors);
    }
}