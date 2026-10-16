package com.alumni.alumni_management_system.dto;

public record PublicStatsResponse(
        long alumniCount,
        long departmentCount,
        long batchCount,
        long contributionCount) {
}