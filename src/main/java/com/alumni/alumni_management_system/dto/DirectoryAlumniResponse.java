package com.alumni.alumni_management_system.dto;

import com.alumni.alumni_management_system.model.AlumniProfile;

public record DirectoryAlumniResponse(
        String id,
        String fullName,
        String department,
        String degree,
        Integer graduationYear,
        String jobTitle,
        String company,
        String city,
        String linkedinUrl) {

    public static DirectoryAlumniResponse from(AlumniProfile p) {
        return new DirectoryAlumniResponse(
                p.getId(), p.getFullName(), p.getDepartment(), p.getDegree(), p.getGraduationYear(),
                p.getJobTitle(), p.getCompany(), p.getCity(), p.getLinkedinUrl());
    }
}