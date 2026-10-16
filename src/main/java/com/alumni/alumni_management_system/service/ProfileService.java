package com.alumni.alumni_management_system.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alumni.alumni_management_system.dto.ProfileResponse;
import com.alumni.alumni_management_system.dto.ProfileUpdateRequest;
import com.alumni.alumni_management_system.exception.ResourceNotFoundException;
import com.alumni.alumni_management_system.model.AlumniProfile;
import com.alumni.alumni_management_system.model.User;
import com.alumni.alumni_management_system.repository.AlumniProfileRepository;
import com.alumni.alumni_management_system.repository.UserRepository;

@Service
public class ProfileService {

    private static final int TRACKED_FIELDS = 10;

    private final AlumniProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(AlumniProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public AlumniProfile requireProfile(String userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No alumni profile exists for this account."));
    }

    public ProfileResponse getMyProfile(String userId) {
        AlumniProfile profile = requireProfile(userId);
        return ProfileResponse.of(profile, emailOf(userId));
    }

    public ProfileResponse updateMyProfile(String userId, ProfileUpdateRequest request) {
        AlumniProfile profile = requireProfile(userId);
        applyFields(profile, request);
        profile = profileRepository.save(profile);
        return ProfileResponse.of(profile, emailOf(userId));
    }

    /**
     * Copies the editable fields onto the profile (in memory only; the caller saves it).
     * Used for self-service edits and for admin create/edit.
     */
    public void applyFields(AlumniProfile target, ProfileUpdateRequest in) {
        Map<String, String> errors = new LinkedHashMap<>();
        InputChecks.checkGraduationYear(in.graduationYear(), errors);
        InputChecks.throwIfAny(errors);

        target.setFullName(in.fullName());
        target.setPhone(in.phone());
        target.setDepartment(in.department());
        target.setDegree(in.degree());
        target.setGraduationYear(in.graduationYear());
        target.setCompany(in.company());
        target.setJobTitle(in.jobTitle());
        target.setCity(in.city());
        target.setLinkedinUrl(in.linkedinUrl());
        target.setBio(in.bio());
        if (in.showInDirectory() != null) {
            target.setShowInDirectory(in.showInDirectory());
        }

        Instant now = Instant.now();
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(now);
        }
        target.setUpdatedAt(now);
    }

    /** Percentage (0-100) of the ten profile fields that are filled in. */
    public int completionPercent(AlumniProfile p) {
        int filled = 0;
        if (hasText(p.getFullName())) filled++;
        if (hasText(p.getPhone())) filled++;
        if (hasText(p.getDepartment())) filled++;
        if (hasText(p.getDegree())) filled++;
        if (p.getGraduationYear() != null) filled++;
        if (hasText(p.getCompany())) filled++;
        if (hasText(p.getJobTitle())) filled++;
        if (hasText(p.getCity())) filled++;
        if (hasText(p.getLinkedinUrl())) filled++;
        if (hasText(p.getBio())) filled++;
        return filled * 100 / TRACKED_FIELDS;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String emailOf(String userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }
}