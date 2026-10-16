package com.alumni.alumni_management_system.dto;

import com.alumni.alumni_management_system.model.AlumniProfile;

public record ProfileResponse(
        String id,
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
        boolean showInDirectory) {

    public static ProfileResponse of(AlumniProfile p, String email) {
        return new ProfileResponse(
                p.getId(), p.getFullName(), email, p.getPhone(), p.getDepartment(), p.getDegree(),
                p.getGraduationYear(), p.getCompany(), p.getJobTitle(), p.getCity(), p.getLinkedinUrl(),
                p.getBio(), p.isShowInDirectory());
    }
}