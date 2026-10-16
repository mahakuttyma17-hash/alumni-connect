package com.alumni.alumni_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.PublicStatsResponse;
import com.alumni.alumni_management_system.service.StatsService;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final StatsService statsService;

    public PublicController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public PublicStatsResponse stats() {
        return statsService.publicStats();
    }
}