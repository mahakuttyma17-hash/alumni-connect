package com.alumni.alumni_management_system.dto;

import java.math.BigDecimal;

import com.alumni.alumni_management_system.model.ContributionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContributionRequest(
        @NotNull(message = "Contribution type is required.")
        ContributionType type,

        @NotBlank(message = "Description is required.")
        @Size(max = 500, message = "Description must be at most 500 characters.")
        String description,

        BigDecimal amount,

        @NotBlank(message = "Date is required.")
        String contributionDate) {

    public ContributionRequest {
        description = TextUtil.clean(description);
        contributionDate = TextUtil.clean(contributionDate);
    }
}