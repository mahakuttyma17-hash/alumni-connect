package com.alumni.alumni_management_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.AdminAlumniRequest;
import com.alumni.alumni_management_system.dto.AdminAlumniResponse;
import com.alumni.alumni_management_system.dto.AdminContributionResponse;
import com.alumni.alumni_management_system.dto.AdminStatsResponse;
import com.alumni.alumni_management_system.dto.PageResponse;
import com.alumni.alumni_management_system.dto.StatusUpdateRequest;
import com.alumni.alumni_management_system.model.ContributionStatus;
import com.alumni.alumni_management_system.model.ContributionType;
import com.alumni.alumni_management_system.service.AdminAlumniService;
import com.alumni.alumni_management_system.service.ContributionService;
import com.alumni.alumni_management_system.service.StatsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StatsService statsService;
    private final AdminAlumniService alumniService;
    private final ContributionService contributionService;

    public AdminController(StatsService statsService,
                           AdminAlumniService alumniService,
                           ContributionService contributionService) {
        this.statsService = statsService;
        this.alumniService = alumniService;
        this.contributionService = contributionService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return statsService.adminStats();
    }

    /* ---------- alumni CRUD ---------- */

    @GetMapping("/alumni")
    public PageResponse<AdminAlumniResponse> searchAlumni(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return alumniService.search(q, department, page, size);
    }

    @PostMapping("/alumni")
    public ResponseEntity<AdminAlumniResponse> createAlumni(@Valid @RequestBody AdminAlumniRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alumniService.create(request));
    }

    @GetMapping("/alumni/{id}")
    public AdminAlumniResponse getAlumni(@PathVariable("id") String id) {
        return alumniService.get(id);
    }

    @PutMapping("/alumni/{id}")
    public AdminAlumniResponse updateAlumni(@PathVariable("id") String id,
                                            @Valid @RequestBody AdminAlumniRequest request) {
        return alumniService.update(id, request);
    }

    @DeleteMapping("/alumni/{id}")
    public ResponseEntity<Void> deleteAlumni(@PathVariable("id") String id) {
        alumniService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /* ---------- contributions ---------- */

    @GetMapping("/contributions")
    public PageResponse<AdminContributionResponse> contributions(
            @RequestParam(name = "type", required = false) ContributionType type,
            @RequestParam(name = "status", required = false) ContributionStatus status,
            @RequestParam(name = "alumniId", required = false) String alumniId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return contributionService.adminList(type, status, alumniId, page, size);
    }

    @PatchMapping("/contributions/{id}/status")
    public AdminContributionResponse updateStatus(@PathVariable("id") String id,
                                                  @Valid @RequestBody StatusUpdateRequest request) {
        return contributionService.updateStatus(id, request.status());
    }
}