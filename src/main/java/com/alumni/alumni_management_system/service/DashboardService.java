package com.alumni.alumni_management_system.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.ContributionResponse;
import com.alumni.alumni_management_system.dto.DashboardSummaryResponse;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.ContributionRepository;

@Service
public class DashboardService {

    private static final int RECENT_LIMIT = 5;

    private final ProfileService profileService;
    private final ContributionService contributionService;
    private final AlumniProfileRepository profileRepository;
    private final ContributionRepository contributionRepository;

    public DashboardService(ProfileService profileService,
                            ContributionService contributionService,
                            AlumniProfileRepository profileRepository,
                            ContributionRepository contributionRepository) {
        this.profileService = profileService;
        this.contributionService = contributionService;
        this.profileRepository = profileRepository;
        this.contributionRepository = contributionRepository;
    }

    public DashboardSummaryResponse summary(String userId) {
        AlumniProfile profile = profileService.requireProfile(userId);
        long communitySize = profileRepository.count();
        long mine = contributionRepository.countByUserId(userId);
        List<ContributionResponse> recent = contributionService.recent(userId, RECENT_LIMIT);
        return new DashboardSummaryResponse(communitySize, mine, profileService.completionPercent(profile), recent);
    }
}