package com.alumni.alumni_management_system.dto;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";
    public static final String EMAIL_MESSAGE = "Enter a valid email address.";

    public static final String PHONE = "^[+]?[0-9 ()-]{7,20}$";
    public static final String PHONE_MESSAGE = "Enter a valid phone number (digits, spaces, + ( ) - only).";

    public static final String LINKEDIN = "^https?://([a-z]{2,3}\\.)?linkedin\\.com/\\S+$";
    public static final String LINKEDIN_MESSAGE = "Enter a valid LinkedIn URL, e.g. https://www.linkedin.com/in/your-name";

    /** At least one letter and one digit. Length is checked separately. */
    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String PASSWORD_RULE_MESSAGE = "Password needs at least one letter and one number.";
    public static final String PASSWORD_LENGTH_MESSAGE = "Password must be 8–64 characters.";
}