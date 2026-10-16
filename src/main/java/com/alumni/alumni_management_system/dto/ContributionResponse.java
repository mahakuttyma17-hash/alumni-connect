package com.alumni.alumni_management_system.dto;

import java.math.BigDecimal;

import com.alumni.alumni_management_system.model.Contribution;
import com.alumni.alumni_management_system.model.ContributionStatus;
import com.alumni.alumni_management_system.model.ContributionType;

public record ContributionResponse(
        String id,
        ContributionType type,
        String description,
        BigDecimal amount,
        String contributionDate,
        ContributionStatus status,
        String createdAt) {

    public static ContributionResponse from(Contribution c) {
        return new ContributionResponse(
                c.getId(), c.getType(), c.getDescription(), c.getAmount(),
                c.getContributionDate() == null ? null : c.getContributionDate().toString(),
                c.getStatus(),
                c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
    }
}