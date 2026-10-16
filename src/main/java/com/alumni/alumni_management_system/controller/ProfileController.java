package com.alumni.alumni_management_system.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.ProfileResponse;
import com.alumni.alumni_management_system.dto.ProfileUpdateRequest;
import com.alumni.alumni_management_system.security.AppUserDetails;
import com.alumni.alumni_management_system.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getMine(@AuthenticationPrincipal AppUserDetails user) {
        return profileService.getMyProfile(user.getId());
    }

    @PutMapping("/me")
    public ProfileResponse updateMine(@AuthenticationPrincipal AppUserDetails user,
                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateMyProfile(user.getId(), request);
    }
}