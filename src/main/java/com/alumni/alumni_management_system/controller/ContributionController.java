package com.alumni.alumni_management_system.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.ContributionRequest;
import com.alumni.alumni_management_system.dto.ContributionResponse;
import com.alumni.alumni_management_system.security.AppUserDetails;
import com.alumni.alumni_management_system.service.ContributionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contributions")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @PostMapping
    public ResponseEntity<ContributionResponse> create(@AuthenticationPrincipal AppUserDetails user,
                                                       @Valid @RequestBody ContributionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contributionService.create(user.getId(), request));
    }

    @GetMapping("/me")
    public List<ContributionResponse> mine(@AuthenticationPrincipal AppUserDetails user) {
        return contributionService.listMine(user.getId());
    }
}