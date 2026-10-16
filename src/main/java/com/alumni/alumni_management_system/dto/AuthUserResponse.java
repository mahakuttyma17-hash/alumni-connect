package com.alumni.alumni_management_system.dto;

public record AuthUserResponse(
        String id,
        String email,
        String fullName,
        String role,
        String redirect) {
}