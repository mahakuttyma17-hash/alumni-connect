package com.alumni.alumni_management_system.dto;

import java.math.BigDecimal;

import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.Contribution;
import com.alumni.alumni_management_system.model.ContributionStatus;
import com.alumni.alumni_management_system.model.ContributionType;

public record AdminContributionResponse(
        String id,
        ContributionType type,
        String description,
        BigDecimal amount,
        String contributionDate,
        ContributionStatus status,
        String createdAt,
        String alumniId,
        String alumniName) {

    /** The profile may be null if the alumnus record no longer exists. */
    public static AdminContributionResponse of(Contribution c, AlumniProfile profile) {
        return new AdminContributionResponse(
                c.getId(), c.getType(), c.getDescription(), c.getAmount(),
                c.getContributionDate() == null ? null : c.getContributionDate().toString(),
                c.getStatus(),
                c.getCreatedAt() == null ? null : c.getCreatedAt().toString(),
                profile == null ? null : profile.getId(),
                profile == null ? "Unknown alumnus" : profile.getFullName());
    }
}