package com.alumni.alumni_management_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 100, message = "Full name must be at most 100 characters.")
        String fullName,

        @NotBlank(message = "Email address is required.")
        @Size(max = 120, message = "Email must be at most 120 characters.")
        @Pattern(regexp = ValidationPatterns.EMAIL, message = ValidationPatterns.EMAIL_MESSAGE)
        String email,

        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @NotBlank(message = "Department is required.")
        @Size(max = 100, message = "Department must be at most 100 characters.")
        String department,

        @NotBlank(message = "Degree is required.")
        @Size(max = 50, message = "Degree must be at most 50 characters.")
        String degree,

        @NotNull(message = "Graduation year is required.")
        @Min(value = 1950, message = "Enter a valid graduation year (1950 or later).")
        Integer graduationYear,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 64, message = ValidationPatterns.PASSWORD_LENGTH_MESSAGE)
        @Pattern(regexp = ValidationPatterns.PASSWORD_RULE, message = ValidationPatterns.PASSWORD_RULE_MESSAGE)
        String password,

        @NotBlank(message = "Please confirm your password.")
        String confirmPassword) {

    public RegisterRequest {
        fullName = TextUtil.clean(fullName);
        email = TextUtil.email(email);
        phone = TextUtil.clean(phone);
        department = TextUtil.clean(department);
        degree = TextUtil.clean(degree);
        // Passwords are never trimmed or altered.
    }
}