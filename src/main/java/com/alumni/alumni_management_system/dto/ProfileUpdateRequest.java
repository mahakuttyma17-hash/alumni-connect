package com.alumni.alumni_management_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 100, message = "Full name must be at most 100 characters.")
        String fullName,

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

        @Size(max = 100, message = "Company must be at most 100 characters.")
        String company,

        @Size(max = 100, message = "Job title must be at most 100 characters.")
        String jobTitle,

        @Size(max = 80, message = "City must be at most 80 characters.")
        String city,

        @Size(max = 200, message = "LinkedIn URL must be at most 200 characters.")
        @Pattern(regexp = ValidationPatterns.LINKEDIN, flags = Pattern.Flag.CASE_INSENSITIVE,
                message = ValidationPatterns.LINKEDIN_MESSAGE)
        String linkedinUrl,

        @Size(max = 500, message = "Bio must be at most 500 characters.")
        String bio,

        Boolean showInDirectory) {

    public ProfileUpdateRequest {
        fullName = TextUtil.clean(fullName);
        phone = TextUtil.clean(phone);
        department = TextUtil.clean(department);
        degree = TextUtil.clean(degree);
        company = TextUtil.clean(company);
        jobTitle = TextUtil.clean(jobTitle);
        city = TextUtil.clean(city);
        linkedinUrl = TextUtil.clean(linkedinUrl);
        bio = TextUtil.clean(bio);
    }
}