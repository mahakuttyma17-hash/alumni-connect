package com.alumni.alumni_management_system.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.DashboardSummaryResponse;
import com.alumni.alumni_management_system.security.AppUserDetails;
import com.alumni.alumni_management_system.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal AppUserDetails user) {
        return dashboardService.summary(user.getId());
    }
}