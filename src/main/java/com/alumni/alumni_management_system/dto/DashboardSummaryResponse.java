package com.alumni.alumni_management_system.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long alumniCount,
        long myContributionCount,
        int profileCompletion,
        List<ContributionResponse> recentContributions) {
}