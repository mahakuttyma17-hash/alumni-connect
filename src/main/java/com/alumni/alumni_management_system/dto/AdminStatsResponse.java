package com.alumni.alumni_management_system.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AdminStatsResponse(
        long totalAlumni,
        long totalContributions,
        long pendingContributions,
        BigDecimal verifiedDonationAmount,
        Map<String, Long> byType,
        Map<String, Long> byStatus) {
}