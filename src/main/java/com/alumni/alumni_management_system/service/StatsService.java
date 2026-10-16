package com.alumni.alumni_management_system.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.AdminStatsResponse;
import com.alumni.alumni_management_system.dto.PublicStatsResponse;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.Contribution;
import com.alumni.alumni_management_system.model.ContributionStatus;
import com.alumni.alumni_management_system.model.ContributionType;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.ContributionRepository;

@Service
public class StatsService {

    private final AlumniProfileRepository profileRepository;
    private final ContributionRepository contributionRepository;
    private final MongoTemplate mongoTemplate;

    public StatsService(AlumniProfileRepository profileRepository,
                        ContributionRepository contributionRepository,
                        MongoTemplate mongoTemplate) {
        this.profileRepository = profileRepository;
        this.contributionRepository = contributionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /** Counts shown on the public landing page. Alumni, departments and batches cover directory-visible profiles. */
    public PublicStatsResponse publicStats() {
        long alumni = profileRepository.countByShowInDirectoryTrue();
        long departments = mongoTemplate.findDistinct(visibleQuery(), "department", AlumniProfile.class, String.class).size();
        long batches = mongoTemplate.findDistinct(visibleQuery(), "graduationYear", AlumniProfile.class, Integer.class).size();
        long contributions = contributionRepository.count();
        return new PublicStatsResponse(alumni, departments, batches, contributions);
    }

    public AdminStatsResponse adminStats() {
        long totalAlumni = profileRepository.count();
        long totalContributions = contributionRepository.count();
        long pending = contributionRepository.countByStatus(ContributionStatus.PENDING);

        BigDecimal verifiedDonations = contributionRepository
                .findByTypeAndStatus(ContributionType.DONATION, ContributionStatus.VERIFIED).stream()
                .map(Contribution::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (ContributionType type : ContributionType.values()) {
            byType.put(type.name(), contributionRepository.countByType(type));
        }
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ContributionStatus status : ContributionStatus.values()) {
            byStatus.put(status.name(), contributionRepository.countByStatus(status));
        }

        return new AdminStatsResponse(totalAlumni, totalContributions, pending, verifiedDonations, byType, byStatus);
    }

    private Query visibleQuery() {
        return new Query(Criteria.where("showInDirectory").is(true));
    }
}