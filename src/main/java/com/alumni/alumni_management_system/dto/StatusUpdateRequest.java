package com.alumni.alumni_management_system.dto;

import com.alumni.alumni_management_system.model.ContributionStatus;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Status is required.")
        ContributionStatus status) {
}