package com.alumni.alumni_management_system.service;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.alumni.alumni_management_system.dto.ValidationPatterns;
import com.alumni.alumni_management_system.exception.FieldValidationException;

final class InputChecks {

    static final int MIN_GRADUATION_YEAR = 1950;
    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 50;
    static final String PASSWORD_MESSAGE =
            "Password must be 8–64 characters with at least one letter and one number.";

    private static final Pattern PASSWORD_RULE = Pattern.compile(ValidationPatterns.PASSWORD_RULE);

    private InputChecks() {
    }

    static int maxGraduationYear() {
        return Year.now().getValue() + 6;
    }

    static void checkGraduationYear(Integer year, Map<String, String> errors) {
        if (year == null) {
            return; // the @NotNull annotation reports a missing year
        }
        int max = maxGraduationYear();
        if (year < MIN_GRADUATION_YEAR || year > max) {
            errors.put("graduationYear", "Enter a year between " + MIN_GRADUATION_YEAR + " and " + max + ".");
        }
    }

    /** 8-64 characters, at least one letter and one digit, and within BCrypt's 72-byte limit. */
    static boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        int length = password.length();
        if (length < 8 || length > 64) {
            return false;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return PASSWORD_RULE.matcher(password).matches();
    }

    static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    static Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    /** A case-insensitive "contains" pattern. User text is quoted, so it is never treated as a regex. */
    static Pattern containsIgnoreCase(String text) {
        return Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
    }
}