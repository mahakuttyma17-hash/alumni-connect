package com.alumni.alumni_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/api/status")
    public String home() {
        return "Alumni Management System Backend is Running!";
    }
}