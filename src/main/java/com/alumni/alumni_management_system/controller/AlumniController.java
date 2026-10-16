package com.alumni.alumni_management_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.alumni_management_system.dto.DirectoryAlumniResponse;
import com.alumni.alumni_management_system.dto.FiltersResponse;
import com.alumni.alumni_management_system.dto.PageResponse;
import com.alumni.alumni_management_system.service.AlumniDirectoryService;

@RestController
@RequestMapping("/api/alumni")
public class AlumniController {

    private final AlumniDirectoryService directoryService;

    public AlumniController(AlumniDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping
    public PageResponse<DirectoryAlumniResponse> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "graduationYear", required = false) Integer graduationYear,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return directoryService.search(q, department, graduationYear, page, size);
    }

    @GetMapping("/filters")
    public FiltersResponse filters() {
        return directoryService.filters();
    }

    @GetMapping("/{id}")
    public DirectoryAlumniResponse get(@PathVariable("id") String id) {
        return directoryService.getById(id);
    }
}