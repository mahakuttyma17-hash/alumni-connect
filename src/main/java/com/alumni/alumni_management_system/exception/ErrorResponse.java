package com.alumni.alumni_management_system.exception;

import java.util.Map;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {
}