package com.alumni.alumni_management_system.dto;

import com.alumni.alumni_management_system.model.AlumniProfile;

public record AdminAlumniResponse(
        String id,
        String userId,
        String fullName,
        String email,
        String phone,
        String department,
        String degree,
        Integer graduationYear,
        String company,
        String jobTitle,
        String city,
        String linkedinUrl,
        String bio,
        boolean showInDirectory,
        String createdAt) {

    public static AdminAlumniResponse of(AlumniProfile p, String email) {
        return new AdminAlumniResponse(
                p.getId(), p.getUserId(), p.getFullName(), email, p.getPhone(), p.getDepartment(),
                p.getDegree(), p.getGraduationYear(), p.getCompany(), p.getJobTitle(), p.getCity(),
                p.getLinkedinUrl(), p.getBio(), p.isShowInDirectory(),
                p.getCreatedAt() == null ? null : p.getCreatedAt().toString());
    }
}